package com.coderpage.mine.app.tally.persistence.sql.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import com.coderpage.mine.app.tally.persistence.sql.entity.LargeExpenseEntity;

import java.util.List;

@Dao
public interface LargeExpenseDao {
    @Query("SELECT * FROM large_expense ORDER BY expense_time DESC")
    List<LargeExpenseEntity> all();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void save(LargeExpenseEntity... expenses);

    @Query("DELETE FROM large_expense WHERE id = :id")
    void delete(String id);
}
