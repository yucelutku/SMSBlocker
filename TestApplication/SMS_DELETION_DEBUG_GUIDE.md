# SMS Deletion Debugging Guide - Samsung Galaxy A51

## Critical Logcat Filters for Debugging

Run these commands to monitor SMS deletion operations:

```bash
# Monitor all SMS operations
adb logcat -s SmsHelper:* MainActivity:* SmsRepository:* PermissionHelper:*

# Or use combined filter
adb logcat | grep -E "(SmsHelper|MainActivity|SmsRepository|PermissionHelper)"
```

## Debug Output Analysis

### 1. Permission Check Output
Look for these log lines when attempting deletion:
```
[DELETE] ========== SMS DELETION DEBUG START ==========
[DELETE] Message ID to delete: [ID]
[DELETE] Package name: com.example.testapplication
[DELETE] Android SDK: [VERSION]
[DELETE] Device: samsung SM-A515F
[DELETE] Permission - READ_SMS: [true/false]
[DELETE] Permission - SEND_SMS: [true/false]
[DELETE] Permission - RECEIVE_SMS: [true/false]
[DELETE] Is Default SMS App: [true/false]
```

### 2. Default SMS App Status
Critical check - if FALSE, deletion will FAIL:
```
[DELETE] Is Default SMS App: false
[DELETE] FAILED: App is not default SMS app
[DELETE] Current default SMS app: com.samsung.android.messaging
[DELETE] This app package: com.example.testapplication
```

### 3. Deletion Attempt Output
```
[DELETE] ContentResolver: [class name]
[DELETE] Delete URI: content://sms/[MESSAGE_ID]
[DELETE] Message details - From: [number], Body: [preview]
[DELETE] Executing ContentResolver.delete() on URI: [URI]
[DELETE] ContentResolver.delete() returned: [0 or 1] rows
```

### 4. Success/Failure Indicators

**SUCCESS:**
```
[DELETE] SUCCESS: Deleted SMS message [ID], rows affected: 1
[DELETE] ========== SMS DELETION DEBUG END (SUCCESS) ==========
```

**FAILURE - Not Default App:**
```
[DELETE] FAILED: App is not default SMS app
[DELETE] ========== SMS DELETION DEBUG END (DEFAULT APP FAILURE) ==========
```

**FAILURE - Security Exception:**
```
[DELETE] SECURITY EXCEPTION: java.lang.SecurityException
[DELETE] Exception message: [details]
[DELETE] ========== SMS DELETION DEBUG END (SECURITY EXCEPTION) ==========
```

**FAILURE - Zero Rows:**
```
[DELETE] FAILED: No rows deleted for message ID: [ID]
[DELETE] Possible causes: Message already deleted, invalid ID, or system protection
[DELETE] ========== SMS DELETION DEBUG END (ZERO ROWS) ==========
```

## Samsung-Specific Issues

### Knox Security
Samsung devices with Knox may block SMS deletion even when app is default SMS handler.

**Check Knox status:**
```bash
adb shell pm list packages | grep knox
```

### One UI Restrictions
One UI 5.1 may have additional SMS protections:
- Check Settings > Apps > Default apps > SMS app
- Verify app appears in default SMS app list
- Check if Samsung Messages has any "protection" settings

## Testing Workflow

### Step 1: Verify App as Default SMS Handler
1. Open app
2. Check status text shows: "✓ Default SMS App: Full deletion enabled"
3. If not, dialog should appear to set as default
4. Accept default SMS app request

### Step 2: Test Single Message Deletion
1. Long press any SMS in list
2. Click Delete
3. Check logcat for full debug output
4. Note exact failure point if it fails

### Step 3: Analyze Logcat Output
```bash
adb logcat -c  # Clear logcat
# Attempt deletion in app
adb logcat -s SmsHelper:* MainActivity:* SmsRepository:*
```

### Step 4: Common Failure Patterns

**Pattern 1: Not Default App**
- Symptom: `Is Default SMS App: false`
- Solution: Request default SMS app permission
- Check: System Settings > Default Apps > SMS

**Pattern 2: Security Exception**
- Symptom: SecurityException in logcat
- Possible causes: 
  - Missing runtime permission
  - Samsung Knox protection
  - System-level SMS lock
- Solution: Check all permissions granted

**Pattern 3: Zero Rows Deleted**
- Symptom: `ContentResolver.delete() returned: 0 rows`
- Possible causes:
  - Message ID invalid/already deleted
  - Content provider access denied
  - Samsung protection blocking deletion
- Solution: Try deleting different message, check message exists

## Expected Logcat Flow for Successful Deletion

```
[UI] Delete requested for message ID: 12345
[REPO] Delete message request received for ID: 12345
[REPO] Executing delete on background thread for message ID: 12345
[DELETE] ========== SMS DELETION DEBUG START ==========
[DELETE] Message ID to delete: 12345
[DELETE] Package name: com.example.testapplication
[DELETE] Android SDK: 33
[DELETE] Device: samsung SM-A515F
[DELETE] Permission - READ_SMS: true
[DELETE] Permission - SEND_SMS: true
[DELETE] Permission - RECEIVE_SMS: true
[DELETE] Is Default SMS App: true
[DELETE] ContentResolver: android.app.ContextImpl$ApplicationContentResolver
[DELETE] Delete URI: content://sms/12345
[DELETE] Message details - From: +905551234567, Body: Test message
[DELETE] Message type: 1, ThreadID: 42
[DELETE] Executing ContentResolver.delete() on URI: content://sms/12345
[DELETE] ContentResolver.delete() returned: 1 rows
[DELETE] SUCCESS: Deleted SMS message 12345, rows affected: 1
[DELETE] ========== SMS DELETION DEBUG END (SUCCESS) ==========
[REPO] Delete operation result: true for message ID: 12345
[REPO] Deletion successful, refreshing all data
[UI] Delete SUCCESS for message ID: 12345
```

## Troubleshooting Commands

```bash
# Check if app is default SMS handler
adb shell cmd role get-held-roles-from-controller | grep com.example.testapplication

# Check SMS permissions
adb shell dumpsys package com.example.testapplication | grep -A 20 "granted=true"

# Check SMS database content
adb shell content query --uri content://sms --projection _id,address,body,date --where "_id=12345"

# Force stop and restart app
adb shell am force-stop com.example.testapplication
adb shell am start -n com.example.testapplication/.MainActivity
```

## Next Steps Based on Failure Type

### If "Not Default App" Error:
1. Check if dialog appears to set as default
2. Manually set in Settings if dialog doesn't work
3. Verify with logcat that `Is Default SMS App: true`

### If Security Exception:
1. Verify all SMS permissions granted: `adb shell dumpsys package`
2. Check Knox status if Samsung device
3. Try on non-Samsung device to isolate issue

### If Zero Rows Deleted:
1. Verify message ID exists: `adb shell content query --uri content://sms --where "_id=[ID]"`
2. Try different message IDs
3. Check Samsung Messages settings for deletion locks

### If Still Failing:
1. Capture full logcat: `adb logcat > deletion_debug.log`
2. Share log with error details
3. Test on different Android version/device