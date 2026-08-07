package com.example.service.aiteacher.service.impl;

import com.example.service.aiteacher.config.AiTeacherProperties;
import com.example.service.dao.AiReportDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 报告生成调度器（方案 B：DB 任务队列 + 定时扫描）。
 *
 * 取代原先"HTTP 请求内即时 @Async 提交"的触发方式：
 *  - {@code AiReportServiceImpl.generateReport} 仅把任务写成 status=4 并立即返回；
 *  - 本调度器按固定间隔扫描 status=4（及超时 status=5）的任务，通过原子 UPDATE 认领后交给
 *    {@link ReportGenerationExecutor} 的异步线程池执行。
 *
 * 防重复提交双保险：
 *   1）DB 原子认领：UPDATE ... WHERE status=4 命中行加 X 锁，多实例只有一个能认领成功；
 *   2）本进程内 submitted 集合：同一报告 id 在一个进程生命周期内只提交一次，
 *      避免因事务/复制延迟导致 SELECT 重复看到 status=4 而被反复认领（曾出现单次生成被打满 30 次）。
 *
 * 收益：
 *   1. 重启/崩溃不丢任务 —— 卡在 status=4/5 的任务会被下一轮扫描重新认领；
 *   2. 多实例安全 —— 认领 UPDATE 命中行加 X 锁，并发实例不会重复执行同一任务；
 *   3. 背压可控 —— 认领数量受工作线程池容量约束，不会瞬间打爆大模型。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportScheduler {

    private final AiReportDao aiReportDao;
    private final ReportGenerationExecutor reportGenerationExecutor;
    private final AiTeacherProperties properties;

    /** 每轮扫描上限，避免单次 SELECT 过大 */
    private static final int SCAN_LIMIT = 50;

    /** 本进程已提交生成的报告ID（进程级去重，重启后清空 → 卡死任务可被重新认领） */
    private final Set<Long> submitted = ConcurrentHashMap.newKeySet();

    /**
     * 扫描并分发待生成 / 卡死任务。
     * fixedDelay = 上一轮结束后延迟 5s 再执行，避免空转与高频扫描。
     * 整个扫描在一个事务内完成，确保认领 UPDATE 必然提交（避免事务/连接归还导致回滚而重复认领）。
     */
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void scanAndDispatch() {
        int leaseMinutes = properties.getReportLeaseMinutes();
        List<Long> claimable = aiReportDao.selectClaimableIds(SCAN_LIMIT, leaseMinutes);
        if (claimable == null || claimable.isEmpty()) {
            return;
        }

        for (Long id : claimable) {
            if (submitted.contains(id)) {
                continue; // 本进程已提交过，跳过（防重复提交）
            }
            boolean owned = aiReportDao.claimPending(id) > 0;
            if (!owned) {
                // 可能是 status=5 的卡死任务，尝试超时认领
                owned = aiReportDao.reclaimStale(id, leaseMinutes) > 0;
            }
            if (owned) {
                submitted.add(id);
                reportGenerationExecutor.executeGeneration(id);
                log.info("调度器认领报告生成任务: id={}", id);
            }
        }
    }
}
