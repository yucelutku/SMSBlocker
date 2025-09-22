# CLAUDE.md - SMS Spam Blocker Project Context

## 📱 Project Overview

**Project Name**: SMS Spam Blocker  
**Package**: `com.example.testapplication`  
**Target SDK**: API 33 (Android 13)  
**Minimum SDK**: API 26 (Android 8.0)  
**Language**: Java  
**Architecture**: MVVM + Repository Pattern

## 🎯 MVP Phase 1 Goals

1. **Default SMS App Registration**
    - Implement required components for default SMS handler
    - Request user to set as default SMS app
    - Handle SMS_DELIVER and WAP_PUSH_DELIVER intents

2. **SMS Permissions & Access**
    - Request READ_SMS, WRITE_SMS, RECEIVE_SMS permissions
    - Handle runtime permissions properly for Android 13
    - Implement permission check utilities

3. **Message Management**
    - Display SMS messages in modern Material Design 3 UI
    - Implement manual message deletion functionality
    - Basic spam keyword detection (Turkish gambling terms)

4. **UI/UX Requirements**
    - Material Design 3 components
    - Dark/Light theme support with dynamic colors
    - Modern, clean interface with intuitive navigation

## 🏗️ Technical Architecture

### Core Components Required:

**1. Manifest Requirements:**
```xml
<!-- SMS Permissions -->
<uses-permission android:name="android.permission.SEND_SMS" />
<uses-permission android:name="android.permission.RECEIVE_SMS" />
<uses-permission android:name="android.permission.READ_SMS" />
<uses-permission android:name="android.permission.WRITE_SMS" />

<!-- Default SMS App Components -->
<receiver android:name=".receivers.SmsReceiver">
    <intent-filter android:priority="1000">
        <action android:name="android.provider.Telephony.SMS_DELIVER" />
    </intent-filter>
</receiver>

<receiver android:name=".receivers.MmsReceiver">
    <intent-filter android:priority="1000">
        <action android:name="android.provider.Telephony.WAP_PUSH_DELIVER" />
        <data android:mimeType="application/vnd.wap.mms-message" />
    </intent-filter>
</receiver>

<activity android:name=".activities.ComposeActivity">
    <intent-filter>
        <action android:name="android.intent.action.SEND" />
        <action android:name="android.intent.action.SENDTO" />
        <data android:scheme="sms" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</activity>

<service android:name=".services.ResponseService"
         android:permission="android.permission.SEND_RESPOND_VIA_MESSAGE">
    <intent-filter>
        <action android:name="android.intent.action.RESPOND_VIA_MESSAGE" />
        <data android:scheme="sms" />
    </intent-filter>
</service>
```

**2. Key Classes to Implement:**

- `MainActivity.java` - Main dashboard with Material Design 3
- `SmsListAdapter.java` - RecyclerView adapter for SMS display
- `SmsReceiver.java` - BroadcastReceiver for incoming SMS
- `MmsReceiver.java` - BroadcastReceiver for incoming MMS
- `SmsRepository.java` - Data access layer for SMS operations
- `SmsViewModel.java` - MVVM ViewModel for UI state management
- `PermissionHelper.java` - Runtime permission management
- `DefaultSmsHelper.java` - Default SMS app utilities
- `SpamDetector.java` - Keyword-based spam detection

**3. Database Schema:**
```java
@Entity(tableName = "sms_messages")
public class SmsMessage {
    @PrimaryKey
    public long id;
    public String address;
    public String body;
    public long date;
    public int type; // 1=inbox, 2=sent
    public boolean isSpam;
    public boolean isBlocked;
}

@Entity(tableName = "blocked_numbers")
public class BlockedNumber {
    @PrimaryKey
    public String phoneNumber;
    public long blockedDate;
    public String reason;
}
```

## 🎨 UI/UX Guidelines

### Material Design 3 Components:
- `MaterialToolbar` for app bars
- `FloatingActionButton` for primary actions
- `MaterialCardView` for SMS message items
- `MaterialButton` for actions
- `MaterialAlertDialog` for confirmations
- `NavigationView` or `BottomNavigation` for navigation

### Color Scheme:
- Use Material You dynamic color system
- Support both Light and Dark themes
- Primary colors should reflect security/protection theme

### Typography:
- Use Material Design 3 type scale
- Clear hierarchy with proper text sizes
- Support for Turkish characters

## 🔧 Development Priorities

### Phase 1 (Current):
1. Setup default SMS app infrastructure
2. Implement permission handling
3. Create basic SMS list view
4. Add manual delete functionality
5. Basic spam keyword detection

### Phase 2 (Future):
1. Advanced spam detection algorithms
2. Cloud sync for blocked numbers
3. Premium features (freemium model)
4. Export/Import functionality
5. Advanced settings and customization

## 🚫 Turkish Spam Keywords (Initial Set):
```java
private static final String[] GAMBLING_KEYWORDS = {
    "bahis", "kumar", "bet", "casino", "bonus", 
    "freespin", "çevrim", "yatır", "kazanç", "slot",
    "rulet", "poker", "jackpot", "bedava", "para kazan"
};
```

## 🔒 Security & Compliance

- KVKV (Turkish GDPR) compliance for data handling
- Secure local storage with encryption
- No sensitive data logging
- Proper user consent flows

## 🧪 Testing Strategy

- Unit tests for spam detection logic
- Integration tests for SMS operations
- UI tests for critical user flows
- Manual testing on different Android versions

## 📝 Code Style Guidelines

- Follow Google Java Style Guide
- Use descriptive variable and method names
- Add comprehensive Javadoc comments
- Implement proper error handling
- Use constants for magic numbers and strings

---

**Important Notes for Claude Code:**
- Always check for null values when dealing with SMS data
- Handle SMS permissions gracefully with fallback options
- Implement proper lifecycle management for SMS operations
- Use WorkManager for background operations if needed
- Follow Material Design 3 guidelines strictly for UI components
- **COMMIT AFTER EACH WORKING FEATURE** - use conventional commit format
- Document progress for seamless continuation with regular Cursor

## 🔄 Development Workflow & Continuation Strategy

### **Git Workflow:**
```bash
# After completing each feature:
feat(sms): add basic SMS reading functionality
feat(ui): implement Material Design 3 main dashboard  
feat(db): setup Room database with spam detection entities
fix(permissions): handle Android 13 runtime permission edge cases
ui(theme): add dark/light theme support with dynamic colors
```

### **Progress Tracking:**
- ✅ **Phase 1 MVP**: Default SMS app + basic deletion + spam detection
- 📋 **Phase 2 Future**: Cloud sync + premium features + advanced AI
- 🎯 **Current Focus**: [Update based on current development status]

### **Continuation Without Claude Code:**
This documentation enables **seamless development continuation** with:
- Regular Cursor autocompletion + architecture context
- Any other AI tool (ChatGPT, Codeium, GitHub Copilot)
- Traditional development using comprehensive code examples
- Team collaboration with complete technical specifications

**Zero Interruption Development** - Documentation-driven approach works with any development environment! 🚀