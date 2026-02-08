# 如何合理设置 Java 线程池的线程数？

> 根据任务类型设置：CPU 密集型用 N+1，IO 密集型用 N×(1+IO等待/CPU计算)

## 30秒速答

线程数设置公式：
- **CPU 密集型**: `N + 1`（N = CPU 核心数）
- **IO 密集型**: `N × (1 + 等待时间/计算时间)` 或简化为 `2N`
- **混合型**: 拆分为 CPU 密集和 IO 密集两个线程池

关键原则：**压测调优**，公式只是起点。

## 一分钟详解

### 任务类型分析

```
┌────────────────────────────────────────────────────────────┐
│  CPU 密集型任务                                            │
│  • 计算、加密解密、压缩、排序                              │
│  • 特点：CPU 利用率高，几乎无等待                          │
│  • 线程数：N + 1                                           │
│  • 多一个线程：防止线程偶尔的缺页中断或其他阻塞            │
├────────────────────────────────────────────────────────────┤
│  IO 密集型任务                                             │
│  • 数据库操作、网络请求、文件读写                          │
│  • 特点：等待 IO 时间长，CPU 空闲                          │
│  • 线程数：N × (1 + 等待时间/计算时间)                     │
│  • 简化：2N 或 N × 2                                       │
└────────────────────────────────────────────────────────────┘
```

### 公式推导

```
目标：CPU 利用率最大化

CPU 密集型：
┌──────────────────────────────────────────────────────────┐
│  每个线程几乎100%使用CPU                                  │
│  线程数 = CPU核心数 + 1                                   │
│  多1个是为了当某个线程因页缺失暂停时，替补上              │
└──────────────────────────────────────────────────────────┘

IO 密集型：
┌──────────────────────────────────────────────────────────┐
│  线程时间 = CPU计算时间 + IO等待时间                      │
│                                                          │
│  假设：                                                   │
│  - CPU计算时间 = 1秒                                      │
│  - IO等待时间 = 9秒                                       │
│  - 总时间 = 10秒                                          │
│                                                          │
│  单线程 CPU利用率 = 1/10 = 10%                           │
│                                                          │
│  要使 CPU 100% 利用，需要 10 个线程                       │
│  公式：N × (1 + 9/1) = N × 10                            │
│  如果 N=1，则需要 10 个线程                              │
└──────────────────────────────────────────────────────────┘

通用公式（美团技术）：
线程数 = N × U × (1 + W/C)
- N = CPU 核心数
- U = 目标 CPU 利用率 (0~1)
- W = 等待时间
- C = 计算时间
```

### 实际配置示例

```java
public class ThreadPoolConfig {
    
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    
    // CPU 密集型线程池
    public static ExecutorService cpuIntensivePool() {
        return new ThreadPoolExecutor(
            CPU_COUNT + 1,              // corePoolSize
            CPU_COUNT + 1,              // maxPoolSize
            0L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            new NamedThreadFactory("cpu-pool"),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
    
    // IO 密集型线程池
    public static ExecutorService ioIntensivePool() {
        return new ThreadPoolExecutor(
            CPU_COUNT * 2,              // corePoolSize
            CPU_COUNT * 2,              // maxPoolSize
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(200),
            new NamedThreadFactory("io-pool"),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
    
    // 根据实际 IO 等待时间计算
    public static ExecutorService customPool(double waitTime, double computeTime) {
        int threads = (int) (CPU_COUNT * (1 + waitTime / computeTime));
        return new ThreadPoolExecutor(
            threads, threads,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(500),
            new NamedThreadFactory("custom-pool"),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
```

### 压测调优方法

```java
// 1. 监控指标
// - CPU 利用率
// - 线程池活跃线程数
// - 队列大小
// - 任务响应时间
// - 吞吐量

// 2. 动态调整
ThreadPoolExecutor executor = ...;

// 运行时调整参数
executor.setCorePoolSize(newCoreSize);
executor.setMaximumPoolSize(newMaxSize);

// 3. 监控代码
public class ThreadPoolMonitor {
    private final ThreadPoolExecutor executor;
    
    public void monitor() {
        System.out.println("活跃线程数: " + executor.getActiveCount());
        System.out.println("核心线程数: " + executor.getCorePoolSize());
        System.out.println("最大线程数: " + executor.getMaximumPoolSize());
        System.out.println("队列大小: " + executor.getQueue().size());
        System.out.println("完成任务数: " + executor.getCompletedTaskCount());
    }
}

// 4. 使用 Arthas 监控
// thread -n 5  查看最忙的5个线程
// thread --state WAITING  查看等待状态的线程
```

### 常见场景配置

| 场景 | 核心线程 | 最大线程 | 队列 | 说明 |
|------|---------|---------|------|------|
| Web 请求 | 2N | 2N | 200 | IO 密集 |
| 批量计算 | N+1 | N+1 | 100 | CPU 密集 |
| 定时任务 | 5 | 10 | 1000 | 视任务类型 |
| 异步日志 | 1 | 2 | 10000 | 单线程顺序写 |
| 消息消费 | N | 2N | 500 | 取决于下游 |

### 注意事项

```
┌────────────────────────────────────────────────────────────┐
│  ⚠️ 公式只是起点，实际需要压测调优                          │
│                                                            │
│  考虑因素：                                                │
│  1. 系统整体资源（不只有你一个线程池）                      │
│  2. 下游服务承受能力（数据库连接数、第三方限流）            │
│  3. 任务执行时间的波动（平均 vs 峰值）                      │
│  4. 内存限制（每个线程约 1MB 栈空间）                       │
│                                                            │
│  实践建议：                                                │
│  1. 根据公式设置初始值                                      │
│  2. 压测观察 CPU、内存、响应时间                           │
│  3. 逐步调整，找到最优值                                    │
│  4. 预留 buffer，不要把资源用满                            │
└────────────────────────────────────────────────────────────┘
```

## 关键记忆点

```
┌─────────────────────────────────────────────────────┐
│  线程数设置速记：                                    │
│                                                     │
│  ┌──────────────┬────────────────────────────────┐ │
│  │ CPU 密集型   │ N + 1                          │ │
│  │ IO 密集型    │ 2N 或 N × (1 + W/C)            │ │
│  │ 混合型       │ 拆分为两个线程池               │ │
│  └──────────────┴────────────────────────────────┘ │
│                                                     │
│  N = Runtime.getRuntime().availableProcessors()    │
│                                                     │
│  核心原则：                                         │
│  公式是起点，压测是终点                            │
│  CPU利用率、响应时间、吞吐量综合考量               │
└─────────────────────────────────────────────────────┘
```

## 面试追问

**Q: 为什么 CPU 密集型不是正好 N 个线程？**

A: 多 1 个线程是为了**防止线程偶尔的阻塞**（如页缺失中断、GC STW 等），确保 CPU 始终有任务执行，最大化 CPU 利用率。但也不能太多，否则频繁上下文切换反而降低性能。
