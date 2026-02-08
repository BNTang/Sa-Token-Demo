# 什么是Java的CyclicBarrier?

## 回答

CyclicBarrier（循环屏障）是一种同步工具，让一组线程互相等待，全部到达屏障点后一起继续执行：

```
┌─────────────────────────────────────────────────────────────┐
│                  CyclicBarrier 原理                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  CyclicBarrier(3)  需要3个线程到达                           │
│                                                             │
│   T1 ─────→ await() ─┐                                     │
│                      │                                      │
│   T2 ─────→ await() ─┼──→ 屏障 ──→ 全部继续执行              │
│                      │                                      │
│   T3 ─────→ await() ─┘                                     │
│                                                             │
│   所有线程到达屏障后，屏障打开，可循环使用                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 基本使用

```java
public class CyclicBarrierDemo {
    
    public static void main(String[] args) {
        // 创建屏障，需要3个线程到达
        CyclicBarrier barrier = new CyclicBarrier(3, () -> {
            // 所有线程到达后执行的回调
            System.out.println("所有线程到达屏障，开始下一阶段");
        });
        
        for (int i = 0; i < 3; i++) {
            final int id = i;
            new Thread(() -> {
                try {
                    System.out.println("线程" + id + " 准备就绪");
                    barrier.await();  // 等待其他线程
                    System.out.println("线程" + id + " 继续执行");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}

// 输出：
// 线程0 准备就绪
// 线程1 准备就绪
// 线程2 准备就绪
// 所有线程到达屏障，开始下一阶段
// 线程0 继续执行
// 线程1 继续执行
// 线程2 继续执行
```

## 核心方法

```java
CyclicBarrier barrier = new CyclicBarrier(3);

// 等待其他线程
int arriveIndex = barrier.await();  // 返回到达序号（0表示最后到达）

// 超时等待
int arriveIndex = barrier.await(5, TimeUnit.SECONDS);

// 查询方法
int parties = barrier.getParties();           // 参与者数量
int waiting = barrier.getNumberWaiting();     // 正在等待的线程数
boolean broken = barrier.isBroken();          // 屏障是否已损坏

// 重置屏障
barrier.reset();  // 重置为初始状态（会唤醒等待线程抛BrokenBarrierException）
```

## 循环使用特性

```java
CyclicBarrier barrier = new CyclicBarrier(3, () -> {
    System.out.println("=== 屏障触发 ===");
});

for (int round = 0; round < 3; round++) {
    for (int i = 0; i < 3; i++) {
        final int id = i;
        final int r = round;
        executor.execute(() -> {
            System.out.println("Round" + r + " 线程" + id);
            barrier.await();
        });
    }
    Thread.sleep(100);  // 等待本轮完成
}

// 输出：
// Round0 线程0
// Round0 线程1
// Round0 线程2
// === 屏障触发 ===
// Round1 线程0
// Round1 线程1
// Round1 线程2
// === 屏障触发 ===
// ...
```

## 与CountDownLatch对比

```
┌─────────────────────────────────────────────────────────────┐
│           CyclicBarrier vs CountDownLatch                   │
├─────────────────┬───────────────┬───────────────────────────┤
│ 特性            │ CyclicBarrier │ CountDownLatch            │
├─────────────────┼───────────────┼───────────────────────────┤
│ 等待方式        │ 线程互相等待  │ 一个线程等待其他线程       │
├─────────────────┼───────────────┼───────────────────────────┤
│ 可复用          │ ✅ 可循环使用 │ ❌ 一次性                  │
├─────────────────┼───────────────┼───────────────────────────┤
│ 回调方法        │ ✅ 有barrierAction │ ❌ 无                 │
├─────────────────┼───────────────┼───────────────────────────┤
│ 计数方式        │ 等待线程数    │ countDown次数            │
├─────────────────┼───────────────┼───────────────────────────┤
│ 异常处理        │ BrokenBarrierException │ 无               │
└─────────────────┴───────────────┴───────────────────────────┘
```

```java
// CountDownLatch: 主线程等待多个子任务
CountDownLatch latch = new CountDownLatch(3);
// 子任务完成后 latch.countDown()
latch.await();  // 主线程等待

// CyclicBarrier: 多个线程互相等待
CyclicBarrier barrier = new CyclicBarrier(3);
// 每个线程 barrier.await()
// 所有线程同时继续
```

## 实际应用场景

### 1. 多线程计算汇总

```java
public class ParallelCalculator {
    
    private final CyclicBarrier barrier;
    private final int[] partialResults;
    
    public ParallelCalculator(int threadCount) {
        this.partialResults = new int[threadCount];
        this.barrier = new CyclicBarrier(threadCount, () -> {
            // 所有计算完成，汇总结果
            int total = Arrays.stream(partialResults).sum();
            System.out.println("总计: " + total);
        });
    }
    
    public void calculate(int threadId, int[] data) {
        new Thread(() -> {
            // 计算部分结果
            partialResults[threadId] = Arrays.stream(data).sum();
            System.out.println("线程" + threadId + " 计算完成: " + partialResults[threadId]);
            
            try {
                barrier.await();  // 等待其他线程
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
```

### 2. 多阶段任务处理

```java
public class MultiPhaseTask {
    
    private final CyclicBarrier barrier;
    
    public MultiPhaseTask(int threadCount) {
        this.barrier = new CyclicBarrier(threadCount, () -> {
            System.out.println("=== 阶段完成 ===");
        });
    }
    
    public void execute() {
        for (int i = 0; i < 3; i++) {
            final int id = i;
            new Thread(() -> {
                try {
                    // 阶段1：加载数据
                    System.out.println("线程" + id + " 加载数据");
                    barrier.await();
                    
                    // 阶段2：处理数据
                    System.out.println("线程" + id + " 处理数据");
                    barrier.await();
                    
                    // 阶段3：保存结果
                    System.out.println("线程" + id + " 保存结果");
                    barrier.await();
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
```

### 3. 并行测试（模拟并发）

```java
public class ConcurrentTest {
    
    public static void main(String[] args) throws InterruptedException {
        int threadCount = 100;
        CyclicBarrier barrier = new CyclicBarrier(threadCount, () -> {
            System.out.println("所有线程就绪，开始并发测试");
        });
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    // 准备工作
                    prepareData();
                    
                    // 等待所有线程就绪
                    barrier.await();
                    
                    // 同时发起请求
                    sendRequest();
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        
        executor.shutdown();
    }
}
```

## 源码分析

```java
public class CyclicBarrier {
    
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition trip = lock.newCondition();
    private final int parties;           // 参与者数量
    private final Runnable barrierCommand;  // 回调
    private Generation generation = new Generation();
    private int count;  // 还需要等待的线程数
    
    public int await() throws InterruptedException, BrokenBarrierException {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            final Generation g = generation;
            
            int index = --count;  // 减少等待数
            if (index == 0) {  // 最后一个到达
                // 执行回调
                if (barrierCommand != null)
                    barrierCommand.run();
                // 唤醒所有等待线程
                nextGeneration();
                return 0;
            }
            
            // 不是最后一个，等待
            for (;;) {
                trip.await();
                if (g != generation)
                    return index;
            }
        } finally {
            lock.unlock();
        }
    }
    
    private void nextGeneration() {
        trip.signalAll();  // 唤醒所有
        count = parties;   // 重置计数
        generation = new Generation();  // 新一代
    }
}
```

## 异常处理

```java
CyclicBarrier barrier = new CyclicBarrier(3);

// 如果一个线程被中断或超时
// 其他等待线程会抛出 BrokenBarrierException

try {
    barrier.await(1, TimeUnit.SECONDS);
} catch (TimeoutException e) {
    // 超时，屏障损坏
} catch (BrokenBarrierException e) {
    // 屏障已损坏（其他线程出问题）
}

// 重置屏障
if (barrier.isBroken()) {
    barrier.reset();
}
```

## 面试回答

### 30秒版本
> CyclicBarrier是让多个线程互相等待的同步工具。调用await()后线程阻塞，直到指定数量的线程都到达屏障点，然后同时继续执行。支持回调函数在屏障触发时执行，可循环使用。与CountDownLatch区别：CyclicBarrier是线程互相等待可复用，CountDownLatch是一个线程等待其他线程且一次性。

### 1分钟版本
> CyclicBarrier（循环屏障）是JUC提供的同步工具，用于让一组线程互相等待，全部到达屏障点后一起继续执行。
> 
> **使用方式**：创建时指定参与者数量，每个线程调用await()后阻塞，直到所有线程都到达。支持构造时传入Runnable回调，在屏障触发时由最后到达的线程执行。
> 
> **核心特点**：①**可循环**：屏障触发后自动重置，可用于多阶段任务；②**有回调**：barrierAction在所有线程到达时执行；③**异常处理**：一个线程异常或超时，其他线程抛BrokenBarrierException。
> 
> **vs CountDownLatch**：CountDownLatch是一个线程等待其他线程完成，一次性的；CyclicBarrier是多个线程互相等待，可循环使用。
> 
> **应用场景**：多线程分段计算汇总、多阶段任务协调、并发测试模拟同时发起请求。

## 相关问题
- [[countdownlatch]] - CountDownLatch详解
- [[concurrent-utils]] - 并发工具类
- [[semaphore]] - Semaphore信号量
