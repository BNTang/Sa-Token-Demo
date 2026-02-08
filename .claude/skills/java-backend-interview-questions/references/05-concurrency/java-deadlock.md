# Java 死锁详解

> 分类: Java 并发 | 难度: ⭐⭐⭐⭐ | 频率: 高频

---

## 一、什么是死锁

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          死锁定义                                                 │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  死锁: 两个或多个线程相互等待对方持有的资源，导致所有线程都无法继续执行           │
│                                                                                  │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │                                                                            │ │
│  │   线程A                                  线程B                             │ │
│  │     │                                      │                               │ │
│  │     │ 持有锁1                              │ 持有锁2                       │ │
│  │     │    │                                 │    │                          │ │
│  │     ↓    │                                 ↓    │                          │ │
│  │  请求锁2 │                              请求锁1 │                          │ │
│  │     ↓    │                                 ↓    │                          │ │
│  │   等待... ←────── 相互等待 ──────→       等待...                           │ │
│  │                                                                            │ │
│  │   结果: 两个线程都无法继续，程序卡死                                       │ │
│  │                                                                            │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 二、死锁的四个必要条件

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                    死锁的四个必要条件 (缺一不可)                                  │
├─────────────────────┬────────────────────────────────────────────────────────────┤
│  1. 互斥条件         │  资源只能被一个线程持有，其他线程必须等待                   │
├─────────────────────┼────────────────────────────────────────────────────────────┤
│  2. 持有并等待       │  线程持有资源的同时，又在等待其他资源                       │
├─────────────────────┼────────────────────────────────────────────────────────────┤
│  3. 不可抢占         │  线程持有的资源不能被其他线程强行夺取                       │
├─────────────────────┼────────────────────────────────────────────────────────────┤
│  4. 循环等待         │  存在线程的循环等待链 (A等B，B等C，C等A)                    │
└─────────────────────┴────────────────────────────────────────────────────────────┘

破坏任意一个条件即可避免死锁
```

---

## 三、死锁代码示例

```java
/**
 * 典型死锁示例
 */
public class DeadlockExample {
    
    private static final Object lockA = new Object();
    private static final Object lockB = new Object();
    
    public static void main(String[] args) {
        // 线程1: 先获取 lockA，再获取 lockB
        Thread t1 = new Thread(() -> {
            synchronized (lockA) {
                System.out.println("线程1 获取了 lockA");
                try { Thread.sleep(100); } catch (InterruptedException e) {}
                
                System.out.println("线程1 等待 lockB...");
                synchronized (lockB) {
                    System.out.println("线程1 获取了 lockB");
                }
            }
        });
        
        // 线程2: 先获取 lockB，再获取 lockA
        Thread t2 = new Thread(() -> {
            synchronized (lockB) {
                System.out.println("线程2 获取了 lockB");
                try { Thread.sleep(100); } catch (InterruptedException e) {}
                
                System.out.println("线程2 等待 lockA...");
                synchronized (lockA) {
                    System.out.println("线程2 获取了 lockA");
                }
            }
        });
        
        t1.start();
        t2.start();
        
        // 结果: 两个线程相互等待，程序卡死
    }
}
```

---

## 四、如何避免死锁

### 4.1 固定加锁顺序

```java
/**
 * 方案1: 固定加锁顺序 (破坏循环等待)
 */
public class FixedOrderLock {
    
    private static final Object lockA = new Object();
    private static final Object lockB = new Object();
    
    public void method1() {
        // 所有线程都按 A → B 的顺序加锁
        synchronized (lockA) {
            synchronized (lockB) {
                // 业务逻辑
            }
        }
    }
    
    public void method2() {
        // 同样按 A → B 的顺序，不会死锁
        synchronized (lockA) {
            synchronized (lockB) {
                // 业务逻辑
            }
        }
    }
}

/**
 * 转账场景: 按账户ID大小排序，固定顺序
 */
public void transfer(Account from, Account to, int amount) {
    // 按账户ID排序，保证加锁顺序一致
    Account first = from.getId() < to.getId() ? from : to;
    Account second = from.getId() < to.getId() ? to : from;
    
    synchronized (first) {
        synchronized (second) {
            from.deduct(amount);
            to.add(amount);
        }
    }
}
```

### 4.2 使用 tryLock 超时

```java
/**
 * 方案2: 使用 tryLock + 超时 (破坏不可抢占)
 */
public class TryLockExample {
    
    private final Lock lockA = new ReentrantLock();
    private final Lock lockB = new ReentrantLock();
    
    public void safeMethod() {
        while (true) {
            if (lockA.tryLock()) {
                try {
                    if (lockB.tryLock(1, TimeUnit.SECONDS)) {
                        try {
                            // 成功获取两把锁，执行业务
                            doWork();
                            return;
                        } finally {
                            lockB.unlock();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    lockA.unlock();
                }
            }
            // 获取失败，随机等待后重试
            try {
                Thread.sleep(new Random().nextInt(100));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
```

### 4.3 一次性获取所有资源

```java
/**
 * 方案3: 一次性获取所有资源 (破坏持有并等待)
 */
public class AllOrNothingLock {
    
    private final Lock globalLock = new ReentrantLock();
    private final Object resourceA = new Object();
    private final Object resourceB = new Object();
    
    public void doWork() {
        // 用一把大锁保护所有资源
        globalLock.lock();
        try {
            // 操作 resourceA 和 resourceB
            useResources();
        } finally {
            globalLock.unlock();
        }
    }
}
```

### 4.4 使用并发工具类

```java
/**
 * 方案4: 使用 java.util.concurrent 工具类
 */
public class ConcurrentToolsExample {
    
    // 使用 ConcurrentHashMap 代替 synchronized + HashMap
    private final ConcurrentHashMap<String, Object> map = new ConcurrentHashMap<>();
    
    // 使用 AtomicInteger 代替 synchronized 计数
    private final AtomicInteger counter = new AtomicInteger(0);
    
    // 使用线程安全的队列
    private final BlockingQueue<Task> queue = new LinkedBlockingQueue<>();
}
```

---

## 五、死锁检测与诊断

### 5.1 使用 jstack

```bash
# 获取线程 dump
jstack <pid>

# 输出示例:
Found one Java-level deadlock:
=============================
"Thread-1":
  waiting to lock monitor 0x00007f8a1c003828 (object 0x000000076ab96c80)
  which is held by "Thread-0"
"Thread-0":
  waiting to lock monitor 0x00007f8a1c0062c8 (object 0x000000076ab96c90)
  which is held by "Thread-1"
```

### 5.2 代码检测

```java
/**
 * 使用 ThreadMXBean 检测死锁
 */
public class DeadlockDetector {
    
    public void detectDeadlock() {
        ThreadMXBean mxBean = ManagementFactory.getThreadMXBean();
        long[] deadlockedThreadIds = mxBean.findDeadlockedThreads();
        
        if (deadlockedThreadIds != null) {
            ThreadInfo[] threadInfos = mxBean.getThreadInfo(deadlockedThreadIds);
            for (ThreadInfo info : threadInfos) {
                System.out.println("死锁线程: " + info.getThreadName());
                System.out.println("等待的锁: " + info.getLockName());
                System.out.println("持有锁的线程: " + info.getLockOwnerName());
            }
        }
    }
}
```

---

## 六、面试回答

### 30秒版本

> **死锁**：两个或多个线程相互等待对方持有的资源，导致所有线程都无法继续。
>
> **四个必要条件**：互斥、持有并等待、不可抢占、循环等待。
>
> **避免方法**：
> 1. 固定加锁顺序（破坏循环等待）
> 2. tryLock 超时（破坏不可抢占）
> 3. 一次性获取所有资源（破坏持有并等待）
>
> 检测：用 `jstack` 或 `ThreadMXBean.findDeadlockedThreads()`。

### 1分钟版本

> **什么是死锁**：
> 两个或多个线程互相持有对方需要的资源，又都在等待对方释放，导致所有线程都无法继续执行。
>
> **四个必要条件**：
> 1. 互斥条件：资源只能被一个线程持有
> 2. 持有并等待：线程持有资源的同时等待其他资源
> 3. 不可抢占：资源不能被强行夺取
> 4. 循环等待：存在线程等待环
>
> **导致死锁的场景**：
> - 多线程以不同顺序获取多把锁
> - 转账操作中 A→B 和 B→A 同时进行
>
> **避免方法**：
> 1. **固定加锁顺序**：所有线程按相同顺序获取锁
> 2. **tryLock 超时**：获取不到就放弃重试
> 3. **一次性获取**：用大锁包住所有资源
> 4. **使用并发工具类**：ConcurrentHashMap、Atomic 类等
>
> **检测工具**：
> - `jstack <pid>` 查看线程状态
> - `ThreadMXBean.findDeadlockedThreads()` 代码检测
