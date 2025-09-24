# SMS Permissions Setup Guide - Samsung Galaxy A51 (One UI 5.1)

## Required Permissions for Full Functionality

### 1. Runtime SMS Permissions
- ✅ READ_SMS - Read SMS messages
- ✅ SEND_SMS - Send SMS messages  
- ✅ RECEIVE_SMS - Receive SMS broadcasts

### 2. Default SMS App Status
- ✅ Must be set as default SMS app to delete messages (Android 4.4+)

## Automatic Permission Flow

### Step 1: Grant SMS Permissions
1. App will show permission dialog
2. Tap "Allow" for each permission:
   - Allow SMS Blocker to send and view SMS messages
   - Allow SMS Blocker to receive SMS messages
3. Check logcat for confirmation:
   ```
   [PERM_RESULT] ✓ All SMS permissions granted successfully
   ```

### Step 2: Set as Default SMS App
1. After SMS permissions granted, dialog appears:
   - **Samsung Users**: Pay attention to special instructions
2. Tap "Set as Default"
3. System will show SMS app selector
4. **IMPORTANT**: Select "TestApplication" (or app name) from list
5. Check logcat for confirmation:
   ```
   [DEFAULT_RESULT] ✓ Successfully became default SMS app
   ```

## Samsung-Specific Instructions

### Setting Default SMS App on Samsung Devices

**What to Expect:**
- After clicking "Set as Default", you'll see Samsung's app selector
- Look for **"TestApplication"** or the app name in the list
- If you don't see it, the app may not qualify as SMS app

**Important Notes:**
- Samsung Messages will be your current default
- You can switch back to Samsung Messages later
- The app selection happens in system settings overlay

### If App Doesn't Appear in Selector

This means the app is missing required components. Check:

1. **AndroidManifest.xml Requirements:**
   ```xml
   <!-- Must have SMS_DELIVER receiver -->
   <receiver android:name=".receivers.SmsReceiver" android:exported="true">
       <intent-filter android:priority="1000">
           <action android:name="android.provider.Telephony.SMS_DELIVER" />
       </intent-filter>
   </receiver>
   
   <!-- Must have RESPOND_VIA_MESSAGE service -->
   <service android:name=".services.ResponseService" android:exported="true">
       <intent-filter>
           <action android:name="android.intent.action.RESPOND_VIA_MESSAGE" />
       </intent-filter>
   </service>
   ```

2. **Verify components exist:**
   ```bash
   adb shell dumpsys package com.example.testapplication | grep -A 5 "SMS_DELIVER"
   ```

## Manual Setup (If Automatic Fails)

### Method 1: Settings > Default Apps
1. Open Samsung Settings
2. Go to Apps
3. Tap "⋮" (three dots) → Default apps
4. Tap "SMS app"
5. Select "TestApplication"

### Method 2: Settings > Apps
1. Open Samsung Settings
2. Go to Apps → TestApplication
3. Tap "Set as default"
4. Choose "SMS app"
5. Confirm selection

## Troubleshooting

### Issue: Permissions Dialog Never Appears
**Logcat Check:**
```bash
adb logcat -s MainActivity:* | grep PERM_REQUEST
```

**Expected:**
```
[PERM_REQUEST] Permission request initiated
[PERM_REQUEST] SMS permissions not granted, requesting...
[PERM_REQUEST] Permission dialog launched
```

**If missing**: Permission request not triggering. Reinstall app.

### Issue: Some Permissions Denied
**Logcat Shows:**
```
[PERM_RESULT] ✗ Some permissions denied: [android.permission.READ_SMS]
```

**Solution:**
1. App will show retry dialog automatically
2. Tap "Retry" to request again
3. Or grant manually in Settings > Apps > TestApplication > Permissions

### Issue: Default SMS App Dialog Doesn't Show Apps
**Logcat Check:**
```bash
adb logcat -s PermissionHelper:* | grep DEFAULT_SMS
```

**Expected:**
```
[DEFAULT_SMS] Launching default SMS app selection dialog
[DEFAULT_SMS] Intent launched successfully with request code: 101
```

**If "Activity not found" error:**
```
[DEFAULT_SMS] ERROR: Activity not found for ACTION_CHANGE_DEFAULT
```

**This means:** Samsung has custom implementation. Use manual setup.

### Issue: Selected App But Still Not Default
**Logcat Shows:**
```
[DEFAULT_RESULT] ✗ Not default SMS app after selection
[DEFAULT_RESULT] Current default SMS app: com.samsung.android.messaging
```

**Reasons:**
1. **User selected Samsung Messages instead** - Try again, select correct app
2. **App doesn't qualify** - Missing AndroidManifest components
3. **Samsung Knox blocking** - Check Knox restrictions

**Verify Selection:**
```bash
adb shell dumpsys role | grep com.example.testapplication
```

Should show:
```
RoleName: android.app.role.SMS
  Holders: com.example.testapplication
```

## Verification Commands

### Check All Permissions
```bash
# Check runtime permissions
adb shell dumpsys package com.example.testapplication | grep -A 3 "android.permission"

# Check default SMS app
adb shell cmd role get-holders android.app.role.SMS

# Expected output:
# com.example.testapplication
```

### Monitor Permission Flow
```bash
# Watch full permission flow
adb logcat -s MainActivity:* PermissionHelper:* | grep -E "(PERM_|DEFAULT_)"
```

### Force Reset
```bash
# Revoke all permissions
adb shell pm revoke com.example.testapplication android.permission.READ_SMS
adb shell pm revoke com.example.testapplication android.permission.SEND_SMS
adb shell pm revoke com.example.testapplication android.permission.RECEIVE_SMS

# Reset default SMS app to Samsung Messages
adb shell cmd role remove-role-holder android.app.role.SMS com.example.testapplication

# Restart app and try again
adb shell am force-stop com.example.testapplication
adb shell am start -n com.example.testapplication/.MainActivity
```

## Success Indicators

### UI Indicators
- ✅ Status shows: "Protection is active - SMS spam detection enabled"
- ✅ Status shows: "✓ Default SMS App: Full deletion enabled"
- ✅ Toast shows: "✓ Full protection enabled!"

### Logcat Indicators
```
[PERM_RESULT] ✓ All SMS permissions granted successfully
[DEFAULT_RESULT] ✓ Successfully became default SMS app
[PERM_CHECK] All SMS permissions granted: true
Default SMS app: com.example.testapplication, Current app: com.example.testapplication
```

### Functional Test
1. Try to delete an SMS message
2. Should see in logcat:
   ```
   [DELETE] Is Default SMS App: true
   [DELETE] SUCCESS: Deleted SMS message
   ```

## Known Samsung Issues

### One UI 5.1 Specific
- Default SMS app selector may take 2-3 seconds to appear
- App list might not include newly installed apps immediately
- Reboot device if app doesn't appear in selector

### Knox Security
- Enterprise devices may block default SMS app changes
- Check: `adb shell pm list packages | grep knox`
- If Knox active, may need MDM admin approval

### Samsung Messages Protection
- Some Samsung devices have "Message protection" feature
- Check: Settings > Messages > More settings > Message protection
- Disable if it blocks deletion

## Reverting to Samsung Messages

### After Testing
1. Go to Settings > Apps > Default apps > SMS app
2. Select "Messages" (Samsung Messages)
3. Or set from app: Settings > Default apps

### Via ADB
```bash
adb shell cmd role add-role-holder android.app.role.SMS com.samsung.android.messaging
```