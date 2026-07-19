package com.example.service.service;

import com.example.api.dto.EncryptProgressDTO;

/**
 * 加密进度追踪服务接口。
 * <p>
 * 提供加密任务进度的更新和查询能力。
 * 进度更新使用独立事务（REQUIRES_NEW），确保加密事务回滚时进度不丢失。
 * </p>
 */
public interface EncryptProgressService {

    /**
     * 更新加密任务进度。
     * <p>
     * 使用独立事务更新，与加密业务事务隔离。
     * </p>
     *
     * @param taskId              任务ID
     * @param currentFieldName    当前加密字段名
     * @param currentBatchNo      当前批次号
     * @param processedRecordCount 已处理记录数
     * @param failedRecordCount   失败记录数
     * @param completedFieldList  已完成字段列表（逗号分隔）
     */
    void updateProgress(Long taskId, String currentFieldName, int currentBatchNo,
                        long processedRecordCount, long failedRecordCount, String completedFieldList);

    /**
     * 查询加密任务进度详情。
     *
     * @param taskId 任务ID
     * @return 加密进度信息，包含完成百分比和预估剩余时间
     */
    EncryptProgressDTO getProgress(Long taskId);
}
