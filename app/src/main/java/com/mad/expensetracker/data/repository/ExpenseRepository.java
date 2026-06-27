package com.mad.expensetracker.data.repository;

import androidx.annotation.NonNull;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.mad.expensetracker.data.model.Expense;
import java.util.List;

public class ExpenseRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final AuthRepository authRepository = new AuthRepository();

    public interface ListCallback {
        void onResult(List<Expense> expenses);
        void onError(String message);
    }

    public interface WriteCallback {
        void onSuccess();
        void onError(String message);
    }

    private com.google.firebase.firestore.CollectionReference expensesRef() {
        String uid = authRepository.getCurrentUserId();
        return db.collection("users").document(uid).collection("expenses");
    }

    public void addExpense(@NonNull Expense expense, WriteCallback callback) {
        expense.setUserId(authRepository.getCurrentUserId());
        expensesRef().add(expense)
                .addOnSuccessListener(ref -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void updateExpense(@NonNull Expense expense, WriteCallback callback) {
        expense.setUserId(authRepository.getCurrentUserId());
        expensesRef().document(expense.getId()).set(expense)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void deleteExpense(@NonNull String expenseId, WriteCallback callback) {
        expensesRef().document(expenseId).delete()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getAllExpenses(ListCallback callback) {
        expensesRef().orderBy("dateMillis", Query.Direction.DESCENDING).get()
                .addOnSuccessListener(snapshot -> {
                    List<Expense> list = snapshot.toObjects(Expense.class);
                    for (int i = 0; i < list.size(); i++) {
                        list.get(i).setId(snapshot.getDocuments().get(i).getId());
                    }
                    callback.onResult(list);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getExpensesInRange(long startMillis, long endMillis, ListCallback callback) {
        expensesRef()
                .whereGreaterThanOrEqualTo("dateMillis", startMillis)
                .whereLessThan("dateMillis", endMillis)
                .orderBy("dateMillis", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Expense> list = snapshot.toObjects(Expense.class);
                    for (int i = 0; i < list.size(); i++) {
                        list.get(i).setId(snapshot.getDocuments().get(i).getId());
                    }
                    callback.onResult(list);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}