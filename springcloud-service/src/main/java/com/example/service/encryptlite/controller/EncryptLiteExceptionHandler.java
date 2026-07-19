package com.example.service.encryptlite.controller;

import com.example.common.result.Result;
import com.example.service.encryptlite.exception.EncryptLiteException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 轻量加密组件全局异常处理器。
 */
@Slf4j
@RestControllerAdvice
public class EncryptLiteExceptionHandler {

    /**
     * 处理加密业务异常。
     */
    @ExceptionHandler(EncryptLiteException.class)
    public Result<Void> handleEncryptLiteException(EncryptLiteException e) {
        log.warn("加密业务异常: code={}, message={}", e.getErrorCode().getCode(), e.getMessage());
        return Result.fail(e.getErrorCode().getCode(), e.getMessage());
    }

    /**
     * 兜底处理未捕获异常。
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统内部错误", e);
        return Result.fail(500, "系统内部错误");
    }
}