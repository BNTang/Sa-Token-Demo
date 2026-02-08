# 什么是服务降级？

## 概念解析

**服务降级 (Service Degradation)** 是指当系统压力过大或部分服务不可用时，**主动放弃非核心功能**，保证核心业务正常运行的一种容错策略。

```
┌─────────────────────────────────────────────────────────────┐
│                      服务降级 vs 服务熔断                     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   服务降级 (Degradation)          服务熔断 (Circuit Breaker) │
│   ┌─────────────────────┐       ┌─────────────────────────┐ │
│   │ • 主动放弃非核心功能  │       │ • 被动阻断故障服务      │ │
│   │ • 返回兜底数据       │       │ • 快速失败，不再调用    │ │
│   │ • 保证核心业务可用   │       │ • 防止故障扩散          │ │
│   │ • 预防性策略        │       │ • 响应性策略            │ │
│   └─────────────────────┘       └─────────────────────────┘ │
│                                                             │
│   两者常配合使用：熔断触发后，通常会执行降级逻辑              │
└─────────────────────────────────────────────────────────────┘
```

## 降级场景

```
┌─────────────────────────────────────────────────────────────┐
│                      降级触发场景                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 系统负载过高                                           │
│      ├── CPU > 80%                                          │
│      ├── 内存不足                                           │
│      └── 线程池满                                           │
│                                                             │
│   2. 依赖服务异常                                           │
│      ├── 调用超时                                           │
│      ├── 调用失败率高                                        │
│      └── 服务不可用                                         │
│                                                             │
│   3. 流量突增                                               │
│      ├── 大促活动                                           │
│      ├── 热点事件                                           │
│      └── 恶意攻击                                           │
│                                                             │
│   4. 主动降级（运维开关）                                    │
│      ├── 预期流量高峰                                        │
│      ├── 系统发布维护                                        │
│      └── 资源紧张                                           │
└─────────────────────────────────────────────────────────────┘
```

## 降级策略

```
┌─────────────────────────────────────────────────────────────┐
│                      常见降级策略                            │
├──────────────┬──────────────────────────────────────────────┤
│   策略        │   说明                                       │
├──────────────┼──────────────────────────────────────────────┤
│   返回默认值  │   返回预设的静态数据或默认响应                 │
│   返回缓存    │   返回之前缓存的数据（可能不是最新）           │
│   排队等待    │   请求进入队列，异步处理                      │
│   限流拒绝    │   超过阈值的请求直接拒绝                      │
│   功能关闭    │   关闭非核心功能，释放资源                    │
│   服务Mock    │   返回模拟数据，保证流程不中断                 │
└──────────────┴──────────────────────────────────────────────┘
```

## 代码实现

### 1. 使用 Sentinel 实现降级

```java
// 1. 添加依赖
// <dependency>
//     <groupId>com.alibaba.cloud</groupId>
//     <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
// </dependency>

// 2. 定义资源和降级规则
@Service
public class ProductService {
    
    @SentinelResource(
        value = "getProductDetail",
        fallback = "getProductDetailFallback",      // 业务异常降级
        blockHandler = "getProductDetailBlockHandler" // 限流降级
    )
    public Product getProductDetail(Long productId) {
        // 调用商品服务获取详情
        return productClient.getProduct(productId);
    }
    
    // 业务异常降级方法（参数需一致，可加 Throwable）
    public Product getProductDetailFallback(Long productId, Throwable ex) {
        log.warn("获取商品详情降级，productId={}, error={}", productId, ex.getMessage());
        // 返回兜底数据
        return Product.builder()
            .id(productId)
            .name("商品信息暂不可用")
            .price(BigDecimal.ZERO)
            .build();
    }
    
    // 限流/熔断降级方法（参数需一致，加 BlockException）
    public Product getProductDetailBlockHandler(Long productId, BlockException ex) {
        log.warn("获取商品详情被限流，productId={}", productId);
        return Product.builder()
            .id(productId)
            .name("系统繁忙，请稍后重试")
            .build();
    }
}
```

### 2. 使用 Resilience4j 实现降级

```java
// 1. 添加依赖
// <dependency>
//     <groupId>io.github.resilience4j</groupId>
//     <artifactId>resilience4j-spring-boot2</artifactId>
// </dependency>

// 2. 配置 application.yml
/*
resilience4j:
  circuitbreaker:
    instances:
      productService:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
        sliding-window-size: 10
*/

// 3. 使用注解
@Service
public class ProductService {
    
    @CircuitBreaker(name = "productService", fallbackMethod = "fallback")
    @Retry(name = "productService")
    @RateLimiter(name = "productService")
    public Product getProductDetail(Long productId) {
        return productClient.getProduct(productId);
    }
    
    // 降级方法
    public Product fallback(Long productId, Throwable ex) {
        log.warn("商品服务降级: {}", ex.getMessage());
        return getProductFromCache(productId)
            .orElse(getDefaultProduct(productId));
    }
    
    private Optional<Product> getProductFromCache(Long productId) {
        return Optional.ofNullable(redisTemplate.opsForValue()
            .get("product:" + productId));
    }
    
    private Product getDefaultProduct(Long productId) {
        return Product.builder()
            .id(productId)
            .name("商品加载中...")
            .available(false)
            .build();
    }
}
```

### 3. 手动降级开关

```java
@Service
public class OrderService {
    
    @Autowired
    private DegradeConfigService degradeConfig;
    
    @Autowired
    private RecommendService recommendService;
    
    public OrderDetailVO getOrderDetail(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        OrderDetailVO vo = convertToVO(order);
        
        // 推荐功能降级开关
        if (degradeConfig.isEnabled("order.recommend")) {
            try {
                List<Product> recommends = recommendService.getRecommends(order.getUserId());
                vo.setRecommends(recommends);
            } catch (Exception e) {
                log.warn("推荐服务调用失败，使用热门商品替代");
                vo.setRecommends(getHotProducts());
            }
        } else {
            // 主动降级，直接使用缓存热门商品
            vo.setRecommends(getHotProducts());
        }
        
        return vo;
    }
}

// 降级配置服务（支持动态配置）
@Service
public class DegradeConfigService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    // 检查降级开关（true=正常，false=降级）
    public boolean isEnabled(String key) {
        String value = redisTemplate.opsForValue().get("degrade:" + key);
        return !"off".equalsIgnoreCase(value);
    }
    
    // 开启/关闭降级
    public void setDegrade(String key, boolean enabled) {
        redisTemplate.opsForValue().set("degrade:" + key, enabled ? "on" : "off");
    }
}
```

### 4. 多级降级策略

```java
@Service
public class UserProfileService {
    
    @Autowired
    private UserServiceClient userClient;
    
    @Autowired
    private RedisTemplate<String, UserProfile> redisTemplate;
    
    @Autowired
    private LocalCache<Long, UserProfile> localCache;
    
    /**
     * 多级降级获取用户信息
     * L1: 本地缓存
     * L2: Redis 缓存
     * L3: 远程服务
     * L4: 默认值
     */
    public UserProfile getUserProfile(Long userId) {
        // L1: 本地缓存
        UserProfile profile = localCache.get(userId);
        if (profile != null) {
            return profile;
        }
        
        // L2: Redis 缓存
        String redisKey = "user:profile:" + userId;
        profile = redisTemplate.opsForValue().get(redisKey);
        if (profile != null) {
            localCache.put(userId, profile);
            return profile;
        }
        
        // L3: 远程服务（带熔断）
        try {
            profile = userClient.getUserProfile(userId);
            if (profile != null) {
                // 回填缓存
                redisTemplate.opsForValue().set(redisKey, profile, 1, TimeUnit.HOURS);
                localCache.put(userId, profile);
                return profile;
            }
        } catch (Exception e) {
            log.error("获取用户信息失败，降级处理", e);
        }
        
        // L4: 返回默认值
        return UserProfile.defaultProfile(userId);
    }
}
```

## 降级设计原则

```
┌─────────────────────────────────────────────────────────────┐
│                      降级设计原则                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 核心优先                                               │
│      └── 优先保证核心业务，非核心可降级                       │
│                                                             │
│   2. 有损服务                                               │
│      └── 降级后功能受损但服务可用，好过完全不可用              │
│                                                             │
│   3. 快速失败                                               │
│      └── 降级逻辑要简单快速，不能引入新的依赖                  │
│                                                             │
│   4. 可观测                                                 │
│      └── 降级事件要记录日志、告警，便于运维                   │
│                                                             │
│   5. 可恢复                                                 │
│      └── 降级应该是临时的，系统恢复后能自动回归正常            │
│                                                             │
│   6. 可测试                                                 │
│      └── 降级逻辑需要测试覆盖，确保降级正常工作               │
└─────────────────────────────────────────────────────────────┘
```

## 电商降级案例

```
┌─────────────────────────────────────────────────────────────┐
│                    电商大促降级方案                          │
├──────────────────┬──────────────────────────────────────────┤
│   功能            │   降级策略                               │
├──────────────────┼──────────────────────────────────────────┤
│   商品详情页      │   关闭评论、推荐，只展示核心信息           │
│   购物车          │   库存异步校验，不实时检查                 │
│   下单           │   关闭优惠券计算，使用缓存价格              │
│   支付           │   核心功能，不降级，只熔断                  │
│   物流查询        │   返回缓存数据，异步刷新                   │
│   个性化推荐      │   返回热门商品，关闭个性化                  │
│   搜索           │   返回缓存结果，降低精准度                  │
│   客服入口        │   关闭在线客服，显示常见问题               │
└──────────────────┴──────────────────────────────────────────┘
```

## 最佳实践

### ✅ 推荐做法

```java
// 1. 降级数据要有合理性
public Product fallback(Long productId) {
    return Product.builder()
        .id(productId)
        .name("商品信息加载中...")    // 友好提示
        .price(null)                 // 不返回错误价格
        .available(false)            // 标记不可购买
        .degraded(true)              // 标记降级状态
        .build();
}

// 2. 降级时记录日志和监控
public Object fallbackWithMetrics(Throwable ex) {
    // 记录日志
    log.warn("服务降级触发", ex);
    // 上报监控
    meterRegistry.counter("service.degraded", "reason", ex.getClass().getSimpleName()).increment();
    return defaultValue;
}

// 3. 区分降级原因，采取不同策略
public Product fallback(Long productId, Throwable ex) {
    if (ex instanceof TimeoutException) {
        return getProductFromCache(productId);  // 超时用缓存
    } else if (ex instanceof CircuitBreakerException) {
        return getDefaultProduct();             // 熔断用默认值
    } else {
        throw new ServiceException("服务暂不可用");  // 其他抛异常
    }
}
```

### ❌ 避免做法

```java
// 1. 避免降级方法中再调用可能失败的服务
public Product fallback(Long productId) {
    return anotherService.getProduct(productId);  // 错误！可能再次失败
}

// 2. 避免返回 null 或不合理数据
public Product fallback(Long productId) {
    return null;  // 调用方可能 NPE
}

// 3. 避免降级逻辑过于复杂
public Product fallback(Long productId) {
    // 复杂计算、多次数据库查询...  // 违背快速失败原则
}
```

## 面试回答

### 30秒版本

> 服务降级是当系统压力过大或依赖服务不可用时，**主动放弃非核心功能**，返回兜底数据，保证核心业务可用的容错策略。常见策略包括返回默认值、返回缓存、功能关闭等。与熔断的区别是：降级是主动预防，熔断是被动响应。

### 1分钟版本

> **服务降级**是微服务容错的重要手段，核心思想是"有损服务胜过无服务"。
>
> **触发场景**：
> - 系统负载过高（CPU、内存、线程池）
> - 依赖服务超时或失败率高
> - 流量突增（大促、热点事件）
> - 运维主动开启降级开关
>
> **降级策略**：返回默认值、返回缓存数据、功能关闭、排队等待
>
> **与熔断区别**：降级是主动放弃非核心功能（预防性），熔断是被动阻断故障服务（响应性）。实际中常配合使用：熔断后执行降级逻辑。
>
> **实现方式**：Sentinel 的 `@SentinelResource(fallback=...)`，Resilience4j 的 `@CircuitBreaker(fallbackMethod=...)`，或手动降级开关配合配置中心。

---

*关联文档：[circuit-breaker.md](circuit-breaker.md) | [service-avalanche.md](service-avalanche.md)*
