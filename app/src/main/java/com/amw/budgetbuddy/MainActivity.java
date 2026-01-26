package com.amw.budgetbuddy;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    AppDatabase db;
    RecyclerView recyclerView;
    TextView txtBalance, txtMonth;
    TransactionAdapter adapter;

    // Track the currently displayed month
    Calendar currentMonth;

    TextView txtTotalIncome, txtTotalExpense;
    LinearLayout layoutBalanceDetails, bottomBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Calendar to "Right Now"
        currentMonth = Calendar.getInstance();

        recyclerView = findViewById(R.id.recyclerView);
        txtBalance = findViewById(R.id.txtBalance);
        txtMonth = findViewById(R.id.txtMonth);

        db = Room.databaseBuilder(getApplicationContext(),
                        AppDatabase.class, "budget-db")
                .allowMainThreadQueries()
                .build();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnAdd).setOnClickListener(v -> {
            startActivity(new Intent(this, AddTransactionActivity.class));
        });

        // --- Navigation Logic ---
        findViewById(R.id.btnPrevMonth).setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, -1); // Go back 1 month
            loadData();
        });

        findViewById(R.id.btnNextMonth).setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, 1); // Go forward 1 month
            loadData();
        });

        txtTotalIncome = findViewById(R.id.txtTotalIncome);
        txtTotalExpense = findViewById(R.id.txtTotalExpense);
        layoutBalanceDetails = findViewById(R.id.layoutBalanceDetails);
        bottomBar = findViewById(R.id.bottomBar);

        // Show total income and expenses
        bottomBar.setOnClickListener(v -> {
            if (layoutBalanceDetails.getVisibility() == View.VISIBLE) {
                layoutBalanceDetails.setVisibility(View.GONE);
            } else {
                layoutBalanceDetails.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        // 1. Update Month Title (e.g., "February 2026")
        SimpleDateFormat fmt = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        txtMonth.setText(fmt.format(currentMonth.getTime()));

        // 2. Calculate Start and End timestamps for the selected month
        Calendar startCal = (Calendar) currentMonth.clone();
        startCal.set(Calendar.DAY_OF_MONTH, 1);
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);
        long startTimestamp = startCal.getTimeInMillis();

        Calendar endCal = (Calendar) currentMonth.clone();
        endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH));
        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);
        long endTimestamp = endCal.getTimeInMillis();

        // 3. Get Data from DB
        List<Transaction> list = db.transactionDao().getTransactionsForMonth(startTimestamp, endTimestamp);

        // 4. Calculate Opening Balance (Previous Month's carry over)
        double previousBalance = db.transactionDao().getPreviousBalance(startTimestamp);

        // 5. Create a "Fake" Transaction to show the Opening Balance at the top
        if (previousBalance != 0) {
            String type = previousBalance >= 0 ? "Income" : "Expense";
            Transaction openingTxn = new Transaction(type, "Previous Balance", Math.abs(previousBalance), "Carried forward from last month");
            // Set timestamp to start of month so it sorts correctly if needed
            openingTxn.timestamp = startTimestamp;
            // Add to the top of the list
            list.add(0, openingTxn);
        }

        // 6. Update Bottom Bar (Total Balance)
        // We calculate this purely based on the visible list to avoid confusion
        double currentViewBalance = previousBalance;

        double monthIncome = 0;
        double monthExpense = 0;
        for (Transaction t : db.transactionDao().getTransactionsForMonth(startTimestamp, endTimestamp)) {
            if(t.type.equals("Income")) {
                currentViewBalance += t.amount;
                monthIncome += t.amount;
            }
            else {
                currentViewBalance -= t.amount;
                monthExpense += t.amount;
            }
        }

        //  balance:
        txtBalance.setText(String.format("%.0f", currentViewBalance));

        txtTotalIncome.setText("+ " + String.format("%.0f", monthIncome));
        txtTotalExpense.setText("- " + String.format("%.0f", monthExpense));

        adapter = new TransactionAdapter(list, transaction -> {
            if (transaction.category.equals("Previous Balance")) return;

            db.transactionDao().delete(transaction);
            loadData();
        });
        recyclerView.setAdapter(adapter);
    }
}