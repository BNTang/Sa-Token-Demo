# 你使用过哪些Java并发工具类?

## 回答

Java并发工具类主要位于`java.util.concurrent`包，常用的包括：

```
┌─────────────────────────────────────────────────────────────┐
│                Java并发工具类分类                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  同步工具                                                    │
│  ├── CountDownLatch    倒计时门闩，等待N个任务完成            │
│  ├── CyclicBarrier     循环屏障，N个线程互相等待              │
│  ├── Semaphore         信号量，控制并发数                    │
│  ├── Phaser            阶段器，动态增减参与者                 │
│  └── Exchanger         交换器，两个线程交换数据               │
│                                                             │
│  锁工具                                                      │
│  ├── ReentrantLock     可重入锁                             │
│  ├── ReentrantReadWriteLock  读写锁                         │
│  ├── StampedLock       乐观读锁                             │
│  └── LockSupport       线程阻塞原语                          │
│                                                             │
│  并发容器                                                    │
│  ├── ConcurrentHashMap 并发哈希表                           │
│  ├── CopyOnWriteArrayList  写时复制列表                      │
│  └── ConcurrentLinkedQueue 无锁并发队列                      │
│                                                             │
│  阻塞队列                                                    │
│  ├── ArrayBlockingQueue    有界阻塞队列                      │
│  ├── LinkedBlockingQueue   链式阻塞队列                      │
│  ├── PriorityBlockingQueue 优先级阻塞队列                    │
│  ├── DelayQueue            延迟队列                          │
│  └── SynchronousQueue      直接传递队列                      │
│                                                             │
│  原子类                                                      │
│  ├── AtomicInteger/Long    原子整数                         │
│  ├── AtomicReference       原子引用                         │
│  └── LongAdder             高性能累加器                      │
│                                                             │
│  线程池                                                      │
│  ├── ThreadPoolExecutor    线程池                           │
│  ├── ScheduledThreadPoolExecutor 定时任务池                  │
│  └── ForkJoinPool          分治任务池                        │
│                                                             │
│  异步编排                                                    │
│  └── CompletableFuture     异步编排                         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 常用工具详解

### 1. CountDownLatch（倒计时门闩）

```java
// 主线程等待N个子任务完成
CountDownLatch latch = new CountDownLatch(3);

for (int i = 0; i < 3; i++) {
    executor.execute(() -> {
        try {
            // 执行任务
            doWork();
        } finally {
            latch.countDown();  // 计数器减1
        }
    });
}

latch.await();  // 阻塞直到计数器为0
System.out.println("所有任务完成");
```

### 2. CyclicBarrier（循环屏障）

```java
// 多个线程互相等待，全部到达后一起继续
CyclicBarrier barrier = new CyclicBarrier(3, () -> {
    System.out.println("所有线程到达屏障");
});

for (int i = 0; i < 3; i++) {
    executor.execute(() -> {
        System.out.println(Thread.currentThread().getName() + " 到达");
        barrier.await();  // 等待其他线程
        System.out.println(Thread.currentThread().getName() + " 继续执行");
    });
}
```

### 3. Semaphore（信号量）

```java
// 限制同时访问资源的线程数
Semaphore semaphore = new Semaphore(3);  // 最多3个并发

for (int i = 0; i < 10; i++) {
    executor.execute(() -> {
        try {
            semaphore.acquire();  // 获取许可
            // 访问有限资源
            accessResource();
        } finally {
            semaphore.release();  // 释放许可
        }
    });
}
```

### 4. ReentrantLock（可重入锁）

```java
ReentrantLock lock = new ReentrantLock();

public void doSomething() {
    lock.lock();
    try {
        // 临界区代码
    } finally {
        lock.unlock();
    }
}

// 高级特性
lock.tryLock();                    // 非阻塞尝试
lock.tryLock(1, TimeUnit.SECONDS); // 超时等待
lock.lockInterruptibly();          // 可中断等待
Condition condition = lock.newCondition();  // 条件变量
```

### 5. CompletableFuture（异步编排）

```java
// 异步任务编排
CompletableFuture.supplyAsync(() -> getUserInfo(userId))
    .thenApply(user -> getOrderList(user))
    .thenAccept(orders -> System.out.println(orders))
    .exceptionally(e -> {
        log.error("异常", e);
        return null;
    });

// 并行执行多个任务
CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> "任务1");
CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> "任务2");
CompletableFuture<Void> allOf = CompletableFuture.allOf(f1, f2);
```

### 6. BlockingQueue（阻塞队列）

```java
BlockingQueue<Task> queue = new LinkedBlockingQueue<>(100);

// 生产者
queue.put(task);     // 队列满时阻塞
queue.offer(task, 1, TimeUnit.SECONDS);  // 超时等待

// 消费者
Task task = queue.take();  // 队列空时阻塞
Task task = queue.poll(1, TimeUnit.SECONDS);  // 超时等待
```

## 工具选择指南

| 场景 | 推荐工具 |
|------|----------|
| 等待多个任务完成 | CountDownLatch |
| 多线程分阶段协作 | CyclicBarrier / Phaser |
| 限制并发数 | Semaphore |
| 生产者消费者 | BlockingQueue |
| 读多写少 | ReentrantReadWriteLock / StampedLock |
| 异步编排 | CompletableFuture |
| 无锁计数 | AtomicInteger / LongAdder |
| 并发Map | ConcurrentHashMap |
| 读多写少List | CopyOnWriteArrayList |
| 定时任务 | ScheduledThreadPoolExecutor |
| 分治计算 | ForkJoinPool |

## 实际应用示例

### 批量数据处理

```java
public void batchProcess(List<Task> tasks) {
    CountDownLatch latch = new CountDownLatch(tasks.size());
    
    for (Task task : tasks) {
        executor.execute(() -> {
            try {
                process(task);
            } finally {
                latch.countDown();
            }
        });
    }
    
    latch.await(30, TimeUnit.SECONDS);
    System.out.println("批量处理完成");
}
```

### 接口限流

```java
public class RateLimiter {
    private final Semaphore semaphore;
    
    public RateLimiter(int permits) {
        this.semaphore = new Semaphore(permits);
    }
    
    public <T> T execute(Supplier<T> supplier) {
        semaphore.acquire();
        try {
            return supplier.get();
        } finally {
            semaphore.release();
        }
    }
}
```

### 并行聚合查询

```java
public UserDetail getUserDetail(Long userId) {
    CompletableFuture<User> userFuture = 
        CompletableFuture.supplyAsync(() -> userService.getUser(userId));
    CompletableFuture<List<Order>> orderFuture = 
        CompletableFuture.supplyAsync(() -> orderService.getOrders(userId));
    CompletableFuture<Account> accountFuture = 
        CompletableFuture.supplyAsync(() -> accountService.getAccount(userId));
    
    return CompletableFuture.allOf(userFuture, orderFuture, accountFuture)
        .thenApply(v -> new UserDetail(
            userFuture.join(),
            orderFuture.join(),
            accountFuture.join()
        ))
        .join();
}
```

## 面试回答

### 30秒版本
> 常用并发工具类：①**同步工具**CountDownLatch等待多任务完成、CyclicBarrier多线程互相等待、Semaphore限制并发数；②**锁**ReentrantLock可重入锁、StampedLock乐观读；③**并发容器**ConcurrentHashMap、CopyOnWriteArrayList；④**阻塞队列**LinkedBlockingQueue生产者消费者；⑤**原子类**AtomicInteger、LongAdder；⑥**异步**CompletableFuture编排异步任务。

### 1分钟版本
> 我使用过的Java并发工具类主要包括以下几类：
> 
> **同步协调工具**：CountDownLatch用于主线程等待多个子任务完成，计数减到0时放行；CyclicBarrier用于多个线程互相等待，全部到达屏障后一起继续，可循环使用；Semaphore信号量控制并发数，比如限流。
> 
> **锁工具**：ReentrantLock比synchronized更灵活，支持tryLock、可中断、条件变量；ReentrantReadWriteLock读写分离；StampedLock支持乐观读，性能更好。
> 
> **并发容器**：ConcurrentHashMap高并发Map；CopyOnWriteArrayList读多写少场景；BlockingQueue阻塞队列用于生产者消费者模式。
> 
> **原子类**：AtomicInteger、AtomicReference无锁更新；LongAdder高并发计数性能更好。
> 
> **异步编排**：CompletableFuture链式组合异步任务，支持异常处理和多任务并行。
> 
> 实际项目中最常用的是CompletableFuture做接口聚合、Semaphore做限流、CountDownLatch做批量任务等待。

## 相关问题
- [[countdownlatch]] - CountDownLatch详解
- [[cyclic-barrier]] - CyclicBarrier详解
- [[semaphore]] - Semaphore详解
- [[completable-future]] - CompletableFuture详解
