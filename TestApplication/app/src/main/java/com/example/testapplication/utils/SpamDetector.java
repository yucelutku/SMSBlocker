package com.example.testapplication.utils;

import android.content.Context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class SpamDetector {
    
    private static final String[] TURKISH_GAMBLING_KEYWORDS = {
        "bahis", "kumar", "bet", "casino", "bonus", "freespin",
        "çevrim", "yatır", "kazanç", "slot", "rulet", "poker",
        "jackpot", "bedava", "para kazan", "deneme bonusu",
        "çevrimsiz", "hoşgeldin", "promosyon", "oyna", "kazan"
    };
    
    private static final String[] SUSPICIOUS_PATTERNS = {
        "\\b\\d{2,}\\s*(TL|₺|lira)", // Money amounts
        "\\b(www\\.|http|https)", // Links  
        "\\b\\d{2,}\\s*%", // Percentages
        "\\b(tikla|kayit|kayıt|bonus|hemen|acele)", // Action words
        "\\b\\d{4}\\s*kod", // Promo codes
        "\\+\\d{1,3}\\s*\\d{3,}" // International numbers
    };
    
    private static final String[] HIGH_RISK_SENDERS = {
        "\\d{4,5}", // Short numeric codes
        ".*bonus.*", 
        ".*bet.*",
        ".*casino.*"
    };

    public static class SpamAnalysisResult {
        public final boolean isSpam;
        public final float spamScore;
        public final String reason;
        public final List<String> detectionReasons;

        public SpamAnalysisResult(boolean isSpam, float spamScore, String reason, List<String> detectionReasons) {
            this.isSpam = isSpam;
            this.spamScore = spamScore;
            this.reason = reason;
            this.detectionReasons = detectionReasons != null ? detectionReasons : new ArrayList<>();
        }
    }

    public static SpamAnalysisResult analyzeMessage(String messageBody, String sender) {
        return analyzeMessage(messageBody, sender, null);
    }
    
    public static SpamAnalysisResult analyzeMessage(String messageBody, String sender, Context context) {
        if (messageBody == null || messageBody.trim().isEmpty()) {
            return new SpamAnalysisResult(false, 0.0f, "Empty message", new ArrayList<>());
        }

        float spamScore = 0.0f;
        List<String> reasons = new ArrayList<>();
        
        String lowerBody = messageBody.toLowerCase(new Locale("tr", "TR"));
        
        // Keyword detection with custom keywords if context provided
        spamScore += analyzeKeywords(lowerBody, reasons, context);
        
        // Pattern detection
        spamScore += analyzePatterns(messageBody, reasons);
        
        // Sender analysis
        spamScore += analyzeSender(sender, reasons);
        
        // Message length and characteristics
        spamScore += analyzeMessageCharacteristics(messageBody, reasons);

        boolean isSpam = spamScore >= 0.5f;
        String mainReason = reasons.isEmpty() ? "No spam indicators" : 
                           String.join(", ", reasons.subList(0, Math.min(3, reasons.size())));

        return new SpamAnalysisResult(isSpam, Math.min(spamScore, 1.0f), mainReason, reasons);
    }

    private static float analyzeKeywords(String lowerBody, List<String> reasons, Context context) {
        float score = 0.0f;
        int keywordCount = 0;
        
        List<String> allKeywords = new ArrayList<>(Arrays.asList(TURKISH_GAMBLING_KEYWORDS));
        
        if (context != null) {
            allKeywords.addAll(KeywordManager.getInstance(context).getCustomKeywords());
        }
        
        for (String keyword : allKeywords) {
            if (lowerBody.contains(keyword.toLowerCase(new Locale("tr", "TR")))) {
                score += 0.25f;
                keywordCount++;
                if (keywordCount <= 3) {
                    reasons.add("Spam keyword: " + keyword);
                }
            }
        }
        
        // Bonus for multiple keywords
        if (keywordCount >= 3) {
            score += 0.2f;
            reasons.add("Multiple gambling keywords detected");
        }
        
        return score;
    }

    private static float analyzePatterns(String messageBody, List<String> reasons) {
        float score = 0.0f;
        
        for (String pattern : SUSPICIOUS_PATTERNS) {
            if (Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(messageBody).find()) {
                score += 0.15f;
                reasons.add("Suspicious pattern detected");
            }
        }
        
        return score;
    }

    private static float analyzeSender(String sender, List<String> reasons) {
        if (sender == null || sender.isEmpty()) {
            return 0.0f;
        }
        
        float score = 0.0f;
        String lowerSender = sender.toLowerCase(new Locale("tr", "TR"));
        
        // Check high-risk sender patterns
        for (String pattern : HIGH_RISK_SENDERS) {
            if (Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(lowerSender).matches()) {
                score += 0.2f;
                reasons.add("Suspicious sender: " + sender);
                break;
            }
        }
        
        // Short numeric sender (common for bulk SMS)
        if (sender.matches("\\d{4,6}")) {
            score += 0.15f;
            reasons.add("Short numeric sender");
        }
        
        return score;
    }

    private static float analyzeMessageCharacteristics(String messageBody, List<String> reasons) {
        float score = 0.0f;
        
        // Very short messages with urgent language
        if (messageBody.length() < 50 && 
            (messageBody.toLowerCase().contains("hemen") || 
             messageBody.toLowerCase().contains("acele") ||
             messageBody.toLowerCase().contains("son"))) {
            score += 0.1f;
            reasons.add("Short urgent message");
        }
        
        // Excessive punctuation or caps
        long exclamationCount = messageBody.chars().filter(ch -> ch == '!').count();
        if (exclamationCount >= 3) {
            score += 0.1f;
            reasons.add("Excessive punctuation");
        }
        
        // Count capital letters ratio
        long upperCount = messageBody.chars().filter(Character::isUpperCase).count();
        if (messageBody.length() > 10 && (upperCount / (float) messageBody.length()) > 0.5) {
            score += 0.1f;
            reasons.add("Excessive capital letters");
        }
        
        return score;
    }

    public static boolean isKnownSpamNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return false;
        }
        
        // Common Turkish spam number patterns
        String[] spamPatterns = {
            "^0850.*", // 0850 numbers often used for marketing
            "^444.*",  // 444 short codes
            "^\\d{4}$" // 4-digit short codes
        };
        
        for (String pattern : spamPatterns) {
            if (Pattern.matches(pattern, phoneNumber)) {
                return true;
            }
        }
        
        return false;
    }

    public static String getSpamCategory(SpamAnalysisResult result) {
        if (!result.isSpam) {
            return "Not Spam";
        }
        
        if (result.spamScore >= 0.8f) {
            return "High Risk Spam";
        } else if (result.spamScore >= 0.6f) {
            return "Likely Spam";
        } else {
            return "Possible Spam";
        }
    }
    
    public static List<String> getDefaultKeywords() {
        return Arrays.asList(TURKISH_GAMBLING_KEYWORDS);
    }
    
    public static boolean isDefaultKeyword(String keyword) {
        if (keyword == null) return false;
        String lower = keyword.toLowerCase(new Locale("tr", "TR"));
        for (String defaultKeyword : TURKISH_GAMBLING_KEYWORDS) {
            if (defaultKeyword.toLowerCase(new Locale("tr", "TR")).equals(lower)) {
                return true;
            }
        }
        return false;
    }
}