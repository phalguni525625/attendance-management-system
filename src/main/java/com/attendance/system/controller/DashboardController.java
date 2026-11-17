package com.attendance.system.controller;

import com.attendance.system.dto.ApiResponse;
import com.attendance.system.dto.AttendanceSessionDto;
import com.attendance.system.dto.StudentAttendanceSummary;
import com.attendance.system.service.AttendanceService;
import com.attendance.system.service.CourseService;
import com.attendance.system.service.SettingsService;
import com.attendance.system.service.SubjectService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final AttendanceService attendanceService;
    private final SettingsService settingsService;
    private final CourseService courseService;
    private final SubjectService subjectService;

    public DashboardController(AttendanceService attendanceService, SettingsService settingsService,
                               CourseService courseService, SubjectService subjectService) {
        this.attendanceService = attendanceService;
        this.settingsService = settingsService;
        this.courseService = courseService;
        this.subjectService = subjectService;
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ApiResponse<Map<String, Object>> stats() {
        int threshold = settingsService.getAttendanceThreshold();
        List<StudentAttendanceSummary> summaries = attendanceService.allStudentSummaries();

        long totalStudents = summaries.size();
        long totalRecords = attendanceService.countActiveRecords();
        long totalPresent = attendanceService.countPresentRecords();
        double overallPercentage = totalRecords == 0 ? 0.0 : (totalPresent * 100.0) / totalRecords;
        long belowThreshold = summaries.stream()
            .filter(s -> s.getTotalSessions() > 0 && s.isBelowThreshold()).count();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalStudents", totalStudents);
        data.put("totalCourses", courseService.findAll().size());
        data.put("totalSubjects", subjectService.findAll().size());
        data.put("totalSessions", attendanceService.countActiveSessions());
        data.put("totalRecords", totalRecords);
        data.put("presentRecords", totalPresent);
        data.put("overallPercentage", Math.round(overallPercentage * 100.0) / 100.0);
        data.put("belowThreshold", belowThreshold);
        data.put("threshold", threshold);
        return ApiResponse.ok(data);
    }

    @GetMapping("/recent-activity")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ApiResponse<List<AttendanceSessionDto>> recentActivity() {
        List<AttendanceSessionDto> sessions = attendanceService.listSessions(null, null, null, null, null);
        return ApiResponse.ok(sessions.subList(0, Math.min(5, sessions.size())));
    }
}