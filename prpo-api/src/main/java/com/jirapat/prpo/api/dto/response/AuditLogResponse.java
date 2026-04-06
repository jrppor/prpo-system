package com.jirapat.prpo.api.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.jirapat.prpo.api.entity.AuditAction;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuditLogResponse {
    private UUID id;
    private String entityType;
    private UUID entityId;
    private AuditAction action;
    private String performedBy;
    private String oldValue;
    private String newValue;
    private String ipAddress;
    private LocalDateTime createdAt;
}
