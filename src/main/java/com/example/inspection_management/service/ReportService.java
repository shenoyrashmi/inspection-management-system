package com.example.inspection_management.service;

import com.example.inspection_management.dto.InspectionResponseDTO;
import com.example.inspection_management.dto.ReportResponseDTO;
import com.example.inspection_management.entity.Inspection;
import com.example.inspection_management.entity.InspectionStatus;
import com.example.inspection_management.entity.MaintenanceStatus;
import com.example.inspection_management.exception.ResourceNotFoundException;
import com.example.inspection_management.repository.AssetRepository;
import com.example.inspection_management.repository.InspectionRepository;
import com.example.inspection_management.repository.MaintenanceRequestRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReportService {

    private final AssetRepository assetRepository;
    private final InspectionRepository inspectionRepository;
    private final MaintenanceRequestRepository maintenanceRequestRepository;

    public ReportService(
            AssetRepository assetRepository,
            InspectionRepository inspectionRepository,
            MaintenanceRequestRepository maintenanceRequestRepository) {

        this.assetRepository = assetRepository;
        this.inspectionRepository = inspectionRepository;
        this.maintenanceRequestRepository = maintenanceRequestRepository;
    }

    public ReportResponseDTO getSummaryReport() {

        long totalAssets = assetRepository.count();

        long totalInspections = inspectionRepository.count();

        long completedInspections =
                inspectionRepository.countByStatus(InspectionStatus.COMPLETED);

        long failedInspections =
                inspectionRepository.countByStatus(InspectionStatus.FAILED);

        long pendingInspections =
                inspectionRepository.countByStatus(InspectionStatus.SCHEDULED)
                + inspectionRepository.countByStatus(InspectionStatus.IN_PROGRESS);

        long openMaintenanceRequests =
                maintenanceRequestRepository.countByStatus(MaintenanceStatus.OPEN);

        return new ReportResponseDTO(
                totalAssets,
                totalInspections,
                completedInspections,
                failedInspections,
                pendingInspections,
                openMaintenanceRequests
        );
    }

    public List<InspectionResponseDTO> getAssetInspectionHistory(Long assetId) {

        if (!assetRepository.existsById(assetId)) {
            throw new ResourceNotFoundException(
                    "Asset not found with id: " + assetId
            );
        }

        return inspectionRepository.findByAssetId(assetId)
                .stream()
                .map(inspection -> new InspectionResponseDTO(
                        inspection.getId(),
                        inspection.getAsset().getId(),
                        inspection.getAsset().getName(),
                        inspection.getInspector().getId(),
                        inspection.getInspector().getName(),
                        inspection.getScheduledDate(),
                        inspection.getStatus(),
                        inspection.getFindings()
                ))
                .toList();
    }
}
