package com.example.api.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 字段加密配置实体。
 * <p>
 * 对应数据库表 encrypt_field_config，以字段为维度配置加密参数，
 * 通过 tableConfigId 关联到 {@link EncryptTableConfig}，
 * 每个字段可独立配置加密算法和执行顺序。
 * </p>
 */
@Data
public class EncryptFieldConfig implements Serializable {

    /** 主键ID */
    private Long id;

    /** 关联的表配置ID */
    private Long tableConfigId;

    /** 字段名 */
    private String fieldName;

    /** 加密算法标识（如 "SM4"、"AES"），为空时使用全局默认算法 */
    private String algorithmId;

    /** 字段加密执行顺序，数值越小越先执行 */
    private Integer fieldOrder;

    /** 是否跳过已加密数据（true=跳过，false=不跳过） */
    private Boolean skipEncrypted;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
