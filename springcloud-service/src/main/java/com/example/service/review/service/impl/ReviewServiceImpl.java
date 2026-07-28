package com.example.service.review.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.api.dto.ReviewDailyVO;
import com.example.api.dto.ReviewItemVO;
import com.example.api.entity.ReviewDaily;
import com.example.api.entity.ReviewItem;
import com.example.api.entity.ReviewProgress;
import com.example.common.result.Result;
import com.example.service.dao.ReviewDailyDao;
import com.example.service.dao.ReviewItemDao;
import com.example.service.dao.ReviewProgressDao;
import com.example.service.review.generator.ReviewScheduleGenerator;
import com.example.service.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewDailyDao reviewDailyDao;
    private final ReviewItemDao reviewItemDao;
    private final ReviewProgressDao reviewProgressDao;
    private final ReviewScheduleGenerator reviewScheduleGenerator;

    @Override
    public ReviewDailyVO getTodayReview() {
        return getReviewByDate(LocalDate.now());
    }

    @Override
    public ReviewDailyVO getReviewByDate(LocalDate date) {
        LambdaQueryWrapper<ReviewDaily> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReviewDaily::getReviewDate, date);
        ReviewDaily daily = reviewDailyDao.selectOne(wrapper);
        if (daily == null) {
            return null;
        }
        return buildDailyVO(daily);
    }

    @Override
    public List<ReviewDailyVO> getReviewList(int page, int size) {
        Page<ReviewDaily> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ReviewDaily> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(ReviewDaily::getReviewDate);
        Page<ReviewDaily> result = reviewDailyDao.selectPage(pageParam, wrapper);
        return result.getRecords().stream()
                .map(d -> {
                    ReviewDailyVO vo = new ReviewDailyVO();
                    vo.setId(d.getId());
                    vo.setReviewDate(d.getReviewDate());
                    vo.setTitle(d.getTitle());
                    vo.setModuleCount(d.getModuleCount());
                    vo.setItemCount(d.getItemCount());
                    vo.setStatus(d.getStatus());
                    vo.setCreatedAt(d.getCreatedAt());
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Result<Void> updateProgress(Long itemId, Integer status) {
        LambdaQueryWrapper<ReviewProgress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReviewProgress::getItemId, itemId)
               .eq(ReviewProgress::getUserId, "default");
        ReviewProgress existing = reviewProgressDao.selectOne(wrapper);

        if (existing != null) {
            existing.setStatus(status);
            existing.setUpdatedAt(LocalDateTime.now());
            reviewProgressDao.updateById(existing);
        } else {
            ReviewProgress progress = new ReviewProgress();
            progress.setItemId(itemId);
            progress.setUserId("default");
            progress.setStatus(status);
            progress.setUpdatedAt(LocalDateTime.now());
            reviewProgressDao.insert(progress);
        }
        return Result.success(null);
    }

    @Override
    public Result<Void> manualGenerate() {
        try {
            reviewScheduleGenerator.doGenerate(LocalDate.now());
            return Result.success(null);
        } catch (Exception e) {
            log.error("手动生成复习内容失败", e);
            return Result.fail("生成失败: " + e.getMessage());
        }
    }

    private ReviewDailyVO buildDailyVO(ReviewDaily daily) {
        ReviewDailyVO vo = new ReviewDailyVO();
        vo.setId(daily.getId());
        vo.setReviewDate(daily.getReviewDate());
        vo.setTitle(daily.getTitle());
        vo.setModuleCount(daily.getModuleCount());
        vo.setItemCount(daily.getItemCount());
        vo.setStatus(daily.getStatus());
        vo.setCreatedAt(daily.getCreatedAt());

        LambdaQueryWrapper<ReviewItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(ReviewItem::getDailyId, daily.getId())
                   .orderByAsc(ReviewItem::getSortOrder);
        List<ReviewItem> items = reviewItemDao.selectList(itemWrapper);

        List<Long> itemIds = items.stream().map(ReviewItem::getId).collect(Collectors.toList());
        Map<Long, Integer> progressMap = new java.util.HashMap<>();
        if (!itemIds.isEmpty()) {
            LambdaQueryWrapper<ReviewProgress> progressWrapper = new LambdaQueryWrapper<>();
            progressWrapper.in(ReviewProgress::getItemId, itemIds)
                           .eq(ReviewProgress::getUserId, "default");
            List<ReviewProgress> progressList = reviewProgressDao.selectList(progressWrapper);
            for (ReviewProgress p : progressList) {
                progressMap.put(p.getItemId(), p.getStatus());
            }
        }

        List<ReviewItemVO> itemVOs = new ArrayList<>();
        for (ReviewItem item : items) {
            ReviewItemVO itemVO = new ReviewItemVO();
            itemVO.setId(item.getId());
            itemVO.setModuleName(item.getModuleName());
            itemVO.setQuestion(item.getQuestion());
            itemVO.setAnswer(item.getAnswer());
            itemVO.setSortOrder(item.getSortOrder());
            itemVO.setSource(item.getSource());
            itemVO.setProgressStatus(progressMap.getOrDefault(item.getId(), 0));
            itemVOs.add(itemVO);
        }
        vo.setItems(itemVOs);

        int mastered = (int) itemVOs.stream().filter(i -> i.getProgressStatus() == 1).count();
        vo.setMasteredCount(mastered);

        return vo;
    }
}