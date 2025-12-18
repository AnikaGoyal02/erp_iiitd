package edu.univ.erp.domain;

import java.time.LocalDateTime;

public class Notification {

    private final int notifId;
    private final int senderUserId;
    private final String senderName;
    private final String type;
    private final Integer targetId;
    private final String title;
    private final String message;
    private final LocalDateTime createdAt;

    public Notification(int notifId, int senderUserId, String senderName,
                        String type, Integer targetId,
                        String title, String message,
                        LocalDateTime createdAt) {

        this.notifId = notifId;
        this.senderUserId = senderUserId;
        this.senderName = senderName;
        this.type = type;
        this.targetId = targetId;
        this.title = title;
        this.message = message;
        this.createdAt = createdAt;
    }

    public int getNotifId() { return notifId; }
    public int getSenderUserId() { return senderUserId; }
    public String getSenderName() { return senderName; }
    public String getType() { return type; }
    public Integer getTargetId() { return targetId; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
