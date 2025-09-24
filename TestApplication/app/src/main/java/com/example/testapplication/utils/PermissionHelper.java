package com.example.testapplication.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;

public class PermissionHelper {
    
    public static final String[] SMS_PERMISSIONS = {
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.WRITE_SMS
    };

    public static boolean hasSmsPermissions(Context context) {
        for (String permission : SMS_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) 
                != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
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
        return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_SMS) 
               == PackageManager.PERMISSION_GRANTED;
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
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            if (!isDefaultSmsApp(activity)) {
                android.content.Intent intent = new android.content.Intent(android.provider.Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
                intent.putExtra(android.provider.Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, activity.getPackageName());
                android.util.Log.d("PermissionHelper", "Requesting to be default SMS app");
                activity.startActivityForResult(intent, requestCode);
            } else {
                android.util.Log.d("PermissionHelper", "Already default SMS app");
            }
        }
    }
}