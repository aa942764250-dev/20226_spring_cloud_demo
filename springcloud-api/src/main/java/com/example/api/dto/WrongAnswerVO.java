package com.example.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class WrongAnswerVO implements Serializable {
    private Long id;
    private String question;
    private String correctAnswer;
    private String userAnswer;
    private String moduleName;
    private String source;
    private Integer wrongCount;
    private LocalDateTime lastWrongAt;
}