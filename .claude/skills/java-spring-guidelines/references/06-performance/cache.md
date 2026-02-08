# 缓存规范

> Java/Spring Boot 编码规范 - 缓存使用

---

## Key 命名规范

### 命名格式

```
{业务}:{模块}:{标识}
```

### 示例

| 场景 | Key 格式 | 示例 |
|------|---------|------|
| 商品详情 | `product:detail:{id}` | `product:detail:123` |
| 用户信息 | `user:info:{id}` | `user:info:456` |
| 用户 Token | `user:token:{userId}` | `user:token:789` |
| 分类列表 | `category:list` | `category:list` |
| 订单统计 | `order:stats:{date}` | `order:stats:2026-01-28` |
| 库存数量 | `stock:product:{productId}` | `stock:product:100` |

```java
// ✅ 正确：层级清晰
String key = "product:detail:" + productId;
String key = "user:info:" + userId;
String key = "order:stats:" + LocalDate.now();

// ❌ 错误：无层级
String key = "product" + productId;
String key = "product_detail_" + productId;
```

---

## TTL 设置规范

### TTL 推荐值

| 数据类型 | TTL | 说明 |
|---------|-----|------|
| 热点数据 | 1-5 分钟 | 高频访问，如秒杀商品 |
| 普通数据 | 30 分钟 | 一般业务数据 |
| 配置数据 | 1 小时 | 变更较少的配置 |
| 统计数据 | 5-15 分钟 | 允许短时延迟 |
| **永不过期** | ❌ **禁止** | 必须设置 TTL |

### TTL 设置示例

```java
// 商品详情 - 30 分钟
redisTemplate.opsForValue().set(key, value, 30, TimeUnit.MINUTES);

// 用户信息 - 1 小时
redisTemplate.opsForValue().set(key, value, 1, TimeUnit.HOURS);

// 热点数据 - 5 分钟
redisTemplate.opsForValue().set(key, value, 5, TimeUnit.MINUTES);

// ❌ 错误：永不过期
redisTemplate.opsForValue().set(key, value);
```

### 使用常量管理 TTL

```java
/**
 * Redis TTL 常量
 */
public class RedisTtlConstants {

    /** 5 分钟 */
    public static final long TTL_5_MIN = 5 * 60;
    /** 15 分钟 */
    public static final long TTL_15_MIN = 15 * 60;
    /** 30 分钟 */
    public static final long TTL_30_MIN = 30 * 60;
    /** 1 小时 */
    public static final long TTL_1_HOUR = 60 * 60;
    /** 1 天 */
    public static final long TTL_1_DAY = 24 * 60 * 60;
}

// 使用
redisTemplate.opsForValue().set(key, value, RedisTtlConstants.TTL_30_MIN, TimeUnit.SECONDS);
```

---

## 缓存穿透防护

### 方案一：空值缓存

```java
public Product getProductById(Long productId) {
    String key = "product:detail:" + productId;

    // 1. 查询缓存
    Product product = (Product) redisTemplate.opsForValue().get(key);
    if (product != null) {
        // 空对象标记
        if ("NULL".equals(product.getId())) {
            return null;
        }
        return product;
    }

    // 2. 查询数据库
    product = productMapper.selectById(productId);

    // 3. 缓存结果（包括空值）
    if (product != null) {
        redisTemplate.opsForValue().set(key, product, 30, TimeUnit.MINUTES);
    } else {
        // 空值缓存，TTL 较短
        Product nullMarker = new Product();
        nullMarker.setId("NULL");
        redisTemplate.opsForValue().set(key, nullMarker, 5, TimeUnit.MINUTES);
    }

    return product;
}
```

### 方案二：布隆过滤器

```java
@Component
@RequiredArgsConstructor
public class ProductService {

    private final BloomFilter<Long> bloomFilter;

    @PostConstruct
    public void init() {
        // 初始化布隆过滤器
        bloomFilter = BloomFilter.create(
            Funnels.longFunnel(),
            1000000,  // 预计元素数量
            0.01      // 误判率
        );

        // 预加载所有商品 ID
        List<Long> allIds = productMapper.selectAllIds();
        allIds.forEach(bloomFilter::put);
    }

    public Product getProductById(Long productId) {
        // 布隆过滤器判断
        if (!bloomFilter.mightContain(productId)) {
            return null;  // 一定不存在
        }

        String key = "product:detail:" + productId;
        Product product = (Product) redisTemplate.opsForValue().get(key);
        if (product != null) {
            return product;
        }

        product = productMapper.selectById(productId);
        if (product != null) {
            redisTemplate.opsForValue().set(key, product, 30, TimeUnit.MINUTES);
        }

        return product;
    }
}
```

---

## 缓存雪崩防护

### 方案一：TTL 随机化

```java
public void cacheProduct(Product product) {
    String key = "product:detail:" + product.getId();

    // TTL 基础 30 分钟 + 随机 0-5 分钟
    long ttl = 30 * 60 + new Random().nextInt(5 * 60);

    redisTemplate.opsForValue().set(key, product, ttl, TimeUnit.SECONDS);
}
```

### 方案二：多级缓存

```java
@Component
@RequiredArgsConstructor
public class ProductService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final LoadingCache<Long, Product> localCache;

    @PostConstruct
    public void init() {
        // 本地缓存（Caffeine）
        localCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build(id -> loadProductFromDb(id));
    }

    public Product getProductById(Long productId) {
        // 1. 本地缓存
        Product product = localCache.getIfPresent(productId);
        if (product != null) {
            return product;
        }

        // 2. Redis 缓存
        String key = "product:detail:" + productId;
        product = (Product) redisTemplate.opsForValue().get(key);
        if (product != null) {
            localCache.put(productId, product);
            return product;
        }

        // 3. 数据库
        product = loadProductFromDb(productId);
        if (product != null) {
            redisTemplate.opsForValue().set(key, product, 30, TimeUnit.MINUTES);
            localCache.put(productId, product);
        }

        return product;
    }
}
```

---

## 缓存更新策略

### 先更新数据库，再删除缓存

```java
@Transactional(rollbackFor = Exception.class)
public void updateProduct(ProductUpdateReq req) {
    // 1. 更新数据库
    Product product = new Product();
    BeanUtils.copyProperties(req, product);
    productMapper.updateById(product);

    // 2. 删除缓存（而不是更新）
    String key = "product:detail:" + req.getId();
    redisTemplate.delete(key);

    log.info("[商品更新]，已删除缓存，key: {}", key);
}

// 下次查询时重新加载缓存
public Product getProductById(Long productId) {
    String key = "product:detail:" + productId;

    // 先查缓存
    Product product = (Product) redisTemplate.opsForValue().get(key);
    if (product != null) {
        return product;
    }

    // 缓存未命中，查数据库
    product = productMapper.selectById(productId);
    if (product != null) {
        redisTemplate.opsForValue().set(key, product, 30, TimeUnit.MINUTES);
    }

    return product;
}
```

### 延迟双删（高并发场景）

```java
@Transactional(rollbackFor = Exception.class)
public void updateProduct(ProductUpdateReq req) {
    String key = "product:detail:" + req.getId();

    // 1. 删除缓存
    redisTemplate.delete(key);

    // 2. 更新数据库
    Product product = new Product();
    BeanUtils.copyProperties(req, product);
    productMapper.updateById(product);

    // 3. 延迟再删除缓存（异步）
    CompletableFuture.runAsync(() -> {
        try {
            Thread.sleep(500);  // 延迟 500ms
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        redisTemplate.delete(key);
        log.info("[商品更新]，延迟删除缓存完成，key: {}", key);
    });
}
```

---

## 分布式锁缓存更新

```java
public Product getProductById(Long productId) {
    String key = "product:detail:" + productId;
    String lockKey = "lock:product:" + productId;

    // 1. 查询缓存
    Product product = (Product) redisTemplate.opsForValue().get(key);
    if (product != null) {
        return product;
    }

    // 2. 获取分布式锁
    RLock lock = redissonClient.getLock(lockKey);
    try {
        // 尝试加锁，最多等待 5 秒，锁 10 秒后自动释放
        if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
            try {
                // 3. 双重检查（其他线程可能已加载）
                product = (Product) redisTemplate.opsForValue().get(key);
                if (product != null) {
                    return product;
                }

                // 4. 查询数据库
                product = productMapper.selectById(productId);

                // 5. 写入缓存
                if (product != null) {
                    redisTemplate.opsForValue().set(key, product, 30, TimeUnit.MINUTES);
                }

                return product;
            } finally {
                lock.unlock();
            }
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }

    // 获取锁失败，直接查数据库
    return productMapper.selectById(productId);
}
```

---

## 缓存使用示例

### String 类型

```java
// 存储
redisTemplate.opsForValue().set(key, value, 30, TimeUnit.MINUTES);

// 获取
Product product = (Product) redisTemplate.opsForValue().get(key);

// 删除
redisTemplate.delete(key);

// 批量删除
redisTemplate.delete(Arrays.asList(key1, key2, key3));
```

### Hash 类型

```java
// 存储
redisTemplate.opsForHash().put("user:info:" + userId, "name", "张三");
redisTemplate.opsForHash().put("user:info:" + userId, "age", "25");

// 获取单个字段
String name = (String) redisTemplate.opsForHash().get("user:info:" + userId, "name");

// 获取整个对象
Map<Object, Object> map = redisTemplate.opsForHash().entries("user:info:" + userId);

// 删除字段
redisTemplate.opsForHash().delete("user:info:" + userId, "age");
```

### List 类型

```java
// 右侧推入
redisTemplate.opsForList().rightPush("order:queue", orderId);

// 左侧弹出（阻塞）
String orderId = (String) redisTemplate.opsForList().leftPop("order:queue", 5, TimeUnit.SECONDS);

// 获取列表
List<String> list = redisTemplate.opsForList().range("order:queue", 0, -1);
```

### Set 类型

```java
// 添加
redisTemplate.opsForSet().add("user:tags:" + userId, "tag1", "tag2", "tag3");

// 获取所有
Set<String> tags = redisTemplate.opsForSet().members("user:tags:" + userId);

// 判断是否存在
Boolean exists = redisTemplate.opsForSet().isMember("user:tags:" + userId, "tag1");
```

---

## 缓存规范速查表

| 规范 | 要点 |
|------|------|
| **Key 格式** | `{业务}:{模块}:{标识}` |
| **TTL** | 必须 TTL 设置，禁止永不过期 |
| **热点数据** | TTL 1-5 分钟 |
| **普通数据** | TTL 30 分钟 |
| **更新策略** | 先更新 DB，再删除缓存 |
| **穿透防护** | 空值缓存（短 TTL）或布隆过滤器 |
| **雪崩防护** | TTL 随机化、多级缓存 |
| **并发更新** | 分布式锁 |
