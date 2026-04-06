package com.jirapat.prpo.api.service;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.jirapat.prpo.api.entity.AuditAction;
import com.jirapat.prpo.api.entity.AuditLog;
import com.jirapat.prpo.api.entity.Role;
import com.jirapat.prpo.api.entity.User;
import com.jirapat.prpo.api.mapper.AuditLogMapper;
import com.jirapat.prpo.api.repository.AuditLogRepository;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLogService Unit Tests")
class AuditLogServiceTest {

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private AuditLogMapper auditLogMapper;
    @Mock private SecurityService securityService;
    @Mock private HttpServletRequest httpServletRequest;

    @InjectMocks
    private AuditLogService auditLogService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .id(UUID.randomUUID())
                .email("admin@example.com")
                .firstName("Admin")
                .role(Role.builder().name("ADMIN").build())
                .build();
    }

    @Nested
    @DisplayName("getAuditLogs()")
    class GetAuditLogsTests {

        @Test
        @DisplayName("should filter by entityType and entityId")
        void getAuditLogs_WithTypeAndId_Filters() {
            UUID entityId = UUID.randomUUID();
            PageRequest pageable = PageRequest.of(0, 10);
            when(auditLogRepository.findByEntityTypeAndEntityId("PurchaseRequest", entityId, pageable))
                    .thenReturn(Page.empty());

            auditLogService.getAuditLogs("PurchaseRequest", entityId, pageable);

            verify(auditLogRepository).findByEntityTypeAndEntityId("PurchaseRequest", entityId, pageable);
        }

        @Test
        @DisplayName("should filter by entityType only")
        void getAuditLogs_WithTypeOnly_Filters() {
            PageRequest pageable = PageRequest.of(0, 10);
            when(auditLogRepository.findByEntityType("PurchaseOrder", pageable))
                    .thenReturn(Page.empty());

            auditLogService.getAuditLogs("PurchaseOrder", null, pageable);

            verify(auditLogRepository).findByEntityType("PurchaseOrder", pageable);
        }

        @Test
        @DisplayName("should return all when no filters")
        void getAuditLogs_NoFilters_ReturnsAll() {
            PageRequest pageable = PageRequest.of(0, 10);
            when(auditLogRepository.findAllWithPerformedBy(pageable))
                    .thenReturn(Page.empty());

            auditLogService.getAuditLogs(null, null, pageable);

            verify(auditLogRepository).findAllWithPerformedBy(pageable);
        }
    }

    @Nested
    @DisplayName("log()")
    class LogTests {

        @Test
        @DisplayName("should create audit log with current user and IP")
        void log_CreatesAuditLog() {
            UUID entityId = UUID.randomUUID();
            when(securityService.getCurrentUser()).thenReturn(currentUser);
            when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
            when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");

            auditLogService.log("PurchaseRequest", entityId, AuditAction.CREATE, null, "PR-001");

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(captor.capture());

            AuditLog saved = captor.getValue();
            assertThat(saved.getEntityType()).isEqualTo("PurchaseRequest");
            assertThat(saved.getEntityId()).isEqualTo(entityId);
            assertThat(saved.getAction()).isEqualTo(AuditAction.CREATE);
            assertThat(saved.getPerformedBy()).isEqualTo(currentUser);
            assertThat(saved.getIpAddress()).isEqualTo("127.0.0.1");
            assertThat(saved.getNewValue()).isEqualTo("PR-001");
        }

        @Test
        @DisplayName("should use X-Forwarded-For IP when available")
        void log_UsesXForwardedForIp() {
            UUID entityId = UUID.randomUUID();
            when(securityService.getCurrentUser()).thenReturn(currentUser);
            when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 10.0.0.1");

            auditLogService.log("User", entityId, AuditAction.UPDATE, "old", "new");

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(captor.capture());
            assertThat(captor.getValue().getIpAddress()).isEqualTo("192.168.1.1");
        }

        @Test
        @DisplayName("should not throw on exception (swallows errors)")
        void log_OnException_DoesNotThrow() {
            UUID entityId = UUID.randomUUID();
            when(securityService.getCurrentUser()).thenThrow(new RuntimeException("auth error"));

            // Should not throw — the method has try-catch
            auditLogService.log("PurchaseRequest", entityId, AuditAction.DELETE, "old", null);
        }
    }

    @Nested
    @DisplayName("convenience methods")
    class ConvenienceMethodTests {

        @Test
        @DisplayName("logCreate should call log with CREATE action")
        void logCreate_CallsLog() {
            UUID entityId = UUID.randomUUID();
            when(securityService.getCurrentUser()).thenReturn(currentUser);
            when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
            when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");

            auditLogService.logCreate("Vendor", entityId, "VEN-001");

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(captor.capture());
            assertThat(captor.getValue().getAction()).isEqualTo(AuditAction.CREATE);
            assertThat(captor.getValue().getOldValue()).isNull();
            assertThat(captor.getValue().getNewValue()).isEqualTo("VEN-001");
        }

        @Test
        @DisplayName("logStatusChange should call log with STATUS_CHANGE action")
        void logStatusChange_CallsLog() {
            UUID entityId = UUID.randomUUID();
            when(securityService.getCurrentUser()).thenReturn(currentUser);
            when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
            when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");

            auditLogService.logStatusChange("PurchaseRequest", entityId, "DRAFT", "SUBMITTED");

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(captor.capture());
            assertThat(captor.getValue().getAction()).isEqualTo(AuditAction.STATUS_CHANGE);
            assertThat(captor.getValue().getOldValue()).isEqualTo("DRAFT");
            assertThat(captor.getValue().getNewValue()).isEqualTo("SUBMITTED");
        }
    }
}
