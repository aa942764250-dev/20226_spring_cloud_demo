package com.example.service.aiteacher.service;

import com.example.service.dao.AiReportDao;
import com.example.service.dao.LearningRecordDao;
import com.example.service.dao.StudentDao;

import java.util.Map;

/**
 * 工作台首页数据看板（4.2）
 * 返回结构与前端 mock/teacher.ts 的 dashboard.overview 完全一致。
 */
public interface DashboardService {
    /** 首页概览：stats / pendingRecords / recentReports / weeklyTrend */
    Map<String, Object> overview();
}
