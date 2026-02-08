# 如何解决 Redis 中的热点 Key 问题？

## 热点 Key 问题分析

```
┌─────────────────────────────────────────────────────────────┐
│                    热点 Key 问题                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   定义：某个 Key 被大量并发请求访问                          │
│                                                             │
│   常见场景：                                                 │
│   ├── 热门商品详情 (双11爆款)                               │
│   ├── 热点新闻/明星八卦                                     │
│   ├── 秒杀商品库存                                          │
│   └── 热门直播间数据                                         │
│                                                             │
│   危害：                                                     │
│   ├── 单 Redis 节点 CPU 飙升                                │
│   ├── 该节点带宽打满                                         │
│   ├── 连接数耗尽                                            │
│   ├── 其他 Key 请求受影响                                   │
│   └── 集群模式下负载不均衡                                   │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │                   Redis 集群                         │  │
│   │   ┌───────┐   ┌───────┐   ┌───────┐               │  │
│   │   │ Node1 │   │ Node2 │   │ Node3 │               │  │
│   │   │  5%   │   │ 90% ← │   │  5%   │               │  │
│   │   └───────┘   └───────┘   └───────┘               │  │
│   │               热点Key在这                            │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 热点 Key 发现

```java
/**
 * 热点 Key 发现方法
 */
@Component
public class HotKeyDetector {
    
    // 方法1: redis-cli --hotkeys (需开启 maxmemory-policy)
    // redis-cli --hotkeys
    
    // 方法2: MONITOR 命令 (生产慎用，性能影响大)
    // redis-cli MONITOR
    
    // 方法3: 业务埋点统计
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    private static final String HOT_KEY_COUNTER = "hotkey:counter";
    
    public void recordAccess(String key) {
        // 使用 HyperLogLog 或计数器统计访问量
        redisTemplate.opsForZSet().incrementScore(HOT_KEY_COUNTER, key, 1);
    }
    
    public Set<ZSetOperations.TypedTuple<String>> getTopHotKeys(int n) {
        return redisTemplate.opsForZSet()
            .reverseRangeWithScores(HOT_KEY_COUNTER, 0, n - 1);
    }
    
    // 方法4: 使用京东 hotkey 框架自动探测
    // https://github.com/jd-opensource/hotkey
}
```

## 解决方案

### 方案一：本地缓存

```java
/**
 * 本地缓存 (Caffeine) + Redis 多级缓存
 */
@Service
public class MultiLevelCacheService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    // 本地缓存：1000个热点Key，10秒过期
    private Cache<String, String> localCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(10, TimeUnit.SECONDS)
        .build();
    
    public String get(String key) {
        // L1: 本地缓存
        String value = localCache.getIfPresent(key);
        if (value != null) {
            return value;
        }
        
        // L2: Redis
        value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            localCache.put(key, value);
        }
        
        return value;
    }
    
    // 注意：本地缓存需要考虑一致性问题
    // 可通过 Redis Pub/Sub 通知各节点清除本地缓存
    public void invalidate(String key) {
        localCache.invalidate(key);
        redisTemplate.delete(key);
        // 发布失效消息
        redisTemplate.convertAndSend("cache:invalidate", key);
    }
}
```

```
┌─────────────────────────────────────────────────────────────┐
│                    本地缓存方案                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ┌─────────┐   ┌─────────┐   ┌─────────┐                 │
│   │ App-1   │   │ App-2   │   │ App-3   │                 │
│   │┌───────┐│   │┌───────┐│   │┌───────┐│                 │
│   ││Caffeine│   ││Caffeine│   ││Caffeine│                 │
│   │└───────┘│   │└───────┘│   │└───────┘│                 │
│   └────┬────┘   └────┬────┘   └────┬────┘                 │
│        │             │             │                       │
│        └─────────────┼─────────────┘                       │
│                      ▼                                     │
│             ┌─────────────────┐                            │
│             │     Redis       │                            │
│             │ 热点Key访问减少 │                             │
│             └─────────────────┘                            │
│                                                             │
│   优点：大幅减少 Redis 访问                                  │
│   缺点：本地缓存数据可能不一致                               │
│   适用：可接受短时间数据不一致的场景                         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 方案二：Key 分片（读写分离）

```java
/**
 * 热点 Key 分片
 * 将一个 Key 拆分为多个，分散到不同节点
 */
@Service
public class KeyShardingService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    private static final int SHARD_COUNT = 10;  // 分片数量
    
    /**
     * 写入时同时写入所有分片
     */
    public void set(String key, String value) {
        for (int i = 0; i < SHARD_COUNT; i++) {
            String shardKey = key + ":" + i;
            redisTemplate.opsForValue().set(shardKey, value);
        }
    }
    
    /**
     * 读取时随机选择一个分片
     */
    public String get(String key) {
        int shard = ThreadLocalRandom.current().nextInt(SHARD_COUNT);
        String shardKey = key + ":" + shard;
        return redisTemplate.opsForValue().get(shardKey);
    }
    
    /**
     * 计数场景：各分片累加
     */
    public void increment(String key) {
        int shard = ThreadLocalRandom.current().nextInt(SHARD_COUNT);
        String shardKey = key + ":counter:" + shard;
        redisTemplate.opsForValue().increment(shardKey);
    }
    
    public Long getCount(String key) {
        long total = 0;
        for (int i = 0; i < SHARD_COUNT; i++) {
            String shardKey = key + ":counter:" + i;
            String val = redisTemplate.opsForValue().get(shardKey);
            if (val != null) {
                total += Long.parseLong(val);
            }
        }
        return total;
    }
}
```

```
┌─────────────────────────────────────────────────────────────┐
│                    Key 分片方案                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   原始热点 Key: product:123                                  │
│                                                             │
│   分片后:                                                    │
│   ┌───────────────────────────────────────────────────────┐│
│   │product:123:0 │product:123:1 │... │product:123:9      ││
│   └───────────────────────────────────────────────────────┘│
│        ↓              ↓                    ↓              │
│   ┌───────┐      ┌───────┐           ┌───────┐           │
│   │ Node1 │      │ Node2 │    ...    │ Node5 │           │
│   │  20%  │      │  20%  │           │  20%  │           │
│   └───────┘      └───────┘           └───────┘           │
│                                                             │
│   读取时随机选择一个分片，负载均摊                           │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 方案三：从节点读取

```java
/**
 * 读写分离 + 从节点负载均衡
 */
@Configuration
public class RedisReadWriteConfig {
    
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
            .readFrom(ReadFrom.REPLICA_PREFERRED)  // 优先从从节点读
            .build();
        
        RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration()
            .clusterNode("node1", 7000)
            .clusterNode("node2", 7001)
            .clusterNode("node3", 7002);
        
        return new LettuceConnectionFactory(clusterConfig, clientConfig);
    }
}
```

### 方案四：提前预热

```java
/**
 * 热点 Key 预热
 * 大促前提前将热点数据加载到缓存
 */
@Component
public class CacheWarmup implements ApplicationRunner {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Autowired
    private ProductService productService;
    
    @Override
    public void run(ApplicationArguments args) {
        // 启动时预热热点商品
        List<Long> hotProductIds = Arrays.asList(1001L, 1002L, 1003L);
        
        for (Long productId : hotProductIds) {
            Product product = productService.getFromDB(productId);
            String key = "product:" + productId;
            redisTemplate.opsForValue().set(key, JSON.toJSONString(product), 
                Duration.ofHours(1));
        }
    }
    
    // 定时任务续期热点 Key
    @Scheduled(fixedRate = 300000)  // 5分钟
    public void refreshHotKeys() {
        // 续期即将过期的热点 Key
    }
}
```

### 方案五：JD HotKey 框架

```java
/**
 * 使用京东 HotKey 框架
 * 自动探测 + 本地缓存
 */
@Service
public class JdHotKeyService {
    
    // 1. 引入依赖
    // <dependency>
    //     <groupId>com.jd.platform.hotkey</groupId>
    //     <artifactId>hotkey-client</artifactId>
    // </dependency>
    
    public String get(String key) {
        // 判断是否是热点 Key
        if (JdHotKeyStore.isHotKey(key)) {
            // 从本地缓存获取
            Object value = JdHotKeyStore.get(key);
            if (value != null) {
                return (String) value;
            }
        }
        
        // 从 Redis 获取
        String value = redisTemplate.opsForValue().get(key);
        
        // 如果是热点，存入本地缓存
        if (JdHotKeyStore.isHotKey(key)) {
            JdHotKeyStore.smartSet(key, value);
        }
        
        return value;
    }
}
```

## 方案对比

```
┌─────────────────────────────────────────────────────────────┐
│                    方案对比                                  │
├──────────────┬─────────────────────────────────────────────┤
│   方案        │   优缺点                                    │
├──────────────┼─────────────────────────────────────────────┤
│   本地缓存    │   ✅ 减少 Redis 访问                        │
│              │   ❌ 一致性问题                              │
│              │   适用: 数据不常变，可接受短暂不一致          │
├──────────────┼─────────────────────────────────────────────┤
│   Key 分片    │   ✅ 负载均摊到多个节点                      │
│              │   ❌ 写入成本增加，需要维护多个 Key           │
│              │   适用: 读多写少场景                         │
├──────────────┼─────────────────────────────────────────────┤
│   从节点读取  │   ✅ 利用现有从节点分摊读压力                │
│              │   ❌ 从节点可能有延迟                         │
│              │   适用: 已有主从架构                         │
├──────────────┼─────────────────────────────────────────────┤
│   提前预热    │   ✅ 避免缓存击穿                            │
│              │   ❌ 需要提前知道热点 Key                     │
│              │   适用: 大促等可预期场景                     │
├──────────────┼─────────────────────────────────────────────┤
│   HotKey框架  │   ✅ 自动探测 + 本地缓存                     │
│              │   ❌ 需要额外部署探测服务                     │
│              │   适用: 大规模系统                           │
└──────────────┴─────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> 热点 Key 解决方案：1）**本地缓存**（Caffeine）减少 Redis 访问；2）**Key 分片**，将热点 Key 拆分为多个分散到不同节点；3）**读写分离**，从节点分摊读压力；4）**提前预热**；5）使用京东 **HotKey 框架**自动探测和本地缓存。

### 1分钟版本

> **热点 Key 危害**：
> - 单节点压力过大，CPU/带宽打满
> - 影响同节点其他 Key 请求
>
> **发现方法**：
> - `redis-cli --hotkeys`
> - 业务埋点统计
> - HotKey 框架自动探测
>
> **解决方案**：
>
> 1. **本地缓存**：Caffeine 缓存热点数据，10秒过期
>    - 缺点：需处理一致性（Pub/Sub 通知失效）
>
> 2. **Key 分片**：`product:123` 拆分为 `product:123:0~9`
>    - 读取随机选一个分片，负载均摊
>
> 3. **从节点读取**：`ReadFrom.REPLICA_PREFERRED`
>
> 4. **HotKey 框架**：自动探测热点，自动本地缓存
>
> **最佳实践**：本地缓存 + Key 分片组合使用。

---

*关联文档：[redis-bigkey.md](redis-bigkey.md) | [redis-use-cases.md](redis-use-cases.md) | [seckill-design.md](../14-system-design/seckill-design.md)*
