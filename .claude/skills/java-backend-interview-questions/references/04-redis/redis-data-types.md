# Redis 数据类型

> Java 后端面试知识点 - Redis 深入

---

## 数据类型概览

Redis 提供丰富的数据类型，满足不同业务场景需求。

```
┌─────────────────────────────────────────────────────────────────┐
│                        Redis 数据类型                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  基础类型                     高级类型                           │
│  ├── String (字符串)          ├── Bitmaps (位图)                │
│  ├── List (列表)              ├── HyperLogLog (基数统计)         │
│  ├── Hash (哈希)              ├── Geospatial (地理位置)          │
│  ├── Set (集合)               └── Stream (消息流)                │
│  └── Sorted Set (有序集合)                                      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 1. String（字符串）

### 特点

- 最基础的类型，可存储字符串、整数、浮点数
- 最大存储 512MB
- 二进制安全

### 应用场景

| 场景 | 示例 |
|------|------|
| 缓存 | 用户信息、商品信息 |
| 计数器 | 访问量、点赞数 |
| 分布式锁 | SETNX 实现 |
| 限流 | 请求次数限制 |

### 编码实践

```java
@Service
@RequiredArgsConstructor
public class StringCacheService {
    
    private final StringRedisTemplate redisTemplate;
    
    // 缓存用户信息
    public void cacheUser(User user) {
        String key = "user:" + user.getId();
        String value = JsonUtils.toJson(user);
        redisTemplate.opsForValue().set(key, value, 30, TimeUnit.MINUTES);
    }
    
    // 计数器
    public Long incrementViewCount(Long articleId) {
        String key = "article:view:" + articleId;
        return redisTemplate.opsForValue().increment(key);
    }
    
    // 分布式锁
    public boolean tryLock(String lockKey, String value, long timeout) {
        Boolean success = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, value, timeout, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }
}
```

---

## 2. List（列表）

### 特点

- 双向链表实现
- 有序，可重复
- 支持左右两端操作

### 应用场景

| 场景 | 示例 |
|------|------|
| 消息队列 | 简单任务队列 |
| 时间线 | 微博 Timeline |
| 最新列表 | 最新评论、最新文章 |

### 编码实践

```java
@Service
@RequiredArgsConstructor
public class ListService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    // 添加最新消息（保留最新100条）
    public void addLatestMessage(String userId, Message message) {
        String key = "user:messages:" + userId;
        redisTemplate.opsForList().leftPush(key, message);
        redisTemplate.opsForList().trim(key, 0, 99);  // 只保留最新100条
    }
    
    // 获取最新消息
    public List<Object> getLatestMessages(String userId, int count) {
        String key = "user:messages:" + userId;
        return redisTemplate.opsForList().range(key, 0, count - 1);
    }
    
    // 简单消息队列 - 生产者
    public void pushTask(Task task) {
        redisTemplate.opsForList().rightPush("task:queue", task);
    }
    
    // 简单消息队列 - 消费者（阻塞获取）
    public Task popTask(long timeout) {
        return (Task) redisTemplate.opsForList()
            .leftPop("task:queue", timeout, TimeUnit.SECONDS);
    }
}
```

---

## 3. Hash（哈希）

### 特点

- 键值对集合
- 适合存储对象
- 可单独操作某个字段

### 应用场景

| 场景 | 示例 |
|------|------|
| 对象存储 | 用户信息、商品详情 |
| 购物车 | 商品ID → 数量 |
| 计数器组 | 多维度计数 |

### 编码实践

```java
@Service
@RequiredArgsConstructor
public class HashService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    // 存储用户信息
    public void saveUser(User user) {
        String key = "user:info:" + user.getId();
        Map<String, Object> map = new HashMap<>();
        map.put("name", user.getName());
        map.put("age", user.getAge());
        map.put("email", user.getEmail());
        redisTemplate.opsForHash().putAll(key, map);
    }
    
    // 获取单个字段
    public String getUserName(Long userId) {
        String key = "user:info:" + userId;
        return (String) redisTemplate.opsForHash().get(key, "name");
    }
    
    // 购物车操作
    public void addToCart(Long userId, Long productId, int quantity) {
        String key = "cart:" + userId;
        redisTemplate.opsForHash().increment(key, productId.toString(), quantity);
    }
    
    public Map<Object, Object> getCart(Long userId) {
        String key = "cart:" + userId;
        return redisTemplate.opsForHash().entries(key);
    }
}
```

---

## 4. Set（集合）

### 特点

- 无序，不重复
- 支持交集、并集、差集运算
- 基于哈希表实现，O(1) 查找

### 应用场景

| 场景 | 示例 |
|------|------|
| 标签 | 文章标签、用户标签 |
| 去重 | IP 去重、用户去重 |
| 共同好友 | 交集运算 |
| 抽奖 | 随机获取 |

### 编码实践

```java
@Service
@RequiredArgsConstructor
public class SetService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    // 添加标签
    public void addTags(Long articleId, String... tags) {
        String key = "article:tags:" + articleId;
        redisTemplate.opsForSet().add(key, tags);
    }
    
    // 共同关注
    public Set<Object> getCommonFollows(Long userId1, Long userId2) {
        String key1 = "user:follows:" + userId1;
        String key2 = "user:follows:" + userId2;
        return redisTemplate.opsForSet().intersect(key1, key2);
    }
    
    // 抽奖（随机获取）
    public Object randomPick(String lotteryKey) {
        return redisTemplate.opsForSet().randomMember(lotteryKey);
    }
    
    // 抽奖（随机获取并移除）
    public Object randomPickAndRemove(String lotteryKey) {
        return redisTemplate.opsForSet().pop(lotteryKey);
    }
    
    // 判断是否点赞
    public boolean isLiked(Long userId, Long articleId) {
        String key = "article:likes:" + articleId;
        return Boolean.TRUE.equals(
            redisTemplate.opsForSet().isMember(key, userId));
    }
}
```

---

## 5. Sorted Set（有序集合）

### 特点

- 有序，不重复
- 每个元素关联一个分数（score），按分数排序
- 底层使用跳表 + 哈希表

### 应用场景

| 场景 | 示例 |
|------|------|
| 排行榜 | 积分排行、热度排行 |
| 延迟队列 | 分数=时间戳 |
| 时间线 | 带权重的 Feed |

### 编码实践

```java
@Service
@RequiredArgsConstructor
public class ZSetService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    // 更新排行榜分数
    public void updateScore(String rankKey, String userId, double score) {
        redisTemplate.opsForZSet().add(rankKey, userId, score);
    }
    
    // 增加分数
    public Double incrementScore(String rankKey, String userId, double delta) {
        return redisTemplate.opsForZSet().incrementScore(rankKey, userId, delta);
    }
    
    // 获取排行榜 Top N（分数从高到低）
    public Set<ZSetOperations.TypedTuple<Object>> getTopN(String rankKey, int n) {
        return redisTemplate.opsForZSet()
            .reverseRangeWithScores(rankKey, 0, n - 1);
    }
    
    // 获取用户排名
    public Long getUserRank(String rankKey, String userId) {
        return redisTemplate.opsForZSet().reverseRank(rankKey, userId);
    }
    
    // 延迟队列 - 添加任务
    public void addDelayTask(String queueKey, Object task, long executeTime) {
        redisTemplate.opsForZSet().add(queueKey, task, executeTime);
    }
    
    // 延迟队列 - 获取到期任务
    public Set<Object> getExpiredTasks(String queueKey) {
        long now = System.currentTimeMillis();
        return redisTemplate.opsForZSet().rangeByScore(queueKey, 0, now);
    }
}
```

---

## 6. 高级类型

### Bitmap（位图）

```java
// 用户签到
public void signIn(Long userId, int dayOfMonth) {
    String key = "sign:" + userId + ":" + YearMonth.now();
    redisTemplate.opsForValue().setBit(key, dayOfMonth - 1, true);
}

// 统计签到天数
public Long countSignDays(Long userId) {
    String key = "sign:" + userId + ":" + YearMonth.now();
    return redisTemplate.execute((RedisCallback<Long>) conn -> 
        conn.stringCommands().bitCount(key.getBytes()));
}
```

### HyperLogLog（基数统计）

```java
// UV 统计（允许误差）
public void recordUV(String pageId, String visitorId) {
    String key = "uv:" + pageId + ":" + LocalDate.now();
    redisTemplate.opsForHyperLogLog().add(key, visitorId);
}

public Long getUV(String pageId) {
    String key = "uv:" + pageId + ":" + LocalDate.now();
    return redisTemplate.opsForHyperLogLog().size(key);
}
```

### Geospatial（地理位置）

```java
// 添加门店位置
public void addStore(String storeId, double longitude, double latitude) {
    redisTemplate.opsForGeo().add("stores", 
        new Point(longitude, latitude), storeId);
}

// 附近门店
public GeoResults<RedisGeoCommands.GeoLocation<Object>> nearbyStores(
        double longitude, double latitude, double radiusKm) {
    return redisTemplate.opsForGeo().radius("stores",
        new Circle(new Point(longitude, latitude), 
            new Distance(radiusKm, Metrics.KILOMETERS)));
}
```

---

## 面试要点

### 核心答案

**问：Redis 中常见的数据类型有哪些？**

答：Redis 有 5 种基础类型 + 4 种高级类型：

**基础类型**：
| 类型 | 特点 | 典型场景 |
|------|------|---------|
| String | 最基础，512MB 上限 | 缓存、计数器、分布式锁 |
| List | 双向链表，有序可重复 | 消息队列、最新列表 |
| Hash | 键值对集合 | 对象存储、购物车 |
| Set | 无序不重复 | 标签、去重、交集运算 |
| ZSet | 有序不重复，带分数 | 排行榜、延迟队列 |

**高级类型**：
- **Bitmap**：位操作，适合签到、布隆过滤器
- **HyperLogLog**：基数统计，UV 统计（有误差）
- **Geospatial**：地理位置，附近的人
- **Stream**：消息流，支持消费者组

---

## 编码最佳实践

### ✅ 推荐做法

```java
// 1. Key 命名规范：业务:模块:标识
String key = "user:profile:" + userId;

// 2. 选择合适的数据类型
// 对象存储用 Hash（可单独更新字段）
// 排行榜用 ZSet
// 去重用 Set

// 3. 设置过期时间
redisTemplate.opsForValue().set(key, value, 30, TimeUnit.MINUTES);

// 4. 使用 Pipeline 批量操作
redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
    for (String key : keys) {
        connection.get(key.getBytes());
    }
    return null;
});
```

### ❌ 避免做法

```java
// ❌ 存储大对象到 String
redisTemplate.opsForValue().set("big", hugeJsonString);  // 可能超过阈值

// ❌ List 存储大量数据不 trim
redisTemplate.opsForList().leftPush(key, item);  // 无限增长

// ❌ 不设置过期时间
redisTemplate.opsForValue().set(key, value);  // 永不过期
```
