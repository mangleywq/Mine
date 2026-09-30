package com.coderpage.mine.app.tally.module.recurring;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.coderpage.concurrency.AsyncTaskExecutor;
import com.coderpage.mine.R;
import com.coderpage.mine.app.tally.common.router.TallyRouter;
import com.coderpage.mine.app.tally.persistence.model.CategoryModel;
import com.coderpage.mine.app.tally.persistence.sql.TallyDatabase;
import com.coderpage.mine.app.tally.persistence.sql.entity.RecurringExpenseEntity;
import com.coderpage.mine.ui.BaseActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Route(path = TallyRouter.RECURRING_EXPENSE)
public class RecurringExpenseActivity extends BaseActivity {
    private final List<RecurringExpenseEntity> rules = new ArrayList<>();
    private final Map<String, String> categories = new HashMap<>();
    private BaseAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recurring_expense);
        setToolbarAsBack(v -> finish());
        setToolbarTitle(R.string.recurring_expense_title);
        ListView list = findViewById(R.id.recurringExpenseList);
        Typeface amountFont = Typeface.createFromAsset(getAssets(), "font/Quicksand-Bold.ttf");
        adapter = new BaseAdapter() {
            @Override public int getCount() { return rules.size(); }
            @Override public Object getItem(int position) { return rules.get(position); }
            @Override public long getItemId(int position) { return position; }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View row = convertView == null
                        ? LayoutInflater.from(RecurringExpenseActivity.this)
                                .inflate(R.layout.tally_item_recurring_expense, parent, false)
                        : convertView;
                RecurringExpenseEntity rule = rules.get(position);
                ((TextView) row.findViewById(R.id.recurringExpenseItemName)).setText(rule.name);
                TextView amount = row.findViewById(R.id.recurringExpenseItemAmount);
                amount.setTypeface(amountFont);
                amount.setText("¥" + String.format(Locale.CHINA, "%.2f", rule.amount));
                String day = getString(R.string.recurring_expense_day_format, rule.dayOfMonth);
                String category = categories.get(rule.categoryUniqueName);
                ((TextView) row.findViewById(R.id.recurringExpenseItemDetail)).setText(
                        category == null ? day : day + " · " + category);
                return row;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> showActions(rules.get(position)));
        findViewById(R.id.btnAddRecurringExpense).setOnClickListener(v -> openEditor(null));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        AsyncTaskExecutor.execute(() -> {
            try {
                TallyDatabase database = TallyDatabase.getInstance();
                List<RecurringExpenseEntity> loaded = database.recurringExpenseDao().all();
                List<CategoryModel> categoryList = database.categoryDao().allExpenseCategory();
                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    rules.clear();
                    rules.addAll(loaded);
                    categories.clear();
                    for (CategoryModel category : categoryList) {
                        categories.put(category.getUniqueName(), category.getName());
                    }
                    adapter.notifyDataSetChanged();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        R.string.recurring_expense_error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void openEditor(@Nullable RecurringExpenseEntity rule) {
        Intent intent = new Intent(this, RecurringExpenseEditActivity.class);
        if (rule != null) {
            intent.putExtra(RecurringExpenseEditActivity.EXTRA_ID, rule.id);
            intent.putExtra(RecurringExpenseEditActivity.EXTRA_NAME, rule.name);
            intent.putExtra(RecurringExpenseEditActivity.EXTRA_AMOUNT, rule.amount);
            intent.putExtra(RecurringExpenseEditActivity.EXTRA_DAY, rule.dayOfMonth);
            intent.putExtra(RecurringExpenseEditActivity.EXTRA_CATEGORY, rule.categoryUniqueName);
            intent.putExtra(RecurringExpenseEditActivity.EXTRA_START_MONTH, rule.startMonth);
            intent.putExtra(RecurringExpenseEditActivity.EXTRA_LAST_MONTH, rule.lastGeneratedMonth);
        }
        startActivity(intent);
    }

    private void showActions(RecurringExpenseEntity rule) {
        new AlertDialog.Builder(this)
                .setItems(new String[]{getString(R.string.large_expense_edit), getString(R.string.delete)},
                        (dialog, which) -> {
                            if (which == 0) {
                                openEditor(rule);
                            } else {
                                new AlertDialog.Builder(this)
                                        .setMessage(R.string.recurring_expense_delete_confirm)
                                        .setNegativeButton(R.string.cancel, null)
                                        .setPositiveButton(R.string.delete, (d, w) -> delete(rule.id))
                                        .show();
                            }
                        }).show();
    }

    private void delete(String id) {
        AsyncTaskExecutor.execute(() -> {
            try {
                TallyDatabase.getInstance().recurringExpenseDao().delete(id);
                runOnUiThread(this::refresh);
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        R.string.recurring_expense_error, Toast.LENGTH_SHORT).show());
            }
        });
    }
}
