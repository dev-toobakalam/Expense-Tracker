package com.mad.expensetracker.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Shared input validation and formatting helpers.
 *
 * This file did not exist in the original project, even though
 * AddExpenseFragment, EditExpenseFragment, ExpenseAdapter, and
 * SettingsFragment all call methods on it. Without it the project
 * does not compile.
 */
public class Validators {

    private static final double MAX_REASONABLE_AMOUNT = 10_000_000; // sanity ceiling
    private static final int MAX_NOTE_LENGTH = 200;

    private Validators() {
        // utility class, no instances
    }

    /** Amount must be a valid positive number, not zero, not negative, and within a sane upper bound. */
    public static boolean isValidAmount(String amountText) {
        if (amountText == null || amountText.trim().isEmpty()) return false;
        try {
            double amount = Double.parseDouble(amountText.trim());
            return amount > 0 && amount <= MAX_REASONABLE_AMOUNT;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** At least 6 characters, matching Firebase Auth's own minimum. */
    public static boolean isValidPassword(String password) {
        return password != null && password.trim().length() >= 6;
    }

    /** Optional free-text note. Empty is fine; only rejects unreasonably long notes. */
    public static boolean isValidNote(String note) {
        return note == null || note.length() <= MAX_NOTE_LENGTH;
    }

    public static String formatDate(long millis) {
        return new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date(millis));
    }
}