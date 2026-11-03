package com.example.inspection_management.service;

import com.example.inspection_management.dto.MaintenanceRequestResponseDTO;
import com.example.inspection_management.entity.*;
import com.example.inspection_management.exception.ResourceNotFoundException;
import com.example.inspection_management.repository.MaintenanceRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaintenanceRequestServiceTest {

    @Mock
    private MaintenanceRequestRepository maintenanceRequestRepository;

    @InjectMocks
    private MaintenanceRequestService maintenanceRequestService;

    private Inspection createFailedInspection() {
        Asset asset = new Asset();
        asset.setName("Generator");

        Inspection inspection = new Inspection();
        // JPA generates IDs on persist; set one here so ID-based lookups can be stubbed.
        ReflectionTestUtils.setField(inspection, "id", 1L);
        inspection.setAsset(asset);
        inspection.setScheduledDate(LocalDate.now());
        inspection.setStatus(InspectionStatus.FAILED);
        inspection.setFindings("Oil leakage detected");

        return inspection;
    }

    private MaintenanceRequest createMaintenanceRequest() {
        MaintenanceRequest request = new MaintenanceRequest();
        request.setId(1L);
        request.setAsset(createFailedInspection().getAsset());
        request.setInspection(createFailedInspection());
        request.setDescription("Oil leakage detected");
        request.setStatus(MaintenanceStatus.OPEN);
        return request;
    }

    @Test
    void getAllMaintenanceRequestsShouldReturnRequests() {
        when(maintenanceRequestRepository.findAll())
                .thenReturn(List.of(createMaintenanceRequest()));

        List<MaintenanceRequestResponseDTO> result =
                maintenanceRequestService.getAllMaintenanceRequests();

        assertEquals(1, result.size());
        assertEquals("Oil leakage detected", result.get(0).getDescription());
    }

    @Test
    void createMaintenanceRequestShouldSaveFailedInspection() {
        Inspection inspection = createFailedInspection();

        when(maintenanceRequestRepository.existsByInspectionId(1L))
                .thenReturn(false);

        maintenanceRequestService.createMaintenanceRequest(inspection);

        verify(maintenanceRequestRepository).save(any(MaintenanceRequest.class));
    }

    @Test
    void createMaintenanceRequestShouldRejectNonFailedInspection() {
        Inspection inspection = createFailedInspection();
        inspection.setStatus(InspectionStatus.COMPLETED);

        assertThrows(
                IllegalArgumentException.class,
                () -> maintenanceRequestService.createMaintenanceRequest(inspection)
        );

        verify(maintenanceRequestRepository, never()).save(any());
    }

    @Test
    void createMaintenanceRequestShouldAvoidDuplicateRequests() {
        Inspection inspection = createFailedInspection();

        when(maintenanceRequestRepository.existsByInspectionId(1L))
                .thenReturn(true);

        maintenanceRequestService.createMaintenanceRequest(inspection);

        verify(maintenanceRequestRepository, never()).save(any());
    }

    @Test
    void updateMaintenanceStatusShouldMoveOpenToInProgress() {
        MaintenanceRequest request = createMaintenanceRequest();

        when(maintenanceRequestRepository.findById(1L))
                .thenReturn(Optional.of(request));

        when(maintenanceRequestRepository.save(any(MaintenanceRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MaintenanceRequestResponseDTO result =
                maintenanceRequestService.updateMaintenanceStatus(
                        1L, MaintenanceStatus.IN_PROGRESS
                );

        assertEquals(MaintenanceStatus.IN_PROGRESS, result.getStatus());
        verify(maintenanceRequestRepository).save(request);
    }

    @Test
    void updateMaintenanceStatusShouldMoveInProgressToCompleted() {
        MaintenanceRequest request = createMaintenanceRequest();
        request.setStatus(MaintenanceStatus.IN_PROGRESS);

        when(maintenanceRequestRepository.findById(1L))
                .thenReturn(Optional.of(request));

        when(maintenanceRequestRepository.save(any(MaintenanceRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MaintenanceRequestResponseDTO result =
                maintenanceRequestService.updateMaintenanceStatus(
                        1L, MaintenanceStatus.COMPLETED
                );

        assertEquals(MaintenanceStatus.COMPLETED, result.getStatus());
    }

    @Test
    void updateMaintenanceStatusShouldRejectInvalidTransition() {
        MaintenanceRequest request = createMaintenanceRequest();

        when(maintenanceRequestRepository.findById(1L))
                .thenReturn(Optional.of(request));

        assertThrows(
                IllegalStateException.class,
                () -> maintenanceRequestService.updateMaintenanceStatus(
                        1L, MaintenanceStatus.COMPLETED
                )
        );

        verify(maintenanceRequestRepository, never()).save(any());
    }

    @Test
    void updateMaintenanceStatusShouldThrowWhenRequestNotFound() {
        when(maintenanceRequestRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> maintenanceRequestService.updateMaintenanceStatus(
                        99L, MaintenanceStatus.IN_PROGRESS
                )
        );
    }
}
