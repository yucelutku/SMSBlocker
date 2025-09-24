# SMS App Validation - Default SMS App Requirements

## ✅ Complete SMS App Component Checklist

### Required Components for Default SMS App (Android 4.4+)

#### 1. ✅ SMS_DELIVER BroadcastReceiver
**Location:** AndroidManifest.xml lines 60-73
```xml
<receiver android:name=".receivers.SmsReceiver"
    android:exported="true"
    android:permission="android.permission.BROADCAST_SMS">
    <intent-filter android:priority="1000">
        <action android:name="android.provider.Telephony.SMS_DELIVER" />
    </intent-filter>
</receiver>
```
**Status:** ✅ PRESENT with required permission attribute

#### 2. ✅ WAP_PUSH_DELIVER BroadcastReceiver  
**Location:** AndroidManifest.xml lines 75-106
```xml
<receiver android:name=".receivers.MmsReceiver"
    android:exported="true"
    android:permission="android.permission.BROADCAST_WAP_PUSH">
    <intent-filter android:priority="1000">
        <action android:name="android.provider.Telephony.WAP_PUSH_DELIVER" />
        <data android:mimeType="application/vnd.wap.mms-message" />
    </intent-filter>
</receiver>
```
**Status:** ✅ PRESENT with required permission attribute

#### 3. ✅ RESPOND_VIA_MESSAGE Service
**Location:** AndroidManifest.xml lines 108-121
```xml
<service android:name=".services.ResponseService"
    android:exported="true"
    android:permission="android.permission.SEND_RESPOND_VIA_MESSAGE">
    <intent-filter>
        <action android:name="android.intent.action.RESPOND_VIA_MESSAGE" />
        <category android:name="android.intent.category.DEFAULT" />
        <data android:scheme="sms" />
        <data android:scheme="smsto" />
        <data android:scheme="mms" />
        <data android:scheme="mmsto" />
    </intent-filter>
</service>
```
**Status:** ✅ PRESENT with required permission attribute

#### 4. ✅ SENDTO Activity for SMS/MMS
**Location:** AndroidManifest.xml lines 32-68
```xml
<activity android:name=".activities.ComposeActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.SENDTO" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="sms" />
        <data android:scheme="smsto" />
        <data android:scheme="mms" />
        <data android:scheme="mmsto" />
    </intent-filter>
</activity>
```
**Status:** ✅ PRESENT with all required schemes

## Critical Fixes Applied

### Fix 1: Added BROADCAST_SMS Permission
**Before:**
```xml
<receiver android:name=".receivers.SmsReceiver"
    android:exported="true">
```

**After:**
```xml
<receiver android:name=".receivers.SmsReceiver"
    android:exported="true"
    android:permission="android.permission.BROADCAST_SMS">
```

**Impact:** System now recognizes receiver as SMS-capable

### Fix 2: Added BROADCAST_WAP_PUSH Permission
**Before:**
```xml
<receiver android:name=".receivers.MmsReceiver"
    android:exported="true">
```

**After:**
```xml
<receiver android:name=".receivers.MmsReceiver"
    android:exported="true"
    android:permission="android.permission.BROADCAST_WAP_PUSH">
```

**Impact:** System now recognizes receiver as MMS-capable

### Fix 3: Added MMS Schemes to ComposeActivity
**Before:**
```xml
<data android:scheme="sms" />
<data android:scheme="smsto" />
```

**After:**
```xml
<data android:scheme="sms" />
<data android:scheme="smsto" />
<data android:scheme="mms" />
<data android:scheme="mmsto" />
```

**Impact:** Activity now handles both SMS and MMS composition

## Validation Commands

### 1. Verify App Qualifies as SMS App
```bash
# Check if system recognizes app as SMS capable
adb shell dumpsys package com.example.testapplication | grep -A 10 "SMS_DELIVER"

# Expected output:
# Action: "android.provider.Telephony.SMS_DELIVER"
# Permission: "android.permission.BROADCAST_SMS"
```

### 2. Check Default SMS App Status
```bash
# Get current default SMS app
adb shell cmd role get-holders android.app.role.SMS

# Should show: com.example.testapplication (after manual selection)
```

### 3. List Available SMS Apps
```bash
# Query system for all SMS-capable apps
adb shell pm query-activities -a android.provider.Telephony.ACTION_CHANGE_DEFAULT

# App should appear in this list
```

### 4. Verify Intent-Filter Registration
```bash
# Check SENDTO intent-filter
adb shell dumpsys package com.example.testapplication | grep -A 5 "SENDTO"

# Should show sms, smsto, mms, mmsto schemes
```

## Manual Selection Test

### Samsung Galaxy A51 (One UI 5.1)

**Step 1: Open SMS App Settings**
1. Settings → Apps
2. Tap ⋮ (three dots) → Default apps
3. Tap "SMS app"

**Step 2: Verify App Appears in List**
Expected apps in list:
- ✅ Messages (Samsung Messages) [current default]
- ✅ **TestApplication** [your app] ← **SHOULD BE VISIBLE NOW**

**Step 3: Select App**
1. Tap "TestApplication"
2. Confirm selection

**Step 4: Verify Selection**
```bash
adb shell cmd role get-holders android.app.role.SMS
# Output: com.example.testapplication
```

## Troubleshooting

### Issue: App Still Not in List

**Diagnosis:**
```bash
# Check if all 4 required components exist
adb shell dumpsys package com.example.testapplication | grep -E "(SMS_DELIVER|WAP_PUSH_DELIVER|RESPOND_VIA_MESSAGE|SENDTO)"
```

**Required output:**
```
Action: "android.provider.Telephony.SMS_DELIVER"
Action: "android.provider.Telephony.WAP_PUSH_DELIVER"  
Action: "android.intent.action.RESPOND_VIA_MESSAGE"
Action: "android.intent.action.SENDTO"
```

**If any missing:** Reinstall app, permissions may not have updated

### Issue: Permission Denied Errors

**Check broadcast permissions:**
```bash
adb shell dumpsys package com.example.testapplication | grep -A 2 "BROADCAST_SMS\|BROADCAST_WAP_PUSH"
```

**Expected:**
```
Permission: "android.permission.BROADCAST_SMS"
Permission: "android.permission.BROADCAST_WAP_PUSH"
```

### Issue: Can Select But Doesn't Become Default

**Check role assignment:**
```bash
# Force set as default (for testing)
adb shell cmd role add-role-holder android.app.role.SMS com.example.testapplication

# Verify
adb shell cmd role get-holders android.app.role.SMS
```

## Android Version Compatibility

### Android 4.4 (KitKat) - 13 (Tiramisu)
- ✅ All components compatible
- ✅ SMS_DELIVER is primary delivery mechanism
- ✅ BROADCAST_SMS permission required
- ✅ Priority 1000 for default app behavior

### Android 12+ Specific
- ✅ All receivers have `android:exported="true"` (required)
- ✅ All services have `android:exported="true"` (required)
- ✅ Permission attributes properly set

## Success Indicators

### System Recognition
```bash
# App should appear in default SMS app role query
adb shell pm query-users android.app.role.SMS | grep com.example.testapplication
```

### UI Validation
- ✅ App appears in Settings → Default apps → SMS app list
- ✅ Can be selected without errors
- ✅ Selection persists after app restart

### Functional Validation
```bash
# Check if app receives SMS broadcasts
adb logcat -s SmsReceiver:* | grep SMS_DELIVER

# Should see when SMS arrives:
# SmsReceiver: onReceive: android.provider.Telephony.SMS_DELIVER
```

## Component File Verification

Ensure these files exist:

1. ✅ `/app/src/main/java/com/example/testapplication/receivers/SmsReceiver.java`
2. ✅ `/app/src/main/java/com/example/testapplication/receivers/MmsReceiver.java`
3. ✅ `/app/src/main/java/com/example/testapplication/services/ResponseService.java`
4. ✅ `/app/src/main/java/com/example/testapplication/activities/ComposeActivity.java`

All components must be implemented (even if minimal) for system recognition.

## Next Steps After Fix

1. **Rebuild APK**
   ```bash
   ./gradlew assembleDebug
   ```

2. **Uninstall Previous Version**
   ```bash
   adb uninstall com.example.testapplication
   ```

3. **Install Fresh Build**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

4. **Verify in Settings**
   - Settings → Apps → Default apps → SMS app
   - TestApplication should now appear in list

5. **Test Selection**
   - Select TestApplication
   - Verify with: `adb shell cmd role get-holders android.app.role.SMS`

## References

- [Android Default SMS App Requirements](https://developer.android.com/guide/topics/text/sms#default-sms-app)
- [Telephony SMS API](https://developer.android.com/reference/android/provider/Telephony.Sms)
- [Role Manager API](https://developer.android.com/reference/android/app/role/RoleManager)