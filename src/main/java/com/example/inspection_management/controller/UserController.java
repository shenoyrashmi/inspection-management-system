package com.example.inspection_management.controller;

import com.example.inspection_management.dto.AuthResponseDTO;
import com.example.inspection_management.dto.LoginRequestDTO;
import com.example.inspection_management.dto.UserRequestDTO;
import com.example.inspection_management.dto.UserResponseDTO;
import com.example.inspection_management.dto.UserUpdateRequestDTO;
import com.example.inspection_management.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "User Management",
        description = "APIs for user registration, authentication and user administration"
)
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(
            summary = "Register user",
            description = "Registers a new user with the default INSPECTOR role"
    )
    @SecurityRequirements
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDTO registerUser(
            @Valid @RequestBody UserRequestDTO request) {
        return userService.registerUser(request);
    }

    @Operation(
            summary = "User login",
            description = "Authenticates user credentials and returns a JWT token"
    )
    @SecurityRequirements
    @PostMapping("/login")
    public AuthResponseDTO login(@Valid @RequestBody LoginRequestDTO request) {
        return userService.login(request);
    }

    @Operation(
            summary = "Get all users",
            description = "Retrieves all users. Requires ADMIN role"
    )
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<UserResponseDTO> getAllUsers() {
        return userService.getAllUsers();
    }

    @Operation(
            summary = "Get user by ID",
            description = "Retrieves a single user by ID. Requires ADMIN role"
    )
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public UserResponseDTO getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @Operation(
            summary = "Update user",
            description = "Updates a user's name and email. Requires ADMIN role"
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public UserResponseDTO updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequestDTO request) {

        return userService.updateUser(id, request);
    }

    @Operation(
            summary = "Delete user",
            description = "Deletes a user by ID. Requires ADMIN role"
    )
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
