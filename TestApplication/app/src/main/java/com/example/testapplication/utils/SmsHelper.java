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
}