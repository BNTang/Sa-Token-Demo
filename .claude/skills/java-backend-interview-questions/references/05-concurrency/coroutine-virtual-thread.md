# 什么是协程？Java 支持协程吗？

> 协程是用户态轻量级线程，由程序自身调度。Java 21 起通过虚拟线程原生支持协程

## 30秒速答

协程 (Coroutine) 特点：
- **用户态**: 不依赖操作系统内核调度
- **轻量级**: 内存占用极小（几 KB vs 线程 1MB）
- **协作式**: 主动让出执行权，非抢占式

Java 支持情况：
- **Java 19 预览**: Virtual Threads (虚拟线程)
- **Java 21 正式**: 原生支持，可创建百万级轻量线程

## 一分钟详解

### 线程 vs 协程

```
┌────────────────────────────────────────────────────────┐
│              线程 (Thread)    vs    协程 (Coroutine)   │
├────────────────────────────────────────────────────────┤
│  调度方式    │  OS内核调度(抢占式)  │  程序调度(协作式) │
│  切换开销    │  大(上下文切换)      │  小(用户态切换)   │
│  内存占用    │  ~1MB栈空间          │  ~几KB            │
│  数量限制    │  数千个              │  数百万个         │
│  创建销毁    │  昂贵                │  廉价             │
│  阻塞影响    │  阻塞OS线程          │  不阻塞OS线程     │
└────────────────────────────────────────────────────────┘
```

### Java 虚拟线程 (Virtual Threads)

```java
// Java 21: 虚拟线程使用

// 方式1: 直接创建
Thread vThread = Thread.startVirtualThread(() -> {
    System.out.println("Running in virtual thread");
});

// 方式2: Thread.ofVirtual()
Thread vThread2 = Thread.ofVirtual()
    .name("my-virtual-thread")
    .start(() -> {
        System.out.println("Hello from virtual thread");
    });

// 方式3: 虚拟线程池
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    IntStream.range(0, 100_000).forEach(i -> {
        executor.submit(() -> {
            Thread.sleep(Duration.ofSeconds(1));
            return i;
        });
    });
}  // 自动关闭

// 方式4: 结构化并发 (Java 21)
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    Future<String> user = scope.fork(() -> fetchUser());
    Future<Integer> order = scope.fork(() -> fetchOrder());
    
    scope.join();           // 等待所有任务
    scope.throwIfFailed();  // 传播异常
    
    return new Response(user.resultNow(), order.resultNow());
}
```

### 虚拟线程原理

```
虚拟线程与平台线程关系：
┌─────────────────────────────────────────────────────────┐
│  Virtual Threads (百万级)                               │
│  ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ... ┌────┐        │
│  │VT1 │ │VT2 │ │VT3 │ │VT4 │ │VT5 │     │VTn │        │
│  └──┬─┘ └──┬─┘ └──┬─┘ └──┬─┘ └──┬─┘     └──┬─┘        │
│     │      │      │      │      │          │           │
│     └──────┴──────┴──┬───┴──────┴──────────┘           │
│                      ↓  挂载/卸载                       │
│  ┌──────────────────────────────────────────────────┐  │
│  │  Carrier Threads (平台线程, 数量≈CPU核心数)      │  │
│  │  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐            │  │
│  │  │ PT1  │ │ PT2  │ │ PT3  │ │ PT4  │            │  │
│  │  └──────┘ └──────┘ └──────┘ └──────┘            │  │
│  └──────────────────────────────────────────────────┘  │
│                      ↓  1:1映射                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │  OS Threads (操作系统线程)                        │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘

工作原理：
1. 虚拟线程执行时，挂载到 Carrier Thread
2. 遇到阻塞 IO，虚拟线程卸载，Carrier 执行其他虚拟线程
3. IO 完成后，虚拟线程重新挂载到可用的 Carrier
```

### 性能对比

```java
// 传统线程：创建 10000 个线程
// 内存占用: 10000 × 1MB ≈ 10GB
// 可能 OOM 或线程数超限

// 虚拟线程：创建 1000000 个
// 内存占用: 1000000 × 1KB ≈ 1GB
// 轻松运行

// 实测代码
public static void main(String[] args) {
    long start = System.currentTimeMillis();
    
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        IntStream.range(0, 100_000).forEach(i -> {
            executor.submit(() -> {
                Thread.sleep(1000);  // 模拟 IO
                return i;
            });
        });
    }
    
    System.out.println("耗时: " + (System.currentTimeMillis() - start) + "ms");
    // 传统线程: 需要 100+ 秒
    // 虚拟线程: 约 1-2 秒
}
```

### 使用注意事项

```java
// ⚠️ 不适合 CPU 密集型任务
// 虚拟线程优势在于 IO 等待期间可执行其他任务
// CPU 密集型没有等待，无法发挥优势

// ⚠️ 避免 synchronized 同步块中的阻塞
synchronized (lock) {
    // 如果这里阻塞，会 pin 住 Carrier Thread
    Thread.sleep(1000);  // 不推荐
}

// ✅ 改用 ReentrantLock
lock.lock();
try {
    Thread.sleep(1000);  // 可以正常卸载
} finally {
    lock.unlock();
}

// ⚠️ ThreadLocal 可能占用更多内存
// 百万虚拟线程 × ThreadLocal 副本 = 大量内存
// 推荐使用 ScopedValue (Java 21 预览)
```

### 其他语言的协程

| 语言 | 协程实现 | 特点 |
|------|---------|------|
| Go | goroutine | 原生支持，go 关键字 |
| Kotlin | coroutines | suspend 函数 |
| Python | asyncio | async/await |
| JavaScript | async/await | 单线程事件循环 |
| Java 21+ | Virtual Thread | 与现有代码兼容 |

## 关键记忆点

```
┌─────────────────────────────────────────────────────┐
│  协程速记：                                          │
│                                                     │
│  「轻」轻量级，几 KB 内存 vs 线程 1MB                │
│  「用」用户态调度，无需内核介入                      │
│  「协」协作式切换，主动让出                          │
│  「多」可创建百万级                                  │
│                                                     │
│  Java 虚拟线程：                                     │
│  ┌─────────────────────────────────────────────┐   │
│  │ Java 21 正式支持                            │   │
│  │ Thread.startVirtualThread()                 │   │
│  │ Executors.newVirtualThreadPerTaskExecutor() │   │
│  │ 适合 IO 密集型，不适合 CPU 密集型           │   │
│  └─────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────┘
```

## 面试追问

**Q: Java 虚拟线程和 Go 的 goroutine 有什么区别？**

| 特性 | Java Virtual Thread | Go Goroutine |
|------|---------------------|--------------|
| 调度器 | ForkJoinPool | Go Runtime |
| 语法 | 与普通线程相同 | go 关键字 |
| 栈管理 | 动态栈 | 动态栈 |
| 通信 | 传统共享内存 | Channel(CSP模型) |
| 成熟度 | 2023 年 GA | 2009 年就有 |
