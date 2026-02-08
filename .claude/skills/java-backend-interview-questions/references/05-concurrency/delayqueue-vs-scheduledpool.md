# Java中DelayQueue和ScheduledThreadPool有什么区别?

## 回答

两者都可以实现延迟任务，但定位不同：

```
┌─────────────────────────────────────────────────────────────┐
│           DelayQueue vs ScheduledThreadPool                 │
├─────────────────┬───────────────────────────────────────────┤
│                 │ DelayQueue       │ ScheduledThreadPool    │
├─────────────────┼──────────────────┼────────────────────────┤
│ 本质            │ 延迟阻塞队列     │ 定时任务线程池          │
├─────────────────┼──────────────────┼────────────────────────┤
│ 任务执行        │ 需自己消费       │ 自动执行               │
├─────────────────┼──────────────────┼────────────────────────┤
│ 周期任务        │ 不支持           │ 支持                   │
├─────────────────┼──────────────────┼────────────────────────┤
│ 灵活性          │ 高（纯数据结构）  │ 低（封装好的线程池）    │
├─────────────────┼──────────────────┼────────────────────────┤
│ 使用场景        │ 延迟队列/缓存    │ 定时调度任务           │
└─────────────────┴──────────────────┴────────────────────────┘
```

## DelayQueue详解

### 原理

```
┌─────────────────────────────────────────────────────────────┐
│                     DelayQueue                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  底层：PriorityQueue（按延迟时间排序的小顶堆）                │
│                                                             │
│  ┌──────┐  ┌──────┐  ┌──────┐  ┌──────┐                    │
│  │ 10ms │  │ 30ms │  │ 50ms │  │100ms │  按到期时间排序     │
│  └──────┘  └──────┘  └──────┘  └──────┘                    │
│      ↑                                                      │
│      │                                                      │
│   take() 只能取出已到期的元素，否则阻塞                       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 代码示例

```java
// 1. 定义延迟元素
public class DelayedTask implements Delayed {
    private final String name;
    private final long expireTime;  // 到期时间（绝对时间）
    
    public DelayedTask(String name, long delayMs) {
        this.name = name;
        this.expireTime = System.currentTimeMillis() + delayMs;
    }
    
    @Override
    public long getDelay(TimeUnit unit) {
        // 返回剩余延迟时间
        long remaining = expireTime - System.currentTimeMillis();
        return unit.convert(remaining, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public int compareTo(Delayed other) {
        // 按到期时间排序
        return Long.compare(this.expireTime, 
                           ((DelayedTask) other).expireTime);
    }
    
    public String getName() { return name; }
}

// 2. 使用DelayQueue
public class DelayQueueDemo {
    public static void main(String[] args) throws InterruptedException {
        DelayQueue<DelayedTask> queue = new DelayQueue<>();
        
        // 添加延迟任务
        queue.put(new DelayedTask("任务A", 5000));  // 5秒后
        queue.put(new DelayedTask("任务B", 2000));  // 2秒后
        queue.put(new DelayedTask("任务C", 1000));  // 1秒后
        
        // 消费者（需要自己开线程消费）
        while (!queue.isEmpty()) {
            DelayedTask task = queue.take();  // 阻塞直到有到期元素
            System.out.println(LocalTime.now() + " 执行: " + task.getName());
        }
    }
}
// 输出：
// 10:00:01 执行: 任务C
// 10:00:02 执行: 任务B
// 10:00:05 执行: 任务A
```

### 应用场景

```java
// 1. 缓存过期
public class DelayedCache<K, V> {
    private final Map<K, V> cache = new ConcurrentHashMap<>();
    private final DelayQueue<DelayedKey<K>> delayQueue = new DelayQueue<>();
    
    public void put(K key, V value, long ttlMs) {
        cache.put(key, value);
        delayQueue.put(new DelayedKey<>(key, ttlMs));
    }
    
    // 后台线程清理过期key
    public void startCleaner() {
        new Thread(() -> {
            while (true) {
                try {
                    DelayedKey<K> expired = delayQueue.take();
                    cache.remove(expired.getKey());
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }
}

// 2. 订单超时取消
public class OrderTimeoutService {
    private final DelayQueue<OrderTimeout> timeoutQueue = new DelayQueue<>();
    
    public void createOrder(Order order) {
        // 30分钟后超时
        timeoutQueue.put(new OrderTimeout(order.getId(), 30 * 60 * 1000));
    }
    
    // 消费者处理超时订单
    public void processTimeouts() {
        while (true) {
            OrderTimeout timeout = timeoutQueue.take();
            cancelOrder(timeout.getOrderId());
        }
    }
}
```

## ScheduledThreadPool详解

### 代码示例

```java
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

// 1. 延迟执行（一次性）
scheduler.schedule(() -> {
    System.out.println("延迟5秒执行");
}, 5, TimeUnit.SECONDS);

// 2. 固定速率执行（不考虑任务执行时间）
// 每2秒开始一次，即使上次没完成
scheduler.scheduleAtFixedRate(() -> {
    System.out.println("固定速率执行");
}, 0, 2, TimeUnit.SECONDS);

// 3. 固定延迟执行（上次结束到下次开始的间隔）
scheduler.scheduleWithFixedDelay(() -> {
    System.out.println("固定延迟执行");
}, 0, 2, TimeUnit.SECONDS);
```

### 两种周期策略对比

```
scheduleAtFixedRate (period = 2s, 任务执行1.5s):
    |--执行--|     |--执行--|     |--执行--|
    0        1.5   2        3.5   4        5.5
    ↑              ↑              ↑
    相邻开始时间固定间隔2s

scheduleWithFixedDelay (delay = 2s, 任务执行1.5s):
    |--执行--|           |--执行--|           |--执行--|
    0        1.5         3.5      5           7
    ↑                    ↑                    ↑
             上次结束到下次开始间隔2s
```

### 内部实现

```java
// ScheduledThreadPoolExecutor 内部使用 DelayedWorkQueue
public class ScheduledThreadPoolExecutor extends ThreadPoolExecutor {
    
    // 内部队列就是基于堆的延迟队列
    static class DelayedWorkQueue extends AbstractQueue<Runnable>
            implements BlockingQueue<Runnable> {
        
        private RunnableScheduledFuture<?>[] queue = 
            new RunnableScheduledFuture<?>[16];
        
        // 使用堆结构，按执行时间排序
    }
    
    // 包装任务
    private class ScheduledFutureTask<V> 
            extends FutureTask<V> implements RunnableScheduledFuture<V> {
        
        private long time;        // 下次执行时间
        private final long period; // 周期（0表示一次性）
        
        // 周期任务执行后重新设置time并放回队列
        public void run() {
            boolean periodic = isPeriodic();
            // 执行任务
            if (ScheduledFutureTask.super.runAndReset()) {
                // 计算下次执行时间
                setNextRunTime();
                // 重新入队
                reExecutePeriodic(outerTask);
            }
        }
    }
}
```

## 对比总结

| 特性 | DelayQueue | ScheduledThreadPool |
|------|------------|---------------------|
| **类型** | 数据结构（队列） | 线程池 |
| **执行** | 需自己take()消费 | 自动执行 |
| **周期任务** | ❌ 不支持 | ✅ 支持 |
| **返回值** | 取出的元素 | ScheduledFuture |
| **灵活性** | 高（可自定义消费逻辑） | 低（标准执行） |
| **线程模型** | 自己控制 | 固定线程池 |
| **适用场景** | 缓存过期、延迟队列 | 定时任务调度 |

## 面试回答

### 30秒版本
> DelayQueue是延迟阻塞队列，元素实现Delayed接口，take()阻塞直到元素到期，需要自己消费处理，适合缓存过期、订单超时场景。ScheduledThreadPool是定时任务线程池，支持延迟执行和周期执行（scheduleAtFixedRate/scheduleWithFixedDelay），任务自动执行。ScheduledThreadPool内部用的也是延迟队列DelayedWorkQueue。

### 1分钟版本
> 两者都能实现延迟任务但定位不同。
> 
> **DelayQueue**是纯数据结构，是一个基于优先队列的延迟阻塞队列。元素必须实现Delayed接口的getDelay()方法返回剩余延迟时间。take()会阻塞直到有元素到期，需要自己开线程消费。灵活性高但不支持周期任务，适合缓存过期、订单超时取消等场景。
> 
> **ScheduledThreadPool**是完整的定时任务线程池。支持schedule()一次性延迟、scheduleAtFixedRate()固定速率、scheduleWithFixedDelay()固定延迟三种调度方式。任务自动执行不需要手动消费。内部其实也是用DelayedWorkQueue存储任务，周期任务执行后会重新计算下次执行时间再放回队列。
> 
> 简单说，需要定时周期任务用ScheduledThreadPool，需要灵活控制延迟队列用DelayQueue。

## 相关问题
- [[timer]] - Timer定时器
- [[time-wheel]] - 时间轮算法
- [[blocking-queues]] - 阻塞队列
