package com.attendance.system.dto;

public class SubjectDto {

    private String code;
    private String name;
    private String courseCode;
    private String semester;

    public SubjectDto() {
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
}
