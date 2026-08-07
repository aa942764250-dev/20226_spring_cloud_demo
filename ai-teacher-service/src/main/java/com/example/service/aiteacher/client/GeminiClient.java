package com.example.service.aiteacher.client;

import com.example.service.aiteacher.config.AiTeacherProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Gemini API 客户端
 * 调用 Google Gemini 2.5 Flash 生成AI报告
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiClient {

    private final AiTeacherProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 调用Gemini生成文本
     * @param systemPrompt 系统提示
     * @param userPrompt 用户提示
     * @return 生成文本
     */
    public String generate(String systemPrompt, String userPrompt) {
        String url = String.format(
                "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                properties.getGeminiModel(), properties.getGeminiApiKey());

        try {
            // 构建请求体
            Map<String, Object> systemPart = new HashMap<>();
            systemPart.put("text", systemPrompt);
            Map<String, Object> userPart = new HashMap<>();
            userPart.put("text", userPrompt);

            Map<String, Object> systemContent = new HashMap<>();
            systemContent.put("role", "system");
            systemContent.put("parts", Collections.singletonList(systemPart));

            Map<String, Object> userContent = new HashMap<>();
            userContent.put("role", "user");
            userContent.put("parts", Collections.singletonList(userPart));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", Arrays.asList(systemContent, userContent));

            // 生成配置
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("maxOutputTokens", 4096);
            generationConfig.put("responseMimeType", "application/json");
            requestBody.put("generationConfig", generationConfig);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode candidates = root.get("candidates");
                if (candidates != null && candidates.isArray() && candidates.size() > 0) {
                    JsonNode content = candidates.get(0).get("content");
                    if (content != null) {
                        JsonNode parts = content.get("parts");
                        if (parts != null && parts.isArray() && parts.size() > 0) {
                            return parts.get(0).get("text").asText();
                        }
                    }
                }
            }
            log.warn("Gemini返回异常: status={}", response.getStatusCode());
            return null;
        } catch (Exception e) {
            log.error("Gemini API调用失败", e);
            return null;
        }
    }

    /**
     * 带重试的生成
     */
    public String generateWithRetry(String systemPrompt, String userPrompt) {
        for (int i = 0; i <= properties.getMaxRetries(); i++) {
            String result = generate(systemPrompt, userPrompt);
            if (result != null) {
                return result;
            }
            if (i < properties.getMaxRetries()) {
                log.info("Gemini生成失败，第{}次重试", i + 1);
                try {
                    Thread.sleep(1000L * (i + 1));
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return null;
    }
}
