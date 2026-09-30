package com.coderpage.mine.app.tally.persistence.sql.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

/** A monthly rule; generated expenses are ordinary records. */
@Entity(tableName = "recurring_expense")
public class RecurringExpenseEntity {
    @NonNull
    @PrimaryKey
    @ColumnInfo(name = "id")
    public String id = "";

    @NonNull
    @ColumnInfo(name = "name")
    public String name = "";

    @ColumnInfo(name = "amount")
    public double amount;

    @ColumnInfo(name = "day_of_month")
    public int dayOfMonth;

    @NonNull
    @ColumnInfo(name = "category_unique_name")
    public String categoryUniqueName = "";

    @ColumnInfo(name = "start_month")
    public int startMonth;

    @ColumnInfo(name = "last_generated_month")
    public int lastGeneratedMonth;
}
