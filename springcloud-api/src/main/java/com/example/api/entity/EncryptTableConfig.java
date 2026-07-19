package com.example.api.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 加密表配置实体。
 * <p>
 * 对应数据库表 encrypt_table_config，以表为维度配置加密参数，
 * 一条记录对应一张待加密的业务表。
 * </p>
 */
@Data
public class EncryptTableConfig implements Serializable {

    /** 主键ID */
    private Long id;

    /** 目标表名 */
    private String tableName;

    /** 批次大小，范围 [100, 10000]，为空时使用全局默认值 */
    private Integer batchSize;

    /** 批次间隔（毫秒），范围 [0, 5000]，为空时使用全局默认值 */
    private Integer batchIntervalMs;

    /** 是否启用（true=启用，false=禁用） */
    private Boolean enabled;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
