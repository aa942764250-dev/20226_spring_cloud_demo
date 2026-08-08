package com.example.api.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学生信息表
 */
@Data
public class Student implements Serializable {
    /** 主键 */
    private Long id;
    /** 教师ID（预留多账号，MVP写死1） */
    private Long teacherId;
    /** 学生姓名 */
    private String name;
    /** 英文名 */
    private String englishName;
    /** 年级：如3年级、初一 */
    private String grade;
    /** 英语等级：如KET/PET/剑少2 */
    private String level;
    /** 学习目标 */
    private String goal;
    /** 联系电话 */
    private String phone;
    /** 备注 */
    private String remark;
    /** 深圳教研印象标签（JSON 数组字符串，如 ["导图复述要点遗漏","定语从句用法卡顿"]） */
    private String impressions;
    /** 状态 0=停课 1=在课 */
    private Integer status;
    /** 入学日期 */
    private LocalDate enrollDate;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}