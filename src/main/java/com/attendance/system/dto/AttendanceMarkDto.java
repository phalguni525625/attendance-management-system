package com.attendance.system.dto;

import com.attendance.system.model.AttendanceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AttendanceMarkDto {

    @NotBlank(message = "Student ID is required")
    private String studentId;

    private String studentName;

    @NotNull(message = "Status is required")
    private AttendanceStatus status;

    public AttendanceMarkDto() {
    }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public AttendanceStatus getStatus() { return status; }
    public void setStatus(AttendanceStatus status) { this.status = status; }
}