# Java线程池核心线程数在运行过程中能修改吗?

## 回答

**可以修改**。ThreadPoolExecutor提供了多个setter方法支持运行时动态调整线程池参数：

```java
ThreadPoolExecutor executor = ...;

// 动态修改核心线程数
executor.setCorePoolSize(20);

// 动态修改最大线程数
executor.setMaximumPoolSize(50);

// 动态修改空闲时间
executor.setKeepAliveTime(120, TimeUnit.SECONDS);

// 允许核心线程超时（配合keepAliveTime）
executor.allowCoreThreadTimeOut(true);
```

## 动态调整原理

```
┌─────────────────────────────────────────────────────────────┐
│                  setCorePoolSize(newSize)                   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  if (newSize > 当前核心数) {                                 │
│      // 扩容：立即创建新线程处理队列中的任务                    │
│      创建 (newSize - 当前核心数) 个新线程                      │
│  }                                                          │
│                                                             │
│  if (newSize < 当前核心数) {                                 │
│      // 缩容：中断空闲线程                                    │
│      interruptIdleWorkers()                                 │
│      // 正在执行的线程完成后自动退出                           │
│  }                                                          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 源码分析

### setCorePoolSize

```java
public void setCorePoolSize(int corePoolSize) {
    if (corePoolSize < 0 || maximumPoolSize < corePoolSize)
        throw new IllegalArgumentException();
    
    int delta = corePoolSize - this.corePoolSize;
    this.corePoolSize = corePoolSize;
    
    // 如果当前线程数大于新核心数，中断空闲线程
    if (workerCountOf(ctl.get()) > corePoolSize)
        interruptIdleWorkers();
    
    // 如果扩容，尝试创建新线程处理队列任务
    else if (delta > 0) {
        int k = Math.min(delta, workQueue.size());
        while (k-- > 0 && addWorker(null, true)) {
            if (workQueue.isEmpty())
                break;
        }
    }
}
```

### setMaximumPoolSize

```java
public void setMaximumPoolSize(int maximumPoolSize) {
    if (maximumPoolSize <= 0 || maximumPoolSize < corePoolSize)
        throw new IllegalArgumentException();
    
    this.maximumPoolSize = maximumPoolSize;
    
    // 如果当前线程数超过新最大值，中断空闲线程
    if (workerCountOf(ctl.get()) > maximumPoolSize)
        interruptIdleWorkers();
}
```

## 动态线程池实践

### 1. 配置中心集成方案

```java
@Component
public class DynamicThreadPoolManager {
    
    @Autowired
    private ThreadPoolExecutor businessPool;
    
    // 监听配置中心变化
    @NacosValue(value = "${thread.pool.core.size:10}", autoRefreshed = true)
    private int corePoolSize;
    
    @NacosValue(value = "${thread.pool.max.size:20}", autoRefreshed = true)
    private int maxPoolSize;
    
    @PostConstruct
    public void init() {
        // 注册配置变更监听
        NacosConfigManager.addListener("thread-pool-config", event -> {
            updateThreadPool();
        });
    }
    
    public void updateThreadPool() {
        // 先设置最大值（避免 max < core 异常）
        if (maxPoolSize > businessPool.getMaximumPoolSize()) {
            businessPool.setMaximumPoolSize(maxPoolSize);
            businessPool.setCorePoolSize(corePoolSize);
        } else {
            businessPool.setCorePoolSize(corePoolSize);
            businessPool.setMaximumPoolSize(maxPoolSize);
        }
        log.info("线程池参数已更新: core={}, max={}", corePoolSize, maxPoolSize);
    }
}
```

### 2. 动态队列大小方案

```java
// 标准LinkedBlockingQueue容量不可变
// 方案：自定义可变容量队列

public class ResizableLinkedBlockingQueue<E> extends LinkedBlockingQueue<E> {
    
    private volatile int capacity;
    
    public ResizableLinkedBlockingQueue(int capacity) {
        super(capacity);
        this.capacity = capacity;
    }
    
    // 通过反射修改capacity字段
    public synchronized void setCapacity(int newCapacity) {
        try {
            Field capacityField = LinkedBlockingQueue.class.getDeclaredField("capacity");
            capacityField.setAccessible(true);
            capacityField.setInt(this, newCapacity);
            this.capacity = newCapacity;
        } catch (Exception e) {
            throw new RuntimeException("修改队列容量失败", e);
        }
    }
}
```

### 3. 美团动态线程池思路

```
┌─────────────────────────────────────────────────────────────┐
│                    美团动态线程池架构                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ┌──────────┐    变更推送    ┌──────────────┐             │
│   │ 配置中心  │ ─────────────→ │ 动态线程池SDK │             │
│   │(Apollo等)│               │              │             │
│   └──────────┘               └──────┬───────┘             │
│                                     │                      │
│                                     ▼                      │
│   ┌──────────────────────────────────────────────┐        │
│   │ ThreadPoolExecutor                           │        │
│   │  • setCorePoolSize()                         │        │
│   │  • setMaximumPoolSize()                      │        │
│   │  • setKeepAliveTime()                        │        │
│   │  • 自定义队列.setCapacity()                   │        │
│   └──────────────────────────────────────────────┘        │
│                                     │                      │
│                                     ▼                      │
│   ┌──────────────────────────────────────────────┐        │
│   │ 监控告警                                      │        │
│   │  • 活跃线程数/队列大小/拒绝次数                 │        │
│   │  • Prometheus + Grafana                      │        │
│   └──────────────────────────────────────────────┘        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 注意事项

```java
// ⚠️ 修改顺序很重要！

// 错误示例（可能抛异常）
executor.setCorePoolSize(50);      // 如果当前max=20，会报错
executor.setMaximumPoolSize(100);

// 正确示例
// 扩容时：先设max，再设core
executor.setMaximumPoolSize(100);
executor.setCorePoolSize(50);

// 缩容时：先设core，再设max
executor.setCorePoolSize(10);
executor.setMaximumPoolSize(20);
```

## 开源动态线程池框架

| 框架 | 特点 |
|------|------|
| **Hippo4j** | 无中间件依赖、配置中心集成、监控告警 |
| **DynamicTp** | 轻量级、Spring Boot Starter、多配置中心 |
| **ThreadPoolTaskExecutor** | Spring封装、支持动态调整 |

## 面试回答

### 30秒版本
> 可以修改。ThreadPoolExecutor提供setCorePoolSize()和setMaximumPoolSize()方法支持运行时动态调整。扩容时会立即创建新线程处理队列任务，缩容时会中断空闲线程。注意修改顺序：扩容先设max再设core，缩容先设core再设max，避免IllegalArgumentException。实践中可结合配置中心如Nacos/Apollo实现动态线程池。

### 1分钟版本
> **可以动态修改**。ThreadPoolExecutor提供了setCorePoolSize()、setMaximumPoolSize()、setKeepAliveTime()等方法。
> 
> **扩容原理**：调用setCorePoolSize(新值)后，如果新值大于当前值，会立即创建新线程处理队列中等待的任务。**缩容原理**：如果新值小于当前线程数，会调用interruptIdleWorkers()中断空闲线程，正在执行任务的线程完成后自动退出。
> 
> **注意事项**：修改顺序很重要。扩容时先setMaximumPoolSize再setCorePoolSize，缩容时反过来，否则会抛IllegalArgumentException，因为core不能大于max。
> 
> **实践方案**：结合配置中心（Nacos/Apollo）监听配置变化，动态调整线程池参数。队列大小调整较麻烦，因为LinkedBlockingQueue容量不可变，需要自定义可变容量队列或用反射。美团开源的Hippo4j、DynamicTp都是成熟的动态线程池框架。

## 相关问题
- [[thread-pool-principle]] - 线程池工作原理
- [[executors-thread-pools]] - Executors线程池
- [[thread-pool-size]] - 线程池大小设置
