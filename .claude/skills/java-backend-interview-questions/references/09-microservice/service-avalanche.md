# 什么是服务雪崩？

## 概念解析

**服务雪崩 (Service Avalanche)** 是指在分布式系统中，一个服务的故障引发连锁反应，导致依赖它的其他服务也相继崩溃，最终整个系统瘫痪。

```
┌─────────────────────────────────────────────────────────────┐
│                      服务雪崩示意图                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   正常状态:                                                  │
│   ┌─────────┐   ┌─────────┐   ┌─────────┐   ┌─────────┐   │
│   │ 服务 A  │ → │ 服务 B  │ → │ 服务 C  │ → │ 服务 D  │   │
│   │  ✅     │   │  ✅     │   │  ✅     │   │  ✅     │   │
│   └─────────┘   └─────────┘   └─────────┘   └─────────┘   │
│                                                             │
│   雪崩过程:                                                  │
│   ┌─────────┐   ┌─────────┐   ┌─────────┐   ┌─────────┐   │
│   │ 服务 A  │ → │ 服务 B  │ → │ 服务 C  │ → │ 服务 D  │   │
│   │  ⏳     │   │  ⏳     │   │  ⏳     │   │  ❌     │   │
│   │ 等待... │   │ 等待... │   │ 等待... │   │  故障   │   │
│   └─────────┘   └─────────┘   └─────────┘   └─────────┘   │
│        ↓             ↓             ↓             ↓         │
│   线程池满      线程池满      线程池满       服务不可用     │
│        ↓             ↓             ↓                       │
│      崩溃          崩溃          崩溃                       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 雪崩形成原因

```
┌─────────────────────────────────────────────────────────────┐
│                    雪崩形成的原因                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 服务提供者不可用                                        │
│      ├── 硬件故障（服务器宕机）                              │
│      ├── 程序 Bug（OOM、死锁）                               │
│      ├── 缓存击穿（大量请求穿透到 DB）                        │
│      └── 流量突增（秒杀、爬虫）                              │
│                                                             │
│   2. 重试机制加剧问题                                        │
│      ├── 用户重试（多次刷新页面）                            │
│      ├── 程序重试（配置了失败重试）                          │
│      └── 流量放大（1 次失败 → N 次重试）                     │
│                                                             │
│   3. 服务调用者被拖垮                                        │
│      ├── 同步调用等待（线程阻塞）                            │
│      ├── 资源耗尽（线程池满、连接池满）                       │
│      └── 连锁反应（被调用者变成故障源）                       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 真实案例分析

```
┌─────────────────────────────────────────────────────────────┐
│                    电商系统雪崩案例                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   场景：双11 大促，推荐服务故障                               │
│                                                             │
│   1. 推荐服务响应变慢 (DB 压力大)                            │
│      └── 请求处理从 100ms 变为 10s                          │
│                                                             │
│   2. 商品详情页调用推荐服务                                  │
│      └── 线程等待推荐响应，200 线程被占满                    │
│                                                             │
│   3. 详情页无法处理新请求                                    │
│      └── 用户看到超时，疯狂刷新                              │
│                                                             │
│   4. 下单服务调用详情页获取价格                              │
│      └── 下单服务线程也被阻塞                                │
│                                                             │
│   5. 网关调用下单服务                                        │
│      └── 网关线程耗尽，整站不可用                            │
│                                                             │
│   结果：一个推荐服务问题 → 整个电商平台瘫痪                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 预防方案

```
┌─────────────────────────────────────────────────────────────┐
│                    雪崩预防方案                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  1. 超时控制                                        │  │
│   │     └── 设置合理的超时时间，快速失败                  │  │
│   │     └── 避免无限等待占用资源                         │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  2. 服务熔断                                        │  │
│   │     └── 故障达到阈值后，快速失败，不再调用           │  │
│   │     └── 给下游服务喘息恢复的时间                     │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  3. 服务降级                                        │  │
│   │     └── 返回兜底数据，保证核心流程可用               │  │
│   │     └── 如：推荐服务降级返回热门商品                  │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  4. 服务限流                                        │  │
│   │     └── 限制请求速率，保护系统不被压垮               │  │
│   │     └── 令牌桶、滑动窗口限流                         │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  5. 资源隔离                                        │  │
│   │     └── 不同服务使用独立的线程池/连接池              │  │
│   │     └── 舱壁模式 (Bulkhead)                          │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 代码实现

### 1. 超时控制

```java
// Feign 超时配置
feign:
  client:
    config:
      default:
        connectTimeout: 3000
        readTimeout: 5000

// RestTemplate 超时
@Bean
public RestTemplate restTemplate() {
    HttpComponentsClientHttpRequestFactory factory = 
        new HttpComponentsClientHttpRequestFactory();
    factory.setConnectTimeout(3000);
    factory.setReadTimeout(5000);
    return new RestTemplate(factory);
}

// 异步调用 + 超时
CompletableFuture<Result> future = CompletableFuture.supplyAsync(() -> {
    return remoteService.call();
});
try {
    return future.get(5, TimeUnit.SECONDS);
} catch (TimeoutException e) {
    return fallbackResult();
}
```

### 2. 服务熔断

```java
// Resilience4j 熔断配置
@Service
public class OrderService {
    
    @CircuitBreaker(name = "productService", fallbackMethod = "getProductFallback")
    public Product getProduct(Long productId) {
        return productClient.getProduct(productId);
    }
    
    public Product getProductFallback(Long productId, Throwable t) {
        log.warn("熔断降级: {}", t.getMessage());
        return Product.defaultProduct(productId);
    }
}

// application.yml
resilience4j:
  circuitbreaker:
    instances:
      productService:
        failure-rate-threshold: 50          # 失败率阈值
        wait-duration-in-open-state: 30s    # 熔断时间
        sliding-window-size: 10             # 滑动窗口大小
        minimum-number-of-calls: 5          # 最小调用次数
```

### 3. 资源隔离（舱壁模式）

```java
// 线程池隔离
@Configuration
public class ThreadPoolConfig {
    
    @Bean("productThreadPool")
    public ExecutorService productThreadPool() {
        return new ThreadPoolExecutor(10, 20, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            new ThreadPoolExecutor.CallerRunsPolicy());
    }
    
    @Bean("orderThreadPool")
    public ExecutorService orderThreadPool() {
        return new ThreadPoolExecutor(20, 50, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(200),
            new ThreadPoolExecutor.CallerRunsPolicy());
    }
}

// 使用隔离的线程池
@Service
public class ProductService {
    
    @Autowired
    @Qualifier("productThreadPool")
    private ExecutorService productExecutor;
    
    public CompletableFuture<Product> getProductAsync(Long id) {
        return CompletableFuture.supplyAsync(() -> {
            return productClient.getProduct(id);
        }, productExecutor);  // 使用独立线程池
    }
}

// Resilience4j Bulkhead
@Bulkhead(name = "productService", type = Bulkhead.Type.THREADPOOL)
public Product getProduct(Long productId) {
    return productClient.getProduct(productId);
}
```

### 4. 限流保护

```java
// Sentinel 限流
@SentinelResource(value = "createOrder", 
    blockHandler = "createOrderBlockHandler",
    fallback = "createOrderFallback")
public Order createOrder(OrderRequest request) {
    return orderService.create(request);
}

public Order createOrderBlockHandler(OrderRequest request, BlockException ex) {
    throw new BusinessException("系统繁忙，请稍后重试");
}

// 配置规则
FlowRule rule = new FlowRule();
rule.setResource("createOrder");
rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
rule.setCount(100);  // 每秒 100 QPS
FlowRuleManager.loadRules(Collections.singletonList(rule));
```

## 雪崩 vs 熔断 vs 降级 vs 限流

```
┌─────────────────────────────────────────────────────────────┐
│                    概念对比                                  │
├──────────────┬──────────────────────────────────────────────┤
│   概念        │   说明                                       │
├──────────────┼──────────────────────────────────────────────┤
│   服务雪崩    │   问题：一个服务故障引发连锁反应              │
│   服务熔断    │   方案：故障时快速失败，阻断调用链            │
│   服务降级    │   方案：返回兜底数据，保证核心可用            │
│   服务限流    │   方案：限制请求速率，保护系统容量            │
├──────────────┴──────────────────────────────────────────────┤
│                                                             │
│   关系：                                                     │
│   雪崩 = 问题                                                │
│   熔断 + 降级 + 限流 = 解决方案                              │
│                                                             │
│   触发顺序：                                                 │
│   限流 → 熔断 → 降级                                        │
│   (入口控制) (故障隔离) (兜底返回)                           │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> 服务雪崩是指一个服务故障导致调用方线程阻塞，资源耗尽，进而引发连锁反应，整个系统崩溃。预防方案：**超时控制**（快速失败）、**服务熔断**（故障隔离）、**服务降级**（兜底返回）、**资源隔离**（线程池隔离）、**限流保护**（控制入口流量）。

### 1分钟版本

> **服务雪崩**：分布式系统中，下游服务故障 → 调用方线程阻塞等待 → 线程池耗尽 → 调用方也不可用 → 向上传导 → 整个系统瘫痪。
>
> **形成原因**：
> - 服务提供者不可用（Bug、OOM、流量突增）
> - 重试机制放大流量
> - 同步调用阻塞资源
>
> **预防方案**：
> 1. **超时控制**：快速失败，不无限等待
> 2. **服务熔断**：故障率达到阈值，直接返回失败
> 3. **服务降级**：返回缓存/默认数据
> 4. **资源隔离**：不同服务独立线程池（舱壁模式）
> 5. **限流保护**：控制入口流量（令牌桶、滑动窗口）
>
> **常用框架**：Sentinel、Resilience4j、Hystrix（已停止维护）

---

*关联文档：[circuit-breaker.md](circuit-breaker.md) | [service-degradation.md](service-degradation.md)*
