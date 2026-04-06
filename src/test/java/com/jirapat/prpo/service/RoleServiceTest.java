package com.jirapat.prpo.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jirapat.prpo.dto.request.CreateRoleRequest;
import com.jirapat.prpo.dto.request.UpdateRoleDetailRequest;
import com.jirapat.prpo.dto.response.RoleResponse;
import com.jirapat.prpo.entity.Role;
import com.jirapat.prpo.exception.DuplicateResourceException;
import com.jirapat.prpo.exception.ResourceNotFoundException;
import com.jirapat.prpo.mapper.RoleMapper;
import com.jirapat.prpo.repository.RoleRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleService Unit Tests")
class RoleServiceTest {

    @Mock private RoleRepository roleRepository;
    @Mock private RoleMapper roleMapper;

    @InjectMocks
    private RoleService roleService;

    private Role testRole;
    private RoleResponse testRoleResponse;
    private UUID roleId;

    @BeforeEach
    void setUp() {
        roleId = UUID.randomUUID();
        testRole = Role.builder()
                .id(roleId)
                .name("ADMIN")
                .description("Administrator")
                .isActive(true)
                .build();
        testRoleResponse = RoleResponse.builder()
                .id(roleId)
                .name("ADMIN")
                .description("Administrator")
                .isActive(true)
                .build();
    }

    @Nested
    @DisplayName("getAllRoles()")
    class GetAllRolesTests {

        @Test
        @DisplayName("should return all roles")
        void getAllRoles_ReturnsAll() {
            when(roleRepository.findAll()).thenReturn(List.of(testRole));
            when(roleMapper.toRoleResponse(testRole)).thenReturn(testRoleResponse);

            List<RoleResponse> result = roleService.getAllRoles();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("ADMIN");
        }
    }

    @Nested
    @DisplayName("getRoleById()")
    class GetRoleByIdTests {

        @Test
        @DisplayName("should return role when found")
        void getRoleById_Found_Returns() {
            when(roleRepository.findById(roleId)).thenReturn(Optional.of(testRole));
            when(roleMapper.toRoleResponse(testRole)).thenReturn(testRoleResponse);

            RoleResponse result = roleService.getRoleById(roleId);

            assertThat(result.getName()).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("should throw when not found")
        void getRoleById_NotFound_Throws() {
            UUID missingId = UUID.randomUUID();
            when(roleRepository.findById(missingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roleService.getRoleById(missingId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("createRole()")
    class CreateRoleTests {

        @Test
        @DisplayName("should create role with uppercase name")
        void createRole_Valid_Returns() {
            CreateRoleRequest request = CreateRoleRequest.builder()
                    .name("manager")
                    .description("Manager role")
                    .build();

            when(roleRepository.existsByName("MANAGER")).thenReturn(false);
            when(roleMapper.toEntity(request)).thenReturn(testRole);
            when(roleRepository.save(any(Role.class))).thenReturn(testRole);
            when(roleMapper.toRoleResponse(testRole)).thenReturn(testRoleResponse);

            RoleResponse result = roleService.createRole(request);

            assertThat(result).isNotNull();
            verify(roleRepository).save(any(Role.class));
        }

        @Test
        @DisplayName("should throw on duplicate name")
        void createRole_DuplicateName_Throws() {
            CreateRoleRequest request = CreateRoleRequest.builder()
                    .name("ADMIN")
                    .build();

            when(roleRepository.existsByName("ADMIN")).thenReturn(true);

            assertThatThrownBy(() -> roleService.createRole(request))
                    .isInstanceOf(DuplicateResourceException.class);
            verify(roleRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateRole()")
    class UpdateRoleTests {

        @Test
        @DisplayName("should update role successfully")
        void updateRole_Valid_Returns() {
            UpdateRoleDetailRequest request = UpdateRoleDetailRequest.builder()
                    .name("SUPER_ADMIN")
                    .description("Updated desc")
                    .build();

            when(roleRepository.findById(roleId)).thenReturn(Optional.of(testRole));
            when(roleRepository.existsByName("SUPER_ADMIN")).thenReturn(false);
            when(roleRepository.save(any(Role.class))).thenReturn(testRole);
            when(roleMapper.toRoleResponse(testRole)).thenReturn(testRoleResponse);

            RoleResponse result = roleService.updateRole(roleId, request);

            assertThat(result).isNotNull();
            verify(roleMapper).updateEntityFromRequest(request, testRole);
        }

        @Test
        @DisplayName("should throw when renaming to existing name")
        void updateRole_DuplicateName_Throws() {
            UpdateRoleDetailRequest request = UpdateRoleDetailRequest.builder()
                    .name("USER")
                    .build();

            when(roleRepository.findById(roleId)).thenReturn(Optional.of(testRole));
            when(roleRepository.existsByName("USER")).thenReturn(true);

            assertThatThrownBy(() -> roleService.updateRole(roleId, request))
                    .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        @DisplayName("should allow keeping same name")
        void updateRole_SameName_NoConflict() {
            UpdateRoleDetailRequest request = UpdateRoleDetailRequest.builder()
                    .name("ADMIN")
                    .description("Updated")
                    .build();

            when(roleRepository.findById(roleId)).thenReturn(Optional.of(testRole));
            when(roleRepository.save(any(Role.class))).thenReturn(testRole);
            when(roleMapper.toRoleResponse(testRole)).thenReturn(testRoleResponse);

            RoleResponse result = roleService.updateRole(roleId, request);

            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("deleteRole()")
    class DeleteRoleTests {

        @Test
        @DisplayName("should soft-delete role")
        void deleteRole_Found_SoftDeletes() {
            when(roleRepository.findById(roleId)).thenReturn(Optional.of(testRole));

            roleService.deleteRole(roleId);

            assertThat(testRole.getDeletedAt()).isNotNull();
            verify(roleRepository).save(testRole);
        }
    }

    @Nested
    @DisplayName("findRoleByName()")
    class FindRoleByNameTests {

        @Test
        @DisplayName("should return role by name")
        void findByName_Found_Returns() {
            when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(testRole));

            Role result = roleService.findRoleByName("ADMIN");

            assertThat(result.getName()).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("should throw when name not found")
        void findByName_NotFound_Throws() {
            when(roleRepository.findByName("MISSING")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roleService.findRoleByName("MISSING"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
