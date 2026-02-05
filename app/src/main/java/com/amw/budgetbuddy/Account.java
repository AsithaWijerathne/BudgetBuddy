package com.amw.budgetbuddy;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "accounts")
public class Account {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;
    public double initialBalance;

    public Account(String name, double initialBalance) {
        this.name = name;
        this.initialBalance = initialBalance;
    }
}