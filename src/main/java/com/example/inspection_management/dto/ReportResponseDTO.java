package com.example.inspection_management.dto;

public class ReportResponseDTO {

    private long totalAssets;
    private long totalInspections;
    private long completedInspections;
    private long failedInspections;
    private long pendingInspections;
    private long openMaintenanceRequests;

    public ReportResponseDTO(
            long totalAssets,
            long totalInspections,
            long completedInspections,
            long failedInspections,
            long pendingInspections,
            long openMaintenanceRequests) {

        this.totalAssets = totalAssets;
        this.totalInspections = totalInspections;
        this.completedInspections = completedInspections;
        this.failedInspections = failedInspections;
        this.pendingInspections = pendingInspections;
        this.openMaintenanceRequests = openMaintenanceRequests;
    }

    public long getTotalAssets() {
        return totalAssets;
    }

    public long getTotalInspections() {
        return totalInspections;
    }

    public long getCompletedInspections() {
        return completedInspections;
    }

    public long getFailedInspections() {
        return failedInspections;
    }

    public long getPendingInspections() {
        return pendingInspections;
    }

    public long getOpenMaintenanceRequests() {
        return openMaintenanceRequests;
    }
}
