package com.example.testapplication.dialogs;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testapplication.R;
import com.example.testapplication.adapters.SenderListAdapter;
import com.example.testapplication.repositories.SmsRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Advanced Bulk Delete Dialog with comprehensive options
 * Material Design 3 compliant dialog for bulk SMS operations
 */
public class BulkDeleteDialog {
    
    public interface BulkDeleteListener {
        void onDeleteAll();
        void onDeleteSpam();
        void onDeleteNormal();
        void onDeleteBySender(String phoneNumber);
        void onDeleteByDateRange(long startDate, long endDate);
        void onDeleteByDateRangeAndType(long startDate, long endDate, boolean isSpam);
    }
    
    private final Context context;
    private final BulkDeleteListener listener;
    private final SmsRepository repository;
    private AlertDialog currentDialog;
    private ProgressBar progressBar;
    private TextView progressText;
    
    // Date range variables
    private long selectedStartDate = 0;
    private long selectedEndDate = 0;
    private String selectedSender = null;
    
    // Predefined date ranges (in milliseconds)
    private static final long DAY_MS = 24 * 60 * 60 * 1000L;
    private static final long WEEK_MS = 7 * DAY_MS;
    private static final long MONTH_MS = 30 * DAY_MS;
    
    public BulkDeleteDialog(Context context, BulkDeleteListener listener, SmsRepository repository) {
        this.context = context;
        this.listener = listener;
        this.repository = repository;
    }
    
    /**
     * Show main bulk delete options dialog
     */
    public void showMainDialog() {
        // Get current message counts for button labels
        repository.getMessageCountByType(false, normalCount -> {
            repository.getMessageCountByType(true, spamCount -> {
                int totalCount = normalCount + spamCount;
                
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
                builder.setTitle(R.string.advanced_delete_options);
                
                // Create custom layout for main options
                View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_bulk_delete_main, null);
                builder.setView(dialogView);
                
                // Initialize main action buttons
                MaterialButton btnDeleteAll = dialogView.findViewById(R.id.btn_delete_all);
                MaterialButton btnDeleteSpam = dialogView.findViewById(R.id.btn_delete_spam);
                MaterialButton btnDeleteNormal = dialogView.findViewById(R.id.btn_delete_normal);
                MaterialButton btnAdvancedOptions = dialogView.findViewById(R.id.btn_advanced_options);
                
                // Set button texts with counts
                btnDeleteAll.setText(context.getString(R.string.bulk_delete_all, totalCount));
                btnDeleteSpam.setText(context.getString(R.string.bulk_delete_spam, spamCount));
                btnDeleteNormal.setText(context.getString(R.string.bulk_delete_normal, normalCount));
                
                // Set click listeners
                btnDeleteAll.setOnClickListener(v -> {
                    currentDialog.dismiss();
                    showConfirmationDialog("all", totalCount, () -> listener.onDeleteAll());
                });
                
                btnDeleteSpam.setOnClickListener(v -> {
                    currentDialog.dismiss();
                    showConfirmationDialog("spam", spamCount, () -> listener.onDeleteSpam());
                });
                
                btnDeleteNormal.setOnClickListener(v -> {
                    currentDialog.dismiss();
                    showConfirmationDialog("normal", normalCount, () -> listener.onDeleteNormal());
                });
                
                btnAdvancedOptions.setOnClickListener(v -> {
                    currentDialog.dismiss();
                    showAdvancedOptionsDialog();
                });
                
                builder.setNegativeButton(R.string.action_cancel, null);
                
                currentDialog = builder.create();
                currentDialog.show();
            });
        });
    }
    
    /**
     * Show advanced deletion options
     */
    private void showAdvancedOptionsDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(R.string.advanced_delete_options);
        
        String[] options = {
            context.getString(R.string.delete_by_sender),
            context.getString(R.string.date_range_7_days),
            context.getString(R.string.date_range_30_days),
            context.getString(R.string.date_range_3_months),
            context.getString(R.string.date_range_6_months),
            context.getString(R.string.date_range_custom)
        };
        
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // By sender
                    showSenderSelectionDialog();
                    break;
                case 1: // 7 days
                    setDateRangeAndConfirm(7 * DAY_MS);
                    break;
                case 2: // 30 days
                    setDateRangeAndConfirm(30 * DAY_MS);
                    break;
                case 3: // 3 months
                    setDateRangeAndConfirm(90 * DAY_MS);
                    break;
                case 4: // 6 months
                    setDateRangeAndConfirm(180 * DAY_MS);
                    break;
                case 5: // Custom range
                    showCustomDateRangeDialog();
                    break;
            }
        });
        
        builder.setNegativeButton(R.string.action_cancel, null);
        builder.setNeutralButton("⬅ Geri", (dialog, which) -> showMainDialog());
        
        currentDialog = builder.create();
        currentDialog.show();
    }
    
    /**
     * Show sender selection dialog with frequent senders
     */
    private void showSenderSelectionDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(R.string.frequent_senders);
        
        // Create loading dialog first
        View loadingView = LayoutInflater.from(context).inflate(R.layout.dialog_loading, null);
        TextView loadingText = loadingView.findViewById(R.id.loading_text);
        loadingText.setText(R.string.progress_loading_senders);
        
        builder.setView(loadingView);
        AlertDialog loadingDialog = builder.create();
        loadingDialog.show();
        
        // Load frequent senders
        repository.getFrequentSenders(senderList -> {
            loadingDialog.dismiss();
            
            if (senderList.isEmpty()) {
                // No senders found
                new MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.frequent_senders)
                    .setMessage(R.string.no_frequent_senders)
                    .setPositiveButton("OK", null)
                    .setNeutralButton("⬅ Geri", (dialog, which) -> showAdvancedOptionsDialog())
                    .show();
                return;
            }
            
            // Show sender list dialog
            showSenderListDialog(senderList);
        });
    }
    
    /**
     * Show sender list with RecyclerView
     */
    private void showSenderListDialog(List<SmsRepository.SenderInfo> senderList) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(R.string.select_sender);
        
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_sender_list, null);
        RecyclerView recyclerView = dialogView.findViewById(R.id.sender_recycler_view);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        SenderListAdapter adapter = new SenderListAdapter(senderList, senderInfo -> {
            currentDialog.dismiss();
            selectedSender = senderInfo.phoneNumber;
            
            // Show delete options for this sender
            showSenderDeleteOptions(senderInfo);
        });
        recyclerView.setAdapter(adapter);
        
        builder.setView(dialogView);
        builder.setNegativeButton(R.string.action_cancel, null);
        builder.setNeutralButton("⬅ Geri", (dialog, which) -> showAdvancedOptionsDialog());
        
        currentDialog = builder.create();
        currentDialog.show();
    }
    
    /**
     * Show delete options for selected sender
     */
    private void showSenderDeleteOptions(SmsRepository.SenderInfo senderInfo) {
        String senderName = senderInfo.getDisplayName();
        int totalCount = senderInfo.totalCount;
        int spamCount = senderInfo.spamCount;
        int normalCount = totalCount - spamCount;
        
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(context.getString(R.string.bulk_delete_sender, senderName, totalCount));
        
        String[] options = {
            context.getString(R.string.bulk_delete_sender, senderName, totalCount),
            context.getString(R.string.bulk_delete_spam, spamCount) + " (Sadece bu gönderenden)",
            context.getString(R.string.bulk_delete_normal, normalCount) + " (Sadece bu gönderenden)"
        };
        
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // All messages from sender
                    showConfirmationDialog("sender", totalCount, 
                        () -> listener.onDeleteBySender(senderInfo.phoneNumber));
                    break;
                case 1: // Only spam from sender
                    // This would require a new repository method
                    showConfirmationDialog("sender_spam", spamCount,
                        () -> deleteFromSenderByType(senderInfo.phoneNumber, true));
                    break;
                case 2: // Only normal from sender
                    showConfirmationDialog("sender_normal", normalCount,
                        () -> deleteFromSenderByType(senderInfo.phoneNumber, false));
                    break;
            }
        });
        
        builder.setNegativeButton(R.string.action_cancel, null);
        builder.setNeutralButton("⬅ Geri", (dialog, which) -> showSenderSelectionDialog());
        
        currentDialog = builder.create();
        currentDialog.show();
    }
    
    /**
     * Show custom date range picker
     */
    private void showCustomDateRangeDialog() {
        Calendar calendar = Calendar.getInstance();
        
        // Start date picker
        DatePickerDialog startDatePicker = new DatePickerDialog(context,
            (view, year, month, dayOfMonth) -> {
                Calendar startCal = Calendar.getInstance();
                startCal.set(year, month, dayOfMonth, 0, 0, 0);
                selectedStartDate = startCal.getTimeInMillis();
                
                // End date picker
                DatePickerDialog endDatePicker = new DatePickerDialog(context,
                    (endView, endYear, endMonth, endDayOfMonth) -> {
                        Calendar endCal = Calendar.getInstance();
                        endCal.set(endYear, endMonth, endDayOfMonth, 23, 59, 59);
                        selectedEndDate = endCal.getTimeInMillis();
                        
                        // Show date range confirmation
                        showDateRangeConfirmation();
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
                
                endDatePicker.setTitle(context.getString(R.string.select_end_date));
                endDatePicker.show();
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH));
        
        startDatePicker.setTitle(context.getString(R.string.select_start_date));
        startDatePicker.show();
    }
    
    /**
     * Set predefined date range and show confirmation
     */
    private void setDateRangeAndConfirm(long rangeDuration) {
        selectedEndDate = System.currentTimeMillis();
        selectedStartDate = selectedEndDate - rangeDuration;
        showDateRangeConfirmation();
    }
    
    /**
     * Show date range deletion confirmation
     */
    private void showDateRangeConfirmation() {
        // Get message count for date range
        repository.getMessageCountByDateRange(selectedStartDate, selectedEndDate, messageCount -> {
            if (messageCount == 0) {
                new MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.error_no_messages)
                    .setMessage("Seçilen tarih aralığında mesaj bulunamadı.")
                    .setPositiveButton("OK", null)
                    .setNeutralButton("⬅ Geri", (dialog, which) -> showAdvancedOptionsDialog())
                    .show();
                return;
            }
            
            // Show options for date range
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", new Locale("tr", "TR"));
            String startDateStr = sdf.format(new Date(selectedStartDate));
            String endDateStr = sdf.format(new Date(selectedEndDate));
            
            String[] options = {
                context.getString(R.string.bulk_delete_date_range, startDateStr, endDateStr, messageCount),
                context.getString(R.string.delete_spam_in_range),
                context.getString(R.string.delete_normal_in_range)
            };
            
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            builder.setTitle(context.getString(R.string.select_date_range));
            
            builder.setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: // All messages in range
                        showConfirmationDialog("date_range", messageCount,
                            () -> listener.onDeleteByDateRange(selectedStartDate, selectedEndDate));
                        break;
                    case 1: // Only spam in range
                        showConfirmationDialog("date_range_spam", -1,
                            () -> listener.onDeleteByDateRangeAndType(selectedStartDate, selectedEndDate, true));
                        break;
                    case 2: // Only normal in range
                        showConfirmationDialog("date_range_normal", -1,
                            () -> listener.onDeleteByDateRangeAndType(selectedStartDate, selectedEndDate, false));
                        break;
                }
            });
            
            builder.setNegativeButton(R.string.action_cancel, null);
            builder.setNeutralButton("⬅ Geri", (dialog, which) -> showAdvancedOptionsDialog());
            
            currentDialog = builder.create();
            currentDialog.show();
        });
    }
    
    /**
     * Show confirmation dialog before deletion
     */
    private void showConfirmationDialog(String type, int count, Runnable onConfirm) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        
        // Set title and message based on type
        switch (type) {
            case "all":
                builder.setTitle(R.string.confirm_delete_all);
                builder.setMessage(context.getString(R.string.confirm_delete_all_message, count));
                break;
            case "spam":
                builder.setTitle(R.string.confirm_delete_spam);
                builder.setMessage(context.getString(R.string.confirm_delete_spam_message, count));
                break;
            case "normal":
                builder.setTitle(R.string.confirm_delete_normal);
                builder.setMessage(context.getString(R.string.confirm_delete_normal_message, count));
                break;
            case "sender":
                builder.setTitle(R.string.confirm_delete_sender);
                builder.setMessage(context.getString(R.string.confirm_delete_sender_message, selectedSender, count));
                break;
            case "date_range":
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", new Locale("tr", "TR"));
                String startDateStr = sdf.format(new Date(selectedStartDate));
                String endDateStr = sdf.format(new Date(selectedEndDate));
                builder.setTitle(R.string.confirm_delete_date_range);
                builder.setMessage(context.getString(R.string.confirm_delete_date_range_message, 
                    startDateStr, endDateStr, count));
                break;
            default:
                builder.setTitle(R.string.action_delete);
                builder.setMessage("Bu işlem geri alınamaz. Devam etmek istiyor musunuz?");
                break;
        }
        
        builder.setPositiveButton(R.string.action_delete, (dialog, which) -> onConfirm.run());
        builder.setNegativeButton(R.string.action_cancel, null);
        
        currentDialog = builder.create();
        currentDialog.show();
    }
    
    /**
     * Show progress dialog during bulk operations
     */
    public void showProgressDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle("Silme İşlemi");
        builder.setCancelable(false);
        
        View progressView = LayoutInflater.from(context).inflate(R.layout.dialog_progress, null);
        progressBar = progressView.findViewById(R.id.progress_bar);
        progressText = progressView.findViewById(R.id.progress_text);
        
        progressText.setText(R.string.progress_analyzing);
        
        builder.setView(progressView);
        builder.setNegativeButton(R.string.action_cancel, (dialog, which) -> {
            // Cancel operation logic here
        });
        
        currentDialog = builder.create();
        currentDialog.show();
    }
    
    /**
     * Update progress dialog
     */
    public void updateProgress(int current, int total) {
        if (progressBar != null && progressText != null) {
            int percentage = (int) ((float) current / total * 100);
            progressBar.setProgress(percentage);
            progressText.setText(context.getString(R.string.progress_deleting, current, total));
        }
    }
    
    /**
     * Hide current dialog
     */
    public void hideDialog() {
        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.dismiss();
        }
    }
    
    /**
     * Delete from sender by type (requires custom implementation)
     */
    private void deleteFromSenderByType(String phoneNumber, boolean isSpam) {
        // This would need to be implemented in SmsRepository
        // For now, delegate to the listener
        // listener.onDeleteBySenderAndType(phoneNumber, isSpam);
    }
}
