package com.example.service.review.controller;

import com.example.api.dto.ReviewDailyVO;
import com.example.common.result.Result;
import com.example.service.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/today")
    public Result<ReviewDailyVO> getTodayReview() {
        ReviewDailyVO vo = reviewService.getTodayReview();
        if (vo == null) {
            return Result.fail(404, "今日复习尚未生成");
        }
        return Result.success(vo);
    }

    @GetMapping("/detail")
    public Result<ReviewDailyVO> getReviewByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        ReviewDailyVO vo = reviewService.getReviewByDate(date);
        if (vo == null) {
            return Result.fail(404, "该日期无复习内容");
        }
        return Result.success(vo);
    }

    @GetMapping("/list")
    public Result<List<ReviewDailyVO>> getReviewList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(reviewService.getReviewList(page, size));
    }

    @PostMapping("/progress")
    public Result<Void> updateProgress(@RequestBody java.util.Map<String, Object> body) {
        Long itemId = Long.valueOf(body.get("itemId").toString());
        Integer status = Integer.valueOf(body.get("status").toString());
        return reviewService.updateProgress(itemId, status);
    }

    @PostMapping("/generate")
    public Result<Void> manualGenerate() {
        return reviewService.manualGenerate();
    }
}