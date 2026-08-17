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
 * Embedding 客户端（兼容 OpenAI 格式，支持硅基流动 / 阿里云百炼等）。
 * 用于将查询文本转为向量，供知识库检索使用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingClient {

    private final AiTeacherProperties properties;
    private final ObjectMapper objectMapper;
    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(30_000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * 将单条文本转为向量。
     */
    public float[] embed(String text) {
        if (text == null || text.isEmpty()) {
            return new float[0];
        }
        List<float[]> results = embedBatch(Collections.singletonList(text));
        return results.isEmpty() ? new float[0] : results.get(0);
    }

    /**
     * 批量将文本转为向量。
     */
    public List<float[]> embedBatch(List<String> texts) {
        String apiUrl = properties.getEmbeddingApiUrl();
        String apiKey = properties.getEmbeddingApiKey();
        String model = properties.getEmbeddingModel();

        if (apiUrl == null || apiUrl.isEmpty() || apiKey == null || apiKey.isEmpty()) {
            log.warn("Embedding 未配置（embeddingApiUrl / embeddingApiKey），返回空向量");
            return Collections.emptyList();
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("input", texts);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            String json = objectMapper.writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<>(json, headers);

            ResponseEntity<String> resp = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);
            if (resp.getStatusCode() == HttpStatus.OK && resp.getBody() != null) {
                JsonNode root = objectMapper.readTree(resp.getBody());
                JsonNode data = root.path("data");
                List<float[]> results = new ArrayList<>();
                for (JsonNode item : data) {
                    JsonNode emb = item.path("embedding");
                    float[] vec = new float[emb.size()];
                    for (int i = 0; i < emb.size(); i++) {
                        vec[i] = (float) emb.get(i).asDouble();
                    }
                    results.add(vec);
                }
                return results;
            }
        } catch (Exception e) {
            log.error("Embedding 调用失败: {}", e.getMessage());
        }
        return Collections.emptyList();
    }
}
