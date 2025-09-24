package com.example.testapplication.repositories;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.testapplication.models.SmsMessage;
import com.example.testapplication.utils.SmsHelper;

import java.util.List;
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
        android.util.Log.d(TAG, "[REPO] Delete message request received for ID: " + messageId);
        
        executor.execute(() -> {
            try {
                android.util.Log.d(TAG, "[REPO] Executing delete on background thread for message ID: " + messageId);
                
                boolean success = SmsHelper.deleteSmsMessage(context, messageId);
                
                android.util.Log.d(TAG, "[REPO] Delete operation result: " + success + " for message ID: " + messageId);
                
                postCallback(callback, success);
                
                if (success) {
                    android.util.Log.i(TAG, "[REPO] Deletion successful, refreshing all data");
                    refreshAllData();
                } else {
                    android.util.Log.w(TAG, "[REPO] Deletion failed for message ID: " + messageId);
                }
                
            } catch (Exception e) {
                android.util.Log.e(TAG, "[REPO] Exception during delete: " + e.getMessage(), e);
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