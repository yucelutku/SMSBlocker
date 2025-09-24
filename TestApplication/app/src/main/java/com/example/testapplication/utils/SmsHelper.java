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
        Log.d(TAG, "[DELETE] ========== SMS DELETION DEBUG START ==========");
        Log.d(TAG, "[DELETE] Message ID to delete: " + messageId);
        Log.d(TAG, "[DELETE] Package name: " + context.getPackageName());
        Log.d(TAG, "[DELETE] Android SDK: " + android.os.Build.VERSION.SDK_INT);
        Log.d(TAG, "[DELETE] Device: " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL);
        
        boolean hasReadSms = PermissionHelper.hasReadSmsPermission(context);
        boolean hasSendSms = PermissionHelper.hasSendSmsPermission(context);
        boolean hasReceiveSms = PermissionHelper.hasReceiveSmsPermission(context);
        boolean isDefaultSmsApp = PermissionHelper.isDefaultSmsApp(context);
        
        Log.d(TAG, "[DELETE] Permission - READ_SMS: " + hasReadSms);
        Log.d(TAG, "[DELETE] Permission - SEND_SMS: " + hasSendSms);
        Log.d(TAG, "[DELETE] Permission - RECEIVE_SMS: " + hasReceiveSms);
        Log.d(TAG, "[DELETE] Is Default SMS App: " + isDefaultSmsApp);
        
        if (!PermissionHelper.hasSmsPermissions(context)) {
            Log.e(TAG, "[DELETE] FAILED: SMS permissions not granted");
            Log.e(TAG, "[DELETE] ========== SMS DELETION DEBUG END (PERMISSION FAILURE) ==========");
            return false;
        }

        if (!isDefaultSmsApp) {
            Log.e(TAG, "[DELETE] FAILED: App is not default SMS app - deletion requires default SMS app status on Android 4.4+");
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                String defaultPackage = android.provider.Telephony.Sms.getDefaultSmsPackage(context);
                Log.e(TAG, "[DELETE] Current default SMS app: " + defaultPackage);
                Log.e(TAG, "[DELETE] This app package: " + context.getPackageName());
            }
            Log.e(TAG, "[DELETE] ========== SMS DELETION DEBUG END (DEFAULT APP FAILURE) ==========");
            return false;
        }

        try {
            ContentResolver resolver = context.getContentResolver();
            Uri deleteUri = Uri.parse("content://sms/" + messageId);
            
            Log.d(TAG, "[DELETE] ContentResolver: " + resolver.getClass().getName());
            Log.d(TAG, "[DELETE] Delete URI: " + deleteUri.toString());
            
            SmsMessage messageToDelete = getSmsMessageById(context, messageId);
            if (messageToDelete != null) {
                Log.d(TAG, "[DELETE] Message details - From: " + messageToDelete.address + ", Body: " + messageToDelete.body.substring(0, Math.min(50, messageToDelete.body.length())));
                Log.d(TAG, "[DELETE] Message type: " + messageToDelete.type + ", ThreadID: " + messageToDelete.threadId);
            } else {
                Log.w(TAG, "[DELETE] WARNING: Could not retrieve message details before deletion");
            }
            
            Log.i(TAG, "[DELETE] Executing ContentResolver.delete() on URI: " + deleteUri);
            
            int deletedRows = resolver.delete(deleteUri, null, null);
            
            Log.i(TAG, "[DELETE] ContentResolver.delete() returned: " + deletedRows + " rows");
            
            if (deletedRows > 0) {
                Log.i(TAG, "[DELETE] SUCCESS: Deleted SMS message " + messageId + ", rows affected: " + deletedRows);
                Log.i(TAG, "[DELETE] ========== SMS DELETION DEBUG END (SUCCESS) ==========");
                return true;
            } else {
                Log.e(TAG, "[DELETE] FAILED: No rows deleted for message ID: " + messageId);
                Log.e(TAG, "[DELETE] Possible causes: Message already deleted, invalid ID, or system protection");
                Log.e(TAG, "[DELETE] ========== SMS DELETION DEBUG END (ZERO ROWS) ==========");
                return false;
            }
            
        } catch (SecurityException e) {
            Log.e(TAG, "[DELETE] SECURITY EXCEPTION: " + e.getClass().getName());
            Log.e(TAG, "[DELETE] Exception message: " + e.getMessage());
            Log.e(TAG, "[DELETE] Stack trace:", e);
            Log.e(TAG, "[DELETE] This indicates system-level SMS protection or missing permissions");
            Log.e(TAG, "[DELETE] ========== SMS DELETION DEBUG END (SECURITY EXCEPTION) ==========");
            return false;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "[DELETE] ILLEGAL ARGUMENT: Invalid URI or parameters");
            Log.e(TAG, "[DELETE] Exception: " + e.getMessage(), e);
            Log.e(TAG, "[DELETE] ========== SMS DELETION DEBUG END (ILLEGAL ARGUMENT) ==========");
            return false;
        } catch (Exception e) {
            Log.e(TAG, "[DELETE] UNEXPECTED EXCEPTION: " + e.getClass().getName());
            Log.e(TAG, "[DELETE] Exception message: " + e.getMessage());
            Log.e(TAG, "[DELETE] Stack trace:", e);
            Log.e(TAG, "[DELETE] ========== SMS DELETION DEBUG END (EXCEPTION) ==========");
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