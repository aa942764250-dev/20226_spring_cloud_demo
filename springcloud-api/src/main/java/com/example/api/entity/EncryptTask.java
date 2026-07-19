package com.example.api.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 加密任务实体。
 * <p>
 * 对应数据库表 encrypt_task，记录每次加密初始化任务的执行状态和进度信息，
 * 同时承载断点续执行所需的中断位置信息。
 * 任务状态流转：PENDING → RUNNING → SUCCESS/PARTIAL_SUCCESS/FAILED/CANCELLED
 * </p>
 */
@Data
public class EncryptTask implements Serializable {

    /** 任务ID */
    private Long id;

    /** 任务状态：PENDING/RUNNING/SUCCESS/PARTIAL_SUCCESS/FAILED/CANCELLED */
    private String taskStatus;

    /** 触发时间 */
    private LocalDateTime triggerTime;

    /** 完成时间 */
    private LocalDateTime finishTime;

    /** 触发人 */
    private String triggerUser;

    /** 目标表名 */
    private String targetTableName;

    /** 目标字段列表（JSON格式，含字段名和算法标识） */
    private String targetFieldList;

    /** 总记录数 */
    private Long totalRecordCount;

    /** 已处理记录数 */
    private Long processedRecordCount;

    /** 失败记录数 */
    private Long failedRecordCount;

    /** 当前批次号（用于断点续执行定位） */
    private Integer currentBatchNo;

    /** 当前加密字段名（用于断点续执行定位） */
    private String currentFieldName;

    /** 已完成字段列表（逗号分隔，用于断点续执行跳过已完成字段） */
    private String completedFieldList;

    /** 异常信息摘要 */
    private String errorMessage;
}
