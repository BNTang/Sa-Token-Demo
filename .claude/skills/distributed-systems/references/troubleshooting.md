# 分布式定时任务故障排查指南

> 版本: 1.0 | 更新: 2026-02-05
>
> 分布式定时任务常见问题及排查方法

---

## 问题分类

| 问题类型 | 典型症状 | 优先级 |
|---------|---------|--------|
| 任务重复执行 | 数据重复、接口重复调用 | P0 |
| 任务不执行 | 任务从未触发 | P0 |
| 任务执行超时 | 任务一直处于执行中 | P1 |
| 锁释放失败 | 其他实例无法获取锁 | P1 |
| 任务失败率高 | 大量任务执行失败 | P1 |

---

## 问题1：任务重复执行

### 现象

- 数据库出现重复数据
- 第三方接口收到重复请求
- 日志显示多个实例同时执行任务

### 排查步骤

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class TaskDebugService {

    private final RedissonClient redissonClient;

    @Scheduled(cron = "0 */5 * * * ?")
    public void debugTask() {
        String lockKey = "task:debug";

        // 1. 检查是否获取到锁
        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = lock.isLocked();

        log.info("当前锁状态: {}, 是否被锁定: {}, 是否被当前线程持有: {}",
            lockKey,
            acquired,
            lock.isHeldByCurrentThread());

        // 2. 检查锁的剩余时间
        if (acquired) {
            long remainTime = lock.remainTimeToLive();
            log.info("锁剩余时间: {} ms", remainTime);
        }

        // 3. 尝试获取锁
        try {
            boolean success = lock.tryLock(0, 10, TimeUnit.SECONDS);
            log.info("尝试获取锁结果: {}", success);

            if (success) {
                try {
                    doTask();
                } finally {
                    lock.unlock();
                    log.info("锁已释放");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

### 可能原因及解决方案

| 原因 | 解决方案 |
|------|---------|
| 未实现分布式锁 | 添加 Redis 分布式锁或数据库唯一约束 |
| 锁超时时间太短 | 增加锁超时时间或使用看门狗 |
| 锁未正确释放 | 使用 try-finally 确保释放 |
| 任务执行时间超过超时时间 | 使用看门狗自动续期 |
| 多个 Redis 实例 | 使用 Redis Sentinel 或 Cluster |

---

## 问题2：任务不执行

### 现象

- 定时任务从未触发
- 没有相关日志输出

### 排查步骤

#### 1. 检查定时任务是否启用

```yaml
# application.yml
spring:
  task:
    scheduling:
      pool:
        size: 5  # 检查线程池配置
  # 确保没有禁用定时任务
```

#### 2. 检查 Cron 表达式

```java
@Component
@Slf4j
public class CronValidation {

    @Scheduled(cron = "0 0 2 * * ?")  // 每天凌晨2点
    public void dailyTask() {
        log.info("执行每日任务");
    }

    // 测试用：每5秒执行一次
    @Scheduled(cron = "0/5 * * * * ?")
    public void testTask() {
        log.info("测试任务执行时间: {}", LocalDateTime.now());
    }
}
```

#### 3. 检查时区设置

```yaml
# application.yml
spring:
  task:
    scheduling:
      timezone: "Asia/Shanghai"  # 确保时区正确
```

#### 4. 启用调试日志

```yaml
# logback-spring.xml
<logger name="org.springframework.scheduling" level="DEBUG"/>
<logger name="org.springframework.scheduling.concurrent" level="DEBUG"/>
```

### 可能原因及解决方案

| 原因 | 解决方案 |
|------|---------|
| Cron 表达式错误 | 使用在线工具验证 Cron 表达式 |
| 时区不匹配 | 设置正确的时区 |
| 线程池已满 | 增加线程池大小 |
| 任务方法权限错误 | 确保方法是 public 的 |
| 方法有参数 | 定时任务方法不能有参数 |
| 在同一类中调用 | 从外部调用定时任务方法 |

---

## 问题3：任务执行超时

### 现象

- 任务一直处于执行中状态
- 其他实例无法获取锁
- 没有新的任务触发

### 排查步骤

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class TimeoutDebugService {

    private final RedissonClient redissonClient;

    @Scheduled(cron = "0 */5 * * * ?")
    public void timeoutDebugTask() {
        String lockKey = "task:timeout";
        RLock lock = redissonClient.getLock(lockKey);

        try {
            long startTime = System.currentTimeMillis();

            // 检查锁是否被锁定
            if (lock.isLocked()) {
                long remainTime = lock.remainTimeToLive();
                log.warn("锁仍被持有，剩余时间: {} ms", remainTime);

                if (remainTime == -1) {
                    // 看门狗模式，永不过期
                    log.warn("使用看门狗模式，锁永不过期");
                }
            }

            // 设置超时检测
            if (lock.tryLock(0, 10, TimeUnit.MINUTES)) {
                try {
                    // 执行任务
                    doTaskWithTimeout();

                    long duration = System.currentTimeMillis() - startTime;
                    log.info("任务执行完成，耗时: {} ms", duration);

                } finally {
                    lock.unlock();
                }
            } else {
                log.warn("获取锁失败，任务可能正在执行");
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void doTaskWithTimeout() {
        // 使用 CompletableFuture 实现超时
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            doTask();
        });

        try {
            future.get(5, TimeUnit.MINUTES); // 最多等待5分钟
        } catch (TimeoutException e) {
            log.error("任务执行超时");
            future.cancel(true);
            throw new RuntimeException("任务超时", e);
        } catch (Exception e) {
            throw new RuntimeException("任务执行失败", e);
        }
    }
}
```

### 可能原因及解决方案

| 原因 | 解决方案 |
|------|---------|
| 任务执行时间过长 | 优化任务逻辑或分批处理 |
| 死循环 | 检查循环条件，添加超时 |
| 外部接口超时 | 设置合理的超时时间 |
| 数据库查询慢 | 优化 SQL 或添加索引 |
| 网络问题 | 添加重试机制 |

---

## 问题4：锁释放失败

### 现象

- 任务完成后其他实例仍无法获取锁
- Redis 中锁 key 仍然存在

### 排查步骤

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class LockReleaseDebugService {

    private final RedissonClient redissonClient;
    private final StringRedisTemplate redisTemplate;

    @Scheduled(cron = "0 */5 * * * ?")
    public void lockReleaseDebugTask() {
        String lockKey = "task:lock:debug";
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(0, 10, TimeUnit.SECONDS);

            if (acquired) {
                log.info("获取锁成功");

                // 检查锁的详细信息
                if (lock instanceof RedissonLock) {
                    log.info("锁的 hash 值: {}",
                        redisTemplate.opsForValue().get(lockKey));
                }

                try {
                    doTask();
                } finally {
                    // 检查锁是否被当前线程持有
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                        log.info("锁已释放");
                    } else {
                        log.warn("锁不被当前线程持有，可能已被释放");
                    }

                    // 确认锁已被删除
                    Boolean exists = redisTemplate.hasKey(lockKey);
                    log.info("锁是否仍存在: {}", exists);
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

### 可能原因及解决方案

| 原因 | 解决方案 |
|------|---------|
| 异常时未释放锁 | 使用 try-finally 确保释放 |
| 释放了其他实例的锁 | 检查锁的持有者 |
| Redisson 客户端问题 | 重启应用或升级版本 |
| 网络 问题 | 使用自动续期机制 |

---

## 问题5：任务失败率高

### 现象

- 大量任务执行失败
- 日志中出现大量异常

### 排查步骤

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class FailureRateDebugService {

    private final TaskRecordMapper taskRecordMapper;

    @Scheduled(cron = "0 */5 * * * ?")
    public void failureRateDebugTask() {
        String taskId = "failure-debug-" + System.currentTimeMillis();

        TaskRecord record = new TaskRecord();
        record.setTaskId(taskId);
        record.setStartTime(LocalDateTime.now());

        try {
            taskRecordMapper.insert(record);

            doTask();

            record.setStatus("SUCCESS");
            record.setEndTime(LocalDateTime.now());

        } catch (Exception e) {
            log.error("任务执行失败: {}", taskId, e);

            record.setStatus("FAILED");
            record.setErrorMsg(e.getMessage());
            record.setEndTime(LocalDateTime.now());

            // 分析失败原因
            analyzeFailure(e);

        } finally {
            taskRecordMapper.updateById(record);

            // 统计失败率
            checkFailureRate();
        }
    }

    private void analyzeFailure(Exception e) {
        if (e instanceof SQLException) {
            log.error("数据库错误，检查 SQL 和连接");
        } else if (e instanceof SocketTimeoutException) {
            log.error("网络超时，检查外部接口可用性");
        } else if (e instanceof OutOfMemoryError) {
            log.error("内存不足，检查 JVM 内存设置");
        } else {
            log.error("其他错误: {}", e.getClass().getName());
        }
    }

    private void checkFailureRate() {
        // 查询最近1小时的失败率
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

        int totalCount = taskRecordMapper.countByCreateTimeAfter(oneHourAgo);
        int failedCount = taskRecordMapper.countByStatusAndCreateTimeAfter(
            "FAILED", oneHourAgo);

        double failureRate = totalCount == 0 ? 0 :
            (double) failedCount / totalCount * 100;

        log.info("最近1小时任务统计: 总数={}, 失败={}, 失败率={}%",
            totalCount, failedCount, String.format("%.2f", failureRate));

        if (failureRate > 50) {
            log.error("失败率过高，发送告警");
            sendAlert(failureRate);
        }
    }

    private void sendAlert(double failureRate) {
        // 发送告警通知
    }
}
```

### 可能原因及解决方案

| 原因 | 解决方案 |
|------|---------|
| 数据库连接耗尽 | 增加连接池大小或优化查询 |
| 外部接口不可用 | 添加重试机制和降级逻辑 |
| 内存不足 | 增加 JVM 内存或优化代码 |
| 并发量过大 | 使用分布式锁限流 |
| 业务逻辑错误 | 修复代码中的 bug |

---

## 通用排查工具

### 1. 任务执行监控

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class TaskMonitor {

    private final TaskRecordMapper taskRecordMapper;

    @Scheduled(cron = "0 */10 * * * ?") // 每10分钟
    public void monitorTasks() {
        LocalDateTime now = LocalDateTime.now();

        // 1. 检查运行中的任务
        List<TaskRecord> runningTasks = taskRecordMapper.findByStatus("RUNNING");
        for (TaskRecord task : runningTasks) {
            long runningMinutes = Duration.between(
                task.getStartTime(), now
            ).toMinutes();

            if (runningMinutes > 60) {
                log.warn("任务运行时间过长: {}, 已运行 {} 分钟",
                    task.getTaskId(), runningMinutes);
            }
        }

        // 2. 统计失败率
        LocalDateTime oneHourAgo = now.minusHours(1);
        int totalCount = taskRecordMapper.countByCreateTimeAfter(oneHourAgo);
        int failedCount = taskRecordMapper.countByStatusAndCreateTimeAfter(
            "FAILED", oneHourAgo);

        if (totalCount > 0) {
            double failureRate = (double) failedCount / totalCount * 100;
            log.info("最近1小时失败率: {}%", String.format("%.2f", failureRate));

            if (failureRate > 30) {
                log.error("失败率过高，需要关注");
            }
        }
    }
}
```

### 2. 锁状态检查

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class LockMonitor {

    private final RedissonClient redissonClient;

    @Scheduled(cron = "0 */5 * * * ?")
    public void monitorLocks() {
        String[] lockKeys = {
            "task:sync:data",
            "task:daily:report",
            "task:cleanup"
        };

        for (String lockKey : lockKeys) {
            RLock lock = redissonClient.getLock(lockKey);

            if (lock.isLocked()) {
                long remainTime = lock.remainTimeToLive();
                boolean heldByCurrentThread = lock.isHeldByCurrentThread();

                log.info("锁: {}, 剩余时间: {} ms, 当前线程持有: {}",
                    lockKey, remainTime, heldByCurrentThread);

                if (remainTime > 0 && remainTime < 60000) {
                    log.warn("锁即将过期: {}, 剩余时间: {} ms",
                        lockKey, remainTime);
                }
            }
        }
    }
}
```

### 3. 性能指标收集

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class PerformanceMetrics {

    private final MeterRegistry meterRegistry;

    public void recordTaskExecution(String taskName, long duration, boolean success) {
        // 记录执行时间
        meterRegistry.timer("task.execution.time",
            "task", taskName,
            "status", success ? "success" : "failure"
        ).record(duration, TimeUnit.MILLISECONDS);

        // 记录成功/失败次数
        meterRegistry.counter("task.execution.count",
            "task", taskName,
            "status", success ? "success" : "failure"
        ).increment();
    }

    @Scheduled(cron = "0 */5 * * * ?")
    public void reportMetrics() {
        // 打印关键指标
        log.info("任务执行统计: {}",
            meterRegistry.getMeters()
                .stream()
                .filter(m -> m.getId().getName().startsWith("task.execution"))
                .map(m -> m.getId().getName() + "=" + m.measure())
                .collect(Collectors.joining(", "))
        );
    }
}
```

---

## 快速诊断检查清单

使用以下检查清单快速诊断问题：

### 基础检查

- [ ] 定时任务注解是否正确（@Scheduled）
- [ ] Cron 表达式是否正确
- [ ] 时区设置是否正确
- [ ] 任务方法是否是 public
- [ ] 任务方法是否有参数（不应该有）
- [ ] 是否从外部调用任务方法

### 分布式锁检查

- [ ] 是否实现了分布式锁
- [ ] 锁的 key 是否唯一
- [ ] 锁超时时间是否合理
- [ ] 是否正确释放锁
- [ ] 是否使用 try-finally 确保释放
- [ ] 是否处理了 InterruptedException

### 异常处理检查

- [ ] 是否捕获了异常
- [ ] 异常是否正确记录
- [ ] 锁是否在异常时释放
- [ ] 是否有失败重试机制
- [ ] 是否有告警通知

### 性能检查

- [ ] 任务执行时间是否过长
- [ ] 是否有性能监控
- [ ] 是否记录了执行日志
- [ ] 是否有超时控制
- [ ] 数据库查询是否优化

---

## 常用调试命令

### Redis 相关

```bash
# 查看所有锁
redis-cli keys "task:*"

# 查看锁的详细信息
redis-cli get "task:sync:data"
redis-cli ttl "task:sync:data"

# 手动释放锁
redis-cli del "task:sync:data"

# 查看锁的持有者
redis-cli hgetall "task:sync:data"
```

### 日志查看

```bash
# 查看定时任务相关日志
grep "@Scheduled" logs/app.log

# 查看锁相关日志
grep "分布式锁" logs/app.log

# 查看任务执行日志
grep "任务开始\\|任务完成" logs/app.log

# 查看异常日志
grep "ERROR" logs/app.log | grep "任务"
```

---

## 参考资料

- Spring Scheduling 文档：https://docs.spring.io/spring-framework/reference/integration/scheduling.html
- Redisson 文档：https://redisson.org
