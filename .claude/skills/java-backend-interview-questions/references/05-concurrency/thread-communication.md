# Java 中线程之间如何进行通信？

> 线程通信主要通过共享内存（volatile/synchronized）和消息传递（wait/notify/阻塞队列）实现

## 30秒速答

Java 线程通信方式：
1. **共享变量**: volatile、synchronized 保证可见性
2. **wait/notify**: Object 类的等待通知机制
3. **Condition**: Lock 的条件变量
4. **阻塞队列**: BlockingQueue 生产者消费者模式
5. **管道流**: PipedInputStream/PipedOutputStream

## 一分钟详解

### 1. volatile 共享变量

```java
public class VolatileCommunication {
    private volatile boolean running = true;
    
    public void run() {
        while (running) {
            // 工作
        }
        System.out.println("线程停止");
    }
    
    public void stop() {
        running = false;  // 立即对其他线程可见
    }
}

// 使用
VolatileCommunication task = new VolatileCommunication();
new Thread(task::run).start();
Thread.sleep(1000);
task.stop();  // 通知线程停止
```

### 2. wait/notify 机制

```java
public class WaitNotifyDemo {
    private final Object lock = new Object();
    private boolean dataReady = false;
    
    // 生产者
    public void produce() throws InterruptedException {
        synchronized (lock) {
            // 准备数据
            Thread.sleep(1000);
            dataReady = true;
            lock.notify();  // 通知等待的消费者
        }
    }
    
    // 消费者
    public void consume() throws InterruptedException {
        synchronized (lock) {
            while (!dataReady) {  // 用 while 而非 if，防止虚假唤醒
                lock.wait();      // 释放锁并等待
            }
            // 消费数据
            System.out.println("消费数据");
        }
    }
}
```

**执行流程：**
```
┌────────────────────────────────────────────────────────┐
│  消费者线程                 生产者线程                  │
├────────────────────────────────────────────────────────┤
│  synchronized(lock)                                    │
│  wait() 释放锁，进入等待                               │
│                            synchronized(lock)          │
│                            准备数据                     │
│                            notify() 唤醒消费者         │
│                            释放锁                      │
│  被唤醒，重新获取锁                                    │
│  消费数据                                              │
└────────────────────────────────────────────────────────┘
```

### 3. Condition 条件变量

```java
public class ConditionDemo {
    private final Lock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();
    
    private final Queue<Integer> queue = new LinkedList<>();
    private final int MAX_SIZE = 10;
    
    // 生产者
    public void produce(int item) throws InterruptedException {
        lock.lock();
        try {
            while (queue.size() == MAX_SIZE) {
                notFull.await();  // 等待队列不满
            }
            queue.add(item);
            notEmpty.signal();  // 通知消费者
        } finally {
            lock.unlock();
        }
    }
    
    // 消费者
    public int consume() throws InterruptedException {
        lock.lock();
        try {
            while (queue.isEmpty()) {
                notEmpty.await();  // 等待队列不空
            }
            int item = queue.poll();
            notFull.signal();  // 通知生产者
            return item;
        } finally {
            lock.unlock();
        }
    }
}
```

### 4. BlockingQueue 阻塞队列

```java
public class BlockingQueueDemo {
    private final BlockingQueue<String> queue = new ArrayBlockingQueue<>(10);
    
    // 生产者
    public void produce() throws InterruptedException {
        queue.put("data");  // 队列满时阻塞
    }
    
    // 消费者
    public void consume() throws InterruptedException {
        String data = queue.take();  // 队列空时阻塞
    }
}

// 常用 BlockingQueue 实现
// ArrayBlockingQueue: 有界数组队列
// LinkedBlockingQueue: 可选有界链表队列
// SynchronousQueue: 同步移交，无缓冲
// PriorityBlockingQueue: 优先级队列
// DelayQueue: 延迟队列
```

### 5. CountDownLatch / CyclicBarrier

```java
// CountDownLatch: 一个或多个线程等待其他线程完成
CountDownLatch latch = new CountDownLatch(3);

for (int i = 0; i < 3; i++) {
    new Thread(() -> {
        // 执行任务
        latch.countDown();  // 计数减1
    }).start();
}

latch.await();  // 等待计数归零
System.out.println("所有任务完成");

// CyclicBarrier: 多个线程互相等待到达屏障点
CyclicBarrier barrier = new CyclicBarrier(3, () -> {
    System.out.println("所有线程到达屏障");
});

for (int i = 0; i < 3; i++) {
    new Thread(() -> {
        // 执行第一阶段
        barrier.await();  // 等待其他线程
        // 执行第二阶段
    }).start();
}
```

### 6. Semaphore 信号量

```java
// 控制并发访问数量
Semaphore semaphore = new Semaphore(3);  // 允许3个线程并发

for (int i = 0; i < 10; i++) {
    new Thread(() -> {
        try {
            semaphore.acquire();  // 获取许可
            // 执行任务（最多3个线程同时执行）
            Thread.sleep(1000);
        } finally {
            semaphore.release();  // 释放许可
        }
    }).start();
}
```

### 通信方式对比

| 方式 | 特点 | 适用场景 |
|------|------|---------|
| volatile | 简单，只保证可见性 | 状态标志 |
| wait/notify | 需要 synchronized | 简单的等待通知 |
| Condition | 比 wait/notify 灵活 | 多条件等待 |
| BlockingQueue | 开箱即用 | 生产者消费者 |
| CountDownLatch | 一次性，不可重用 | 主线程等待子线程 |
| CyclicBarrier | 可重用，互相等待 | 分阶段任务 |
| Semaphore | 控制并发数 | 限流、资源池 |

## 关键记忆点

```
┌─────────────────────────────────────────────────────┐
│  线程通信方式速记：                                  │
│                                                     │
│  ┌──────────────────┬────────────────────────────┐ │
│  │ 共享变量         │ volatile、synchronized     │ │
│  │ 等待通知         │ wait/notify、Condition     │ │
│  │ 阻塞队列         │ BlockingQueue (最推荐)     │ │
│  │ 同步工具         │ CountDownLatch/Barrier     │ │
│  │ 信号量           │ Semaphore                  │ │
│  └──────────────────┴────────────────────────────┘ │
│                                                     │
│  生产者消费者推荐：BlockingQueue                    │
│  等待通知推荐：Condition (比 wait/notify 灵活)     │
│  状态标志推荐：volatile                            │
└─────────────────────────────────────────────────────┘
```

## 面试追问

**Q: wait() 为什么必须在 synchronized 块中调用？**

A: 因为 wait() 需要释放锁，如果没有 synchronized 就没有锁可释放。同时也是为了保证 wait() 和条件检查的原子性，避免**丢失信号**问题。
