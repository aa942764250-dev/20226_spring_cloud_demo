package com.example.service.aiteacher.service;

import com.example.api.dto.LessonPlanRequest;
import com.example.api.dto.LessonPlanResult;

/**
 * 专属教案生成服务
 */
public interface LessonPlanService {
    LessonPlanResult generate(LessonPlanRequest request);
}
