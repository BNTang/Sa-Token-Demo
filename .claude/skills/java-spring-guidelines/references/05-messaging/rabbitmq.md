# RabbitMQ 使用规范

> Java/Spring Boot 编码规范 - RabbitMQ 消息队列最佳实践

---

## RabbitMQ 基础概念

### 核心组件

| 组件 | 说明 |
|------|------|
| **Producer** | 消息生产者 |
| **Consumer** | 消息消费者 |
| **Exchange** | 交换机，负责路由消息 |
| **Queue** | 队列，存储消息 |
| **Binding** | 绑定，Exchange 和 Queue 的关系 |
| **Virtual Host** | 虚拟主机，隔离环境 |

### Exchange 类型

| 类型 | 说明 | 使用场景 |
|------|------|---------|
| **Direct** | 精确匹配 routing key | 点对点消息 |
| **Fanout** | 广播，忽略 routing key | 发布订阅 |
| **Topic** | 模式匹配 routing key | 灵活路由 |
| **Headers** | 根据消息头路由 | 复杂路由 |

---

## Spring Boot 集成 RabbitMQ

### 依赖配置

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

### 配置文件

```yaml
# application.yml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /
    # 发送者确认
    publisher-confirm-type: correlated
    publisher-returns: true
    # 消费者配置
    listener:
      simple:
        # 手动确认
        acknowledge-mode: manual
        # 并发消费者数量
        concurrency: 5
        max-concurrency: 10
        # 预取数量
        prefetch: 10
        # 重试配置
        retry:
          enabled: true
          initial-interval: 1000
          max-attempts: 3
          multiplier: 2
```

---

## 消息发送规范

### 定义交换机和队列

```java
@Configuration
public class RabbitMQConfig {

    // 订单交换机
    public static final String ORDER_EXCHANGE = "order.exchange";
    
    // 订单队列
    public static final String ORDER_QUEUE = "order.queue";
    public static final String ORDER_ROUTING_KEY = "order.create";
    
    // 死信交换机
    public static final String DLX_EXCHANGE = "order.dlx.exchange";
    public static final String DLX_QUEUE = "order.dlx.queue";
    public static final String DLX_ROUTING_KEY = "order.dlx";

    /**
     * 订单交换机
     */
    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE, true, false);
    }

    /**
     * 订单队列（配置死信队列）
     */
    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable(ORDER_QUEUE)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(DLX_ROUTING_KEY)
                .ttl(60000) // 消息 TTL 60秒
                .build();
    }

    /**
     * 绑定关系
     */
    @Bean
    public Binding orderBinding(Queue orderQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(orderQueue).to(orderExchange).with(ORDER_ROUTING_KEY);
    }

    /**
     * 死信交换机
     */
    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    /**
     * 死信队列
     */
    @Bean
    public Queue dlxQueue() {
        return QueueBuilder.durable(DLX_QUEUE).build();
    }

    /**
     * 死信绑定
     */
    @Bean
    public Binding dlxBinding(Queue dlxQueue, DirectExchange dlxExchange) {
        return BindingBuilder.bind(dlxQueue).to(dlxExchange).with(DLX_ROUTING_KEY);
    }
}
```

### 发送消息

**【推荐】使用 RabbitTemplate 发送消息，配置发送确认。**

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送订单消息
     */
    public void sendOrderMessage(Order order) {
        try {
            // 设置消息ID
            CorrelationData correlationData = new CorrelationData(order.getId().toString());
            
            // 设置确认回调
            correlationData.getFuture().addCallback(
                result -> {
                    if (result != null && result.isAck()) {
                        log.info("[RabbitMQ]，消息发送成功，订单ID: {}", order.getId());
                    } else {
                        log.error("[RabbitMQ]，消息发送失败，订单ID: {}", order.getId());
                        // 记录失败消息，后续补偿
                        saveFailedMessage(order);
                    }
                },
                ex -> log.error("[RabbitMQ]，消息发送异常，订单ID: {}", order.getId(), ex)
            );
            
            // 发送消息
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_ROUTING_KEY,
                order,
                correlationData
            );
            
        } catch (Exception e) {
            log.error("[RabbitMQ]，发送消息异常，订单ID: {}", order.getId(), e);
            throw new BusinessException("消息发送失败");
        }
    }

    /**
     * 延迟消息（使用 TTL + 死信队列实现）
     */
    public void sendDelayMessage(Order order, long delayMillis) {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.ORDER_EXCHANGE,
            RabbitMQConfig.ORDER_ROUTING_KEY,
            order,
            message -> {
                message.getMessageProperties().setExpiration(String.valueOf(delayMillis));
                return message;
            }
        );
        log.info("[RabbitMQ]，发送延迟消息，订单ID: {}，延迟: {}ms", order.getId(), delayMillis);
    }

    /**
     * 保存失败消息
     */
    private void saveFailedMessage(Order order) {
        // 记录到数据库或 Redis，后续补偿重试
    }
}
```

---

## 消息消费规范

### 消费者配置

**【强制】消费者必须使用手动确认模式。**

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderConsumer {

    private final OrderService orderService;

    /**
     * 消费订单消息
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_QUEUE)
    public void handleOrderMessage(Order order, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        
        try {
            log.info("[RabbitMQ]，接收订单消息，订单ID: {}", order.getId());
            
            // 业务处理
            orderService.handleOrder(order);
            
            // 手动确认
            channel.basicAck(deliveryTag, false);
            log.info("[RabbitMQ]，消息消费成功，订单ID: {}", order.getId());
            
        } catch (BusinessException e) {
            // 业务异常，拒绝消息，不重新入队
            log.error("[RabbitMQ]，业务处理失败，订单ID: {}，拒绝消息", order.getId(), e);
            channel.basicReject(deliveryTag, false);
            
        } catch (Exception e) {
            // 系统异常，重新入队重试
            log.error("[RabbitMQ]，系统异常，订单ID: {}，重新入队", order.getId(), e);
            
            // 判断是否重复投递
            Integer retryCount = (Integer) message.getMessageProperties().getHeaders().get("x-retry-count");
            if (retryCount == null) {
                retryCount = 0;
            }
            
            if (retryCount < 3) {
                // 重新入队
                channel.basicNack(deliveryTag, false, true);
            } else {
                // 超过重试次数，拒绝消息（进入死信队列）
                log.error("[RabbitMQ]，消息重试超限，订单ID: {}，进入死信队列", order.getId());
                channel.basicReject(deliveryTag, false);
            }
        }
    }

    /**
     * 死信队列消费者
     */
    @RabbitListener(queues = RabbitMQConfig.DLX_QUEUE)
    public void handleDlxMessage(Order order, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        
        try {
            log.warn("[RabbitMQ]，接收死信消息，订单ID: {}", order.getId());
            
            // 记录到数据库，人工处理
            saveDlxMessage(order);
            
            // 确认消息
            channel.basicAck(deliveryTag, false);
            
        } catch (Exception e) {
            log.error("[RabbitMQ]，死信消息处理失败，订单ID: {}", order.getId(), e);
            channel.basicReject(deliveryTag, false);
        }
    }

    private void saveDlxMessage(Order order) {
        // 记录到数据库
    }
}
```

---

## RabbitMQ vs RocketMQ 选择

### 功能对比

| 特性 | RabbitMQ | RocketMQ |
|------|----------|----------|
| **语言** | Erlang | Java |
| **协议** | AMQP | 自定义 |
| **吞吐量** | 万级 | 十万级 |
| **延迟消息** | TTL + 死信队列 | 原生支持 |
| **消息回溯** | ❌ 不支持 | ✅ 支持 |
| **事务消息** | ✅ 支持 | ✅ 支持 |
| **消息顺序** | 单队列顺序 | 分区顺序 |
| **运维复杂度** | 低 | 中 |
| **社区** | 成熟 | 活跃 |

### 选择建议

```java
// ✅ RabbitMQ 适用场景
// - 中小规模系统（TPS < 1万）
// - 对 AMQP 协议有要求
// - 运维团队熟悉 Erlang
// - 需要灵活的路由规则（Topic Exchange）

// ✅ RocketMQ 适用场景
// - 大规模系统（TPS > 1万）
// - 需要延迟消息、消息回溯
// - 需要事务消息
// - 对消息顺序有严格要求
// - 团队熟悉 Java
```

---

## 消息幂等性保证

**【强制】消费者必须保证幂等性，避免重复消费。**

### 方案 1：使用消息 ID 去重

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderConsumer {

    private final RedisTemplate<String, String> redisTemplate;
    private final OrderService orderService;

    @RabbitListener(queues = RabbitMQConfig.ORDER_QUEUE)
    public void handleOrderMessage(Order order, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String messageId = message.getMessageProperties().getMessageId();
        
        try {
            // 幂等性检查
            String key = "mq:order:" + messageId;
            Boolean isProcessed = redisTemplate.opsForValue().setIfAbsent(key, "1", 1, TimeUnit.HOURS);
            
            if (Boolean.FALSE.equals(isProcessed)) {
                log.warn("[RabbitMQ]，消息已处理，跳过，消息ID: {}", messageId);
                channel.basicAck(deliveryTag, false);
                return;
            }
            
            // 业务处理
            orderService.handleOrder(order);
            
            // 确认消息
            channel.basicAck(deliveryTag, false);
            
        } catch (Exception e) {
            log.error("[RabbitMQ]，消息处理失败，消息ID: {}", messageId, e);
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
```

### 方案 2：使用数据库唯一约束

```java
@Service
@Transactional
public class OrderService {

    public void handleOrder(Order order) {
        // 使用唯一约束防止重复插入
        try {
            orderMapper.insert(order);
        } catch (DuplicateKeyException e) {
            log.warn("[订单处理]，订单已存在，跳过，订单ID: {}", order.getId());
        }
    }
}
```

---

## 消息可靠性保证

### 1. 发送端确认

```java
@Configuration
public class RabbitTemplateConfig {

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        
        // 设置确认回调
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.info("[RabbitMQ]，消息投递到交换机成功");
            } else {
                log.error("[RabbitMQ]，消息投递到交换机失败: {}", cause);
            }
        });
        
        // 设置退回回调（消息无法路由到队列时）
        template.setReturnsCallback(returned -> {
            log.error("[RabbitMQ]，消息路由到队列失败: {}", returned.getMessage());
        });
        
        template.setMandatory(true);
        
        return template;
    }
}
```

### 2. 消息持久化

```java
@Configuration
public class RabbitMQConfig {

    /**
     * 持久化交换机
     */
    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE, true, false); // durable = true
    }

    /**
     * 持久化队列
     */
    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable(ORDER_QUEUE).build(); // durable
    }
}
```

### RabbitMQ 无法路由的消息处理

**【强制】配置消息退回机制，处理无法路由的消息。**

**无法路由的场景：**

```
1. Exchange 存在，但没有绑定队列
2. Exchange 存在，Routing Key 不匹配任何队列
3. Exchange 不存在（直接报错）
```

**消息去向：**

```java
// 未设置 mandatory：消息被丢弃（默认行为）
RabbitTemplate template = new RabbitTemplate();
template.setMandatory(false);  // 默认
// 无法路由的消息直接丢弃，不通知发送者

// 设置 mandatory：消息退回给发送者
RabbitTemplate template = new RabbitTemplate();
template.setMandatory(true);
template.setReturnsCallback(returned -> {
    log.error("[RabbitMQ]，消息无法路由: {}", returned.getMessage());
    // 处理无法路由的消息：记录日志、存储到数据库、告警等
});
```

**完整配置示例：**

```java
@Configuration
public class RabbitTemplateConfig {

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        
        // ✅ 必须开启 mandatory
        template.setMandatory(true);
        
        // 消息无法路由时的回调
        template.setReturnsCallback(returned -> {
            log.error("[RabbitMQ]，消息无法路由");
            log.error("  Exchange: {}", returned.getExchange());
            log.error("  RoutingKey: {}", returned.getRoutingKey());
            log.error("  ReplyCode: {}", returned.getReplyCode());
            log.error("  ReplyText: {}", returned.getReplyText());
            log.error("  Message: {}", returned.getMessage());
            
            // 处理策略：
            // 1. 记录到数据库
            saveFailedMessage(returned);
            // 2. 发送告警
            alertService.sendAlert("消息路由失败");
            // 3. 重试或人工处理
        });
        
        return template;
    }
}
```

### 3. 手动确认

```yaml
# application.yml
spring:
  rabbitmq:
    listener:
      simple:
        acknowledge-mode: manual # 手动确认
```

---

## 最佳实践检查清单

| 检查项 | 说明 | 优先级 |
|--------|------|--------|
| ✅ 配置死信队列 | 处理失败消息 | 🔴 必须 |
| ✅ 使用手动确认模式 | 保证消息不丢失 | 🔴 必须 |
| ✅ 消费者幂等性 | 避免重复消费 | 🔴 必须 |
| ✅ 配置发送确认 | 确保消息投递成功 | 🔴 必须 |
| ✅ 消息持久化 | 防止消息丢失 | 🔴 必须 |
| ✅ 设置消息 TTL | 避免消息堆积 | 🟡 推荐 |
| ✅ 限制重试次数 | 避免死循环 | 🟡 推荐 |
| ✅ 监控队列积压 | 及时发现问题 | 🟡 推荐 |

---

## 参考资料

- RabbitMQ 官方文档
- Spring AMQP 文档
- 阿里巴巴 Java 开发手册
