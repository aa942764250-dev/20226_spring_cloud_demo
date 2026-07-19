package com.example.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 加密表配置传输对象。
 * <p>
 * 用于创建/更新/查询加密配置时的请求和响应载体，
 * 包含表级配置和关联的字段加密配置列表。
 * </p>
 */
@Data
public class EncryptTableConfigDTO implements Serializable {

    /** 配置ID（创建时为空，更新/查询时有值） */
    private Long id;

    /** 目标表名 */
    private String tableName;

    /** 批次大小，为空时使用全局默认值 */
    private Integer batchSize;

    /** 批次间隔（毫秒），为空时使用全局默认值 */
    private Integer batchIntervalMs;

    /** 是否启用 */
    private Boolean enabled;

    /** 该表下待加密的字段配置列表 */
    private List<EncryptFieldConfigDTO> fields;
}
