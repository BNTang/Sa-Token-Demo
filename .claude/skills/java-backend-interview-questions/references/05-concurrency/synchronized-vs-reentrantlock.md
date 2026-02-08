# Synchronized 和 ReentrantLock 有什么区别？

## 核心对比

```
┌─────────────────────────────────────────────────────────────┐
│              synchronized vs ReentrantLock                   │
├──────────────────┬──────────────────────────────────────────┤
│   特性            │   synchronized    │   ReentrantLock     │
├──────────────────┼───────────────────┼─────────────────────┤
│   实现层面        │   JVM 关键字       │   JDK API (Lock)    │
│   锁获取方式      │   自动获取释放     │   手动 lock/unlock  │
│   可中断          │   ❌ 不支持        │   ✅ lockInterruptibly() │
│   超时获取        │   ❌ 不支持        │   ✅ tryLock(timeout) │
│   公平锁          │   ❌ 仅非公平      │   ✅ 可选公平/非公平   │
│   条件变量        │   单一 (wait/notify) │   多个 Condition    │
│   锁状态查询      │   ❌ 不支持        │   ✅ isLocked() 等    │
│   读写锁          │   ❌ 不支持        │   ✅ ReentrantReadWriteLock │
│   性能(JDK 6+)   │   优化后差不多     │   差不多             │
│   使用难度        │   简单             │   复杂（需手动释放） │
└──────────────────┴───────────────────┴─────────────────────┘
```

## 代码对比

### 基本使用

```java
// =============== synchronized ===============
public class SynchronizedExample {
    private int count = 0;
    
    // 方式1: 同步方法
    public synchronized void increment() {
        count++;
    }
    
    // 方式2: 同步代码块
    public void incrementBlock() {
        synchronized (this) {
            count++;
        }
    }
    
    // 方式3: 类锁
    public static synchronized void staticMethod() {
        // 锁的是 Class 对象
    }
}

// =============== ReentrantLock ===============
public class ReentrantLockExample {
    private int count = 0;
    private final ReentrantLock lock = new ReentrantLock();
    
    public void increment() {
        lock.lock();  // 获取锁
        try {
            count++;
        } finally {
            lock.unlock();  // 必须在 finally 中释放锁！
        }
    }
}
```

### 可中断锁

```java
public class InterruptibleLockExample {
    private final ReentrantLock lock = new ReentrantLock();
    
    public void doWork() throws InterruptedException {
        // 可中断地获取锁
        lock.lockInterruptibly();
        try {
            // 执行业务逻辑
            Thread.sleep(10000);
        } finally {
            lock.unlock();
        }
    }
}

// 使用示例
Thread t = new Thread(() -> {
    try {
        example.doWork();
    } catch (InterruptedException e) {
        System.out.println("获取锁被中断，可以做其他事情");
    }
});
t.start();

// 中断线程
Thread.sleep(1000);
t.interrupt();  // synchronized 无法响应中断
```

### 超时获取锁

```java
public class TryLockExample {
    private final ReentrantLock lock = new ReentrantLock();
    
    public boolean tryDoWork() {
        // 尝试获取锁，最多等待 3 秒
        boolean acquired = false;
        try {
            acquired = lock.tryLock(3, TimeUnit.SECONDS);
            if (acquired) {
                try {
                    // 执行业务逻辑
                    return true;
                } finally {
                    lock.unlock();
                }
            } else {
                // 获取锁超时，执行降级逻辑
                return handleTimeout();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    // 不等待，立即返回
    public boolean tryDoWorkNonBlocking() {
        if (lock.tryLock()) {
            try {
                // 执行业务
                return true;
            } finally {
                lock.unlock();
            }
        }
        return false;
    }
}
```

### 公平锁

```java
public class FairLockExample {
    // 非公平锁（默认）- 性能更好，可能导致线程饥饿
    private final ReentrantLock unfairLock = new ReentrantLock(false);
    
    // 公平锁 - 按等待顺序获取锁，吞吐量较低
    private final ReentrantLock fairLock = new ReentrantLock(true);
    
    /*
     * 公平锁 vs 非公平锁
     * 
     * 非公平锁：新线程可能直接抢到锁，无需排队
     *   优点：减少上下文切换，吞吐量高
     *   缺点：可能导致线程饥饿
     * 
     * 公平锁：严格按 FIFO 顺序获取锁
     *   优点：不会饥饿
     *   缺点：额外维护队列，性能开销大
     */
}
```

### 多条件变量 (Condition)

```java
public class BoundedBuffer<T> {
    private final Object[] items = new Object[100];
    private int count, putIdx, takeIdx;
    
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();   // 条件1：不满
    private final Condition notEmpty = lock.newCondition();  // 条件2：不空
    
    public void put(T item) throws InterruptedException {
        lock.lock();
        try {
            while (count == items.length) {
                notFull.await();  // 满了就等待
            }
            items[putIdx] = item;
            if (++putIdx == items.length) putIdx = 0;
            count++;
            notEmpty.signal();  // 通知消费者
        } finally {
            lock.unlock();
        }
    }
    
    @SuppressWarnings("unchecked")
    public T take() throws InterruptedException {
        lock.lock();
        try {
            while (count == 0) {
                notEmpty.await();  // 空了就等待
            }
            T item = (T) items[takeIdx];
            items[takeIdx] = null;
            if (++takeIdx == items.length) takeIdx = 0;
            count--;
            notFull.signal();  // 通知生产者
            return item;
        } finally {
            lock.unlock();
        }
    }
}

// 对比 synchronized 只有一个等待队列
// synchronized 需要所有线程共用 wait/notify
// ReentrantLock 可以精确唤醒指定条件的线程
```

## 实现原理对比

```
┌─────────────────────────────────────────────────────────────┐
│                  synchronized 实现原理                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   对象头 Mark Word (64 位 HotSpot)                           │
│   ┌────────────────────────────────────────────────────┐   │
│   │ 无锁    │ hashCode(25) │ age(4) │ biased(1) │ 01   │   │
│   │ 偏向锁  │ threadId(54) │ epoch(2) │ age(4) │ 1│01  │   │
│   │ 轻量锁  │     Lock Record 指针 (62)         │  00  │   │
│   │ 重量锁  │      Monitor 指针 (62)            │  10  │   │
│   │ GC标记  │                                  │  11  │   │
│   └────────────────────────────────────────────────────┘   │
│                                                             │
│   锁升级过程: 无锁 → 偏向锁 → 轻量级锁 → 重量级锁              │
│                                                             │
│   ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐ │
│   │  无锁   │ ──→│ 偏向锁  │ ──→│ 轻量锁  │ ──→│ 重量锁  │ │
│   │ (无竞争) │    │ (单线程) │    │ (CAS自旋)│    │ (OS互斥)│ │
│   └─────────┘    └─────────┘    └─────────┘    └─────────┘ │
│                                                             │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                 ReentrantLock 实现原理                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   基于 AQS (AbstractQueuedSynchronizer)                     │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │                        AQS                          │  │
│   │   ┌─────────────────────────────────────────────┐   │  │
│   │   │  state = 0 (未锁) / >0 (重入次数)             │   │  │
│   │   └─────────────────────────────────────────────┘   │  │
│   │                         │                           │  │
│   │   ┌─────────────────────▼───────────────────────┐   │  │
│   │   │              CLH 等待队列                    │   │  │
│   │   │   head ←→ Node1 ←→ Node2 ←→ Node3 ←→ tail  │   │  │
│   │   └─────────────────────────────────────────────┘   │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   获取锁: CAS 修改 state，失败则入队等待                      │
│   释放锁: state - 1，state=0 时唤醒队首节点                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 选型指南

```
┌─────────────────────────────────────────────────────────────┐
│                       选型建议                               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   使用 synchronized:                                        │
│   ├── 代码简单，不需要高级特性                               │
│   ├── 不需要中断、超时获取锁                                 │
│   ├── 不需要公平锁                                          │
│   └── JDK 6+ 性能已优化，大多数场景足够                       │
│                                                             │
│   使用 ReentrantLock:                                       │
│   ├── 需要可中断的锁获取 (lockInterruptibly)                 │
│   ├── 需要超时获取锁 (tryLock with timeout)                  │
│   ├── 需要公平锁                                            │
│   ├── 需要多个条件变量 (Condition)                           │
│   ├── 需要读写分离 (ReentrantReadWriteLock)                  │
│   └── 需要查询锁状态 (isLocked, getHoldCount 等)             │
│                                                             │
│   ⚠️ 注意: ReentrantLock 必须在 finally 中释放锁！           │
└─────────────────────────────────────────────────────────────┘
```

## 最佳实践

### ✅ 推荐做法

```java
// 1. ReentrantLock 标准用法 - 必须 try-finally
ReentrantLock lock = new ReentrantLock();
lock.lock();
try {
    // 业务逻辑
} finally {
    lock.unlock();  // 必须释放！
}

// 2. 使用 tryLock 避免死锁
public void transferMoney(Account from, Account to, int amount) {
    while (true) {
        if (from.lock.tryLock()) {
            try {
                if (to.lock.tryLock()) {
                    try {
                        // 执行转账
                        from.balance -= amount;
                        to.balance += amount;
                        return;
                    } finally {
                        to.lock.unlock();
                    }
                }
            } finally {
                from.lock.unlock();
            }
        }
        // 没拿到锁，随机等待后重试（避免活锁）
        Thread.sleep(new Random().nextInt(10));
    }
}

// 3. 简单场景优先 synchronized
public synchronized void simpleIncrement() {
    count++;
}
```

### ❌ 避免做法

```java
// 1. 忘记在 finally 中释放锁
public void badCode() {
    lock.lock();
    doSomething();  // 如果这里抛异常，锁永远不会释放！
    lock.unlock();
}

// 2. lock 在 try 块内获取
public void wrongPosition() {
    try {
        lock.lock();  // 应该在 try 外
        doSomething();
    } finally {
        lock.unlock();  // 如果 lock() 前就异常，这里会报错
    }
}

// 3. 滥用公平锁
ReentrantLock lock = new ReentrantLock(true);  // 无特殊需求不用公平锁
```

## 面试回答

### 30秒版本

> **synchronized** 是 JVM 关键字，自动获取释放锁，简单但功能有限。**ReentrantLock** 是 JDK API，需手动 lock/unlock，但支持**可中断、超时获取、公平锁、多条件变量**等高级特性。JDK 6 后两者性能接近，简单场景用 synchronized，需要高级特性用 ReentrantLock。

### 1分钟版本

> **主要区别**：
>
> | 特性 | synchronized | ReentrantLock |
> |------|--------------|---------------|
> | 实现层面 | JVM 关键字 | JDK Lock 接口 |
> | 锁释放 | 自动 | 手动（finally 必须释放）|
> | 可中断 | ❌ | ✅ lockInterruptibly() |
> | 超时 | ❌ | ✅ tryLock(timeout) |
> | 公平锁 | ❌ | ✅ new ReentrantLock(true) |
> | 条件变量 | 单一 wait/notify | 多个 Condition |
>
> **实现原理**：
> - synchronized：对象头 Mark Word + Monitor，有偏向锁→轻量锁→重量锁升级
> - ReentrantLock：基于 AQS，state 记录重入次数，CLH 队列管理等待线程
>
> **选型建议**：简单场景用 synchronized（代码简洁），需要中断/超时/公平锁/多条件时用 ReentrantLock。

---

*关联文档：[aqs.md](aqs.md) | [synchronized-implementation.md](synchronized-implementation.md) | [lock-optimization.md](lock-optimization.md)*
