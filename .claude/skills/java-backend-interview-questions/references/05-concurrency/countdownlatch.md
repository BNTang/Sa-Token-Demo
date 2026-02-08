# 什么是Java的CountDownLatch?

## 回答

CountDownLatch（倒计时门闩）是一种同步工具，让一个或多个线程等待其他线程完成操作：

```
┌─────────────────────────────────────────────────────────────┐
│                 CountDownLatch 原理                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   CountDownLatch(3)  初始计数=3                              │
│                                                             │
│      countDown()  countDown()  countDown()                  │
│          ↓            ↓            ↓                        │
│       ┌──┴──┐      ┌──┴──┐      ┌──┴──┐                    │
│       │ T1  │      │ T2  │      │ T3  │  子线程             │
│       └─────┘      └─────┘      └─────┘                    │
│          │            │            │                        │
│          └────────────┴────────────┘                        │
│                       │                                     │
│              计数: 3 → 2 → 1 → 0                            │
│                       │                                     │
│                       ▼                                     │
│                 ┌──────────┐                               │
│                 │  主线程   │  await() 阻塞等待              │
│                 │          │  计数=0时放行                   │
│                 └──────────┘                               │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 基本使用

```java
public class CountDownLatchDemo {
    
    public static void main(String[] args) throws InterruptedException {
        // 创建计数器，初始值3
        CountDownLatch latch = new CountDownLatch(3);
        
        for (int i = 0; i < 3; i++) {
            final int taskId = i;
            new Thread(() -> {
                try {
                    System.out.println("任务" + taskId + " 开始执行");
                    Thread.sleep(1000 + taskId * 500);
                    System.out.println("任务" + taskId + " 执行完成");
                } finally {
                    latch.countDown();  // 计数减1
                }
            }).start();
        }
        
        System.out.println("主线程等待所有任务完成...");
        latch.await();  // 阻塞直到计数为0
        System.out.println("所有任务已完成，主线程继续");
    }
}

// 输出：
// 主线程等待所有任务完成...
// 任务0 开始执行
// 任务1 开始执行
// 任务2 开始执行
// 任务0 执行完成
// 任务1 执行完成
// 任务2 执行完成
// 所有任务已完成，主线程继续
```

## 核心方法

```java
CountDownLatch latch = new CountDownLatch(3);

// 等待
latch.await();                              // 阻塞直到计数为0
boolean result = latch.await(5, TimeUnit.SECONDS);  // 超时等待

// 计数减1
latch.countDown();

// 查询
long count = latch.getCount();  // 获取当前计数
```

## 两种使用模式

### 模式1：一等多（主线程等待多个子任务）

```java
// 主线程等待N个子任务完成
CountDownLatch latch = new CountDownLatch(N);

for (int i = 0; i < N; i++) {
    executor.execute(() -> {
        try {
            doTask();
        } finally {
            latch.countDown();
        }
    });
}

latch.await();  // 主线程等待
System.out.println("所有任务完成");
```

### 模式2：多等一（多个线程等待信号）

```java
// 多个线程等待发令枪
CountDownLatch startSignal = new CountDownLatch(1);

for (int i = 0; i < N; i++) {
    new Thread(() -> {
        startSignal.await();  // 等待开始信号
        doWork();
    }).start();
}

// 准备工作...
Thread.sleep(1000);
startSignal.countDown();  // 发出开始信号
```

## 实际应用场景

### 1. 服务启动依赖检查

```java
public class ServiceStarter {
    
    public void startServices() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        
        // 检查数据库连接
        executor.execute(() -> {
            checkDatabase();
            latch.countDown();
        });
        
        // 检查Redis连接
        executor.execute(() -> {
            checkRedis();
            latch.countDown();
        });
        
        // 检查MQ连接
        executor.execute(() -> {
            checkMQ();
            latch.countDown();
        });
        
        // 等待所有检查完成
        boolean ready = latch.await(30, TimeUnit.SECONDS);
        if (ready) {
            System.out.println("所有依赖就绪，启动服务");
            startServer();
        } else {
            System.out.println("依赖检查超时，启动失败");
        }
    }
}
```

### 2. 并行接口聚合

```java
public class ParallelApiCall {
    
    public UserDetail getUserDetail(Long userId) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        
        AtomicReference<User> userRef = new AtomicReference<>();
        AtomicReference<List<Order>> ordersRef = new AtomicReference<>();
        AtomicReference<Account> accountRef = new AtomicReference<>();
        
        // 并行调用三个接口
        executor.execute(() -> {
            userRef.set(userService.getUser(userId));
            latch.countDown();
        });
        
        executor.execute(() -> {
            ordersRef.set(orderService.getOrders(userId));
            latch.countDown();
        });
        
        executor.execute(() -> {
            accountRef.set(accountService.getAccount(userId));
            latch.countDown();
        });
        
        // 等待所有接口返回
        latch.await(5, TimeUnit.SECONDS);
        
        return new UserDetail(userRef.get(), ordersRef.get(), accountRef.get());
    }
}
```

### 3. 压力测试模拟并发

```java
public class ConcurrentTester {
    
    public void runTest(int threadCount) throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);   // 开始信号
        CountDownLatch endLatch = new CountDownLatch(threadCount);  // 结束信号
        
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();  // 等待开始信号
                    // 执行测试请求
                    sendRequest();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    endLatch.countDown();
                }
            }).start();
        }
        
        // 所有线程准备好后，同时开始
        Thread.sleep(100);
        System.out.println("开始并发测试");
        startLatch.countDown();  // 发出开始信号
        
        // 等待所有请求完成
        endLatch.await();
        System.out.println("测试完成");
    }
}
```

### 4. 批量任务超时控制

```java
public class BatchProcessor {
    
    public void processBatch(List<Task> tasks, long timeoutMs) {
        CountDownLatch latch = new CountDownLatch(tasks.size());
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();
        
        for (Task task : tasks) {
            executor.execute(() -> {
                try {
                    processTask(task);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        boolean completed = latch.await(timeoutMs, TimeUnit.MILLISECONDS);
        
        if (completed) {
            System.out.println("全部完成: 成功=" + successCount + ", 失败=" + failCount);
        } else {
            long remaining = latch.getCount();
            System.out.println("超时，未完成任务数: " + remaining);
        }
    }
}
```

## 源码分析

```java
public class CountDownLatch {
    
    private final Sync sync;
    
    // 基于AQS实现
    private static final class Sync extends AbstractQueuedSynchronizer {
        
        Sync(int count) {
            setState(count);  // state表示计数
        }
        
        int getCount() {
            return getState();
        }
        
        // await()调用
        protected int tryAcquireShared(int acquires) {
            return (getState() == 0) ? 1 : -1;  // 计数为0返回1（成功）
        }
        
        // countDown()调用
        protected boolean tryReleaseShared(int releases) {
            for (;;) {
                int c = getState();
                if (c == 0)
                    return false;
                int nextc = c - 1;
                if (compareAndSetState(c, nextc))
                    return nextc == 0;  // 减到0时返回true，唤醒等待线程
            }
        }
    }
}
```

## 注意事项

```java
// ⚠️ CountDownLatch是一次性的，不能重置

CountDownLatch latch = new CountDownLatch(3);
latch.countDown();
latch.countDown();
latch.countDown();
latch.await();  // 通过

// 此时计数已经为0，无法重用
// 如果需要重用，使用CyclicBarrier

// ⚠️ countDown()要在finally中调用
executor.execute(() -> {
    try {
        doWork();
    } finally {
        latch.countDown();  // 确保一定会执行
    }
});
```

## 面试回答

### 30秒版本
> CountDownLatch是倒计时门闩，让一个线程等待其他线程完成。await()阻塞直到计数为0，countDown()将计数减1。基于AQS实现，state表示计数。一次性使用不可重置。典型应用：主线程等待子任务完成、服务启动依赖检查、并行接口聚合。

### 1分钟版本
> CountDownLatch（倒计时门闩）是JUC提供的同步工具，用于让一个或多个线程等待其他线程完成操作。
> 
> **使用方式**：构造时设置初始计数，调用await()的线程会阻塞直到计数为0。其他线程完成任务后调用countDown()将计数减1。支持await(timeout)超时等待。
> 
> **两种模式**：①**一等多**：主线程await()，多个子线程countDown()；②**多等一**：多个线程await()等待信号，一个线程countDown()发令。
> 
> **实现原理**：基于AQS的共享模式，state表示计数。countDown()时CAS减少state，减到0时唤醒所有等待线程。
> 
> **注意**：一次性使用，计数到0后不能重置。countDown()应在finally中调用确保执行。如需重用考虑CyclicBarrier。

## 相关问题
- [[cyclic-barrier]] - CyclicBarrier详解
- [[concurrent-utils]] - 并发工具类
- [[semaphore]] - Semaphore信号量
- [[aqs]] - AQS原理
