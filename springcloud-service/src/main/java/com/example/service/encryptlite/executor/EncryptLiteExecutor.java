package com.example.service.encryptlite.executor;

import com.example.service.encryptlite.crypto.DefaultEncryptLiteService;
import com.example.service.encryptlite.config.EncryptLiteProperties;
import com.example.service.encryptlite.config.EncryptLiteProperties.FieldConfig;
import com.example.service.encryptlite.crypto.EncryptLiteService;
import com.example.service.encryptlite.exception.EncryptLiteErrorCode;
import com.example.service.encryptlite.exception.EncryptLiteException;
import com.example.service.encryptlite.model.EncryptLiteErrorDetail;
import com.example.service.encryptlite.model.EncryptLiteSummaryResult;
import com.example.service.encryptlite.model.EncryptLiteTableResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EncryptLiteExecutor {

    private final EncryptLiteDao encryptLiteDao;
    private final EncryptLiteService encryptLiteService;
    private final EncryptLiteProperties properties;
    private final EncryptLiteMutexManager mutexManager;
    private final EncryptLiteTransactionHelper transactionHelper;
    private final DefaultEncryptLiteService defaultEncryptLiteService;

    /**
     * 校验接口：检查 JSON 配置的所有表/字段是否符合加密条件。
     * CLOB字段(length=0)跳过长度校验，VARCHAR2字段校验长度是否足够。
     */
    public EncryptLiteSummaryResult verify() {
        Map<String, EncryptLiteProperties.TableConfig> tableConfigs = requireConfigs();
        EncryptLiteSummaryResult summary = new EncryptLiteSummaryResult();
        List<EncryptLiteTableResult> tableResults = new ArrayList<>();

        for (Map.Entry<String, EncryptLiteProperties.TableConfig> entry : tableConfigs.entrySet()) {
            String tableName = entry.getKey();
            List<FieldConfig> fields = entry.getValue().getFields();
            List<String> fieldNames = fields.stream().map(FieldConfig::getName).collect(Collectors.toList());

            EncryptLiteTableResult tableResult = new EncryptLiteTableResult();
            tableResult.setTableName(tableName);

            if (!encryptLiteDao.checkTableExists(tableName)) {
                tableResult.setFailedCount(-1);
                tableResult.setErrorMessage("配置表不存在: " + tableName);
                tableResults.add(tableResult);
                continue;
            }

            for (FieldConfig fc : fields) {
                if (!encryptLiteDao.checkFieldExists(tableName, fc.getName())) {
                    tableResult.setFailedCount(tableResult.getFailedCount() + 1);
                    tableResult.setErrorMessage("字段不存在: " + tableName + "." + fc.getName());
                    continue;
                }
                if (fc.getLength() == 0) continue;
                long actualLen = encryptLiteDao.queryFieldCharLength(tableName, fc.getName());
                if (actualLen <= 0) continue;
                long maxCipher = calcMaxCiphertextLength(actualLen);
                if (maxCipher > actualLen) {
                    tableResult.setFailedCount(tableResult.getFailedCount() + 1);
                    tableResult.setErrorMessage(tableName + "." + fc.getName()
                            + " 长度" + actualLen + "不足(需" + maxCipher + ")");
                }
            }

            tableResults.add(tableResult);
        }

        int failed = (int) tableResults.stream().filter(t -> t.getFailedCount() != 0).count();
        summary.setTotalTables(tableConfigs.size());
        summary.setSuccessTables(tableConfigs.size() - failed);
        summary.setFailedTables(failed);
        summary.setTableResults(tableResults);
        return summary;
    }

    public EncryptLiteSummaryResult check() {
        Map<String, EncryptLiteProperties.TableConfig> tableConfigs = requireConfigs();
        EncryptLiteSummaryResult summary = new EncryptLiteSummaryResult();
        List<EncryptLiteTableResult> tableResults = new ArrayList<>();

        for (Map.Entry<String, EncryptLiteProperties.TableConfig> entry : tableConfigs.entrySet()) {
            String tableName = entry.getKey();
            List<FieldConfig> fields = entry.getValue().getFields();
            List<String> fieldNames = fields.stream().map(FieldConfig::getName).collect(Collectors.toList());

            if (!encryptLiteDao.checkTableExists(tableName)) {
                throw new EncryptLiteException(EncryptLiteErrorCode.ENCRYPT_CONFIG_INVALID, "配置表不存在: " + tableName);
            }
            String pkColumnName = encryptLiteDao.queryPrimaryKeyColumn(tableName);
            if (pkColumnName == null) {
                throw new EncryptLiteException(EncryptLiteErrorCode.ENCRYPT_CONFIG_INVALID, "配置表没有主键: " + tableName);
            }
            for (FieldConfig fc : fields) {
                if (!encryptLiteDao.checkFieldExists(tableName, fc.getName())) {
                    throw new EncryptLiteException(EncryptLiteErrorCode.ENCRYPT_CONFIG_INVALID, "配置字段不存在: " + tableName + "." + fc.getName());
                }
            }

            long candidateRows = encryptLiteDao.countCandidates(tableName, fieldNames);
            EncryptLiteTableResult tableResult = new EncryptLiteTableResult();
            tableResult.setTableName(tableName);
            tableResult.setSkippedCount(candidateRows);
            tableResults.add(tableResult);
        }

        summary.setTotalTables(tableConfigs.size());
        summary.setSuccessTables(tableConfigs.size());
        summary.setFailedTables(0);
        summary.setTableResults(tableResults);
        return summary;
    }

    public EncryptLiteSummaryResult execute() {
        if (!mutexManager.tryAcquire()) {
            throw new EncryptLiteException(EncryptLiteErrorCode.ENCRYPT_TASK_RUNNING);
        }

        try {
            defaultEncryptLiteService.selfCheck();
            Map<String, EncryptLiteProperties.TableConfig> tableConfigs = requireConfigs();
            EncryptLiteSummaryResult summary = new EncryptLiteSummaryResult();
            List<EncryptLiteTableResult> tableResults = new ArrayList<>();
            int successTables = 0;
            int failedTables = 0;

            for (Map.Entry<String, EncryptLiteProperties.TableConfig> entry : tableConfigs.entrySet()) {
                String tableName = entry.getKey();
                List<FieldConfig> fields = entry.getValue().getFields();

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

    private Map<String, EncryptLiteProperties.TableConfig> requireConfigs() {
        Map<String, EncryptLiteProperties.TableConfig> tableConfigs = properties.getTables();
        if (tableConfigs == null || tableConfigs.isEmpty()) {
            throw new EncryptLiteException(EncryptLiteErrorCode.ENCRYPT_NO_CONFIG);
        }
        return tableConfigs;
    }

    private EncryptLiteTableResult processTable(String tableName, List<FieldConfig> fields) {
        EncryptLiteTableResult result = new EncryptLiteTableResult();
        result.setTableName(tableName);
        result.setSuccessCount(0);
        result.setFailedCount(0);
        result.setSkippedCount(0);
        List<EncryptLiteErrorDetail> errorDetails = new ArrayList<>();

        if (!encryptLiteDao.checkTableExists(tableName)) {
            throw new EncryptLiteException(EncryptLiteErrorCode.ENCRYPT_CONFIG_INVALID, "配置表不存在: " + tableName);
        }

        String pkColumnName = encryptLiteDao.queryPrimaryKeyColumn(tableName);
        if (pkColumnName == null) {
            throw new EncryptLiteException(EncryptLiteErrorCode.ENCRYPT_CONFIG_INVALID, "配置表没有主键: " + tableName);
        }

        List<FieldConfig> validFields = new ArrayList<>();
        for (FieldConfig fc : fields) {
            if (fc.getName().equalsIgnoreCase(pkColumnName)) {
                log.warn("表 {} 的字段 {} 为主键，跳过加密", tableName, fc.getName());
                continue;
            }
            if (!encryptLiteDao.checkFieldExists(tableName, fc.getName())) {
                throw new EncryptLiteException(EncryptLiteErrorCode.ENCRYPT_CONFIG_INVALID, "配置字段不存在: " + tableName + "." + fc.getName());
            }
            validFields.add(fc);
        }

        if (validFields.isEmpty()) {
            log.warn("表 {} 无有效加密字段，跳过", tableName);
            result.setErrorDetails(errorDetails);
            return result;
        }

        List<String> columnNames = validFields.stream().map(FieldConfig::getName).collect(Collectors.toList());
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
                failedCount = -1;
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
                boolean changed = false;

                for (FieldConfig fc : validFields) {
                    Object fieldValue = record.get(fc.getName());
                    if (fieldValue == null) {
                        continue;
                    }

                    String originalValue = fieldValue.toString();
                    if (encryptLiteService.isEncrypted(originalValue)) {
                        continue;
                    }

                    try {
                        String encryptedValue = encryptLiteService.encrypt(originalValue);
                        encryptedRecord.put(fc.getName(), encryptedValue);
                        changed = true;
                    } catch (Exception e) {
                        EncryptLiteErrorDetail detail = new EncryptLiteErrorDetail();
                        detail.setPrimaryKeyValue(String.valueOf(pkValue));
                        detail.setFieldName(fc.getName());
                        detail.setErrorCode(EncryptLiteErrorCode.ENCRYPT_METHOD_ERROR.name());
                        detail.setErrorMessage(e.getMessage());
                        errorDetails.add(detail);
                        throw new EncryptLiteException(EncryptLiteErrorCode.ENCRYPT_METHOD_ERROR,
                                "表 " + tableName + " 字段 " + fc.getName() + " 加密失败", e);
                    }
                }

                if (encryptedRecord.size() > 1) {
                    successRecords.add(encryptedRecord);
                } else if (!changed) {
                    skippedCount++;
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
                    throw new EncryptLiteException(EncryptLiteErrorCode.ENCRYPT_DB_WRITE_ERROR,
                            "表 " + tableName + " 批次回写失败", e);
                }
            }

            lastPkValue = batchData.get(batchData.size() - 1).get(pkColumnName);
            log.info("DATA_INIT_ENCRYPT BATCH_END table={} success={} skipped={}", tableName, successCount, skippedCount);
        }

        result.setSuccessCount(successCount);
        result.setFailedCount(failedCount);
        result.setSkippedCount(skippedCount);
        result.setErrorDetails(errorDetails);
        return result;
    }

    static long calcMaxCiphertextLength(long fieldCharLength) {
        long cipherBytes = (long) Math.ceil((fieldCharLength + 1) / 16.0) * 16;
        long base64Chars = (long) Math.ceil(cipherBytes / 3.0) * 4;
        return 5 + base64Chars;
    }
}