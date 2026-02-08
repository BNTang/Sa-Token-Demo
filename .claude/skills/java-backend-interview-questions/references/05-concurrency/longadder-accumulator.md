# 你使用过Java的累加器吗?

## 回答

Java 8引入了LongAdder和LongAccumulator等累加器，专门用于高并发计数场景：

```
┌─────────────────────────────────────────────────────────────┐
│                  累加器类型                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  LongAdder         长整型累加器（最常用）                    │
│  DoubleAdder       双精度累加器                             │
│  LongAccumulator   长整型累积器（自定义函数）                │
│  DoubleAccumulator 双精度累积器（自定义函数）                │
│                                                             │
│  核心思想：分散热点，减少CAS竞争                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## LongAdder原理

```
┌─────────────────────────────────────────────────────────────┐
│                 LongAdder 分段累加原理                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  AtomicLong：所有线程竞争同一个value                         │
│  ┌─────────────┐                                           │
│  │   value     │ ← T1, T2, T3, T4 都CAS这一个值             │
│  └─────────────┘   高并发下大量CAS失败重试                   │
│                                                             │
│  ─────────────────────────────────────────────             │
│                                                             │
│  LongAdder：分散到base + Cells数组                          │
│                                                             │
│  ┌──────┐                                                  │
│  │ base │  ← 低并发时直接累加到base                         │
│  └──────┘                                                  │
│                                                             │
│  ┌────────┬────────┬────────┬────────┐                    │
│  │ Cell[0]│ Cell[1]│ Cell[2]│ Cell[3]│ ← 高并发分散到Cell   │
│  └────────┴────────┴────────┴────────┘                    │
│      ↑        ↑        ↑        ↑                          │
│      T1       T2       T3       T4    不同线程操作不同Cell   │
│                                                             │
│  sum() = base + Cell[0] + Cell[1] + Cell[2] + Cell[3]      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## LongAdder使用

```java
// 创建
LongAdder counter = new LongAdder();

// 累加操作
counter.increment();      // +1
counter.decrement();      // -1
counter.add(10);          // +10

// 获取值
long sum = counter.sum();           // 获取当前和（不清零）
counter.reset();                    // 重置为0
long value = counter.sumThenReset();// 获取后重置（原子）

// 转换
long longValue = counter.longValue();
int intValue = counter.intValue();
```

## LongAccumulator使用

```java
// 自定义累积函数
LongAccumulator accumulator = new LongAccumulator(
    (left, right) -> left + right,  // 累积函数
    0L                               // 初始值
);

accumulator.accumulate(5);
accumulator.accumulate(10);
System.out.println(accumulator.get());  // 15

// 求最大值
LongAccumulator maxAccumulator = new LongAccumulator(
    Long::max,          // 取最大值
    Long.MIN_VALUE      // 初始值
);
maxAccumulator.accumulate(5);
maxAccumulator.accumulate(20);
maxAccumulator.accumulate(10);
System.out.println(maxAccumulator.get());  // 20

// 求最小值
LongAccumulator minAccumulator = new LongAccumulator(
    Long::min,
    Long.MAX_VALUE
);

// 求乘积
LongAccumulator productAccumulator = new LongAccumulator(
    (x, y) -> x * y,
    1L
);
```

## 性能对比

```java
// 高并发计数性能测试
public class PerformanceTest {
    private static final int THREAD_COUNT = 100;
    private static final int INCREMENT_COUNT = 1000000;
    
    public static void main(String[] args) throws Exception {
        // AtomicLong
        AtomicLong atomicLong = new AtomicLong();
        long start1 = System.currentTimeMillis();
        runTest(() -> atomicLong.incrementAndGet());
        long time1 = System.currentTimeMillis() - start1;
        System.out.println("AtomicLong: " + time1 + "ms");
        
        // LongAdder
        LongAdder longAdder = new LongAdder();
        long start2 = System.currentTimeMillis();
        runTest(() -> longAdder.increment());
        long time2 = System.currentTimeMillis() - start2;
        System.out.println("LongAdder: " + time2 + "ms");
    }
    
    private static void runTest(Runnable task) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        
        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.execute(() -> {
                for (int j = 0; j < INCREMENT_COUNT; j++) {
                    task.run();
                }
                latch.countDown();
            });
        }
        
        latch.await();
        executor.shutdown();
    }
}

// 典型结果（100线程，100万次累加）：
// AtomicLong: 2500ms
// LongAdder:  300ms   ← 快约8倍
```

## 源码关键点

```java
// LongAdder继承自Striped64
public class LongAdder extends Striped64 {
    
    public void add(long x) {
        Cell[] cs; long b, v; int m; Cell c;
        
        // 1. 先尝试CAS更新base
        if ((cs = cells) != null || !casBase(b = base, b + x)) {
            
            // 2. base更新失败，说明有竞争，使用Cell
            boolean uncontended = true;
            
            if (cs == null || (m = cs.length - 1) < 0 ||
                // 3. 根据线程hash定位Cell
                (c = cs[getProbe() & m]) == null ||
                // 4. CAS更新对应Cell
                !(uncontended = c.cas(v = c.value, v + x)))
                
                // 5. Cell也竞争失败，进入复杂逻辑（扩容等）
                longAccumulate(x, null, uncontended);
        }
    }
    
    public long sum() {
        Cell[] cs = cells;
        long sum = base;
        if (cs != null) {
            for (Cell c : cs)
                if (c != null)
                    sum += c.value;
        }
        return sum;
    }
}

// Cell使用@Contended避免伪共享
@jdk.internal.vm.annotation.Contended
static final class Cell {
    volatile long value;
    // ...
}
```

## 使用场景

### 1. 请求计数器

```java
@Component
public class RequestCounter {
    private final LongAdder counter = new LongAdder();
    
    public void recordRequest() {
        counter.increment();
    }
    
    public long getCount() {
        return counter.sum();
    }
    
    @Scheduled(fixedRate = 60000)
    public void reportAndReset() {
        long count = counter.sumThenReset();
        log.info("过去1分钟请求数: {}", count);
    }
}
```

### 2. 统计最大耗时

```java
public class LatencyTracker {
    private final LongAccumulator maxLatency = 
        new LongAccumulator(Long::max, 0L);
    
    public void recordLatency(long latencyMs) {
        maxLatency.accumulate(latencyMs);
    }
    
    public long getMaxLatency() {
        return maxLatency.get();
    }
}
```

### 3. 限流计数

```java
public class SlidingWindowCounter {
    private final LongAdder[] slots;
    private final int windowSize;
    
    public SlidingWindowCounter(int windowSize) {
        this.windowSize = windowSize;
        this.slots = new LongAdder[windowSize];
        for (int i = 0; i < windowSize; i++) {
            slots[i] = new LongAdder();
        }
    }
    
    public void increment() {
        int index = (int) (System.currentTimeMillis() / 1000 % windowSize);
        slots[index].increment();
    }
    
    public long sum() {
        long total = 0;
        for (LongAdder slot : slots) {
            total += slot.sum();
        }
        return total;
    }
}
```

## 对比总结

| 特性 | AtomicLong | LongAdder |
|------|------------|-----------|
| **低并发性能** | 相近 | 相近 |
| **高并发性能** | 较差（CAS竞争） | 很好（分Cell） |
| **内存占用** | 小 | 较大（多个Cell） |
| **精确读取** | ✅ 精确 | ❌ 近似（统计时可能变化） |
| **适用场景** | 低并发、需精确值 | 高并发计数统计 |

## 面试回答

### 30秒版本
> LongAdder是JDK8引入的高并发累加器，原理是分段累加。低并发时CAS更新base，高并发时分散到Cells数组，每个线程操作不同Cell减少竞争。sum()时汇总base+所有Cell。比AtomicLong高并发下快5-10倍。LongAccumulator支持自定义累积函数如求最大值。

### 1分钟版本
> LongAdder和LongAccumulator是JDK8引入的累加器，专门解决AtomicLong在高并发下CAS竞争严重的问题。
> 
> **LongAdder原理**：内部维护base变量和Cells数组。低并发时直接CAS更新base，高并发下分散到Cells数组，不同线程根据hash操作不同Cell，减少竞争。sum()时汇总base和所有Cell的值。Cell使用@Contended注解避免伪共享。
> 
> **性能对比**：高并发（100线程）下LongAdder比AtomicLong快5-10倍。但sum()不是精确值（统计时值可能变化），不适合需要精确读取的场景。
> 
> **LongAccumulator**：支持自定义累积函数，如Long::max求最大值、Long::min求最小值，比LongAdder更灵活。
> 
> **使用场景**：高并发计数器、统计指标、限流计数。不需要精确实时值的统计场景首选LongAdder。

## 相关问题
- [[atomic-classes]] - 原子类
- [[cas]] - CAS原理
- [[thread-safety]] - 线程安全
