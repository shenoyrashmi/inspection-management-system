package com.example.inspection_management.controller;

import com.example.inspection_management.dto.InspectionResponseDTO;
import com.example.inspection_management.dto.ReportResponseDTO;
import com.example.inspection_management.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(
        name = "Reports",
        description = "APIs for inspection summaries and asset inspection history"
)
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @Operation(
            summary = "Get inspection summary",
            description = "Returns asset, inspection and maintenance statistics"
    )
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ReportResponseDTO getSummaryReport() {
        return reportService.getSummaryReport();
    }

    @Operation(
            summary = "Get asset inspection history",
            description = "Retrieves inspection history for a specific asset"
    )
    @GetMapping("/assets/{assetId}/inspections")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public List<InspectionResponseDTO> getAssetInspectionHistory(
            @PathVariable Long assetId) {

        return reportService.getAssetInspectionHistory(assetId);
    }
}
