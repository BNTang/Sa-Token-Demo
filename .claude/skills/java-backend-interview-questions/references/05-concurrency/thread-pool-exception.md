# Java线程池内部任务出异常后如何知道是哪个线程出了异常?

## 回答

线程池中任务异常可能被"吞掉"，需要通过以下方式捕获和追踪：

```
┌─────────────────────────────────────────────────────────────┐
│              线程池异常处理方案                              │
├─────────────────────────────────────────────────────────────┤
│ 1. try-catch            → 任务内部捕获                      │
│ 2. Future.get()         → submit()返回值获取异常             │
│ 3. afterExecute()       → 重写钩子方法                      │
│ 4. UncaughtExceptionHandler → 设置线程异常处理器            │
│ 5. 自定义ThreadFactory  → 给线程设置名称便于追踪             │
└─────────────────────────────────────────────────────────────┘
```

## execute() vs submit() 异常行为

```java
// execute() - 异常直接抛出，线程会终止
executor.execute(() -> {
    throw new RuntimeException("execute异常");
    // 异常会打印到控制台，线程终止
});

// submit() - 异常被封装在Future中
Future<?> future = executor.submit(() -> {
    throw new RuntimeException("submit异常");
});

// 只有调用get()时才会抛出异常
try {
    future.get();  // 阻塞等待
} catch (ExecutionException e) {
    Throwable cause = e.getCause();  // 获取原始异常
    System.out.println("捕获异常: " + cause.getMessage());
}
```

## 方案详解

### 方案1：任务内try-catch（推荐）

```java
executor.execute(() -> {
    try {
        // 业务逻辑
        riskyOperation();
    } catch (Exception e) {
        // 记录异常，包含线程信息
        log.error("线程[{}]执行异常: ", 
                  Thread.currentThread().getName(), e);
        // 可以上报监控系统
        MetricsReporter.reportError(e);
    }
});
```

### 方案2：Future.get()获取异常

```java
Future<?> future = executor.submit(() -> {
    throw new RuntimeException("任务异常");
});

try {
    future.get(5, TimeUnit.SECONDS);
} catch (ExecutionException e) {
    // 获取原始异常
    Throwable cause = e.getCause();
    log.error("任务执行异常: ", cause);
} catch (TimeoutException e) {
    log.error("任务超时");
    future.cancel(true);
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
}
```

### 方案3：重写afterExecute()

```java
public class TracingThreadPoolExecutor extends ThreadPoolExecutor {
    
    public TracingThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
                                      long keepAliveTime, TimeUnit unit,
                                      BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }
    
    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        
        // 处理execute()直接抛出的异常
        if (t != null) {
            log.error("线程[{}]执行异常: ", 
                      Thread.currentThread().getName(), t);
        }
        
        // 处理submit()封装的异常
        if (t == null && r instanceof Future<?>) {
            try {
                Future<?> future = (Future<?>) r;
                if (future.isDone()) {
                    future.get();  // 触发异常
                }
            } catch (CancellationException e) {
                log.warn("任务被取消");
            } catch (ExecutionException e) {
                log.error("线程[{}]执行异常: ", 
                          Thread.currentThread().getName(), e.getCause());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
```

### 方案4：UncaughtExceptionHandler

```java
// 自定义线程工厂
ThreadFactory threadFactory = new ThreadFactory() {
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    
    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r, "business-pool-" + threadNumber.getAndIncrement());
        
        // 设置未捕获异常处理器
        thread.setUncaughtExceptionHandler((t, e) -> {
            log.error("线程[{}]发生未捕获异常: ", t.getName(), e);
            // 发送告警
            AlertService.sendAlert("线程异常", t.getName(), e);
        });
        
        return thread;
    }
};

// 使用自定义工厂创建线程池
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    10, 20, 60, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(100),
    threadFactory
);
```

### 方案5：自定义ThreadFactory（推荐）

```java
// 使用Guava的ThreadFactoryBuilder
ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
    .setNameFormat("order-process-pool-%d")
    .setUncaughtExceptionHandler((t, e) -> {
        log.error("线程[{}]异常: {}", t.getName(), e.getMessage(), e);
    })
    .build();

// 使用
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    10, 20, 60, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(100),
    namedThreadFactory,
    new ThreadPoolExecutor.CallerRunsPolicy()
);

// 执行任务 - 异常会包含线程名称
executor.execute(() -> {
    log.info("当前线程: {}", Thread.currentThread().getName());
    // 输出: 当前线程: order-process-pool-1
});
```

## 完整示例：生产级线程池

```java
@Configuration
public class ThreadPoolConfig {
    
    @Bean("businessExecutor")
    public ThreadPoolExecutor businessExecutor() {
        // 自定义线程工厂
        ThreadFactory factory = new ThreadFactoryBuilder()
            .setNameFormat("business-pool-%d")
            .setUncaughtExceptionHandler((t, e) -> {
                log.error("[{}] Uncaught exception: ", t.getName(), e);
                Metrics.counter("thread.exception", 
                    "pool", "business", 
                    "thread", t.getName()
                ).increment();
            })
            .build();
        
        // 创建带异常追踪的线程池
        return new ThreadPoolExecutor(
            10, 20, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            factory,
            new ThreadPoolExecutor.CallerRunsPolicy()
        ) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                
                Throwable exception = t;
                if (exception == null && r instanceof Future<?>) {
                    try {
                        ((Future<?>) r).get();
                    } catch (ExecutionException e) {
                        exception = e.getCause();
                    } catch (Exception e) {
                        exception = e;
                    }
                }
                
                if (exception != null) {
                    log.error("[{}] Task exception: ", 
                              Thread.currentThread().getName(), exception);
                }
            }
        };
    }
}

// 使用
@Service
public class OrderService {
    
    @Autowired
    @Qualifier("businessExecutor")
    private ThreadPoolExecutor executor;
    
    public void processOrder(Order order) {
        executor.execute(() -> {
            // 业务逻辑
            // 如果抛异常，会被afterExecute捕获并记录线程名称
        });
    }
}
```

## 面试回答

### 30秒版本
> 线程池异常需要主动处理，否则可能被吞掉。主要方案：①任务内try-catch记录异常；②submit()返回Future，调用get()获取异常；③自定义ThreadPoolExecutor重写afterExecute()钩子方法；④设置UncaughtExceptionHandler；⑤自定义ThreadFactory给线程设置有意义的名称便于追踪。推荐用Guava的ThreadFactoryBuilder设置线程名称格式。

### 1分钟版本
> 线程池中任务异常需要特别处理，execute()会直接抛出异常导致线程终止，submit()会把异常封装在Future中，不调用get()就感知不到。
> 
> **追踪方案**：①**任务内try-catch**最直接，捕获异常记录日志；②**Future.get()**调用时会抛出ExecutionException，getCause()获取原始异常；③**重写afterExecute()**钩子方法，submit的任务需要从Future中get异常；④**UncaughtExceptionHandler**处理未捕获异常；⑤**自定义ThreadFactory**设置有意义的线程名称，如"order-pool-1"，便于从日志定位问题。
> 
> **推荐实践**：使用Guava的ThreadFactoryBuilder设置线程名称格式和异常处理器，同时重写afterExecute()统一处理异常，配合日志和监控系统实现异常追踪。

## 相关问题
- [[thread-pool-principle]] - 线程池工作原理
- [[thread-pool-shutdown]] - 线程池关闭
- [[completable-future]] - CompletableFuture异步处理
