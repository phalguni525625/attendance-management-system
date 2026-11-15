package com.attendance.system;

import static org.assertj.core.api.Assertions.assertThat;

import com.attendance.system.dto.AttendanceSessionDto;
import com.attendance.system.dto.ReportRow;
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
import com.attendance.system.service.ReportService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = AttendanceManagementApplication.class)
@Transactional
class ReportServiceTest {

    @Autowired
    private ReportService reportService;
    @Autowired
    private AttendanceSessionRepository sessionRepository;
    @Autowired
    private AttendanceRecordRepository recordRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private SubjectRepository subjectRepository;

    @BeforeEach
    void setUp() {
        courseRepository.save(new com.attendance.system.model.Course(null, "CS101", "Intro", "CS", null, null));
        subjectRepository.save(new Subject(null, "CS101", "Intro to Programming", "CS101", "Fall 2026", null, null));
        Student s1 = studentRepository.findById(studentRepository.save(new Student(null, "S1", "Alice", "a@e.edu", "CS101", "Fall 2026", "A", null, null))).orElseThrow();
        AttendanceSession ses = new AttendanceSession(null, "CS101", "CS101", "Fall 2026", "A", "2026-09-01", "faculty", null, "active");
        Long sessionId = sessionRepository.save(ses);
        AttendanceSession saved = new AttendanceSession(sessionId, "CS101", "CS101", "Fall 2026", "A", "2026-09-01", "faculty", null, "active");
        recordRepository.save(new AttendanceRecord(null, saved.getId(), "S1", "Alice", AttendanceStatus.PRESENT, "faculty", null));
    }

    @Test
    @DisplayName("student-wise report filtered by student")
    void studentWiseFiltered() {
        List<ReportRow> rows = reportService.studentWise("S1", null, null, null, null);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getStudentId()).isEqualTo("S1");
        assertThat(rows.get(0).getStatus()).isEqualTo("PRESENT");
    }

    @Test
    @DisplayName("student-wise report ignores other student")
    void studentWiseIgnoresOther() {
        List<ReportRow> rows = reportService.studentWise("S999", null, null, null, null);
        assertThat(rows).isEmpty();
    }

    @Test
    @DisplayName("subject-wise report filtered")
    void subjectWiseFiltered() {
        List<ReportRow> rows = reportService.subjectWise("CS101", "CS101", "Fall 2026", "A", null, null);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getSubjectCode()).isEqualTo("CS101");
    }

    @Test
    @DisplayName("sessions by date range")
    void sessionsByDateRange() {
        List<AttendanceSessionDto> sessions = reportService.sessionsByDateRange("2026-09-01", "2026-09-02");
        assertThat(sessions).hasSize(1);
        List<AttendanceSessionDto> none = reportService.sessionsByDateRange("2026-08-01", "2026-08-31");
        assertThat(none).isEmpty();
    }

    @Test
    @DisplayName("all students")
    void allStudents() {
        List<Student> students = reportService.allStudents();
        assertThat(students).hasSize(1);
        assertThat(students.get(0).getStudentId()).isEqualTo("S1");
    }
}
