package com.example.inspection_management.service;

import com.example.inspection_management.dto.AssetRequestDTO;
import com.example.inspection_management.dto.AssetResponseDTO;
import com.example.inspection_management.entity.Asset;
import com.example.inspection_management.exception.ResourceNotFoundException;
import com.example.inspection_management.repository.AssetRepository;

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
class AssetServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @InjectMocks
    private AssetService assetService;

    private Asset createAsset() {
        Asset asset = new Asset();
        asset.setName("Generator");
        asset.setSerialNumber("GEN-101");
        asset.setLocation("Building A");
        asset.setStatus("ACTIVE");
        return asset;
    }

    private AssetRequestDTO createRequest() {
        AssetRequestDTO request = new AssetRequestDTO();
        request.setName("Generator");
        request.setSerialNumber("GEN-101");
        request.setLocation("Building A");
        request.setStatus("ACTIVE");
        return request;
    }

    @Test
    void createAssetShouldReturnSavedAsset() {
        Asset savedAsset = createAsset();

        when(assetRepository.save(any(Asset.class)))
                .thenReturn(savedAsset);

        AssetResponseDTO response =
                assetService.createAsset(createRequest());

        assertEquals("Generator", response.getName());
        assertEquals("GEN-101", response.getSerialNumber());

        verify(assetRepository).save(any(Asset.class));
    }

    @Test
    void getAllAssetsShouldReturnAllAssets() {
        when(assetRepository.findAll())
                .thenReturn(List.of(createAsset(), createAsset()));

        List<AssetResponseDTO> response =
                assetService.getAllAssets();

        assertEquals(2, response.size());
        verify(assetRepository).findAll();
    }

    @Test
    void getAssetByIdShouldReturnAsset() {
        when(assetRepository.findById(1L))
                .thenReturn(Optional.of(createAsset()));

        AssetResponseDTO response =
                assetService.getAssetById(1L);

        assertEquals("Generator", response.getName());
        verify(assetRepository).findById(1L);
    }

    @Test
    void getAssetByIdShouldThrowExceptionWhenNotFound() {
        when(assetRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> assetService.getAssetById(999L)
        );

        verify(assetRepository).findById(999L);
    }

    @Test
    void updateAssetShouldReturnUpdatedAsset() {
        Asset asset = createAsset();

        AssetRequestDTO request = createRequest();
        request.setName("Updated Generator");
        request.setLocation("Building B");

        when(assetRepository.findById(1L))
                .thenReturn(Optional.of(asset));

        when(assetRepository.save(any(Asset.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AssetResponseDTO response =
                assetService.updateAsset(1L, request);

        assertEquals("Updated Generator", response.getName());
        assertEquals("Building B", response.getLocation());

        verify(assetRepository).save(asset);
    }

    @Test
    void deleteAssetShouldDeleteExistingAsset() {
        Asset asset = createAsset();

        when(assetRepository.findById(1L))
                .thenReturn(Optional.of(asset));

        assertDoesNotThrow(() -> assetService.deleteAsset(1L));

        verify(assetRepository).findById(1L);
        verify(assetRepository).delete(asset);
    }

    @Test
    void deleteAssetShouldThrowExceptionWhenNotFound() {
        when(assetRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> assetService.deleteAsset(999L)
        );

        verify(assetRepository, never()).delete(any(Asset.class));
    }
}
