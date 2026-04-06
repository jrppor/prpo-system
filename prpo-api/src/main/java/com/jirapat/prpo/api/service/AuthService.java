package com.jirapat.prpo.api.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import com.jirapat.prpo.api.dto.request.LoginRequest;
import com.jirapat.prpo.api.dto.request.RefreshTokenRequest;
import com.jirapat.prpo.api.dto.response.AuthResponse;
import com.jirapat.prpo.api.entity.User;
import com.jirapat.prpo.api.exception.UnauthorizedException;
import com.jirapat.prpo.api.repository.UserRepository;
import com.jirapat.prpo.api.security.JwtService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Login User
     */
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        // Authenticate
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().toLowerCase().trim(),
                        request.getPassword()
                )
        );

        // ดึง User
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow();

        log.info("User logged in successfully: {}", user.getEmail());
        return generateAuthResponse(user);
    }

    /**
     * Refresh access token ด้วย refresh token
     */
    public AuthResponse refresh(RefreshTokenRequest request) {
        log.info("Refresh token attempt");

        String refreshToken = request.getRefreshToken();
        String email;

        try {
            email = jwtService.extractUsername(refreshToken);
        } catch (Exception e) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new UnauthorizedException("Refresh token is expired or invalid");
        }

        log.info("Token refreshed successfully for user: {}", user.getEmail());
        return generateAuthResponse(user);
    }

    /**
     * สร้าง Auth Response พร้อม JWT tokens
     */
    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getJwtExpiration())
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId().toString())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .fullName(user.getFullName())
                        .role(user.getRole().getName())
                        .build())
                .build();
    }
}
