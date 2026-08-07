package com.example.service.aiteacher.controller;

import com.example.common.result.Result;
import com.example.service.aiteacher.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 能力分析页数据接口（4.4）
 */
@RestController
@RequestMapping("/ai-teacher/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    /** 分析概览：levelDistribution / courseDistribution / abilityCompare / frequencyTrend */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.success(analysisService.overview());
    }
}
