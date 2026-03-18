package com.amw.budgetbuddy;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    private List<String> categories;
    private OnCategoryClickListener listener;

    // Interface to send click events to the Activity
    public interface OnCategoryClickListener {
        void onCategoryClick(String categoryName);
    }

    public CategoryAdapter(List<String> categories, OnCategoryClickListener listener) {
        this.categories = categories;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Button btn = new Button(parent.getContext());

        // USE RECYCLERVIEW LAYOUT PARAMS (This fixes the crash)
        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                150); // Height in pixels

        // Set Margins here safely
        params.setMargins(10, 10, 10, 10);
        btn.setLayoutParams(params);

        return new ViewHolder(btn);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String name = categories.get(position);
        holder.btn.setText(name);

        // Styling
        int textColor = ContextCompat.getColor(holder.btn.getContext(), R.color.text_primary);
        holder.btn.setTextColor(textColor);
        holder.btn.setBackgroundResource(R.drawable.bg_category_btn);

        // Normal Click Listener (Select Category)
        holder.btn.setOnClickListener(v -> {
            listener.onCategoryClick(name);
        });

        // NEW: Long Click Listener (Delete Category Popup)
        holder.btn.setOnLongClickListener(v -> {
            // Check if the context is your AddTransactionActivity
            if (v.getContext() instanceof AddTransactionActivity) {
                // Pass 'v' (the button) so the popup anchors exactly to it
                ((AddTransactionActivity) v.getContext()).showDeleteCategoryPopup(v, name, position);
            }
            return true; // Return true so Android knows the long-press was handled
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        Button btn;
        public ViewHolder(View itemView) {
            super(itemView);
            btn = (Button) itemView;
        }
    }
}