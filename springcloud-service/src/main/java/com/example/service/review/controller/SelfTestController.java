package com.example.service.review.controller;

import com.example.api.dto.SelfTestDailyVO;
import com.example.api.dto.SelfTestReportVO;
import com.example.api.dto.WrongAnswerVO;
import com.example.common.result.Result;
import com.example.service.review.service.SelfTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/selftest")
@RequiredArgsConstructor
public class SelfTestController {

    private final SelfTestService selfTestService;

    @GetMapping("/today")
    public Result<SelfTestDailyVO> getTodaySelfTest() {
        SelfTestDailyVO vo = selfTestService.getTodaySelfTest();
        if (vo == null) {
            return Result.fail(404, "今日自测尚未生成");
        }
        return Result.success(vo);
    }

    @GetMapping("/detail")
    public Result<SelfTestDailyVO> getSelfTestByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        SelfTestDailyVO vo = selfTestService.getSelfTestByDate(date);
        if (vo == null) {
            return Result.fail(404, "该日期无自测内容");
        }
        return Result.success(vo);
    }

    @PostMapping("/answer")
    public Result<Void> submitAnswer(@RequestBody Map<String, Object> body) {
        Long itemId = Long.valueOf(body.get("itemId").toString());
        String userAnswer = body.get("userAnswer").toString();
        return selfTestService.submitAnswer(itemId, userAnswer);
    }

    @GetMapping("/report")
    public Result<SelfTestReportVO> getTodayReport() {
        SelfTestReportVO vo = selfTestService.getTodayReport();
        if (vo == null) {
            return Result.fail(404, "今日暂无自测报告");
        }
        return Result.success(vo);
    }

    @PostMapping("/report/generate")
    public Result<SelfTestReportVO> generateReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        SelfTestReportVO vo = selfTestService.generateReport(date);
        if (vo == null) {
            return Result.fail(404, "该日期无自测数据");
        }
        return Result.success(vo);
    }

    @GetMapping("/wrong-book")
    public Result<List<WrongAnswerVO>> getWrongAnswerBook(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(selfTestService.getWrongAnswerBook(page, size));
    }

    @DeleteMapping("/wrong-book/{id}")
    public Result<Void> removeFromWrongBook(@PathVariable Long id) {
        return selfTestService.removeFromWrongBook(id);
    }
}