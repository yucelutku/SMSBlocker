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
        Log.d(TAG, "SMS received, action: " + intent.getAction());

        if (intent.getAction() == null) {
            Log.w(TAG, "Received intent with null action");
            return;
        }

        switch (intent.getAction()) {
            case Telephony.Sms.Intents.SMS_DELIVER_ACTION:
                handleSmsDelivered(context, intent);
                break;
            case Telephony.Sms.Intents.SMS_RECEIVED_ACTION:
                handleSmsReceived(context, intent);
                break;
            default:
                Log.w(TAG, "Unknown action: " + intent.getAction());
                break;
        }
    }

    private void handleSmsDelivered(Context context, Intent intent) {
        Log.d(TAG, "Handling SMS_DELIVER_ACTION");
        
        try {
            // Extract SMS messages from intent
            SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
            
            if (messages == null || messages.length == 0) {
                Log.w(TAG, "No SMS messages found in intent");
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
        Log.d(TAG, "Handling SMS_RECEIVED_ACTION (legacy)");
        
        try {
            Bundle bundle = intent.getExtras();
            if (bundle == null) {
                Log.w(TAG, "No extras in SMS_RECEIVED intent");
                return;
            }

            Object[] pdus = (Object[]) bundle.get("pdus");
            String format = bundle.getString("format");
            
            if (pdus == null || pdus.length == 0) {
                Log.w(TAG, "No PDUs found in SMS_RECEIVED intent");
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
            long timestamp = smsMessage.getTimestampMillis();
            
            Log.d(TAG, "[SMS_PROCESS] Processing SMS from: " + sender + ", body length: " + 
                  (messageBody != null ? messageBody.length() : 0));

            Log.i(TAG, "[SMS_SAVE] CRITICAL: Saving SMS to system database (required for default SMS app)");
            Uri savedUri = saveSmsToSystem(context, smsMessage);
            
            if (savedUri != null) {
                Log.i(TAG, "[SMS_SAVE] Successfully saved SMS to system at: " + savedUri);
            } else {
                Log.e(TAG, "[SMS_SAVE] FAILED to save SMS to system - message may be lost!");
            }

            SpamDetector.SpamAnalysisResult spamResult = 
                SpamDetector.analyzeMessage(messageBody, sender);
            
            if (spamResult.isSpam) {
                Log.i(TAG, "[SPAM_DETECT] Spam detected from " + sender + 
                      " (score: " + spamResult.spamScore + "): " + spamResult.reason);
                
                handleSpamMessage(context, sender, messageBody, spamResult);
            } else {
                Log.d(TAG, "[CLEAN_MSG] Clean message from " + sender);
                handleCleanMessage(context, sender, messageBody);
            }

            refreshSmsData(context);
            
        } catch (Exception e) {
            Log.e(TAG, "[SMS_PROCESS] Error processing SMS message: " + e.getMessage(), e);
        }
    }

    private Uri saveSmsToSystem(Context context, SmsMessage smsMessage) {
        try {
            ContentValues values = new ContentValues();
            values.put(Telephony.TextBasedSmsColumns.ADDRESS, smsMessage.getDisplayOriginatingAddress());
            values.put(Telephony.TextBasedSmsColumns.BODY, smsMessage.getDisplayMessageBody());
            values.put(Telephony.TextBasedSmsColumns.DATE, smsMessage.getTimestampMillis());
            values.put(Telephony.TextBasedSmsColumns.DATE_SENT, smsMessage.getTimestampMillis());
            values.put(Telephony.TextBasedSmsColumns.TYPE, Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX);
            values.put(Telephony.TextBasedSmsColumns.READ, 0);
            values.put(Telephony.TextBasedSmsColumns.SEEN, 0);
            values.put(Telephony.TextBasedSmsColumns.PROTOCOL, smsMessage.getProtocolIdentifier());
            
            Log.d(TAG, "[SMS_SAVE] Inserting SMS into system database...");
            Uri uri = context.getContentResolver().insert(Telephony.Sms.Inbox.CONTENT_URI, values);
            
            if (uri != null) {
                Log.i(TAG, "[SMS_SAVE] SMS saved to system successfully, URI: " + uri);
            } else {
                Log.e(TAG, "[SMS_SAVE] ContentResolver.insert() returned null");
            }
            
            return uri;
            
        } catch (SecurityException e) {
            Log.e(TAG, "[SMS_SAVE] SecurityException - app may not be default SMS app: " + e.getMessage(), e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "[SMS_SAVE] Error saving SMS to system: " + e.getMessage(), e);
            return null;
        }
    }

    private void handleSpamMessage(Context context, String sender, String messageBody, 
                                 SpamDetector.SpamAnalysisResult spamResult) {
        Log.i(TAG, "Handling spam message from " + sender + 
              " - Category: " + SpamDetector.getSpamCategory(spamResult));
        
        // TODO Phase 2: Implement spam blocking logic
        // - Move message to spam folder
        // - Add sender to blocked list
        // - Show notification about blocked spam
        // - Update spam statistics
    }

    private void handleCleanMessage(Context context, String sender, String messageBody) {
        Log.d(TAG, "Handling clean message from " + sender);
        
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