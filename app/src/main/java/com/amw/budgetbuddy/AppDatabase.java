package com.amw.budgetbuddy;

import androidx.room.Database;
import androidx.room.Delete;
import androidx.room.RoomDatabase;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Database(entities = {Transaction.class, Account.class}, version = 2)
public abstract class AppDatabase extends RoomDatabase {
    public abstract TransactionDao transactionDao();
    public abstract AccountDao accountDao();
}

@Dao
interface TransactionDao {
    @Insert
    void insert(Transaction transaction);

    @Delete
    void delete(Transaction transaction);

    // 1. Get transactions between Start Date and End Date
    @Query("SELECT * FROM transactions WHERE timestamp >= :start AND timestamp <= :end AND accountId = :accId ORDER BY timestamp DESC")
    List<Transaction> getTransactionsForMonthFiltered(long start, long end, int accId);

    // 2. Calculate Opening Balance (Sum of all older transactions)
    @Query("SELECT SUM(CASE WHEN type = 'Income' THEN amount ELSE -amount END) FROM transactions WHERE timestamp < :start AND accountId = :accId")
    double getPreviousBalanceFiltered(long start, int accId);

    // 3. Total Current Balance (All time) for the bottom bar
    @Query("SELECT SUM(CASE WHEN type = 'Income' THEN amount ELSE -amount END) FROM transactions")
    double getTotalBalance();

    @Query("SELECT * FROM transactions WHERE accountId = :id ORDER BY timestamp DESC")
    List<Transaction> getTransactionsForAccount(int id);

    @Query("SELECT SUM(CASE WHEN type = 'Income' THEN amount ELSE -amount END) FROM transactions WHERE accountId = :id")
    double getAccountBalance(int id);
}

@Dao
interface AccountDao {
    @Insert
    void insert (Account account);

    @Query("SELECT * FROM accounts")
    List<Account> getAllAccounts();
}