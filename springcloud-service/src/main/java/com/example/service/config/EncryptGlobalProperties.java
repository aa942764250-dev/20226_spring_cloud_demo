package com.example.service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * 加密初始化全局配置属性。
 * <p>
 * 绑定 application.yml 中 {@code encrypt.init} 前缀下的配置项，
 * 提供加密初始化组件的默认参数。支持通过 Nacos 配置中心热加载。
 * </p>
 */
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "encrypt.init")
public class EncryptGlobalProperties {

    /** 全局默认加密算法标识，字段级未指定时使用此值 */
    private String defaultAlgorithm = "SM4";

    /** 全局默认批次大小，范围 [100, 10000] */
    private Integer defaultBatchSize = 1000;

    /** 全局默认批次间隔（毫秒），范围 [0, 5000] */
    private Integer defaultBatchIntervalMs = 0;

    /** 全局默认是否跳过已加密数据 */
    private Boolean defaultSkipEncrypted = true;

    /** 数据库操作重试配置 */
    private DbRetry dbRetry = new DbRetry();

    /** 单批次超时时间（秒） */
    private Integer batchTimeoutSeconds = 30;

    /** 密文中算法标识前缀的分隔符 */
    private String algorithmPrefixSeparator = ":";

    /**
     * 数据库操作重试配置。
     */
    @Data
    public static class DbRetry {

        /** 最大重试次数 */
        private Integer maxAttempts = 3;

        /** 重试间隔（毫秒） */
        private Long retryIntervalMs = 5000L;
    }
}
