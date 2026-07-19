package com.example.service.handler;

import com.example.common.encrypt.EncryptException;
import com.example.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 * <p>
 * 统一拦截控制器层抛出的异常，转换为标准的 {@link Result} 响应格式。
 * 处理 {@link EncryptException} 加密业务异常和通用 {@link Exception} 兜底异常。
 * </p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理加密业务异常。
     * <p>
     * 将 {@link EncryptException} 中的错误码和消息转换为 Result.fail 响应。
     * </p>
     *
     * @param e 加密业务异常
     * @return 包含错误码和消息的失败响应
     */
    @ExceptionHandler(EncryptException.class)
    public Result<Void> handleEncryptException(EncryptException e) {
        log.warn("加密业务异常: code={}, message={}", e.getErrorCode().getCode(), e.getMessage());
        return Result.fail(e.getErrorCode().getCode(), e.getMessage());
    }

    /**
     * 兜底处理所有未捕获的异常。
     *
     * @param e 未捕获的异常
     * @return 通用系统内部错误响应
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统内部错误", e);
        return Result.fail(500, "系统内部错误");
    }
}
