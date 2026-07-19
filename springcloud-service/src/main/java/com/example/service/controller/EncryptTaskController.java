package com.example.service.controller;

import com.example.api.dto.EncryptProgressDTO;
import com.example.api.dto.EncryptTaskDTO;
import com.example.api.dto.EncryptTaskRequest;
import com.example.common.result.Result;
import com.example.service.service.EncryptProgressService;
import com.example.service.service.EncryptTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 加密任务管理REST接口。
 * <p>
 * 提供加密任务的触发、断点续执行、取消和进度查询能力。
 * 同一时刻仅允许一个加密任务执行。
 * </p>
 */
@RestController
@RequestMapping("/api/v1/encrypt/task")
@RequiredArgsConstructor
public class EncryptTaskController {

    private final EncryptTaskService encryptTaskService;
    private final EncryptProgressService encryptProgressService;

    /**
     * 触发加密初始化任务。
     * <p>
     * 可通过 request.tableNames 指定待加密的表，为空时对所有启用的配置表执行。
     * 任务异步执行，触发后立即返回任务信息。
     * </p>
     *
     * @param request 任务触发请求
     * @return 加密任务信息
     */
    @PostMapping
    public Result<EncryptTaskDTO> triggerTask(@RequestBody EncryptTaskRequest request) {
        return Result.success(encryptTaskService.triggerTask(request));
    }

    /**
     * 从断点位置继续执行加密任务。
     * <p>
     * 仅支持状态为 PARTIAL_SUCCESS 或 FAILED 的任务续执行。
     * </p>
     *
     * @param id 任务ID
     * @return 更新后的加密任务信息
     */
    @PostMapping("/{id}/resume")
    public Result<EncryptTaskDTO> resumeTask(@PathVariable Long id) {
        return Result.success(encryptTaskService.resumeTask(id));
    }

    /**
     * 取消正在执行的加密任务。
     *
     * @param id 任务ID
     * @return 空响应
     */
    @PostMapping("/{id}/cancel")
    public Result<Void> cancelTask(@PathVariable Long id) {
        encryptTaskService.cancelTask(id);
        return Result.success(null);
    }

    /**
     * 查询加密任务进度。
     *
     * @param id 任务ID
     * @return 加密进度详情（含完成百分比和预估剩余时间）
     */
    @GetMapping("/{id}/progress")
    public Result<EncryptProgressDTO> getProgress(@PathVariable Long id) {
        return Result.success(encryptProgressService.getProgress(id));
    }
}
