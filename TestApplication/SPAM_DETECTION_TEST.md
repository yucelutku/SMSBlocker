# Spam Detection Test Cases

## Test with Debug Logging Enabled

### Test Case 1: Single Word "bet"
**Input**: "bet"
**Expected**: 
- Exact match detected
- Score: 0.8 (threshold 0.5)
- Result: SPAM

**Logcat Filter**: `adb logcat | grep SPAM_`

### Test Case 2: Custom Keyword "secose"
**Input**: "secose"
**Steps**:
1. Add "secose" as custom keyword in app
2. Send SMS with "secose"
**Expected**:
- Custom keywords loaded: [secose]
- Exact match detected
- Score: 0.8
- Result: SPAM

### Test Case 3: Keyword in Sentence
**Input**: "Bu bir bet mesajı"
**Expected**:
- Keyword match found: 'bet'
- Score: 0.35+
- Result: SPAM (if score >= 0.5)

### Test Case 4: Multiple Keywords
**Input**: "bet bahis bonus"
**Expected**:
- 3 keyword matches
- Score: 1.05+ (3×0.35 + 0.2 bonus)
- Result: SPAM

### Test Case 5: Normal Message
**Input**: "Normal mesaj"
**Expected**:
- No keywords matched
- Score: 0.0
- Result: NOT SPAM

## Algorithm Improvements

### 1. Exact Match Detection
```java
if (lowerBody.equals(normalizedKeyword)) {
    score += 0.8f;  // High score for exact matches
}
```

### 2. Word Boundary Matching
```java
// Checks for:
// - " bet " (surrounded by spaces)
// - "bet " (starts with)
// - " bet" (ends with)
// - Contains anywhere (fallback)
```

### 3. Turkish Normalization
```java
Locale.forLanguageTag("tr")  // Proper Turkish locale
```

### 4. Custom Keyword Loading
```java
List<String> customKeywords = KeywordManager.getInstance(context).getCustomKeywords();
allKeywords.addAll(customKeywords);
```

## Debug Output Format

```
[SPAM_DEBUG] Custom keywords loaded: 1 - [secose]
[SPAM_DEBUG] Analyzing message: 'bet' (normalized: 'bet')
[SPAM_MATCH] EXACT match found: 'bet' (score +0.8)
[SPAM_RESULT] Total keywords matched: 1, Score: 0.8, Is spam: true
```

## Scoring System

| Match Type | Score | Example |
|------------|-------|---------|
| Exact match | 0.8 | "bet" = "bet" |
| Keyword in text | 0.35 | "Bu bet mesajı" |
| Multiple keywords (3+) | +0.2 bonus | "bet bahis bonus" |
| **Spam Threshold** | **0.5** | Score >= 0.5 → SPAM |

## Known Issues Fixed

1. ✅ Single word detection (score too low)
2. ✅ Custom keywords not loading
3. ✅ Turkish normalization (Locale.forLanguageTag)
4. ✅ Exact match priority
5. ✅ Word boundary detection