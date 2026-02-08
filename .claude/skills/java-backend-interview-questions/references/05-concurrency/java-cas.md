# Java CAS 操作详解

> 分类: Java 并发 | 难度: ⭐⭐⭐⭐ | 频率: 高频

---

## 一、什么是 CAS

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                    CAS (Compare-And-Swap)                                         │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  定义: CAS 是一种无锁的原子操作，包含三个操作数:                                 │
│        V (内存值) - 要更新的变量                                                  │
│        E (期望值) - 认为变量应该是的值                                            │
│        N (新值)   - 想要更新成的值                                                │
│                                                                                  │
│  操作逻辑:                                                                       │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │  if (V == E) {                                                              │ │
│  │      V = N;           // 如果内存值等于期望值，更新为新值                   │ │
│  │      return true;     // 更新成功                                          │ │
│  │  } else {                                                                   │ │
│  │      return false;    // 更新失败，说明有其他线程修改了                     │ │
│  │  }                                                                          │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  关键: 这整个操作是原子的，由 CPU 硬件指令保证 (如 x86 的 CMPXCHG)               │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 二、CAS 执行过程

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          CAS 执行流程                                             │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  场景: 两个线程同时对 count 进行 +1 操作                                         │
│  初始: count = 0                                                                 │
│                                                                                  │
│  线程A                              线程B                                        │
│  ────────────────────────────────────────────────────────────────────────────    │
│  1. 读取 count = 0                  1. 读取 count = 0                            │
│  2. 计算 new = 0 + 1 = 1            2. 计算 new = 0 + 1 = 1                      │
│  3. CAS(count, 0, 1) → 成功         3. 等待...                                   │
│     count 现在 = 1                                                               │
│                                     4. CAS(count, 0, 1) → 失败!                  │
│                                        (内存值是1，不等于期望值0)                 │
│                                     5. 重新读取 count = 1                        │
│                                     6. 计算 new = 1 + 1 = 2                      │
│                                     7. CAS(count, 1, 2) → 成功                   │
│                                        count 现在 = 2                            │
│                                                                                  │
│  结果: count = 2 (正确!)                                                         │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 三、Unsafe 类与 CAS

```java
/**
 * Unsafe 类提供 CAS 操作
 */
public class UnsafeCasExample {
    
    private static final Unsafe unsafe;
    private static final long valueOffset;
    private volatile int value = 0;
    
    static {
        try {
            // 通过反射获取 Unsafe 实例
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);
            
            // 获取 value 字段的内存偏移量
            valueOffset = unsafe.objectFieldOffset(
                UnsafeCasExample.class.getDeclaredField("value")
            );
        } catch (Exception e) {
            throw new Error(e);
        }
    }
    
    /**
     * CAS 操作: 如果 value == expect，则更新为 update
     */
    public boolean compareAndSwap(int expect, int update) {
        return unsafe.compareAndSwapInt(this, valueOffset, expect, update);
    }
    
    /**
     * 自旋 CAS 实现原子自增
     */
    public int incrementAndGet() {
        int current;
        int next;
        do {
            current = value;           // 读取当前值
            next = current + 1;        // 计算新值
        } while (!compareAndSwap(current, next));  // CAS 更新，失败则重试
        return next;
    }
}
```

---

## 四、Atomic 类实现原理

```java
/**
 * AtomicInteger 源码分析 (简化版)
 */
public class AtomicInteger {
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static final long valueOffset;
    
    static {
        try {
            valueOffset = unsafe.objectFieldOffset(
                AtomicInteger.class.getDeclaredField("value"));
        } catch (Exception ex) { throw new Error(ex); }
    }
    
    private volatile int value;  // 注意是 volatile
    
    public final int get() {
        return value;
    }
    
    public final int incrementAndGet() {
        return unsafe.getAndAddInt(this, valueOffset, 1) + 1;
    }
    
    public final boolean compareAndSet(int expect, int update) {
        return unsafe.compareAndSwapInt(this, valueOffset, expect, update);
    }
}

// Unsafe.getAndAddInt 实现 (自旋 CAS)
public final int getAndAddInt(Object o, long offset, int delta) {
    int v;
    do {
        v = getIntVolatile(o, offset);  // 读取最新值
    } while (!compareAndSwapInt(o, offset, v, v + delta));  // CAS 更新
    return v;
}
```

---

## 五、CAS 的问题

### 5.1 ABA 问题

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          ABA 问题                                                 │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  场景: 初始值 A，线程1想把 A 改成 C                                               │
│                                                                                  │
│  线程1                              线程2                                        │
│  ────────────────────────────────────────────────────────────────────────────    │
│  1. 读取值 = A                      2. 将 A 改成 B                               │
│  3. 被挂起...                       4. 将 B 改回 A                               │
│  5. 恢复执行                                                                     │
│  6. CAS(A, C) → 成功!                                                            │
│     (虽然值是A，但中间已经变化过了)                                              │
│                                                                                  │
│  问题: CAS 无法感知值曾经被修改过                                                │
│                                                                                  │
│  解决方案: AtomicStampedReference (带版本号)                                     │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
/**
 * 使用 AtomicStampedReference 解决 ABA 问题
 */
public class ABAExample {
    
    private AtomicStampedReference<Integer> atomicRef = 
        new AtomicStampedReference<>(100, 0);  // 初始值100，版本号0
    
    public void update() {
        int[] stampHolder = new int[1];
        Integer current = atomicRef.get(stampHolder);  // 获取值和版本号
        int currentStamp = stampHolder[0];
        
        // CAS 更新，同时检查值和版本号
        boolean success = atomicRef.compareAndSet(
            current,           // 期望值
            200,               // 新值
            currentStamp,      // 期望版本号
            currentStamp + 1   // 新版本号
        );
    }
}
```

### 5.2 自旋开销

```java
/**
 * 高并发下自旋开销大
 */
public class SpinCostExample {
    private AtomicInteger counter = new AtomicInteger(0);
    
    public void increment() {
        // 高并发下，CAS 失败率高，会不断自旋
        // 造成 CPU 空转
        counter.incrementAndGet();
    }
}

// 解决方案: LongAdder (分段 CAS)
public class LongAdderExample {
    private LongAdder counter = new LongAdder();
    
    public void increment() {
        // 多个 Cell，减少竞争
        counter.increment();
    }
    
    public long sum() {
        return counter.sum();
    }
}
```

### 5.3 只能保证一个变量

```java
/**
 * CAS 只能操作一个变量
 */
public class SingleVariableLimit {
    
    // 方案1: AtomicReference 包装对象
    private AtomicReference<State> state = new AtomicReference<>(new State(0, 0));
    
    public void updateBoth(int newX, int newY) {
        State oldState;
        State newState;
        do {
            oldState = state.get();
            newState = new State(newX, newY);
        } while (!state.compareAndSet(oldState, newState));
    }
    
    static class State {
        final int x, y;
        State(int x, int y) { this.x = x; this.y = y; }
    }
    
    // 方案2: 使用锁
}
```

---

## 六、CAS vs synchronized

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                        CAS vs synchronized                                        │
├─────────────────┬────────────────────────────┬───────────────────────────────────┤
│                 │          CAS               │         synchronized              │
├─────────────────┼────────────────────────────┼───────────────────────────────────┤
│   实现方式      │  乐观锁（无锁）             │  悲观锁                           │
│   阻塞          │  非阻塞（自旋）             │  可能阻塞                         │
│   性能          │  低并发时更好               │  高并发时更稳定                   │
│   CPU 开销      │  自旋消耗 CPU               │  阻塞不消耗 CPU                   │
│   适用场景      │  读多写少、竞争不激烈       │  写多、竞争激烈                   │
│   复杂度        │  只能操作单个变量           │  可保护代码块                     │
└─────────────────┴────────────────────────────┴───────────────────────────────────┘
```

---

## 七、面试回答

### 30秒版本

> CAS (Compare-And-Swap) 是一种**无锁原子操作**，比较内存值与期望值，相等则更新。
>
> **原理**：V(内存值) == E(期望值) 则更新为 N(新值)，否则重试。由 CPU 指令保证原子性。
>
> **优点**：无锁，避免线程阻塞和上下文切换。
>
> **问题**：
> - ABA 问题 → 用 AtomicStampedReference
> - 自旋开销 → 用 LongAdder
> - 只能操作单变量 → 用 AtomicReference 包装

### 1分钟版本

> **CAS 是什么：**
> Compare-And-Swap，比较并交换。包含三个操作数：内存值V、期望值E、新值N。如果 V == E，则将 V 更新为 N，整个操作是原子的。
>
> **底层实现：**
> Java 通过 Unsafe 类调用 native 方法，最终由 CPU 指令（如 CMPXCHG）保证原子性。Atomic 系列类都是基于 CAS 实现。
>
> **自旋 CAS：**
> 如果 CAS 失败，会在循环中重试，直到成功。这就是自旋 CAS。
>
> **三个问题：**
> 1. **ABA 问题**：值从 A→B→A，CAS 检测不到中间变化。解决：AtomicStampedReference 加版本号
> 2. **自旋开销**：高并发时失败率高，CPU 空转。解决：LongAdder 分段 CAS
> 3. **单变量限制**：只能操作一个变量。解决：AtomicReference 包装对象
>
> **应用场景：**
> AtomicInteger、AtomicLong、ConcurrentHashMap、AQS 的 state 变更等。

---

## 八、代码示例

### 8.1 实现简单的自旋锁

```java
/**
 * 基于 CAS 实现的自旋锁
 */
public class SpinLock {
    private AtomicReference<Thread> owner = new AtomicReference<>();
    
    public void lock() {
        Thread current = Thread.currentThread();
        // 自旋等待获取锁
        while (!owner.compareAndSet(null, current)) {
            // 自旋
        }
    }
    
    public void unlock() {
        Thread current = Thread.currentThread();
        owner.compareAndSet(current, null);
    }
}
```

### 8.2 实现无锁栈

```java
/**
 * 基于 CAS 实现的无锁栈
 */
public class LockFreeStack<E> {
    private AtomicReference<Node<E>> top = new AtomicReference<>();
    
    public void push(E item) {
        Node<E> newNode = new Node<>(item);
        Node<E> oldTop;
        do {
            oldTop = top.get();
            newNode.next = oldTop;
        } while (!top.compareAndSet(oldTop, newNode));
    }
    
    public E pop() {
        Node<E> oldTop;
        Node<E> newTop;
        do {
            oldTop = top.get();
            if (oldTop == null) return null;
            newTop = oldTop.next;
        } while (!top.compareAndSet(oldTop, newTop));
        return oldTop.item;
    }
    
    private static class Node<E> {
        E item;
        Node<E> next;
        Node(E item) { this.item = item; }
    }
}
```
