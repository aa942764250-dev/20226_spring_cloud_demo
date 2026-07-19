package com.example.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 加密进度传输对象。
 * <p>
 * 用于返回加密任务的详细进度信息，包括当前处理位置、完成百分比和预估剩余时间。
 * </p>
 */
@Data
public class EncryptProgressDTO implements Serializable {

    /** 任务ID */
    private Long taskId;

    /** 当前处理的表名 */
    private String currentTableName;

    /** 当前加密的字段名 */
    private String currentFieldName;

    /** 字段加密执行顺序列表 */
    private List<String> fieldOrderList;

    /** 已完成加密的字段列表 */
    private List<String> completedFieldList;

    /** 已处理记录数 */
    private Long processedRecordCount;

    /** 总记录数 */
    private Long totalRecordCount;

    /** 完成百分比（0.00~100.00） */
    private BigDecimal completionPercentage;

    /** 预估剩余时间（秒） */
    private Long estimatedRemainingSeconds;

    /** 最后更新时间 */
    private LocalDateTime lastUpdateTime;
}
