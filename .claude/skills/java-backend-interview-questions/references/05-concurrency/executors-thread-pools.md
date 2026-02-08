# Java并发库中提供了哪些线程池实现?

## 回答

Java通过`Executors`工厂类提供了多种预定义线程池实现：

```
┌─────────────────────────────────────────────────────────────────┐
│                    Executors 工厂方法                            │
├─────────────────┬───────────────────────────────────────────────┤
│ FixedThreadPool │ 固定大小，任务排队等待                          │
├─────────────────┼───────────────────────────────────────────────┤
│ CachedThreadPool│ 弹性伸缩，空闲60s回收                          │
├─────────────────┼───────────────────────────────────────────────┤
│ SingleThread    │ 单线程，保证顺序执行                            │
├─────────────────┼───────────────────────────────────────────────┤
│ ScheduledPool   │ 定时/周期任务调度                               │
├─────────────────┼───────────────────────────────────────────────┤
│ WorkStealingPool│ 工作窃取，ForkJoin实现 (JDK8+)                  │
└─────────────────┴───────────────────────────────────────────────┘
```

## 各线程池详解

### 1. FixedThreadPool（固定线程池）

```java
public static ExecutorService newFixedThreadPool(int nThreads) {
    return new ThreadPoolExecutor(
        nThreads,                      // corePoolSize
        nThreads,                      // maximumPoolSize（相同）
        0L, TimeUnit.MILLISECONDS,     // 不回收
        new LinkedBlockingQueue<>()    // 无界队列
    );
}
```

**特点**：
- 线程数固定，不会增减
- 无界队列（可能OOM）
- 适合负载稳定的场景

### 2. CachedThreadPool（缓存线程池）

```java
public static ExecutorService newCachedThreadPool() {
    return new ThreadPoolExecutor(
        0,                             // corePoolSize = 0
        Integer.MAX_VALUE,             // maximumPoolSize = 无限
        60L, TimeUnit.SECONDS,         // 空闲60s回收
        new SynchronousQueue<>()       // 直接传递
    );
}
```

**特点**：
- 线程数弹性伸缩（0 ~ Integer.MAX_VALUE）
- 空闲60秒回收
- 适合短期异步任务
- **风险**：可能创建大量线程导致OOM

### 3. SingleThreadExecutor（单线程池）

```java
public static ExecutorService newSingleThreadExecutor() {
    return new FinalizableDelegatedExecutorService(
        new ThreadPoolExecutor(
            1, 1,                          // 固定1个线程
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>()    // 无界队列
        )
    );
}
```

**特点**：
- 保证任务顺序执行
- 线程异常会创建新线程替代
- 适合需要顺序执行的场景

### 4. ScheduledThreadPool（调度线程池）

```java
public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize) {
    return new ScheduledThreadPoolExecutor(corePoolSize);
}

// 使用示例
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

// 延迟执行
scheduler.schedule(() -> System.out.println("延迟5秒"), 5, TimeUnit.SECONDS);

// 周期执行（固定速率）
scheduler.scheduleAtFixedRate(() -> System.out.println("每2秒"), 0, 2, TimeUnit.SECONDS);

// 周期执行（固定延迟）
scheduler.scheduleWithFixedDelay(() -> System.out.println("间隔2秒"), 0, 2, TimeUnit.SECONDS);
```

**特点**：
- 支持延迟执行
- 支持周期执行
- 内部使用DelayedWorkQueue

### 5. WorkStealingPool（工作窃取线程池）

```java
// JDK 8+
public static ExecutorService newWorkStealingPool() {
    return new ForkJoinPool(
        Runtime.getRuntime().availableProcessors(),  // 并行度
        ForkJoinPool.defaultForkJoinWorkerThreadFactory,
        null, true
    );
}
```

**特点**：
- 基于ForkJoinPool实现
- 每个线程有独立队列
- 空闲线程"窃取"其他线程任务
- 适合计算密集型任务

## 对比表格

| 线程池 | 核心线程 | 最大线程 | 队列类型 | 适用场景 |
|--------|----------|----------|----------|----------|
| Fixed | n | n | 无界LinkedBlockingQueue | 负载稳定 |
| Cached | 0 | MAX_VALUE | SynchronousQueue | 短期任务 |
| Single | 1 | 1 | 无界LinkedBlockingQueue | 顺序执行 |
| Scheduled | n | MAX_VALUE | DelayedWorkQueue | 定时任务 |
| WorkStealing | CPU核数 | CPU核数 | 工作窃取队列 | 计算密集 |

## Executors的隐患

```
┌─────────────────────────────────────────────────────────────┐
│              阿里巴巴Java开发手册 规范                        │
├─────────────────────────────────────────────────────────────┤
│ 【强制】线程池不允许使用Executors创建，要通过                  │
│         ThreadPoolExecutor方式明确资源限制                    │
├─────────────────────────────────────────────────────────────┤
│ FixedThreadPool / SingleThread:                             │
│   → 无界队列LinkedBlockingQueue，可能堆积大量请求导致OOM      │
│                                                             │
│ CachedThreadPool / ScheduledThreadPool:                     │
│   → maximumPoolSize=Integer.MAX_VALUE，可能创建大量线程OOM   │
└─────────────────────────────────────────────────────────────┘
```

## 推荐做法

```java
// 手动创建，明确参数
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    10,                                     // 核心线程数
    20,                                     // 最大线程数
    60L, TimeUnit.SECONDS,                  // 空闲时间
    new LinkedBlockingQueue<>(1000),        // 有界队列
    new ThreadFactoryBuilder()
        .setNameFormat("business-pool-%d")
        .build(),
    new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略
);
```

## 面试回答

### 30秒版本
> Java通过Executors提供5种线程池：①FixedThreadPool固定线程数，无界队列；②CachedThreadPool弹性线程0到无限，60秒回收；③SingleThreadExecutor单线程顺序执行；④ScheduledThreadPool定时周期任务；⑤WorkStealingPool工作窃取ForkJoin实现。但阿里规范禁止用Executors，因为无界队列或无限线程可能OOM，推荐手动ThreadPoolExecutor明确参数。

### 1分钟版本
> Java并发库通过Executors工厂类提供5种线程池实现。
> 
> **FixedThreadPool**固定线程数，使用无界LinkedBlockingQueue，适合负载稳定场景。**CachedThreadPool**核心线程0、最大无限，空闲60秒回收，使用SynchronousQueue直接传递，适合短期异步任务。**SingleThreadExecutor**单线程保证顺序执行，线程挂掉会新建替代。**ScheduledThreadPool**支持延迟和周期执行，内部用DelayedWorkQueue。**WorkStealingPool**是JDK8新增的，基于ForkJoinPool实现工作窃取，适合计算密集型。
> 
> 但阿里规范强制禁止用Executors创建，因为Fixed和Single用无界队列可能堆积大量请求OOM，Cached和Scheduled最大线程数Integer.MAX_VALUE可能创建大量线程OOM。推荐手动创建ThreadPoolExecutor，明确核心线程数、最大线程数、有界队列大小和拒绝策略。

## 相关问题
- [[thread-pool-principle]] - 线程池工作原理
- [[thread-pool-size]] - 线程池大小设置
- [[fork-join-pool]] - ForkJoinPool详解
- [[thread-pool-shutdown]] - 线程池关闭方式
