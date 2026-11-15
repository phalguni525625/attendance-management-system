package com.attendance.system.dto;

import com.attendance.system.model.AttendanceStatus;

public class AttendanceMarkDto {

    private String studentId;
    private String studentName;
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
