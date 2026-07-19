package com.example.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 字段加密配置传输对象。
 * <p>
 * 用于配置单个字段的加密参数，每个字段可独立指定加密算法和执行顺序。
 * </p>
 */
@Data
public class EncryptFieldConfigDTO implements Serializable {

    /** 字段配置ID（创建时为空，更新/查询时有值） */
    private Long id;

    /** 字段名 */
    private String fieldName;

    /** 加密算法标识，为空时使用全局默认算法 */
    private String algorithmId;

    /** 字段加密执行顺序，数值越小越先执行 */
    private Integer fieldOrder;

    /** 是否跳过已加密数据 */
    private Boolean skipEncrypted;
}
