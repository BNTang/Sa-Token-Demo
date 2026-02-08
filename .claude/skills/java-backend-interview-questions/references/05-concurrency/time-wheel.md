# 时间轮(TimeWheel)算法

## 回答

时间轮是一种高效的定时任务调度算法，通过环形数组和指针实现O(1)时间复杂度的任务添加：

```
┌─────────────────────────────────────────────────────────────┐
│                    时间轮结构示意                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│              0   1   2   3   4   5   6   7                 │
│            ┌───┬───┬───┬───┬───┬───┬───┬───┐              │
│            │ ○ │   │ ○ │   │   │ ○ │   │   │              │
│            └─┬─┴───┴─┬─┴───┴───┴─┬─┴───┴───┘              │
│              │       │           │                         │
│              ▼       ▼           ▼                         │
│            任务链   任务链      任务链                       │
│                                                             │
│                  ───→ 指针（每tick移动一格）                 │
│                                                             │
│  tick = 100ms, 8格 → 可表示 0~800ms 的延迟                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 核心概念

| 概念 | 说明 |
|------|------|
| **tick** | 时间轮的最小时间单位（精度） |
| **wheel** | 环形数组，每个槽位存放任务链表 |
| **pointer** | 当前指针位置，每tick移动一格 |
| **bucket** | 槽位，存放相同执行时间的任务 |
| **round** | 圈数，用于处理超过一圈的延迟 |

## 简单实现

```java
public class SimpleTimeWheel {
    
    private final int tickMs;           // 每tick毫秒数
    private final int wheelSize;        // 槽位数
    private final long interval;        // 一圈总时间
    private final List<TimerTask>[] buckets;  // 槽位数组
    private volatile int currentIndex;  // 当前指针位置
    private final AtomicLong currentTime;
    
    public SimpleTimeWheel(int tickMs, int wheelSize) {
        this.tickMs = tickMs;
        this.wheelSize = wheelSize;
        this.interval = (long) tickMs * wheelSize;
        this.buckets = new ArrayList[wheelSize];
        for (int i = 0; i < wheelSize; i++) {
            buckets[i] = new ArrayList<>();
        }
        this.currentIndex = 0;
        this.currentTime = new AtomicLong(System.currentTimeMillis());
    }
    
    // 添加任务 O(1)
    public void addTask(TimerTask task, long delayMs) {
        if (delayMs < tickMs) {
            // 立即执行
            executeTask(task);
            return;
        }
        
        // 计算槽位
        long expireTime = currentTime.get() + delayMs;
        int ticks = (int) (delayMs / tickMs);
        int bucketIndex = (currentIndex + ticks) % wheelSize;
        
        // 计算圈数（用于多圈延迟）
        task.setRounds(ticks / wheelSize);
        task.setExpireTime(expireTime);
        
        synchronized (buckets[bucketIndex]) {
            buckets[bucketIndex].add(task);
        }
    }
    
    // 指针推进（由定时线程调用）
    public void advanceClock() {
        currentTime.addAndGet(tickMs);
        currentIndex = (currentIndex + 1) % wheelSize;
        
        List<TimerTask> bucket = buckets[currentIndex];
        synchronized (bucket) {
            Iterator<TimerTask> it = bucket.iterator();
            while (it.hasNext()) {
                TimerTask task = it.next();
                if (task.getRounds() <= 0) {
                    // 到期执行
                    executeTask(task);
                    it.remove();
                } else {
                    // 减少圈数，下次再检查
                    task.decrementRounds();
                }
            }
        }
    }
    
    private void executeTask(TimerTask task) {
        // 提交到线程池执行
        executor.execute(task);
    }
}
```

## 多层时间轮

```
┌─────────────────────────────────────────────────────────────┐
│                  多层时间轮（Hierarchical）                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  秒级轮 (tick=1s, 60格)     分钟轮 (tick=1min, 60格)        │
│  ┌──────────────────────┐   ┌──────────────────────┐       │
│  │ 0 1 2 ... 58 59     │   │ 0 1 2 ... 58 59     │       │
│  │ ○   ○      ○        │   │ ○       ○           │       │
│  └──────────┬───────────┘   └─────────┬────────────┘       │
│             │    溢出降级              │                    │
│             │←─────────────────────────┘                    │
│                                                             │
│  任务延迟30秒 → 放入秒级轮                                    │
│  任务延迟5分钟 → 放入分钟轮，到期降级到秒级轮                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Kafka时间轮实现

```java
// Kafka TimingWheel 简化示意
public class TimingWheel {
    
    private final long tickMs;          // 每tick时间
    private final int wheelSize;        // 槽位数
    private final long interval;        // 一圈总时间
    private final TimerTaskList[] buckets;
    private final AtomicLong currentTime;
    
    // 溢出轮（高层时间轮）
    private volatile TimingWheel overflowWheel;
    
    public void add(TimerTaskEntry entry) {
        long expiration = entry.getExpirationMs();
        
        if (expiration < currentTime.get() + tickMs) {
            // 已过期，立即执行
        } else if (expiration < currentTime.get() + interval) {
            // 在当前轮范围内
            int virtualId = (int) (expiration / tickMs);
            int bucketIndex = virtualId % wheelSize;
            buckets[bucketIndex].add(entry);
        } else {
            // 超出当前轮范围，放入溢出轮
            if (overflowWheel == null) {
                addOverflowWheel();
            }
            overflowWheel.add(entry);
        }
    }
    
    private synchronized void addOverflowWheel() {
        if (overflowWheel == null) {
            // 创建更高层的时间轮
            // tick = 当前轮的interval，wheelSize不变
            overflowWheel = new TimingWheel(
                interval,           // 新轮的tick = 当前轮一圈时间
                wheelSize,
                currentTime.get()
            );
        }
    }
}
```

## Netty HashedWheelTimer

```java
// Netty HashedWheelTimer 使用示例
HashedWheelTimer timer = new HashedWheelTimer(
    Executors.defaultThreadFactory(),
    100, TimeUnit.MILLISECONDS,  // tick精度100ms
    512,                          // 512个槽位
    true,                         // 记录待处理任务数
    -1                            // 无限制任务数
);

// 添加延迟任务
Timeout timeout = timer.newTimeout(
    task -> System.out.println("任务执行"),
    5, TimeUnit.SECONDS
);

// 取消任务
timeout.cancel();

// 关闭
timer.stop();
```

### Netty实现特点

```
┌─────────────────────────────────────────────────────────────┐
│              Netty HashedWheelTimer 特点                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 单线程Worker推进时间轮，执行到期任务                      │
│  2. 任务执行如果耗时，会阻塞其他任务（建议提交到线程池）        │
│  3. 延迟任务先放入Queue，Worker启动后批量添加到槽位           │
│  4. 使用MpscQueue支持多生产者添加任务                        │
│  5. 适合大量相对均匀分布的定时任务                            │
│                                                             │
│  优点：添加删除O(1)，内存占用低                               │
│  缺点：精度受tick影响，不适合精确定时                         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 应用场景

| 场景 | 说明 |
|------|------|
| **连接超时检测** | Netty心跳检测、连接空闲检测 |
| **延迟消息** | Kafka延迟消息队列 |
| **限流窗口** | 滑动窗口限流 |
| **缓存过期** | 大量key过期检测 |
| **重试机制** | 消息重试、失败重试调度 |
| **任务调度** | 分布式任务调度系统 |

## 时间复杂度对比

| 数据结构 | 添加任务 | 删除任务 | 获取最近任务 |
|----------|----------|----------|--------------|
| 小顶堆 | O(log n) | O(log n) | O(1) |
| 红黑树 | O(log n) | O(log n) | O(log n) |
| **时间轮** | **O(1)** | **O(1)** | O(1) |

## 面试回答

### 30秒版本
> 时间轮是高效的定时任务调度算法，核心是环形数组+链表，指针每tick移动一格。任务根据延迟时间计算槽位直接插入，复杂度O(1)。超过一圈的用圈数标记或溢出到更高层时间轮。典型应用：Netty的HashedWheelTimer做心跳检测、Kafka的延迟消息队列。比堆结构更适合大量定时任务场景。

### 1分钟版本
> 时间轮是一种高效的定时任务调度算法，核心思想是用**环形数组+链表**实现。数组每个槽位存放任务链表，指针每tick时间移动一格，执行当前槽位的到期任务。
> 
> **添加任务**：计算延迟时间对应的槽位直接插入，O(1)复杂度。**处理长延迟**：①用rounds圈数标记，每经过一圈rounds减1，为0时执行；②多层时间轮，秒级→分钟级→小时级，任务到期从高层降级到低层。
> 
> **典型实现**：Netty的HashedWheelTimer用于连接超时检测、心跳；Kafka的TimingWheel用于延迟消息。**优点**：添加删除O(1)，适合大量任务。**缺点**：精度受tick影响，不适合毫秒级精确定时。相比堆结构O(log n)，时间轮在大量定时任务场景下性能更好。

## 相关问题
- [[timer]] - Java Timer
- [[delayqueue-vs-scheduledpool]] - DelayQueue vs ScheduledThreadPool
- [[executors-thread-pools]] - 线程池实现
