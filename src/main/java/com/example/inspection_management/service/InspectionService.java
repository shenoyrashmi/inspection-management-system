package com.example.inspection_management.service;

import com.example.inspection_management.dto.InspectionRequestDTO;
import com.example.inspection_management.dto.InspectionResponseDTO;
import com.example.inspection_management.dto.InspectionResultRequestDTO;
import com.example.inspection_management.entity.Asset;
import com.example.inspection_management.entity.Inspection;
import com.example.inspection_management.entity.InspectionStatus;
import com.example.inspection_management.entity.User;
import com.example.inspection_management.exception.ResourceNotFoundException;
import com.example.inspection_management.exception.InspectionAccessDeniedException;
import com.example.inspection_management.repository.AssetRepository;
import com.example.inspection_management.repository.InspectionRepository;
import com.example.inspection_management.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InspectionService {

    private final InspectionRepository inspectionRepository;
    private final AssetRepository assetRepository;
    private final UserRepository userRepository;
    private final MaintenanceRequestService maintenanceRequestService;

    public InspectionService(
            InspectionRepository inspectionRepository,
            AssetRepository assetRepository,
            UserRepository userRepository,
            MaintenanceRequestService maintenanceRequestService) {
        this.inspectionRepository = inspectionRepository;
        this.assetRepository = assetRepository;
        this.userRepository = userRepository;
        this.maintenanceRequestService = maintenanceRequestService;
    }

    public InspectionResponseDTO createInspection(InspectionRequestDTO request) {

        Asset asset = assetRepository.findById(request.getAssetId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Asset not found"));

        User inspector = userRepository.findById(request.getInspectorId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Inspector not found"));

        if (!inspector.getRole().getName().equals("INSPECTOR")) {
            throw new RuntimeException("Assigned user must have INSPECTOR role");
        }

        Inspection inspection = new Inspection();
        inspection.setAsset(asset);
        inspection.setInspector(inspector);
        inspection.setScheduledDate(request.getScheduledDate());
        inspection.setStatus(InspectionStatus.SCHEDULED);
        inspection.setFindings(request.getFindings());

        Inspection savedInspection = inspectionRepository.save(inspection);

        return convertToDTO(savedInspection);
    }

    public List<InspectionResponseDTO> getAllInspections() {
        return inspectionRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    public InspectionResponseDTO getInspectionById(Long id) {

        Inspection inspection = inspectionRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Inspection not found"));

        return convertToDTO(inspection);
    }

    public List<InspectionResponseDTO> getMyInspections(String email) {

        return inspectionRepository.findByInspectorEmail(email)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional
    public InspectionResponseDTO submitInspectionResult(
            Long inspectionId,
            String email,
            InspectionResultRequestDTO request) {

        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Inspection not found"));

        if (!inspection.getInspector().getEmail().equals(email)) {
            throw new InspectionAccessDeniedException(
                    "You are not authorized to submit this inspection");
        }

        validateStatusTransition(
                inspection.getStatus(),
                request.getStatus()
        );

        inspection.setStatus(request.getStatus());
        inspection.setFindings(request.getFindings());

        Inspection savedInspection = inspectionRepository.save(inspection);

        if (savedInspection.getStatus() == InspectionStatus.FAILED) {
            maintenanceRequestService.createMaintenanceRequest(savedInspection);
        }

        return convertToDTO(savedInspection);
    }

    private InspectionResponseDTO convertToDTO(Inspection inspection) {

        return new InspectionResponseDTO(
                inspection.getId(),
                inspection.getAsset().getId(),
                inspection.getAsset().getName(),
                inspection.getInspector().getId(),
                inspection.getInspector().getName(),
                inspection.getScheduledDate(),
                inspection.getStatus(),
                inspection.getFindings()
        );
    }

    private void validateStatusTransition(
            InspectionStatus currentStatus,
            InspectionStatus newStatus) {

        boolean validTransition = switch (currentStatus) {
            case SCHEDULED -> newStatus == InspectionStatus.IN_PROGRESS;
            case IN_PROGRESS -> newStatus == InspectionStatus.COMPLETED
                    || newStatus == InspectionStatus.FAILED;
            case COMPLETED, FAILED -> false;
        };

        if (!validTransition) {
            throw new IllegalStateException(
                    "Invalid inspection status transition: "
                            + currentStatus + " -> " + newStatus);
        }
    }
}
