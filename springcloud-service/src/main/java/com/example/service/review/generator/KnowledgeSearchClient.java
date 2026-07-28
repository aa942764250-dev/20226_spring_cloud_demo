package com.example.service.review.generator;

import com.example.service.review.config.ReviewProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeSearchClient {

    private final ReviewProperties reviewProperties;
    private final ObjectMapper objectMapper;

    public List<Map<String, String>> search(String query, int topK) {
        List<Map<String, String>> results = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    reviewProperties.getPythonPath(),
                    reviewProperties.getKbScriptPath(),
                    "search",
                    query,
                    "--top-k", String.valueOf(topK)
            );
            pb.directory(new java.io.File(reviewProperties.getKbScriptPath()).getParentFile());
            pb.redirectErrorStream(true);

            Map<String, String> env = pb.environment();
            env.put("HF_HUB_OFFLINE", "1");
            env.remove("ALL_PROXY");
            env.remove("all_proxy");

            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(reviewProperties.getSearchTimeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("kb.py search 超时: query={}", query);
                return results;
            }

            if (process.exitValue() != 0) {
                log.warn("kb.py search 返回非零退出码 {}: query={}", process.exitValue(), query);
                return results;
            }

            String json = output.toString().trim();
            if (json.isEmpty()) {
                return results;
            }

            JsonNode root = objectMapper.readTree(json);
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
            log.error("kb.py search 调用失败: query={}", query, e);
        }
        return results;
    }
}