package com.example.api.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ReviewProgress implements Serializable {
    /** 主键 */
    private Long id;
    /** 关联 review_item.id */
    private Long itemId;
    /** 用户标识 */
    private String userId;
    /** 0=未看 1=已掌握 2=未掌握 */
    private Integer status;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}