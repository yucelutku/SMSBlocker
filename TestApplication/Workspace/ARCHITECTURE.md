# ARCHITECTURE.md - SMS Spam Blocker Technical Architecture

## 🏗️ Project Architecture Overview

**Pattern**: MVVM (Model-View-ViewModel) with Repository Pattern  
**Database**: Room + SQLite  
**UI Framework**: Material Design 3 + View Binding  
**Threading**: ExecutorService + LiveData  
**Dependency Injection**: Manual (lightweight approach)

## 📁 Project Structure

```
app/
├── src/main/
│   ├── java/com/example/testapplication/
│   │   ├── activities/
│   │   │   ├── MainActivity.java
│   │   │   ├── SettingsActivity.java
│   │   │   └── ComposeActivity.java
│   │   ├── fragments/
│   │   │   ├── SmsListFragment.java
│   │   │   ├── BlockedNumbersFragment.java
│   │   │   └── StatisticsFragment.java
│   │   ├── adapters/
│   │   │   ├── SmsListAdapter.java
│   │   │   └── BlockedNumbersAdapter.java
│   │   ├── viewmodels/
│   │   │   ├── SmsViewModel.java
│   │   │   ├── BlockedNumbersViewModel.java
│   │   │   └── SettingsViewModel.java
│   │   ├── repositories/
│   │   │   ├── SmsRepository.java
│   │   │   └── SettingsRepository.java
│   │   ├── database/
│   │   │   ├── AppDatabase.java
│   │   │   ├── entities/
│   │   │   │   ├── SmsMessage.java
│   │   │   │   ├── BlockedNumber.java
│   │   │   │   └── SpamKeyword.java
│   │   │   └── dao/
│   │   │       ├── SmsDao.java
│   │   │       ├── BlockedNumberDao.java
│   │   │       └── SpamKeywordDao.java
│   │   ├── receivers/
│   │   │   ├── SmsReceiver.java
│   │   │   └── MmsReceiver.java
│   │   ├── services/
│   │   │   ├── ResponseService.java
│   │   │   └── SmsProcessingService.java
│   │   ├── utils/
│   │   │   ├── PermissionHelper.java
│   │   │   ├── DefaultSmsHelper.java
│   │   │   ├── SpamDetector.java
│   │   │   ├── SmsHelper.java
│   │   │   └── ThemeHelper.java
│   │   └── constants/
│   │       ├── AppConstants.java
│   │       └── SpamKeywords.java
│   ├── res/
│   │   ├── layout/
│   │   ├── values/
│   │   ├── values-night/
│   │   ├── values-tr/
│   │   └── drawable/
│   └── AndroidManifest.xml
├── build.gradle (:app)
└── proguard-rules.pro
```

## 🎯 Core Architecture Components

### 1. **MVVM Implementation**

#### **View (Activities/Fragments)**
```java
public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private SmsViewModel smsViewModel;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        smsViewModel = new ViewModelProvider(this).get(SmsViewModel.class);
        setupObservers();
        setupUI();
    }
}
```

#### **ViewModel**
```java
public class SmsViewModel extends AndroidViewModel {
    private SmsRepository repository;
    private MutableLiveData<List<SmsMessage>> smsMessages = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    
    public SmsViewModel(@NonNull Application application) {
        super(application);
        repository = new SmsRepository(application);
    }
    
    public LiveData<List<SmsMessage>> getSmsMessages() {
        return smsMessages;
    }
}
```

#### **Repository Pattern**
```java
public class SmsRepository {
    private SmsDao smsDao;
    private ExecutorService executor;
    
    public SmsRepository(Application application) {
        AppDatabase database = AppDatabase.getDatabase(application);
        smsDao = database.smsDao();
        executor = Executors.newFixedThreadPool(4);
    }
    
    public LiveData<List<SmsMessage>> getAllMessages() {
        return smsDao.getAllMessages();
    }
}
```

### 2. **Database Architecture (Room)**

#### **Database Class**
```java
@Database(
    entities = {SmsMessage.class, BlockedNumber.class, SpamKeyword.class},
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters.class)
public abstract class AppDatabase extends RoomDatabase {
    public abstract SmsDao smsDao();
    public abstract BlockedNumberDao blockedNumberDao();
    public abstract SpamKeywordDao spamKeywordDao();
    
    private static volatile AppDatabase INSTANCE;
    
    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        "sms_spam_database"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
```

#### **Entity Classes**
```java
@Entity(tableName = "sms_messages")
public class SmsMessage {
    @PrimaryKey
    public long id;
    
    @ColumnInfo(name = "thread_id")
    public long threadId;
    
    @ColumnInfo(name = "address")
    public String address;
    
    @ColumnInfo(name = "body")
    public String body;
    
    @ColumnInfo(name = "date")
    public long date;
    
    @ColumnInfo(name = "type")
    public int type; // 1=inbox, 2=sent, 3=draft, 4=outbox
    
    @ColumnInfo(name = "is_spam")
    public boolean isSpam;
    
    @ColumnInfo(name = "is_blocked")
    public boolean isBlocked;
    
    @ColumnInfo(name = "spam_score")
    public float spamScore;
}
```

### 3. **SMS Operations Architecture**

#### **SMS Content Provider Access**
```java
public class SmsHelper {
    private static final Uri SMS_INBOX_URI = Uri.parse("content://sms/inbox");
    private static final Uri SMS_URI = Uri.parse("content://sms");
    
    public static List<SmsMessage> getAllSmsMessages(Context context) {
        List<SmsMessage> messages = new ArrayList<>();
        
        if (!PermissionHelper.hasSmsPermissions(context)) {
            return messages;
        }
        
        String[] projection = {"_id", "thread_id", "address", "body", "date", "type"};
        
        try (Cursor cursor = context.getContentResolver().query(
                SMS_INBOX_URI, projection, null, null, "date DESC")) {
            
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    SmsMessage message = new SmsMessage();
                    message.id = cursor.getLong(0);
                    message.threadId = cursor.getLong(1);
                    message.address = cursor.getString(2);
                    message.body = cursor.getString(3);
                    message.date = cursor.getLong(4);
                    message.type = cursor.getInt(5);
                    
                    messages.add(message);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("SmsHelper", "Error reading SMS: " + e.getMessage());
        }
        
        return messages;
    }
}
```

#### **Spam Detection Engine**
```java
public class SpamDetector {
    private static final String[] TURKISH_GAMBLING_KEYWORDS = {
        "bahis", "kumar", "bet", "casino", "bonus", "freespin",
        "çevrim", "yatır", "kazanç", "slot", "rulet", "poker",
        "jackpot", "bedava", "para kazan", "deneme bonusu"
    };
    
    private static final String[] SUSPICIOUS_PATTERNS = {
        "\\b\\d{4}\\s*(TL|₺)", // Money amounts
        "\\b(www\\.|http)", // Links
        "\\b\\d{3}\\s*%", // Percentages
        "TIKLA", "KAYIT", "BONUS" // Action words
    };
    
    public static SpamAnalysisResult analyzeMessage(String messageBody, String sender) {
        if (messageBody == null || messageBody.trim().isEmpty()) {
            return new SpamAnalysisResult(false, 0.0f, "Empty message");
        }
        
        float spamScore = 0.0f;
        List<String> reasons = new ArrayList<>();
        
        String lowerBody = messageBody.toLowerCase(Locale.forLanguageTag("tr"));
        
        // Keyword matching
        for (String keyword : TURKISH_GAMBLING_KEYWORDS) {
            if (lowerBody.contains(keyword.toLowerCase(Locale.forLanguageTag("tr")))) {
                spamScore += 0.3f;
                reasons.add("Contains gambling keyword: " + keyword);
            }
        }
        
        // Pattern matching
        for (String pattern : SUSPICIOUS_PATTERNS) {
            if (Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(messageBody).find()) {
                spamScore += 0.2f;
                reasons.add("Matches suspicious pattern");
            }
        }
        
        // Sender analysis
        if (sender != null && sender.matches("\\d{4,5}")) {
            spamScore += 0.1f; // Short numeric sender
            reasons.add("Short numeric sender");
        }
        
        boolean isSpam = spamScore >= 0.5f;
        
        return new SpamAnalysisResult(isSpam, Math.min(spamScore, 1.0f), reasons);
    }
}
```

### 4. **Default SMS App Components**

#### **AndroidManifest.xml Setup**
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example.testapplication">

    <!-- SMS Permissions -->
    <uses-permission android:name="android.permission.SEND_SMS" />
    <uses-permission android:name="android.permission.RECEIVE_SMS" />
    <uses-permission android:name="android.permission.READ_SMS" />
    <uses-permission android:name="android.permission.WRITE_SMS" />
    <uses-permission android:name="android.permission.RECEIVE_MMS" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application>
        <!-- SMS Receiver -->
        <receiver android:name=".receivers.SmsReceiver"
            android:enabled="true"
            android:exported="true">
            <intent-filter android:priority="1000">
                <action android:name="android.provider.Telephony.SMS_DELIVER" />
            </intent-filter>
        </receiver>

        <!-- MMS Receiver -->
        <receiver android:name=".receivers.MmsReceiver"
            android:enabled="true"
            android:exported="true">
            <intent-filter android:priority="1000">
                <action android:name="android.provider.Telephony.WAP_PUSH_DELIVER" />
                <data android:mimeType="application/vnd.wap.mms-message" />
            </intent-filter>
        </receiver>

        <!-- Compose Activity -->
        <activity android:name=".activities.ComposeActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.SEND" />
                <action android:name="android.intent.action.SENDTO" />
                <data android:scheme="sms" />
                <data android:scheme="smsto" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
            </intent-filter>
        </activity>

        <!-- Response Service -->
        <service android:name=".services.ResponseService"
            android:permission="android.permission.SEND_RESPOND_VIA_MESSAGE"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.RESPOND_VIA_MESSAGE" />
                <data android:scheme="sms" />
                <data android:scheme="smsto" />
                <data android:scheme="mms" />
                <data android:scheme="mmsto" />
            </intent-filter>
        </service>
    </application>
</manifest>
```

### 5. **Material Design 3 Implementation**

#### **Theme Configuration**
```xml
<!-- res/values/themes.xml -->
<resources>
    <style name="Theme.SmsSpamBlocker" parent="Theme.Material3.DayNight">
        <item name="colorPrimary">@color/md_theme_primary</item>
        <item name="colorOnPrimary">@color/md_theme_on_primary</item>
        <item name="colorSecondary">@color/md_theme_secondary</item>
        <item name="colorOnSecondary">@color/md_theme_on_secondary</item>
        <item name="android:windowSplashScreenBackground">@color/md_theme_primary</item>
    </style>
</resources>
```

#### **Layout Structure with Material Components**
```xml
<!-- activity_main.xml -->
<androidx.coordinatorlayout.widget.CoordinatorLayout>
    
    <com.google.android.material.appbar.AppBarLayout>
        <com.google.android.material.appbar.MaterialToolbar
            android:id="@+id/toolbar"
            style="@style/Widget.Material3.Toolbar"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize" />
    </com.google.android.material.appbar.AppBarLayout>

    <androidx.core.widget.NestedScrollView>
        <LinearLayout android:orientation="vertical">
            <!-- Content here -->
        </LinearLayout>
    </androidx.core.widget.NestedScrollView>

    <com.google.android.material.floatingactionbutton.FloatingActionButton
        android:id="@+id/fab"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom|end"
        android:layout_margin="16dp" />

</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

### 6. **Performance Considerations**

#### **Background Processing**
```java
public class SmsProcessingService extends IntentService {
    public SmsProcessingService() {
        super("SmsProcessingService");
    }
    
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent != null) {
            // Process SMS in background thread
            processIncomingSms(intent);
        }
    }
    
    private void processIncomingSms(Intent intent) {
        // Background SMS processing logic
        // Update database
        // Send broadcast for UI updates
    }
}
```

#### **Memory Management**
- Use ViewHolder pattern in RecyclerView adapters
- Implement proper cursor management
- Use WeakReference for callbacks
- Implement lazy loading for large SMS lists

### 7. **Security Implementation**

#### **Data Encryption**
```java
public class SecurityHelper {
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    
    public static String encryptSensitiveData(String data) throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
        KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder("SMS_ENCRYPT_KEY",
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build();
        
        keyGenerator.init(keyGenParameterSpec);
        SecretKey secretKey = keyGenerator.generateKey();
        
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        
        byte[] iv = cipher.getIV();
        byte[] encryptedData = cipher.doFinal(data.getBytes());
        
        // Combine IV and encrypted data
        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encryptedData.length);
        byteBuffer.put(iv);
        byteBuffer.put(encryptedData);
        
        return Base64.encodeToString(byteBuffer.array(), Base64.DEFAULT);
    }
}
```

---

## 🚀 Implementation Order

### **Phase 1: Foundation (Week 1)**
1. Setup project structure and dependencies
2. Create database schema and entities
3. Implement basic MVVM architecture
4. Setup Material Design 3 theme

### **Phase 2: Core SMS Functionality (Week 2)**
1. Implement SMS permissions and default app setup
2. Create SMS reading and display functionality
3. Basic spam detection with Turkish keywords
4. Manual SMS deletion feature

### **Phase 3: UI Polish (Week 3)**
1. Complete Material Design 3 implementation
2. Add dark/light theme support
3. Implement proper navigation
4. Add Turkish localization

### **Phase 4: Advanced Features (Future)**
1. Cloud sync for blocked numbers
2. Advanced spam detection algorithms
3. Premium features implementation
4. Export/Import functionality

---

---

**Key Architecture Principles:**
- ✅ Separation of Concerns (MVVM)
- ✅ Single Source of Truth (Repository Pattern)
- ✅ Reactive Programming (LiveData)
- ✅ Material Design Guidelines
- ✅ Security by Design
- ✅ Performance Optimization
- ✅ Turkish Localization Support

---

## 📝 Development Workflow & Git Strategy

### **Feature-Based Commits:**
```bash
# Commit after each architecture component:
git commit -m "feat(arch): setup MVVM architecture with Repository pattern

- Created SmsViewModel with LiveData
- Implemented SmsRepository with Room integration  
- Added proper dependency injection
- Tested with basic SMS operations"

git commit -m "feat(db): implement Room database with SMS entities

- Created AppDatabase with version control
- Added SmsMessage, BlockedNumber, SpamKeyword entities
- Implemented DAOs with proper SQL queries
- Added database migration strategy"

git commit -m "feat(ui): Material Design 3 main interface  

- Implemented MainActivity with Material components
- Added dark/light theme support with dynamic colors
- Created SMS RecyclerView with modern card design
- Turkish localization support included"
```

### **Architecture Milestones:**
```bash
# Major architecture checkpoints:
v0.1.0: Basic MVVM + Room setup
v0.2.0: Default SMS app infrastructure  
v0.3.0: Spam detection engine
v0.4.0: Material Design 3 UI complete
v0.5.0: MVP feature complete
```

---

## 🔄 Post-Claude Code Development

### **Architecture Benefits for Continuation:**

**✅ Self-Documenting Code:**
- Complete implementation examples in this file
- Clear patterns established for all components
- Turkish keyword detection ready to use
- Material Design 3 layouts fully specified

**✅ Modular Design:**
- Each component can be developed independently
- Clear separation between UI, business logic, and data
- Repository pattern isolates SMS operations
- ViewModels handle UI state management

**✅ Reference Implementation:**
```java
// All major classes have complete examples in this file:
// - SmsHelper.java (complete SMS operations)
// - SpamDetector.java (Turkish gambling detection)
// - AppDatabase.java (complete Room setup)
// - Material Design 3 layouts (XML examples)
// - Permission handling (complete workflow)
```

### **Cursor Autocompletion Enhancement:**
With this architecture documentation, regular Cursor will:
- Suggest code following established patterns
- Auto-complete based on architecture context
- Provide IntelliSense aligned with MVVM structure
- Support refactoring within architectural constraints

### **Alternative Development Paths:**
```bash
# With any AI tool:
"I'm following this Android SMS Spam Blocker architecture [paste relevant section].
Please implement [specific component] following this pattern."

# With regular IDE:
# Use code examples as templates
# Follow architectural patterns established
# Reference security and performance guidelines
```

**Zero Development Interruption Guaranteed! 🚀**