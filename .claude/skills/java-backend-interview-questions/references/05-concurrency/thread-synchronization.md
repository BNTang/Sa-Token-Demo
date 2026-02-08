# 什么是 Java 中的线程同步？

> 线程同步是控制多个线程按顺序访问共享资源的机制，防止数据不一致和竞态条件

## 30秒速答

线程同步的核心：
- **目的**: 保证共享资源的正确访问，避免数据不一致
- **手段**: synchronized、Lock、volatile、原子类等
- **本质**: 互斥访问 + 可见性保证
- **代价**: 性能开销（上下文切换、锁竞争）

## 一分钟详解

### 为什么需要同步？

```java
// 没有同步：可能产生数据不一致
class Counter {
    private int count = 0;
    
    public void increment() {
        count++;  // 非原子操作：读取→加1→写入
    }
}

Counter counter = new Counter();
// 两个线程各执行 1000 次 increment
// 预期结果: 2000
// 实际结果: 可能是 1500、1800 等任意值

// 问题分析：
// Thread A: 读取 count=100
// Thread B: 读取 count=100
// Thread A: 写入 count=101
// Thread B: 写入 count=101  ← 丢失一次更新！
```

### 主要同步方式

```java
// 1. synchronized 关键字
public class SyncCounter {
    private int count = 0;
    
    // 同步方法
    public synchronized void increment() {
        count++;
    }
    
    // 同步代码块
    public void decrement() {
        synchronized (this) {
            count--;
        }
    }
}

// 2. ReentrantLock 显式锁
public class LockCounter {
    private int count = 0;
    private final ReentrantLock lock = new ReentrantLock();
    
    public void increment() {
        lock.lock();
        try {
            count++;
        } finally {
            lock.unlock();  // 必须在 finally 中释放
        }
    }
}

// 3. volatile (保证可见性，不保证原子性)
public class VolatileFlag {
    private volatile boolean running = true;
    
    public void stop() {
        running = false;  // 立即对其他线程可见
    }
}

// 4. 原子类 (CAS 无锁)
public class AtomicCounter {
    private AtomicInteger count = new AtomicInteger(0);
    
    public void increment() {
        count.incrementAndGet();  // 原子操作
    }
}
```

### synchronized 原理

```
synchronized 锁升级过程：
┌───────────────────────────────────────────────────────┐
│  无锁 → 偏向锁 → 轻量级锁 → 重量级锁                   │
│                                                       │
│  偏向锁：单线程，记录线程ID，几乎无开销               │
│  轻量级锁：少量竞争，CAS自旋，避免阻塞                │
│  重量级锁：激烈竞争，阻塞等待，由OS调度               │
└───────────────────────────────────────────────────────┘

Monitor (监视器) 结构：
┌─────────────────────────┐
│  Owner: 持有锁的线程    │
│  EntryList: 等待锁队列  │
│  WaitSet: wait()等待队列│
└─────────────────────────┘
```

### 同步方式对比

| 特性 | synchronized | ReentrantLock | volatile | Atomic* |
|------|--------------|---------------|----------|---------|
| 类型 | 关键字 | 类 | 关键字 | 类 |
| 锁类型 | 隐式锁 | 显式锁 | 无锁 | CAS无锁 |
| 可中断 | 否 | 是 | - | - |
| 公平锁 | 否 | 可选 | - | - |
| 超时获取 | 否 | 是 | - | - |
| 原子性 | 是 | 是 | 否 | 是 |
| 可见性 | 是 | 是 | 是 | 是 |

### 常见同步场景

```java
// 场景1: 单例模式 (双重检查锁)
public class Singleton {
    private static volatile Singleton instance;
    
    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class) {
                if (instance == null) {
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}

// 场景2: 生产者消费者
public class ProducerConsumer {
    private final Queue<Integer> queue = new LinkedList<>();
    private final int MAX_SIZE = 10;
    
    public synchronized void produce(int item) throws InterruptedException {
        while (queue.size() == MAX_SIZE) {
            wait();  // 队列满，等待
        }
        queue.add(item);
        notifyAll();  // 通知消费者
    }
    
    public synchronized int consume() throws InterruptedException {
        while (queue.isEmpty()) {
            wait();  // 队列空，等待
        }
        int item = queue.poll();
        notifyAll();  // 通知生产者
        return item;
    }
}

// 场景3: 读写锁
public class Cache {
    private final Map<String, Object> map = new HashMap<>();
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    
    public Object get(String key) {
        rwLock.readLock().lock();  // 读锁，允许多个读
        try {
            return map.get(key);
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    public void put(String key, Object value) {
        rwLock.writeLock().lock();  // 写锁，独占
        try {
            map.put(key, value);
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
```

## 关键记忆点

```
┌─────────────────────────────────────────────────────┐
│  线程同步核心概念：                                  │
│                                                     │
│  ┌──────────────────────────────────────────────┐  │
│  │ 同步目的：互斥访问 + 可见性 + 有序性         │  │
│  └──────────────────────────────────────────────┘  │
│                                                     │
│  同步方式选择：                                     │
│  • synchronized → 简单场景，自动释放               │
│  • ReentrantLock → 需要超时/中断/公平锁           │
│  • volatile → 只需可见性，不需原子性              │
│  • Atomic* → 单变量原子操作                       │
│  • ReadWriteLock → 读多写少场景                   │
│                                                     │
│  注意事项：                                         │
│  ✓ 缩小同步范围，减少锁竞争                        │
│  ✓ 避免嵌套锁，防止死锁                            │
│  ✓ finally 中释放显式锁                            │
└─────────────────────────────────────────────────────┘
```

## 面试追问

**Q: synchronized 和 Lock 怎么选？**

- **synchronized**: 简单场景，代码简洁，JVM 优化好
- **Lock**: 需要超时获取、可中断、公平锁、多条件变量时使用
