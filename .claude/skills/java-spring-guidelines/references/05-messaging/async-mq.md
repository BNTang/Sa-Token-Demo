# 异步与消息

> Java/Spring Boot 编码规范 - 异步处理与消息队列

---

## @Async 异步处理

### 配置线程池

```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Bean("mqExecutor")
    public Executor mqExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("mq-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

### 使用 @Async

```java
@Service
public class NotificationService {

    /**
     * 异步发送邮件
     */
    @Async("asyncExecutor")
    public void sendEmail(String to, String subject, String content) {
        try {
            emailClient.send(to, subject, content);
            log.info("[邮件发送]，发送成功，收件人: {}", to);
        } catch (Exception e) {
            log.error("[邮件发送]，发送失败，收件人: {}", to, e);
            // 根据业务决定是否重试或记录
        }
    }

    /**
     * 异步发送短信
     */
    @Async("asyncExecutor")
    public void sendSms(String mobile, String content) {
        smsClient.send(mobile, content);
    }

    /**
     * 异步方法有返回值
     */
    @Async("asyncExecutor")
    public CompletableFuture<String> asyncQuery(Long orderId) {
        String result = externalApi.query(orderId);
        return CompletableFuture.completedFuture(result);
    }
}
```

### 调用异步方法

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final NotificationService notificationService;

    public void createOrder(Order order) {
        // 1. 同步创建订单
        orderMapper.insert(order);

        // 2. 异步发送通知（不阻塞主流程）
        notificationService.sendEmail(order.getUserEmail(), "订单创建", "您的订单已创建");
        notificationService.sendSms(order.getUserMobile(), "您的订单已创建");

        // 3. 立即返回
        log.info("[订单创建]，订单创建成功，订单ID: {}", order.getId());
    }
}
```

### @Async 注意事项

```java
// ❌ 错误：同类调用 @Async 方法（代理失效）
@Service
public class OrderService {

    public void createOrder(Order order) {
        sendNotification(order);  // @Async 不生效
    }

    @Async
    public void sendNotification(Order order) { }
}

// ✅ 正确：注入自身或拆分到另一个 Service
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderService self;  // 自注入

    public void createOrder(Order order) {
        self.sendNotification(order);  // 通过代理调用
    }

    @Async
    public void sendNotification(Order order) { }
}

// ❌ 错误：@Async 方法中使用 @Transactional
@Async
@Transactional  // 事务不生效
public void asyncMethod() { }

// ✅ 正确：拆分方法
public void mainMethod() {
    asyncMethod();
}

@Async
public void asyncMethod() {
    transactionalMethod();
}

@Transactional
public void transactionalMethod() { }
```

---

## RocketMQ 事务消息

### 发送事务消息

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final RocketMQTemplate rocketMQTemplate;

    public void createOrder(Order order) {
        // 1. 发送事务消息
        TransactionSendResult result = rocketMQTemplate.sendMessageInTransaction(
            "order-group",           // 生产者组
            "order-topic",           // Topic
            MessageBuilder.withPayload(order)
                .setHeader("orderId", order.getId())
                .build(),
            null                      // 参数
        );

        log.info("[订单创建]，事务消息发送结果: {}", result);
    }
}
```

### 事务监听器

```java
@Component
@RocketMQTransactionListener(rocketMQTemplateBeanName = "rocketMQTemplate")
@RequiredArgsConstructor
public class OrderTransactionListenerImpl implements RocketMQLocalTransactionListener {

    private final OrderMapper orderMapper;

    /**
     * 执行本地事务
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        try {
            Order order = (Order) msg.getPayload();
            log.info("[事务消息]，执行本地事务，订单ID: {}", order.getId());

            // 插入订单
            orderMapper.insert(order);

            // 扣减库存
            // ...

            log.info("[事务消息]，本地事务执行成功，订单ID: {}", order.getId());
            return RocketMQLocalTransactionState.COMMIT;
        } catch (Exception e) {
            log.error("[事务消息]，本地事务执行失败", e);
            return RocketMQLocalTransactionState.ROLLBACK;
        }
    }

    /**
     * 事务回查（MQ 未收到确认时调用）
     */
    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        Order order = (Order) msg.getPayload();
        log.info("[事务消息]，事务回查，订单ID: {}", order.getId());

        // 幂等查询订单是否存在
        Order exists = orderMapper.selectById(order.getId());
        if (exists != null) {
            log.info("[事务消息]，订单已存在，提交消息");
            return RocketMQLocalTransactionState.COMMIT;
        }

        log.info("[事务消息]，订单不存在，回滚消息");
        return RocketMQLocalTransactionState.ROLLBACK;
    }
}
```

---

## 消息消费

### 基本消费

```java
@Component
@RocketMQMessageListener(
    topic = "order-topic",
    consumerGroup = "order-consumer-group"
)
@RequiredArgsConstructor
public class OrderConsumer implements RocketMQListener<Order> {

    private final StatisticsService statisticsService;

    @Override
    public void onMessage(Order order) {
        log.info("[消息消费]，收到订单消息，订单ID: {}", order.getId());

        try {
            // 处理订单消息
            statisticsService.updateOrderStatistics(order);
        } catch (Exception e) {
            log.error("[消息消费]，处理失败，订单ID: {}", order.getId(), e);
            throw e;  // 抛出异常触发重试
        }
    }
}
```

### 幂等消费

```java
@Component
@RocketMQMessageListener(
    topic = "order-topic",
    consumerGroup = "order-consumer-group"
)
@RequiredArgsConstructor
public class OrderConsumer implements RocketMQListener<Order> {

    private final StringRedisTemplate redisTemplate;
    private final StatisticsService statisticsService;

    @Override
    public void onMessage(Order order) {
        String key = "mq:consumed:order:" + order.getId();

        // 幂等判断：检查是否已处理
        Boolean consumed = redisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(consumed)) {
            log.warn("[消息消费]，订单已处理，跳过，订单ID: {}", order.getId());
            return;
        }

        try {
            // 处理业务
            statisticsService.updateOrderStatistics(order);

            // 标记已处理
            redisTemplate.opsForValue().set(key, "1", 24, TimeUnit.HOURS);

        } catch (Exception e) {
            log.error("[消息消费]，处理失败，订单ID: {}", order.getId(), e);
            throw e;  // 抛出异常触发重试
        }
    }
}
```

### 消费重试策略

```java
@Component
@RocketMQMessageListener(
    topic = "order-topic",
    consumerGroup = "order-consumer-group",
    maxReconsumeTimes = 3,  // 最大重试次数
    consumeThreadNumber = 10  // 消费线程数
)
@RequiredArgsConstructor
public class OrderConsumer implements RocketMQListener<Order> {

    @Override
    public void onMessage(Order order) {
        try {
            processOrder(order);
        } catch (BusinessException e) {
            // 业务异常：不重试，记录日志
            log.error("[消息消费]，业务异常，不重试，订单ID: {}，错误: {}",
                order.getId(), e.getMessage());
        } catch (Exception e) {
            // 系统异常：抛出触发重试
            log.error("[消息消费]，系统异常，将重试，订单ID: {}", order.getId(), e);
            throw e;
        }
    }
}
```

---

## 延迟消息

### 延迟等级（RocketMQ 4.x）

| 等级 | 延迟时间 | 等级 | 延迟时间 |
|------|---------|------|---------|
| 1 | 1 秒 | 10 | 30 分钟 |
| 2 | 5 秒 | 11 | 1 小时 |
| 3 | 10 秒 | 12 | 2 小时 |
| 4 | 30 秒 | 13 | 6 小时 |
| 5 | 1 分钟 | 14 | 12 小时 |
| 6 | 2 分钟 | 15 | 24 小时 |
| 7 | 3 分钟 | 16 | 2 天 |
| 8 | 4 分钟 | 17 | 3 天 |
| 9 | 5 分钟 | 18 | 4 天 |

### 发送延迟消息

```java
@Service
public class OrderService {

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 订单超时取消（30 分钟后）
     */
    public void createOrder(Order order) {
        orderMapper.insert(order);

        // 发送延迟消息，30 分钟后检查订单
        rocketMQTemplate.syncSend(
            "order-timeout-topic",
            MessageBuilder.withPayload(order.getId())
                .setHeader("delayTimeLevel", 6)  // 2 分钟（测试用）
                // .setHeader("delayTimeLevel", 5)  // 1 分钟
                .build()
        );
    }
}

/**
 * 订单超时消费者
 */
@Component
@RocketMQMessageListener(
    topic = "order-timeout-topic",
    consumerGroup = "order-timeout-consumer-group"
)
public class OrderTimeoutConsumer implements RocketMQListener<Long> {

    @Override
    public void onMessage(Long orderId) {
        Order order = orderMapper.selectById(orderId);

        // 检查订单状态，未支付则取消
        if (order != null && order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            order.setStatus(OrderStatus.CANCELLED);
            order.setCancelReason("超时未支付");
            orderMapper.updateById(order);
            log.info("[订单超时]，订单已取消，订单ID: {}", orderId);
        }
    }
}
```

---

## 异步与消息规范速查表

| 规范 | 要点 |
|------|------|
| **线程池** | 必须指定线程池，禁止使用默认 |
| **同类调用** | @Async 同类调用不生效，需自注入或拆分 |
| **事务配合** | @Async 方法中不能使用 @Transactional |
| **事务消息** | 使用 RocketMQ 事务消息保证最终一致性 |
| **幂等消费** | 消费端必须实现幂等 |
| **重试策略** | 业务异常不重试，系统异常重试 |
| **异常处理** | 记录日志，根据异常类型决定是否重试 |
