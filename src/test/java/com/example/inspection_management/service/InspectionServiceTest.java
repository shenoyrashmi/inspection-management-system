package com.example.inspection_management.service;

import com.example.inspection_management.dto.InspectionRequestDTO;
import com.example.inspection_management.dto.InspectionResultRequestDTO;
import com.example.inspection_management.dto.InspectionResponseDTO;
import com.example.inspection_management.entity.*;
import com.example.inspection_management.exception.ResourceNotFoundException;
import com.example.inspection_management.repository.AssetRepository;
import com.example.inspection_management.repository.InspectionRepository;
import com.example.inspection_management.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InspectionServiceTest {

    @Mock
    private InspectionRepository inspectionRepository;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MaintenanceRequestService maintenanceRequestService;

    @InjectMocks
    private InspectionService inspectionService;

    private Asset createAsset() {
        Asset asset = new Asset();
        asset.setName("Generator");
        asset.setSerialNumber("GEN-101");
        asset.setLocation("Building A");
        asset.setStatus("ACTIVE");
        return asset;
    }

    private User createInspector() {
        return new User(
                "Inspector One",
                "inspector@example.com",
                "encoded-password",
                new Role("INSPECTOR")
        );
    }

    private Inspection createInspection() {
        Inspection inspection = new Inspection();
        inspection.setAsset(createAsset());
        inspection.setInspector(createInspector());
        inspection.setScheduledDate(LocalDate.now());
        inspection.setStatus(InspectionStatus.SCHEDULED);
        return inspection;
    }

    @Test
    void createInspectionShouldCreateScheduledInspection() {
        InspectionRequestDTO request = new InspectionRequestDTO();
        request.setAssetId(1L);
        request.setInspectorId(2L);
        request.setScheduledDate(LocalDate.now());

        when(assetRepository.findById(1L))
                .thenReturn(Optional.of(createAsset()));

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(createInspector()));

        when(inspectionRepository.save(any(Inspection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InspectionResponseDTO response =
                inspectionService.createInspection(request);

        assertEquals(InspectionStatus.SCHEDULED, response.getStatus());
        verify(inspectionRepository).save(any(Inspection.class));
    }

    @Test
    void createInspectionShouldFailWhenAssetDoesNotExist() {
        InspectionRequestDTO request = new InspectionRequestDTO();
        request.setAssetId(999L);
        request.setInspectorId(2L);
        request.setScheduledDate(LocalDate.now());

        when(assetRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> inspectionService.createInspection(request)
        );

        verify(inspectionRepository, never()).save(any());
    }

    @Test
    void createInspectionShouldFailWhenInspectorDoesNotExist() {
        InspectionRequestDTO request = new InspectionRequestDTO();
        request.setAssetId(1L);
        request.setInspectorId(999L);
        request.setScheduledDate(LocalDate.now());

        when(assetRepository.findById(1L))
                .thenReturn(Optional.of(createAsset()));

        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> inspectionService.createInspection(request)
        );

        verify(inspectionRepository, never()).save(any());
    }

    @Test
    void createInspectionShouldRejectNonInspectorUser() {
        InspectionRequestDTO request = new InspectionRequestDTO();
        request.setAssetId(1L);
        request.setInspectorId(2L);
        request.setScheduledDate(LocalDate.now());

        User manager = new User(
                "Manager",
                "manager@example.com",
                "encoded-password",
                new Role("MANAGER")
        );

        when(assetRepository.findById(1L))
                .thenReturn(Optional.of(createAsset()));

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(manager));

        assertThrows(
                RuntimeException.class,
                () -> inspectionService.createInspection(request)
        );

        verify(inspectionRepository, never()).save(any());
    }

    @Test
    void submitInspectionShouldMoveScheduledToInProgress() {
        Inspection inspection = createInspection();

        InspectionResultRequestDTO request =
                new InspectionResultRequestDTO();
        request.setStatus(InspectionStatus.IN_PROGRESS);
        request.setFindings("Inspection started");

        when(inspectionRepository.findById(1L))
                .thenReturn(Optional.of(inspection));

        when(inspectionRepository.save(any(Inspection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InspectionResponseDTO response =
                inspectionService.submitInspectionResult(
                        1L, "inspector@example.com", request
                );

        assertEquals(InspectionStatus.IN_PROGRESS, response.getStatus());
        verify(maintenanceRequestService, never())
                .createMaintenanceRequest(any());
    }

    @Test
    void submitInspectionShouldCreateMaintenanceWhenFailed() {
        Inspection inspection = createInspection();
        inspection.setStatus(InspectionStatus.IN_PROGRESS);

        InspectionResultRequestDTO request =
                new InspectionResultRequestDTO();
        request.setStatus(InspectionStatus.FAILED);
        request.setFindings("Motor overheating");

        when(inspectionRepository.findById(1L))
                .thenReturn(Optional.of(inspection));

        when(inspectionRepository.save(any(Inspection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InspectionResponseDTO response =
                inspectionService.submitInspectionResult(
                        1L, "inspector@example.com", request
                );

        assertEquals(InspectionStatus.FAILED, response.getStatus());

        verify(maintenanceRequestService)
                .createMaintenanceRequest(inspection);
    }

    @Test
    void submitInspectionShouldRejectUnauthorizedInspector() {
        Inspection inspection = createInspection();

        InspectionResultRequestDTO request =
                new InspectionResultRequestDTO();
        request.setStatus(InspectionStatus.IN_PROGRESS);

        when(inspectionRepository.findById(1L))
                .thenReturn(Optional.of(inspection));

        assertThrows(
                RuntimeException.class,
                () -> inspectionService.submitInspectionResult(
                        1L, "another@example.com", request
                )
        );

        verify(inspectionRepository, never()).save(any());
    }

    @Test
    void submitInspectionShouldRejectInvalidStatusTransition() {
        Inspection inspection = createInspection();

        InspectionResultRequestDTO request =
                new InspectionResultRequestDTO();
        request.setStatus(InspectionStatus.COMPLETED);

        when(inspectionRepository.findById(1L))
                .thenReturn(Optional.of(inspection));

        assertThrows(
                IllegalStateException.class,
                () -> inspectionService.submitInspectionResult(
                        1L, "inspector@example.com", request
                )
        );

        verify(inspectionRepository, never()).save(any());
    }
}
