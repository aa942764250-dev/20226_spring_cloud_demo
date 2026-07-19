package com.example.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.api.dto.EncryptFieldConfigDTO;
import com.example.api.dto.EncryptTableConfigDTO;
import com.example.api.entity.EncryptFieldConfig;
import com.example.api.entity.EncryptTableConfig;
import com.example.api.entity.EncryptTask;
import com.example.common.encrypt.EncryptErrorCode;
import com.example.common.encrypt.EncryptException;
import com.example.service.dao.EncryptFieldConfigDao;
import com.example.service.dao.EncryptTableConfigDao;
import com.example.service.dao.EncryptTaskDao;
import com.example.service.service.EncryptConfigService;
import com.example.service.service.EncryptConfigValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 加密配置管理服务实现。
 * <p>
 * 提供加密配置的增删改查能力，创建和更新时调用 {@link EncryptConfigValidator} 校验配置合法性，
 * 更新和删除时校验无执行中的任务使用该配置。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class EncryptConfigServiceImpl implements EncryptConfigService {

    private final EncryptTableConfigDao tableConfigDao;
    private final EncryptFieldConfigDao fieldConfigDao;
    private final EncryptTaskDao taskDao;
    private final EncryptConfigValidator configValidator;

    /**
     * {@inheritDoc}
     * <p>
     * 校验配置 → 保存表配置 → 保存字段配置列表 → 返回配置ID。
     * </p>
     */
    @Override
    @Transactional
    public Long createConfig(EncryptTableConfigDTO config) {
        configValidator.validate(config);

        EncryptTableConfig tableConfig = new EncryptTableConfig();
        tableConfig.setTableName(config.getTableName());
        tableConfig.setBatchSize(config.getBatchSize());
        tableConfig.setBatchIntervalMs(config.getBatchIntervalMs());
        tableConfig.setEnabled(config.getEnabled() != null ? config.getEnabled() : true);
        tableConfig.setCreateTime(LocalDateTime.now());
        tableConfig.setUpdateTime(LocalDateTime.now());
        tableConfigDao.insert(tableConfig);

        saveFieldConfigs(tableConfig.getId(), config.getFields());
        return tableConfig.getId();
    }

    /**
     * {@inheritDoc}
     * <p>
     * 校验配置存在 → 校验无执行中任务 → 校验新配置合法性 → 删除旧字段配置 → 保存新字段配置 → 更新表配置。
     * </p>
     */
    @Override
    @Transactional
    public void updateConfig(Long id, EncryptTableConfigDTO config) {
        EncryptTableConfig existing = tableConfigDao.selectById(id);
        if (existing == null) {
            throw new EncryptException(EncryptErrorCode.ENCRYPT_CONFIG_INVALID, "配置不存在: " + id);
        }

        checkNoRunningTask(existing.getTableName());

        config.setTableName(existing.getTableName());
        configValidator.validate(config);

        existing.setBatchSize(config.getBatchSize());
        existing.setBatchIntervalMs(config.getBatchIntervalMs());
        existing.setEnabled(config.getEnabled());
        existing.setUpdateTime(LocalDateTime.now());
        tableConfigDao.updateById(existing);

        fieldConfigDao.delete(new LambdaQueryWrapper<EncryptFieldConfig>()
                .eq(EncryptFieldConfig::getTableConfigId, id));
        saveFieldConfigs(id, config.getFields());
    }

    /**
     * {@inheritDoc}
     * <p>
     * 校验配置存在 → 校验无执行中任务 → 级联删除字段配置 → 删除表配置。
     * </p>
     */
    @Override
    @Transactional
    public void deleteConfig(Long id) {
        EncryptTableConfig existing = tableConfigDao.selectById(id);
        if (existing == null) {
            throw new EncryptException(EncryptErrorCode.ENCRYPT_CONFIG_INVALID, "配置不存在: " + id);
        }

        checkNoRunningTask(existing.getTableName());

        fieldConfigDao.delete(new LambdaQueryWrapper<EncryptFieldConfig>()
                .eq(EncryptFieldConfig::getTableConfigId, id));
        tableConfigDao.deleteById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<EncryptTableConfigDTO> listConfigs() {
        List<EncryptTableConfig> tableConfigs = tableConfigDao.selectList(null);
        List<EncryptTableConfigDTO> result = new ArrayList<>();
        for (EncryptTableConfig tc : tableConfigs) {
            result.add(convertToDTO(tc));
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EncryptTableConfigDTO getConfigByTableName(String tableName) {
        EncryptTableConfig tc = tableConfigDao.selectOne(
                new LambdaQueryWrapper<EncryptTableConfig>()
                        .eq(EncryptTableConfig::getTableName, tableName));
        return tc != null ? convertToDTO(tc) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<EncryptTableConfigDTO> listEnabledConfigs() {
        List<EncryptTableConfig> tableConfigs = tableConfigDao.selectList(
                new LambdaQueryWrapper<EncryptTableConfig>()
                        .eq(EncryptTableConfig::getEnabled, true));
        List<EncryptTableConfigDTO> result = new ArrayList<>();
        for (EncryptTableConfig tc : tableConfigs) {
            result.add(convertToDTO(tc));
        }
        return result;
    }

    /**
     * 保存字段配置列表。
     *
     * @param tableConfigId 关联的表配置ID
     * @param fields        字段配置DTO列表
     */
    private void saveFieldConfigs(Long tableConfigId, List<EncryptFieldConfigDTO> fields) {
        if (fields == null) {
            return;
        }
        for (int i = 0; i < fields.size(); i++) {
            EncryptFieldConfigDTO dto = fields.get(i);
            EncryptFieldConfig entity = new EncryptFieldConfig();
            entity.setTableConfigId(tableConfigId);
            entity.setFieldName(dto.getFieldName());
            entity.setAlgorithmId(dto.getAlgorithmId());
            entity.setFieldOrder(dto.getFieldOrder() != null ? dto.getFieldOrder() : i + 1);
            entity.setSkipEncrypted(dto.getSkipEncrypted() != null ? dto.getSkipEncrypted() : true);
            entity.setCreateTime(LocalDateTime.now());
            entity.setUpdateTime(LocalDateTime.now());
            fieldConfigDao.insert(entity);
        }
    }

    /**
     * 校验指定表名无执行中的加密任务。
     *
     * @param tableName 表名
     * @throws EncryptException 存在执行中的任务时抛出 ENCRYPT_TASK_RUNNING
     */
    private void checkNoRunningTask(String tableName) {
        Long runningCount = taskDao.selectCount(
                new LambdaQueryWrapper<EncryptTask>()
                        .eq(EncryptTask::getTargetTableName, tableName)
                        .eq(EncryptTask::getTaskStatus, "RUNNING"));
        if (runningCount != null && runningCount > 0) {
            throw new EncryptException(EncryptErrorCode.ENCRYPT_TASK_RUNNING,
                    "表 " + tableName + " 有执行中的加密任务");
        }
    }

    /**
     * 将表配置实体转换为DTO（含关联的字段配置）。
     *
     * @param tc 表配置实体
     * @return 表配置DTO
     */
    private EncryptTableConfigDTO convertToDTO(EncryptTableConfig tc) {
        EncryptTableConfigDTO dto = new EncryptTableConfigDTO();
        dto.setId(tc.getId());
        dto.setTableName(tc.getTableName());
        dto.setBatchSize(tc.getBatchSize());
        dto.setBatchIntervalMs(tc.getBatchIntervalMs());
        dto.setEnabled(tc.getEnabled());

        List<EncryptFieldConfig> fieldConfigs = fieldConfigDao.selectList(
                new LambdaQueryWrapper<EncryptFieldConfig>()
                        .eq(EncryptFieldConfig::getTableConfigId, tc.getId())
                        .orderByAsc(EncryptFieldConfig::getFieldOrder));
        dto.setFields(fieldConfigs.stream().map(this::convertFieldToDTO).collect(Collectors.toList()));
        return dto;
    }

    /**
     * 将字段配置实体转换为DTO。
     *
     * @param fc 字段配置实体
     * @return 字段配置DTO
     */
    private EncryptFieldConfigDTO convertFieldToDTO(EncryptFieldConfig fc) {
        EncryptFieldConfigDTO dto = new EncryptFieldConfigDTO();
        dto.setId(fc.getId());
        dto.setFieldName(fc.getFieldName());
        dto.setAlgorithmId(fc.getAlgorithmId());
        dto.setFieldOrder(fc.getFieldOrder());
        dto.setSkipEncrypted(fc.getSkipEncrypted());
        return dto;
    }
}
