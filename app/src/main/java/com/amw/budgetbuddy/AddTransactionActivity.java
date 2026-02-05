package com.amw.budgetbuddy;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import android.content.SharedPreferences;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AddTransactionActivity extends AppCompatActivity {

    String selectedType = "Expense";
    RecyclerView recyclerCategories;
    TextView tabIncome, tabExpense;
    AppDatabase db;

    SharedPreferences prefs;

    // Default Lists (We can make these dynamic later)
    List<String> expenseCategories = new ArrayList<>(Arrays.asList("Food", "Shopping", "Transport", "Bills", "Rent"));
    List<String> incomeCategories = new ArrayList<>(Arrays.asList("Salary", "Investments", "Gifts", "Other"));

    int accountIdToSave = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        prefs = getSharedPreferences("BudgetBuddyData", MODE_PRIVATE);
        loadCategoriesFromStorage(); // <--- Load saved categories immediately

        accountIdToSave = getIntent().getIntExtra("selected_account_id", -1);

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "budget-db")
                .allowMainThreadQueries().build();

        recyclerCategories = findViewById(R.id.recyclerCategories);
        recyclerCategories.setLayoutManager(new GridLayoutManager(this, 3)); // 3 Columns

        tabIncome = findViewById(R.id.tabIncome);
        tabExpense = findViewById(R.id.tabExpense);

        // 1. Setup Toggle Logic
        tabIncome.setOnClickListener(v -> setType("Income"));
        tabExpense.setOnClickListener(v -> setType("Expense"));

        // 2. Setup "Add Category" Button (Simple placeholder for now)
        findViewById(R.id.btnAddCategory).setOnClickListener(v -> {
            // For now, let's just add a temporary one to test
            showAddCategoryPopup();
        });

        // 3. Setup Back Button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Initial Load
        setType("Expense");
    }

    private void setType(String type) {
        selectedType = type;

        // Create the rounded drawable programmatically to change colors
        android.graphics.drawable.GradientDrawable selectedBg = new android.graphics.drawable.GradientDrawable();
        selectedBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        selectedBg.setCornerRadius(100f); // Fully round

        int colorGreen = ContextCompat.getColor(this, R.color.brand_green);
        int colorPink = ContextCompat.getColor(this, R.color.brand_accent);
        int colorTextInv = ContextCompat.getColor(this, R.color.text_inverse);
        int colorTextSec = ContextCompat.getColor(this, R.color.text_secondary);

        if (type.equals("Income")) {
            selectedBg.setColor(colorGreen);
            tabIncome.setBackground(selectedBg);
            tabIncome.setTextColor(colorTextInv);

            tabExpense.setBackgroundColor(Color.TRANSPARENT);
            tabExpense.setTextColor(colorTextSec);
        } else {
            selectedBg.setColor(colorPink);
            tabExpense.setBackground(selectedBg);
            tabExpense.setTextColor(colorTextInv);

            tabIncome.setBackgroundColor(Color.TRANSPARENT);
            tabIncome.setTextColor(colorTextSec);
        }
        loadCategories();
    }

    private void loadCategories() {
        List<String> currentList = selectedType.equals("Income") ? incomeCategories : expenseCategories;

        CategoryAdapter adapter = new CategoryAdapter(currentList, categoryName -> {
            // On Click -> Show Popup
            showInputPopup(categoryName);
        });
        recyclerCategories.setAdapter(adapter);
    }

    private void saveCategoriesToStorage() {
        SharedPreferences.Editor editor = prefs.edit();
        // Join list into string like "Food,Shopping,Transport"
        editor.putString("EXPENSE_CATS", TextUtils.join(",", expenseCategories));
        editor.putString("INCOME_CATS", TextUtils.join(",", incomeCategories));
        editor.apply();
    }

    private void loadCategoriesFromStorage() {
        String expStr = prefs.getString("EXPENSE_CATS", "");
        String incStr = prefs.getString("INCOME_CATS", "");

        if (!expStr.isEmpty()) {
            expenseCategories = new ArrayList<>(Arrays.asList(expStr.split(",")));
        }
        if (!incStr.isEmpty()) {
            incomeCategories = new ArrayList<>(Arrays.asList(incStr.split(",")));
        }
    }

    private void showInputPopup(String category) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_input, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();

        TextView title = view.findViewById(R.id.dialogTitle);
        title.setText("Add " + selectedType + ": " + category);

        EditText etAmount = view.findViewById(R.id.etDialogAmount);
        EditText etNote = view.findViewById(R.id.etDialogNote);

        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());

        view.findViewById(R.id.btnSave).setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString();
            if (!amountStr.isEmpty()) {
                double amount = Double.parseDouble(amountStr);
                String note = etNote.getText().toString();

                // 3. Save with the correct Account ID
                Transaction t = new Transaction(selectedType, category, amount, note);

                // IMPORTANT: Set the account ID manually before inserting!
                t.accountId = accountIdToSave;

                db.transactionDao().insert(t);

                dialog.dismiss();
                finish();
            }
        });

        dialog.show();
    }

    // Add this method inside AddTransactionActivity
    private void showAddCategoryPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_category, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        EditText etName = view.findViewById(R.id.etCategoryName);

        // Cancel Button
        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());

        // Add Button
        view.findViewById(R.id.btnAdd).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (!name.isEmpty()) {
                // Add to the correct list based on what tab is selected
                if (selectedType.equals("Income")) {
                    incomeCategories.add(name);
                } else {
                    expenseCategories.add(name);
                }

                // Refresh the grid to show the new button
                saveCategoriesToStorage();
                loadCategories();
                dialog.dismiss();
            }
        });

        dialog.show();
    }
}