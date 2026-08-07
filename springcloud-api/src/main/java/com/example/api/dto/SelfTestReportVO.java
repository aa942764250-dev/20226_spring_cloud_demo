package com.example.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SelfTestReportVO implements Serializable {
    private Long dailyId;
    private Integer totalCount;
    private Integer correctCount;
    private Integer wrongCount;
    private Integer skipCount;
    private BigDecimal score;
    private BigDecimal correctRate;
    private BigDecimal coverageRate;
    private LocalDateTime createdAt;
}