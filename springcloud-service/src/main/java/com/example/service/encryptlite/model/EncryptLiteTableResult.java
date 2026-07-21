package com.example.service.encryptlite.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 单表加密结果DTO。
 */
@Data
public class EncryptLiteTableResult implements Serializable {

    /** 表名 */
    private String tableName;

    /** 成功加密记录数 */
    private long successCount;

    /** 失败记录数 */
    private long failedCount;

    /** 跳过记录数（已加密或NULL） */
    private long skippedCount;

    /** 失败详情列表 */
    private List<EncryptLiteErrorDetail> errorDetails;

    /** 校验不通过的原因描述 */
    private String errorMessage;
}