package com.example.api.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Prompt模板表
 */
@Data
public class PromptTemplate implements Serializable {
    /** 主键 */
    private Long id;
    /** 模板名称 */
    private String name;
    /** 模板类型：weekly_report/monthly_report/ability_analysis */
    private String type;
    /** System Prompt */
    private String systemPrompt;
    /** 用户Prompt模板（含变量占位符如{{student_name}}） */
    private String userPromptTemplate;
    /** 输出格式说明（JSON Schema描述） */
    private String outputFormat;
    /** 模型名称（如gemini-2.5-flash） */
    private String modelName;
    /** 温度参数 */
    private Double temperature;
    /** 最大输出Token */
    private Integer maxTokens;
    /** 是否启用 0=否 1=是 */
    private Integer enabled;
    /** 版本号 */
    private Integer version;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}