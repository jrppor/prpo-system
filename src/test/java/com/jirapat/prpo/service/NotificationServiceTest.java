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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.jirapat.prpo.dto.response.NotificationResponse;
import com.jirapat.prpo.dto.response.UnreadCountResponse;
import com.jirapat.prpo.entity.Notification;
import com.jirapat.prpo.entity.NotificationType;
import com.jirapat.prpo.entity.Role;
import com.jirapat.prpo.entity.User;
import com.jirapat.prpo.exception.ResourceNotFoundException;
import com.jirapat.prpo.mapper.NotificationMapper;
import com.jirapat.prpo.repository.NotificationRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Unit Tests")
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationMapper notificationMapper;
    @Mock private SecurityService securityService;
    @Mock private EmailService emailService;

    @InjectMocks
    private NotificationService notificationService;

    private UUID userId;
    private User testUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = User.builder()
                .id(userId)
                .email("user@example.com")
                .firstName("Test")
                .lastName("User")
                .role(Role.builder().name("USER").build())
                .isActive(true)
                .build();
    }

    @Nested
    @DisplayName("getMyNotifications()")
    class GetMyNotificationsTests {

        @Test
        @DisplayName("should return notifications for current user")
        void getMyNotifications_ReturnsPage() {
            Notification notif = Notification.builder()
                    .id(UUID.randomUUID())
                    .recipient(testUser)
                    .type(NotificationType.PR_APPROVED)
                    .title("PR Approved")
                    .build();
            NotificationResponse resp = NotificationResponse.builder()
                    .id(notif.getId())
                    .title("PR Approved")
                    .build();

            when(securityService.getCurrentUserId()).thenReturn(userId);
            when(notificationRepository.findByRecipientId(userId, PageRequest.of(0, 10)))
                    .thenReturn(new PageImpl<>(List.of(notif)));
            when(notificationMapper.toResponse(notif)).thenReturn(resp);

            Page<NotificationResponse> result = notificationService.getMyNotifications(PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("PR Approved");
        }
    }

    @Nested
    @DisplayName("getUnreadCount()")
    class GetUnreadCountTests {

        @Test
        @DisplayName("should return unread count")
        void getUnreadCount_Returns() {
            when(securityService.getCurrentUserId()).thenReturn(userId);
            when(notificationRepository.countByRecipientIdAndIsReadFalse(userId)).thenReturn(5L);

            UnreadCountResponse result = notificationService.getUnreadCount();

            assertThat(result.getCount()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("markAsRead()")
    class MarkAsReadTests {

        @Test
        @DisplayName("should mark notification as read")
        void markAsRead_OwnNotification_Marks() {
            UUID notifId = UUID.randomUUID();
            Notification notif = Notification.builder()
                    .id(notifId)
                    .recipient(testUser)
                    .isRead(false)
                    .build();
            NotificationResponse resp = NotificationResponse.builder()
                    .id(notifId)
                    .isRead(true)
                    .build();

            when(securityService.getCurrentUserId()).thenReturn(userId);
            when(notificationRepository.findById(notifId)).thenReturn(Optional.of(notif));
            when(notificationRepository.save(notif)).thenReturn(notif);
            when(notificationMapper.toResponse(notif)).thenReturn(resp);

            NotificationResponse result = notificationService.markAsRead(notifId);

            assertThat(notif.getIsRead()).isTrue();
            assertThat(result.getIsRead()).isTrue();
        }

        @Test
        @DisplayName("should throw when notification not found")
        void markAsRead_NotFound_Throws() {
            UUID notifId = UUID.randomUUID();
            when(securityService.getCurrentUserId()).thenReturn(userId);
            when(notificationRepository.findById(notifId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.markAsRead(notifId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw when notification belongs to another user")
        void markAsRead_OtherUser_Throws() {
            UUID notifId = UUID.randomUUID();
            User otherUser = User.builder().id(UUID.randomUUID()).build();
            Notification notif = Notification.builder()
                    .id(notifId)
                    .recipient(otherUser)
                    .build();

            when(securityService.getCurrentUserId()).thenReturn(userId);
            when(notificationRepository.findById(notifId)).thenReturn(Optional.of(notif));

            assertThatThrownBy(() -> notificationService.markAsRead(notifId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("markAllAsRead()")
    class MarkAllAsReadTests {

        @Test
        @DisplayName("should mark all as read for current user")
        void markAllAsRead_Calls() {
            when(securityService.getCurrentUserId()).thenReturn(userId);
            when(notificationRepository.markAllAsReadByRecipientId(userId)).thenReturn(3);

            notificationService.markAllAsRead();

            verify(notificationRepository).markAllAsReadByRecipientId(userId);
        }
    }

    @Nested
    @DisplayName("send()")
    class SendTests {

        @Test
        @DisplayName("should save notification and send email")
        void send_SavesAndSendsEmail() {
            UUID refId = UUID.randomUUID();

            notificationService.send(
                    testUser,
                    NotificationType.PR_APPROVED,
                    "PR Approved",
                    "Your PR has been approved",
                    "PurchaseRequest",
                    refId
            );

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());

            Notification saved = captor.getValue();
            assertThat(saved.getRecipient()).isEqualTo(testUser);
            assertThat(saved.getType()).isEqualTo(NotificationType.PR_APPROVED);
            assertThat(saved.getTitle()).isEqualTo("PR Approved");
            assertThat(saved.getReferenceType()).isEqualTo("PurchaseRequest");
            assertThat(saved.getReferenceId()).isEqualTo(refId);

            verify(emailService).sendNotificationEmail(
                    "user@example.com", "PR Approved", "Your PR has been approved",
                    "PurchaseRequest", refId.toString());
        }
    }
}
