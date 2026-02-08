---
name: distributed-systems
description: 分布式系统技术方案与最佳实践。涵盖分布式定时任务重复执行解决方案（Redis 分布式锁、数据库唯一约束、ZooKeeper 顺序节点、XXL-Job/Elastic Job 调度框架）、分布式锁、分布式事务、分布式缓存、服务治理等。包括方案对比、代码实现、选型决策树、避坑指南。当开发分布式系统、处理定时任务幂等、分布式一致性问题时使用。
metadata:
  author: skill-hub
  version: "1.0"
  compatibility: Java 17+, Spring Boot 3.x, Redisson 3.x, XXL-Job 2.4+
---

# 分布式系统技术方案与最佳实践

> 版本: 1.0 | 更新: 2026-02-05
>
> 解决分布式系统中的常见问题：定时任务重复执行、分布式锁、分布式事务等

---

## 概述

### 分布式系统核心挑战

```
┌─────────────────────────────────────────────────────────────────┐
│                   分布式系统核心挑战                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. 定时任务重复执行  → 多实例同时触发同一任务                   │
│  2. 并发控制          → 多个服务同时修改同一资源                 │
│  3. 数据一致性        → 分布式事务、最终一致性                   │
│  4. 服务治理          → 服务发现、负载均衡、熔断降级             │
│  5. 缓存一致性        → 缓存穿透、击穿、雪崩                     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 本 Skill 覆盖范围

| 问题领域 | 解决方案 | 详见 |
|---------|---------|------|
| **定时任务重复执行** | Redis 锁 / DB 唯一约束 / XXL-Job | [本文档](#定时任务重复执行解决方案) |
| **分布式锁** | Redis / ZooKeeper / DB | [分布式锁方案](references/distributed-lock.md) |
| **分布式事务** | Seata / TCC / SAGA / 本地消息表 | [分布式事务方案](references/distributed-transaction.md) |
| **分布式缓存** | Redis 多级缓存 / 一致性哈希 | [分布式缓存方案](references/distributed-cache.md) |
| **服务治理** | Nacos / Eureka / Sentinel | [服务治理方案](references/service-governance.md) |

---

## 何时使用此 Skill

| 场景 | 触发词 |
|------|--------|
| 定时任务 | @Scheduled、定时任务、cron、Quartz、XXL-Job、任务重复 |
| 分布式锁 | 分布式锁、并发控制、Redis 锁、ZooKeeper 锁 |
| 分布式事务 | 分布式事务、Seata、TCC、SAGA、最终一致性 |
| 分布式缓存 | 缓存穿透、缓存击穿、缓存雪崩、多级缓存 |
| 服务治理 | 服务发现、负载均衡、熔断、降级、限流 |

---

## 定时任务重复执行解决方案

### 问题是什么

在单机环境下，使用 `@Scheduled` 或 Quartz 定时任务能正常工作。但部署到多实例（集群）后：

```
┌────────────────────────────────────────────────────────────┐
│  问题：三个实例同时触发同一任务                             │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  实例A  @Scheduled(cron="0 0 2 * * ?")  ────┐            │
│  实例B  @Scheduled(cron="0 0 2 * * ?")  ────┼──> 重复执行！
│  实例C  @Scheduled(cron="0 0 2 * * ?")  ────┘            │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

**后果**：
- 数据重复处理
- 资源浪费
- 数据不一致
- 第三方接口重复调用

### 核心解决方案

| 方案 | 核心思路 | 适用场景 | 复杂度 |
|------|---------|---------|--------|
| **分布式锁** | 同一时刻只有一个实例获得锁 | 高并发、已有 Redis | ⭐⭐ |
| **数据库唯一约束** | 通过唯一索引防止重复 | 强一致性、涉及 DB | ⭐ |
| **专用调度框架** | 调度器层面保证单实例执行 | 企业级应用、多任务 | ⭐⭐⭐⭐ |

---

## 快速决策树（30秒选型）

```
问题：你的定时任务需要哪种方案？

START
  │
  ├─ 是否有多个定时任务（>10个）？
  │   ├─ 是 → 使用 XXL-Job 或 Elastic Job
  │   └─ 否 → 继续判断
  │
  ├─ 任务是否涉及数据库写操作？
  │   ├─ 是 → 数据库唯一约束（最简单）
  │   └─ 否 → 继续判断
  │
  ├─ 是否已有 Redis 集群？
  │   ├─ 是 → Redis 分布式锁（Redisson）
  │   └─ 否 → 继续判断
  │
  ├─ 是否是金融级强一致性场景？
  │   ├─ 是 → ZooKeeper 锁
  │   └─ 否 → 数据库唯一约束
  │
  └─ END
```

---

## 方案速查表

| 场景 | 推荐方案 | 实现复杂度 | 性能 | 可靠性 | 详细文档 |
|------|---------|-----------|------|--------|---------|
| 简单任务 + 有 Redis | Redis 锁 | ⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | [查看](references/scheduled-task-redis-lock.md) |
| 涉及数据库操作 | DB 唯一约束 | ⭐ | ⭐⭐ | ⭐⭐⭐⭐⭐ | [查看](references/scheduled-task-db-unique.md) |
| 多任务管理 | XXL-Job | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | [查看](references/scheduled-task-xxl-job.md) |
| 强一致性要求 | ZooKeeper | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | [查看](references/scheduled-task-zookeeper.md) |
| 已用 Quartz | Quartz 集群 | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ | [查看](references/scheduled-task-quartz.md) |

---

## 方案一：Redis 分布式锁（推荐）

### 核心代码

```java
@Service
@RequiredArgsConstructor
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
                // 执行任务
                doSyncData();
            } finally {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("任务执行被中断", e);
        }
    }
}
```

### 关键配置

```yaml
spring:
  redis:
    redisson:
      config: |
        singleServerConfig:
          address: "redis://localhost:6379"
          connectionPoolSize: 64
```

### 优缺点

| 优点 | 缺点 |
|------|------|
| ✅ 性能高 | ❌ 依赖 Redis |
| ✅ 实现简单 | ❌ 锁超时难设置 |
| ✅ 自动续期（看门狗） | ❌ 极端情况可能丢锁 |
| ✅ 支持可重入 |  |

**适用场景**：
- 已有 Redis 集群
- 任务执行时间可控
- 对性能要求高

**详细实现**：[Redis 分布式锁完整方案](references/scheduled-task-redis-lock.md)

---

## 方案二：数据库唯一约束

### 核心代码

```java
@Service
@RequiredArgsConstructor
public class ScheduledTaskService {

    private final TaskRecordMapper taskRecordMapper;

    @Scheduled(cron = "0 0 2 * * ?")
    public void dailyReport() {
        String taskId = "daily-report-" + LocalDate.now();

        try {
            // 插入任务记录（唯一索引保证幂等）
            TaskRecord record = new TaskRecord();
            record.setTaskId(taskId);
            record.setCreateTime(LocalDateTime.now());
            taskRecordMapper.insert(record);

            // 执行任务
            doDailyReport();

        } catch (DuplicateKeyException e) {
            log.info("任务已在其他实例执行: {}", taskId);
        }
    }
}
```

### 建表语句

```sql
CREATE TABLE task_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id VARCHAR(128) NOT NULL UNIQUE COMMENT '任务ID',
    status VARCHAR(32) DEFAULT 'SUCCESS',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_create_time (create_time)
) COMMENT='定时任务执行记录';
```

### 优缺点

| 优点 | 缺点 |
|------|------|
| ✅ 可靠性最高 | ❌ 性能较低 |
| ✅ 无额外依赖 | ❌ DB 压力大 |
| ✅ 天然幂等 | ❌ 需要建表 |
| ✅ 实现简单 |  |

**适用场景**：
- 强一致性要求
- 任务涉及数据库操作
- 低并发场景

**详细实现**：[数据库唯一约束完整方案](references/scheduled-task-db-unique.md)

---

## 方案三：XXL-Job（企业级推荐）

### 为什么选择 XXL-Job

```
┌────────────────────────────────────────────────────────────┐
│              XXL-Job 核心能力                              │
├────────────────────────────────────────────────────────────┤
│  ✅ 调度层面避免重复（自动选主）                           │
│  ✅ 可视化管理界面                                          │
│  ✅ 任务依赖、失败重试、邮件告警                            │
│  ✅ 支持任务分片（大数据处理）                              │
│  ✅ 路由策略（第一个、轮询、随机等）                        │
│  ✅ 执行日志实时查看                                        │
└────────────────────────────────────────────────────────────┘
```

### 快速开始

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.xuxueli</groupId>
    <artifactId>xxl-job-core</artifactId>
    <version>2.4.0</version>
</dependency>
```

```java
@Component
public class SampleXxlJob {

    @XxlJob("syncDataJob")
    public void syncData() {
        XxlJobHelper.log("任务开始执行");

        try {
            // 任务逻辑
            doSyncData();

            XxlJobHelper.handleSuccess();
        } catch (Exception e) {
            XxlJobHelper.handleFailure("任务执行失败: " + e.getMessage());
        }
    }
}
```

**适用场景**：
- 企业级应用
- 任务数量多（>10个）
- 需要可视化管理
- 需要监控告警

**详细实现**：[XXL-Job 完整方案](references/scheduled-task-xxl-job.md)

---

## 方案对比决策表

### 按场景选型

| 你的场景 | 推荐方案 | 理由 |
|---------|---------|------|
| 项目已有 Redis，任务执行时间可控 | Redis 锁 | 性能高、实现简单 |
| 任务涉及数据库操作，对一致性要求高 | DB 唯一约束 | 可靠性最高 |
| 有 10+ 个定时任务，需要管理 | XXL-Job | 统一管理、功能完善 |
| 金融级强一致性要求 | ZooKeeper 锁 | CP 模型、强一致 |
| 已使用 Quartz | Quartz 集群 | 复用现有框架 |

### 性能对比

| 方案 | QPS | 响应时间 | 依赖 |
|------|-----|---------|------|
| Redis 锁 | 10万+ | < 5ms | Redis |
| DB 唯一约束 | 1千+ | 10-50ms | MySQL |
| ZooKeeper 锁 | 1万+ | 5-20ms | ZooKeeper |
| XXL-Job | - | 调度延迟<1s | 调度中心 |

---

## 常见陷阱与避坑指南

详见：[故障排查完整指南](references/troubleshooting.md)

### Top 5 常见错误

#### 1. 锁超时时间设置不当

```java
// ❌ 错误：锁超时太短
lock.tryLock(0, 5, TimeUnit.SECONDS);  // 任务执行10秒，锁5秒就释放

// ✅ 正确：根据任务执行时间设置
lock.tryLock(0, 30, TimeUnit.MINUTES);  // 预留足够时间
```

#### 2. 未处理中断异常

```java
// ❌ 错误：未处理 InterruptedException
try {
    if (lock.tryLock(0, 10, TimeUnit.SECONDS)) {
        lock.unlock();
    }
} catch (InterruptedException e) {
    // 什么都不做
}

// ✅ 正确：恢复中断状态
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    log.error("任务被中断", e);
}
```

#### 3. 忘记释放锁

```java
// ❌ 错误：异常时未释放锁
if (lock.tryLock()) {
    doTask();  // 如果抛异常，锁永远不会释放
    lock.unlock();
}

// ✅ 正确：使用 try-finally
if (lock.tryLock()) {
    try {
        doTask();
    } finally {
        lock.unlock();
    }
}
```

#### 4. 数据库幂等表设计不当

```sql
-- ❌ 错误：只有 task_id 唯一索引
CREATE TABLE task_record (
    id BIGINT PRIMARY KEY,
    task_id VARCHAR(64) UNIQUE  -- 可能重复
);

-- ✅ 正确：增加日期维度
CREATE TABLE task_record (
    id BIGINT PRIMARY KEY,
    task_date DATE NOT NULL,
    task_id VARCHAR(64) NOT NULL,
    UNIQUE KEY uk_task (task_date, task_id)  -- 复合唯一键
);
```

#### 5. 任务执行时间监控缺失

```java
// ✅ 推荐：增加任务执行时间监控
@Scheduled(cron = "0 */5 * * * ?")
public void taskWithMetrics() {
    long startTime = System.currentTimeMillis();

    try {
        doTask();

        long duration = System.currentTimeMillis() - startTime;
        log.info("任务执行成功，耗时: {}ms", duration);

        if (duration > 60000) {
            log.warn("任务执行时间超过1分钟，可能需要优化");
        }
    } catch (Exception e) {
        log.error("任务执行失败，耗时: {}ms",
            System.currentTimeMillis() - startTime, e);
        throw e;
    }
}
```

---

## 快速检查清单

使用此清单验证你的定时任务实现：

### 编码检查

| # | 检查项 | 通过 |
|---|--------|------|
| 1 | 是否有多实例部署风险？ | ☐ |
| 2 | 是否选择了合适的防重方案？ | ☐ |
| 3 | 锁的超时时间是否合理？ | ☐ |
| 4 | 是否正确处理异常？ | ☐ |
| 5 | 是否在 finally 块释放锁？ | ☐ |
| 6 | 是否增加了日志和监控？ | ☐ |

### 测试检查

| # | 测试项 | 通过 |
|---|--------|------|
| 1 | 多实例同时启动测试 | ☐ |
| 2 | 任务执行超时测试 | ☐ |
| 3 | 应用重启测试 | ☐ |
| 4 | 锁释放失败测试 | ☐ |
| 5 | 幂等性验证测试 | ☐ |

---

## 代码模板

| 模板 | 说明 | 文件 |
|------|------|------|
| Redisson 锁模板 | 完整的 Redis 分布式锁封装 | [templates/redisson-lock-template.java](templates/redisson-lock-template.java) |
| DB 幂等模板 | 数据库唯一约束幂等模板 | [templates/db-idempotent-template.java](templates/db-idempotent-template.java) |
| XXL-Job 模板 | XXL-Job 任务模板 | [templates/xxl-job-template.java](templates/xxl-job-template.java) |
| ZooKeeper 锁模板 | Curator 锁模板 | [templates/zookeeper-lock-template.java](templates/zookeeper-lock-template.java) |

---

## 更多分布式系统方案

### 分布式锁

详见：[分布式锁完整方案](references/distributed-lock.md)

- Redis 分布式锁（Redisson）
- ZooKeeper 分布式锁（Curator）
- 数据库分布式锁

### 分布式事务

详见：[分布式事务完整方案](references/distributed-transaction.md)

- Seata（AT/TCC/SAGA 模式）
- 本地消息表
- MQ 事务消息
- TCC 编码模式

### 分布式缓存

详见：[分布式缓存完整方案](references/distributed-cache.md)

- 缓存穿透/击穿/雪崩解决方案
- 多级缓存设计
- 缓存一致性策略
- 热点 Key 发现与处理

### 服务治理

详见：[服务治理完整方案](references/service-governance.md)

- 服务注册与发现（Nacos/Eureka/Consul）
- 负载均衡策略
- 熔断降级（Sentinel/Hystrix）
- 限流算法（令牌桶/漏桶/滑动窗口）

---

## 参考资料

| 来源 | 说明 | 链接 |
|------|------|------|
| Redisson 官方文档 | Redis 分布式锁实现 | https://redisson.org |
| XXL-Job 官方文档 | 分布式任务调度框架 | https://www.xuxueli.com/xxl-job |
| Apache Curator | ZooKeeper 客户端 | https://curator.apache.org |
| Quartz 官方文档 | Quartz 集群配置 | https://www.quartz-scheduler.org |
| Seata 官方文档 | 分布式事务框架 | https://seata.io |
| Alibaba Sentinel | 流量控制与熔断降级 | https://sentinelguard.io |

---

## 版本历史

| 版本 | 日期 | 变更 |
|------|------|------|
| 1.0 | 2026-02-05 | 初始版本，包含定时任务重复执行解决方案 |
