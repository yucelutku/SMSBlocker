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
            // Set basic message info
            binding.senderText.setText(message.getSenderName());
            binding.messageBodyText.setText(message.body != null ? message.body : "");
            binding.dateText.setText(formatDate(message.date));
            binding.messageIdText.setText("ID: " + message.id);

            // Set message type
            String typeText = getMessageTypeText(message.type);
            binding.typeText.setText(typeText);

            // Handle spam detection and highlighting
            if (message.isSpam) {
                setupSpamMessage(message);
            } else {
                setupCleanMessage();
            }

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