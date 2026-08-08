package com.example.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 专属教案生成请求：基于学生「深圳教研印象标签」生成本地化训练方案
 */
@Data
public class LessonPlanRequest implements Serializable {
    /** 学生ID（可选；提供则从学生档案补全姓名/年级） */
    private Long studentId;
    /** 学生姓名（studentId 缺失时作为回退，用于展示与年级推断） */
    private String studentName;
    /** 学生年级（studentId 缺失时作为回退，如：初二/初三/高一） */
    private String grade;
    /** 深圳教研印象标签（如：导图复述要点遗漏、定语从句用法卡顿） */
    private List<String> impressions;
    /** 教师补充要求（可选） */
    private String note;
}
