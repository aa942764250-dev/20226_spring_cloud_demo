package com.example.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SelfTestDailyVO implements Serializable {
    private Long dailyId;
    private LocalDate reviewDate;
    private String title;
    private Integer totalTestCount;
    private Integer answeredCount;
    private Integer correctCount;
    private BigDecimal correctRate;
    private BigDecimal coverageRate;
    private BigDecimal score;
    private List<SelfTestItemVO> items;
}