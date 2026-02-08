# 分布式锁完整方案

> 版本: 1.0 | 更新: 2026-02-05
>
* 分布式锁是解决并发控制问题的核心技术，适用于多种分布式场景

---

## 概述

分布式锁用于控制在分布式系统中多个进程对共享资源的互斥访问。

### 应用场景

| 场景 | 说明 |
|------|------|
| 定时任务防重 | 避免多实例同时执行定时任务 |
| 库存扣减 | 防止超卖 |
| 限流控制 | 控制并发访问量 |
| 幂等保证 | 确保接口只执行一次 |

---

## 方案对比

| 方案 | 优点 | 缺点 | 适用场景 |
|------|------|------|---------|
| **Redis 锁** | 性能高、实现简单 | AP 模型、可能丢锁 | 高并发场景 |
| **ZooKeeper 锁** | 强一致性、自动释放 | 性能中等、复杂 | 金融级场景 |
| **数据库锁** | 可靠性高、无额外依赖 | 性能低、DB 压力 | 低并发场景 |

---

## Redis 分布式锁

详见：[Redis 分布式锁方案](scheduled-task-redis-lock.md)

---

## ZooKeeper 分布式锁

### 依赖

```xml
<dependency>
    <groupId>org.apache.curator</groupId>
    <artifactId>curator-recipes</artifactId>
    <version>5.5.0</version>
</dependency>
```

### 配置

```java
@Configuration
public class CuratorConfig {

    @Bean
    public CuratorFramework curatorFramework() {
        CuratorFramework client = CuratorFrameworkFactory.builder()
            .connectString("localhost:2181")
            .sessionTimeoutMs(5000)
            .connectionTimeoutMs(5000)
            .retryPolicy(new ExponentialBackoffRetry(1000, 3))
            .build();

        client.start();
        return client;
    }
}
```

### 基础用法

```java
@Service
@RequiredArgsConstructor
public class ZooKeeperLockService {

    private final CuratorFramework curatorFramework;

    public void executeWithLock(String lockPath, Runnable task) {
        InterProcessMutex lock = new InterProcessMutex(curatorFramework, lockPath);

        try {
            // 获取锁，最多等待 10 秒
            if (lock.acquire(10, TimeUnit.SECONDS)) {
                try {
                    task.run();
                } finally {
                    lock.release();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("分布式锁执行失败", e);
        }
    }
}
```

### 可重入锁

```java
@Service
@RequiredArgsConstructor
public class ZooKeeperReentrantLockService {

    private final CuratorFramework curatorFramework;

    public void executeWithReentrantLock(String lockPath, Runnable task) {
        InterProcessMutex lock = new InterProcessMutex(curatorFramework, lockPath);

        try {
            // 可重入锁
            if (lock.acquire(10, TimeUnit.SECONDS)) {
                try {
                    // 在锁内部再次获取锁（可重入）
                    if (lock.acquire(10, TimeUnit.SECONDS)) {
                        try {
                            task.run();
                        } finally {
                            lock.release(); // 内部释放
                        }
                    }
                } finally {
                    lock.release(); // 外部释放
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("分布式锁执行失败", e);
        }
    }
}
```

### 读写锁

```java
@Service
@RequiredArgsConstructor
public class ZooKeeperReadWriteLockService {

    private final CuratorFramework curatorFramework;

    public void read(String lockPath, Runnable task) {
        InterProcessReadWriteLock rwLock = new InterProcessReadWriteLock(curatorFramework, lockPath);
        InterProcessMutex readLock = rwLock.readLock();

        try {
            if (readLock.acquire(10, TimeUnit.SECONDS)) {
                try {
                    task.run();
                } finally {
                    readLock.release();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("读锁执行失败", e);
        }
    }

    public void write(String lockPath, Runnable task) {
        InterProcessReadWriteLock rwLock = new InterProcessReadWriteLock(curatorFramework, lockPath);
        InterProcessMutex writeLock = rwLock.writeLock();

        try {
            if (writeLock.acquire(10, TimeUnit.SECONDS)) {
                try {
                    task.run();
                } finally {
                    writeLock.release();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("写锁执行失败", e);
        }
    }
}
```

---

## 数据库分布式锁

### 基于唯一索引

详见：[数据库唯一约束方案](scheduled-task-db-unique.md)

### 基于排他锁

```sql
-- 获取锁
SELECT * FROM distributed_lock
WHERE lock_key = 'my_lock'
FOR UPDATE;

-- 释放锁（提交事务时自动释放）
COMMIT;
```

```java
@Service
@RequiredArgsConstructor
public class DatabaseLockService {

    private final JdbcTemplate jdbcTemplate;
    private final PlatformTransactionManager transactionManager;

    public void executeWithLock(String lockKey, Runnable task) {
        new TransactionTemplate(transactionManager).execute(status -> {
            try {
                // 获取排他锁
                jdbcTemplate.queryForObject(
                    "SELECT lock_key FROM distributed_lock WHERE lock_key = ? FOR UPDATE",
                    String.class,
                    lockKey
                );

                // 执行任务
                task.run();

                return null;
            } catch (Exception e) {
                throw new RuntimeException("数据库锁执行失败", e);
            }
        });
    }
}
```

### 建表语句

```sql
CREATE TABLE distributed_lock (
    lock_key VARCHAR(128) PRIMARY KEY,
    lock_value VARCHAR(128),
    expire_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='分布式锁表';
```

---

## 锁选择决策树

```
需要分布式锁
    │
    ├─ 是否有 Redis 集群？
    │   ├─ 是 → Redis 分布式锁
    │   └─ 否 → 继续
    │
    ├─ 是否是金融级强一致性？
    │   ├─ 是 → ZooKeeper 锁
    │   └─ 否 → 继续
    │
    ├─ 并发量是否 < 1000 QPS？
    │   ├─ 是 → 数据库锁
    │   └─ 否 → 部署 Redis 集群
    │
    └─ END
```

---

## 参考资料

- Apache Curator：https://curator.apache.org
- Redis 分布式锁：https://redis.io/docs/manual/patterns/distributed-locks/
