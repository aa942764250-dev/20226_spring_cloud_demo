package com.example.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.api.dto.EncryptProgressDTO;
import com.example.api.entity.EncryptTask;
import com.example.common.encrypt.EncryptErrorCode;
import com.example.common.encrypt.EncryptException;
import com.example.service.dao.EncryptTaskDao;
import com.example.service.service.EncryptProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 加密进度追踪服务实现。
 * <p>
 * 进度更新使用 REQUIRES_NEW 独立事务，确保加密业务事务回滚时进度信息不丢失。
 * 进度查询时计算完成百分比和基于处理速率的预估剩余时间。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class EncryptProgressServiceImpl implements EncryptProgressService {

    private final EncryptTaskDao taskDao;

    /**
     * {@inheritDoc}
     * <p>
     * 使用 REQUIRES_NEW 传播级别，在独立事务中更新进度，与加密业务事务隔离。
     * </p>
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateProgress(Long taskId, String currentFieldName, int currentBatchNo,
                               long processedRecordCount, long failedRecordCount, String completedFieldList) {
        EncryptTask task = taskDao.selectById(taskId);
        if (task == null) {
            return;
        }
        task.setCurrentFieldName(currentFieldName);
        task.setCurrentBatchNo(currentBatchNo);
        task.setProcessedRecordCount(processedRecordCount);
        task.setFailedRecordCount(failedRecordCount);
        task.setCompletedFieldList(completedFieldList);
        taskDao.updateById(task);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 从任务记录中读取进度信息，计算完成百分比和预估剩余时间。
     * 预估算法：基于已处理记录数/已消耗时间计算处理速率，再推算剩余时间。
     * </p>
     */
    @Override
    public EncryptProgressDTO getProgress(Long taskId) {
        EncryptTask task = taskDao.selectById(taskId);
        if (task == null) {
            throw new EncryptException(EncryptErrorCode.ENCRYPT_CONFIG_INVALID, "任务不存在: " + taskId);
        }

        EncryptProgressDTO dto = new EncryptProgressDTO();
        dto.setTaskId(task.getId());
        dto.setCurrentTableName(task.getTargetTableName());
        dto.setCurrentFieldName(task.getCurrentFieldName());
        dto.setProcessedRecordCount(task.getProcessedRecordCount());
        dto.setTotalRecordCount(task.getTotalRecordCount());
        dto.setLastUpdateTime(task.getFinishTime() != null ? task.getFinishTime() : task.getTriggerTime());

        String fieldList = task.getTargetFieldList();
        if (fieldList != null && !fieldList.isEmpty()) {
            dto.setFieldOrderList(parseFieldNames(fieldList));
        } else {
            dto.setFieldOrderList(Collections.emptyList());
        }

        String completedList = task.getCompletedFieldList();
        if (completedList != null && !completedList.isEmpty()) {
            dto.setCompletedFieldList(Arrays.stream(completedList.split(","))
                    .map(String::trim).collect(Collectors.toList()));
        } else {
            dto.setCompletedFieldList(Collections.emptyList());
        }

        if (task.getTotalRecordCount() != null && task.getTotalRecordCount() > 0) {
            BigDecimal processed = BigDecimal.valueOf(task.getProcessedRecordCount() != null ? task.getProcessedRecordCount() : 0);
            BigDecimal total = BigDecimal.valueOf(task.getTotalRecordCount());
            dto.setCompletionPercentage(processed.multiply(BigDecimal.valueOf(100))
                    .divide(total, 2, RoundingMode.HALF_UP));

            if (task.getProcessedRecordCount() != null && task.getProcessedRecordCount() > 0) {
                Duration elapsed = Duration.between(task.getTriggerTime(), LocalDateTime.now());
                long elapsedSeconds = elapsed.getSeconds();
                if (elapsedSeconds > 0) {
                    BigDecimal rate = processed.divide(BigDecimal.valueOf(elapsedSeconds), 4, RoundingMode.HALF_UP);
                    BigDecimal remaining = total.subtract(processed);
                    dto.setEstimatedRemainingSeconds(remaining.divide(rate, 0, RoundingMode.HALF_UP).longValue());
                }
            }
        } else {
            dto.setCompletionPercentage(BigDecimal.ZERO);
        }

        return dto;
    }

    /**
     * 解析字段列表字符串为字段名列表。
     * <p>
     * 支持从 JSON 数组格式（如 "[field1, field2]"）和逗号分隔格式中提取字段名。
     * </p>
     *
     * @param fieldList 字段列表字符串
     * @return 字段名列表
     */
    private List<String> parseFieldNames(String fieldList) {
        if (fieldList.startsWith("[")) {
            fieldList = fieldList.substring(1);
        }
        if (fieldList.endsWith("]")) {
            fieldList = fieldList.substring(0, fieldList.length() - 1);
        }
        return Arrays.stream(fieldList.split(","))
                .map(s -> s.replaceAll("[\"\\s{}]", "").trim())
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
