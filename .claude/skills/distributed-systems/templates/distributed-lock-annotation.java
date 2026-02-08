/**
 * 分布式锁注解 - 整洁代码版本
 *
 * 通过注解 + AOP 实现分布式锁，使用更简洁
 *
 * 依赖：
 * <dependency>
 *   <groupId>org.redisson</groupId>
 *   <artifactId>redisson-spring-boot-starter</artifactId>
 *   <version>3.25.0</version>
 * </dependency>
 * <dependency>
 *   <groupId>org.springframework.boot</groupId>
 *   <artifactId>spring-boot-starter-aop</artifactId>
 * </dependency>
 */

package com.example.distributed.lock;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁注解
 *
 * 使用示例：
 * <pre>
 * {@code
 * @DistributedLock(key = "task:sync:data", leaseTime = 10, timeUnit = TimeUnit.MINUTES)
 * public void syncData() {
 *     // 业务逻辑
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {

    /**
     * 锁的 key，支持 SpEL 表达式
     *
     * 示例：
     * - "task:sync:data" - 固定 key
     * - "'task:user:' + #userId" - SpEL 表达式
     * - "'task:order:' + #orderDTO.id" - 从参数获取
     */
    String key();

    /**
     * 等待获取锁的时间，默认不等待
     */
    long waitTime() default 0;

    /**
     * 锁的超时时间，默认 30 秒
     */
    long leaseTime() default 30;

    /**
     * 时间单位，默认秒
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 是否启用看门狗（自动续期）
     * 启用后 leaseTime 参数无效
     */
    boolean watchdog() default false;
}

/**
 * 分布式锁 AOP 切面
 */
@Component
@RequiredArgsConstructor
@Aspect
@Slf4j
class DistributedLockAspect {

    private final RedissonClient redissonClient;
    private final SpelExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        // 1. 解析锁的 key（支持 SpEL 表达式）
        String lockKey = parseLockKey(joinPoint, distributedLock.key());

        // 2. 获取锁
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired;

            if (distributedLock.watchdog()) {
                // 看门狗模式：不指定超时时间
                lock.lock();
                acquired = true;
            } else {
                // 普通模式：指定超时时间
                acquired = lock.tryLock(
                    distributedLock.waitTime(),
                    distributedLock.leaseTime(),
                    distributedLock.timeUnit()
                );
            }

            if (!acquired) {
                log.info("获取锁失败，任务可能正在执行: {}", lockKey);
                return null;
            }

            // 3. 执行业务逻辑
            long startTime = System.currentTimeMillis();
            try {
                Object result = joinPoint.proceed();

                long duration = System.currentTimeMillis() - startTime;
                log.debug("分布式锁执行成功: {}, 耗时: {}ms", lockKey, duration);

                return result;

            } finally {
                // 4. 释放锁
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    log.debug("分布式锁已释放: {}", lockKey);
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("分布式锁执行被中断: {}", lockKey, e);
            throw e;
        }
    }

    /**
     * 解析锁的 key，支持 SpEL 表达式
     */
    private String parseLockKey(ProceedingJoinPoint joinPoint, String key) {
        // 如果不包含 SpEL 表达式，直接返回
        if (!key.contains("#") && !key.contains("'")) {
            return key;
        }

        // 解析 SpEL 表达式
        StandardEvaluationContext context = new StandardEvaluationContext();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameterNames.length; i++) {
            context.setVariable(parameterNames[i], args[i]);
        }

        Expression expression = parser.parseExpression(key);
        return expression.getValue(context, String.class);
    }
}

/**
 * 配置类
 */
@Configuration
class DistributedLockConfig {

    @Bean
    public DistributedLockAspect distributedLockAspect(RedissonClient redissonClient) {
        return new DistributedLockAspect(redissonClient);
    }
}

/**
 * 使用示例
 */
@Component
@Slf4j
class DistributedLockUsageExample {

    /**
     * 示例1：基础用法
     */
    @Scheduled(cron = "0 */5 * * * ?")
    @DistributedLock(key = "task:sync:data", leaseTime = 10, timeUnit = TimeUnit.MINUTES)
    public void syncData() {
        log.info("执行数据同步任务");
        // 业务逻辑
    }

    /**
     * 示例2：使用 SpEL 表达式（动态 key）
     */
    @DistributedLock(key = "'task:user:' + #userId", leaseTime = 5, timeUnit = TimeUnit.MINUTES)
    public void processUser(Long userId) {
        log.info("处理用户: {}", userId);
        // 业务逻辑
    }

    /**
     * 示例3：从对象参数获取
     */
    @DistributedLock(key = "'task:order:' + #order.id", leaseTime = 5, timeUnit = TimeUnit.MINUTES)
    public void processOrder(OrderDTO order) {
        log.info("处理订单: {}", order.getId());
        // 业务逻辑
    }

    /**
     * 示例4：使用看门狗（自动续期）
     * 适用于执行时间不确定的任务
     */
    @DistributedLock(key = "task:long:running", watchdog = true)
    public void longRunningTask() {
        log.info("执行长时间任务");
        // 任务执行时间可能超过 leaseTime
        // 看门狗会自动续期
    }

    /**
     * 示例5：等待获取锁
     */
    @DistributedLock(
        key = "task:with:wait",
        waitTime = 10,  // 等待 10 秒
        leaseTime = 5,   // 锁 5 分钟
        timeUnit = TimeUnit.MINUTES
    )
    public void taskWithWait() {
        log.info("执行需要等待的任务");
        // 业务逻辑
    }

    /**
     * 示例6：有返回值的方法
     */
    @DistributedLock(key = "task:with:return", leaseTime = 1, timeUnit = TimeUnit.MINUTES)
    public int processWithReturn() {
        log.info("执行有返回值的任务");
        return 100;
    }
}

/**
 * 数据传输对象示例
 */
@Data
class OrderDTO {
    private Long id;
    private String orderNo;
    private Long userId;
    private BigDecimal amount;
}

/**
 * Redisson 配置示例
 */
/*
@Configuration
class RedissonConfig {

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();

        // 单机模式
        config.useSingleServer()
            .setAddress("redis://localhost:6379")
            .setConnectionPoolSize(64)
            .setConnectionMinimumIdleSize(10);

        // 哨兵模式
        // config.useSentinelServers()
        //     .setMasterName("mymaster")
        //     .addSentinelAddress("redis://sentinel1:26379", "redis://sentinel2:26379");

        // 集群模式
        // config.useClusterServers()
        //     .addNodeAddress("redis://node1:6379", "redis://node2:6379");

        return Redisson.create(config);
    }
}
*/

/**
 * 导入声明（完整代码需要）
 */
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import lombok.Data;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
