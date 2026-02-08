# Redis Hash 数据类型

> 分类: Redis | 难度: ⭐⭐ | 频率: 高频

---

## 一、什么是 Redis Hash

Hash 是 Redis 中用于存储**键值对集合**的数据结构，适合存储对象。

```
┌─────────────────────────────────────────────────────────────────┐
│                    Redis Hash 结构                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│   Key: user:1001                                                  │
│   ┌─────────────────────────────────────────────────────────────┐ │
│   │  Field        │         Value                               │ │
│   ├───────────────┼─────────────────────────────────────────────┤ │
│   │  name         │         "张三"                              │ │
│   │  age          │         "28"                                │ │
│   │  email        │         "zhang@example.com"                 │ │
│   │  phone        │         "13800138000"                       │ │
│   └───────────────┴─────────────────────────────────────────────┘ │
│                                                                   │
│   等价于:  Map<String, Map<String, String>>                       │
│            外层key是Redis key，内层是field-value对                │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 二、底层数据结构

### 2.1 两种编码方式

```
┌────────────────────────────────────────────────────────────────────────────┐
│                      Hash 编码方式                                          │
├─────────────────────┬──────────────────────────────────────────────────────┤
│                     │                                                       │
│   listpack/ziplist  │   当元素个数 < 512 且所有值 < 64字节时使用            │
│   (紧凑列表)        │   内存连续，节省空间，适合小 Hash                     │
│                     │   Redis 7.0+ 使用 listpack 替代 ziplist              │
│                     │                                                       │
├─────────────────────┼──────────────────────────────────────────────────────┤
│                     │                                                       │
│   hashtable         │   元素较多或值较大时使用                              │
│   (哈希表)          │   使用 dict 实现，O(1) 查找                           │
│                     │   渐进式 rehash 避免阻塞                              │
│                     │                                                       │
└─────────────────────┴──────────────────────────────────────────────────────┘
```

### 2.2 编码转换阈值

```bash
# redis.conf 配置
hash-max-listpack-entries 512    # 元素个数阈值
hash-max-listpack-value 64       # 单个值大小阈值(字节)

# 超过任一阈值自动转换为 hashtable
```

---

## 三、常用命令

### 3.1 基础操作

```bash
# 设置单个字段
HSET user:1001 name "张三"

# 设置多个字段
HMSET user:1001 name "张三" age "28" email "zhang@example.com"
# Redis 4.0+ 推荐直接用 HSET
HSET user:1001 name "张三" age "28" email "zhang@example.com"

# 获取单个字段
HGET user:1001 name
# 返回: "张三"

# 获取多个字段
HMGET user:1001 name age
# 返回: 1) "张三" 2) "28"

# 获取所有字段和值
HGETALL user:1001
# 返回: 1) "name" 2) "张三" 3) "age" 4) "28" ...

# 获取所有字段名
HKEYS user:1001

# 获取所有值
HVALS user:1001

# 获取字段数量
HLEN user:1001

# 判断字段是否存在
HEXISTS user:1001 name
# 返回: 1 (存在) 或 0 (不存在)

# 删除字段
HDEL user:1001 phone

# 字段不存在时设置
HSETNX user:1001 name "李四"
# 返回: 0 (name已存在，设置失败)
```

### 3.2 数值操作

```bash
# 整数增减
HINCRBY user:1001 age 1
# 返回: "29"

# 浮点数增减
HINCRBYFLOAT product:1 price 0.5

# 实现计数器
HINCRBY article:1001 views 1
HINCRBY article:1001 likes 1
```

### 3.3 扫描命令

```bash
# 增量迭代(避免阻塞)
HSCAN user:1001 0 MATCH "email*" COUNT 10
```

---

## 四、Java 操作示例

### 4.1 使用 RedisTemplate

```java
@Service
public class UserCacheService {
    
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String USER_KEY_PREFIX = "user:";
    
    /**
     * 缓存用户信息
     */
    public void cacheUser(User user) {
        String key = USER_KEY_PREFIX + user.getId();
        
        Map<String, String> userMap = new HashMap<>();
        userMap.put("name", user.getName());
        userMap.put("age", String.valueOf(user.getAge()));
        userMap.put("email", user.getEmail());
        userMap.put("phone", user.getPhone());
        
        redisTemplate.opsForHash().putAll(key, userMap);
        redisTemplate.expire(key, 24, TimeUnit.HOURS);
    }
    
    /**
     * 获取用户信息
     */
    public User getUser(Long userId) {
        String key = USER_KEY_PREFIX + userId;
        
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        if (entries.isEmpty()) {
            return null;
        }
        
        User user = new User();
        user.setId(userId);
        user.setName((String) entries.get("name"));
        user.setAge(Integer.parseInt((String) entries.get("age")));
        user.setEmail((String) entries.get("email"));
        user.setPhone((String) entries.get("phone"));
        return user;
    }
    
    /**
     * 更新单个字段
     */
    public void updateField(Long userId, String field, String value) {
        String key = USER_KEY_PREFIX + userId;
        redisTemplate.opsForHash().put(key, field, value);
    }
    
    /**
     * 增加浏览次数
     */
    public Long incrementViews(Long articleId) {
        String key = "article:" + articleId;
        return redisTemplate.opsForHash().increment(key, "views", 1);
    }
}
```

### 4.2 使用 Redisson

```java
@Service
public class UserRedissonService {
    
    @Resource
    private RedissonClient redissonClient;
    
    public void cacheUser(User user) {
        RMap<String, String> userMap = redissonClient.getMap("user:" + user.getId());
        userMap.put("name", user.getName());
        userMap.put("age", String.valueOf(user.getAge()));
        userMap.put("email", user.getEmail());
        userMap.expire(Duration.ofHours(24));
    }
    
    public Map<String, String> getUser(Long userId) {
        RMap<String, String> userMap = redissonClient.getMap("user:" + userId);
        return userMap.readAllMap();
    }
}
```

---

## 五、使用场景

### 5.1 存储对象

```java
// 优势: 可以单独更新某个字段，无需序列化整个对象

// 场景1: 用户信息
HSET user:1001 name "张三" age "28" vip_level "3"

// 场景2: 商品信息
HSET product:2001 name "iPhone" price "9999" stock "100"

// 更新库存(无需读取整个对象)
HINCRBY product:2001 stock -1
```

### 5.2 购物车

```java
/**
 * 购物车服务
 */
@Service
public class CartService {
    
    @Resource
    private RedisTemplate<String, String> redisTemplate;
    
    private String getCartKey(Long userId) {
        return "cart:" + userId;
    }
    
    // 添加商品
    public void addItem(Long userId, Long productId, int quantity) {
        String key = getCartKey(userId);
        redisTemplate.opsForHash().increment(key, productId.toString(), quantity);
    }
    
    // 获取购物车
    public Map<Long, Integer> getCart(Long userId) {
        String key = getCartKey(userId);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        
        return entries.entrySet().stream()
            .collect(Collectors.toMap(
                e -> Long.parseLong((String) e.getKey()),
                e -> Integer.parseInt((String) e.getValue())
            ));
    }
    
    // 删除商品
    public void removeItem(Long userId, Long productId) {
        String key = getCartKey(userId);
        redisTemplate.opsForHash().delete(key, productId.toString());
    }
    
    // 清空购物车
    public void clearCart(Long userId) {
        redisTemplate.delete(getCartKey(userId));
    }
}
```

### 5.3 计数统计

```java
// 文章统计
HSET article:1001 views "0" likes "0" comments "0" shares "0"

// 增加浏览
HINCRBY article:1001 views 1

// 获取统计
HGETALL article:1001
```

---

## 六、Hash vs String 存储对象

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    Hash vs String 对比                                       │
├─────────────────┬────────────────────────────┬──────────────────────────────┤
│                 │         Hash               │         String               │
├─────────────────┼────────────────────────────┼──────────────────────────────┤
│  存储方式       │  字段分开存储               │  序列化为JSON/二进制         │
│  部分更新       │  ✅ 可以只更新一个字段      │  ❌ 需要全量覆盖             │
│  内存效率       │  ✅ 小对象使用listpack压缩  │  较大开销                    │
│  获取部分字段   │  ✅ HGET/HMGET             │  ❌ 必须获取整个对象          │
│  过期时间       │  ❌ 只能对整个key设置       │  ✅ 可对每个key设置          │
│  复杂对象       │  ❌ 只支持一层              │  ✅ 支持嵌套对象             │
│  适用场景       │  频繁部分更新的对象         │  整体读写的对象              │
└─────────────────┴────────────────────────────┴──────────────────────────────┘
```

---

## 七、面试回答

### 30秒版本

> Redis Hash 是存储键值对集合的数据类型，类似 `Map<String, Map<String, String>>`。底层使用 **listpack**(小数据量) 或 **hashtable**(大数据量)。适合存储对象，支持**单字段更新**，比 String 存 JSON 更节省内存，常用于用户信息、购物车、计数统计等场景。

### 1分钟版本

> Hash 是 Redis 的复合数据类型，一个 key 对应多个 field-value 对：
>
> **底层结构：**
> - 小 Hash（元素<512，值<64字节）使用 listpack（Redis 7+）压缩存储
> - 大 Hash 使用 hashtable，O(1) 查找
>
> **优势：**
> - 支持单字段读写，无需全量序列化
> - 小对象内存效率高
> - 支持 HINCRBY 原子计数
>
> **典型场景：**
> - 用户信息缓存
> - 购物车（用户ID为key，商品ID为field）
> - 文章计数（views/likes/comments）
>
> **缺点：**
> - 不支持字段级别过期
> - 不支持嵌套对象

---

## 八、最佳实践

### ✅ 推荐做法

```java
// 1. 合理使用 Hash 存储对象
// 字段少于512个，单值小于64字节时内存最优

// 2. 使用 HMSET/HMGET 批量操作减少网络往返
redisTemplate.opsForHash().putAll(key, map);
redisTemplate.opsForHash().multiGet(key, Arrays.asList("f1", "f2"));

// 3. 使用 HSCAN 遍历大 Hash
Cursor<Map.Entry<Object, Object>> cursor = redisTemplate.opsForHash()
    .scan(key, ScanOptions.scanOptions().match("*").count(100).build());

// 4. 计数场景使用 HINCRBY 保证原子性
redisTemplate.opsForHash().increment(key, "views", 1);
```

### ❌ 避免做法

```java
// ❌ Hash 元素过多（超过10万字段）
// 应该拆分为多个 Hash

// ❌ 单个字段值过大
// 大于 1MB 的值应该单独存储

// ❌ 对 Hash 字段设置过期时间
// Redis 不支持，应该用定时任务清理或拆分为多个 key

// ❌ 使用 HGETALL 获取大 Hash
// 会阻塞 Redis，应使用 HSCAN 增量获取
```
