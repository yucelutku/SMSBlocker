package com.example.testapplication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testapplication.R;
import com.example.testapplication.repositories.SmsRepository;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

/**
 * Adapter for displaying frequent SMS senders in selection dialog
 * Material Design 3 compliant list with sender information
 */
public class SenderListAdapter extends RecyclerView.Adapter<SenderListAdapter.SenderViewHolder> {
    
    public interface OnSenderClickListener {
        void onSenderSelected(SmsRepository.SenderInfo senderInfo);
    }
    
    private final List<SmsRepository.SenderInfo> senderList;
    private final OnSenderClickListener clickListener;
    
    public SenderListAdapter(List<SmsRepository.SenderInfo> senderList, OnSenderClickListener clickListener) {
        this.senderList = senderList;
        this.clickListener = clickListener;
    }
    
    @NonNull
    @Override
    public SenderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sender_selection, parent, false);
        return new SenderViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull SenderViewHolder holder, int position) {
        SmsRepository.SenderInfo senderInfo = senderList.get(position);
        holder.bind(senderInfo, clickListener);
    }
    
    @Override
    public int getItemCount() {
        return senderList.size();
    }
    
    static class SenderViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView senderText;
        private final TextView messageCountText;
        private final TextView spamInfoText;
        
        public SenderViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.sender_card);
            senderText = itemView.findViewById(R.id.sender_text);
            messageCountText = itemView.findViewById(R.id.message_count_text);
            spamInfoText = itemView.findViewById(R.id.spam_info_text);
        }
        
        public void bind(SmsRepository.SenderInfo senderInfo, OnSenderClickListener clickListener) {
            // Set sender display name
            senderText.setText(senderInfo.getDisplayName());
            
            // Set message count
            messageCountText.setText(
                itemView.getContext().getString(R.string.stats_total_messages, senderInfo.totalCount)
            );
            
            // Set spam info
            if (senderInfo.spamCount > 0) {
                float spamPercentage = senderInfo.getSpamPercentage();
                spamInfoText.setText(
                    String.format("🚫 %d spam (%.0f%%)", senderInfo.spamCount, spamPercentage)
                );
                spamInfoText.setTextColor(
                    itemView.getContext().getColor(android.R.color.holo_red_dark)
                );
                spamInfoText.setVisibility(View.VISIBLE);
            } else {
                spamInfoText.setText("✅ Spam bulunamadı");
                spamInfoText.setTextColor(
                    itemView.getContext().getColor(android.R.color.holo_green_dark)
                );
                spamInfoText.setVisibility(View.VISIBLE);
            }
            
            // Set click listener
            cardView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onSenderSelected(senderInfo);
                }
            });
            
            // Add ripple effect
            cardView.setClickable(true);
            cardView.setFocusable(true);
        }
    }
}
