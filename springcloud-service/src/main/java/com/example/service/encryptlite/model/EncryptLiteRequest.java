package com.example.service.encryptlite.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 加密初始化请求DTO。
 */
@Data
public class EncryptLiteRequest implements Serializable {

    /** EXECUTE 模式必填，固定值 ENCRYPT_EXISTING_DATA */
    private String confirmCode;
}
