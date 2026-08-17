package com.example.service.aiteacher.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.api.entity.*;
import com.example.service.aiteacher.client.OpenAiCompatibleClient;
import com.example.service.aiteacher.config.AiTeacherProperties;
import com.example.service.aiteacher.config.LlmProviderMeta;
import com.example.service.dao.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 报告异步生成执行器。
 * 由 {@code AiReportServiceImpl} 提交任务（status=4 生成中），
 * 本类在独立线程中调用真实大模型并回填结果（4→1 待审核 / 4→0 占位）。
 * 单独成类是为了避免 Spring @Async 自调用代理失效。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportGenerationExecutor {

    private final AiReportDao aiReportDao;
    private final LearningRecordDao learningRecordDao;
    private final AbilityRecordDao abilityRecordDao;
    private final PromptTemplateDao promptTemplateDao;
    private final StudentDao studentDao;
    private final AiModelConfigDao modelConfigDao;
    private final OpenAiCompatibleClient openAiClient;
    private final AiTeacherProperties properties;
    private final ObjectMapper objectMapper;

    @Async("reportAsyncExecutor")
    public void executeGeneration(Long reportId) {
        AiReport report = aiReportDao.selectById(reportId);
        if (report == null) {
            log.warn("异步生成任务找不到报告记录: reportId={}", reportId);
            return;
        }
        Long studentId = report.getStudentId();
        String reportType = report.getReportType();
        LocalDate startDate = report.getStartDate();
        LocalDate endDate = report.getEndDate();
        LocalDateTime now = LocalDateTime.now();

        try {
            // 学习记录
            LambdaQueryWrapper<LearningRecord> lrWrapper = new LambdaQueryWrapper<>();
            lrWrapper.eq(LearningRecord::getStudentId, studentId)
                    .between(LearningRecord::getLessonDate, startDate, endDate)
                    .orderByAsc(LearningRecord::getLessonDate);
            List<LearningRecord> records = learningRecordDao.selectList(lrWrapper);

            // 能力评分
            LambdaQueryWrapper<AbilityRecord> arWrapper = new LambdaQueryWrapper<>();
            arWrapper.eq(AbilityRecord::getStudentId, studentId)
                    .between(AbilityRecord::getAssessDate, startDate, endDate);
            List<AbilityRecord> abilities = abilityRecordDao.selectList(arWrapper);

            // Prompt 模板
            String templateType;
            if ("daily".equals(reportType)) {
                templateType = "daily_report";
            } else if ("weekly".equals(reportType)) {
                templateType = "weekly_report";
            } else {
                templateType = "monthly_report";
            }
            LambdaQueryWrapper<PromptTemplate> ptWrapper = new LambdaQueryWrapper<>();
            ptWrapper.eq(PromptTemplate::getType, templateType)
                    .eq(PromptTemplate::getEnabled, 1)
                    .orderByDesc(PromptTemplate::getVersion)
                    .last("LIMIT 1");
            PromptTemplate template = promptTemplateDao.selectOne(ptWrapper);

            Student student = studentDao.selectById(studentId);
            String systemPrompt = buildSystemPrompt(template);
            String userPrompt = buildUserPrompt(student, records, abilities, startDate, endDate, reportType);

            // 读取启用的模型配置
            AiModelConfig cfg = modelConfigDao.selectList(
                            new LambdaQueryWrapper<AiModelConfig>().eq(AiModelConfig::getEnabled, 1).last("LIMIT 1"))
                    .stream().findFirst().orElse(null);
            String baseUrl = cfg != null ? LlmProviderMeta.getBaseUrl(cfg.getProvider()) : null;
            String model = cfg != null ? cfg.getModelName() : null;
            String apiKey = cfg != null ? cfg.getApiKey() : null;

            if (baseUrl != null && apiKey != null && !apiKey.isEmpty()) {
                String aiResult = openAiClient.generateWithRetry(baseUrl, model, apiKey, systemPrompt, userPrompt);
                if (aiResult != null && !aiResult.isEmpty()) {
                    parseAndFillReport(report, aiResult);
                    report.setStatus(1); // 待审核
                    report.setReviewNote(null);
                    report.setUpdatedAt(now);
                    aiReportDao.updateById(report);
                    log.info("AI报告真实生成完成(异步): id={}, studentId={}, model={}", reportId, studentId, model);
                    return;
                } else {
                    fillPlaceholder(report, systemPrompt, userPrompt);
                    report.setStatus(0);
                    report.setReviewNote("AI 调用失败，已降级为占位内容；请检查模型配置与网络");
                }
            } else {
                fillPlaceholder(report, systemPrompt, userPrompt);
                report.setStatus(0);
                report.setReviewNote("未配置可用的 AI 模型（无启用配置或缺少 API Key），已生成占位内容");
            }
            report.setUpdatedAt(now);
            aiReportDao.updateById(report);
        } catch (Exception e) {
            log.error("AI报告异步生成异常: reportId={}", reportId, e);
            report.setStatus(0);
            report.setReviewNote("生成异常: " + e.getMessage());
            report.setUpdatedAt(now);
            aiReportDao.updateById(report);
        }
    }

    private void fillPlaceholder(AiReport report, String systemPrompt, String userPrompt) {
        report.setSummary("【占位】本报告由系统自动生成，AI 解析尚未接入。后续本地启动后将由 Gemini 填充真实分析内容。");
        report.setAbilityAnalysis("【占位】能力分析：基于学习记录与能力评分，待 AI 解析后生成。");
        report.setProblemDiagnosis("【占位】问题诊断：待 AI 解析后生成。");
        report.setTeachingSuggestion("【占位】教学建议：待 AI 解析后生成。");
        report.setFullContent("【SYSTEM_PROMPT】\n" + systemPrompt + "\n\n【USER_PROMPT】\n" + userPrompt);
    }

    private String buildSystemPrompt(PromptTemplate template) {
        if (template != null && template.getSystemPrompt() != null) {
            return template.getSystemPrompt();
        }
        return "你是一位专业的英语教学分析师。基于学生的学习数据，生成客观、专业的学习报告。\n" +
                "要求：\n" +
                "1. 基于真实学习数据，不夸大学习成果\n" +
                "2. 使用家长可理解的语言\n" +
                "3. 提供可执行的教学建议\n" +
                "4. 避免无依据的评价\n" +
                "5. 输出JSON格式，包含summary、abilityAnalysis、problemDiagnosis、teachingSuggestion四个字段";
    }

    private String buildUserPrompt(Student student, List<LearningRecord> records,
                                   List<AbilityRecord> abilities, LocalDate startDate,
                                   LocalDate endDate, String reportType) {
        StringBuilder sb = new StringBuilder();
        sb.append("请为以下学生生成").append(typeLabel(reportType)).append("报报告。\n\n");
        sb.append("## 学生信息\n");
        if (student != null) {
            sb.append("- 姓名：").append(student.getName()).append("\n");
            sb.append("- 年级：").append(student.getGrade()).append("\n");
            sb.append("- 等级：").append(student.getLevel()).append("\n");
            sb.append("- 目标：").append(student.getGoal()).append("\n");
        }
        sb.append("\n## 报告周期\n");
        sb.append(startDate).append(" 至 ").append(endDate).append("\n");
        sb.append("\n## 学习记录（共").append(records.size()).append("次）\n");
        for (LearningRecord r : records) {
            sb.append("- ").append(r.getLessonDate()).append(" | ");
            sb.append(r.getCourseType()).append(" | ");
            sb.append(r.getTopic()).append(" | ");
            sb.append("听").append(r.getListeningScore()).append("/说").append(r.getSpeakingScore());
            sb.append("/读").append(r.getReadingScore()).append("/写").append(r.getWritingScore());
            sb.append("/语").append(r.getGrammarScore()).append("/词").append(r.getVocabularyScore());
            if (r.getProblemTags() != null && !r.getProblemTags().isEmpty()) {
                sb.append(" | 问题：").append(r.getProblemTags());
            }
            sb.append("\n");
        }
        if (!abilities.isEmpty()) {
            sb.append("\n## 能力评分记录\n");
            for (AbilityRecord a : abilities) {
                sb.append("- ").append(a.getAssessDate()).append(" | ");
                sb.append(a.getDimension()).append(": ").append(a.getScore()).append("\n");
            }
        }
        return sb.toString();
    }

    private void parseAndFillReport(AiReport report, String aiResult) {
        report.setFullContent(aiResult);
        try {
            String json = extractJson(aiResult);
            JsonNode node = objectMapper.readTree(json);
            report.setSummary(getTextOrNull(node, "summary"));
            report.setAbilityAnalysis(getTextOrNull(node, "abilityAnalysis"));
            report.setProblemDiagnosis(getTextOrNull(node, "problemDiagnosis"));
            report.setTeachingSuggestion(getTextOrNull(node, "teachingSuggestion"));
        } catch (Exception e) {
            log.warn("AI输出非标准JSON，作为summary存储", e);
            report.setSummary(aiResult);
        }
    }

    /** 从 AI 返回文本中提取 JSON 对象：去掉代码块、取第一个 { 到最后一个 } */
    private String extractJson(String text) {
        if (text == null) return null;
        String t = stripCodeFence(text);
        int first = t.indexOf('{');
        int last = t.lastIndexOf('}');
        if (first != -1 && last != -1 && last > first) {
            return t.substring(first, last + 1);
        }
        return t;
    }

    private String stripCodeFence(String text) {
        if (text == null) {
            return null;
        }
        String t = text.trim();
        if (t.startsWith("```")) {
            int firstNewline = t.indexOf('\n');
            int lastFence = t.lastIndexOf("```");
            if (firstNewline != -1 && lastFence > firstNewline) {
                t = t.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return t;
    }

    private String getTextOrNull(JsonNode node, String field) {
        JsonNode f = node.get(field);
        return f != null ? f.asText() : null;
    }

    private String typeLabel(String reportType) {
        if ("daily".equals(reportType)) {
            return "日";
        }
        if ("weekly".equals(reportType)) {
            return "周";
        }
        return "月";
    }
}
