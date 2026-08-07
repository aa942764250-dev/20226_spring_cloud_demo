package com.example.api.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class WrongAnswerBook implements Serializable {
    private Long id;
    private String userId;
    private String question;
    private String correctAnswer;
    private String userAnswer;
    private String moduleName;
    private String source;
    private Integer wrongCount;
    private LocalDateTime lastWrongAt;
    private LocalDateTime createdAt;
}