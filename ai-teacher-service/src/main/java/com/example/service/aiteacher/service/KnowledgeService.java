package com.example.service.aiteacher.service;

import com.example.service.aiteacher.config.AiTeacherProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.util.*;

/**
 * 知识库检索服务。
 * 加载预计算的向量文件（knowledge_vectors.json），实现余弦相似度检索。
 * 向量在本地预处理生成，远端只加载到内存，零额外服务。
 */
@Slf4j
@Service
public class KnowledgeService {

    private final ObjectMapper objectMapper;
    private final AiTeacherProperties properties;

    private List<KnowledgeChunk> chunks = new ArrayList<>();
    private int dimension = 0;
    private boolean loaded = false;

    public KnowledgeService(ObjectMapper objectMapper, AiTeacherProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("knowledge_vectors.json")) {
            if (is == null) {
                log.warn("知识库向量文件不存在 (knowledge_vectors.json)，检索功能暂不可用");
                return;
            }
            JsonNode root = objectMapper.readTree(is);
            this.dimension = root.path("dimension").asInt(0);
            JsonNode chunksNode = root.path("chunks");
            for (JsonNode node : chunksNode) {
                KnowledgeChunk chunk = new KnowledgeChunk();
                chunk.id = node.path("id").asLong();
                chunk.text = node.path("text").asText();
                chunk.source = node.path("source").asText();
                chunk.category = node.path("category").asText();
                chunk.label = node.path("label").asText();
                JsonNode embNode = node.path("embedding");
                float[] emb = new float[embNode.size()];
                for (int i = 0; i < embNode.size(); i++) {
                    emb[i] = (float) embNode.get(i).asDouble();
                }
                chunk.embedding = emb;
                chunks.add(chunk);
            }
            this.loaded = true;
            log.info("知识库加载完成: {} 个文本块, 向量维度: {}", chunks.size(), dimension);
        } catch (Exception e) {
            log.error("知识库加载失败", e);
        }
    }

    /**
     * 检索最相似的 topK 个文本块。
     * queryEmbedding 由调用方通过 Embedding API 获取。
     */
    public List<SearchResult> search(float[] queryEmbedding, int topK, String category) {
        if (!loaded || chunks.isEmpty()) {
            return Collections.emptyList();
        }
        List<SearchResult> results = new ArrayList<>();
        for (KnowledgeChunk chunk : chunks) {
            if (category != null && !category.isEmpty() && !category.equals(chunk.category)) {
                continue;
            }
            double score = cosineSimilarity(queryEmbedding, chunk.embedding);
            results.add(new SearchResult(chunk, score));
        }
        results.sort((a, b) -> Double.compare(b.score, a.score));
        return results.subList(0, Math.min(topK, results.size()));
    }

    /**
     * 检索并拼接成上下文文本，用于传给大模型。
     */
    public String searchAsContext(float[] queryEmbedding, int topK, String category) {
        List<SearchResult> results = search(queryEmbedding, topK, category);
        if (results.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            SearchResult r = results.get(i);
            sb.append("【资料").append(i + 1).append("】来源:").append(r.chunk.label);
            sb.append(" (相似度:").append(String.format("%.3f", r.score)).append(")\n");
            sb.append(r.chunk.text).append("\n\n");
        }
        return sb.toString();
    }

    public boolean isLoaded() {
        return loaded;
    }

    public int getChunkCount() {
        return chunks.size();
    }

    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) return 0;
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public static class KnowledgeChunk {
        public long id;
        public String text;
        public String source;
        public String category;
        public String label;
        public float[] embedding;
    }

    public static class SearchResult {
        public KnowledgeChunk chunk;
        public double score;

        public SearchResult(KnowledgeChunk chunk, double score) {
            this.chunk = chunk;
            this.score = score;
        }
    }
}
