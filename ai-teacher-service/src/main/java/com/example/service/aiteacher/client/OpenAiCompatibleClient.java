package com.example.service.aiteacher.client;

import com.example.service.aiteacher.config.AiTeacherProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * OpenAI 兼容协议客户端（Kimi / DeepSeek / 硅基流动 通用）。
 * 带连接/读取超时 + 重试；apiKey 日志脱敏。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiCompatibleClient implements LlmClient {

    private final AiTeacherProperties properties;
    private final ObjectMapper objectMapper;
    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        // 读取超时至少 60s，长上下文(如 Kimi 128k)生成较慢
        int read = Math.max(60_000, properties.getTimeoutSeconds() * 1000);
        factory.setReadTimeout(read);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public String generate(String baseUrl, String model, String apiKey, String systemPrompt, String userPrompt) {
        if (baseUrl == null || baseUrl.isEmpty() || apiKey == null || apiKey.isEmpty()) {
            log.warn("OpenAiCompatibleClient 缺少 baseUrl 或 apiKey，跳过调用");
            return null;
        }
        String url = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";
        try {
            Map<String, Object> sysMsg = new HashMap<>();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemPrompt);
            Map<String, Object> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userPrompt);

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("messages", Arrays.asList(sysMsg, userMsg));
            // Kimi K2 系列推理模型强制 temperature=1；其余 OpenAI 兼容模型用 0.7
            double temperature = (baseUrl != null && baseUrl.contains("moonshot")) ? 1.0 : 0.7;
            body.put("temperature", temperature);
            // K2 为推理模型，reasoning 会消耗较多 token，输出上限放宽到 8192
            body.put("max_tokens", 8192);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            String json = objectMapper.writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<>(json, headers);

            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            if (resp.getStatusCode() == HttpStatus.OK && resp.getBody() != null) {
                JsonNode root = objectMapper.readTree(resp.getBody());
                JsonNode choices = root.get("choices");
                if (choices != null && choices.isArray() && choices.size() > 0) {
                    JsonNode msg = choices.get(0).get("message");
                    if (msg != null) {
                        JsonNode content = msg.get("content");
                        if (content != null && !content.asText().isEmpty()) {
                            return content.asText();
                        }
                    }
                }
                log.warn("OpenAI兼容接口返回结构异常: {}", mask(resp.getBody()));
            } else {
                log.warn("OpenAI兼容接口HTTP状态异常: {}", resp.getStatusCode());
            }
        } catch (Exception e) {
            log.error("OpenAI兼容接口调用失败 baseUrl={} model={} key={}", baseUrl, model, maskKey(apiKey), e);
        }
        return null;
    }

    /** 带重试的生成（使用 properties.maxRetries） */
    public String generateWithRetry(String baseUrl, String model, String apiKey, String systemPrompt, String userPrompt) {
        int retries = Math.max(0, properties.getMaxRetries());
        for (int i = 0; i <= retries; i++) {
            String r = generate(baseUrl, model, apiKey, systemPrompt, userPrompt);
            if (r != null && !r.isEmpty()) {
                return r;
            }
            if (i < retries) {
                log.info("AI生成第{}次失败，准备重试", i + 1);
                try {
                    Thread.sleep(1000L * (i + 1));
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return null;
    }

    private String maskKey(String key) {
        if (key == null || key.length() <= 6) {
            return "******";
        }
        return key.substring(0, 6) + "****";
    }

    private String mask(String body) {
        if (body == null) {
            return "";
        }
        return body.length() > 300 ? body.substring(0, 300) + "..." : body;
    }
}
