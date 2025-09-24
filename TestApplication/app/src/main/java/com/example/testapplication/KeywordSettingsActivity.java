package com.example.testapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testapplication.adapters.KeywordListAdapter;
import com.example.testapplication.databinding.ActivityKeywordSettingsBinding;
import com.example.testapplication.utils.KeywordManager;
import com.example.testapplication.utils.SpamDetector;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class KeywordSettingsActivity extends AppCompatActivity {
    
    private ActivityKeywordSettingsBinding binding;
    private KeywordListAdapter customAdapter;
    private KeywordListAdapter defaultAdapter;
    private KeywordManager keywordManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        binding = ActivityKeywordSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        keywordManager = KeywordManager.getInstance(this);
        
        setupToolbar();
        setupRecyclerViews();
        setupFab();
        loadKeywords();
    }
    
    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Spam Anahtar Kelimeler");
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    private void setupRecyclerViews() {
        customAdapter = new KeywordListAdapter(new ArrayList<>(), true, this::onDeleteKeyword);
        binding.customKeywordsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.customKeywordsRecyclerView.setAdapter(customAdapter);
        
        defaultAdapter = new KeywordListAdapter(new ArrayList<>(), false, null);
        binding.defaultKeywordsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.defaultKeywordsRecyclerView.setAdapter(defaultAdapter);
    }
    
    private void setupFab() {
        binding.addKeywordFab.setOnClickListener(v -> showAddKeywordDialog());
    }
    
    private void loadKeywords() {
        List<String> customKeywords = keywordManager.getCustomKeywords();
        customAdapter.updateKeywords(customKeywords);
        
        binding.customKeywordsEmpty.setVisibility(customKeywords.isEmpty() ? View.VISIBLE : View.GONE);
        binding.customKeywordsRecyclerView.setVisibility(customKeywords.isEmpty() ? View.GONE : View.VISIBLE);
        
        binding.customKeywordCount.setText(customKeywords.size() + " özel kelime");
        
        List<String> defaultKeywords = SpamDetector.getDefaultKeywords();
        defaultAdapter.updateKeywords(defaultKeywords);
        binding.defaultKeywordCount.setText(defaultKeywords.size() + " varsayılan kelime");
    }
    
    private void showAddKeywordDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_keyword, null);
        EditText keywordInput = dialogView.findViewById(R.id.keywordInput);
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("Yeni Anahtar Kelime Ekle")
                .setView(dialogView)
                .setPositiveButton("Ekle", (dialog, which) -> {
                    String keyword = keywordInput.getText().toString().trim();
                    addKeyword(keyword);
                })
                .setNegativeButton("İptal", null)
                .show();
        
        keywordInput.requestFocus();
    }
    
    private void addKeyword(String keyword) {
        if (keyword.isEmpty()) {
            Toast.makeText(this, "Kelime boş olamaz", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (keyword.length() < 2) {
            Toast.makeText(this, "Kelime en az 2 karakter olmalı", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (keywordManager.addKeyword(keyword)) {
            Toast.makeText(this, "✓ '" + keyword + "' eklendi", Toast.LENGTH_SHORT).show();
            loadKeywords();
        } else {
            Toast.makeText(this, "Bu kelime zaten mevcut", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void onDeleteKeyword(String keyword) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Kelimeyi Sil")
                .setMessage("'" + keyword + "' kelimesini silmek istediğinize emin misiniz?")
                .setPositiveButton("Sil", (dialog, which) -> {
                    if (keywordManager.removeKeyword(keyword)) {
                        Toast.makeText(this, "✓ Kelime silindi", Toast.LENGTH_SHORT).show();
                        loadKeywords();
                    }
                })
                .setNegativeButton("İptal", null)
                .show();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}