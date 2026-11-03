package com.example.inspection_management.dto;

import com.example.inspection_management.entity.InspectionStatus;

import java.time.LocalDate;

public class InspectionResponseDTO {

    private Long id;
    private Long assetId;
    private String assetName;
    private Long inspectorId;
    private String inspectorName;
    private LocalDate scheduledDate;
    private InspectionStatus status;
    private String findings;

    public InspectionResponseDTO(
            Long id,
            Long assetId,
            String assetName,
            Long inspectorId,
            String inspectorName,
            LocalDate scheduledDate,
            InspectionStatus status,
            String findings) {

        this.id = id;
        this.assetId = assetId;
        this.assetName = assetName;
        this.inspectorId = inspectorId;
        this.inspectorName = inspectorName;
        this.scheduledDate = scheduledDate;
        this.status = status;
        this.findings = findings;
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

    public Long getInspectorId() {
        return inspectorId;
    }

    public String getInspectorName() {
        return inspectorName;
    }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public InspectionStatus getStatus() {
        return status;
    }

    public String getFindings() {
        return findings;
    }
}
