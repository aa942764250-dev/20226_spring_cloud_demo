package com.example.service.aiteacher.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.api.dto.AiReportVO;
import com.example.api.dto.ReportGenerateRequest;
import com.example.api.entity.*;
import com.example.common.result.Result;
import com.example.service.aiteacher.config.AiTeacherProperties;
import com.example.service.aiteacher.config.LlmProviderMeta;
import com.example.service.dao.*;
import com.example.service.aiteacher.service.AiReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiReportServiceImpl implements AiReportService {

    private final AiReportDao aiReportDao;
    private final LearningRecordDao learningRecordDao;
    private final AbilityRecordDao abilityRecordDao;
    private final PromptTemplateDao promptTemplateDao;
    private final StudentDao studentDao;
    private final AiModelConfigDao modelConfigDao;
    private final AiTeacherProperties properties;

    /** 报告状态：0=草稿/占位，1=待审核，2=已发布，3=已驳回，4=待生成（已写入待调度），5=已认领执行中 */
    private static final int STATUS_GENERATING = 4;

    @Override
    public Result<Long> generateReport(ReportGenerateRequest request) {
        Long studentId = request.getStudentId();
        String reportType = request.getReportType();

        // 计算日期范围
        LocalDate endDate = LocalDate.now().minusDays(1);
        LocalDate startDate;
        if ("daily".equals(reportType)) {
            startDate = endDate; // 日报：昨天一天
        } else if ("weekly".equals(reportType)) {
            startDate = endDate.minusDays(6);
        } else {
            startDate = endDate.minusDays(29);
        }
        if (request.getStartDate() != null && !request.getStartDate().isEmpty()) {
            startDate = LocalDate.parse(request.getStartDate());
        }
        if (request.getEndDate() != null && !request.getEndDate().isEmpty()) {
            endDate = LocalDate.parse(request.getEndDate());
        }

        // 防重校验：同一学生 + 同一报告类型 + 同一日期范围，取最新版本
        LambdaQueryWrapper<AiReport> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(AiReport::getStudentId, studentId)
                .eq(AiReport::getReportType, reportType)
                .eq(AiReport::getStartDate, startDate)
                .eq(AiReport::getEndDate, endDate)
                .orderByDesc(AiReport::getVersion);
        List<AiReport> existReports = aiReportDao.selectList(existWrapper);
        int nextVersion = 1;
        if (!existReports.isEmpty()) {
            AiReport latest = existReports.get(0);
            nextVersion = latest.getVersion() + 1;
            int st = latest.getStatus();
            if (st == 4 || st == 5) {
                return Result.fail("该周期报告正在生成中，请稍候再试");
            }
            if (st == 1) {
                return Result.fail("该周期报告待审核中，无需重复生成");
            }
            // 已发布(2)→生成新版本；草稿(0)/驳回(3)→允许重新生成
        }

        // 查询学习记录
        LambdaQueryWrapper<LearningRecord> lrWrapper = new LambdaQueryWrapper<>();
        lrWrapper.eq(LearningRecord::getStudentId, studentId)
                .between(LearningRecord::getLessonDate, startDate, endDate)
                .orderByAsc(LearningRecord::getLessonDate);
        List<LearningRecord> records = learningRecordDao.selectList(lrWrapper);

        if (records.isEmpty()) {
            // 本周期无学习记录：直接返回失败，不生成空报告，避免列表出现大量无数据占位
            Student student = studentDao.selectById(studentId);
            String name = student != null ? student.getName() : "该学生";
            log.info("学生无学习记录，跳过生成: studentId={}, name={}", studentId, name);
            return Result.fail(name + "在该周期内无学习记录，请先录入后再生成");
        }

        // 有数据：读取 Prompt 模板与模型配置，写入"生成中"记录后异步生成
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

        AiModelConfig cfg = modelConfigDao.selectList(
                        new LambdaQueryWrapper<AiModelConfig>().eq(AiModelConfig::getEnabled, 1).last("LIMIT 1"))
                .stream().findFirst().orElse(null);
        String model = cfg != null ? cfg.getModelName() : null;

        Student student = studentDao.selectById(studentId);
        AiReport report = new AiReport();
        report.setTeacherId(properties.getDefaultTeacherId());
        report.setStudentId(studentId);
        report.setReportType(reportType);
        report.setStartDate(startDate);
        report.setEndDate(endDate);
        report.setTitle(String.format("%s %s报告", student != null ? student.getName() : "学生",
                typeLabel(reportType)));
        report.setStatus(STATUS_GENERATING); // 生成中
        report.setVersion(nextVersion);
        report.setModelName(model != null ? model : properties.getGeminiModel());
        if (template != null) {
            report.setPromptTemplateId(template.getId());
        }
        report.setCreatedAt(LocalDateTime.now());
        report.setUpdatedAt(LocalDateTime.now());
        aiReportDao.insert(report);

        // 写入"待生成"(status=4) 后立即返回，生成由 ReportScheduler 定时扫描认领后在异步线程执行。
        // 这样重启/崩溃不会丢任务，且多实例部署也不会重复生成。
        log.info("报告已写入待生成队列: id={}, studentId={}", report.getId(), studentId);
        return Result.success(report.getId());
    }

    @Override
    public Page<AiReportVO> listReports(int page, int size, Long studentId, String reportType) {
        Page<AiReport> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<AiReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiReport::getTeacherId, properties.getDefaultTeacherId());
        if (studentId != null) {
            wrapper.eq(AiReport::getStudentId, studentId);
        }
        if (reportType != null && !reportType.isEmpty()) {
            wrapper.eq(AiReport::getReportType, reportType);
        }
        wrapper.orderByDesc(AiReport::getCreatedAt);
        Page<AiReport> result = aiReportDao.selectPage(pageParam, wrapper);

        Page<AiReportVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public AiReportVO getReportDetail(Long reportId) {
        AiReport report = aiReportDao.selectById(reportId);
        return report != null ? toVO(report) : null;
    }

    @Override
    public Result<Void> reviewReport(Long reportId, Integer status, String reviewNote) {
        if (status == null || (status != 2 && status != 3)) {
            return Result.fail("审核状态只能为2(已发布)或3(已驳回)");
        }
        AiReport report = aiReportDao.selectById(reportId);
        if (report == null) {
            return Result.fail(404, "报告不存在");
        }
        if (report.getStatus() != 1) {
            return Result.fail("只有待审核状态的报告才能进行审核操作");
        }
        report.setStatus(status);
        report.setReviewNote(reviewNote);
        report.setUpdatedAt(LocalDateTime.now());
        aiReportDao.updateById(report);
        return Result.success(null);
    }

    @Override
    public Result<Void> updateReport(AiReport report) {
        AiReport existing = aiReportDao.selectById(report.getId());
        if (existing != null && existing.getStatus() == 3) {
            // 已驳回的报告编辑后自动回到待审核
            report.setStatus(1);
        }
        report.setUpdatedAt(LocalDateTime.now());
        aiReportDao.updateById(report);
        return Result.success(null);
    }

    @Override
    public Result<Void> deleteReport(Long reportId) {
        aiReportDao.deleteById(reportId);
        return Result.success(null);
    }

    /** 报告类型中文标签：daily→日 / weekly→周 / 其余→月 */
    private String typeLabel(String reportType) {
        if ("daily".equals(reportType)) {
            return "日";
        }
        if ("weekly".equals(reportType)) {
            return "周";
        }
        return "月";
    }

    private AiReportVO toVO(AiReport r) {
        AiReportVO vo = new AiReportVO();
        vo.setId(r.getId());
        vo.setStudentId(r.getStudentId());
        vo.setReportType(r.getReportType());
        vo.setStartDate(r.getStartDate());
        vo.setEndDate(r.getEndDate());
        vo.setTitle(r.getTitle());
        vo.setSummary(r.getSummary());
        vo.setAbilityAnalysis(r.getAbilityAnalysis());
        vo.setProblemDiagnosis(r.getProblemDiagnosis());
        vo.setTeachingSuggestion(r.getTeachingSuggestion());
        vo.setFullContent(r.getFullContent());
        vo.setStatus(r.getStatus());
        vo.setVersion(r.getVersion());
        vo.setModelName(r.getModelName());
        vo.setTokenUsage(r.getTokenUsage());
        vo.setReviewNote(r.getReviewNote());
        vo.setCreatedAt(r.getCreatedAt());

        Student student = studentDao.selectById(r.getStudentId());
        if (student != null) {
            vo.setStudentName(student.getName());
        }
        return vo;
    }
}
