# AQS 原理

> Java 后端面试知识点 - 并发编程

---

## 什么是 AQS

AQS（AbstractQueuedSynchronizer）是 Java 并发包的核心框架，用于构建锁和同步器。

### 基于 AQS 实现的同步工具

| 工具 | 说明 |
|------|------|
| **ReentrantLock** | 可重入锁 |
| **ReentrantReadWriteLock** | 读写锁 |
| **Semaphore** | 信号量 |
| **CountDownLatch** | 倒计时门闩 |
| **CyclicBarrier** | 循环栅栏 |

---

## 核心原理

### 三大核心组件

```
┌─────────────────────────────────────────────────────────────────┐
│                            AQS                                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. state 变量（同步状态）                                        │
│     volatile int state;                                         │
│     └── 0 = 未锁定, >0 = 锁定次数                                │
│                                                                 │
│  2. CLH 双向队列（等待队列）                                      │
│     head ←→ Node1 ←→ Node2 ←→ Node3 ←→ tail                    │
│     └── 每个 Node 封装一个等待线程                               │
│                                                                 │
│  3. CAS 操作                                                    │
│     compareAndSetState(expect, update)                          │
│     └── 原子性修改 state                                         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### CLH 队列节点

```java
static final class Node {
    volatile int waitStatus;     // 等待状态
    volatile Node prev;          // 前驱节点
    volatile Node next;          // 后继节点
    volatile Thread thread;      // 等待的线程
    Node nextWaiter;             // 条件队列的下一个节点
    
    // waitStatus 的取值
    static final int CANCELLED =  1;  // 线程已取消
    static final int SIGNAL    = -1;  // 后继节点需要被唤醒
    static final int CONDITION = -2;  // 在条件队列中等待
    static final int PROPAGATE = -3;  // 共享模式下传播唤醒
}
```

---

## 加锁流程（以 ReentrantLock 为例）

```
线程 A 尝试获取锁
        ↓
CAS 尝试将 state 从 0 改为 1
        ↓
   成功？──是──> 获取锁成功，记录当前线程为持有者
        ↓ 否
   是否重入？──是──> state + 1，获取成功
        ↓ 否
创建 Node 节点，加入 CLH 队列尾部
        ↓
自旋检查：前驱是 head 且 tryAcquire 成功？
        ↓
   成功？──是──> 设置当前节点为 head，获取锁成功
        ↓ 否
LockSupport.park() 阻塞当前线程
        ↓
被唤醒后继续自旋...
```

### 源码分析

```java
// ReentrantLock.lock() 最终调用
public final void acquire(int arg) {
    if (!tryAcquire(arg) &&                    // 1. 尝试获取锁
        acquireQueued(addWaiter(Node.EXCLUSIVE), arg))  // 2. 入队等待
        selfInterrupt();
}

// 尝试获取锁（非公平锁）
protected final boolean tryAcquire(int acquires) {
    final Thread current = Thread.currentThread();
    int c = getState();
    
    if (c == 0) {  // 锁空闲
        if (compareAndSetState(0, acquires)) {  // CAS 抢锁
            setExclusiveOwnerThread(current);   // 记录持有者
            return true;
        }
    } else if (current == getExclusiveOwnerThread()) {  // 重入
        int nextc = c + acquires;
        setState(nextc);
        return true;
    }
    return false;
}

// 入队并等待
final boolean acquireQueued(final Node node, int arg) {
    try {
        boolean interrupted = false;
        for (;;) {  // 自旋
            final Node p = node.predecessor();
            // 前驱是 head，尝试获取锁
            if (p == head && tryAcquire(arg)) {
                setHead(node);
                p.next = null;  // help GC
                return interrupted;
            }
            // 阻塞当前线程
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                interrupted = true;
        }
    } catch (Throwable t) {
        cancelAcquire(node);
        throw t;
    }
}
```

---

## 释放锁流程

```
线程 A 释放锁
        ↓
state - 1
        ↓
   state == 0？──否──> 返回（重入未完全释放）
        ↓ 是
清除持有者线程
        ↓
唤醒 CLH 队列的下一个节点
LockSupport.unpark(node.thread)
```

### 源码分析

```java
public final boolean release(int arg) {
    if (tryRelease(arg)) {          // 尝试释放
        Node h = head;
        if (h != null && h.waitStatus != 0)
            unparkSuccessor(h);      // 唤醒后继节点
        return true;
    }
    return false;
}

protected final boolean tryRelease(int releases) {
    int c = getState() - releases;
    if (Thread.currentThread() != getExclusiveOwnerThread())
        throw new IllegalMonitorStateException();
    boolean free = false;
    if (c == 0) {  // 完全释放
        free = true;
        setExclusiveOwnerThread(null);
    }
    setState(c);
    return free;
}
```

---

## 公平锁 vs 非公平锁

| 特性 | 公平锁 | 非公平锁 |
|------|--------|---------|
| **获取顺序** | 按照请求顺序（FIFO） | 可以插队 |
| **性能** | 较低（频繁线程切换） | 较高 |
| **饥饿问题** | 不会 | 可能 |

```java
// 非公平锁：直接尝试 CAS，不检查队列
final boolean nonfairTryAcquire(int acquires) {
    if (compareAndSetState(0, acquires)) {  // 直接抢
        setExclusiveOwnerThread(current);
        return true;
    }
    // ...
}

// 公平锁：检查队列中是否有前驱
protected final boolean tryAcquire(int acquires) {
    if (c == 0) {
        if (!hasQueuedPredecessors() &&      // 检查队列
            compareAndSetState(0, acquires)) {
            setExclusiveOwnerThread(current);
            return true;
        }
    }
    // ...
}
```

---

## 自定义同步器

```java
// 自定义互斥锁
public class MyMutex implements Lock {
    
    private final Sync sync = new Sync();
    
    private static class Sync extends AbstractQueuedSynchronizer {
        
        @Override
        protected boolean tryAcquire(int arg) {
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }
        
        @Override
        protected boolean tryRelease(int arg) {
            if (getState() == 0) {
                throw new IllegalMonitorStateException();
            }
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }
        
        @Override
        protected boolean isHeldExclusively() {
            return getState() == 1;
        }
        
        Condition newCondition() {
            return new ConditionObject();
        }
    }
    
    @Override
    public void lock() {
        sync.acquire(1);
    }
    
    @Override
    public void unlock() {
        sync.release(1);
    }
    
    @Override
    public boolean tryLock() {
        return sync.tryAcquire(1);
    }
    
    @Override
    public Condition newCondition() {
        return sync.newCondition();
    }
    
    // ... 其他方法
}
```

---

## 面试要点

### 核心答案

**问：说说 AQS 吧？**

答：

**什么是 AQS**：
AQS（AbstractQueuedSynchronizer）是 Java 并发包的核心框架，用于构建锁和同步器。ReentrantLock、Semaphore、CountDownLatch 都基于它实现。

**三大核心组件**：

1. **state 变量**
   - `volatile int state` 表示同步状态
   - 0 表示未锁定，>0 表示锁定次数（支持重入）
   - 通过 CAS 原子修改

2. **CLH 双向队列**
   - 先进先出的等待队列
   - 每个节点封装一个等待线程
   - 获取锁失败的线程会入队等待

3. **CAS 操作**
   - `compareAndSetState()` 原子修改 state
   - 无锁化竞争，保证线程安全

**工作流程**：
- 加锁：CAS 修改 state → 失败则入队 → 阻塞等待 → 被唤醒后重试
- 解锁：state 减 1 → 为 0 则唤醒队列头节点

---

## 编码最佳实践

### ✅ 推荐做法

```java
// 1. 使用 ReentrantLock 替代 synchronized（需要高级功能时）
private final ReentrantLock lock = new ReentrantLock();

public void method() {
    lock.lock();
    try {
        // 业务逻辑
    } finally {
        lock.unlock();  // 必须在 finally 中释放
    }
}

// 2. 使用 tryLock 避免死锁
if (lock.tryLock(3, TimeUnit.SECONDS)) {
    try {
        // 业务逻辑
    } finally {
        lock.unlock();
    }
} else {
    // 获取锁超时处理
}

// 3. 使用 Condition 实现等待通知
private final Condition condition = lock.newCondition();

public void await() throws InterruptedException {
    lock.lock();
    try {
        while (!conditionMet) {
            condition.await();
        }
    } finally {
        lock.unlock();
    }
}

public void signal() {
    lock.lock();
    try {
        conditionMet = true;
        condition.signalAll();
    } finally {
        lock.unlock();
    }
}
```

### ❌ 避免做法

```java
// ❌ 忘记释放锁
lock.lock();
doSomething();  // 如果抛异常，锁不会释放
lock.unlock();

// ❌ 释放锁不在 finally 中
lock.lock();
try {
    doSomething();
    lock.unlock();  // 应该在 finally 中
} catch (Exception e) {
    // 异常时锁没释放
}

// ❌ 使用公平锁（除非必要）
new ReentrantLock(true);  // 性能较低
```
