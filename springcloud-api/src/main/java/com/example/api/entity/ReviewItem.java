package com.example.api.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ReviewItem implements Serializable {
    /** 主键 */
    private Long id;
    /** 关联 review_daily.id */
    private Long dailyId;
    /** 模块名 */
    private String moduleName;
    /** 题目 */
    private String question;
    /** 答案（Markdown） */
    private String answer;
    /** 模块内排序 */
    private Integer sortOrder;
    /** 来源文档名 */
    private String source;
    /** 创建时间 */
    private LocalDateTime createdAt;
}