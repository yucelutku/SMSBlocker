package com.example.testapplication;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.testapplication.databinding.ActivityMainBinding;
import com.example.testapplication.viewmodels.SmsViewModel;
import com.example.testapplication.utils.PermissionHelper;
import com.example.testapplication.utils.SmsHelper;

import android.Manifest;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SmsViewModel smsViewModel;
    private static final int SMS_PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setupViewModel();
        setupToolbar();
        setupUI();
        setupObservers();
        checkPermissions();
    }

    private void setupViewModel() {
        smsViewModel = new ViewModelProvider(this).get(SmsViewModel.class);
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
            // You could show a progress indicator here
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
        } else {
            showToast("SMS permissions already granted");
        }
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
                updateProtectionStatus(true);
                smsViewModel.refreshData(); // Load SMS data after permissions granted
                showToast("SMS permissions granted! Protection enabled.");
            } else {
                updateProtectionStatus(false);
                showToast("SMS permissions are required for protection features.");
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


    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}