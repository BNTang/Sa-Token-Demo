# Redis Pipeline 功能是什么？

## 概念解析

**Pipeline（管道）** 是 Redis 提供的批量执行命令的机制，将多个命令打包一次性发送给服务器，减少网络往返次数（RTT），显著提高性能。

```
┌─────────────────────────────────────────────────────────────┐
│               普通模式 vs Pipeline 模式                      │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   普通模式 (逐条执行):                                       │
│   ┌────────┐                              ┌────────┐       │
│   │ Client │ ── CMD1 ──→ 等待 ←── RESP1 ─ │ Server │       │
│   │        │ ── CMD2 ──→ 等待 ←── RESP2 ─ │        │       │
│   │        │ ── CMD3 ──→ 等待 ←── RESP3 ─ │        │       │
│   └────────┘                              └────────┘       │
│                                                             │
│   总时间 = N × (发送时间 + RTT + 处理时间)                   │
│                                                             │
│   ──────────────────────────────────────────────────────    │
│                                                             │
│   Pipeline 模式 (批量执行):                                  │
│   ┌────────┐                              ┌────────┐       │
│   │ Client │ ── CMD1 ──→                  │ Server │       │
│   │        │ ── CMD2 ──→                  │        │       │
│   │        │ ── CMD3 ──→                  │        │       │
│   │        │              ←── RESP1 ───── │        │       │
│   │        │              ←── RESP2 ───── │        │       │
│   │        │              ←── RESP3 ───── │        │       │
│   └────────┘                              └────────┘       │
│                                                             │
│   总时间 = 发送时间 + 1 × RTT + N × 处理时间                 │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 性能对比

```
┌─────────────────────────────────────────────────────────────┐
│                    性能提升示例                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   假设: RTT = 10ms, 命令处理 = 0.1ms, 发送 1000 个命令       │
│                                                             │
│   普通模式:                                                 │
│   1000 × (10ms + 0.1ms) ≈ 10100ms ≈ 10.1 秒                 │
│                                                             │
│   Pipeline 模式:                                            │
│   10ms + 1000 × 0.1ms = 110ms ≈ 0.11 秒                    │
│                                                             │
│   性能提升: 约 100 倍！                                      │
│                                                             │
│   注意: 实际提升取决于网络延迟，延迟越高提升越明显            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 代码示例

### 1. Jedis 使用 Pipeline

```java
// ❌ 普通模式：1000 次网络往返
public void normalSet(Jedis jedis) {
    for (int i = 0; i < 1000; i++) {
        jedis.set("key:" + i, "value:" + i);
    }
}

// ✅ Pipeline 模式：1 次网络往返
public void pipelineSet(Jedis jedis) {
    Pipeline pipeline = jedis.pipelined();
    
    for (int i = 0; i < 1000; i++) {
        pipeline.set("key:" + i, "value:" + i);
    }
    
    // 执行并获取结果
    List<Object> results = pipeline.syncAndReturnAll();
}

// Pipeline 读取数据
public Map<String, String> pipelineGet(Jedis jedis, List<String> keys) {
    Pipeline pipeline = jedis.pipelined();
    
    // 发送所有 GET 命令
    Map<String, Response<String>> responseMap = new HashMap<>();
    for (String key : keys) {
        responseMap.put(key, pipeline.get(key));
    }
    
    // 执行
    pipeline.sync();
    
    // 收集结果
    Map<String, String> result = new HashMap<>();
    for (Map.Entry<String, Response<String>> entry : responseMap.entrySet()) {
        result.put(entry.getKey(), entry.getValue().get());
    }
    return result;
}
```

### 2. Lettuce 使用 Pipeline

```java
@Service
public class RedisPipelineService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // Spring RedisTemplate Pipeline
    public List<Object> batchGet(List<String> keys) {
        return redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (String key : keys) {
                connection.get(key.getBytes());
            }
            return null;  // 返回值会被忽略，结果从 executePipelined 返回
        });
    }
    
    // 批量写入
    public void batchSet(Map<String, String> data) {
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            data.forEach((key, value) -> 
                connection.set(key.getBytes(), value.getBytes())
            );
            return null;
        });
    }
    
    // 批量操作多种类型
    public void batchOperations() {
        List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            connection.set("key1".getBytes(), "value1".getBytes());
            connection.incr("counter".getBytes());
            connection.hSet("hash".getBytes(), "field".getBytes(), "value".getBytes());
            connection.lPush("list".getBytes(), "item".getBytes());
            return null;
        });
        
        // results 包含每个命令的返回值
    }
}
```

### 3. Redisson 批量操作

```java
@Service
public class RedissonBatchService {
    
    @Autowired
    private RedissonClient redisson;
    
    public void batchOperations() {
        RBatch batch = redisson.createBatch();
        
        // 添加命令
        batch.getBucket("key1").setAsync("value1");
        batch.getBucket("key2").setAsync("value2");
        batch.getAtomicLong("counter").incrementAndGetAsync();
        batch.getMap("myMap").putAsync("field", "value");
        
        // 执行批量操作
        BatchResult<?> results = batch.execute();
        
        // 获取结果
        List<?> responses = results.getResponses();
    }
    
    // 带重试的批量操作
    public void batchWithRetry() {
        RBatch batch = redisson.createBatch(BatchOptions.defaults()
            .executionMode(BatchOptions.ExecutionMode.IN_MEMORY)  // 先在内存中累积
            .responseTimeout(3, TimeUnit.SECONDS)
            .retryInterval(1, TimeUnit.SECONDS)
            .retryAttempts(3));
        
        // 添加命令并执行
        batch.getBucket("key").setAsync("value");
        batch.execute();
    }
}
```

## Pipeline vs 事务 vs Lua

```
┌─────────────────────────────────────────────────────────────┐
│            Pipeline vs Transaction vs Lua                    │
├──────────────┬────────────┬────────────┬────────────────────┤
│   特性        │ Pipeline   │ Transaction │ Lua 脚本          │
├──────────────┼────────────┼────────────┼────────────────────┤
│   原子性      │   ❌ 否    │   ✅ 是     │   ✅ 是            │
│   减少 RTT    │   ✅ 是    │   ✅ 是     │   ✅ 是            │
│   条件逻辑    │   ❌ 否    │   ❌ 否     │   ✅ 是            │
│   回滚       │   ❌ 否    │   ❌ 否     │   ❌ 否（但原子执行）│
│   复杂度      │   简单     │   中等      │   复杂             │
│   使用场景    │   批量操作  │   简单事务   │   复杂原子操作     │
└──────────────┴────────────┴────────────┴────────────────────┘

Pipeline: 批量发送，批量返回，但命令之间可能被其他客户端插入
Transaction: MULTI/EXEC 保证原子执行，但不支持条件判断
Lua: 原子执行 + 支持条件逻辑，最强大但最复杂
```

```java
// Pipeline：不保证原子性
Pipeline pipeline = jedis.pipelined();
pipeline.set("a", "1");
// 其他客户端可能在这里执行命令
pipeline.set("b", "2");
pipeline.sync();

// Transaction：原子执行
Transaction tx = jedis.multi();
tx.set("a", "1");
tx.set("b", "2");
tx.exec();  // 原子执行

// Lua：原子执行 + 条件逻辑
String script = 
    "local val = redis.call('GET', KEYS[1]) " +
    "if val then " +
    "    return redis.call('SET', KEYS[2], val) " +
    "else " +
    "    return nil " +
    "end";
jedis.eval(script, 2, "source", "dest");
```

## 注意事项

```
┌─────────────────────────────────────────────────────────────┐
│                    Pipeline 注意事项                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 命令数量限制                                            │
│      └── 不要一次发送太多命令（建议 1000-10000）              │
│      └── 太多会占用大量内存，阻塞服务端                       │
│                                                             │
│   2. 不保证原子性                                            │
│      └── Pipeline 只是批量发送，不是事务                     │
│      └── 命令之间可能被其他客户端命令插入                     │
│                                                             │
│   3. 错误处理                                               │
│      └── 部分命令失败不影响其他命令                          │
│      └── 需要检查每个命令的返回结果                          │
│                                                             │
│   4. 集群环境                                               │
│      └── Redis Cluster 中，Pipeline 命令需在同一节点         │
│      └── 使用 hash tag {tag}key 确保同一 slot               │
│                                                             │
│   5. 内存消耗                                               │
│      └── 服务端和客户端都需要缓存所有响应                     │
│      └── 大量命令可能导致内存压力                            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 最佳实践

### ✅ 推荐做法

```java
// 1. 分批执行，控制每批数量
public void batchSetWithLimit(List<KeyValue> data, int batchSize) {
    Pipeline pipeline = jedis.pipelined();
    int count = 0;
    
    for (KeyValue kv : data) {
        pipeline.set(kv.key, kv.value);
        count++;
        
        if (count >= batchSize) {
            pipeline.sync();  // 每批执行一次
            count = 0;
        }
    }
    
    if (count > 0) {
        pipeline.sync();  // 执行剩余命令
    }
}

// 2. 处理返回结果
public Map<String, String> batchGetWithResult(List<String> keys) {
    Pipeline pipeline = jedis.pipelined();
    List<Response<String>> responses = new ArrayList<>();
    
    for (String key : keys) {
        responses.add(pipeline.get(key));
    }
    
    pipeline.sync();
    
    Map<String, String> result = new HashMap<>();
    for (int i = 0; i < keys.size(); i++) {
        result.put(keys.get(i), responses.get(i).get());
    }
    return result;
}

// 3. 集群环境使用 hash tag
String key1 = "{user:123}:profile";
String key2 = "{user:123}:settings";  // 同一 slot，可以 pipeline
```

### ❌ 避免做法

```java
// 1. 避免一次发送过多命令
for (int i = 0; i < 1000000; i++) {
    pipeline.set("key:" + i, "value");  // 内存爆炸！
}

// 2. 避免依赖前一个命令的结果
pipeline.set("key", "value");
pipeline.get("key");  // 无法用这个结果做条件判断
// 需要用 Lua 脚本

// 3. 集群环境避免跨 slot
pipeline.set("key1", "value1");  // 可能在 slot A
pipeline.set("key2", "value2");  // 可能在 slot B
// 会报错或性能下降
```

## 面试回答

### 30秒版本

> Pipeline 是 Redis 批量执行命令的机制，将多个命令打包一次发送，减少网络 RTT。例如 1000 个命令，普通模式需要 1000 次往返，Pipeline 只需 1 次，性能提升可达 **100 倍**。但 Pipeline 不保证原子性，需要原子性用事务或 Lua。

### 1分钟版本

> **Pipeline 原理**：客户端将多个命令打包发送，服务端批量执行并返回，减少网络往返次数。
>
> **性能提升**：假设 RTT=10ms，1000 个命令普通模式需要 10 秒，Pipeline 只需 0.1 秒，提升 100 倍。延迟越高提升越明显。
>
> **与事务/Lua 区别**：
> - Pipeline：只是批量发送，**不保证原子性**
> - Transaction：MULTI/EXEC 原子执行，但不支持条件逻辑
> - Lua：原子执行 + 支持条件逻辑
>
> **注意事项**：
> - 控制每批命令数量（1000-10000）
> - 处理部分命令失败的情况
> - Redis Cluster 需确保命令在同一 slot（hash tag）
>
> **使用场景**：批量读写、数据预热、批量统计等。

---

*关联文档：[redis-lua-script.md](redis-lua-script.md) | [redis-transaction.md](redis-transaction.md)*
