package com.attendance.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.attendance.system.dto.AttendanceMarkDto;
import com.attendance.system.dto.AttendanceSessionDto;
import com.attendance.system.dto.StudentAttendanceSummary;
import com.attendance.system.model.AttendanceRecord;
import com.attendance.system.model.AttendanceSession;
import com.attendance.system.model.AttendanceStatus;
import com.attendance.system.model.Student;
import com.attendance.system.model.Subject;
import com.attendance.system.repository.AttendanceRecordRepository;
import com.attendance.system.repository.AttendanceSessionRepository;
import com.attendance.system.repository.CourseRepository;
import com.attendance.system.repository.StudentRepository;
import com.attendance.system.repository.SubjectRepository;
import com.attendance.system.service.AttendanceService;
import com.attendance.system.service.SettingsService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = AttendanceManagementApplication.class)
@Transactional
class AttendanceServiceTest {

    @Autowired
    private AttendanceService attendanceService;
    @Autowired
    private AttendanceSessionRepository sessionRepository;
    @Autowired
    private AttendanceRecordRepository recordRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private SubjectRepository subjectRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private SettingsService settingsService;

    private Student s1, s2, s3;
    private int dayOffset = 0;

    @BeforeEach
    void setUp() {
        dayOffset = 0;
        courseRepository.save(new com.attendance.system.model.Course(null, "CS101", "Intro", "CS", null, null));
        subjectRepository.save(new Subject(null, "CS101", "Intro to Programming", "CS101", "Fall 2026", null, null));

        s1 = studentRepository.findById(studentRepository.save(new Student(null, "S1", "Alice", "a@e.edu", "CS101", "Fall 2026", "A", null, null))).orElseThrow();
        s2 = studentRepository.findById(studentRepository.save(new Student(null, "S2", "Bob", "b@e.edu", "CS101", "Fall 2026", "A", null, null))).orElseThrow();
        s3 = studentRepository.findById(studentRepository.save(new Student(null, "S3", "Carol", "c@e.edu", "CS101", "Fall 2026", "B", null, null))).orElseThrow();
    }

    private AttendanceSessionDto newSessionDto() {
        AttendanceSessionDto d = new AttendanceSessionDto();
        d.setCourseCode("CS101");
        d.setSubjectCode("CS101");
        d.setSemester("Fall 2026");
        d.setSection("A");
        d.setSessionDate(LocalDate.now().toString());
        return d;
    }

    @Test
    @DisplayName("create session creates absent records for matching students only")
    void createSession() {
        AttendanceSessionDto created = attendanceService.createSession(newSessionDto(), "faculty");
        assertThat(created.getId()).isNotNull();
        assertThat(created.getStatus()).isEqualTo("active");

        List<AttendanceRecord> records = recordRepository.findBySession(created.getId());
        assertThat(records).hasSize(2); // only section A students (S1, S2)
        assertThat(records.stream().allMatch(r -> r.getStatus() == AttendanceStatus.ABSENT)).isTrue();
    }

    @Test
    @DisplayName("duplicate session same day/section rejected")
    void duplicateSessionRejected() {
        attendanceService.createSession(newSessionDto(), "faculty");
        assertThatThrownBy(() -> attendanceService.createSession(newSessionDto(), "faculty"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("different section allows separate session")
    void differentSectionAllowed() {
        attendanceService.createSession(newSessionDto(), "faculty");
        AttendanceSessionDto d2 = newSessionDto();
        d2.setSection("B");
        AttendanceSessionDto created = attendanceService.createSession(d2, "faculty");
        assertThat(created.getId()).isNotNull();
    }

    @Test
    @DisplayName("future date rejected")
    void futureDateRejected() {
        AttendanceSessionDto d = newSessionDto();
        d.setSessionDate(LocalDate.now().plusDays(1).toString());
        assertThatThrownBy(() -> attendanceService.createSession(d, "faculty"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("mark and edit attendance")
    void markAndEditAttendance() {
        AttendanceSessionDto created = attendanceService.createSession(newSessionDto(), "faculty");
        Long id = created.getId();

        AttendanceMarkDto m1 = new AttendanceMarkDto();
        m1.setStudentId("S1"); m1.setStudentName("Alice"); m1.setStatus(AttendanceStatus.PRESENT);
        AttendanceMarkDto m2 = new AttendanceMarkDto();
        m2.setStudentId("S2"); m2.setStudentName("Bob"); m2.setStatus(AttendanceStatus.ABSENT);
        List<AttendanceMarkDto> marks = List.of(m1, m2);

        List<com.attendance.system.dto.AttendanceRecordDto> result =
            attendanceService.markAttendance(id, marks, "faculty");
        assertThat(result).hasSize(2);

        List<AttendanceRecord> records = recordRepository.findBySession(id);
        assertThat(records.stream().filter(r -> "S1".equals(r.getStudentId())).findFirst().get().getStatus())
            .isEqualTo(AttendanceStatus.PRESENT);
        assertThat(records.stream().filter(r -> "S2".equals(r.getStudentId())).findFirst().get().getStatus())
            .isEqualTo(AttendanceStatus.ABSENT);

        // edit: flip Bob to present
        m2.setStatus(AttendanceStatus.PRESENT);
        attendanceService.markAttendance(id, List.of(m2), "faculty");
        List<AttendanceRecord> recordsAfter = recordRepository.findBySession(id);
        AttendanceRecord bob = recordsAfter.stream().filter(r -> "S2".equals(r.getStudentId())).findFirst().get();
        assertThat(bob.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
    }

    @Test
    @DisplayName("percentage: zero sessions")
    void percentageZeroSessions() {
        StudentAttendanceSummary sum = summaryFor("S1");
        assertThat(sum.getTotalSessions()).isZero();
        assertThat(sum.getPercentage()).isEqualTo(0.0);
        assertThat(sum.isBelowThreshold()).isFalse();
        assertThat(sum.getShortage()).isZero();
    }

    @Test
    @DisplayName("percentage: exactly 75% not below threshold")
    void percentageExactlyThreshold() {
        createMarkedSession("S1", true);
        createMarkedSession("S1", true);
        createMarkedSession("S1", true);
        createMarkedSession("S1", false); // 3/4 = 75%
        StudentAttendanceSummary sum = summaryFor("S1");
        assertThat(sum.getPercentage()).isEqualTo(75.0);
        assertThat(sum.isBelowThreshold()).isFalse();
        assertThat(sum.getShortage()).isZero();
    }

    @Test
    @DisplayName("percentage: below 75% flagged with shortage")
    void percentageBelowThreshold() {
        createMarkedSession("S1", true);
        createMarkedSession("S1", false);
        createMarkedSession("S1", false);
        createMarkedSession("S1", false); // 1/4 = 25%
        StudentAttendanceSummary sum = summaryFor("S1");
        assertThat(sum.getPercentage()).isEqualTo(25.0);
        assertThat(sum.isBelowThreshold()).isTrue();
        assertThat(sum.getShortage()).isEqualTo(2); // need 3 of 4, have 1
    }

    @Test
    @DisplayName("defaulters list")
    void defaulters() {
        settingsService.setAttendanceThreshold(75);
        // S1: 2/4 present = 50% -> defaulter. S2: 4/4 present = 100% -> not defaulter.
        markSession("S1", true, "S2", true);
        markSession("S1", true, "S2", true);
        markSession("S1", false, "S2", true);
        markSession("S1", false, "S2", true);

        List<StudentAttendanceSummary> defaulters = attendanceService.defaulters();
        assertThat(defaulters).hasSize(1);
        assertThat(defaulters.get(0).getStudentId()).isEqualTo("S1");
    }

    private void markSession(String id1, boolean p1, String id2, boolean p2) {
        AttendanceSessionDto d = newSessionDto();
        d.setSessionDate(LocalDate.now().minusDays(dayOffset++).toString());
        AttendanceSessionDto created = attendanceService.createSession(d, "faculty");
        Long id = created.getId();
        List<AttendanceMarkDto> marks = new java.util.ArrayList<>();
        AttendanceMarkDto m1 = new AttendanceMarkDto();
        m1.setStudentId(id1); m1.setStudentName(id1);
        m1.setStatus(p1 ? AttendanceStatus.PRESENT : AttendanceStatus.ABSENT);
        marks.add(m1);
        AttendanceMarkDto m2 = new AttendanceMarkDto();
        m2.setStudentId(id2); m2.setStudentName(id2);
        m2.setStatus(p2 ? AttendanceStatus.PRESENT : AttendanceStatus.ABSENT);
        marks.add(m2);
        attendanceService.markAttendance(id, marks, "faculty");
    }

    private void createMarkedSession(String studentId, boolean present) {
        AttendanceSessionDto d = newSessionDto();
        d.setSessionDate(LocalDate.now().minusDays(dayOffset++).toString());
        AttendanceSessionDto created = attendanceService.createSession(d, "faculty");
        Long id = created.getId();
        AttendanceMarkDto m = new AttendanceMarkDto();
        m.setStudentId(studentId); m.setStudentName(studentId);
        m.setStatus(present ? AttendanceStatus.PRESENT : AttendanceStatus.ABSENT);
        attendanceService.markAttendance(id, List.of(m), "faculty");
    }

    private StudentAttendanceSummary summaryFor(String studentId) {
        return attendanceService.allStudentSummaries().stream()
            .filter(s -> studentId.equals(s.getStudentId())).findFirst().orElseThrow();
    }
}
