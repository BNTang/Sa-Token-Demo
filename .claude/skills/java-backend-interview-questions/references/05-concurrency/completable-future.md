# 什么是Java的CompletableFuture?

## 回答

CompletableFuture是JDK8引入的异步编程工具，支持链式调用和组合多个异步任务：

```
┌─────────────────────────────────────────────────────────────┐
│                CompletableFuture 特点                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────┐    ┌─────────┐    ┌─────────┐                │
│  │ 任务1   │───→│ 任务2   │───→│ 任务3   │  链式编排       │
│  └─────────┘    └─────────┘    └─────────┘                │
│                                                             │
│  ┌─────────┐                                               │
│  │ 任务A   │──┐                                            │
│  └─────────┘  │    ┌─────────┐                            │
│               ├───→│  汇聚   │  并行执行后汇聚              │
│  ┌─────────┐  │    └─────────┘                            │
│  │ 任务B   │──┘                                            │
│  └─────────┘                                               │
│                                                             │
│  • 非阻塞异步执行                                            │
│  • 链式调用编排                                              │
│  • 多任务组合                                                │
│  • 异常处理                                                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 创建异步任务

```java
// 1. 无返回值
CompletableFuture<Void> cf1 = CompletableFuture.runAsync(() -> {
    System.out.println("异步执行，无返回值");
});

// 2. 有返回值
CompletableFuture<String> cf2 = CompletableFuture.supplyAsync(() -> {
    return "异步执行结果";
});

// 3. 指定线程池
ExecutorService executor = Executors.newFixedThreadPool(10);
CompletableFuture<String> cf3 = CompletableFuture.supplyAsync(() -> {
    return "自定义线程池执行";
}, executor);

// 4. 已完成的Future
CompletableFuture<String> completed = CompletableFuture.completedFuture("已完成");
```

## 链式调用

### 转换类方法（thenApply系列）

```java
CompletableFuture.supplyAsync(() -> "Hello")
    .thenApply(s -> s + " World")       // 同步转换
    .thenApplyAsync(s -> s.toUpperCase())  // 异步转换
    .thenAccept(System.out::println);   // 消费结果
    
// 输出: HELLO WORLD
```

### 常用方法分类

```java
// ===== 转换（有返回值）=====
thenApply(Function)       // 同步转换
thenApplyAsync(Function)  // 异步转换

// ===== 消费（无返回值）=====
thenAccept(Consumer)      // 同步消费
thenAcceptAsync(Consumer) // 异步消费
thenRun(Runnable)         // 执行动作，不用上个结果

// ===== 组合 =====
thenCompose(Function)     // 扁平化（类似flatMap）
thenCombine(CF, BiFunction)  // 合并两个结果

// ===== 异常处理 =====
exceptionally(Function)   // 异常处理
handle(BiFunction)        // 同时处理结果和异常
whenComplete(BiConsumer)  // 完成时回调（不改变结果）
```

## 组合多个任务

### thenCompose（扁平化）

```java
// 任务2依赖任务1的结果
CompletableFuture<String> result = 
    CompletableFuture.supplyAsync(() -> getUserId())
        .thenCompose(userId -> CompletableFuture.supplyAsync(() -> getUser(userId)));

// 类似于：
// Future<Future<User>> 变成 Future<User>
```

### thenCombine（合并两个结果）

```java
CompletableFuture<String> name = CompletableFuture.supplyAsync(() -> "张三");
CompletableFuture<Integer> age = CompletableFuture.supplyAsync(() -> 25);

CompletableFuture<String> combined = name.thenCombine(age, 
    (n, a) -> n + "今年" + a + "岁");
    
System.out.println(combined.get());  // 张三今年25岁
```

### allOf（等待所有完成）

```java
CompletableFuture<String> cf1 = CompletableFuture.supplyAsync(() -> "任务1");
CompletableFuture<String> cf2 = CompletableFuture.supplyAsync(() -> "任务2");
CompletableFuture<String> cf3 = CompletableFuture.supplyAsync(() -> "任务3");

// 等待所有完成
CompletableFuture<Void> all = CompletableFuture.allOf(cf1, cf2, cf3);
all.join();

// 收集结果
List<String> results = Stream.of(cf1, cf2, cf3)
    .map(CompletableFuture::join)
    .collect(Collectors.toList());
```

### anyOf（任一完成）

```java
CompletableFuture<Object> any = CompletableFuture.anyOf(cf1, cf2, cf3);
System.out.println(any.get());  // 最先完成的结果
```

## 异常处理

```java
CompletableFuture.supplyAsync(() -> {
    if (true) throw new RuntimeException("出错了");
    return "result";
})
// 方式1: exceptionally - 只处理异常
.exceptionally(e -> {
    System.out.println("异常: " + e.getMessage());
    return "默认值";
})

// 方式2: handle - 同时处理结果和异常
.handle((result, ex) -> {
    if (ex != null) {
        return "异常默认值";
    }
    return result;
})

// 方式3: whenComplete - 完成时回调，不改变结果
.whenComplete((result, ex) -> {
    if (ex != null) {
        log.error("执行失败", ex);
    } else {
        log.info("执行成功: {}", result);
    }
});
```

## 实际应用示例

### 1. 并行接口聚合

```java
public UserDetail getUserDetail(Long userId) {
    // 并行调用三个接口
    CompletableFuture<User> userFuture = 
        CompletableFuture.supplyAsync(() -> userService.getById(userId));
    
    CompletableFuture<List<Order>> ordersFuture = 
        CompletableFuture.supplyAsync(() -> orderService.getByUserId(userId));
    
    CompletableFuture<Account> accountFuture = 
        CompletableFuture.supplyAsync(() -> accountService.getByUserId(userId));
    
    // 汇聚结果
    return CompletableFuture.allOf(userFuture, ordersFuture, accountFuture)
        .thenApply(v -> new UserDetail(
            userFuture.join(),
            ordersFuture.join(),
            accountFuture.join()
        ))
        .join();
}
```

### 2. 超时控制

```java
// JDK 9+
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> slowOperation())
    .orTimeout(5, TimeUnit.SECONDS)  // 5秒超时
    .exceptionally(e -> "超时默认值");

// JDK 8
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> slowOperation());
try {
    String result = future.get(5, TimeUnit.SECONDS);
} catch (TimeoutException e) {
    future.cancel(true);
}
```

### 3. 重试机制

```java
public <T> CompletableFuture<T> retry(Supplier<T> supplier, int maxRetries) {
    CompletableFuture<T> future = CompletableFuture.supplyAsync(supplier);
    
    for (int i = 0; i < maxRetries; i++) {
        future = future.exceptionally(e -> {
            log.warn("重试中...");
            return supplier.get();
        });
    }
    
    return future;
}
```

### 4. 批量并行处理

```java
public List<Result> batchProcess(List<Long> ids) {
    List<CompletableFuture<Result>> futures = ids.stream()
        .map(id -> CompletableFuture.supplyAsync(() -> process(id), executor))
        .collect(Collectors.toList());
    
    return futures.stream()
        .map(CompletableFuture::join)
        .collect(Collectors.toList());
}
```

## 注意事项

```java
// ⚠️ 默认使用ForkJoinPool.commonPool()
// 建议指定自定义线程池
CompletableFuture.supplyAsync(() -> task(), customExecutor);

// ⚠️ get()会抛出检查异常，join()抛出非检查异常
future.get();   // throws InterruptedException, ExecutionException
future.join();  // throws CompletionException

// ⚠️ 异常可能被吞掉
CompletableFuture.supplyAsync(() -> {
    throw new RuntimeException();  // 如果不调用get/join，异常被吞
});

// ⚠️ 链式调用中thenApply vs thenApplyAsync
.thenApply()      // 在上个任务的线程中执行
.thenApplyAsync() // 在线程池中异步执行
```

## 面试回答

### 30秒版本
> CompletableFuture是JDK8引入的异步编程工具，支持链式调用和多任务组合。supplyAsync()创建有返回值的异步任务，thenApply/thenAccept链式处理结果，thenCompose扁平化组合，allOf等待所有任务，anyOf任一完成。支持exceptionally/handle异常处理。默认用ForkJoinPool，生产建议自定义线程池。

### 1分钟版本
> CompletableFuture是JDK8引入的异步编程工具，解决了Future的阻塞获取结果问题，支持非阻塞的链式调用和任务组合。
> 
> **创建任务**：runAsync()无返回值，supplyAsync()有返回值，可指定自定义线程池。
> 
> **链式调用**：thenApply()转换结果，thenAccept()消费结果，thenRun()执行动作。Async后缀版本在线程池中异步执行。
> 
> **任务组合**：thenCompose()扁平化依赖任务，thenCombine()合并两个结果，allOf()等待所有完成，anyOf()任一完成。
> 
> **异常处理**：exceptionally()处理异常返回默认值，handle()同时处理结果和异常，whenComplete()完成回调。
> 
> **注意事项**：默认使用ForkJoinPool.commonPool()，生产环境建议指定线程池。典型应用：并行接口聚合、异步任务编排。

## 相关问题
- [[fork-join-pool]] - ForkJoinPool
- [[thread-pool-principle]] - 线程池原理
- [[concurrent-utils]] - 并发工具类
