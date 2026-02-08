# 什么是Java的Semaphore?

## 回答

Semaphore（信号量）是一种并发控制工具，用于限制同时访问共享资源的线程数量：

```
┌─────────────────────────────────────────────────────────────┐
│                    Semaphore 原理                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   Semaphore(3)  初始3个许可                                  │
│                                                             │
│   ┌─────┐ ┌─────┐ ┌─────┐                                  │
│   │ 许可 │ │ 许可 │ │ 许可 │  ← 可用许可池                    │
│   └─────┘ └─────┘ └─────┘                                  │
│      ↑       ↑       ↑                                      │
│   acquire acquire acquire                                   │
│      │       │       │                                      │
│   ┌──┴──┐ ┌──┴──┐ ┌──┴──┐ ┌─────┐ ┌─────┐                 │
│   │ T1  │ │ T2  │ │ T3  │ │ T4  │ │ T5  │  ← 线程          │
│   └─────┘ └─────┘ └─────┘ └──┬──┘ └──┬──┘                  │
│   (执行中) (执行中) (执行中)    │       │                    │
│                             等待阻塞                        │
│                                                             │
│   T1 release() → T4 获得许可开始执行                         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 基本使用

```java
public class SemaphoreDemo {
    
    // 创建信号量，最多3个并发
    private final Semaphore semaphore = new Semaphore(3);
    
    public void accessResource() throws InterruptedException {
        // 获取许可（阻塞）
        semaphore.acquire();
        try {
            System.out.println(Thread.currentThread().getName() + " 获取许可");
            // 访问共享资源
            Thread.sleep(2000);
        } finally {
            // 释放许可（必须在finally中）
            semaphore.release();
            System.out.println(Thread.currentThread().getName() + " 释放许可");
        }
    }
    
    public static void main(String[] args) {
        SemaphoreDemo demo = new SemaphoreDemo();
        
        // 启动10个线程，只有3个能同时执行
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                try {
                    demo.accessResource();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "Thread-" + i).start();
        }
    }
}
```

## 核心方法

```java
Semaphore semaphore = new Semaphore(3);

// 获取许可
semaphore.acquire();           // 阻塞等待
semaphore.acquire(2);          // 一次获取多个许可
semaphore.acquireUninterruptibly();  // 不可中断

// 尝试获取
boolean success = semaphore.tryAcquire();  // 非阻塞
boolean success = semaphore.tryAcquire(1, TimeUnit.SECONDS);  // 超时等待

// 释放许可
semaphore.release();           // 释放1个
semaphore.release(2);          // 释放多个

// 查询方法
int available = semaphore.availablePermits();  // 可用许可数
int waiting = semaphore.getQueueLength();      // 等待线程数
boolean hasWaiting = semaphore.hasQueuedThreads();  // 是否有等待线程
```

## 公平与非公平

```java
// 非公平信号量（默认）- 性能更好
Semaphore unfair = new Semaphore(3);

// 公平信号量 - 按等待顺序获取
Semaphore fair = new Semaphore(3, true);
```

```
┌─────────────────────────────────────────────────────────────┐
│                  公平 vs 非公平                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  非公平（默认）：                                            │
│    新线程可能插队，直接抢到许可                               │
│    优点：吞吐量高，减少上下文切换                             │
│    缺点：可能导致某些线程饥饿                                 │
│                                                             │
│  公平：                                                      │
│    严格按照FIFO顺序获取许可                                   │
│    优点：不会饥饿                                            │
│    缺点：吞吐量略低                                          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 实际应用场景

### 1. 数据库连接池限流

```java
public class ConnectionPool {
    private final Semaphore semaphore;
    private final Queue<Connection> pool;
    
    public ConnectionPool(int size) {
        this.semaphore = new Semaphore(size);
        this.pool = new LinkedList<>();
        // 初始化连接
        for (int i = 0; i < size; i++) {
            pool.offer(createConnection());
        }
    }
    
    public Connection getConnection() throws InterruptedException {
        semaphore.acquire();  // 限制最大连接数
        synchronized (pool) {
            return pool.poll();
        }
    }
    
    public void releaseConnection(Connection conn) {
        synchronized (pool) {
            pool.offer(conn);
        }
        semaphore.release();
    }
}
```

### 2. 接口限流

```java
@Component
public class ApiRateLimiter {
    
    // 每个接口最多10个并发
    private final Map<String, Semaphore> limiters = new ConcurrentHashMap<>();
    
    public <T> T executeWithLimit(String api, int permits, Callable<T> callable) 
            throws Exception {
        Semaphore semaphore = limiters.computeIfAbsent(api, 
            k -> new Semaphore(permits));
        
        if (!semaphore.tryAcquire(100, TimeUnit.MILLISECONDS)) {
            throw new RateLimitException("接口限流: " + api);
        }
        
        try {
            return callable.call();
        } finally {
            semaphore.release();
        }
    }
}

// 使用
@GetMapping("/order/{id}")
public Order getOrder(@PathVariable Long id) throws Exception {
    return rateLimiter.executeWithLimit("/order", 10, 
        () -> orderService.getById(id));
}
```

### 3. 停车场模型

```java
public class ParkingLot {
    private final Semaphore spaces;
    
    public ParkingLot(int totalSpaces) {
        this.spaces = new Semaphore(totalSpaces);
    }
    
    public boolean enter() {
        if (spaces.tryAcquire()) {
            System.out.println("车辆进入，剩余车位: " + spaces.availablePermits());
            return true;
        }
        System.out.println("车位已满");
        return false;
    }
    
    public void exit() {
        spaces.release();
        System.out.println("车辆离开，剩余车位: " + spaces.availablePermits());
    }
}
```

### 4. 互斥锁实现

```java
// Semaphore(1) 可实现互斥锁效果
public class MutexLock {
    private final Semaphore mutex = new Semaphore(1);
    
    public void lock() throws InterruptedException {
        mutex.acquire();
    }
    
    public void unlock() {
        mutex.release();
    }
}
```

## 源码分析

```java
public class Semaphore {
    
    private final Sync sync;
    
    // 基于AQS实现
    abstract static class Sync extends AbstractQueuedSynchronizer {
        
        Sync(int permits) {
            setState(permits);  // state表示可用许可数
        }
        
        final int getPermits() {
            return getState();
        }
        
        // 非公平尝试获取
        final int nonfairTryAcquireShared(int acquires) {
            for (;;) {
                int available = getState();
                int remaining = available - acquires;
                if (remaining < 0 ||
                    compareAndSetState(available, remaining))
                    return remaining;
            }
        }
        
        // 释放
        protected final boolean tryReleaseShared(int releases) {
            for (;;) {
                int current = getState();
                int next = current + releases;
                if (compareAndSetState(current, next))
                    return true;
            }
        }
    }
}
```

## Semaphore vs ReentrantLock

| 特性 | Semaphore | ReentrantLock |
|------|-----------|---------------|
| 许可数 | 可配置多个 | 只有1个 |
| 可重入 | 不可重入 | 可重入 |
| 用途 | 限制并发数 | 互斥访问 |
| 释放者 | 任意线程 | 持有锁的线程 |

## 面试回答

### 30秒版本
> Semaphore是并发控制信号量，通过许可机制限制同时访问共享资源的线程数。acquire()获取许可，没有则阻塞；release()释放许可。基于AQS实现，state表示可用许可数。支持公平/非公平模式。典型应用：数据库连接池限制连接数、接口限流。

### 1分钟版本
> Semaphore（信号量）是JUC提供的并发控制工具，用于限制同时访问共享资源的线程数量。
> 
> **核心方法**：acquire()获取许可，如果没有可用许可则阻塞等待；release()释放许可。支持tryAcquire()非阻塞尝试和超时等待。可以一次获取/释放多个许可。
> 
> **实现原理**：基于AQS实现，state表示可用许可数。acquire时CAS减少state，release时CAS增加state。支持公平和非公平两种模式，公平模式按FIFO顺序获取许可。
> 
> **应用场景**：①**连接池限流**限制最大连接数；②**接口限流**控制接口并发数；③**资源访问控制**如停车场车位管理。Semaphore(1)可以实现类似互斥锁的效果，但不可重入且任意线程可释放。

## 相关问题
- [[aqs]] - AQS原理
- [[concurrent-utils]] - 并发工具类
- [[countdownlatch]] - CountDownLatch
- [[cyclic-barrier]] - CyclicBarrier
