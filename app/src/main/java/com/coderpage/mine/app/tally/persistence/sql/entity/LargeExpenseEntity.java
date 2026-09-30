package com.coderpage.mine.app.tally.persistence.sql.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

/** A one-off expense kept outside the ordinary monthly record table. */
@Entity(tableName = "large_expense")
public class LargeExpenseEntity {
    @NonNull
    @PrimaryKey
    @ColumnInfo(name = "id")
    public String id = "";

    @ColumnInfo(name = "amount")
    public double amount;

    @NonNull
    @ColumnInfo(name = "note")
    public String note = "";

    @ColumnInfo(name = "expense_time")
    public long time;
}
