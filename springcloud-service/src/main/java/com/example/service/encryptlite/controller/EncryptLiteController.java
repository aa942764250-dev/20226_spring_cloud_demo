package com.example.service.encryptlite.controller;

import com.example.common.result.Result;
import com.example.service.encryptlite.config.EncryptLiteProperties;
import com.example.service.encryptlite.exception.EncryptLiteErrorCode;
import com.example.service.encryptlite.exception.EncryptLiteException;
import com.example.service.encryptlite.model.EncryptLiteRequest;
import com.example.service.encryptlite.model.EncryptLiteSummaryResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 轻量加密初始化REST接口。
 * <p>
 * 提供预检（CHECK）和正式执行（EXECUTE）两种模式。
 * 表名和字段名只从外部JSON读取，请求不接收表名、字段名、SQL或密钥。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/encrypt-lite")
@RequiredArgsConstructor
public class EncryptLiteController {

    private static final String CONFIRM_CODE = "ENCRYPT_EXISTING_DATA";

    private final com.example.service.encryptlite.executor.EncryptLiteExecutor encryptLiteExecutor;
    private final EncryptLiteProperties properties;

    /**
     * 校验接口：检查 JSON 配置的所有表/字段是否符合加密条件。
     * CLOB字段跳过长度校验，VARCHAR2字段校验长度是否足够容纳V1密文。
     *
     * @return 字段级校验结果
     */
    @PostMapping("/verify")
    public Result<EncryptLiteSummaryResult> verify() {
        requireEnabled();
        return Result.success(encryptLiteExecutor.verify());
    }

    /**
     * 预检模式：校验配置、元数据和待处理行数，不执行任何数据修改。
     *
     * @return 预检汇总结果
     */
    @PostMapping("/check")
    public Result<EncryptLiteSummaryResult> check() {
        requireEnabled();
        return Result.success(encryptLiteExecutor.check());
    }

    /**
     * 正式执行模式：校验确认码后执行批量加密。
     *
     * @param request 请求体，必须包含 confirmCode
     * @return 执行汇总结果
     */
    @PostMapping("/execute")
    public Result<EncryptLiteSummaryResult> execute(@RequestBody EncryptLiteRequest request) {
        requireEnabled();
        if (request == null || !CONFIRM_CODE.equals(request.getConfirmCode())) {
            throw new EncryptLiteException(EncryptLiteErrorCode.ENCRYPT_CONFIG_INVALID, "确认码不正确");
        }
        log.warn("DATA_INIT_ENCRYPT EXECUTE triggered by confirmCode");
        return Result.success(encryptLiteExecutor.execute());
    }

    /**
     * 校验轻量加密组件是否已启用，未启用则抛出服务不可用异常。
     */
    private void requireEnabled() {
        if (!properties.isEnabled()) {
            throw new EncryptLiteException(EncryptLiteErrorCode.ENCRYPT_SERVICE_UNAVAILABLE,
                    "加密初始化组件未启用，请配置 encrypt-lite.enabled=true 并提供 config-path");
        }
    }
}
