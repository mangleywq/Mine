package com.coderpage.mine.app.tally.module.recurring;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.coderpage.base.utils.UIUtils;
import com.coderpage.concurrency.AsyncTaskExecutor;
import com.coderpage.mine.R;
import com.coderpage.mine.app.tally.data.CategoryContant;
import com.coderpage.mine.app.tally.persistence.model.CategoryModel;
import com.coderpage.mine.app.tally.persistence.sql.TallyDatabase;
import com.coderpage.mine.app.tally.persistence.sql.entity.RecurringExpenseEntity;
import com.coderpage.mine.app.tally.ui.widget.TallyAmountKeypad;
import com.coderpage.mine.ui.BaseActivity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class RecurringExpenseEditActivity extends BaseActivity {
    private static final int[] AVAILABLE_DAYS = {1, 15};
    public static final String EXTRA_ID = "recurring_id";
    public static final String EXTRA_NAME = "recurring_name";
    public static final String EXTRA_AMOUNT = "recurring_amount";
    public static final String EXTRA_DAY = "recurring_day";
    public static final String EXTRA_CATEGORY = "recurring_category";
    public static final String EXTRA_START_MONTH = "recurring_start_month";
    public static final String EXTRA_LAST_MONTH = "recurring_last_month";

    private final List<CategoryModel> categories = new ArrayList<>();
    private EditText nameView;
    private TextView amountView;
    private TextView dayView;
    private TextView categoryView;
    private int dayOfMonth;
    private String categoryUniqueName;
    private boolean saving;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recurring_expense_edit);
        setToolbarAsClose(v -> finish());
        setToolbarTitle(R.string.recurring_expense_title);
        nameView = findViewById(R.id.recurringExpenseEditName);
        nameView.setOnEditorActionListener((view, actionId, event) -> {
            UIUtils.hideSoftKeyboard(this, nameView);
            nameView.clearFocus();
            return true;
        });
        amountView = findViewById(R.id.recurringExpenseEditAmount);
        amountView.setTypeface(Typeface.createFromAsset(getAssets(), "font/Quicksand-Medium.ttf"));
        dayView = findViewById(R.id.recurringExpenseEditDay);
        categoryView = findViewById(R.id.recurringExpenseEditCategory);

        String id = getIntent().getStringExtra(EXTRA_ID);
        dayOfMonth = getIntent().getIntExtra(EXTRA_DAY, 1);
        categoryUniqueName = getIntent().getStringExtra(EXTRA_CATEGORY);
        if (categoryUniqueName == null) categoryUniqueName = CategoryContant.NAME_TONG_XUN;
        if (id != null) {
            nameView.setText(getIntent().getStringExtra(EXTRA_NAME));
            amountView.setText(BigDecimal.valueOf(getIntent().getDoubleExtra(EXTRA_AMOUNT, 0))
                    .stripTrailingZeros().toPlainString());
        }
        updateDay();
        categoryView.setText(R.string.recurring_expense_category);
        dayView.setOnClickListener(v -> {
            UIUtils.hideSoftKeyboard(this, nameView);
            chooseDay();
        });
        categoryView.setOnClickListener(v -> {
            UIUtils.hideSoftKeyboard(this, nameView);
            chooseCategory();
        });
        TallyAmountKeypad.attach(this, findViewById(R.id.recurringExpenseKeypad), amountView,
                this::save);
        loadCategories();
    }

    private void updateDay() {
        dayView.setText(getString(R.string.recurring_expense_day_format, dayOfMonth));
    }

    private void chooseDay() {
        String[] days = {
                getString(R.string.recurring_expense_day_format, AVAILABLE_DAYS[0]),
                getString(R.string.recurring_expense_day_format, AVAILABLE_DAYS[1])};
        int selectedDay = dayOfMonth == 1 ? 0 : dayOfMonth == 15 ? 1 : -1;
        new AlertDialog.Builder(this)
                .setTitle(R.string.recurring_expense_day)
                .setSingleChoiceItems(days, selectedDay, (dialog, selected) -> {
                    dayOfMonth = AVAILABLE_DAYS[selected];
                    updateDay();
                    dialog.dismiss();
                }).show();
    }

    private void loadCategories() {
        AsyncTaskExecutor.execute(() -> {
            try {
                List<CategoryModel> loaded = TallyDatabase.getInstance()
                        .categoryDao().allExpenseCategory();
                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    categories.clear();
                    categories.addAll(loaded);
                    if (categories.isEmpty()) return;
                    int selected = selectedCategoryIndex();
                    if (selected < 0) {
                        selected = 0;
                        categoryUniqueName = categories.get(0).getUniqueName();
                    }
                    updateCategory();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        R.string.recurring_expense_error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private int selectedCategoryIndex() {
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).getUniqueName().equals(categoryUniqueName)) return i;
        }
        return -1;
    }

    private void updateCategory() {
        int selected = selectedCategoryIndex();
        if (selected >= 0) {
            categoryView.setText(getString(R.string.recurring_expense_category)
                    + "：" + categories.get(selected).getName());
        }
    }

    private void chooseCategory() {
        if (categories.isEmpty()) return;
        String[] names = new String[categories.size()];
        for (int i = 0; i < names.length; i++) names[i] = categories.get(i).getName();
        new AlertDialog.Builder(this)
                .setTitle(R.string.recurring_expense_category)
                .setSingleChoiceItems(names, selectedCategoryIndex(), (dialog, selected) -> {
                    categoryUniqueName = categories.get(selected).getUniqueName();
                    updateCategory();
                    dialog.dismiss();
                }).show();
    }

    private void save() {
        if (saving) return;
        if (categories.isEmpty()) {
            Toast.makeText(this, R.string.recurring_expense_error, Toast.LENGTH_SHORT).show();
            return;
        }
        String name = nameView.getText().toString().trim();
        BigDecimal amount;
        try {
            amount = new BigDecimal(amountView.getText().toString());
            if (name.isEmpty() || amount.signum() <= 0 || amount.scale() > 2
                    || Double.isInfinite(amount.doubleValue())) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.recurring_expense_invalid, Toast.LENGTH_SHORT).show();
            return;
        }

        RecurringExpenseEntity rule = new RecurringExpenseEntity();
        String id = getIntent().getStringExtra(EXTRA_ID);
        rule.id = id == null ? UUID.randomUUID().toString() : id;
        rule.name = name;
        rule.amount = amount.doubleValue();
        rule.dayOfMonth = dayOfMonth;
        rule.categoryUniqueName = categoryUniqueName;
        Calendar firstDue = Calendar.getInstance();
        if (Math.min(dayOfMonth, firstDue.getActualMaximum(Calendar.DAY_OF_MONTH))
                < firstDue.get(Calendar.DAY_OF_MONTH)) {
            firstDue.add(Calendar.MONTH, 1);
        }
        rule.startMonth = getIntent().getIntExtra(EXTRA_START_MONTH,
                RecurringExpenseGenerator.monthKey(firstDue));
        rule.lastGeneratedMonth = getIntent().getIntExtra(EXTRA_LAST_MONTH, 0);
        saving = true;
        AsyncTaskExecutor.execute(() -> {
            try {
                TallyDatabase database = TallyDatabase.getInstance();
                database.runInTransaction(() -> {
                    RecurringExpenseEntity current = database.recurringExpenseDao().byId(rule.id);
                    if (current != null) {
                        rule.startMonth = current.startMonth;
                        rule.lastGeneratedMonth = Math.max(rule.lastGeneratedMonth,
                                current.lastGeneratedMonth);
                    }
                    database.recurringExpenseDao().save(rule);
                });
                RecurringExpenseGenerator.generateDue();
                runOnUiThread(this::finish);
            } catch (Exception e) {
                runOnUiThread(() -> {
                    saving = false;
                    Toast.makeText(this, R.string.recurring_expense_error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
