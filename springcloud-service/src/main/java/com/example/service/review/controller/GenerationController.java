package com.example.service.review.controller;

import com.example.api.dto.GenerationLogVO;
import com.example.common.result.Result;
import com.example.service.review.service.impl.GenerationServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/generation")
@RequiredArgsConstructor
public class GenerationController {

    private final GenerationServiceImpl generationService;

    @PostMapping("/trigger")
    public Result<GenerationLogVO> triggerGenerate() {
        return generationService.triggerGenerate();
    }

    @GetMapping("/status")
    public Result<GenerationLogVO> getTodayStatus() {
        GenerationLogVO vo = generationService.getTodayStatus();
        if (vo == null) {
            return Result.fail(404, "今日无生成记录");
        }
        return Result.success(vo);
    }

    @GetMapping("/status/detail")
    public Result<GenerationLogVO> getStatusByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        GenerationLogVO vo = generationService.getLatestStatus(date);
        if (vo == null) {
            return Result.fail(404, "该日期无生成记录");
        }
        return Result.success(vo);
    }

    @GetMapping("/history")
    public Result<List<GenerationLogVO>> getHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(generationService.getHistory(page, size));
    }
}