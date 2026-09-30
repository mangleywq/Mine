package com.coderpage.mine.app.tally.module.backup;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.coderpage.base.common.Callback;
import com.coderpage.base.common.IError;
import com.coderpage.base.common.NonThrowError;
import com.coderpage.base.utils.ArrayUtils;
import com.coderpage.base.utils.CommonUtils;
import com.coderpage.base.utils.LogUtils;
import com.coderpage.concurrency.AsyncTaskExecutor;
import com.coderpage.mine.BuildConfig;
import com.coderpage.mine.app.tally.common.error.ErrorCode;
import com.coderpage.mine.app.tally.persistence.model.CategoryModel;
import com.coderpage.mine.app.tally.persistence.model.Record;
import com.coderpage.mine.app.tally.persistence.sql.TallyDatabase;
import com.coderpage.mine.app.tally.persistence.sql.dao.CategoryDao;
import com.coderpage.mine.app.tally.persistence.sql.entity.CategoryEntity;
import com.coderpage.mine.app.tally.persistence.sql.entity.LargeExpenseEntity;
import com.coderpage.mine.app.tally.persistence.sql.entity.RecordEntity;
import com.coderpage.mine.app.tally.persistence.sql.entity.RecurringExpenseEntity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.coderpage.base.utils.LogUtils.LOGE;
import static com.coderpage.base.utils.LogUtils.makeLogTag;

/**
 * @author abner-l. 2017-06-01
 * @since 0.4.0
 */

public class Backup {

    private static final String TAG = makeLogTag(Backup.class);

    /**
     * 备份过程回调；
     * 回调包括 {@link BackupProgress#READ_DATA} {@link BackupProgress#WRITE_FILE}
     */
    public interface BackupProgressListener extends Callback<Void, IError> {
        /**
         * 回到
         *
         * @param backupProgress progress
         */
        void onProgressUpdate(BackupProgress backupProgress);
    }

    public enum BackupProgress {
        // 读取数据
        READ_DATA,
        // 写入备份文件
        WRITE_FILE,
    }

    /**
     * 恢复文件过程回调；
     */
    public interface RestoreProgressListener extends Callback<BackupModel, IError> {
        /**
         * 更新回调
         *
         * @param restoreProgress progress
         */
        void onProgressUpdate(RestoreProgress restoreProgress);
    }

    public enum RestoreProgress {
        // 读取文件
        READ_FILE,
        // 检查文件格式
        CHECK_FILE_FORMAT,
        // 恢复文件到数据库
        RESTORE_TO_DB
    }

    /**
     * 备份消费记录到 JSON 文件中；
     *
     * @param context  {@link Context}
     * @param listener 备份回调
     */
    public static void backupToJsonFile(Context context, BackupProgressListener listener) {
        AsyncTaskExecutor.execute(() -> {
            listener.onProgressUpdate(BackupProgress.READ_DATA);
            BackupModel backupModel = readData();

            listener.onProgressUpdate(BackupProgress.WRITE_FILE);
            new BackupCache(context).backup2JsonFile(backupModel, listener);
        });
    }

    /**
     * 读取备份的 JSON 文件。
     *
     * @param file     {@link File}备份文件
     * @param listener 回调
     */
    public static void readBackupJsonFile(File file, RestoreProgressListener listener) {
        AsyncTaskExecutor.execute(() -> {
            listener.onProgressUpdate(RestoreProgress.READ_FILE);
            if (file == null || !file.isFile()) {
                listener.failure(new NonThrowError(ErrorCode.ILLEGAL_ARGS, "File not found"));
                return;
            }
            LogUtils.LOGD(TAG, "Read backup json file: " + file.getAbsolutePath());
            try {
                readBackupJsonStream(new FileInputStream(file), listener);
            } catch (IOException e) {
                LOGE(TAG, "File io err", e);
                listener.failure(new NonThrowError(ErrorCode.INTERNAL_ERR, "File io err"));
            }
        });
    }

    /** Read a selected document without resolving its content URI to a filesystem path. */
    public static void readBackupJsonUri(Context context, Uri uri, RestoreProgressListener listener) {
        AsyncTaskExecutor.execute(() -> {
            listener.onProgressUpdate(RestoreProgress.READ_FILE);
            if (uri == null) {
                listener.failure(new NonThrowError(ErrorCode.ILLEGAL_ARGS, "File not found"));
                return;
            }
            try {
                InputStream stream = context.getContentResolver().openInputStream(uri);
                if (stream == null) {
                    listener.failure(new NonThrowError(ErrorCode.ILLEGAL_ARGS, "File not found"));
                    return;
                }
                readBackupJsonStream(stream, listener);
            } catch (IOException | SecurityException e) {
                LOGE(TAG, "File io err", e);
                listener.failure(new NonThrowError(ErrorCode.INTERNAL_ERR, "File io err"));
            }
        });
    }

    private static void readBackupJsonStream(InputStream stream, RestoreProgressListener listener) {
            String sourceString;
            try (BufferedReader bufferedReader =
                         new BufferedReader(new InputStreamReader(stream, "UTF-8"))) {
                String line;
                StringBuilder sourceBuilder = new StringBuilder();
                while ((line = bufferedReader.readLine()) != null) {
                    sourceBuilder.append(line);
                }
                sourceString = sourceBuilder.toString();
            } catch (IOException e) {
                LOGE(TAG, "IO Err", e);
                listener.failure(new NonThrowError(ErrorCode.INTERNAL_ERR, "File io err"));
                return;
            }

            listener.onProgressUpdate(RestoreProgress.CHECK_FILE_FORMAT);
            try {
                BackupModel backupModel = JSON.parseObject(sourceString, BackupModel.class);
                if (backupModel == null || backupModel.getMetadata() == null) {
                    throw new IllegalArgumentException("Missing backup metadata");
                }
                listener.success(backupModel);
            } catch (Exception e) {
                LOGE(TAG, "Parse json err", e);
                listener.failure(new NonThrowError(ErrorCode.INTERNAL_ERR, "not a json file"));
            }
    }

    /**
     * 将备份的数据恢复到数据库
     *
     * @param context     {@link Context}
     * @param backupModel {@link BackupModel} 备份的数据
     * @param listener    恢复到数据库的回调
     */
    public static void restoreDataFromBackupData(Context context,
                                                 BackupModel backupModel,
                                                 RestoreProgressListener listener) {
        AsyncTaskExecutor.execute(() -> {
            listener.onProgressUpdate(RestoreProgress.RESTORE_TO_DB);
            if (backupModel == null || backupModel.getMetadata() == null) {
                listener.failure(new NonThrowError(ErrorCode.ILLEGAL_ARGS, "备份文件缺少基本信息"));
                return;
            }
            try {
                TallyDatabase.getInstance().runInTransaction(() -> restoreTables(backupModel));
                listener.success(backupModel);
            } catch (Exception e) {
                LOGE(TAG, "恢复备份失败", e);
                listener.failure(new NonThrowError(ErrorCode.SQL_ERR, "恢复备份失败，数据未导入"));
            }
        });
    }

    private static void restoreTables(BackupModel backupModel) {
        BackupModelMetadata metadata = backupModel.getMetadata();
            // 恢复分类表数据
            List<BackupModelCategory> categoryList = backupModel.getCategoryList();
            if (categoryList != null && !categoryList.isEmpty()) {
                boolean restoreCategoryOk = restoreCategoryTable(metadata, categoryList);
                if (!restoreCategoryOk) {
                    throw new IllegalStateException("恢复分类数据失败");
                }
            }

            // 恢复消费表数据
            List<BackupModelRecord> expenseList = backupModel.getExpenseList();
            if (expenseList != null && !expenseList.isEmpty()) {
                boolean restoreExpenseOk = restoreExpenseTable(metadata, expenseList);
                if (!restoreExpenseOk) {
                    throw new IllegalStateException("恢复消费数据失败");
                }
            }

            List<BackupModelLargeExpense> largeExpenseList = backupModel.getLargeExpenseList();
            if (largeExpenseList != null && !largeExpenseList.isEmpty()) {
                    List<LargeExpenseEntity> entities = new ArrayList<>();
                    for (BackupModelLargeExpense item : largeExpenseList) {
                        if (TextUtils.isEmpty(item.getId()) || item.getAmount() <= 0) {
                            continue;
                        }
                        LargeExpenseEntity entity = new LargeExpenseEntity();
                        entity.id = item.getId();
                        entity.amount = item.getAmount();
                        entity.note = item.getNote() == null ? "" : item.getNote();
                        entity.time = item.getTime();
                        entities.add(entity);
                    }
                    TallyDatabase.getInstance().largeExpenseDao().save(
                            entities.toArray(new LargeExpenseEntity[0]));
            }

            List<BackupModelRecurringExpense> recurringList = backupModel.getRecurringExpenseList();
            if (recurringList != null && !recurringList.isEmpty()) {
                    List<RecurringExpenseEntity> entities = new ArrayList<>();
                    for (BackupModelRecurringExpense item : recurringList) {
                        if (TextUtils.isEmpty(item.getId()) || TextUtils.isEmpty(item.getName())
                                || TextUtils.isEmpty(item.getCategoryUniqueName())
                                || item.getAmount() <= 0 || item.getDayOfMonth() < 1
                                || item.getDayOfMonth() > 31 || item.getStartMonth() <= 0) continue;
                        RecurringExpenseEntity entity = new RecurringExpenseEntity();
                        entity.id = item.getId();
                        entity.name = item.getName();
                        entity.amount = item.getAmount();
                        entity.dayOfMonth = item.getDayOfMonth();
                        entity.categoryUniqueName = item.getCategoryUniqueName();
                        entity.startMonth = item.getStartMonth();
                        entity.lastGeneratedMonth = item.getLastGeneratedMonth();
                        entities.add(entity);
                    }
                    TallyDatabase.getInstance().recurringExpenseDao().save(
                            entities.toArray(new RecurringExpenseEntity[0]));
            }
    }

    /**
     * 读取默认备份文件目录中所有的备份文件。
     *
     * @param context {@link Context}
     * @return 默认备份文件存放目录中的所有备份文件
     */
    public static List<File> listBackupFiles(Context context) {
        return new BackupCache(context).listBackupFiles();
    }

    private static boolean restoreCategoryTable(BackupModelMetadata metadata,
                                                List<BackupModelCategory> categoryList) {
        CategoryDao categoryDao = TallyDatabase.getInstance().categoryDao();
        List<CategoryModel> currentExistCategoryList = categoryDao.allCategory();

        List<CategoryEntity> entityList = new ArrayList<>();
        for (int i = 0; i < categoryList.size(); i++) {
            BackupModelCategory backupCategory = categoryList.get(i);
            CategoryEntity entity = new CategoryEntity();
            entity.setName(backupCategory.getName());
            entity.setIcon(backupCategory.getIcon());
            entity.setAccountId(backupCategory.getAccountId());
            entity.setSyncStatus(backupCategory.getSyncStatus());
            entity.setHidden(backupCategory.getHidden());
            // 0.6.0 版本之前没有 type 之分，全部为支出分类类型
            entity.setType(metadata.getClientVersionCode() < 60 ?
                    CategoryEntity.TYPE_EXPENSE : backupCategory.getType());
            // 0.6.0 版本之前没有 uniqueCategoryName，全部统一使用 category icon
            entity.setUniqueName(TextUtils.isEmpty(backupCategory.getUniqueName()) ?
                    backupCategory.getIcon() : backupCategory.getUniqueName());

            boolean alreadyContains = ArrayUtils.contains(currentExistCategoryList, item -> {
                return CommonUtils.isEqual(entity.getUniqueName(), item.getUniqueName())
                        && entity.getAccountId() == item.getAccountId();
            });
            if (alreadyContains) {
                continue;
            }
            entityList.add(entity);
        }

        if (entityList.isEmpty()) {
            return true;
        }

        CategoryEntity[] insertArray = new CategoryEntity[entityList.size()];
        ArrayUtils.forEach(entityList, (count, index, item) -> {
            insertArray[index] = item;
        });
        try {
            categoryDao.insert(insertArray);
            return true;
        } catch (Exception e) {
            LOGE(TAG, "恢复数据失败-分类表", e);
        }

        return false;
    }

    private static boolean restoreExpenseTable(BackupModelMetadata metadata,
                                               List<BackupModelRecord> expenseList) {
        // 0.6.0 版本之前备份的数据。单独处理
        if (metadata.getClientVersionCode() < 60) {
            return restoreExpenseTableBefore060(expenseList);
        }

        TallyDatabase database = TallyDatabase.getInstance();
        RecordEntity[] insertArray = new RecordEntity[expenseList.size()];
        for (int i = 0; i < expenseList.size(); i++) {
            BackupModelRecord backupExpense = expenseList.get(i);

            RecordEntity entity = new RecordEntity();
            entity.setAccountId(backupExpense.getAccountId());
            entity.setAmount(backupExpense.getAmount());
            entity.setTime(backupExpense.getTime());
            entity.setCategoryUniqueName(backupExpense.getCategoryUniqueName());
            entity.setDesc(backupExpense.getDesc());
            entity.setSyncId(backupExpense.getSyncId());
            entity.setSyncStatus(backupExpense.getSyncStatus());
            entity.setType(backupExpense.getType());

            insertArray[i] = entity;
        }

        try {
            database.recordDao().insert(insertArray);
            return true;
        } catch (Exception e) {
            LOGE(TAG, "恢复数据失败-消费记录表", e);
        }

        return false;
    }

    /**
     * 恢复 0.6.0 版本之前的备份数据
     *
     * @param recordList 记录列表
     * @return 恢复结果
     */
    private static boolean restoreExpenseTableBefore060(List<BackupModelRecord> recordList) {
        TallyDatabase database = TallyDatabase.getInstance();
        List<CategoryModel> expenseCategoryList = database.categoryDao().allCategory();

        // categoryName - categoryUniqueName Map
        HashMap<String, String> getCategoryUniqueNameByName = new HashMap<>();
        for (CategoryModel category : expenseCategoryList) {
            if (category.getType() == CategoryModel.TYPE_EXPENSE) {
                getCategoryUniqueNameByName.put(category.getName(), category.getUniqueName());
            }
        }

        RecordEntity[] insertArray = new RecordEntity[recordList.size()];
        for (int i = 0; i < recordList.size(); i++) {
            BackupModelRecord backupExpense = recordList.get(i);

            // 0.6.0 版本之前，不支持收入类型的记录，不支持修改分类名称
            // 可以通过"分类名称"来获取对应的 categoryUniqueName
            String categoryUniqueName = getCategoryUniqueNameByName.get(backupExpense.getCategory());

            RecordEntity entity = new RecordEntity();
            entity.setAccountId(backupExpense.getAccountId());
            entity.setAmount(backupExpense.getAmount());
            entity.setTime(backupExpense.getTime());
            entity.setCategoryUniqueName(categoryUniqueName);
            entity.setDesc(backupExpense.getDesc());
            entity.setSyncId(backupExpense.getSyncId());
            entity.setSyncStatus(backupExpense.getSyncStatus());
            entity.setType(backupExpense.getType());

            insertArray[i] = entity;
        }

        try {
            database.recordDao().insert(insertArray);
            return true;
        } catch (Exception e) {
            LOGE(TAG, "恢复数据失败-消费记录表", e);
        }

        return false;
    }

    /**
     * 读取数据库数据并格式化为{@link BackupModel}
     *
     * @return 返回从数据库读取的所有数据
     */
    private static BackupModel readData() {

        List<BackupModelCategory> categoryList = new ArrayList<>();
        List<BackupModelRecord> recordList = null;
        BackupModelMetadata metadata = new BackupModelMetadata();

        TallyDatabase database = TallyDatabase.getInstance();

        List<CategoryModel> categoryEntityList = database.categoryDao().allCategory();
        for (CategoryModel entity : categoryEntityList) {
            BackupModelCategory category = new BackupModelCategory();
            category.setName(entity.getName());
            category.setUniqueName(entity.getUniqueName());
            category.setIcon(entity.getIcon());
            category.setAccountId(entity.getAccountId());
            category.setType(entity.getType());
            category.setSyncStatus(entity.getSyncStatus());
            category.setHidden(entity.getHidden());

            categoryList.add(category);
        }

        List<Record> recordEntityList = database.recordDao().queryAll();
        recordList = new ArrayList<>(recordEntityList.size());
        for (Record entity : recordEntityList) {
            BackupModelRecord expense = new BackupModelRecord();
            expense.setAmount(entity.getAmount());
            expense.setDesc(entity.getDesc());
            expense.setCategory(entity.getCategoryName());
            expense.setTime(entity.getTime());
            expense.setSyncId(entity.getSyncId());
            expense.setAccountId(entity.getAccountId());
            expense.setSyncStatus(entity.getSyncStatus());
            expense.setCategoryUniqueName(entity.getCategoryUniqueName());
            expense.setType(entity.getType());

            recordList.add(expense);
        }

        metadata.setBackupDate(System.currentTimeMillis());
        metadata.setClientVersion(BuildConfig.VERSION_NAME);
        metadata.setDeviceName(Build.MODEL);
        metadata.setClientVersion(BuildConfig.VERSION_NAME);
        metadata.setClientVersionCode(BuildConfig.VERSION_CODE);
        metadata.setExpenseNumber(recordList.size());

        BackupModel backupModel = new BackupModel();

        backupModel.setMetadata(metadata);
        backupModel.setCategoryList(categoryList);
        backupModel.setExpenseList(recordList);

        List<BackupModelLargeExpense> largeExpenseList = new ArrayList<>();
        for (LargeExpenseEntity entity : database.largeExpenseDao().all()) {
            BackupModelLargeExpense item = new BackupModelLargeExpense();
            item.setId(entity.id);
            item.setAmount(entity.amount);
            item.setNote(entity.note);
            item.setTime(entity.time);
            largeExpenseList.add(item);
        }
        backupModel.setLargeExpenseList(largeExpenseList);

        List<BackupModelRecurringExpense> recurringList = new ArrayList<>();
        for (RecurringExpenseEntity entity : database.recurringExpenseDao().all()) {
            BackupModelRecurringExpense item = new BackupModelRecurringExpense();
            item.setId(entity.id);
            item.setName(entity.name);
            item.setAmount(entity.amount);
            item.setDayOfMonth(entity.dayOfMonth);
            item.setCategoryUniqueName(entity.categoryUniqueName);
            item.setStartMonth(entity.startMonth);
            item.setLastGeneratedMonth(entity.lastGeneratedMonth);
            recurringList.add(item);
        }
        backupModel.setRecurringExpenseList(recurringList);

        return backupModel;
    }


}
