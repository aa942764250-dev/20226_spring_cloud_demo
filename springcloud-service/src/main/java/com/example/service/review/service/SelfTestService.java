package com.example.service.review.service;

import com.example.api.dto.SelfTestDailyVO;
import com.example.api.dto.SelfTestReportVO;
import com.example.api.dto.WrongAnswerVO;
import com.example.common.result.Result;

import java.time.LocalDate;
import java.util.List;

public interface SelfTestService {
    SelfTestDailyVO getTodaySelfTest();
    SelfTestDailyVO getSelfTestByDate(LocalDate date);
    Result<Void> submitAnswer(Long itemId, String userAnswer);
    SelfTestReportVO getTodayReport();
    SelfTestReportVO generateReport(LocalDate date);
    List<WrongAnswerVO> getWrongAnswerBook(int page, int size);
    Result<Void> removeFromWrongBook(Long wrongId);
}