package com.attendance.system.controller;

import com.attendance.system.dto.ApiResponse;
import com.attendance.system.dto.AttendanceMarkDto;
import com.attendance.system.dto.AttendanceRecordDto;
import com.attendance.system.dto.AttendanceSessionDto;
import com.attendance.system.security.CustomUserDetails;
import com.attendance.system.service.AttendanceService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @GetMapping("/sessions")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ApiResponse<List<AttendanceSessionDto>> listSessions(
        @RequestParam(required = false) String courseCode,
        @RequestParam(required = false) String subjectCode,
        @RequestParam(required = false) String semester,
        @RequestParam(required = false) String section,
        @RequestParam(required = false) String date) {
        return ApiResponse.ok(attendanceService.listSessions(courseCode, subjectCode, semester, section, date));
    }

    @PostMapping("/sessions")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ApiResponse<AttendanceSessionDto> createSession(@Valid @RequestBody AttendanceSessionDto dto,
                                                           @AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponse.ok("Attendance session created", attendanceService.createSession(dto, user.getUsername()));
    }

    @GetMapping("/sessions/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ApiResponse<AttendanceSessionDto> getSession(@PathVariable Long id) {
        return attendanceService.getSession(id)
            .map(ApiResponse::ok)
            .orElseGet(() -> ApiResponse.<AttendanceSessionDto>error("Session not found"));
    }

    @GetMapping("/sessions/{id}/records")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ApiResponse<List<AttendanceRecordDto>> getRecords(@PathVariable Long id) {
        return ApiResponse.ok(attendanceService.getRecords(id));
    }

    @PostMapping("/sessions/{id}/marks")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ApiResponse<List<AttendanceRecordDto>> markAttendance(@PathVariable Long id,
                                                                 @RequestBody List<AttendanceMarkDto> marks,
                                                                 @AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponse.ok("Attendance marked", attendanceService.markAttendance(id, marks, user.getUsername()));
    }

    @PostMapping("/sessions/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ApiResponse<Void> cancelSession(@PathVariable Long id) {
        attendanceService.cancelSession(id);
        return ApiResponse.ok("Session cancelled", null);
    }
}
