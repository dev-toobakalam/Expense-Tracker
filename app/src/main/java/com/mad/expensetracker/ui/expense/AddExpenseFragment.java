package com.mad.expensetracker.ui.expense;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.google.android.material.snackbar.Snackbar;
import com.mad.expensetracker.R;
import com.mad.expensetracker.data.model.Expense;
import com.mad.expensetracker.databinding.FragmentAddExpenseBinding;
import com.mad.expensetracker.utils.Validators;

public class AddExpenseFragment extends Fragment {

    private FragmentAddExpenseBinding binding;
    private ExpenseViewModel viewModel;
    private String selectedCategory = null;
    private Long selectedDateMillis = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAddExpenseBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(ExpenseViewModel.class);

        setupCategoryChips();
        setupDatePicker();
        setupValidationListeners();

        binding.btnSave.setEnabled(true);
        binding.btnSave.setOnClickListener(v -> {
            if (validateAllFields()) {
                saveExpense();
            }
        });

        viewModel.getSaveSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null && success) {
                setSaving(false);
                Snackbar.make(binding.getRoot(), "Expense saved", Snackbar.LENGTH_SHORT).show();
                clearForm();
                Navigation.findNavController(view).navigate(R.id.dashboardFragment);
            }
        });

        viewModel.getSaveError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                setSaving(false);
                Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void setSaving(boolean saving) {
        if (binding == null) return;
        binding.btnSave.setEnabled(!saving);
        binding.btnSave.setText(saving ? "Saving…" : "Save Expense");
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
                    binding.tvCategoryError.setVisibility(View.GONE);
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
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select expense date")
                    .build();
            picker.addOnPositiveButtonClickListener(selection -> {
                selectedDateMillis = selection;
                binding.btnPickDate.setText(Validators.formatDate(selection));
                binding.tvDateError.setVisibility(View.GONE);
            });
            picker.show(getParentFragmentManager(), "DATE_PICKER");
        });
    }

    private void setupValidationListeners() {
        binding.etAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                binding.tilAmount.setError(null);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.etNote.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                binding.tilNote.setError(null);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private boolean validateAllFields() {
        boolean isValid = true;

        // Amount
        String amountText = binding.etAmount.getText().toString().trim();
        if (!Validators.isValidAmount(amountText)) {
            binding.tilAmount.setError("Please enter a valid amount greater than 0");
            isValid = false;
        } else {
            binding.tilAmount.setError(null);
        }

        // Category
        if (selectedCategory == null) {
            binding.tvCategoryError.setVisibility(View.VISIBLE);
            binding.tvCategoryError.setText("Please select a category");
            isValid = false;
        } else {
            binding.tvCategoryError.setVisibility(View.GONE);
        }

        // Date
        if (selectedDateMillis == null) {
            binding.tvDateError.setVisibility(View.VISIBLE);
            binding.tvDateError.setText("Please select a date");
            isValid = false;
        } else {
            binding.tvDateError.setVisibility(View.GONE);
        }

        // Note (optional field, but capped in length)
        String note = binding.etNote.getText().toString().trim();
        if (!Validators.isValidNote(note)) {
            binding.tilNote.setError("Note is too long (max 200 characters)");
            isValid = false;
        } else {
            binding.tilNote.setError(null);
        }

        return isValid;
    }

    private void saveExpense() {
        setSaving(true);

        if (!isNetworkAvailable()) {
            // We still let the write go through — Firestore's offline cache will
            // queue it and sync once the connection returns, which is correct
            // behavior for "data must persist." But the user needs to be told
            // this clearly, otherwise they assume nothing happened and tap Save
            // again, which is what caused the duplicate-entry bug.
            Snackbar.make(binding.getRoot(),
                    "You're offline. This expense will be saved once you're back online.",
                    Snackbar.LENGTH_LONG).show();
        }

        double amount = Double.parseDouble(binding.etAmount.getText().toString().trim());
        String note = binding.etNote.getText().toString().trim();
        Expense expense = new Expense(amount, selectedCategory, note, selectedDateMillis, "");
        viewModel.addExpense(expense);

        // Firestore's offline cache resolves the success callback immediately
        // even without network (the write is queued locally), so the button
        // will re-enable correctly via getSaveSuccess() either way — this
        // network check exists purely to inform the user, not to block them.
    }

    private boolean isNetworkAvailable() {
        android.net.ConnectivityManager cm =
                (android.net.ConnectivityManager) requireContext().getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
        if (cm == null) return true; // fail open — don't block saving on a check failure
        android.net.Network network = cm.getActiveNetwork();
        if (network == null) return false;
        android.net.NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
        return capabilities != null
                && capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private void clearForm() {
        binding.etAmount.setText("");
        binding.etNote.setText("");
        binding.chipGroupCategory.clearCheck();
        binding.btnPickDate.setText("Select date");
        binding.tvDateError.setVisibility(View.GONE);
        binding.tilAmount.setError(null);
        binding.tilNote.setError(null);
        binding.tvCategoryError.setVisibility(View.GONE);
        selectedCategory = null;
        selectedDateMillis = null;
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