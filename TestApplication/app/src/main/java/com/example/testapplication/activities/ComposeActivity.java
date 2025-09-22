package com.example.testapplication.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.testapplication.R;
import com.example.testapplication.databinding.ActivityComposeBinding;
import com.example.testapplication.utils.PermissionHelper;

import java.util.ArrayList;

public class ComposeActivity extends AppCompatActivity {
    private static final String TAG = "ComposeActivity";
    private ActivityComposeBinding binding;
    
    private String recipientNumber;
    private String initialMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        binding = ActivityComposeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setupToolbar();
        handleIncomingIntent();
        setupUI();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Compose SMS");
        }
    }

    private void handleIncomingIntent() {
        Intent intent = getIntent();
        String action = intent.getAction();
        
        Log.d(TAG, "Handling intent with action: " + action);

        if (Intent.ACTION_SEND.equals(action)) {
            handleSendAction(intent);
        } else if (Intent.ACTION_SENDTO.equals(action)) {
            handleSendToAction(intent);
        } else if (Intent.ACTION_VIEW.equals(action)) {
            handleViewAction(intent);
        } else {
            Log.w(TAG, "Unknown intent action: " + action);
        }
    }

    private void handleSendAction(Intent intent) {
        // Handle ACTION_SEND - typically for sharing text to SMS
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
        
        if (sharedText != null) {
            initialMessage = sharedText;
            Log.d(TAG, "Received shared text: " + sharedText);
        }
        
        if (subject != null) {
            initialMessage = subject + (initialMessage != null ? "\n" + initialMessage : "");
        }
    }

    private void handleSendToAction(Intent intent) {
        // Handle ACTION_SENDTO - typically sms:number or smsto:number
        Uri uri = intent.getData();
        if (uri != null) {
            String scheme = uri.getScheme();
            
            if ("sms".equals(scheme) || "smsto".equals(scheme)) {
                recipientNumber = uri.getSchemeSpecificPart();
                // Remove any URI encoding
                if (recipientNumber.startsWith("//")) {
                    recipientNumber = recipientNumber.substring(2);
                }
                
                Log.d(TAG, "Recipient from URI: " + recipientNumber);
            }
        }
        
        // Check for body parameter
        String body = intent.getStringExtra("sms_body");
        if (body == null) {
            body = intent.getStringExtra(Intent.EXTRA_TEXT);
        }
        
        if (body != null) {
            initialMessage = body;
        }
    }

    private void handleViewAction(Intent intent) {
        // Handle ACTION_VIEW for SMS URIs
        Uri uri = intent.getData();
        if (uri != null) {
            handleSmsUri(uri);
        }
    }

    private void handleSmsUri(Uri uri) {
        String scheme = uri.getScheme();
        
        if ("sms".equals(scheme)) {
            recipientNumber = uri.getSchemeSpecificPart();
            
            // Parse query parameters for body
            String body = uri.getQueryParameter("body");
            if (body != null) {
                initialMessage = body;
            }
        }
    }

    private void setupUI() {
        // Set initial values if available
        if (recipientNumber != null && !recipientNumber.isEmpty()) {
            binding.recipientEditText.setText(recipientNumber);
        }
        
        if (initialMessage != null && !initialMessage.isEmpty()) {
            binding.messageEditText.setText(initialMessage);
            // Move cursor to end
            binding.messageEditText.setSelection(initialMessage.length());
        }

        // Setup character counter
        binding.messageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateCharacterCount(s.length());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Setup send button
        binding.sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendSms();
            }
        });

        // Initial character count
        updateCharacterCount(binding.messageEditText.getText().length());
    }

    private void updateCharacterCount(int length) {
        int smsLimit = 160;
        int remaining = smsLimit - length;
        
        if (length <= smsLimit) {
            binding.characterCountText.setText(remaining + " remaining");
            binding.characterCountText.setTextColor(getColor(android.R.color.darker_gray));
        } else {
            int messages = (length / smsLimit) + 1;
            binding.characterCountText.setText(messages + " messages, " + remaining + " over");
            binding.characterCountText.setTextColor(getColor(android.R.color.holo_orange_dark));
        }
    }

    private void sendSms() {
        String recipient = binding.recipientEditText.getText().toString().trim();
        String message = binding.messageEditText.getText().toString().trim();

        // Validate input
        if (recipient.isEmpty()) {
            binding.recipientEditText.setError("Recipient is required");
            binding.recipientEditText.requestFocus();
            return;
        }

        if (message.isEmpty()) {
            binding.messageEditText.setError("Message is required");
            binding.messageEditText.requestFocus();
            return;
        }

        // Check SMS permissions
        if (!PermissionHelper.hasSendSmsPermission(this)) {
            Toast.makeText(this, "SMS send permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            SmsManager smsManager = SmsManager.getDefault();
            
            // For long messages, divide into multiple parts
            ArrayList<String> parts = smsManager.divideMessage(message);
            
            if (parts.size() == 1) {
                // Single message
                smsManager.sendTextMessage(recipient, null, message, null, null);
                Log.d(TAG, "Sent single SMS to " + recipient);
            } else {
                // Multiple parts
                smsManager.sendMultipartTextMessage(recipient, null, parts, null, null);
                Log.d(TAG, "Sent multipart SMS (" + parts.size() + " parts) to " + recipient);
            }

            Toast.makeText(this, "Message sent successfully", Toast.LENGTH_SHORT).show();
            
            // Clear the message field for new composition
            binding.messageEditText.setText("");
            
            // Close activity after sending
            finish();
            
        } catch (Exception e) {
            Log.e(TAG, "Error sending SMS: " + e.getMessage(), e);
            Toast.makeText(this, "Failed to send message: " + e.getMessage(), 
                         Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.compose_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_clear) {
            clearFields();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    private void clearFields() {
        binding.messageEditText.setText("");
        binding.recipientEditText.setText("");
        binding.recipientEditText.requestFocus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}