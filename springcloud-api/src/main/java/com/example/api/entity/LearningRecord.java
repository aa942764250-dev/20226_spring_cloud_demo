package com.example.api.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学习记录表（核心业务表，教师每日填写）
 */
@Data
public class LearningRecord implements Serializable {
    /** 主键 */
    private Long id;
    /** 教师ID（预留） */
    private Long teacherId;
    /** 学生ID */
    private Long studentId;
    /** 上课日期 */
    private LocalDate lessonDate;
    /** 课程类型：1v1/1v2/小班/大班 */
    private String courseType;
    /** 课程主题 */
    private String topic;
    /** 知识点（逗号分隔） */
    private String knowledgePoints;
    /** 听力评分 1-5 */
    private Integer listeningScore;
    /** 口语评分 1-5 */
    private Integer speakingScore;
    /** 阅读评分 1-5 */
    private Integer readingScore;
    /** 写作评分 1-5 */
    private Integer writingScore;
    /** 语法评分 1-5 */
    private Integer grammarScore;
    /** 词汇评分 1-5 */
    private Integer vocabularyScore;
    /** 课堂表现：excellent/good/average/needs_improvement */
    private String performance;
    /** 问题标签（逗号分隔，如：发音不准,词汇量不足,语法混淆） */
    private String problemTags;
    /** 教师备注 */
    private String teacherNote;
    /** 作业完成情况：completed/partial/not_done */
    private String homeworkStatus;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}