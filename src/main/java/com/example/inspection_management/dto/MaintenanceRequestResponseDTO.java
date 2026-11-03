package com.example.inspection_management.dto;

import com.example.inspection_management.entity.MaintenanceStatus;

public class MaintenanceRequestResponseDTO {

    private Long id;
    private Long assetId;
    private String assetName;
    private Long inspectionId;
    private String description;
    private MaintenanceStatus status;

    public MaintenanceRequestResponseDTO(
            Long id,
            Long assetId,
            String assetName,
            Long inspectionId,
            String description,
            MaintenanceStatus status) {

        this.id = id;
        this.assetId = assetId;
        this.assetName = assetName;
        this.inspectionId = inspectionId;
        this.description = description;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public Long getAssetId() {
        return assetId;
    }

    public String getAssetName() {
        return assetName;
    }

    public Long getInspectionId() {
        return inspectionId;
    }

    public String getDescription() {
        return description;
    }

    public MaintenanceStatus getStatus() {
        return status;
    }
}
