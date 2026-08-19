package com.example.service.review.generator;

import com.example.service.review.config.ReviewProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 本地后端：自部署的 kb_server（Project_006 LocalKnowledgeMCP，默认 127.0.0.1:9876）。
 * 从原 KnowledgeSearchClient 整体迁移 GET 逻辑，行为保持一致。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalKbServerSearcher implements KnowledgeSearcher {

    private final ReviewProperties reviewProperties;
    private final ObjectMapper objectMapper;

    @Override
    public List<Map<String, String>> search(String query, int topK) {
        List<Map<String, String>> results = new ArrayList<>();
        try {
            String baseUrl = reviewProperties.getKbServerUrl();
            if (baseUrl == null || baseUrl.trim().isEmpty()) {
                log.warn("[local] kb-server-url not configured, skipping");
                return results;
            }

            String url = baseUrl + "/search?q="
                    + java.net.URLEncoder.encode(query, "UTF-8") + "&top_k=" + topK;
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(reviewProperties.getSearchTimeoutSeconds() * 1000);

            int code = conn.getResponseCode();
            if (code != 200) {
                log.warn("[local] KB server returned {}: query={}", code, query);
                conn.disconnect();
                return results;
            }

            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line);
                }
            }
            conn.disconnect();

            JsonNode root = objectMapper.readTree(body.toString());
            JsonNode resultsNode = root.has("results") ? root.get("results") : root;
            if (resultsNode.isArray()) {
                for (JsonNode item : resultsNode) {
                    Map<String, String> map = new HashMap<>();
                    String title = item.has("title") ? item.get("title").asText() : "";
                    String content = item.has("content") ? item.get("content").asText() : "";
                    String source = item.has("source") ? item.get("source").asText() : "";
                    String cleaned = KnowledgeSearchUtils.cleanContent(content);
                    String question = KnowledgeSearchUtils.extractQuestion(cleaned, query, title);
                    map.put("question", question);
                    map.put("answer", cleaned);
                    map.put("source", source);
                    String dedup = cleaned.length() > 100 ? cleaned.substring(0, 100) : cleaned;
                    map.put("dedupKey", dedup);
                    results.add(map);
                }
            }
        } catch (Exception e) {
            log.error("[local] KB server search failed: query={}", query, e);
        }
        return results;
    }
}
