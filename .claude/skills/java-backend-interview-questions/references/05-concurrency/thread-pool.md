# Java 线程池

> Java 后端面试知识点 - 并发编程

---

## 线程池核心参数

```java
public ThreadPoolExecutor(
    int corePoolSize,           // 核心线程数
    int maximumPoolSize,        // 最大线程数
    long keepAliveTime,         // 空闲线程存活时间
    TimeUnit unit,              // 时间单位
    BlockingQueue<Runnable> workQueue,  // 任务队列
    ThreadFactory threadFactory,        // 线程工厂
    RejectedExecutionHandler handler    // 拒绝策略
)
```

### 参数详解

| 参数 | 说明 | 建议值 |
|------|------|-------|
| **corePoolSize** | 核心线程数，即使空闲也不销毁 | CPU 密集：N+1，IO 密集：2N |
| **maximumPoolSize** | 最大线程数 | 核心线程的 2-4 倍 |
| **keepAliveTime** | 非核心线程空闲存活时间 | 60-120 秒 |
| **workQueue** | 任务队列 | LinkedBlockingQueue(有界) |
| **threadFactory** | 线程创建工厂 | 设置线程名便于排查 |
| **handler** | 拒绝策略 | CallerRunsPolicy |

---

## 执行流程

```
提交任务
    ↓
核心线程数未满？ ──是──> 创建核心线程执行
    ↓ 否
队列未满？ ──是──> 加入队列等待
    ↓ 否
最大线程数未满？ ──是──> 创建非核心线程执行
    ↓ 否
执行拒绝策略
```

---

## 动态调整线程池参数

### 核心线程数可以动态修改

```java
@Service
@RequiredArgsConstructor
public class ThreadPoolManager {
    
    private final ThreadPoolExecutor executor;
    
    /**
     * 动态调整核心线程数
     * 注意：如果新值 < 当前运行线程数，多余线程会在空闲后被回收
     */
    public void setCorePoolSize(int corePoolSize) {
        if (corePoolSize <= 0 || corePoolSize > executor.getMaximumPoolSize()) {
            throw new IllegalArgumentException("非法的核心线程数");
        }
        executor.setCorePoolSize(corePoolSize);
        log.info("核心线程数调整为: {}", corePoolSize);
    }
    
    /**
     * 动态调整最大线程数
     */
    public void setMaximumPoolSize(int maximumPoolSize) {
        if (maximumPoolSize < executor.getCorePoolSize()) {
            throw new IllegalArgumentException("最大线程数不能小于核心线程数");
        }
        executor.setMaximumPoolSize(maximumPoolSize);
        log.info("最大线程数调整为: {}", maximumPoolSize);
    }
    
    /**
     * 获取线程池状态
     */
    public ThreadPoolStatus getStatus() {
        return ThreadPoolStatus.builder()
            .corePoolSize(executor.getCorePoolSize())
            .maximumPoolSize(executor.getMaximumPoolSize())
            .activeCount(executor.getActiveCount())
            .poolSize(executor.getPoolSize())
            .queueSize(executor.getQueue().size())
            .completedTaskCount(executor.getCompletedTaskCount())
            .build();
    }
}

@Data
@Builder
public class ThreadPoolStatus {
    private int corePoolSize;
    private int maximumPoolSize;
    private int activeCount;
    private int poolSize;
    private int queueSize;
    private long completedTaskCount;
}
```

### 动态配置实现

```java
@Configuration
public class DynamicThreadPoolConfig {
    
    @Bean
    @RefreshScope  // 配置刷新时重新创建
    public ThreadPoolExecutor dynamicExecutor(
            @Value("${thread-pool.core-size:10}") int coreSize,
            @Value("${thread-pool.max-size:20}") int maxSize,
            @Value("${thread-pool.queue-size:1000}") int queueSize) {
        
        return new ThreadPoolExecutor(
            coreSize,
            maxSize,
            60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(queueSize),
            new ThreadFactoryBuilder()
                .setNameFormat("dynamic-pool-%d")
                .build(),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}

// 或者通过监听配置变更手动调整
@Component
@RefreshScope
public class ThreadPoolRefresher {
    
    @Autowired
    private ThreadPoolExecutor executor;
    
    @Value("${thread-pool.core-size:10}")
    private int coreSize;
    
    @Value("${thread-pool.max-size:20}")
    private int maxSize;
    
    @EventListener
    public void onRefresh(RefreshScopeRefreshedEvent event) {
        executor.setCorePoolSize(coreSize);
        executor.setMaximumPoolSize(maxSize);
        log.info("线程池参数已刷新: core={}, max={}", coreSize, maxSize);
    }
}
```

### 基于 Apollo/Nacos 动态配置

```java
@Component
public class ThreadPoolDynamicConfig {
    
    @Autowired
    private ThreadPoolExecutor executor;
    
    // Apollo 配置监听
    @ApolloConfigChangeListener
    public void onChange(ConfigChangeEvent event) {
        if (event.isChanged("thread-pool.core-size")) {
            int newCore = Integer.parseInt(
                event.getChange("thread-pool.core-size").getNewValue());
            executor.setCorePoolSize(newCore);
            log.info("核心线程数动态调整为: {}", newCore);
        }
        
        if (event.isChanged("thread-pool.max-size")) {
            int newMax = Integer.parseInt(
                event.getChange("thread-pool.max-size").getNewValue());
            executor.setMaximumPoolSize(newMax);
            log.info("最大线程数动态调整为: {}", newMax);
        }
    }
}
```

---

## 线程池监控

```java
@Component
@RequiredArgsConstructor
public class ThreadPoolMonitor {
    
    private final ThreadPoolExecutor executor;
    private final MeterRegistry meterRegistry;
    
    @PostConstruct
    public void registerMetrics() {
        // 注册 Prometheus 指标
        Gauge.builder("thread_pool_core_size", executor, ThreadPoolExecutor::getCorePoolSize)
            .register(meterRegistry);
        
        Gauge.builder("thread_pool_max_size", executor, ThreadPoolExecutor::getMaximumPoolSize)
            .register(meterRegistry);
        
        Gauge.builder("thread_pool_active_count", executor, ThreadPoolExecutor::getActiveCount)
            .register(meterRegistry);
        
        Gauge.builder("thread_pool_queue_size", executor, e -> e.getQueue().size())
            .register(meterRegistry);
        
        Gauge.builder("thread_pool_completed_tasks", executor, ThreadPoolExecutor::getCompletedTaskCount)
            .register(meterRegistry);
    }
    
    @Scheduled(fixedRate = 60000)
    public void logStatus() {
        log.info("线程池状态: core={}, max={}, active={}, queue={}",
            executor.getCorePoolSize(),
            executor.getMaximumPoolSize(),
            executor.getActiveCount(),
            executor.getQueue().size());
    }
}
```

---

## 拒绝策略

| 策略 | 行为 | 适用场景 |
|------|------|---------|
| **AbortPolicy** | 抛出 RejectedExecutionException | 严格控制，需要感知拒绝 |
| **CallerRunsPolicy** | 调用者线程执行 | 不丢弃任务，可降级 |
| **DiscardPolicy** | 静默丢弃 | 允许丢弃的场景 |
| **DiscardOldestPolicy** | 丢弃队列头部任务 | 新任务优先 |

### 自定义拒绝策略

```java
public class CustomRejectedHandler implements RejectedExecutionHandler {
    
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        // 1. 记录日志
        log.warn("任务被拒绝: {}, 队列大小: {}", 
            r.toString(), executor.getQueue().size());
        
        // 2. 发送告警
        alertService.sendAlert("线程池任务被拒绝");
        
        // 3. 尝试加入队列（带超时）
        try {
            boolean success = executor.getQueue().offer(r, 5, TimeUnit.SECONDS);
            if (!success) {
                throw new RejectedExecutionException("任务队列已满");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RejectedExecutionException("等待入队被中断");
        }
    }
}
```

---

## 面试要点

### 核心答案

**问：Java 线程池核心线程数在运行过程中能修改吗？如何修改？**

答：**可以动态修改**。

**修改方法**：
1. `setCorePoolSize(int n)` - 修改核心线程数
2. `setMaximumPoolSize(int n)` - 修改最大线程数

**修改后的行为**：
- **调大**：立即生效，会创建新线程处理队列中的任务
- **调小**：空闲线程会在 keepAliveTime 后被回收
- **注意**：core 不能大于 max

**实际应用**：
```java
// 直接调用 API
executor.setCorePoolSize(20);
executor.setMaximumPoolSize(40);

// 配合配置中心动态调整
@ApolloConfigChangeListener
public void onChange(ConfigChangeEvent event) {
    if (event.isChanged("thread-pool.core-size")) {
        executor.setCorePoolSize(newValue);
    }
}
```

**动态调整的场景**：
- 高峰期增加线程数
- 低峰期减少资源占用
- 根据 CPU 负载自动调节

---

## 编码最佳实践

### ✅ 推荐做法

```java
// 1. 自定义线程池，不使用 Executors
new ThreadPoolExecutor(
    10, 20, 60, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(1000),  // 有界队列
    new ThreadFactoryBuilder().setNameFormat("biz-%d").build(),
    new ThreadPoolExecutor.CallerRunsPolicy()
);

// 2. 设置有意义的线程名
new ThreadFactoryBuilder()
    .setNameFormat("order-process-%d")
    .setUncaughtExceptionHandler((t, e) -> log.error("线程异常", e))
    .build();

// 3. 监控线程池状态
@Scheduled(fixedRate = 60000)
public void monitor() {
    log.info("线程池: active={}, queue={}", 
        executor.getActiveCount(), 
        executor.getQueue().size());
}

// 4. 优雅关闭
@PreDestroy
public void shutdown() {
    executor.shutdown();
    if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
        executor.shutdownNow();
    }
}
```

### ❌ 避免做法

```java
// ❌ 使用 Executors 工厂方法
Executors.newFixedThreadPool(10);     // 无界队列，OOM 风险
Executors.newCachedThreadPool();       // 无限线程，OOM 风险
Executors.newSingleThreadExecutor();   // 无界队列

// ❌ 不设置队列容量
new LinkedBlockingQueue<>();  // 无界，OOM 风险

// ❌ 不设置拒绝策略（默认抛异常）
new ThreadPoolExecutor(...);  // 使用默认 AbortPolicy

// ❌ 不处理异常
executor.execute(() -> {
    throw new RuntimeException();  // 异常被吞没
});
```
