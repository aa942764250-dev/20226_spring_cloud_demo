package com.example.api.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class SelfTestAnswer implements Serializable {
    private Long id;
    private Long itemId;
    private String userId;
    private String userAnswer;
    private Integer isCorrect;
    private LocalDateTime answeredAt;
}