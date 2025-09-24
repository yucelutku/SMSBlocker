package com.example.testapplication.utils;

import android.Manifest;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;

import java.util.HashMap;
import java.util.Map;

public class ContactsHelper {
    private static final String TAG = "ContactsHelper";
    
    private static final Map<String, String> contactNameCache = new HashMap<>();
    
    public static boolean hasContactsPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) 
               == PackageManager.PERMISSION_GRANTED;
    }
    
    public static String getContactName(Context context, String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return "Bilinmeyen";
        }
        
        String cleanNumber = phoneNumber.trim();
        
        if (contactNameCache.containsKey(cleanNumber)) {
            return contactNameCache.get(cleanNumber);
        }
        
        if (!hasContactsPermission(context)) {
            Log.d(TAG, "No contacts permission, returning phone number: " + cleanNumber);
            return cleanNumber;
        }
        
        String contactName = lookupContactName(context, cleanNumber);
        
        contactNameCache.put(cleanNumber, contactName);
        
        return contactName;
    }
    
    private static String lookupContactName(Context context, String phoneNumber) {
        Uri uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI, 
            Uri.encode(phoneNumber)
        );
        
        String[] projection = {ContactsContract.PhoneLookup.DISPLAY_NAME};
        
        try (Cursor cursor = context.getContentResolver().query(
                uri, 
                projection, 
                null, 
                null, 
                null)) {
            
            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(0);
                if (name != null && !name.trim().isEmpty()) {
                    Log.d(TAG, "Found contact name for " + phoneNumber + ": " + name);
                    return name;
                }
            }
            
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception looking up contact for " + phoneNumber, e);
        } catch (Exception e) {
            Log.e(TAG, "Error looking up contact for " + phoneNumber, e);
        }
        
        Log.d(TAG, "No contact found for " + phoneNumber + ", returning number");
        return phoneNumber;
    }
    
    public static void clearCache() {
        Log.d(TAG, "Clearing contact name cache (" + contactNameCache.size() + " entries)");
        contactNameCache.clear();
    }
    
    public static int getCacheSize() {
        return contactNameCache.size();
    }
    
    public static String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 10) {
            return phoneNumber;
        }
        
        String cleaned = phoneNumber.replaceAll("[^0-9+]", "");
        
        if (cleaned.startsWith("+90") && cleaned.length() == 13) {
            return String.format("+90 %s %s %s %s",
                cleaned.substring(3, 6),
                cleaned.substring(6, 9),
                cleaned.substring(9, 11),
                cleaned.substring(11, 13)
            );
        }
        
        if (cleaned.startsWith("0") && cleaned.length() == 11) {
            return String.format("0%s %s %s %s",
                cleaned.substring(1, 4),
                cleaned.substring(4, 7),
                cleaned.substring(7, 9),
                cleaned.substring(9, 11)
            );
        }
        
        return phoneNumber;
    }
}