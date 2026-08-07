package com.example.workbench.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MenuVO {
    private Long id;
    private Long parentId;
    private String name;
    private String path;
    private String icon;
    private Integer type;
    private Integer sortOrder;
    private String sectionName;
}
