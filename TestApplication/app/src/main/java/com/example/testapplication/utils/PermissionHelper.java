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
        return hasReadSmsPermission(context) && 
               hasSendSmsPermission(context) && 
               hasReceiveSmsPermission(context);
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
            return context.getPackageName().equals(defaultSmsPackage);
        }
        return true;
    }

    public static void requestDefaultSmsApp(android.app.Activity activity, int requestCode) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            String currentPackage = activity.getPackageName();
            String defaultPackage = android.provider.Telephony.Sms.getDefaultSmsPackage(activity);
            
            if (currentPackage.equals(defaultPackage)) {
                return;
            }
            
            try {
                android.content.Intent intent = new android.content.Intent(android.provider.Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
                intent.putExtra(android.provider.Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, currentPackage);
                activity.startActivityForResult(intent, requestCode);
            } catch (android.content.ActivityNotFoundException e) {
                android.util.Log.e("PermissionHelper", "Cannot set default SMS app", e);
                android.widget.Toast.makeText(activity, 
                    "Cannot set as default SMS app. Please set manually in Settings > Default Apps > SMS app",
                    android.widget.Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                android.util.Log.e("PermissionHelper", "Error setting default SMS app", e);
                android.widget.Toast.makeText(activity,
                    "Error setting default SMS app: " + e.getMessage(),
                    android.widget.Toast.LENGTH_LONG).show();
            }
        }
    }
}