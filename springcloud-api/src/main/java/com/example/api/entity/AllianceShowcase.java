package com.example.api.entity;

import lombok.Data;
import java.io.Serializable;

@Data
public class AllianceShowcase implements Serializable {
    private Long id;
    private String section;       // core_member | hall_of_fame
    private String memberName;    // 成员名称
    private String roleLabel;     // 职务标签：团长/副团长/元老/核心等
    private String imageUrl;      // 头像图片路径
    private String description;   // 描述（名人堂用）
    private Integer sortOrder;    // 排序
    private Integer isActive;     // 1=启用 0=禁用
}
