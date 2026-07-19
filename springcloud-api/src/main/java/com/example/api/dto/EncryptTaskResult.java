package com.example.api.dto;

import com.example.api.entity.EncryptTaskError;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 加密任务执行结果对象。
 * <p>
 * 用于封装加密任务执行完成后的结果信息，包括成功/失败计数和失败记录详情。
 * </p>
 */
@Data
public class EncryptTaskResult implements Serializable {

    /** 任务ID */
    private Long taskId;

    /** 任务最终状态 */
    private String taskStatus;

    /** 成功加密的记录数 */
    private Long successCount;

    /** 失败的记录数 */
    private Long failedCount;

    /** 失败记录详情列表 */
    private List<EncryptTaskError> errors;
}
