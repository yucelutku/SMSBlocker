package com.example.testapplication.repositories;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.BlockedNumberContract;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.testapplication.models.SmsMessage;
import com.example.testapplication.models.SpamSender;
import com.example.testapplication.utils.SmsHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SmsRepository {
    private static final String TAG = "SmsRepository";
    
    private static SmsRepository instance;
    private final Context context;
    private final ExecutorService executor;
    private final Handler mainHandler;
    
    // LiveData for reactive updates
    private final MutableLiveData<List<SmsMessage>> allMessages = new MutableLiveData<>();
    private final MutableLiveData<List<SmsMessage>> inboxMessages = new MutableLiveData<>();
    private final MutableLiveData<List<SmsMessage>> spamMessages = new MutableLiveData<>();
    private final MutableLiveData<SmsHelper.SmsStatistics> statistics = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    
    // Spam Senders LiveData
    private final MutableLiveData<List<SpamSender>> spamSenders = new MutableLiveData<>();

    private SmsRepository(Context context) {
        this.context = context.getApplicationContext();
        this.executor = Executors.newFixedThreadPool(3);
        this.mainHandler = new Handler(Looper.getMainLooper());
        
        // Initialize with empty state
        isLoading.setValue(false);
        errorMessage.setValue(null);
    }

    public static synchronized SmsRepository getInstance(Context context) {
        if (instance == null) {
            instance = new SmsRepository(context);
        }
        return instance;
    }

    // LiveData getters
    public LiveData<List<SmsMessage>> getAllMessages() {
        return allMessages;
    }

    public LiveData<List<SmsMessage>> getInboxMessages() {
        return inboxMessages;
    }

    public LiveData<List<SmsMessage>> getSpamMessages() {
        return spamMessages;
    }

    public LiveData<SmsHelper.SmsStatistics> getStatistics() {
        return statistics;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<List<SpamSender>> getSpamSenders() {
        return spamSenders;
    }

    // Data loading methods
    public void loadAllMessages() {
        loadAllMessages(100); // Default limit
    }

    public void loadAllMessages(int limit) {
        setLoading(true);
        clearError();
        
        executor.execute(() -> {
            try {
                List<SmsMessage> messages = SmsHelper.getAllSmsMessages(context, limit);
                postResult(allMessages, messages);
                
            } catch (Exception e) {
                postError("Failed to load messages: " + e.getMessage());
            } finally {
                setLoading(false);
            }
        });
    }

    public void loadInboxMessages() {
        loadInboxMessages(50);
    }

    public void loadInboxMessages(int limit) {
        setLoading(true);
        clearError();
        
        executor.execute(() -> {
            try {
                List<SmsMessage> messages = SmsHelper.getInboxMessages(context, limit);
                postResult(inboxMessages, messages);
                
            } catch (Exception e) {
                postError("Failed to load inbox messages: " + e.getMessage());
            } finally {
                setLoading(false);
            }
        });
    }

    public void loadSpamMessages() {
        setLoading(true);
        clearError();
        
        executor.execute(() -> {
            try {
                List<SmsMessage> messages = SmsHelper.getSpamMessages(context);
                postResult(spamMessages, messages);
                
            } catch (Exception e) {
                postError("Failed to load spam messages: " + e.getMessage());
            } finally {
                setLoading(false);
            }
        });
    }

    public void loadStatistics() {
        executor.execute(() -> {
            try {
                SmsHelper.SmsStatistics stats = SmsHelper.getSmsStatistics(context);
                postResult(statistics, stats);
                
            } catch (Exception e) {
                postError("Failed to load statistics: " + e.getMessage());
            }
        });
    }

    // Message operations
    public void deleteMessage(long messageId, RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                boolean success = SmsHelper.deleteSmsMessage(context, messageId);
                postCallback(callback, success);
                
                if (success) {
                    refreshAllData();
                }
            } catch (Exception e) {
                android.util.Log.e(TAG, "Delete failed: " + e.getMessage(), e);
                postError("Failed to delete message: " + e.getMessage());
                postCallback(callback, false);
            }
        });
    }

    public void deleteSpamMessages(RepositoryCallback<Integer> callback) {
        setLoading(true);
        clearError();
        
        executor.execute(() -> {
            try {
                int deletedCount = SmsHelper.deleteSpamMessages(context);
                postCallback(callback, deletedCount);
                
                // Refresh data after deletion
                refreshAllData();
                
            } catch (Exception e) {
                postError("Failed to delete spam messages: " + e.getMessage());
                postCallback(callback, 0);
            } finally {
                setLoading(false);
            }
        });
    }

    public void getMessageById(long messageId, RepositoryCallback<SmsMessage> callback) {
        executor.execute(() -> {
            try {
                SmsMessage message = SmsHelper.getSmsMessageById(context, messageId);
                postCallback(callback, message);
                
            } catch (Exception e) {
                postError("Failed to get message: " + e.getMessage());
                postCallback(callback, null);
            }
        });
    }

    // Utility methods
    public void refreshAllData() {
        loadStatistics();
        loadAllMessages();
        loadInboxMessages();
        loadSpamMessages();
        loadSpamSenders();
        loadBlockedNumbers();
    }

    private void setLoading(boolean loading) {
        mainHandler.post(() -> isLoading.setValue(loading));
    }

    private void clearError() {
        mainHandler.post(() -> errorMessage.setValue(null));
    }

    private void postError(String error) {
        mainHandler.post(() -> errorMessage.setValue(error));
    }

    private <T> void postResult(MutableLiveData<T> liveData, T result) {
        mainHandler.post(() -> liveData.setValue(result));
    }

    private <T> void postCallback(RepositoryCallback<T> callback, T result) {
        if (callback != null) {
            mainHandler.post(() -> callback.onResult(result));
        }
    }

    // ==================== COMPREHENSIVE BULK DELETE OPERATIONS ====================
    
    // Callback interface for progress updates during bulk operations
    public interface BulkOperationCallback {
        void onProgress(int current, int total);
        void onCompleted(int deletedCount);
        void onError(String error);
    }
    
    // Basic bulk delete operations
    public void deleteAllMessages(BulkOperationCallback callback) {
        executor.execute(() -> {
            try {
                setLoading(true);
                clearError();
                
                List<SmsMessage> allMessages = SmsHelper.getAllMessages(context);
                int total = allMessages.size();
                int deletedCount = 0;
                
                for (int i = 0; i < allMessages.size(); i++) {
                    SmsMessage message = allMessages.get(i);
                    boolean success = SmsHelper.deleteMessage(context, message.id);
                    
                    if (success) {
                        deletedCount++;
                    }
                    
                    // Update progress every 10 messages or on last message
                    if (i % 10 == 0 || i == allMessages.size() - 1) {
                        final int current = i + 1;
                        final int deleted = deletedCount;
                        mainHandler.post(() -> {
                            if (callback != null) {
                                callback.onProgress(current, total);
                            }
                        });
                    }
                }
                
                final int finalDeletedCount = deletedCount;
                mainHandler.post(() -> {
                    setLoading(false);
                    if (callback != null) {
                        callback.onCompleted(finalDeletedCount);
                    }
                    refreshAllData();
                });
                
            } catch (Exception e) {
                mainHandler.post(() -> {
                    setLoading(false);
                    String error = "Tüm mesajları silme hatası: " + e.getMessage();
                    postError(error);
                    if (callback != null) {
                        callback.onError(error);
                    }
                });
            }
        });
    }
    
    public void deleteAllSpamMessages(BulkOperationCallback callback) {
        executor.execute(() -> {
            try {
                setLoading(true);
                clearError();
                
                List<SmsMessage> allMessages = SmsHelper.getAllMessages(context);
                List<SmsMessage> spamMessages = new ArrayList<>();
                
                // Filter spam messages
                for (SmsMessage message : allMessages) {
                    if (message.isSpam) {
                        spamMessages.add(message);
                    }
                }
                
                int total = spamMessages.size();
                int deletedCount = 0;
                
                for (int i = 0; i < spamMessages.size(); i++) {
                    SmsMessage message = spamMessages.get(i);
                    boolean success = SmsHelper.deleteMessage(context, message.id);
                    
                    if (success) {
                        deletedCount++;
                    }
                    
                    if (i % 5 == 0 || i == spamMessages.size() - 1) {
                        final int current = i + 1;
                        final int deleted = deletedCount;
                        mainHandler.post(() -> {
                            if (callback != null) {
                                callback.onProgress(current, total);
                            }
                        });
                    }
                }
                
                final int finalDeletedCount = deletedCount;
                mainHandler.post(() -> {
                    setLoading(false);
                    if (callback != null) {
                        callback.onCompleted(finalDeletedCount);
                    }
                    refreshAllData();
                });
                
            } catch (Exception e) {
                mainHandler.post(() -> {
                    setLoading(false);
                    String error = "Spam mesajları silme hatası: " + e.getMessage();
                    postError(error);
                    if (callback != null) {
                        callback.onError(error);
                    }
                });
            }
        });
    }
    
    public void deleteAllNormalMessages(BulkOperationCallback callback) {
        executor.execute(() -> {
            try {
                setLoading(true);
                clearError();
                
                List<SmsMessage> allMessages = SmsHelper.getAllMessages(context);
                List<SmsMessage> normalMessages = new ArrayList<>();
                
                // Filter normal (non-spam) messages
                for (SmsMessage message : allMessages) {
                    if (!message.isSpam) {
                        normalMessages.add(message);
                    }
                }
                
                int total = normalMessages.size();
                int deletedCount = 0;
                
                for (int i = 0; i < normalMessages.size(); i++) {
                    SmsMessage message = normalMessages.get(i);
                    boolean success = SmsHelper.deleteMessage(context, message.id);
                    
                    if (success) {
                        deletedCount++;
                    }
                    
                    if (i % 5 == 0 || i == normalMessages.size() - 1) {
                        final int current = i + 1;
                        final int deleted = deletedCount;
                        mainHandler.post(() -> {
                            if (callback != null) {
                                callback.onProgress(current, total);
                            }
                        });
                    }
                }
                
                final int finalDeletedCount = deletedCount;
                mainHandler.post(() -> {
                    setLoading(false);
                    if (callback != null) {
                        callback.onCompleted(finalDeletedCount);
                    }
                    refreshAllData();
                });
                
            } catch (Exception e) {
                mainHandler.post(() -> {
                    setLoading(false);
                    String error = "Normal mesajları silme hatası: " + e.getMessage();
                    postError(error);
                    if (callback != null) {
                        callback.onError(error);
                    }
                });
            }
        });
    }
    
    // Advanced bulk delete operations
    public void deleteMessagesBySender(String phoneNumber, BulkOperationCallback callback) {
        executor.execute(() -> {
            try {
                setLoading(true);
                clearError();
                
                List<SmsMessage> allMessages = SmsHelper.getAllMessages(context);
                List<SmsMessage> senderMessages = new ArrayList<>();
                
                // Filter messages from specific sender
                for (SmsMessage message : allMessages) {
                    if (phoneNumber.equals(message.address)) {
                        senderMessages.add(message);
                    }
                }
                
                int total = senderMessages.size();
                int deletedCount = 0;
                
                for (int i = 0; i < senderMessages.size(); i++) {
                    SmsMessage message = senderMessages.get(i);
                    boolean success = SmsHelper.deleteMessage(context, message.id);
                    
                    if (success) {
                        deletedCount++;
                    }
                    
                    if (i % 3 == 0 || i == senderMessages.size() - 1) {
                        final int current = i + 1;
                        final int deleted = deletedCount;
                        mainHandler.post(() -> {
                            if (callback != null) {
                                callback.onProgress(current, total);
                            }
                        });
                    }
                }
                
                final int finalDeletedCount = deletedCount;
                mainHandler.post(() -> {
                    setLoading(false);
                    if (callback != null) {
                        callback.onCompleted(finalDeletedCount);
                    }
                    refreshAllData();
                });
                
            } catch (Exception e) {
                mainHandler.post(() -> {
                    setLoading(false);
                    String error = "Gönderici bazlı silme hatası: " + e.getMessage();
                    postError(error);
                    if (callback != null) {
                        callback.onError(error);
                    }
                });
            }
        });
    }
    
    public void deleteMessagesByDateRange(long startDate, long endDate, BulkOperationCallback callback) {
        executor.execute(() -> {
            try {
                setLoading(true);
                clearError();
                
                List<SmsMessage> allMessages = SmsHelper.getAllMessages(context);
                List<SmsMessage> dateRangeMessages = new ArrayList<>();
                
                // Filter messages by date range
                for (SmsMessage message : allMessages) {
                    if (message.date >= startDate && message.date <= endDate) {
                        dateRangeMessages.add(message);
                    }
                }
                
                int total = dateRangeMessages.size();
                int deletedCount = 0;
                
                for (int i = 0; i < dateRangeMessages.size(); i++) {
                    SmsMessage message = dateRangeMessages.get(i);
                    boolean success = SmsHelper.deleteMessage(context, message.id);
                    
                    if (success) {
                        deletedCount++;
                    }
                    
                    if (i % 5 == 0 || i == dateRangeMessages.size() - 1) {
                        final int current = i + 1;
                        final int deleted = deletedCount;
                        mainHandler.post(() -> {
                            if (callback != null) {
                                callback.onProgress(current, total);
                            }
                        });
                    }
                }
                
                final int finalDeletedCount = deletedCount;
                mainHandler.post(() -> {
                    setLoading(false);
                    if (callback != null) {
                        callback.onCompleted(finalDeletedCount);
                    }
                    refreshAllData();
                });
                
            } catch (Exception e) {
                mainHandler.post(() -> {
                    setLoading(false);
                    String error = "Tarih aralığı silme hatası: " + e.getMessage();
                    postError(error);
                    if (callback != null) {
                        callback.onError(error);
                    }
                });
            }
        });
    }
    
    public void deleteMessagesByDateRangeAndType(long startDate, long endDate, boolean isSpam, BulkOperationCallback callback) {
        executor.execute(() -> {
            try {
                setLoading(true);
                clearError();
                
                List<SmsMessage> allMessages = SmsHelper.getAllMessages(context);
                List<SmsMessage> filteredMessages = new ArrayList<>();
                
                // Filter messages by date range and type
                for (SmsMessage message : allMessages) {
                    if (message.date >= startDate && message.date <= endDate && message.isSpam == isSpam) {
                        filteredMessages.add(message);
                    }
                }
                
                int total = filteredMessages.size();
                int deletedCount = 0;
                
                for (int i = 0; i < filteredMessages.size(); i++) {
                    SmsMessage message = filteredMessages.get(i);
                    boolean success = SmsHelper.deleteMessage(context, message.id);
                    
                    if (success) {
                        deletedCount++;
                    }
                    
                    if (i % 5 == 0 || i == filteredMessages.size() - 1) {
                        final int current = i + 1;
                        final int deleted = deletedCount;
                        mainHandler.post(() -> {
                            if (callback != null) {
                                callback.onProgress(current, total);
                            }
                        });
                    }
                }
                
                final int finalDeletedCount = deletedCount;
                mainHandler.post(() -> {
                    setLoading(false);
                    if (callback != null) {
                        callback.onCompleted(finalDeletedCount);
                    }
                    refreshAllData();
                });
                
            } catch (Exception e) {
                mainHandler.post(() -> {
                    setLoading(false);
                    String error = "Kombinasyon silme hatası: " + e.getMessage();
                    postError(error);
                    if (callback != null) {
                        callback.onError(error);
                    }
                });
            }
        });
    }
    
    // Statistics methods for bulk operations
    public void getMessageCountByType(boolean isSpam, RepositoryCallback<Integer> callback) {
        executor.execute(() -> {
            // PERFORMANCE: Add timing measurement
            long startTime = System.currentTimeMillis();
            android.util.Log.d("PERFORMANCE", "🔍 getMessageCountByType(" + isSpam + ") started");
            
            try {
                List<SmsMessage> allMessages = SmsHelper.getAllMessages(context);
                android.util.Log.d("PERFORMANCE", "📱 SmsHelper.getAllMessages() took: " + (System.currentTimeMillis() - startTime) + "ms");
                int count = 0;
                
                for (SmsMessage message : allMessages) {
                    if (message.isSpam == isSpam) {
                        count++;
                    }
                }
                
                final int finalCount = count;
                // PERFORMANCE: Log total query time
                long totalTime = System.currentTimeMillis() - startTime;
                android.util.Log.d("PERFORMANCE", "⚡ getMessageCountByType(" + isSpam + ") TOTAL: " + totalTime + "ms, found: " + finalCount);
                
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onResult(finalCount);
                    }
                });
                
            } catch (Exception e) {
                android.util.Log.d("PERFORMANCE", "❌ getMessageCountByType(" + isSpam + ") ERROR: " + (System.currentTimeMillis() - startTime) + "ms");
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onResult(0);
                    }
                });
            }
        });
    }
    
    public void getMessageCountBySender(String phoneNumber, RepositoryCallback<Integer> callback) {
        executor.execute(() -> {
            try {
                List<SmsMessage> allMessages = SmsHelper.getAllMessages(context);
                int count = 0;
                
                for (SmsMessage message : allMessages) {
                    if (phoneNumber.equals(message.address)) {
                        count++;
                    }
                }
                
                final int finalCount = count;
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onResult(finalCount);
                    }
                });
                
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onResult(0);
                    }
                });
            }
        });
    }
    
    public void getMessageCountByDateRange(long startDate, long endDate, RepositoryCallback<Integer> callback) {
        executor.execute(() -> {
            try {
                List<SmsMessage> allMessages = SmsHelper.getAllMessages(context);
                int count = 0;
                
                for (SmsMessage message : allMessages) {
                    if (message.date >= startDate && message.date <= endDate) {
                        count++;
                    }
                }
                
                final int finalCount = count;
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onResult(finalCount);
                    }
                });
                
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onResult(0);
                    }
                });
            }
        });
    }
    
    public void getFrequentSenders(RepositoryCallback<List<SenderInfo>> callback) {
        executor.execute(() -> {
            try {
                List<SmsMessage> allMessages = SmsHelper.getAllMessages(context);
                Map<String, SenderInfo> senderMap = new HashMap<>();
                
                // Count messages per sender
                for (SmsMessage message : allMessages) {
                    String sender = message.address != null ? message.address : "Unknown";
                    SenderInfo info = senderMap.get(sender);
                    
                    if (info == null) {
                        info = new SenderInfo(sender, 0, 0);
                        senderMap.put(sender, info);
                    }
                    
                    info.totalCount++;
                    if (message.isSpam) {
                        info.spamCount++;
                    }
                }
                
                // Convert to sorted list
                List<SenderInfo> senderList = new ArrayList<>(senderMap.values());
                Collections.sort(senderList, (a, b) -> Integer.compare(b.totalCount, a.totalCount));
                
                // Limit to top 20 senders
                if (senderList.size() > 20) {
                    senderList = senderList.subList(0, 20);
                }
                
                final List<SenderInfo> finalList = senderList;
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onResult(finalList);
                    }
                });
                
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onResult(new ArrayList<>());
                    }
                });
            }
        });
    }
    
    // Data class for sender information
    public static class SenderInfo {
        public final String phoneNumber;
        public int totalCount;
        public int spamCount;
        
        public SenderInfo(String phoneNumber, int totalCount, int spamCount) {
            this.phoneNumber = phoneNumber;
            this.totalCount = totalCount;
            this.spamCount = spamCount;
        }
        
        public String getDisplayName() {
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                return "Bilinmeyen";
            }
            return phoneNumber;
        }
        
        public float getSpamPercentage() {
            if (totalCount == 0) return 0f;
            return (float) spamCount / totalCount * 100f;
        }
    }

    // ==================== SPAM SENDER OPERATIONS (Step 1) ====================
    
    /**
     * Load spam senders - groups messages by sender and filters those with >50% spam rate
     */
    public void loadSpamSenders() {
        setLoading(true);
        clearError();
        
        executor.execute(() -> {
            try {
                List<SpamSender> senders = getSpamSendersInternal();
                postResult(spamSenders, senders);
                
            } catch (Exception e) {
                postError("Failed to load spam senders: " + e.getMessage());
            } finally {
                setLoading(false);
            }
        });
    }
    
    /**
     * Internal method to generate spam senders list from all messages
     */
    private List<SpamSender> getSpamSendersInternal() {
        List<SmsMessage> allMessages = SmsHelper.getAllMessages(context);
        Map<String, SpamSenderData> senderDataMap = new HashMap<>();
        
        // Group messages by sender and collect statistics
        for (SmsMessage message : allMessages) {
            String phoneNumber = message.address != null ? message.address : "Unknown";
            SpamSenderData data = senderDataMap.get(phoneNumber);
            
            if (data == null) {
                data = new SpamSenderData(phoneNumber);
                senderDataMap.put(phoneNumber, data);
            }
            
            data.totalCount++;
            if (message.isSpam) {
                data.spamCount++;
                // Keep the most recent spam message as sample
                if (data.sampleSpamMessage == null || message.date > data.lastMessageDate) {
                    data.sampleSpamMessage = message.body;
                }
            }
            
            // Update last message date
            if (message.date > data.lastMessageDate) {
                data.lastMessageDate = message.date;
            }
        }
        
        // Convert to SpamSender objects and filter by spam percentage
        List<SpamSender> spamSenderList = new ArrayList<>();
        for (SpamSenderData data : senderDataMap.values()) {
            float spamPercentage = data.totalCount > 0 ? 
                ((float) data.spamCount / data.totalCount) * 100.0f : 0.0f;
            
            // Only include senders with >50% spam rate
            if (spamPercentage >= 50.0f) {
                SpamSender spamSender = new SpamSender(
                    data.phoneNumber,
                    null, // Contact name to be populated later
                    data.totalCount,
                    data.spamCount,
                    data.sampleSpamMessage,
                    data.lastMessageDate
                );
                spamSenderList.add(spamSender);
            }
        }
        
        // Sort by spam count (highest first)
        Collections.sort(spamSenderList, (a, b) -> Integer.compare(b.spamMessages, a.spamMessages));
        
        return spamSenderList;
    }
    
    /**
     * Get spam senders with different sorting options
     */
    public void loadSpamSendersSorted(SpamSenderSortType sortType, RepositoryCallback<List<SpamSender>> callback) {
        executor.execute(() -> {
            try {
                List<SpamSender> senders = getSpamSendersInternal();
                
                // Apply sorting
                switch (sortType) {
                    case MOST_SPAM:
                        Collections.sort(senders, (a, b) -> Integer.compare(b.spamMessages, a.spamMessages));
                        break;
                    case MOST_MESSAGES:
                        Collections.sort(senders, (a, b) -> Integer.compare(b.totalMessages, a.totalMessages));
                        break;
                    case RECENT_ACTIVITY:
                        Collections.sort(senders, (a, b) -> Long.compare(b.lastMessageDate, a.lastMessageDate));
                        break;
                    case HIGHEST_SPAM_RATE:
                        Collections.sort(senders, (a, b) -> Float.compare(b.spamPercentage, a.spamPercentage));
                        break;
                }
                
                postCallback(callback, senders);
                
            } catch (Exception e) {
                postError("Failed to load sorted spam senders: " + e.getMessage());
                postCallback(callback, new ArrayList<>());
            }
        });
    }
    
    /**
     * Get spam sender by phone number
     */
    public void getSpamSenderByPhoneNumber(String phoneNumber, RepositoryCallback<SpamSender> callback) {
        executor.execute(() -> {
            try {
                List<SpamSender> allSpamSenders = getSpamSendersInternal();
                SpamSender foundSender = null;
                
                for (SpamSender sender : allSpamSenders) {
                    if (phoneNumber.equals(sender.phoneNumber)) {
                        foundSender = sender;
                        break;
                    }
                }
                
                postCallback(callback, foundSender);
                
            } catch (Exception e) {
                postError("Failed to get spam sender: " + e.getMessage());
                postCallback(callback, null);
            }
        });
    }
    
    /**
     * Get count of spam senders
     */
    public void getSpamSenderCount(RepositoryCallback<Integer> callback) {
        executor.execute(() -> {
            try {
                List<SpamSender> senders = getSpamSendersInternal();
                postCallback(callback, senders.size());
                
            } catch (Exception e) {
                postCallback(callback, 0);
            }
        });
    }
    
    // Helper class for collecting sender data
    private static class SpamSenderData {
        String phoneNumber;
        int totalCount = 0;
        int spamCount = 0;
        String sampleSpamMessage = null;
        long lastMessageDate = 0;
        
        SpamSenderData(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }
    }
    
    // Enum for sorting spam senders
    public enum SpamSenderSortType {
        MOST_SPAM,
        MOST_MESSAGES,
        RECENT_ACTIVITY,
        HIGHEST_SPAM_RATE
    }

    // ==================== BLOCKING OPERATIONS (Step 2) ====================
    
    // LiveData for blocked numbers
    private final MutableLiveData<List<String>> blockedNumbers = new MutableLiveData<>();
    
    public LiveData<List<String>> getBlockedNumbers() {
        return blockedNumbers;
    }
    
    /**
     * Block a single phone number using Android's BlockedNumberContract
     */
    public void blockPhoneNumber(String phoneNumber, BlockCallback callback) {
        executor.execute(() -> {
            try {
                // Check if already blocked
                if (isNumberBlockedInternal(phoneNumber)) {
                    postBlockCallback(callback, false, "Number is already blocked");
                    return;
                }
                
                // Insert into system blocked numbers
                ContentValues values = new ContentValues();
                values.put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, phoneNumber);
                
                Uri result = context.getContentResolver().insert(
                    BlockedNumberContract.BlockedNumbers.CONTENT_URI, values);
                
                boolean success = result != null;
                
                if (success) {
                    // Update the SpamSender object in our data
                    updateSpamSenderBlockedStatus(phoneNumber, true);
                    // Refresh blocked numbers list
                    loadBlockedNumbers();
                    postBlockCallback(callback, true, "Successfully blocked " + phoneNumber);
                } else {
                    postBlockCallback(callback, false, "Failed to block number");
                }
                
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error blocking number: " + e.getMessage(), e);
                postBlockCallback(callback, false, "Error: " + e.getMessage());
            }
        });
    }
    
    /**
     * Unblock a phone number
     */
    public void unblockPhoneNumber(String phoneNumber, BlockCallback callback) {
        executor.execute(() -> {
            try {
                // Delete from system blocked numbers
                String selection = BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER + " = ?";
                String[] selectionArgs = {phoneNumber};
                
                int deletedRows = context.getContentResolver().delete(
                    BlockedNumberContract.BlockedNumbers.CONTENT_URI, 
                    selection, 
                    selectionArgs);
                
                boolean success = deletedRows > 0;
                
                if (success) {
                    // Update the SpamSender object in our data
                    updateSpamSenderBlockedStatus(phoneNumber, false);
                    // Refresh blocked numbers list
                    loadBlockedNumbers();
                    postBlockCallback(callback, true, "Successfully unblocked " + phoneNumber);
                } else {
                    postBlockCallback(callback, false, "Number was not blocked or already unblocked");
                }
                
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error unblocking number: " + e.getMessage(), e);
                postBlockCallback(callback, false, "Error: " + e.getMessage());
            }
        });
    }
    
    /**
     * Block multiple phone numbers with progress updates
     */
    public void blockMultipleNumbers(List<String> phoneNumbers, BulkBlockCallback callback) {
        executor.execute(() -> {
            try {
                setLoading(true);
                clearError();
                
                int total = phoneNumbers.size();
                int blockedCount = 0;
                int alreadyBlockedCount = 0;
                int failedCount = 0;
                
                for (int i = 0; i < phoneNumbers.size(); i++) {
                    String phoneNumber = phoneNumbers.get(i);
                    
                    try {
                        // Check if already blocked
                        if (isNumberBlockedInternal(phoneNumber)) {
                            alreadyBlockedCount++;
                        } else {
                            // Insert into system blocked numbers
                            ContentValues values = new ContentValues();
                            values.put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, phoneNumber);
                            
                            Uri result = context.getContentResolver().insert(
                                BlockedNumberContract.BlockedNumbers.CONTENT_URI, values);
                            
                            if (result != null) {
                                blockedCount++;
                                updateSpamSenderBlockedStatus(phoneNumber, true);
                            } else {
                                failedCount++;
                            }
                        }
                        
                        // Update progress every number or on last number
                        final int current = i + 1;
                        final int blocked = blockedCount;
                        final int alreadyBlocked = alreadyBlockedCount;
                        final int failed = failedCount;
                        
                        mainHandler.post(() -> {
                            if (callback != null) {
                                callback.onProgress(current, total, blocked, alreadyBlocked, failed);
                            }
                        });
                        
                        // Small delay to prevent overwhelming the system
                        Thread.sleep(50);
                        
                    } catch (Exception e) {
                        failedCount++;
                        android.util.Log.e(TAG, "Error blocking " + phoneNumber + ": " + e.getMessage());
                    }
                }
                
                // Refresh data
                loadBlockedNumbers();
                loadSpamSenders();
                
                final int finalBlocked = blockedCount;
                final int finalAlreadyBlocked = alreadyBlockedCount;
                final int finalFailed = failedCount;
                
                mainHandler.post(() -> {
                    setLoading(false);
                    if (callback != null) {
                        callback.onCompleted(finalBlocked, finalAlreadyBlocked, finalFailed);
                    }
                });
                
            } catch (Exception e) {
                mainHandler.post(() -> {
                    setLoading(false);
                    String error = "Bulk blocking error: " + e.getMessage();
                    postError(error);
                    if (callback != null) {
                        callback.onError(error);
                    }
                });
            }
        });
    }
    
    /**
     * Check if a phone number is blocked
     */
    public void isNumberBlocked(String phoneNumber, RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                boolean blocked = isNumberBlockedInternal(phoneNumber);
                postCallback(callback, blocked);
            } catch (Exception e) {
                postCallback(callback, false);
            }
        });
    }
    
    /**
     * Internal method to check if number is blocked (synchronous)
     */
    private boolean isNumberBlockedInternal(String phoneNumber) {
        try {
            String selection = BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER + " = ?";
            String[] selectionArgs = {phoneNumber};
            
            try (Cursor cursor = context.getContentResolver().query(
                    BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                    new String[]{BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER},
                    selection,
                    selectionArgs,
                    null)) {
                
                return cursor != null && cursor.getCount() > 0;
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error checking if number is blocked: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Load all blocked numbers from system
     */
    public void loadBlockedNumbers() {
        executor.execute(() -> {
            try {
                List<String> blocked = new ArrayList<>();
                
                try (Cursor cursor = context.getContentResolver().query(
                        BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                        new String[]{BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER},
                        null,
                        null,
                        null)) {
                    
                    if (cursor != null && cursor.moveToFirst()) {
                        do {
                            String phoneNumber = cursor.getString(0);
                            if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                                blocked.add(phoneNumber);
                            }
                        } while (cursor.moveToNext());
                    }
                }
                
                postResult(blockedNumbers, blocked);
                
                // Update spam senders with blocked status
                updateAllSpamSendersBlockedStatus(blocked);
                
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error loading blocked numbers: " + e.getMessage());
                postResult(blockedNumbers, new ArrayList<>());
            }
        });
    }
    
    /**
     * Get count of blocked numbers
     */
    public void getBlockedNumberCount(RepositoryCallback<Integer> callback) {
        executor.execute(() -> {
            try {
                int count = 0;
                try (Cursor cursor = context.getContentResolver().query(
                        BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                        new String[]{BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER},
                        null,
                        null,
                        null)) {
                    
                    if (cursor != null) {
                        count = cursor.getCount();
                    }
                }
                
                postCallback(callback, count);
                
            } catch (Exception e) {
                postCallback(callback, 0);
            }
        });
    }
    
    /**
     * Update blocked status for a specific spam sender
     */
    private void updateSpamSenderBlockedStatus(String phoneNumber, boolean isBlocked) {
        List<SpamSender> currentSenders = spamSenders.getValue();
        if (currentSenders != null) {
            for (SpamSender sender : currentSenders) {
                if (phoneNumber.equals(sender.phoneNumber)) {
                    sender.isBlocked = isBlocked;
                    break;
                }
            }
            // Update LiveData on main thread
            mainHandler.post(() -> spamSenders.setValue(currentSenders));
        }
    }
    
    /**
     * Update blocked status for all spam senders based on system blocked numbers
     */
    private void updateAllSpamSendersBlockedStatus(List<String> blockedNumbers) {
        List<SpamSender> currentSenders = spamSenders.getValue();
        if (currentSenders != null) {
            for (SpamSender sender : currentSenders) {
                sender.isBlocked = blockedNumbers.contains(sender.phoneNumber);
            }
            // Update LiveData on main thread
            mainHandler.post(() -> spamSenders.setValue(currentSenders));
        }
    }
    
    /**
     * Helper method to post block callback results
     */
    private void postBlockCallback(BlockCallback callback, boolean success, String message) {
        if (callback != null) {
            mainHandler.post(() -> callback.onResult(success, message));
        }
    }
    
    // Callback interfaces for blocking operations
    public interface BlockCallback {
        void onResult(boolean success, String message);
    }
    
    public interface BulkBlockCallback {
        void onProgress(int current, int total, int blocked, int alreadyBlocked, int failed);
        void onCompleted(int blockedCount, int alreadyBlockedCount, int failedCount);
        void onError(String error);
    }

    // Callback interface for async operations
    public interface RepositoryCallback<T> {
        void onResult(T result);
    }

    // Cleanup method
    public void cleanup() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}