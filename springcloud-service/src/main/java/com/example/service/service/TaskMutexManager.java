package com.example.service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 加密任务互斥控制器。
 * <p>
 * 使用 {@link AtomicBoolean} 确保同一时刻仅允许一个加密任务执行，
 * 防止并发执行导致的数据不一致问题。
 * </p>
 */
@Slf4j
@Component
public class TaskMutexManager {

    /** 任务执行状态标志，true=有任务执行中，false=无任务执行 */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 尝试获取任务执行权。
     *
     * @return true=获取成功，false=已有任务执行中
     */
    public boolean tryAcquire() {
        return running.compareAndSet(false, true);
    }

    /**
     * 释放任务执行权，允许后续任务执行。
     */
    public void release() {
        running.set(false);
    }

    /**
     * 查询当前是否有任务正在执行。
     *
     * @return true=有任务执行中，false=无任务执行
     */
    public boolean isRunning() {
        return running.get();
    }
}
