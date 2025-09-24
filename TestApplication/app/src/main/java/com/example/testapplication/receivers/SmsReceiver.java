package com.example.testapplication.receivers;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

// Import not needed - using fully qualified name to avoid conflict with android.telephony.SmsMessage
// import com.example.testapplication.models.SmsMessage;
import com.example.testapplication.repositories.SmsRepository;
import com.example.testapplication.utils.SpamDetector;

public class SmsReceiver extends BroadcastReceiver {
    private static final String TAG = "SmsReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null) {
            return;
        }

        switch (intent.getAction()) {
            case Telephony.Sms.Intents.SMS_DELIVER_ACTION:
                handleSmsDelivered(context, intent);
                break;
            case Telephony.Sms.Intents.SMS_RECEIVED_ACTION:
                handleSmsReceived(context, intent);
                break;
        }
    }

    private void handleSmsDelivered(Context context, Intent intent) {
        try {
            // Extract SMS messages from intent
            SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
            
            if (messages == null || messages.length == 0) {
                return;
            }

            for (SmsMessage smsMessage : messages) {
                if (smsMessage == null) continue;
                
                processSmsMessage(context, smsMessage);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling SMS_DELIVER_ACTION: " + e.getMessage(), e);
        }
    }

    private void handleSmsReceived(Context context, Intent intent) {
        try {
            Bundle bundle = intent.getExtras();
            if (bundle == null) {
                return;
            }

            Object[] pdus = (Object[]) bundle.get("pdus");
            String format = bundle.getString("format");
            
            if (pdus == null || pdus.length == 0) {
                return;
            }

            for (Object pdu : pdus) {
                SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu, format);
                if (smsMessage != null) {
                    processSmsMessage(context, smsMessage);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling SMS_RECEIVED_ACTION: " + e.getMessage(), e);
        }
    }

    private void processSmsMessage(Context context, SmsMessage smsMessage) {
        try {
            String sender = smsMessage.getDisplayOriginatingAddress();
            String messageBody = smsMessage.getDisplayMessageBody();
            
            Uri savedUri = saveSmsToSystem(context, smsMessage);
            if (savedUri == null) {
                Log.e(TAG, "Failed to save SMS to system - message may be lost!");
            }

            SpamDetector.SpamAnalysisResult spamResult = 
                SpamDetector.analyzeMessage(messageBody, sender, context);
            
            if (spamResult.isSpam) {
                handleSpamMessage(context, sender, messageBody, spamResult);
            } else {
                handleCleanMessage(context, sender, messageBody);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing SMS: " + e.getMessage(), e);
        }
    }

    private Uri saveSmsToSystem(Context context, SmsMessage smsMessage) {
        try {
            String address = smsMessage.getDisplayOriginatingAddress();
            String body = smsMessage.getDisplayMessageBody();
            long timestamp = smsMessage.getTimestampMillis();
            
            if (smsAlreadyExists(context, address, body, timestamp)) {
                return null;
            }
            
            ContentValues values = new ContentValues();
            values.put(Telephony.TextBasedSmsColumns.ADDRESS, address);
            values.put(Telephony.TextBasedSmsColumns.BODY, body);
            values.put(Telephony.TextBasedSmsColumns.DATE, timestamp);
            values.put(Telephony.TextBasedSmsColumns.DATE_SENT, timestamp);
            values.put(Telephony.TextBasedSmsColumns.TYPE, Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX);
            values.put(Telephony.TextBasedSmsColumns.READ, 0);
            values.put(Telephony.TextBasedSmsColumns.SEEN, 0);
            values.put(Telephony.TextBasedSmsColumns.PROTOCOL, smsMessage.getProtocolIdentifier());
            
            return context.getContentResolver().insert(Telephony.Sms.Inbox.CONTENT_URI, values);
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving SMS: " + e.getMessage(), e);
            return null;
        }
    }
    
    private boolean smsAlreadyExists(Context context, String address, String body, long timestamp) {
        try {
            android.database.Cursor cursor = context.getContentResolver().query(
                Telephony.Sms.Inbox.CONTENT_URI,
                new String[]{"_id"},
                "address = ? AND body = ? AND date >= ? AND date <= ?",
                new String[]{
                    address,
                    body,
                    String.valueOf(timestamp - 2000),
                    String.valueOf(timestamp + 2000)
                },
                null
            );
            
            if (cursor != null) {
                boolean exists = cursor.getCount() > 0;
                cursor.close();
                return exists;
            }
            
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking duplicate: " + e.getMessage());
            return false;
        }
    }

    private void handleSpamMessage(Context context, String sender, String messageBody, 
                                 SpamDetector.SpamAnalysisResult spamResult) {
        
        // TODO Phase 2: Implement spam blocking logic
        // - Move message to spam folder
        // - Add sender to blocked list
        // - Show notification about blocked spam
        // - Update spam statistics
    }

    private void handleCleanMessage(Context context, String sender, String messageBody) {
        
        // TODO Phase 2: Implement clean message handling
        // - Show notification for legitimate messages
        // - Apply custom notification sounds
        // - Update message statistics
    }

    private void refreshSmsData(Context context) {
        try {
            // Refresh SMS repository data to update UI
            SmsRepository repository = SmsRepository.getInstance(context);
            repository.refreshAllData();
            
        } catch (Exception e) {
            Log.e(TAG, "Error refreshing SMS data: " + e.getMessage(), e);
        }
    }
}