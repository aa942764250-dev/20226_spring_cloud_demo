package com.example.service.review.generator;

import com.example.service.review.config.ReviewProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 云端后端：腾讯 IMA 知识库 OpenAPI（WorkBuddy 资料库底层即 IMA）。
 *
 * 已查证要点：
 *  - 检索接口 POST https://ima.tencent.com/wiki/v1/knowledge/search
 *  - 认证不是标准 Authorization，而是两个自定义头：
 *      ima-openapi-clientid / ima-openapi-apikey
 *  - Content-Type 必须显式 application/json（缺了会被拒）
 *  - 路径前缀必须是 /wiki/v1/（写成 /api/v1/ 会 401）
 *  - 请求体 { knowledge_id, query, top_k }
 *
 * 凭证（client_id / api_key / knowledge_id）由 ReviewProperties 注入，
 * 远程部署务必通过环境变量传入，禁止硬编码。
 *
 * TODO(实测): IMA 真实返回字段名待用户拿到 knowledge_id 后跑一次 curl 确认，
 * 下方用宽松解析兼容多种可能的结构（results/data/list + content/text/snippet/answer），
 * 实测后应收紧字段名。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImaKnowledgeSearcher implements KnowledgeSearcher {

    private static final String ENDPOINT = "https://ima.tencent.com/wiki/v1/knowledge/search";

    private final ReviewProperties reviewProperties;
    private final ObjectMapper objectMapper;

    @Override
    public List<Map<String, String>> search(String query, int topK) {
        List<Map<String, String>> results = new ArrayList<>();
        String clientId = reviewProperties.getImaClientId();
        String apiKey = reviewProperties.getImaApiKey();
        String knowledgeId = reviewProperties.getImaKnowledgeId();
        if (isBlank(clientId) || isBlank(apiKey) || isBlank(knowledgeId)) {
            log.warn("[ima] IMA client_id/api_key/knowledge_id not configured, skipping");
            return results;
        }
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(ENDPOINT).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(reviewProperties.getSearchTimeoutSeconds() * 1000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("ima-openapi-clientid", clientId);
            conn.setRequestProperty("ima-openapi-apikey", apiKey);

            Map<String, Object> payload = new HashMap<>();
            payload.put("knowledge_id", knowledgeId);
            payload.put("query", query);
            payload.put("top_k", topK);
            String body = objectMapper.writeValueAsString(payload);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code != 200) {
                log.warn("[ima] IMA API returned {}: query={}", code, query);
                conn.disconnect();
                return results;
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            conn.disconnect();

            JsonNode root = objectMapper.readTree(sb.toString());
            JsonNode arr = resolveResultArray(root);
            if (arr != null && arr.isArray()) {
                for (JsonNode item : arr) {
                    Map<String, String> map = new HashMap<>();
                    String title = textOrEmpty(item, "title", "name", "doc_name");
                    String content = textOrEmpty(item, "content", "text", "snippet", "answer", "fragment");
                    String source = textOrEmpty(item, "source", "doc_name", "title", "url");
                    String cleaned = KnowledgeSearchUtils.cleanContent(content);
                    String question = KnowledgeSearchUtils.extractQuestion(cleaned, query, title);
                    map.put("question", question);
                    map.put("answer", cleaned);
                    map.put("source", source.isEmpty() ? title : source);
                    String dedup = cleaned.length() > 100 ? cleaned.substring(0, 100) : cleaned;
                    map.put("dedupKey", dedup);
                    results.add(map);
                }
            } else {
                log.warn("[ima] unexpected response shape: {}",
                        sb.length() > 200 ? sb.substring(0, 200) : sb);
            }
        } catch (Exception e) {
            log.error("[ima] IMA search failed: query={}", query, e);
        }
        return results;
    }

    /**
     * 宽松兼容 IMA 可能的返回结构：
     * { results:[...] } / { data:[...] } / { data:{ list:[...] } } / { list:[...] } / 直接数组
     */
    private static JsonNode resolveResultArray(JsonNode root) {
        if (root == null) {
            return null;
        }
        if (root.has("results")) {
            return root.get("results");
        }
        if (root.has("data")) {
            JsonNode data = root.get("data");
            if (data.isArray()) {
                return data;
            }
            if (data.has("list")) {
                return data.get("list");
            }
            if (data.has("results")) {
                return data.get("results");
            }
            return null;
        }
        if (root.has("list")) {
            return root.get("list");
        }
        return root.isArray() ? root : null;
    }

    private static String textOrEmpty(JsonNode node, String... fields) {
        for (String f : fields) {
            if (node.has(f) && !node.get(f).isNull()) {
                return node.get(f).asText();
            }
        }
        return "";
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
