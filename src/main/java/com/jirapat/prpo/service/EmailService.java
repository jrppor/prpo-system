package com.jirapat.prpo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:noreply@prpo-system.com}")
    private String fromAddress;

    @Value("${app.mail.subject-prefix:[PRPO] }")
    private String subjectPrefix;

    @Async
    public void sendEmail(String to, String subject, String body) {
        if (!mailEnabled) {
            log.debug("Email disabled — skipping send to {}: {}", to, subject);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subjectPrefix + subject);
            helper.setText(body, true);

            mailSender.send(message);
            log.info("Email sent to {}: {}", to, subject);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendNotificationEmail(String to, String title, String message, String referenceType, String referenceId) {
        String body = buildNotificationHtml(title, message, referenceType, referenceId);
        sendEmail(to, title, body);
    }

    private String buildNotificationHtml(String title, String message, String referenceType, String referenceId) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <div style="background-color: #1a56db; color: white; padding: 20px; border-radius: 8px 8px 0 0;">
                        <h2 style="margin: 0;">PRPO System</h2>
                    </div>
                    <div style="padding: 20px; border: 1px solid #e5e7eb; border-top: none; border-radius: 0 0 8px 8px;">
                        <h3 style="color: #1f2937;">%s</h3>
                        <p style="color: #4b5563; line-height: 1.6;">%s</p>
                        %s
                        <hr style="border: none; border-top: 1px solid #e5e7eb; margin: 20px 0;">
                        <p style="color: #9ca3af; font-size: 12px;">This is an automated notification from PRPO System. Please do not reply to this email.</p>
                    </div>
                </div>
                """.formatted(
                title,
                message,
                referenceType != null
                        ? "<p style=\"color: #6b7280; font-size: 13px;\">Reference: %s #%s</p>".formatted(referenceType, referenceId)
                        : ""
        );
    }
}
