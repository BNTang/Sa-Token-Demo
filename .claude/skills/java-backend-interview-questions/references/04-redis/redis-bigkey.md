# Redis 中的 BigKey 问题是什么？如何解决？

## 什么是 BigKey

**BigKey** 是指 Redis 中 Key 对应的 Value 占用内存过大，或元素数量过多，导致操作该 Key 时出现性能问题。

```
┌─────────────────────────────────────────────────────────────┐
│                    BigKey 定义标准                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   String 类型:                                               │
│   └── 单个 Value > 10KB 就算大，> 1MB 严重                   │
│                                                             │
│   Hash / Set / ZSet / List:                                 │
│   └── 元素数量 > 5000 就算大，> 10 万需要优化                 │
│   └── 或者总内存 > 10MB                                      │
│                                                             │
│   示例:                                                      │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  ❌ "user:avatar:123" → 5MB 的图片 Base64            │  │
│   │  ❌ "product:all" → 100 万个商品 ID 的 Set           │  │
│   │  ❌ "logs:today" → 50 万条日志的 List                │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## BigKey 的危害

```
┌─────────────────────────────────────────────────────────────┐
│                    BigKey 危害                               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 阻塞 Redis                                             │
│      ├── Redis 单线程，大 Key 操作耗时长                     │
│      ├── DEL 大 Key 可能阻塞几秒                            │
│      └── HGETALL / SMEMBERS 等命令全量读取                  │
│                                                             │
│   2. 内存不均衡                                              │
│      ├── Redis Cluster 中，大 Key 导致数据倾斜              │
│      ├── 某节点内存远高于其他节点                            │
│      └── 迁移 slot 时卡住                                   │
│                                                             │
│   3. 网络拥塞                                               │
│      ├── 读取大 Key 占用大量带宽                            │
│      └── 客户端超时                                         │
│                                                             │
│   4. 内存碎片                                               │
│      ├── 删除大 Key 后产生大量碎片                          │
│      └── 影响内存分配效率                                   │
│                                                             │
│   5. 持久化问题                                              │
│      ├── AOF 重写变慢                                       │
│      └── RDB 生成时间长                                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 如何发现 BigKey

### 1. redis-cli 扫描

```bash
# 方式一：--bigkeys 扫描 (推荐)
redis-cli --bigkeys

# 输出示例:
# Biggest string found 'user:avatar:123' has 5242880 bytes
# Biggest list found 'logs:today' has 500000 items
# Biggest set found 'product:all' has 1000000 members

# 方式二：memory usage 检查特定 key
redis-cli memory usage mykey
# (integer) 10485760  # 10MB
```

### 2. SCAN + DEBUG OBJECT

```bash
# SCAN 遍历所有 key
redis-cli --scan --pattern '*' | while read key; do
    size=$(redis-cli debug object "$key" | grep -oP 'serializedlength:\K\d+')
    if [ "$size" -gt 10240 ]; then
        echo "$key: $size bytes"
    fi
done
```

### 3. 代码检测

```java
/**
 * BigKey 检测工具
 */
@Service
public class BigKeyScanner {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    private static final long BIG_STRING_THRESHOLD = 10 * 1024;      // 10KB
    private static final long BIG_COLLECTION_THRESHOLD = 5000;       // 5000 元素
    
    public void scanBigKeys() {
        ScanOptions options = ScanOptions.scanOptions().match("*").count(100).build();
        
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                checkBigKey(key);
            }
        }
    }
    
    private void checkBigKey(String key) {
        DataType type = redisTemplate.type(key);
        
        switch (type) {
            case STRING:
                Long strLen = redisTemplate.opsForValue().size(key);
                if (strLen != null && strLen > BIG_STRING_THRESHOLD) {
                    log.warn("BigKey [STRING]: {} size={} bytes", key, strLen);
                }
                break;
                
            case LIST:
                Long listLen = redisTemplate.opsForList().size(key);
                if (listLen != null && listLen > BIG_COLLECTION_THRESHOLD) {
                    log.warn("BigKey [LIST]: {} count={}", key, listLen);
                }
                break;
                
            case SET:
                Long setLen = redisTemplate.opsForSet().size(key);
                if (setLen != null && setLen > BIG_COLLECTION_THRESHOLD) {
                    log.warn("BigKey [SET]: {} count={}", key, setLen);
                }
                break;
                
            case ZSET:
                Long zsetLen = redisTemplate.opsForZSet().size(key);
                if (zsetLen != null && zsetLen > BIG_COLLECTION_THRESHOLD) {
                    log.warn("BigKey [ZSET]: {} count={}", key, zsetLen);
                }
                break;
                
            case HASH:
                Long hashLen = redisTemplate.opsForHash().size(key);
                if (hashLen != null && hashLen > BIG_COLLECTION_THRESHOLD) {
                    log.warn("BigKey [HASH]: {} count={}", key, hashLen);
                }
                break;
        }
    }
}
```

## 如何解决 BigKey

### 1. 拆分大 Key

```java
/**
 * 拆分大 Hash
 * 将一个大 Hash 拆分成多个小 Hash
 */
public class HashSplitter {
    
    private static final int BUCKET_COUNT = 100;
    
    // 拆分存储
    public void set(String baseKey, String field, String value) {
        String bucket = getBucket(field);
        String key = baseKey + ":" + bucket;
        redisTemplate.opsForHash().put(key, field, value);
    }
    
    // 拆分读取
    public String get(String baseKey, String field) {
        String bucket = getBucket(field);
        String key = baseKey + ":" + bucket;
        return (String) redisTemplate.opsForHash().get(key, field);
    }
    
    private String getBucket(String field) {
        int hash = Math.abs(field.hashCode());
        return String.valueOf(hash % BUCKET_COUNT);
    }
}

/**
 * 拆分大 List
 * 按固定长度拆分
 */
public class ListSplitter {
    
    private static final int SEGMENT_SIZE = 1000;
    
    public void add(String baseKey, String value, long index) {
        int segment = (int) (index / SEGMENT_SIZE);
        int offset = (int) (index % SEGMENT_SIZE);
        String key = baseKey + ":" + segment;
        redisTemplate.opsForList().set(key, offset, value);
    }
    
    public String get(String baseKey, long index) {
        int segment = (int) (index / SEGMENT_SIZE);
        int offset = (int) (index % SEGMENT_SIZE);
        String key = baseKey + ":" + segment;
        return redisTemplate.opsForList().index(key, offset);
    }
}
```

### 2. 压缩存储

```java
/**
 * 压缩大 String
 */
public class CompressedStorage {
    
    // 存储时压缩
    public void setCompressed(String key, String value) throws IOException {
        byte[] compressed = compress(value.getBytes(StandardCharsets.UTF_8));
        redisTemplate.opsForValue().set(key, Base64.getEncoder().encodeToString(compressed));
    }
    
    // 读取时解压
    public String getCompressed(String key) throws IOException {
        String compressed = redisTemplate.opsForValue().get(key);
        if (compressed == null) return null;
        byte[] decompressed = decompress(Base64.getDecoder().decode(compressed));
        return new String(decompressed, StandardCharsets.UTF_8);
    }
    
    private byte[] compress(byte[] data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
            gzip.write(data);
        }
        return bos.toByteArray();
    }
    
    private byte[] decompress(byte[] data) throws IOException {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(data))) {
            return gzip.readAllBytes();
        }
    }
}
```

### 3. 异步删除

```java
/**
 * 异步删除大 Key
 * Redis 4.0+ 使用 UNLINK 代替 DEL
 */
public void deleteBigKey(String key) {
    // 方式一：UNLINK (推荐)
    // 异步删除，不阻塞主线程
    redisTemplate.unlink(key);
    
    // 方式二：分批删除 (Redis 4.0 之前)
    DataType type = redisTemplate.type(key);
    switch (type) {
        case HASH:
            deleteHashInBatches(key);
            break;
        case SET:
            deleteSetInBatches(key);
            break;
        case ZSET:
            deleteZSetInBatches(key);
            break;
        case LIST:
            deleteListInBatches(key);
            break;
        default:
            redisTemplate.delete(key);
    }
}

private void deleteHashInBatches(String key) {
    ScanOptions options = ScanOptions.scanOptions().count(100).build();
    try (Cursor<Map.Entry<Object, Object>> cursor = 
            redisTemplate.opsForHash().scan(key, options)) {
        
        List<Object> fields = new ArrayList<>();
        while (cursor.hasNext()) {
            fields.add(cursor.next().getKey());
            if (fields.size() >= 100) {
                redisTemplate.opsForHash().delete(key, fields.toArray());
                fields.clear();
            }
        }
        if (!fields.isEmpty()) {
            redisTemplate.opsForHash().delete(key, fields.toArray());
        }
    }
    redisTemplate.delete(key);
}
```

### 4. 渐进式处理

```java
/**
 * 使用 HSCAN 替代 HGETALL
 */
public Map<String, String> getHashSafely(String key) {
    Map<String, String> result = new HashMap<>();
    ScanOptions options = ScanOptions.scanOptions().count(100).build();
    
    try (Cursor<Map.Entry<Object, Object>> cursor = 
            redisTemplate.opsForHash().scan(key, options)) {
        while (cursor.hasNext()) {
            Map.Entry<Object, Object> entry = cursor.next();
            result.put(entry.getKey().toString(), entry.getValue().toString());
        }
    }
    return result;
}

/**
 * 使用 ZRANGEBYSCORE + LIMIT 替代 ZRANGE 0 -1
 */
public List<String> getZSetSafely(String key, int batchSize) {
    List<String> result = new ArrayList<>();
    long start = 0;
    
    while (true) {
        Set<String> batch = redisTemplate.opsForZSet()
            .rangeByScore(key, Double.MIN_VALUE, Double.MAX_VALUE, start, batchSize);
        
        if (batch == null || batch.isEmpty()) break;
        
        result.addAll(batch);
        start += batchSize;
        
        if (batch.size() < batchSize) break;
    }
    return result;
}
```

## 预防 BigKey

```
┌─────────────────────────────────────────────────────────────┐
│                    预防 BigKey 措施                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 设计阶段                                                │
│      ├── 评估数据量，提前拆分                                │
│      ├── 避免全量存储，使用分页                              │
│      └── 大对象存 DB/OSS，Redis 只存索引                     │
│                                                             │
│   2. 开发规范                                                │
│      ├── 禁止使用 KEYS *、HGETALL、SMEMBERS 等              │
│      ├── 使用 SCAN 系列命令替代                              │
│      └── 删除使用 UNLINK 替代 DEL                           │
│                                                             │
│   3. 监控告警                                                │
│      ├── 定期扫描 BigKey                                    │
│      ├── 慢日志监控                                         │
│      └── 内存监控                                           │
│                                                             │
│   4. 数据结构选择                                            │
│      ├── 热点数据用 String，避免大 Hash                     │
│      ├── 时间序列用 Stream 或外部存储                        │
│      └── 计数器用 HyperLogLog                               │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 命令对比

```
┌─────────────────────────────────────────────────────────────┐
│                危险命令 vs 安全命令                          │
├─────────────────────────┬───────────────────────────────────┤
│   ❌ 危险                │   ✅ 安全替代                     │
├─────────────────────────┼───────────────────────────────────┤
│   KEYS *                │   SCAN                           │
│   HGETALL               │   HSCAN                          │
│   SMEMBERS              │   SSCAN                          │
│   ZRANGE 0 -1           │   ZSCAN                          │
│   LRANGE 0 -1           │   分批 LRANGE                    │
│   DEL bigkey            │   UNLINK (Redis 4.0+)            │
├─────────────────────────┴───────────────────────────────────┤
│   配置禁用危险命令:                                          │
│   rename-command KEYS ""                                    │
│   rename-command FLUSHALL ""                                │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> BigKey 是指 Value 内存过大（String > 10KB）或元素过多（集合 > 5000）。危害：阻塞 Redis、内存不均衡、网络拥塞。**发现**：redis-cli --bigkeys；**解决**：拆分大 Key、压缩存储、用 UNLINK 异步删除、用 SCAN 替代全量命令。

### 1分钟版本

> **什么是 BigKey**：
> - String > 10KB 或集合元素 > 5000
> - 如存储图片 Base64、百万 ID 列表
>
> **危害**：
> - Redis 单线程，大 Key 操作阻塞
> - Cluster 数据倾斜
> - 网络拥塞、客户端超时
> - 删除时可能卡住几秒
>
> **发现方法**：
> - `redis-cli --bigkeys` 扫描
> - `memory usage key` 查看内存
>
> **解决方案**：
> 1. **拆分**：大 Hash 拆成多个小 Hash（取模分桶）
> 2. **压缩**：GZIP 压缩后存储
> 3. **异步删除**：UNLINK 替代 DEL
> 4. **渐进式读取**：HSCAN/SSCAN 替代 HGETALL/SMEMBERS
>
> **预防**：设计时评估数据量，禁用危险命令，定期监控。

---

*关联文档：[redis-use-cases.md](redis-use-cases.md) | [redis-performance.md](redis-performance.md) | [redis-cluster.md](redis-cluster.md)*
