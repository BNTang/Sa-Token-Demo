# 锁优化规范

> Java 并发编程规范 - 锁升级、自旋优化与性能调优策略

---

## 1. 锁升级机制

### 1.1 核心语义

| 要素 | 说明 |
|------|------|
| **是什么** | JVM 根据竞争情况自动调整锁的实现方式 |
| **解决什么** | 避免一开始就使用重量级锁的性能损失 |
| **方向** | 单向升级，不降级（无锁 → 偏向锁 → 轻量级锁 → 重量级锁） |

### 1.2 升级流程

```
┌─────────┐   首次获取    ┌─────────┐   其他线程竞争   ┌──────────┐   CAS失败/自旋超限   ┌──────────┐
│  无锁   │ ───────────► │ 偏向锁  │ ───────────────► │ 轻量级锁 │ ─────────────────► │ 重量级锁 │
└─────────┘              └─────────┘                 └──────────┘                    └──────────┘
     │                       │                            │                              │
   无竞争                  单线程                      交替执行                       真正竞争
```

### 1.3 各级锁详解

#### 偏向锁（Biased Locking）

| 属性 | 说明 |
|------|------|
| **场景** | 只有一个线程反复访问同步块 |
| **原理** | Mark Word 记录线程 ID，后续进入直接通过 |
| **开销** | 几乎为零（只比较线程 ID） |
| **撤销时机** | 其他线程尝试获取锁时 |
| **JVM 参数** | `-XX:+UseBiasedLocking`（JDK 15 后默认禁用） |

#### 轻量级锁（Lightweight Locking）

| 属性 | 说明 |
|------|------|
| **场景** | 多线程交替执行，无实际竞争 |
| **原理** | CAS 将 Mark Word 替换为指向栈中锁记录的指针 |
| **自旋** | CAS 失败后自旋等待 |
| **开销** | CAS 操作 + 可能的自旋 |
| **膨胀时机** | 自旋超过阈值或有第三个线程竞争 |

#### 重量级锁（Heavyweight Locking）

| 属性 | 说明 |
|------|------|
| **场景** | 真正的多线程竞争 |
| **原理** | Mark Word 指向操作系统 Monitor 对象 |
| **开销** | 线程阻塞/唤醒，用户态/内核态切换 |
| **降级** | **不会降级**，即使所有线程释放锁 |

### 1.4 Mark Word 结构（64 位 JVM）

| 锁状态 | 存储内容 | 标志位 |
|--------|---------|--------|
| 无锁 | hashCode、GC 年龄 | 001 |
| 偏向锁 | 线程 ID、Epoch、GC 年龄 | 101 |
| 轻量级锁 | 指向栈中锁记录的指针 | 00 |
| 重量级锁 | 指向 Monitor 的指针 | 10 |
| GC 标记 | 空 | 11 |

---

## 2. 自旋锁与自适应自旋

### 2.1 自旋锁

| 属性 | 说明 |
|------|------|
| **是什么** | 获取锁失败时不阻塞，循环尝试 |
| **优点** | 避免线程切换开销 |
| **缺点** | 占用 CPU 资源 |
| **适用场景** | 锁持有时间短 |

### 2.2 自适应自旋

| 属性 | 说明 |
|------|------|
| **JDK 版本** | JDK 6+ |
| **原理** | 根据上次自旋结果动态调整自旋次数 |
| **成功后** | 增加自旋次数（预期仍会成功） |
| **失败后** | 减少自旋甚至跳过（直接阻塞更划算） |

### 2.3 决策逻辑

```
获取锁失败
    │
    ▼
上次自旋成功？
├── 是 → 增加自旋次数，继续自旋
└── 否 → 曾经自旋成功过？
          ├── 是 → 减少自旋次数
          └── 否 → 直接阻塞（放弃自旋）
```

---

## 3. JVM 锁优化

### 3.1 锁消除（Lock Elimination）

| 属性 | 说明 |
|------|------|
| **是什么** | JIT 编译器消除不可能存在共享竞争的锁 |
| **前提** | 逃逸分析证明对象只在当前线程使用 |
| **JVM 参数** | `-XX:+EliminateLocks`（默认开启） |

```java
// JIT 会消除这个锁，因为 sb 不会逃逸
public String concat(String s1, String s2) {
    StringBuffer sb = new StringBuffer();  // sb 局部变量，不会逃逸
    sb.append(s1);  // append 是 synchronized 的
    sb.append(s2);  // 但 JIT 会消除锁
    return sb.toString();
}
```

### 3.2 锁粗化（Lock Coarsening）

| 属性 | 说明 |
|------|------|
| **是什么** | 将连续的加锁/解锁操作合并为一次 |
| **目的** | 减少锁操作的频率 |
| **JVM 参数** | `-XX:+EliminateLocks`（默认开启） |

```java
// 优化前：多次加锁/解锁
for (int i = 0; i < 100; i++) {
    synchronized(lock) {
        doSomething();
    }
}

// 优化后：一次加锁/解锁（锁粗化）
synchronized(lock) {
    for (int i = 0; i < 100; i++) {
        doSomething();
    }
}
```

---

## 4. 代码级锁优化策略

### 4.1 减小锁粒度

```java
// ❌ 错误：锁粒度太大
public synchronized void process(List<Task> tasks) {
    for (Task task : tasks) {
        heavyComputation(task);  // 耗时操作也被锁住
        updateResult(task);
    }
}

// ✅ 正确：只锁必要的部分
public void process(List<Task> tasks) {
    for (Task task : tasks) {
        Result result = heavyComputation(task);  // 不需要锁
        synchronized(this) {
            updateResult(task, result);  // 只锁共享资源操作
        }
    }
}
```

### 4.2 减少锁持有时间

```java
// ❌ 错误：锁持有时间长
public synchronized void doWithLock() {
    prepareData();      // 可以在锁外执行
    accessSharedData(); // 需要锁
    cleanUp();          // 可以在锁外执行
}

// ✅ 正确：最小化锁持有时间
public void doWithLock() {
    prepareData();
    synchronized(this) {
        accessSharedData();
    }
    cleanUp();
}
```

### 4.3 锁分离

```java
// ❌ 错误：读写都用同一把锁
public synchronized Object read() { ... }
public synchronized void write(Object o) { ... }

// ✅ 正确：读写分离
private ReadWriteLock rwLock = new ReentrantReadWriteLock();

public Object read() {
    rwLock.readLock().lock();
    try { ... }
    finally { rwLock.readLock().unlock(); }
}

public void write(Object o) {
    rwLock.writeLock().lock();
    try { ... }
    finally { rwLock.writeLock().unlock(); }
}
```

### 4.4 锁分段（Lock Striping）

```java
// ConcurrentHashMap 的设计思想
// 将数据分成多个段，每段一把锁

public class StripedMap<K, V> {
    private static final int N_LOCKS = 16;
    private final Object[] locks = new Object[N_LOCKS];
    private final Map<K, V>[] buckets;
    
    private Object lockFor(K key) {
        return locks[Math.abs(key.hashCode() % N_LOCKS)];
    }
    
    public V get(K key) {
        synchronized(lockFor(key)) {
            return buckets[bucketIndex(key)].get(key);
        }
    }
}
```

### 4.5 使用无锁数据结构

| 场景 | 推荐方案 |
|------|---------|
| 计数器 | AtomicInteger、AtomicLong、LongAdder |
| 引用更新 | AtomicReference |
| 数组元素更新 | AtomicIntegerArray、AtomicReferenceArray |
| 字段更新 | AtomicIntegerFieldUpdater |
| 高并发计数 | LongAdder（比 AtomicLong 更高效） |

```java
// ✅ 使用 Atomic 类替代锁
private AtomicInteger counter = new AtomicInteger(0);

public void increment() {
    counter.incrementAndGet();  // CAS 操作，无锁
}
```

---

## 5. 避免死锁

### 5.1 死锁的四个必要条件

| 条件 | 说明 |
|------|------|
| 互斥 | 资源只能被一个线程持有 |
| 持有并等待 | 持有资源的同时等待其他资源 |
| 不可剥夺 | 已持有的资源不能被强制释放 |
| 循环等待 | 形成资源等待环 |

### 5.2 预防死锁的策略

| 策略 | 实现方式 |
|------|---------|
| **破坏循环等待** | 按固定顺序获取锁 |
| **破坏持有并等待** | 一次性获取所有锁 |
| **使用超时** | tryLock(timeout) |
| **死锁检测** | 定期检测并恢复 |

```java
// ✅ 按固定顺序获取锁
public void transfer(Account from, Account to, int amount) {
    // 按账户 ID 排序，保证顺序一致
    Account first = from.id < to.id ? from : to;
    Account second = from.id < to.id ? to : from;
    
    synchronized(first) {
        synchronized(second) {
            // 转账操作
        }
    }
}

// ✅ 使用 tryLock 超时
public boolean transfer(Account from, Account to, int amount) {
    while (true) {
        if (from.lock.tryLock(100, TimeUnit.MILLISECONDS)) {
            try {
                if (to.lock.tryLock(100, TimeUnit.MILLISECONDS)) {
                    try {
                        // 转账操作
                        return true;
                    } finally {
                        to.lock.unlock();
                    }
                }
            } finally {
                from.lock.unlock();
            }
        }
        // 短暂休眠后重试
        Thread.sleep(10);
    }
}
```

---

## 6. 性能对比与选择

### 6.1 锁类型性能对比

| 锁类型 | 适用场景 | 性能特点 |
|--------|---------|---------|
| synchronized | 简单同步 | JDK 6+ 优化好，大多数场景首选 |
| ReentrantLock | 需要高级特性 | 竞争激烈时略优 |
| ReadWriteLock | 读多写少 | 读并发高 |
| StampedLock | 读多写少 + 乐观读 | 比 ReadWriteLock 更高效 |
| Atomic 类 | 单变量操作 | 无锁，最高效 |
| LongAdder | 高并发计数 | 比 AtomicLong 更高效 |

### 6.2 决策矩阵

| 场景 | 推荐方案 | 原因 |
|------|---------|------|
| 简单互斥 | synchronized | 简洁、自动释放 |
| 需要超时/中断 | ReentrantLock | 高级特性 |
| 读多写少 | ReadWriteLock | 读并发 |
| 计数器 | LongAdder | 高并发性能 |
| CAS 单变量 | Atomic 类 | 无锁 |
| 无共享状态 | ThreadLocal | 避免锁 |

---

## 7. 记忆锚点

| 概念 | 记忆句（≤20字） |
|------|----------------|
| 锁升级 | 偏→轻→重，单向升级不降级 |
| 偏向锁 | 一个人用，记 ID 就行 |
| 轻量级锁 | 交替用，CAS 抢 |
| 重量级锁 | 真竞争，排队等 |
| 自适应自旋 | 上次成功多转，失败少转 |
| 锁消除 | JIT 发现不逃逸，直接删锁 |
| 锁粗化 | 连续小锁合成一把大锁 |
| 锁优化核心 | 减粒度、减时间、分离、无锁 |
| 死锁预防 | 固定顺序 + 超时机制 |
