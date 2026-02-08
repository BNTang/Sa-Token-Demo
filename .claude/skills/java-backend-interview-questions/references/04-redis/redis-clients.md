# Redis 客户端选型

> Java 后端面试知识点 - Redis 实践

---

## Java Redis 客户端对比

| 客户端 | 连接模式 | 线程安全 | 特点 | 推荐场景 |
|--------|---------|---------|------|---------|
| **Jedis** | 同步阻塞 | 需连接池 | 简单直观，API 友好 | 简单应用 |
| **Lettuce** | 异步非阻塞 | 线程安全 | 高性能，响应式 | 高并发、Spring Boot 默认 |
| **Redisson** | 异步非阻塞 | 线程安全 | 功能丰富，分布式支持 | 分布式锁、复杂数据结构 |

---

## 1. Jedis

### 特点

- **同步阻塞** IO 模型
- 需要**连接池**保证线程安全
- API 简单，与 Redis 命令一一对应
- 不支持响应式编程

### 配置

```xml
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
    <version>4.4.0</version>
</dependency>
```

```java
@Configuration
public class JedisConfig {
    
    @Bean
    public JedisPool jedisPool() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(50);          // 最大连接数
        config.setMaxIdle(20);           // 最大空闲连接
        config.setMinIdle(5);            // 最小空闲连接
        config.setMaxWaitMillis(3000);   // 获取连接超时
        config.setTestOnBorrow(true);    // 借用时检测
        
        return new JedisPool(config, "localhost", 6379, 3000, "password");
    }
}

@Service
@RequiredArgsConstructor
public class JedisService {
    
    private final JedisPool jedisPool;
    
    public String get(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.get(key);
        }
    }
    
    public void set(String key, String value, int seconds) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(key, seconds, value);
        }
    }
}
```

---

## 2. Lettuce

### 特点

- **异步非阻塞** IO（基于 Netty）
- 单个连接**线程安全**，无需连接池
- 支持响应式编程（Reactive）
- **Spring Boot 2.x+ 默认客户端**
- 支持 Redis Cluster、Sentinel

### 配置

```yaml
# application.yml（Spring Boot 默认使用 Lettuce）
spring:
  redis:
    host: localhost
    port: 6379
    password: password
    timeout: 3000ms
    lettuce:
      pool:
        max-active: 50
        max-idle: 20
        min-idle: 5
        max-wait: 3000ms
```

```java
@Configuration
public class LettuceConfig {
    
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
            .commandTimeout(Duration.ofSeconds(3))
            .shutdownTimeout(Duration.ZERO)
            .build();
        
        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration();
        serverConfig.setHostName("localhost");
        serverConfig.setPort(6379);
        serverConfig.setPassword("password");
        
        return new LettuceConnectionFactory(serverConfig, clientConfig);
    }
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}
```

### 响应式使用

```java
@Service
@RequiredArgsConstructor
public class ReactiveRedisService {
    
    private final ReactiveRedisTemplate<String, String> reactiveTemplate;
    
    public Mono<String> get(String key) {
        return reactiveTemplate.opsForValue().get(key);
    }
    
    public Mono<Boolean> set(String key, String value, Duration ttl) {
        return reactiveTemplate.opsForValue().set(key, value, ttl);
    }
    
    // 批量操作
    public Flux<String> multiGet(List<String> keys) {
        return reactiveTemplate.opsForValue()
            .multiGet(keys)
            .flatMapMany(Flux::fromIterable);
    }
}
```

---

## 3. Redisson

### 特点

- 基于 Netty 异步非阻塞
- **功能最丰富**：分布式锁、限流器、信号量、布隆过滤器等
- 完善的**分布式数据结构**支持
- 支持 Redis 各种部署模式
- 适合复杂分布式场景

### 配置

```xml
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.24.0</version>
</dependency>
```

```yaml
# application.yml
spring:
  redis:
    redisson:
      config: |
        singleServerConfig:
          address: "redis://localhost:6379"
          password: password
          connectionPoolSize: 50
          connectionMinimumIdleSize: 10
          timeout: 3000
          retryAttempts: 3
          retryInterval: 1500
```

### 分布式锁使用

```java
@Service
@RequiredArgsConstructor
public class RedissonService {
    
    private final RedissonClient redissonClient;
    
    // 分布式锁
    public void executeWithLock(String lockKey, Runnable task) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 尝试加锁，最多等待 3 秒，锁定 10 秒后自动释放
            if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                try {
                    task.run();
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    // 可重入锁
    public void reentrantLock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        lock.lock();  // 默认 30 秒过期，有看门狗续期
        try {
            // 业务逻辑
        } finally {
            lock.unlock();
        }
    }
    
    // 读写锁
    public void readWriteLock(String lockKey) {
        RReadWriteLock rwLock = redissonClient.getReadWriteLock(lockKey);
        RLock readLock = rwLock.readLock();
        RLock writeLock = rwLock.writeLock();
        
        // 读操作
        readLock.lock();
        try {
            // 多个读可以并发
        } finally {
            readLock.unlock();
        }
        
        // 写操作
        writeLock.lock();
        try {
            // 写时独占
        } finally {
            writeLock.unlock();
        }
    }
}
```

### 分布式数据结构

```java
@Service
@RequiredArgsConstructor
public class RedissonDataStructureService {
    
    private final RedissonClient redissonClient;
    
    // 分布式 Map
    public void useMap() {
        RMap<String, User> map = redissonClient.getMap("users");
        map.put("user:1", new User(1L, "张三"));
        User user = map.get("user:1");
    }
    
    // 分布式队列
    public void useQueue() {
        RBlockingQueue<Task> queue = redissonClient.getBlockingQueue("tasks");
        queue.offer(new Task("task1"));
        Task task = queue.poll(10, TimeUnit.SECONDS);
    }
    
    // 限流器
    public boolean tryAcquire(String limiterKey) {
        RRateLimiter limiter = redissonClient.getRateLimiter(limiterKey);
        limiter.trySetRate(RateType.OVERALL, 100, 1, RateIntervalUnit.SECONDS);
        return limiter.tryAcquire();
    }
    
    // 布隆过滤器
    public void useBloomFilter() {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter("bf");
        bloomFilter.tryInit(100000, 0.03);  // 预期插入 10 万，误判率 3%
        bloomFilter.add("item1");
        boolean contains = bloomFilter.contains("item1");
    }
}
```

---

## 选型建议

| 场景 | 推荐客户端 | 原因 |
|------|-----------|------|
| 简单 CRUD | Lettuce | Spring Boot 默认，配置简单 |
| 分布式锁 | Redisson | 完善的锁实现，有看门狗 |
| 高并发场景 | Lettuce/Redisson | 异步非阻塞 |
| 复杂数据结构 | Redisson | Map/Queue/BloomFilter 等 |
| 限流/信号量 | Redisson | 内置支持 |
| 学习/调试 | Jedis | API 简单直观 |

---

## 面试要点

### 核心答案

**问：你在项目中使用的 Redis 客户端是什么？**

答：

我在项目中主要使用 **Redisson** 和 **Lettuce**：

1. **Lettuce**（基础操作）
   - Spring Boot 默认集成
   - 异步非阻塞，性能好
   - 用于普通的缓存读写

2. **Redisson**（分布式场景）
   - 分布式锁（有看门狗自动续期）
   - 限流器、信号量
   - 布隆过滤器（防缓存穿透）
   - 分布式 Map、Queue

**为什么不用 Jedis**：
- 同步阻塞，高并发下性能差
- 需要自己管理连接池
- 不支持响应式编程

---

## 编码最佳实践

### ✅ 推荐做法

```java
// 1. 使用 Redisson 分布式锁
RLock lock = redissonClient.getLock("order:lock:" + orderId);
if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
    try {
        // 业务逻辑
    } finally {
        lock.unlock();
    }
}

// 2. Lettuce 配置合理的连接池
spring:
  redis:
    lettuce:
      pool:
        max-active: 50
        max-wait: 3000ms

// 3. 使用 Pipeline 批量操作
List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) conn -> {
    for (String key : keys) {
        conn.get(key.getBytes());
    }
    return null;
});
```

### ❌ 避免做法

```java
// ❌ Jedis 不使用连接池
Jedis jedis = new Jedis("localhost", 6379);  // 线程不安全

// ❌ 循环中逐个操作 Redis
for (String key : keys) {
    redisTemplate.opsForValue().get(key);  // 应该用 Pipeline
}

// ❌ 分布式锁不设置过期时间
lock.lock();  // 如果异常，锁永远不释放
```
