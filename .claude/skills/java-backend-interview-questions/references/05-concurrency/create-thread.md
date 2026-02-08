# Java 中如何创建多线程？

> Java 创建线程有 4 种方式：继承 Thread、实现 Runnable、实现 Callable、使用线程池

## 30秒速答

创建线程的 4 种方式：
1. **继承 Thread 类**: 重写 run() 方法
2. **实现 Runnable 接口**: 无返回值，run() 方法
3. **实现 Callable 接口**: 有返回值，call() 方法
4. **使用线程池**: Executors / ThreadPoolExecutor（推荐）

实际项目中**推荐使用线程池**，避免频繁创建销毁线程。

## 一分钟详解

### 方式 1: 继承 Thread 类

```java
public class MyThread extends Thread {
    @Override
    public void run() {
        System.out.println("Thread: " + Thread.currentThread().getName());
    }
}

// 使用
MyThread thread = new MyThread();
thread.start();

// 或者匿名类
new Thread() {
    @Override
    public void run() {
        System.out.println("Anonymous Thread");
    }
}.start();
```

### 方式 2: 实现 Runnable 接口

```java
public class MyRunnable implements Runnable {
    @Override
    public void run() {
        System.out.println("Runnable: " + Thread.currentThread().getName());
    }
}

// 使用
Thread thread = new Thread(new MyRunnable());
thread.start();

// Lambda 表达式（Java 8+）
new Thread(() -> {
    System.out.println("Lambda Runnable");
}).start();

// 方法引用
new Thread(System.out::println).start();
```

### 方式 3: 实现 Callable 接口

```java
public class MyCallable implements Callable<Integer> {
    @Override
    public Integer call() throws Exception {
        Thread.sleep(1000);
        return 42;  // 有返回值
    }
}

// 使用 FutureTask
FutureTask<Integer> futureTask = new FutureTask<>(new MyCallable());
new Thread(futureTask).start();

Integer result = futureTask.get();  // 阻塞获取结果
System.out.println("Result: " + result);

// 使用 ExecutorService
ExecutorService executor = Executors.newSingleThreadExecutor();
Future<Integer> future = executor.submit(new MyCallable());
System.out.println("Result: " + future.get());
executor.shutdown();
```

### 方式 4: 使用线程池（推荐）

```java
// 不推荐：Executors 工厂方法
ExecutorService executor1 = Executors.newFixedThreadPool(10);
ExecutorService executor2 = Executors.newCachedThreadPool();
ExecutorService executor3 = Executors.newSingleThreadExecutor();
// 这些可能导致 OOM（无界队列或无限创建线程）

// 推荐：自定义 ThreadPoolExecutor
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    5,                      // 核心线程数
    10,                     // 最大线程数
    60, TimeUnit.SECONDS,   // 空闲线程存活时间
    new ArrayBlockingQueue<>(100),  // 有界队列
    new ThreadFactory() {   // 自定义线程工厂
        private int count = 0;
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "worker-" + count++);
        }
    },
    new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略
);

// 提交任务
executor.execute(() -> System.out.println("Execute"));
Future<String> future = executor.submit(() -> "Submit result");

// 优雅关闭
executor.shutdown();
executor.awaitTermination(60, TimeUnit.SECONDS);
```

### 4 种方式对比

| 特性 | Thread | Runnable | Callable | 线程池 |
|------|--------|----------|----------|--------|
| 创建方式 | 继承 | 接口 | 接口 | 工厂/构造器 |
| 返回值 | 无 | 无 | 有 | 支持两者 |
| 异常处理 | 不可throws | 不可throws | 可throws | 支持 |
| 多继承问题 | 有 | 无 | 无 | 无 |
| 资源复用 | 否 | 否 | 否 | 是 |
| 推荐程度 | ❌ | ⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |

### 线程池参数详解

```
ThreadPoolExecutor 7 大参数：
┌─────────────────────────────────────────────────────────┐
│  new ThreadPoolExecutor(                                │
│      corePoolSize,     // 核心线程数，常驻              │
│      maximumPoolSize,  // 最大线程数                    │
│      keepAliveTime,    // 非核心线程空闲存活时间        │
│      unit,             // 时间单位                      │
│      workQueue,        // 任务队列                      │
│      threadFactory,    // 线程工厂                      │
│      handler           // 拒绝策略                      │
│  )                                                      │
└─────────────────────────────────────────────────────────┘

任务执行流程：
1. 线程数 < corePoolSize → 创建核心线程执行
2. 线程数 ≥ corePoolSize → 任务放入队列
3. 队列满 且 线程数 < maxPoolSize → 创建非核心线程
4. 队列满 且 线程数 = maxPoolSize → 执行拒绝策略
```

### 拒绝策略

```java
// 1. AbortPolicy（默认）: 抛出 RejectedExecutionException
new ThreadPoolExecutor.AbortPolicy();

// 2. CallerRunsPolicy: 调用者线程执行任务
new ThreadPoolExecutor.CallerRunsPolicy();

// 3. DiscardPolicy: 静默丢弃任务
new ThreadPoolExecutor.DiscardPolicy();

// 4. DiscardOldestPolicy: 丢弃队列最老的任务
new ThreadPoolExecutor.DiscardOldestPolicy();

// 自定义拒绝策略
new RejectedExecutionHandler() {
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        // 记录日志、持久化任务等
        log.warn("Task rejected: " + r.toString());
    }
};
```

## 关键记忆点

```
┌─────────────────────────────────────────────────────┐
│  创建线程 4 种方式：                                 │
│                                                     │
│  ┌──────────────────┬────────────────────────────┐ │
│  │ 继承 Thread      │ 简单，但有单继承限制       │ │
│  │ 实现 Runnable    │ 推荐，无返回值             │ │
│  │ 实现 Callable    │ 有返回值，可抛异常         │ │
│  │ 线程池           │ 最推荐！复用线程           │ │
│  └──────────────────┴────────────────────────────┘ │
│                                                     │
│  线程池 7 参数：                                    │
│  核心数、最大数、存活时间、时间单位、               │
│  队列、线程工厂、拒绝策略                           │
│                                                     │
│  阿里规约：禁用 Executors，用 ThreadPoolExecutor   │
└─────────────────────────────────────────────────────┘
```

## 面试追问

**Q: Runnable 和 Callable 的区别？**

| 区别 | Runnable | Callable |
|------|----------|----------|
| 返回值 | 无 (void) | 有 (泛型V) |
| 方法名 | run() | call() |
| 异常 | 不可声明 throws | 可声明 throws |
| 搭配 | Thread, ExecutorService | FutureTask, ExecutorService |
