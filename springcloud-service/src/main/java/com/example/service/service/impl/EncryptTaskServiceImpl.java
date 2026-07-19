package com.example.service.service.impl;

import com.example.api.dto.EncryptFieldConfigDTO;
import com.example.api.dto.EncryptTableConfigDTO;
import com.example.api.dto.EncryptTaskDTO;
import com.example.api.dto.EncryptTaskRequest;
import com.example.api.dto.EncryptTaskResult;
import com.example.api.entity.EncryptTask;
import com.example.common.encrypt.EncryptErrorCode;
import com.example.common.encrypt.EncryptException;
import com.example.service.dao.EncryptTaskDao;
import com.example.service.service.BatchEncryptExecutor;
import com.example.service.service.EncryptConfigService;
import com.example.service.service.EncryptTaskService;
import com.example.service.service.TaskMutexManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 加密任务管理服务实现。
 * <p>
 * 负责加密任务的生命周期管理，包括触发、断点续执行、取消和查询。
 * 通过 {@link TaskMutexManager} 保证同一时刻仅一个任务执行，
 * 任务异步执行，触发后立即返回任务信息。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EncryptTaskServiceImpl implements EncryptTaskService {

    private final EncryptConfigService configService;
    private final BatchEncryptExecutor batchEncryptExecutor;
    private final TaskMutexManager taskMutexManager;
    private final EncryptTaskDao taskDao;

    /**
     * {@inheritDoc}
     * <p>
     * 获取互斥锁 → 解析加密配置 → 创建任务记录 → 异步执行加密 → 返回任务信息。
     * 互斥锁获取失败或配置无效时释放锁并抛出异常。
     * </p>
     */
    @Override
    public EncryptTaskDTO triggerTask(EncryptTaskRequest request) {
        if (!taskMutexManager.tryAcquire()) {
            throw new EncryptException(EncryptErrorCode.ENCRYPT_TASK_RUNNING);
        }

        try {
            List<EncryptTableConfigDTO> configs;
            if (request.getTableNames() != null && !request.getTableNames().isEmpty()) {
                configs = new ArrayList<>();
                for (String tableName : request.getTableNames()) {
                    EncryptTableConfigDTO config = configService.getConfigByTableName(tableName);
                    if (config == null) {
                        throw new EncryptException(EncryptErrorCode.ENCRYPT_CONFIG_INVALID,
                                "表 " + tableName + " 未配置加密规则");
                    }
                    configs.add(config);
                }
            } else {
                configs = configService.listEnabledConfigs();
            }

            if (configs.isEmpty()) {
                taskMutexManager.release();
                throw new EncryptException(EncryptErrorCode.ENCRYPT_CONFIG_INVALID, "无可用加密配置");
            }

            List<EncryptTask> tasks = new ArrayList<>();
            for (EncryptTableConfigDTO config : configs) {
                EncryptTask task = new EncryptTask();
                task.setTaskStatus("RUNNING");
                task.setTriggerTime(LocalDateTime.now());
                task.setTriggerUser("system");
                task.setTargetTableName(config.getTableName());
                task.setTargetFieldList(config.getFields().stream()
                        .map(EncryptFieldConfigDTO::getFieldName)
                        .collect(Collectors.toList()).toString());
                task.setTotalRecordCount(0L);
                task.setProcessedRecordCount(0L);
                task.setFailedRecordCount(0L);
                task.setCurrentBatchNo(0);
                taskDao.insert(task);
                tasks.add(task);
            }

            for (EncryptTask task : tasks) {
                EncryptTableConfigDTO config = configs.stream()
                        .filter(c -> c.getTableName().equals(task.getTargetTableName()))
                        .findFirst().orElse(null);
                if (config != null) {
                    executeAsync(task, config);
                }
            }

            EncryptTask firstTask = tasks.get(0);
            return convertToDTO(firstTask);
        } catch (EncryptException e) {
            taskMutexManager.release();
            throw e;
        } catch (Exception e) {
            taskMutexManager.release();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * 校验任务状态 → 获取互斥锁 → 恢复任务状态为RUNNING → 异步从断点位置继续执行。
     * </p>
     */
    @Override
    public EncryptTaskDTO resumeTask(Long id) {
        EncryptTask task = taskDao.selectById(id);
        if (task == null) {
            throw new EncryptException(EncryptErrorCode.ENCRYPT_CONFIG_INVALID, "任务不存在: " + id);
        }

        if (!"PARTIAL_SUCCESS".equals(task.getTaskStatus()) && !"FAILED".equals(task.getTaskStatus())) {
            throw new EncryptException(EncryptErrorCode.ENCRYPT_CONFIG_INVALID,
                    "任务状态不支持续执行: " + task.getTaskStatus());
        }

        if (!taskMutexManager.tryAcquire()) {
            throw new EncryptException(EncryptErrorCode.ENCRYPT_TASK_RUNNING);
        }

        task.setTaskStatus("RUNNING");
        taskDao.updateById(task);

        EncryptTableConfigDTO config = configService.getConfigByTableName(task.getTargetTableName());
        if (config == null) {
            taskMutexManager.release();
            throw new EncryptException(EncryptErrorCode.ENCRYPT_CONFIG_INVALID,
                    "表 " + task.getTargetTableName() + " 未配置加密规则");
        }

        executeAsync(task, config);
        return convertToDTO(task);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 设置取消标志 → 更新任务状态为CANCELLED → 释放互斥锁。
     * 当前批次完成后才会真正停止执行。
     * </p>
     */
    @Override
    public void cancelTask(Long id) {
        EncryptTask task = taskDao.selectById(id);
        if (task == null) {
            throw new EncryptException(EncryptErrorCode.ENCRYPT_CONFIG_INVALID, "任务不存在: " + id);
        }

        if (!"RUNNING".equals(task.getTaskStatus())) {
            throw new EncryptException(EncryptErrorCode.ENCRYPT_CONFIG_INVALID,
                    "任务状态不支持取消: " + task.getTaskStatus());
        }

        batchEncryptExecutor.cancel();
        task.setTaskStatus("CANCELLED");
        task.setFinishTime(LocalDateTime.now());
        taskDao.updateById(task);
        taskMutexManager.release();
        log.info("加密任务已取消: id={}", id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EncryptTaskDTO getTask(Long id) {
        EncryptTask task = taskDao.selectById(id);
        if (task == null) {
            throw new EncryptException(EncryptErrorCode.ENCRYPT_CONFIG_INVALID, "任务不存在: " + id);
        }
        return convertToDTO(task);
    }

    /**
     * 异步执行加密任务。
     * <p>
     * 在新线程中调用 {@link BatchEncryptExecutor#execute} 执行加密，
     * 执行完成后根据结果更新任务最终状态（SUCCESS/PARTIAL_SUCCESS/FAILED），
     * 异常时更新为 FAILED，finally 中释放互斥锁。
     * </p>
     *
     * @param task   加密任务记录
     * @param config 加密配置
     */
    private void executeAsync(EncryptTask task, EncryptTableConfigDTO config) {
        new Thread(() -> {
            try {
                log.info("开始执行加密任务: id={}, table={}", task.getId(), config.getTableName());
                EncryptTaskResult result = batchEncryptExecutor.execute(task, config);

                EncryptTask updated = taskDao.selectById(task.getId());
                if (updated != null && "RUNNING".equals(updated.getTaskStatus())) {
                    if (result.getFailedCount() > 0 && result.getSuccessCount() > 0) {
                        updated.setTaskStatus("PARTIAL_SUCCESS");
                    } else if (result.getFailedCount() > 0) {
                        updated.setTaskStatus("FAILED");
                    } else {
                        updated.setTaskStatus("SUCCESS");
                    }
                    updated.setFinishTime(LocalDateTime.now());
                    updated.setProcessedRecordCount(result.getSuccessCount() + result.getFailedCount());
                    updated.setFailedRecordCount(result.getFailedCount());
                    taskDao.updateById(updated);
                }

                log.info("加密任务完成: id={}, status={}", task.getId(),
                        taskDao.selectById(task.getId()).getTaskStatus());
            } catch (Exception e) {
                log.error("加密任务执行异常: id={}", task.getId(), e);
                EncryptTask updated = taskDao.selectById(task.getId());
                if (updated != null) {
                    updated.setTaskStatus("FAILED");
                    updated.setFinishTime(LocalDateTime.now());
                    updated.setErrorMessage(e.getMessage());
                    taskDao.updateById(updated);
                }
            } finally {
                taskMutexManager.release();
            }
        }, "encrypt-task-" + task.getId()).start();
    }

    /**
     * 将加密任务实体转换为DTO。
     *
     * @param task 加密任务实体
     * @return 加密任务DTO
     */
    private EncryptTaskDTO convertToDTO(EncryptTask task) {
        EncryptTaskDTO dto = new EncryptTaskDTO();
        dto.setId(task.getId());
        dto.setTaskStatus(task.getTaskStatus());
        dto.setTriggerTime(task.getTriggerTime());
        dto.setFinishTime(task.getFinishTime());
        dto.setTriggerUser(task.getTriggerUser());
        dto.setTargetTableName(task.getTargetTableName());
        dto.setTargetFieldList(task.getTargetFieldList());
        dto.setTotalRecordCount(task.getTotalRecordCount());
        dto.setProcessedRecordCount(task.getProcessedRecordCount());
        dto.setFailedRecordCount(task.getFailedRecordCount());
        dto.setCurrentBatchNo(task.getCurrentBatchNo());
        dto.setCurrentFieldName(task.getCurrentFieldName());
        return dto;
    }
}
