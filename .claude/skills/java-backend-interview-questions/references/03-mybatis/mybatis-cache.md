# MyBatis 缓存机制

> Java 后端面试知识点 - MyBatis 深入

---

## 缓存概述

MyBatis 提供两级缓存机制：

```
┌─────────────────────────────────────────────────────────────────┐
│                        应用程序                                  │
│                            ↓                                    │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                   一级缓存（SqlSession 级别）             │   │
│  │                   默认开启，Session 范围                  │   │
│  └─────────────────────────────────────────────────────────┘   │
│                            ↓ 未命中                             │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                   二级缓存（Mapper 级别）                 │   │
│  │                   需手动开启，Namespace 范围              │   │
│  └─────────────────────────────────────────────────────────┘   │
│                            ↓ 未命中                             │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                       数据库                              │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 一级缓存（Local Cache）

### 特点

| 特性 | 说明 |
|------|------|
| **作用域** | SqlSession 级别 |
| **默认状态** | 开启，无法关闭 |
| **存储位置** | 内存（HashMap） |
| **生命周期** | SqlSession 创建到关闭 |

### 工作原理

```java
// 一级缓存示例
SqlSession session = sqlSessionFactory.openSession();
UserMapper mapper = session.getMapper(UserMapper.class);

User user1 = mapper.selectById(1L);  // 查询数据库
User user2 = mapper.selectById(1L);  // 命中缓存，不查库

System.out.println(user1 == user2);  // true，同一对象

session.close();
```

### 缓存失效场景

```java
// 1. 不同 SqlSession
SqlSession session1 = sqlSessionFactory.openSession();
SqlSession session2 = sqlSessionFactory.openSession();
User user1 = session1.getMapper(UserMapper.class).selectById(1L);
User user2 = session2.getMapper(UserMapper.class).selectById(1L);
// user1 != user2，不同 Session 不共享缓存

// 2. 执行了增删改操作
User user1 = mapper.selectById(1L);  // 查询
mapper.updateById(user);              // 更新，清空缓存
User user2 = mapper.selectById(1L);  // 重新查库

// 3. 手动清空缓存
session.clearCache();

// 4. 执行了 commit/rollback
session.commit();  // 清空缓存
```

### Spring 中一级缓存的问题

```java
// ⚠️ Spring 中每次调用 Mapper 方法都是新的 SqlSession
@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    public void demo() {
        User user1 = userMapper.selectById(1L);  // SqlSession1
        User user2 = userMapper.selectById(1L);  // SqlSession2
        // 一级缓存不生效！
    }
    
    // ✅ 加 @Transactional 保证同一 SqlSession
    @Transactional
    public void demoWithTransaction() {
        User user1 = userMapper.selectById(1L);  // 相同 SqlSession
        User user2 = userMapper.selectById(1L);  // 命中一级缓存
    }
}
```

---

## 二级缓存（Global Cache）

### 特点

| 特性 | 说明 |
|------|------|
| **作用域** | Mapper（Namespace）级别 |
| **默认状态** | 关闭，需手动开启 |
| **存储位置** | 内存或外部缓存 |
| **生命周期** | 应用级别 |

### 开启二级缓存

```xml
<!-- mybatis-config.xml 全局开启 -->
<settings>
    <setting name="cacheEnabled" value="true"/>
</settings>

<!-- Mapper.xml 中开启 -->
<mapper namespace="com.example.mapper.UserMapper">
    <cache
        eviction="LRU"           <!-- 淘汰策略：LRU/FIFO/SOFT/WEAK -->
        flushInterval="60000"    <!-- 刷新间隔：60秒 -->
        size="512"               <!-- 缓存对象数量 -->
        readOnly="false"/>       <!-- 是否只读 -->
    
    <select id="selectById" resultType="User" useCache="true">
        SELECT * FROM user WHERE id = #{id}
    </select>
</mapper>
```

```java
// 实体类需要实现 Serializable（非只读模式）
@Data
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
}
```

### 二级缓存工作流程

```java
// 二级缓存示例
SqlSession session1 = sqlSessionFactory.openSession();
User user1 = session1.getMapper(UserMapper.class).selectById(1L);
session1.close();  // ⚠️ 必须关闭 Session，缓存才会写入二级缓存

SqlSession session2 = sqlSessionFactory.openSession();
User user2 = session2.getMapper(UserMapper.class).selectById(1L);  // 命中二级缓存
session2.close();
```

### 缓存淘汰策略

| 策略 | 说明 |
|------|------|
| **LRU** | 最近最少使用（默认） |
| **FIFO** | 先进先出 |
| **SOFT** | 软引用，内存不足时回收 |
| **WEAK** | 弱引用，GC 时回收 |

---

## 整合第三方缓存

### 整合 Redis

```xml
<!-- 依赖 -->
<dependency>
    <groupId>org.mybatis.caches</groupId>
    <artifactId>mybatis-redis</artifactId>
    <version>1.0.0-beta2</version>
</dependency>
```

```xml
<!-- Mapper.xml -->
<cache type="org.mybatis.caches.redis.RedisCache"/>
```

```properties
# redis.properties
redis.host=localhost
redis.port=6379
```

### 自定义缓存

```java
// 实现 Cache 接口
public class MyRedisCache implements Cache {
    
    private final String id;
    private final RedisTemplate<String, Object> redisTemplate;
    
    public MyRedisCache(String id) {
        this.id = id;
        this.redisTemplate = SpringContextHolder.getBean(RedisTemplate.class);
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public void putObject(Object key, Object value) {
        redisTemplate.opsForHash().put(id, key.toString(), value);
    }
    
    @Override
    public Object getObject(Object key) {
        return redisTemplate.opsForHash().get(id, key.toString());
    }
    
    @Override
    public Object removeObject(Object key) {
        return redisTemplate.opsForHash().delete(id, key.toString());
    }
    
    @Override
    public void clear() {
        redisTemplate.delete(id);
    }
    
    @Override
    public int getSize() {
        return redisTemplate.opsForHash().size(id).intValue();
    }
}
```

---

## 二级缓存的问题

### 1. 脏读问题

```
UserMapper 和 OrderMapper 都操作 user 表

时间线：
T1: UserMapper 查询用户，结果存入 UserMapper 二级缓存
T2: OrderMapper 更新用户（关联更新）
T3: UserMapper 查询用户，命中缓存，但数据已过期！
```

### 2. 分布式环境问题

```
服务器 A 更新数据 → 清空本地二级缓存
服务器 B 查询数据 → 命中本地过期缓存 ❌
```

---

## 最佳实践

### ✅ 推荐做法

```java
// 1. 使用 Redis 代替 MyBatis 二级缓存
@Service
public class UserService {
    
    @Cacheable(value = "user", key = "#id")
    public User getById(Long id) {
        return userMapper.selectById(id);
    }
    
    @CacheEvict(value = "user", key = "#user.id")
    public void update(User user) {
        userMapper.updateById(user);
    }
}

// 2. 合理利用一级缓存
@Transactional
public void batchProcess() {
    // 同一事务内的重复查询会命中一级缓存
    for (Long id : ids) {
        User user = userMapper.selectById(id);  // 相同 id 只查一次
    }
}

// 3. 只读场景可开启二级缓存
// 配置字典、枚举等不经常变化的数据
```

### ❌ 避免做法

```java
// ❌ 在分布式环境使用本地二级缓存
<cache/>  // 多节点数据不一致

// ❌ 多表关联场景使用二级缓存
// 会导致脏读

// ❌ 忘记事务导致一级缓存失效
// Spring 中不加 @Transactional 每次都是新 Session
```

---

## 面试要点

### 核心答案

**问：说说 MyBatis 的缓存机制？**

答：MyBatis 有两级缓存：

1. **一级缓存**
   - SqlSession 级别，默认开启
   - 同一 Session 内相同查询命中缓存
   - 增删改或 Session 关闭会清空缓存
   - Spring 中需 `@Transactional` 保证生效

2. **二级缓存**
   - Mapper（Namespace）级别，需手动开启
   - 跨 Session 共享，Session 关闭后写入
   - 支持 LRU/FIFO 等淘汰策略
   - 有脏读和分布式一致性问题

**实际建议**：
- 一级缓存配合事务使用
- 二级缓存慎用，推荐 Spring Cache + Redis 代替
