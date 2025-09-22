package com.example.testapplication.services;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.testapplication.utils.PermissionHelper;

import java.util.ArrayList;

public class ResponseService extends Service {
    private static final String TAG = "ResponseService";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // This service doesn't provide binding
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "ResponseService started with action: " + 
              (intent != null ? intent.getAction() : "null"));

        if (intent != null && Intent.ACTION_RESPOND_VIA_MESSAGE.equals(intent.getAction())) {
            handleRespondViaMessage(intent);
        } else {
            Log.w(TAG, "Unknown or null intent action");
        }

        // Stop the service after handling the request
        stopSelf(startId);
        
        return START_NOT_STICKY;
    }

    private void handleRespondViaMessage(Intent intent) {
        try {
            // Extract recipient from intent data
            Uri uri = intent.getData();
            String recipient = null;
            
            if (uri != null) {
                String scheme = uri.getScheme();
                if ("sms".equals(scheme) || "smsto".equals(scheme) || 
                    "tel".equals(scheme)) {
                    recipient = uri.getSchemeSpecificPart();
                    
                    // Clean up the recipient number
                    if (recipient != null && recipient.startsWith("//")) {
                        recipient = recipient.substring(2);
                    }
                }
            }

            // Extract message from intent extras
            String message = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (message == null) {
                message = intent.getStringExtra("android.intent.extra.TEXT");
            }

            Log.d(TAG, "Respond via message - Recipient: " + recipient + 
                  ", Message: " + (message != null ? message.substring(0, Math.min(message.length(), 50)) : "null"));

            if (recipient == null || recipient.isEmpty()) {
                Log.e(TAG, "No recipient specified for respond via message");
                return;
            }

            if (message == null || message.isEmpty()) {
                Log.e(TAG, "No message specified for respond via message");
                return;
            }

            // Check permissions before sending
            if (!PermissionHelper.hasSendSmsPermission(this)) {
                Log.e(TAG, "SMS send permission not granted");
                return;
            }

            // Send the SMS response
            sendSmsResponse(recipient, message);

        } catch (Exception e) {
            Log.e(TAG, "Error handling respond via message: " + e.getMessage(), e);
        }
    }

    private void sendSmsResponse(String recipient, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            
            // For long messages, divide into multiple parts
            ArrayList<String> parts = smsManager.divideMessage(message);
            
            if (parts.size() == 1) {
                // Single message
                smsManager.sendTextMessage(recipient, null, message, null, null);
                Log.i(TAG, "Sent quick response SMS to " + recipient);
            } else {
                // Multiple parts
                smsManager.sendMultipartTextMessage(recipient, null, parts, null, null);
                Log.i(TAG, "Sent multipart quick response SMS (" + parts.size() + " parts) to " + recipient);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error sending SMS response: " + e.getMessage(), e);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "ResponseService created");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "ResponseService destroyed");
    }
}