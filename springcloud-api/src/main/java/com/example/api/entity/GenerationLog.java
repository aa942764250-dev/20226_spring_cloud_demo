package com.example.api.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class GenerationLog implements Serializable {
    private Long id;
    private LocalDate targetDate;
    private String type;
    private String status;
    private Integer reviewItemCount;
    private Integer testItemCount;
    private Integer moduleCount;
    private String errorMessage;
    private Long durationMs;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}