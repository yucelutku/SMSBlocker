package com.example.testapplication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testapplication.databinding.ItemKeywordBinding;

import java.util.ArrayList;
import java.util.List;

public class KeywordListAdapter extends RecyclerView.Adapter<KeywordListAdapter.KeywordViewHolder> {
    
    private List<String> keywords;
    private final boolean isDeletable;
    private final OnKeywordDeleteListener deleteListener;
    
    public interface OnKeywordDeleteListener {
        void onDelete(String keyword);
    }
    
    public KeywordListAdapter(List<String> keywords, boolean isDeletable, OnKeywordDeleteListener deleteListener) {
        this.keywords = new ArrayList<>(keywords);
        this.isDeletable = isDeletable;
        this.deleteListener = deleteListener;
    }
    
    public void updateKeywords(List<String> newKeywords) {
        this.keywords = new ArrayList<>(newKeywords);
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public KeywordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemKeywordBinding binding = ItemKeywordBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new KeywordViewHolder(binding);
    }
    
    @Override
    public void onBindViewHolder(@NonNull KeywordViewHolder holder, int position) {
        holder.bind(keywords.get(position));
    }
    
    @Override
    public int getItemCount() {
        return keywords.size();
    }
    
    class KeywordViewHolder extends RecyclerView.ViewHolder {
        private final ItemKeywordBinding binding;
        
        KeywordViewHolder(ItemKeywordBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
        
        void bind(String keyword) {
            binding.keywordText.setText(keyword);
            
            if (isDeletable && deleteListener != null) {
                binding.deleteButton.setVisibility(View.VISIBLE);
                binding.deleteButton.setOnClickListener(v -> 
                    deleteListener.onDelete(keyword));
            } else {
                binding.deleteButton.setVisibility(View.GONE);
            }
        }
    }
}