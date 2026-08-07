package com.example.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 学习记录VO
 */
@Data
public class LearningRecordVO implements Serializable {
    private Long id;
    private Long studentId;
    private String studentName;
    private LocalDate lessonDate;
    private String courseType;
    private String topic;
    private String knowledgePoints;
    private Integer listeningScore;
    private Integer speakingScore;
    private Integer readingScore;
    private Integer writingScore;
    private Integer grammarScore;
    private Integer vocabularyScore;
    private String performance;
    private String problemTags;
    private String teacherNote;
    private String homeworkStatus;
}