package com.example.inspection_management.service;

import com.example.inspection_management.dto.*;
import com.example.inspection_management.entity.Role;
import com.example.inspection_management.entity.User;
import com.example.inspection_management.exception.ResourceNotFoundException;
import com.example.inspection_management.repository.RoleRepository;
import com.example.inspection_management.repository.UserRepository;
import com.example.inspection_management.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UserService userService;

    private Role createRole() {
        return new Role("INSPECTOR");
    }

    private User createUser() {
        return new User(
                "Test User",
                "test@example.com",
                "encoded-password",
                createRole()
        );
    }

    private UserRequestDTO createRegistrationRequest() {
        UserRequestDTO request = new UserRequestDTO();
        request.setName("Test User");
        request.setEmail("test@example.com");
        request.setPassword("password123");
        return request;
    }

    @Test
    void registerUserShouldSaveUserWithEncodedPassword() {
        User user = createUser();

        when(userRepository.existsByEmail("test@example.com"))
                .thenReturn(false);
        when(roleRepository.findByName("INSPECTOR"))
                .thenReturn(Optional.of(createRole()));
        when(passwordEncoder.encode("password123"))
                .thenReturn("encoded-password");
        when(userRepository.save(any(User.class)))
                .thenReturn(user);

        UserResponseDTO response =
                userService.registerUser(createRegistrationRequest());

        assertEquals("Test User", response.getName());
        assertEquals("test@example.com", response.getEmail());

        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUserShouldRejectDuplicateEmail() {
        when(userRepository.existsByEmail("test@example.com"))
                .thenReturn(true);

        assertThrows(
                RuntimeException.class,
                () -> userService.registerUser(createRegistrationRequest())
        );

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUserShouldFailWhenInspectorRoleMissing() {
        when(userRepository.existsByEmail("test@example.com"))
                .thenReturn(false);
        when(roleRepository.findByName("INSPECTOR"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> userService.registerUser(createRegistrationRequest())
        );

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getAllUsersShouldReturnUsers() {
        when(userRepository.findAll())
                .thenReturn(List.of(createUser()));

        List<UserResponseDTO> response = userService.getAllUsers();

        assertEquals(1, response.size());
        assertEquals("test@example.com", response.get(0).getEmail());
    }

    @Test
    void getUserByIdShouldReturnUser() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(createUser()));

        UserResponseDTO response = userService.getUserById(1L);

        assertEquals("Test User", response.getName());
    }

    @Test
    void getUserByIdShouldThrowWhenNotFound() {
        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> userService.getUserById(999L)
        );
    }

    @Test
    void updateUserShouldUpdateExistingUser() {
        User user = createUser();

        UserUpdateRequestDTO request = new UserUpdateRequestDTO();
        request.setName("Updated User");
        request.setEmail("updated@example.com");

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndIdNot(
                "updated@example.com", 1L))
                .thenReturn(false);
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDTO response = userService.updateUser(1L, request);

        assertEquals("Updated User", response.getName());
        assertEquals("updated@example.com", response.getEmail());
    }

    @Test
    void updateUserShouldRejectDuplicateEmail() {
        UserUpdateRequestDTO request = new UserUpdateRequestDTO();
        request.setName("Updated User");
        request.setEmail("existing@example.com");

        // updateUser() checks for a duplicate email before loading the user,
        // so findById() is never reached in this scenario.
        when(userRepository.existsByEmailAndIdNot(
                "existing@example.com", 1L))
                .thenReturn(true);

        assertThrows(
                RuntimeException.class,
                () -> userService.updateUser(1L, request)
        );

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deleteUserShouldDeleteExistingUser() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(createUser()));

        assertDoesNotThrow(() -> userService.deleteUser(1L));

        verify(userRepository).delete(any(User.class));
    }

    @Test
    void loginShouldReturnTokenForValidCredentials() {
        User user = createUser();

        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(
                "password123", "encoded-password"))
                .thenReturn(true);
        when(jwtService.generateToken(
                "test@example.com", "INSPECTOR"))
                .thenReturn("test-jwt-token");

        AuthResponseDTO response = userService.login(request);

        assertEquals("test-jwt-token", response.getToken());
        assertEquals("test@example.com", response.getEmail());
    }

    @Test
    void loginShouldRejectInvalidPassword() {
        User user = createUser();

        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("test@example.com");
        request.setPassword("wrong-password");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(
                "wrong-password", "encoded-password"))
                .thenReturn(false);

        assertThrows(
                RuntimeException.class,
                () -> userService.login(request)
        );

        verify(jwtService, never()).generateToken(anyString(), anyString());
    }

    @Test
    void loginShouldRejectUnknownEmail() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("unknown@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                RuntimeException.class,
                () -> userService.login(request)
        );
    }
}
