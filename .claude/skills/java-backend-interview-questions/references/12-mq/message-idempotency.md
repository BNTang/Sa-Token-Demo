# 消息队列如何保证幂等性

> 分类: 消息队列 | 难度: ⭐⭐⭐⭐ | 频率: 高频

---

## 一、为什么会有重复消息

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          消息重复的原因                                           │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  1. 生产者重复发送                                                               │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │  Producer ──→ Broker ──→ 返回 ACK                                          │ │
│  │                              ↓ 网络超时                                     │ │
│  │  Producer 没收到 ACK，重试发送 ──→ Broker (消息重复)                        │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  2. 消费者重复消费                                                               │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │  Consumer ←── Broker ──→ 处理成功                                          │ │
│  │                              ↓ ACK 失败/消费者宕机                          │ │
│  │  Broker 认为消费失败，重新投递 ──→ Consumer (重复消费)                      │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  3. Broker 重复                                                                  │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │  主从同步时，主节点宕机，从节点提升为主                                     │ │
│  │  可能导致消息重复投递                                                       │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  结论: 消息重复是不可避免的，需要消费端保证幂等性                                │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 二、幂等性解决方案

### 2.1 方案总览

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          幂等性解决方案                                           │
├─────────────────────┬────────────────────────────────────────────────────────────┤
│      方案           │                        说明                                 │
├─────────────────────┼────────────────────────────────────────────────────────────┤
│  唯一ID + 去重表    │  消息带唯一ID，消费前查表判断是否处理过                     │
│  Redis 去重         │  用 Redis SETNX 实现快速去重                                │
│  数据库唯一约束     │  利用唯一索引防止重复插入                                   │
│  乐观锁/版本号      │  更新时带版本号条件                                         │
│  状态机             │  只允许状态单向流转                                         │
└─────────────────────┴────────────────────────────────────────────────────────────┘
```

### 2.2 唯一ID + 去重表

```java
/**
 * 方案1: 数据库去重表
 */
@Service
public class MessageConsumer {
    
    @Autowired
    private MessageLogMapper messageLogMapper;
    
    @Autowired
    private OrderService orderService;
    
    @Transactional
    public void consume(Message message) {
        String messageId = message.getMessageId();
        
        // 1. 尝试插入去重表
        MessageLog log = new MessageLog();
        log.setMessageId(messageId);
        log.setStatus("PROCESSING");
        log.setCreateTime(new Date());
        
        try {
            messageLogMapper.insert(log);  // 唯一索引保证不会重复
        } catch (DuplicateKeyException e) {
            // 消息已处理过，直接返回
            log.info("消息已处理: {}", messageId);
            return;
        }
        
        try {
            // 2. 处理业务逻辑
            orderService.createOrder(message.getOrderInfo());
            
            // 3. 更新状态为成功
            messageLogMapper.updateStatus(messageId, "SUCCESS");
        } catch (Exception e) {
            // 处理失败，更新状态
            messageLogMapper.updateStatus(messageId, "FAILED");
            throw e;
        }
    }
}

-- 去重表结构
CREATE TABLE message_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    create_time DATETIME NOT NULL,
    UNIQUE KEY uk_message_id (message_id)
);
```

### 2.3 Redis 去重

```java
/**
 * 方案2: Redis SETNX 去重 (推荐，高性能)
 */
@Service
public class MessageConsumer {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    private static final String KEY_PREFIX = "mq:consumed:";
    private static final long EXPIRE_DAYS = 7;  // 保留7天
    
    public void consume(Message message) {
        String messageId = message.getMessageId();
        String key = KEY_PREFIX + messageId;
        
        // SETNX: 只有key不存在时才设置成功
        Boolean success = redisTemplate.opsForValue()
            .setIfAbsent(key, "1", EXPIRE_DAYS, TimeUnit.DAYS);
        
        if (!Boolean.TRUE.equals(success)) {
            // key 已存在，说明消息已处理
            log.info("消息重复，跳过: {}", messageId);
            return;
        }
        
        try {
            // 处理业务逻辑
            processMessage(message);
        } catch (Exception e) {
            // 处理失败，删除 key，允许重试
            redisTemplate.delete(key);
            throw e;
        }
    }
}
```

### 2.4 数据库唯一约束

```java
/**
 * 方案3: 利用业务唯一约束
 */
@Service
public class OrderService {
    
    @Autowired
    private OrderMapper orderMapper;
    
    public void createOrder(OrderDTO dto) {
        Order order = new Order();
        order.setOrderNo(dto.getOrderNo());  // 订单号唯一
        order.setUserId(dto.getUserId());
        order.setAmount(dto.getAmount());
        
        try {
            // order_no 有唯一索引，重复插入会失败
            orderMapper.insert(order);
        } catch (DuplicateKeyException e) {
            // 订单已存在，幂等处理
            log.info("订单已存在: {}", dto.getOrderNo());
        }
    }
}
```

### 2.5 乐观锁/版本号

```java
/**
 * 方案4: 乐观锁 (适合更新操作)
 */
@Service
public class AccountService {
    
    @Autowired
    private AccountMapper accountMapper;
    
    public boolean deductBalance(Long userId, BigDecimal amount, Integer version) {
        // UPDATE account 
        // SET balance = balance - #{amount}, version = version + 1
        // WHERE user_id = #{userId} AND version = #{version}
        int rows = accountMapper.deductWithVersion(userId, amount, version);
        
        // 返回0说明版本号不匹配，已被其他操作修改
        return rows > 0;
    }
}
```

### 2.6 状态机

```java
/**
 * 方案5: 状态机 (只允许状态单向流转)
 */
@Service
public class OrderService {
    
    public void payOrder(String orderNo) {
        // UPDATE orders 
        // SET status = 'PAID' 
        // WHERE order_no = #{orderNo} AND status = 'UNPAID'
        int rows = orderMapper.updateStatusToPaid(orderNo);
        
        if (rows == 0) {
            // 状态不是 UNPAID，说明已处理过或状态不对
            log.info("订单状态不允许支付: {}", orderNo);
        }
    }
}

订单状态流转:
UNPAID → PAID → SHIPPED → COMPLETED
                  ↓
               CANCELLED

只允许单向流转，天然幂等
```

---

## 三、Kafka 幂等生产者

```java
/**
 * Kafka 生产者幂等性配置 (解决生产者重复发送)
 */
Properties props = new Properties();
props.put("bootstrap.servers", "localhost:9092");
props.put("enable.idempotence", "true");  // 开启幂等
props.put("acks", "all");
props.put("retries", Integer.MAX_VALUE);

// Kafka 通过 PID + Sequence Number 实现幂等
// 相同 PID 的相同 Sequence 消息，Broker 只保留一份
```

---

## 四、面试回答

### 30秒版本

> 消息重复不可避免（网络超时、消费者宕机、主从切换等），需要**消费端保证幂等性**。
>
> 常用方案：
> 1. **唯一ID + Redis/数据库去重**：消费前判断是否处理过
> 2. **数据库唯一约束**：利用唯一索引防止重复插入
> 3. **乐观锁/版本号**：更新时带版本号条件
> 4. **状态机**：只允许状态单向流转
>
> 推荐 Redis SETNX 去重，高性能且实现简单。

### 1分钟版本

> **消息重复的原因**：
> - 生产者：发送后 ACK 超时重试
> - 消费者：处理成功但 ACK 失败，消息被重新投递
> - Broker：主从切换可能导致重复
>
> **解决方案**：
>
> 1. **唯一ID + 去重表**
>    - 每条消息带唯一 messageId
>    - 消费前查表或用 Redis SETNX 判断是否处理过
>    - 处理后记录 messageId
>
> 2. **数据库唯一约束**
>    - 利用业务字段的唯一索引（如订单号）
>    - 重复插入会抛异常，捕获后忽略
>
> 3. **乐观锁/版本号**
>    - 更新操作带 version 条件
>    - `WHERE version = #{version}` 更新失败说明已被处理
>
> 4. **状态机**
>    - 业务状态只允许单向流转
>    - `WHERE status = 'UNPAID'` 天然幂等
>
> **Kafka 生产者幂等**：
> 开启 `enable.idempotence=true`，通过 PID + Sequence Number 去重。

---

## 五、最佳实践

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          最佳实践                                                 │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ✅ 推荐做法:                                                                    │
│  • 生产者发送时带全局唯一 messageId (UUID/雪花ID)                                │
│  • 高性能场景用 Redis SETNX 去重                                                 │
│  • 需要持久化用数据库去重表                                                      │
│  • 更新操作用乐观锁或状态机                                                      │
│  • 去重记录设置过期时间，避免无限增长                                            │
│                                                                                  │
│  ❌ 避免做法:                                                                    │
│  • 依赖 MQ 的 exactly-once 语义（大多数 MQ 不支持）                              │
│  • 不做幂等处理，指望消息不会重复                                                │
│  • 去重表不设过期时间                                                            │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```
