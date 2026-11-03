package com.example.inspection_management.service;

import com.example.inspection_management.dto.AuthResponseDTO;
import com.example.inspection_management.dto.LoginRequestDTO;
import com.example.inspection_management.dto.UserRequestDTO;
import com.example.inspection_management.dto.UserResponseDTO;
import com.example.inspection_management.dto.UserUpdateRequestDTO;
import com.example.inspection_management.entity.Role;
import com.example.inspection_management.entity.User;
import com.example.inspection_management.exception.ResourceNotFoundException;
import com.example.inspection_management.exception.ConflictException;
import com.example.inspection_management.exception.AuthenticationException;
import com.example.inspection_management.repository.RoleRepository;
import com.example.inspection_management.repository.UserRepository;
import com.example.inspection_management.security.JwtService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository, BCryptPasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public UserResponseDTO registerUser(UserRequestDTO request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already registered");
        }

        Role role = roleRepository.findByName("INSPECTOR")
                .orElseThrow(() ->
                        new ResourceNotFoundException("INSPECTOR role not found"));

        User user = new User(
                request.getName(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                role
        );

        User savedUser = userRepository.save(user);

        return new UserResponseDTO(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole().getName()
        );
    }

    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(user -> new UserResponseDTO(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getRole().getName()
                ))
                .toList();
    }

    public UserResponseDTO getUserById(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with ID: " + id));

        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().getName()
        );
    }

    public UserResponseDTO updateUser(Long id, UserUpdateRequestDTO request) {

        if (userRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
            throw new RuntimeException("Email already registered");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with ID: " + id));

        user.setName(request.getName());
        user.setEmail(request.getEmail());

        User updatedUser = userRepository.save(user);

        return new UserResponseDTO(
                updatedUser.getId(),
                updatedUser.getName(),
                updatedUser.getEmail(),
                updatedUser.getRole().getName()
        );
    }

    public void deleteUser(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with ID: " + id));

        userRepository.delete(user);
    }

    public AuthResponseDTO login(LoginRequestDTO request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new AuthenticationException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthenticationException("Invalid email or password");
        }

        String token = jwtService.generateToken(
                user.getEmail(),
                user.getRole().getName()
        );

        return new AuthResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().getName(),
                token
        );
    }
}
