package com.example.service.encryptlite.executor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 轻量加密任务互斥管理器。
 * <p>
 * 使用 AtomicBoolean 确保同一时刻仅允许一个加密任务执行，仅适用于单实例部署。
 * </p>
 */
@Slf4j
@Component
public class EncryptLiteMutexManager {

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
     * 释放任务执行权。
     */
    public void release() {
        running.set(false);
    }

    /**
     * 查询当前是否有任务正在执行。
     *
     * @return true=有任务执行中
     */
    public boolean isRunning() {
        return running.get();
    }
}