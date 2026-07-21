package com.example.service.encryptlite.config;

import com.example.service.encryptlite.exception.EncryptLiteErrorCode;
import com.example.service.encryptlite.exception.EncryptLiteException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 轻量加密组件配置属性，表和字段从外部JSON读取。
 * <p>
 * JSON格式支持字段长度配置，length=0表示CLOB等无限类型：
 * <pre>
 * [{"tableName":"T1","fields":[{"name":"COL1","length":128},{"name":"REMARK","length":0}]}]
 * </pre>
 * </p>
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "encrypt-lite")
public class EncryptLiteProperties {

    private static final Pattern IDENTIFIER = Pattern.compile("^[A-Z][A-Z0-9_]{0,29}$");

    private boolean enabled = false;
    private Integer defaultBatchSize = 200;
    private String secretKey;
    private String configPath;
    private Map<String, TableConfig> tables;

    @PostConstruct
    public void validate() {
        if (defaultBatchSize < 1 || defaultBatchSize > 1000) {
            throw new EncryptLiteException(EncryptLiteErrorCode.ENCRYPT_CONFIG_INVALID,
                    "default-batch-size 范围 [1, 1000], 当前: " + defaultBatchSize);
        }
        if (!enabled) return;
        if (configPath == null || configPath.trim().isEmpty()) throw invalid("config-path 未配置");
        try {
            List<TableConfig> configs = new ObjectMapper().readValue(Files.readAllBytes(Paths.get(configPath)),
                    new TypeReference<List<TableConfig>>() {});
            tables = new LinkedHashMap<>();
            for (TableConfig config : configs) {
                if (config == null || !valid(config.getTableName())) throw invalid("表名不合法");
                if (config.getFields() == null || config.getFields().isEmpty()) throw invalid("字段列表不能为空");
                if (tables.put(config.getTableName(), config) != null) throw invalid("表名重复");
                java.util.Set<String> fieldNames = new java.util.HashSet<>();
                for (FieldConfig fc : config.getFields()) {
                    if (fc == null || !valid(fc.getName())) throw invalid("字段名不合法");
                    if (!fieldNames.add(fc.getName())) throw invalid("字段名重复");
                }
            }
        } catch (EncryptLiteException e) {
            throw e;
        } catch (Exception e) {
            throw invalid("读取JSON配置失败: " + e.getClass().getSimpleName());
        }
    }

    private static boolean valid(String value) { return value != null && IDENTIFIER.matcher(value).matches(); }
    private static EncryptLiteException invalid(String detail) {
        return new EncryptLiteException(EncryptLiteErrorCode.ENCRYPT_CONFIG_INVALID, detail);
    }

    @Data
    public static class TableConfig {
        private String tableName;
        private List<FieldConfig> fields;
    }

    @Data
    public static class FieldConfig {
        private String name;
        /** 字段长度，0表示CLOB等无限类型 */
        private int length;
    }
}
