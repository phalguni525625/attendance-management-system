package com.attendance.system.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class Course {

    private Long id;

    @NotBlank(message = "Course code is required")
    @Size(max = 20, message = "Course code must not exceed 20 characters")
    private String code;

    @NotBlank(message = "Course name is required")
    @Size(max = 100, message = "Course name must not exceed 100 characters")
    private String name;

    @Size(max = 100, message = "Department must not exceed 100 characters")
    private String department;

    private String createdAt;
    private String updatedAt;

    public Course() {
    }

    public Course(Long id, String code, String name, String department, String createdAt, String updatedAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.department = department;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
