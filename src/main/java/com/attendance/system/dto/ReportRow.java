package com.attendance.system.dto;

public class ReportRow {

    private String date;
    private String courseCode;
    private String subjectCode;
    private String section;
    private String studentId;
    private String studentName;
    private String status;

    public ReportRow() {
    }

    public ReportRow(String date, String courseCode, String subjectCode, String section,
                     String studentId, String studentName, String status) {
        this.date = date;
        this.courseCode = courseCode;
        this.subjectCode = subjectCode;
        this.section = section;
        this.studentId = studentId;
        this.studentName = studentName;
        this.status = status;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }
    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
