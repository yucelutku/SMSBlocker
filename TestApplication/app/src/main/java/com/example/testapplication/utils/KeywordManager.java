package com.example.testapplication.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KeywordManager {
    private static final String PREFS_NAME = "spam_keywords";
    private static final String KEY_CUSTOM_KEYWORDS = "custom_keywords";
    
    private static KeywordManager instance;
    private final SharedPreferences prefs;
    private final Set<String> customKeywords;
    
    private KeywordManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        customKeywords = new HashSet<>(prefs.getStringSet(KEY_CUSTOM_KEYWORDS, new HashSet<>()));
    }
    
    public static synchronized KeywordManager getInstance(Context context) {
        if (instance == null) {
            instance = new KeywordManager(context);
        }
        return instance;
    }
    
    public List<String> getCustomKeywords() {
        return new ArrayList<>(customKeywords);
    }
    
    public List<String> getAllKeywords() {
        List<String> allKeywords = new ArrayList<>();
        allKeywords.addAll(SpamDetector.getDefaultKeywords());
        allKeywords.addAll(customKeywords);
        return allKeywords;
    }
    
    public boolean addKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return false;
        }
        
        String normalized = keyword.trim().toLowerCase(new java.util.Locale("tr", "TR"));
        
        if (normalized.length() < 2) {
            return false;
        }
        
        if (customKeywords.contains(normalized) || SpamDetector.isDefaultKeyword(normalized)) {
            return false;
        }
        
        customKeywords.add(normalized);
        saveKeywords();
        return true;
    }
    
    public boolean removeKeyword(String keyword) {
        String normalized = keyword.trim().toLowerCase(new java.util.Locale("tr", "TR"));
        
        if (customKeywords.remove(normalized)) {
            saveKeywords();
            return true;
        }
        return false;
    }
    
    public void clearCustomKeywords() {
        customKeywords.clear();
        saveKeywords();
    }
    
    private void saveKeywords() {
        prefs.edit()
             .putStringSet(KEY_CUSTOM_KEYWORDS, new HashSet<>(customKeywords))
             .apply();
    }
    
    public int getCustomKeywordCount() {
        return customKeywords.size();
    }
}