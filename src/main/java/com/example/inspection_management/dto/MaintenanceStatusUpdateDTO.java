package com.example.inspection_management.dto;

import com.example.inspection_management.entity.MaintenanceStatus;
import jakarta.validation.constraints.NotNull;

public class MaintenanceStatusUpdateDTO {

    @NotNull
    private MaintenanceStatus status;

    public MaintenanceStatus getStatus() {
        return status;
    }

    public void setStatus(MaintenanceStatus status) {
        this.status = status;
    }
}
