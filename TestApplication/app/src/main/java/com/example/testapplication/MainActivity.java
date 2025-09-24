package com.example.testapplication;

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
        checkPermissions();
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
                    smsViewModel.loadMessages(50);
                    showToast("Loading messages...");
                } else {
                    showToast("SMS permissions required");
                }
            }
        });

        binding.blockedNumbersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PermissionHelper.hasSmsPermissions(MainActivity.this)) {
                    smsViewModel.loadSpamMessages();
                    showToast("Loading spam messages...");
                } else {
                    showToast("SMS permissions required");
                }
            }
        });

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PermissionHelper.hasSmsPermissions(MainActivity.this)) {
                    smsViewModel.deleteAllSpamMessages(deletedCount -> {
                        if (deletedCount > 0) {
                            showToast("Deleted " + deletedCount + " spam messages");
                        } else {
                            showToast("No spam messages to delete");
                        }
                    });
                } else {
                    showToast("Settings feature - Coming soon!");
                }
            }
        });
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
            smsViewModel.refreshData(); // Load SMS data when permissions are available
        } else {
            updateProtectionStatus(false);
        }
    }

    private void requestSmsPermissions() {
        if (!PermissionHelper.hasSmsPermissions(this)) {
            ActivityCompat.requestPermissions(this, PermissionHelper.SMS_PERMISSIONS, SMS_PERMISSION_REQUEST_CODE);
        } else if (!PermissionHelper.isDefaultSmsApp(this)) {
            showDefaultSmsAppDialog();
        } else {
            showToast("SMS permissions already granted");
        }
    }

    private void showDefaultSmsAppDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Default SMS App Required")
                .setMessage("To delete SMS messages, this app needs to be set as your default SMS app. You can change it back later.")
                .setPositiveButton("Set as Default", (dialog, which) -> {
                    PermissionHelper.requestDefaultSmsApp(this, DEFAULT_SMS_APP_REQUEST_CODE);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            
            if (allPermissionsGranted) {
                if (!PermissionHelper.isDefaultSmsApp(this)) {
                    showDefaultSmsAppDialog();
                } else {
                    updateProtectionStatus(true);
                    smsViewModel.refreshData();
                    showToast("SMS permissions granted! Protection enabled.");
                }
            } else {
                updateProtectionStatus(false);
                showToast("SMS permissions are required for protection features.");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == DEFAULT_SMS_APP_REQUEST_CODE) {
            if (PermissionHelper.isDefaultSmsApp(this)) {
                updateProtectionStatus(true);
                smsViewModel.refreshData();
                showToast("App is now default SMS app. Full functionality enabled.");
            } else {
                showToast("Default SMS app not set. SMS deletion will not work.");
            }
        }
    }

    private void updateProtectionStatus(boolean isProtected) {
        if (isProtected) {
            binding.statusText.setText("Protection is active - SMS spam detection enabled");
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
        smsViewModel.deleteMessage(message.id, success -> {
            if (success) {
                showToast("Message deleted successfully");
            } else {
                showToast("Failed to delete message");
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
        binding = null;
    }
}