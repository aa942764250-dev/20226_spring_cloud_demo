package com.example.service.aiteacher.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.api.dto.AiReportVO;
import com.example.api.dto.ReportGenerateRequest;
import com.example.api.entity.AiReport;
import com.example.common.result.Result;
import com.example.service.aiteacher.service.AiReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI报告接口
 */
@RestController
@RequestMapping("/ai-teacher/report")
@RequiredArgsConstructor
public class AiReportController {

    private final AiReportService aiReportService;

    /** 生成AI报告 */
    @PostMapping("/generate")
    public Result<Long> generate(@RequestBody ReportGenerateRequest request) {
        if (request.getStudentId() == null) {
            return Result.fail("学生ID不能为空");
        }
        if (request.getReportType() == null || request.getReportType().isEmpty()) {
            return Result.fail("报告类型不能为空");
        }
        if (!"daily".equals(request.getReportType()) && !"weekly".equals(request.getReportType())
                && !"monthly".equals(request.getReportType())) {
            return Result.fail("报告类型只能为daily、weekly或monthly");
        }
        return aiReportService.generateReport(request);
    }

    /** 分页查询报告列表 */
    @GetMapping("/list")
    public Result<Page<AiReportVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) String reportType) {
        return Result.success(aiReportService.listReports(page, size, studentId, reportType));
    }

    /** 获取报告详情 */
    @GetMapping("/{id}")
    public Result<AiReportVO> detail(@PathVariable Long id) {
        AiReportVO vo = aiReportService.getReportDetail(id);
        if (vo == null) {
            return Result.fail(404, "报告不存在");
        }
        return Result.success(vo);
    }

    /** 审核报告（发布/驳回） */
    @PostMapping("/review")
    public Result<Void> review(@RequestBody Map<String, Object> body) {
        Long reportId = Long.valueOf(body.get("reportId").toString());
        Integer status = Integer.valueOf(body.get("status").toString());
        String reviewNote = body.get("reviewNote") != null ? body.get("reviewNote").toString() : null;
        return aiReportService.reviewReport(reportId, status, reviewNote);
    }

    /** 编辑报告内容 */
    @PutMapping
    public Result<Void> update(@RequestBody AiReport report) {
        if (report.getId() == null) {
            return Result.fail("报告ID不能为空");
        }
        return aiReportService.updateReport(report);
    }

    /** 删除报告 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return aiReportService.deleteReport(id);
    }
}
