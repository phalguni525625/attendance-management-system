package com.attendance.system.service;

import com.attendance.system.model.Setting;
import com.attendance.system.repository.SettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SettingsService {

    private static final String THRESHOLD_KEY = "attendance.threshold";

    private final SettingRepository settingRepository;

    public SettingsService(SettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    public List<Setting> findAll() {
        return settingRepository.findAll();
    }

    public Integer getAttendanceThreshold() {
        Optional<Setting> s = settingRepository.findByKey(THRESHOLD_KEY);
        if (s.isPresent()) {
            try {
                return Integer.parseInt(s.get().getValue());
            } catch (NumberFormatException ignored) {
            }
        }
        return 75;
    }

    @Transactional
    public void setAttendanceThreshold(Integer threshold) {
        if (threshold == null || threshold < 0 || threshold > 100) {
            throw new IllegalArgumentException("Threshold must be between 0 and 100");
        }
        settingRepository.upsert(THRESHOLD_KEY, String.valueOf(threshold));
    }
}
