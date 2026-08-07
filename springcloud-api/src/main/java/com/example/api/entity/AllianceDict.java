package com.example.api.entity;

import lombok.Data;
import java.io.Serializable;

@Data
public class AllianceDict implements Serializable {
    private Long id;
    private String dictType;
    private String dictKey;
    private String dictValue;
    private Integer sortOrder;
    private String remark;
}
