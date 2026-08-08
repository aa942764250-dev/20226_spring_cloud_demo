package com.example.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 学生详情VO（含能力画像和最近学习记录）
 */
@Data
public class StudentDetailVO implements Serializable {
    private Long id;
    private String name;
    private String englishName;
    private String grade;
    private String level;
    private String goal;
    private String phone;
    private String remark;
    /** 深圳教研印象标签（JSON 数组字符串） */
    private String impressions;
    private Integer status;
    private LocalDate enrollDate;
    /** 能力画像：{listening:4.2, speaking:3.8, ...} */
    private Map<String, Double> abilityProfile;
    /** 近30天能力趋势：{维度: [30天每日平均评分]} —— 对齐前端趋势图 */
    private Map<String, List<Double>> abilityTrend;
    /** 最近学习记录 */
    private List<LearningRecordVO> recentRecords;
    /** 最近AI报告 */
    private List<AiReportVO> recentReports;
    /** 总学习次数 */
    private Integer totalLessons;
    /** 入学天数 */
    private Integer enrolledDays;
}