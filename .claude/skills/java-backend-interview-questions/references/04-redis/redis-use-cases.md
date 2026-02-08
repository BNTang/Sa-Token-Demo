# Redis 通常应用于哪些场景？

## 场景概览

```
┌─────────────────────────────────────────────────────────────┐
│                    Redis 典型应用场景                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 缓存 (Cache)                    - String/Hash          │
│   2. 分布式锁 (Distributed Lock)     - String + Lua         │
│   3. 计数器/限流 (Counter/RateLimiter)- String/Lua          │
│   4. 排行榜 (Leaderboard)            - ZSet                 │
│   5. 消息队列 (Message Queue)        - List/Stream          │
│   6. 会话存储 (Session)              - String/Hash          │
│   7. 社交关系 (Social Graph)         - Set                  │
│   8. 地理位置 (Geo)                  - Geo                  │
│   9. 布隆过滤器 (Bloom Filter)       - Module               │
│  10. 实时推荐 (Recommendation)       - ZSet/HyperLogLog     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 详细场景

### 1. 缓存 (最常见)

```java
/**
 * 缓存：减少数据库压力，提升响应速度
 */
@Service
public class UserCacheService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private UserMapper userMapper;
    
    private static final String USER_CACHE_KEY = "user:";
    
    // 读取缓存
    public User getUser(Long userId) {
        String key = USER_CACHE_KEY + userId;
        
        // 1. 查缓存
        User user = (User) redisTemplate.opsForValue().get(key);
        if (user != null) {
            return user;
        }
        
        // 2. 查数据库
        user = userMapper.selectById(userId);
        if (user != null) {
            // 3. 写入缓存
            redisTemplate.opsForValue().set(key, user, 1, TimeUnit.HOURS);
        }
        return user;
    }
    
    // 更新时删除缓存
    public void updateUser(User user) {
        userMapper.updateById(user);
        redisTemplate.delete(USER_CACHE_KEY + user.getId());
    }
}

// 缓存穿透解决：缓存空值
if (user == null) {
    redisTemplate.opsForValue().set(key, "NULL", 5, TimeUnit.MINUTES);
}
```

### 2. 分布式锁

```java
/**
 * 分布式锁：保证分布式环境下的互斥访问
 */
@Service
public class DistributedLockService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    private static final String LOCK_PREFIX = "lock:";
    
    // 加锁 (简化版)
    public boolean tryLock(String key, String value, long expireTime) {
        return redisTemplate.opsForValue()
            .setIfAbsent(LOCK_PREFIX + key, value, expireTime, TimeUnit.SECONDS);
    }
    
    // 解锁 (Lua 脚本保证原子性)
    public boolean unlock(String key, String value) {
        String script = 
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('del', KEYS[1]) " +
            "else " +
            "    return 0 " +
            "end";
        
        Long result = redisTemplate.execute(
            new DefaultRedisScript<>(script, Long.class),
            Collections.singletonList(LOCK_PREFIX + key),
            value
        );
        return result != null && result == 1;
    }
}

// 使用 Redisson 更安全
RLock lock = redisson.getLock("order:" + orderId);
try {
    if (lock.tryLock(5, 30, TimeUnit.SECONDS)) {
        // 业务逻辑
    }
} finally {
    lock.unlock();
}
```

### 3. 计数器 / 限流

```java
/**
 * 计数器：文章阅读量、点赞数
 */
public Long incrementViewCount(Long articleId) {
    String key = "article:view:" + articleId;
    return redisTemplate.opsForValue().increment(key);
}

/**
 * 限流：滑动窗口限流
 */
public boolean isAllowed(String userId, int maxRequests, int windowSeconds) {
    String key = "ratelimit:" + userId;
    long now = System.currentTimeMillis();
    long windowStart = now - windowSeconds * 1000;
    
    // 移除窗口外的请求
    redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);
    
    // 统计窗口内请求数
    Long count = redisTemplate.opsForZSet().zCard(key);
    if (count != null && count >= maxRequests) {
        return false;  // 超过限制
    }
    
    // 添加当前请求
    redisTemplate.opsForZSet().add(key, String.valueOf(now), now);
    redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
    return true;
}
```

### 4. 排行榜

```java
/**
 * 排行榜：游戏积分、文章热度
 */
@Service
public class LeaderboardService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    private static final String LEADERBOARD_KEY = "leaderboard:game";
    
    // 更新分数
    public void updateScore(String userId, double score) {
        redisTemplate.opsForZSet().add(LEADERBOARD_KEY, userId, score);
    }
    
    // 增加分数
    public Double incrementScore(String userId, double delta) {
        return redisTemplate.opsForZSet().incrementScore(LEADERBOARD_KEY, userId, delta);
    }
    
    // 获取排名 (从 0 开始)
    public Long getRank(String userId) {
        return redisTemplate.opsForZSet().reverseRank(LEADERBOARD_KEY, userId);
    }
    
    // 获取 Top N
    public Set<ZSetOperations.TypedTuple<String>> getTopN(int n) {
        return redisTemplate.opsForZSet()
            .reverseRangeWithScores(LEADERBOARD_KEY, 0, n - 1);
    }
    
    // 获取用户排名和分数
    public Map<String, Object> getUserRankInfo(String userId) {
        Map<String, Object> result = new HashMap<>();
        result.put("rank", getRank(userId) + 1);
        result.put("score", redisTemplate.opsForZSet().score(LEADERBOARD_KEY, userId));
        return result;
    }
}
```

### 5. 消息队列

```java
/**
 * 简单消息队列：List 实现
 */
public void publishMessage(String queue, String message) {
    redisTemplate.opsForList().rightPush(queue, message);
}

public String consumeMessage(String queue) {
    // 阻塞式消费
    return redisTemplate.opsForList().leftPop(queue, 30, TimeUnit.SECONDS);
}

/**
 * Stream 消息队列 (Redis 5.0+)
 * 支持消费者组、消息确认、持久化
 */
// 发送消息
StringRecord record = StreamRecords.string(
    Collections.singletonMap("data", message)
).withStreamKey("mystream");
redisTemplate.opsForStream().add(record);

// 消费消息 (消费者组)
redisTemplate.opsForStream().read(
    Consumer.from("mygroup", "consumer1"),
    StreamReadOptions.empty().count(10),
    StreamOffset.create("mystream", ReadOffset.lastConsumed())
);
```

### 6. 会话存储

```java
/**
 * 分布式 Session
 */
// Spring Session + Redis 配置
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 3600)
public class SessionConfig {
    
    @Bean
    public LettuceConnectionFactory connectionFactory() {
        return new LettuceConnectionFactory();
    }
}

// 自动存储到 Redis
// spring:session:sessions:{sessionId}

/**
 * Token 存储
 */
public String createToken(Long userId) {
    String token = UUID.randomUUID().toString();
    redisTemplate.opsForValue().set(
        "token:" + token, 
        userId.toString(), 
        2, TimeUnit.HOURS
    );
    return token;
}

public Long getUserIdByToken(String token) {
    String userId = redisTemplate.opsForValue().get("token:" + token);
    return userId != null ? Long.parseLong(userId) : null;
}
```

### 7. 社交关系

```java
/**
 * 社交关系：关注、好友、共同好友
 */
@Service
public class SocialService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    // 关注
    public void follow(String userId, String followeeId) {
        // 我的关注列表
        redisTemplate.opsForSet().add("following:" + userId, followeeId);
        // 对方的粉丝列表
        redisTemplate.opsForSet().add("followers:" + followeeId, userId);
    }
    
    // 取消关注
    public void unfollow(String userId, String followeeId) {
        redisTemplate.opsForSet().remove("following:" + userId, followeeId);
        redisTemplate.opsForSet().remove("followers:" + followeeId, userId);
    }
    
    // 获取共同关注
    public Set<String> getCommonFollowing(String userA, String userB) {
        return redisTemplate.opsForSet().intersect(
            "following:" + userA, 
            "following:" + userB
        );
    }
    
    // 是否关注
    public boolean isFollowing(String userId, String followeeId) {
        return Boolean.TRUE.equals(
            redisTemplate.opsForSet().isMember("following:" + userId, followeeId)
        );
    }
    
    // 获取粉丝数
    public Long getFollowerCount(String userId) {
        return redisTemplate.opsForSet().size("followers:" + userId);
    }
}
```

### 8. 地理位置

```java
/**
 * 地理位置：附近的人、门店
 */
@Service
public class GeoService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    private static final String GEO_KEY = "stores";
    
    // 添加门店位置
    public void addStore(String storeId, double longitude, double latitude) {
        redisTemplate.opsForGeo().add(GEO_KEY, 
            new Point(longitude, latitude), storeId);
    }
    
    // 获取附近门店
    public List<String> getNearbyStores(double longitude, double latitude, double radiusKm) {
        Circle circle = new Circle(new Point(longitude, latitude), 
            new Distance(radiusKm, Metrics.KILOMETERS));
        
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = 
            redisTemplate.opsForGeo().radius(GEO_KEY, circle);
        
        return results.getContent().stream()
            .map(result -> result.getContent().getName())
            .collect(Collectors.toList());
    }
    
    // 计算两店距离
    public Double getDistance(String storeA, String storeB) {
        Distance distance = redisTemplate.opsForGeo()
            .distance(GEO_KEY, storeA, storeB, Metrics.KILOMETERS);
        return distance != null ? distance.getValue() : null;
    }
}
```

### 9. UV 统计 (HyperLogLog)

```java
/**
 * UV 统计：基数估算，误差约 0.81%
 * 优点：每个 key 只需 12KB 内存
 */
public void addPageView(String pageId, String visitorId) {
    String key = "uv:" + pageId + ":" + LocalDate.now();
    redisTemplate.opsForHyperLogLog().add(key, visitorId);
}

public Long getUniqueVisitors(String pageId) {
    String key = "uv:" + pageId + ":" + LocalDate.now();
    return redisTemplate.opsForHyperLogLog().size(key);
}

// 合并多天 UV
public Long getWeeklyUV(String pageId) {
    String[] keys = new String[7];
    LocalDate today = LocalDate.now();
    for (int i = 0; i < 7; i++) {
        keys[i] = "uv:" + pageId + ":" + today.minusDays(i);
    }
    return redisTemplate.opsForHyperLogLog().union("uv:weekly:" + pageId, keys);
}
```

## 场景与数据结构对照

```
┌─────────────────────────────────────────────────────────────┐
│                  场景与数据结构对照                          │
├──────────────────┬──────────────┬───────────────────────────┤
│   场景            │  数据结构     │  核心命令                  │
├──────────────────┼──────────────┼───────────────────────────┤
│   缓存           │  String/Hash │  GET SET HGET HSET        │
│   分布式锁       │  String      │  SETNX + EXPIRE + Lua     │
│   计数器         │  String      │  INCR INCRBY              │
│   限流           │  ZSet/String │  ZADD + Lua               │
│   排行榜         │  ZSet        │  ZADD ZRANK ZRANGE        │
│   消息队列       │  List/Stream │  LPUSH BRPOP XADD XREAD   │
│   会话存储       │  String/Hash │  SET GET HSET HGET        │
│   社交关系       │  Set         │  SADD SINTER SMEMBERS     │
│   地理位置       │  Geo         │  GEOADD GEORADIUS         │
│   UV 统计        │  HyperLogLog │  PFADD PFCOUNT PFMERGE    │
│   位图签到       │  Bitmap      │  SETBIT GETBIT BITCOUNT   │
│   布隆过滤器     │  Module      │  BF.ADD BF.EXISTS         │
└──────────────────┴──────────────┴───────────────────────────┘
```

## 面试回答

### 30秒版本

> Redis 常见应用场景：**缓存**（最核心）、**分布式锁**（SETNX + Lua）、**计数器/限流**（INCR）、**排行榜**（ZSet）、**消息队列**（List/Stream）、**会话存储**（分布式 Session）、**社交关系**（Set 交集并集）、**地理位置**（GEO 命令）。

### 1分钟版本

> **Redis 典型场景**：
>
> 1. **缓存**（String/Hash）：减少 DB 压力，提升响应速度
> 2. **分布式锁**（String + Lua）：保证分布式环境互斥
> 3. **计数器/限流**（INCR / ZSet）：文章阅读量、接口限流
> 4. **排行榜**（ZSet）：ZADD 更新分数，ZREVRANGE 获取 Top N
> 5. **消息队列**（List/Stream）：LPUSH + BRPOP，Stream 支持消费者组
> 6. **会话存储**：Spring Session + Redis 实现分布式 Session
> 7. **社交关系**（Set）：关注列表、共同好友（SINTER）
> 8. **地理位置**（Geo）：附近的人、门店搜索
> 9. **UV 统计**（HyperLogLog）：只需 12KB 估算基数
>
> **关键**：根据场景选择合适的数据结构，发挥 Redis 最大效能。

---

*关联文档：[redis-data-types.md](redis-data-types.md) | [redis-lua-script.md](redis-lua-script.md) | [redis-pipeline.md](redis-pipeline.md)*
