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
}