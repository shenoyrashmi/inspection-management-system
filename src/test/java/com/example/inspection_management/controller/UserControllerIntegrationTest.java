package com.example.inspection_management.controller;

import com.example.inspection_management.entity.Role;
import com.example.inspection_management.entity.User;
import com.example.inspection_management.repository.RoleRepository;
import com.example.inspection_management.repository.UserRepository;
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
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JwtService jwtService;

    private String adminToken;
    private String inspectorToken;

    @BeforeEach
    void setUp() {
        adminToken = jwtService.generateToken("admin@test.com", "ADMIN");
        inspectorToken = jwtService.generateToken("inspector@test.com", "INSPECTOR");
    }

    private User createUser(String email) {
        Role role = roleRepository.findByName("INSPECTOR")
                .orElseThrow();

        User user = new User(
                "Test Inspector",
                email,
                "encoded-password",
                role
        );

        return userRepository.save(user);
    }

    @Test
    void registerUserShouldReturn201() throws Exception {
        String suffix = System.nanoTime() + "";
        String email = "new-inspector-" + suffix + "@test.com";

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "New Inspector",
                                  "email": "%s",
                                  "password": "Password123"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void registerUserShouldReturn400ForInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "email": "invalid-email",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerUserShouldRejectDuplicateEmail() throws Exception {
        String suffix = System.nanoTime() + "";
        String email = "duplicate-" + suffix + "@test.com";

        createUser(email);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Another Inspector",
                                  "email": "%s",
                                  "password": "Password123"
                                }
                                """.formatted(email)))
                .andExpect(status().isConflict());
    }

    @Test
    void loginShouldReturnTokenForValidCredentials() throws Exception {
        String suffix = System.nanoTime() + "";
        String email = "login-inspector-" + suffix + "@test.com";

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Login Inspector",
                                  "email": "%s",
                                  "password": "Password123"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "Password123"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void loginShouldRejectInvalidPassword() throws Exception {
        String suffix = System.nanoTime() + "";
        String email = "password-test-" + suffix + "@test.com";

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Password Test",
                                  "email": "%s",
                                  "password": "Password123"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "WrongPassword"
                                }
                                """.formatted(email)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllUsersShouldReturn200ForAdmin() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getAllUsersShouldReturn403ForInspector() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + inspectorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserByIdShouldReturn200() throws Exception {
        String suffix = System.nanoTime() + "";
        String email = "get-user-" + suffix + "@test.com";

        User user = createUser(email);

        mockMvc.perform(get("/api/users/" + user.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void updateUserShouldReturn200() throws Exception {
        String suffix = System.nanoTime() + "";
        String email = "update-user-" + suffix + "@test.com";
        String updatedEmail = "updated-user-" + suffix + "@test.com";

        User user = createUser(email);

        mockMvc.perform(put("/api/users/" + user.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Updated Inspector",
                                  "email": "%s"
                                }
                                """.formatted(updatedEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Inspector"));
    }

    @Test
    void deleteUserShouldReturn204() throws Exception {
        User user = createUser("delete-user-" + System.nanoTime() + "@test.com");

        mockMvc.perform(delete("/api/users/" + user.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void getAllUsersShouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }
}
