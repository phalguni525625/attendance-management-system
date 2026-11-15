package com.attendance.system.service;

import com.attendance.system.dto.AttendanceSessionDto;
import com.attendance.system.dto.ReportRow;
import com.attendance.system.repository.AttendanceRecordRepository;
import com.attendance.system.repository.AttendanceSessionRepository;
import com.attendance.system.model.AttendanceStatus;
import com.attendance.system.model.Student;
import com.attendance.system.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;
    private final StudentRepository studentRepository;

    public ReportService(AttendanceSessionRepository sessionRepository,
                         AttendanceRecordRepository recordRepository,
                         StudentRepository studentRepository) {
        this.sessionRepository = sessionRepository;
        this.recordRepository = recordRepository;
        this.studentRepository = studentRepository;
    }

    public List<ReportRow> studentWise(String studentId, String courseCode, String subjectCode,
                                       String startDate, String endDate) {
        List<com.attendance.system.model.AttendanceRecord> records = recordRepository.findAll();
        List<com.attendance.system.model.AttendanceSession> sessions = sessionRepository.findAll();
        Map<Long, com.attendance.system.model.AttendanceSession> sessionMap = sessions.stream()
            .collect(Collectors.toMap(com.attendance.system.model.AttendanceSession::getId, s -> s));

        List<ReportRow> rows = new ArrayList<>();
        for (com.attendance.system.model.AttendanceRecord r : records) {
            if (studentId != null && !studentId.equals(r.getStudentId())) continue;
            com.attendance.system.model.AttendanceSession s = sessionMap.get(r.getSessionId());
            if (s == null) continue;
            if (courseCode != null && !courseCode.equals(s.getCourseCode())) continue;
            if (subjectCode != null && !subjectCode.equals(s.getSubjectCode())) continue;
            if (startDate != null && s.getSessionDate().compareTo(startDate) < 0) continue;
            if (endDate != null && s.getSessionDate().compareTo(endDate) > 0) continue;
            rows.add(new ReportRow(s.getSessionDate(), s.getCourseCode(), s.getSubjectCode(),
                s.getSection(), r.getStudentId(), r.getStudentName(), r.getStatus().name()));
        }
        return rows;
    }

    public List<ReportRow> subjectWise(String subjectCode, String courseCode, String semester,
                                       String section, String startDate, String endDate) {
        List<com.attendance.system.model.AttendanceRecord> records = recordRepository.findAll();
        List<com.attendance.system.model.AttendanceSession> sessions = sessionRepository.findAll();
        Map<Long, com.attendance.system.model.AttendanceSession> sessionMap = sessions.stream()
            .collect(Collectors.toMap(com.attendance.system.model.AttendanceSession::getId, s -> s));

        List<ReportRow> rows = new ArrayList<>();
        for (com.attendance.system.model.AttendanceRecord r : records) {
            com.attendance.system.model.AttendanceSession s = sessionMap.get(r.getSessionId());
            if (s == null) continue;
            if (subjectCode != null && !subjectCode.equals(s.getSubjectCode())) continue;
            if (courseCode != null && !courseCode.equals(s.getCourseCode())) continue;
            if (semester != null && !semester.equals(s.getSemester())) continue;
            if (section != null && !section.equals(s.getSection())) continue;
            if (startDate != null && s.getSessionDate().compareTo(startDate) < 0) continue;
            if (endDate != null && s.getSessionDate().compareTo(endDate) > 0) continue;
            rows.add(new ReportRow(s.getSessionDate(), s.getCourseCode(), s.getSubjectCode(),
                s.getSection(), r.getStudentId(), r.getStudentName(), r.getStatus().name()));
        }
        return rows;
    }

    public List<AttendanceSessionDto> sessionsByDateRange(String startDate, String endDate) {
        return sessionRepository.findAll().stream()
            .filter(s -> (startDate == null || s.getSessionDate().compareTo(startDate) >= 0)
                && (endDate == null || s.getSessionDate().compareTo(endDate) <= 0))
            .map(s -> {
                AttendanceSessionDto d = new AttendanceSessionDto();
                d.setId(s.getId());
                d.setCourseCode(s.getCourseCode());
                d.setSubjectCode(s.getSubjectCode());
                d.setSemester(s.getSemester());
                d.setSection(s.getSection());
                d.setSessionDate(s.getSessionDate());
                d.setCreatedBy(s.getCreatedBy());
                d.setCreatedAt(s.getCreatedAt());
                d.setStatus(s.getStatus());
                return d;
            }).collect(Collectors.toList());
    }

    public List<Student> allStudents() {
        return studentRepository.findAll();
    }

    public enum ReportType { STUDENT_WISE, SUBJECT_WISE, DATE_RANGE, DEFAULTER }
}
