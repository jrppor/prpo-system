package com.jirapat.prpo.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jirapat.prpo.dto.request.CreateUserRequest;
import com.jirapat.prpo.dto.request.UpdateActiveRequest;
import com.jirapat.prpo.dto.request.UpdateProfileRequest;
import com.jirapat.prpo.dto.request.UpdateRoleRequest;
import com.jirapat.prpo.dto.response.UserResponse;
import com.jirapat.prpo.entity.Role;
import com.jirapat.prpo.entity.User;
import com.jirapat.prpo.exception.BadRequestException;
import com.jirapat.prpo.exception.DuplicateResourceException;
import com.jirapat.prpo.exception.ResourceNotFoundException;
import com.jirapat.prpo.mapper.UserMapper;
import com.jirapat.prpo.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final SecurityService securityService;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;

    public UserResponse createUser(CreateUserRequest request) {
        log.info("Creating new user with email: {}", request.getEmail());

        String email = request.getEmail().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        Role role = roleService.findRoleById(request.getRoleId());
        User user = userMapper.toEntity(request);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);

        User savedUser = userRepository.save(user);
        log.info("User created successfully with id: {}", savedUser.getId());
        return userMapper.toUserResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        log.info("Fetching all users, page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        return userRepository.findAll(pageable)
                .map(userMapper::toUserResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        log.info("Fetching user by id: {}", id);
        User user = findUserById(id);
        return userMapper.toUserResponse(user);
    }

    public UserResponse updateUserRole(UUID id, UpdateRoleRequest request) {
        log.info("Updating role for user: {} to roleId: {}", id, request.getRoleId());
        User user = findUserById(id);

        UUID currentUserId = securityService.getCurrentUserId();
        if (user.getId().equals(currentUserId)) {
            throw new BadRequestException("Cannot change your own role");
        }

        Role role = roleService.findRoleById(request.getRoleId());
        user.setRole(role);
        User savedUser = userRepository.save(user);
        log.info("Role updated successfully for user: {}", id);
        return userMapper.toUserResponse(savedUser);
    }

    public UserResponse updateUserActive(UUID id, UpdateActiveRequest request) {
        log.info("Updating active status for user: {} to {}", id, request.getIsActive());
        User user = findUserById(id);

        UUID currentUserId = securityService.getCurrentUserId();
        if (user.getId().equals(currentUserId)) {
            throw new BadRequestException("Cannot deactivate your own account");
        }

        user.setIsActive(request.getIsActive());
        User savedUser = userRepository.save(user);
        log.info("Active status updated successfully for user: {}", id);
        return userMapper.toUserResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public UserResponse getMyProfile() {
        User currentUser = securityService.getCurrentUser();
        log.info("Fetching profile for user: {}", currentUser.getId());
        return userMapper.toUserResponse(currentUser);
    }

    public UserResponse updateMyProfile(UpdateProfileRequest request) {
        User currentUser = securityService.getCurrentUser();
        log.info("Updating profile for user: {}", currentUser.getId());

        User user = findUserById(currentUser.getId());
        userMapper.updateProfileFromRequest(request, user);

        User savedUser = userRepository.save(user);
        log.info("Profile updated successfully for user: {}", savedUser.getId());
        return userMapper.toUserResponse(savedUser);
    }

    private User findUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id.toString()));
    }
}
