package com.example.service.encryptlite.executor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 批次回写事务辅助组件。
 * <p>
 * 使用 REQUIRES_NEW 传播级别确保批次级事务隔离，
 * 任一记录回写失败则回滚整个批次。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EncryptLiteTransactionHelper {

    private final EncryptLiteDao encryptLiteDao;

    /**
     * 在独立事务中批量回写加密后的数据。
     *
     * @param tableName    目标表名
     * @param pkColumnName 主键列名
     * @param records      待回写记录列表（每条含主键值和加密后的字段值）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeBackBatch(String tableName, String pkColumnName, List<Map<String, Object>> records) {
        for (Map<String, Object> record : records) {
            Object pkValue = record.remove(pkColumnName);
            Map<String, String> fieldValues = new HashMap<>();
            for (Map.Entry<String, Object> entry : record.entrySet()) {
                if (entry.getValue() != null) {
                    fieldValues.put(entry.getKey(), entry.getValue().toString());
                }
            }
            if (!fieldValues.isEmpty()) {
                encryptLiteDao.updateRecord(tableName, fieldValues, pkColumnName, pkValue);
            }
        }
    }
}