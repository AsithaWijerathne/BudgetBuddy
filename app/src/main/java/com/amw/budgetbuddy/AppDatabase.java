package com.amw.budgetbuddy;

import androidx.room.Database;
import androidx.room.Delete;
import androidx.room.RoomDatabase;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Database(entities = {Transaction.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract TransactionDao transactionDao();
}

@Dao
interface TransactionDao {
    @Insert
    void insert(Transaction transaction);

    @Delete
    void delete(Transaction transaction);

    // 1. Get transactions between Start Date and End Date
    @Query("SELECT * FROM transactions WHERE timestamp >= :start AND timestamp <= :end ORDER BY timestamp DESC")
    List<Transaction> getTransactionsForMonth(long start, long end);

    // 2. Calculate Opening Balance (Sum of all older transactions)
    @Query("SELECT SUM(CASE WHEN type = 'Income' THEN amount ELSE -amount END) FROM transactions WHERE timestamp < :start")
    double getPreviousBalance(long start);

    // 3. Total Current Balance (All time) for the bottom bar
    @Query("SELECT SUM(CASE WHEN type = 'Income' THEN amount ELSE -amount END) FROM transactions")
    double getTotalBalance();
}