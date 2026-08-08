package com.example.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 专属教案生成结果（深圳本地化、确定性合成，无需 LLM 即可返回）
 */
@Data
public class LessonPlanResult implements Serializable {
    /** 学生姓名 */
    private String studentName;
    /** 年级（原始值） */
    private String grade;
    /** 深圳机考/笔试语境（如：深圳中考听说第三节：思维导图复述(12分)） */
    private String examContext;
    /** 使用的印象标签 */
    private List<String> impressions;
    /** 教案段落 */
    private List<LessonPlanSection> sections;
}
