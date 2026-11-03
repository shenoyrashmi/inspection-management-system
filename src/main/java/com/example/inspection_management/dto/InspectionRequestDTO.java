package com.example.inspection_management.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class InspectionRequestDTO {

    @NotNull
    private Long assetId;

    @NotNull
    private Long inspectorId;

    @NotNull
    private LocalDate scheduledDate;

    private String findings;

    public Long getAssetId() {
        return assetId;
    }

    public void setAssetId(Long assetId) {
        this.assetId = assetId;
    }

    public Long getInspectorId() {
        return inspectorId;
    }

    public void setInspectorId(Long inspectorId) {
        this.inspectorId = inspectorId;
    }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(LocalDate scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public String getFindings() {
        return findings;
    }

    public void setFindings(String findings) {
        this.findings = findings;
    }
}
