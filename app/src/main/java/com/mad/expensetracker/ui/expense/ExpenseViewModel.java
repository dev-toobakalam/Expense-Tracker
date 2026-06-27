package com.mad.expensetracker.ui.expense;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.mad.expensetracker.data.model.Expense;
import com.mad.expensetracker.data.repository.ExpenseRepository;
import java.util.List;

public class ExpenseViewModel extends ViewModel {
    private final ExpenseRepository repository = new ExpenseRepository();
    private final MutableLiveData<List<Expense>> allExpenses = new MutableLiveData<>();
    private final MutableLiveData<String> saveError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> loadError = new MutableLiveData<>();

    public LiveData<List<Expense>> getAllExpenses() { return allExpenses; }
    public LiveData<String> getSaveError() { return saveError; }
    public LiveData<Boolean> getSaveSuccess() { return saveSuccess; }
    public LiveData<String> getLoadError() { return loadError; }

    public void loadAll() {
        repository.getAllExpenses(new ExpenseRepository.ListCallback() {
            @Override public void onResult(List<Expense> expenses) { allExpenses.setValue(expenses); }
            @Override public void onError(String message) { loadError.setValue(message); }
        });
    }

    public void addExpense(Expense expense) {
        repository.addExpense(expense, new ExpenseRepository.WriteCallback() {
            @Override public void onSuccess() { saveSuccess.setValue(true); }
            @Override public void onError(String message) { saveError.setValue(message); }
        });
    }

    public void updateExpense(Expense expense) {
        repository.updateExpense(expense, new ExpenseRepository.WriteCallback() {
            @Override public void onSuccess() { saveSuccess.setValue(true); }
            @Override public void onError(String message) { saveError.setValue(message); }
        });
    }

    public void deleteExpense(String id) {
        repository.deleteExpense(id, new ExpenseRepository.WriteCallback() {
            @Override public void onSuccess() { loadAll(); }
            @Override public void onError(String message) { saveError.setValue(message); }
        });
    }
}