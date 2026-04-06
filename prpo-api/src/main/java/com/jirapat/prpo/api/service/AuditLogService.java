package com.jirapat.prpo.api.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jirapat.prpo.api.dto.response.AuditLogResponse;
import com.jirapat.prpo.api.entity.AuditAction;
import com.jirapat.prpo.api.entity.AuditLog;
import com.jirapat.prpo.api.entity.User;
import com.jirapat.prpo.api.mapper.AuditLogMapper;
import com.jirapat.prpo.api.repository.AuditLogRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;
    private final SecurityService securityService;
    private final HttpServletRequest httpServletRequest;

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(String entityType, UUID entityId, Pageable pageable) {
        Page<AuditLog> logs;

        if (entityType != null && entityId != null) {
            logs = auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable);
        } else if (entityType != null) {
            logs = auditLogRepository.findByEntityType(entityType, pageable);
        } else {
            logs = auditLogRepository.findAllWithPerformedBy(pageable);
        }

        return logs.map(auditLogMapper::toResponse);
    }

    public void log(String entityType, UUID entityId, AuditAction action, String oldValue, String newValue) {
        try {
            User currentUser = securityService.getCurrentUser();
            String ipAddress = getClientIpAddress();

            AuditLog auditLog = AuditLog.builder()
                    .entityType(entityType)
                    .entityId(entityId)
                    .action(action)
                    .performedBy(currentUser)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .ipAddress(ipAddress)
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log created: {} {} on {}:{}", action, currentUser.getEmail(), entityType, entityId);
        } catch (Exception e) {
            log.error("Failed to create audit log for {}:{} - {}", entityType, entityId, e.getMessage());
        }
    }

    public void logCreate(String entityType, UUID entityId, String newValue) {
        log(entityType, entityId, AuditAction.CREATE, null, newValue);
    }

    public void logUpdate(String entityType, UUID entityId, String oldValue, String newValue) {
        log(entityType, entityId, AuditAction.UPDATE, oldValue, newValue);
    }

    public void logDelete(String entityType, UUID entityId, String oldValue) {
        log(entityType, entityId, AuditAction.DELETE, oldValue, null);
    }

    public void logStatusChange(String entityType, UUID entityId, String oldStatus, String newStatus) {
        log(entityType, entityId, AuditAction.STATUS_CHANGE, oldStatus, newStatus);
    }

    private String getClientIpAddress() {
        String xForwardedFor = httpServletRequest.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return httpServletRequest.getRemoteAddr();
    }
}
