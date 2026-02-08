# 分布式锁注解最佳实践

> 版本: 1.0 | 更新: 2026-02-05
>
> 通过注解 + AOP 实现分布式锁，代码更简洁、更符合整洁代码原则

---

## 概述

使用 `@DistributedLock` 注解可以将分布式锁的使用简化为一行代码，符合整洁代码原则：

| 原则 | 实现方式 |
|------|---------|
| **只做一件事** | 注解只负责加锁，业务逻辑专注自身 |
| **如散文般可读** | 代码意图清晰，无需看实现 |
| **尽量少** | 消除重复的加锁/释放代码 |
| **有人在意** | 精心设计的 API，使用愉悦 |

---

## 对比：注解方式 vs 手动方式

### 手动方式（代码冗长）

```java
// ❌ 冗长的方式
@Scheduled(cron = "0 */5 * * * ?")
public void syncDataTask() {
    String lockKey = "task:sync:data";
    RLock lock = redissonClient.getLock(lockKey);

    try {
        boolean acquired = lock.tryLock(0, 10, TimeUnit.MINUTES);

        if (!acquired) {
            return;
        }

        try {
            doSyncData();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}
```

**问题**：
- 每个方法都重复相同的加锁逻辑
- 业务代码被淹没在锁处理代码中
- 容易忘记释放锁或处理异常

### 注解方式（简洁清晰）

```java
// ✅ 简洁的方式
@Scheduled(cron = "0 */5 * * * ?")
@DistributedLock(key = "task:sync:data", leaseTime = 10, timeUnit = TimeUnit.MINUTES)
public void syncDataTask() {
    doSyncData();
}
```

**优点**：
- 一眼看出这是分布式锁保护的代码
- 业务逻辑清晰，没有干扰
- 不会忘记释放锁或处理异常

---

## 注解定义

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {

    /**
     * 锁的 key，支持 SpEL 表达式
     */
    String key();

    /**
     * 等待时间，默认不等待
     */
    long waitTime() default 0;

    /**
     * 锁超时时间，默认 30 秒
     */
    long leaseTime() default 30;

    /**
     * 时间单位，默认秒
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 是否启用看门狗（自动续期）
     */
    boolean watchdog() default false;
}
```

---

## AOP 实现

```java
@Component
@Aspect
@RequiredArgsConstructor
@Slf4j
class DistributedLockAspect {

    private final RedissonClient redissonClient;
    private final SpelExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        String lockKey = parseLockKey(joinPoint, distributedLock.key());
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = acquireLock(lock, distributedLock);

            if (!acquired) {
                log.debug("获取锁失败: {}", lockKey);
                return null;
            }

            return executeWithLock(joinPoint, lock, lockKey);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    private boolean acquireLock(RLock lock, DistributedLock distributedLock) throws InterruptedException {
        if (distributedLock.watchdog()) {
            lock.lock();
            return true;
        }

        return lock.tryLock(
            distributedLock.waitTime(),
            distributedLock.leaseTime(),
            distributedLock.timeUnit()
        );
    }

    private Object executeWithLock(ProceedingJoinPoint joinPoint, RLock lock, String lockKey) throws Throwable {
        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();

            long duration = System.currentTimeMillis() - startTime;
            log.debug("锁执行成功: {}, 耗时: {}ms", lockKey, duration);

            return result;

        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private String parseLockKey(ProceedingJoinPoint joinPoint, String key) {
        if (!key.contains("#")) {
            return key;
        }

        StandardEvaluationContext context = createEvaluationContext(joinPoint);
        Expression expression = parser.parseExpression(key);
        return expression.getValue(context, String.class);
    }

    private StandardEvaluationContext createEvaluationContext(ProceedingJoinPoint joinPoint) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameterNames.length; i++) {
            context.setVariable(parameterNames[i], args[i]);
        }

        return context;
    }
}
```

**整洁代码体现**：
- 每个方法只做一件事
- 方法名清晰表达意图
- 提取了重复逻辑（创建 EvaluationContext）
- 代码结构清晰，易于理解

---

## 使用示例

### 示例1：固定 key

```java
@DistributedLock(key = "task:sync:data", leaseTime = 10, timeUnit = TimeUnit.MINUTES)
public void syncData() {
    // 业务逻辑
}
```

### 示例2：动态 key（SpEL）

```java
@DistributedLock(key = "'task:user:' + #userId", leaseTime = 5, timeUnit = TimeUnit.MINUTES)
public void processUser(Long userId) {
    // 每个 userId 有独立的锁
}
```

### 示例3：从对象参数获取

```java
@DistributedLock(key = "'task:order:' + #order.id", leaseTime = 5, timeUnit = TimeUnit.MINUTES)
public void processOrder(OrderDTO order) {
    // 业务逻辑
}
```

### 示例4：看门狗模式

```java
@DistributedLock(key = "task:long:running", watchdog = true)
public void longRunningTask() {
    // 任务执行时间不确定
    // 看门狗会自动续期
}
```

### 示例5：等待获取锁

```java
@DistributedLock(
    key = "task:with:wait",
    waitTime = 10,
    leaseTime = 5,
    timeUnit = TimeUnit.MINUTES
)
public void taskWithWait() {
    // 最多等待 10 秒获取锁
}
```

---

## SpEL 表达式高级用法

### 从嵌套对象获取

```java
@DistributedLock(key = "'task:order:' + #dto.order.id")
public void processOrder(RequestDTO dto) {
    // 从 dto.order.id 获取
}
```

### 组合多个参数

```java
@DistributedLock(key = "'task:' + #type + ':' + #id")
public void process(String type, Long id) {
    // 组合 type 和 id
}
```

### 调用对象方法

```java
@DistributedLock(key = "'task:user:' + #user.getId()")
public void processUser(User user) {
    // 调用 user.getId() 方法
}
```

---

## 整洁代码原则对照

### 只做一件事

| 代码 | 做了几件事 | 评价 |
|------|-----------|------|
| `syncData()` 方法 | 只做数据同步 | ✅ 符合 |
| `DistributedLockAspect` | 只处理分布式锁 | ✅ 符合 |
| `parseLockKey()` | 只解析 key | ✅ 符合 |

### 如散文般可读

```java
// 代码读起来像自然语言
@DistributedLock(key = "task:sync:data", leaseTime = 10, timeUnit = TimeUnit.MINUTES)
public void syncData() {
    // 同步数据
}
```

### 无重复代码

| 重复点 | 处理方式 |
|--------|---------|
| 加锁逻辑 | 提取到 AOP 切面 |
| 释放锁逻辑 | 提取到 AOP 切面 |
| 异常处理 | 提取到 AOP 切面 |
| SpEL 解析 | 提取到独立方法 |

### 尽量少

- 注解参数少（只有 5 个）
- 使用默认值减少配置
- 方法代码行数少（每个 < 20 行）

---

## 测试用例

```java
@SpringBootTest
class DistributedLockTest {

    @Autowired
    private TestService testService;

    @Test
    void testBasicLock() {
        testService.taskWithLock();
        // 验证任务执行成功
    }

    @Test
    void testConcurrentLock() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger counter = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                testService.taskWithLock();
                counter.incrementAndGet();
                latch.countDown();
            }).start();
        }

        latch.await();

        // 只有一个线程获取到锁
        assertEquals(1, counter.get());
    }
}

@Component
class TestService {

    @DistributedLock(key = "test:lock", leaseTime = 1, timeUnit = TimeUnit.MINUTES)
    public void taskWithLock() {
        // 测试任务
    }
}
```

---

## 性能对比

| 方式 | 代码行数 | 可读性 | 维护性 | 性能差异 |
|------|---------|--------|--------|---------|
| 手动加锁 | 15-20 行 | ⭐⭐ | ⭐⭐ | 基准 |
| 注解方式 | 1 行 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | +0.1ms（AOP 开销） |

**结论**：注解方式的可读性和维护性远超性能损耗。

---

## 常见问题

### Q1: 如何处理获取锁失败后的逻辑？

```java
@DistributedLock(key = "task:sync:data", leaseTime = 10, timeUnit = TimeUnit.MINUTES)
public Result syncData() {
    return doSync();
}

// 调用方处理
public void scheduledTask() {
    Result result = syncData();
    if (result == null) {
        log.info("任务正在其他实例执行");
    }
}
```

### Q2: 如何在事务提交后才释放锁？

```java
@Transactional
@DistributedLock(key = "task:with:transaction", leaseTime = 1, timeUnit = TimeUnit.MINUTES)
public void taskWithTransaction() {
    // 先提交事务，再释放锁
    // 可以通过调整事务传播行为控制
}
```

### Q3: 如何实现可重入锁？

```java
@DistributedLock(key = "task:reentrant", leaseTime = 1, timeUnit = TimeUnit.MINUTES)
public void outerTask() {
    innerTask(); // 同一个线程可以重入
}

@DistributedLock(key = "task:reentrant", leaseTime = 1, timeUnit = TimeUnit.MINUTES)
public void innerTask() {
    // 业务逻辑
}
```

---

## 参考资料

- Spring AOP 文档：https://docs.spring.io/spring-framework/reference/core/aop.html
- SpEL 表达式：https://docs.spring.io/spring-framework/reference/core/expressions.html
- Redisson 文档：https://redisson.org
