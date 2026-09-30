package com.coderpage.mine.app.tally.module.backup;

import android.support.annotation.Keep;

@Keep
public class BackupModelRecurringExpense {
    private String id;
    private String name;
    private double amount;
    private int dayOfMonth;
    private String categoryUniqueName;
    private int startMonth;
    private int lastGeneratedMonth;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public int getDayOfMonth() { return dayOfMonth; }
    public void setDayOfMonth(int dayOfMonth) { this.dayOfMonth = dayOfMonth; }
    public String getCategoryUniqueName() { return categoryUniqueName; }
    public void setCategoryUniqueName(String categoryUniqueName) { this.categoryUniqueName = categoryUniqueName; }
    public int getStartMonth() { return startMonth; }
    public void setStartMonth(int startMonth) { this.startMonth = startMonth; }
    public int getLastGeneratedMonth() { return lastGeneratedMonth; }
    public void setLastGeneratedMonth(int lastGeneratedMonth) { this.lastGeneratedMonth = lastGeneratedMonth; }
}
