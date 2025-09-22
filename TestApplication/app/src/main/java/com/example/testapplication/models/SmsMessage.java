package com.example.testapplication.models;

public class SmsMessage {
    public long id;
    public long threadId;
    public String address;
    public String body;
    public long date;
    public int type; // 1=inbox, 2=sent, 3=draft, 4=outbox
    public boolean isSpam;
    public boolean isBlocked;
    public float spamScore;
    public String spamReason;

    public SmsMessage() {
        this.isSpam = false;
        this.isBlocked = false;
        this.spamScore = 0.0f;
        this.spamReason = "";
    }

    public SmsMessage(long id, long threadId, String address, String body, long date, int type) {
        this();
        this.id = id;
        this.threadId = threadId;
        this.address = address;
        this.body = body;
        this.date = date;
        this.type = type;
    }

    public boolean isInbox() {
        return type == 1;
    }

    public boolean isSent() {
        return type == 2;
    }

    public String getFormattedDate() {
        return android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", date).toString();
    }

    public String getSenderName() {
        if (address == null || address.isEmpty()) {
            return "Unknown";
        }
        
        // Return phone number for now - can be enhanced with contact lookup
        return address;
    }

    @Override
    public String toString() {
        return "SmsMessage{" +
                "id=" + id +
                ", address='" + address + '\'' +
                ", body='" + body + '\'' +
                ", date=" + date +
                ", type=" + type +
                ", isSpam=" + isSpam +
                ", spamScore=" + spamScore +
                '}';
    }
}