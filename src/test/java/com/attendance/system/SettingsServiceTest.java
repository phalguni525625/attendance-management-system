package com.attendance.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.attendance.system.service.SettingsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = AttendanceManagementApplication.class)
@Transactional
class SettingsServiceTest {

    @Autowired
    private SettingsService settingsService;

    @Test
    @DisplayName("default threshold is 75 when unset")
    void defaultThreshold() {
        assertThat(settingsService.getAttendanceThreshold()).isEqualTo(75);
    }

    @Test
    @DisplayName("set and get threshold")
    void setGetThreshold() {
        settingsService.setAttendanceThreshold(80);
        assertThat(settingsService.getAttendanceThreshold()).isEqualTo(80);
    }

    @Test
    @DisplayName("threshold out of bounds rejected")
    void thresholdBounds() {
        assertThatThrownBy(() -> settingsService.setAttendanceThreshold(-1))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> settingsService.setAttendanceThreshold(101))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> settingsService.setAttendanceThreshold(null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
