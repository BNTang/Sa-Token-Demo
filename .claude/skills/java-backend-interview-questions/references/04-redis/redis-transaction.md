# Redis 事务详解

> 分类: Redis | 难度: ⭐⭐⭐ | 频率: 高频

---

## 一、Redis 事务概述

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          Redis 事务特点                                           │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  Redis 事务是一组命令的集合，具有以下特点:                                        │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────────┐│
│  │  • 批量操作: 将多个命令打包一起执行                                          ││
│  │  • 顺序执行: 事务中的命令按顺序执行，不会被其他客户端打断                    ││
│  │  • 非原子性: 不支持回滚，部分命令失败不影响其他命令                          ││
│  │  • 无隔离级别: 事务执行前命令不会被实际执行                                  ││
│  └─────────────────────────────────────────────────────────────────────────────┘│
│                                                                                  │
│  ⚠️ 重要: Redis 事务不是传统意义上的 ACID 事务!                                  │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────────┐│
│  │  关系型数据库事务 (ACID)          │  Redis 事务                             ││
│  │  ─────────────────────────────────┼──────────────────────────────────────── ││
│  │  支持回滚                         │  ❌ 不支持回滚                           ││
│  │  原子性 (全部成功或全部失败)       │  ❌ 部分成功也会执行                     ││
│  │  隔离性 (多种隔离级别)             │  ✅ 命令排队，执行时不被打断              ││
│  │  持久性 (写入磁盘)                │  取决于持久化配置                        ││
│  └─────────────────────────────────────────────────────────────────────────────┘│
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 二、事务命令

### 2.1 基本命令

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          事务命令                                                 │
├─────────────────┬────────────────────────────────────────────────────────────────┤
│     命令        │                        说明                                     │
├─────────────────┼────────────────────────────────────────────────────────────────┤
│     MULTI       │  开启事务，后续命令进入队列                                     │
│     EXEC        │  执行事务中的所有命令                                           │
│     DISCARD     │  放弃事务，清空命令队列                                         │
│     WATCH       │  监视 key，如果事务执行前 key 被修改，事务中止                  │
│     UNWATCH     │  取消对所有 key 的监视                                          │
└─────────────────┴────────────────────────────────────────────────────────────────┘
```

### 2.2 执行流程

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          事务执行流程                                             │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│   客户端                                              Redis 服务器               │
│   ───────────────────────────────────────────────────────────────────────────    │
│                                                                                  │
│   MULTI ──────────────────────────────────────────→  开启事务                    │
│                                                      返回 OK                     │
│   SET k1 v1 ──────────────────────────────────────→  命令入队                    │
│                                                      返回 QUEUED                 │
│   SET k2 v2 ──────────────────────────────────────→  命令入队                    │
│                                                      返回 QUEUED                 │
│   INCR k3 ────────────────────────────────────────→  命令入队                    │
│                                                      返回 QUEUED                 │
│   EXEC ───────────────────────────────────────────→  执行所有命令                │
│                                                      返回执行结果数组             │
│                                                                                  │
│   ┌─────────────────────────────────────────────────────────────────────────┐   │
│   │  命令队列                                                                │   │
│   │  ┌────────┐  ┌────────┐  ┌────────┐                                     │   │
│   │  │SET k1  │→ │SET k2  │→ │INCR k3 │                                     │   │
│   │  └────────┘  └────────┘  └────────┘                                     │   │
│   │                    ↓ EXEC                                                │   │
│   │              顺序执行所有命令                                             │   │
│   └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 三、错误处理

### 3.1 语法错误 (编译时错误)

```redis
127.0.0.1:6379> MULTI
OK
127.0.0.1:6379> SET key1 value1
QUEUED
127.0.0.1:6379> SETTT key2 value2    # 命令拼写错误
(error) ERR unknown command 'SETTT'
127.0.0.1:6379> SET key3 value3
QUEUED
127.0.0.1:6379> EXEC
(error) EXECABORT Transaction discarded because of previous errors.

# 语法错误: 整个事务被取消，所有命令都不执行
```

### 3.2 运行时错误

```redis
127.0.0.1:6379> SET name "hello"
OK
127.0.0.1:6379> MULTI
OK
127.0.0.1:6379> INCR name           # name 是字符串，不能 INCR
QUEUED
127.0.0.1:6379> SET key1 value1
QUEUED
127.0.0.1:6379> EXEC
1) (error) ERR value is not an integer or out of range
2) OK

# 运行时错误: 错误的命令返回错误，其他命令正常执行
# ⚠️ Redis 不支持回滚!
```

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          错误处理对比                                             │
├─────────────────────────┬────────────────────────────────────────────────────────┤
│   语法错误 (编译时)      │  整个事务取消，所有命令都不执行                         │
│   运行时错误             │  错误命令失败，其他命令正常执行 (无回滚)                │
└─────────────────────────┴────────────────────────────────────────────────────────┘

为什么 Redis 不支持回滚?
• Redis 作者认为事务错误通常是编程错误，不应该在生产环境出现
• 不需要回滚可以保持 Redis 的简单和高性能
• 实际上很少需要回滚功能
```

---

## 四、WATCH 乐观锁

### 4.1 WATCH 机制

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          WATCH 乐观锁                                             │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  WATCH 用于监视一个或多个 key:                                                    │
│  • 在 EXEC 执行时，如果被监视的 key 被其他客户端修改，事务失败                   │
│  • 类似于 CAS (Compare-And-Swap) 的乐观锁                                        │
│                                                                                  │
│   客户端 A                                客户端 B                               │
│   ────────────────────────────────────────────────────────────────────────────   │
│   WATCH balance                                                                  │
│   GET balance  → 100                                                             │
│   MULTI                                                                          │
│   SET balance 90                          SET balance 50   ← 修改了 balance      │
│   EXEC ──────────────────────────────────────────────────→ 返回 nil (失败)       │
│                                                                                  │
│   事务失败，因为 balance 在 WATCH 之后被其他客户端修改                           │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

### 4.2 WATCH 使用示例

```redis
# 实现安全的余额扣减

WATCH balance                # 监视 balance
balance_val = GET balance    # 读取当前余额
if (balance_val >= 100) {
    MULTI
    DECRBY balance 100       # 扣减 100
    EXEC                     # 如果 balance 被修改，EXEC 返回 nil
    if (result == nil) {
        # 事务失败，重试
        retry()
    }
} else {
    UNWATCH                  # 不需要事务，取消监视
}
```

---

## 五、Java 实现

### 5.1 Jedis 事务

```java
/**
 * Jedis 事务示例
 */
public class JedisTransactionExample {
    
    public void basicTransaction(Jedis jedis) {
        // 开启事务
        Transaction tx = jedis.multi();
        
        try {
            tx.set("key1", "value1");
            tx.set("key2", "value2");
            tx.incr("counter");
            
            // 执行事务，返回每个命令的结果
            List<Object> results = tx.exec();
            
            for (Object result : results) {
                System.out.println(result);
            }
        } catch (Exception e) {
            tx.discard();  // 放弃事务
            throw e;
        }
    }
    
    /**
     * 使用 WATCH 实现乐观锁
     */
    public boolean secureDecrBalance(Jedis jedis, String key, int amount) {
        while (true) {
            jedis.watch(key);  // 监视 key
            
            int balance = Integer.parseInt(jedis.get(key));
            if (balance < amount) {
                jedis.unwatch();
                return false;  // 余额不足
            }
            
            Transaction tx = jedis.multi();
            tx.decrBy(key, amount);
            
            List<Object> results = tx.exec();
            if (results != null) {
                return true;  // 事务成功
            }
            // results == null 表示事务失败，重试
        }
    }
}
```

### 5.2 Lettuce 事务

```java
/**
 * Lettuce 事务示例
 */
public class LettuceTransactionExample {
    
    public void transaction(RedisCommands<String, String> commands) {
        commands.multi();
        
        commands.set("key1", "value1");
        commands.set("key2", "value2");
        commands.incr("counter");
        
        TransactionResult results = commands.exec();
        
        for (Object result : results) {
            System.out.println(result);
        }
    }
}
```

### 5.3 Spring Data Redis 事务

```java
/**
 * Spring Data Redis 事务
 */
@Service
public class RedisTransactionService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    public void executeTransaction() {
        // 方式1: SessionCallback
        redisTemplate.execute(new SessionCallback<List<Object>>() {
            @Override
            public List<Object> execute(RedisOperations operations) {
                operations.multi();
                
                operations.opsForValue().set("key1", "value1");
                operations.opsForValue().set("key2", "value2");
                operations.opsForValue().increment("counter");
                
                return operations.exec();
            }
        });
        
        // 方式2: 使用 @Transactional (需要配置)
    }
    
    /**
     * WATCH + 事务
     */
    public boolean secureTransfer(String from, String to, int amount) {
        return Boolean.TRUE.equals(redisTemplate.execute(
            new SessionCallback<Boolean>() {
                @Override
                public Boolean execute(RedisOperations ops) {
                    ops.watch(from);
                    
                    Integer balance = Integer.parseInt(
                        (String) ops.opsForValue().get(from));
                    
                    if (balance == null || balance < amount) {
                        ops.unwatch();
                        return false;
                    }
                    
                    ops.multi();
                    ops.opsForValue().decrement(from, amount);
                    ops.opsForValue().increment(to, amount);
                    
                    List<Object> results = ops.exec();
                    return results != null && !results.isEmpty();
                }
            }
        ));
    }
}
```

---

## 六、Lua 脚本 vs 事务

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          Lua 脚本 vs 事务                                         │
├─────────────────────────┬────────────────────────────────────────────────────────┤
│        事务              │                 Lua 脚本                               │
├─────────────────────────┼────────────────────────────────────────────────────────┤
│  不支持条件判断          │  ✅ 支持完整的逻辑控制                                 │
│  不能获取中间结果        │  ✅ 可以获取中间结果并基于此决策                       │
│  网络开销: 多次往返       │  ✅ 一次网络请求                                       │
│  原子性: 部分             │  ✅ 整个脚本原子执行                                   │
└─────────────────────────┴────────────────────────────────────────────────────────┘

推荐: 复杂场景使用 Lua 脚本代替事务
```

```java
/**
 * Lua 脚本实现原子操作
 */
public class LuaScriptExample {
    
    private static final String TRANSFER_SCRIPT = 
        "local from = KEYS[1] " +
        "local to = KEYS[2] " +
        "local amount = tonumber(ARGV[1]) " +
        "local balance = tonumber(redis.call('GET', from)) " +
        "if balance >= amount then " +
        "    redis.call('DECRBY', from, amount) " +
        "    redis.call('INCRBY', to, amount) " +
        "    return 1 " +
        "else " +
        "    return 0 " +
        "end";
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    public boolean transfer(String from, String to, int amount) {
        RedisScript<Long> script = new DefaultRedisScript<>(TRANSFER_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script, 
            Arrays.asList(from, to), 
            String.valueOf(amount));
        return result != null && result == 1;
    }
}
```

---

## 七、面试回答

### 30秒版本

> Redis 支持事务，使用 `MULTI/EXEC/DISCARD` 命令。
>
> **特点**：
> - 命令打包顺序执行，不会被其他客户端打断
> - **不支持回滚**（运行时错误不影响其他命令）
> - 使用 `WATCH` 实现乐观锁
>
> **与传统事务区别**：
> - 不保证原子性（部分失败不回滚）
> - 不支持隔离级别
>
> 复杂场景推荐用 **Lua 脚本**代替事务。

### 1分钟版本

> **Redis 事务命令**：
> - `MULTI`：开启事务
> - `EXEC`：执行事务
> - `DISCARD`：放弃事务
> - `WATCH`：乐观锁，监视 key 变化
>
> **事务特点**：
> 1. 命令入队后顺序执行，执行期间不会被其他客户端打断
> 2. 语法错误（如命令拼写错误）：整个事务取消
> 3. 运行时错误（如类型错误）：错误命令失败，其他命令正常执行，不回滚
>
> **WATCH 乐观锁**：
> 监视 key，如果在 EXEC 前被其他客户端修改，事务失败返回 nil，需要重试。
>
> **与传统 ACID 事务对比**：
> Redis 事务不是真正的 ACID 事务，不支持回滚，原因是作者认为事务错误是编程错误，且不回滚可以保持简单和高性能。
>
> **实践建议**：
> 简单场景用事务，需要条件判断或获取中间结果用 Lua 脚本（原子性更好）。
