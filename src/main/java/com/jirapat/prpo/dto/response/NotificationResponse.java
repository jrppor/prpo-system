package com.jirapat.prpo.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.jirapat.prpo.entity.NotificationType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationResponse {
    private UUID id;
    private NotificationType type;
    private String title;
    private String message;
    private String referenceType;
    private UUID referenceId;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
