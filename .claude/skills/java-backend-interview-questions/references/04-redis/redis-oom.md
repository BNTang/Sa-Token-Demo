# 如果发现 Redis 内存溢出了？你会怎么做？

## 问题识别

```
┌─────────────────────────────────────────────────────────────┐
│                    Redis 内存溢出症状                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   症状表现:                                                  │
│   ├── OOM 错误: "OOM command not allowed when used memory   │
│   │            > 'maxmemory'"                               │
│   ├── 写入失败，读取正常                                    │
│   ├── 响应变慢                                              │
│   └── 服务器 swap 使用增加                                  │
│                                                             │
│   常见原因:                                                  │
│   ├── 数据量增长超过预期                                    │
│   ├── BigKey 占用大量内存                                   │
│   ├── 内存碎片率过高                                        │
│   ├── 未设置过期时间，数据堆积                              │
│   ├── 未配置 maxmemory 或淘汰策略                           │
│   └── 内存泄漏（客户端未正确关闭连接等）                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 排查思路

```
┌─────────────────────────────────────────────────────────────┐
│                    排查流程                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   Step 1: 确认内存使用情况                                   │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  > INFO memory                                      │  │
│   │                                                     │  │
│   │  used_memory:10737418240         # 已使用内存 10GB  │  │
│   │  used_memory_human:10.00G                           │  │
│   │  used_memory_rss:12884901888     # RSS 内存         │  │
│   │  used_memory_peak:10737418240    # 峰值内存         │  │
│   │  mem_fragmentation_ratio:1.20    # 碎片率           │  │
│   │  maxmemory:10737418240           # 最大内存限制     │  │
│   │  maxmemory_policy:noeviction     # 淘汰策略         │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   Step 2: 分析内存组成                                       │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  > MEMORY STATS                                     │  │
│   │                                                     │  │
│   │  keys.count: 1000000             # Key 数量         │  │
│   │  peak.allocated: 10GB                               │  │
│   │  dataset.bytes: 8GB              # 数据占用         │  │
│   │  overhead.total: 2GB             # 额外开销         │  │
│   │  clients.normal: 100MB           # 客户端缓冲区     │  │
│   │  replication.backlog: 100MB      # 复制积压缓冲区   │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   Step 3: 查找 BigKey                                        │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  # 方法1: --bigkeys 扫描                            │  │
│   │  > redis-cli --bigkeys                              │  │
│   │                                                     │  │
│   │  # 方法2: MEMORY USAGE 查看单个 Key                 │  │
│   │  > MEMORY USAGE mykey                               │  │
│   │                                                     │  │
│   │  # 方法3: DEBUG OBJECT                              │  │
│   │  > DEBUG OBJECT mykey                               │  │
│   │  serializedlength: 1024000                          │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   Step 4: 检查过期 Key 情况                                  │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  > INFO keyspace                                    │  │
│   │  db0:keys=1000000,expires=500000,avg_ttl=86400000  │  │
│   │                                                     │  │
│   │  # expires=500000 表示有一半 Key 没设置过期时间     │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 解决方案

### 1. 设置内存淘汰策略

```bash
# redis.conf 配置

# 设置最大内存
maxmemory 8gb

# 设置淘汰策略
maxmemory-policy allkeys-lru

# 淘汰策略说明:
# noeviction     - 不淘汰，写入报错 (默认)
# allkeys-lru    - 所有 Key 中 LRU 淘汰 (推荐)
# volatile-lru   - 有过期时间的 Key 中 LRU 淘汰
# allkeys-lfu    - 所有 Key 中 LFU 淘汰 (Redis 4.0+)
# volatile-lfu   - 有过期时间的 Key 中 LFU 淘汰
# allkeys-random - 所有 Key 中随机淘汰
# volatile-random- 有过期时间的 Key 中随机淘汰
# volatile-ttl   - 淘汰即将过期的 Key
```

```
┌─────────────────────────────────────────────────────────────┐
│                    淘汰策略选择                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   推荐策略:                                                  │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  缓存场景 (数据可丢失):                              │  │
│   │  └── allkeys-lru (推荐)                              │  │
│   │  └── allkeys-lfu (热点数据更精准)                    │  │
│   │                                                     │  │
│   │  混合场景 (部分数据不可丢失):                        │  │
│   │  └── volatile-lru                                    │  │
│   │  └── 重要数据不设过期时间                            │  │
│   │                                                     │  │
│   │  业务数据存储 (不可丢失):                            │  │
│   │  └── noeviction + 监控报警                           │  │
│   │  └── 需要人工介入处理                                │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 2. 清理无用数据

```bash
# 删除指定模式的 Key (生产慎用)
# 使用 SCAN 避免阻塞
redis-cli --scan --pattern "temp:*" | xargs redis-cli DEL

# 异步删除大 Key
UNLINK bigkey

# 批量删除脚本 (推荐)
redis-cli --scan --pattern "prefix:*" | while read key; do
    redis-cli UNLINK "$key"
    sleep 0.001  # 避免太快
done
```

### 3. 处理 BigKey

```java
/**
 * BigKey 拆分示例 - Hash 拆分
 */
public class BigKeyHandler {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    // 原来: user:1001 -> {field1: value1, field2: value2, ... field1000000: value}
    // 拆分: user:1001:0 -> {field1-100}
    //       user:1001:1 -> {field101-200}
    
    private static final int BUCKET_SIZE = 1000;
    
    public void hset(String key, String field, String value) {
        int bucket = Math.abs(field.hashCode()) % BUCKET_SIZE;
        String realKey = key + ":" + bucket;
        redisTemplate.opsForHash().put(realKey, field, value);
    }
    
    public String hget(String key, String field) {
        int bucket = Math.abs(field.hashCode()) % BUCKET_SIZE;
        String realKey = key + ":" + bucket;
        return (String) redisTemplate.opsForHash().get(realKey, field);
    }
    
    // BigKey 删除 (渐进式)
    public void deleteBigKey(String key) {
        String type = redisTemplate.type(key).name();
        
        switch (type) {
            case "HASH":
                // 渐进式删除 Hash
                ScanOptions options = ScanOptions.scanOptions().count(100).build();
                Cursor<Map.Entry<Object, Object>> cursor = 
                    redisTemplate.opsForHash().scan(key, options);
                while (cursor.hasNext()) {
                    Map.Entry<Object, Object> entry = cursor.next();
                    redisTemplate.opsForHash().delete(key, entry.getKey());
                }
                break;
            case "SET":
                // 渐进式删除 Set
                redisTemplate.opsForSet().scan(key, options)
                    .forEachRemaining(member -> 
                        redisTemplate.opsForSet().remove(key, member));
                break;
            // ... 其他类型类似
        }
        redisTemplate.delete(key);
    }
}
```

### 4. 优化数据结构

```
┌─────────────────────────────────────────────────────────────┐
│                    内存优化技巧                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 使用更紧凑的数据结构                                   │
│      ┌──────────────────────────────────────────────────┐  │
│      │  String 存储 JSON → Hash 存储字段 (更省内存)     │  │
│      │  多个 String → 合并为一个 Hash                    │  │
│      └──────────────────────────────────────────────────┘  │
│                                                             │
│   2. 缩短 Key 名称                                          │
│      ┌──────────────────────────────────────────────────┐  │
│      │  user:profile:12345678 → u:p:12345678            │  │
│      │  order:detail:xxxxx → o:d:xxxxx                  │  │
│      └──────────────────────────────────────────────────┘  │
│                                                             │
│   3. 整数编码优化                                           │
│      ┌──────────────────────────────────────────────────┐  │
│      │  小整数 (< 10000) 使用共享对象                    │  │
│      │  hash-max-ziplist-entries 512                    │  │
│      │  hash-max-ziplist-value 64                       │  │
│      └──────────────────────────────────────────────────┘  │
│                                                             │
│   4. 压缩数据                                               │
│      ┌──────────────────────────────────────────────────┐  │
│      │  大 Value 使用 GZIP/Snappy 压缩后存储             │  │
│      │  读取时解压                                       │  │
│      └──────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 5. 处理内存碎片

```bash
# 查看碎片率
> INFO memory
mem_fragmentation_ratio: 1.50  # 1.0-1.5 正常，> 1.5 需要处理

# 自动碎片整理 (Redis 4.0+)
config set activedefrag yes
config set active-defrag-ignore-bytes 100mb
config set active-defrag-threshold-lower 10
config set active-defrag-threshold-upper 100

# 手动碎片整理 (重启)
# 1. 使用 BGSAVE 生成 RDB
# 2. 重启 Redis，加载 RDB
```

### 6. 扩展内存

```
┌─────────────────────────────────────────────────────────────┐
│                    扩展方案                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   纵向扩展:                                                  │
│   ├── 增加服务器内存                                        │
│   └── 修改 maxmemory 配置                                   │
│                                                             │
│   横向扩展:                                                  │
│   ├── Redis Cluster 分片                                    │
│   ├── 客户端分片 (一致性哈希)                               │
│   └── 代理分片 (Codis, Twemproxy)                           │
│                                                             │
│   数据迁移:                                                  │
│   ├── 冷热分离：热数据 Redis，冷数据 MySQL/磁盘             │
│   └── 历史数据归档                                          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 排查流程图

```
┌─────────────────────────────────────────────────────────────┐
│                    完整排查流程                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   发现 OOM                                                   │
│      │                                                      │
│      ▼                                                      │
│   INFO memory 查看内存使用                                   │
│      │                                                      │
│      ├── 检查 maxmemory 配置 ──→ 未配置 ──→ 设置 maxmemory  │
│      │                                                      │
│      ├── 检查淘汰策略 ──→ noeviction ──→ 改为 allkeys-lru   │
│      │                                                      │
│      ├── 检查碎片率 ──→ > 1.5 ──→ 开启自动碎片整理          │
│      │                                                      │
│      ▼                                                      │
│   redis-cli --bigkeys 查找大 Key                            │
│      │                                                      │
│      ├── 发现 BigKey ──→ 拆分或删除                         │
│      │                                                      │
│      ▼                                                      │
│   INFO keyspace 检查过期 Key                                │
│      │                                                      │
│      ├── 大量无过期时间 ──→ 补充 TTL / 清理无用数据          │
│      │                                                      │
│      ▼                                                      │
│   仍不够 ──→ 扩容 (纵向/横向)                               │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> Redis OOM 排查：1）**INFO memory** 查看内存使用和淘汰策略；2）**--bigkeys** 查找大 Key；3）检查是否有大量 Key 无过期时间。解决：设置 **maxmemory + 淘汰策略**（推荐 allkeys-lru），清理无用数据，拆分 BigKey，开启碎片整理，必要时扩容或分片。

### 1分钟版本

> **排查思路**：
>
> 1. **INFO memory** 查看：
>    - used_memory：当前使用内存
>    - maxmemory：内存限制
>    - maxmemory-policy：淘汰策略
>    - mem_fragmentation_ratio：碎片率
>
> 2. **查找 BigKey**：
>    - `redis-cli --bigkeys`
>    - `MEMORY USAGE key`
>
> 3. **检查过期 Key**：
>    - `INFO keyspace` 查看 expires 比例
>
> **解决方案**：
>
> 1. 设置淘汰策略（allkeys-lru）
> 2. 清理无用数据，补充 TTL
> 3. BigKey 拆分或渐进式删除（UNLINK）
> 4. 开启自动碎片整理（activedefrag yes）
> 5. 优化数据结构（短 Key、压缩 Value）
> 6. 扩容：纵向增加内存 / 横向 Redis Cluster

---

*关联文档：[redis-bigkey.md](redis-bigkey.md) | [redis-hotkey.md](redis-hotkey.md) | [redis-persistence.md](redis-persistence.md)*
