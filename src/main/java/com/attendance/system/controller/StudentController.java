package com.attendance.system.controller;

import com.attendance.system.dto.ApiResponse;
import com.attendance.system.dto.StudentDto;
import com.attendance.system.model.Student;
import com.attendance.system.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ApiResponse<List<Student>> list(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String course,
        @RequestParam(required = false) String semester,
        @RequestParam(required = false) String section) {
        List<Student> students = studentService.findAll();
        List<Student> filtered = students.stream()
            .filter(s -> search == null || search.isBlank()
                || s.getName().toLowerCase().contains(search.toLowerCase())
                || s.getStudentId().toLowerCase().contains(search.toLowerCase()))
            .filter(s -> course == null || course.isBlank() || course.equals(s.getCourse()))
            .filter(s -> semester == null || semester.isBlank() || semester.equals(s.getSemester()))
            .filter(s -> section == null || section.isBlank() || section.equals(s.getSection()))
            .toList();
        return ApiResponse.ok(filtered);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ApiResponse<Student> get(@PathVariable Long id) {
        return studentService.findById(id)
            .map(ApiResponse::ok)
            .orElseGet(() -> ApiResponse.<Student>error("Student not found"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Student> create(@Valid @RequestBody StudentDto dto) {
        return ApiResponse.ok("Student created", studentService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Student> update(@PathVariable Long id, @Valid @RequestBody StudentDto dto) {
        return ApiResponse.ok("Student updated", studentService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        studentService.delete(id);
        return ApiResponse.ok("Student deleted", null);
    }
}
