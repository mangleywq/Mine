package com.coderpage.mine.app.tally.module.backup;

import android.support.annotation.Keep;

@Keep
public class BackupModelLargeExpense {
    private String id;
    private double amount;
    private String note;
    private long time;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public long getTime() { return time; }
    public void setTime(long time) { this.time = time; }
}
