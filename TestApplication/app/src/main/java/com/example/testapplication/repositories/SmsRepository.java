package com.example.testapplication.repositories;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.testapplication.models.SmsMessage;
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
            try {
                List<SmsMessage> allMessages = SmsHelper.getAllMessages(context);
                int count = 0;
                
                for (SmsMessage message : allMessages) {
                    if (message.isSpam == isSpam) {
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