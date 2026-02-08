---
name: microservices-best-practices
description: 微服务/分布式系统高可用最佳实践。涵盖容错设计、服务治理、代码复用、稳定性保障等。当编写微服务代码或进行架构设计时使用。
metadata:
  author: skill-hub
  version: "1.0"
  compatibility: Java 17+, Spring Boot 3.x, Spring Cloud 2022+
---

# 微服务高可用最佳实践

> 版本: 1.0 | 更新: 2026-02-04
>
> 让系统稳定、代码精简、维护轻松

---

## 概述

### 做了什么
提供一套微服务/分布式系统的最佳实践，覆盖高可用设计、容错机制、代码复用模式。

### 为什么需要
- 分布式系统故障是常态，不做容错就是在裸奔
- 重复代码越多，维护成本越高，BUG 越多
- 没有规范的系统，扩展一次痛苦一次

### 什么时候必须用
- 设计新微服务时
- 处理服务间调用时
- 代码评审时检查是否遵循最佳实践

---

## 何时使用此 Skill

| 场景 | 触发词 |
|------|--------|
| 服务调用 | 远程调用、Feign、RestTemplate、HTTP 调用 |
| 容错设计 | 熔断、降级、重试、限流 |
| 代码复用 | 重复代码、公共组件、工具类 |
| 高可用 | 稳定性、可用性、故障处理 |

---

## 核心原则

| 原则 | 一句话 |
|------|--------|
| **Design for Failure** | 假设一切都会失败，提前做好应对 |
| **DRY** | Don't Repeat Yourself，重复超过 2 次就抽取 |
| **Fail Fast** | 快速失败，快速恢复，别拖着整个系统 |
| **Graceful Degradation** | 核心功能保命，非核心功能可降级 |

---

## 一、远程调用必须有容错

### 问题空间
微服务之间通过网络调用，网络不可靠、服务可能宕机、响应可能超时。

### 核心语义
**每一次远程调用，都必须考虑：超时、重试、熔断、降级。**

### 【强制】规则清单

| 规则 | 说明 |
|------|------|
| 必须设置超时 | 不设超时 = 线程池耗尽 = 服务雪崩 |
| 必须有重试策略 | 幂等接口可重试，非幂等禁止重试 |
| 必须有熔断机制 | 下游故障时快速失败，保护自己 |
| 必须有降级方案 | 熔断后返回兜底数据，而不是报错 |

### 反例与正例

```java
// ❌ 反例 - 裸调用，没有任何保护
@Service
public class OrderService {
    
    @Autowired
    private UserFeignClient userClient;
    
    public OrderDTO getOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        // 如果 user-service 挂了，这里就卡死
        User user = userClient.getUser(order.getUserId());
        return buildOrderDTO(order, user);
    }
}
```

```java
// ✅ 正例 - 完整的容错保护

// 1. Feign 客户端配置超时
@FeignClient(
    name = "user-service",
    fallbackFactory = UserClientFallbackFactory.class
)
public interface UserFeignClient {
    
    @GetMapping("/users/{id}")
    User getUser(@PathVariable Long id);
}

// 2. 降级工厂（推荐用 Factory，可以拿到异常信息）
@Component
public class UserClientFallbackFactory implements FallbackFactory<UserFeignClient> {
    
    @Override
    public UserFeignClient create(Throwable cause) {
        return new UserFeignClient() {
            @Override
            public User getUser(Long id) {
                // 记录日志，返回兜底数据
                log.warn("调用 user-service 失败，降级处理: {}", cause.getMessage());
                return User.unknown(id);
            }
        };
    }
}

// 3. 全局超时配置 (application.yml)
// feign:
//   client:
//     config:
//       default:
//         connectTimeout: 2000
//         readTimeout: 5000
//   circuitbreaker:
//     enabled: true

// 4. 熔断器配置（Resilience4j）
// resilience4j:
//   circuitbreaker:
//     instances:
//       user-service:
//         failureRateThreshold: 50
//         waitDurationInOpenState: 30s
//         slidingWindowSize: 10
```

### 超时设置参考值

| 场景 | 连接超时 | 读取超时 |
|------|---------|---------|
| 内部服务调用 | 1-2s | 3-5s |
| 外部 API 调用 | 3-5s | 10-30s |
| 批量任务 | 5s | 60-120s |

### 记忆锚点
> **远程调用四件套：超时、重试、熔断、降级，缺一不可。**

---

## 二、重试必须保证幂等

### 问题空间
网络抖动导致请求重发，如果接口不幂等，可能重复扣款、重复下单。

### 核心语义
**只有幂等的接口才能重试，不幂等就别重试。**

### 【强制】规则清单

| 规则 | 说明 |
|------|------|
| 查询接口 | 天然幂等，可重试 |
| 删除接口 | 按 ID 删除是幂等的，可重试 |
| 新增接口 | 必须用唯一键或幂等 Token |
| 更新接口 | 用版本号或条件更新保证幂等 |

### 反例与正例

```java
// ❌ 反例 - 非幂等接口开启重试
@FeignClient(name = "payment-service")
public interface PaymentClient {
    
    // 这个接口不幂等，重试会重复扣款！
    @PostMapping("/pay")
    @Retryable(maxAttempts = 3)  // 危险！
    PayResult pay(PayRequest request);
}
```

```java
// ✅ 正例 - 通过幂等 Token 保证幂等

// 1. 请求中带幂等 Token
@Data
public class PayRequest {
    private String idempotentKey;  // 幂等键，如：订单号+支付类型
    private Long orderId;
    private BigDecimal amount;
}

// 2. 服务端幂等校验
@Service
public class PaymentService {
    
    @Autowired
    private StringRedisTemplate redis;
    
    public PayResult pay(PayRequest request) {
        String key = "pay:idempotent:" + request.getIdempotentKey();
        
        // 幂等校验：已处理过就直接返回
        Boolean isNew = redis.opsForValue()
            .setIfAbsent(key, "1", Duration.ofHours(24));
        
        if (Boolean.FALSE.equals(isNew)) {
            // 已经处理过，查询结果返回
            return getExistingResult(request.getIdempotentKey());
        }
        
        try {
            // 执行真正的支付逻辑
            return doPayment(request);
        } catch (Exception e) {
            // 失败时删除幂等键，允许重试
            redis.delete(key);
            throw e;
        }
    }
}

// 3. 安全的重试配置
@Configuration
public class RetryConfig {
    
    @Bean
    public Retryer feignRetryer() {
        // 只对 GET 请求重试，POST/PUT/DELETE 不重试
        return new Retryer.Default(100, 1000, 3);
    }
}
```

### 记忆锚点
> **能重试的前提是幂等，不幂等就一次定生死。**

---

## 三、公共代码必须抽取

### 问题空间
相同的代码复制粘贴到多个服务，修改时遗漏导致行为不一致。

### 核心语义
**重复超过 2 次的代码，必须抽取到公共模块。**

### 【强制】分层抽取策略

| 层级 | 抽取内容 | 模块命名 |
|------|---------|---------|
| **基础工具层** | 工具类、异常定义、常量 | xxx-common |
| **领域模型层** | 共享的 DTO、VO、枚举 | xxx-api |
| **服务调用层** | Feign 接口、降级实现 | xxx-client |
| **业务组件层** | 通用业务逻辑（如短信、OSS） | xxx-starter |

### 项目结构示例

```
project-root/
├── project-common/              # 基础工具
│   ├── src/main/java/
│   │   ├── exception/           # 统一异常
│   │   ├── result/              # 统一响应
│   │   ├── utils/               # 工具类
│   │   └── constants/           # 常量
│   └── pom.xml
│
├── project-api/                 # 共享契约
│   ├── user-api/                # 用户服务 API
│   │   ├── dto/
│   │   ├── vo/
│   │   └── enums/
│   └── order-api/               # 订单服务 API
│
├── project-client/              # Feign 客户端
│   ├── user-client/
│   └── order-client/
│
├── project-starter/             # 自定义 Starter
│   ├── sms-starter/             # 短信组件
│   ├── oss-starter/             # 对象存储组件
│   └── log-starter/             # 日志组件
│
├── user-service/                # 用户服务
├── order-service/               # 订单服务
└── pom.xml
```

### 统一响应封装

```java
// ✅ project-common 中定义统一响应

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result<T> {
    
    private int code;
    private String message;
    private T data;
    private long timestamp = System.currentTimeMillis();
    
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data, System.currentTimeMillis());
    }
    
    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null, System.currentTimeMillis());
    }
    
    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null, System.currentTimeMillis());
    }
    
    public boolean isSuccess() {
        return this.code == 200;
    }
}
```

### 统一异常处理

```java
// ✅ 业务异常定义
public class BusinessException extends RuntimeException {
    
    private final int code;
    
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }
    
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}

// ✅ 全局异常处理器（每个服务引入即可）
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
            .map(err -> err.getField() + ": " + err.getDefaultMessage())
            .collect(Collectors.joining("; "));
        return Result.fail(400, msg);
    }
    
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(500, "系统繁忙，请稍后重试");
    }
}
```

### 记忆锚点
> **复制两次就抽取，公共模块分四层。**

---

## 四、自定义 Starter 封装通用能力

### 问题空间
每个服务都要配置短信、OSS、日志等组件，重复且容易出错。

### 核心语义
**通用能力封装成 Spring Boot Starter，引入依赖即可使用。**

### Starter 开发模板

```java
// 1. 自动配置类
@Configuration
@ConditionalOnClass(SmsClient.class)
@EnableConfigurationProperties(SmsProperties.class)
public class SmsAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "sms", name = "enabled", havingValue = "true", matchIfMissing = true)
    public SmsClient smsClient(SmsProperties properties) {
        return new SmsClient(properties);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public SmsTemplate smsTemplate(SmsClient smsClient) {
        return new SmsTemplate(smsClient);
    }
}

// 2. 配置属性类
@Data
@ConfigurationProperties(prefix = "sms")
public class SmsProperties {
    
    private boolean enabled = true;
    private String accessKey;
    private String secretKey;
    private String signName;
    private String templateCode;
}

// 3. 模板类（对外暴露的 API）
public class SmsTemplate {
    
    private final SmsClient smsClient;
    
    public SmsTemplate(SmsClient smsClient) {
        this.smsClient = smsClient;
    }
    
    public void sendVerifyCode(String phone, String code) {
        Map<String, String> params = Map.of("code", code);
        smsClient.send(phone, params);
    }
}

// 4. spring.factories（Spring Boot 2.x）或 AutoConfiguration.imports（Spring Boot 3.x）
// resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
// com.example.sms.SmsAutoConfiguration
```

### 使用方式

```yaml
# application.yml
sms:
  enabled: true
  access-key: xxx
  secret-key: xxx
  sign-name: 我的应用
```

```java
// 直接注入使用
@Service
public class UserService {
    
    @Autowired
    private SmsTemplate smsTemplate;
    
    public void sendVerifyCode(String phone) {
        String code = RandomUtil.randomNumbers(6);
        smsTemplate.sendVerifyCode(phone, code);
    }
}
```

### 记忆锚点
> **通用能力做 Starter，配置一下就能用。**

---

## 五、分布式事务要慎重

### 问题空间
跨服务的数据一致性问题，处理不当会导致数据错乱。

### 核心语义
**能用最终一致性就别用强一致性，能不跨服务事务就别跨。**

### 【强制】决策规则

| 场景 | 方案 | 复杂度 |
|------|------|--------|
| 单服务多表 | 本地事务 @Transactional | ⭐ |
| 跨服务允许延迟 | 消息队列 + 最终一致性 | ⭐⭐ |
| 跨服务强一致 | Seata AT 模式 | ⭐⭐⭐ |
| 跨服务高并发 | Seata TCC 模式 | ⭐⭐⭐⭐ |

### 最终一致性方案（推荐）

```java
// ✅ 基于消息队列的最终一致性

// 1. 订单服务：创建订单 + 发消息
@Service
public class OrderService {
    
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    
    @Transactional
    public void createOrder(OrderDTO dto) {
        // 创建订单（本地事务）
        Order order = new Order();
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        orderMapper.insert(order);
        
        // 发送消息通知库存服务扣减库存
        // 使用事务消息保证消息一定发送成功
        rocketMQTemplate.sendMessageInTransaction(
            "order-topic",
            MessageBuilder.withPayload(new StockDeductMessage(order.getId(), dto.getItems()))
                .build(),
            order
        );
    }
}

// 2. 库存服务：消费消息扣减库存
@Component
@RocketMQMessageListener(topic = "order-topic", consumerGroup = "stock-consumer")
public class StockDeductListener implements RocketMQListener<StockDeductMessage> {
    
    @Override
    public void onMessage(StockDeductMessage message) {
        // 幂等校验
        if (stockRecordMapper.exists(message.getOrderId())) {
            return;
        }
        
        // 扣减库存
        for (OrderItem item : message.getItems()) {
            int rows = stockMapper.deduct(item.getSkuId(), item.getQuantity());
            if (rows == 0) {
                // 库存不足，发送补偿消息
                throw new BusinessException("库存不足");
            }
        }
        
        // 记录已处理
        stockRecordMapper.insert(new StockRecord(message.getOrderId()));
    }
}
```

### 本地消息表方案

```java
// ✅ 本地消息表：消息和业务数据在同一个事务

@Service
public class OrderService {
    
    @Transactional
    public void createOrder(OrderDTO dto) {
        // 1. 创建订单
        Order order = new Order();
        orderMapper.insert(order);
        
        // 2. 写入本地消息表（同一个事务）
        LocalMessage message = new LocalMessage();
        message.setTopic("stock-deduct");
        message.setPayload(JSON.toJSONString(new StockDeductMessage(order.getId())));
        message.setStatus(MessageStatus.PENDING);
        localMessageMapper.insert(message);
    }
}

// 3. 定时任务扫描发送
@Scheduled(fixedRate = 5000)
public void sendPendingMessages() {
    List<LocalMessage> messages = localMessageMapper.selectPending();
    for (LocalMessage msg : messages) {
        try {
            rocketMQTemplate.syncSend(msg.getTopic(), msg.getPayload());
            msg.setStatus(MessageStatus.SENT);
        } catch (Exception e) {
            msg.setRetryCount(msg.getRetryCount() + 1);
        }
        localMessageMapper.updateById(msg);
    }
}
```

### 记忆锚点
> **分布式事务能不用就不用，用消息队列做最终一致。**

---

## 六、配置必须外部化

### 问题空间
配置写死在代码里，每次改配置都要重新发版。

### 核心语义
**所有可能变化的配置，都要外部化，支持动态刷新。**

### 【强制】规则清单

| 类型 | 处理方式 |
|------|---------|
| 环境相关 | 放配置中心（Nacos/Apollo） |
| 敏感信息 | 加密存储或用密钥管理服务 |
| 业务开关 | 配置中心 + @RefreshScope |
| 限流阈值 | 配置中心，支持动态调整 |

### 反例与正例

```java
// ❌ 反例 - 硬编码配置
@Service
public class PaymentService {
    
    // 写死的配置，改动需要重新发版
    private static final String PAY_URL = "https://api.payment.com/pay";
    private static final int MAX_RETRY = 3;
    private static final boolean ENABLE_LOG = true;
}
```

```java
// ✅ 正例 - 配置外部化 + 动态刷新

@Data
@Component
@RefreshScope  // 支持动态刷新
@ConfigurationProperties(prefix = "payment")
public class PaymentProperties {
    
    private String payUrl;
    private int maxRetry = 3;
    private boolean enableLog = true;
    private BigDecimal maxAmount = new BigDecimal("50000");
}

@Service
public class PaymentService {
    
    @Autowired
    private PaymentProperties properties;
    
    public void pay(PayRequest request) {
        if (request.getAmount().compareTo(properties.getMaxAmount()) > 0) {
            throw new BusinessException("超过单笔限额");
        }
        // 使用配置的 URL
        httpClient.post(properties.getPayUrl(), request);
    }
}
```

### 记忆锚点
> **配置不写代码里，改配置不用重发版。**

---

## 七、日志必须规范

### 问题空间
日志格式不统一、关键信息缺失，排查问题时一脸懵。

### 核心语义
**日志要能回答：谁、什么时候、做了什么、结果如何。**

### 【强制】规则清单

| 规则 | 说明 |
|------|------|
| 必须有 traceId | 串联一次请求的所有日志 |
| 入口必须打日志 | 记录请求参数和响应结果 |
| 异常必须打日志 | 记录完整堆栈 |
| 禁止打敏感信息 | 手机号、身份证、密码脱敏 |

### 统一日志格式

```java
// ✅ 使用 MDC 传递 traceId

// 1. 过滤器设置 traceId
@Component
public class TraceFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        try {
            String traceId = Optional.ofNullable(((HttpServletRequest) request).getHeader("X-Trace-Id"))
                .orElse(UUID.randomUUID().toString().replace("-", ""));
            MDC.put("traceId", traceId);
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}

// 2. logback 配置
// <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{traceId}] %-5level %logger{36} - %msg%n</pattern>

// 3. Feign 拦截器传递 traceId
@Component
public class FeignTraceInterceptor implements RequestInterceptor {
    
    @Override
    public void apply(RequestTemplate template) {
        String traceId = MDC.get("traceId");
        if (traceId != null) {
            template.header("X-Trace-Id", traceId);
        }
    }
}
```

### 日志打印规范

```java
// ✅ 正确的日志打印

@Slf4j
@RestController
public class OrderController {
    
    @PostMapping("/orders")
    public Result<OrderVO> createOrder(@Valid @RequestBody OrderDTO dto) {
        // 入口日志
        log.info("创建订单请求: userId={}, productId={}, quantity={}", 
            dto.getUserId(), dto.getProductId(), dto.getQuantity());
        
        try {
            OrderVO result = orderService.createOrder(dto);
            // 成功日志
            log.info("创建订单成功: orderId={}", result.getOrderId());
            return Result.success(result);
        } catch (BusinessException e) {
            // 业务异常，warn 级别
            log.warn("创建订单失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            // 系统异常，error 级别，打印堆栈
            log.error("创建订单异常: userId={}", dto.getUserId(), e);
            throw e;
        }
    }
}
```

### 记忆锚点
> **日志带 traceId，入口出口都要打，异常必须有堆栈。**

---

## 八、接口必须有防护

### 问题空间
接口被恶意刷、无限重试，导致系统被打垮。

### 核心语义
**对外接口必须有限流、防重、鉴权。**

### 【强制】规则清单

| 防护类型 | 实现方式 |
|---------|---------|
| 限流 | Sentinel / Guava RateLimiter |
| 防重提交 | 幂等 Token / 分布式锁 |
| 鉴权 | JWT / OAuth2 / 网关统一鉴权 |
| 参数校验 | @Valid + 全局异常处理 |

### 限流实现

```java
// ✅ 使用 Sentinel 限流

// 1. 注解方式
@SentinelResource(
    value = "createOrder",
    blockHandler = "createOrderBlockHandler",
    fallback = "createOrderFallback"
)
public Result<OrderVO> createOrder(OrderDTO dto) {
    return orderService.createOrder(dto);
}

// 限流后的处理
public Result<OrderVO> createOrderBlockHandler(OrderDTO dto, BlockException e) {
    return Result.fail(429, "请求过于频繁，请稍后重试");
}

// 2. 配置限流规则（可通过 Sentinel Dashboard 动态配置）
@PostConstruct
public void initFlowRules() {
    List<FlowRule> rules = new ArrayList<>();
    FlowRule rule = new FlowRule();
    rule.setResource("createOrder");
    rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
    rule.setCount(100);  // QPS 限制 100
    rules.add(rule);
    FlowRuleManager.loadRules(rules);
}
```

### 防重提交

```java
// ✅ 基于 Redis 的防重提交

@Aspect
@Component
public class IdempotentAspect {
    
    @Autowired
    private StringRedisTemplate redis;
    
    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint point, Idempotent idempotent) throws Throwable {
        HttpServletRequest request = getRequest();
        String token = request.getHeader("Idempotent-Token");
        
        if (StringUtils.isBlank(token)) {
            throw new BusinessException("缺少幂等 Token");
        }
        
        String key = "idempotent:" + token;
        Boolean success = redis.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(idempotent.expire()));
        
        if (Boolean.FALSE.equals(success)) {
            throw new BusinessException("请勿重复提交");
        }
        
        try {
            return point.proceed();
        } catch (Exception e) {
            // 失败时删除 key，允许重试
            redis.delete(key);
            throw e;
        }
    }
}

// 使用注解
@Idempotent(expire = 10)
@PostMapping("/orders")
public Result<OrderVO> createOrder(@RequestBody OrderDTO dto) {
    return orderService.createOrder(dto);
}
```

### 记忆锚点
> **对外接口三件套：限流、防重、鉴权。**

---

## 快速检查清单

### 代码评审必查项

| # | 检查项 | 通过 |
|---|--------|------|
| 1 | 远程调用有超时配置吗？ | ☐ |
| 2 | 远程调用有降级方案吗？ | ☐ |
| 3 | 重试的接口是幂等的吗？ | ☐ |
| 4 | 重复代码抽取到公共模块了吗？ | ☐ |
| 5 | 配置是外部化的吗？ | ☐ |
| 6 | 日志有 traceId 吗？ | ☐ |
| 7 | 对外接口有限流吗？ | ☐ |
| 8 | 分布式事务考虑最终一致性了吗？ | ☐ |

---

## 禁则速查表

| ❌ 禁止 | ✅ 正确做法 | 原因 |
|--------|-----------|------|
| 远程调用不设超时 | 配置连接和读取超时 | 防止线程池耗尽 |
| 远程调用无降级 | 实现 FallbackFactory | 下游故障时快速失败 |
| 非幂等接口重试 | 幂等 Token + 数据库唯一键 | 防止重复操作 |
| 复制粘贴代码 | 抽取到公共模块 | 统一维护 |
| 配置写死在代码 | 配置中心 + @RefreshScope | 改配置不用发版 |
| 日志不带 traceId | MDC + 过滤器 | 方便问题排查 |
| 接口无限流 | Sentinel 限流 | 防止被打垮 |
| 跨服务强一致事务 | 消息队列最终一致 | 降低系统复杂度 |

---

## 参考资料

| 来源 | 说明 |
|------|------|
| Spring Cloud 官方文档 | 微服务组件使用指南 |
| Resilience4j 文档 | 熔断、限流、重试实现 |
| Sentinel 文档 | 阿里开源的流量防护组件 |
| Seata 文档 | 分布式事务解决方案 |
