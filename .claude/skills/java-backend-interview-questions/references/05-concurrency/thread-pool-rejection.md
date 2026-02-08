# Java 线程池拒绝策略

> 分类: Java 并发 | 难度: ⭐⭐⭐ | 频率: 高频

---

## 一、拒绝策略触发时机

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          拒绝策略触发时机                                         │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  当以下两个条件同时满足时，触发拒绝策略:                                         │
│                                                                                  │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │  1. 任务队列已满                                                            │ │
│  │  2. 线程数已达到 maximumPoolSize                                            │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  执行流程:                                                                       │
│                                                                                  │
│   提交任务 → 核心线程满? → 入队 → 队列满? → 创建非核心线程 → 达最大? → 拒绝     │
│                    ↓No          ↓No              ↓No                             │
│              创建核心线程    等待执行       创建线程执行                          │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 二、四种内置拒绝策略

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          四种内置拒绝策略                                         │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  1. AbortPolicy (默认)                                                           │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │  直接抛出 RejectedExecutionException                                        │ │
│  │  适用场景: 需要感知任务提交失败，由调用方处理                               │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  2. CallerRunsPolicy                                                             │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │  由提交任务的线程(调用者)执行该任务                                         │ │
│  │  适用场景: 不丢弃任务，平滑降级，自动限流调用方                             │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  3. DiscardPolicy                                                                │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │  静默丢弃新提交的任务，不抛异常                                             │ │
│  │  适用场景: 无关紧要的任务，可以丢弃                                         │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  4. DiscardOldestPolicy                                                          │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │  丢弃队列中最老的任务，然后尝试提交新任务                                   │ │
│  │  适用场景: 新任务比老任务更重要                                             │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 三、代码示例

```java
/**
 * 四种内置拒绝策略示例
 */
public class RejectionPolicyExample {
    
    public static void main(String[] args) {
        // 1. AbortPolicy - 默认策略，抛异常
        ThreadPoolExecutor executor1 = new ThreadPoolExecutor(
            1, 1, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(1),
            new ThreadPoolExecutor.AbortPolicy()  // 抛 RejectedExecutionException
        );
        
        // 2. CallerRunsPolicy - 调用者执行
        ThreadPoolExecutor executor2 = new ThreadPoolExecutor(
            1, 1, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(1),
            new ThreadPoolExecutor.CallerRunsPolicy()  // main 线程执行
        );
        
        // 3. DiscardPolicy - 静默丢弃
        ThreadPoolExecutor executor3 = new ThreadPoolExecutor(
            1, 1, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(1),
            new ThreadPoolExecutor.DiscardPolicy()  // 直接丢弃
        );
        
        // 4. DiscardOldestPolicy - 丢弃最老任务
        ThreadPoolExecutor executor4 = new ThreadPoolExecutor(
            1, 1, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(1),
            new ThreadPoolExecutor.DiscardOldestPolicy()  // 丢弃队头任务
        );
    }
}
```

---

## 四、自定义拒绝策略

```java
/**
 * 自定义拒绝策略 - 记录日志 + 持久化
 */
public class CustomRejectedHandler implements RejectedExecutionHandler {
    
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        // 1. 记录日志
        log.error("任务被拒绝: {}, 线程池状态: 队列大小={}, 活跃线程={}",
            r.toString(),
            executor.getQueue().size(),
            executor.getActiveCount()
        );
        
        // 2. 持久化到数据库，后续重试
        if (r instanceof TaskWrapper) {
            TaskWrapper task = (TaskWrapper) r;
            taskRepository.save(task.toEntity());
        }
        
        // 3. 发送告警
        alertService.sendAlert("线程池任务被拒绝");
        
        // 4. 也可以选择抛异常
        // throw new RejectedExecutionException("任务被拒绝");
    }
}

/**
 * 使用自定义拒绝策略
 */
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    5, 10, 60, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(100),
    new CustomRejectedHandler()
);

/**
 * 另一个常见实现: 阻塞提交
 */
public class BlockingRejectedHandler implements RejectedExecutionHandler {
    
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        try {
            // 阻塞等待队列有空位
            executor.getQueue().put(r);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RejectedExecutionException("任务入队被中断");
        }
    }
}
```

---

## 五、策略选择指南

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          策略选择指南                                             │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌───────────────────┬──────────────────────────────────────────────────────┐   │
│  │  策略              │  适用场景                                            │   │
│  ├───────────────────┼──────────────────────────────────────────────────────┤   │
│  │  AbortPolicy      │  需要感知失败，由调用方决定重试策略                   │   │
│  │  (默认)           │  如: 支付、下单等关键业务                             │   │
│  ├───────────────────┼──────────────────────────────────────────────────────┤   │
│  │  CallerRunsPolicy │  不能丢弃任务，可接受延迟                             │   │
│  │                   │  如: 日志写入、报表生成                               │   │
│  │                   │  注意: 会阻塞调用线程                                 │   │
│  ├───────────────────┼──────────────────────────────────────────────────────┤   │
│  │  DiscardPolicy    │  可以丢弃的非关键任务                                 │   │
│  │                   │  如: 统计打点、监控上报                               │   │
│  ├───────────────────┼──────────────────────────────────────────────────────┤   │
│  │  DiscardOldest    │  新任务比老任务重要                                   │   │
│  │  Policy           │  如: 实时消息推送                                     │   │
│  ├───────────────────┼──────────────────────────────────────────────────────┤   │
│  │  自定义策略        │  需要记录日志、持久化重试、告警等                     │   │
│  │                   │  生产环境推荐                                         │   │
│  └───────────────────┴──────────────────────────────────────────────────────┘   │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 六、面试回答

### 30秒版本

> 当任务队列满且线程数达到最大时，触发拒绝策略。
>
> **四种内置策略**：
> 1. AbortPolicy（默认）：抛 RejectedExecutionException
> 2. CallerRunsPolicy：调用者线程执行
> 3. DiscardPolicy：静默丢弃
> 4. DiscardOldestPolicy：丢弃队列最老任务
>
> 生产环境推荐**自定义策略**：记录日志 + 持久化 + 告警。

### 1分钟版本

> **触发时机**：
> 任务队列已满，且线程数已达到 maximumPoolSize。
>
> **四种内置策略**：
>
> 1. **AbortPolicy**（默认）
>    - 抛出 RejectedExecutionException
>    - 适用：关键业务，需要感知失败
>
> 2. **CallerRunsPolicy**
>    - 由提交任务的线程执行该任务
>    - 适用：不能丢弃的任务（会阻塞调用线程）
>    - 自动起到限流效果
>
> 3. **DiscardPolicy**
>    - 静默丢弃，不抛异常
>    - 适用：非关键任务（统计打点等）
>
> 4. **DiscardOldestPolicy**
>    - 丢弃队列头部最老的任务
>    - 适用：新任务比老任务重要
>
> **自定义策略**（推荐）：
> 实现 RejectedExecutionHandler 接口，加入：
> - 日志记录
> - 持久化到数据库便于重试
> - 发送告警
> - 监控指标上报
