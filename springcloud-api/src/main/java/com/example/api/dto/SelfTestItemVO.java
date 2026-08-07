package com.example.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class SelfTestItemVO implements Serializable {
    private Long id;
    private String moduleName;
    private String question;
    private String questionType;
    private List<String> options;
    private Integer sortOrder;
    private String source;
    private String userAnswer;
    private Integer isCorrect;
}