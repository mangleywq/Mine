package com.coderpage.mine.app.tally.module.largeexpense;

import android.app.DatePickerDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.coderpage.concurrency.AsyncTaskExecutor;
import com.coderpage.mine.R;
import com.coderpage.mine.app.tally.persistence.sql.TallyDatabase;
import com.coderpage.mine.app.tally.persistence.sql.entity.LargeExpenseEntity;
import com.coderpage.mine.app.tally.ui.widget.TallyAmountKeypad;
import com.coderpage.mine.ui.BaseActivity;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

/** Full-screen editor using the same keypad and layout rhythm as ordinary records. */
public class LargeExpenseEditActivity extends BaseActivity {
    public static final String EXTRA_ID = "large_expense_id";
    public static final String EXTRA_AMOUNT = "large_expense_amount";
    public static final String EXTRA_NOTE = "large_expense_note";
    public static final String EXTRA_TIME = "large_expense_time";

    private final Calendar selectedDate = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA);
    private TextView amountView;
    private EditText noteView;
    private TextView dateView;
    private boolean saving;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_large_expense_edit);
        setToolbarAsClose(v -> finish());

        String id = getIntent().getStringExtra(EXTRA_ID);
        setToolbarTitle(id == null ? R.string.large_expense_add : R.string.large_expense_edit);
        amountView = findViewById(R.id.largeExpenseEditAmount);
        amountView.setTypeface(Typeface.createFromAsset(getAssets(), "font/Quicksand-Medium.ttf"));
        noteView = findViewById(R.id.largeExpenseEditNote);
        dateView = findViewById(R.id.largeExpenseEditDate);
        if (id != null) {
            amountView.setText(BigDecimal.valueOf(getIntent().getDoubleExtra(EXTRA_AMOUNT, 0))
                    .stripTrailingZeros().toPlainString());
            noteView.setText(getIntent().getStringExtra(EXTRA_NOTE));
            selectedDate.setTimeInMillis(getIntent().getLongExtra(EXTRA_TIME,
                    System.currentTimeMillis()));
        }
        updateDate();
        dateView.setOnClickListener(v -> new DatePickerDialog(this, (picker, year, month, day) -> {
            selectedDate.set(year, month, day);
            updateDate();
        }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)).show());
        TallyAmountKeypad.attach(this, findViewById(R.id.largeExpenseKeypad), amountView,
                this::save);
    }

    private void updateDate() {
        dateView.setText(dateFormat.format(selectedDate.getTime()));
    }

    private void save() {
        if (saving) return;
        BigDecimal value;
        try {
            value = new BigDecimal(amountView.getText().toString());
            if (value.signum() <= 0 || value.scale() > 2
                    || Double.isInfinite(value.doubleValue()) || Double.isNaN(value.doubleValue())) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.large_expense_invalid_amount, Toast.LENGTH_SHORT).show();
            return;
        }
        saving = true;
        LargeExpenseEntity expense = new LargeExpenseEntity();
        String originalId = getIntent().getStringExtra(EXTRA_ID);
        expense.id = originalId == null ? UUID.randomUUID().toString() : originalId;
        expense.amount = value.doubleValue();
        expense.note = noteView.getText().toString().trim();
        expense.time = selectedDate.getTimeInMillis();
        AsyncTaskExecutor.execute(() -> {
            try {
                TallyDatabase.getInstance().largeExpenseDao().save(expense);
                runOnUiThread(this::finish);
            } catch (Exception e) {
                runOnUiThread(() -> {
                    saving = false;
                    Toast.makeText(this, R.string.large_expense_save_error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
