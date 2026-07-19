package com.example.service.service;

import com.example.api.dto.EncryptTableConfigDTO;

import java.util.List;

/**
 * 加密配置管理服务接口。
 * <p>
 * 提供加密配置的增删改查能力，支持表+多字段的灵活配置组合。
 * </p>
 */
public interface EncryptConfigService {

    /**
     * 创建加密配置。
     *
     * @param config 加密配置（含表名和字段列表）
     * @return 新创建的配置ID
     */
    Long createConfig(EncryptTableConfigDTO config);

    /**
     * 更新加密配置。
     * <p>
     * 更新前会校验无执行中的任务使用该配置，校验通过后先删除旧字段配置再保存新的。
     * </p>
     *
     * @param id     配置ID
     * @param config 新的加密配置
     */
    void updateConfig(Long id, EncryptTableConfigDTO config);

    /**
     * 删除加密配置。
     * <p>
     * 级联删除关联的字段配置，删除前校验无执行中的任务使用该配置。
     * </p>
     *
     * @param id 配置ID
     */
    void deleteConfig(Long id);

    /**
     * 查询所有加密配置。
     *
     * @return 加密配置列表（含字段配置）
     */
    List<EncryptTableConfigDTO> listConfigs();

    /**
     * 根据表名查询加密配置。
     *
     * @param tableName 表名
     * @return 加密配置，不存在时返回 null
     */
    EncryptTableConfigDTO getConfigByTableName(String tableName);

    /**
     * 查询所有已启用的加密配置。
     *
     * @return 已启用的加密配置列表
     */
    List<EncryptTableConfigDTO> listEnabledConfigs();
}
