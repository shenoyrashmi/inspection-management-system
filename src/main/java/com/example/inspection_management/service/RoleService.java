package com.example.inspection_management.service;

import com.example.inspection_management.entity.Role;
import com.example.inspection_management.repository.RoleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    public Role createRole(String name) {
        if (roleRepository.findByName(name).isPresent()) {
            throw new RuntimeException("Role already exists: " + name);
        }

        return roleRepository.save(new Role(name));
    }
}
