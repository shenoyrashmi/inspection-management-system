package com.example.inspection_management.controller;

import com.example.inspection_management.entity.*;
import com.example.inspection_management.repository.*;
import com.example.inspection_management.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MaintenanceRequestControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private InspectionRepository inspectionRepository;

    @Autowired
    private MaintenanceRequestRepository maintenanceRequestRepository;

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

    private MaintenanceRequest createMaintenanceRequest() {
        Asset asset = new Asset();
        asset.setName("Maintenance Generator");
        asset.setSerialNumber("MNT-" + System.nanoTime());
        asset.setLocation("Building A");
        asset.setStatus("ACTIVE");
        asset = assetRepository.save(asset);

        Role role = roleRepository.findByName("INSPECTOR").orElseThrow();

        User inspector = new User(
                "Maintenance Inspector",
                "maintenance-" + System.nanoTime() + "@test.com",
                "encoded-password",
                role
        );
        inspector = userRepository.save(inspector);

        Inspection inspection = new Inspection();
        inspection.setAsset(asset);
        inspection.setInspector(inspector);
        inspection.setScheduledDate(LocalDate.now());
        inspection.setStatus(InspectionStatus.FAILED);
        inspection.setFindings("Oil leakage detected");
        inspection = inspectionRepository.save(inspection);

        MaintenanceRequest request = new MaintenanceRequest();
        request.setAsset(asset);
        request.setInspection(inspection);
        request.setDescription("Oil leakage detected");
        request.setStatus(MaintenanceStatus.OPEN);

        return maintenanceRequestRepository.save(request);
    }

    @Test
    void getAllMaintenanceRequestsShouldReturn200ForAdmin() throws Exception {
        createMaintenanceRequest();

        mockMvc.perform(get("/api/maintenance")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getAllMaintenanceRequestsShouldReturn200ForManager() throws Exception {
        mockMvc.perform(get("/api/maintenance")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk());
    }

    @Test
    void getAllMaintenanceRequestsShouldReturn403ForInspector() throws Exception {
        mockMvc.perform(get("/api/maintenance")
                        .header("Authorization", "Bearer " + inspectorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateMaintenanceStatusShouldReturn200() throws Exception {
        MaintenanceRequest request = createMaintenanceRequest();

        mockMvc.perform(put("/api/maintenance/" + request.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "IN_PROGRESS"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void updateMaintenanceStatusShouldRejectInvalidTransition() throws Exception {
        MaintenanceRequest request = createMaintenanceRequest();

        mockMvc.perform(put("/api/maintenance/" + request.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "COMPLETED"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateMaintenanceStatusShouldReturn404WhenMissing() throws Exception {
        mockMvc.perform(put("/api/maintenance/999999/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "IN_PROGRESS"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateMaintenanceStatusShouldReturn403ForInspector() throws Exception {
        MaintenanceRequest request = createMaintenanceRequest();

        mockMvc.perform(put("/api/maintenance/" + request.getId() + "/status")
                        .header("Authorization", "Bearer " + inspectorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "IN_PROGRESS"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllMaintenanceRequestsShouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/maintenance"))
                .andExpect(status().isUnauthorized());
    }
}
