# Redis Lua 脚本功能是什么？如何使用？

## 概念解析

**Redis Lua 脚本** 允许在 Redis 服务端执行 Lua 脚本，将多个命令打包成**原子操作**执行，避免多次网络往返和竞态条件。

```
┌─────────────────────────────────────────────────────────────┐
│                   Lua 脚本的优势                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 原子性                                                 │
│      └── 脚本执行期间不会被其他命令打断                       │
│                                                             │
│   2. 减少网络开销                                            │
│      └── 多个操作一次发送，减少 RTT (Round-Trip Time)         │
│                                                             │
│   3. 复用性                                                 │
│      └── 脚本可缓存复用 (EVALSHA)                            │
│                                                             │
│   4. 灵活性                                                 │
│      └── 支持条件判断、循环等复杂逻辑                         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 基本语法

```
┌─────────────────────────────────────────────────────────────┐
│                   EVAL 命令格式                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   EVAL script numkeys key [key ...] arg [arg ...]           │
│                                                             │
│   • script   : Lua 脚本内容                                  │
│   • numkeys  : KEYS 数组的长度                               │
│   • key      : 操作的 Redis key (KEYS[1], KEYS[2]...)        │
│   • arg      : 额外参数 (ARGV[1], ARGV[2]...)                │
│                                                             │
│   Lua 脚本中:                                                │
│   • redis.call('命令', args)  : 执行命令，出错则中断          │
│   • redis.pcall('命令', args) : 执行命令，出错返回错误信息     │
│   • KEYS[n] : 访问第 n 个 key (从 1 开始)                    │
│   • ARGV[n] : 访问第 n 个参数 (从 1 开始)                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 代码示例

### 1. 基本使用 (redis-cli)

```bash
# 简单示例：设置并获取值
redis-cli EVAL "redis.call('SET', KEYS[1], ARGV[1]); return redis.call('GET', KEYS[1])" 1 mykey "hello"

# 带条件判断：如果 key 不存在则设置
redis-cli EVAL "if redis.call('EXISTS', KEYS[1]) == 0 then redis.call('SET', KEYS[1], ARGV[1]) return 1 else return 0 end" 1 mykey "value"
```

### 2. 分布式锁实现

```java
@Component
public class RedisDistributedLock {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    // 加锁脚本：SET NX EX 原子操作
    private static final String LOCK_SCRIPT = 
        "if redis.call('SET', KEYS[1], ARGV[1], 'NX', 'EX', ARGV[2]) then " +
        "    return 1 " +
        "else " +
        "    return 0 " +
        "end";
    
    // 解锁脚本：只有持有者才能解锁
    private static final String UNLOCK_SCRIPT = 
        "if redis.call('GET', KEYS[1]) == ARGV[1] then " +
        "    return redis.call('DEL', KEYS[1]) " +
        "else " +
        "    return 0 " +
        "end";
    
    private final DefaultRedisScript<Long> lockScript;
    private final DefaultRedisScript<Long> unlockScript;
    
    public RedisDistributedLock() {
        lockScript = new DefaultRedisScript<>(LOCK_SCRIPT, Long.class);
        unlockScript = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
    }
    
    /**
     * 获取锁
     * @param lockKey 锁的 key
     * @param requestId 请求标识（UUID）
     * @param expireSeconds 过期时间
     */
    public boolean tryLock(String lockKey, String requestId, int expireSeconds) {
        Long result = redisTemplate.execute(
            lockScript,
            Collections.singletonList(lockKey),
            requestId,
            String.valueOf(expireSeconds)
        );
        return Long.valueOf(1L).equals(result);
    }
    
    /**
     * 释放锁
     */
    public boolean unlock(String lockKey, String requestId) {
        Long result = redisTemplate.execute(
            unlockScript,
            Collections.singletonList(lockKey),
            requestId
        );
        return Long.valueOf(1L).equals(result);
    }
}
```

### 3. 限流器实现

```java
@Component
public class RateLimiter {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    // 滑动窗口限流脚本
    private static final String RATE_LIMIT_SCRIPT = 
        "local key = KEYS[1] " +
        "local limit = tonumber(ARGV[1]) " +      // 限制次数
        "local window = tonumber(ARGV[2]) " +      // 窗口大小(秒)
        "local now = tonumber(ARGV[3]) " +         // 当前时间戳
        "local windowStart = now - window * 1000 " + // 窗口开始时间
        
        // 移除窗口外的记录
        "redis.call('ZREMRANGEBYSCORE', key, 0, windowStart) " +
        
        // 获取当前窗口内的请求数
        "local count = redis.call('ZCARD', key) " +
        
        "if count < limit then " +
        "    redis.call('ZADD', key, now, now) " +  // 记录当前请求
        "    redis.call('EXPIRE', key, window) " +  // 设置过期时间
        "    return 1 " +                           // 允许请求
        "else " +
        "    return 0 " +                           // 拒绝请求
        "end";
    
    private final DefaultRedisScript<Long> rateLimitScript;
    
    public RateLimiter() {
        rateLimitScript = new DefaultRedisScript<>(RATE_LIMIT_SCRIPT, Long.class);
    }
    
    /**
     * 检查是否允许请求
     * @param key 限流 key
     * @param limit 限制次数
     * @param windowSeconds 时间窗口(秒)
     */
    public boolean isAllowed(String key, int limit, int windowSeconds) {
        Long result = redisTemplate.execute(
            rateLimitScript,
            Collections.singletonList("rate_limit:" + key),
            String.valueOf(limit),
            String.valueOf(windowSeconds),
            String.valueOf(System.currentTimeMillis())
        );
        return Long.valueOf(1L).equals(result);
    }
}
```

### 4. 库存扣减（秒杀）

```java
@Component
public class StockService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    // 扣减库存脚本
    private static final String DEDUCT_STOCK_SCRIPT = 
        "local stock = tonumber(redis.call('GET', KEYS[1])) " +
        "local quantity = tonumber(ARGV[1]) " +
        
        "if stock == nil then " +
        "    return -1 " +              // key 不存在
        "end " +
        
        "if stock < quantity then " +
        "    return -2 " +              // 库存不足
        "end " +
        
        "redis.call('DECRBY', KEYS[1], quantity) " +
        "return stock - quantity";      // 返回剩余库存
    
    private final DefaultRedisScript<Long> deductScript;
    
    public StockService() {
        deductScript = new DefaultRedisScript<>(DEDUCT_STOCK_SCRIPT, Long.class);
    }
    
    /**
     * 扣减库存
     * @return 剩余库存，-1=商品不存在，-2=库存不足
     */
    public long deductStock(String productId, int quantity) {
        Long result = redisTemplate.execute(
            deductScript,
            Collections.singletonList("stock:" + productId),
            String.valueOf(quantity)
        );
        return result != null ? result : -1;
    }
}
```

### 5. 脚本缓存 (EVALSHA)

```java
@Component
public class LuaScriptExecutor {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    /**
     * 使用 EVALSHA 执行脚本（避免每次传输脚本内容）
     * 
     * 原理：
     * 1. SCRIPT LOAD 将脚本加载到 Redis，返回 SHA1
     * 2. EVALSHA 通过 SHA1 执行脚本
     * 3. 如果 SHA1 不存在，降级使用 EVAL
     */
    public <T> T executeScript(RedisScript<T> script, List<String> keys, Object... args) {
        return redisTemplate.execute(script, keys, args);
        // Spring RedisTemplate 内部自动处理 EVALSHA/EVAL 降级
    }
    
    // 从文件加载脚本
    @Bean
    public DefaultRedisScript<Long> stockScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/deduct_stock.lua"));
        script.setResultType(Long.class);
        return script;
    }
}
```

**scripts/deduct_stock.lua**:
```lua
-- 扣减库存脚本
local stock = tonumber(redis.call('GET', KEYS[1]))
local quantity = tonumber(ARGV[1])

if stock == nil then
    return -1
end

if stock < quantity then
    return -2
end

redis.call('DECRBY', KEYS[1], quantity)
return stock - quantity
```

## 注意事项

```
┌─────────────────────────────────────────────────────────────┐
│                      使用注意事项                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 脚本原子性                                              │
│      └── 脚本执行期间会阻塞其他命令，避免复杂耗时脚本          │
│                                                             │
│   2. 超时配置                                               │
│      └── lua-time-limit 默认 5 秒，超时不会终止，但可响应     │
│          SCRIPT KILL 命令                                   │
│                                                             │
│   3. 集群限制                                               │
│      └── Redis Cluster 要求脚本中所有 key 必须在同一 slot    │
│      └── 使用 {hash_tag} 确保相关 key 落在同一节点            │
│                                                             │
│   4. 错误处理                                               │
│      └── redis.call() 出错会中断脚本                         │
│      └── redis.pcall() 出错返回错误，不中断                   │
│                                                             │
│   5. 副作用                                                 │
│      └── 脚本中避免随机/时间相关操作（影响主从复制）           │
│      └── 使用 redis.replicate_commands() 开启效果复制        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 最佳实践

### ✅ 推荐做法

```java
// 1. 脚本预编译，避免重复解析
@Configuration
public class RedisScriptConfig {
    
    @Bean
    public DefaultRedisScript<Long> lockScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText("...");
        script.setResultType(Long.class);
        return script;  // 单例复用
    }
}

// 2. 使用 hash tag 保证集群兼容
String key1 = "{order:123}:stock";
String key2 = "{order:123}:status";  // 同一 slot

// 3. 脚本保持简洁
// 复杂逻辑拆分成多个简单脚本
```

### ❌ 避免做法

```lua
-- 1. 避免大循环
for i = 1, 1000000 do
    redis.call('INCR', KEYS[1])  -- 长时间阻塞
end

-- 2. 避免在脚本中使用随机/时间
math.random()           -- 主从数据不一致
redis.call('TIME')      -- 不推荐

-- 3. 避免脚本中调用可能阻塞的命令
redis.call('BLPOP', ...)  -- 会阻塞整个 Redis
```

## 面试回答

### 30秒版本

> Redis Lua 脚本可以将多个命令打包成**原子操作**执行，优势是：原子性（执行期间不被打断）、减少网络 RTT、支持复杂逻辑。常用于分布式锁（SET NX + 验证解锁）、限流器（滑动窗口）、库存扣减等场景。注意脚本要简洁，避免阻塞 Redis。

### 1分钟版本

> **Lua 脚本的作用**：在 Redis 服务端执行脚本，将多命令原子执行，减少网络往返。
>
> **使用方式**：`EVAL script numkeys keys... args...`
> - `redis.call('命令', ...)` 执行 Redis 命令
> - `KEYS[n]` 访问 key，`ARGV[n]` 访问参数
>
> **典型场景**：
> - 分布式锁：加锁用 SET NX EX，解锁需验证 owner 再删除
> - 限流：滑动窗口用 ZSET 存储时间戳
> - 库存扣减：先判断库存再扣减，保证原子性
>
> **注意事项**：
> - 脚本执行期间阻塞其他命令，保持简洁
> - Redis Cluster 要求脚本中的 key 在同一 slot（用 `{hash_tag}`）
> - 使用 `EVALSHA` + 脚本缓存避免重复传输

---

*关联文档：[redis-transaction.md](redis-transaction.md) | [redis-pipeline.md](redis-pipeline.md)*
