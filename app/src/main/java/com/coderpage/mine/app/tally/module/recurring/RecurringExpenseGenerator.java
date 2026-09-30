package com.coderpage.mine.app.tally.module.recurring;

import com.coderpage.mine.app.tally.persistence.sql.TallyDatabase;
import com.coderpage.mine.app.tally.persistence.sql.entity.RecordEntity;
import com.coderpage.mine.app.tally.persistence.sql.entity.RecurringExpenseEntity;

import java.util.Calendar;

/** Creates due monthly expenses when the app is opened. Call from a worker thread. */
public final class RecurringExpenseGenerator {
    private RecurringExpenseGenerator() { }

    public static synchronized boolean generateDue() {
        TallyDatabase database = TallyDatabase.getInstance();
        Calendar now = Calendar.getInstance();
        int currentMonth = monthKey(now);
        final boolean[] created = {false};
        database.runInTransaction(() -> {
            for (RecurringExpenseEntity rule : database.recurringExpenseDao().all()) {
                int first = Math.max(rule.startMonth, nextMonth(rule.lastGeneratedMonth));
                if (first <= 0 || rule.dayOfMonth < 1 || rule.dayOfMonth > 31
                        || rule.amount <= 0 || rule.categoryUniqueName.isEmpty()) continue;
                for (int month = first; month <= currentMonth; month = nextMonth(month)) {
                    Calendar due = Calendar.getInstance();
                    due.clear();
                    due.set(month / 100, month % 100 - 1, 1, 12, 0, 0);
                    due.set(Calendar.DAY_OF_MONTH,
                            Math.min(rule.dayOfMonth, due.getActualMaximum(Calendar.DAY_OF_MONTH)));
                    if (month == currentMonth
                            && due.get(Calendar.DAY_OF_MONTH) > now.get(Calendar.DAY_OF_MONTH)) break;

                    RecordEntity record = new RecordEntity();
                    record.setAmount(rule.amount);
                    record.setTime(due.getTimeInMillis());
                    record.setCategoryUniqueName(rule.categoryUniqueName);
                    record.setDesc(rule.name);
                    record.setSyncId("recurring:" + rule.id + ":" + month);
                    record.setType(RecordEntity.TYPE_EXPENSE);
                    created[0] |= database.recordDao().insertIgnoringConflict(record) != -1;
                    database.recurringExpenseDao().markGenerated(rule.id, month);
                }
            }
        });
        return created[0];
    }

    public static int monthKey(Calendar date) {
        return date.get(Calendar.YEAR) * 100 + date.get(Calendar.MONTH) + 1;
    }

    private static int nextMonth(int month) {
        if (month <= 0) return 0;
        return month % 100 == 12 ? (month / 100 + 1) * 100 + 1 : month + 1;
    }
}
