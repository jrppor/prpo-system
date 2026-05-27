package com.jirapat.prpo.api.service;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.jirapat.prpo.api.entity.Role;
import com.jirapat.prpo.api.entity.User;
import com.jirapat.prpo.api.exception.UnauthorizedException;


@DisplayName("SecurityService — verifyOwnershipOrAdmin")
class SecurityServiceTest {

    private final SecurityService securityService = new SecurityService();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private static User userWithRole(String roleName) {
        return User.builder()
                .id(UUID.randomUUID())
                .email(roleName.toLowerCase() + "@example.com")
                .passwordHash("hash")
                .role(Role.builder().id(UUID.randomUUID()).name(roleName).build())
                .isActive(true)
                .build();
    }

    private void authenticateAs(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @Test
    @DisplayName("เจ้าของ resource ผ่านได้")
    void owner_passes() {
        User owner = userWithRole("USER");
        authenticateAs(owner);

        assertThatCode(() -> securityService.verifyOwnershipOrAdmin(owner.getId()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("ADMIN ผ่านได้แม้ไม่ใช่เจ้าของ")
    void admin_passesEvenIfNotOwner() {
        User admin = userWithRole("ADMIN");
        authenticateAs(admin);

        assertThatCode(() -> securityService.verifyOwnershipOrAdmin(UUID.randomUUID()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("ผู้ที่ไม่ใช่เจ้าของและไม่ใช่ ADMIN ถูกปฏิเสธ")
    void nonOwnerNonAdmin_throws() {
        User other = userWithRole("USER");
        authenticateAs(other);

        assertThatThrownBy(() -> securityService.verifyOwnershipOrAdmin(UUID.randomUUID()))
                .isInstanceOf(UnauthorizedException.class);
    }
}
