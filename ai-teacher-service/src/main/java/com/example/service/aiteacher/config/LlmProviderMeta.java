package com.example.service.aiteacher.config;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * LLM 提供方元信息（baseUrl 与可选模型）。三家均为 OpenAI 兼容协议。
 */
@Getter
public class LlmProviderMeta {
    private final String provider;
    private final String label;
    private final String baseUrl;
    private final List<ModelOption> models;

    public LlmProviderMeta(String provider, String label, String baseUrl, List<ModelOption> models) {
        this.provider = provider;
        this.label = label;
        this.baseUrl = baseUrl;
        this.models = models;
    }

    @Getter
    public static class ModelOption {
        private final String value;
        private final String label;

        public ModelOption(String value, String label) {
            this.value = value;
            this.label = label;
        }
    }

    public static final List<LlmProviderMeta> ALL = Arrays.asList(
            new LlmProviderMeta("kimi", "Kimi (月之暗面)", "https://api.moonshot.cn/v1",
                    Arrays.asList(
                            new ModelOption("kimi-k2.6", "kimi-k2.6（推荐·通用对话）"),
                            new ModelOption("kimi-k2.7-code", "kimi-k2.7-code（代码）"))),
            new LlmProviderMeta("deepseek", "DeepSeek", "https://api.deepseek.com/v1",
                    Arrays.asList(
                            new ModelOption("deepseek-chat", "deepseek-chat（推荐）"),
                            new ModelOption("deepseek-reasoner", "deepseek-reasoner"))),
            new LlmProviderMeta("siliconflow", "硅基流动 (SiliconFlow)", "https://api.siliconflow.cn/v1",
                    Arrays.asList(
                            new ModelOption("Qwen/Qwen2.5-72B-Instruct", "Qwen2.5-72B-Instruct"),
                            new ModelOption("deepseek-ai/DeepSeek-V3", "DeepSeek-V3"),
                            new ModelOption("Pro/deepseek-ai/DeepSeek-R1", "DeepSeek-R1 (Pro)")))
    );

    public static String getBaseUrl(String provider) {
        return ALL.stream()
                .filter(p -> p.provider != null && p.provider.equals(provider))
                .map(LlmProviderMeta::getBaseUrl)
                .findFirst()
                .orElse(null);
    }
}
