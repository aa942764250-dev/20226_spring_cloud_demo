package com.example.service.review.generator;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.api.entity.GenerationLog;
import com.example.api.entity.ReviewDaily;
import com.example.api.entity.ReviewItem;
import com.example.service.dao.GenerationLogDao;
import com.example.service.dao.ReviewDailyDao;
import com.example.service.dao.ReviewItemDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewScheduleGenerator {

    private final GenerationAsyncExecutor asyncExecutor;
    private final ReviewDailyDao reviewDailyDao;
    private final GenerationLogDao generationLogDao;
    private final ReviewItemDao reviewItemDao;
    private final SelfTestQuestionGenerator selfTestQuestionGenerator;

    @Scheduled(cron = "0 0 6 * * ?")
    public void generateDailyReview() {
        log.info("[定时] 开始生成每日复习重点...");
        triggerGenerate(LocalDate.now(), "scheduled");
    }

    public GenerationLog triggerGenerate(LocalDate date, String type) {
        GenerationLog runningLog = findRunningLog(date);
        if (runningLog != null) {
            log.info("日期 {} 已有正在运行的生成任务(id={})，跳过", date, runningLog.getId());
            return runningLog;
        }

        ReviewDaily existing = getExistingDaily(date);
        if (existing != null && existing.getStatus() == 1) {
            long testCount = countTestItems(existing.getId());
            if (testCount > 0) {
                log.info("日期 {} 的复习内容已存在且含自测题，跳过生成", date);
                GenerationLog logEntity = new GenerationLog();
                logEntity.setTargetDate(date);
                logEntity.setType(type);
                logEntity.setStatus("skipped");
                logEntity.setReviewItemCount(existing.getItemCount());
                logEntity.setModuleCount(existing.getModuleCount());
                logEntity.setStartedAt(LocalDateTime.now());
                logEntity.setFinishedAt(LocalDateTime.now());
                logEntity.setDurationMs(0L);
                generationLogDao.insert(logEntity);
                return logEntity;
            }
            log.info("日期 {} 的复习内容存在但缺少自测题，补生成自测题", date);
            GenerationLog logEntity = new GenerationLog();
            logEntity.setTargetDate(date);
            logEntity.setType(type);
            logEntity.setStatus("running");
            logEntity.setStartedAt(LocalDateTime.now());
            generationLogDao.insert(logEntity);
            asyncExecutor.executeSupplementTest(date, existing.getId(), logEntity.getId());
            return logEntity;
        }

        GenerationLog logEntity = new GenerationLog();
        logEntity.setTargetDate(date);
        logEntity.setType(type);
        logEntity.setStatus("running");
        logEntity.setStartedAt(LocalDateTime.now());
        generationLogDao.insert(logEntity);

        asyncExecutor.executeAsync(date, logEntity.getId());
        return logEntity;
    }

    public GenerationLog findRunningLog(LocalDate date) {
        LambdaQueryWrapper<GenerationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GenerationLog::getTargetDate, date)
               .eq(GenerationLog::getStatus, "running")
               .orderByDesc(GenerationLog::getStartedAt)
               .last("LIMIT 1");
        return generationLogDao.selectOne(wrapper);
    }

    private ReviewDaily getExistingDaily(LocalDate date) {
        LambdaQueryWrapper<ReviewDaily> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReviewDaily::getReviewDate, date);
        return reviewDailyDao.selectOne(wrapper);
    }

    private long countTestItems(Long dailyId) {
        LambdaQueryWrapper<ReviewItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReviewItem::getDailyId, dailyId)
               .ne(ReviewItem::getQuestionType, "review");
        return reviewItemDao.selectCount(wrapper);
    }
}
