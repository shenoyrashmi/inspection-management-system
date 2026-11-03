package com.example.inspection_management.controller;

import com.example.inspection_management.entity.Role;
import com.example.inspection_management.service.RoleService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public List<Role> getAllRoles() {
        return roleService.getAllRoles();
    }

    @PostMapping
    public Role createRole(@RequestBody String name) {
        return roleService.createRole(name);
    }
}
