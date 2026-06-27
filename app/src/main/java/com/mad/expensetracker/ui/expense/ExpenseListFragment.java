package com.mad.expensetracker.ui.expense;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mad.expensetracker.R;
import com.mad.expensetracker.data.model.Expense;
import com.mad.expensetracker.databinding.FragmentExpenseListBinding;

import java.util.ArrayList;
import java.util.List;

public class ExpenseListFragment extends Fragment {

    private FragmentExpenseListBinding binding;
    private ExpenseViewModel viewModel;
    private ExpenseAdapter adapter;
    private List<Expense> fullList = new ArrayList<>();
    private String activeCategory = "All";
    private Long rangeStart = null;
    private Long rangeEnd = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentExpenseListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(ExpenseViewModel.class);

        adapter = new ExpenseAdapter(expense -> {
            Bundle args = new Bundle();
            args.putString("expenseId", expense.getId());
            Navigation.findNavController(view).navigate(R.id.action_list_to_edit, args);
        });

        binding.recyclerExpenses.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerExpenses.setAdapter(adapter);

        setupSwipeToDelete();
        setupCategoryChips();
        setupDateRangePicker();

        viewModel.getAllExpenses().observe(getViewLifecycleOwner(), expenses -> {
            fullList = expenses != null ? expenses : new ArrayList<>();
            applyFilters();
        });

        // Surface list-load failures (network issues etc.) instead of failing silently
        viewModel.getLoadError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && binding != null) {
                com.google.android.material.snackbar.Snackbar.make(
                        binding.getRoot(), "Couldn't load expenses: " + error,
                        com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show();
            }
        });

        viewModel.loadAll();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null && binding != null) {
            viewModel.loadAll();
        }
    }

    private void setupCategoryChips() {
        List<String> options = new ArrayList<>();
        options.add("All");
        for (String c : getResources().getStringArray(R.array.categories)) options.add(c);

        for (String option : options) {
            Chip chip = new Chip(requireContext());
            chip.setText(option);
            chip.setCheckable(true);
            chip.setChecked(option.equals("All"));
            chip.setOnClickListener(v -> {
                activeCategory = option;
                for (int i = 0; i < binding.chipGroupFilter.getChildCount(); i++) {
                    Chip sibling = (Chip) binding.chipGroupFilter.getChildAt(i);
                    sibling.setChecked(sibling == chip);
                }
                applyFilters();
            });
            binding.chipGroupFilter.addView(chip);
        }
    }

    private void setupDateRangePicker() {
        binding.btnDateRange.setOnClickListener(v -> {
            MaterialDatePicker<Pair<Long, Long>> picker = MaterialDatePicker.Builder.dateRangePicker().build();
            picker.addOnPositiveButtonClickListener(selection -> {
                rangeStart = selection.first;
                rangeEnd = selection.second;
                applyFilters();
            });
            picker.show(getParentFragmentManager(), "RANGE_PICKER");
        });
    }

    private void applyFilters() {
        if (binding == null) return;
        List<Expense> filtered = new ArrayList<>();
        for (Expense e : fullList) {
            boolean categoryMatch = activeCategory.equals("All") || e.getCategory().equals(activeCategory);
            boolean dateMatch = (rangeStart == null || e.getDateMillis() >= rangeStart)
                    && (rangeEnd == null || e.getDateMillis() <= rangeEnd);
            if (categoryMatch && dateMatch) filtered.add(e);
        }
        adapter.submitList(filtered);
        binding.tvEmptyState.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Expense toDelete = adapter.getExpenseAt(position);

                if (toDelete == null) {
                    adapter.notifyItemChanged(position);
                    return;
                }

                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Delete Expense")
                        .setMessage("Are you sure you want to delete this expense?")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton("Delete", (dialog, which) -> {
                            viewModel.deleteExpense(toDelete.getId());
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            adapter.notifyItemChanged(position);
                        })
                        .show();
            }
        };
        new ItemTouchHelper(callback).attachToRecyclerView(binding.recyclerExpenses);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}