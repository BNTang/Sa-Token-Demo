# 什么是Java的CAS (Compare-And-Swap)操作?

## 回答

CAS是一种无锁的原子操作，通过比较并交换实现线程安全：

```
┌─────────────────────────────────────────────────────────────┐
│                    CAS 原理                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  compareAndSwap(内存地址V, 期望值A, 新值B)                    │
│                                                             │
│  if (V == A) {     // 比较                                  │
│      V = B;        // 交换                                  │
│      return true;  // 成功                                  │
│  } else {                                                   │
│      return false; // 失败，值已被其他线程修改                │
│  }                                                          │
│                                                             │
│  这三步是原子的，由CPU指令保证                                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## CAS流程图

```
┌─────────────────────────────────────────────────────────────┐
│                   CAS 自旋重试                               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│       ┌────────────────┐                                   │
│       │  读取当前值 V   │                                   │
│       └───────┬────────┘                                   │
│               │                                             │
│               ▼                                             │
│       ┌────────────────┐                                   │
│       │  计算新值 B     │                                   │
│       └───────┬────────┘                                   │
│               │                                             │
│               ▼                                             │
│       ┌────────────────┐                                   │
│       │ CAS(V, 旧值, B) │                                   │
│       └───────┬────────┘                                   │
│               │                                             │
│        ┌──────┴──────┐                                     │
│        │             │                                      │
│      成功          失败                                      │
│        │             │                                      │
│        ▼             │                                      │
│       返回       ────┴────→ 重试（自旋）                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## Java中的实现

### Unsafe类

```java
// sun.misc.Unsafe 提供CAS操作
public final class Unsafe {
    // 比较并交换int
    public final native boolean compareAndSwapInt(
        Object obj,     // 对象
        long offset,    // 字段偏移量
        int expect,     // 期望值
        int update      // 新值
    );
    
    // 比较并交换long
    public final native boolean compareAndSwapLong(
        Object obj, long offset, long expect, long update);
    
    // 比较并交换Object引用
    public final native boolean compareAndSwapObject(
        Object obj, long offset, Object expect, Object update);
}
```

### AtomicInteger源码

```java
public class AtomicInteger extends Number {
    
    private static final Unsafe U = Unsafe.getUnsafe();
    private static final long VALUE;  // value字段的偏移量
    
    static {
        VALUE = U.objectFieldOffset(
            AtomicInteger.class.getDeclaredField("value"));
    }
    
    private volatile int value;  // volatile保证可见性
    
    public final int incrementAndGet() {
        return U.getAndAddInt(this, VALUE, 1) + 1;
    }
    
    public final boolean compareAndSet(int expect, int update) {
        return U.compareAndSwapInt(this, VALUE, expect, update);
    }
}

// Unsafe.getAndAddInt - 自旋CAS
public final int getAndAddInt(Object obj, long offset, int delta) {
    int v;
    do {
        v = getIntVolatile(obj, offset);  // 读取当前值
    } while (!compareAndSwapInt(obj, offset, v, v + delta));  // CAS失败重试
    return v;
}
```

## CAS优缺点

```
┌─────────────────────────────────────────────────────────────┐
│                    CAS 优缺点                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  优点：                                                      │
│  • 无锁操作，避免线程阻塞和上下文切换                          │
│  • 性能高，适合低竞争场景                                     │
│  • 不会死锁                                                  │
│                                                             │
│  缺点：                                                      │
│  • ABA问题：值从A→B→A，CAS认为没变                           │
│  • 自旋开销：高竞争时CPU空转                                  │
│  • 只能保证单个变量原子性                                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## ABA问题

```
┌─────────────────────────────────────────────────────────────┐
│                    ABA 问题示例                              │
├─────────────────────────────────────────────────────────────┐
│                                                             │
│  线程1:                    线程2:                           │
│    读取 V=A                                                 │
│    准备改成B               读取 V=A                         │
│    (被挂起)                V=B (改成B)                      │
│                            V=A (又改回A)                    │
│    CAS(A, B) 成功!         (完成)                          │
│                                                             │
│  问题：线程1的CAS成功了，但V其实已经被改过了！                 │
│                                                             │
│  类比：你取款前ATM显示1000，                                 │
│        期间有人存了500又取了500，                            │
│        余额还是1000，但中间状态变化了                        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 解决ABA问题

```java
// 方案1：AtomicStampedReference（版本号）
AtomicStampedReference<Integer> ref = 
    new AtomicStampedReference<>(100, 0);  // 初始值100，版本0

// 获取值和版本
int[] stampHolder = new int[1];
Integer value = ref.get(stampHolder);
int stamp = stampHolder[0];

// CAS时同时检查值和版本
ref.compareAndSet(100, 200, stamp, stamp + 1);

// 方案2：AtomicMarkableReference（标记位）
AtomicMarkableReference<Integer> ref = 
    new AtomicMarkableReference<>(100, false);
```

## CAS vs synchronized

| 特性 | CAS | synchronized |
|------|-----|--------------|
| **锁类型** | 无锁（乐观锁思想） | 有锁（悲观锁） |
| **阻塞** | 不阻塞，自旋重试 | 阻塞等待 |
| **性能（低竞争）** | 更好 | 较好 |
| **性能（高竞争）** | 差（自旋消耗CPU） | 较好 |
| **死锁风险** | 无 | 有 |
| **适用范围** | 单个变量 | 代码块 |

## 应用场景

### 1. 原子计数器

```java
AtomicInteger counter = new AtomicInteger(0);
counter.incrementAndGet();
```

### 2. 无锁队列

```java
public class LockFreeQueue<E> {
    private AtomicReference<Node<E>> head;
    private AtomicReference<Node<E>> tail;
    
    public void enqueue(E item) {
        Node<E> newNode = new Node<>(item);
        while (true) {
            Node<E> curTail = tail.get();
            Node<E> tailNext = curTail.next.get();
            
            if (curTail == tail.get()) {
                if (tailNext != null) {
                    // 尾节点落后，帮助推进
                    tail.compareAndSet(curTail, tailNext);
                } else {
                    // 尝试添加新节点
                    if (curTail.next.compareAndSet(null, newNode)) {
                        tail.compareAndSet(curTail, newNode);
                        return;
                    }
                }
            }
        }
    }
}
```

### 3. 自旋锁

```java
public class SpinLock {
    private AtomicBoolean locked = new AtomicBoolean(false);
    
    public void lock() {
        while (!locked.compareAndSet(false, true)) {
            // 自旋等待
        }
    }
    
    public void unlock() {
        locked.set(false);
    }
}
```

### 4. 单例模式

```java
public class Singleton {
    private static final AtomicReference<Singleton> INSTANCE = 
        new AtomicReference<>();
    
    public static Singleton getInstance() {
        while (true) {
            Singleton instance = INSTANCE.get();
            if (instance != null) {
                return instance;
            }
            instance = new Singleton();
            if (INSTANCE.compareAndSet(null, instance)) {
                return instance;
            }
        }
    }
}
```

## CPU层面实现

```
┌─────────────────────────────────────────────────────────────┐
│                  CPU CAS 指令                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  x86: CMPXCHG 指令                                          │
│       LOCK前缀保证原子性（锁定总线或缓存行）                   │
│                                                             │
│  lock cmpxchg [mem], reg                                    │
│                                                             │
│  执行过程：                                                  │
│  1. LOCK锁定缓存行（或总线）                                 │
│  2. 比较EAX和内存值                                          │
│  3. 相等则将reg写入内存                                      │
│  4. 不等则将内存值写入EAX                                    │
│  5. 解锁                                                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本
> CAS是Compare-And-Swap，无锁原子操作。比较内存值与期望值，相等则更新为新值，不等则失败重试。Java通过Unsafe类调用CPU的CMPXCHG指令实现。优点：无锁、不阻塞、无死锁。缺点：ABA问题（用版本号解决）、高竞争时自旋消耗CPU、只能保证单变量原子性。

### 1分钟版本
> CAS（Compare-And-Swap）是一种无锁并发技术，核心是比较并交换：将内存值与期望值比较，相等则更新为新值并返回成功，不等则返回失败。失败后通常自旋重试。
> 
> **Java实现**：Unsafe类提供compareAndSwapInt/Long/Object方法，底层调用CPU的CMPXCHG指令。AtomicInteger等原子类基于CAS实现。
> 
> **优点**：①无锁操作，避免线程阻塞和上下文切换；②低竞争时性能好；③不会死锁。
> 
> **缺点**：①**ABA问题**，值从A变B又变回A，CAS误以为没变，解决方案是AtomicStampedReference加版本号；②**自旋开销**，高竞争时CPU空转，可用LongAdder分散热点；③只能保证单个变量原子性。
> 
> **应用**：原子类、ConcurrentHashMap、无锁队列、AQS同步器都大量使用CAS。

## 相关问题
- [[atomic-classes]] - 原子类
- [[aqs]] - AQS原理
- [[longadder-accumulator]] - 累加器
- [[thread-safety]] - 线程安全
