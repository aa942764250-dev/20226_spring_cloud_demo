package com.example.api.entity;

import lombok.Data;
import java.io.Serializable;

@Data
public class AllianceMember implements Serializable {
    private Long id;
    private String season;
    private String memberName;
    private String roleType;
    private Integer sortOrder;
}
