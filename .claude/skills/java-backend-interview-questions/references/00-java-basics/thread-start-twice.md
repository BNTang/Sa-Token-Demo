# 如果一个线程在 Java 中被两次调用 start() 方法，会发生什么？

## 答案

```
┌─────────────────────────────────────────────────────────────┐
│                    两次调用 start() 的结果                   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   结果: 抛出 IllegalThreadStateException                    │
│                                                             │
│   原因: 线程状态不允许                                       │
│   • 线程只能从 NEW 状态调用 start()                         │
│   • 调用一次 start() 后，状态变为 RUNNABLE                  │
│   • 已经不是 NEW 状态，再次调用会抛异常                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 代码验证

```java
Thread thread = new Thread(() -> {
    System.out.println("线程执行");
});

thread.start();  // 第一次调用，正常
thread.start();  // 第二次调用，抛异常!

// 输出:
// 线程执行
// Exception in thread "main" java.lang.IllegalThreadStateException
```

## 源码分析

```java
// Thread.start() 源码
public synchronized void start() {
    // 检查线程状态，threadStatus != 0 表示不是 NEW 状态
    if (threadStatus != 0)
        throw new IllegalThreadStateException();
    
    // 加入线程组
    group.add(this);
    
    boolean started = false;
    try {
        start0();  // 调用 native 方法启动线程
        started = true;
    } finally {
        // ...
    }
}

// threadStatus 值含义:
// 0 = NEW (新建)
// 非0 = 已启动过 (RUNNABLE/BLOCKED/WAITING/TERMINATED 等)
```

## 线程状态转换

```
┌─────────────────────────────────────────────────────────────┐
│                    线程生命周期                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│       ┌─────────────────────────────────────────────────┐  │
│       │                                                 │  │
│       ▼                                                 │  │
│   ┌───────┐  start()  ┌──────────┐      ┌────────────┐ │  │
│   │  NEW  │ ────────→ │ RUNNABLE │ ───→ │ TERMINATED │ │  │
│   └───────┘           └──────────┘      └────────────┘ │  │
│       │                    │                            │  │
│       │                    ▼                            │  │
│       │              ┌─────────────┐                    │  │
│       │              │  BLOCKED /  │                    │  │
│       │              │  WAITING /  │                    │  │
│       │              │TIMED_WAITING│                    │  │
│       │              └─────────────┘                    │  │
│       │                                                 │  │
│       └── start() 只能在 NEW 状态调用 ──────────────────┘  │
│                                                             │
│   线程结束后 (TERMINATED) 也无法再次 start()                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 如果需要重复执行

```java
// ❌ 错误: 重复调用 start()
Thread t = new Thread(task);
t.start();
t.start();  // IllegalThreadStateException

// ✅ 正确: 创建新线程
Thread t1 = new Thread(task);
t1.start();

Thread t2 = new Thread(task);  // 新线程对象
t2.start();

// ✅ 推荐: 使用线程池
ExecutorService executor = Executors.newFixedThreadPool(10);
executor.submit(task);  // 可多次提交同一任务
executor.submit(task);
```

## 为什么这样设计

```
┌─────────────────────────────────────────────────────────────┐
│                    设计原因                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 线程与操作系统资源绑定                                  │
│      └── 一个 Thread 对应一个 OS 线程                       │
│      └── 线程结束后资源已释放，无法复用                     │
│                                                             │
│   2. 保证线程安全                                            │
│      └── 避免同一线程被多次启动导致的竞态条件               │
│                                                             │
│   3. 职责清晰                                                │
│      └── Thread 对象代表一次执行，而非执行单元              │
│      └── Runnable/Callable 才是可复用的任务                 │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> 会抛出 **IllegalThreadStateException**。因为 `start()` 只能在线程处于 **NEW** 状态时调用。第一次调用后状态变为 RUNNABLE，不再是 NEW，再次调用就会抛异常。线程结束后（TERMINATED）也无法再次启动。如需重复执行任务，应创建新 Thread 对象或使用**线程池**。

### 1分钟版本

> **结果**：抛出 IllegalThreadStateException
>
> **原因**：
> - start() 检查 threadStatus
> - 只有 NEW 状态 (threadStatus=0) 才能启动
> - 调用后状态变为 RUNNABLE
>
> **源码检查**：
> ```java
> if (threadStatus != 0)
>     throw new IllegalThreadStateException();
> ```
>
> **设计原因**：
> - Thread 与 OS 线程一一对应
> - 结束后资源已释放
>
> **正确做法**：
> - 创建新 Thread 对象
> - 使用线程池提交任务

---

*关联文档：[thread-lifecycle.md](../05-concurrency/thread-lifecycle.md) | [thread-pool.md](../05-concurrency/thread-pool.md)*
