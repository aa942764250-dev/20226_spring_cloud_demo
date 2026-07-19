package com.example.service.encryptlite.config;

import com.example.service.encryptlite.exception.EncryptLiteErrorCode;
import com.example.service.encryptlite.exception.EncryptLiteException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * 轻量加密组件YAML配置属性。
 * <p>
 * 绑定 encrypt-lite 前缀，配置格式示例：
 * <pre>
 * encrypt-lite:
 *   default-batch-size: 1000
 *   secret-key: "your-aes-secret-key"
 *   tables:
 *     table_a:
 *       fields:
 *         - field1
 *         - field2
 *     table_b:
 *       fields:
 *         - field3
 * </pre>
 * </p>
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "encrypt-lite")
public class EncryptLiteProperties {

    /** 默认批次大小，范围 [100, 10000] */
    private Integer defaultBatchSize = 1000;

    /** AES加密密钥，未配置时使用默认密钥 */
    private String secretKey;

    /** 表名→表配置映射 */
    private Map<String, TableConfig> tables;

    /**
     * 启动后校验配置合法性。
     */
    @PostConstruct
    public void validate() {
        if (defaultBatchSize < 100 || defaultBatchSize > 10000) {
            throw new EncryptLiteException(EncryptLiteErrorCode.ENCRYPT_CONFIG_INVALID,
                    "default-batch-size 范围 [100, 10000], 当前: " + defaultBatchSize);
        }
        if (tables == null || tables.isEmpty()) {
            log.warn("encrypt-lite.tables 未配置，加密初始化接口将无法使用");
        } else {
            for (Map.Entry<String, TableConfig> entry : tables.entrySet()) {
                if (entry.getValue() == null || entry.getValue().getFields() == null || entry.getValue().getFields().isEmpty()) {
                    log.warn("表 {} 的 fields 配置为空，将被跳过", entry.getKey());
                }
            }
        }
        if (secretKey == null || secretKey.isEmpty()) {
            log.warn("encrypt-lite.secret-key 未配置，将使用默认密钥（不安全，生产环境请配置）");
        }
    }

    /**
     * 单表配置。
     */
    @Data
    public static class TableConfig {

        /** 加密字段列表 */
        private java.util.List<String> fields;
    }
}