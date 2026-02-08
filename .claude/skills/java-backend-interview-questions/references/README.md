# Java 后端面试知识点与最佳实践

本 Skill 涵盖 Java 后端开发高频面试题及实际编码最佳实践。

## 目录结构

```
java-backend-interview-questions/
├── SKILL.md                          # 主入口文件
├── README.md                         # 说明文档
└── references/
    ├── 01-network/                   # 网络协议
    │   ├── osi-model.md              # OSI 七层模型
    │   ├── tcp-ip-model.md           # TCP/IP 四层模型
    │   ├── tcp-time-wait.md          # TCP TIME_WAIT 状态
    │   ├── tcp-retransmission.md     # TCP 超时重传机制
    │   └── tcp-sliding-window.md     # TCP 滑动窗口
    │
    ├── 02-spring/                    # Spring 生态
    │   ├── spring-boot-intro.md      # 什么是 Spring Boot
    │   └── spring-boot-features.md   # Spring Boot 核心特性
    │
    ├── 03-mybatis/                   # MyBatis
    │   ├── mybatis-cache.md          # MyBatis 缓存机制
    │   └── mybatis-plugin.md         # MyBatis 插件机制
    │
    ├── 04-redis/                     # Redis
    │   ├── redis-data-types.md       # Redis 数据类型
    │   ├── redis-skiplist.md         # Redis 跳表原理
    │   ├── redis-clients.md          # Redis 客户端选型
    │   └── redis-performance.md      # Redis 性能优化
    │
    ├── 05-concurrency/               # Java 并发
    │   ├── thread-creation.md        # 多线程创建方式
    │   ├── thread-pool.md            # 线程池核心参数
    │   ├── aqs.md                    # AQS 原理
    │   └── reactor-model.md          # Reactor 线程模型
    │
    ├── 06-data-structure/            # 数据结构
    │   ├── hashmap-resize.md         # HashMap 扩容机制
    │   └── array-vs-linkedlist.md    # 数组与链表
    │
    └── 07-session/                   # 会话管理
        └── session-management.md     # Cookie/Session/Token
```

## 快速索引

### 网络协议

| 问题 | 文档 | 核心答案 |
|------|------|---------|
| 为什么 TCP 需要 TIME_WAIT? | [tcp-time-wait.md](references/01-network/tcp-time-wait.md) | 1. 确保最后 ACK 到达 2. 让旧数据包消失 |
| TCP 超时重传解决什么问题? | [tcp-retransmission.md](references/01-network/tcp-retransmission.md) | 解决丢包问题，保证可靠传输 |
| TCP 滑动窗口的作用? | [tcp-sliding-window.md](references/01-network/tcp-sliding-window.md) | 流量控制 + 提高传输效率 |
| OSI 七层模型? | [osi-model.md](references/01-network/osi-model.md) | 应表会传网数物 |
| TCP/IP 四层模型? | [tcp-ip-model.md](references/01-network/tcp-ip-model.md) | 应用/传输/网络/网络接口 |

### Spring 生态

| 问题 | 文档 | 核心答案 |
|------|------|---------|
| 什么是 Spring Boot? | [spring-boot-intro.md](references/02-spring/spring-boot-intro.md) | 约定优于配置的快速开发工具 |
| Spring Boot 核心特性? | [spring-boot-features.md](references/02-spring/spring-boot-features.md) | 自动配置、Starter、嵌入式服务器、Actuator |

### MyBatis

| 问题 | 文档 | 核心答案 |
|------|------|---------|
| MyBatis 缓存机制? | [mybatis-cache.md](references/03-mybatis/mybatis-cache.md) | 一级缓存(Session级) + 二级缓存(Mapper级) |
| MyBatis 插件原理? | [mybatis-plugin.md](references/03-mybatis/mybatis-plugin.md) | JDK 动态代理 + 拦截器链 |

### Redis

| 问题 | 文档 | 核心答案 |
|------|------|---------|
| Redis 数据类型? | [redis-data-types.md](references/04-redis/redis-data-types.md) | String/List/Hash/Set/ZSet + 高级类型 |
| Redis 跳表原理? | [redis-skiplist.md](references/04-redis/redis-skiplist.md) | 多级索引链表，O(log n) 查询 |
| Redis 客户端选型? | [redis-clients.md](references/04-redis/redis-clients.md) | Lettuce(默认)/Redisson(分布式锁) |
| Redis 性能优化? | [redis-performance.md](references/04-redis/redis-performance.md) | Pipeline/SCAN/大Key拆分/连接池 |

### Java 并发

| 问题 | 文档 | 核心答案 |
|------|------|---------|
| 如何创建多线程? | [thread-creation.md](references/05-concurrency/thread-creation.md) | Thread/Runnable/Callable/线程池 |
| 线程池参数能动态修改吗? | [thread-pool.md](references/05-concurrency/thread-pool.md) | 可以，setCorePoolSize()/setMaximumPoolSize() |
| 说说 AQS? | [aqs.md](references/05-concurrency/aqs.md) | state变量 + CLH队列 + CAS |
| Reactor 线程模型? | [reactor-model.md](references/05-concurrency/reactor-model.md) | 主从Reactor多线程(Netty模型) |

### 数据结构

| 问题 | 文档 | 核心答案 |
|------|------|---------|
| HashMap 为什么用 2^n 扩容? | [hashmap-resize.md](references/06-data-structure/hashmap-resize.md) | 位运算高效 + 元素分布均匀 |
| 数组和链表区别? | [array-vs-linkedlist.md](references/06-data-structure/array-vs-linkedlist.md) | 随机访问vs插入删除 |

### 会话管理

| 问题 | 文档 | 核心答案 |
|------|------|---------|
| Cookie/Session/Token 区别? | [session-management.md](references/07-session/session-management.md) | 存储位置/安全性/分布式支持 |

## 使用方式

1. **面试准备**：按目录阅读各主题的核心答案
2. **编码参考**：查看每个文档中的"编码最佳实践"部分
3. **深入学习**：阅读完整文档了解原理和源码分析

## 编码规范速查

### ✅ 推荐做法

```java
// 线程池：使用 ThreadPoolExecutor
ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 20, 60, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(1000),
    new ThreadFactoryBuilder().setNameFormat("biz-%d").build(),
    new ThreadPoolExecutor.CallerRunsPolicy());

// Redis：使用连接池 + Pipeline
redisTemplate.executePipelined((RedisCallback<Object>) conn -> {
    for (String key : keys) conn.get(key.getBytes());
    return null;
});

// HashMap：预估容量
Map<String, User> map = new HashMap<>((int)(expectedSize / 0.75) + 1);

// 分布式锁：使用 Redisson
RLock lock = redissonClient.getLock(lockKey);
if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
    try { /* 业务逻辑 */ } finally { lock.unlock(); }
}
```

### ❌ 避免做法

```java
// ❌ 裸创建线程
new Thread(() -> doWork()).start();

// ❌ 使用 Executors 工厂
Executors.newFixedThreadPool(10);  // 无界队列，OOM 风险

// ❌ Redis KEYS 命令
redisTemplate.keys("user:*");  // 阻塞 Redis

// ❌ 循环单个 Redis 操作
for (String key : keys) redisTemplate.opsForValue().get(key);
```

## 贡献

欢迎提交 PR 补充更多面试题和最佳实践。
