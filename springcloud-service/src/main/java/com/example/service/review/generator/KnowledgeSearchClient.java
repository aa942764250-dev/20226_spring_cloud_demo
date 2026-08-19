package com.example.service.review.generator;

import com.example.service.review.config.ReviewProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库检索路由（对外入口，签名与历史版本保持一致，上游零改动）。
 *
 * 由 review.kb-mode 决定行为：
 *   ima   - 仅走 IMA 云端向量（腾讯 ima 知识库 OpenAPI）
 *   local - 仅走自部署 kb_server（Project_006，默认 127.0.0.1:9876）
 *   both  - 双查合并：IMA 优先，本地补充，按 dedupKey 去重（默认）
 *
 * 任一后端失败只记日志、返回空列表，不影响另一个后端。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeSearchClient {

    private final ReviewProperties reviewProperties;
    private final ImaKnowledgeSearcher imaSearcher;
    private final LocalKbServerSearcher localSearcher;

    public List<Map<String, String>> search(String query, int topK) {
        String mode = reviewProperties.getKbMode() == null
                ? "both" : reviewProperties.getKbMode().trim().toLowerCase();
        List<Map<String, String>> results = new ArrayList<>();
        try {
            if ("ima".equals(mode) || "both".equals(mode)) {
                results.addAll(imaSearcher.search(query, topK));
            }
            if ("local".equals(mode) || "both".equals(mode)) {
                results.addAll(localSearcher.search(query, topK));
            }
            if ("both".equals(mode)) {
                results = dedupe(results);
            }
        } catch (Exception e) {
            log.error("KB search routing failed: query={}, mode={}", query, mode, e);
        }
        return results;
    }

    /**
     * both 模式去重：同 dedupKey 只保留先到的（IMA 在前，即 IMA 优先）。
     */
    private List<Map<String, String>> dedupe(List<Map<String, String>> list) {
        Map<String, Map<String, String>> seen = new LinkedHashMap<>();
        for (Map<String, String> m : list) {
            String key = m.getOrDefault("dedupKey", "");
            if (key.isEmpty()) {
                continue;
            }
            seen.putIfAbsent(key, m);
        }
        return new ArrayList<>(seen.values());
    }
}
