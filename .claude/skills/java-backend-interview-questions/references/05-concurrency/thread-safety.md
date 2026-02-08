# Java 中的线程安全是什么意思？

> 线程安全指多线程环境下，代码执行结果与单线程执行结果一致，不会因并发访问产生数据不一致

## 30秒速答

线程安全的定义：
- **核心**: 多线程访问时，无需额外同步，结果仍正确
- **三要素**: 原子性、可见性、有序性
- **实现**: 不可变对象、加锁、CAS、ThreadLocal

## 一分钟详解

### 线程不安全示例

```java
// 线程不安全的计数器
public class UnsafeCounter {
    private int count = 0;
    
    public void increment() {
        count++;  // 非原子：读取 → +1 → 写入
    }
    
    public int getCount() {
        return count;
    }
}

// 测试
UnsafeCounter counter = new UnsafeCounter();
ExecutorService executor = Executors.newFixedThreadPool(10);

for (int i = 0; i < 1000; i++) {
    executor.submit(() -> counter.increment());
}

executor.shutdown();
executor.awaitTermination(1, TimeUnit.SECONDS);

System.out.println(counter.getCount());  
// 预期: 1000，实际: 可能是 980、990 等
```

### 线程安全三要素

```
┌────────────────────────────────────────────────────────┐
│                   线程安全三要素                        │
├──────────────┬──────────────┬──────────────────────────┤
│    原子性    │    可见性    │       有序性             │
├──────────────┼──────────────┼──────────────────────────┤
│ 操作不可分割 │ 修改对其他   │ 程序执行顺序            │
│ 要么全执行   │ 线程立即可见 │ 符合预期                │
│ 要么不执行   │              │                          │
├──────────────┼──────────────┼──────────────────────────┤
│ synchronized │ volatile     │ volatile                │
│ Lock         │ synchronized │ synchronized            │
│ Atomic*      │ Lock         │ happens-before          │
└──────────────┴──────────────┴──────────────────────────┘
```

### 实现线程安全的方法

```java
// 方法1: synchronized 同步
public class SyncCounter {
    private int count = 0;
    
    public synchronized void increment() {
        count++;
    }
}

// 方法2: 原子类 (CAS)
public class AtomicCounter {
    private AtomicInteger count = new AtomicInteger(0);
    
    public void increment() {
        count.incrementAndGet();
    }
}

// 方法3: 不可变对象
public final class ImmutablePoint {
    private final int x;
    private final int y;
    
    public ImmutablePoint(int x, int y) {
        this.x = x;
        this.y = y;
    }
    // 只有 getter，没有 setter
}

// 方法4: ThreadLocal 线程隔离
public class ThreadLocalExample {
    private static ThreadLocal<SimpleDateFormat> dateFormat = 
        ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));
    
    public String format(Date date) {
        return dateFormat.get().format(date);  // 每个线程独立副本
    }
}

// 方法5: 使用线程安全的集合
List<String> safeList = new CopyOnWriteArrayList<>();
Map<String, Object> safeMap = new ConcurrentHashMap<>();
```

### 线程安全级别

```
┌───────────────────────────────────────────────────────┐
│  线程安全级别 (从高到低)                               │
│                                                       │
│  1. 不可变 (Immutable)                                │
│     String, Integer, 不可变集合                       │
│     完全线程安全，任意访问                            │
│                                                       │
│  2. 绝对线程安全                                      │
│     无论运行时环境如何，调用者无需额外同步            │
│     几乎不存在（Vector也不算）                        │
│                                                       │
│  3. 相对线程安全                                      │
│     单独操作线程安全，复合操作需同步                  │
│     ConcurrentHashMap, CopyOnWriteArrayList          │
│                                                       │
│  4. 线程兼容                                          │
│     本身不安全，调用者可通过同步保证安全              │
│     ArrayList, HashMap                               │
│                                                       │
│  5. 线程对立                                          │
│     无法在多线程环境使用                              │
│     Thread.suspend()/resume() (已废弃)               │
└───────────────────────────────────────────────────────┘
```

### 常见线程安全/不安全类

| 类型 | 线程不安全 | 线程安全 |
|------|-----------|----------|
| List | ArrayList | CopyOnWriteArrayList, Vector |
| Map | HashMap | ConcurrentHashMap, Hashtable |
| Set | HashSet | CopyOnWriteArraySet |
| 字符串 | StringBuilder | StringBuffer |
| 日期 | SimpleDateFormat | DateTimeFormatter |
| 计数 | int++ | AtomicInteger |

### 复合操作的线程安全问题

```java
// ❌ 即使用 ConcurrentHashMap，复合操作仍不安全
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
map.put("count", 0);

// 线程1和线程2同时执行：
Integer count = map.get("count");  // 都读到 0
map.put("count", count + 1);       // 都写入 1，丢失更新

// ✅ 使用原子复合操作
map.compute("count", (k, v) -> v == null ? 1 : v + 1);
// 或
map.merge("count", 1, Integer::sum);
```

## 关键记忆点

```
┌─────────────────────────────────────────────────────┐
│  线程安全速记：                                      │
│                                                     │
│  定义：多线程执行结果 = 单线程执行结果               │
│                                                     │
│  三要素：                                           │
│  ┌──────────┬─────────────────────────────────┐    │
│  │ 原子性   │ synchronized, Lock, Atomic*     │    │
│  │ 可见性   │ volatile, synchronized          │    │
│  │ 有序性   │ volatile, happens-before        │    │
│  └──────────┴─────────────────────────────────┘    │
│                                                     │
│  实现方式：                                         │
│  • 不可变对象 → 最安全                              │
│  • 加锁同步   → 最常用                              │
│  • CAS无锁    → 高性能                              │
│  • ThreadLocal→ 线程隔离                            │
│  • 安全集合   → 开箱即用                            │
└─────────────────────────────────────────────────────┘
```

## 面试追问

**Q: volatile 能保证线程安全吗？**

A: **不能完全保证**。volatile 只保证可见性和有序性，不保证原子性。`volatile int count; count++;` 仍然不是线程安全的，因为 `count++` 是复合操作。
