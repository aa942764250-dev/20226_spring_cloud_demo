package com.example.service.encryptlite.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 加密失败详情DTO。
 */
@Data
public class EncryptLiteErrorDetail implements Serializable {

    /** 失败记录的主键值 */
    private String primaryKeyValue;

    /** 失败字段名 */
    private String fieldName;

    /** 错误码 */
    private String errorCode;

    /** 异常信息 */
    private String errorMessage;
}