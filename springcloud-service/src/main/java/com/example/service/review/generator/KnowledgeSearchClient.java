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
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeSearchClient {

    private final ReviewProperties reviewProperties;
    private final ObjectMapper objectMapper;

    public List<Map<String, String>> search(String query, int topK) {
        List<Map<String, String>> results = new ArrayList<>();
        try {
            String baseUrl = reviewProperties.getKbServerUrl();
            if (baseUrl == null || baseUrl.trim().isEmpty()) {
                log.warn("kb-server-url not configured, returning empty results");
                return results;
            }

            String url = baseUrl + "/search?q=" + java.net.URLEncoder.encode(query, "UTF-8") + "&top_k=" + topK;
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(reviewProperties.getSearchTimeoutSeconds() * 1000);

            int code = conn.getResponseCode();
            if (code != 200) {
                log.warn("KB server returned {}: query={}", code, query);
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
                    map.put("question", title.isEmpty() ? query : title);
                    map.put("answer", content);
                    map.put("source", source);
                    results.add(map);
                }
            }
        } catch (Exception e) {
            log.error("KB server search failed: query={}", query, e);
        }
        return results;
    }
}
