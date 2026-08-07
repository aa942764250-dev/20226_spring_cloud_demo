package com.example.service.aiteacher.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步执行器配置：报告生成走独立有界线程池，
 * 避免批量生成时瞬间打爆 K2（K2 单条需 45~60s）。
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("reportAsyncExecutor")
    public Executor reportAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(3);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("report-gen-");
        // 队列满时由调用线程同步执行，保证任务不丢（最坏情况阻塞提交方，不会丢报告）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
