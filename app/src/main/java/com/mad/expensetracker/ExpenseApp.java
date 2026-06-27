package com.mad.expensetracker;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class ExpenseApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
    }
}