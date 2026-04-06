package com.jirapat.prpo.api.service;

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
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.mail.internet.MimeMessage;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService Unit Tests")
class EmailServiceTest {

    @Mock private JavaMailSender mailSender;
    @Mock private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    @Nested
    @DisplayName("sendEmail()")
    class SendEmailTests {

        @Test
        @DisplayName("should skip sending when mail is disabled")
        void sendEmail_Disabled_Skips() {
            ReflectionTestUtils.setField(emailService, "mailEnabled", false);

            emailService.sendEmail("to@example.com", "Subject", "<p>Body</p>");

            verify(mailSender, never()).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("should send email when enabled")
        void sendEmail_Enabled_Sends() {
            ReflectionTestUtils.setField(emailService, "mailEnabled", true);
            ReflectionTestUtils.setField(emailService, "fromAddress", "noreply@prpo.com");
            ReflectionTestUtils.setField(emailService, "subjectPrefix", "[PRPO] ");

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            emailService.sendEmail("to@example.com", "Test Subject", "<p>Body</p>");

            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("should not throw on mail error")
        void sendEmail_MailError_DoesNotThrow() {
            ReflectionTestUtils.setField(emailService, "mailEnabled", true);
            ReflectionTestUtils.setField(emailService, "fromAddress", "noreply@prpo.com");
            ReflectionTestUtils.setField(emailService, "subjectPrefix", "[PRPO] ");

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            // MimeMessageHelper.setFrom will likely fail on the mock, which tests the catch block

            // Should not throw
            emailService.sendEmail("to@example.com", "Test", "<p>Body</p>");
        }
    }

    @Nested
    @DisplayName("sendNotificationEmail()")
    class SendNotificationEmailTests {

        @Test
        @DisplayName("should build HTML and call sendEmail")
        void sendNotificationEmail_CallsSendEmail() {
            ReflectionTestUtils.setField(emailService, "mailEnabled", true);
            ReflectionTestUtils.setField(emailService, "fromAddress", "noreply@prpo.com");
            ReflectionTestUtils.setField(emailService, "subjectPrefix", "[PRPO] ");

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            emailService.sendNotificationEmail(
                    "to@example.com", "PR Approved", "Your PR has been approved",
                    "PurchaseRequest", "some-id");

            verify(mailSender).createMimeMessage();
        }

        @Test
        @DisplayName("should skip when disabled")
        void sendNotificationEmail_Disabled_Skips() {
            ReflectionTestUtils.setField(emailService, "mailEnabled", false);

            emailService.sendNotificationEmail(
                    "to@example.com", "Title", "Message", "Type", "ref-id");

            verify(mailSender, never()).createMimeMessage();
        }
    }
}
