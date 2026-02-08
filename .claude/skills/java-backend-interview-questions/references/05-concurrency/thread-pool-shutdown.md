# Java线程池shutdown与shutdownNow的区别

## 回答

两者都是关闭线程池的方法，但行为不同：

```
┌─────────────────────────────────────────────────────────────┐
│                    shutdown() vs shutdownNow()              │
├─────────────────┬───────────────────────────────────────────┤
│                 │  shutdown()     │  shutdownNow()          │
├─────────────────┼─────────────────┼─────────────────────────┤
│ 新任务          │ 拒绝接收        │ 拒绝接收                │
├─────────────────┼─────────────────┼─────────────────────────┤
│ 队列中任务      │ 继续执行完      │ 不执行，返回列表         │
├─────────────────┼─────────────────┼─────────────────────────┤
│ 正在执行任务    │ 等待完成        │ 尝试中断(interrupt)      │
├─────────────────┼─────────────────┼─────────────────────────┤
│ 返回值          │ void            │ List<Runnable>          │
├─────────────────┼─────────────────┼─────────────────────────┤
│ 温和程度        │ 优雅关闭        │ 暴力关闭                │
└─────────────────┴─────────────────┴─────────────────────────┘
```

## 状态转换图

```
                    shutdown()
    RUNNING ─────────────────────────→ SHUTDOWN
       │                                  │
       │ shutdownNow()                    │ 队列空 && 工作线程=0
       │                                  ▼
       └────────────────────────────→  STOP
                                         │
                                         │ 工作线程=0
                                         ▼
                                      TIDYING
                                         │
                                         │ terminated()执行完
                                         ▼
                                     TERMINATED
```

## 源码分析

### shutdown()

```java
public void shutdown() {
    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
        checkShutdownAccess();
        // 状态改为 SHUTDOWN
        advanceRunState(SHUTDOWN);
        // 中断空闲线程（不中断正在执行任务的线程）
        interruptIdleWorkers();
        onShutdown(); // 钩子方法
    } finally {
        mainLock.unlock();
    }
    tryTerminate();
}
```

**特点**：
- 状态变为SHUTDOWN
- 只中断空闲线程
- 队列中任务继续执行
- 等待所有任务完成

### shutdownNow()

```java
public List<Runnable> shutdownNow() {
    List<Runnable> tasks;
    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
        checkShutdownAccess();
        // 状态改为 STOP
        advanceRunState(STOP);
        // 中断所有线程（包括正在执行任务的）
        interruptWorkers();
        // 清空队列，返回未执行的任务
        tasks = drainQueue();
    } finally {
        mainLock.unlock();
    }
    tryTerminate();
    return tasks;
}

private void interruptWorkers() {
    for (Worker w : workers) {
        w.thread.interrupt();  // 中断所有工作线程
    }
}
```

**特点**：
- 状态变为STOP
- 中断所有工作线程
- 清空队列，返回未执行任务
- 不保证任务正常完成

## 代码示例

```java
public class ShutdownDemo {
    
    public static void main(String[] args) throws InterruptedException {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            2, 4, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(10)
        );
        
        // 提交任务
        for (int i = 0; i < 10; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    System.out.println("Task " + taskId + " 开始执行");
                    Thread.sleep(2000);
                    System.out.println("Task " + taskId + " 执行完成");
                } catch (InterruptedException e) {
                    System.out.println("Task " + taskId + " 被中断");
                }
            });
        }
        
        // 方式1：优雅关闭
        gracefulShutdown(executor);
        
        // 方式2：强制关闭（二选一）
        // forceShutdown(executor);
    }
    
    // 优雅关闭
    static void gracefulShutdown(ExecutorService executor) throws InterruptedException {
        executor.shutdown();  // 停止接收新任务
        
        // 等待任务完成，最多等待60秒
        if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
            // 超时后强制关闭
            List<Runnable> droppedTasks = executor.shutdownNow();
            System.out.println("强制关闭，丢弃任务数: " + droppedTasks.size());
            
            // 再等待一段时间
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                System.err.println("线程池未能正常关闭");
            }
        }
        System.out.println("线程池已关闭");
    }
    
    // 强制关闭
    static void forceShutdown(ExecutorService executor) {
        List<Runnable> notExecuted = executor.shutdownNow();
        System.out.println("未执行的任务数: " + notExecuted.size());
    }
}
```

## 最佳实践

### 推荐的关闭模式

```java
/**
 * 线程池优雅关闭工具类
 */
public class ExecutorShutdownUtil {
    
    /**
     * 优雅关闭线程池
     * 1. 先shutdown()拒绝新任务
     * 2. 等待现有任务完成
     * 3. 超时后shutdownNow()强制关闭
     */
    public static void shutdownGracefully(ExecutorService executor, 
                                          long timeout, 
                                          TimeUnit unit) {
        // 禁止提交新任务
        executor.shutdown();
        
        try {
            // 等待现有任务完成
            if (!executor.awaitTermination(timeout, unit)) {
                // 超时，强制关闭
                List<Runnable> dropped = executor.shutdownNow();
                log.warn("强制关闭，丢弃{}个任务", dropped.size());
                
                // 等待被中断的任务响应中断
                if (!executor.awaitTermination(timeout, unit)) {
                    log.error("线程池无法关闭");
                }
            }
        } catch (InterruptedException e) {
            // 当前线程被中断，强制关闭
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

// 使用
@PreDestroy
public void destroy() {
    ExecutorShutdownUtil.shutdownGracefully(executor, 30, TimeUnit.SECONDS);
}
```

### Spring中的线程池关闭

```java
@Bean
public ThreadPoolTaskExecutor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(10);
    executor.setMaxPoolSize(20);
    executor.setQueueCapacity(100);
    
    // 设置优雅关闭
    executor.setWaitForTasksToCompleteOnShutdown(true);  // 等待任务完成
    executor.setAwaitTerminationSeconds(60);             // 最多等待60秒
    
    return executor;
}
```

## 相关方法

| 方法 | 说明 |
|------|------|
| `shutdown()` | 优雅关闭，等待任务完成 |
| `shutdownNow()` | 立即关闭，中断所有线程 |
| `isShutdown()` | 是否调用过shutdown/shutdownNow |
| `isTerminated()` | 是否完全终止 |
| `awaitTermination()` | 阻塞等待终止 |

## 面试回答

### 30秒版本
> shutdown()是优雅关闭，状态变SHUTDOWN，拒绝新任务但队列中任务继续执行，等待所有任务完成。shutdownNow()是暴力关闭，状态变STOP，中断所有工作线程包括正在执行的，清空队列并返回未执行的任务列表。最佳实践是先shutdown()，再awaitTermination()等待超时，最后shutdownNow()强制关闭。

### 1分钟版本
> shutdown()和shutdownNow()都是关闭线程池，但行为不同。
> 
> **shutdown()**是优雅关闭：状态变为SHUTDOWN，拒绝新任务提交，但队列中等待的任务会继续执行，只中断空闲线程，正在执行的任务会等待完成。
> 
> **shutdownNow()**是暴力关闭：状态变为STOP，拒绝新任务，调用interruptWorkers()中断所有线程包括正在执行任务的，清空队列并返回未执行的任务列表List<Runnable>，不保证任务正常完成。
> 
> **最佳实践**是组合使用：先调用shutdown()拒绝新任务，然后awaitTermination()等待一段时间让现有任务完成，如果超时仍未完成再调用shutdownNow()强制关闭。Spring的ThreadPoolTaskExecutor可以设置waitForTasksToCompleteOnShutdown(true)和awaitTerminationSeconds来自动处理优雅关闭。

## 相关问题
- [[thread-pool-principle]] - 线程池工作原理
- [[executors-thread-pools]] - Executors线程池
- [[thread-pool-exception]] - 线程池异常处理
