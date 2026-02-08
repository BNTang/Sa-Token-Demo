# 线程的生命周期在 Java 中是如何定义的？

## 线程状态概览

```
┌─────────────────────────────────────────────────────────────┐
│                    Java 线程 6 种状态                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   定义在 java.lang.Thread.State 枚举中:                      │
│                                                             │
│   1. NEW          - 新建，尚未启动                          │
│   2. RUNNABLE     - 可运行 (包含就绪和运行中)               │
│   3. BLOCKED      - 阻塞，等待获取锁                        │
│   4. WAITING      - 无限等待                                │
│   5. TIMED_WAITING - 限时等待                               │
│   6. TERMINATED   - 终止                                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 状态转换图

```
┌─────────────────────────────────────────────────────────────┐
│                    线程状态转换                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│                    ┌───────────┐                            │
│                    │    NEW    │  new Thread()              │
│                    └─────┬─────┘                            │
│                          │ start()                          │
│                          ▼                                  │
│                    ┌───────────┐                            │
│              ┌────>│ RUNNABLE  │<────────────────┐          │
│              │     └─────┬─────┘                 │          │
│              │           │                       │          │
│   ┌──────────┼───────────┼───────────────────────┼───────┐ │
│   │          │           │                       │       │ │
│   │   synchronized       │              notify() │       │ │
│   │   获取锁失败          │              interrupt│       │ │
│   │          │           │                       │       │ │
│   │          ▼           │           ┌───────────┴───┐   │ │
│   │   ┌───────────┐      │           │    WAITING    │   │ │
│   │   │  BLOCKED  │      │           │   wait()      │   │ │
│   │   │  等待锁   │      │           │   join()      │   │ │
│   │   └─────┬─────┘      │           │   park()      │   │ │
│   │         │            │           └───────────────┘   │ │
│   │   获取到锁│           │                               │ │
│   │         └────────────┼───────────────────────────────┘ │
│   │                      │                                 │
│   │                      │           ┌───────────────────┐ │
│   │                      │           │  TIMED_WAITING    │ │
│   │                      │           │  sleep(time)      │ │
│   │                      │           │  wait(time)       │ │
│   │                      │           │  join(time)       │ │
│   │                      │           └─────────┬─────────┘ │
│   │                      │                     │           │
│   │                      │         超时/唤醒   │           │
│   │                      └──────────┬──────────┘           │
│   │                                 │                      │
│   └─────────────────────────────────│──────────────────────┘
│                                     │                       │
│                                     ▼                       │
│                              ┌───────────┐                  │
│                              │TERMINATED │ run() 结束       │
│                              └───────────┘                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 各状态详解

### 1. NEW (新建)

```java
// 线程创建但未启动
Thread thread = new Thread(() -> {
    System.out.println("Running");
});
System.out.println(thread.getState());  // NEW
```

### 2. RUNNABLE (可运行)

```java
// 调用 start() 后进入 RUNNABLE
thread.start();
System.out.println(thread.getState());  // RUNNABLE

// 注意: RUNNABLE 包含两种情况
// 1. Ready (就绪): 等待 CPU 时间片
// 2. Running (运行): 正在 CPU 上执行
```

### 3. BLOCKED (阻塞)

```java
// 等待获取 synchronized 锁时进入 BLOCKED
Object lock = new Object();

Thread t1 = new Thread(() -> {
    synchronized (lock) {
        try { Thread.sleep(5000); } catch (Exception e) {}
    }
});

Thread t2 = new Thread(() -> {
    synchronized (lock) {
        System.out.println("Got lock");
    }
});

t1.start();
Thread.sleep(100);  // 确保 t1 先获取锁
t2.start();
Thread.sleep(100);
System.out.println(t2.getState());  // BLOCKED
```

### 4. WAITING (无限等待)

```java
// 以下方法会进入 WAITING
// - Object.wait()
// - Thread.join()
// - LockSupport.park()

Thread t = new Thread(() -> {
    synchronized (lock) {
        try {
            lock.wait();  // 进入 WAITING
        } catch (Exception e) {}
    }
});

t.start();
Thread.sleep(100);
System.out.println(t.getState());  // WAITING
```

### 5. TIMED_WAITING (限时等待)

```java
// 以下方法会进入 TIMED_WAITING
// - Thread.sleep(time)
// - Object.wait(time)
// - Thread.join(time)
// - LockSupport.parkNanos(time)

Thread t = new Thread(() -> {
    try {
        Thread.sleep(5000);  // 进入 TIMED_WAITING
    } catch (Exception e) {}
});

t.start();
Thread.sleep(100);
System.out.println(t.getState());  // TIMED_WAITING
```

### 6. TERMINATED (终止)

```java
// run() 方法执行完毕或抛出异常
Thread t = new Thread(() -> System.out.println("Done"));
t.start();
t.join();  // 等待线程结束
System.out.println(t.getState());  // TERMINATED
```

## 状态转换触发条件

```
┌─────────────────────────────────────────────────────────────┐
│                    状态转换条件                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   RUNNABLE → BLOCKED                                        │
│   └── 等待获取 synchronized 锁                              │
│                                                             │
│   BLOCKED → RUNNABLE                                        │
│   └── 获取到 synchronized 锁                                │
│                                                             │
│   RUNNABLE → WAITING                                        │
│   ├── Object.wait()                                         │
│   ├── Thread.join()                                         │
│   └── LockSupport.park()                                    │
│                                                             │
│   WAITING → RUNNABLE                                        │
│   ├── Object.notify() / notifyAll()                         │
│   ├── 被 join 的线程结束                                    │
│   ├── LockSupport.unpark()                                  │
│   └── interrupt()                                           │
│                                                             │
│   RUNNABLE → TIMED_WAITING                                  │
│   ├── Thread.sleep(time)                                    │
│   ├── Object.wait(time)                                     │
│   ├── Thread.join(time)                                     │
│   └── LockSupport.parkNanos(time)                           │
│                                                             │
│   TIMED_WAITING → RUNNABLE                                  │
│   ├── 超时                                                  │
│   ├── notify() / unpark()                                   │
│   └── interrupt()                                           │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## BLOCKED vs WAITING vs TIMED_WAITING

```
┌─────────────────────────────────────────────────────────────┐
│                    阻塞状态对比                              │
├────────────────┬────────────────────────────────────────────┤
│   状态          │   说明                                     │
├────────────────┼────────────────────────────────────────────┤
│   BLOCKED      │ 等待 synchronized 锁                       │
│                │ 被动的，无法响应中断                        │
├────────────────┼────────────────────────────────────────────┤
│   WAITING      │ 主动调用 wait/join/park                    │
│                │ 可响应 notify/interrupt                     │
│                │ 无超时，需要被唤醒                          │
├────────────────┼────────────────────────────────────────────┤
│   TIMED_WAITING│ 带超时的等待                               │
│                │ 超时后自动恢复                              │
│                │ 也可被提前唤醒                              │
├────────────────┴────────────────────────────────────────────┤
│                                                             │
│   注意: Lock.lock() 阻塞时状态是 WAITING，不是 BLOCKED      │
│   BLOCKED 只针对 synchronized                               │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> Java 线程有 6 种状态：**NEW**（创建未启动）、**RUNNABLE**（可运行，包含就绪和运行）、**BLOCKED**（等待 synchronized 锁）、**WAITING**（无限等待，wait/join/park）、**TIMED_WAITING**（限时等待，sleep/wait(time)）、**TERMINATED**（终止）。注意 BLOCKED 只针对 synchronized，Lock 阻塞时是 WAITING。

### 1分钟版本

> **6 种状态（Thread.State 枚举）**：
>
> 1. **NEW**：new Thread() 后，未调用 start()
> 2. **RUNNABLE**：调用 start() 后，包括就绪和运行
> 3. **BLOCKED**：等待 synchronized 锁
> 4. **WAITING**：无限等待
>    - wait()、join()、park()
>    - 需要 notify/unpark 唤醒
> 5. **TIMED_WAITING**：限时等待
>    - sleep(time)、wait(time)、join(time)
>    - 超时自动恢复
> 6. **TERMINATED**：run() 结束
>
> **关键区别**：
> - BLOCKED：等 synchronized 锁，被动
> - WAITING：主动调用，可响应中断
> - Lock.lock() 阻塞是 WAITING，不是 BLOCKED

---

*关联文档：[volatile-keyword.md](volatile-keyword.md) | [synchronized.md](synchronized.md)*
