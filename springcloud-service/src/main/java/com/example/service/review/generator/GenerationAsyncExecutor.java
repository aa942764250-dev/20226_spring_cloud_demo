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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class GenerationAsyncExecutor {

    private final KnowledgeSearchClient knowledgeSearchClient;
    private final ReviewDailyDao reviewDailyDao;
    private final ReviewItemDao reviewItemDao;
    private final SelfTestQuestionGenerator selfTestQuestionGenerator;
    private final GenerationLogDao generationLogDao;

    private static final LinkedHashMap<String, String[]> MODULE_QUERIES = new LinkedHashMap<>();
    static {
        MODULE_QUERIES.put("Java基础", new String[]{"Java基本类型", "String StringBuilder", "final关键字"});
        MODULE_QUERIES.put("集合框架", new String[]{"HashMap原理", "ArrayList LinkedList", "ConcurrentHashMap"});
        MODULE_QUERIES.put("多线程与锁", new String[]{"synchronized原理", "ReentrantLock", "volatile关键字"});
        MODULE_QUERIES.put("线程池", new String[]{"ThreadPoolExecutor参数", "线程池拒绝策略", "线程池工作原理"});
        MODULE_QUERIES.put("JVM", new String[]{"JVM内存模型", "GC算法", "类加载机制"});
        MODULE_QUERIES.put("Spring", new String[]{"Spring IOC原理", "Spring AOP", "Spring Bean生命周期"});
        MODULE_QUERIES.put("MySQL", new String[]{"MySQL索引原理", "事务隔离级别", "InnoDB存储引擎"});
        MODULE_QUERIES.put("Redis", new String[]{"Redis数据类型", "Redis持久化", "Redis集群"});
        MODULE_QUERIES.put("设计模式", new String[]{"单例模式", "工厂模式", "策略模式"});
    }

    @Async
    public void executeAsync(LocalDate date, Long logId) {
        long startMs = System.currentTimeMillis();
        GenerationLog logEntity = generationLogDao.selectById(logId);
        try {
            int[] counts = doGenerate(date);
            long duration = System.currentTimeMillis() - startMs;
            logEntity.setStatus("success");
            logEntity.setReviewItemCount(counts[0]);
            logEntity.setTestItemCount(counts[1]);
            logEntity.setModuleCount(counts[2]);
            logEntity.setDurationMs(duration);
            logEntity.setFinishedAt(LocalDateTime.now());
            generationLogDao.updateById(logEntity);
            log.info("生成完成: date={}, review={}, test={}, modules={}, duration={}ms", date, counts[0], counts[1], counts[2], duration);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startMs;
            logEntity.setStatus("failed");
            logEntity.setErrorMessage(e.getMessage() != null && e.getMessage().length() > 500 ? e.getMessage().substring(0, 500) : e.getMessage());
            logEntity.setDurationMs(duration);
            logEntity.setFinishedAt(LocalDateTime.now());
            generationLogDao.updateById(logEntity);
            log.error("生成失败: date={}, duration={}ms", date, duration, e);
        }
    }

    @Async
    public void executeSupplementTest(LocalDate date, Long dailyId, Long logId) {
        long startMs = System.currentTimeMillis();
        GenerationLog logEntity = generationLogDao.selectById(logId);
        log.info("补生成自测题: date={}, dailyId={}", date, dailyId);
        try {
            List<ReviewItem> testItems = selfTestQuestionGenerator.generateTestItems(dailyId);
            for (ReviewItem item : testItems) {
                reviewItemDao.insert(item);
            }
            ReviewDaily daily = reviewDailyDao.selectById(dailyId);
            int reviewCount = 0;
            if (daily != null) {
                LambdaQueryWrapper<ReviewItem> reviewWrapper = new LambdaQueryWrapper<>();
                reviewWrapper.eq(ReviewItem::getDailyId, dailyId).eq(ReviewItem::getQuestionType, "review");
                reviewCount = reviewItemDao.selectCount(reviewWrapper).intValue();
                LambdaQueryWrapper<ReviewItem> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(ReviewItem::getDailyId, dailyId);
                daily.setItemCount(reviewItemDao.selectCount(wrapper).intValue());
                daily.setUpdatedAt(LocalDateTime.now());
                reviewDailyDao.updateById(daily);
            }
            long duration = System.currentTimeMillis() - startMs;
            logEntity.setStatus("success");
            logEntity.setReviewItemCount(reviewCount);
            logEntity.setTestItemCount(testItems.size());
            logEntity.setModuleCount(daily != null ? daily.getModuleCount() : 0);
            logEntity.setDurationMs(duration);
            logEntity.setFinishedAt(LocalDateTime.now());
            generationLogDao.updateById(logEntity);
            log.info("补生成自测题完成: dailyId={}, testCount={}, duration={}ms", dailyId, testItems.size(), duration);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startMs;
            logEntity.setStatus("failed");
            logEntity.setErrorMessage(e.getMessage() != null && e.getMessage().length() > 500 ? e.getMessage().substring(0, 500) : e.getMessage());
            logEntity.setDurationMs(duration);
            logEntity.setFinishedAt(LocalDateTime.now());
            generationLogDao.updateById(logEntity);
            log.error("补生成自测题失败: dailyId={}, duration={}ms", dailyId, duration, e);
        }
    }

    int[] doGenerate(LocalDate date) {
        List<ReviewItem> allItems = new ArrayList<>();
        Set<String> seenQuestions = new HashSet<>();
        int sortOrder = 0;

        for (Map.Entry<String, String[]> entry : MODULE_QUERIES.entrySet()) {
            String moduleName = entry.getKey();
            for (String query : entry.getValue()) {
                List<Map<String, String>> results = knowledgeSearchClient.search(query, 3);
                for (Map<String, String> result : results) {
                    String question = result.get("question");
                    if (question == null || question.isEmpty() || seenQuestions.contains(question)) {
                        continue;
                    }
                    seenQuestions.add(question);
                    ReviewItem item = new ReviewItem();
                    item.setModuleName(moduleName);
                    item.setQuestion(question);
                    item.setAnswer(result.getOrDefault("answer", ""));
                    item.setSource(result.getOrDefault("source", ""));
                    item.setSortOrder(sortOrder++);
                    item.setCreatedAt(LocalDateTime.now());
                    allItems.add(item);
                }
            }
        }

        if (allItems.isEmpty()) {
            throw new RuntimeException("未检索到任何复习题目");
        }

        int moduleCount = (int) allItems.stream().map(ReviewItem::getModuleName).distinct().count();

        ReviewDaily daily = new ReviewDaily();
        daily.setReviewDate(date);
        daily.setTitle(date + " Java面试复习重点");
        daily.setModuleCount(moduleCount);
        daily.setItemCount(allItems.size());
        daily.setStatus(1);
        daily.setCreatedAt(LocalDateTime.now());
        daily.setUpdatedAt(LocalDateTime.now());
        reviewDailyDao.insert(daily);

        for (ReviewItem item : allItems) {
            item.setDailyId(daily.getId());
            item.setQuestionType("review");
            reviewItemDao.insert(item);
        }

        List<ReviewItem> testItems = selfTestQuestionGenerator.generateTestItems(daily.getId());
        for (ReviewItem item : testItems) {
            reviewItemDao.insert(item);
        }

        daily.setItemCount(allItems.size() + testItems.size());
        daily.setUpdatedAt(LocalDateTime.now());
        reviewDailyDao.updateById(daily);

        return new int[]{allItems.size(), testItems.size(), moduleCount};
    }
}