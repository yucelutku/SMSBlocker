package com.example.testapplication.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.example.testapplication.models.SmsMessage;

import java.util.ArrayList;
import java.util.List;

public class SmsHelper {
    private static final String TAG = "SmsHelper";
    
    private static final Uri SMS_INBOX_URI = Uri.parse("content://sms/inbox");
    private static final Uri SMS_SENT_URI = Uri.parse("content://sms/sent");
    private static final Uri SMS_URI = Uri.parse("content://sms");
    
    private static final String[] SMS_PROJECTION = {
        "_id",          // 0
        "thread_id",    // 1
        "address",      // 2
        "body",         // 3
        "date",         // 4
        "type"          // 5
    };

    public static List<SmsMessage> getAllSmsMessages(Context context) {
        return getAllSmsMessages(context, 100); // Default limit
    }

    public static List<SmsMessage> getAllSmsMessages(Context context, int limit) {
        List<SmsMessage> messages = new ArrayList<>();
        
        if (!PermissionHelper.hasSmsPermissions(context)) {
            Log.w(TAG, "SMS permissions not granted");
            return messages;
        }

        ContentResolver resolver = context.getContentResolver();
        String sortOrder = "date DESC" + (limit > 0 ? " LIMIT " + limit : "");
        String selection = "type = ?";
        String[] selectionArgs = {"1"};

        try (Cursor cursor = resolver.query(
                SMS_URI, 
                SMS_PROJECTION, 
                selection, 
                selectionArgs, 
                sortOrder)) {
            
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    SmsMessage message = createSmsMessageFromCursor(cursor);
                    if (message != null) {
                        SpamDetector.SpamAnalysisResult result = 
                            SpamDetector.analyzeMessage(message.body, message.address, context);
                        
                        message.isSpam = result.isSpam;
                        message.spamScore = result.spamScore;
                        message.spamReason = result.reason;
                        
                        messages.add(message);
                    }
                } while (cursor.moveToNext());
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception reading SMS: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error reading SMS: " + e.getMessage());
        }

        Log.d(TAG, "Retrieved " + messages.size() + " SMS messages");
        return messages;
    }

    public static List<SmsMessage> getInboxMessages(Context context, int limit) {
        List<SmsMessage> messages = new ArrayList<>();
        
        if (!PermissionHelper.hasSmsPermissions(context)) {
            Log.w(TAG, "SMS permissions not granted");
            return messages;
        }

        ContentResolver resolver = context.getContentResolver();
        String sortOrder = "date DESC" + (limit > 0 ? " LIMIT " + limit : "");

        try (Cursor cursor = resolver.query(
                SMS_INBOX_URI, 
                SMS_PROJECTION, 
                null, 
                null, 
                sortOrder)) {
            
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    SmsMessage message = createSmsMessageFromCursor(cursor);
                    if (message != null) {
                        SpamDetector.SpamAnalysisResult result = 
                            SpamDetector.analyzeMessage(message.body, message.address, context);
                        
                        message.isSpam = result.isSpam;
                        message.spamScore = result.spamScore;
                        message.spamReason = result.reason;
                        
                        messages.add(message);
                    }
                } while (cursor.moveToNext());
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception reading inbox SMS: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error reading inbox SMS: " + e.getMessage());
        }

        return messages;
    }

    public static List<SmsMessage> getSpamMessages(Context context) {
        List<SmsMessage> allMessages = getAllSmsMessages(context);
        List<SmsMessage> spamMessages = new ArrayList<>();
        
        for (SmsMessage message : allMessages) {
            if (message.isSpam) {
                spamMessages.add(message);
            }
        }
        
        return spamMessages;
    }

    public static boolean deleteSmsMessage(Context context, long messageId) {
        if (!PermissionHelper.hasSmsPermissions(context)) {
            Log.e(TAG, "Delete failed: SMS permissions not granted");
            return false;
        }

        if (!PermissionHelper.isDefaultSmsApp(context)) {
            Log.e(TAG, "Delete failed: App must be default SMS app for deletion");
            return false;
        }

        try {
            ContentResolver resolver = context.getContentResolver();
            Uri deleteUri = Uri.parse("content://sms/" + messageId);
            int deletedRows = resolver.delete(deleteUri, null, null);
            
            if (deletedRows > 0) {
                return true;
            } else {
                Log.w(TAG, "Delete failed: Message ID " + messageId + " not found or already deleted");
                return false;
            }
            
        } catch (SecurityException e) {
            Log.e(TAG, "Delete failed: Security exception - " + e.getMessage());
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Delete failed: " + e.getMessage(), e);
            return false;
        }
    }

    public static int deleteSpamMessages(Context context) {
        List<SmsMessage> spamMessages = getSpamMessages(context);
        int deletedCount = 0;
        
        for (SmsMessage message : spamMessages) {
            if (deleteSmsMessage(context, message.id)) {
                deletedCount++;
            }
        }
        
        if (deletedCount > 0) {
            Log.i(TAG, "Deleted " + deletedCount + " spam messages");
        }
        return deletedCount;
    }

    public static SmsMessage getSmsMessageById(Context context, long messageId) {
        if (!PermissionHelper.hasSmsPermissions(context)) {
            return null;
        }

        ContentResolver resolver = context.getContentResolver();
        Uri messageUri = Uri.parse("content://sms/" + messageId);

        try (Cursor cursor = resolver.query(
                messageUri, 
                SMS_PROJECTION, 
                null, 
                null, 
                null)) {
            
            if (cursor != null && cursor.moveToFirst()) {
                return createSmsMessageFromCursor(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading SMS by ID: " + e.getMessage());
        }

        return null;
    }

    private static SmsMessage createSmsMessageFromCursor(Cursor cursor) {
        try {
            long id = cursor.getLong(0);
            long threadId = cursor.getLong(1);
            String address = cursor.getString(2);
            String body = cursor.getString(3);
            long date = cursor.getLong(4);
            int type = cursor.getInt(5);

            return new SmsMessage(id, threadId, address, body, date, type);
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating SMS message from cursor: " + e.getMessage());
            return null;
        }
    }

    public static class SmsStatistics {
        public int totalMessages;
        public int inboxMessages;
        public int sentMessages;
        public int spamMessages;
        public int todayMessages;
        
        public SmsStatistics() {
            this.totalMessages = 0;
            this.inboxMessages = 0;
            this.sentMessages = 0;
            this.spamMessages = 0;
            this.todayMessages = 0;
        }
    }

    public static SmsStatistics getSmsStatistics(Context context) {
        SmsStatistics stats = new SmsStatistics();
        
        List<SmsMessage> allMessages = getAllSmsMessages(context, 0); // No limit
        
        long todayStart = System.currentTimeMillis() - (24 * 60 * 60 * 1000); // 24 hours ago
        
        for (SmsMessage message : allMessages) {
            stats.totalMessages++;
            
            if (message.isInbox()) {
                stats.inboxMessages++;
            } else if (message.isSent()) {
                stats.sentMessages++;
            }
            
            if (message.isSpam) {
                stats.spamMessages++;
            }
            
            if (message.date >= todayStart) {
                stats.todayMessages++;
            }
        }
        
        return stats;
    }
    
    /**
     * Get all SMS messages without limit (for bulk operations)
     */
    public static List<SmsMessage> getAllMessages(Context context) {
        return getAllSmsMessages(context, 0); // No limit
    }
    
    /**
     * Delete a single SMS message by ID
     */
    public static boolean deleteMessage(Context context, long messageId) {
        if (!PermissionHelper.hasSmsPermissions(context)) {
            Log.w(TAG, "SMS permissions not granted for deletion");
            return false;
        }
        
        try {
            ContentResolver resolver = context.getContentResolver();
            Uri deleteUri = Uri.parse("content://sms/" + messageId);
            
            int deletedRows = resolver.delete(deleteUri, null, null);
            
            if (deletedRows > 0) {
                Log.d(TAG, "Successfully deleted SMS message with ID: " + messageId);
                return true;
            } else {
                Log.w(TAG, "No message found with ID: " + messageId);
                return false;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error deleting SMS message with ID " + messageId + ": " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Bulk delete multiple SMS messages by IDs
     */
    public static int bulkDeleteMessages(Context context, List<Long> messageIds) {
        if (!PermissionHelper.hasSmsPermissions(context)) {
            Log.w(TAG, "SMS permissions not granted for bulk deletion");
            return 0;
        }
        
        int deletedCount = 0;
        ContentResolver resolver = context.getContentResolver();
        
        for (Long messageId : messageIds) {
            try {
                Uri deleteUri = Uri.parse("content://sms/" + messageId);
                int deletedRows = resolver.delete(deleteUri, null, null);
                
                if (deletedRows > 0) {
                    deletedCount++;
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error deleting message ID " + messageId + ": " + e.getMessage());
            }
        }
        
        Log.d(TAG, "Bulk deleted " + deletedCount + " out of " + messageIds.size() + " messages");
        return deletedCount;
    }
    
    /**
     * Delete all SMS messages of specific type
     */
    public static int deleteMessagesByType(Context context, int type) {
        if (!PermissionHelper.hasSmsPermissions(context)) {
            Log.w(TAG, "SMS permissions not granted for type deletion");
            return 0;
        }
        
        try {
            ContentResolver resolver = context.getContentResolver();
            String selection = "type = ?";
            String[] selectionArgs = {String.valueOf(type)};
            
            int deletedRows = resolver.delete(SMS_URI, selection, selectionArgs);
            Log.d(TAG, "Deleted " + deletedRows + " messages of type " + type);
            return deletedRows;
            
        } catch (Exception e) {
            Log.e(TAG, "Error deleting messages by type " + type + ": " + e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * Delete messages from specific sender
     */
    public static int deleteMessagesBySender(Context context, String senderAddress) {
        if (!PermissionHelper.hasSmsPermissions(context)) {
            Log.w(TAG, "SMS permissions not granted for sender deletion");
            return 0;
        }
        
        try {
            ContentResolver resolver = context.getContentResolver();
            String selection = "address = ?";
            String[] selectionArgs = {senderAddress};
            
            int deletedRows = resolver.delete(SMS_URI, selection, selectionArgs);
            Log.d(TAG, "Deleted " + deletedRows + " messages from sender: " + senderAddress);
            return deletedRows;
            
        } catch (Exception e) {
            Log.e(TAG, "Error deleting messages from sender " + senderAddress + ": " + e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * Delete messages in date range
     */
    public static int deleteMessagesByDateRange(Context context, long startDate, long endDate) {
        if (!PermissionHelper.hasSmsPermissions(context)) {
            Log.w(TAG, "SMS permissions not granted for date range deletion");
            return 0;
        }
        
        try {
            ContentResolver resolver = context.getContentResolver();
            String selection = "date >= ? AND date <= ?";
            String[] selectionArgs = {String.valueOf(startDate), String.valueOf(endDate)};
            
            int deletedRows = resolver.delete(SMS_URI, selection, selectionArgs);
            Log.d(TAG, "Deleted " + deletedRows + " messages in date range");
            return deletedRows;
            
        } catch (Exception e) {
            Log.e(TAG, "Error deleting messages by date range: " + e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * Check if SMS content provider is available
     */
    public static boolean isSmsProviderAvailable(Context context) {
        try {
            ContentResolver resolver = context.getContentResolver();
            Cursor cursor = resolver.query(SMS_URI, new String[]{"_id"}, null, null, "date DESC LIMIT 1");
            
            if (cursor != null) {
                cursor.close();
                return true;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "SMS provider not available: " + e.getMessage());
        }
        
        return false;
    }
}