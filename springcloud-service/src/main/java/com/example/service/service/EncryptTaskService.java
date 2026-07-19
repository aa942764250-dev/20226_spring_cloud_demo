package com.example.service.service;

import com.example.api.dto.EncryptTaskDTO;
import com.example.api.dto.EncryptTaskRequest;

/**
 * 加密任务管理服务接口。
 * <p>
 * 提供加密任务的触发、断点续执行、取消和查询能力。
 * 同一时刻仅允许一个加密任务执行（通过 {@link TaskMutexManager} 互斥控制）。
 * </p>
 */
public interface EncryptTaskService {

    /**
     * 触发加密初始化任务。
     * <p>
     * 可指定表名列表，为空时对所有启用的配置表执行。
     * 任务异步执行，触发后立即返回任务信息。
     * </p>
     *
     * @param request 任务触发请求
     * @return 加密任务信息
     */
    EncryptTaskDTO triggerTask(EncryptTaskRequest request);

    /**
     * 从断点位置继续执行加密任务。
     * <p>
     * 仅支持状态为 PARTIAL_SUCCESS 或 FAILED 的任务续执行。
     * </p>
     *
     * @param id 任务ID
     * @return 更新后的加密任务信息
     */
    EncryptTaskDTO resumeTask(Long id);

    /**
     * 取消正在执行的加密任务。
     * <p>
     * 当前批次完成后停止执行，任务状态变为 CANCELLED。
     * </p>
     *
     * @param id 任务ID
     */
    void cancelTask(Long id);

    /**
     * 查询加密任务信息。
     *
     * @param id 任务ID
     * @return 加密任务信息
     */
    EncryptTaskDTO getTask(Long id);
}
