package com.example.inspection_management.service;

import com.example.inspection_management.dto.MaintenanceRequestResponseDTO;
import com.example.inspection_management.entity.Inspection;
import com.example.inspection_management.entity.InspectionStatus;
import com.example.inspection_management.entity.MaintenanceRequest;
import com.example.inspection_management.entity.MaintenanceStatus;
import com.example.inspection_management.exception.ResourceNotFoundException;
import com.example.inspection_management.repository.MaintenanceRequestRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MaintenanceRequestService {

    private final MaintenanceRequestRepository maintenanceRequestRepository;

    public MaintenanceRequestService(
            MaintenanceRequestRepository maintenanceRequestRepository) {
        this.maintenanceRequestRepository = maintenanceRequestRepository;
    }

    public List<MaintenanceRequestResponseDTO> getAllMaintenanceRequests() {
        return maintenanceRequestRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    public void createMaintenanceRequest(Inspection inspection) {

        if (inspection.getStatus() != InspectionStatus.FAILED) {
            throw new IllegalArgumentException(
                    "Maintenance requests can only be created for failed inspections"
            );
        }

        boolean alreadyExists =
                maintenanceRequestRepository.existsByInspectionId(
                        inspection.getId()
                );

        if (alreadyExists) {
            return;
        }

        MaintenanceRequest maintenanceRequest = new MaintenanceRequest();

        maintenanceRequest.setAsset(inspection.getAsset());
        maintenanceRequest.setInspection(inspection);
        maintenanceRequest.setDescription(inspection.getFindings());
        maintenanceRequest.setStatus(MaintenanceStatus.OPEN);

        maintenanceRequestRepository.save(maintenanceRequest);
    }

    public MaintenanceRequestResponseDTO updateMaintenanceStatus(
            Long id,
            MaintenanceStatus newStatus) {

        MaintenanceRequest maintenanceRequest =
                maintenanceRequestRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Maintenance request not found with id: " + id
                                ));

        MaintenanceStatus currentStatus = maintenanceRequest.getStatus();

        boolean validTransition =
                (currentStatus == MaintenanceStatus.OPEN
                        && newStatus == MaintenanceStatus.IN_PROGRESS)
                ||
                (currentStatus == MaintenanceStatus.IN_PROGRESS
                        && newStatus == MaintenanceStatus.COMPLETED);

        if (!validTransition) {
            throw new IllegalStateException(
                    "Invalid maintenance status transition: "
                            + currentStatus + " -> " + newStatus
            );
        }

        maintenanceRequest.setStatus(newStatus);

        MaintenanceRequest savedRequest =
                maintenanceRequestRepository.save(maintenanceRequest);

        return convertToDTO(savedRequest);
    }

    private MaintenanceRequestResponseDTO convertToDTO(
            MaintenanceRequest maintenanceRequest) {

        return new MaintenanceRequestResponseDTO(
                maintenanceRequest.getId(),
                maintenanceRequest.getAsset().getId(),
                maintenanceRequest.getAsset().getName(),
                maintenanceRequest.getInspection().getId(),
                maintenanceRequest.getDescription(),
                maintenanceRequest.getStatus()
        );
    }
}
