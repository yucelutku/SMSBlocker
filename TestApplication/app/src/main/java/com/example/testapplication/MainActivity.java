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
import com.example.testapplication.utils.KeywordManager;
import com.example.testapplication.adapters.SmsListAdapter;
import com.example.testapplication.models.SmsMessage;
import com.example.testapplication.dialogs.BulkDeleteDialog;
import com.example.testapplication.repositories.SmsRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import android.Manifest;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SmsViewModel smsViewModel;
    private SmsListAdapter smsAdapter;
    private BulkDeleteDialog bulkDeleteDialog;
    private SmsRepository smsRepository;
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
        setupFilterChips();
        setupObservers();
        setupAutoRefresh();
        setupBulkDelete();
        checkPermissions();
    }

    private void setupAutoRefresh() {
        permissionCheckHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        
        smsContentObserver = new android.database.ContentObserver(new android.os.Handler(android.os.Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
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
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Failed to register SMS observer", e);
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
    
    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_bulk_delete) {
            // PERFORMANCE: Add timing measurement
            long startTime = System.currentTimeMillis();
            android.util.Log.d("PERFORMANCE", "🔍 Bulk delete button clicked at: " + startTime);
            
            if (bulkDeleteDialog != null) {
                bulkDeleteDialog.showMainDialog();
                
                // Log timing after dialog initiation
                long endTime = System.currentTimeMillis();
                android.util.Log.d("PERFORMANCE", "⚡ Dialog initiation took: " + (endTime - startTime) + "ms");
            }
            return true;
        } else if (id == R.id.action_refresh) {
            refreshData();
            showToast("📥 Mesajlar yenileniyor...");
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    private void setupFilterChips() {
        binding.filterChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            
            int checkedId = checkedIds.get(0);
            SmsViewModel.SmsFilter filter;
            
            if (checkedId == binding.filterAllChip.getId()) {
                filter = SmsViewModel.SmsFilter.ALL;
            } else if (checkedId == binding.filterSpamChip.getId()) {
                filter = SmsViewModel.SmsFilter.SPAM_ONLY;
            } else if (checkedId == binding.filterNormalChip.getId()) {
                filter = SmsViewModel.SmsFilter.NORMAL_ONLY;
            } else {
                return;
            }
            
            smsViewModel.setFilter(filter);
        });
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
                showCustomKeywordDialog();
            }
        });
    }
    
    private void setupBulkDelete() {
        smsRepository = SmsRepository.getInstance(this);
        bulkDeleteDialog = new BulkDeleteDialog(this, new BulkDeleteDialog.BulkDeleteListener() {
            @Override
            public void onDeleteAll() {
                performBulkDelete("all");
            }
            
            @Override
            public void onDeleteSpam() {
                performBulkDelete("spam");
            }
            
            @Override
            public void onDeleteNormal() {
                performBulkDelete("normal");
            }
            
            @Override
            public void onDeleteBySender(String phoneNumber) {
                performBulkDeleteBySender(phoneNumber);
            }
            
            @Override
            public void onDeleteByDateRange(long startDate, long endDate) {
                performBulkDeleteByDateRange(startDate, endDate);
            }
            
            @Override
            public void onDeleteByDateRangeAndType(long startDate, long endDate, boolean isSpam) {
                performBulkDeleteByDateRangeAndType(startDate, endDate, isSpam);
            }
        }, smsRepository);
    }
    
    private void performBulkDelete(String type) {
        if (!PermissionHelper.hasSmsPermissions(this)) {
            showToast("SMS izinleri gerekli");
            return;
        }
        
        bulkDeleteDialog.showProgressDialog();
        
        SmsRepository.BulkOperationCallback callback = new SmsRepository.BulkOperationCallback() {
            @Override
            public void onProgress(int current, int total) {
                runOnUiThread(() -> {
                    bulkDeleteDialog.updateProgress(current, total);
                });
            }
            
            @Override
            public void onCompleted(int deletedCount) {
                runOnUiThread(() -> {
                    bulkDeleteDialog.hideDialog();
                    showToast(getString(R.string.delete_completed, deletedCount));
                    refreshData(); // Refresh the message list
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    bulkDeleteDialog.hideDialog();
                    showToast("Hata: " + error);
                });
            }
        };
        
        switch (type) {
            case "all":
                smsRepository.deleteAllMessages(callback);
                break;
            case "spam":
                smsRepository.deleteAllSpamMessages(callback);
                break;
            case "normal":
                smsRepository.deleteAllNormalMessages(callback);
                break;
        }
    }
    
    private void performBulkDeleteBySender(String phoneNumber) {
        if (!PermissionHelper.hasSmsPermissions(this)) {
            showToast("SMS izinleri gerekli");
            return;
        }
        
        bulkDeleteDialog.showProgressDialog();
        
        smsRepository.deleteMessagesBySender(phoneNumber, new SmsRepository.BulkOperationCallback() {
            @Override
            public void onProgress(int current, int total) {
                runOnUiThread(() -> {
                    bulkDeleteDialog.updateProgress(current, total);
                });
            }
            
            @Override
            public void onCompleted(int deletedCount) {
                runOnUiThread(() -> {
                    bulkDeleteDialog.hideDialog();
                    showToast(getString(R.string.delete_completed, deletedCount));
                    refreshData();
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    bulkDeleteDialog.hideDialog();
                    showToast("Hata: " + error);
                });
            }
        });
    }
    
    private void performBulkDeleteByDateRange(long startDate, long endDate) {
        if (!PermissionHelper.hasSmsPermissions(this)) {
            showToast("SMS izinleri gerekli");
            return;
        }
        
        bulkDeleteDialog.showProgressDialog();
        
        smsRepository.deleteMessagesByDateRange(startDate, endDate, new SmsRepository.BulkOperationCallback() {
            @Override
            public void onProgress(int current, int total) {
                runOnUiThread(() -> {
                    bulkDeleteDialog.updateProgress(current, total);
                });
            }
            
            @Override
            public void onCompleted(int deletedCount) {
                runOnUiThread(() -> {
                    bulkDeleteDialog.hideDialog();
                    showToast(getString(R.string.delete_completed, deletedCount));
                    refreshData();
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    bulkDeleteDialog.hideDialog();
                    showToast("Hata: " + error);
                });
            }
        });
    }
    
    private void performBulkDeleteByDateRangeAndType(long startDate, long endDate, boolean isSpam) {
        if (!PermissionHelper.hasSmsPermissions(this)) {
            showToast("SMS izinleri gerekli");
            return;
        }
        
        bulkDeleteDialog.showProgressDialog();
        
        smsRepository.deleteMessagesByDateRangeAndType(startDate, endDate, isSpam, new SmsRepository.BulkOperationCallback() {
            @Override
            public void onProgress(int current, int total) {
                runOnUiThread(() -> {
                    bulkDeleteDialog.updateProgress(current, total);
                });
            }
            
            @Override
            public void onCompleted(int deletedCount) {
                runOnUiThread(() -> {
                    bulkDeleteDialog.hideDialog();
                    showToast(getString(R.string.delete_completed, deletedCount));
                    refreshData();
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    bulkDeleteDialog.hideDialog();
                    showToast("Hata: " + error);
                });
            }
        });
    }

    private void refreshData() {
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

        // Observe filtered SMS messages for RecyclerView
        smsViewModel.getFilteredMessages().observe(this, messages -> {
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
            
        } else {
            updateProtectionStatus(false);
        }
    }

    private void requestSmsPermissions() {
        if (!PermissionHelper.hasSmsPermissions(this)) {
            showToast("📱 Requesting SMS permissions...");
            ActivityCompat.requestPermissions(this, PermissionHelper.SMS_PERMISSIONS, SMS_PERMISSION_REQUEST_CODE);
        } else if (!PermissionHelper.isDefaultSmsApp(this)) {
            showDefaultSmsAppDialog();
        } else {
            showToast("✓ SMS permissions already granted");
        }
    }

    private void showDefaultSmsAppDialog() {
        new AlertDialog.Builder(this)
                .setTitle("SMS Uygulaması Olarak Ayarla")
                .setMessage("SMS silme özelliği için bu uygulamanın varsayılan SMS uygulaması olması gerekiyor.\n\n" +
                           "Açılacak ayarlar sayfasında:\n" +
                           "1. 'SMS uygulaması' seçeneğine dokunun\n" +
                           "2. 'TestApplication' ı seçin\n\n" +
                           "Not: İstediğiniz zaman eski SMS uygulamanıza geri dönebilirsiniz.")
                .setPositiveButton("Ayarları Aç", (dialog, which) -> openSmsAppSettings())
                .setNegativeButton("İptal", (dialog, which) -> showToast("⚠️ SMS silme özelliği varsayılan uygulama olmadan çalışmaz"))
                .setCancelable(false)
                .show();
    }

    private void openSmsAppSettings() {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
            startActivityForResult(intent, DEFAULT_SMS_APP_REQUEST_CODE);
            showToast("📱 Ayarlar açıldı - SMS uygulaması seçeneğini bulun");
        } catch (android.content.ActivityNotFoundException e) {
            try {
                Intent intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
                startActivityForResult(intent, DEFAULT_SMS_APP_REQUEST_CODE);
                showToast("⚙️ Ayarlar > Uygulamalar > Varsayılan uygulamalar > SMS uygulaması");
            } catch (Exception e2) {
                
                PermissionHelper.requestDefaultSmsApp(this, DEFAULT_SMS_APP_REQUEST_CODE);
                showToast("📲 SMS uygulama seçicisi açılıyor...");
            }
        } catch (Exception e) {
            PermissionHelper.requestDefaultSmsApp(this, DEFAULT_SMS_APP_REQUEST_CODE);
            showToast("📲 SMS uygulama seçicisi açılıyor...");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == CONTACTS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showToast("✓ Kişi isimleri gösterilecek");
                smsViewModel.refreshData();
            } else {
                showToast("Telefon numaraları gösterilecek");
            }
            finalizeSetup();
        } else if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            java.util.List<String> deniedPermissions = new java.util.ArrayList<>();
            
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    deniedPermissions.add(permissions[i]);
                }
            }
            
            if (allPermissionsGranted) {
                showToast("✓ SMS izinleri verildi!");
                
                if (!PermissionHelper.isDefaultSmsApp(this)) {
                    showDefaultSmsAppDialog();
                } else {
                    requestContactsPermission();
                }
            } else {
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
            new android.os.Handler().postDelayed(() -> {
                boolean isDefault = PermissionHelper.isDefaultSmsApp(this);
                
                if (isDefault) {
                    updateProtectionStatus(true);
                    smsViewModel.refreshData();
                    showToast("✓ App is now default SMS app. Full functionality enabled!");
                    requestContactsPermission();
                } else {
                    String currentDefault = "";
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                        currentDefault = android.provider.Telephony.Sms.getDefaultSmsPackage(this);
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
            new AlertDialog.Builder(this)
                    .setTitle("Kişi İsimleri")
                    .setMessage("SMS listesinde telefon numaraları yerine kişi isimlerini görmek ister misiniz?\n\n" +
                               "Bu isteğe bağlıdır ve reddedebilirsiniz.")
                    .setPositiveButton("İzin Ver", (dialog, which) -> {
                        ActivityCompat.requestPermissions(this, 
                            new String[]{Manifest.permission.READ_CONTACTS}, 
                            CONTACTS_PERMISSION_REQUEST_CODE);
                    })
                    .setNegativeButton("Hayır", (dialog, which) -> finalizeSetup())
                    .show();
        } else {
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
            } else {
                String errorMsg = "Failed to delete message";
                if (!PermissionHelper.isDefaultSmsApp(this)) {
                    errorMsg += ": Not default SMS app";
                }
                showToast("✗ " + errorMsg);
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
    
    /**
     * Show custom keyword management dialog
     */
    private void showCustomKeywordDialog() {
        KeywordManager keywordManager = KeywordManager.getInstance(this);
        
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("🔧 Özel Anahtar Kelimeler");
        
        // Create main options
        String[] options = {
            "➕ Yeni Kelime Ekle",
            "📋 Mevcut Kelimeleri Görüntüle (" + keywordManager.getCustomKeywordCount() + ")",
            "🗑️ Tüm Özel Kelimeleri Sil"
        };
        
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    showAddKeywordDialog();
                    break;
                case 1:
                    showKeywordListDialog();
                    break;
                case 2:
                    showClearKeywordsConfirmation();
                    break;
            }
        });
        
        builder.setNegativeButton("İptal", null);
        builder.show();
    }
    
    /**
     * Show add new keyword dialog
     */
    private void showAddKeywordDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Yeni Anahtar Kelime Ekle");
        
        // Create input layout
        TextInputLayout inputLayout = new TextInputLayout(this);
        inputLayout.setHint("Anahtar kelime girin");
        inputLayout.setBoxStyle(TextInputLayout.BOX_STYLE_OUTLINED);
        
        TextInputEditText editText = new TextInputEditText(inputLayout.getContext());
        editText.setSingleLine(true);
        inputLayout.addView(editText);
        
        // Set padding
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        inputLayout.setPadding(padding, padding, padding, 0);
        
        builder.setView(inputLayout);
        
        builder.setPositiveButton("Ekle", (dialog, which) -> {
            String keyword = editText.getText().toString().trim();
            addCustomKeyword(keyword);
        });
        
        builder.setNegativeButton("İptal", null);
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        // Focus on input
        editText.requestFocus();
    }
    
    /**
     * Show existing keywords list
     */
    private void showKeywordListDialog() {
        KeywordManager keywordManager = KeywordManager.getInstance(this);
        java.util.List<String> keywords = keywordManager.getCustomKeywords();
        
        if (keywords.isEmpty()) {
            showToast("Henüz özel anahtar kelime eklenmemiş");
            return;
        }
        
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Özel Anahtar Kelimeler (" + keywords.size() + ")");
        
        String[] keywordArray = keywords.toArray(new String[0]);
        
        builder.setItems(keywordArray, (dialog, which) -> {
            String selectedKeyword = keywordArray[which];
            showKeywordOptionsDialog(selectedKeyword);
        });
        
        builder.setNegativeButton("Kapat", null);
        builder.show();
    }
    
    /**
     * Show options for selected keyword
     */
    private void showKeywordOptionsDialog(String keyword) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("\"" + keyword + "\" için seçenekler");
        
        String[] options = {"🗑️ Sil", "🔙 Geri"};
        
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                removeCustomKeyword(keyword);
            }
            // which == 1 just closes dialog
        });
        
        builder.show();
    }
    
    /**
     * Show confirmation for clearing all keywords
     */
    private void showClearKeywordsConfirmation() {
        KeywordManager keywordManager = KeywordManager.getInstance(this);
        int count = keywordManager.getCustomKeywordCount();
        
        if (count == 0) {
            showToast("Silinecek özel kelime yok");
            return;
        }
        
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Tüm Özel Kelimeleri Sil");
        builder.setMessage(count + " adet özel anahtar kelime kalıcı olarak silinecek.\n\nDevam etmek istiyor musunuz?");
        
        builder.setPositiveButton("Sil", (dialog, which) -> {
            keywordManager.clearCustomKeywords();
            showToast("✅ " + count + " özel kelime silindi");
            // Refresh spam detection
            refreshData();
        });
        
        builder.setNegativeButton("İptal", null);
        builder.show();
    }
    
    /**
     * Add new custom keyword
     */
    private void addCustomKeyword(String keyword) {
        if (keyword.isEmpty()) {
            showToast("❌ Boş kelime eklenemez");
            return;
        }
        
        if (keyword.length() < 2) {
            showToast("❌ Kelime en az 2 karakter olmalı");
            return;
        }
        
        KeywordManager keywordManager = KeywordManager.getInstance(this);
        
        if (keywordManager.addKeyword(keyword)) {
            showToast("✅ \"" + keyword + "\" eklendi");
            // Refresh spam detection to include new keyword
            refreshData();
        } else {
            showToast("❌ Kelime zaten mevcut veya geçersiz");
        }
    }
    
    /**
     * Remove custom keyword
     */
    private void removeCustomKeyword(String keyword) {
        KeywordManager keywordManager = KeywordManager.getInstance(this);
        
        if (keywordManager.removeKeyword(keyword)) {
            showToast("✅ \"" + keyword + "\" silindi");
            // Refresh spam detection
            refreshData();
        } else {
            showToast("❌ Kelime silinemedi");
        }
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
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "Error unregistering SMS observer", e);
            }
        }
        
        binding = null;
    }
}