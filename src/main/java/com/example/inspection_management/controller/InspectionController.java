package com.example.inspection_management.controller;

import com.example.inspection_management.dto.InspectionRequestDTO;
import com.example.inspection_management.dto.InspectionResponseDTO;
import com.example.inspection_management.dto.InspectionResultRequestDTO;
import com.example.inspection_management.service.InspectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "Inspection Management",
        description = "APIs for scheduling inspections, assigning inspectors and submitting inspection results"
)
@RestController
@RequestMapping("/api/inspections")
public class InspectionController {

    private final InspectionService inspectionService;

    public InspectionController(InspectionService inspectionService) {
        this.inspectionService = inspectionService;
    }

    @Operation(
            summary = "Schedule inspection",
            description = "Creates an inspection for an asset and assigns it to an inspector"
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InspectionResponseDTO createInspection(
            @Valid @RequestBody InspectionRequestDTO request) {
        return inspectionService.createInspection(request);
    }

    @Operation(
            summary = "Get all inspections",
            description = "Retrieves all inspections"
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping
    public List<InspectionResponseDTO> getAllInspections() {
        return inspectionService.getAllInspections();
    }

    @Operation(
            summary = "Get inspection by ID",
            description = "Retrieves a single inspection by its ID"
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/{id}")
    public InspectionResponseDTO getInspectionById(@PathVariable Long id) {
        return inspectionService.getInspectionById(id);
    }

    @Operation(
            summary = "Get my inspections",
            description = "Retrieves inspections assigned to the authenticated inspector"
    )
    @PreAuthorize("hasRole('INSPECTOR')")
    @GetMapping("/my-inspections")
    public List<InspectionResponseDTO> getMyInspections(
            @AuthenticationPrincipal String email) {

        return inspectionService.getMyInspections(email);
    }

    @Operation(
            summary = "Submit inspection results",
            description = "Submits findings and updates the inspection status. Failed inspections may generate maintenance requests."
    )
    @PreAuthorize("hasRole('INSPECTOR')")
    @PutMapping("/{id}/submit")
    public InspectionResponseDTO submitInspectionResult(
            @PathVariable Long id,
            @AuthenticationPrincipal String email,
            @Valid @RequestBody InspectionResultRequestDTO request) {

        return inspectionService.submitInspectionResult(id, email, request);
    }
}
