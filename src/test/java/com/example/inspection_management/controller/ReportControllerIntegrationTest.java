package com.example.inspection_management.controller;

import com.example.inspection_management.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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

    @Test
    void getSummaryReportShouldReturn200ForAdmin() throws Exception {
        mockMvc.perform(get("/api/reports/summary")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAssets").exists())
                .andExpect(jsonPath("$.totalInspections").exists())
                .andExpect(jsonPath("$.completedInspections").exists())
                .andExpect(jsonPath("$.failedInspections").exists())
                .andExpect(jsonPath("$.pendingInspections").exists())
                .andExpect(jsonPath("$.openMaintenanceRequests").exists());
    }

    @Test
    void getSummaryReportShouldReturn200ForManager() throws Exception {
        mockMvc.perform(get("/api/reports/summary")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk());
    }

    @Test
    void getSummaryReportShouldReturn403ForInspector() throws Exception {
        mockMvc.perform(get("/api/reports/summary")
                        .header("Authorization", "Bearer " + inspectorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAssetInspectionHistoryShouldReturn404ForMissingAsset() throws Exception {
        mockMvc.perform(get("/api/reports/assets/999999/inspections")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAssetInspectionHistoryShouldReturn403ForInspector() throws Exception {
        mockMvc.perform(get("/api/reports/assets/1/inspections")
                        .header("Authorization", "Bearer " + inspectorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getSummaryReportShouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/reports/summary"))
                .andExpect(status().isUnauthorized());
    }
}
