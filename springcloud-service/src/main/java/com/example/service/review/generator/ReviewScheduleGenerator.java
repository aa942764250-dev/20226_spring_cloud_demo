package com.example.service.review.generator;

import com.example.api.entity.ReviewDaily;
import com.example.api.entity.ReviewItem;
import com.example.service.dao.ReviewDailyDao;
import com.example.service.dao.ReviewItemDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewScheduleGenerator {

    private final KnowledgeSearchClient knowledgeSearchClient;
    private final ReviewDailyDao reviewDailyDao;
    private final ReviewItemDao reviewItemDao;

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

    @Scheduled(cron = "0 0 6 * * ?")
    public void generateDailyReview() {
        log.info("开始生成每日复习重点...");
        try {
            doGenerate(LocalDate.now());
        } catch (Exception e) {
            log.error("生成每日复习重点失败", e);
        }
    }

    public void doGenerate(LocalDate date) {
        ReviewDaily existing = getExistingDaily(date);
        if (existing != null && existing.getStatus() == 1) {
            log.info("日期 {} 的复习内容已存在，跳过生成", date);
            return;
        }

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
            log.warn("未检索到任何复习题目，跳过生成");
            return;
        }

        long moduleCount = allItems.stream().map(ReviewItem::getModuleName).distinct().count();

        ReviewDaily daily = new ReviewDaily();
        daily.setReviewDate(date);
        daily.setTitle(date + " Java面试复习重点");
        daily.setModuleCount((int) moduleCount);
        daily.setItemCount(allItems.size());
        daily.setStatus(1);
        daily.setCreatedAt(LocalDateTime.now());
        daily.setUpdatedAt(LocalDateTime.now());
        reviewDailyDao.insert(daily);

        for (ReviewItem item : allItems) {
            item.setDailyId(daily.getId());
            reviewItemDao.insert(item);
        }

        log.info("生成完成: date={}, modules={}, items={}", date, moduleCount, allItems.size());
    }

    private ReviewDaily getExistingDaily(LocalDate date) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ReviewDaily> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(ReviewDaily::getReviewDate, date);
        return reviewDailyDao.selectOne(wrapper);
    }
}