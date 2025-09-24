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

        try (Cursor cursor = resolver.query(
                SMS_URI, 
                SMS_PROJECTION, 
                null, 
                null, 
                sortOrder)) {
            
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    SmsMessage message = createSmsMessageFromCursor(cursor);
                    if (message != null) {
                        // Apply spam detection
                        SpamDetector.SpamAnalysisResult result = 
                            SpamDetector.analyzeMessage(message.body, message.address);
                        
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
                        // Apply spam detection
                        SpamDetector.SpamAnalysisResult result = 
                            SpamDetector.analyzeMessage(message.body, message.address);
                        
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
            Log.e(TAG, "[DELETE] SMS permissions not granted for deletion");
            return false;
        }

        if (!PermissionHelper.hasWriteSmsPermission(context)) {
            Log.e(TAG, "[DELETE] WRITE_SMS permission not granted");
            return false;
        }

        if (!PermissionHelper.isDefaultSmsApp(context)) {
            Log.e(TAG, "[DELETE] App is not default SMS app - deletion will fail on Android 4.4+");
            return false;
        }

        try {
            ContentResolver resolver = context.getContentResolver();
            Uri deleteUri = Uri.parse("content://sms/" + messageId);
            
            Log.d(TAG, "[DELETE] Attempting to delete SMS message ID: " + messageId + " from URI: " + deleteUri);
            
            int deletedRows = resolver.delete(deleteUri, null, null);
            
            if (deletedRows > 0) {
                Log.i(TAG, "[DELETE] Successfully deleted SMS message " + messageId + ", rows affected: " + deletedRows);
                return true;
            } else {
                Log.w(TAG, "[DELETE] No rows deleted for message ID: " + messageId);
                return false;
            }
            
        } catch (SecurityException e) {
            Log.e(TAG, "[DELETE] Security exception deleting SMS " + messageId + ": " + e.getMessage(), e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "[DELETE] Error deleting SMS " + messageId + ": " + e.getMessage(), e);
            return false;
        }
    }

    public static int deleteSpamMessages(Context context) {
        Log.d(TAG, "[DELETE_SPAM] Starting spam message deletion process");
        
        List<SmsMessage> spamMessages = getSpamMessages(context);
        Log.d(TAG, "[DELETE_SPAM] Found " + spamMessages.size() + " spam messages to delete");
        
        int deletedCount = 0;
        
        for (SmsMessage message : spamMessages) {
            Log.d(TAG, "[DELETE_SPAM] Attempting to delete spam message ID: " + message.id + " from: " + message.address);
            if (deleteSmsMessage(context, message.id)) {
                deletedCount++;
                Log.d(TAG, "[DELETE_SPAM] Successfully deleted message ID: " + message.id);
            } else {
                Log.w(TAG, "[DELETE_SPAM] Failed to delete message ID: " + message.id);
            }
        }
        
        Log.i(TAG, "[DELETE_SPAM] Deleted " + deletedCount + " out of " + spamMessages.size() + " spam messages");
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
}