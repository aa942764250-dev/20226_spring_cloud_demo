package com.example.service.aiteacher.client;

/**
 * 大模型调用客户端接口。各家（Kimi / DeepSeek / 硅基流动）均为 OpenAI 兼容协议，共用实现。
 */
public interface LlmClient {
    /**
     * 单次调用大模型生成文本
     *
     * @param baseUrl      OpenAI 兼容接口的 baseUrl（如 https://api.moonshot.cn/v1）
     * @param model        模型名称
     * @param apiKey       API Key（日志中会脱敏）
     * @param systemPrompt 系统提示
     * @param userPrompt   用户提示
     * @return 生成文本；失败返回 null
     */
    String generate(String baseUrl, String model, String apiKey, String systemPrompt, String userPrompt);
}
