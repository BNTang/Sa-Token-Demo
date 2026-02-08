# Redis 性能优化

> Java 后端面试知识点 - Redis 实践

---

## 性能瓶颈分析

### 常见瓶颈

| 瓶颈类型 | 表现 | 原因 |
|---------|------|------|
| **CPU** | 单线程满载 | 复杂命令、大量计算 |
| **内存** | OOM、频繁淘汰 | 数据量大、无 TTL |
| **网络** | 延迟高、带宽满 | 大 Key、大量请求 |
| **持久化** | 延迟抖动 | RDB fork、AOF 重写 |
| **慢查询** | 响应慢 | O(n) 命令、大 Key |

### 诊断命令

```bash
# 实时监控
redis-cli monitor

# 慢查询日志
CONFIG SET slowlog-log-slower-than 10000  # 10ms
CONFIG SET slowlog-max-len 128
SLOWLOG GET 10

# 内存分析
INFO memory
MEMORY DOCTOR

# 客户端连接
INFO clients
CLIENT LIST

# 大 Key 扫描
redis-cli --bigkeys

# 热 Key 分析（Redis 4.0+）
redis-cli --hotkeys
```

---

## 优化方案

### 1. 命令优化

```java
// ❌ 避免 O(n) 命令操作大集合
KEYS *              // 全量扫描
SMEMBERS huge_set   // 大 Set
HGETALL huge_hash   // 大 Hash
LRANGE list 0 -1    // 全量 List

// ✅ 使用增量扫描
@Service
@RequiredArgsConstructor
public class RedisScanService {
    
    private final StringRedisTemplate redisTemplate;
    
    // 增量扫描 Keys
    public void scanKeys(String pattern, Consumer<String> consumer) {
        ScanOptions options = ScanOptions.scanOptions()
            .match(pattern)
            .count(100)  // 每次扫描 100 个
            .build();
        
        try (Cursor<String> cursor = redisTemplate
                .scan(options)) {
            while (cursor.hasNext()) {
                consumer.accept(cursor.next());
            }
        }
    }
    
    // 增量扫描 Hash
    public void scanHash(String key, Consumer<Map.Entry<Object, Object>> consumer) {
        ScanOptions options = ScanOptions.scanOptions()
            .count(100)
            .build();
        
        try (Cursor<Map.Entry<Object, Object>> cursor = redisTemplate
                .opsForHash()
                .scan(key, options)) {
            while (cursor.hasNext()) {
                consumer.accept(cursor.next());
            }
        }
    }
}
```

### 2. Pipeline 批量操作

```java
// ❌ 循环单个操作（n 次网络往返）
for (String key : keys) {
    redisTemplate.opsForValue().get(key);
}

// ✅ Pipeline（1 次网络往返）
@Service
@RequiredArgsConstructor
public class PipelineService {
    
    private final StringRedisTemplate redisTemplate;
    
    public List<Object> batchGet(List<String> keys) {
        return redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            StringRedisConnection stringConn = (StringRedisConnection) connection;
            for (String key : keys) {
                stringConn.get(key);
            }
            return null;
        });
    }
    
    public void batchSet(Map<String, String> data) {
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            StringRedisConnection stringConn = (StringRedisConnection) connection;
            data.forEach((key, value) -> {
                stringConn.setEx(key, 3600, value);
            });
            return null;
        });
    }
}
```

### 3. 大 Key 拆分

```java
// ❌ 一个大 Hash 存储所有用户
HSET users user:1 {...}
HSET users user:2 {...}
// ... 百万用户

// ✅ 分片存储
@Service
public class ShardingService {
    
    private static final int SHARD_COUNT = 100;
    
    private String getShardKey(String userId) {
        int shard = Math.abs(userId.hashCode()) % SHARD_COUNT;
        return "users:" + shard;
    }
    
    public void setUser(String userId, User user) {
        String shardKey = getShardKey(userId);
        redisTemplate.opsForHash().put(shardKey, userId, user);
    }
    
    public User getUser(String userId) {
        String shardKey = getShardKey(userId);
        return (User) redisTemplate.opsForHash().get(shardKey, userId);
    }
}

// ✅ 大 Value 压缩
@Component
public class CompressedCache {
    
    public void setCompressed(String key, Object value) {
        byte[] json = JsonUtils.toJsonBytes(value);
        byte[] compressed = Snappy.compress(json);
        redisTemplate.opsForValue().set(key, compressed);
    }
    
    public <T> T getCompressed(String key, Class<T> type) {
        byte[] compressed = redisTemplate.opsForValue().get(key);
        if (compressed == null) return null;
        byte[] json = Snappy.uncompress(compressed);
        return JsonUtils.fromJson(json, type);
    }
}
```

### 4. 连接池优化

```yaml
spring:
  redis:
    lettuce:
      pool:
        max-active: 50      # 最大连接数（根据 QPS 调整）
        max-idle: 20        # 最大空闲连接
        min-idle: 5         # 最小空闲连接
        max-wait: 3000ms    # 获取连接超时
        time-between-eviction-runs: 30s  # 空闲连接检测间隔
```

### 5. 内存优化

```bash
# Redis 配置
maxmemory 4gb                    # 最大内存
maxmemory-policy allkeys-lru     # 淘汰策略

# 压缩配置
hash-max-ziplist-entries 512
hash-max-ziplist-value 64
list-max-ziplist-size -2
set-max-intset-entries 512
zset-max-ziplist-entries 128
zset-max-ziplist-value 64
```

```java
// ✅ 合理设置 TTL
redisTemplate.opsForValue().set(key, value, 30, TimeUnit.MINUTES);

// ✅ 使用短 Key
String key = "u:" + userId;  // 而非 "user:info:detail:"

// ✅ 使用合适的数据结构
// 数值用 String（incr 操作）
// 对象用 Hash（可单独更新字段）
// 列表用 List + trim（限制长度）
```

### 6. 持久化优化

```bash
# RDB 优化
save 900 1          # 15分钟至少1个key变化才save
save 300 10         # 5分钟至少10个key变化
save 60 10000       # 1分钟至少10000个key变化

# AOF 优化
appendfsync everysec          # 每秒同步
auto-aof-rewrite-percentage 100
auto-aof-rewrite-min-size 64mb
aof-rewrite-incremental-fsync yes  # 增量写入

# 关闭 THP（透明大页）
echo never > /sys/kernel/mm/transparent_hugepage/enabled
```

### 7. 读写分离与集群

```java
// 读写分离配置
@Configuration
public class RedisReadWriteConfig {
    
    @Bean
    public LettuceClientConfiguration lettuceClientConfiguration() {
        return LettuceClientConfiguration.builder()
            .readFrom(ReadFrom.REPLICA_PREFERRED)  // 优先从副本读
            .build();
    }
}

// 集群配置
spring:
  redis:
    cluster:
      nodes:
        - 192.168.1.1:6379
        - 192.168.1.2:6379
        - 192.168.1.3:6379
      max-redirects: 3
```

---

## 缓存问题处理

### 缓存穿透

```java
// 布隆过滤器
@Service
@RequiredArgsConstructor
public class BloomFilterService {
    
    private final RedissonClient redissonClient;
    
    private RBloomFilter<Long> userIdFilter;
    
    @PostConstruct
    public void init() {
        userIdFilter = redissonClient.getBloomFilter("user:bf");
        userIdFilter.tryInit(1000000, 0.01);  // 100万数据，1%误判率
    }
    
    public User getUser(Long userId) {
        // 1. 布隆过滤器检查
        if (!userIdFilter.contains(userId)) {
            return null;  // 一定不存在
        }
        
        // 2. 查缓存
        // 3. 查数据库
        return userMapper.selectById(userId);
    }
}
```

### 缓存击穿

```java
// 分布式锁 + 双重检查
public User getUserWithLock(Long userId) {
    String cacheKey = "user:" + userId;
    User user = (User) redisTemplate.opsForValue().get(cacheKey);
    if (user != null) {
        return user;
    }
    
    String lockKey = "lock:user:" + userId;
    RLock lock = redissonClient.getLock(lockKey);
    try {
        if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
            // 双重检查
            user = (User) redisTemplate.opsForValue().get(cacheKey);
            if (user != null) {
                return user;
            }
            
            user = userMapper.selectById(userId);
            redisTemplate.opsForValue().set(cacheKey, user, 30, TimeUnit.MINUTES);
            return user;
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    } finally {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
    return null;
}
```

### 缓存雪崩

```java
// 随机过期时间
public void setWithRandomExpire(String key, Object value, int baseSeconds) {
    // 基础时间 + 随机时间（避免同时过期）
    int randomSeconds = ThreadLocalRandom.current().nextInt(0, 300);
    redisTemplate.opsForValue().set(key, value, 
        baseSeconds + randomSeconds, TimeUnit.SECONDS);
}
```

---

## 面试要点

### 核心答案

**问：Redis 性能瓶颈时如何处理？**

答：

**1. 诊断问题**
- `SLOWLOG GET` 查看慢查询
- `redis-cli --bigkeys` 扫描大 Key
- `INFO` 查看内存、连接、命中率

**2. 命令优化**
- 避免 `KEYS *`，使用 `SCAN`
- 避免 O(n) 命令操作大集合
- 使用 Pipeline 批量操作

**3. 大 Key 处理**
- 拆分大 Key 为多个小 Key
- 压缩大 Value
- 异步删除大 Key（UNLINK）

**4. 架构优化**
- 读写分离
- 集群分片
- 本地缓存 + Redis 多级缓存

**5. 内存优化**
- 设置合理 TTL
- 选择合适淘汰策略
- 使用紧凑数据结构

---

## 编码最佳实践

### ✅ 推荐做法

```java
// 1. 批量操作用 Pipeline
redisTemplate.executePipelined(...)

// 2. 大集合用 SCAN
cursor.scan(key, options)

// 3. 必须设置 TTL
redisTemplate.opsForValue().set(key, value, 30, TimeUnit.MINUTES);

// 4. 使用布隆过滤器防穿透
bloomFilter.contains(id)

// 5. 热点数据加锁防击穿
lock.tryLock(...)
```

### ❌ 避免做法

```java
// ❌ 使用 KEYS 命令
redisTemplate.keys("user:*");  // 阻塞 Redis

// ❌ 不设置过期时间
redisTemplate.opsForValue().set(key, value);

// ❌ 循环单个操作
for (String key : keys) {
    redisTemplate.opsForValue().get(key);
}

// ❌ 存储超大 Value
redisTemplate.opsForValue().set("huge", hugeJsonString);
```
