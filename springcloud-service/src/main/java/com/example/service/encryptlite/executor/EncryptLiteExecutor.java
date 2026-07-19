package com.example.service.encryptlite.executor;

import com.example.service.encryptlite.config.EncryptLiteProperties;
import com.example.service.encryptlite.crypto.EncryptLiteService;
import com.example.service.encryptlite.exception.EncryptLiteErrorCode;
import com.example.service.encryptlite.exception.EncryptLiteException;
import com.example.service.encryptlite.model.EncryptLiteErrorDetail;
import com.example.service.encryptlite.model.EncryptLiteRequest;
import com.example.service.encryptlite.model.EncryptLiteSummaryResult;
import com.example.service.encryptlite.model.EncryptLiteTableResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 轻量批量加密执行引擎。
 * <p>
 * 核心执行流程：解析配置 → 遍历表 → 按批次读取 → 逐字段加密 → 独立事务回写。
 * 支持任务互斥、已加密数据跳过、异常容错（失败不中断整体流程）。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EncryptLiteExecutor {

    private final EncryptLiteDao encryptLiteDao;
    private final EncryptLiteService encryptLiteService;
    private final EncryptLiteProperties properties;
    private final EncryptLiteMutexManager mutexManager;
    private final EncryptLiteTransactionHelper transactionHelper;

    /**
     * 执行批量加密初始化。
     *
     * @param request 加密请求（可选指定表名列表）
     * @return 汇总结果
     */
    public EncryptLiteSummaryResult execute(EncryptLiteRequest request) {
        if (!mutexManager.tryAcquire()) {
            throw new EncryptLiteException(EncryptLiteErrorCode.ENCRYPT_TASK_RUNNING);
        }

        try {
            Map<String, EncryptLiteProperties.TableConfig> tableConfigs = resolveConfigs(request);

            EncryptLiteSummaryResult summary = new EncryptLiteSummaryResult();
            List<EncryptLiteTableResult> tableResults = new ArrayList<>();
            int successTables = 0;
            int failedTables = 0;

            for (Map.Entry<String, EncryptLiteProperties.TableConfig> entry : tableConfigs.entrySet()) {
                String tableName = entry.getKey();
                List<String> fields = entry.getValue().getFields();

                EncryptLiteTableResult tableResult = processTable(tableName, fields);
                tableResults.add(tableResult);

                if (tableResult.getFailedCount() > 0) {
                    failedTables++;
                } else {
                    successTables++;
                }
            }

            summary.setTotalTables(tableConfigs.size());
            summary.setSuccessTables(successTables);
            summary.setFailedTables(failedTables);
            summary.setTableResults(tableResults);
            return summary;
        } finally {
            mutexManager.release();
        }
    }

    /**
     * 解析请求，确定待加密的表配置列表。
     */
    private Map<String, EncryptLiteProperties.TableConfig> resolveConfigs(EncryptLiteRequest request) {
        Map<String, EncryptLiteProperties.TableConfig> allConfigs = properties.getTables();
        if (allConfigs == null || allConfigs.isEmpty()) {
            throw new EncryptLiteException(EncryptLiteErrorCode.ENCRYPT_NO_CONFIG);
        }

        if (request == null || request.getTableNames() == null || request.getTableNames().isEmpty()) {
            return allConfigs;
        }

        Map<String, EncryptLiteProperties.TableConfig> filtered = new HashMap<>();
        for (String tableName : request.getTableNames()) {
            EncryptLiteProperties.TableConfig config = allConfigs.get(tableName);
            if (config == null) {
                log.warn("表 {} 未在配置中找到，跳过", tableName);
                continue;
            }
            filtered.put(tableName, config);
        }

        if (filtered.isEmpty()) {
            throw new EncryptLiteException(EncryptLiteErrorCode.ENCRYPT_NO_CONFIG, "指定的表均未配置加密规则");
        }
        return filtered;
    }

    /**
     * 处理单张表的加密。
     */
    private EncryptLiteTableResult processTable(String tableName, List<String> fields) {
        EncryptLiteTableResult result = new EncryptLiteTableResult();
        result.setTableName(tableName);
        result.setSuccessCount(0);
        result.setFailedCount(0);
        result.setSkippedCount(0);
        List<EncryptLiteErrorDetail> errorDetails = new ArrayList<>();

        if (!encryptLiteDao.checkTableExists(tableName)) {
            log.warn("表 {} 不存在，跳过", tableName);
            result.setErrorDetails(errorDetails);
            return result;
        }

        String pkColumnName = encryptLiteDao.queryPrimaryKeyColumn(tableName);
        if (pkColumnName == null) {
            log.warn("表 {} 无主键，跳过", tableName);
            result.setErrorDetails(errorDetails);
            return result;
        }

        List<String> validFields = new ArrayList<>();
        for (String field : fields) {
            if (field.equalsIgnoreCase(pkColumnName)) {
                log.warn("表 {} 的字段 {} 为主键，跳过加密", tableName, field);
                continue;
            }
            if (!encryptLiteDao.checkFieldExists(tableName, field)) {
                log.warn("表 {} 的字段 {} 不存在，跳过", tableName, field);
                continue;
            }
            validFields.add(field);
        }

        if (validFields.isEmpty()) {
            log.warn("表 {} 无有效加密字段，跳过", tableName);
            result.setErrorDetails(errorDetails);
            return result;
        }

        List<String> columnNames = new ArrayList<>(validFields);
        columnNames.add(pkColumnName);

        int batchSize = properties.getDefaultBatchSize();
        Object lastPkValue = null;
        long successCount = 0;
        long failedCount = 0;
        long skippedCount = 0;

        while (true) {
            List<Map<String, Object>> batchData;
            try {
                batchData = encryptLiteDao.batchSelect(tableName, columnNames, pkColumnName, lastPkValue, batchSize);
            } catch (Exception e) {
                log.error("读取表 {} 数据失败", tableName, e);
                failedCount += batchSize;
                break;
            }

            if (batchData == null || batchData.isEmpty()) {
                break;
            }

            List<Map<String, Object>> successRecords = new ArrayList<>();

            for (Map<String, Object> record : batchData) {
                Object pkValue = record.get(pkColumnName);
                Map<String, Object> encryptedRecord = new HashMap<>();
                encryptedRecord.put(pkColumnName, pkValue);
                boolean recordSuccess = true;

                for (String field : validFields) {
                    Object fieldValue = record.get(field);
                    if (fieldValue == null) {
                        skippedCount++;
                        continue;
                    }

                    String originalValue = fieldValue.toString();
                    if (encryptLiteService.isEncrypted(originalValue)) {
                        skippedCount++;
                        continue;
                    }

                    try {
                        String encryptedValue = encryptLiteService.encrypt(originalValue);
                        encryptedRecord.put(field, encryptedValue);
                    } catch (Exception e) {
                        EncryptLiteErrorDetail detail = new EncryptLiteErrorDetail();
                        detail.setPrimaryKeyValue(String.valueOf(pkValue));
                        detail.setFieldName(field);
                        detail.setErrorCode(EncryptLiteErrorCode.ENCRYPT_METHOD_ERROR.name());
                        detail.setErrorMessage(e.getMessage());
                        errorDetails.add(detail);
                        recordSuccess = false;
                        break;
                    }
                }

                if (recordSuccess && encryptedRecord.size() > 1) {
                    successRecords.add(encryptedRecord);
                } else if (!recordSuccess) {
                    failedCount++;
                }
            }

            if (!successRecords.isEmpty()) {
                try {
                    transactionHelper.writeBackBatch(tableName, pkColumnName, successRecords);
                    successCount += successRecords.size();
                } catch (Exception e) {
                    log.error("回写表 {} 密文失败", tableName, e);
                    for (Map<String, Object> rec : successRecords) {
                        EncryptLiteErrorDetail detail = new EncryptLiteErrorDetail();
                        detail.setPrimaryKeyValue(String.valueOf(rec.get(pkColumnName)));
                        detail.setErrorCode(EncryptLiteErrorCode.ENCRYPT_DB_WRITE_ERROR.name());
                        detail.setErrorMessage(e.getMessage());
                        errorDetails.add(detail);
                    }
                    failedCount += successRecords.size();
                }
            }

            lastPkValue = batchData.get(batchData.size() - 1).get(pkColumnName);
            log.info("表 {} 批次完成, success={}, failed={}, skipped={}", tableName, successCount, failedCount, skippedCount);
        }

        result.setSuccessCount(successCount);
        result.setFailedCount(failedCount);
        result.setSkippedCount(skippedCount);
        result.setErrorDetails(errorDetails);
        return result;
    }
}