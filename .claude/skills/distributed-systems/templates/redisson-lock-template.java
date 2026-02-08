/**
 * Redisson 分布式锁模板
 *
 * 使用 Redisson 实现分布式定时任务防重执行
 *
 * 依赖：
 * <dependency>
 *   <groupId>org.redisson</groupId>
 *   <artifactId>redisson-spring-boot-starter</artifactId>
 *   <version>3.25.0</version>
 * </dependency>
 */

package com.example.template;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redisson 分布式锁服务
 *
 * 提供统一的分布式锁操作封装
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedissonLockTemplate {

    private final RedissonClient redissonClient;

    /**
     * 执行带锁的任务（无返回值）
     *
     * @param lockKey    锁的 key
     * @param waitTime   等待时间
     * @param leaseTime  锁的超时时间
     * @param unit       时间单位
     * @param task       要执行的任务
     * @return true-执行成功，false-获取锁失败
     */
    public boolean executeWithLock(
        String lockKey,
        long waitTime,
        long leaseTime,
        TimeUnit unit,
        Runnable task
    ) {
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(waitTime, leaseTime, unit);

            if (!acquired) {
                log.debug("获取锁失败: {}", lockKey);
                return false;
            }

            long startTime = System.currentTimeMillis();
            try {
                task.run();

                long duration = System.currentTimeMillis() - startTime;
                log.debug("任务执行成功，锁: {}, 耗时: {}ms", lockKey, duration);
                return true;

            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    log.debug("锁已释放: {}", lockKey);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("任务执行被中断: {}", lockKey, e);
            return false;
        }
    }

    /**
     * 执行带锁的任务（有返回值）
     *
     * @param lockKey    锁的 key
     * @param waitTime   等待时间
     * @param leaseTime  锁的超时时间
     * @param unit       时间单位
     * @param task       要执行的任务
     * @param <T>        返回值类型
     * @return Optional 包装的返回值
     */
    public <T> java.util.Optional<T> executeWithLock(
        String lockKey,
        long waitTime,
        long leaseTime,
        TimeUnit unit,
        Supplier<T> task
    ) {
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(waitTime, leaseTime, unit);

            if (!acquired) {
                log.debug("获取锁失败: {}", lockKey);
                return java.util.Optional.empty();
            }

            long startTime = System.currentTimeMillis();
            try {
                T result = task.get();

                long duration = System.currentTimeMillis() - startTime;
                log.debug("任务执行成功，锁: {}, 耗时: {}ms", lockKey, duration);
                return java.util.Optional.of(result);

            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    log.debug("锁已释放: {}", lockKey);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("任务执行被中断: {}", lockKey, e);
            return java.util.Optional.empty();
        }
    }

    /**
     * 执行带看门狗的任务（自动续期）
     *
     * @param lockKey  锁的 key
     * @param task     要执行的任务
     * @return true-执行成功
     */
    public boolean executeWithWatchdog(String lockKey, Runnable task) {
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 不指定 leaseTime，启用看门狗
            lock.lock();

            long startTime = System.currentTimeMillis();
            try {
                task.run();

                long duration = System.currentTimeMillis() - startTime;
                log.debug("任务执行成功（看门狗），锁: {}, 耗时: {}ms", lockKey, duration);
                return true;

            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("任务执行被中断: {}", lockKey, e);
            return false;
        }
    }
}

/**
 * 使用示例：定时任务防重执行
 */
@Component
@RequiredArgsConstructor
@Slf4j
class ScheduledTaskExample {

    private final RedissonLockTemplate lockTemplate;

    /**
     * 示例1：基础用法
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void basicExample() {
        boolean success = lockTemplate.executeWithLock(
            "task:basic:example",  // 锁的 key
            0,                      // 等待时间：0（不等待）
            10,                     // 锁超时：10分钟
            TimeUnit.MINUTES,       // 时间单位
            () -> {
                log.info("执行任务");
                doTask();
            }
        );

        if (!success) {
            log.info("任务已在其他实例执行");
        }
    }

    /**
     * 示例2：使用看门狗自动续期
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void watchdogExample() {
        boolean success = lockTemplate.executeWithWatchdog(
            "task:watchdog:example",
            () -> {
                log.info("执行长时间任务");
                // 任务执行时间可以超过默认的30秒
                // 看门狗会自动续期
                doLongRunningTask();
            }
        );
    }

    /**
     * 示例3：有返回值的任务
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void withReturnValueExample() {
        java.util.Optional<Integer> result = lockTemplate.executeWithLock(
            "task:return:value:example",
            0,
            5,
            TimeUnit.MINUTES,
            () -> {
                int processed = processBatch();
                log.info("处理了 {} 条数据", processed);
                return processed;
            }
        );

        if (result.isPresent()) {
            log.info("任务执行成功，处理数量: {}", result.get());
        } else {
            log.info("任务已在其他实例执行");
        }
    }

    /**
     * 示例4：带超时控制的任务
     */
    @Scheduled(cron = "0 */15 * * * ?")
    public void withTimeoutExample() {
        boolean success = lockTemplate.executeWithLock(
            "task:timeout:example",
            0,
            5,
            TimeUnit.MINUTES,
            () -> {
                long startTime = System.currentTimeMillis();
                try {
                    doTaskWithTimeout();

                    long duration = System.currentTimeMillis() - startTime;
                    log.info("任务执行成功，耗时: {}ms", duration);

                    // 警告：任务执行时间过长
                    if (duration > 180000) { // 3分钟
                        log.warn("任务执行时间超过3分钟，可能需要优化");
                    }
                } catch (Exception e) {
                    log.error("任务执行失败", e);
                    throw e;
                }
            }
        );
    }

    /**
     * 示例5：公平锁
     */
    @Scheduled(cron = "0 */20 * * * ?")
    public void fairLockExample() {
        // 注意：需要单独实现公平锁支持
        // 这里只是示例，实际需要修改 RedissonLockTemplate
        String lockKey = "task:fair:lock";
        org.redisson.api.RedissonClient redissonClient = null; // 注入
        org.redisson.api.RLock fairLock = redissonClient.getFairLock(lockKey);

        try {
            if (fairLock.tryLock(0, 10, TimeUnit.MINUTES)) {
                try {
                    log.info("执行公平锁任务");
                    doTask();
                } finally {
                    if (fairLock.isHeldByCurrentThread()) {
                        fairLock.unlock();
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 示例6：读写锁
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void readWriteLockExample() {
        // 读任务
        String lockKey = "task:rw:lock";
        org.redisson.api.RedissonClient redissonClient = null; // 注入
        org.redisson.api.RReadWriteLock rwLock = redissonClient.getReadWriteLock(lockKey);
        org.redisson.api.RLock readLock = rwLock.readLock();

        try {
            if (readLock.tryLock(0, 5, TimeUnit.MINUTES)) {
                try {
                    log.info("执行读任务");
                    doRead();
                } finally {
                    if (readLock.isHeldByCurrentThread()) {
                        readLock.unlock();
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ==================== 业务方法 ====================

    private void doTask() {
        // 实际业务逻辑
    }

    private void doLongRunningTask() {
        // 长时间运行的业务逻辑
    }

    private int processBatch() {
        // 批量处理业务逻辑
        return 100;
    }

    private void doTaskWithTimeout() {
        // 带超时控制的任务
    }

    private void doRead() {
        // 读操作
    }
}

/**
 * 配置示例
 */
/*
@Configuration
class RedissonConfig {

    @Bean
    public RedissonClient redissonClient() {
        org.redisson.config.Config config = new org.redisson.config.Config();

        // 单机模式
        config.useSingleServer()
            .setAddress("redis://localhost:6379")
            .setConnectionPoolSize(64)
            .setConnectionMinimumIdleSize(10);

        // 或者：哨兵模式
        // config.useSentinelServers()
        //     .setMasterName("mymaster")
        //     .addSentinelAddress("redis://sentinel1:26379")
        //     .addSentinelAddress("redis://sentinel2:26379");

        // 或者：集群模式
        // config.useClusterServers()
        //     .addNodeAddress("redis://node1:6379")
        //     .addNodeAddress("redis://node2:6379")
        //     .addNodeAddress("redis://node3:6379");

        return Redisson.create(config);
    }
}
*/
