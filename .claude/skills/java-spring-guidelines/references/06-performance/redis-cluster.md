# Redis 集群与分布式锁

> Java/Spring Boot 编码规范 - Redis 集群原理与实战

---

## 一、Redis 集群实现原理

### 集群架构

**Redis Cluster** 采用无中心化架构，所有节点地位平等。

```
节点分布：
- 主节点（Master）: 3 个（最少）
- 从节点（Slave）: 3 个（推荐每个主节点 1 个从节点）

数据分片：
- 16384 个哈希槽（Hash Slot）
- 每个主节点负责一部分槽
- 槽分配：节点 1 (0-5460), 节点 2 (5461-10922), 节点 3 (10923-16383)
```

### 核心概念

#### 1. 哈希槽（Hash Slot）

```
CRC16(key) % 16384 = slot
```

**示例**：

```bash
# 键的槽位计算
127.0.0.1:7000> CLUSTER KEYSLOT "user:1001"
(integer) 1324

# 1324 属于节点 1 (0-5460)，数据存储在节点 1
```

#### 2. 节点通信（Gossip 协议）

**Gossip 协议特点**：
- 每个节点维护集群状态
- 节点定期交换信息（PING/PONG）
- 最终一致性

```
节点 A --PING--> 节点 B
节点 B --PONG--> 节点 A

信息内容：
- 节点状态（在线/下线）
- 槽位分配
- 主从关系
```

#### 3. 故障检测与转移

**故障检测**：

```
1. PFAIL（疑似下线）：节点 A 认为节点 B 下线
2. FAIL（确认下线）：半数以上节点认为节点 B 下线
3. 故障转移：选举从节点为新主节点
```

**主从切换流程**：

```
1. 从节点发现主节点 FAIL
2. 从节点发起选举
3. 其他节点投票
4. 得票最多的从节点成为新主节点
5. 新主节点接管槽位
6. 向集群广播配置更新
```

---

## 二、Redis 集群部署

### Docker Compose 部署（6 节点）

```yaml
# docker-compose-redis-cluster.yml
version: '3.8'

services:
  redis-7000:
    image: redis:7.0
    container_name: redis-7000
    command: redis-server /usr/local/etc/redis/redis.conf
    ports:
      - "7000:7000"
      - "17000:17000"
    volumes:
      - ./redis-7000.conf:/usr/local/etc/redis/redis.conf
      - ./data/redis-7000:/data
    networks:
      - redis-cluster

  redis-7001:
    image: redis:7.0
    container_name: redis-7001
    command: redis-server /usr/local/etc/redis/redis.conf
    ports:
      - "7001:7001"
      - "17001:17001"
    volumes:
      - ./redis-7001.conf:/usr/local/etc/redis/redis.conf
      - ./data/redis-7001:/data
    networks:
      - redis-cluster

  redis-7002:
    image: redis:7.0
    container_name: redis-7002
    command: redis-server /usr/local/etc/redis/redis.conf
    ports:
      - "7002:7002"
      - "17002:17002"
    volumes:
      - ./redis-7002.conf:/usr/local/etc/redis/redis.conf
      - ./data/redis-7002:/data
    networks:
      - redis-cluster

  redis-7003:
    image: redis:7.0
    container_name: redis-7003
    command: redis-server /usr/local/etc/redis/redis.conf
    ports:
      - "7003:7003"
      - "17003:17003"
    volumes:
      - ./redis-7003.conf:/usr/local/etc/redis/redis.conf
      - ./data/redis-7003:/data
    networks:
      - redis-cluster

  redis-7004:
    image: redis:7.0
    container_name: redis-7004
    command: redis-server /usr/local/etc/redis/redis.conf
    ports:
      - "7004:7004"
      - "17004:17004"
    volumes:
      - ./redis-7004.conf:/usr/local/etc/redis/redis.conf
      - ./data/redis-7004:/data
    networks:
      - redis-cluster

  redis-7005:
    image: redis:7.0
    container_name: redis-7005
    command: redis-server /usr/local/etc/redis/redis.conf
    ports:
      - "7005:7005"
      - "17005:17005"
    volumes:
      - ./redis-7005.conf:/usr/local/etc/redis/redis.conf
      - ./data/redis-7005:/data
    networks:
      - redis-cluster

networks:
  redis-cluster:
    driver: bridge
```

### 节点配置文件

```conf
# redis-7000.conf
port 7000
cluster-enabled yes
cluster-config-file nodes-7000.conf
cluster-node-timeout 5000
appendonly yes
dir /data

# 其他节点配置类似，修改端口号
```

### 初始化集群

```bash
# 1. 启动所有节点
docker-compose -f docker-compose-redis-cluster.yml up -d

# 2. 创建集群（3 主 3 从）
docker exec -it redis-7000 redis-cli --cluster create \
  172.18.0.2:7000 \
  172.18.0.3:7001 \
  172.18.0.4:7002 \
  172.18.0.5:7003 \
  172.18.0.6:7004 \
  172.18.0.7:7005 \
  --cluster-replicas 1

# 3. 查看集群状态
docker exec -it redis-7000 redis-cli -c -p 7000 CLUSTER INFO
docker exec -it redis-7000 redis-cli -c -p 7000 CLUSTER NODES
```

---

## 三、Spring Boot 配置

### 依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
</dependency>
```

### 配置文件

```yaml
spring:
  redis:
    cluster:
      nodes:
        - 192.168.1.10:7000
        - 192.168.1.10:7001
        - 192.168.1.10:7002
        - 192.168.1.10:7003
        - 192.168.1.10:7004
        - 192.168.1.10:7005
      max-redirects: 3  # 最大重定向次数
    lettuce:
      pool:
        max-active: 50   # 最大连接数
        max-idle: 20     # 最大空闲连接
        min-idle: 5      # 最小空闲连接
        max-wait: 3000ms # 连接超时
    timeout: 3000ms      # 命令超时
```

### 配置类

```java
/**
 * Redis 集群配置
 */
@Configuration
@EnableCaching
public class RedisConfig {
    
    /**
     * RedisTemplate 配置
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        
        // String 序列化
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        
        // JSON 序列化
        Jackson2JsonRedisSerializer<Object> jsonSerializer = 
            new Jackson2JsonRedisSerializer<>(Object.class);
        
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );
        jsonSerializer.setObjectMapper(om);
        
        // Key 采用 String 序列化
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        
        // Value 采用 JSON 序列化
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.afterPropertiesSet();
        return template;
    }
    
    /**
     * 缓存管理器
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))  // 默认 1 小时过期
            .disableCachingNullValues()     // 不缓存 null
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()
                )
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new Jackson2JsonRedisSerializer<>(Object.class)
                )
            );
        
        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .transactionAware()
            .build();
    }
}
```

---

## 四、Redis 脑裂问题

### 什么是脑裂？

**定义**：网络分区导致集群出现多个主节点，造成数据不一致。

**场景**：

```
正常集群：
Master-1 (0-5460)
  └── Slave-1

Master-2 (5461-10922)
  └── Slave-2

Master-3 (10923-16383)
  └── Slave-3

脑裂场景：
网络分区 → Master-1 与集群失联
  → Slave-1 被选举为新 Master-1'
  → 此时存在 Master-1 和 Master-1' 两个主节点

客户端写入 Master-1 的数据丢失！
```

### 脑裂的危害

```
1. 数据丢失：
   - 客户端写入老主节点（已被集群隔离）
   - 数据不会同步到新主节点
   - 网络恢复后，老主节点降级为从节点，数据被清空

2. 数据不一致：
   - 两个主节点同时接受写请求
   - 相同 key 的不同值
```

### Redis 脑裂检测

**Redis 提供两个参数防止脑裂**：

```conf
# redis.conf
min-replicas-to-write 1       # 最少从节点数量
min-replicas-max-lag 10       # 从节点最大延迟（秒）
```

**工作原理**：

```
1. 主节点检测与从节点的连接
2. 如果连接的从节点 < min-replicas-to-write
   或者延迟 > min-replicas-max-lag
3. 主节点拒绝写请求
```

**示例**：

```conf
# 配置
min-replicas-to-write 1
min-replicas-max-lag 10

# 效果：
# - 主节点至少有 1 个从节点连接
# - 从节点延迟不超过 10 秒
# - 否则主节点拒绝写请求，返回错误
```

### Spring Boot 配置防止脑裂

```yaml
spring:
  redis:
    cluster:
      nodes:
        - 192.168.1.10:7000
        - 192.168.1.10:7001
        - 192.168.1.10:7002
      max-redirects: 3
    # 客户端重试配置
    lettuce:
      cluster:
        refresh:
          adaptive: true  # 自适应拓扑刷新
          period: 5000    # 定期刷新间隔（毫秒）
```

```java
/**
 * Redis 操作失败重试
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisClientWithRetry {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 写入 Redis（带重试）
     */
    public void setWithRetry(String key, Object value, int maxRetries) {
        for (int i = 0; i < maxRetries; i++) {
            try {
                redisTemplate.opsForValue().set(key, value);
                return;
            } catch (Exception e) {
                log.warn("[Redis写入失败] 第 {} 次重试, key: {}", i + 1, key, e);
                
                if (i == maxRetries - 1) {
                    throw exception(ErrorCode.REDIS_ERROR, "Redis 写入失败");
                }
                
                // 休眠后重试
                try {
                    Thread.sleep(100 * (i + 1));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
```

---

## 五、分布式锁实现

### Redisson 分布式锁（推荐）

**依赖**：

```xml
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.24.3</version>
</dependency>
```

**配置**：

```yaml
spring:
  redis:
    redisson:
      config: |
        clusterServersConfig:
          idleConnectionTimeout: 10000
          connectTimeout: 10000
          timeout: 3000
          retryAttempts: 3
          retryInterval: 1500
          nodeAddresses:
            - "redis://192.168.1.10:7000"
            - "redis://192.168.1.10:7001"
            - "redis://192.168.1.10:7002"
            - "redis://192.168.1.10:7003"
            - "redis://192.168.1.10:7004"
            - "redis://192.168.1.10:7005"
        threads: 16
        nettyThreads: 32
```

**使用示例**：

```java
/**
 * Redisson 分布式锁
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements IProductService {
    
    private final ProductMapper productMapper;
    private final RedissonClient redissonClient;
    
    /**
     * 扣减库存（分布式锁）
     */
    @Override
    public void deductStock(Long productId, Integer quantity) {
        String lockKey = "product:stock:lock:" + productId;
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            // 尝试加锁（等待 3 秒，锁自动释放 10 秒）
            boolean acquired = lock.tryLock(3, 10, TimeUnit.SECONDS);
            
            if (!acquired) {
                throw exception(ErrorCode.SYSTEM_BUSY, "系统繁忙，请稍后重试");
            }
            
            // 1. 查询库存
            ProductDO product = productMapper.selectById(productId);
            if (product == null) {
                throw exception(ErrorCode.PRODUCT_NOT_FOUND);
            }
            
            // 2. 检查库存
            if (product.getStock() < quantity) {
                throw exception(ErrorCode.STOCK_NOT_ENOUGH);
            }
            
            // 3. 扣减库存
            product.setStock(product.getStock() - quantity);
            productMapper.updateById(product);
            
            log.info("[库存扣减成功] productId: {}, quantity: {}, 剩余: {}", 
                     productId, quantity, product.getStock() - quantity);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw exception(ErrorCode.SYSTEM_ERROR, "加锁被中断");
        } finally {
            // 4. 释放锁（只有锁的持有者才能释放）
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

### 分布式锁可能遇到的问题

#### 问题 1：死锁

**场景**：加锁后，程序异常退出，锁没有释放。

**解决方案**：

```java
// ✅ 方案 1：设置过期时间
lock.tryLock(3, 10, TimeUnit.SECONDS);
//           ↑     ↑
//        等待时间  锁过期时间

// ✅ 方案 2：Watch Dog 自动续期（Redisson 默认）
RLock lock = redissonClient.getLock(lockKey);
lock.lock();  // 默认 30 秒，Watch Dog 自动续期
```

#### 问题 2：锁误释放

**场景**：线程 A 的锁被线程 B 释放。

```
线程 A 加锁（10 秒）
  → 业务执行 12 秒
  → 锁自动过期
  → 线程 B 加锁成功
  → 线程 A 执行完成，释放锁（误释放线程 B 的锁）
```

**解决方案**：

```java
// ❌ 错误：使用 SETNX + DEL
String lockKey = "lock:product:" + productId;
Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);

// 业务处理...

redisTemplate.delete(lockKey);  // ❌ 可能删除别人的锁

// ✅ 正确：Redisson 自动处理（UUID 标识锁持有者）
RLock lock = redissonClient.getLock(lockKey);
lock.lock();

// 业务处理...

if (lock.isHeldByCurrentThread()) {  // ✅ 检查是否是当前线程持有
    lock.unlock();
}
```

#### 问题 3：主从切换导致锁失效

**场景**：主节点加锁后宕机，从节点未同步锁数据，新主节点允许其他线程加锁。

**解决方案**：

```java
// ✅ 使用 RedLock（多主节点）
RedissonClient client1 = ...;
RedissonClient client2 = ...;
RedissonClient client3 = ...;

RLock lock1 = client1.getLock(lockKey);
RLock lock2 = client2.getLock(lockKey);
RLock lock3 = client3.getLock(lockKey);

RedissonRedLock redLock = new RedissonRedLock(lock1, lock2, lock3);

try {
    // 至少在 N/2+1 个节点加锁成功
    boolean acquired = redLock.tryLock(3, 10, TimeUnit.SECONDS);
    
    if (acquired) {
        // 业务处理
    }
    
} finally {
    redLock.unlock();
}
```

#### 问题 4：锁续期失败

**场景**：业务执行时间过长，Watch Dog 续期失败，锁被释放。

**解决方案**：

```java
// ✅ 方案 1：增加锁过期时间
lock.tryLock(3, 60, TimeUnit.SECONDS);  // 60 秒

// ✅ 方案 2：分段加锁
public void longRunningTask(Long orderId) {
    String lockKey = "order:lock:" + orderId;
    RLock lock = redissonClient.getLock(lockKey);
    
    try {
        // 第 1 阶段
        lock.tryLock(3, 10, TimeUnit.SECONDS);
        stage1(orderId);
        lock.unlock();
        
        // 第 2 阶段
        lock.tryLock(3, 10, TimeUnit.SECONDS);
        stage2(orderId);
        lock.unlock();
        
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}
```

---

## 六、速查表

### Redis 集群命令

| 命令 | 说明 |
|------|------|
| **CLUSTER INFO** | 查看集群状态 |
| **CLUSTER NODES** | 查看节点列表 |
| **CLUSTER SLOTS** | 查看槽位分配 |
| **CLUSTER KEYSLOT <key>** | 查看 key 的槽位 |
| **CLUSTER MEET <ip> <port>** | 添加节点 |
| **CLUSTER FAILOVER** | 手动故障转移 |

### 分布式锁对比

| 方案 | 优点 | 缺点 | 推荐 |
|------|------|------|------|
| **SETNX + DEL** | 简单 | 误删锁、死锁 | ❌ |
| **SETNX + Lua** | 原子操作 | 实现复杂 | ⚠️ |
| **Redisson** | 功能完善、自动续期 | 依赖第三方库 | ✅ |
| **RedLock** | 高可用 | 性能较差 | ⚠️ |

### 脑裂防护配置

| 参数 | 说明 | 推荐值 |
|------|------|--------|
| **min-replicas-to-write** | 最少从节点数 | 1 |
| **min-replicas-max-lag** | 最大延迟（秒） | 10 |

### Redisson 锁类型

| 锁类型 | 说明 | 使用场景 |
|--------|------|---------|
| **RLock** | 可重入锁 | 常规场景 |
| **RFairLock** | 公平锁 | 需要公平性 |
| **RReadWriteLock** | 读写锁 | 读多写少 |
| **RSemaphore** | 信号量 | 限流 |
| **RCountDownLatch** | 倒计数锁 | 等待多个任务 |
