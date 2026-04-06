package com.jirapat.prpo.service;

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
import org.springframework.security.crypto.password.PasswordEncoder;

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

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private SecurityService securityService;
    @Mock private RoleService roleService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private Role testRole;
    private UserResponse testUserResponse;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testRole = Role.builder().id(UUID.randomUUID()).name("USER").build();
        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .passwordHash("encoded")
                .role(testRole)
                .isActive(true)
                .build();
        testUserResponse = UserResponse.builder()
                .id(userId)
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .build();
    }

    @Nested
    @DisplayName("createUser()")
    class CreateUserTests {

        @Test
        @DisplayName("should create user successfully")
        void createUser_ValidRequest_ReturnsUserResponse() {
            CreateUserRequest request = CreateUserRequest.builder()
                    .email("new@example.com")
                    .password("password123")
                    .firstName("New")
                    .lastName("User")
                    .roleId(testRole.getId())
                    .build();

            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(roleService.findRoleById(testRole.getId())).thenReturn(testRole);
            when(userMapper.toEntity(request)).thenReturn(testUser);
            when(passwordEncoder.encode("password123")).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(userMapper.toUserResponse(testUser)).thenReturn(testUserResponse);

            UserResponse result = userService.createUser(request);

            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("test@example.com");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("should throw DuplicateResourceException on duplicate email")
        void createUser_DuplicateEmail_Throws() {
            CreateUserRequest request = CreateUserRequest.builder()
                    .email("test@example.com")
                    .password("password123")
                    .roleId(testRole.getId())
                    .build();

            when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(request))
                    .isInstanceOf(DuplicateResourceException.class);
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getUserById()")
    class GetUserByIdTests {

        @Test
        @DisplayName("should return user when found")
        void getUserById_Found_ReturnsResponse() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userMapper.toUserResponse(testUser)).thenReturn(testUserResponse);

            UserResponse result = userService.getUserById(userId);

            assertThat(result.getId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when not found")
        void getUserById_NotFound_Throws() {
            UUID missingId = UUID.randomUUID();
            when(userRepository.findById(missingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById(missingId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateUserRole()")
    class UpdateRoleTests {

        @Test
        @DisplayName("should update role successfully")
        void updateRole_Valid_ReturnsUpdatedUser() {
            UUID newRoleId = UUID.randomUUID();
            Role newRole = Role.builder().id(newRoleId).name("ADMIN").build();
            UpdateRoleRequest request = UpdateRoleRequest.builder().roleId(newRoleId).build();
            UUID currentUserId = UUID.randomUUID(); // different from testUser

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(securityService.getCurrentUserId()).thenReturn(currentUserId);
            when(roleService.findRoleById(newRoleId)).thenReturn(newRole);
            when(userRepository.save(testUser)).thenReturn(testUser);
            when(userMapper.toUserResponse(testUser)).thenReturn(testUserResponse);

            UserResponse result = userService.updateUserRole(userId, request);

            assertThat(result).isNotNull();
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("should throw when changing own role")
        void updateRole_OwnRole_Throws() {
            UpdateRoleRequest request = UpdateRoleRequest.builder().roleId(UUID.randomUUID()).build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(securityService.getCurrentUserId()).thenReturn(userId);

            assertThatThrownBy(() -> userService.updateUserRole(userId, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Cannot change your own role");
        }
    }

    @Nested
    @DisplayName("updateUserActive()")
    class UpdateActiveTests {

        @Test
        @DisplayName("should update active status")
        void updateActive_Valid_ReturnsUpdatedUser() {
            UpdateActiveRequest request = new UpdateActiveRequest();
            request.setIsActive(false);
            UUID currentUserId = UUID.randomUUID();

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(securityService.getCurrentUserId()).thenReturn(currentUserId);
            when(userRepository.save(testUser)).thenReturn(testUser);
            when(userMapper.toUserResponse(testUser)).thenReturn(testUserResponse);

            UserResponse result = userService.updateUserActive(userId, request);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should throw when deactivating own account")
        void updateActive_OwnAccount_Throws() {
            UpdateActiveRequest request = new UpdateActiveRequest();
            request.setIsActive(false);

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(securityService.getCurrentUserId()).thenReturn(userId);

            assertThatThrownBy(() -> userService.updateUserActive(userId, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Cannot deactivate your own account");
        }
    }

    @Nested
    @DisplayName("getMyProfile() / updateMyProfile()")
    class ProfileTests {

        @Test
        @DisplayName("should return current user profile")
        void getMyProfile_ReturnsProfile() {
            when(securityService.getCurrentUser()).thenReturn(testUser);
            when(userMapper.toUserResponse(testUser)).thenReturn(testUserResponse);

            UserResponse result = userService.getMyProfile();

            assertThat(result.getEmail()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("should update current user profile")
        void updateMyProfile_ReturnsUpdatedProfile() {
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .firstName("Updated")
                    .lastName("Name")
                    .build();

            when(securityService.getCurrentUser()).thenReturn(testUser);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userRepository.save(testUser)).thenReturn(testUser);
            when(userMapper.toUserResponse(testUser)).thenReturn(testUserResponse);

            UserResponse result = userService.updateMyProfile(request);

            assertThat(result).isNotNull();
            verify(userMapper).updateProfileFromRequest(request, testUser);
        }
    }
}
