# 分布式锁一般都怎样实现？

## 分布式锁概述

```
┌─────────────────────────────────────────────────────────────┐
│                    分布式锁核心要求                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 互斥性                                                 │
│      └── 同一时刻只有一个客户端能持有锁                      │
│                                                             │
│   2. 安全性                                                 │
│      └── 只有锁的持有者才能释放锁                            │
│                                                             │
│   3. 可用性                                                 │
│      └── 即使部分节点故障，锁服务仍可用                      │
│                                                             │
│   4. 防死锁                                                 │
│      └── 持有锁的客户端崩溃，锁能自动释放                    │
│                                                             │
│   5. 可重入 (可选)                                           │
│      └── 同一线程可多次获取同一把锁                          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 实现方案对比

```
┌─────────────────────────────────────────────────────────────┐
│                  分布式锁方案对比                            │
├──────────────┬──────────┬──────────┬──────────┬────────────┤
│   方案        │ 互斥性   │ 可用性   │ 性能     │ 复杂度     │
├──────────────┼──────────┼──────────┼──────────┼────────────┤
│   Redis      │  ✅ 高   │  ✅ 高   │ ✅✅ 高  │  ⭐ 简单   │
│   ZooKeeper  │  ✅ 高   │  ✅ 高   │  ✅ 中   │  ⭐⭐ 中等 │
│   MySQL      │  ✅ 高   │  ❌ 低   │  ❌ 低   │  ⭐ 简单   │
│   Etcd       │  ✅ 高   │  ✅ 高   │  ✅ 中   │  ⭐⭐ 中等 │
├──────────────┴──────────┴──────────┴──────────┴────────────┤
│   推荐: Redis (Redisson) 性能最高，Zookeeper 可靠性最高     │
└─────────────────────────────────────────────────────────────┘
```

## 一、Redis 分布式锁

### 1. 基础版 (SETNX + EXPIRE)

```java
/**
 * Redis 分布式锁 - 基础版
 * 问题：SETNX 和 EXPIRE 不是原子操作
 */
public class SimpleRedisLock {
    
    private StringRedisTemplate redisTemplate;
    
    // ❌ 错误示范：非原子操作
    public boolean tryLockWrong(String key, String value, long expireTime) {
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value);
        if (Boolean.TRUE.equals(result)) {
            // 如果这里崩溃，锁永远不会释放！
            redisTemplate.expire(key, expireTime, TimeUnit.SECONDS);
            return true;
        }
        return false;
    }
    
    // ✅ 正确：SET key value NX EX (原子操作)
    public boolean tryLock(String key, String value, long expireSeconds) {
        return Boolean.TRUE.equals(
            redisTemplate.opsForValue().setIfAbsent(key, value, expireSeconds, TimeUnit.SECONDS)
        );
    }
}
```

### 2. 安全版 (Lua 脚本释放)

```java
/**
 * Redis 分布式锁 - 安全版
 * 使用 Lua 脚本保证释放锁的原子性
 */
@Service
public class RedisDistributedLock {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    private static final String LOCK_PREFIX = "lock:";
    
    // 释放锁的 Lua 脚本
    private static final String UNLOCK_SCRIPT = 
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
        "    return redis.call('del', KEYS[1]) " +
        "else " +
        "    return 0 " +
        "end";
    
    /**
     * 加锁
     * @param lockKey 锁的 key
     * @param requestId 请求标识（用于释放锁时验证）
     * @param expireTime 过期时间（秒）
     */
    public boolean tryLock(String lockKey, String requestId, long expireTime) {
        String key = LOCK_PREFIX + lockKey;
        return Boolean.TRUE.equals(
            redisTemplate.opsForValue().setIfAbsent(key, requestId, expireTime, TimeUnit.SECONDS)
        );
    }
    
    /**
     * 释放锁（Lua 脚本保证原子性）
     * 只有锁的持有者才能释放
     */
    public boolean unlock(String lockKey, String requestId) {
        String key = LOCK_PREFIX + lockKey;
        Long result = redisTemplate.execute(
            new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class),
            Collections.singletonList(key),
            requestId
        );
        return result != null && result == 1;
    }
    
    /**
     * 使用示例
     */
    public void doBusinessWithLock(String orderId) {
        String lockKey = "order:" + orderId;
        String requestId = UUID.randomUUID().toString();
        
        try {
            if (tryLock(lockKey, requestId, 30)) {
                // 获取锁成功，执行业务
                processOrder(orderId);
            } else {
                throw new RuntimeException("获取锁失败");
            }
        } finally {
            unlock(lockKey, requestId);
        }
    }
}
```

### 3. Redisson 实现 (推荐)

```java
/**
 * Redisson 分布式锁 - 生产推荐
 * 支持：可重入、看门狗续期、公平锁、读写锁
 */
@Service
public class RedissonLockService {
    
    @Autowired
    private RedissonClient redissonClient;
    
    /**
     * 基本使用
     */
    public void basicLock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 加锁，默认 30 秒过期，看门狗自动续期
            lock.lock();
            // 业务逻辑
            doSomething();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
    
    /**
     * 尝试加锁（带超时）
     */
    public boolean tryLockWithTimeout(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 等待 5 秒获取锁，锁持有 30 秒后自动释放
            boolean acquired = lock.tryLock(5, 30, TimeUnit.SECONDS);
            if (acquired) {
                try {
                    doSomething();
                    return true;
                } finally {
                    lock.unlock();
                }
            }
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    /**
     * 公平锁
     */
    public void fairLock(String lockKey) {
        RLock fairLock = redissonClient.getFairLock(lockKey);
        fairLock.lock();
        try {
            doSomething();
        } finally {
            fairLock.unlock();
        }
    }
    
    /**
     * 读写锁
     */
    public void readWriteLock(String lockKey) {
        RReadWriteLock rwLock = redissonClient.getReadWriteLock(lockKey);
        
        // 读锁（共享）
        RLock readLock = rwLock.readLock();
        readLock.lock();
        try {
            // 读操作
        } finally {
            readLock.unlock();
        }
        
        // 写锁（独占）
        RLock writeLock = rwLock.writeLock();
        writeLock.lock();
        try {
            // 写操作
        } finally {
            writeLock.unlock();
        }
    }
}
```

### Redisson 看门狗机制

```
┌─────────────────────────────────────────────────────────────┐
│                  Redisson 看门狗原理                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   问题：锁过期时间设多少合适？                               │
│   ├── 设太短：业务没执行完锁就释放了                         │
│   └── 设太长：客户端崩溃后锁长时间无法释放                   │
│                                                             │
│   解决：看门狗 (Watch Dog) 自动续期                          │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  1. 默认锁过期时间 30 秒                             │  │
│   │  2. 后台线程每 10 秒检查一次                         │  │
│   │  3. 如果锁还被持有，自动续期到 30 秒                 │  │
│   │  4. 客户端崩溃后，看门狗停止，锁自动过期             │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   注意：只有不指定 leaseTime 时才会启动看门狗               │
│   lock.lock();           // 启动看门狗                      │
│   lock.lock(30, SECONDS); // 不启动看门狗                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 二、ZooKeeper 分布式锁

```java
/**
 * ZooKeeper 分布式锁
 * 原理：临时顺序节点 + Watcher
 */
public class ZkDistributedLock {
    
    private CuratorFramework client;
    private String lockPath = "/locks/mylock";
    
    /**
     * 使用 Curator 框架
     */
    public void lockWithCurator() throws Exception {
        InterProcessMutex lock = new InterProcessMutex(client, lockPath);
        
        try {
            // 获取锁，最多等待 10 秒
            if (lock.acquire(10, TimeUnit.SECONDS)) {
                try {
                    // 业务逻辑
                    doSomething();
                } finally {
                    lock.release();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

```
┌─────────────────────────────────────────────────────────────┐
│                  ZooKeeper 锁原理                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   /locks/mylock                                             │
│       ├── _c_xxxx-0000000001  (客户端A创建)                 │
│       ├── _c_xxxx-0000000002  (客户端B创建)                 │
│       └── _c_xxxx-0000000003  (客户端C创建)                 │
│                                                             │
│   1. 客户端在 /locks/mylock 下创建临时顺序节点              │
│   2. 判断自己是否是最小序号的节点                            │
│      ├── 是：获得锁                                         │
│      └── 否：监听前一个节点的删除事件                        │
│   3. 前一个节点删除后，收到通知，再次判断                    │
│   4. 完成业务后删除自己的节点，释放锁                        │
│                                                             │
│   优点：                                                     │
│   • 临时节点：客户端崩溃自动释放锁                           │
│   • 顺序节点：公平锁，避免惊群效应                           │
│   • Watcher：及时通知                                        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 三、MySQL 分布式锁

```java
/**
 * MySQL 分布式锁
 * 不推荐：性能差，可用性低
 */
@Service
public class MySqlDistributedLock {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    // 方式1：唯一索引
    public boolean tryLock(String lockName, String owner, int expireSeconds) {
        try {
            jdbcTemplate.update(
                "INSERT INTO distributed_lock (lock_name, owner, expire_time) VALUES (?, ?, ?)",
                lockName, owner, LocalDateTime.now().plusSeconds(expireSeconds)
            );
            return true;
        } catch (DuplicateKeyException e) {
            return false; // 已被锁定
        }
    }
    
    public void unlock(String lockName, String owner) {
        jdbcTemplate.update(
            "DELETE FROM distributed_lock WHERE lock_name = ? AND owner = ?",
            lockName, owner
        );
    }
    
    // 方式2：悲观锁 FOR UPDATE
    @Transactional
    public void processWithLock(String resourceId) {
        // SELECT ... FOR UPDATE 会阻塞其他事务
        jdbcTemplate.queryForObject(
            "SELECT * FROM resource WHERE id = ? FOR UPDATE",
            Resource.class, resourceId
        );
        // 业务处理
    }
}

-- 建表语句
CREATE TABLE distributed_lock (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lock_name VARCHAR(64) NOT NULL UNIQUE,
    owner VARCHAR(64) NOT NULL,
    expire_time DATETIME NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

## Redis vs ZooKeeper 选型

```
┌─────────────────────────────────────────────────────────────┐
│                  Redis vs ZooKeeper                          │
├───────────────────────┬─────────────────────────────────────┤
│        Redis          │           ZooKeeper                  │
├───────────────────────┼─────────────────────────────────────┤
│   AP 模型 (高可用)     │   CP 模型 (强一致)                   │
│   性能高 (10万+ QPS)   │   性能中等 (1万 QPS)                 │
│   异步复制可能丢锁     │   Zab协议保证一致性                  │
│   需要续期机制         │   临时节点自动释放                   │
│   Redisson 功能丰富    │   Curator 封装完善                   │
├───────────────────────┴─────────────────────────────────────┤
│                                                             │
│   选择建议：                                                 │
│   • 一般业务：Redis (Redisson) - 性能高，够用                │
│   • 金融级：ZooKeeper - 强一致性要求                         │
│   • 已有 ZK 集群：直接用 ZK 锁                               │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> 分布式锁常用实现：1）**Redis**：SETNX + 过期时间 + Lua 脚本释放，推荐用 Redisson（支持看门狗续期、可重入）；2）**ZooKeeper**：临时顺序节点 + Watcher，天然防死锁；3）**MySQL**：唯一索引或 FOR UPDATE，性能差不推荐。生产推荐 Redisson。

### 1分钟版本

> **分布式锁实现方案**：
>
> **Redis 实现**：
> - `SET key value NX EX` 原子加锁
> - Lua 脚本保证释放锁的原子性（只有持有者能释放）
> - **Redisson（推荐）**：自动续期（看门狗）、可重入、公平锁、读写锁
>
> **ZooKeeper 实现**：
> - 临时顺序节点，客户端崩溃自动释放
> - 监听前一个节点，避免惊群效应
> - 强一致性，适合金融场景
>
> **核心问题**：
> - 死锁 → 设置过期时间 / 临时节点
> - 误删 → 加锁时带 requestId，释放时验证
> - 续期 → Redisson 看门狗机制
>
> **选型**：一般用 Redis（性能高），强一致用 ZooKeeper。

---

*关联文档：[redis-lua-script.md](redis-lua-script.md) | [redis-use-cases.md](redis-use-cases.md)*
