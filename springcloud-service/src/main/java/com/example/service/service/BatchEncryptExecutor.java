package com.example.service.service;

import com.example.api.dto.EncryptFieldConfigDTO;
import com.example.api.dto.EncryptTableConfigDTO;
import com.example.api.dto.EncryptTaskResult;
import com.example.api.entity.EncryptTask;
import com.example.api.entity.EncryptTaskError;
import com.example.common.encrypt.EncryptAlgorithm;
import com.example.common.encrypt.EncryptErrorCode;
import com.example.common.encrypt.EncryptException;
import com.example.service.config.EncryptGlobalProperties;
import com.example.service.dao.DynamicTableDao;
import com.example.service.dao.EncryptTaskDao;
import com.example.service.dao.EncryptTaskErrorDao;
import com.example.service.encrypt.EncryptAlgorithmRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 批量加密执行引擎。
 * <p>
 * 核心加密执行组件，负责按配置对目标表的指定字段进行分批加密处理。
 * 执行流程为三层循环：外层遍历配置表 → 中层遍历表的加密字段 → 内层按批次处理数据。
 * 支持断点续执行、任务取消、重复加密防护、批次级事务提交和失败记录收集。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchEncryptExecutor {

    private final DynamicTableDao dynamicTableDao;
    private final EncryptAlgorithmRouter algorithmRouter;
    private final EncryptTaskDao taskDao;
    private final EncryptTaskErrorDao taskErrorDao;
    private final EncryptProgressService progressService;
    private final EncryptGlobalProperties properties;
    private final RetryTemplate retryTemplate;

    /** 任务取消标志，volatile保证可见性 */
    private volatile boolean cancelled = false;

    /**
     * 设置取消标志，当前批次完成后停止执行。
     */
    public void cancel() {
        this.cancelled = true;
    }

    /**
     * 执行批量加密任务。
     * <p>
     * 执行流程：
     * <ol>
     *     <li>查询目标表主键列名和总记录数</li>
     *     <li>按字段配置的 fieldOrder 排序确定加密顺序</li>
     *     <li>外层循环遍历字段，跳过已完成的字段</li>
     *     <li>内层循环按批次读取数据，对每条记录加密后暂存到内存</li>
     *     <li>批次内所有记录加密完成后，在独立事务中批量回写密文</li>
     *     <li>每批次完成后更新进度，批次间按配置间隔等待</li>
     * </ol>
     * </p>
     *
     * @param task   加密任务记录（含断点续执行信息）
     * @param config 加密配置（含表名、字段列表、批次参数）
     * @return 加密执行结果（含成功/失败计数和错误详情）
     */
    public EncryptTaskResult execute(EncryptTask task, EncryptTableConfigDTO config) {
        this.cancelled = false;
        EncryptTaskResult result = new EncryptTaskResult();
        result.setTaskId(task.getId());
        result.setTaskStatus("RUNNING");

        String tableName = config.getTableName();
        String pkColumnName = dynamicTableDao.queryPrimaryKeyColumn(tableName);
        if (pkColumnName == null) {
            throw new EncryptException(EncryptErrorCode.ENCRYPT_CONFIG_INVALID,
                    "表 " + tableName + " 未找到主键列");
        }

        long totalRecords = dynamicTableDao.countByTable(tableName);
        task.setTotalRecordCount(totalRecords);
        taskDao.updateById(task);

        int batchSize = config.getBatchSize() != null ? config.getBatchSize() : properties.getDefaultBatchSize();
        int batchIntervalMs = config.getBatchIntervalMs() != null ? config.getBatchIntervalMs() : properties.getDefaultBatchIntervalMs();

        List<EncryptFieldConfigDTO> fields = config.getFields().stream()
                .sorted(Comparator.comparingInt(f -> f.getFieldOrder() != null ? f.getFieldOrder() : 0))
                .collect(Collectors.toList());

        List<String> fieldNames = fields.stream().map(EncryptFieldConfigDTO::getFieldName).collect(Collectors.toList());
        List<String> columnNames = new ArrayList<>(fieldNames);
        columnNames.add(pkColumnName);

        Set<String> completedFieldSet = new HashSet<>();
        if (task.getCompletedFieldList() != null && !task.getCompletedFieldList().isEmpty()) {
            completedFieldSet.addAll(Arrays.stream(task.getCompletedFieldList().split(","))
                    .map(String::trim).collect(Collectors.toList()));
        }

        long processedCount = task.getProcessedRecordCount() != null ? task.getProcessedRecordCount() : 0;
        long failedCount = task.getFailedRecordCount() != null ? task.getFailedRecordCount() : 0;
        List<EncryptTaskError> allErrors = new ArrayList<>();
        int startBatchNo = task.getCurrentBatchNo() != null ? task.getCurrentBatchNo() : 0;

        String resumeFieldName = task.getCurrentFieldName();
        boolean skipToResumeField = resumeFieldName != null && !resumeFieldName.isEmpty();

        for (EncryptFieldConfigDTO fieldConfig : fields) {
            if (cancelled) {
                break;
            }

            String fieldName = fieldConfig.getFieldName();

            if (completedFieldSet.contains(fieldName)) {
                continue;
            }

            if (skipToResumeField && !fieldName.equals(resumeFieldName)) {
                continue;
            }
            skipToResumeField = false;

            String algorithmId = fieldConfig.getAlgorithmId() != null ? fieldConfig.getAlgorithmId() : properties.getDefaultAlgorithm();
            boolean skipEncrypted = fieldConfig.getSkipEncrypted() != null ? fieldConfig.getSkipEncrypted() : properties.getDefaultSkipEncrypted();

            log.info("开始加密字段: {}.{}", tableName, fieldName);

            int batchNo = startBatchNo;
            final Object[] lastPkHolder = {null};

            if (batchNo > 0) {
                List<Map<String, Object>> skipData = dynamicTableDao.batchSelect(
                        tableName, Collections.singletonList(pkColumnName), pkColumnName, null, batchNo * batchSize);
                if (!skipData.isEmpty()) {
                    lastPkHolder[0] = skipData.get(skipData.size() - 1).get(pkColumnName);
                }
            }

            while (true) {
                if (cancelled) {
                    break;
                }

                batchNo++;
                final int currentBatchNo = batchNo;
                List<Map<String, Object>> batchData;
                try {
                    batchData = retryTemplate.executeWithRetry(
                            () -> dynamicTableDao.batchSelect(tableName, columnNames, pkColumnName, lastPkHolder[0], batchSize),
                            "读取批次数据[" + tableName + "." + fieldName + " batch=" + currentBatchNo + "]");
                } catch (EncryptException e) {
                    log.error("读取批次数据失败: {}.{} batch={}", tableName, fieldName, batchNo, e);
                    failedCount += batchSize;
                    break;
                }

                if (batchData == null || batchData.isEmpty()) {
                    break;
                }

                List<Map<String, Object>> successRecords = new ArrayList<>();
                List<EncryptTaskError> batchErrors = new ArrayList<>();

                for (Map<String, Object> record : batchData) {
                    Object pkValue = record.get(pkColumnName);
                    String originalValue = record.get(fieldName) != null ? record.get(fieldName).toString() : null;

                    if (originalValue == null) {
                        continue;
                    }

                    if (skipEncrypted && algorithmRouter.isEncrypted(originalValue)) {
                        continue;
                    }

                    try {
                        EncryptAlgorithm algorithm = algorithmRouter.route(algorithmId);
                        String encryptedValue = algorithm.encrypt(originalValue);
                        Map<String, Object> successRecord = new HashMap<>();
                        successRecord.put(pkColumnName, pkValue);
                        successRecord.put(fieldName, encryptedValue);
                        successRecords.add(successRecord);
                    } catch (Exception e) {
                        EncryptTaskError error = new EncryptTaskError();
                        error.setTaskId(task.getId());
                        error.setBatchNo(batchNo);
                        error.setFieldName(fieldName);
                        error.setRecordPrimaryKey(String.valueOf(pkValue));
                        error.setErrorCode(EncryptErrorCode.ENCRYPT_METHOD_ERROR.name());
                        error.setErrorMessage(e.getMessage());
                        error.setCreateTime(LocalDateTime.now());
                        batchErrors.add(error);
                        log.warn("加密失败: {}.{} pk={}", tableName, fieldName, pkValue, e);
                    }
                }

                if (!successRecords.isEmpty()) {
                    try {
                        writeBackEncryptedData(tableName, pkColumnName, successRecords);
                    } catch (Exception e) {
                        log.error("回写密文失败: {}.{} batch={}", tableName, fieldName, batchNo, e);
                        for (Map<String, Object> rec : successRecords) {
                            EncryptTaskError error = new EncryptTaskError();
                            error.setTaskId(task.getId());
                            error.setBatchNo(batchNo);
                            error.setFieldName(fieldName);
                            error.setRecordPrimaryKey(String.valueOf(rec.get(pkColumnName)));
                            error.setErrorCode(EncryptErrorCode.ENCRYPT_DB_WRITE_ERROR.name());
                            error.setErrorMessage(e.getMessage());
                            error.setCreateTime(LocalDateTime.now());
                            batchErrors.add(error);
                        }
                    }
                }

                if (!batchErrors.isEmpty()) {
                    for (EncryptTaskError error : batchErrors) {
                        taskErrorDao.insert(error);
                    }
                    allErrors.addAll(batchErrors);
                    failedCount += batchErrors.size();
                }

                processedCount += batchData.size();
                lastPkHolder[0] = batchData.get(batchData.size() - 1).get(pkColumnName);

                progressService.updateProgress(task.getId(), fieldName, batchNo,
                        processedCount, failedCount, task.getCompletedFieldList());

                log.info("批次完成: {}.{} batch={} processed={}/{}", tableName, fieldName, batchNo, processedCount, totalRecords);

                if (batchIntervalMs > 0) {
                    try {
                        Thread.sleep(batchIntervalMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            completedFieldSet.add(fieldName);
            String completedFieldStr = String.join(",", completedFieldSet);
            progressService.updateProgress(task.getId(), fieldName, 0,
                    processedCount, failedCount, completedFieldStr);
            startBatchNo = 0;

            log.info("字段加密完成: {}.{}", tableName, fieldName);
        }

        result.setSuccessCount(processedCount - failedCount);
        result.setFailedCount(failedCount);
        result.setErrors(allErrors);
        return result;
    }

    /**
     * 在独立事务中批量回写加密后的数据。
     * <p>
     * 使用 REQUIRES_NEW 传播级别，确保回写操作在独立事务中执行。
     * 任一记录回写失败会导致整个批次回滚。
     * </p>
     *
     * @param tableName    目标表名
     * @param pkColumnName 主键列名
     * @param records      待回写的记录列表（每条记录含主键值和加密后的字段值）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeBackEncryptedData(String tableName, String pkColumnName, List<Map<String, Object>> records) {
        for (Map<String, Object> record : records) {
            Object pkValue = record.remove(pkColumnName);
            Map<String, String> fieldValues = new HashMap<>();
            for (Map.Entry<String, Object> entry : record.entrySet()) {
                if (entry.getValue() != null) {
                    fieldValues.put(entry.getKey(), entry.getValue().toString());
                }
            }
            if (!fieldValues.isEmpty()) {
                dynamicTableDao.updateRecord(tableName, fieldValues, pkColumnName, pkValue);
            }
        }
    }
}
