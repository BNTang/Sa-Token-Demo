# 如何设计一个短链系统？

## 需求分析

```
┌─────────────────────────────────────────────────────────────┐
│                    短链系统需求                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   核心功能:                                                  │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  长链接 ──生成──> 短链接                              │  │
│   │  https://example.com/very/long/path?param=value      │  │
│   │                    ↓                                 │  │
│   │  https://t.cn/abc123                                 │  │
│   │                                                      │  │
│   │  短链接 ──重定向──> 长链接                            │  │
│   │  用户访问 https://t.cn/abc123                        │  │
│   │                    ↓ 302 重定向                       │  │
│   │  跳转到 https://example.com/very/long/path           │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   非功能需求:                                                │
│   • 高并发: 读 QPS 远大于写 QPS (读写比约 100:1)            │
│   • 低延迟: 重定向响应 < 50ms                               │
│   • 高可用: 99.99%                                          │
│   • 短码唯一: 不同长链接映射到不同短码                       │
│   • 可统计: 访问次数、来源等                                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                    短链系统架构                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   用户请求                                                   │
│      │                                                      │
│      ▼                                                      │
│   ┌─────────────────────────────────────────────────────┐  │
│   │                     负载均衡                          │  │
│   │                    (Nginx/LB)                        │  │
│   └─────────────────────────────────────────────────────┘  │
│                          │                                  │
│            ┌─────────────┼─────────────┐                   │
│            ▼             ▼             ▼                   │
│   ┌─────────────┐ ┌─────────────┐ ┌─────────────┐        │
│   │  短链服务 1  │ │  短链服务 2  │ │  短链服务 N  │        │
│   └─────────────┘ └─────────────┘ └─────────────┘        │
│            │             │             │                   │
│            └─────────────┼─────────────┘                   │
│                          ▼                                  │
│   ┌─────────────────────────────────────────────────────┐  │
│   │                   Redis 缓存                         │  │
│   │              (短码 → 长链接映射)                      │  │
│   └─────────────────────────────────────────────────────┘  │
│                          │                                  │
│                          ▼                                  │
│   ┌─────────────────────────────────────────────────────┐  │
│   │                    MySQL                             │  │
│   │             (持久化存储 + 统计)                       │  │
│   │               主库 + 从库                            │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 核心设计

### 1. 短码生成方案

```
┌─────────────────────────────────────────────────────────────┐
│                    短码生成方案对比                          │
├──────────────┬──────────────────────────────────────────────┤
│   方案        │   说明                                       │
├──────────────┼──────────────────────────────────────────────┤
│   自增 ID     │   分布式 ID 转 62 进制                       │
│              │   优点：简单有序，无碰撞                      │
│              │   缺点：易被遍历，泄露业务量                   │
├──────────────┼──────────────────────────────────────────────┤
│   Hash 算法   │   对长链接 MD5/MurmurHash + 取前 N 位        │
│              │   优点：分散，不易遍历                        │
│              │   缺点：可能碰撞，需处理冲突                   │
├──────────────┼──────────────────────────────────────────────┤
│   随机生成    │   随机生成 N 位字符                          │
│              │   优点：简单                                 │
│              │   缺点：碰撞概率高，需查重                    │
├──────────────┼──────────────────────────────────────────────┤
│   预生成池    │   预先生成大量短码存入队列                    │
│              │   优点：生成快，无碰撞                        │
│              │   缺点：需要维护短码池                        │
└──────────────┴──────────────────────────────────────────────┘

推荐：自增 ID + 62 进制转换 (可选加密混淆)
```

### 2. 短码生成实现

```java
/**
 * 方案一：分布式 ID + 62 进制转换
 */
@Service
public class ShortUrlService {
    
    private static final String BASE62 = 
        "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    
    @Autowired
    private DistributedIdGenerator idGenerator;
    
    @Autowired
    private ShortUrlRepository repository;
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    // 生成短链
    public String createShortUrl(String longUrl) {
        // 1. 检查是否已存在
        String existingCode = repository.findCodeByLongUrl(longUrl);
        if (existingCode != null) {
            return buildShortUrl(existingCode);
        }
        
        // 2. 生成分布式 ID
        long id = idGenerator.nextId();
        
        // 3. 转换为 62 进制短码
        String shortCode = toBase62(id);
        
        // 4. 保存映射关系
        ShortUrlMapping mapping = new ShortUrlMapping();
        mapping.setId(id);
        mapping.setShortCode(shortCode);
        mapping.setLongUrl(longUrl);
        mapping.setCreateTime(LocalDateTime.now());
        repository.save(mapping);
        
        // 5. 缓存
        cacheMapping(shortCode, longUrl);
        
        return buildShortUrl(shortCode);
    }
    
    // 10 进制转 62 进制
    private String toBase62(long num) {
        StringBuilder sb = new StringBuilder();
        while (num > 0) {
            sb.insert(0, BASE62.charAt((int) (num % 62)));
            num /= 62;
        }
        // 补齐到 6 位
        while (sb.length() < 6) {
            sb.insert(0, '0');
        }
        return sb.toString();
    }
    
    private String buildShortUrl(String code) {
        return "https://t.cn/" + code;
    }
}

/**
 * 方案二：MurmurHash + 冲突处理
 */
public String createShortUrlByHash(String longUrl) {
    int hash = MurmurHash3.hash32(longUrl.getBytes());
    String shortCode = toBase62(Math.abs(hash) % 56800235584L); // 62^6
    
    // 检查冲突
    String existing = repository.findLongUrlByCode(shortCode);
    while (existing != null && !existing.equals(longUrl)) {
        // 冲突，加盐重新计算
        hash = MurmurHash3.hash32((longUrl + System.nanoTime()).getBytes());
        shortCode = toBase62(Math.abs(hash) % 56800235584L);
        existing = repository.findLongUrlByCode(shortCode);
    }
    
    return shortCode;
}
```

### 3. 短链访问（重定向）

```java
/**
 * 重定向接口
 */
@RestController
public class RedirectController {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Autowired
    private ShortUrlRepository repository;
    
    @Autowired
    private StatisticsService statisticsService;
    
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(
            @PathVariable String shortCode,
            HttpServletRequest request) {
        
        // 1. 先查缓存
        String longUrl = redisTemplate.opsForValue().get("short:" + shortCode);
        
        // 2. 缓存未命中，查数据库
        if (longUrl == null) {
            ShortUrlMapping mapping = repository.findByShortCode(shortCode);
            if (mapping == null) {
                return ResponseEntity.notFound().build();
            }
            longUrl = mapping.getLongUrl();
            // 回写缓存
            redisTemplate.opsForValue().set(
                "short:" + shortCode, longUrl, 1, TimeUnit.DAYS);
        }
        
        // 3. 异步记录统计
        statisticsService.recordVisitAsync(shortCode, request);
        
        // 4. 302 重定向
        return ResponseEntity.status(HttpStatus.FOUND)
            .header("Location", longUrl)
            .build();
    }
}

/**
 * 301 vs 302 重定向
 * 301 永久重定向：浏览器缓存，下次直接跳转，无法统计
 * 302 临时重定向：每次都经过服务器，可统计（推荐）
 */
```

### 4. 数据库设计

```sql
-- 短链映射表
CREATE TABLE short_url_mapping (
    id BIGINT PRIMARY KEY COMMENT '分布式ID',
    short_code VARCHAR(10) NOT NULL COMMENT '短码',
    long_url VARCHAR(2048) NOT NULL COMMENT '原始链接',
    create_time DATETIME NOT NULL,
    expire_time DATETIME COMMENT '过期时间',
    creator_id BIGINT COMMENT '创建者',
    status TINYINT DEFAULT 1 COMMENT '状态: 1-有效, 0-禁用',
    
    UNIQUE KEY uk_short_code (short_code),
    KEY idx_long_url (long_url(255)),
    KEY idx_creator (creator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 访问统计表 (按天分表)
CREATE TABLE url_statistics_20240101 (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    short_code VARCHAR(10) NOT NULL,
    visit_time DATETIME NOT NULL,
    ip VARCHAR(45),
    user_agent VARCHAR(512),
    referer VARCHAR(1024),
    
    KEY idx_short_code (short_code),
    KEY idx_visit_time (visit_time)
) ENGINE=InnoDB;
```

### 5. 缓存策略

```java
/**
 * 缓存策略
 */
@Service
public class ShortUrlCacheService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    private static final String CACHE_PREFIX = "short:";
    private static final long DEFAULT_EXPIRE = 86400; // 1天
    
    // 布隆过滤器防止缓存穿透
    private BloomFilter<String> bloomFilter = BloomFilter.create(
        Funnels.stringFunnel(Charset.forName("UTF-8")),
        10000000,  // 预计容量 1000 万
        0.01       // 误判率 1%
    );
    
    public String getLongUrl(String shortCode) {
        // 1. 布隆过滤器判断是否存在
        if (!bloomFilter.mightContain(shortCode)) {
            return null;  // 一定不存在
        }
        
        // 2. 查缓存
        String longUrl = redisTemplate.opsForValue().get(CACHE_PREFIX + shortCode);
        if (longUrl != null) {
            if ("NULL".equals(longUrl)) {
                return null;  // 缓存空值
            }
            return longUrl;
        }
        
        // 3. 查数据库 (分布式锁防止缓存击穿)
        String lockKey = "lock:" + shortCode;
        try {
            if (redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS)) {
                // 获得锁，查数据库
                ShortUrlMapping mapping = repository.findByShortCode(shortCode);
                if (mapping != null) {
                    cacheMapping(shortCode, mapping.getLongUrl());
                    return mapping.getLongUrl();
                } else {
                    // 缓存空值
                    redisTemplate.opsForValue().set(
                        CACHE_PREFIX + shortCode, "NULL", 300, TimeUnit.SECONDS);
                    return null;
                }
            } else {
                // 未获得锁，等待后重试
                Thread.sleep(50);
                return getLongUrl(shortCode);
            }
        } finally {
            redisTemplate.delete(lockKey);
        }
    }
    
    public void cacheMapping(String shortCode, String longUrl) {
        redisTemplate.opsForValue().set(
            CACHE_PREFIX + shortCode, longUrl, DEFAULT_EXPIRE, TimeUnit.SECONDS);
        bloomFilter.put(shortCode);
    }
}
```

### 6. 高可用设计

```
┌─────────────────────────────────────────────────────────────┐
│                    高可用设计                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 服务层                                                  │
│      ├── 多实例部署，无状态设计                              │
│      ├── Kubernetes 自动扩缩容                               │
│      └── 负载均衡 + 健康检查                                 │
│                                                             │
│   2. 缓存层                                                  │
│      ├── Redis Cluster 或 Sentinel                          │
│      ├── 本地缓存 + 远程缓存多级架构                         │
│      └── 缓存预热                                           │
│                                                             │
│   3. 数据库层                                                │
│      ├── MySQL 主从复制                                      │
│      ├── 读写分离                                           │
│      └── 分库分表 (按 short_code 哈希)                       │
│                                                             │
│   4. 异地多活                                                │
│      ├── 多机房部署                                          │
│      ├── 就近访问                                           │
│      └── 数据同步                                           │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 容量估算

```
┌─────────────────────────────────────────────────────────────┐
│                    容量估算                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   短码长度: 6 位 (62^6 = 56.8 亿个)                          │
│                                                             │
│   假设:                                                      │
│   • 每天生成 100 万短链                                      │
│   • 每条记录约 1KB                                           │
│                                                             │
│   存储:                                                      │
│   • 每天: 100 万 × 1KB = 1GB                                │
│   • 每年: 365GB                                             │
│   • 5 年: 约 2TB                                            │
│                                                             │
│   QPS:                                                       │
│   • 写: 100 万 / 86400 ≈ 12 QPS                             │
│   • 读: 假设读写比 100:1，约 1200 QPS                        │
│   • 峰值: 3-5 倍日常，约 6000 QPS                           │
│                                                             │
│   结论: 读多写少，适合缓存                                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> 短链系统核心：1）**短码生成**——分布式 ID 转 62 进制，6 位可支持 56 亿链接；2）**存储设计**——MySQL 持久化 + Redis 缓存映射关系；3）**重定向**——302 临时重定向，可统计访问；4）**防穿透**——布隆过滤器 + 缓存空值。

### 1分钟版本

> **短码生成方案**：
> - 分布式 ID（雪花算法）转 62 进制，6 位短码
> - 或 MurmurHash + 冲突处理
>
> **存储设计**：
> - MySQL：存储映射关系（short_code, long_url）
> - Redis：缓存热点数据，读多写少场景
> - 布隆过滤器：防止缓存穿透
>
> **访问流程**：
> 1. 查 Redis 缓存
> 2. 未命中查 DB，回写缓存
> 3. 异步记录统计
> 4. **302 重定向**（可统计，301 会被浏览器缓存）
>
> **高可用**：
> - 无状态服务多实例
> - Redis Cluster
> - MySQL 主从 + 读写分离
>
> **容量**：6 位 62 进制 = 56.8 亿，足够使用。

---

*关联文档：[distributed-id-generator.md](distributed-id-generator.md) | [seckill-design.md](seckill-design.md)*
