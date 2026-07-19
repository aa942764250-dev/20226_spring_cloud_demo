package com.example.service.encryptlite.exception;

import lombok.Getter;

/**
 * 轻量加密组件错误码枚举。
 */
@Getter
public enum EncryptLiteErrorCode {

    ENCRYPT_TASK_RUNNING(20001, "加密任务正在执行中"),
    ENCRYPT_NO_CONFIG(20002, "未配置加密规则"),
    ENCRYPT_METHOD_ERROR(20003, "加密方法执行失败"),
    ENCRYPT_DB_WRITE_ERROR(20004, "数据库写入失败"),
    ENCRYPT_DB_READ_ERROR(20005, "数据库读取失败"),
    ENCRYPT_SERVICE_UNAVAILABLE(20006, "加密服务不可用"),
    ENCRYPT_CONFIG_INVALID(20007, "配置无效");

    private final int code;
    private final String message;

    EncryptLiteErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}