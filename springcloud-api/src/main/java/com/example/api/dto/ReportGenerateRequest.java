package com.example.api.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * AI报告生成请求
 */
@Data
public class ReportGenerateRequest implements Serializable {
    /** 学生ID */
    private Long studentId;
    /** 报告类型：weekly/monthly */
    private String reportType;
    /** 自定义起始日期（可选，默认自动计算） */
    private String startDate;
    /** 自定义结束日期（可选） */
    private String endDate;
}