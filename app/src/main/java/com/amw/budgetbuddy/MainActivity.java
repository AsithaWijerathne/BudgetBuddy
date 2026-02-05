package com.amw.budgetbuddy;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.google.android.material.navigation.NavigationView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    AppDatabase db;
    RecyclerView recyclerView;
    TextView txtBalance, txtMonth, txtTitle; // Added txtTitle
    TransactionAdapter adapter;

    // Track the currently displayed month
    Calendar currentMonth;

    TextView txtTotalIncome, txtTotalExpense;
    LinearLayout layoutBalanceDetails, bottomBar;

    DrawerLayout drawerLayout;
    NavigationView navView;

    // -1 means "Home" (All Budgets), otherwise it's the Account ID
    int currentAccountId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Database
        db = Room.databaseBuilder(getApplicationContext(),
                        AppDatabase.class, "budget-db")
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration() // Handle DB updates safely
                .build();

        // Initialize Views
        drawerLayout = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.navView);
        txtTitle = findViewById(R.id.txtTitle); // Ensure this ID exists in XML
        recyclerView = findViewById(R.id.recyclerView);
        txtBalance = findViewById(R.id.txtBalance);
        txtMonth = findViewById(R.id.txtMonth);

        txtTotalIncome = findViewById(R.id.txtTotalIncome);
        txtTotalExpense = findViewById(R.id.txtTotalExpense);
        layoutBalanceDetails = findViewById(R.id.layoutBalanceDetails);
        bottomBar = findViewById(R.id.bottomBar);

        // Setup Drawer
        findViewById(R.id.btnMenu).setOnClickListener(v -> drawerLayout.open());
        setupDrawerMenu();

        // Initialize Calendar to "Right Now"
        currentMonth = Calendar.getInstance();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnAdd).setOnClickListener(v -> {
            Intent intent = new Intent(this, AddTransactionActivity.class);
            // Pass the ID of the account we are currently viewing
            intent.putExtra("selected_account_id", currentAccountId);
            startActivity(intent);
        });

        // --- Navigation Logic ---
        findViewById(R.id.btnPrevMonth).setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, -1);
            loadData();
        });

        findViewById(R.id.btnNextMonth).setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, 1);
            loadData();
        });

        // Bottom Bar Expansion
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
        // Refresh menu in case a new account was added
        setupDrawerMenu();
        loadData();
    }

    // --- NEW: Drawer Menu Logic ---
    private void setupDrawerMenu() {
        Menu menu = navView.getMenu();
        menu.clear(); // Clear old items to avoid duplicates

        // 1. Add "Home" Option
        menu.add(0, -1, 0, "Home (Monthly Budget)").setIcon(android.R.drawable.ic_menu_my_calendar);

        // 2. Add Accounts from Database
        List<Account> accounts = db.accountDao().getAllAccounts();
        for (Account acc : accounts) {
            menu.add(1, acc.id, 1, acc.name).setIcon(android.R.drawable.ic_menu_view);
        }

        // 3. Add "Create New Account" Button
        menu.add(2, 999, 2, "+ Add New Account");

        // Handle Clicks
        navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == 999) {
                showAddAccountDialog();
            } else {
                currentAccountId = id; // Switch Mode
                updateUIForMode();     // Toggle Month View visibility
                loadData();            // Reload correct data
                drawerLayout.close();
            }
            return true;
        });
    }

    // --- NEW: Switch UI between "Month View" and "Account View" ---
    private void updateUIForMode() {
        LinearLayout monthNav = findViewById(R.id.monthNavigation);

        if (currentAccountId == -1) {
            // HOME MODE
            monthNav.setVisibility(View.VISIBLE);
            if (txtTitle != null) txtTitle.setText("CashKeeper");
        } else {
            // ACCOUNT MODE
            monthNav.setVisibility(View.GONE); // Hide Month Arrows

            // Set Title to Account Name
            // (Quick way: loop DB list again, or fetch single account)
            List<Account> accounts = db.accountDao().getAllAccounts();
            for(Account a : accounts) {
                if(a.id == currentAccountId) {
                    if (txtTitle != null) txtTitle.setText(a.name);
                    break;
                }
            }
        }
    }

    // --- NEW: Dialog to Create Account ---
    private void showAddAccountDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("New Account");

        final EditText input = new EditText(this);
        input.setHint("Account Name (e.g. HNB Bank)");
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String name = input.getText().toString();
            if (!name.isEmpty()) {
                Account newAcc = new Account(name, 0); // Start with 0 balance
                db.accountDao().insert(newAcc);
                setupDrawerMenu(); // Refresh menu to show it
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // --- UPDATED: Data Loading Logic ---
    private void loadData() {
        List<Transaction> list;
        double currentViewBalance = 0;
        double monthIncome = 0;
        double monthExpense = 0;

        if (currentAccountId == -1) {
            // ==========================
            // MODE 1: HOME (Monthly View)
            // ==========================

            // 1. Set Month Title
            SimpleDateFormat fmt = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
            txtMonth.setText(fmt.format(currentMonth.getTime()));

            // 2. Calculate Dates
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

            // 3. Get Data
            list = db.transactionDao().getTransactionsForMonthFiltered(startTimestamp, endTimestamp, -1);
            double previousBalance = db.transactionDao().getPreviousBalanceFiltered(startTimestamp, -1);

            // 4. Add "Previous Balance" Row
            if (previousBalance != 0) {
                String type = previousBalance >= 0 ? "Income" : "Expense";
                Transaction openingTxn = new Transaction(type, "Previous Balance", Math.abs(previousBalance), "Carried forward");
                openingTxn.timestamp = startTimestamp;
                list.add(0, openingTxn);
            }

            currentViewBalance = previousBalance;

        } else {
            // ==========================
            // MODE 2: ACCOUNT (Ledger View)
            // ==========================

            // 1. Get ALL transactions for this account (No month filter)
            // Make sure you added this query to your DAO!
            list = db.transactionDao().getTransactionsForAccount(currentAccountId);

            // 2. No "Previous Balance" row needed for raw ledger
            currentViewBalance = 0;
        }

        // --- Calculate Totals for Bottom Bar ---
        for (Transaction t : list) {
            // Skip the "Previous Balance" fake transaction for income/expense sums
            if (t.category.equals("Previous Balance")) {
                if (currentAccountId == -1) continue; // Don't add to income/expense in Month mode
            }

            if(t.type.equals("Income")) {
                currentViewBalance += t.amount;
                monthIncome += t.amount;
            } else {
                currentViewBalance -= t.amount;
                monthExpense += t.amount;
            }
        }

        // --- Update UI ---
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