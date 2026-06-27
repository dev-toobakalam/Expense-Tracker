package com.mad.expensetracker.ui.expense;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.mad.expensetracker.R;
import com.mad.expensetracker.data.model.Expense;
import com.mad.expensetracker.utils.Validators;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    public interface OnExpenseClickListener {
        void onExpenseClick(Expense expense);
    }

    private List<Expense> expenses = new ArrayList<>();
    private final OnExpenseClickListener listener;

    public ExpenseAdapter(OnExpenseClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Expense> newList) {
        this.expenses = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    public Expense getExpenseAt(int position) {
        if (position < 0 || position >= expenses.size()) return null;
        return expenses.get(position);
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = expenses.get(position);
        if (expense == null) return;

        holder.tvCategory.setText(expense.getCategory());
        holder.tvAmount.setText(String.format(Locale.getDefault(), "Rs. %,.0f", expense.getAmount()));

        String noteText = expense.getNote() == null || expense.getNote().isEmpty()
                ? Validators.formatDate(expense.getDateMillis())
                : expense.getNote() + " · " + Validators.formatDate(expense.getDateMillis());
        holder.tvNoteDate.setText(noteText);

        // Set chip color
        holder.iconChip.setCardBackgroundColor(
                ContextCompat.getColor(holder.itemView.getContext(), categoryColorRes(expense.getCategory()))
        );
        holder.ivCategoryIcon.setImageResource(categoryIconRes(expense.getCategory()));

        holder.itemView.setOnClickListener(v -> listener.onExpenseClick(expense));
        holder.itemView.setAnimation(AnimationUtils.loadAnimation(holder.itemView.getContext(), R.anim.item_fade_in));
    }

    private int categoryColorRes(String category) {
        switch (category) {
            case "Food": return R.color.cat_food;
            case "Transport": return R.color.cat_transport;
            case "Shopping": return R.color.cat_shopping;
            case "Bills": return R.color.cat_bills;
            case "Entertainment": return R.color.cat_entertainment;
            default: return R.color.cat_other;
        }
    }

    private int categoryIconRes(String category) {
        switch (category) {
            case "Food": return R.drawable.ic_food;
            case "Transport": return R.drawable.ic_transport;
            case "Shopping": return R.drawable.ic_shopping;
            case "Bills": return R.drawable.ic_bill;
            case "Entertainment": return R.drawable.ic_entertainment;
            default: return R.drawable.ic_others;
        }
    }

    @Override
    public int getItemCount() {
        return expenses.size();
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView iconChip;
        android.widget.ImageView ivCategoryIcon;
        android.widget.TextView tvCategory, tvNoteDate, tvAmount;

        ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCategoryIcon = itemView.findViewById(R.id.ivCategoryIcon);
            iconChip = (MaterialCardView) ivCategoryIcon.getParent();
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvNoteDate = itemView.findViewById(R.id.tvNoteDate);
            tvAmount = itemView.findViewById(R.id.tvAmount);
        }
    }
}