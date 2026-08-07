package com.example.api.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * AI报告表
 */
@Data
public class AiReport implements Serializable {
    /** 主键 */
    private Long id;
    /** 教师ID（预留） */
    private Long teacherId;
    /** 学生ID */
    private Long studentId;
    /** 报告类型：weekly/monthly */
    private String reportType;
    /** 报告起始日期 */
    private LocalDate startDate;
    /** 报告结束日期 */
    private LocalDate endDate;
    /** 报告标题 */
    private String title;
    /** 学习总结（Markdown） */
    private String summary;
    /** 能力分析（JSON，含各维度评分和趋势） */
    private String abilityAnalysis;
    /** 问题诊断（Markdown） */
    private String problemDiagnosis;
    /** 教学建议（Markdown） */
    private String teachingSuggestion;
    /** 完整内容（JSON，AI原始输出） */
    private String fullContent;
    /** 状态：0=生成中 1=待审核 2=已发布 3=已驳回 */
    private Integer status;
    /** 版本号（支持修改后重新生成） */
    private Integer version;
    /** 使用的Prompt模板ID */
    private Long promptTemplateId;
    /** AI模型名称 */
    private String modelName;
    /** Token消耗 */
    private Integer tokenUsage;
    /** 审核备注 */
    private String reviewNote;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}