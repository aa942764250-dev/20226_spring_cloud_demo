package com.example.service.aiteacher.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.api.dto.AiReportVO;
import com.example.api.dto.ReportGenerateRequest;
import com.example.api.entity.AiReport;
import com.example.common.result.Result;

/**
 * AI报告服务
 */
public interface AiReportService {
    /** 生成AI报告（异步） */
    Result<Long> generateReport(ReportGenerateRequest request);
    /** 分页查询报告列表 */
    Page<AiReportVO> listReports(int page, int size, Long studentId, String reportType);
    /** 获取报告详情 */
    AiReportVO getReportDetail(Long reportId);
    /** 审核报告（发布/驳回） */
    Result<Void> reviewReport(Long reportId, Integer status, String reviewNote);
    /** 编辑报告内容 */
    Result<Void> updateReport(AiReport report);
    /** 删除报告 */
    Result<Void> deleteReport(Long reportId);
}
