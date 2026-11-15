package com.attendance.system.controller;

import com.attendance.system.dto.ApiResponse;
import com.attendance.system.dto.SubjectDto;
import com.attendance.system.model.Subject;
import com.attendance.system.service.SubjectService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subjects")
public class SubjectController {

    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ApiResponse<List<Subject>> list(
        @RequestParam(required = false) String courseCode,
        @RequestParam(required = false) String semester) {
        if (courseCode != null && !courseCode.isBlank() && semester != null && !semester.isBlank()) {
            return ApiResponse.ok(subjectService.findByCourseAndSemester(courseCode, semester));
        }
        return ApiResponse.ok(subjectService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ApiResponse<Subject> get(@PathVariable Long id) {
        return subjectService.findById(id)
            .map(ApiResponse::ok)
            .orElseGet(() -> ApiResponse.<Subject>error("Subject not found"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Subject> create(@Valid @RequestBody SubjectDto dto) {
        return ApiResponse.ok("Subject created", subjectService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Subject> update(@PathVariable Long id, @Valid @RequestBody SubjectDto dto) {
        return ApiResponse.ok("Subject updated", subjectService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        subjectService.delete(id);
        return ApiResponse.ok("Subject deleted", null);
    }
}
