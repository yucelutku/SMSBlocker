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
    
    // Message length categories for context-aware scoring
    private static final int SHORT_MESSAGE = 50;     // SMS length
    private static final int MEDIUM_MESSAGE = 150;   // Normal message  
    private static final int LONG_MESSAGE = 300;     // Detailed message
    
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
        public final ContextAnalysis contextAnalysis;

        public SpamAnalysisResult(boolean isSpam, float spamScore, String reason, List<String> detectionReasons, ContextAnalysis contextAnalysis) {
            this.isSpam = isSpam;
            this.spamScore = spamScore;
            this.reason = reason;
            this.detectionReasons = detectionReasons != null ? detectionReasons : new ArrayList<>();
            this.contextAnalysis = contextAnalysis != null ? contextAnalysis : new ContextAnalysis();
        }
        
        // Backward compatibility
        public SpamAnalysisResult(boolean isSpam, float spamScore, String reason, List<String> detectionReasons) {
            this(isSpam, spamScore, reason, detectionReasons, new ContextAnalysis());
        }
    }
    
    public static class ContextAnalysis {
        public final int messageLength;
        public final int keywordCount;
        public final float keywordDensity;
        public final String lengthCategory;
        public final float contextMultiplier;
        public final String contextDescription;
        
        public ContextAnalysis() {
            this(0, 0, 0.0f, "Unknown", 1.0f, "");
        }
        
        public ContextAnalysis(int messageLength, int keywordCount, float keywordDensity, 
                             String lengthCategory, float contextMultiplier, String contextDescription) {
            this.messageLength = messageLength;
            this.keywordCount = keywordCount;
            this.keywordDensity = keywordDensity;
            this.lengthCategory = lengthCategory;
            this.contextMultiplier = contextMultiplier;
            this.contextDescription = contextDescription;
        }
    }
    
    private static class KeywordAnalysisResult {
        public final float contextAwareScore;
        public final int keywordCount;
        public final float keywordDensity;
        public final String lengthCategory;
        public final float contextMultiplier;
        public final String contextDescription;
        
        public KeywordAnalysisResult(float contextAwareScore, int keywordCount, float keywordDensity,
                                   String lengthCategory, float contextMultiplier, String contextDescription) {
            this.contextAwareScore = contextAwareScore;
            this.keywordCount = keywordCount;
            this.keywordDensity = keywordDensity;
            this.lengthCategory = lengthCategory;
            this.contextMultiplier = contextMultiplier;
            this.contextDescription = contextDescription;
        }
    }

    public static SpamAnalysisResult analyzeMessage(String messageBody, String sender) {
        return analyzeMessage(messageBody, sender, null);
    }
    
    public static SpamAnalysisResult analyzeMessage(String messageBody, String sender, Context context) {
        if (messageBody == null || messageBody.trim().isEmpty()) {
            return new SpamAnalysisResult(false, 0.0f, "Empty message", new ArrayList<>(), new ContextAnalysis());
        }

        float spamScore = 0.0f;
        List<String> reasons = new ArrayList<>();
        
        String trimmedBody = messageBody.trim();
        String lowerBody = trimmedBody.toLowerCase(Locale.forLanguageTag("tr"));
        
        // CONTEXT-AWARE KEYWORD ANALYSIS
        KeywordAnalysisResult keywordResult = analyzeKeywordsWithContext(trimmedBody, lowerBody, reasons, context);
        spamScore += keywordResult.contextAwareScore;
        
        // Pattern detection
        spamScore += analyzePatterns(messageBody, reasons);
        
        // Sender analysis  
        spamScore += analyzeSender(sender, reasons);
        
        // Message length and characteristics
        spamScore += analyzeMessageCharacteristics(messageBody, reasons);

        // Create context analysis
        ContextAnalysis contextAnalysis = new ContextAnalysis(
            trimmedBody.length(),
            keywordResult.keywordCount,
            keywordResult.keywordDensity,
            keywordResult.lengthCategory,
            keywordResult.contextMultiplier,
            keywordResult.contextDescription
        );

        boolean isSpam = spamScore >= 0.5f;
        String mainReason = reasons.isEmpty() ? "No spam indicators" : 
                           String.join(", ", reasons.subList(0, Math.min(3, reasons.size())));

        return new SpamAnalysisResult(isSpam, Math.min(spamScore, 1.0f), mainReason, reasons, contextAnalysis);
    }

    /**
     * Context-aware keyword analysis with message length consideration
     */
    private static KeywordAnalysisResult analyzeKeywordsWithContext(String trimmedBody, String lowerBody, 
                                                                   List<String> reasons, Context context) {
        float baseScore = 0.0f;
        int keywordCount = 0;
        List<String> foundKeywords = new ArrayList<>();
        
        List<String> allKeywords = new ArrayList<>(Arrays.asList(TURKISH_GAMBLING_KEYWORDS));
        
        if (context != null) {
            List<String> customKeywords = KeywordManager.getInstance(context).getCustomKeywords();
            allKeywords.addAll(customKeywords);
        }
        
        // Analyze keywords
        for (String keyword : allKeywords) {
            String normalizedKeyword = keyword.toLowerCase(Locale.forLanguageTag("tr"));
            
            if (lowerBody.equals(normalizedKeyword)) {
                baseScore += 0.8f;
                keywordCount++;
                foundKeywords.add(keyword);
                reasons.add("Exact match: " + keyword);
                continue;
            }
            
            if (lowerBody.contains(" " + normalizedKeyword + " ") ||
                lowerBody.startsWith(normalizedKeyword + " ") ||
                lowerBody.endsWith(" " + normalizedKeyword) ||
                lowerBody.contains(normalizedKeyword)) {
                
                baseScore += 0.35f;
                keywordCount++;
                foundKeywords.add(keyword);
                if (keywordCount <= 3) {
                    reasons.add("Spam keyword: " + keyword);
                }
            }
        }
        
        if (keywordCount >= 3) {
            baseScore += 0.2f;
            reasons.add("Multiple spam keywords");
        }
        
        // CONTEXT-AWARE SCORING BASED ON MESSAGE LENGTH
        int messageLength = trimmedBody.length();
        String lengthCategory;
        float contextMultiplier;
        String contextDescription;
        
        if (messageLength <= SHORT_MESSAGE) {
            // Short messages: keywords have HIGH impact
            lengthCategory = "Kısa";
            contextMultiplier = 1.5f;
            contextDescription = "Kısa mesaj - artırılmış spam skoru";
            if (keywordCount > 0) {
                reasons.add("Kısa mesajda spam kelime - yüksek risk");
            }
        } else if (messageLength <= MEDIUM_MESSAGE) {
            // Medium messages: keywords have NORMAL impact  
            lengthCategory = "Orta";
            contextMultiplier = 1.0f;
            contextDescription = "Orta mesaj - normal spam skoru";
        } else {
            // Long messages: keywords have REDUCED impact
            lengthCategory = "Uzun";
            contextMultiplier = 0.6f;
            contextDescription = "Uzun mesaj - azaltılmış spam skoru";
            if (keywordCount > 0) {
                reasons.add("Uzun mesajda tek kelime - düşük risk");
            }
        }
        
        // Calculate keyword density
        float keywordDensity = messageLength > 0 ? (keywordCount * 100.0f) / messageLength : 0.0f;
        
        // Apply context multiplier
        float contextAwareScore = baseScore * contextMultiplier;
        
        return new KeywordAnalysisResult(
            contextAwareScore,
            keywordCount,
            keywordDensity,
            lengthCategory,
            contextMultiplier,
            contextDescription
        );
    }
    
    /**
     * Legacy method for backward compatibility
     */
    private static float analyzeKeywords(String trimmedBody, String lowerBody, List<String> reasons, Context context) {
        KeywordAnalysisResult result = analyzeKeywordsWithContext(trimmedBody, lowerBody, reasons, context);
        return result.contextAwareScore;
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

    /**
     * Get default keywords for KeywordManager
     */
    public static List<String> getDefaultKeywords() {
        return Arrays.asList(TURKISH_GAMBLING_KEYWORDS);
    }
    
    /**
     * Check if keyword is in default list
     */
    public static boolean isDefaultKeyword(String keyword) {
        String normalized = keyword.toLowerCase(Locale.forLanguageTag("tr"));
        for (String defaultKeyword : TURKISH_GAMBLING_KEYWORDS) {
            if (defaultKeyword.equals(normalized)) {
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