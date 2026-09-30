package com.coderpage.mine.app.tally.module.backup;

import com.alibaba.fastjson.JSON;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeTrue;

public class BackupModelCompatibilityTest {
    @Test
    public void oldBackupWithoutNewModulesParses() {
        String json = "{\"metadata\":{\"client_version_code\":73},"
                + "\"category_list\":[],\"expense_list\":[{\"1\":12.5,\"2\":\"车票\","
                + "\"4\":1790000000000,\"5\":\"old-id\",\"7\":\"JiaoTong\",\"9\":0}]}";
        BackupModel backup = JSON.parseObject(json, BackupModel.class);
        assertNotNull(backup.getMetadata());
        assertEquals(1, backup.getExpenseList().size());
        assertEquals("old-id", backup.getExpenseList().get(0).getSyncId());
        assertNull(backup.getLargeExpenseList());
        assertNull(backup.getRecurringExpenseList());
    }

    @Test
    public void suppliedOldBackupParsesWhenProvided() throws Exception {
        String path = System.getenv("MINE_TEST_BACKUP_FILE");
        assumeTrue(path != null && !path.isEmpty());
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        BackupModel backup = JSON.parseObject(new String(bytes, StandardCharsets.UTF_8), BackupModel.class);
        assertNotNull(backup.getMetadata());
        assertEquals(833, backup.getExpenseList().size());
        assertEquals(28, backup.getCategoryList().size());
        assertNull(backup.getLargeExpenseList());
        assertNull(backup.getRecurringExpenseList());
    }
}
