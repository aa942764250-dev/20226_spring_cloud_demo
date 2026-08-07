package com.example.service.aiteacher.service;

import java.util.Map;

/**
 * 能力分析页数据（4.4）
 * 返回结构与前端 mock/teacher.ts 的 analysis.overview 完全一致。
 */
public interface AnalysisService {
    /** 分析概览：levelDistribution / courseDistribution / abilityCompare / frequencyTrend */
    Map<String, Object> overview();
}
