package com.attendance.system.controller;

import com.attendance.system.dto.ApiResponse;
import com.attendance.system.dto.CourseDto;
import com.attendance.system.model.Course;
import com.attendance.system.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ApiResponse<List<Course>> list() {
        return ApiResponse.ok(courseService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ApiResponse<Course> get(@PathVariable Long id) {
        return courseService.findById(id)
            .map(ApiResponse::ok)
            .orElseGet(() -> ApiResponse.<Course>error("Course not found"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Course> create(@Valid @RequestBody CourseDto dto) {
        return ApiResponse.ok("Course created", courseService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Course> update(@PathVariable Long id, @Valid @RequestBody CourseDto dto) {
        return ApiResponse.ok("Course updated", courseService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        courseService.delete(id);
        return ApiResponse.ok("Course deleted", null);
    }
}
