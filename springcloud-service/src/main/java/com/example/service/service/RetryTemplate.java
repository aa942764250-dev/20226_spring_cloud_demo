package com.example.service.service;

import com.example.common.encrypt.EncryptErrorCode;
import com.example.common.encrypt.EncryptException;
import com.example.service.config.EncryptGlobalProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * 通用重试模板。
 * <p>
 * 提供可配置的重试次数和间隔的重试执行能力，
 * 用于数据库读写操作失败时的自动重试，重试耗尽后抛出 {@link EncryptException}。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RetryTemplate {

    private final EncryptGlobalProperties properties;

    /**
     * 带重试的执行操作（有返回值）。
     * <p>
     * 按指定次数和间隔重试，每次失败输出结构化日志，重试耗尽后抛出异常。
     * </p>
     *
     * @param action          待执行的操作
     * @param maxAttempts     最大重试次数
     * @param retryIntervalMs 重试间隔（毫秒）
     * @param actionDesc      操作描述（用于日志）
     * @param <T>             返回值类型
     * @return 操作执行结果
     * @throws EncryptException 重试耗尽后抛出 ENCRYPT_DB_WRITE_ERROR
     */
    public <T> T executeWithRetry(Supplier<T> action, int maxAttempts, long retryIntervalMs, String actionDesc) {
        Exception lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return action.get();
            } catch (Exception e) {
                lastException = e;
                log.warn("{} 执行失败, 第{}/{}次重试, 异常: {}", actionDesc, attempt, maxAttempts, e.getMessage());
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(retryIntervalMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new EncryptException(EncryptErrorCode.ENCRYPT_SERVICE_UNAVAILABLE, ie);
                    }
                }
            }
        }
        throw new EncryptException(EncryptErrorCode.ENCRYPT_DB_WRITE_ERROR,
                actionDesc + " 重试耗尽", lastException);
    }

    /**
     * 使用全局默认重试配置执行操作（有返回值）。
     *
     * @param action     待执行的操作
     * @param actionDesc 操作描述
     * @param <T>        返回值类型
     * @return 操作执行结果
     */
    public <T> T executeWithRetry(Supplier<T> action, String actionDesc) {
        return executeWithRetry(action,
                properties.getDbRetry().getMaxAttempts(),
                properties.getDbRetry().getRetryIntervalMs(),
                actionDesc);
    }

    /**
     * 带重试的执行操作（无返回值）。
     *
     * @param action          待执行的操作
     * @param maxAttempts     最大重试次数
     * @param retryIntervalMs 重试间隔（毫秒）
     * @param actionDesc      操作描述
     */
    public void executeWithRetry(Runnable action, int maxAttempts, long retryIntervalMs, String actionDesc) {
        executeWithRetry(() -> {
            action.run();
            return null;
        }, maxAttempts, retryIntervalMs, actionDesc);
    }
}
