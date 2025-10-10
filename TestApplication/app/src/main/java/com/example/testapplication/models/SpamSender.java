package com.example.testapplication.models;

/**
 * Data model representing a spam sender with their statistics and blocking status
 */
public class SpamSender {
    public String phoneNumber;
    public String contactName;
    public int totalMessages;
    public int spamMessages;
    public float spamPercentage;
    public String sampleSpamMessage;
    public long lastMessageDate;
    public boolean isBlocked;
    public boolean isSelected; // For multi-selection UI

    public SpamSender() {
        this.isBlocked = false;
        this.isSelected = false;
        this.spamPercentage = 0.0f;
    }

    public SpamSender(String phoneNumber, int totalMessages, int spamMessages) {
        this();
        this.phoneNumber = phoneNumber;
        this.totalMessages = totalMessages;
        this.spamMessages = spamMessages;
        this.spamPercentage = calculateSpamPercentage();
    }

    public SpamSender(String phoneNumber, String contactName, int totalMessages, 
                     int spamMessages, String sampleSpamMessage, long lastMessageDate) {
        this(phoneNumber, totalMessages, spamMessages);
        this.contactName = contactName;
        this.sampleSpamMessage = sampleSpamMessage;
        this.lastMessageDate = lastMessageDate;
    }

    /**
     * Calculate spam percentage based on total and spam messages
     */
    private float calculateSpamPercentage() {
        if (totalMessages == 0) return 0.0f;
        return ((float) spamMessages / totalMessages) * 100.0f;
    }

    /**
     * Update spam percentage when counts change
     */
    public void updateSpamPercentage() {
        this.spamPercentage = calculateSpamPercentage();
    }

    /**
     * Get display name - contact name if available, otherwise phone number
     */
    public String getDisplayName() {
        if (contactName != null && !contactName.trim().isEmpty()) {
            return contactName;
        }
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            return phoneNumber;
        }
        return "Bilinmeyen";
    }

    /**
     * Get formatted spam percentage for display
     */
    public String getFormattedSpamPercentage() {
        return String.format("%%%.0f spam", spamPercentage);
    }

    /**
     * Get short sample message for preview (max 50 chars)
     */
    public String getShortSampleMessage() {
        if (sampleSpamMessage == null || sampleSpamMessage.trim().isEmpty()) {
            return "Örnek mesaj bulunamadı";
        }
        
        String trimmed = sampleSpamMessage.trim();
        if (trimmed.length() <= 50) {
            return trimmed;
        }
        
        return trimmed.substring(0, 47) + "...";
    }

    /**
     * Get formatted last message date
     */
    public String getFormattedLastMessageDate() {
        if (lastMessageDate <= 0) {
            return "Bilinmiyor";
        }
        return android.text.format.DateFormat.format("dd/MM/yyyy", lastMessageDate).toString();
    }

    /**
     * Check if this sender should be considered high-risk based on spam percentage
     */
    public boolean isHighRisk() {
        return spamPercentage >= 70.0f;
    }

    /**
     * Check if this sender should be considered medium-risk
     */
    public boolean isMediumRisk() {
        return spamPercentage >= 50.0f && spamPercentage < 70.0f;
    }

    /**
     * Check if this sender qualifies as a spam sender (>50% spam rate)
     */
    public boolean isSpamSender() {
        return spamPercentage >= 50.0f;
    }

    /**
     * Get spam risk level as text
     */
    public String getSpamRiskLevel() {
        if (isHighRisk()) {
            return "Yüksek Risk";
        } else if (isMediumRisk()) {
            return "Orta Risk";
        } else {
            return "Düşük Risk";
        }
    }

    /**
     * Create SpamSender from existing SenderInfo (for migration/compatibility)
     */
    public static SpamSender fromSenderInfo(com.example.testapplication.repositories.SmsRepository.SenderInfo senderInfo) {
        SpamSender spamSender = new SpamSender();
        spamSender.phoneNumber = senderInfo.phoneNumber;
        spamSender.totalMessages = senderInfo.totalCount;
        spamSender.spamMessages = senderInfo.spamCount;
        spamSender.updateSpamPercentage();
        spamSender.contactName = null; // Will be populated later via ContactsHelper
        return spamSender;
    }

    @Override
    public String toString() {
        return "SpamSender{" +
                "phoneNumber='" + phoneNumber + '\'' +
                ", contactName='" + contactName + '\'' +
                ", totalMessages=" + totalMessages +
                ", spamMessages=" + spamMessages +
                ", spamPercentage=" + spamPercentage +
                ", isBlocked=" + isBlocked +
                ", lastMessageDate=" + lastMessageDate +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        SpamSender that = (SpamSender) obj;
        return phoneNumber != null ? phoneNumber.equals(that.phoneNumber) : that.phoneNumber == null;
    }

    @Override
    public int hashCode() {
        return phoneNumber != null ? phoneNumber.hashCode() : 0;
    }
}