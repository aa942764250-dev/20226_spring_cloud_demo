package com.example.api.entity;

import lombok.Data;
import java.io.Serializable;

@Data
public class AllianceThanks implements Serializable {
    private Long id;
    private String unitName;
    private Integer sortOrder;
}
