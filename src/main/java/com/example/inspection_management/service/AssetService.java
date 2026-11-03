package com.example.inspection_management.service;

import com.example.inspection_management.dto.AssetRequestDTO;
import com.example.inspection_management.dto.AssetResponseDTO;
import com.example.inspection_management.entity.Asset;
import com.example.inspection_management.exception.ResourceNotFoundException;
import com.example.inspection_management.repository.AssetRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AssetService {

    private final AssetRepository assetRepository;

    public AssetService(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }


    public AssetResponseDTO createAsset(AssetRequestDTO assetRequestDTO) {
      Asset asset = new Asset(
              assetRequestDTO.getName(),
              assetRequestDTO.getSerialNumber(),
              assetRequestDTO.getLocation(),
              assetRequestDTO.getStatus()
      );

      Asset savedAsset = assetRepository.save(asset);
      return  convertToDTO(savedAsset);

    }

    public List<AssetResponseDTO> getAllAssets() {
        return assetRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public AssetResponseDTO getAssetById(Long id) {
         Asset asset = assetRepository.findById(id)
                 .orElseThrow(() ->
                         new ResourceNotFoundException(
                                 "Asset not foun with ID: " + id
                         )
                 );

        return convertToDTO(asset);
    }

    public AssetResponseDTO updateAsset(Long id, AssetRequestDTO requestDTO) {
        Asset existingAsset = assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found with ID: " + id));
        existingAsset.setName(requestDTO.getName());
        existingAsset.setSerialNumber(requestDTO.getSerialNumber());
        existingAsset.setLocation(requestDTO.getLocation());
        existingAsset.setStatus(requestDTO.getStatus());

        Asset updatedAsset = assetRepository.save(existingAsset);

        return convertToDTO(updatedAsset);    }

    public void deleteAsset(Long id) {
        Asset existingAsset = assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found with ID: " + id));

        assetRepository.delete(existingAsset);
    }

    // Entity to Response DTO conversion
    private AssetResponseDTO convertToDTO(Asset asset) {
        return new AssetResponseDTO(
                asset.getId(),
                asset.getName(),
                asset.getSerialNumber(),
                asset.getLocation(),
                asset.getStatus()
        );
    }
}
