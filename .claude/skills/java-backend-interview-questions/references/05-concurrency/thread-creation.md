# Java 多线程创建方式

> Java 后端面试知识点 - 并发编程

---

## 创建线程的四种方式

| 方式 | 特点 | 推荐度 |
|------|------|--------|
| 继承 Thread | 简单，但不能继承其他类 | ⭐ |
| 实现 Runnable | 可继承其他类，无返回值 | ⭐⭐ |
| 实现 Callable | 有返回值，可抛异常 | ⭐⭐⭐ |
| 线程池 | 复用线程，资源可控 | ⭐⭐⭐⭐⭐ |

---

## 1. 继承 Thread 类

```java
public class MyThread extends Thread {
    
    @Override
    public void run() {
        System.out.println("线程执行：" + Thread.currentThread().getName());
    }
    
    public static void main(String[] args) {
        MyThread thread = new MyThread();
        thread.start();  // 启动线程，调用 run 方法
    }
}
```

**缺点**：
- Java 单继承，继承 Thread 后无法继承其他类
- 无法获取返回值
- 每次创建新线程，开销大

---

## 2. 实现 Runnable 接口

```java
public class MyRunnable implements Runnable {
    
    @Override
    public void run() {
        System.out.println("线程执行：" + Thread.currentThread().getName());
    }
    
    public static void main(String[] args) {
        Thread thread = new Thread(new MyRunnable());
        thread.start();
        
        // Lambda 简写
        new Thread(() -> {
            System.out.println("Lambda 线程执行");
        }).start();
    }
}
```

**优点**：
- 可以继承其他类
- 多个线程可共享同一个 Runnable

**缺点**：
- 无返回值
- 无法抛出受检异常

---

## 3. 实现 Callable 接口

```java
public class MyCallable implements Callable<Integer> {
    
    @Override
    public Integer call() throws Exception {
        Thread.sleep(1000);
        return 42;
    }
    
    public static void main(String[] args) throws Exception {
        FutureTask<Integer> futureTask = new FutureTask<>(new MyCallable());
        Thread thread = new Thread(futureTask);
        thread.start();
        
        // 获取返回值（阻塞等待）
        Integer result = futureTask.get();
        System.out.println("结果：" + result);
        
        // 超时获取
        Integer resultWithTimeout = futureTask.get(2, TimeUnit.SECONDS);
    }
}
```

**优点**：
- 可以获取返回值
- 可以抛出异常

**缺点**：
- 使用稍复杂
- 仍需手动管理线程

---

## 4. 线程池（推荐）

### 标准创建方式

```java
@Configuration
public class ThreadPoolConfig {
    
    @Bean("businessThreadPool")
    public ThreadPoolExecutor businessThreadPool() {
        return new ThreadPoolExecutor(
            10,                                      // 核心线程数
            20,                                      // 最大线程数
            60, TimeUnit.SECONDS,                    // 空闲线程存活时间
            new LinkedBlockingQueue<>(1000),         // 任务队列
            new ThreadFactoryBuilder()
                .setNameFormat("business-pool-%d")
                .setUncaughtExceptionHandler((t, e) -> {
                    log.error("线程{}发生异常", t.getName(), e);
                })
                .build(),
            new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
        );
    }
}
```

### 使用线程池

```java
@Service
@RequiredArgsConstructor
public class AsyncService {
    
    private final ThreadPoolExecutor businessThreadPool;
    
    // 提交无返回值任务
    public void executeAsync(Runnable task) {
        businessThreadPool.execute(task);
    }
    
    // 提交有返回值任务
    public <T> Future<T> submitAsync(Callable<T> task) {
        return businessThreadPool.submit(task);
    }
    
    // 批量执行
    public void batchExecute(List<Runnable> tasks) {
        for (Runnable task : tasks) {
            businessThreadPool.execute(task);
        }
    }
}
```

### Spring @Async

```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
    
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            log.error("异步方法{}发生异常", method.getName(), ex);
        };
    }
}

@Service
public class NotificationService {
    
    @Async
    public void sendEmailAsync(String email, String content) {
        // 异步发送邮件
    }
    
    @Async
    public CompletableFuture<Boolean> sendSmsAsync(String phone, String content) {
        // 异步发送短信，返回 Future
        boolean success = smsClient.send(phone, content);
        return CompletableFuture.completedFuture(success);
    }
}
```

---

## CompletableFuture（推荐）

```java
@Service
public class CompletableFutureService {
    
    @Autowired
    private ThreadPoolExecutor executor;
    
    // 基本使用
    public void basicUsage() {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            return "Hello";
        }, executor);
        
        String result = future.join();
    }
    
    // 链式调用
    public CompletableFuture<OrderDTO> getOrderWithDetails(Long orderId) {
        return CompletableFuture.supplyAsync(() -> orderService.getById(orderId), executor)
            .thenApplyAsync(order -> {
                // 查询订单详情
                order.setItems(itemService.getByOrderId(order.getId()));
                return order;
            }, executor)
            .thenApplyAsync(order -> {
                // 转换为 DTO
                return orderConverter.toDTO(order);
            }, executor);
    }
    
    // 并行执行
    public OrderVO getOrderVO(Long orderId) {
        CompletableFuture<Order> orderFuture = 
            CompletableFuture.supplyAsync(() -> orderService.getById(orderId), executor);
        
        CompletableFuture<User> userFuture = 
            CompletableFuture.supplyAsync(() -> userService.getById(order.getUserId()), executor);
        
        CompletableFuture<List<OrderItem>> itemsFuture = 
            CompletableFuture.supplyAsync(() -> itemService.getByOrderId(orderId), executor);
        
        // 等待所有完成
        CompletableFuture.allOf(orderFuture, userFuture, itemsFuture).join();
        
        return OrderVO.builder()
            .order(orderFuture.join())
            .user(userFuture.join())
            .items(itemsFuture.join())
            .build();
    }
    
    // 任意一个完成
    public String getFromAnySource() {
        CompletableFuture<String> source1 = 
            CompletableFuture.supplyAsync(() -> slowApi1(), executor);
        CompletableFuture<String> source2 = 
            CompletableFuture.supplyAsync(() -> slowApi2(), executor);
        
        // 谁先完成用谁
        return CompletableFuture.anyOf(source1, source2).join().toString();
    }
    
    // 异常处理
    public CompletableFuture<String> withExceptionHandler() {
        return CompletableFuture.supplyAsync(() -> {
            if (Math.random() > 0.5) {
                throw new RuntimeException("随机失败");
            }
            return "success";
        }, executor)
        .exceptionally(ex -> {
            log.error("执行失败", ex);
            return "default";
        });
    }
}
```

---

## 面试要点

### 核心答案

**问：Java 中如何创建多线程？**

答：Java 有四种创建线程的方式：

1. **继承 Thread 类**
   - 重写 `run()` 方法
   - 缺点：单继承限制，无返回值

2. **实现 Runnable 接口**
   - 实现 `run()` 方法
   - 优点：可继承其他类
   - 缺点：无返回值

3. **实现 Callable 接口**
   - 实现 `call()` 方法
   - 优点：有返回值，可抛异常
   - 配合 FutureTask 使用

4. **线程池（推荐）**
   - `ThreadPoolExecutor` 或 `ExecutorService`
   - 优点：线程复用、资源可控、统一管理

**实际开发中**：
- 禁止裸创建 Thread
- 统一使用线程池
- 推荐 CompletableFuture 处理异步

---

## 编码最佳实践

### ✅ 推荐做法

```java
// 1. 使用线程池
@Bean
public ThreadPoolExecutor executor() {
    return new ThreadPoolExecutor(
        Runtime.getRuntime().availableProcessors(),
        Runtime.getRuntime().availableProcessors() * 2,
        60, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(1000),
        new ThreadFactoryBuilder().setNameFormat("pool-%d").build(),
        new ThreadPoolExecutor.CallerRunsPolicy()
    );
}

// 2. 使用 CompletableFuture
CompletableFuture.supplyAsync(() -> doWork(), executor)
    .thenApply(result -> process(result))
    .exceptionally(ex -> handleError(ex));

// 3. 使用 @Async
@Async("businessExecutor")
public void asyncMethod() { }
```

### ❌ 避免做法

```java
// ❌ 裸创建线程
new Thread(() -> doWork()).start();

// ❌ 使用 Executors 工厂方法
Executors.newFixedThreadPool(10);     // 队列无界，OOM 风险
Executors.newCachedThreadPool();       // 线程无界，OOM 风险

// ❌ 不设置线程名称
new ThreadPoolExecutor(...);  // 排查问题困难

// ❌ 不处理异步异常
CompletableFuture.supplyAsync(() -> {
    throw new RuntimeException();  // 异常被吞
});
```
