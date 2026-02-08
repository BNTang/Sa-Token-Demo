# Java 并发工具类

> 分类: Java 并发 | 难度: ⭐⭐⭐ | 频率: 高频

---

## 一、常用并发工具类概览

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                    java.util.concurrent 常用工具类                                │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  同步工具:                                                                       │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │  CountDownLatch   计数器，等待多个线程完成                                  │ │
│  │  CyclicBarrier    屏障，多个线程互相等待                                    │ │
│  │  Semaphore        信号量，控制并发数                                        │ │
│  │  Phaser           阶段器，分阶段任务 (Java 7+)                              │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  线程安全容器:                                                                   │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │  ConcurrentHashMap    并发 HashMap                                          │ │
│  │  CopyOnWriteArrayList 写时复制 List                                         │ │
│  │  ConcurrentLinkedQueue 并发队列                                             │ │
│  │  BlockingQueue        阻塞队列 (多种实现)                                   │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  原子类:                                                                         │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │  AtomicInteger/Long   原子整数                                              │ │
│  │  AtomicReference      原子引用                                              │ │
│  │  AtomicStampedReference 带版本号的原子引用 (解决ABA)                        │ │
│  │  LongAdder            高性能计数器 (Java 8+)                                │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  锁:                                                                             │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │  ReentrantLock        可重入锁                                              │ │
│  │  ReentrantReadWriteLock 读写锁                                              │ │
│  │  StampedLock          乐观读锁 (Java 8+)                                    │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 二、CountDownLatch 计数器

```java
/**
 * CountDownLatch: 等待多个线程完成
 * 场景: 主线程等待多个子线程完成后继续
 */
public class CountDownLatchDemo {
    
    public static void main(String[] args) throws InterruptedException {
        int threadCount = 5;
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            new Thread(() -> {
                try {
                    // 模拟任务
                    Thread.sleep(1000);
                    System.out.println("线程 " + index + " 完成");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();  // 计数减1
                }
            }).start();
        }
        
        latch.await();  // 阻塞等待计数归零
        System.out.println("所有线程完成，继续执行");
    }
}

// 特点:
// • 一次性，计数归零后不可重用
// • 适合: 主线程等待多个子线程
```

---

## 三、CyclicBarrier 循环屏障

```java
/**
 * CyclicBarrier: 多个线程互相等待，到齐后一起执行
 * 场景: 多线程分段计算，每段完成后汇总
 */
public class CyclicBarrierDemo {
    
    public static void main(String[] args) {
        int threadCount = 3;
        CyclicBarrier barrier = new CyclicBarrier(threadCount, () -> {
            // 所有线程到达后执行的回调
            System.out.println("所有线程到达屏障，开始下一阶段");
        });
        
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            new Thread(() -> {
                try {
                    System.out.println("线程 " + index + " 到达屏障");
                    barrier.await();  // 等待其他线程
                    System.out.println("线程 " + index + " 继续执行");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}

// 特点:
// • 可重用 (cyclic)，一轮结束自动重置
// • 适合: 多线程分阶段任务
```

---

## 四、Semaphore 信号量

```java
/**
 * Semaphore: 控制并发访问数量
 * 场景: 限流、连接池、资源访问控制
 */
public class SemaphoreDemo {
    
    public static void main(String[] args) {
        // 最多3个线程同时访问
        Semaphore semaphore = new Semaphore(3);
        
        for (int i = 0; i < 10; i++) {
            final int index = i;
            new Thread(() -> {
                try {
                    semaphore.acquire();  // 获取许可
                    System.out.println("线程 " + index + " 进入");
                    Thread.sleep(2000);
                    System.out.println("线程 " + index + " 离开");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    semaphore.release();  // 释放许可
                }
            }).start();
        }
    }
}

// 特点:
// • 控制并发数
// • 可用于限流
// • acquire() 阻塞获取，tryAcquire() 非阻塞
```

---

## 五、Exchanger 交换器

```java
/**
 * Exchanger: 两个线程交换数据
 * 场景: 生产者消费者交换缓冲区
 */
public class ExchangerDemo {
    
    public static void main(String[] args) {
        Exchanger<String> exchanger = new Exchanger<>();
        
        new Thread(() -> {
            try {
                String data = "来自线程A的数据";
                String received = exchanger.exchange(data);  // 交换
                System.out.println("A 收到: " + received);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        
        new Thread(() -> {
            try {
                String data = "来自线程B的数据";
                String received = exchanger.exchange(data);  // 交换
                System.out.println("B 收到: " + received);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
```

---

## 六、工具类对比

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          同步工具类对比                                           │
├───────────────────┬──────────────────────────────────────────────────────────────┤
│  工具类            │  特点                                                        │
├───────────────────┼──────────────────────────────────────────────────────────────┤
│  CountDownLatch   │  一次性，主线程等待多个子线程完成                             │
│                   │  计数减到0不可重置                                            │
├───────────────────┼──────────────────────────────────────────────────────────────┤
│  CyclicBarrier    │  可重用，多个线程互相等待，到齐后一起继续                     │
│                   │  一轮结束自动重置，可设置回调                                 │
├───────────────────┼──────────────────────────────────────────────────────────────┤
│  Semaphore        │  控制并发数，类似令牌桶                                       │
│                   │  可用于限流、连接池                                           │
├───────────────────┼──────────────────────────────────────────────────────────────┤
│  Exchanger        │  两个线程交换数据                                             │
│                   │  用于生产者消费者交换缓冲区                                   │
├───────────────────┼──────────────────────────────────────────────────────────────┤
│  Phaser           │  可重用，支持动态注册/注销                                    │
│                   │  比 CyclicBarrier 更灵活                                      │
└───────────────────┴──────────────────────────────────────────────────────────────┘
```

---

## 七、面试回答

### 30秒版本

> 常用的 JUC 并发工具类：
>
> - **CountDownLatch**：计数器，主线程等待多个子线程完成，一次性
> - **CyclicBarrier**：屏障，多个线程互相等待，可重用
> - **Semaphore**：信号量，控制并发访问数量
> - **Exchanger**：两个线程交换数据
>
> 还有 ConcurrentHashMap、BlockingQueue、AtomicInteger、ReentrantLock 等。

### 1分钟版本

> **同步工具类**：
>
> 1. **CountDownLatch**（倒计时门闩）
>    - 场景：主线程等待多个子线程完成
>    - countDown() 减计数，await() 阻塞等待归零
>    - 一次性，不可重置
>
> 2. **CyclicBarrier**（循环屏障）
>    - 场景：多个线程互相等待，到齐后一起继续
>    - 可重用，支持回调
>    - 适合分阶段任务
>
> 3. **Semaphore**（信号量）
>    - 场景：控制并发访问数量（限流、连接池）
>    - acquire() 获取许可，release() 释放
>
> 4. **Exchanger**
>    - 场景：两个线程交换数据
>
> **其他常用类**：
> - ConcurrentHashMap：线程安全 Map
> - CopyOnWriteArrayList：写时复制 List
> - BlockingQueue：阻塞队列
> - AtomicInteger/LongAdder：原子操作
> - ReentrantLock/ReadWriteLock：可重入锁
