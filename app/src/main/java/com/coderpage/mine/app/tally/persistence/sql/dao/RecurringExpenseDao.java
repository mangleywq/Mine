package com.coderpage.mine.app.tally.persistence.sql.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import com.coderpage.mine.app.tally.persistence.sql.entity.RecurringExpenseEntity;

import java.util.List;

@Dao
public interface RecurringExpenseDao {
    @Query("SELECT * FROM recurring_expense ORDER BY name")
    List<RecurringExpenseEntity> all();

    @Query("SELECT * FROM recurring_expense WHERE id = :id LIMIT 1")
    RecurringExpenseEntity byId(String id);

    @Query("SELECT count(*) FROM recurring_expense WHERE category_unique_name = :categoryUniqueName")
    int countByCategory(String categoryUniqueName);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void save(RecurringExpenseEntity... rules);

    @Query("DELETE FROM recurring_expense WHERE id = :id")
    void delete(String id);

    @Query("UPDATE recurring_expense SET last_generated_month = :month WHERE id = :id")
    void markGenerated(String id, int month);
}
