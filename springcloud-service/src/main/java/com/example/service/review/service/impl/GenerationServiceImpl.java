package com.example.service.review.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.api.dto.GenerationLogVO;
import com.example.api.entity.GenerationLog;
import com.example.common.result.Result;
import com.example.service.dao.GenerationLogDao;
import com.example.service.review.generator.ReviewScheduleGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationServiceImpl {

    private final GenerationLogDao generationLogDao;
    private final ReviewScheduleGenerator reviewScheduleGenerator;

    public Result<GenerationLogVO> triggerGenerate() {
        LocalDate today = LocalDate.now();
        GenerationLog running = reviewScheduleGenerator.findRunningLog(today);
        if (running != null) {
            return Result.fail(409, "今日生成任务正在执行中(id=" + running.getId() + ")，请勿重复触发");
        }

        GenerationLog logEntity = reviewScheduleGenerator.triggerGenerate(today, "manual");
        if ("skipped".equals(logEntity.getStatus())) {
            return Result.fail(409, "今日复习内容已存在，无需重复生成");
        }
        return Result.success(toVO(logEntity));
    }

    public GenerationLogVO getTodayStatus() {
        return getLatestStatus(LocalDate.now());
    }

    public GenerationLogVO getLatestStatus(LocalDate date) {
        LambdaQueryWrapper<GenerationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GenerationLog::getTargetDate, date)
               .orderByDesc(GenerationLog::getStartedAt)
               .last("LIMIT 1");
        GenerationLog logEntity = generationLogDao.selectOne(wrapper);
        return logEntity != null ? toVO(logEntity) : null;
    }

    public List<GenerationLogVO> getHistory(int page, int size) {
        LambdaQueryWrapper<GenerationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(GenerationLog::getStartedAt);
        List<GenerationLog> all = generationLogDao.selectList(wrapper);
        int start = (page - 1) * size;
        int end = Math.min(start + size, all.size());
        if (start >= all.size()) return Collections.emptyList();
        return all.subList(start, end).stream().map(this::toVO).collect(Collectors.toList());
    }

    private GenerationLogVO toVO(GenerationLog e) {
        GenerationLogVO vo = new GenerationLogVO();
        vo.setId(e.getId());
        vo.setTargetDate(e.getTargetDate());
        vo.setType(e.getType());
        vo.setStatus(e.getStatus());
        vo.setReviewItemCount(e.getReviewItemCount());
        vo.setTestItemCount(e.getTestItemCount());
        vo.setModuleCount(e.getModuleCount());
        vo.setErrorMessage(e.getErrorMessage());
        vo.setDurationMs(e.getDurationMs());
        vo.setStartedAt(e.getStartedAt());
        vo.setFinishedAt(e.getFinishedAt());
        return vo;
    }
}