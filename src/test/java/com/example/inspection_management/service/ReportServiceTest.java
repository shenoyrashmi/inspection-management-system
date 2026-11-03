package com.example.inspection_management.service;

import com.example.inspection_management.dto.InspectionResponseDTO;
import com.example.inspection_management.dto.ReportResponseDTO;
import com.example.inspection_management.entity.*;
import com.example.inspection_management.exception.ResourceNotFoundException;
import com.example.inspection_management.repository.AssetRepository;
import com.example.inspection_management.repository.InspectionRepository;
import com.example.inspection_management.repository.MaintenanceRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private InspectionRepository inspectionRepository;

    @Mock
    private MaintenanceRequestRepository maintenanceRequestRepository;

    @InjectMocks
    private ReportService reportService;

    @Test
    void getSummaryReportShouldReturnCorrectCounts() {
        when(assetRepository.count()).thenReturn(10L);
        when(inspectionRepository.count()).thenReturn(20L);

        when(inspectionRepository.countByStatus(InspectionStatus.COMPLETED))
                .thenReturn(8L);
        when(inspectionRepository.countByStatus(InspectionStatus.FAILED))
                .thenReturn(3L);
        when(inspectionRepository.countByStatus(InspectionStatus.SCHEDULED))
                .thenReturn(5L);
        when(inspectionRepository.countByStatus(InspectionStatus.IN_PROGRESS))
                .thenReturn(4L);

        when(maintenanceRequestRepository.countByStatus(MaintenanceStatus.OPEN))
                .thenReturn(2L);

        ReportResponseDTO result = reportService.getSummaryReport();

        assertEquals(10L, result.getTotalAssets());
        assertEquals(20L, result.getTotalInspections());
        assertEquals(8L, result.getCompletedInspections());
        assertEquals(3L, result.getFailedInspections());
        assertEquals(9L, result.getPendingInspections());
        assertEquals(2L, result.getOpenMaintenanceRequests());
    }

    @Test
    void getSummaryReportShouldReturnZeroWhenNoRecordsExist() {
        when(assetRepository.count()).thenReturn(0L);
        when(inspectionRepository.count()).thenReturn(0L);

        when(inspectionRepository.countByStatus(any(InspectionStatus.class)))
                .thenReturn(0L);

        when(maintenanceRequestRepository.countByStatus(MaintenanceStatus.OPEN))
                .thenReturn(0L);

        ReportResponseDTO result = reportService.getSummaryReport();

        assertEquals(0L, result.getTotalAssets());
        assertEquals(0L, result.getTotalInspections());
        assertEquals(0L, result.getCompletedInspections());
        assertEquals(0L, result.getFailedInspections());
        assertEquals(0L, result.getPendingInspections());
        assertEquals(0L, result.getOpenMaintenanceRequests());
    }

    @Test
    void getAssetInspectionHistoryShouldReturnInspections() {
        Asset asset = new Asset();
        asset.setName("Generator");

        Inspection inspection = new Inspection();
        inspection.setAsset(asset);
        inspection.setInspector(new User(
                "Inspector One",
                "inspector@example.com",
                "encoded-password",
                new Role("INSPECTOR")
        ));
        inspection.setScheduledDate(java.time.LocalDate.now());
        inspection.setStatus(InspectionStatus.COMPLETED);
        inspection.setFindings("Inspection passed");

        when(assetRepository.existsById(1L)).thenReturn(true);
        when(inspectionRepository.findByAssetId(1L))
                .thenReturn(List.of(inspection));

        List<InspectionResponseDTO> result =
                reportService.getAssetInspectionHistory(1L);

        assertEquals(1, result.size());
        assertEquals("Generator", result.get(0).getAssetName());
        assertEquals(InspectionStatus.COMPLETED, result.get(0).getStatus());

        verify(inspectionRepository).findByAssetId(1L);
    }

    @Test
    void getAssetInspectionHistoryShouldReturnEmptyListWhenNoInspectionsExist() {
        when(assetRepository.existsById(1L)).thenReturn(true);
        when(inspectionRepository.findByAssetId(1L))
                .thenReturn(List.of());

        List<InspectionResponseDTO> result =
                reportService.getAssetInspectionHistory(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void getAssetInspectionHistoryShouldThrowWhenAssetNotFound() {
        when(assetRepository.existsById(99L)).thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () -> reportService.getAssetInspectionHistory(99L)
        );

        verify(inspectionRepository, never()).findByAssetId(anyLong());
    }
}
