package com.attendance.system.service;

import com.attendance.system.dto.AttendanceMarkDto;
import com.attendance.system.dto.AttendanceRecordDto;
import com.attendance.system.dto.AttendanceSessionDto;
import com.attendance.system.dto.StudentAttendanceSummary;
import com.attendance.system.model.AttendanceRecord;
import com.attendance.system.model.AttendanceSession;
import com.attendance.system.model.AttendanceStatus;
import com.attendance.system.model.Course;
import com.attendance.system.model.Student;
import com.attendance.system.model.Subject;
import com.attendance.system.repository.AttendanceRecordRepository;
import com.attendance.system.repository.AttendanceSessionRepository;
import com.attendance.system.repository.CourseRepository;
import com.attendance.system.repository.StudentRepository;
import com.attendance.system.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
    private final CourseRepository courseRepository;
    private final SubjectRepository subjectRepository;
    private final SettingsService settingsService;

    public AttendanceService(AttendanceSessionRepository sessionRepository,
                             AttendanceRecordRepository recordRepository,
                             StudentRepository studentRepository,
                             CourseRepository courseRepository,
                             SubjectRepository subjectRepository,
                             SettingsService settingsService) {
        this.sessionRepository = sessionRepository;
        this.recordRepository = recordRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.subjectRepository = subjectRepository;
        this.settingsService = settingsService;
    }

    public List<AttendanceSessionDto> listSessions(String courseCode, String subjectCode,
                                                   String semester, String section, String date) {
        return sessionRepository.findByFilters(courseCode, subjectCode, semester, section, date)
            .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public AttendanceSessionDto createSession(AttendanceSessionDto dto, String createdBy) {
        if (dto == null) {
            throw new IllegalArgumentException("Attendance session data is required");
        }
        if (dto.getSessionDate() == null || dto.getSessionDate().isBlank()) {
            throw new IllegalArgumentException("Session date is required");
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dto.getSessionDate());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid session date. Expected yyyy-MM-dd.");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Session date cannot be in the future");
        }

        Optional<Course> course = courseRepository.findByCode(dto.getCourseCode());
        if (course.isEmpty()) {
            throw new IllegalArgumentException("Course not found: " + dto.getCourseCode());
        }
        Optional<Subject> subject = subjectRepository.findByCode(dto.getSubjectCode());
        if (subject.isEmpty()) {
            throw new IllegalArgumentException("Subject not found: " + dto.getSubjectCode());
        }
        if (!dto.getCourseCode().equals(subject.get().getCourseCode())) {
            throw new IllegalArgumentException("Subject " + dto.getSubjectCode()
                + " does not belong to course " + dto.getCourseCode());
        }
        if (!dto.getSemester().equals(subject.get().getSemester())) {
            throw new IllegalArgumentException("Subject " + dto.getSubjectCode()
                + " belongs to semester " + subject.get().getSemester()
                + ", not " + dto.getSemester());
        }

        Optional<AttendanceSession> existing = sessionRepository.findByCourseSubjectDateSection(
            dto.getCourseCode(), dto.getSubjectCode(), dto.getSemester(), dto.getSection(), date);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("An attendance session already exists for " +
                dto.getSubjectCode() + " on " + dto.getSessionDate() + " (" + dto.getSection() + ")");
        }

        List<Student> students = studentRepository.findByCourseSemesterSection(
            dto.getCourseCode(), dto.getSemester(), dto.getSection());

        AttendanceSession session = new AttendanceSession();
        session.setCourseCode(dto.getCourseCode());
        session.setSubjectCode(dto.getSubjectCode());
        session.setSemester(dto.getSemester());
        session.setSection(dto.getSection());
        session.setSessionDate(dto.getSessionDate());
        session.setCreatedBy(createdBy);
        Long sessionId = sessionRepository.save(session);
        if (sessionId == null) {
            throw new IllegalStateException("Could not create attendance session");
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
        AttendanceSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
        if (marks == null || marks.isEmpty()) {
            throw new IllegalArgumentException("No attendance records provided");
        }

        List<Student> roster = studentRepository.findByCourseSemesterSection(
            session.getCourseCode(), session.getSemester(), session.getSection());
        Map<String, Student> rosterMap = new HashMap<>();
        for (Student s : roster) {
            rosterMap.put(s.getStudentId(), s);
        }

        List<AttendanceRecordDto> result = new ArrayList<>();
        for (AttendanceMarkDto m : marks) {
            if (m.getStudentId() == null || m.getStudentId().isBlank()) {
                throw new IllegalArgumentException("Student ID is required for every record");
            }
            if (m.getStatus() == null) {
                throw new IllegalArgumentException("Status is required for student " + m.getStudentId());
            }
            Student rosterStudent = rosterMap.get(m.getStudentId());
            if (rosterStudent == null) {
                throw new IllegalArgumentException("Student " + m.getStudentId()
                    + " is not enrolled in this course/semester/section");
            }

            AttendanceStatus status = m.getStatus();
            Optional<AttendanceRecord> existing = recordRepository.findBySessionAndStudent(sessionId, m.getStudentId());
            String studentName = rosterStudent.getName();
            if (existing.isPresent()) {
                recordRepository.update(sessionId, m.getStudentId(), status, updatedBy);
                AttendanceRecord updated = existing.get();
                updated.setStatus(status);
                updated.setUpdatedBy(updatedBy);
                result.add(toRecordDto(updated));
            } else {
                AttendanceRecord r = new AttendanceRecord();
                r.setSessionId(sessionId);
                r.setStudentId(m.getStudentId());
                r.setStudentName(studentName);
                r.setStatus(status);
                r.setUpdatedBy(updatedBy);
                Long id = recordRepository.save(r);
                r.setId(id);
                result.add(toRecordDto(r));
            }
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
        int threshold = settingsService.getAttendanceThreshold();
        List<Student> students = studentRepository.findAll();
        Map<String, List<AttendanceRecord>> byStudent = recordRepository.findAllActive().stream()
            .collect(Collectors.groupingBy(AttendanceRecord::getStudentId));

        List<StudentAttendanceSummary> summaries = new ArrayList<>();
        for (Student s : students) {
            List<AttendanceRecord> mine = byStudent.getOrDefault(s.getStudentId(), List.of());
            summaries.add(summaryForStudent(s.getStudentId(), mine, threshold));
        }
        return summaries;
    }

    private StudentAttendanceSummary summaryForStudent(String studentId, List<AttendanceRecord> records, int threshold) {
        long total = records.size();
        long present = records.stream().filter(r -> r.getStatus() == AttendanceStatus.PRESENT).count();
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
        return allStudentSummaries().stream()
            .filter(s -> s.getTotalSessions() > 0 && s.isBelowThreshold())
            .collect(Collectors.toList());
    }

    public long countActiveSessions() {
        return sessionRepository.countActive();
    }

    public long countActiveRecords() {
        return recordRepository.findAllActive().size();
    }

    public long countPresentRecords() {
        return recordRepository.findAllActive().stream()
            .filter(r -> r.getStatus() == AttendanceStatus.PRESENT).count();
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