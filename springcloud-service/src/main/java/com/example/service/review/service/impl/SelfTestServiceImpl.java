package com.example.service.review.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.api.dto.SelfTestDailyVO;
import com.example.api.dto.SelfTestItemVO;
import com.example.api.dto.SelfTestReportVO;
import com.example.api.dto.WrongAnswerVO;
import com.example.api.entity.*;
import com.example.common.result.Result;
import com.example.service.dao.*;
import com.example.service.review.service.SelfTestService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SelfTestServiceImpl implements SelfTestService {

    private final ReviewDailyDao reviewDailyDao;
    private final ReviewItemDao reviewItemDao;
    private final SelfTestAnswerDao selfTestAnswerDao;
    private final WrongAnswerBookDao wrongAnswerBookDao;
    private final SelfTestReportDao selfTestReportDao;
    private final ObjectMapper objectMapper;

    @Override
    public SelfTestDailyVO getTodaySelfTest() {
        return getSelfTestByDate(LocalDate.now());
    }

    @Override
    public SelfTestDailyVO getSelfTestByDate(LocalDate date) {
        LambdaQueryWrapper<ReviewDaily> dailyWrapper = new LambdaQueryWrapper<>();
        dailyWrapper.eq(ReviewDaily::getReviewDate, date);
        ReviewDaily daily = reviewDailyDao.selectOne(dailyWrapper);
        if (daily == null) return null;

        LambdaQueryWrapper<ReviewItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(ReviewItem::getDailyId, daily.getId())
                   .ne(ReviewItem::getQuestionType, "review")
                   .orderByAsc(ReviewItem::getSortOrder);
        List<ReviewItem> testItems = reviewItemDao.selectList(itemWrapper);
        if (testItems.isEmpty()) return null;

        List<Long> itemIds = testItems.stream().map(ReviewItem::getId).collect(Collectors.toList());
        Map<Long, SelfTestAnswer> answerMap = new HashMap<>();
        if (!itemIds.isEmpty()) {
            LambdaQueryWrapper<SelfTestAnswer> answerWrapper = new LambdaQueryWrapper<>();
            answerWrapper.in(SelfTestAnswer::getItemId, itemIds)
                         .eq(SelfTestAnswer::getUserId, "default");
            List<SelfTestAnswer> answers = selfTestAnswerDao.selectList(answerWrapper);
            for (SelfTestAnswer a : answers) {
                answerMap.put(a.getItemId(), a);
            }
        }

        SelfTestDailyVO vo = new SelfTestDailyVO();
        vo.setDailyId(daily.getId());
        vo.setReviewDate(daily.getReviewDate());
        vo.setTitle(daily.getTitle());
        vo.setTotalTestCount(testItems.size());

        List<SelfTestItemVO> itemVOs = new ArrayList<>();
        int answeredCount = 0;
        int correctCount = 0;
        for (ReviewItem item : testItems) {
            SelfTestItemVO itemVO = new SelfTestItemVO();
            itemVO.setId(item.getId());
            itemVO.setModuleName(item.getModuleName());
            itemVO.setQuestion(item.getQuestion());
            itemVO.setQuestionType(item.getQuestionType());
            itemVO.setSortOrder(item.getSortOrder());
            itemVO.setSource(item.getSource());

            if (item.getOptions() != null && !item.getOptions().isEmpty()) {
                try {
                    List<String> opts = objectMapper.readValue(item.getOptions(), new TypeReference<List<String>>() {});
                    itemVO.setOptions(opts);
                } catch (Exception e) {
                    itemVO.setOptions(Collections.emptyList());
                }
            } else {
                itemVO.setOptions(Collections.emptyList());
            }

            SelfTestAnswer answer = answerMap.get(item.getId());
            if (answer != null) {
                itemVO.setUserAnswer(answer.getUserAnswer());
                itemVO.setIsCorrect(answer.getIsCorrect());
                answeredCount++;
                if (answer.getIsCorrect() != null && answer.getIsCorrect() == 1) {
                    correctCount++;
                }
            }
            itemVOs.add(itemVO);
        }
        vo.setItems(itemVOs);
        vo.setAnsweredCount(answeredCount);
        vo.setCorrectCount(correctCount);

        if (answeredCount > 0) {
            vo.setCorrectRate(BigDecimal.valueOf(correctCount * 100.0 / answeredCount).setScale(1, RoundingMode.HALF_UP));
        } else {
            vo.setCorrectRate(BigDecimal.ZERO);
        }
        if (testItems.size() > 0) {
            vo.setCoverageRate(BigDecimal.valueOf(answeredCount * 100.0 / testItems.size()).setScale(1, RoundingMode.HALF_UP));
        } else {
            vo.setCoverageRate(BigDecimal.ZERO);
        }
        vo.setScore(vo.getCorrectRate());

        return vo;
    }

    @Override
    public Result<Void> submitAnswer(Long itemId, String userAnswer) {
        ReviewItem item = reviewItemDao.selectById(itemId);
        if (item == null) {
            return Result.fail(404, "题目不存在");
        }
        if ("review".equals(item.getQuestionType())) {
            return Result.fail(400, "复习重点题不支持作答");
        }

        int isCorrect = judgeAnswer(item, userAnswer);

        LambdaQueryWrapper<SelfTestAnswer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SelfTestAnswer::getItemId, itemId)
               .eq(SelfTestAnswer::getUserId, "default");
        SelfTestAnswer existing = selfTestAnswerDao.selectOne(wrapper);

        if (existing != null) {
            existing.setUserAnswer(userAnswer);
            existing.setIsCorrect(isCorrect);
            existing.setAnsweredAt(LocalDateTime.now());
            selfTestAnswerDao.updateById(existing);
        } else {
            SelfTestAnswer answer = new SelfTestAnswer();
            answer.setItemId(itemId);
            answer.setUserId("default");
            answer.setUserAnswer(userAnswer);
            answer.setIsCorrect(isCorrect);
            answer.setAnsweredAt(LocalDateTime.now());
            selfTestAnswerDao.insert(answer);
        }

        if (isCorrect == 0) {
            addToWrongBook(item, userAnswer);
        }

        return Result.success(null);
    }

    @Override
    public SelfTestReportVO getTodayReport() {
        return generateReport(LocalDate.now());
    }

    @Override
    public SelfTestReportVO generateReport(LocalDate date) {
        LambdaQueryWrapper<ReviewDaily> dailyWrapper = new LambdaQueryWrapper<>();
        dailyWrapper.eq(ReviewDaily::getReviewDate, date);
        ReviewDaily daily = reviewDailyDao.selectOne(dailyWrapper);
        if (daily == null) return null;

        LambdaQueryWrapper<SelfTestReport> reportWrapper = new LambdaQueryWrapper<>();
        reportWrapper.eq(SelfTestReport::getDailyId, daily.getId())
                     .eq(SelfTestReport::getUserId, "default");
        SelfTestReport existing = selfTestReportDao.selectOne(reportWrapper);
        if (existing != null) {
            SelfTestReportVO vo = new SelfTestReportVO();
            vo.setDailyId(existing.getDailyId());
            vo.setTotalCount(existing.getTotalCount());
            vo.setCorrectCount(existing.getCorrectCount());
            vo.setWrongCount(existing.getWrongCount());
            vo.setSkipCount(existing.getSkipCount());
            vo.setScore(existing.getScore());
            vo.setCorrectRate(existing.getCorrectRate());
            vo.setCoverageRate(existing.getCoverageRate());
            vo.setCreatedAt(existing.getCreatedAt());
            return vo;
        }

        LambdaQueryWrapper<ReviewItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(ReviewItem::getDailyId, daily.getId())
                   .ne(ReviewItem::getQuestionType, "review");
        List<ReviewItem> testItems = reviewItemDao.selectList(itemWrapper);

        List<Long> itemIds = testItems.stream().map(ReviewItem::getId).collect(Collectors.toList());
        int correctCount = 0;
        int wrongCount = 0;
        int skipCount = 0;

        if (!itemIds.isEmpty()) {
            LambdaQueryWrapper<SelfTestAnswer> answerWrapper = new LambdaQueryWrapper<>();
            answerWrapper.in(SelfTestAnswer::getItemId, itemIds)
                         .eq(SelfTestAnswer::getUserId, "default");
            List<SelfTestAnswer> answers = selfTestAnswerDao.selectList(answerWrapper);
            Set<Long> answeredIds = new HashSet<>();
            for (SelfTestAnswer a : answers) {
                answeredIds.add(a.getItemId());
                if (a.getIsCorrect() != null && a.getIsCorrect() == 1) {
                    correctCount++;
                } else {
                    wrongCount++;
                }
            }
            skipCount = testItems.size() - answeredIds.size();
        } else {
            skipCount = testItems.size();
        }

        int total = testItems.size();
        int answered = correctCount + wrongCount;
        BigDecimal correctRate = answered > 0 ? BigDecimal.valueOf(correctCount * 100.0 / answered).setScale(1, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal coverageRate = total > 0 ? BigDecimal.valueOf(answered * 100.0 / total).setScale(1, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal score = correctRate;

        SelfTestReport report = new SelfTestReport();
        report.setDailyId(daily.getId());
        report.setUserId("default");
        report.setTotalCount(total);
        report.setCorrectCount(correctCount);
        report.setWrongCount(wrongCount);
        report.setSkipCount(skipCount);
        report.setScore(score);
        report.setCorrectRate(correctRate);
        report.setCoverageRate(coverageRate);
        selfTestReportDao.insert(report);

        SelfTestReportVO vo = new SelfTestReportVO();
        vo.setDailyId(report.getDailyId());
        vo.setTotalCount(total);
        vo.setCorrectCount(correctCount);
        vo.setWrongCount(wrongCount);
        vo.setSkipCount(skipCount);
        vo.setScore(score);
        vo.setCorrectRate(correctRate);
        vo.setCoverageRate(coverageRate);
        vo.setCreatedAt(report.getCreatedAt());
        return vo;
    }

    @Override
    public List<WrongAnswerVO> getWrongAnswerBook(int page, int size) {
        LambdaQueryWrapper<WrongAnswerBook> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WrongAnswerBook::getUserId, "default")
               .orderByDesc(WrongAnswerBook::getLastWrongAt);
        List<WrongAnswerBook> records = wrongAnswerBookDao.selectList(wrapper);

        int start = (page - 1) * size;
        int end = Math.min(start + size, records.size());
        if (start >= records.size()) return Collections.emptyList();

        return records.subList(start, end).stream().map(r -> {
            WrongAnswerVO vo = new WrongAnswerVO();
            vo.setId(r.getId());
            vo.setQuestion(r.getQuestion());
            vo.setCorrectAnswer(r.getCorrectAnswer());
            vo.setUserAnswer(r.getUserAnswer());
            vo.setModuleName(r.getModuleName());
            vo.setSource(r.getSource());
            vo.setWrongCount(r.getWrongCount());
            vo.setLastWrongAt(r.getLastWrongAt());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public Result<Void> removeFromWrongBook(Long wrongId) {
        wrongAnswerBookDao.deleteById(wrongId);
        return Result.success(null);
    }

    private int judgeAnswer(ReviewItem item, String userAnswer) {
        if (userAnswer == null || userAnswer.isEmpty()) return 0;
        String correct = item.getCorrectAnswer();
        if (correct == null || correct.isEmpty()) return 0;

        String type = item.getQuestionType();
        if ("choice".equals(type)) {
            return userAnswer.trim().equalsIgnoreCase(correct.trim()) ? 1 : 0;
        } else if ("true_false".equals(type)) {
            String ua = userAnswer.trim().toUpperCase();
            String ca = correct.trim().toUpperCase();
            if (ua.startsWith("T") || ua.startsWith("正")) ua = "T";
            else if (ua.startsWith("F") || ua.startsWith("错")) ua = "F";
            return ua.equals(ca) ? 1 : 0;
        } else if ("fill_blank".equals(type)) {
            return userAnswer.trim().equalsIgnoreCase(correct.trim()) ? 1 : 0;
        }
        return 0;
    }

    private void addToWrongBook(ReviewItem item, String userAnswer) {
        LambdaQueryWrapper<WrongAnswerBook> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WrongAnswerBook::getUserId, "default")
               .eq(WrongAnswerBook::getQuestion, item.getQuestion());
        WrongAnswerBook existing = wrongAnswerBookDao.selectOne(wrapper);

        if (existing != null) {
            existing.setWrongCount(existing.getWrongCount() + 1);
            existing.setUserAnswer(userAnswer);
            existing.setLastWrongAt(LocalDateTime.now());
            wrongAnswerBookDao.updateById(existing);
        } else {
            WrongAnswerBook book = new WrongAnswerBook();
            book.setUserId("default");
            book.setQuestion(item.getQuestion());
            book.setCorrectAnswer(item.getCorrectAnswer());
            book.setUserAnswer(userAnswer);
            book.setModuleName(item.getModuleName());
            book.setSource(item.getSource());
            book.setWrongCount(1);
            book.setLastWrongAt(LocalDateTime.now());
            book.setCreatedAt(LocalDateTime.now());
            wrongAnswerBookDao.insert(book);
        }
    }
}