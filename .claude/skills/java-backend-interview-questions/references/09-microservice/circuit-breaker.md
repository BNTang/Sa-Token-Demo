# 服务熔断详解

> 分类: 微服务 | 难度: ⭐⭐⭐ | 频率: 高频

---

## 一、什么是服务熔断

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          服务熔断                                                 │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  熔断器模式: 当下游服务故障时，快速失败，避免级联故障                             │
│                                                                                  │
│  类比: 家用电路保险丝，电流过大时自动断开，保护电器                               │
│                                                                                  │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │                                                                            │ │
│  │  服务A ──→ 服务B ──→ 服务C (故障)                                          │ │
│  │                         ↓                                                  │ │
│  │                   请求超时...                                              │ │
│  │                         ↓                                                  │ │
│  │  没有熔断: 服务A线程耗尽 ──→ 服务A也不可用 ──→ 级联故障                    │ │
│  │                                                                            │ │
│  │  有熔断: 服务B检测到C故障 ──→ 开启熔断 ──→ 快速返回fallback ──→ 保护A     │ │
│  │                                                                            │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 二、熔断器三种状态

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          熔断器状态机                                             │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│                        ┌─────────────────┐                                       │
│                        │    CLOSED       │  正常状态，请求正常通过               │
│                        │    (关闭)       │                                       │
│                        └────────┬────────┘                                       │
│                                 │                                                │
│                    失败率达到阈值 (如 50%)                                        │
│                                 ↓                                                │
│                        ┌─────────────────┐                                       │
│                        │     OPEN        │  熔断状态，快速失败                   │
│                        │    (打开)       │  返回 fallback                        │
│                        └────────┬────────┘                                       │
│                                 │                                                │
│                    等待超时 (如 5秒)                                              │
│                                 ↓                                                │
│                        ┌─────────────────┐                                       │
│                        │  HALF_OPEN      │  半开状态，允许部分请求               │
│                        │   (半开)        │  试探下游服务是否恢复                 │
│                        └────────┬────────┘                                       │
│                                 │                                                │
│               ┌─────────────────┴─────────────────┐                              │
│               │                                   │                              │
│          试探成功                            试探失败                            │
│               ↓                                   ↓                              │
│        回到 CLOSED                          回到 OPEN                            │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 三、熔断 vs 降级 vs 限流

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                    熔断 vs 降级 vs 限流                                           │
├─────────────────┬────────────────────────────────────────────────────────────────┤
│      策略       │                        说明                                     │
├─────────────────┼────────────────────────────────────────────────────────────────┤
│  熔断 (Circuit  │  下游服务故障时，快速失败，保护上游和自身                       │
│  Breaker)       │  触发条件: 错误率/慢调用率达到阈值                              │
├─────────────────┼────────────────────────────────────────────────────────────────┤
│  降级 (Fallback)│  服务不可用时，返回备选方案 (缓存、默认值、兜底逻辑)            │
│                 │  是熔断触发后的处理策略                                         │
├─────────────────┼────────────────────────────────────────────────────────────────┤
│  限流 (Rate     │  控制请求速率，防止过载                                         │
│  Limiting)      │  触发条件: QPS 超过阈值                                         │
└─────────────────┴────────────────────────────────────────────────────────────────┘

关系: 限流是入口防护，熔断是出口防护，降级是兜底策略
```

---

## 四、代码实现

### 4.1 Resilience4j (推荐)

```java
/**
 * Resilience4j 熔断器配置
 */
@Configuration
public class CircuitBreakerConfig {
    
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            // 失败率阈值 50%
            .failureRateThreshold(50)
            // 慢调用率阈值 50%
            .slowCallRateThreshold(50)
            // 慢调用时间阈值 2秒
            .slowCallDurationThreshold(Duration.ofSeconds(2))
            // 熔断打开持续时间 5秒
            .waitDurationInOpenState(Duration.ofSeconds(5))
            // 半开状态允许的请求数
            .permittedNumberOfCallsInHalfOpenState(3)
            // 滑动窗口大小
            .slidingWindowSize(10)
            .build();
        
        return CircuitBreakerRegistry.of(config);
    }
}

/**
 * 使用熔断器
 */
@Service
public class OrderService {
    
    private final CircuitBreaker circuitBreaker;
    private final PaymentClient paymentClient;
    
    public OrderService(CircuitBreakerRegistry registry, PaymentClient paymentClient) {
        this.circuitBreaker = registry.circuitBreaker("payment");
        this.paymentClient = paymentClient;
    }
    
    public PaymentResult pay(Order order) {
        // 使用熔断器装饰调用
        Supplier<PaymentResult> decoratedSupplier = CircuitBreaker
            .decorateSupplier(circuitBreaker, () -> paymentClient.pay(order));
        
        // 添加 fallback
        return Try.ofSupplier(decoratedSupplier)
            .recover(throwable -> {
                // 熔断时的降级逻辑
                return PaymentResult.fail("支付服务暂不可用");
            })
            .get();
    }
}
```

### 4.2 Spring Cloud 注解方式

```java
/**
 * 注解方式使用熔断
 */
@Service
public class PaymentService {
    
    @CircuitBreaker(name = "payment", fallbackMethod = "payFallback")
    public PaymentResult pay(Order order) {
        // 正常调用支付服务
        return paymentClient.pay(order);
    }
    
    // 降级方法
    private PaymentResult payFallback(Order order, Exception e) {
        log.error("支付服务熔断: {}", e.getMessage());
        return PaymentResult.fail("支付服务繁忙，请稍后重试");
    }
}
```

### 4.3 Sentinel (阿里)

```java
/**
 * Sentinel 熔断配置
 */
@Configuration
public class SentinelConfig {
    
    @PostConstruct
    public void init() {
        // 配置熔断规则
        List<DegradeRule> rules = new ArrayList<>();
        DegradeRule rule = new DegradeRule("paymentService")
            // 熔断策略: 慢调用比例
            .setGrade(RuleConstant.DEGRADE_GRADE_RT)
            // 阈值: 200ms
            .setCount(200)
            // 熔断时长: 10秒
            .setTimeWindow(10)
            // 最小请求数
            .setMinRequestAmount(5)
            // 慢调用比例阈值
            .setSlowRatioThreshold(0.5);
        
        rules.add(rule);
        DegradeRuleManager.loadRules(rules);
    }
}

/**
 * 使用 Sentinel
 */
@Service
public class PaymentService {
    
    @SentinelResource(value = "paymentService", 
                      fallback = "payFallback",
                      blockHandler = "payBlockHandler")
    public PaymentResult pay(Order order) {
        return paymentClient.pay(order);
    }
    
    // 业务异常降级
    public PaymentResult payFallback(Order order, Throwable t) {
        return PaymentResult.fail("支付失败");
    }
    
    // 熔断/限流降级
    public PaymentResult payBlockHandler(Order order, BlockException e) {
        return PaymentResult.fail("服务繁忙");
    }
}
```

---

## 五、面试回答

### 30秒版本

> **服务熔断**：当下游服务故障率达到阈值时，熔断器打开，后续请求快速失败返回 fallback，避免级联故障。
>
> **三种状态**：CLOSED（正常）→ OPEN（熔断）→ HALF_OPEN（半开试探）
>
> **与限流、降级的区别**：
> - 限流：入口防护，控制请求速率
> - 熔断：出口防护，下游故障时快速失败
> - 降级：兜底策略，返回备选方案
>
> 常用框架：Resilience4j、Sentinel。

### 1分钟版本

> **什么是服务熔断**：
> 类似电路保险丝，当下游服务故障时，熔断器打开，请求不再发往下游，而是快速返回 fallback，避免线程阻塞导致上游服务也不可用。
>
> **熔断器三种状态**：
> 1. CLOSED：正常状态，请求正常通过
> 2. OPEN：熔断状态，失败率超阈值，快速失败
> 3. HALF_OPEN：半开状态，允许部分请求试探，成功则关闭熔断
>
> **触发条件**：
> - 失败率阈值（如 50%）
> - 慢调用率阈值（如响应超过 2 秒）
>
> **与限流、降级的关系**：
> - 限流：入口流量控制，防止系统过载
> - 熔断：出口保护，下游故障时快速失败
> - 降级：熔断触发后的兜底策略
>
> **常用框架**：
> - Resilience4j：轻量级，Spring 官方推荐
> - Sentinel：阿里开源，功能丰富，有控制台
> - Hystrix：Netflix 开源，已停止维护
