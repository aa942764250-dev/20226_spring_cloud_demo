package com.example.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 加密任务传输对象。
 * <p>
 * 用于返回加密任务的基本信息和状态，不包含进度详情（进度通过 EncryptProgressDTO 查询）。
 * </p>
 */
@Data
public class EncryptTaskDTO implements Serializable {

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

    /** 目标字段列表（JSON格式） */
    private String targetFieldList;

    /** 总记录数 */
    private Long totalRecordCount;

    /** 已处理记录数 */
    private Long processedRecordCount;

    /** 失败记录数 */
    private Long failedRecordCount;

    /** 当前批次号 */
    private Integer currentBatchNo;

    /** 当前加密字段名 */
    private String currentFieldName;
}
