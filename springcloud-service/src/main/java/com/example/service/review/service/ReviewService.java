package com.example.service.review.service;

import com.example.api.dto.ReviewDailyVO;
import com.example.common.result.Result;

import java.time.LocalDate;
import java.util.List;

public interface ReviewService {
    ReviewDailyVO getTodayReview();
    ReviewDailyVO getReviewByDate(LocalDate date);
    List<ReviewDailyVO> getReviewList(int page, int size);
    Result<Void> updateProgress(Long itemId, Integer status);
    Result<Void> manualGenerate();
}