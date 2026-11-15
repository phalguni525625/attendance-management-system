package com.attendance.system.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class Student {

    private Long id;

    @NotBlank(message = "Student ID is required")
    @Size(max = 30, message = "Student ID must not exceed 30 characters")
    private String studentId;

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Size(max = 150, message = "Email must not exceed 150 characters")
    private String email;

    @Size(max = 100, message = "Course must not exceed 100 characters")
    private String course;

    @Size(max = 20, message = "Semester must not exceed 20 characters")
    private String semester;

    @Size(max = 20, message = "Section must not exceed 20 characters")
    private String section;

    private String createdAt;
    private String updatedAt;

    public Student() {
    }

    public Student(Long id, String studentId, String name, String email, String course,
                   String semester, String section, String createdAt, String updatedAt) {
        this.id = id;
        this.studentId = studentId;
        this.name = name;
        this.email = email;
        this.course = course;
        this.semester = semester;
        this.section = section;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }
    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
