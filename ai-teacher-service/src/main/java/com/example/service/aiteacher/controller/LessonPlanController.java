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
     * 生成教案。支持两种模式：
     * 1. 课文教案：topic（单元/课文主题）+ grade + category
     * 2. 专属教案：studentId + studentName + grade + impressions（印象标签数组）+ note
     */
    @PostMapping("/generate")
    public Map<String, Object> generate(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        String topic = getStr(request, "topic", "");
        String grade = getStr(request, "grade", "");
        String category = getStr(request, "category", "");
        String studentName = getStr(request, "studentName", "");
        String note = getStr(request, "note", "");
        int topK = getInt(request, "topK", 5);

        // 解析 impressions（可能是 List 或 JSON 字符串）
        java.util.List<String> impressions = new java.util.ArrayList<>();
        Object impObj = request.get("impressions");
        if (impObj instanceof java.util.List) {
            for (Object o : (java.util.List<?>) impObj) {
                if (o != null) impressions.add(o.toString());
            }
        } else if (impObj instanceof String && !((String) impObj).isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                impressions = mapper.readValue((String) impObj, new com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>>() {});
            } catch (Exception ignored) {}
        }

        // 专属教案模式：没有 topic 时用学生印象标签构建 topic
        if (topic.isEmpty() && !impressions.isEmpty()) {
            topic = String.join(" ", impressions);
            log.info("专属教案模式：用印象标签构建 topic={}, studentName={}", topic, studentName);
        }

        if (topic.isEmpty()) {
            result.put("code", 400);
            result.put("message", "topic 不能为空（专属教案需提供 impressions 印象标签）");
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
            String userPrompt = buildUserPrompt(topic, grade, context, studentName, impressions, note);

            // 3. 调用 AI 生成教案
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

            // 4. 解析成 sections 数组（适配前端 GenerationPanel）
            java.util.List<Map<String, String>> sections = parseSections(lessonPlan);

            result.put("code", 200);
            result.put("data", lessonPlan);
            result.put("sections", sections);
            result.put("context_used", context.length() > 0);
            result.put("topic", topic);
            result.put("studentName", studentName);
            result.put("examContext", szExamContext(grade));
            return result;

        } catch (Exception e) {
            log.error("教案生成失败", e);
            result.put("code", 500);
            result.put("message", "生成失败: " + e.getMessage());
            return result;
        }
    }

    private String getStr(Map<String, Object> map, String key, String def) {
        Object v = map.get(key);
        return v != null ? v.toString() : def;
    }

    private int getInt(Map<String, Object> map, String key, int def) {
        Object v = map.get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        if (v != null) {
            try { return Integer.parseInt(v.toString()); } catch (Exception ignored) {}
        }
        return def;
    }

    /** 把生成的教案文本按 Markdown 标题分割成 sections */
    private java.util.List<Map<String, String>> parseSections(String text) {
        java.util.List<Map<String, String>> sections = new java.util.ArrayList<>();
        if (text == null || text.isEmpty()) return sections;

        String[] lines = text.split("\n");
        StringBuilder currentContent = new StringBuilder();
        String currentTitle = "教案内容";

        for (String line : lines) {
            String trimmed = line.trim();
            // 匹配 Markdown 标题（## 或 ### 或 一、二、等中文序号）
            if (trimmed.matches("^#{1,3}\\s+.+") || trimmed.matches("^[一二三四五六七八九十]+[、.．]\\s*.+")) {
                if (currentContent.length() > 0 || !currentTitle.equals("教案内容")) {
                    Map<String, String> sec = new HashMap<>();
                    sec.put("title", currentTitle.replaceAll("^#+\\s*", "").trim());
                    sec.put("content", currentContent.toString().trim());
                    sections.add(sec);
                }
                currentTitle = trimmed;
                currentContent = new StringBuilder();
            } else {
                currentContent.append(line).append("\n");
            }
        }
        // 最后一个 section
        if (currentContent.length() > 0) {
            Map<String, String> sec = new HashMap<>();
            sec.put("title", currentTitle.replaceAll("^#+\\s*", "").trim());
            sec.put("content", currentContent.toString().trim());
            sections.add(sec);
        }
        // 如果没有分割到任何 section，把整个文本作为一个 section
        if (sections.isEmpty()) {
            Map<String, String> sec = new HashMap<>();
            sec.put("title", "专属教案");
            sec.put("content", text);
            sections.add(sec);
        }
        return sections;
    }

    /** 年级 -> 深圳机考语境 */
    private String szExamContext(String grade) {
        if (grade == null || grade.isEmpty()) return "深圳中高考英语";
        if (grade.contains("7")) return "深圳中考听说第一节：模仿朗读(8分)";
        if (grade.contains("8")) return "深圳中考听说第二节：听选信息与回答问题(10分)";
        if (grade.contains("9")) return "深圳中考听说第三节：思维导图复述(12分)";
        if (grade.contains("高一") || grade.contains("高二") || grade.contains("高三") || grade.contains("高中"))
            return "广东高考英语听说(20分制) Part B 故事复述 / 读后续写";
        return "深圳中考英语";
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

    private String buildUserPrompt(String topic, String grade, String context, String studentName, java.util.List<String> impressions, String note) {
        StringBuilder sb = new StringBuilder();
        boolean isPersonalized = (studentName != null && !studentName.isEmpty()) || (impressions != null && !impressions.isEmpty());

        if (isPersonalized) {
            sb.append("请为以下学生生成一份深圳本地化专属英语教案/特训方案：\n\n");
            if (studentName != null && !studentName.isEmpty()) {
                sb.append("【学生姓名】").append(studentName).append("\n");
            }
            if (grade != null && !grade.isEmpty()) {
                sb.append("【年级】").append(grade).append("\n");
            }
            sb.append("【深圳机考语境】").append(szExamContext(grade)).append("\n");
            if (impressions != null && !impressions.isEmpty()) {
                sb.append("【学生教研印象标签】\n");
                for (String imp : impressions) {
                    sb.append("- ").append(imp).append("\n");
                }
            }
            if (note != null && !note.isEmpty()) {
                sb.append("【教师补充要求】").append(note).append("\n");
            }
            sb.append("\n【训练重点】").append(topic).append("\n\n");
        } else {
            sb.append("请为以下内容生成一份完整的英语教案：\n\n");
            sb.append("【单元/课文主题】").append(topic).append("\n");
            if (grade != null && !grade.isEmpty()) {
                sb.append("【年级】").append(grade).append("\n");
            }
            sb.append("\n");
        }

        if (context != null && !context.isEmpty()) {
            sb.append("【参考资料（从知识库检索）】\n").append(context).append("\n");
        } else {
            sb.append("【注意】未检索到相关参考资料，请基于通用英语教学知识生成教案。\n\n");
        }
        sb.append("请生成完整教案。");
        return sb.toString();
    }
}
