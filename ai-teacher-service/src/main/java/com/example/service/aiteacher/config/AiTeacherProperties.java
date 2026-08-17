package com.example.service.aiteacher.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI教师工作台配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai-teacher")
public class AiTeacherProperties {
    /** Gemini API Key */
    private String geminiApiKey;
    /** Gemini模型名称 */
    private String geminiModel = "gemini-2.5-flash";
    /** 请求超时秒数 */
    private int timeoutSeconds = 60;
    /** 最大重试次数 */
    private int maxRetries = 2;
    /** 默认教师ID（MVP单教师模式） */
    private Long defaultTeacherId = 1L;
    /** 报告生成任务租约（分钟）：status=5 超过该时间仍视为卡死，由调度器重新认领 */
    private int reportLeaseMinutes = 5;

    /** Embedding API 地址（兼容 OpenAI 格式，如硅基流动 https://api.siliconflow.cn/v1/embeddings） */
    private String embeddingApiUrl;
    /** Embedding API Key */
    private String embeddingApiKey;
    /** Embedding 模型名（如 BAAI/bge-m3） */
    private String embeddingModel = "BAAI/bge-m3";
}
