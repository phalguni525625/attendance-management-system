package com.attendance.system.service;

import com.attendance.system.repository.NotificationHistoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final JavaMailSender mailSender;
    private final String smtpHost;
    private final NotificationHistoryRepository historyRepository;

    public NotificationService(JavaMailSender mailSender,
                               @Value("${spring.mail.host:}") String smtpHost,
                               NotificationHistoryRepository historyRepository) {
        this.mailSender = mailSender;
        this.smtpHost = smtpHost;
        this.historyRepository = historyRepository;
    }

    public boolean isSmtpConfigured() {
        return mailSender != null && smtpHost != null && !smtpHost.isBlank();
    }

    @Transactional
    public NotificationResult sendDefaulterNotification(String recipient, String subject, String body) {
        if (!isSmtpConfigured()) {
            historyRepository.save("defaulter", recipient, subject, "skipped", "SMTP not configured");
            return new NotificationResult(false, "SMTP is not configured. Email was not sent.", null);
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(recipient);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            historyRepository.save("defaulter", recipient, subject, "sent", null);
            return new NotificationResult(true, "Defaulter notification sent to " + recipient, recipient);
        } catch (MailSendException | IllegalStateException ex) {
            String msg = ex.getMessage();
            historyRepository.save("defaulter", recipient, subject, "failed", msg);
            return new NotificationResult(false, "Failed to send email to " + recipient + ": " + sanitize(msg), null);
        }
    }

    private String sanitize(String message) {
        if (message == null) return "unknown error";
        return message.replaceAll("(?i)(password|passwd|pwd|secret|token|api[_-]?key)\\s*[:=]\\S*", "$1=***");
    }

    public record NotificationResult(boolean sent, String message, String recipient) {}
}
