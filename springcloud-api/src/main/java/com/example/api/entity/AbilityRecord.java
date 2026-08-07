package com.example.api.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 能力评分记录表（按维度记录，支持趋势分析）
 */
@Data
public class AbilityRecord implements Serializable {
    /** 主键 */
    private Long id;
    /** 学生ID */
    private Long studentId;
    /** 评估日期 */
    private LocalDate assessDate;
    /** 能力维度：listening/speaking/reading/writing/grammar/vocabulary */
    private String dimension;
    /** 评分 1-5 */
    private Integer score;
    /** 评分来源：manual/ai_calculated */
    private String source;
    /** 创建时间 */
    private LocalDateTime createdAt;
}