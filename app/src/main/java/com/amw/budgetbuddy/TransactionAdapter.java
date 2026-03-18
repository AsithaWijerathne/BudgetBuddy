package com.amw.budgetbuddy;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private List<Transaction> transactionList;
    private OnItemClickListener listener; // Renamed to handle both clicks
    private int expandedPosition = -1; // -1 means nothing is expanded

    // NEW: Updated Interface to talk to MainActivity for both actions
    public interface OnItemClickListener {
        void onDeleteClick(Transaction transaction);
        void onEditClick(Transaction transaction);
    }

    // Constructor updated to use the new listener
    public TransactionAdapter(List<Transaction> transactionList, OnItemClickListener listener) {
        this.transactionList = transactionList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction t = transactionList.get(position);

        // --- Standard Data Binding ---
        holder.txtCategory.setText(t.category);
        holder.txtNote.setText(t.note.isEmpty() ? "No additional notes" : t.note);

        // Format Date (e.g., "Jan 21, 1:30 PM")
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());
        holder.txtDate.setText(sdf.format(new Date(t.timestamp)));

        if (t.type.equals("Income")) {
            holder.txtIncome.setText(String.valueOf((int) t.amount));
            holder.txtExpense.setText("");
        } else {
            holder.txtIncome.setText("");
            holder.txtExpense.setText("-" + (int) t.amount);
        }

        // --- Expansion Logic ---
        boolean isExpanded = position == expandedPosition;
        holder.layoutDetails.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

        // Toggle Expand/Collapse on Click
        holder.itemView.setOnClickListener(v -> {
            // If clicking the already expanded item, collapse it (-1). Otherwise expand current.
            expandedPosition = isExpanded ? -1 : position;
            notifyDataSetChanged(); // Refresh list to show/hide views
        });

        // --- Button Click Logic ---

        // Delete Button
        holder.btnDelete.setOnClickListener(v -> {
            listener.onDeleteClick(t);
        });

        // NEW: Edit Button
        holder.btnEdit.setOnClickListener(v -> {
            listener.onEditClick(t);
        });
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtCategory, txtIncome, txtExpense, txtDate, txtNote;
        LinearLayout layoutDetails;
        ImageButton btnDelete, btnEdit; // Added btnEdit here

        public ViewHolder(View itemView) {
            super(itemView);
            txtCategory = itemView.findViewById(R.id.txtCategory);
            txtIncome = itemView.findViewById(R.id.txtIncome);
            txtExpense = itemView.findViewById(R.id.txtExpense);

            // Details section
            layoutDetails = itemView.findViewById(R.id.layoutDetails);
            txtDate = itemView.findViewById(R.id.txtDate);
            txtNote = itemView.findViewById(R.id.txtNote);

            // Buttons
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnEdit = itemView.findViewById(R.id.btnEdit); // Linked it to the layout here
        }
    }
}