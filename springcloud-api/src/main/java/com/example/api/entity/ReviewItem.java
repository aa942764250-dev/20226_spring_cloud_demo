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
    /** 题目类型：review=复习重点 fill_blank=填空 true_false=判断 choice=选择 */
    private String questionType;
    /** 选择题选项JSON数组 */
    private String options;
    /** 正确答案 */
    private String correctAnswer;
    /** 创建时间 */
    private LocalDateTime createdAt;
}