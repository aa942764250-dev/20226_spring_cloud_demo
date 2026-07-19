package com.example.service.service;

import com.example.api.dto.EncryptFieldConfigDTO;
import com.example.api.dto.EncryptTableConfigDTO;
import com.example.common.encrypt.EncryptErrorCode;
import com.example.common.encrypt.EncryptException;
import com.example.service.dao.DynamicTableDao;
import com.example.service.encrypt.EncryptAlgorithmRouter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 加密配置校验器。
 * <p>
 * 在创建或更新加密配置时执行全面的合法性校验，包括：
 * <ul>
 *     <li>表名非空且存在于数据库</li>
 *     <li>字段列表非空且数量不超过20个</li>
 *     <li>字段名存在于表中且为字符串类型</li>
 *     <li>字段不重复、不包含主键字段</li>
 *     <li>加密算法标识已注册</li>
 *     <li>批次大小和间隔在允许范围内</li>
 * </ul>
 * </p>
 */
@Component
@RequiredArgsConstructor
public class EncryptConfigValidator {

    /** 允许加密的字符串类型集合 */
    private static final Set<String> STRING_TYPES = new HashSet<>(Arrays.asList(
            "VARCHAR2", "CHAR", "NVARCHAR2", "NCHAR", "VARCHAR"
    ));

    /** 批次大小下限 */
    private static final int MIN_BATCH_SIZE = 100;

    /** 批次大小上限 */
    private static final int MAX_BATCH_SIZE = 10000;

    /** 单表最大加密字段数 */
    private static final int MAX_FIELD_COUNT = 20;

    /** 批次间隔下限（毫秒） */
    private static final int MIN_BATCH_INTERVAL = 0;

    /** 批次间隔上限（毫秒） */
    private static final int MAX_BATCH_INTERVAL = 5000;

    private final DynamicTableDao dynamicTableDao;
    private final EncryptAlgorithmRouter algorithmRouter;

    /**
     * 校验加密配置的合法性。
     * <p>
     * 校验失败时抛出 {@link EncryptException}，包含具体的错误码和描述信息。
     * </p>
     *
     * @param config 待校验的加密配置
     * @throws EncryptException 校验失败时抛出
     */
    public void validate(EncryptTableConfigDTO config) {
        if (config.getTableName() == null || config.getTableName().trim().isEmpty()) {
            throw new EncryptException(EncryptErrorCode.ENCRYPT_CONFIG_FORMAT_ERROR, "表名不能为空");
        }

        if (!dynamicTableDao.checkTableExists(config.getTableName())) {
            throw new EncryptException(EncryptErrorCode.ENCRYPT_CONFIG_INVALID,
                    "表不存在: " + config.getTableName());
        }

        if (config.getFields() == null || config.getFields().isEmpty()) {
            throw new EncryptException(EncryptErrorCode.ENCRYPT_CONFIG_FORMAT_ERROR, "字段列表不能为空");
        }

        if (config.getFields().size() > MAX_FIELD_COUNT) {
            throw new EncryptException(EncryptErrorCode.ENCRYPT_CONFIG_FORMAT_ERROR,
                    "字段数量不能超过" + MAX_FIELD_COUNT + "个");
        }

        String pkColumn = dynamicTableDao.queryPrimaryKeyColumn(config.getTableName());
        Set<String> fieldNames = new HashSet<>();
        for (EncryptFieldConfigDTO field : config.getFields()) {
            if (field.getFieldName() == null || field.getFieldName().trim().isEmpty()) {
                throw new EncryptException(EncryptErrorCode.ENCRYPT_CONFIG_FORMAT_ERROR, "字段名不能为空");
            }

            if (fieldNames.contains(field.getFieldName())) {
                throw new EncryptException(EncryptErrorCode.ENCRYPT_FIELD_DUPLICATE,
                        "字段重复配置: " + field.getFieldName());
            }
            fieldNames.add(field.getFieldName());

            if (!dynamicTableDao.checkFieldExists(config.getTableName(), field.getFieldName())) {
                throw new EncryptException(EncryptErrorCode.ENCRYPT_CONFIG_INVALID,
                        "字段不存在: " + config.getTableName() + "." + field.getFieldName());
            }

            String fieldType = dynamicTableDao.queryFieldType(config.getTableName(), field.getFieldName());
            if (fieldType != null && !STRING_TYPES.contains(fieldType.toUpperCase())) {
                throw new EncryptException(EncryptErrorCode.ENCRYPT_CONFIG_INVALID,
                        "字段类型非字符串: " + config.getTableName() + "." + field.getFieldName() + "(" + fieldType + ")");
            }

            if (pkColumn != null && pkColumn.equalsIgnoreCase(field.getFieldName())) {
                throw new EncryptException(EncryptErrorCode.ENCRYPT_CONFIG_INVALID,
                        "主键字段不允许加密: " + field.getFieldName());
            }

            if (field.getAlgorithmId() != null && !field.getAlgorithmId().trim().isEmpty()) {
                algorithmRouter.route(field.getAlgorithmId());
            }
        }

        if (config.getBatchSize() != null) {
            if (config.getBatchSize() < MIN_BATCH_SIZE || config.getBatchSize() > MAX_BATCH_SIZE) {
                throw new EncryptException(EncryptErrorCode.ENCRYPT_CONFIG_FORMAT_ERROR,
                        "批次大小范围[" + MIN_BATCH_SIZE + ", " + MAX_BATCH_SIZE + "]");
            }
        }

        if (config.getBatchIntervalMs() != null) {
            if (config.getBatchIntervalMs() < MIN_BATCH_INTERVAL || config.getBatchIntervalMs() > MAX_BATCH_INTERVAL) {
                throw new EncryptException(EncryptErrorCode.ENCRYPT_CONFIG_FORMAT_ERROR,
                        "批次间隔范围[" + MIN_BATCH_INTERVAL + ", " + MAX_BATCH_INTERVAL + "]");
            }
        }
    }
}
