package com.mad.expensetracker.ui.expense;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.chip.Chip;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.mad.expensetracker.R;
import com.mad.expensetracker.data.model.Expense;
import com.mad.expensetracker.databinding.FragmentAddExpenseBinding;
import com.mad.expensetracker.utils.Validators;

public class EditExpenseFragment extends Fragment {

    private FragmentAddExpenseBinding binding;
    private ExpenseViewModel viewModel;
    private String expenseId;
    private String selectedCategory;
    private Long selectedDateMillis;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAddExpenseBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(ExpenseViewModel.class);

        binding.btnSave.setText("Update Expense");
        binding.btnDelete.setVisibility(View.VISIBLE);

        expenseId = getArguments() != null ? getArguments().getString("expenseId") : null;

        setupCategoryChips();
        setupDatePicker();
        prefillExisting();

        binding.btnSave.setOnClickListener(v -> {
            if (validateAllFields()) {
                updateExpense();
            }
        });
        binding.btnDelete.setOnClickListener(v -> deleteExpense());

        viewModel.getSaveSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null && success) {
                Snackbar.make(binding.getRoot(), "Expense updated", Snackbar.LENGTH_SHORT).show();
                Navigation.findNavController(view).navigateUp();
            }
        });

        viewModel.getSaveError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_LONG).show();
        });

        viewModel.getLoadError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) Snackbar.make(binding.getRoot(), "Error loading expenses: " + error, Snackbar.LENGTH_LONG).show();
        });
    }

    // ---- Helper: format amount without unnecessary .0 ----
    private String formatAmountForInput(double amount) {
        if (amount == Math.floor(amount)) {
            return String.valueOf((long) amount);
        }
        return String.valueOf(amount);
    }

    private void setupCategoryChips() {
        String[] categories = getResources().getStringArray(R.array.categories);
        for (String category : categories) {
            Chip chip = new Chip(requireContext());
            chip.setText(category);
            chip.setCheckable(true);
            chip.setChipIcon(ContextCompat.getDrawable(requireContext(), iconFor(category)));
            chip.setChipIconTint(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.text_secondary)));
            chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.surface_muted)));
            chip.setCheckedIconVisible(false);

            chip.setOnCheckedChangeListener((button, isChecked) -> {
                if (isChecked) {
                    selectedCategory = category;
                    chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(
                            ContextCompat.getColor(requireContext(), colorFor(category))));
                    chip.setChipIconTint(android.content.res.ColorStateList.valueOf(
                            ContextCompat.getColor(requireContext(), R.color.white)));
                    chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
                } else {
                    chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(
                            ContextCompat.getColor(requireContext(), R.color.surface_muted)));
                    chip.setChipIconTint(android.content.res.ColorStateList.valueOf(
                            ContextCompat.getColor(requireContext(), R.color.text_secondary)));
                    chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
                }
            });
            binding.chipGroupCategory.addView(chip);
        }
    }

    private void setupDatePicker() {
        binding.btnPickDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker().build();
            picker.addOnPositiveButtonClickListener(selection -> {
                selectedDateMillis = selection;
                binding.btnPickDate.setText(Validators.formatDate(selection));
                binding.tvDateError.setVisibility(View.GONE);
            });
            picker.show(getParentFragmentManager(), "DATE_PICKER");
        });
    }

    private void prefillExisting() {
        viewModel.getAllExpenses().observe(getViewLifecycleOwner(), expenses -> {
            if (expenses == null) return;
            for (Expense e : expenses) {
                if (e.getId().equals(expenseId)) {
                    // FIX: Format amount without trailing .0
                    binding.etAmount.setText(formatAmountForInput(e.getAmount()));
                    binding.etNote.setText(e.getNote());
                    selectedCategory = e.getCategory();
                    selectedDateMillis = e.getDateMillis();
                    binding.btnPickDate.setText(Validators.formatDate(e.getDateMillis()));
                    binding.btnSave.setEnabled(true);

                    for (int i = 0; i < binding.chipGroupCategory.getChildCount(); i++) {
                        Chip chip = (Chip) binding.chipGroupCategory.getChildAt(i);
                        if (chip.getText().toString().equals(e.getCategory())) {
                            chip.setChecked(true);
                            chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(
                                    ContextCompat.getColor(requireContext(), colorFor(e.getCategory()))));
                            chip.setChipIconTint(android.content.res.ColorStateList.valueOf(
                                    ContextCompat.getColor(requireContext(), R.color.white)));
                            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
                        }
                    }
                    break;
                }
            }
        });
        viewModel.loadAll();
    }

    private boolean validateAllFields() {
        boolean isValid = true;

        String amountText = binding.etAmount.getText().toString().trim();
        if (!Validators.isValidAmount(amountText)) {
            binding.tilAmount.setError("Please enter a valid amount greater than 0");
            isValid = false;
        } else {
            binding.tilAmount.setError(null);
        }

        if (selectedCategory == null) {
            binding.tvCategoryError.setVisibility(View.VISIBLE);
            binding.tvCategoryError.setText("Please select a category");
            isValid = false;
        } else {
            binding.tvCategoryError.setVisibility(View.GONE);
        }

        if (selectedDateMillis == null) {
            binding.tvDateError.setVisibility(View.VISIBLE);
            binding.tvDateError.setText("Please select a date");
            isValid = false;
        } else {
            binding.tvDateError.setVisibility(View.GONE);
        }

        String note = binding.etNote.getText().toString().trim();
        if (!Validators.isValidNote(note)) {
            binding.tilNote.setError("Note is too long (max 200 characters)");
            isValid = false;
        } else {
            binding.tilNote.setError(null);
        }

        return isValid;
    }

    private void updateExpense() {
        double amount = Double.parseDouble(binding.etAmount.getText().toString().trim());
        String note = binding.etNote.getText().toString().trim();
        Expense updated = new Expense(amount, selectedCategory, note, selectedDateMillis, "");
        updated.setId(expenseId);
        viewModel.updateExpense(updated);
    }

    private void deleteExpense() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Expense")
                .setMessage("Are you sure you want to delete this expense permanently?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Delete", (dialog, which) -> {
                    viewModel.deleteExpense(expenseId);
                    Snackbar.make(binding.getRoot(), "Expense deleted", Snackbar.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).navigateUp();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private int iconFor(String category) {
        switch (category) {
            case "Food": return R.drawable.ic_food;
            case "Transport": return R.drawable.ic_transport;
            case "Shopping": return R.drawable.ic_shopping;
            case "Bills": return R.drawable.ic_bill;
            case "Entertainment": return R.drawable.ic_entertainment;
            default: return R.drawable.ic_others;
        }
    }

    private int colorFor(String category) {
        switch (category) {
            case "Food": return R.color.cat_food;
            case "Transport": return R.color.cat_transport;
            case "Shopping": return R.color.cat_shopping;
            case "Bills": return R.color.cat_bills;
            case "Entertainment": return R.color.cat_entertainment;
            default: return R.color.cat_other;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}