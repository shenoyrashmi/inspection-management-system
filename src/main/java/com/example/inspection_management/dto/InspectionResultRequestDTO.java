package com.example.inspection_management.dto;

import com.example.inspection_management.entity.InspectionStatus;
import jakarta.validation.constraints.NotNull;

public class InspectionResultRequestDTO {

    @NotNull
    private InspectionStatus status;

    private String findings;

    public InspectionStatus getStatus() {
        return status;
    }

    public void setStatus(InspectionStatus status) {
        this.status = status;
    }

    public String getFindings() {
        return findings;
    }

    public void setFindings(String findings) {
        this.findings = findings;
    }
}
