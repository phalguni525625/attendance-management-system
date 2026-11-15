package com.attendance.system.service;

import com.attendance.system.dto.AttendanceMarkDto;
import com.attendance.system.dto.AttendanceRecordDto;
import com.attendance.system.dto.AttendanceSessionDto;
import com.attendance.system.dto.StudentAttendanceSummary;
import com.attendance.system.model.AttendanceRecord;
import com.attendance.system.model.AttendanceSession;
import com.attendance.system.model.AttendanceStatus;
import com.attendance.system.model.Student;
import com.attendance.system.repository.AttendanceRecordRepository;
import com.attendance.system.repository.AttendanceSessionRepository;
import com.attendance.system.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;
    private final StudentRepository studentRepository;
    private final SettingsService settingsService;

    public AttendanceService(AttendanceSessionRepository sessionRepository,
                             AttendanceRecordRepository recordRepository,
                             StudentRepository studentRepository,
                             SettingsService settingsService) {
        this.sessionRepository = sessionRepository;
        this.recordRepository = recordRepository;
        this.studentRepository = studentRepository;
        this.settingsService = settingsService;
    }

    public List<AttendanceSessionDto> listSessions(String courseCode, String subjectCode,
                                                   String semester, String section, String date) {
        return sessionRepository.findByFilters(courseCode, subjectCode, semester, section, date)
            .stream().map(this::toDto).collect(Collectors.toList());
    }

    public AttendanceSessionDto createSession(AttendanceSessionDto dto, String createdBy) {
        LocalDate date = LocalDate.parse(dto.getSessionDate());
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Session date cannot be in the future");
        }
        Optional<AttendanceSession> existing = sessionRepository.findByCourseSubjectDateSection(
            dto.getCourseCode(), dto.getSubjectCode(), dto.getSemester(), dto.getSection(), date);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("An attendance session already exists for " +
                dto.getSubjectCode() + " on " + dto.getSessionDate() + " (" + dto.getSection() + ")");
        }

        List<Student> students = studentRepository.findAll().stream()
            .filter(s -> dto.getCourseCode().equals(s.getCourse())
                && dto.getSemester().equals(s.getSemester())
                && dto.getSection().equals(s.getSection()))
            .collect(Collectors.toList());

        AttendanceSession session = new AttendanceSession();
        session.setCourseCode(dto.getCourseCode());
        session.setSubjectCode(dto.getSubjectCode());
        session.setSemester(dto.getSemester());
        session.setSection(dto.getSection());
        session.setSessionDate(dto.getSessionDate());
        session.setCreatedBy(createdBy);
        Long sessionId = sessionRepository.save(session);

        if (students.isEmpty()) {
            return toDto(sessionRepository.findById(sessionId).orElseThrow());
        }

        List<AttendanceRecord> records = new ArrayList<>();
        for (Student s : students) {
            AttendanceRecord r = new AttendanceRecord();
            r.setSessionId(sessionId);
            r.setStudentId(s.getStudentId());
            r.setStudentName(s.getName());
            r.setStatus(AttendanceStatus.ABSENT);
            r.setUpdatedBy(createdBy);
            records.add(r);
        }
        for (AttendanceRecord r : records) {
            recordRepository.save(r);
        }
        return toDto(sessionRepository.findById(sessionId).orElseThrow());
    }

    public Optional<AttendanceSessionDto> getSession(Long id) {
        return sessionRepository.findById(id).map(this::toDto);
    }

    public List<AttendanceRecordDto> getRecords(Long sessionId) {
        if (!sessionRepository.findById(sessionId).isPresent()) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        return recordRepository.findBySession(sessionId).stream()
            .map(this::toRecordDto).collect(Collectors.toList());
    }

    @Transactional
    public List<AttendanceRecordDto> markAttendance(Long sessionId, List<AttendanceMarkDto> marks, String updatedBy) {
        if (!sessionRepository.findById(sessionId).isPresent()) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        if (marks == null || marks.isEmpty()) {
            throw new IllegalArgumentException("No attendance records provided");
        }
        List<AttendanceRecordDto> result = new ArrayList<>();
        for (AttendanceMarkDto m : marks) {
            if (m.getStudentId() == null || m.getStudentId().isBlank()) {
                throw new IllegalArgumentException("Student ID is required for every record");
            }
            if (m.getStatus() == null) {
                throw new IllegalArgumentException("Status is required for student " + m.getStudentId());
            }
            AttendanceStatus status = m.getStatus();
            Optional<AttendanceRecord> existing = recordRepository.findBySessionAndStudent(sessionId, m.getStudentId());
            if (existing.isPresent()) {
                recordRepository.update(sessionId, m.getStudentId(), status, updatedBy);
            } else {
                AttendanceRecord r = new AttendanceRecord();
                r.setSessionId(sessionId);
                r.setStudentId(m.getStudentId());
                r.setStudentName(m.getStudentName());
                r.setStatus(status);
                r.setUpdatedBy(updatedBy);
                recordRepository.save(r);
            }
            result.add(toRecordDto(new AttendanceRecord(null, sessionId, m.getStudentId(),
                m.getStudentName(), status, updatedBy, null)));
        }
        return result;
    }

    public void cancelSession(Long id) {
        int updated = sessionRepository.cancel(id);
        if (updated == 0) {
            throw new IllegalArgumentException("Session not found: " + id);
        }
    }

    public List<StudentAttendanceSummary> allStudentSummaries() {
        Integer threshold = settingsService.getAttendanceThreshold();
        List<Student> students = studentRepository.findAll();
        List<StudentAttendanceSummary> summaries = new ArrayList<>();
        for (Student s : students) {
            summaries.add(summaryForStudent(s.getStudentId(), threshold));
        }
        return summaries;
    }

    private StudentAttendanceSummary summaryForStudent(String studentId, Integer threshold) {
        List<AttendanceRecord> all = recordRepository.findAll();
        List<AttendanceRecord> mine = all.stream()
            .filter(r -> studentId.equals(r.getStudentId()))
            .collect(Collectors.toList());

        long total = mine.size();
        long present = mine.stream().filter(r -> r.getStatus() == AttendanceStatus.PRESENT).count();
        double percentage = total == 0 ? 0.0 : (present * 100.0) / total;
        int shortage = total == 0 ? 0 : Math.max(0, (int) Math.ceil(threshold * total / 100.0) - (int) present);
        boolean belowThreshold = total > 0 && percentage < threshold;

        StudentAttendanceSummary s = new StudentAttendanceSummary();
        s.setStudentId(studentId);
        s.setTotalSessions((int) total);
        s.setPresentSessions((int) present);
        s.setAbsentSessions((int) (total - present));
        s.setPercentage(percentage);
        s.setBelowThreshold(belowThreshold);
        s.setShortage(shortage);
        s.setThreshold(threshold);
        return s;
    }

    public List<StudentAttendanceSummary> defaulters() {
        Integer threshold = settingsService.getAttendanceThreshold();
        return allStudentSummaries().stream()
            .filter(s -> s.getTotalSessions() > 0 && s.isBelowThreshold())
            .collect(Collectors.toList());
    }

    private AttendanceSessionDto toDto(AttendanceSession s) {
        AttendanceSessionDto dto = new AttendanceSessionDto();
        dto.setId(s.getId());
        dto.setCourseCode(s.getCourseCode());
        dto.setSubjectCode(s.getSubjectCode());
        dto.setSemester(s.getSemester());
        dto.setSection(s.getSection());
        dto.setSessionDate(s.getSessionDate());
        dto.setCreatedBy(s.getCreatedBy());
        dto.setCreatedAt(s.getCreatedAt());
        dto.setStatus(s.getStatus());
        return dto;
    }

    private AttendanceRecordDto toRecordDto(AttendanceRecord r) {
        AttendanceRecordDto dto = new AttendanceRecordDto();
        dto.setId(r.getId());
        dto.setSessionId(r.getSessionId());
        dto.setStudentId(r.getStudentId());
        dto.setStudentName(r.getStudentName());
        dto.setStatus(r.getStatus());
        dto.setUpdatedBy(r.getUpdatedBy());
        dto.setUpdatedAt(r.getUpdatedAt());
        return dto;
    }
}
