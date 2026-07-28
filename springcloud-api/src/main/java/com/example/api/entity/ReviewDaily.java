package com.example.api.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ReviewDaily implements Serializable {
    /** 主键 */
    private Long id;
    /** 复习日期 */
    private LocalDate reviewDate;
    /** 当日标题 */
    private String title;
    /** 模块数 */
    private Integer moduleCount;
    /** 题目总数 */
    private Integer itemCount;
    /** 0=草稿 1=已发布 */
    private Integer status;
    /** 生成时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}