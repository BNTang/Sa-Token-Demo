# Redis 中的缓存击穿、缓存穿透和缓存雪崩是什么？

## 概念区分

```
┌─────────────────────────────────────────────────────────────┐
│                    三种缓存问题对比                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   缓存穿透 (Cache Penetration)                              │
│   ├── 查询不存在的数据                                      │
│   ├── 缓存和数据库都没有                                    │
│   └── 每次都打到数据库                                      │
│                                                             │
│   缓存击穿 (Cache Breakdown)                                │
│   ├── 热点 Key 过期                                         │
│   ├── 大量请求同时打到数据库                                │
│   └── 单个热点 Key 问题                                     │
│                                                             │
│   缓存雪崩 (Cache Avalanche)                                │
│   ├── 大量 Key 同时过期                                     │
│   ├── 或 Redis 服务宕机                                     │
│   └── 大规模请求打到数据库                                  │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  穿透 < 击穿 < 雪崩 (影响范围递增)                   │  │
│   │                                                     │  │
│   │  穿透: 数据不存在                                    │  │
│   │  击穿: 单个热点 Key 过期                             │  │
│   │  雪崩: 大量 Key 过期 / Redis 宕机                    │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 一、缓存穿透 (Cache Penetration)

```
┌─────────────────────────────────────────────────────────────┐
│                    缓存穿透                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   问题场景:                                                  │
│   查询一个不存在的 ID (如 id=-1 或随机字符串)               │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │   请求 id=-1                                        │  │
│   │       │                                             │  │
│   │       ▼                                             │  │
│   │   [Redis] ──→ Miss (缓存没有)                       │  │
│   │       │                                             │  │
│   │       ▼                                             │  │
│   │   [MySQL] ──→ 查不到 (数据库也没有)                 │  │
│   │       │                                             │  │
│   │       ▼                                             │  │
│   │   返回 null (不缓存，下次还查库)                    │  │
│   │                                                     │  │
│   │   恶意请求: 大量不存在的 ID → 数据库崩溃            │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 解决方案

```java
/**
 * 方案1: 缓存空值
 */
public Object getById(Long id) {
    String key = "data:" + id;
    String cached = redisTemplate.opsForValue().get(key);
    
    if (cached != null) {
        if ("NULL".equals(cached)) {
            return null;  // 缓存的空值
        }
        return JSON.parseObject(cached, Data.class);
    }
    
    // 查数据库
    Data data = dataMapper.selectById(id);
    
    if (data == null) {
        // 缓存空值，设置较短过期时间
        redisTemplate.opsForValue().set(key, "NULL", 5, TimeUnit.MINUTES);
        return null;
    }
    
    redisTemplate.opsForValue().set(key, JSON.toJSONString(data), 30, TimeUnit.MINUTES);
    return data;
}
```

```java
/**
 * 方案2: 布隆过滤器 (推荐)
 */
@Component
public class BloomFilterService {
    
    private BloomFilter<Long> bloomFilter;
    
    @PostConstruct
    public void init() {
        // 预期元素数量 100万，误判率 0.01%
        bloomFilter = BloomFilter.create(
            Funnels.longFunnel(), 1_000_000, 0.0001);
        
        // 初始化：将所有有效 ID 加入布隆过滤器
        List<Long> allIds = dataMapper.selectAllIds();
        allIds.forEach(bloomFilter::put);
    }
    
    public Object getById(Long id) {
        // 先查布隆过滤器
        if (!bloomFilter.mightContain(id)) {
            return null;  // 一定不存在，直接返回
        }
        
        // 可能存在，查缓存和数据库
        // ...
    }
    
    // 新增数据时，加入布隆过滤器
    public void addData(Data data) {
        dataMapper.insert(data);
        bloomFilter.put(data.getId());
    }
}
```

```
┌─────────────────────────────────────────────────────────────┐
│                    布隆过滤器原理                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   原理: 多个哈希函数 + 位数组                                │
│                                                             │
│   添加元素:                                                  │
│   ┌─────────────────────────────────────────────────────┐  │
│   │   元素 "abc"                                        │  │
│   │       │                                             │  │
│   │   ┌───┼───────────────────────────────────┐        │  │
│   │   │ hash1 → 3  hash2 → 7  hash3 → 11      │        │  │
│   │   └───┼───────────────────────────────────┘        │  │
│   │       ▼                                             │  │
│   │   [0,0,0,1,0,0,0,1,0,0,0,1,0,0,0]                   │  │
│   │        ↑       ↑       ↑                            │  │
│   │       位3     位7     位11 设为1                    │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   查询元素:                                                  │
│   如果所有哈希位置都是1 → 可能存在                          │
│   如果有任一位置是0 → 一定不存在                            │
│                                                             │
│   特点:                                                      │
│   • 空间效率高                                              │
│   • 有误判率 (假阳性)，但不会漏判                           │
│   • 不能删除元素 (可用 Counting Bloom Filter)               │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 二、缓存击穿 (Cache Breakdown)

```
┌─────────────────────────────────────────────────────────────┐
│                    缓存击穿                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   问题场景:                                                  │
│   热点 Key 过期的瞬间，大量请求同时访问                      │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │   热点 Key "product:1001" 过期                      │  │
│   │                                                     │  │
│   │   请求1 ──→ [Redis] Miss ──→ [MySQL] 查询           │  │
│   │   请求2 ──→ [Redis] Miss ──→ [MySQL] 查询           │  │
│   │   请求3 ──→ [Redis] Miss ──→ [MySQL] 查询           │  │
│   │   ...                                               │  │
│   │   请求N ──→ [Redis] Miss ──→ [MySQL] 查询           │  │
│   │                                                     │  │
│   │   大量请求同时打到数据库！                           │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 解决方案

```java
/**
 * 方案1: 互斥锁 (分布式锁)
 */
public Object getHotData(String key) {
    String cached = redisTemplate.opsForValue().get(key);
    if (cached != null) {
        return JSON.parseObject(cached, Data.class);
    }
    
    // 获取分布式锁
    String lockKey = "lock:" + key;
    boolean locked = redisTemplate.opsForValue()
        .setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
    
    if (locked) {
        try {
            // 双重检查
            cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return JSON.parseObject(cached, Data.class);
            }
            
            // 查数据库
            Data data = dataMapper.selectByKey(key);
            redisTemplate.opsForValue().set(key, JSON.toJSONString(data), 30, TimeUnit.MINUTES);
            return data;
        } finally {
            redisTemplate.delete(lockKey);
        }
    } else {
        // 未获取到锁，等待后重试
        Thread.sleep(100);
        return getHotData(key);  // 递归重试
    }
}
```

```java
/**
 * 方案2: 逻辑过期 (不设置 TTL)
 */
@Data
public class CacheData {
    private Object data;
    private long expireTime;  // 逻辑过期时间
}

public Object getDataWithLogicalExpire(String key) {
    String cached = redisTemplate.opsForValue().get(key);
    if (cached == null) {
        return null;
    }
    
    CacheData cacheData = JSON.parseObject(cached, CacheData.class);
    
    // 未过期，直接返回
    if (System.currentTimeMillis() < cacheData.getExpireTime()) {
        return cacheData.getData();
    }
    
    // 已过期，异步更新
    String lockKey = "lock:" + key;
    if (redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS)) {
        // 异步更新缓存
        executor.submit(() -> {
            try {
                Data data = dataMapper.selectByKey(key);
                CacheData newCache = new CacheData();
                newCache.setData(data);
                newCache.setExpireTime(System.currentTimeMillis() + 30 * 60 * 1000);
                redisTemplate.opsForValue().set(key, JSON.toJSONString(newCache));
            } finally {
                redisTemplate.delete(lockKey);
            }
        });
    }
    
    // 返回旧数据 (可接受短暂的数据不一致)
    return cacheData.getData();
}
```

```java
/**
 * 方案3: 热点数据永不过期 + 定时刷新
 */
@Component
public class HotDataRefresher {
    
    @Scheduled(fixedRate = 60000)  // 每分钟刷新
    public void refreshHotData() {
        List<String> hotKeys = getHotKeys();
        for (String key : hotKeys) {
            Data data = dataMapper.selectByKey(key);
            redisTemplate.opsForValue().set(key, JSON.toJSONString(data));
            // 不设置过期时间，永不过期
        }
    }
}
```

## 三、缓存雪崩 (Cache Avalanche)

```
┌─────────────────────────────────────────────────────────────┐
│                    缓存雪崩                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   问题场景:                                                  │
│   1. 大量 Key 同时过期                                      │
│   2. Redis 服务宕机                                          │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │   同一时刻，1000 个 Key 过期                        │  │
│   │                                                     │  │
│   │   请求群 ──→ [Redis] 全部 Miss ──→ [MySQL]          │  │
│   │                                     ↓               │  │
│   │                                 数据库崩溃           │  │
│   │                                     ↓               │  │
│   │                               服务不可用             │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 解决方案

```java
/**
 * 方案1: 过期时间加随机值
 */
public void setWithRandomExpire(String key, Object value, long baseExpireSeconds) {
    // 基础过期时间 + 随机时间 (0-5分钟)
    long randomSeconds = new Random().nextInt(300);
    long expireSeconds = baseExpireSeconds + randomSeconds;
    
    redisTemplate.opsForValue().set(key, JSON.toJSONString(value), 
        expireSeconds, TimeUnit.SECONDS);
}

// 批量设置缓存
public void batchSetCache(Map<String, Object> dataMap) {
    dataMap.forEach((key, value) -> {
        setWithRandomExpire(key, value, 3600);  // 基础1小时
    });
}
```

```java
/**
 * 方案2: 多级缓存
 */
@Service
public class MultiLevelCacheService {
    
    // L1: 本地缓存 (Caffeine)
    private Cache<String, Object> localCache = Caffeine.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build();
    
    // L2: Redis
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    public Object get(String key) {
        // L1: 本地缓存
        Object local = localCache.getIfPresent(key);
        if (local != null) {
            return local;
        }
        
        // L2: Redis
        String redis = redisTemplate.opsForValue().get(key);
        if (redis != null) {
            Object data = JSON.parseObject(redis, Object.class);
            localCache.put(key, data);  // 回填本地缓存
            return data;
        }
        
        // L3: 数据库
        Object db = loadFromDB(key);
        if (db != null) {
            redisTemplate.opsForValue().set(key, JSON.toJSONString(db), 
                randomExpire(3600), TimeUnit.SECONDS);
            localCache.put(key, db);
        }
        return db;
    }
}
```

```java
/**
 * 方案3: 熔断降级 (Sentinel/Hystrix)
 */
@Service
public class DataService {
    
    @SentinelResource(value = "getData", 
        fallback = "getDataFallback",
        blockHandler = "getDataBlockHandler")
    public Object getData(String key) {
        // 正常逻辑
        return loadFromCache(key);
    }
    
    // 熔断后的降级方法
    public Object getDataFallback(String key, Throwable e) {
        // 返回默认值或从本地缓存获取
        return getDefault(key);
    }
    
    // 限流后的处理
    public Object getDataBlockHandler(String key, BlockException e) {
        return "系统繁忙，请稍后重试";
    }
}
```

```
┌─────────────────────────────────────────────────────────────┐
│                    雪崩预防措施                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   预防:                                                      │
│   1. 过期时间加随机值                                       │
│   2. 多级缓存 (本地 + Redis)                                │
│   3. Redis 高可用 (集群 + 哨兵)                             │
│                                                             │
│   发生后:                                                    │
│   1. 熔断降级                                               │
│   2. 限流                                                   │
│   3. 返回默认值/缓存数据                                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 对比总结

```
┌─────────────────────────────────────────────────────────────┐
│                    三种问题对比                              │
├──────────────┬──────────────────────────────────────────────┤
│   问题        │   原因             │   解决方案             │
├──────────────┼───────────────────┼───────────────────────┤
│   穿透        │   数据不存在      │ 布隆过滤器/缓存空值    │
│   击穿        │   热点Key过期     │ 互斥锁/逻辑过期/不过期 │
│   雪崩        │   大量Key同时过期 │ 随机过期/多级缓存/限流 │
├──────────────┴───────────────────┴───────────────────────┤
│                                                             │
│   共同思路:                                                  │
│   • 减少对数据库的直接访问                                  │
│   • 控制并发请求数                                          │
│   • 保证数据的可用性                                        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> **缓存穿透**：查询不存在的数据，解决方案是**布隆过滤器**或**缓存空值**。**缓存击穿**：热点 Key 过期，大量请求打到数据库，解决方案是**互斥锁**或**逻辑过期**。**缓存雪崩**：大量 Key 同时过期或 Redis 宕机，解决方案是**随机过期时间**、**多级缓存**、**熔断限流**。

### 1分钟版本

> **缓存穿透**（数据不存在）：
> - 原因：恶意请求查询不存在的数据
> - 方案：布隆过滤器拦截 / 缓存空值（短TTL）
>
> **缓存击穿**（热点Key过期）：
> - 原因：热点 Key 过期瞬间大量请求
> - 方案：
>   - 互斥锁：只让一个请求查库
>   - 逻辑过期：数据永不过期，异步更新
>   - 定时刷新：热点数据提前续期
>
> **缓存雪崩**（大量Key同时过期）：
> - 原因：Key 过期时间相同 / Redis 宕机
> - 方案：
>   - 过期时间加随机值
>   - 多级缓存（本地缓存兜底）
>   - Redis 高可用（集群+哨兵）
>   - 熔断降级（返回默认值）

---

*关联文档：[redis-hotkey.md](redis-hotkey.md) | [redis-bigkey.md](redis-bigkey.md) | [service-avalanche.md](../09-microservice/service-avalanche.md)*
