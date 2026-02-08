# 如何设计一个分布式 ID 发号器？

## 设计目标

```
┌─────────────────────────────────────────────────────────────┐
│                  分布式 ID 设计目标                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 全局唯一性                                              │
│      └── 多节点、多数据中心不冲突                            │
│                                                             │
│   2. 趋势递增                                               │
│      └── 方便数据库索引，减少页分裂                          │
│                                                             │
│   3. 高性能                                                 │
│      └── 高并发下低延迟生成                                  │
│                                                             │
│   4. 高可用                                                 │
│      └── 单点故障不影响 ID 生成                              │
│                                                             │
│   5. 信息安全                                               │
│      └── 不暴露业务信息（如订单量）                          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 方案对比

```
┌─────────────────────────────────────────────────────────────┐
│                  分布式 ID 方案对比                          │
├──────────────┬──────┬──────┬──────┬──────┬────────────────┤
│   方案        │ 唯一性│ 有序性│ 性能  │ 可用性│ 适用场景       │
├──────────────┼──────┼──────┼──────┼──────┼────────────────┤
│   UUID       │  ✅   │  ❌   │  ✅   │  ✅   │ 非核心业务     │
│   数据库自增  │  ✅   │  ✅   │  ❌   │  ❌   │ 单机/小并发    │
│   数据库号段  │  ✅   │  ✅   │  ✅   │  ✅   │ 中等并发       │
│   Redis      │  ✅   │  ✅   │  ✅   │  ✅   │ 中等并发       │
│   雪花算法   │  ✅   │  ✅   │  ✅✅ │  ✅   │ 高并发（推荐） │
│   Leaf      │  ✅   │  ✅   │  ✅✅ │  ✅   │ 大规模系统     │
└──────────────┴──────┴──────┴──────┴──────┴────────────────┘
```

## 各方案详解

### 1. UUID

```java
/**
 * UUID 方案
 * 优点：简单，本地生成，无需网络
 * 缺点：无序，字符串长（36位），索引效率差
 */
public class UuidGenerator {
    
    public String generateUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    // UUID v1: 基于时间 + MAC 地址，可能泄露隐私
    // UUID v4: 随机生成（最常用）
}

// 适用场景：非核心业务、请求追踪 ID、临时标识
```

### 2. 数据库自增

```java
/**
 * 数据库自增
 * 优点：简单，保证唯一，有序
 * 缺点：单点瓶颈，性能差
 */
@Service
public class DatabaseIdGenerator {
    
    @Autowired
    private IdSequenceMapper idMapper;
    
    public Long nextId(String bizType) {
        IdSequence seq = new IdSequence();
        seq.setBizType(bizType);
        idMapper.insert(seq);  // AUTO_INCREMENT
        return seq.getId();
    }
}

// 改进：多主库 + 步长
// 主库1: 1, 3, 5, 7... (起始1, 步长2)
// 主库2: 2, 4, 6, 8... (起始2, 步长2)
-- SET @@auto_increment_increment = 2;
-- SET @@auto_increment_offset = 1; -- 或 2
```

### 3. 数据库号段模式

```java
/**
 * 号段模式 (Leaf-Segment)
 * 原理：每次从数据库获取一段 ID（如 1000 个），本地分发
 */
@Service
public class SegmentIdGenerator {
    
    private final ConcurrentHashMap<String, Segment> segmentCache = new ConcurrentHashMap<>();
    
    @Autowired
    private IdSegmentMapper segmentMapper;
    
    public Long nextId(String bizType) {
        Segment segment = segmentCache.computeIfAbsent(bizType, this::loadSegment);
        
        Long id = segment.getAndIncrement();
        if (id != null) {
            return id;
        }
        
        // 号段用完，重新获取
        synchronized (segment) {
            if (segment.isEmpty()) {
                segment = loadSegment(bizType);
                segmentCache.put(bizType, segment);
            }
            return segment.getAndIncrement();
        }
    }
    
    private Segment loadSegment(String bizType) {
        // 数据库乐观锁更新
        // UPDATE id_segment SET max_id = max_id + step, version = version + 1 
        // WHERE biz_type = ? AND version = ?
        return segmentMapper.getNextSegment(bizType);
    }
    
    static class Segment {
        private AtomicLong current;
        private long max;
        
        public Long getAndIncrement() {
            long value = current.getAndIncrement();
            return value <= max ? value : null;
        }
        
        public boolean isEmpty() {
            return current.get() > max;
        }
    }
}

// 表结构
/*
CREATE TABLE id_segment (
    biz_type VARCHAR(64) PRIMARY KEY,
    max_id BIGINT NOT NULL,
    step INT NOT NULL DEFAULT 1000,
    version INT NOT NULL DEFAULT 0,
    update_time DATETIME
);
*/
```

### 4. Redis 自增

```java
/**
 * Redis INCR 方案
 * 优点：性能好，有序
 * 缺点：依赖 Redis，持久化可能丢失
 */
@Service
public class RedisIdGenerator {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    private static final String KEY_PREFIX = "id:";
    
    public Long nextId(String bizType) {
        String key = KEY_PREFIX + bizType;
        return redisTemplate.opsForValue().increment(key);
    }
    
    // 按日期分 Key，方便统计
    public Long nextIdWithDate(String bizType) {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String key = KEY_PREFIX + bizType + ":" + date;
        Long seq = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, 2, TimeUnit.DAYS);  // 2 天后过期
        
        // 组合 ID: 日期 + 序号
        return Long.parseLong(date) * 1000000 + seq;
    }
}
```

### 5. 雪花算法（Snowflake）⭐ 推荐

```
┌─────────────────────────────────────────────────────────────┐
│                    雪花算法 ID 结构                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   64 位 Long 型 ID:                                          │
│   ┌───┬────────────────────────┬──────────┬──────────────┐ │
│   │ 0 │       时间戳 (41 bit)   │ 机器ID   │   序列号     │ │
│   │   │       毫秒级            │ (10 bit) │  (12 bit)    │ │
│   └───┴────────────────────────┴──────────┴──────────────┘ │
│    1      41                     5+5          12            │
│   符号                        数据中心+                      │
│   位                          机器ID                        │
│                                                             │
│   • 时间戳: 41 bit，可用 69 年                               │
│   • 机器ID: 10 bit，最多 1024 个节点                         │
│   • 序列号: 12 bit，每毫秒 4096 个 ID                        │
│   • 理论 QPS: 约 400 万/秒                                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

```java
/**
 * 雪花算法实现
 */
public class SnowflakeIdGenerator {
    
    // 起始时间戳 (2020-01-01)
    private static final long EPOCH = 1577836800000L;
    
    // 位数分配
    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;
    
    // 最大值
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);          // 31
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);  // 31
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);             // 4095
    
    // 位移
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;                    // 12
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS; // 17
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS; // 22
    
    private final long workerId;
    private final long datacenterId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;
    
    public SnowflakeIdGenerator(long workerId, long datacenterId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException("Worker ID 超出范围");
        }
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException("Datacenter ID 超出范围");
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }
    
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        
        // 时钟回拨检测
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("时钟回拨，拒绝生成 ID");
        }
        
        // 同一毫秒内
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                // 序列号用完，等待下一毫秒
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        
        lastTimestamp = timestamp;
        
        // 组装 ID
        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
             | (datacenterId << DATACENTER_ID_SHIFT)
             | (workerId << WORKER_ID_SHIFT)
             | sequence;
    }
    
    private long waitNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
    
    // 解析 ID
    public static Map<String, Long> parseId(long id) {
        Map<String, Long> result = new HashMap<>();
        result.put("timestamp", (id >> TIMESTAMP_SHIFT) + EPOCH);
        result.put("datacenterId", (id >> DATACENTER_ID_SHIFT) & MAX_DATACENTER_ID);
        result.put("workerId", (id >> WORKER_ID_SHIFT) & MAX_WORKER_ID);
        result.put("sequence", id & MAX_SEQUENCE);
        return result;
    }
}
```

### 6. 百度 UidGenerator / 美团 Leaf

```java
// 百度 UidGenerator 优化点：
// 1. RingBuffer 预生成 ID，减少锁竞争
// 2. 借用未来时间，解决时钟回拨

// 美团 Leaf 优化点：
// 1. 双 Buffer，异步加载号段
// 2. Snowflake 模式 + 号段模式双引擎
// 3. ZooKeeper 管理 workerId
```

## 时钟回拨问题

```
┌─────────────────────────────────────────────────────────────┐
│                    时钟回拨解决方案                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 抛出异常                                               │
│      └── 简单粗暴，但会影响可用性                            │
│                                                             │
│   2. 等待时钟追上                                           │
│      └── 回拨时间短时可用，长时间会阻塞                       │
│                                                             │
│   3. 借用未来时间                                           │
│      └── 百度 UidGenerator 方案                              │
│      └── 预生成 ID 到 RingBuffer，避免实时依赖时钟            │
│                                                             │
│   4. 使用原子序列号                                         │
│      └── 每次启动生成新的随机 workerId                       │
│      └── 配合序列号递增保证唯一性                            │
│                                                             │
│   5. 关闭 NTP 自动同步                                       │
│      └── 使用 NTP 的 slew 模式而非 step 模式                 │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 最佳实践

### ✅ 推荐做法

```java
// 1. 使用成熟框架
// 百度 uid-generator
// 美团 Leaf
// Hutool IdUtil

// 2. workerId 自动分配
@Component
public class WorkerIdAllocator {
    @Autowired
    private CuratorFramework zkClient;
    
    @PostConstruct
    public void init() {
        // 从 ZooKeeper 获取唯一 workerId
        String path = zkClient.create()
            .creatingParentsIfNeeded()
            .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
            .forPath("/snowflake/worker-");
        
        int workerId = parseWorkerId(path);
        SnowflakeIdGenerator.init(workerId);
    }
}

// 3. ID 预热
@Component
public class IdPreloader {
    private final BlockingQueue<Long> idPool = new LinkedBlockingQueue<>(10000);
    
    @Scheduled(fixedRate = 100)
    public void preload() {
        while (idPool.size() < 5000) {
            idPool.offer(snowflake.nextId());
        }
    }
    
    public Long nextId() {
        return idPool.poll();
    }
}
```

## 面试回答

### 30秒版本

> 分布式 ID 常用方案：**雪花算法（推荐）**——64 位，由时间戳 + 机器 ID + 序列号组成，单机每毫秒生成 4096 个，趋势递增；**号段模式**——从数据库批量获取一段 ID，本地分发；**Redis INCR**——简单但依赖 Redis。需要解决时钟回拨和 workerId 分配问题。

### 1分钟版本

> **方案对比**：
> - UUID：简单但无序，索引效率差
> - 数据库自增：单点瓶颈
> - 号段模式：批量获取，性能好，美团 Leaf 采用
> - 雪花算法：64 位（时间戳 41 + 机器 ID 10 + 序列号 12），单机 QPS 约 400 万
>
> **雪花算法**（推荐）：
> - 优点：本地生成、趋势递增、高性能
> - 问题：时钟回拨、workerId 分配
> - 解决：ZooKeeper 分配 workerId，时钟回拨时等待或借用未来时间
>
> **生产实践**：
> - 使用成熟框架（百度 UidGenerator、美团 Leaf）
> - workerId 从配置中心/ZK 获取
> - ID 预热到本地队列

---

*关联文档：[seckill-design.md](seckill-design.md) | [short-url-design.md](short-url-design.md)*
