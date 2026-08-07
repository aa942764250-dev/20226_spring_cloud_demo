package com.example.service.aiteacher.controller;

import com.example.common.result.Result;
import com.example.service.aiteacher.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 工作台首页数据看板接口（4.2）
 */
@RestController
@RequestMapping("/ai-teacher/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /** 首页概览：stats / pendingRecords / recentReports / weeklyTrend */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.success(dashboardService.overview());
    }
}
