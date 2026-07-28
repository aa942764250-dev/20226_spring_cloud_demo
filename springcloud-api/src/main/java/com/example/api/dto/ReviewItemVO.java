package com.example.api.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class ReviewItemVO implements Serializable {
    private Long id;
    private String moduleName;
    private String question;
    private String answer;
    private Integer sortOrder;
    private String source;
    /** 0=未看 1=已掌握 2=未掌握 */
    private Integer progressStatus;
}