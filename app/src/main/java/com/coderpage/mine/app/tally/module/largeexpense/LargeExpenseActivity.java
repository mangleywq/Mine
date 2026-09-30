package com.coderpage.mine.app.tally.module.largeexpense;

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
import com.coderpage.mine.app.tally.persistence.sql.TallyDatabase;
import com.coderpage.mine.app.tally.persistence.sql.entity.LargeExpenseEntity;
import com.coderpage.mine.ui.BaseActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Route(path = TallyRouter.LARGE_EXPENSE)
public class LargeExpenseActivity extends BaseActivity {

    private final List<LargeExpenseEntity> expenses = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA);
    private BaseAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_large_expense);
        setToolbarAsBack(v -> finish());
        setToolbarTitle(R.string.large_expense_title);

        ListView list = findViewById(R.id.largeExpenseList);
        Typeface amountFont = Typeface.createFromAsset(getAssets(), "font/Quicksand-Bold.ttf");
        adapter = new BaseAdapter() {
            @Override public int getCount() { return expenses.size(); }
            @Override public Object getItem(int position) { return expenses.get(position); }
            @Override public long getItemId(int position) { return position; }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View row = convertView == null
                        ? LayoutInflater.from(LargeExpenseActivity.this)
                                .inflate(R.layout.tally_item_large_expense, parent, false)
                        : convertView;
                LargeExpenseEntity expense = expenses.get(position);
                TextView noteView = row.findViewById(R.id.largeExpenseNote);
                noteView.setText(expense.note);
                noteView.setVisibility(expense.note.isEmpty() ? View.GONE : View.VISIBLE);
                TextView amountView = row.findViewById(R.id.largeExpenseAmount);
                amountView.setTypeface(amountFont);
                amountView.setText("¥" + String.format(Locale.CHINA, "%.2f", expense.amount));
                ((TextView) row.findViewById(R.id.largeExpenseDate)).setText(
                        dateFormat.format(new Date(expense.time)));
                return row;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> showActions(expenses.get(position)));
        findViewById(R.id.btnAddLargeExpense).setOnClickListener(v -> openEditor(null));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        AsyncTaskExecutor.execute(() -> {
            try {
                List<LargeExpenseEntity> result = TallyDatabase.getInstance().largeExpenseDao().all();
                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    expenses.clear();
                    expenses.addAll(result);
                    adapter.notifyDataSetChanged();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        R.string.large_expense_load_error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void openEditor(@Nullable LargeExpenseEntity expense) {
        Intent intent = new Intent(this, LargeExpenseEditActivity.class);
        if (expense != null) {
            intent.putExtra(LargeExpenseEditActivity.EXTRA_ID, expense.id);
            intent.putExtra(LargeExpenseEditActivity.EXTRA_AMOUNT, expense.amount);
            intent.putExtra(LargeExpenseEditActivity.EXTRA_NOTE, expense.note);
            intent.putExtra(LargeExpenseEditActivity.EXTRA_TIME, expense.time);
        }
        startActivity(intent);
    }

    private void showActions(LargeExpenseEntity expense) {
        new AlertDialog.Builder(this)
                .setItems(new String[]{getString(R.string.large_expense_edit), getString(R.string.delete)},
                        (dialog, which) -> {
                            if (which == 0) {
                                openEditor(expense);
                            } else {
                                new AlertDialog.Builder(this)
                                        .setMessage(R.string.large_expense_delete_confirm)
                                        .setNegativeButton(R.string.cancel, null)
                                        .setPositiveButton(R.string.delete, (d, w) -> delete(expense.id))
                                        .show();
                            }
                        }).show();
    }

    private void delete(String id) {
        AsyncTaskExecutor.execute(() -> {
            try {
                TallyDatabase.getInstance().largeExpenseDao().delete(id);
                runOnUiThread(this::refresh);
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        R.string.large_expense_save_error, Toast.LENGTH_SHORT).show());
            }
        });
    }
}
