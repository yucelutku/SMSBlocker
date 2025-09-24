package com.example.testapplication.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;

public class PermissionHelper {
    
    public static final String[] SMS_PERMISSIONS = {
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.RECEIVE_SMS
    };

    public static boolean hasSmsPermissions(Context context) {
        android.util.Log.d("PermissionHelper", "[PERM_CHECK] Checking SMS permissions...");
        
        boolean hasRead = hasReadSmsPermission(context);
        boolean hasSend = hasSendSmsPermission(context);
        boolean hasReceive = hasReceiveSmsPermission(context);
        
        android.util.Log.d("PermissionHelper", "[PERM_CHECK] READ_SMS: " + hasRead);
        android.util.Log.d("PermissionHelper", "[PERM_CHECK] SEND_SMS: " + hasSend);
        android.util.Log.d("PermissionHelper", "[PERM_CHECK] RECEIVE_SMS: " + hasReceive);
        
        boolean allGranted = hasRead && hasSend && hasReceive;
        android.util.Log.d("PermissionHelper", "[PERM_CHECK] All SMS permissions granted: " + allGranted);
        
        return allGranted;
    }

    public static boolean hasReadSmsPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) 
               == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasSendSmsPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) 
               == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasReceiveSmsPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) 
               == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasWriteSmsPermission(Context context) {
        return isDefaultSmsApp(context);
    }

    public static boolean isDefaultSmsApp(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            String defaultSmsPackage = android.provider.Telephony.Sms.getDefaultSmsPackage(context);
            String currentPackage = context.getPackageName();
            android.util.Log.d("PermissionHelper", "Default SMS app: " + defaultSmsPackage + ", Current app: " + currentPackage);
            return currentPackage.equals(defaultSmsPackage);
        }
        return true;
    }

    public static void requestDefaultSmsApp(android.app.Activity activity, int requestCode) {
        android.util.Log.d("PermissionHelper", "[DEFAULT_SMS] Request to become default SMS app initiated");
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            String currentPackage = activity.getPackageName();
            String defaultPackage = android.provider.Telephony.Sms.getDefaultSmsPackage(activity);
            
            android.util.Log.d("PermissionHelper", "[DEFAULT_SMS] Current package: " + currentPackage);
            android.util.Log.d("PermissionHelper", "[DEFAULT_SMS] Current default SMS app: " + defaultPackage);
            android.util.Log.d("PermissionHelper", "[DEFAULT_SMS] Device manufacturer: " + android.os.Build.MANUFACTURER);
            android.util.Log.d("PermissionHelper", "[DEFAULT_SMS] Android version: " + android.os.Build.VERSION.SDK_INT);
            
            if (currentPackage.equals(defaultPackage)) {
                android.util.Log.i("PermissionHelper", "[DEFAULT_SMS] Already default SMS app - no action needed");
                return;
            }
            
            try {
                android.content.Intent intent = new android.content.Intent(android.provider.Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
                intent.putExtra(android.provider.Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, currentPackage);
                
                android.util.Log.i("PermissionHelper", "[DEFAULT_SMS] Launching default SMS app selection dialog");
                android.util.Log.d("PermissionHelper", "[DEFAULT_SMS] Intent action: " + intent.getAction());
                android.util.Log.d("PermissionHelper", "[DEFAULT_SMS] Extra package name: " + currentPackage);
                
                activity.startActivityForResult(intent, requestCode);
                android.util.Log.d("PermissionHelper", "[DEFAULT_SMS] Intent launched successfully with request code: " + requestCode);
                
            } catch (android.content.ActivityNotFoundException e) {
                android.util.Log.e("PermissionHelper", "[DEFAULT_SMS] ERROR: Activity not found for ACTION_CHANGE_DEFAULT", e);
                android.util.Log.e("PermissionHelper", "[DEFAULT_SMS] This may be a Samsung-specific issue or missing system component");
                
                android.widget.Toast.makeText(activity, 
                    "Cannot set as default SMS app. Please set manually in Settings > Default Apps > SMS app",
                    android.widget.Toast.LENGTH_LONG).show();
                
            } catch (Exception e) {
                android.util.Log.e("PermissionHelper", "[DEFAULT_SMS] ERROR: Unexpected exception launching default SMS intent", e);
                
                android.widget.Toast.makeText(activity,
                    "Error setting default SMS app: " + e.getMessage(),
                    android.widget.Toast.LENGTH_LONG).show();
            }
        } else {
            android.util.Log.d("PermissionHelper", "[DEFAULT_SMS] Android version < KitKat, no default SMS app needed");
        }
    }
}