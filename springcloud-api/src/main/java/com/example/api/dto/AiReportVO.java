package com.example.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * AI报告VO
 */
@Data
public class AiReportVO implements Serializable {
    private Long id;
    private Long studentId;
    private String studentName;
    private String reportType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String title;
    private String summary;
    private String abilityAnalysis;
    private String problemDiagnosis;
    private String teachingSuggestion;
    private String fullContent;
    private Integer status;
    private Integer version;
    private String modelName;
    private Integer tokenUsage;
    private String reviewNote;
    private LocalDateTime createdAt;
}