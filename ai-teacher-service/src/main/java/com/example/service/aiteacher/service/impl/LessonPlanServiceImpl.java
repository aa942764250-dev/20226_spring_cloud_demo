package com.example.service.aiteacher.service.impl;

import com.example.api.dto.LessonPlanRequest;
import com.example.api.dto.LessonPlanResult;
import com.example.api.dto.LessonPlanSection;
import com.example.api.entity.Student;
import com.example.service.aiteacher.service.LessonPlanService;
import com.example.service.dao.StudentDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 专属教案生成：基于学生「深圳教研印象标签」确定性合成深圳本地化训练方案。
 * 不依赖外部 LLM，保证可运行；后续可替换为调用大模型生成更丰富内容。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LessonPlanServiceImpl implements LessonPlanService {

    private final StudentDao studentDao;

    /** 年级关键词 -> 深圳机考/笔试语境 */
    private static final Map<String, String> EXAM_CONTEXT = new LinkedHashMap<>();
    static {
        EXAM_CONTEXT.put("7", "深圳中考听说第一节：模仿朗读(8分) — 重点训练元音饱满度与基础语调");
        EXAM_CONTEXT.put("8", "深圳中考听说第二节：听选信息与回答问题(10分) — 重点训练口头回答的时态与完整句式");
        EXAM_CONTEXT.put("9", "深圳中考听说第三节：思维导图复述(12分) — 重点训练要点抓取与逻辑连接词");
        EXAM_CONTEXT.put("高", "广东高考英语听说(20分制) Part B 故事复述 / 读后续写与概要写作");
    }

    /** 训练维度顺序（用于归类与展示） */
    private static final String[] DIMENSIONS = { "听说", "语法", "词汇", "读写", "综合" };

    @Override
    public LessonPlanResult generate(LessonPlanRequest request) {
        Student student = request.getStudentId() != null ? studentDao.selectById(request.getStudentId()) : null;
        String name = student != null ? student.getName()
                : (request.getStudentName() != null && !request.getStudentName().trim().isEmpty() ? request.getStudentName() : "学生");
        String grade = student != null ? student.getGrade() : request.getGrade();
        List<String> impressions = request.getImpressions() == null ? Collections.emptyList() : request.getImpressions();

        LessonPlanResult result = new LessonPlanResult();
        result.setStudentName(name);
        result.setGrade(grade);
        result.setExamContext(resolveExamContext(grade));
        result.setImpressions(impressions);

        // 1) 学情印象回顾：归类到 听说/语法/词汇/读写/综合
        Map<String, List<String>> byDim = new LinkedHashMap<>();
        for (String dim : DIMENSIONS) byDim.put(dim, new ArrayList<>());
        for (String tag : impressions) byDim.get(classify(tag)).add(tag);

        StringBuilder review = new StringBuilder();
        for (String dim : DIMENSIONS) {
            List<String> tags = byDim.get(dim);
            if (!tags.isEmpty()) {
                review.append("- **").append(dim).append("**：").append(String.join("、", tags)).append("\n");
            }
        }
        if (impressions.isEmpty()) {
            review.append("- 暂无印象标签，建议先在学生档案中勾选深圳考纲学情标签后再生成精准教案。\n");
        }

        // 2) 针对性训练目标
        StringBuilder goals = new StringBuilder();
        if (!byDim.get("听说").isEmpty())
            goals.append("- 听说：针对「").append(String.join("、", byDim.get("听说")))
                .append("」，每周 2 次深圳听说机考题型专项（模仿朗读 / 听选信息 / 回答问题 / 导图复述）。\n");
        if (!byDim.get("语法").isEmpty())
            goals.append("- 语法：集中突破「").append(String.join("、", byDim.get("语法")))
                .append("」，用深圳中考/高考真题语境做句型转换与改错。\n");
        if (!byDim.get("词汇").isEmpty())
            goals.append("- 词汇：围绕「").append(String.join("、", byDim.get("词汇")))
                .append("」，用主题词网 + 熟词生义卡片扩充深圳考纲核心词。\n");
        if (!byDim.get("读写").isEmpty())
            goals.append("- 读写：针对「").append(String.join("、", byDim.get("读写")))
                .append("」，强化读后续写 / 概要写作的同义替换与逻辑衔接。\n");
        if (goals.length() == 0)
            goals.append("- 暂未识别薄弱维度，按深圳中高考标准进度推进，并在首次课后补充印象标签。\n");

        // 3) 课堂活动设计（结合深圳题型）
        StringBuilder activities = new StringBuilder();
        activities.append("- 热身（5min）：").append(warmup(grade)).append("\n");
        activities.append("- 核心训练（25min）：").append(coreActivity(grade, byDim)).append("\n");
        activities.append("- 产出与反馈（10min）：学生完成一段深圳机考同构的口头/书面产出，教师按 rubric 即时点评。\n");

        // 4) 作业与评估
        StringBuilder homework = new StringBuilder();
        homework.append("- 作业：录制 1 段 ").append(speakTask(grade))
            .append(" 音频 + 完成 1 篇对应薄弱项的微写作。\n");
        homework.append("- 评估 Rubric（每项 1-5 分）：发音准确度 / 句式完整度 / 逻辑连贯性 / 词汇丰富度 / 时间控制。\n");
        if (request.getNote() != null && !request.getNote().trim().isEmpty()) {
            homework.append("- 教师补充要求：").append(request.getNote()).append("\n");
        }

        List<LessonPlanSection> sections = new ArrayList<>();
        sections.add(section("📋 学情印象回顾", review.toString()));
        sections.add(section("🎯 针对性训练目标", goals.toString()));
        sections.add(section("🧩 课堂活动设计（深圳本地化）", activities.toString()));
        sections.add(section("📝 作业与评估", homework.toString()));
        result.setSections(sections);
        return result;
    }

    private LessonPlanSection section(String title, String content) {
        LessonPlanSection s = new LessonPlanSection();
        s.setTitle(title);
        s.setContent(content.trim());
        return s;
    }

    private String resolveExamContext(String grade) {
        if (grade == null) return "深圳中高考英语（请在学生档案补全年级以匹配具体听说题型）";
        if (grade.contains("初三") || grade.contains("九年级") || grade.contains("9")) return EXAM_CONTEXT.get("9");
        if (grade.contains("初二") || grade.contains("八年级") || grade.contains("8")) return EXAM_CONTEXT.get("8");
        if (grade.contains("初一") || grade.contains("七年级") || grade.contains("7")) return EXAM_CONTEXT.get("7");
        if (grade.contains("高一") || grade.contains("高二") || grade.contains("高三") || grade.contains("高中") || grade.contains("高"))
            return EXAM_CONTEXT.get("高");
        return "深圳中考英语（未精确匹配年级，按中考标准设计）";
    }

    private String classify(String tag) {
        if (tag.contains("发音") || tag.contains("朗读") || tag.contains("重音") || tag.contains("降调") ||
            tag.contains("回答") || tag.contains("复述") || tag.contains("停顿") || tag.contains("流利") ||
            tag.contains("语调") || tag.contains("听") || tag.contains("口语")) return "听说";
        if (tag.contains("时态") || tag.contains("语态") || tag.contains("从句") || tag.contains("定语") ||
            tag.contains("宾语") || tag.contains("状语") || tag.contains("语序") || tag.contains("be动词") ||
            tag.contains("语法")) return "语法";
        if (tag.contains("词汇") || tag.contains("词") || tag.contains("熟词生义") || tag.contains("一词多义")) return "词汇";
        if (tag.contains("写作") || tag.contains("读后") || tag.contains("概要") || tag.contains("summary") ||
            tag.contains("续写") || tag.contains("阅读") || tag.contains("写")) return "读写";
        return "综合";
    }

    private String warmup(String grade) {
        if (grade != null && grade.contains("高")) return "高考听力速记 + 故事主线预测";
        return "深圳中考听说模仿朗读跟读，纠正元音与重音";
    }

    private String coreActivity(String grade, Map<String, List<String>> byDim) {
        if (grade != null && grade.contains("高")) {
            return "高考故事复述（Part B）骨架搭建 + 读后续写微技能（动作/心理描写）；结合印象标签做句型升级";
        }
        boolean any = DIMENSIONS.length > 0 &&
            (streamNotEmpty(byDim.get("听说")) || streamNotEmpty(byDim.get("语法")) ||
             streamNotEmpty(byDim.get("词汇")) || streamNotEmpty(byDim.get("读写")));
        if (!any) return "深圳中考听说全题型轮训 + 语法易错点诊断";
        StringBuilder sb = new StringBuilder();
        if (streamNotEmpty(byDim.get("听说"))) sb.append("听选信息+口头回答/导图复述专项；");
        if (streamNotEmpty(byDim.get("语法"))) sb.append("薄弱语法点真题句型转换；");
        if (streamNotEmpty(byDim.get("词汇"))) sb.append("主题词汇网与熟词生义卡片；");
        if (streamNotEmpty(byDim.get("读写"))) sb.append("概要写作同义替换训练；");
        String s = sb.toString();
        return s.endsWith("；") ? s.substring(0, s.length() - 1) : s;
    }

    private boolean streamNotEmpty(List<String> l) {
        return l != null && !l.isEmpty();
    }

    private String speakTask(String grade) {
        if (grade != null && grade.contains("高")) return "高考故事复述";
        return "深圳中考听说导图复述";
    }
}
