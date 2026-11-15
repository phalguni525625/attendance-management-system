package com.attendance.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.attendance.system.dto.CourseDto;
import com.attendance.system.model.Course;
import com.attendance.system.service.CourseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = AttendanceManagementApplication.class)
@Transactional
class CourseServiceTest {

    @Autowired
    private CourseService courseService;

    private CourseDto dto;

    @BeforeEach
    void setUp() {
        dto = new CourseDto();
        dto.setCode("CS101");
        dto.setName("Intro to Programming");
        dto.setDepartment("Computer Science");
    }

    @Test
    @DisplayName("create and find course")
    void createAndFind() {
        Course created = courseService.create(dto);
        assertThat(created.getId()).isNotNull();
        Course found = courseService.findById(created.getId()).orElseThrow();
        assertThat(found.getCode()).isEqualTo("CS101");
        assertThat(found.getDepartment()).isEqualTo("Computer Science");
    }

    @Test
    @DisplayName("duplicate course code rejected")
    void duplicateCodeRejected() {
        courseService.create(dto);
        assertThatThrownBy(() -> courseService.create(dto))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("update course")
    void updateCourse() {
        Course created = courseService.create(dto);
        CourseDto update = new CourseDto();
        update.setCode("CS101");
        update.setName("Programming I");
        update.setDepartment("Computing");
        Course updated = courseService.update(created.getId(), update);
        assertThat(updated.getName()).isEqualTo("Programming I");
    }

    @Test
    @DisplayName("delete course")
    void deleteCourse() {
        Course created = courseService.create(dto);
        courseService.delete(created.getId());
        assertThat(courseService.findById(created.getId())).isEmpty();
    }

    @Test
    @DisplayName("find all ordered")
    void findAll() {
        courseService.create(dto);
        CourseDto d2 = new CourseDto();
        d2.setCode("MA101"); d2.setName("Calculus"); d2.setDepartment("Mathematics");
        courseService.create(d2);
        assertThat(courseService.findAll()).hasSize(2);
    }
}
