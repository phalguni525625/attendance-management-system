package com.attendance.system;

import static org.assertj.core.api.Assertions.assertThat;

import com.attendance.system.repository.NotificationHistoryRepository;
import com.attendance.system.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;

@SpringBootTest(classes = AttendanceManagementApplication.class)
class NotificationServiceTest {

    @Autowired
    private NotificationHistoryRepository historyRepository;

    @Test
    @DisplayName("SMTP not configured returns skipped without sending")
    void smtpNotConfigured() {
        NotificationService service = new NotificationService(null, "", historyRepository);
        assertThat(service.isSmtpConfigured()).isFalse();
        NotificationService.NotificationResult result =
            service.sendDefaulterNotification("student@example.edu", "Subject", "Body");
        assertThat(result.sent()).isFalse();
        assertThat(result.message()).contains("SMTP is not configured");
        assertThat(result.recipient()).isNull();
    }

    @Test
    @DisplayName("SMTP configured and send succeeds returns sent")
    void smtpConfiguredSendSucceeds() {
        JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
        NotificationService service = new NotificationService(mailSender, "smtp.example.com", historyRepository);
        assertThat(service.isSmtpConfigured()).isTrue();
        NotificationService.NotificationResult result =
            service.sendDefaulterNotification("student@example.edu", "Subject", "Body");
        assertThat(result.sent()).isTrue();
        assertThat(result.recipient()).isEqualTo("student@example.edu");
    }
}
