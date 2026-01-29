package org.example.domain.entity;

public class Message {
    // Сообщения
    private String activeMessageLine1;
    private String activeMessageLine2;
    private String activeMessageLine3;
    private int messageTimer = 0;

    public String getActiveMessageLine1() {
        return activeMessageLine1;
    }

    public void setActiveMessageLine1(String activeMessageLine1) {
        this.activeMessageLine1 = activeMessageLine1;
    }

    public String getActiveMessageLine2() {
        return activeMessageLine2;
    }

    public void setActiveMessageLine2(String activeMessageLine2) {
        this.activeMessageLine2 = activeMessageLine2;
    }

    public String getActiveMessageLine3() {
        return activeMessageLine3;
    }

    public void setActiveMessageLine3(String activeMessageLine3) {
        this.activeMessageLine3 = activeMessageLine3;
    }

    public int getMessageTimer() {
        return messageTimer;
    }

    public void setMessageTimer(int messageTimer) {
        this.messageTimer = messageTimer;
    }

    public void resetMessage() {
        activeMessageLine1 = null;
        activeMessageLine2 = null;
        activeMessageLine3 = null;
    }
}
