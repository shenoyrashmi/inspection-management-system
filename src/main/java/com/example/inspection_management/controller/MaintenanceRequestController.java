package com.example.inspection_management.controller;

import com.example.inspection_management.dto.MaintenanceRequestResponseDTO;
import com.example.inspection_management.dto.MaintenanceStatusUpdateDTO;
import com.example.inspection_management.service.MaintenanceRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "Maintenance Management",
        description = "APIs for managing maintenance requests generated from failed inspections"
)
@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceRequestController {

    private final MaintenanceRequestService maintenanceRequestService;

    public MaintenanceRequestController(
            MaintenanceRequestService maintenanceRequestService) {
        this.maintenanceRequestService = maintenanceRequestService;
    }

    @Operation(
            summary = "Get maintenance requests",
            description = "Retrieves all maintenance requests in the system"
    )
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public List<MaintenanceRequestResponseDTO> getAllMaintenanceRequests() {
        return maintenanceRequestService.getAllMaintenanceRequests();
    }

    @Operation(
            summary = "Update maintenance status",
            description = "Updates the status of a maintenance request through its permitted workflow"
    )
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public MaintenanceRequestResponseDTO updateMaintenanceStatus(
            @PathVariable Long id,
            @Valid @RequestBody MaintenanceStatusUpdateDTO request) {

        return maintenanceRequestService.updateMaintenanceStatus(
                id,
                request.getStatus()
        );
    }
}
