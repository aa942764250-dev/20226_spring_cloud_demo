package com.example.api.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 教师账号表（MVP一期预留，暂不启用鉴权，默认单教师模式）
 */
@Data
public class Teacher implements Serializable {
    /** 主键 */
    private Long id;
    /** 教师姓名 */
    private String name;
    /** 手机号 */
    private String phone;
    /** 所属机构（预留） */
    private String institution;
    /** 状态 0=禁用 1=启用 */
    private Integer status;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}