package com.attendance.system.dto;

public class StudentAttendanceSummary {

    private String studentId;
    private String studentName;
    private int totalSessions;
    private int presentSessions;
    private int absentSessions;
    private double percentage;
    private boolean belowThreshold;
    private int shortage;
    private int threshold;

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public int getTotalSessions() { return totalSessions; }
    public void setTotalSessions(int totalSessions) { this.totalSessions = totalSessions; }
    public int getPresentSessions() { return presentSessions; }
    public void setPresentSessions(int presentSessions) { this.presentSessions = presentSessions; }
    public int getAbsentSessions() { return absentSessions; }
    public void setAbsentSessions(int absentSessions) { this.absentSessions = absentSessions; }
    public double getPercentage() { return percentage; }
    public void setPercentage(double percentage) { this.percentage = percentage; }
    public boolean isBelowThreshold() { return belowThreshold; }
    public void setBelowThreshold(boolean belowThreshold) { this.belowThreshold = belowThreshold; }
    public int getShortage() { return shortage; }
    public void setShortage(int shortage) { this.shortage = shortage; }
    public int getThreshold() { return threshold; }
    public void setThreshold(int threshold) { this.threshold = threshold; }
}
