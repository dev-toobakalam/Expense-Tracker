package com.mad.expensetracker.ui.dashboard;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.mad.expensetracker.data.model.Expense;
import com.mad.expensetracker.data.repository.ExpenseRepository;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardViewModel extends ViewModel {

    private final ExpenseRepository repository = new ExpenseRepository();
    private final MutableLiveData<Double> totalAmount = new MutableLiveData<>(0.0);
    private final MutableLiveData<Map<String, Double>> categoryTotals = new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<Boolean> isEmpty = new MutableLiveData<>(false);
    private boolean shouldRefresh = true;

    public LiveData<Double> getTotalAmount() { return totalAmount; }
    public LiveData<Map<String, Double>> getCategoryTotals() { return categoryTotals; }
    public LiveData<Boolean> getIsEmpty() { return isEmpty; }

    public boolean shouldRefresh() { return shouldRefresh; }
    public void setRefresh(boolean refresh) { this.shouldRefresh = refresh; }

    public void loadCurrentMonth() {
        Calendar startCal = Calendar.getInstance();
        startCal.set(Calendar.DAY_OF_MONTH, 1);
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        long startMillis = startCal.getTimeInMillis();

        Calendar endCal = (Calendar) startCal.clone();
        endCal.add(Calendar.MONTH, 1);
        long endMillis = endCal.getTimeInMillis();

        repository.getExpensesInRange(startMillis, endMillis, new ExpenseRepository.ListCallback() {
            @Override
            public void onResult(List<Expense> expenses) {
                double sum = 0;
                Map<String, Double> byCategory = new HashMap<>();
                for (Expense e : expenses) {
                    sum += e.getAmount();
                    byCategory.merge(e.getCategory(), e.getAmount(), Double::sum);
                }
                totalAmount.setValue(sum);
                categoryTotals.setValue(byCategory);
                isEmpty.setValue(expenses.isEmpty());
                shouldRefresh = false;
            }

            @Override
            public void onError(String message) {
                isEmpty.setValue(true);
                shouldRefresh = false;
            }
        });
    }
}