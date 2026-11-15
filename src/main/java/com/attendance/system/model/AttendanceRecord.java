package com.attendance.system.model;

public class AttendanceRecord {

    private Long id;
    private Long sessionId;
    private String studentId;
    private String studentName;
    private AttendanceStatus status;
    private String updatedBy;
    private String updatedAt;

    public AttendanceRecord() {
    }

    public AttendanceRecord(Long id, Long sessionId, String studentId, String studentName,
                            AttendanceStatus status, String updatedBy, String updatedAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.status = status;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public AttendanceStatus getStatus() { return status; }
    public void setStatus(AttendanceStatus status) { this.status = status; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
