package com.attendance.system.service;

import com.attendance.system.dto.CourseDto;
import com.attendance.system.model.Course;
import com.attendance.system.repository.CourseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CourseService {

    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    public List<Course> findAll() {
        return courseRepository.findAll();
    }

    public Optional<Course> findById(Long id) {
        return courseRepository.findById(id);
    }

    @Transactional
    public Course create(CourseDto dto) {
        if (courseRepository.existsByCode(dto.getCode())) {
            throw new IllegalArgumentException("Course code already exists: " + dto.getCode());
        }
        Course course = toEntity(dto);
        Long id = courseRepository.save(course);
        course.setId(id);
        return course;
    }

    @Transactional
    public Course update(Long id, CourseDto dto) {
        Course course = courseRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Course not found: " + id));
        if (courseRepository.existsByCodeExcluding(dto.getCode(), id)) {
            throw new IllegalArgumentException("Course code already exists: " + dto.getCode());
        }
        course.setCode(dto.getCode());
        course.setName(dto.getName());
        course.setDepartment(dto.getDepartment());
        courseRepository.update(course);
        return course;
    }

    @Transactional
    public void delete(Long id) {
        courseRepository.delete(id);
    }

    private Course toEntity(CourseDto dto) {
        Course course = new Course();
        course.setCode(dto.getCode());
        course.setName(dto.getName());
        course.setDepartment(dto.getDepartment());
        return course;
    }
}
