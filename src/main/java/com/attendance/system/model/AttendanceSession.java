package com.attendance.system.model;

public class AttendanceSession {

    private Long id;
    private String courseCode;
    private String subjectCode;
    private String semester;
    private String section;
    private String sessionDate;
    private String createdBy;
    private String createdAt;
    private String status;

    public AttendanceSession() {
    }

    public AttendanceSession(Long id, String courseCode, String subjectCode, String semester,
                             String section, String sessionDate, String createdBy, String createdAt, String status) {
        this.id = id;
        this.courseCode = courseCode;
        this.subjectCode = subjectCode;
        this.semester = semester;
        this.section = section;
        this.sessionDate = sessionDate;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }
    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
    public String getSessionDate() { return sessionDate; }
    public void setSessionDate(String sessionDate) { this.sessionDate = sessionDate; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
