package com.example.inspection_management.controller;

import com.example.inspection_management.entity.Asset;
import com.example.inspection_management.repository.AssetRepository;
import com.example.inspection_management.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AssetControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private JwtService jwtService;

    private String adminToken;
    private String managerToken;
    private String inspectorToken;

    @BeforeEach
    void setUp() {
        adminToken = jwtService.generateToken("admin@test.com", "ADMIN");
        managerToken = jwtService.generateToken("manager@test.com", "MANAGER");
        inspectorToken = jwtService.generateToken("inspector@test.com", "INSPECTOR");
    }

    private Asset createAsset() {
        Asset asset = new Asset();
        asset.setName("Generator");
        asset.setSerialNumber("GEN-" + System.nanoTime());
        asset.setLocation("Building A");
        asset.setStatus("ACTIVE");
        return assetRepository.save(asset);
    }

    @Test
    void createAssetShouldReturn201ForAdmin() throws Exception {
        mockMvc.perform(post("/api/assets")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Generator",
                                  "serialNumber": "%s",
                                  "location": "Building A",
                                  "status": "ACTIVE"
                                }
                                """.formatted("GEN-TEST-" + System.nanoTime())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Generator"));
    }

    @Test
    void createAssetShouldReturn201ForManager() throws Exception {
        mockMvc.perform(post("/api/assets")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Pump",
                                  "serialNumber": "%s",
                                  "location": "Building B",
                                  "status": "ACTIVE"
                                }
                                """.formatted("PUMP-TEST-" + System.nanoTime())))
                .andExpect(status().isCreated());
    }

    @Test
    void createAssetShouldReturn403ForInspector() throws Exception {
        mockMvc.perform(post("/api/assets")
                        .header("Authorization", "Bearer " + inspectorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Pump",
                                  "serialNumber": "PUMP-TEST-002",
                                  "location": "Building B",
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void createAssetShouldReturn400ForInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/assets")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "serialNumber": "",
                                  "location": "",
                                  "status": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllAssetsShouldReturn200ForManager() throws Exception {
        createAsset();

        mockMvc.perform(get("/api/assets")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getAssetByIdShouldReturn200() throws Exception {
        Asset asset = createAsset();

        mockMvc.perform(get("/api/assets/" + asset.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(asset.getId()));
    }

    @Test
    void getAssetByIdShouldReturn404WhenMissing() throws Exception {
        mockMvc.perform(get("/api/assets/999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateAssetShouldReturn200() throws Exception {
        Asset asset = createAsset();

        mockMvc.perform(put("/api/assets/" + asset.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Updated Generator",
                                  "serialNumber": "%s",
                                  "location": "Building C",
                                  "status": "ACTIVE"
                                }
                                """.formatted("GEN-UPDATED-" + System.nanoTime())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Generator"));
    }

    @Test
    void deleteAssetShouldReturn204ForAdmin() throws Exception {
        Asset asset = createAsset();

        mockMvc.perform(delete("/api/assets/" + asset.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteAssetShouldReturn403ForManager() throws Exception {
        Asset asset = createAsset();

        mockMvc.perform(delete("/api/assets/" + asset.getId())
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllAssetsShouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/assets"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllAssetsShouldReturn401ForInvalidToken() throws Exception {
        mockMvc.perform(get("/api/assets")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }
}
