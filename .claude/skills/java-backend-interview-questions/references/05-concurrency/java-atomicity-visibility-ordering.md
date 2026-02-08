# Java 原子性、可见性、有序性

> 分类: Java 并发 | 难度: ⭐⭐⭐⭐ | 频率: 高频

---

## 一、三大特性概述

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                    并发编程三大特性                                               │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────────┐ │
│  │                         原子性 (Atomicity)                                   │ │
│  │  ─────────────────────────────────────────────────────────────────────────  │ │
│  │  定义: 一个操作是不可分割的，要么全部执行成功，要么全部不执行                  │ │
│  │  问题: count++ 实际是 读取-修改-写入 三步操作，非原子                         │ │
│  │  保证: synchronized、Lock、Atomic类                                          │ │
│  └─────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────────┐ │
│  │                         可见性 (Visibility)                                  │ │
│  │  ─────────────────────────────────────────────────────────────────────────  │ │
│  │  定义: 一个线程对共享变量的修改，其他线程能够立即看到                         │ │
│  │  问题: 每个线程有自己的工作内存(CPU缓存)，修改后不一定写回主内存               │ │
│  │  保证: volatile、synchronized、Lock、final                                   │ │
│  └─────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────────┐ │
│  │                         有序性 (Ordering)                                    │ │
│  │  ─────────────────────────────────────────────────────────────────────────  │ │
│  │  定义: 程序执行的顺序按照代码的先后顺序执行                                   │ │
│  │  问题: 编译器和处理器会进行指令重排序优化                                     │ │
│  │  保证: volatile、synchronized、Lock、happens-before规则                       │ │
│  └─────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 二、原子性详解

### 2.1 非原子操作问题

```java
public class AtomicityProblem {
    private int count = 0;
    
    // count++ 实际是三步操作，非原子
    public void increment() {
        count++;  
        // 1. 读取 count 到工作内存
        // 2. count + 1
        // 3. 写回主内存
    }
}
```

```
线程A和线程B同时执行 count++:

初始: count = 0

线程A                           线程B
───────────────────────────────────────────────
读取 count = 0                  
                               读取 count = 0
count = 0 + 1 = 1              
                               count = 0 + 1 = 1
写回 count = 1                 
                               写回 count = 1

结果: count = 1 (期望是2)
```

### 2.2 原子性保证方案

```java
// 方案1: synchronized
public class SynchronizedSolution {
    private int count = 0;
    
    public synchronized void increment() {
        count++;
    }
}

// 方案2: Lock
public class LockSolution {
    private int count = 0;
    private final Lock lock = new ReentrantLock();
    
    public void increment() {
        lock.lock();
        try {
            count++;
        } finally {
            lock.unlock();
        }
    }
}

// 方案3: Atomic类 (推荐)
public class AtomicSolution {
    private AtomicInteger count = new AtomicInteger(0);
    
    public void increment() {
        count.incrementAndGet();  // CAS原子操作
    }
}
```

---

## 三、可见性详解

### 3.1 可见性问题

```java
public class VisibilityProblem {
    private boolean running = true;
    
    public void stop() {
        running = false;  // 线程B修改
    }
    
    public void run() {
        while (running) {  // 线程A可能永远看不到修改
            // do something
        }
    }
}
```

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                          Java 内存模型 (JMM)                                     │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│     线程A                          主内存                        线程B          │
│   ┌─────────┐                   ┌─────────┐                  ┌─────────┐       │
│   │工作内存  │                   │         │                  │工作内存  │       │
│   │         │                   │ running │                  │         │       │
│   │running  │←───── read ──────│  = true │────── read ─────→│running  │       │
│   │ = true  │                   │    ↓    │                  │ = true  │       │
│   └─────────┘                   │ = false │                  └─────────┘       │
│       ↑                         └─────────┘                       │            │
│       │                              ↑                            │            │
│       │                              │                            ↓            │
│   线程A一直读取                  线程B写回主内存             线程B本地修改        │
│   本地缓存的true                                           running = false    │
│                                                                                 │
│   问题: 线程B的修改没有及时同步到线程A的工作内存                                 │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 可见性保证方案

```java
// 方案1: volatile (推荐用于状态标志)
public class VolatileSolution {
    private volatile boolean running = true;
    
    public void stop() {
        running = false;  // 立即刷新到主内存
    }
    
    public void run() {
        while (running) {  // 每次从主内存读取
            // do something
        }
    }
}

// 方案2: synchronized
public class SynchronizedVisibility {
    private boolean running = true;
    
    public synchronized void stop() {
        running = false;
    }
    
    public void run() {
        while (true) {
            synchronized (this) {
                if (!running) break;
            }
        }
    }
}
```

---

## 四、有序性详解

### 4.1 指令重排序问题

```java
public class ReorderingProblem {
    private int a = 0;
    private boolean flag = false;
    
    public void writer() {
        a = 1;        // 1
        flag = true;  // 2
    }
    
    public void reader() {
        if (flag) {     // 3
            int i = a;  // 4  可能读到 a = 0!
        }
    }
}
```

```
正常预期顺序: 1 → 2 → 3 → 4

实际可能的重排序:
┌────────────────────────────────────────────────────────────────┐
│  线程A (writer)           │  线程B (reader)                    │
├───────────────────────────┼────────────────────────────────────┤
│  flag = true  (重排到前面) │                                    │
│                           │  if (flag) → true                  │
│                           │  int i = a → 读到 0!               │
│  a = 1                    │                                    │
└───────────────────────────┴────────────────────────────────────┘

问题: 步骤1和2没有数据依赖，编译器/CPU可能重排序
```

### 4.2 典型案例：双重检查锁单例

```java
// ❌ 错误实现 (有重排序问题)
public class UnsafeSingleton {
    private static UnsafeSingleton instance;
    
    public static UnsafeSingleton getInstance() {
        if (instance == null) {
            synchronized (UnsafeSingleton.class) {
                if (instance == null) {
                    instance = new UnsafeSingleton();
                    // new 操作可能被重排序:
                    // 1. 分配内存
                    // 2. 初始化对象
                    // 3. 引用赋值给 instance
                    // 可能变成 1 → 3 → 2
                    // 另一个线程可能拿到未初始化的对象!
                }
            }
        }
        return instance;
    }
}

// ✅ 正确实现 (volatile禁止重排序)
public class SafeSingleton {
    private static volatile SafeSingleton instance;
    
    public static SafeSingleton getInstance() {
        if (instance == null) {
            synchronized (SafeSingleton.class) {
                if (instance == null) {
                    instance = new SafeSingleton();  // volatile写，禁止重排序
                }
            }
        }
        return instance;
    }
}
```

### 4.3 有序性保证方案

```java
// 1. volatile: 禁止指令重排序
private volatile int value;

// 2. synchronized: 保证同一时刻只有一个线程执行
synchronized (lock) {
    // 这里面的代码不会和外面的代码重排序
}

// 3. happens-before 规则 (JMM 保证)
```

---

## 五、happens-before 规则

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                        happens-before 规则                                        │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  如果操作A happens-before 操作B，则A的执行结果对B可见，且A的执行顺序在B之前        │
│                                                                                  │
│  8条规则:                                                                        │
│  ┌────────────────────────────────────────────────────────────────────────────┐  │
│  │ 1. 程序顺序规则: 同一线程中，前面的操作 happens-before 后面的操作           │  │
│  │ 2. 监视器锁规则: unlock happens-before 后续的 lock                         │  │
│  │ 3. volatile规则: volatile写 happens-before 后续的 volatile读               │  │
│  │ 4. 线程启动规则: Thread.start() happens-before 线程中的任何操作            │  │
│  │ 5. 线程终止规则: 线程中的任何操作 happens-before Thread.join()              │  │
│  │ 6. 线程中断规则: interrupt() happens-before 被中断线程检测到中断           │  │
│  │ 7. 对象终结规则: 构造函数执行完成 happens-before finalize()                 │  │
│  │ 8. 传递性规则:   A happens-before B，B happens-before C，则A hb C          │  │
│  └────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 六、三种保证方式对比

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          保证方式对比                                             │
├─────────────────┬─────────────┬─────────────┬─────────────┬──────────────────────┤
│                 │   原子性    │   可见性    │   有序性    │       使用场景       │
├─────────────────┼─────────────┼─────────────┼─────────────┼──────────────────────┤
│  synchronized   │     ✅      │     ✅      │     ✅      │  临界区保护          │
│  volatile       │     ❌*     │     ✅      │     ✅      │  状态标志            │
│  Atomic类       │     ✅      │     ✅      │     ❌      │  计数器              │
│  Lock           │     ✅      │     ✅      │     ✅      │  复杂同步场景        │
│  final          │     -       │     ✅**    │     ✅      │  不可变对象          │
└─────────────────┴─────────────┴─────────────┴─────────────┴──────────────────────┘

*  volatile 对单个读写是原子的，但复合操作(如i++)不是原子的
** final 只保证初始化完成后的可见性
```

---

## 七、代码示例

### 7.1 综合示例

```java
/**
 * 演示三大特性的保证方式
 */
public class ConcurrencyFeatureDemo {
    
    // 原子性: Atomic类
    private AtomicInteger counter = new AtomicInteger(0);
    
    // 可见性: volatile
    private volatile boolean running = true;
    
    // 有序性 + 原子性 + 可见性: synchronized
    private int data;
    
    public void increment() {
        counter.incrementAndGet();  // 原子操作
    }
    
    public void stop() {
        running = false;  // 可见性保证
    }
    
    public synchronized void setData(int value) {
        this.data = value;  // 三种特性都保证
    }
    
    public synchronized int getData() {
        return data;
    }
    
    public void work() {
        while (running) {  // 每次读取主内存
            increment();
            // do work
        }
    }
}
```

### 7.2 实战: 安全发布对象

```java
/**
 * 安全发布对象示例
 */
public class SafePublication {
    
    // 方式1: volatile + 不可变对象
    private volatile ImmutableConfig config;
    
    public void updateConfig(ImmutableConfig newConfig) {
        this.config = newConfig;  // volatile写保证可见性
    }
    
    public ImmutableConfig getConfig() {
        return config;  // volatile读保证可见性
    }
    
    // 方式2: synchronized
    private Config mutableConfig;
    
    public synchronized void setMutableConfig(Config config) {
        this.mutableConfig = config;
    }
    
    public synchronized Config getMutableConfig() {
        return mutableConfig;
    }
    
    // 方式3: AtomicReference
    private final AtomicReference<Config> atomicConfig = new AtomicReference<>();
    
    public void casUpdateConfig(Config expect, Config update) {
        atomicConfig.compareAndSet(expect, update);
    }
}
```

---

## 八、面试回答

### 30秒版本

> Java 并发三大特性：
> - **原子性**：操作不可分割，用 synchronized/Lock/Atomic类 保证
> - **可见性**：一个线程修改对其他线程立即可见，用 volatile/synchronized 保证
> - **有序性**：禁止指令重排序，用 volatile/synchronized 保证
>
> volatile 保证可见性和有序性，但不保证复合操作的原子性。synchronized 三者都保证。

### 1分钟版本

> **原子性**：一个操作要么全部完成，要么完全不执行。count++ 不是原子操作，因为包含读取-修改-写入三步。使用 synchronized、Lock 或 Atomic 类可以保证原子性。
>
> **可见性**：线程修改共享变量后，其他线程能立即看到。由于 CPU 缓存的存在，线程可能读取到过期数据。volatile 可以保证可见性，每次读取都从主内存读，每次写入都刷新到主内存。
>
> **有序性**：编译器和 CPU 会进行指令重排序优化，可能导致执行顺序与代码顺序不一致。典型案例是双重检查锁单例模式，需要 volatile 禁止重排序。
>
> JMM 通过 happens-before 规则来定义操作间的可见性和有序性关系。

---

## 九、最佳实践

### ✅ 推荐做法

```java
// 1. 状态标志使用 volatile
private volatile boolean shutdown = false;

// 2. 计数器使用 Atomic 类
private AtomicLong counter = new AtomicLong();

// 3. 复合操作使用 synchronized
private int[] data;
public synchronized void updateData(int index, int value) {
    data[index] = value;
}

// 4. 单例使用 volatile + DCL 或 静态内部类
private static volatile Singleton instance;

// 5. 不可变对象使用 final
public final class ImmutableConfig {
    private final String value;
    // ...
}
```

### ❌ 避免做法

```java
// ❌ 用 volatile 保证复合操作原子性
private volatile int count;
public void increment() {
    count++;  // 不是原子操作!
}

// ❌ DCL 不使用 volatile
private static Singleton instance;  // 缺少 volatile

// ❌ 依赖普通变量的可见性
private boolean running = true;  // 其他线程可能看不到修改
```
