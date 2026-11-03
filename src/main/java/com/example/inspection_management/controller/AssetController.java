package com.example.inspection_management.controller;

import com.example.inspection_management.dto.AssetRequestDTO;
import com.example.inspection_management.dto.AssetResponseDTO;
import com.example.inspection_management.service.AssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "Asset Management",
        description = "APIs for creating, retrieving, updating and deleting assets"
)
@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @Operation(summary = "Create asset", description = "Creates a new asset in the system")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssetResponseDTO createAsset(@Valid @RequestBody AssetRequestDTO asset) {
        return assetService.createAsset(asset);
    }

    @Operation(summary = "Get all assets", description = "Retrieves all assets")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping
    public List<AssetResponseDTO> getAllAssets() {
        return assetService.getAllAssets();
    }

    @Operation(summary = "Get asset by ID", description = "Retrieves a single asset by its ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/{id}")
    public AssetResponseDTO getAssetById(@PathVariable Long id) {
        return assetService.getAssetById(id);
    }

    @Operation(summary = "Update asset", description = "Updates an existing asset by its ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PutMapping("/{id}")
    public AssetResponseDTO updateAsset(@Valid @RequestBody AssetRequestDTO asset, @PathVariable Long id) {
        return assetService.updateAsset(id, asset);
    }

    @Operation(summary = "Delete asset", description = "Deletes an asset by its ID")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateAsset(@PathVariable Long id) {
         assetService.deleteAsset(id);
    }
}


