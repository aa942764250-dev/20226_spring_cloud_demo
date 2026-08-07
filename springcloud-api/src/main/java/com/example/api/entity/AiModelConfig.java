package com.example.api.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 模型配置表（Kimi / DeepSeek / 硅基流动 等 OpenAI 兼容提供方）
 * 表名由 MyBatis-Plus 按类名自动映射为 ai_model_config。
 */
@Data
public class AiModelConfig implements Serializable {
    /** 主键 */
    private Long id;
    /** 提供方：kimi / deepseek / siliconflow */
    private String provider;
    /** 模型名称 */
    private String modelName;
    /** API Key（敏感，日志/接口不回显明文） */
    private String apiKey;
    /** 是否启用 0=否 1=是 */
    private Integer enabled;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
