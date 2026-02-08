# Redis 分布式锁方案详解

> 版本: 1.0 | 更新: 2026-02-05
>
> 使用 Redis 分布式锁解决定时任务重复执行问题

---

## 概述

Redis 分布式锁是解决分布式定时任务重复执行最常用的方案之一，具有高性能、实现简单的优点。

### 核心原理

```
┌────────────────────────────────────────────────────────────┐
│              Redis 分布式锁工作原理                         │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  实例A      实例B      实例C                               │
│    │          │          │                                │
│    └─ tryLock │ tryLock ─┘                                 │
│         └──────┴──────┘                                   │
│                ↓                                          │
│         SET key NX EX 300                                 │
│         (原子操作)                                         │
│                ↓                                          │
│         只有第一个请求成功                                  │
│         (获得锁)                                           │
│                ↓                                          │
│         执行定时任务                                        │
│                ↓                                          │
│         DEL key (释放锁)                                   │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

---

## 方案一：Redisson 实现（推荐）

Redisson 是一个高级 Redis 客户端，提供了开箱即用的分布式锁实现。

### 1. 添加依赖

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.25.0</version>
</dependency>
```

### 2. 配置

```yaml
# application.yml
spring:
  redis:
    redisson:
      config: |
        singleServerConfig:
          address: "redis://localhost:6379"
          password: null
          database: 0
          connectionPoolSize: 64
          connectionMinimumIdleSize: 10
```

或使用 Java 配置：

```java
@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
            .setAddress("redis://localhost:6379")
            .setConnectionPoolSize(64)
            .setConnectionMinimumIdleSize(10);

        return Redisson.create(config);
    }
}
```

### 3. 基础用法

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledTaskService {

    private final RedissonClient redissonClient;

    @Scheduled(cron = "0 */5 * * * ?")
    public void syncDataTask() {
        String lockKey = "task:sync:data";
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试加锁：不等待，锁10分钟自动释放
            boolean acquired = lock.tryLock(0, 10, TimeUnit.MINUTES);

            if (!acquired) {
                log.info("任务已在其他实例执行，跳过");
                return;
            }

            try {
                log.info("开始执行定时任务");
                doSyncData();
                log.info("定时任务执行完成");
            } finally {
                // 释放锁
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("任务执行被中断", e);
        }
    }

    private void doSyncData() {
        // 实际业务逻辑
        log.info("执行数据同步任务");
    }
}
```

### 4. 高级用法：看门狗自动续期

Redisson 的看门狗机制会自动为锁续期，避免任务执行时间过长导致锁过期。

```java
@Scheduled(cron = "0 */5 * * * ?")
public void taskWithWatchdog() {
    String lockKey = "task:with:watchdog";
    RLock lock = redissonClient.getLock(lockKey);

    try {
        // 使用看门狗：lock() 方法不指定 leaseTime
        // 看门狗默认每隔 10 秒续期一次，锁过期时间为 30 秒
        lock.lock();

        // 任务执行时间可以超过 30 秒
        // 看门狗会自动续期，直到任务完成
        doLongRunningTask();

    } finally {
        lock.unlock();
    }
}
```

**看门狗工作原理**：
- 默认锁过期时间：30 秒
- 续期间隔：10 秒（过期时间的 1/3）
- 自动续期条件：线程还持有锁
- 任务完成：停止续期，释放锁

### 5. 公平锁

公平锁按照请求锁的顺序获得锁，避免饥饿现象。

```java
@Scheduled(cron = "0 */5 * * * ?")
public void taskWithFairLock() {
    String lockKey = "task:fair:lock";
    RLock lock = redissonClient.getFairLock(lockKey);

    try {
        // 公平锁：按请求顺序获得锁
        if (lock.tryLock(0, 10, TimeUnit.MINUTES)) {
            try {
                doTask();
            } finally {
                lock.unlock();
            }
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}
```

### 6. 读写锁

适用于读多写少的场景，多个读操作可以同时执行。

```java
@Service
@RequiredArgsConstructor
public class ReadWriteLockTask {

    private final RedissonClient redissonClient;

    @Scheduled(cron = "0 */1 * * * ?")
    public void readTask() {
        String lockKey = "task:rw:lock";
        RReadWriteLock rwLock = redissonClient.getReadWriteLock(lockKey);
        RLock readLock = rwLock.readLock();

        try {
            if (readLock.tryLock(0, 5, TimeUnit.MINUTES)) {
                try {
                    // 多个读任务可以同时执行
                    doRead();
                } finally {
                    readLock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Scheduled(cron = "0 */5 * * * ?")
    public void writeTask() {
        String lockKey = "task:rw:lock";
        RReadWriteLock rwLock = redissonClient.getReadWriteLock(lockKey);
        RLock writeLock = rwLock.writeLock();

        try {
            // 写任务独占，阻塞所有读任务
            if (writeLock.tryLock(0, 5, TimeUnit.MINUTES)) {
                try {
                    doWrite();
                } finally {
                    writeLock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

---

## 方案二：原生 Redis 实现

如果不使用 Redisson，也可以使用原生 Redis 命令实现。

### 1. 使用 SET NX EX

```java
@Service
@RequiredArgsConstructor
public class NativeRedisLockTask {

    private final StringRedisTemplate redisTemplate;

    @Scheduled(cron = "0 */5 * * * ?")
    public void taskWithNativeLock() {
        String lockKey = "task:native:lock";
        String lockValue = UUID.randomUUID().toString(); // 防止误删
        long expireTime = 300; // 5分钟

        // 尝试获取锁
        Boolean acquired = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, lockValue, expireTime, TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(acquired)) {
            log.info("任务已在其他实例执行，跳过");
            return;
        }

        try {
            log.info("获得锁，开始执行任务");
            doTask();
        } finally {
            // 释放锁：使用 Lua 脚本保证原子性
            releaseLock(lockKey, lockValue);
        }
    }

    private void releaseLock(String lockKey, String lockValue) {
        String script = """
            if redis.call("get", KEYS[1]) == ARGV[1] then
                return redis.call("del", KEYS[1])
            else
                return 0
            end
            """;

        redisTemplate.execute(
            new DefaultRedisScript<>(script, Long.class),
            Collections.singletonList(lockKey),
            lockValue
        );
    }
}
```

### 2. Lua 脚本实现

将加锁和释放逻辑封装为 Lua 脚本：

```java
@Service
@RequiredArgsConstructor
public class LuaScriptLockTask {

    private final StringRedisTemplate redisTemplate;

    // 加锁脚本
    private static final String LOCK_SCRIPT = """
        if redis.call("setnx", KEYS[1], ARGV[1]) == 1 then
            return redis.call("expire", KEYS[1], ARGV[2])
        else
            return 0
        end
        """;

    // 释放锁脚本
    private static final String UNLOCK_SCRIPT = """
        if redis.call("get", KEYS[1]) == ARGV[1] then
            return redis.call("del", KEYS[1])
        else
            return 0
        end
        """;

    @Scheduled(cron = "0 */5 * * * ?")
    public void taskWithLuaLock() {
        String lockKey = "task:lua:lock";
        String lockValue = UUID.randomUUID().toString();
        long expireTime = 300; // 5分钟

        // 执行加锁脚本
        Long result = redisTemplate.execute(
            new DefaultRedisScript<>(LOCK_SCRIPT, Long.class),
            Collections.singletonList(lockKey),
            lockValue,
            String.valueOf(expireTime)
        );

        if (result == null || result == 0) {
            log.info("获取锁失败，任务已在其他实例执行");
            return;
        }

        try {
            log.info("获得锁，开始执行任务");
            doTask();
        } finally {
            // 执行释放锁脚本
            redisTemplate.execute(
                new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class),
                Collections.singletonList(lockKey),
                lockValue
            );
        }
    }
}
```

---

## 锁超时时间设置策略

### 如何确定超时时间

```java
// 超时时间 = 正常执行时间 × 2 + 缓冲时间
long normalExecutionTime = 60; // 正常执行 60 秒
long lockTimeout = normalExecutionTime * 2 + 300; // 锁 420 秒（7分钟）

lock.tryLock(0, lockTimeout, TimeUnit.SECONDS);
```

### 动态调整策略

```java
@Service
@RequiredArgsConstructor
public class AdaptiveLockTimeoutTask {

    private final RedissonClient redissonClient;
    private final TaskExecutionMetrics metrics;

    @Scheduled(cron = "0 */5 * * * ?")
    public void taskWithAdaptiveTimeout() {
        String lockKey = "task:adaptive:lock";
        RLock lock = redissonClient.getLock(lockKey);

        // 根据历史执行时间动态调整锁超时
        long avgExecutionTime = metrics.getAverageExecutionTime();
        long lockTimeout = Math.max(avgExecutionTime * 2, 300); // 最少5分钟

        try {
            if (lock.tryLock(0, lockTimeout, TimeUnit.SECONDS)) {
                try {
                    long startTime = System.currentTimeMillis();
                    doTask();
                    long executionTime = System.currentTimeMillis() - startTime;

                    // 记录执行时间用于动态调整
                    metrics.recordExecution(executionTime);
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

---

## 常见问题与解决方案

### 问题1：锁超时时间难以确定

**解决方案**：使用看门狗自动续期

```java
// 不指定 leaseTime，启用看门狗
lock.lock(); // 看门狗每 10 秒续期一次

// 或者手动实现续期
CompletableFuture.runAsync(() -> {
    while (lock.isHeldByCurrentThread()) {
        try {
            TimeUnit.SECONDS.sleep(10);
            redisTemplate.expire(lockKey, 300, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            break;
        }
    }
});
```

### 问题2：任务执行时间过长

**解决方案**：任务分解 + 子任务追踪

```java
@Service
@RequiredArgsConstructor
public class LongRunningTask {

    private final RedissonClient redissonClient;
    private final TaskProgressRepository progressRepository;

    @Scheduled(cron = "0 0 2 * * ?")
    public void longRunningTask() {
        String lockKey = "task:long:running";
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (!lock.tryLock(0, 1, TimeUnit.HOURS)) {
                // 检查任务进度
                TaskProgress progress = progressRepository.findLatest();
                if (progress != null && progress.isExpired()) {
                    log.warn("任务超时，准备重试");
                    progressRepository.markFailed(progress.getId());
                    return;
                }
                log.info("任务正在执行中");
                return;
            }

            try {
                // 创建进度记录
                TaskProgress progress = new TaskProgress();
                progress.setStartTime(LocalDateTime.now());
                progressRepository.save(progress);

                // 分批执行
                List<Data> dataList = fetchData();
                int batchSize = 1000;

                for (int i = 0; i < dataList.size(); i += batchSize) {
                    List<Data> batch = dataList.subList(i, Math.min(i + batchSize, dataList.size()));
                    processBatch(batch);

                    // 更新进度
                    progress.setProcessed(i + batch.size());
                    progressRepository.save(progress);
                }

                progress.setStatus("SUCCESS");
                progressRepository.save(progress);

            } finally {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

### 问题3：Redis 宕机导致锁失效

**解决方案**：使用 Redis Sentinel 或 Cluster

```yaml
# Redis Sentinel 配置
spring:
  redis:
    redisson:
      config: |
        sentinelServersConfig:
          masterName: "mymaster"
          sentinelAddresses:
            - "redis://sentinel1:26379"
            - "redis://sentinel2:26379"
            - "redis://sentinel3:26379"
```

```yaml
# Redis Cluster 配置
spring:
  redis:
    redisson:
      config: |
        clusterServersConfig:
          nodeAddresses:
            - "redis://node1:6379"
            - "redis://node2:6379"
            - "redis://node3:6379"
```

---

## 完整工具类封装

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedLockService {

    private final RedissonClient redissonClient;

    /**
     * 执行带锁的任务
     *
     * @param lockKey    锁的key
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

            try {
                long startTime = System.currentTimeMillis();
                task.run();
                long duration = System.currentTimeMillis() - startTime;

                log.debug("任务执行成功，锁: {}, 耗时: {}ms", lockKey, duration);
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

    /**
     * 执行带锁的任务（有返回值）
     */
    public <T> Optional<T> executeWithLock(
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
                return Optional.empty();
            }

            try {
                T result = task.get();
                log.debug("任务执行成功，锁: {}", lockKey);
                return Optional.of(result);

            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("任务执行被中断: {}", lockKey, e);
            return Optional.empty();
        }
    }
}
```

**使用工具类**：

```java
@Service
@RequiredArgsConstructor
public class ScheduledTaskService {

    private final DistributedLockService lockService;

    @Scheduled(cron = "0 */5 * * * ?")
    public void syncDataTask() {
        boolean success = lockService.executeWithLock(
            "task:sync:data",
            0,          // 不等待
            10,         // 锁10分钟
            TimeUnit.MINUTES,
            () -> {
                log.info("执行数据同步任务");
                doSyncData();
            }
        );

        if (!success) {
            log.info("任务已在其他实例执行");
        }
    }
}
```

---

## 测试用例

```java
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RedisLockTest {

    @Autowired
    private DistributedLockService lockService;

    @Autowired
    private RedissonClient redissonClient;

    @Test
    public void testBasicLock() throws Exception {
        CountDownLatch latch = new CountDownLatch(3);
        AtomicInteger counter = new AtomicInteger(0);

        // 模拟3个并发任务
        for (int i = 0; i < 3; i++) {
            new Thread(() -> {
                lockService.executeWithLock(
                    "test:lock",
                    0,
                    10,
                    TimeUnit.SECONDS,
                    () -> {
                        counter.incrementAndGet();
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                );
                latch.countDown();
            }).start();
        }

        latch.await(15, TimeUnit.SECONDS);

        // 只有1个任务获得锁并执行
        assertEquals(1, counter.get());
    }

    @Test
    public void testLockTimeout() {
        String lockKey = "test:timeout";
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 获得锁并持有5秒
            lock.lock(5, TimeUnit.SECONDS);

            // 另一个线程尝试获取锁
            boolean acquired = lock.tryLock(0, 3, TimeUnit.SECONDS);

            assertFalse(acquired); // 应该获取失败

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

---

## 性能优化建议

1. **锁粒度**：尽量细化锁的粒度，避免全局锁
   ```java
   // 好的做法：按用户ID加锁
   String lockKey = "task:user:" + userId;

   // 坏的做法：使用全局锁
   String lockKey = "task:all";
   ```

2. **锁等待时间**：定时任务通常设置 waitTime=0，避免等待
   ```java
   lock.tryLock(0, leaseTime, unit); // 不等待
   ```

3. **锁超时时间**：根据任务执行时间合理设置
   ```java
   long leaseTime = estimateTaskTime() * 2 + buffer;
   ```

4. **监控指标**：记录锁等待时间、任务执行时间
   ```java
   // 记录锁获取失败次数
   metrics.increment("lock.failed");

   // 记录任务执行时间
   metrics.record("task.duration", duration);
   ```

---

## 参考资料

- Redisson 官方文档：https://redisson.org
- Redis 分布式锁实现：https://redis.io/docs/manual/patterns/distributed-locks/