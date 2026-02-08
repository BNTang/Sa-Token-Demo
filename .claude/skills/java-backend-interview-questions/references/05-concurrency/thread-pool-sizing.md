# 如何合理设置线程池线程数

> 分类: Java 并发 | 难度: ⭐⭐⭐⭐ | 频率: 高频

---

## 一、理论公式

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          线程数设置理论                                           │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  1. CPU 密集型任务                                                               │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │  特点: 计算密集，CPU 使用率高                                               │ │
│  │  例如: 加解密、压缩解压、排序、复杂计算                                     │ │
│  │                                                                             │ │
│  │  线程数 = CPU 核心数 + 1                                                    │ │
│  │                                                                             │ │
│  │  为什么 +1: 防止某线程偶尔阻塞时 CPU 空闲                                   │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  2. IO 密集型任务                                                                │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │  特点: 大量等待 IO，CPU 空闲                                                │ │
│  │  例如: 网络请求、数据库操作、文件读写                                       │ │
│  │                                                                             │ │
│  │  线程数 = CPU 核心数 × 2                                                    │ │
│  │  或                                                                         │ │
│  │  线程数 = CPU 核心数 × (1 + 等待时间/计算时间)                              │ │
│  │                                                                             │ │
│  │  例: 8核，等待时间=90ms，计算时间=10ms                                      │ │
│  │  线程数 = 8 × (1 + 90/10) = 80                                             │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 二、实际调优方法

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                    实际调优方法 (推荐)                                            │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  理论公式只是起点，实际需要根据业务压测调优                                       │
│                                                                                  │
│  步骤:                                                                           │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │  1. 初始值: 按理论公式设置初始线程数                                        │ │
│  │                                                                             │ │
│  │  2. 压测观察:                                                               │ │
│  │     • CPU 使用率 (目标: 70%-80%)                                            │ │
│  │     • 响应时间 (RT)                                                         │ │
│  │     • 吞吐量 (QPS)                                                          │ │
│  │     • 线程池队列大小                                                        │ │
│  │                                                                             │ │
│  │  3. 调整策略:                                                               │ │
│  │     • CPU 使用率低 + 队列堆积 → 增加线程数                                  │ │
│  │     • CPU 使用率高 + 响应慢 → 减少线程数或优化代码                          │ │
│  │     • CPU 使用率合适 → 当前配置可行                                         │ │
│  │                                                                             │ │
│  │  4. 反复压测，找到最优配置                                                  │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 三、美团的动态线程池实践

```java
/**
 * 动态线程池 - 支持运行时调整参数
 */
@Configuration
public class DynamicThreadPoolConfig {
    
    @Bean
    public ThreadPoolExecutor orderExecutor() {
        return new ThreadPoolExecutor(
            10,   // 核心线程数，可动态调整
            20,   // 最大线程数，可动态调整
            60, TimeUnit.SECONDS,
            new ResizableLinkedBlockingQueue<>(100),  // 可调整大小的队列
            new ThreadFactoryBuilder().setNameFormat("order-pool-%d").build(),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
    
    /**
     * 动态调整线程池参数
     */
    @Scheduled(fixedRate = 60000)  // 每分钟检查配置
    public void refreshConfig() {
        // 从配置中心获取最新配置
        int coreSize = configCenter.getInt("order.pool.core.size");
        int maxSize = configCenter.getInt("order.pool.max.size");
        
        orderExecutor.setCorePoolSize(coreSize);
        orderExecutor.setMaximumPoolSize(maxSize);
    }
}

/**
 * 线程池监控
 */
@Scheduled(fixedRate = 10000)
public void monitor() {
    ThreadPoolExecutor executor = orderExecutor;
    
    log.info("线程池状态: " +
        "核心线程数=" + executor.getCorePoolSize() +
        ", 最大线程数=" + executor.getMaximumPoolSize() +
        ", 当前线程数=" + executor.getPoolSize() +
        ", 活跃线程数=" + executor.getActiveCount() +
        ", 队列大小=" + executor.getQueue().size() +
        ", 已完成任务=" + executor.getCompletedTaskCount()
    );
    
    // 告警: 队列使用率 > 80%
    if (executor.getQueue().size() > 80) {
        alertService.sendAlert("线程池队列即将满");
    }
}
```

---

## 四、不同场景的配置参考

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                    不同场景线程池配置参考                                         │
├───────────────────┬──────────────────────────────────────────────────────────────┤
│      场景          │                        配置建议                              │
├───────────────────┼──────────────────────────────────────────────────────────────┤
│  HTTP 接口调用     │  IO 密集型，核心线程数 = CPU × 2 或更多                       │
│                   │  队列用有界队列，避免 OOM                                     │
├───────────────────┼──────────────────────────────────────────────────────────────┤
│  数据库操作        │  IO 密集型，但要考虑数据库连接池大小                          │
│                   │  线程数 ≤ 数据库连接池大小                                    │
├───────────────────┼──────────────────────────────────────────────────────────────┤
│  图片/视频处理     │  CPU 密集型，线程数 = CPU + 1                                 │
│                   │  过多线程会增加上下文切换开销                                 │
├───────────────────┼──────────────────────────────────────────────────────────────┤
│  定时任务          │  根据任务类型决定                                            │
│                   │  注意任务执行时间，避免任务堆积                               │
├───────────────────┼──────────────────────────────────────────────────────────────┤
│  消息消费          │  IO 密集型，但要考虑消息顺序性                                │
│                   │  如需顺序消费，同一 key 用单线程                              │
└───────────────────┴──────────────────────────────────────────────────────────────┘
```

---

## 五、代码示例

```java
/**
 * 根据任务类型创建线程池
 */
public class ThreadPoolFactory {
    
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    
    /**
     * CPU 密集型线程池
     */
    public static ThreadPoolExecutor createCpuIntensivePool(String name) {
        return new ThreadPoolExecutor(
            CPU_COUNT + 1,        // 核心线程 = CPU + 1
            CPU_COUNT + 1,        // 最大线程 = 核心线程
            0, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            new ThreadFactoryBuilder().setNameFormat(name + "-%d").build(),
            new ThreadPoolExecutor.AbortPolicy()
        );
    }
    
    /**
     * IO 密集型线程池
     */
    public static ThreadPoolExecutor createIoIntensivePool(String name) {
        return new ThreadPoolExecutor(
            CPU_COUNT * 2,        // 核心线程 = CPU × 2
            CPU_COUNT * 4,        // 最大线程 = CPU × 4
            60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(200),
            new ThreadFactoryBuilder().setNameFormat(name + "-%d").build(),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
    
    /**
     * 混合型线程池 (需要压测调优)
     */
    public static ThreadPoolExecutor createMixedPool(String name, 
            int waitTime, int computeTime) {
        // 线程数 = CPU × (1 + 等待时间/计算时间)
        int threadCount = (int) (CPU_COUNT * (1 + (double) waitTime / computeTime));
        
        return new ThreadPoolExecutor(
            threadCount,
            threadCount * 2,
            60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(500),
            new ThreadFactoryBuilder().setNameFormat(name + "-%d").build(),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
```

---

## 六、面试回答

### 30秒版本

> **CPU 密集型**：线程数 = CPU 核心数 + 1  
> **IO 密集型**：线程数 = CPU 核心数 × 2
>
> 公式只是起点，实际需要**压测调优**：
> - 观察 CPU 使用率（目标 70%-80%）
> - 观察队列堆积情况
> - 观察吞吐量和响应时间
>
> 生产环境推荐**动态线程池**，支持运行时调整参数。

### 1分钟版本

> **理论公式**：
> - CPU 密集型：线程数 = CPU + 1（避免偶发阻塞导致 CPU 空闲）
> - IO 密集型：线程数 = CPU × 2 或 CPU × (1 + 等待时间/计算时间)
>
> **实际调优方法**：
> 公式只是初始值，需要压测调优：
> 1. 按公式设置初始线程数
> 2. 压测观察 CPU 使用率（目标 70%-80%）、响应时间、吞吐量、队列大小
> 3. CPU 低 + 队列堆积 → 增加线程
>    CPU 高 + 响应慢 → 减少线程或优化代码
> 4. 反复压测找到最优配置
>
> **注意事项**：
> - 数据库操作：线程数不超过连接池大小
> - 要用有界队列，避免任务无限堆积 OOM
> - 自定义线程名称，便于排查问题
>
> **动态线程池**：
> 支持运行时动态调整核心/最大线程数和队列大小，配合监控告警，是生产环境最佳实践。
