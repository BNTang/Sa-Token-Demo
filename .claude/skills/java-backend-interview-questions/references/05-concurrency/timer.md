# 什么是Java的Timer?

## 回答

Timer是Java早期提供的定时任务调度工具，基于单线程执行：

```
┌─────────────────────────────────────────────────────────────┐
│                      Timer 架构                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ┌──────────────┐     添加任务     ┌────────────────┐     │
│   │  TimerTask   │ ───────────────→ │   TaskQueue    │     │
│   │  (任务定义)   │                  │  (小顶堆队列)   │     │
│   └──────────────┘                  └───────┬────────┘     │
│                                             │               │
│                                    取出到期任务              │
│                                             ▼               │
│                                    ┌────────────────┐      │
│                                    │  TimerThread   │      │
│                                    │  (单线程执行)   │      │
│                                    └────────────────┘      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 基本使用

```java
public class TimerDemo {
    public static void main(String[] args) {
        Timer timer = new Timer("MyTimer");
        
        // 1. 延迟执行（一次性）
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("延迟3秒执行");
            }
        }, 3000);
        
        // 2. 固定延迟周期执行
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("每2秒执行");
            }
        }, 0, 2000);  // 首次立即执行，之后每2秒
        
        // 3. 固定速率周期执行
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                System.out.println("固定速率执行");
            }
        }, 0, 2000);
        
        // 4. 指定时间执行
        Date runTime = new Date(System.currentTimeMillis() + 5000);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("指定时间执行");
            }
        }, runTime);
        
        // 取消定时器
        // timer.cancel();
    }
}
```

## Timer的缺陷

### 1. 单线程问题

```java
Timer timer = new Timer();

// 任务1：执行5秒
timer.schedule(new TimerTask() {
    @Override
    public void run() {
        System.out.println("任务1开始: " + LocalTime.now());
        try {
            Thread.sleep(5000);  // 模拟耗时操作
        } catch (InterruptedException e) {}
        System.out.println("任务1结束: " + LocalTime.now());
    }
}, 0);

// 任务2：应该2秒后执行，但会被任务1阻塞
timer.schedule(new TimerTask() {
    @Override
    public void run() {
        System.out.println("任务2执行: " + LocalTime.now());
    }
}, 2000);

// 输出：
// 任务1开始: 10:00:00
// 任务1结束: 10:00:05
// 任务2执行: 10:00:05  ← 延迟了3秒！
```

### 2. 异常导致终止

```java
Timer timer = new Timer();

// 任务1：抛出异常
timer.schedule(new TimerTask() {
    @Override
    public void run() {
        System.out.println("任务1执行");
        throw new RuntimeException("任务1异常");
    }
}, 0);

// 任务2：不会执行！Timer线程已终止
timer.schedule(new TimerTask() {
    @Override
    public void run() {
        System.out.println("任务2执行");  // 永远不会执行
    }
}, 2000);

// 结果：任务1抛异常后，整个Timer终止，任务2不执行
```

### 3. 时间敏感问题

```java
// schedule() vs scheduleAtFixedRate()

// schedule(): 基于上次执行结束时间
// 如果任务耗时，会不断推迟

// scheduleAtFixedRate(): 基于初始时间
// 如果任务耗时超过间隔，会连续快速执行补偿
```

## Timer vs ScheduledThreadPool

```
┌─────────────────────────────────────────────────────────────┐
│              Timer vs ScheduledThreadPool                   │
├─────────────────┬───────────────┬───────────────────────────┤
│ 特性            │ Timer         │ ScheduledThreadPool       │
├─────────────────┼───────────────┼───────────────────────────┤
│ 线程数          │ 单线程        │ 可配置多线程               │
├─────────────────┼───────────────┼───────────────────────────┤
│ 异常处理        │ 异常终止Timer │ 只影响当前任务             │
├─────────────────┼───────────────┼───────────────────────────┤
│ 任务延迟        │ 单线程阻塞    │ 多线程并行                 │
├─────────────────┼───────────────┼───────────────────────────┤
│ 时间基准        │ 系统时间      │ nanoTime（单调）           │
├─────────────────┼───────────────┼───────────────────────────┤
│ 返回值          │ void          │ ScheduledFuture           │
├─────────────────┼───────────────┼───────────────────────────┤
│ 推荐程度        │ ❌ 不推荐     │ ✅ 推荐                    │
└─────────────────┴───────────────┴───────────────────────────┘
```

## 替代方案

### 1. ScheduledThreadPoolExecutor（推荐）

```java
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

scheduler.scheduleAtFixedRate(() -> {
    try {
        System.out.println("任务执行: " + LocalTime.now());
    } catch (Exception e) {
        // 捕获异常，不影响后续执行
        log.error("任务异常", e);
    }
}, 0, 2, TimeUnit.SECONDS);
```

### 2. Spring @Scheduled

```java
@Component
@EnableScheduling
public class ScheduledTasks {
    
    @Scheduled(fixedRate = 2000)  // 每2秒
    public void task1() {
        System.out.println("任务1: " + LocalTime.now());
    }
    
    @Scheduled(cron = "0 0 * * * ?")  // 每小时整点
    public void task2() {
        System.out.println("任务2: 整点执行");
    }
}
```

### 3. HashedWheelTimer（Netty）

```java
// 适合大量定时任务场景
HashedWheelTimer timer = new HashedWheelTimer(
    100, TimeUnit.MILLISECONDS,  // 时间轮精度
    512                           // 槽位数
);

timer.newTimeout(timeout -> {
    System.out.println("延迟任务执行");
}, 5, TimeUnit.SECONDS);
```

## 面试回答

### 30秒版本
> Timer是Java早期的定时任务工具，基于单线程和小顶堆队列实现。主要缺陷：①单线程，一个任务耗时会阻塞其他任务；②一个任务抛异常整个Timer终止；③基于系统时间，时钟调整会影响执行。现在推荐用ScheduledThreadPoolExecutor替代，它多线程并行、异常隔离、基于单调时间。

### 1分钟版本
> Timer是JDK1.3引入的定时任务调度器，内部由TaskQueue（小顶堆优先队列）存储任务，TimerThread单线程轮询执行。支持schedule()延迟执行、固定延迟周期执行，scheduleAtFixedRate()固定速率执行。
> 
> **主要缺陷**：①**单线程阻塞**，一个任务耗时会延迟其他任务执行；②**异常致命**，一个任务抛出未捕获异常会导致整个Timer终止，后续任务都不执行；③**时间敏感**，基于System.currentTimeMillis()，系统时钟调整会影响任务执行。
> 
> **推荐替代**：①**ScheduledThreadPoolExecutor**多线程、异常隔离、基于nanoTime；②**Spring @Scheduled**注解方式更简洁；③**HashedWheelTimer**适合大量定时任务的高性能场景。生产环境不建议使用Timer。

## 相关问题
- [[delayqueue-vs-scheduledpool]] - DelayQueue vs ScheduledThreadPool
- [[time-wheel]] - 时间轮算法
- [[executors-thread-pools]] - Executors线程池
