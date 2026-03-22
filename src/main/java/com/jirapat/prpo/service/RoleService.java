package com.jirapat.prpo.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jirapat.prpo.dto.request.CreateRoleRequest;
import com.jirapat.prpo.dto.request.UpdateRoleDetailRequest;
import com.jirapat.prpo.dto.response.RoleResponse;
import com.jirapat.prpo.entity.Role;
import com.jirapat.prpo.exception.DuplicateResourceException;
import com.jirapat.prpo.exception.ResourceNotFoundException;
import com.jirapat.prpo.mapper.RoleMapper;
import com.jirapat.prpo.repository.RoleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        log.info("Fetching all roles");
        return roleRepository.findAll().stream()
                .map(roleMapper::toRoleResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoleResponse getRoleById(UUID id) {
        log.info("Fetching role by id: {}", id);
        Role role = findRoleById(id);
        return roleMapper.toRoleResponse(role);
    }

    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        String roleName = request.getName().toUpperCase().trim();
        log.info("Creating new role: {}", roleName);

        if (roleRepository.existsByName(roleName)) {
            throw new DuplicateResourceException("Role", "name", roleName);
        }

        Role role = roleMapper.toEntity(request);
        role.setName(roleName);

        Role savedRole = roleRepository.save(role);
        log.info("Role created successfully with id: {}", savedRole.getId());
        return roleMapper.toRoleResponse(savedRole);
    }

    @Transactional
    public RoleResponse updateRole(UUID id, UpdateRoleDetailRequest request) {
        log.info("Updating role: {}", id);
        Role role = findRoleById(id);

        if (request.getName() != null) {
            String newName = request.getName().toUpperCase().trim();
            if (!role.getName().equals(newName) && roleRepository.existsByName(newName)) {
                throw new DuplicateResourceException("Role", "name", newName);
            }
            request.setName(newName);
        }

        roleMapper.updateEntityFromRequest(request, role);

        Role savedRole = roleRepository.save(role);
        log.info("Role updated successfully: {}", id);
        return roleMapper.toRoleResponse(savedRole);
    }

    @Transactional
    public void deleteRole(UUID id) {
        log.info("Deleting role: {}", id);
        Role role = findRoleById(id);
        roleRepository.delete(role);
        log.info("Role deleted successfully: {}", id);
    }

    // ============ Helper Methods ============

    public Role findRoleById(UUID id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", id.toString()));
    }

    public Role findRoleByName(String name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", name));
    }
}
