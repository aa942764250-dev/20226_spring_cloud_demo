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
        MODULE_QUERIES.put("Java基础", new String[]{"值传递与引用", "equals hashCode", "String StringBuilder", "异常体系", "泛型与反射", "Stream API"});
        MODULE_QUERIES.put("集合框架", new String[]{"HashMap原理", "ConcurrentHashMap", "ArrayList LinkedList", "TreeMap红黑树", "fail-fast"});
        MODULE_QUERIES.put("并发与锁", new String[]{"synchronized锁升级", "ReentrantLock", "volatile原理", "AQS原理", "CAS与ABA", "ThreadLocal"});
        MODULE_QUERIES.put("线程池", new String[]{"ThreadPoolExecutor参数", "线程池拒绝策略", "线程池工作原理"});
        MODULE_QUERIES.put("JVM", new String[]{"JVM内存模型", "GC算法与收集器", "类加载机制", "G1与ZGC", "OOM处理", "线上排查工具"});
        MODULE_QUERIES.put("IO与网络", new String[]{"BIO NIO AIO", "TCP三次握手", "HTTP版本演进", "epoll多路复用", "HTTPS TLS"});
        MODULE_QUERIES.put("Spring", new String[]{"IOC与Bean生命周期", "AOP代理", "循环依赖三级缓存", "Spring事务传播", "Spring Boot自动配置"});
        MODULE_QUERIES.put("MyBatis", new String[]{"MyBatis一二级缓存", "#{}与${}", "N+1问题与深分页"});
        MODULE_QUERIES.put("MySQL", new String[]{"B+Tree索引", "MVCC多版本并发", "行锁间隙锁", "事务隔离级别", "redo undo binlog", "EXPLAIN执行计划"});
        MODULE_QUERIES.put("Redis", new String[]{"Redis数据类型", "Redis持久化与集群", "缓存三大问题", "Redis分布式锁", "缓存一致性", "跳表结构"});
        MODULE_QUERIES.put("分库分表", new String[]{"分库分表方式", "全局唯一ID", "跨库JOIN与分页"});
        MODULE_QUERIES.put("消息队列", new String[]{"Kafka为什么快", "Kafka架构", "RocketMQ事务消息", "消息可靠性"});
        MODULE_QUERIES.put("微服务与分布式", new String[]{"Spring Cloud Gateway", "CAP与BASE", "分布式事务Seata", "Nacos注册中心", "Sentinel限流", "幂等处理"});
        MODULE_QUERIES.put("RPC与Dubbo", new String[]{"RPC原理", "Dubbo SPI机制", "ZAB协议与ZK选举", "Dubbo负载均衡"});
        MODULE_QUERIES.put("Elasticsearch", new String[]{"倒排索引", "ES写入流程", "BM25评分"});
        MODULE_QUERIES.put("Docker", new String[]{"Docker底层技术", "容器与虚拟机对比"});
        MODULE_QUERIES.put("数据结构与算法", new String[]{"LRU缓存实现", "一致性哈希", "二分查找"});
        MODULE_QUERIES.put("设计模式", new String[]{"单例模式", "工厂模式", "策略模式", "责任链模式", "观察者模式", "动态代理"});
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
        Set<String> seenKeys = new HashSet<>();
        int sortOrder = 0;

        for (Map.Entry<String, String[]> entry : MODULE_QUERIES.entrySet()) {
            String moduleName = entry.getKey();
            for (String query : entry.getValue()) {
                List<Map<String, String>> results = knowledgeSearchClient.search(query, 3);
                for (Map<String, String> result : results) {
                    String question = result.get("question");
                    if (question == null || question.isEmpty()) {
                        continue;
                    }
                    String dedupKey = result.getOrDefault("dedupKey", question);
                    if (seenKeys.contains(dedupKey)) {
                        continue;
                    }
                    seenKeys.add(dedupKey);
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