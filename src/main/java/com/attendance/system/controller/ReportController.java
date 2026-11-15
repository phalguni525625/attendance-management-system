package com.attendance.system.controller;

import com.attendance.system.dto.ApiResponse;
import com.attendance.system.dto.AttendanceSessionDto;
import com.attendance.system.dto.ReportRow;
import com.attendance.system.dto.StudentAttendanceSummary;
import com.attendance.system.model.Student;
import com.attendance.system.service.AttendanceService;
import com.attendance.system.service.ReportService;
import com.attendance.system.service.SettingsService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.attendance.system.security.CustomUserDetails;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final AttendanceService attendanceService;
    private final SettingsService settingsService;

    public ReportController(ReportService reportService, AttendanceService attendanceService,
                            SettingsService settingsService) {
        this.reportService = reportService;
        this.attendanceService = attendanceService;
        this.settingsService = settingsService;
    }

    @GetMapping("/student-wise")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ApiResponse<List<ReportRow>> studentWise(
        @RequestParam(required = false) String studentId,
        @RequestParam(required = false) String courseCode,
        @RequestParam(required = false) String subjectCode,
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate) {
        if (studentId == null || studentId.isBlank()) {
            return ApiResponse.error("studentId is required for student-wise report");
        }
        return ApiResponse.ok(reportService.studentWise(studentId, courseCode, subjectCode, startDate, endDate));
    }

    @GetMapping("/subject-wise")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ApiResponse<List<ReportRow>> subjectWise(
        @RequestParam(required = false) String subjectCode,
        @RequestParam(required = false) String courseCode,
        @RequestParam(required = false) String semester,
        @RequestParam(required = false) String section,
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate) {
        return ApiResponse.ok(reportService.subjectWise(subjectCode, courseCode, semester, section, startDate, endDate));
    }

    @GetMapping("/sessions")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ApiResponse<List<AttendanceSessionDto>> sessionsByDateRange(
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate) {
        return ApiResponse.ok(reportService.sessionsByDateRange(startDate, endDate));
    }

    @GetMapping("/defaulters")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ApiResponse<List<StudentAttendanceSummary>> defaulters() {
        return ApiResponse.ok(attendanceService.defaulters());
    }

    @GetMapping("/export-csv")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<String> exportCsv(
        @RequestParam(required = false) String type,
        @RequestParam(required = false) String studentId,
        @RequestParam(required = false) String subjectCode,
        @RequestParam(required = false) String courseCode,
        @RequestParam(required = false) String semester,
        @RequestParam(required = false) String section,
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate) {
        List<ReportRow> rows;
        if ("subject".equalsIgnoreCase(type)) {
            rows = reportService.subjectWise(subjectCode, courseCode, semester, section, startDate, endDate);
        } else {
            if (studentId == null || studentId.isBlank()) {
                return ResponseEntity.badRequest()
                    .body("studentId is required for student-wise CSV export");
            }
            rows = reportService.studentWise(studentId, courseCode, subjectCode, startDate, endDate);
        }
        StringBuilder csv = new StringBuilder();
        csv.append("\uFEFF"); // UTF-8 BOM so Excel opens it correctly
        csv.append("Date,Course Code,Subject Code,Section,Student ID,Student Name,Status\n");
        for (ReportRow r : rows) {
            csv.append(escapeCsv(r.getDate())).append(',');
            csv.append(escapeCsv(r.getCourseCode())).append(',');
            csv.append(escapeCsv(r.getSubjectCode())).append(',');
            csv.append(escapeCsv(r.getSection())).append(',');
            csv.append(escapeCsv(r.getStudentId())).append(',');
            csv.append(escapeCsv(r.getStudentName())).append(',');
            csv.append(escapeCsv(r.getStatus())).append('\n');
        }
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"attendance-report.csv\"");
        headers.add(HttpHeaders.CACHE_CONTROL, "no-cache");
        return ResponseEntity.ok()
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .headers(headers)
            .body(csv.toString());
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        String v = value.replace("\"", "\"\"");
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v + "\"";
        }
        return v;
    }
}
