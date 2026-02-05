package com.amw.budgetbuddy;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "transactions")
public class Transaction {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String type; // "Income" or "Expense"
    public String category; // e.g., "Food", "Salary"
    public double amount;
    public String note;
    public long timestamp;
    public int accountId;

    // Constructor
    public Transaction(String type, String category, double amount, String note) {
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.note = note;
        this.timestamp = System.currentTimeMillis();
    }
}
