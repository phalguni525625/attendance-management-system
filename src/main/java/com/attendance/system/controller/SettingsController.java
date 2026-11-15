package com.attendance.system.controller;

import com.attendance.system.dto.ApiResponse;
import com.attendance.system.service.SettingsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping("/attendance-threshold")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ApiResponse<Integer> getThreshold() {
        return ApiResponse.ok(settingsService.getAttendanceThreshold());
    }

    @PostMapping("/attendance-threshold")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> setThreshold(@RequestBody Map<String, Object> body) {
        Object raw = body.get("threshold");
        Integer threshold;
        if (raw == null) {
            return ApiResponse.error("threshold is required");
        }
        try {
            threshold = Integer.parseInt(String.valueOf(raw));
        } catch (NumberFormatException e) {
            return ApiResponse.error("threshold must be a number between 0 and 100");
        }
        settingsService.setAttendanceThreshold(threshold);
        return ApiResponse.ok("Attendance threshold updated", null);
    }
}
