package com.example.api.entity;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SelfTestReport implements Serializable {
    private Long id;
    private Long dailyId;
    private String userId;
    private Integer totalCount;
    private Integer correctCount;
    private Integer wrongCount;
    private Integer skipCount;
    private BigDecimal score;
    private BigDecimal correctRate;
    private BigDecimal coverageRate;
    private LocalDateTime createdAt;
}