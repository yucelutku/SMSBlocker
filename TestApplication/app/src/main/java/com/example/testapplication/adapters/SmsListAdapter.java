package com.example.testapplication.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testapplication.R;
import com.example.testapplication.databinding.ItemSmsMessageBinding;
import com.example.testapplication.models.SmsMessage;
import com.example.testapplication.utils.SpamDetector;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SmsListAdapter extends ListAdapter<SmsMessage, SmsListAdapter.SmsViewHolder> {

    public interface OnSmsActionListener {
        void onDeleteMessage(SmsMessage message);
        void onMessageClick(SmsMessage message);
    }

    private OnSmsActionListener actionListener;

    public SmsListAdapter() {
        super(new SmsMessageDiffCallback());
    }

    public void setOnSmsActionListener(OnSmsActionListener listener) {
        this.actionListener = listener;
    }

    @NonNull
    @Override
    public SmsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSmsMessageBinding binding = ItemSmsMessageBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new SmsViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SmsViewHolder holder, int position) {
        SmsMessage message = getItem(position);
        holder.bind(message);
    }

    class SmsViewHolder extends RecyclerView.ViewHolder {
        private final ItemSmsMessageBinding binding;

        public SmsViewHolder(@NonNull ItemSmsMessageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(SmsMessage message) {
            String contactName = com.example.testapplication.utils.ContactsHelper.getContactName(
                itemView.getContext(), 
                message.address
            );
            
            binding.senderText.setText(contactName);
            
            if (!contactName.equals(message.address)) {
                binding.messageIdText.setText(message.address + " • ID: " + message.id);
            } else {
                binding.messageIdText.setText("ID: " + message.id);
            }
            
            binding.messageBodyText.setText(message.body != null ? message.body : "");
            binding.dateText.setText(formatDate(message.date));

            // Set message type
            String typeText = getMessageTypeText(message.type);
            binding.typeText.setText(typeText);

            // Handle spam detection and highlighting
            if (message.isSpam) {
                setupSpamMessage(message);
            } else {
                setupCleanMessage();
            }
            
            // Setup context analysis (for all messages)
            setupContextAnalysis(message);

            // Set click listeners
            binding.deleteButton.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onDeleteMessage(message);
                }
            });

            binding.messageCard.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onMessageClick(message);
                }
            });
            
            // Setup expand/collapse functionality
            binding.expandButton.setOnClickListener(v -> {
                toggleContextAnalysis();
            });

            // Set long click for additional actions
            binding.messageCard.setOnLongClickListener(v -> {
                // Could show context menu in future
                return true;
            });
        }

        private void setupSpamMessage(SmsMessage message) {
            // Show spam chip
            binding.spamChip.setVisibility(View.VISIBLE);
            
            // Show spam score
            binding.spamScoreText.setVisibility(View.VISIBLE);
            binding.spamScoreText.setText(String.format(Locale.getDefault(), 
                "Spam Score: %.0f%%", message.spamScore * 100));

            // Apply Turkish spam highlighting - red background
            binding.messageCard.setCardBackgroundColor(
                ContextCompat.getColor(itemView.getContext(), android.R.color.holo_red_light));
            
            // Adjust text colors for better contrast on red background
            binding.messageBodyText.setTextColor(
                ContextCompat.getColor(itemView.getContext(), android.R.color.black));
            binding.senderText.setTextColor(
                ContextCompat.getColor(itemView.getContext(), android.R.color.black));

            // Make spam chip more prominent
            binding.spamChip.setChipBackgroundColor(
                ContextCompat.getColorStateList(itemView.getContext(), android.R.color.holo_red_dark));
        }

        private void setupCleanMessage() {
            // Hide spam indicators
            binding.spamChip.setVisibility(View.GONE);
            binding.spamScoreText.setVisibility(View.GONE);

            // Use default card background
            binding.messageCard.setCardBackgroundColor(
                ContextCompat.getColor(itemView.getContext(), android.R.color.white));

            // Use default text colors
            binding.messageBodyText.setTextColor(
                ContextCompat.getColor(itemView.getContext(), android.R.color.black));
            binding.senderText.setTextColor(
                ContextCompat.getColor(itemView.getContext(), android.R.color.black));
        }
        
        /**
         * Setup context analysis display for message
         */
        private void setupContextAnalysis(SmsMessage message) {
            // Analyze message with context
            SpamDetector.SpamAnalysisResult result = SpamDetector.analyzeMessage(
                message.body, message.address, itemView.getContext());
            
            // Show expand button for messages with context data
            if (result.contextAnalysis != null && result.contextAnalysis.messageLength > 0) {
                binding.expandButton.setVisibility(View.VISIBLE);
                
                // Build context details text
                String contextDetails = buildContextDetailsText(result.contextAnalysis);
                binding.contextDetailsText.setText(contextDetails);
            } else {
                binding.expandButton.setVisibility(View.GONE);
                binding.contextAnalysisLayout.setVisibility(View.GONE);
            }
        }
        
        /**
         * Build context analysis details text
         */
        private String buildContextDetailsText(SpamDetector.ContextAnalysis context) {
            StringBuilder details = new StringBuilder();
            
            // Message length info
            details.append("• Mesaj uzunluğu: ").append(context.messageLength)
                   .append(" karakter (").append(context.lengthCategory).append(")\n");
            
            // Keyword count and density
            details.append("• Anahtar kelime sayısı: ").append(context.keywordCount).append("\n");
            details.append("• Kelime yoğunluğu: %").append(String.format(Locale.getDefault(), "%.1f", context.keywordDensity)).append("\n");
            
            // Context multiplier
            details.append("• Bağlam çarpanı: ").append(String.format(Locale.getDefault(), "%.1fx", context.contextMultiplier));
            
            // Context description
            if (!context.contextDescription.isEmpty()) {
                details.append("\n• ").append(context.contextDescription);
            }
            
            return details.toString();
        }
        
        /**
         * Toggle context analysis visibility
         */
        private void toggleContextAnalysis() {
            if (binding.contextAnalysisLayout.getVisibility() == View.GONE) {
                // Expand
                binding.contextAnalysisLayout.setVisibility(View.VISIBLE);
                binding.expandButton.setText("Detayları Gizle");
                binding.expandButton.setIconResource(android.R.drawable.arrow_up_float);
            } else {
                // Collapse
                binding.contextAnalysisLayout.setVisibility(View.GONE);
                binding.expandButton.setText("Detayları Göster");
                binding.expandButton.setIconResource(android.R.drawable.arrow_down_float);
            }
        }

        private String formatDate(long timestamp) {
            try {
                Date date = new Date(timestamp);
                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm", 
                    new Locale("tr", "TR"));
                return formatter.format(date);
            } catch (Exception e) {
                return "Unknown date";
            }
        }

        private String getMessageTypeText(int type) {
            switch (type) {
                case 1:
                    return "INBOX";
                case 2:
                    return "SENT";
                case 3:
                    return "DRAFT";
                case 4:
                    return "OUTBOX";
                case 5:
                    return "FAILED";
                case 6:
                    return "QUEUED";
                default:
                    return "UNKNOWN";
            }
        }
    }

    private static class SmsMessageDiffCallback extends DiffUtil.ItemCallback<SmsMessage> {
        @Override
        public boolean areItemsTheSame(@NonNull SmsMessage oldItem, @NonNull SmsMessage newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull SmsMessage oldItem, @NonNull SmsMessage newItem) {
            return oldItem.id == newItem.id &&
                   oldItem.isSpam == newItem.isSpam &&
                   oldItem.spamScore == newItem.spamScore &&
                   java.util.Objects.equals(oldItem.body, newItem.body) &&
                   java.util.Objects.equals(oldItem.address, newItem.address) &&
                   oldItem.date == newItem.date &&
                   oldItem.type == newItem.type;
        }
    }
}