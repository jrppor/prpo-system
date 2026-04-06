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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.jirapat.prpo.dto.request.LoginRequest;
import com.jirapat.prpo.dto.request.RefreshTokenRequest;
import com.jirapat.prpo.dto.response.AuthResponse;
import com.jirapat.prpo.entity.Role;
import com.jirapat.prpo.entity.User;
import com.jirapat.prpo.exception.UnauthorizedException;
import com.jirapat.prpo.repository.UserRepository;
import com.jirapat.prpo.security.JwtService;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private Role testRole;

    @BeforeEach
    void setUp() {
        testRole = Role.builder()
                .id(UUID.randomUUID())
                .name("USER")
                .build();

        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .passwordHash("encoded-password")
                .role(testRole)
                .isActive(true)
                .build();
    }

    @Nested
    @DisplayName("login()")
    class LoginTests {

        @Test
        @DisplayName("should return auth response on valid credentials")
        void login_ValidCredentials_ReturnsAuthResponse() {
            LoginRequest request = LoginRequest.builder()
                    .email("test@example.com")
                    .password("password123")
                    .build();

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null);
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(jwtService.generateToken(testUser)).thenReturn("access-token");
            when(jwtService.generateRefreshToken(testUser)).thenReturn("refresh-token");
            when(jwtService.getJwtExpiration()).thenReturn(86400000L);

            AuthResponse response = authService.login(request);

            assertThat(response.getAccessToken()).isEqualTo("access-token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(response.getTokenType()).isEqualTo("Bearer");
            assertThat(response.getUser().getEmail()).isEqualTo("test@example.com");
            assertThat(response.getUser().getRole()).isEqualTo("USER");
            verify(authenticationManager).authenticate(any());
        }

        @Test
        @DisplayName("should normalize email to lowercase and trim")
        void login_EmailWithUpperCase_NormalizesEmail() {
            LoginRequest request = LoginRequest.builder()
                    .email("  TEST@Example.COM  ")
                    .password("password123")
                    .build();

            when(authenticationManager.authenticate(any())).thenReturn(null);
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(jwtService.generateToken(testUser)).thenReturn("token");
            when(jwtService.generateRefreshToken(testUser)).thenReturn("refresh");
            when(jwtService.getJwtExpiration()).thenReturn(86400000L);

            authService.login(request);

            verify(userRepository).findByEmail("test@example.com");
        }

        @Test
        @DisplayName("should throw when credentials are invalid")
        void login_InvalidCredentials_Throws() {
            LoginRequest request = LoginRequest.builder()
                    .email("test@example.com")
                    .password("wrong")
                    .build();

            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class);
        }
    }

    @Nested
    @DisplayName("refresh()")
    class RefreshTests {

        @Test
        @DisplayName("should return new tokens on valid refresh token")
        void refresh_ValidToken_ReturnsNewTokens() {
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken("valid-refresh-token")
                    .build();

            when(jwtService.extractUsername("valid-refresh-token")).thenReturn("test@example.com");
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(jwtService.isTokenValid("valid-refresh-token", testUser)).thenReturn(true);
            when(jwtService.generateToken(testUser)).thenReturn("new-access");
            when(jwtService.generateRefreshToken(testUser)).thenReturn("new-refresh");
            when(jwtService.getJwtExpiration()).thenReturn(86400000L);

            AuthResponse response = authService.refresh(request);

            assertThat(response.getAccessToken()).isEqualTo("new-access");
            assertThat(response.getRefreshToken()).isEqualTo("new-refresh");
        }

        @Test
        @DisplayName("should throw UnauthorizedException on invalid refresh token")
        void refresh_InvalidToken_ThrowsUnauthorized() {
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken("invalid-token")
                    .build();

            when(jwtService.extractUsername("invalid-token")).thenThrow(new RuntimeException("bad token"));

            assertThatThrownBy(() -> authService.refresh(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Invalid refresh token");
        }

        @Test
        @DisplayName("should throw UnauthorizedException when user not found")
        void refresh_UserNotFound_ThrowsUnauthorized() {
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken("token")
                    .build();

            when(jwtService.extractUsername("token")).thenReturn("missing@example.com");
            when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("User not found");
        }

        @Test
        @DisplayName("should throw UnauthorizedException when token is expired")
        void refresh_ExpiredToken_ThrowsUnauthorized() {
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken("expired-token")
                    .build();

            when(jwtService.extractUsername("expired-token")).thenReturn("test@example.com");
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(jwtService.isTokenValid("expired-token", testUser)).thenReturn(false);

            assertThatThrownBy(() -> authService.refresh(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Refresh token is expired or invalid");
        }
    }
}
