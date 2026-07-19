package com.example.common.encrypt;

import lombok.Getter;

/**
 * 加密业务异常。
 * <p>
 * 封装加密初始化组件中所有业务异常，包含 {@link EncryptErrorCode} 错误码和详细信息。
 * 由 {@link com.example.service.handler.GlobalExceptionHandler} 统一拦截并转换为标准响应。
 * </p>
 */
@Getter
public class EncryptException extends RuntimeException {

    /** 错误码枚举 */
    private final EncryptErrorCode errorCode;

    /**
     * 仅使用错误码构造异常。
     *
     * @param errorCode 错误码枚举
     */
    public EncryptException(EncryptErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 使用错误码和详细信息构造异常。
     *
     * @param errorCode 错误码枚举
     * @param detail    补充详情，将追加在默认描述之后
     */
    public EncryptException(EncryptErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + ": " + detail);
        this.errorCode = errorCode;
    }

    /**
     * 使用错误码和原始异常构造异常。
     *
     * @param errorCode 错误码枚举
     * @param cause     原始异常
     */
    public EncryptException(EncryptErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    /**
     * 使用错误码、详细信息和原始异常构造异常。
     *
     * @param errorCode 错误码枚举
     * @param detail    补充详情
     * @param cause     原始异常
     */
    public EncryptException(EncryptErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode.getMessage() + ": " + detail, cause);
        this.errorCode = errorCode;
    }
}
