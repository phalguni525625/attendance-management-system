package com.attendance.system.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class Subject {

    private Long id;

    @NotBlank(message = "Subject code is required")
    @Size(max = 20, message = "Subject code must not exceed 20 characters")
    private String code;

    @NotBlank(message = "Subject name is required")
    @Size(max = 100, message = "Subject name must not exceed 100 characters")
    private String name;

    @Size(max = 100, message = "Course code must not exceed 100 characters")
    private String courseCode;

    @Size(max = 20, message = "Semester must not exceed 20 characters")
    private String semester;

    private String createdAt;
    private String updatedAt;

    public Subject() {
    }

    public Subject(Long id, String code, String name, String courseCode, String semester,
                   String createdAt, String updatedAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.courseCode = courseCode;
        this.semester = semester;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
