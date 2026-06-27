package com.mad.expensetracker.data.model;

public class Expense {
    private String id;
    private double amount;
    private String category;
    private String note;
    private long dateMillis;
    private String userId;

    // Empty constructor REQUIRED by Firestore for deserialization
    public Expense() {}

    public Expense(double amount, String category, String note, long dateMillis, String userId) {
        this.amount = amount;
        this.category = category;
        this.note = note;
        this.dateMillis = dateMillis;
        this.userId = userId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public long getDateMillis() { return dateMillis; }
    public void setDateMillis(long dateMillis) { this.dateMillis = dateMillis; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}