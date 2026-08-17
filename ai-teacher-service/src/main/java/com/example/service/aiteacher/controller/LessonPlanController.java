package com.example.service.aiteacher.controller;

import com.example.api.entity.AiModelConfig;
import com.example.service.aiteacher.client.EmbeddingClient;
import com.example.service.aiteacher.client.OpenAiCompatibleClient;
import com.example.service.aiteacher.config.AiTeacherProperties;
import com.example.service.aiteacher.config.LlmProviderMeta;
import com.example.service.dao.AiModelConfigDao;
import com.example.service.aiteacher.service.KnowledgeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 课文教案生成控制器。
 * 基于知识库检索 + Kimi 大模型生成教案。
 */
@Slf4j
@RestController
@RequestMapping("/ai-teacher/lesson-plan")
@RequiredArgsConstructor
public class LessonPlanController {

    private final KnowledgeService knowledgeService;
    private final EmbeddingClient embeddingClient;
    private final OpenAiCompatibleClient openAiClient;
    private final AiModelConfigDao modelConfigDao;
    private final AiTeacherProperties properties;

    /**
     * 生成教案。
     * @param request 包含 topic（单元/课文主题）、grade（年级）、category（可选：grammar/vocabulary/textbook/sentence）
     */
    @PostMapping("/generate")
    public Map<String, Object> generate(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        String topic = request.getOrDefault("topic", "");
        String grade = request.getOrDefault("grade", "");
        String category = request.getOrDefault("category", "");
        int topK = Integer.parseInt(request.getOrDefault("topK", "5"));

        if (topic.isEmpty()) {
            result.put("code", 400);
            result.put("message", "topic 不能为空");
            return result;
        }

        try {
            // 1. 知识库检索
            String context = "";
            if (knowledgeService.isLoaded()) {
                float[] queryEmb = embeddingClient.embed(topic + " " + grade);
                if (queryEmb.length > 0) {
                    context = knowledgeService.searchAsContext(queryEmb, topK, category);
                }
                log.info("知识库检索完成: topic={}, context长度={}", topic, context.length());
            } else {
                log.warn("知识库未加载，跳过检索");
            }

            // 2. 构建 Prompt
            String systemPrompt = buildSystemPrompt();
            String userPrompt = buildUserPrompt(topic, grade, context);

            // 3. 调用 Kimi 生成教案（全局锁串行，避免并发429）
            AiModelConfig cfg = modelConfigDao.selectList(
                    new LambdaQueryWrapper<AiModelConfig>().eq(AiModelConfig::getEnabled, 1).last("LIMIT 1"))
                    .stream().findFirst().orElse(null);

            if (cfg == null || cfg.getApiKey() == null || cfg.getApiKey().isEmpty()) {
                result.put("code", 500);
                result.put("message", "未配置可用的 AI 模型");
                return result;
            }

            String baseUrl = LlmProviderMeta.getBaseUrl(cfg.getProvider());
            String model = cfg.getModelName();
            String apiKey = cfg.getApiKey();

            String lessonPlan = openAiClient.generate(baseUrl, model, apiKey, systemPrompt, userPrompt);

            if (lessonPlan == null || lessonPlan.isEmpty()) {
                result.put("code", 500);
                result.put("message", "AI 生成失败");
                return result;
            }

            result.put("code", 200);
            result.put("data", lessonPlan);
            result.put("context_used", context.length() > 0);
            result.put("topic", topic);
            return result;

        } catch (Exception e) {
            log.error("教案生成失败", e);
            result.put("code", 500);
            result.put("message", "生成失败: " + e.getMessage());
            return result;
        }
    }

    /** 查询知识库状态 */
    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("knowledge_loaded", knowledgeService.isLoaded());
        result.put("knowledge_chunks", knowledgeService.getChunkCount());
        return result;
    }

    private String buildSystemPrompt() {
        return "你是资深英语教研组长，擅长设计精炼实用的教案。\n" +
                "要求：\n" +
                "1. 包含：教学目标(3条)、重难点、词汇/语法精讲(各3-5个)、课堂活动(2-3个)、分层作业\n" +
                "2. 严格基于参考资料，不编造\n" +
                "3. Markdown格式，总字数控制在1500字以内，只写要点不展开论述";
    }

    private String buildUserPrompt(String topic, String grade, String context) {
        StringBuilder sb = new StringBuilder();
        sb.append("请为以下内容生成一份完整的英语教案：\n\n");
        sb.append("【单元/课文主题】").append(topic).append("\n");
        if (!grade.isEmpty()) {
            sb.append("【年级】").append(grade).append("\n");
        }
        sb.append("\n");
        if (!context.isEmpty()) {
            sb.append("【参考资料（从知识库检索）】\n").append(context).append("\n");
        } else {
            sb.append("【注意】未检索到相关参考资料，请基于通用英语教学知识生成教案。\n\n");
        }
        sb.append("请生成完整教案。");
        return sb.toString();
    }
}
