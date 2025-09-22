package com.example.testapplication.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.example.testapplication.models.SmsMessage;
import com.example.testapplication.repositories.SmsRepository;
import com.example.testapplication.utils.SmsHelper;

import java.util.List;

public class SmsViewModel extends AndroidViewModel {
    private final SmsRepository repository;
    
    // LiveData observables
    private final LiveData<List<SmsMessage>> allMessages;
    private final LiveData<List<SmsMessage>> inboxMessages;
    private final LiveData<List<SmsMessage>> spamMessages;
    private final LiveData<SmsHelper.SmsStatistics> statistics;
    private final LiveData<Boolean> isLoading;
    private final LiveData<String> errorMessage;
    
    // Computed LiveData
    private final MediatorLiveData<String> statusText;
    private final MediatorLiveData<Boolean> hasData;

    public SmsViewModel(@NonNull Application application) {
        super(application);
        
        repository = SmsRepository.getInstance(application);
        
        // Initialize LiveData from repository
        allMessages = repository.getAllMessages();
        inboxMessages = repository.getInboxMessages();
        spamMessages = repository.getSpamMessages();
        statistics = repository.getStatistics();
        isLoading = repository.getIsLoading();
        errorMessage = repository.getErrorMessage();
        
        // Setup computed LiveData
        statusText = new MediatorLiveData<>();
        hasData = new MediatorLiveData<>();
        
        setupComputedLiveData();
        
        // Load initial data
        refreshData();
    }

    private void setupComputedLiveData() {
        // Status text based on statistics and loading state
        statusText.addSource(statistics, stats -> updateStatusText());
        statusText.addSource(isLoading, loading -> updateStatusText());
        statusText.addSource(errorMessage, error -> updateStatusText());
        
        // Has data indicator
        hasData.addSource(allMessages, messages -> 
            hasData.setValue(messages != null && !messages.isEmpty()));
    }

    private void updateStatusText() {
        Boolean loading = isLoading.getValue();
        String error = errorMessage.getValue();
        SmsHelper.SmsStatistics stats = statistics.getValue();
        
        if (error != null && !error.isEmpty()) {
            statusText.setValue("Error: " + error);
        } else if (loading != null && loading) {
            statusText.setValue("Loading SMS messages...");
        } else if (stats != null) {
            if (stats.totalMessages == 0) {
                statusText.setValue("No SMS messages found. Enable permissions to view messages.");
            } else {
                statusText.setValue(String.format("Protection active - %d messages scanned, %d spam blocked", 
                    stats.totalMessages, stats.spamMessages));
            }
        } else {
            statusText.setValue("Welcome to SMS Spam Blocker! Enable protection to start monitoring.");
        }
    }

    // Public getters for LiveData
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

    public LiveData<String> getStatusText() {
        return statusText;
    }

    public LiveData<Boolean> getHasData() {
        return hasData;
    }

    // Public methods for UI actions
    public void refreshData() {
        repository.refreshAllData();
    }

    public void loadMessages() {
        repository.loadAllMessages();
    }

    public void loadMessages(int limit) {
        repository.loadAllMessages(limit);
    }

    public void loadInboxMessages() {
        repository.loadInboxMessages();
    }

    public void loadInboxMessages(int limit) {
        repository.loadInboxMessages(limit);
    }

    public void loadSpamMessages() {
        repository.loadSpamMessages();
    }

    public void deleteMessage(long messageId, SmsRepository.RepositoryCallback<Boolean> callback) {
        repository.deleteMessage(messageId, callback);
    }

    public void deleteAllSpamMessages(SmsRepository.RepositoryCallback<Integer> callback) {
        repository.deleteSpamMessages(callback);
    }

    public void getMessageById(long messageId, SmsRepository.RepositoryCallback<SmsMessage> callback) {
        repository.getMessageById(messageId, callback);
    }

    // Utility methods
    public int getTotalMessageCount() {
        SmsHelper.SmsStatistics stats = statistics.getValue();
        return stats != null ? stats.totalMessages : 0;
    }

    public int getSpamMessageCount() {
        SmsHelper.SmsStatistics stats = statistics.getValue();
        return stats != null ? stats.spamMessages : 0;
    }

    public int getInboxMessageCount() {
        SmsHelper.SmsStatistics stats = statistics.getValue();
        return stats != null ? stats.inboxMessages : 0;
    }

    public int getTodayMessageCount() {
        SmsHelper.SmsStatistics stats = statistics.getValue();
        return stats != null ? stats.todayMessages : 0;
    }

    public boolean hasError() {
        String error = errorMessage.getValue();
        return error != null && !error.isEmpty();
    }

    public boolean isDataLoaded() {
        Boolean hasDataValue = hasData.getValue();
        return hasDataValue != null && hasDataValue;
    }

    public boolean isCurrentlyLoading() {
        Boolean loading = isLoading.getValue();
        return loading != null && loading;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Cleanup repository resources if needed
        // Note: Repository is singleton, so we don't clean it up here
    }
}