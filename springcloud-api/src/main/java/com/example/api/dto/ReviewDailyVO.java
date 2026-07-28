package com.example.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReviewDailyVO implements Serializable {
    private Long id;
    private LocalDate reviewDate;
    private String title;
    private Integer moduleCount;
    private Integer itemCount;
    private Integer masteredCount;
    private Integer status;
    private LocalDateTime createdAt;
    private List<ReviewItemVO> items;
}