# 你使用过Java中的哪些阻塞队列?

## 回答

Java提供了多种阻塞队列实现，位于`java.util.concurrent`包：

```
┌─────────────────────────────────────────────────────────────┐
│                   阻塞队列分类                               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  有界队列                                                    │
│  ├── ArrayBlockingQueue     数组实现，固定容量               │
│  └── LinkedBlockingQueue    链表实现，可选容量               │
│                                                             │
│  无界队列                                                    │
│  ├── PriorityBlockingQueue  优先级队列                      │
│  ├── DelayQueue             延迟队列                        │
│  └── LinkedTransferQueue    传输队列                        │
│                                                             │
│  特殊队列                                                    │
│  ├── SynchronousQueue       无容量，直接传递                 │
│  └── LinkedBlockingDeque    双端阻塞队列                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 各队列详解

### 1. ArrayBlockingQueue

```java
// 数组实现的有界阻塞队列
BlockingQueue<String> queue = new ArrayBlockingQueue<>(100);

// 特点：
// • 数组结构，固定容量
// • 一把锁（生产消费共用）
// • 支持公平/非公平模式
ArrayBlockingQueue<String> fairQueue = new ArrayBlockingQueue<>(100, true);
```

### 2. LinkedBlockingQueue

```java
// 链表实现的阻塞队列
BlockingQueue<String> queue = new LinkedBlockingQueue<>();     // 默认Integer.MAX_VALUE
BlockingQueue<String> queue = new LinkedBlockingQueue<>(100);  // 指定容量

// 特点：
// • 链表结构，可选容量
// • 两把锁（putLock + takeLock）
// • 吞吐量通常高于ArrayBlockingQueue
```

### 3. PriorityBlockingQueue

```java
// 优先级阻塞队列
BlockingQueue<Task> queue = new PriorityBlockingQueue<>();

// 自定义优先级
BlockingQueue<Task> queue = new PriorityBlockingQueue<>(100, 
    Comparator.comparing(Task::getPriority));

// 特点：
// • 无界队列
// • 元素需实现Comparable或提供Comparator
// • 内部用堆结构
// • 不保证同优先级元素的顺序
```

### 4. DelayQueue

```java
// 延迟队列
DelayQueue<DelayedTask> queue = new DelayQueue<>();

class DelayedTask implements Delayed {
    private long expireTime;
    
    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(expireTime - System.currentTimeMillis(), 
                           TimeUnit.MILLISECONDS);
    }
    
    @Override
    public int compareTo(Delayed o) {
        return Long.compare(this.getDelay(TimeUnit.MILLISECONDS), 
                           o.getDelay(TimeUnit.MILLISECONDS));
    }
}

// 特点：
// • 元素必须实现Delayed接口
// • 只有到期元素才能被取出
// • 无界队列
```

### 5. SynchronousQueue

```java
// 无容量的同步队列
BlockingQueue<String> queue = new SynchronousQueue<>();

// 特点：
// • 没有存储空间
// • 每个put必须等待一个take
// • 适合传递场景
// • Executors.newCachedThreadPool()使用

// 公平模式（FIFO）
SynchronousQueue<String> fairQueue = new SynchronousQueue<>(true);
```

### 6. LinkedTransferQueue

```java
// 传输队列（JDK 7+）
TransferQueue<String> queue = new LinkedTransferQueue<>();

// 特殊方法
queue.transfer(item);      // 阻塞直到有消费者接收
queue.tryTransfer(item);   // 非阻塞尝试传输
queue.tryTransfer(item, 1, TimeUnit.SECONDS);  // 超时传输

// 特点：
// • 融合了LinkedBlockingQueue和SynchronousQueue
// • 无界队列
// • 支持直接传递给消费者
```

### 7. LinkedBlockingDeque

```java
// 双端阻塞队列
BlockingDeque<String> deque = new LinkedBlockingDeque<>();

// 两端操作
deque.putFirst(item);   // 头部插入
deque.putLast(item);    // 尾部插入
deque.takeFirst();      // 头部取出
deque.takeLast();       // 尾部取出

// 特点：
// • 可选容量
// • 支持FIFO和LIFO
// • 可实现工作窃取算法
```

## 核心方法

```java
BlockingQueue<String> queue = new LinkedBlockingQueue<>(100);

// ===== 插入操作 =====
queue.add(item);        // 队列满抛异常
queue.offer(item);      // 队列满返回false
queue.offer(item, 1, TimeUnit.SECONDS);  // 超时等待
queue.put(item);        // 队列满阻塞等待

// ===== 移除操作 =====
queue.remove();         // 队列空抛异常
queue.poll();           // 队列空返回null
queue.poll(1, TimeUnit.SECONDS);  // 超时等待
queue.take();           // 队列空阻塞等待

// ===== 检查操作 =====
queue.element();        // 队列空抛异常
queue.peek();           // 队列空返回null
```

## 对比表格

| 队列 | 数据结构 | 有界 | 锁 | 适用场景 |
|------|----------|------|-----|----------|
| **ArrayBlockingQueue** | 数组 | ✅ | 单锁 | 一般生产消费 |
| **LinkedBlockingQueue** | 链表 | 可选 | 双锁 | 高吞吐场景 |
| **PriorityBlockingQueue** | 堆 | ❌ | 单锁 | 优先级任务 |
| **DelayQueue** | 堆 | ❌ | 单锁 | 延迟任务 |
| **SynchronousQueue** | 无 | - | 无锁CAS | 直接传递 |
| **LinkedTransferQueue** | 链表 | ❌ | 无锁CAS | 高性能传递 |

## 实际应用

### 生产者消费者模式

```java
public class ProducerConsumer {
    private final BlockingQueue<Task> queue = new LinkedBlockingQueue<>(100);
    
    // 生产者
    public void produce(Task task) throws InterruptedException {
        queue.put(task);  // 队列满时阻塞
    }
    
    // 消费者
    public void consume() throws InterruptedException {
        while (true) {
            Task task = queue.take();  // 队列空时阻塞
            process(task);
        }
    }
}
```

### 线程池任务队列选择

```java
// 固定大小线程池 - 无界队列
new ThreadPoolExecutor(10, 10, 0, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>());  // 可能OOM

// 缓存线程池 - 直接传递
new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
    new SynchronousQueue<>());

// 推荐 - 有界队列
new ThreadPoolExecutor(10, 20, 60, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(1000),  // 有界
    new ThreadPoolExecutor.CallerRunsPolicy());
```

### 延迟任务处理

```java
DelayQueue<DelayedTask> queue = new DelayQueue<>();

// 添加延迟任务
queue.put(new DelayedTask("订单超时", 30, TimeUnit.MINUTES));

// 消费延迟任务
new Thread(() -> {
    while (true) {
        DelayedTask task = queue.take();  // 只有到期才能取出
        processTask(task);
    }
}).start();
```

## 选择建议

| 场景 | 推荐队列 |
|------|----------|
| 一般生产消费 | LinkedBlockingQueue(有界) |
| 需要公平访问 | ArrayBlockingQueue(fair) |
| 优先级任务 | PriorityBlockingQueue |
| 延迟执行 | DelayQueue |
| 线程间直接传递 | SynchronousQueue |
| 高性能传递 | LinkedTransferQueue |
| 工作窃取 | LinkedBlockingDeque |

## 面试回答

### 30秒版本
> 常用阻塞队列：①ArrayBlockingQueue数组实现有界单锁；②LinkedBlockingQueue链表实现可选容量双锁吞吐高；③PriorityBlockingQueue优先级无界；④DelayQueue延迟队列；⑤SynchronousQueue无容量直接传递。线程池推荐用LinkedBlockingQueue并设置容量，避免OOM。

### 1分钟版本
> Java提供了多种阻塞队列，核心方法是put()/take()阻塞、offer()/poll()非阻塞或超时。
> 
> **有界队列**：ArrayBlockingQueue数组实现固定容量，使用单锁，支持公平模式；LinkedBlockingQueue链表实现可选容量，使用put和take两把锁，吞吐量更高。
> 
> **无界队列**：PriorityBlockingQueue按优先级排序，元素需实现Comparable；DelayQueue延迟队列，只有到期元素才能取出，适合定时任务。
> 
> **特殊队列**：SynchronousQueue没有容量，每个put必须等待take，CachedThreadPool使用它；LinkedTransferQueue支持transfer()直接传递给消费者。
> 
> **使用建议**：生产环境推荐LinkedBlockingQueue并设置容量上限，避免无界队列导致OOM。线程池任务队列选择有界队列+合适的拒绝策略。

## 相关问题
- [[thread-pool-principle]] - 线程池原理
- [[delayqueue-vs-scheduledpool]] - DelayQueue vs ScheduledPool
- [[concurrent-utils]] - 并发工具类
