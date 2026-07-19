package com.example.service.encryptlite.exception;

import lombok.Getter;

/**
 * 轻量加密组件业务异常。
 */
@Getter
public class EncryptLiteException extends RuntimeException {

    private final EncryptLiteErrorCode errorCode;
    private final String detail;

    public EncryptLiteException(EncryptLiteErrorCode errorCode) {
        super("[" + errorCode.getCode() + "] " + errorCode.getMessage());
        this.errorCode = errorCode;
        this.detail = null;
    }

    public EncryptLiteException(EncryptLiteErrorCode errorCode, String detail) {
        super("[" + errorCode.getCode() + "] " + errorCode.getMessage() + " - " + detail);
        this.errorCode = errorCode;
        this.detail = detail;
    }

    public EncryptLiteException(EncryptLiteErrorCode errorCode, String detail, Throwable cause) {
        super("[" + errorCode.getCode() + "] " + errorCode.getMessage() + " - " + detail, cause);
        this.errorCode = errorCode;
        this.detail = detail;
    }
}