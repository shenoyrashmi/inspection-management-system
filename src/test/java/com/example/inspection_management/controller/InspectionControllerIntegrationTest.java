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
class InspectionControllerIntegrationTest {

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
    private JwtService jwtService;

    private String adminToken;
    private String managerToken;
    private String inspectorToken;
    private Asset asset;
    private User inspector;

    @BeforeEach
    void setUp() {
        adminToken = jwtService.generateToken("admin@test.com", "ADMIN");
        managerToken = jwtService.generateToken("manager@test.com", "MANAGER");
        inspectorToken = jwtService.generateToken("inspector@test.com", "INSPECTOR");

        asset = createAsset();
        inspector = createInspector();
    }

    private Asset createAsset() {
        Asset asset = new Asset();
        asset.setName("Inspection Generator");
        asset.setSerialNumber("INS-" + System.nanoTime());
        asset.setLocation("Building A");
        asset.setStatus("ACTIVE");
        return assetRepository.save(asset);
    }

    private User createInspector() {
        Role role = roleRepository.findByName("INSPECTOR")
                .orElseThrow();

        User user = new User(
                "Test Inspector",
                "inspector-" + System.nanoTime() + "@test.com",
                "encoded-password",
                role
        );

        return userRepository.save(user);
    }

    private Inspection createInspection(InspectionStatus status) {
        Inspection inspection = new Inspection();
        inspection.setAsset(asset);
        inspection.setInspector(inspector);
        inspection.setScheduledDate(LocalDate.now());
        inspection.setStatus(status);
        inspection.setFindings("Initial findings");

        return inspectionRepository.save(inspection);
    }

    @Test
    void createInspectionShouldReturn201ForManager() throws Exception {
        mockMvc.perform(post("/api/inspections")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assetId": %d,
                                  "inspectorId": %d,
                                  "scheduledDate": "%s",
                                  "findings": "Initial inspection"
                                }
                                """.formatted(asset.getId(), inspector.getId(), LocalDate.now())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    void createInspectionShouldReturn403ForInspector() throws Exception {
        mockMvc.perform(post("/api/inspections")
                        .header("Authorization", "Bearer " + inspectorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assetId": %d,
                                  "inspectorId": %d,
                                  "scheduledDate": "%s"
                                }
                                """.formatted(asset.getId(), inspector.getId(), LocalDate.now())))
                .andExpect(status().isForbidden());
    }

    @Test
    void createInspectionShouldReturn404ForMissingAsset() throws Exception {
        mockMvc.perform(post("/api/inspections")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assetId": 999999,
                                  "inspectorId": %d,
                                  "scheduledDate": "%s"
                                }
                                """.formatted(inspector.getId(), LocalDate.now())))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllInspectionsShouldReturn200ForAdmin() throws Exception {
        createInspection(InspectionStatus.SCHEDULED);

        mockMvc.perform(get("/api/inspections")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getInspectionByIdShouldReturn200() throws Exception {
        Inspection inspection = createInspection(InspectionStatus.SCHEDULED);

        mockMvc.perform(get("/api/inspections/" + inspection.getId())
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(inspection.getId()));
    }

    @Test
    void getMyInspectionsShouldReturn200ForInspector() throws Exception {
        createInspection(InspectionStatus.SCHEDULED);

        String token = jwtService.generateToken(inspector.getEmail(), "INSPECTOR");

        mockMvc.perform(get("/api/inspections/my-inspections")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getMyInspectionsShouldReturn403ForAdmin() throws Exception {
        mockMvc.perform(get("/api/inspections/my-inspections")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void submitInspectionShouldMoveToInProgress() throws Exception {
        Inspection inspection = createInspection(InspectionStatus.SCHEDULED);

        String token = jwtService.generateToken(inspector.getEmail(), "INSPECTOR");

        mockMvc.perform(put("/api/inspections/" + inspection.getId() + "/submit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "IN_PROGRESS",
                                  "findings": "Inspection started"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void submitInspectionShouldReturn400ForInvalidTransition() throws Exception {
        Inspection inspection = createInspection(InspectionStatus.SCHEDULED);

        String token = jwtService.generateToken(inspector.getEmail(), "INSPECTOR");

        mockMvc.perform(put("/api/inspections/" + inspection.getId() + "/submit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "COMPLETED",
                                  "findings": "Attempted completion"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitInspectionShouldReturn403ForAnotherInspector() throws Exception {
        Inspection inspection = createInspection(InspectionStatus.SCHEDULED);

        String anotherInspectorToken =
                jwtService.generateToken("another-inspector@test.com", "INSPECTOR");

        mockMvc.perform(put("/api/inspections/" + inspection.getId() + "/submit")
                        .header("Authorization", "Bearer " + anotherInspectorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "IN_PROGRESS",
                                  "findings": "Unauthorized attempt"
                                }
                                """))
                .andExpect(status().isForbidden());
    }
}
