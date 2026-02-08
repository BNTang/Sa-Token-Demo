# 如何在Java中控制多个线程的执行顺序?

## 回答

控制线程执行顺序的常用方法：

```
┌─────────────────────────────────────────────────────────────┐
│              线程执行顺序控制方法                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. join()           等待线程执行完毕                        │
│  2. CountDownLatch   计数器等待                             │
│  3. CyclicBarrier    互相等待到达屏障                        │
│  4. Semaphore        信号量控制                             │
│  5. Lock + Condition 条件变量                               │
│  6. volatile标志位   状态轮询                               │
│  7. BlockingQueue    阻塞队列传递                           │
│  8. CompletableFuture 异步编排                              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 方法1：Thread.join()

```java
// T1 → T2 → T3 顺序执行
public class JoinDemo {
    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(() -> System.out.println("T1执行"));
        Thread t2 = new Thread(() -> System.out.println("T2执行"));
        Thread t3 = new Thread(() -> System.out.println("T3执行"));
        
        t1.start();
        t1.join();  // 等待t1执行完
        
        t2.start();
        t2.join();  // 等待t2执行完
        
        t3.start();
        t3.join();  // 等待t3执行完
    }
}
```

## 方法2：CountDownLatch

```java
// T1 → T2 → T3 顺序执行
public class CountDownLatchDemo {
    public static void main(String[] args) {
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        
        Thread t1 = new Thread(() -> {
            System.out.println("T1执行");
            latch1.countDown();
        });
        
        Thread t2 = new Thread(() -> {
            try {
                latch1.await();  // 等待T1
                System.out.println("T2执行");
                latch2.countDown();
            } catch (InterruptedException e) {}
        });
        
        Thread t3 = new Thread(() -> {
            try {
                latch2.await();  // 等待T2
                System.out.println("T3执行");
            } catch (InterruptedException e) {}
        });
        
        t3.start();
        t2.start();
        t1.start();  // 启动顺序不影响执行顺序
    }
}
```

## 方法3：Semaphore

```java
// 交替打印ABC
public class SemaphoreDemo {
    private static Semaphore semA = new Semaphore(1);  // A先执行
    private static Semaphore semB = new Semaphore(0);
    private static Semaphore semC = new Semaphore(0);
    
    public static void main(String[] args) {
        new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                try {
                    semA.acquire();
                    System.out.print("A");
                    semB.release();
                } catch (InterruptedException e) {}
            }
        }).start();
        
        new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                try {
                    semB.acquire();
                    System.out.print("B");
                    semC.release();
                } catch (InterruptedException e) {}
            }
        }).start();
        
        new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                try {
                    semC.acquire();
                    System.out.print("C");
                    semA.release();
                } catch (InterruptedException e) {}
            }
        }).start();
    }
}
// 输出: ABCABCABCABC...
```

## 方法4：Lock + Condition

```java
// 三个线程顺序打印1-75
public class ConditionDemo {
    private final Lock lock = new ReentrantLock();
    private final Condition condA = lock.newCondition();
    private final Condition condB = lock.newCondition();
    private final Condition condC = lock.newCondition();
    private int state = 0;  // 0-A, 1-B, 2-C
    
    public void printA() {
        lock.lock();
        try {
            while (state != 0) {
                condA.await();
            }
            for (int i = 1; i <= 5; i++) System.out.print(i + " ");
            state = 1;
            condB.signal();
        } catch (InterruptedException e) {
        } finally {
            lock.unlock();
        }
    }
    
    public void printB() {
        lock.lock();
        try {
            while (state != 1) {
                condB.await();
            }
            for (int i = 6; i <= 10; i++) System.out.print(i + " ");
            state = 2;
            condC.signal();
        } catch (InterruptedException e) {
        } finally {
            lock.unlock();
        }
    }
    
    public void printC() {
        lock.lock();
        try {
            while (state != 2) {
                condC.await();
            }
            for (int i = 11; i <= 15; i++) System.out.print(i + " ");
            state = 0;
            condA.signal();
        } catch (InterruptedException e) {
        } finally {
            lock.unlock();
        }
    }
}
```

## 方法5：volatile标志位

```java
public class VolatileDemo {
    private volatile int flag = 1;
    
    public void printA() {
        while (flag != 1) { /* 自旋等待 */ }
        System.out.println("A");
        flag = 2;
    }
    
    public void printB() {
        while (flag != 2) { /* 自旋等待 */ }
        System.out.println("B");
        flag = 3;
    }
    
    public void printC() {
        while (flag != 3) { /* 自旋等待 */ }
        System.out.println("C");
        flag = 1;
    }
}
```

## 方法6：BlockingQueue

```java
public class BlockingQueueDemo {
    private static BlockingQueue<String> queueAB = new SynchronousQueue<>();
    private static BlockingQueue<String> queueBC = new SynchronousQueue<>();
    
    public static void main(String[] args) {
        new Thread(() -> {
            System.out.println("A执行");
            try {
                queueAB.put("done");  // 通知B
            } catch (InterruptedException e) {}
        }).start();
        
        new Thread(() -> {
            try {
                queueAB.take();  // 等待A
                System.out.println("B执行");
                queueBC.put("done");  // 通知C
            } catch (InterruptedException e) {}
        }).start();
        
        new Thread(() -> {
            try {
                queueBC.take();  // 等待B
                System.out.println("C执行");
            } catch (InterruptedException e) {}
        }).start();
    }
}
```

## 方法7：CompletableFuture

```java
public class CompletableFutureDemo {
    public static void main(String[] args) {
        CompletableFuture
            .runAsync(() -> System.out.println("T1执行"))
            .thenRun(() -> System.out.println("T2执行"))
            .thenRun(() -> System.out.println("T3执行"))
            .join();
    }
}
```

## 方法对比

| 方法 | 适用场景 | 优点 | 缺点 |
|------|----------|------|------|
| **join()** | 简单顺序 | 简单直接 | 必须持有线程引用 |
| **CountDownLatch** | 一次性等待 | 灵活 | 不可重用 |
| **CyclicBarrier** | 多阶段任务 | 可循环 | 需要相同数量 |
| **Semaphore** | 交替执行 | 灵活控制 | 代码较复杂 |
| **Condition** | 精确控制 | 最灵活 | 代码复杂 |
| **volatile** | 简单状态 | 无锁 | CPU空转 |
| **BlockingQueue** | 生产消费 | 解耦 | 需要传递数据 |
| **CompletableFuture** | 异步编排 | 链式优雅 | JDK8+ |

## 经典面试题

### 三个线程交替打印ABC

```java
// 最简洁的方案：Semaphore
public class PrintABC {
    private static Semaphore a = new Semaphore(1);
    private static Semaphore b = new Semaphore(0);
    private static Semaphore c = new Semaphore(0);
    
    public static void main(String[] args) {
        Runnable printA = () -> {
            for (int i = 0; i < 10; i++) {
                try {
                    a.acquire();
                    System.out.print("A");
                    b.release();
                } catch (InterruptedException e) {}
            }
        };
        
        Runnable printB = () -> {
            for (int i = 0; i < 10; i++) {
                try {
                    b.acquire();
                    System.out.print("B");
                    c.release();
                } catch (InterruptedException e) {}
            }
        };
        
        Runnable printC = () -> {
            for (int i = 0; i < 10; i++) {
                try {
                    c.acquire();
                    System.out.print("C");
                    a.release();
                } catch (InterruptedException e) {}
            }
        };
        
        new Thread(printA).start();
        new Thread(printB).start();
        new Thread(printC).start();
    }
}
```

## 面试回答

### 30秒版本
> 控制线程执行顺序常用方法：①join()等待线程完成；②CountDownLatch计数器等待；③Semaphore信号量控制获取顺序；④Lock+Condition精确条件控制；⑤CompletableFuture链式编排。简单顺序用join，交替执行用Semaphore，复杂编排用CompletableFuture。

### 1分钟版本
> 控制多个线程执行顺序有多种方法：
> 
> **简单顺序执行**：join()最直接，等待前一个线程执行完再启动下一个。CountDownLatch也可以，前一个countDown()，后一个await()。
> 
> **交替循环执行**：Semaphore最优雅，A执行完release给B的信号量，B获取后执行再release给C，循环交替。Lock+Condition也可以，用state变量+多个Condition精确控制。
> 
> **异步编排**：CompletableFuture最现代，thenRun()链式调用，代码简洁优雅。
> 
> **其他方式**：volatile标志位+自旋（不推荐，CPU空转）、BlockingQueue传递信号。
> 
> **选择建议**：简单顺序用join，交替打印用Semaphore，复杂异步编排用CompletableFuture。

## 相关问题
- [[countdownlatch]] - CountDownLatch
- [[semaphore]] - Semaphore
- [[completable-future]] - CompletableFuture
- [[thread-communication]] - 线程通信
