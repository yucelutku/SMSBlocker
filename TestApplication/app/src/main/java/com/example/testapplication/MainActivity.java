package com.example.testapplication;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.testapplication.databinding.ActivityMainBinding;
import com.example.testapplication.viewmodels.SmsViewModel;
import com.example.testapplication.utils.PermissionHelper;
import com.example.testapplication.utils.SmsHelper;
import com.example.testapplication.adapters.SmsListAdapter;
import com.example.testapplication.models.SmsMessage;

import android.Manifest;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SmsViewModel smsViewModel;
    private SmsListAdapter smsAdapter;
    private static final int SMS_PERMISSION_REQUEST_CODE = 100;
    private static final int DEFAULT_SMS_APP_REQUEST_CODE = 101;
    private static final int CONTACTS_PERMISSION_REQUEST_CODE = 102;
    
    private android.os.Handler permissionCheckHandler;
    private android.database.ContentObserver smsContentObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setupViewModel();
        setupToolbar();
        setupRecyclerView();
        setupUI();
        setupObservers();
        setupAutoRefresh();
        checkPermissions();
    }

    private void setupAutoRefresh() {
        permissionCheckHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        
        smsContentObserver = new android.database.ContentObserver(new android.os.Handler(android.os.Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                android.util.Log.d("MainActivity", "[SMS_OBSERVER] SMS content changed, refreshing list");
                if (smsViewModel != null) {
                    smsViewModel.refreshData();
                }
            }
        };
        
        try {
            getContentResolver().registerContentObserver(
                android.provider.Telephony.Sms.CONTENT_URI,
                true,
                smsContentObserver
            );
            android.util.Log.d("MainActivity", "[SMS_OBSERVER] Registered SMS content observer");
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "[SMS_OBSERVER] Failed to register observer", e);
        }
        
        startPermissionMonitoring();
    }

    private void startPermissionMonitoring() {
        permissionCheckHandler.post(new Runnable() {
            @Override
            public void run() {
                updatePermissionStatus();
                permissionCheckHandler.postDelayed(this, 3000);
            }
        });
    }

    private void updatePermissionStatus() {
        boolean hasSms = PermissionHelper.hasSmsPermissions(this);
        boolean isDefault = PermissionHelper.isDefaultSmsApp(this);
        boolean hasContacts = com.example.testapplication.utils.ContactsHelper.hasContactsPermission(this);
        
        updateProtectionStatus(hasSms && isDefault);
    }

    private void setupViewModel() {
        smsViewModel = new ViewModelProvider(this).get(SmsViewModel.class);
    }

    private void setupRecyclerView() {
        smsAdapter = new SmsListAdapter();
        binding.smsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.smsRecyclerView.setAdapter(smsAdapter);
        
        // Set up adapter listeners
        smsAdapter.setOnSmsActionListener(new SmsListAdapter.OnSmsActionListener() {
            @Override
            public void onDeleteMessage(SmsMessage message) {
                showDeleteConfirmationDialog(message);
            }

            @Override
            public void onMessageClick(SmsMessage message) {
                showMessageDetails(message);
            }
        });
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }
    }

    private void setupUI() {
        binding.enableProtectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestSmsPermissions();
            }
        });

        binding.viewMessagesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PermissionHelper.hasSmsPermissions(MainActivity.this)) {
                    refreshData();
                    showToast("📥 Mesajlar yenileniyor...");
                } else {
                    showToast("SMS izinleri gerekli");
                }
            }
        });

        binding.blockedNumbersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PermissionHelper.hasSmsPermissions(MainActivity.this)) {
                    smsViewModel.loadSpamMessages();
                    showToast("🚫 Spam mesajlar yükleniyor...");
                } else {
                    showToast("SMS izinleri gerekli");
                }
            }
        });

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PermissionHelper.hasSmsPermissions(MainActivity.this)) {
                    smsViewModel.deleteAllSpamMessages(deletedCount -> {
                        if (deletedCount > 0) {
                            showToast("🗑️ " + deletedCount + " spam mesaj silindi");
                            refreshData();
                        } else {
                            showToast("Silinecek spam mesaj yok");
                        }
                    });
                } else {
                    showToast("Ayarlar - Yakında!");
                }
            }
        });
    }

    private void refreshData() {
        android.util.Log.d("MainActivity", "[REFRESH] Manual refresh triggered");
        if (smsViewModel != null) {
            smsViewModel.refreshData();
        }
        updatePermissionStatus();
    }

    private void setupObservers() {
        // Observe statistics for UI updates
        smsViewModel.getStatistics().observe(this, stats -> {
            if (stats != null) {
                binding.totalMessagesCount.setText(String.valueOf(stats.totalMessages));
                binding.spamBlockedCount.setText(String.valueOf(stats.spamMessages));
            }
        });

        // Observe status text
        smsViewModel.getStatusText().observe(this, statusText -> {
            if (statusText != null) {
                binding.recentActivityText.setText(statusText);
            }
        });

        // Observe loading state
        smsViewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading != null) {
                binding.loadingIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });

        // Observe SMS messages for RecyclerView
        smsViewModel.getAllMessages().observe(this, messages -> {
            if (messages != null) {
                smsAdapter.submitList(messages);
                updateEmptyState(messages.isEmpty());
            }
        });

        // Observe error messages
        smsViewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                showToast("Error: " + errorMessage);
            }
        });
    }

    private void checkPermissions() {
        if (PermissionHelper.hasSmsPermissions(this)) {
            updateProtectionStatus(true);
            smsViewModel.refreshData();
            
            if (!com.example.testapplication.utils.ContactsHelper.hasContactsPermission(this)) {
                android.util.Log.d("MainActivity", "[PERM_CHECK] SMS granted but contacts permission missing");
            }
        } else {
            updateProtectionStatus(false);
        }
    }

    private void requestSmsPermissions() {
        android.util.Log.d("MainActivity", "[PERM_REQUEST] Permission request initiated");
        
        if (!PermissionHelper.hasSmsPermissions(this)) {
            android.util.Log.i("MainActivity", "[PERM_REQUEST] SMS permissions not granted, requesting...");
            
            showToast("📱 Requesting SMS permissions...");
            
            ActivityCompat.requestPermissions(this, PermissionHelper.SMS_PERMISSIONS, SMS_PERMISSION_REQUEST_CODE);
            
            android.util.Log.d("MainActivity", "[PERM_REQUEST] Permission dialog launched");
            
        } else if (!PermissionHelper.isDefaultSmsApp(this)) {
            android.util.Log.i("MainActivity", "[PERM_REQUEST] SMS permissions granted, but not default SMS app");
            showDefaultSmsAppDialog();
            
        } else {
            android.util.Log.i("MainActivity", "[PERM_REQUEST] All permissions granted and default SMS app set");
            showToast("✓ SMS permissions already granted");
        }
    }

    private void showDefaultSmsAppDialog() {
        android.util.Log.d("MainActivity", "[DEFAULT_APP_DIALOG] Showing improved default SMS app dialog");
        
        new AlertDialog.Builder(this)
                .setTitle("SMS Uygulaması Olarak Ayarla")
                .setMessage("SMS silme özelliği için bu uygulamanın varsayılan SMS uygulaması olması gerekiyor.\n\n" +
                           "Açılacak ayarlar sayfasında:\n" +
                           "1. 'SMS uygulaması' seçeneğine dokunun\n" +
                           "2. 'TestApplication' ı seçin\n\n" +
                           "Not: İstediğiniz zaman eski SMS uygulamanıza geri dönebilirsiniz.")
                .setPositiveButton("Ayarları Aç", (dialog, which) -> {
                    android.util.Log.i("MainActivity", "[DEFAULT_APP_DIALOG] Opening SMS app settings directly");
                    openSmsAppSettings();
                })
                .setNegativeButton("İptal", (dialog, which) -> {
                    android.util.Log.w("MainActivity", "[DEFAULT_APP_DIALOG] User cancelled");
                    showToast("⚠️ SMS silme özelliği varsayılan uygulama olmadan çalışmaz");
                })
                .setCancelable(false)
                .show();
    }

    private void openSmsAppSettings() {
        android.util.Log.d("MainActivity", "[SMS_SETTINGS] Attempting to open SMS app settings");
        
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
            startActivityForResult(intent, DEFAULT_SMS_APP_REQUEST_CODE);
            
            android.util.Log.i("MainActivity", "[SMS_SETTINGS] Opened default apps settings");
            showToast("📱 Ayarlar açıldı - SMS uygulaması seçeneğini bulun");
            
        } catch (android.content.ActivityNotFoundException e) {
            android.util.Log.w("MainActivity", "[SMS_SETTINGS] Default apps settings not available, trying alternative");
            
            try {
                Intent intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
                startActivityForResult(intent, DEFAULT_SMS_APP_REQUEST_CODE);
                
                android.util.Log.i("MainActivity", "[SMS_SETTINGS] Opened general settings");
                showToast("⚙️ Ayarlar > Uygulamalar > Varsayılan uygulamalar > SMS uygulaması");
                
            } catch (Exception e2) {
                android.util.Log.e("MainActivity", "[SMS_SETTINGS] Failed to open settings, falling back", e2);
                
                PermissionHelper.requestDefaultSmsApp(this, DEFAULT_SMS_APP_REQUEST_CODE);
                showToast("📲 SMS uygulama seçicisi açılıyor...");
            }
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "[SMS_SETTINGS] Unexpected error opening settings", e);
            
            PermissionHelper.requestDefaultSmsApp(this, DEFAULT_SMS_APP_REQUEST_CODE);
            showToast("📲 SMS uygulama seçicisi açılıyor...");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == CONTACTS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                android.util.Log.i("MainActivity", "[CONTACTS_PERM] Contacts permission granted");
                showToast("✓ Kişi isimleri gösterilecek");
                smsViewModel.refreshData();
            } else {
                android.util.Log.w("MainActivity", "[CONTACTS_PERM] Contacts permission denied");
                showToast("Telefon numaraları gösterilecek");
            }
            finalizeSetup();
        } else if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            android.util.Log.d("MainActivity", "[PERM_RESULT] Permission request result received");
            android.util.Log.d("MainActivity", "[PERM_RESULT] Permissions count: " + permissions.length);
            
            boolean allPermissionsGranted = true;
            java.util.List<String> deniedPermissions = new java.util.ArrayList<>();
            
            for (int i = 0; i < permissions.length; i++) {
                boolean granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                android.util.Log.d("MainActivity", "[PERM_RESULT] " + permissions[i] + ": " + (granted ? "GRANTED" : "DENIED"));
                
                if (!granted) {
                    allPermissionsGranted = false;
                    deniedPermissions.add(permissions[i]);
                }
            }
            
            if (allPermissionsGranted) {
                android.util.Log.i("MainActivity", "[PERM_RESULT] ✓ All SMS permissions granted successfully");
                showToast("✓ SMS izinleri verildi!");
                
                if (!PermissionHelper.isDefaultSmsApp(this)) {
                    android.util.Log.i("MainActivity", "[PERM_RESULT] Proceeding to default SMS app request");
                    showDefaultSmsAppDialog();
                } else {
                    android.util.Log.i("MainActivity", "[PERM_RESULT] Already default SMS app, requesting contacts");
                    requestContactsPermission();
                }
            } else {
                android.util.Log.e("MainActivity", "[PERM_RESULT] ✗ Some permissions denied: " + deniedPermissions);
                updateProtectionStatus(false);
                
                String deniedList = String.join(", ", deniedPermissions);
                showToast("✗ Denied permissions: " + deniedList);
                
                new AlertDialog.Builder(this)
                        .setTitle("Permissions Required")
                        .setMessage("The following permissions were denied:\n\n" + deniedList + 
                                   "\n\nThis app requires all SMS permissions to function properly. " +
                                   "Please grant all permissions.")
                        .setPositiveButton("Retry", (dialog, which) -> requestSmsPermissions())
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == DEFAULT_SMS_APP_REQUEST_CODE) {
            android.util.Log.d("MainActivity", "[DEFAULT_RESULT] onActivityResult called for default SMS app request");
            android.util.Log.d("MainActivity", "[DEFAULT_RESULT] Result code: " + resultCode);
            
            new android.os.Handler().postDelayed(() -> {
                boolean isDefault = PermissionHelper.isDefaultSmsApp(this);
                android.util.Log.d("MainActivity", "[DEFAULT_RESULT] Checked default status (after delay): " + isDefault);
                
                if (isDefault) {
                    android.util.Log.i("MainActivity", "[DEFAULT_RESULT] ✓ Successfully became default SMS app");
                    updateProtectionStatus(true);
                    smsViewModel.refreshData();
                    showToast("✓ App is now default SMS app. Full functionality enabled!");
                    requestContactsPermission();
                } else {
                    android.util.Log.w("MainActivity", "[DEFAULT_RESULT] ✗ Not default SMS app after selection");
                    
                    String currentDefault = "";
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                        currentDefault = android.provider.Telephony.Sms.getDefaultSmsPackage(this);
                        android.util.Log.w("MainActivity", "[DEFAULT_RESULT] Current default SMS app: " + currentDefault);
                    }
                    
                    showToast("✗ Varsayılan SMS uygulaması ayarlanmadı");
                    
                    new AlertDialog.Builder(this)
                            .setTitle("Kurulum Tamamlanmadı")
                            .setMessage("Bu uygulama varsayılan SMS uygulaması olarak ayarlanmadı.\n\n" +
                                       "Şu anki varsayılan: " + currentDefault + "\n\n" +
                                       "SMS silme özelliği için bu uygulamanın varsayılan olması gerekiyor.")
                            .setPositiveButton("Tekrar Dene", (dialog, which) -> showDefaultSmsAppDialog())
                            .setNegativeButton("Atla", (dialog, which) -> requestContactsPermission())
                            .show();
                }
            }, 500);
        }
    }

    private void requestContactsPermission() {
        if (!com.example.testapplication.utils.ContactsHelper.hasContactsPermission(this)) {
            android.util.Log.d("MainActivity", "[CONTACTS_PERM] Requesting contacts permission");
            
            new AlertDialog.Builder(this)
                    .setTitle("Kişi İsimleri")
                    .setMessage("SMS listesinde telefon numaraları yerine kişi isimlerini görmek ister misiniz?\n\n" +
                               "Bu isteğe bağlıdır ve reddedebilirsiniz.")
                    .setPositiveButton("İzin Ver", (dialog, which) -> {
                        ActivityCompat.requestPermissions(this, 
                            new String[]{Manifest.permission.READ_CONTACTS}, 
                            CONTACTS_PERMISSION_REQUEST_CODE);
                    })
                    .setNegativeButton("Hayır", (dialog, which) -> {
                        android.util.Log.d("MainActivity", "[CONTACTS_PERM] User declined contacts permission");
                        finalizeSetup();
                    })
                    .show();
        } else {
            android.util.Log.d("MainActivity", "[CONTACTS_PERM] Contacts permission already granted");
            finalizeSetup();
        }
    }

    private void finalizeSetup() {
        updateProtectionStatus(true);
        smsViewModel.refreshData();
        
        if (com.example.testapplication.utils.ContactsHelper.hasContactsPermission(this)) {
            showToast("✓ Tüm izinler verildi! Koruma aktif.");
        } else {
            showToast("✓ SMS koruması aktif (kişi isimleri olmadan)");
        }
    }

    private void updateProtectionStatus(boolean isProtected) {
        if (isProtected) {
            boolean isDefault = PermissionHelper.isDefaultSmsApp(this);
            String statusMsg = "Protection is active - SMS spam detection enabled";
            if (isDefault) {
                statusMsg += "\n✓ Default SMS App: Full deletion enabled";
            } else {
                statusMsg += "\n⚠ Not default SMS app: Deletion disabled";
            }
            binding.statusText.setText(statusMsg);
            binding.statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            binding.enableProtectionButton.setText("Protection Enabled");
            binding.enableProtectionButton.setEnabled(false);
        } else {
            binding.statusText.setText("SMS permissions not granted - Protection disabled");
            binding.statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark));
            binding.enableProtectionButton.setText("Enable Protection");
            binding.enableProtectionButton.setEnabled(true);
        }
    }


    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            binding.emptyStateLayout.setVisibility(View.VISIBLE);
            binding.smsRecyclerView.setVisibility(View.GONE);
            
            if (!PermissionHelper.hasSmsPermissions(this)) {
                binding.emptyStateText.setText("SMS permissions not granted");
            } else {
                binding.emptyStateText.setText("No SMS messages found");
            }
        } else {
            binding.emptyStateLayout.setVisibility(View.GONE);
            binding.smsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showDeleteConfirmationDialog(SmsMessage message) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Message")
                .setMessage("Are you sure you want to delete this message from " + 
                           message.getSenderName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteMessage(message);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteMessage(SmsMessage message) {
        android.util.Log.d("MainActivity", "[UI] Delete requested for message ID: " + message.id);
        
        if (!PermissionHelper.isDefaultSmsApp(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("Cannot Delete")
                    .setMessage("This app must be set as the default SMS app to delete messages.\n\nWould you like to set it as default now?")
                    .setPositiveButton("Set as Default", (dialog, which) -> {
                        PermissionHelper.requestDefaultSmsApp(this, DEFAULT_SMS_APP_REQUEST_CODE);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }
        
        smsViewModel.deleteMessage(message.id, success -> {
            if (success) {
                showToast("✓ Message deleted successfully");
                android.util.Log.i("MainActivity", "[UI] Delete SUCCESS for message ID: " + message.id);
                smsViewModel.refreshData();
            } else {
                String errorMsg = "Failed to delete message";
                if (!PermissionHelper.isDefaultSmsApp(this)) {
                    errorMsg += ": Not default SMS app";
                }
                showToast("✗ " + errorMsg);
                android.util.Log.e("MainActivity", "[UI] Delete FAILED for message ID: " + message.id);
            }
        });
    }

    private void showMessageDetails(SmsMessage message) {
        String details = "From: " + message.getSenderName() + "\n" +
                        "Date: " + message.getFormattedDate() + "\n" +
                        "Type: " + (message.isInbox() ? "Inbox" : "Sent") + "\n";
        
        if (message.isSpam) {
            details += "Spam Score: " + String.format("%.0f%%", message.spamScore * 100) + "\n" +
                      "Spam Reason: " + message.spamReason + "\n";
        }
        
        details += "\nMessage:\n" + message.body;

        new AlertDialog.Builder(this)
                .setTitle("Message Details")
                .setMessage(details)
                .setPositiveButton("Close", null)
                .setNeutralButton("Delete", (dialog, which) -> {
                    showDeleteConfirmationDialog(message);
                })
                .show();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        if (permissionCheckHandler != null) {
            permissionCheckHandler.removeCallbacksAndMessages(null);
        }
        
        if (smsContentObserver != null) {
            try {
                getContentResolver().unregisterContentObserver(smsContentObserver);
                android.util.Log.d("MainActivity", "[SMS_OBSERVER] Unregistered SMS content observer");
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "[SMS_OBSERVER] Error unregistering observer", e);
            }
        }
        
        binding = null;
    }
}