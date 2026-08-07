package com.example.workbench.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wb_menu")
public class WbMenu {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;

    private String menuName;

    private String menuPath;

    private String menuIcon;

    private Integer menuType;

    private Integer sortOrder;

    private String perms;

    private Integer status;

    private String sectionName;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
