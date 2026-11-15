package com.attendance.system.model;

public class NotificationHistory {

    private Long id;
    private String type;
    private String recipient;
    private String subject;
    private String status;
    private String sentAt;
    private String errorMessage;

    public NotificationHistory() {
    }

    public NotificationHistory(Long id, String type, String recipient, String subject,
                               String status, String sentAt, String errorMessage) {
        this.id = id;
        this.type = type;
        this.recipient = recipient;
        this.subject = subject;
        this.status = status;
        this.sentAt = sentAt;
        this.errorMessage = errorMessage;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSentAt() { return sentAt; }
    public void setSentAt(String sentAt) { this.sentAt = sentAt; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
