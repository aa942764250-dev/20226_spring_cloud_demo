package com.example.service.encryptlite.controller;

import com.example.common.result.Result;
import com.example.service.encryptlite.exception.EncryptLiteException;
import com.example.service.encryptlite.model.EncryptLiteRequest;
import com.example.service.encryptlite.model.EncryptLiteSummaryResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 轻量加密初始化REST接口。
 * <p>
 * 提供批量加密初始化触发接口，读取YAML配置中的表和字段映射，执行批量加密。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/encrypt-lite")
@RequiredArgsConstructor
public class EncryptLiteController {

    private final com.example.service.encryptlite.executor.EncryptLiteExecutor encryptLiteExecutor;

    /**
     * 触发批量加密初始化。
     * <p>
     * 请求体可选指定表名列表，为空时对所有配置表执行。同步执行，返回汇总结果。
     * </p>
     *
     * @param request 加密请求（可选）
     * @return 汇总结果
     */
    @PostMapping("/init")
    public Result<EncryptLiteSummaryResult> initEncrypt(@RequestBody(required = false) EncryptLiteRequest request) {
        return Result.success(encryptLiteExecutor.execute(request));
    }
}