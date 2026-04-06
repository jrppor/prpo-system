package com.jirapat.prpo.api.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jirapat.prpo.api.dto.response.NotificationResponse;
import com.jirapat.prpo.api.dto.response.UnreadCountResponse;
import com.jirapat.prpo.api.entity.Notification;
import com.jirapat.prpo.api.entity.NotificationType;
import com.jirapat.prpo.api.entity.User;
import com.jirapat.prpo.api.exception.ResourceNotFoundException;
import com.jirapat.prpo.api.mapper.NotificationMapper;
import com.jirapat.prpo.api.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final SecurityService securityService;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(Pageable pageable) {
        UUID userId = securityService.getCurrentUserId();
        return notificationRepository.findByRecipientId(userId, pageable)
                .map(notificationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount() {
        UUID userId = securityService.getCurrentUserId();
        long count = notificationRepository.countByRecipientIdAndIsReadFalse(userId);
        return UnreadCountResponse.builder().count(count).build();
    }

    public NotificationResponse markAsRead(UUID notificationId) {
        UUID userId = securityService.getCurrentUserId();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId.toString()));

        if (!notification.getRecipient().getId().equals(userId)) {
            throw new ResourceNotFoundException("Notification", "id", notificationId.toString());
        }

        notification.setIsRead(true);
        Notification saved = notificationRepository.save(notification);
        return notificationMapper.toResponse(saved);
    }

    public void markAllAsRead() {
        UUID userId = securityService.getCurrentUserId();
        int updated = notificationRepository.markAllAsReadByRecipientId(userId);
        log.info("Marked {} notifications as read for user: {}", updated, userId);
    }

    public void send(User recipient, NotificationType type, String title, String message,
                     String referenceType, UUID referenceId) {
        Notification notification = Notification.builder()
                .recipient(recipient)
                .type(type)
                .title(title)
                .message(message)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .build();

        notificationRepository.save(notification);
        log.debug("Notification sent to {}: {} - {}", recipient.getEmail(), type, title);

        // Send email notification asynchronously
        emailService.sendNotificationEmail(
                recipient.getEmail(),
                title,
                message,
                referenceType,
                referenceId != null ? referenceId.toString() : null
        );
    }
}
