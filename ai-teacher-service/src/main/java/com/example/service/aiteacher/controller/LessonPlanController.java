package com.example.service.aiteacher.controller;

import com.example.api.dto.LessonPlanRequest;
import com.example.api.dto.LessonPlanResult;
import com.example.common.result.Result;
import com.example.service.aiteacher.service.LessonPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 专属教案生成接口：基于学生「深圳教研印象标签」生成本地化训练方案
 */
@RestController
@RequestMapping("/ai-teacher/lesson-plan")
@RequiredArgsConstructor
public class LessonPlanController {

    private final LessonPlanService lessonPlanService;

    /** 生成专属教案（确定性合成，无需 LLM 即可返回） */
    @PostMapping("/generate")
    public Result<LessonPlanResult> generate(@RequestBody LessonPlanRequest request) {
        if (request.getStudentId() == null && (request.getStudentName() == null || request.getStudentName().trim().isEmpty())) {
            return Result.fail("请提供学生ID或学生姓名");
        }
        return Result.success(lessonPlanService.generate(request));
    }
}
