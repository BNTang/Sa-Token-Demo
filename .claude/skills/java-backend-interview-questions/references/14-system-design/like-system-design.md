# 如何设计一个点赞系统？

## 需求分析

```
┌─────────────────────────────────────────────────────────────┐
│                    点赞系统需求                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   核心功能:                                                  │
│   ├── 点赞/取消点赞                                         │
│   ├── 查询点赞数                                            │
│   ├── 查询点赞状态（当前用户是否点赞）                       │
│   └── 查询点赞列表（谁点了赞）                               │
│                                                             │
│   扩展功能:                                                  │
│   ├── 热门内容排行（按点赞数）                               │
│   └── 点赞消息通知                                          │
│                                                             │
│   非功能需求:                                                │
│   ├── 高并发（热门内容可能被百万级点赞）                     │
│   ├── 低延迟（点赞操作实时响应）                             │
│   ├── 数据一致性（点赞数准确）                               │
│   └── 幂等性（重复点赞只算一次）                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                    点赞系统架构                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │                     客户端                           │  │
│   └─────────────────────────────────────────────────────┘  │
│                           │                                 │
│                           ▼                                 │
│   ┌─────────────────────────────────────────────────────┐  │
│   │                   API Gateway                        │  │
│   │                   (限流/鉴权)                         │  │
│   └─────────────────────────────────────────────────────┘  │
│                           │                                 │
│                           ▼                                 │
│   ┌─────────────────────────────────────────────────────┐  │
│   │                  点赞服务 (Like Service)             │  │
│   └─────────────────────────────────────────────────────┘  │
│              │                        │                     │
│              ▼                        ▼                     │
│   ┌─────────────────┐     ┌─────────────────────────────┐  │
│   │   Redis 缓存     │     │         消息队列            │  │
│   │  • 点赞状态      │     │   (异步持久化/通知)         │  │
│   │  • 点赞计数      │     │                             │  │
│   └─────────────────┘     └─────────────────────────────┘  │
│              │                        │                     │
│              └────────────────────────┘                     │
│                           │                                 │
│                           ▼                                 │
│   ┌─────────────────────────────────────────────────────┐  │
│   │                      MySQL                           │  │
│   │                  (持久化存储)                         │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 方案一：MySQL 直接存储

### 数据库设计

```sql
-- 点赞记录表
CREATE TABLE `user_like` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '点赞用户',
    `target_id` BIGINT NOT NULL COMMENT '被点赞对象ID',
    `target_type` TINYINT NOT NULL COMMENT '对象类型:1文章,2评论,3视频',
    `status` TINYINT DEFAULT 1 COMMENT '状态:1点赞,0取消',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_target` (`user_id`, `target_id`, `target_type`),
    KEY `idx_target` (`target_id`, `target_type`)
) ENGINE=InnoDB;

-- 点赞计数表（冗余，减少 COUNT 查询）
CREATE TABLE `like_count` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `target_id` BIGINT NOT NULL,
    `target_type` TINYINT NOT NULL,
    `count` INT DEFAULT 0,
    UNIQUE KEY `uk_target` (`target_id`, `target_type`)
) ENGINE=InnoDB;
```

### 基础实现

```java
@Service
public class SimpleLikeService {
    
    @Autowired
    private UserLikeMapper likeMapper;
    
    @Autowired
    private LikeCountMapper countMapper;
    
    @Transactional
    public void like(Long userId, Long targetId, Integer targetType) {
        // 1. 查询是否已点赞
        UserLike existing = likeMapper.findByUserAndTarget(userId, targetId, targetType);
        
        if (existing == null) {
            // 2. 插入点赞记录
            UserLike like = new UserLike(userId, targetId, targetType, 1);
            likeMapper.insert(like);
            // 3. 增加计数
            countMapper.increment(targetId, targetType);
        } else if (existing.getStatus() == 0) {
            // 重新点赞
            likeMapper.updateStatus(existing.getId(), 1);
            countMapper.increment(targetId, targetType);
        }
    }
    
    @Transactional
    public void unlike(Long userId, Long targetId, Integer targetType) {
        UserLike existing = likeMapper.findByUserAndTarget(userId, targetId, targetType);
        if (existing != null && existing.getStatus() == 1) {
            likeMapper.updateStatus(existing.getId(), 0);
            countMapper.decrement(targetId, targetType);
        }
    }
    
    // ❌ 问题：高并发下数据库压力大
}
```

## 方案二：Redis 缓存 + 异步持久化（推荐）

### Redis 数据结构设计

```
┌─────────────────────────────────────────────────────────────┐
│                  Redis 数据结构设计                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 点赞状态 (Set)                                         │
│      Key:  like:{targetType}:{targetId}                     │
│      Value: Set<userId>                                     │
│      示例: like:article:1001 → {101, 102, 103}             │
│                                                             │
│   2. 点赞计数 (String)                                       │
│      Key:  like:count:{targetType}:{targetId}               │
│      Value: 点赞数                                           │
│      示例: like:count:article:1001 → 1234                   │
│                                                             │
│   3. 用户点赞列表 (Set) - 可选                               │
│      Key:  user:likes:{userId}:{targetType}                 │
│      Value: Set<targetId>                                   │
│      用途: 查询用户点赞过的内容                              │
│                                                             │
│   4. 热门排行 (ZSet) - 可选                                  │
│      Key:  hot:{targetType}:daily                           │
│      Value: ZSet<targetId, score>                           │
│      用途: 按点赞数排行                                      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 完整实现

```java
@Service
public class RedisLikeService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    private static final String LIKE_KEY = "like:%s:%s";
    private static final String COUNT_KEY = "like:count:%s:%s";
    private static final String USER_LIKES_KEY = "user:likes:%s:%s";
    
    /**
     * 点赞
     */
    public boolean like(Long userId, Long targetId, String targetType) {
        String likeKey = String.format(LIKE_KEY, targetType, targetId);
        String countKey = String.format(COUNT_KEY, targetType, targetId);
        String userLikesKey = String.format(USER_LIKES_KEY, userId, targetType);
        
        // 使用 Lua 脚本保证原子性
        String script = 
            "local added = redis.call('SADD', KEYS[1], ARGV[1]) " +
            "if added == 1 then " +
            "    redis.call('INCR', KEYS[2]) " +
            "    redis.call('SADD', KEYS[3], ARGV[2]) " +
            "    return 1 " +
            "else " +
            "    return 0 " +
            "end";
        
        Long result = redisTemplate.execute(
            new DefaultRedisScript<>(script, Long.class),
            Arrays.asList(likeKey, countKey, userLikesKey),
            userId.toString(), targetId.toString()
        );
        
        if (result != null && result == 1) {
            // 异步持久化到数据库
            sendToQueue(new LikeEvent(userId, targetId, targetType, "LIKE"));
            return true;
        }
        return false;
    }
    
    /**
     * 取消点赞
     */
    public boolean unlike(Long userId, Long targetId, String targetType) {
        String likeKey = String.format(LIKE_KEY, targetType, targetId);
        String countKey = String.format(COUNT_KEY, targetType, targetId);
        String userLikesKey = String.format(USER_LIKES_KEY, userId, targetType);
        
        String script = 
            "local removed = redis.call('SREM', KEYS[1], ARGV[1]) " +
            "if removed == 1 then " +
            "    redis.call('DECR', KEYS[2]) " +
            "    redis.call('SREM', KEYS[3], ARGV[2]) " +
            "    return 1 " +
            "else " +
            "    return 0 " +
            "end";
        
        Long result = redisTemplate.execute(
            new DefaultRedisScript<>(script, Long.class),
            Arrays.asList(likeKey, countKey, userLikesKey),
            userId.toString(), targetId.toString()
        );
        
        if (result != null && result == 1) {
            sendToQueue(new LikeEvent(userId, targetId, targetType, "UNLIKE"));
            return true;
        }
        return false;
    }
    
    /**
     * 查询点赞数
     */
    public Long getLikeCount(Long targetId, String targetType) {
        String countKey = String.format(COUNT_KEY, targetType, targetId);
        String count = redisTemplate.opsForValue().get(countKey);
        return count != null ? Long.parseLong(count) : 0L;
    }
    
    /**
     * 查询是否点赞
     */
    public boolean isLiked(Long userId, Long targetId, String targetType) {
        String likeKey = String.format(LIKE_KEY, targetType, targetId);
        return Boolean.TRUE.equals(
            redisTemplate.opsForSet().isMember(likeKey, userId.toString())
        );
    }
    
    /**
     * 批量查询点赞状态（用于列表页）
     */
    public Map<Long, Boolean> batchIsLiked(Long userId, List<Long> targetIds, String targetType) {
        Map<Long, Boolean> result = new HashMap<>();
        
        // Pipeline 批量查询
        List<Object> pipelineResults = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (Long targetId : targetIds) {
                String likeKey = String.format(LIKE_KEY, targetType, targetId);
                connection.sIsMember(likeKey.getBytes(), userId.toString().getBytes());
            }
            return null;
        });
        
        for (int i = 0; i < targetIds.size(); i++) {
            result.put(targetIds.get(i), (Boolean) pipelineResults.get(i));
        }
        return result;
    }
    
    /**
     * 发送到消息队列
     */
    private void sendToQueue(LikeEvent event) {
        rabbitTemplate.convertAndSend("like.exchange", "like.event", event);
    }
}
```

### 异步持久化消费者

```java
@Component
public class LikeEventConsumer {
    
    @Autowired
    private UserLikeMapper likeMapper;
    
    @Autowired
    private LikeCountMapper countMapper;
    
    @RabbitListener(queues = "like.queue")
    @Transactional
    public void handleLikeEvent(LikeEvent event) {
        if ("LIKE".equals(event.getAction())) {
            // 插入或更新点赞记录
            likeMapper.upsert(event.getUserId(), event.getTargetId(), 
                event.getTargetType(), 1);
            countMapper.increment(event.getTargetId(), event.getTargetType());
        } else if ("UNLIKE".equals(event.getAction())) {
            likeMapper.updateStatus(event.getUserId(), event.getTargetId(), 
                event.getTargetType(), 0);
            countMapper.decrement(event.getTargetId(), event.getTargetType());
        }
    }
}
```

## 热门排行榜

```java
@Service
public class HotRankService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    private static final String HOT_RANK_KEY = "hot:%s:daily";
    
    /**
     * 点赞时更新排行榜
     */
    public void incrementHot(Long targetId, String targetType) {
        String key = String.format(HOT_RANK_KEY, targetType);
        redisTemplate.opsForZSet().incrementScore(key, targetId.toString(), 1);
    }
    
    /**
     * 获取热门榜单 Top N
     */
    public List<Long> getTopN(String targetType, int n) {
        String key = String.format(HOT_RANK_KEY, targetType);
        Set<String> result = redisTemplate.opsForZSet()
            .reverseRange(key, 0, n - 1);
        
        return result.stream()
            .map(Long::parseLong)
            .collect(Collectors.toList());
    }
    
    /**
     * 每日重置（定时任务）
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void resetDaily() {
        redisTemplate.delete(String.format(HOT_RANK_KEY, "article"));
        redisTemplate.delete(String.format(HOT_RANK_KEY, "video"));
    }
}
```

## 高并发优化

```
┌─────────────────────────────────────────────────────────────┐
│                    高并发优化策略                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 接口限流                                               │
│      └── 同一用户对同一内容，限制点赞频率 (如1秒1次)         │
│                                                             │
│   2. 本地缓存                                               │
│      └── Caffeine 缓存热门内容的点赞数                       │
│                                                             │
│   3. 批量处理                                               │
│      └── 消息队列批量消费，批量写入数据库                    │
│                                                             │
│   4. 数据分片                                               │
│      └── 按 target_id 分库分表                              │
│                                                             │
│   5. 异步处理                                               │
│      └── 点赞操作写 Redis 立即返回，异步持久化               │
│                                                             │
│   6. 热点探测                                               │
│      └── 自动识别热点内容，本地缓存 + 多级缓存               │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> 点赞系统设计：**Redis 缓存 + 异步持久化**。使用 Redis Set 存储点赞用户（去重），String 存储计数。点赞操作写 Redis 立即返回，通过消息队列异步持久化到 MySQL。高并发优化：接口限流、本地缓存热点、批量消费写库。

### 1分钟版本

> **存储设计**：
> - Redis Set：`like:{type}:{id}` → 存储点赞用户ID（天然去重）
> - Redis String：`like:count:{type}:{id}` → 存储点赞数
> - MySQL：持久化存储，异步写入
>
> **核心流程**：
> 1. 点赞：SADD + INCR（Lua保证原子性）
> 2. 发送MQ异步持久化
> 3. 消费者批量写入MySQL
>
> **查询优化**：
> - 单条：SISMEMBER 判断是否点赞
> - 批量：Pipeline 批量查询
> - 计数：直接读 Redis String
>
> **高并发优化**：
> - 接口限流防刷
> - 本地缓存热点内容
> - MQ批量消费
> - 热门排行用 ZSet
>
> **幂等性**：Set 天然去重，重复点赞返回失败。

---

*关联文档：[redis-use-cases.md](../04-redis/redis-use-cases.md) | [seckill-design.md](seckill-design.md)*
