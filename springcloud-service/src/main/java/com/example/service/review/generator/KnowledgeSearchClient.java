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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                    String cleaned = cleanContent(content);
                    String question = extractQuestion(cleaned, query, title);
                    map.put("question", question);
                    map.put("answer", cleaned);
                    map.put("source", source);
                    String dedup = cleaned.length() > 100 ? cleaned.substring(0, 100) : cleaned;
                    map.put("dedupKey", dedup);
                    results.add(map);
                }
            }
        } catch (Exception e) {
            log.error("KB server search failed: query={}", query, e);
        }
        return results;
    }

    private static final Pattern PAGE_NO = Pattern.compile("第\\d+\\s*页\\s*共\\d+\\s*页");
    private static final Pattern[] NOISE = {
        Pattern.compile("我是被编程耽误的文艺Tom.*?。", Pattern.DOTALL),
        Pattern.compile("大家好[，,。]"),
        Pattern.compile("如果[本这]次面试解析对你有帮助[^。]*。"),
        Pattern.compile("关注我[，,][^。]*。"),
        Pattern.compile("请动动手指一键三连[^。]*。"),
        Pattern.compile("可以在评论区留言[^。]*。"),
        Pattern.compile("点赞[^。]*关注[^。]*。"),
        Pattern.compile("一个工作了\\d+\\s*年的程序员[^。]*。"),
        Pattern.compile("更多.*?请关注[^。]*。"),
        Pattern.compile("Java 全栈面试复习大全.*?目录导航"),
        Pattern.compile("[\\.·]{2,}"),
    };

    String cleanContent(String content) {
        if (content == null || content.trim().isEmpty()) return "";
        String s = PAGE_NO.matcher(content).replaceAll("");
        for (Pattern p : NOISE) {
            s = p.matcher(s).replaceAll("");
        }
        s = s.replaceAll("[\\n\\r]+", " ").replaceAll("\\s{2,}", " ").trim();
        return s;
    }

    String extractQuestion(String cleaned, String query, String title) {
        if (cleaned == null || cleaned.isEmpty()) return query != null && !query.isEmpty() ? query : title;
        String s = cleaned.replaceFirst("^\\s*\\d+[、.．]\\s*", "").trim();
        s = s.replaceFirst("^\\s*[A-Za-z]+\\s+", "").trim();
        int qIdx = -1;
        int limit = Math.min(s.length(), 150);
        for (int i = 0; i < limit; i++) {
            char c = s.charAt(i);
            if (c == '？' || c == '?') { qIdx = i; break; }
        }
        if (qIdx > 5) {
            int start = 0;
            for (int i = qIdx - 1; i >= 0; i--) {
                char c = s.charAt(i);
                if (c == '。' || c == '；' || c == ';') { start = i + 1; break; }
            }
            String q = s.substring(start, qIdx + 1).trim();
            return q.length() > 80 ? q.substring(0, 80) + "…" : q;
        }
        int pIdx = -1;
        for (int i = 0; i < Math.min(s.length(), 100); i++) {
            if (s.charAt(i) == '。') { pIdx = i; break; }
        }
        if (pIdx > 5 && pIdx <= 40) {
            return s.substring(0, pIdx + 1).trim();
        }
        return query != null && !query.isEmpty() ? query : (title != null ? title : s.substring(0, Math.min(s.length(), 60)));
    }
}
