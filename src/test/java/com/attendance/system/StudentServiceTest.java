package com.attendance.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.attendance.system.dto.StudentDto;
import com.attendance.system.model.Student;
import com.attendance.system.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = AttendanceManagementApplication.class)
@Transactional
class StudentServiceTest {

    @Autowired
    private StudentService studentService;

    private StudentDto dto;

    @BeforeEach
    void setUp() {
        dto = new StudentDto();
        dto.setStudentId("S1001");
        dto.setName("Alice Johnson");
        dto.setEmail("alice@example.edu");
        dto.setCourse("CS101");
        dto.setSemester("Fall 2026");
        dto.setSection("A");
    }

    @Test
    @DisplayName("create and find student")
    void createAndFind() {
        Student created = studentService.create(dto);
        assertThat(created.getId()).isNotNull();
        assertThat(created.getStudentId()).isEqualTo("S1001");

        Student found = studentService.findById(created.getId()).orElseThrow();
        assertThat(found.getName()).isEqualTo("Alice Johnson");
    }

    @Test
    @DisplayName("duplicate student id rejected")
    void duplicateStudentIdRejected() {
        studentService.create(dto);
        assertThatThrownBy(() -> studentService.create(dto))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("update student")
    void updateStudent() {
        Student created = studentService.create(dto);
        StudentDto update = new StudentDto();
        update.setStudentId("S1001");
        update.setName("Alice Updated");
        update.setEmail("alice2@example.edu");
        update.setCourse("CS101");
        update.setSemester("Fall 2026");
        update.setSection("B");
        Student updated = studentService.update(created.getId(), update);
        assertThat(updated.getName()).isEqualTo("Alice Updated");
        assertThat(updated.getSection()).isEqualTo("B");
    }

    @Test
    @DisplayName("delete student")
    void deleteStudent() {
        Student created = studentService.create(dto);
        studentService.delete(created.getId());
        assertThat(studentService.findById(created.getId())).isEmpty();
    }

    @Test
    @DisplayName("find all")
    void findAll() {
        studentService.create(dto);
        StudentDto d2 = new StudentDto();
        d2.setStudentId("S1002"); d2.setName("Bob"); d2.setEmail("bob@example.edu");
        d2.setCourse("CS101"); d2.setSemester("Fall 2026"); d2.setSection("A");
        studentService.create(d2);
        assertThat(studentService.findAll()).hasSize(2);
    }
}
