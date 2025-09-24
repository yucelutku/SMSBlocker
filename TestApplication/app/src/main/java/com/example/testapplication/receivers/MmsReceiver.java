package com.example.testapplication.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Telephony;
import android.util.Log;

public class MmsReceiver extends BroadcastReceiver {
    private static final String TAG = "MmsReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "MMS received, action: " + intent.getAction());

        if (intent.getAction() == null) {
            Log.w(TAG, "Received intent with null action");
            return;
        }

        switch (intent.getAction()) {
            case Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION:
                handleWapPushDeliver(context, intent);
                break;
            case Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION:
                handleWapPushReceived(context, intent);
                break;
            default:
                Log.w(TAG, "Unknown action: " + intent.getAction());
                break;
        }
    }

    private void handleWapPushDeliver(Context context, Intent intent) {
        Log.d(TAG, "Handling WAP_PUSH_DELIVER_ACTION (MMS)");
        
        try {
            // Extract MMS data from intent
            byte[] data = intent.getByteArrayExtra("data");
            String mimeType = intent.getType();
            
            if (data == null) {
                Log.w(TAG, "No MMS data found in intent");
                return;
            }

            Log.d(TAG, "MMS data received - Type: " + mimeType + ", Size: " + data.length);
            
            // Process MMS message
            processMmsMessage(context, data, mimeType);
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling WAP_PUSH_DELIVER_ACTION: " + e.getMessage(), e);
        }
    }

    private void handleWapPushReceived(Context context, Intent intent) {
        Log.d(TAG, "Handling WAP_PUSH_RECEIVED_ACTION (legacy)");
        
        try {
            byte[] data = intent.getByteArrayExtra("data");
            String mimeType = intent.getType();
            
            if (data == null) {
                Log.w(TAG, "No WAP push data found in intent");
                return;
            }

            Log.d(TAG, "WAP push data received - Type: " + mimeType + ", Size: " + data.length);
            
            // Process WAP push message (could be MMS or other content)
            processWapPushMessage(context, data, mimeType);
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling WAP_PUSH_RECEIVED_ACTION: " + e.getMessage(), e);
        }
    }

    private void processMmsMessage(Context context, byte[] data, String mimeType) {
        try {
            Log.i(TAG, "Processing MMS message - MIME: " + mimeType);
            
            // Check if this is an MMS message
            if (isValidMmsType(mimeType)) {
                handleMmsContent(context, data);
            } else {
                Log.w(TAG, "Unknown MMS MIME type: " + mimeType);
            }

            // For Phase 1 MVP, we mainly focus on SMS spam blocking
            // MMS processing is basic - future phases can add:
            // - MMS spam detection
            // - Media content analysis
            // - Sender verification
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing MMS message: " + e.getMessage(), e);
        }
    }

    private void processWapPushMessage(Context context, byte[] data, String mimeType) {
        try {
            Log.i(TAG, "Processing WAP push message - MIME: " + mimeType);
            
            if (isValidMmsType(mimeType)) {
                // This is likely an MMS notification
                handleMmsContent(context, data);
            } else if (isWapPushType(mimeType)) {
                // This is a WAP push (could be promotional content)
                handleWapPushContent(context, data, mimeType);
            } else {
                Log.w(TAG, "Unknown WAP push MIME type: " + mimeType);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing WAP push message: " + e.getMessage(), e);
        }
    }

    private void handleMmsContent(Context context, byte[] data) {
        Log.d(TAG, "[MMS_HANDLE] Handling MMS content");
        
        try {
            Log.i(TAG, "[MMS_SAVE] CRITICAL: MMS must be written to system by MMS library");
            Log.i(TAG, "[MMS_SAVE] Default SMS apps typically use com.android.mms.transaction.TransactionService");
            Log.i(TAG, "[MMS_SAVE] This app delegates MMS handling to system - MMS should auto-save");
            
            Log.i(TAG, "[MMS_PROCESS] MMS message received - Size: " + data.length + " bytes");
            
            refreshMmsData(context);
            
        } catch (Exception e) {
            Log.e(TAG, "[MMS_HANDLE] Error handling MMS content: " + e.getMessage(), e);
        }
    }

    private void handleWapPushContent(Context context, byte[] data, String mimeType) {
        Log.d(TAG, "Handling WAP push content - Type: " + mimeType);
        
        try {
            // WAP push messages can be promotional/spam content
            // Future phases can implement:
            // - Parse WAP push SI/SL messages
            // - Extract URLs and check against spam databases
            // - Block promotional WAP push content
            // - Allow legitimate service messages
            
            Log.i(TAG, "WAP push message processed - Type: " + mimeType + ", Size: " + data.length);
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling WAP push content: " + e.getMessage(), e);
        }
    }

    private boolean isValidMmsType(String mimeType) {
        if (mimeType == null) return false;
        
        return mimeType.equals("application/vnd.wap.mms-message") ||
               mimeType.equals("application/vnd.wap.mms-delivery-report") ||
               mimeType.equals("application/vnd.wap.mms-read-report");
    }

    private boolean isWapPushType(String mimeType) {
        if (mimeType == null) return false;
        
        return mimeType.equals("application/vnd.wap.sic") ||
               mimeType.equals("application/vnd.wap.slc") ||
               mimeType.equals("text/vnd.wap.si") ||
               mimeType.equals("text/vnd.wap.sl");
    }

    private void refreshMmsData(Context context) {
        try {
            // MMS messages are typically stored in the same content provider as SMS
            // Refresh SMS repository to pick up new MMS messages too
            com.example.testapplication.repositories.SmsRepository repository = 
                com.example.testapplication.repositories.SmsRepository.getInstance(context);
            repository.refreshAllData();
            
        } catch (Exception e) {
            Log.e(TAG, "Error refreshing MMS data: " + e.getMessage(), e);
        }
    }
}