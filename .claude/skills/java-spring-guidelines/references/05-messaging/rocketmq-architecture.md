# RocketMQ 架构原理

> Java/Spring Boot 编码规范 - RocketMQ 深度架构与技术选型

---

## RocketMQ 架构概述

### 核心组件

```
                    ┌──────────────┐
                    │  NameServer  │ ← 注册中心（无状态集群）
                    └──────────────┘
                      ↑           ↑
          注册/心跳   │           │ 路由查询
                      ↓           ↓
         ┌────────────────┐   ┌─────────────┐
         │     Broker     │   │  Producer   │
         │  (消息存储)    │←─┤  (消息生产)  │
         └────────────────┘   └─────────────┘
                ↓
         ┌─────────────┐
         │  Consumer   │
         │  (消息消费)  │
         └─────────────┘
```

| 组件 | 说明 | 作用 |
|------|------|------|
| **NameServer** | 注册中心 | 管理 Broker 路由信息，无状态集群 |
| **Broker** | 消息存储 | 存储消息、处理消息读写 |
| **Producer** | 消息生产者 | 发送消息到 Broker |
| **Consumer** | 消息消费者 | 从 Broker 拉取消息消费 |

---

## NameServer vs Zookeeper

### 为什么不用 Zookeeper？

**RocketMQ 自研 NameServer 的原因：**

| 对比项 | NameServer | Zookeeper |
|--------|-----------|-----------|
| **复杂度** | 🟢 轻量级，代码简单 | 🔴 重量级，复杂 |
| **依赖** | 🟢 无外部依赖 | 🔴 需要额外部署 Zookeeper 集群 |
| **一致性** | 🟡 最终一致性（AP） | 🟢 强一致性（CP） |
| **性能** | 🟢 高性能（无选举） | 🟡 中等（需要选举）|
| **可用性** | 🟢 高可用（无主节点）| 🟡 选举期间不可用 |
| **运维** | 🟢 简单（无状态） | 🔴 复杂（有状态）|
| **适用场景** | 消息队列路由 | 配置中心、分布式锁 |

### NameServer 设计优势

**1. 无状态设计**

```
Zookeeper（有状态）：
- 需要选举 Leader
- Leader 挂了需要重新选举（30-120s）
- 选举期间不可用

NameServer（无状态）：
- 每个节点完全独立
- 任意节点挂了不影响其他节点
- 无选举过程，秒级恢复
```

**2. 最终一致性满足需求**

```
消息队列场景：
- Broker 路由信息变化不频繁
- 短暂的路由不一致可以接受（几秒内同步）
- Producer/Consumer 有重试机制

不需要 Zookeeper 的强一致性（CAP 理论选择 AP）
```

**3. 简化架构**

```
使用 Zookeeper：
RocketMQ → Zookeeper → ZAB 协议 → Paxos 算法
（引入额外的复杂度和依赖）

使用 NameServer：
RocketMQ → NameServer（轻量级路由注册）
（架构简单，易维护）
```

### NameServer 工作原理

**1. Broker 注册与心跳**

```java
// Broker 启动时注册到所有 NameServer
Broker → NameServer1: 注册（IP、端口、Topic 列表）
Broker → NameServer2: 注册（IP、端口、Topic 列表）
Broker → NameServer3: 注册（IP、端口、Topic 列表）

// Broker 定期发送心跳（默认 30s）
Broker → NameServer1: 心跳（我还活着）
Broker → NameServer2: 心跳（我还活着）
Broker → NameServer3: 心跳（我还活着）

// NameServer 检测 Broker 健康（超过 120s 未收到心跳则剔除）
NameServer → Broker: 超时未响应，标记为下线
```

**2. Producer/Consumer 路由发现**

```java
// Producer 启动时从 NameServer 拉取路由信息
Producer → NameServer1: 查询 Topic "order-topic" 的 Broker 列表
NameServer1 → Producer: 返回 [Broker1, Broker2, Broker3]

// Producer 缓存路由信息，定期更新（默认 30s）
Producer 本地缓存: order-topic → [Broker1, Broker2, Broker3]

// Consumer 同理
Consumer → NameServer1: 查询 Topic "order-topic" 的 Broker 列表
NameServer1 → Consumer: 返回 [Broker1, Broker2, Broker3]
```

**3. 路由信息同步**

```
NameServer 节点之间不通信，数据不互相同步！

NameServer1: Broker1, Broker2, Broker3  ← 独立维护
NameServer2: Broker1, Broker2, Broker3  ← 独立维护
NameServer3: Broker1, Broker2, Broker3  ← 独立维护

每个 NameServer 通过 Broker 的注册和心跳独立维护路由信息
最终一致性：几秒内所有 NameServer 数据一致
```

---

## NameServer 高可用设计

### 部署架构

```
            Producer/Consumer
                   ↓
        连接所有 NameServer（随机选择一个）
                   ↓
     ┌─────────────┼─────────────┐
     ↓             ↓             ↓
NameServer1   NameServer2   NameServer3
     ↑             ↑             ↑
     └─────────────┼─────────────┘
                   ↓
              Broker 集群
           （注册到所有 NameServer）
```

### 容错机制

**1. NameServer 节点宕机**

```
场景：NameServer2 宕机

影响：
- Producer/Consumer 会自动切换到 NameServer1 或 NameServer3
- 无需人工干预
- 秒级切换，业务无感知

恢复：
- NameServer2 恢复后，Broker 重新注册
- 数据自动同步（通过 Broker 心跳）
```

**2. Broker 宕机**

```
场景：Broker2 宕机

NameServer 处理：
- 120s 后未收到心跳，标记 Broker2 为下线
- 将 Broker2 从路由表中移除

Producer/Consumer 处理：
- 下次更新路由信息时（30s），发现 Broker2 不可用
- 自动切换到 Broker1 或 Broker3
- 消息发送/消费自动重试
```

---

## RocketMQ vs Kafka vs RabbitMQ

### 架构对比

| 特性 | RocketMQ | Kafka | RabbitMQ |
|------|----------|-------|----------|
| **注册中心** | NameServer | Zookeeper/KRaft | 无 |
| **消息模型** | 发布订阅 + 队列 | 发布订阅 | 多种模式 |
| **顺序消息** | ✅ 支持（分区顺序）| ✅ 支持（分区顺序）| ⚠️ 单队列顺序 |
| **延迟消息** | ✅ 18个级别 | ❌ 不支持 | ✅ TTL + 死信 |
| **事务消息** | ✅ 支持 | ✅ 支持 | ✅ 支持 |
| **消息回溯** | ✅ 支持 | ✅ 支持 | ❌ 不支持 |
| **吞吐量** | 🟢 十万级 | 🟢 百万级 | 🟡 万级 |
| **延迟** | 🟡 毫秒级 | 🟡 毫秒级 | 🟢 微秒级 |
| **语言** | Java | Scala/Java | Erlang |
| **运维** | 🟡 中等 | 🔴 复杂 | 🟢 简单 |

### 选型建议

```java
// ✅ RocketMQ 适用场景
// - 需要事务消息（订单、支付）
// - 需要延迟消息（订单超时取消）
// - 需要消息回溯（数据修复）
// - 对顺序消息有要求
// - Java 技术栈

// ✅ Kafka 适用场景
// - 超高吞吐量（日志收集、埋点上报）
// - 大数据处理（Flink、Spark 集成）
// - 消息存储时间长（日志审计）
// - 需要 exactly-once 语义

// ✅ RabbitMQ 适用场景
// - 中小规模系统（TPS < 1万）
// - 需要低延迟（微秒级）
// - 灵活的路由规则（Topic Exchange）
// - 不需要消息回溯
```

---

## RocketMQ 事务消息原理

### 二阶段提交

```
阶段1：发送半消息（Half Message）
Producer → Broker: 发送半消息（消费者不可见）
Broker → Producer: 半消息发送成功

阶段2：执行本地事务
Producer: 执行本地数据库事务
Producer → Broker: Commit（成功）或 Rollback（失败）

阶段3：Broker 提交消息
Broker: 收到 Commit，消息对消费者可见
Broker: 收到 Rollback，删除半消息

阶段4：事务回查（如果 Broker 未收到 Commit/Rollback）
Broker → Producer: 回查本地事务状态
Producer → Broker: 返回 COMMIT_MESSAGE / ROLLBACK_MESSAGE
```

**代码示例：**

```java
@Service
@RequiredArgsConstructor
public class OrderProducer {

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 发送事务消息
     */
    public void sendTransactionMessage(Order order) {
        TransactionSendResult result = rocketMQTemplate.sendMessageInTransaction(
            "order-topic",
            MessageBuilder.withPayload(order).build(),
            order  // 事务参数
        );
        
        log.info("[RocketMQ]，事务消息发送，状态: {}", result.getLocalTransactionState());
    }
}

/**
 * 事务监听器
 */
@Component
@RocketMQTransactionListener
public class OrderTransactionListener implements RocketMQLocalTransactionListener {

    @Autowired
    private OrderService orderService;

    /**
     * 执行本地事务
     */
    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        try {
            Order order = (Order) arg;
            // 执行本地数据库事务
            orderService.createOrder(order);
            // 提交消息
            return RocketMQLocalTransactionState.COMMIT;
        } catch (Exception e) {
            log.error("[本地事务]，执行失败", e);
            // 回滚消息
            return RocketMQLocalTransactionState.ROLLBACK;
        }
    }

    /**
     * 回查本地事务状态
     */
    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        // 查询数据库，确认订单是否创建成功
        String orderId = msg.getHeaders().get("orderId", String.class);
        Order order = orderService.getById(orderId);
        
        if (order != null) {
            return RocketMQLocalTransactionState.COMMIT;
        }
        return RocketMQLocalTransactionState.ROLLBACK;
    }
}
```

---

## 事务消息对比

### RocketMQ 事务消息的缺点

**【推荐】了解 RocketMQ 事务消息的局限性。**

| 缺点 | 说明 | 影响 |
|------|------|------|
| **只支持单向发送** | 无法接收响应 | 不适合需要返回结果的场景 |
| **回查开销** | 需要实现回查接口 | 增加系统复杂度 |
| **延迟较高** | 二阶段提交有延迟 | 不适合低延迟场景 |
| **不保证完全一致** | 极端情况下可能不一致 | 需要补偿机制 |

### Kafka 事务消息实现

**Kafka 事务消息特点：**

```
Kafka 事务消息与 RocketMQ 不同：

1. 支持跨分区、跨 Topic 的原子性写入
2. 使用事务协调器（Transaction Coordinator）
3. 采用两阶段提交协议
4. 通过事务日志保证一致性

优势：
- 支持 exactly-once 语义
- 适合流处理场景（Kafka Streams、Flink）

劣势：
- 性能开销较大
- 不支持与外部系统（数据库）的事务
```

**Kafka 事务消息示例：**

```java
// Kafka 事务配置
Properties props = new Properties();
props.put("bootstrap.servers", "localhost:9092");
props.put("transactional.id", "my-transactional-id");
props.put("enable.idempotence", "true");

KafkaProducer<String, String> producer = new KafkaProducer<>(props);

// 初始化事务
producer.initTransactions();

try {
    // 开始事务
    producer.beginTransaction();
    
    // 发送消息（可以跨多个 Topic）
    producer.send(new ProducerRecord<>("topic1", "key1", "value1"));
    producer.send(new ProducerRecord<>("topic2", "key2", "value2"));
    
    // 提交事务
    producer.commitTransaction();
    
} catch (Exception e) {
    // 回滚事务
    producer.abortTransaction();
}
```

### 其他事务消息实现

**1. 本地消息表（最常用）**

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final MessageMapper messageMapper;

    /**
     * 本地消息表方案
     */
    @Transactional(rollbackFor = Exception.class)
    public void createOrder(Order order) {
        // 1. 插入订单
        orderMapper.insert(order);
        
        // 2. 插入消息表（同一事务）
        Message message = new Message();
        message.setTopic("order-topic");
        message.setContent(JSON.toJSONString(order));
        message.setStatus(MessageStatus.PENDING);
        messageMapper.insert(message);
        
        // 事务提交后，订单和消息都持久化
    }
    
    /**
     * 定时任务扫描消息表，发送未发送的消息
     */
    @Scheduled(fixedDelay = 5000)
    public void sendPendingMessages() {
        List<Message> messages = messageMapper.selectByStatus(MessageStatus.PENDING);
        for (Message message : messages) {
            try {
                mqProducer.send(message.getTopic(), message.getContent());
                message.setStatus(MessageStatus.SENT);
                messageMapper.updateById(message);
            } catch (Exception e) {
                log.error("消息发送失败: {}", message.getId(), e);
            }
        }
    }
}
```

**优势：**
- ✅ 实现简单，无需依赖 MQ 的事务支持
- ✅ 保证最终一致性
- ✅ 适合任何消息队列

**2. Seata 分布式事务**

```java
@Service
public class OrderService {

    @GlobalTransactional(rollbackFor = Exception.class)
    public void createOrder(Order order) {
        // 1. 本地事务：插入订单
        orderMapper.insert(order);
        
        // 2. 调用库存服务（分布式事务）
        stockService.deduct(order.getProductId(), order.getQuantity());
        
        // 3. 发送消息
        mqProducer.send("order-topic", order);
        
        // Seata 保证所有操作的原子性
    }
}
```

**3. TCC 事务模式**

```java
public interface AccountService {
    
    // Try：预留资源
    boolean tryDeduct(String accountId, BigDecimal amount);
    
    // Confirm：确认提交
    boolean confirmDeduct(String accountId);
    
    // Cancel：取消回滚
    boolean cancelDeduct(String accountId);
}
```

**对比总结：**

| 方案 | 优势 | 劣势 | 适用场景 |
|------|------|------|----------|
| **RocketMQ 事务消息** | 保证最终一致性 | 需要回查接口 | 订单、支付 |
| **Kafka 事务消息** | exactly-once 语义 | 不支持外部系统 | 流处理 |
| **本地消息表** | 简单易实现 | 需要定时扫描 | 通用场景 |
| **Seata** | 强一致性 | 性能开销大 | 金融场景 |
| **TCC** | 灵活可控 | 实现复杂 | 高一致性要求 |

---

## 最佳实践

### NameServer 部署

```yaml
# 生产环境建议：至少 3 个 NameServer 节点
nameserver:
  nodes:
    - 192.168.1.10:9876
    - 192.168.1.11:9876
    - 192.168.1.12:9876

# Spring Boot 配置
rocketmq:
  name-server: 192.168.1.10:9876;192.168.1.11:9876;192.168.1.12:9876
```

### Broker 配置

```properties
# broker.conf

# Broker 名称（集群内唯一）
brokerName=broker-a

# Broker 角色（ASYNC_MASTER, SYNC_MASTER, SLAVE）
brokerRole=ASYNC_MASTER

# 刷盘策略（ASYNC_FLUSH, SYNC_FLUSH）
flushDiskType=ASYNC_FLUSH

# NameServer 地址
namesrvAddr=192.168.1.10:9876;192.168.1.11:9876;192.168.1.12:9876

# 存储路径
storePathRootDir=/data/rocketmq/store
storePathCommitLog=/data/rocketmq/store/commitlog
```

### Producer 配置

```java
@Bean
public RocketMQTemplate rocketMQTemplate() {
    RocketMQTemplate template = new RocketMQTemplate();
    // 设置发送超时时间
    template.setSendMsgTimeout(3000);
    // 设置异步发送消息池大小
    template.setAsyncSenderExecutor(new ThreadPoolExecutor(
        10, 20, 60, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(1000)
    ));
    return template;
}
```

---

## 面试常见问题解答

### Q1: 为什么 RocketMQ 不用 Zookeeper？

**答案：**

1. **架构简化**：Zookeeper 引入额外复杂度和依赖
2. **高可用**：NameServer 无主节点，无选举过程，可用性更高
3. **性能**：NameServer 轻量级，无一致性协议开销
4. **需求匹配**：消息队列只需要最终一致性（AP），不需要强一致性（CP）

### Q2: NameServer 宕机会影响消息收发吗？

**答案：**不会立即影响。

- Producer/Consumer 有本地路由缓存（30s 更新）
- 短时间内可以正常收发消息
- NameServer 恢复后自动同步

### Q3: RocketMQ 如何保证消息顺序？

**答案：**通过 MessageQueue 保证分区顺序。

```java
// 同一订单ID的消息发送到同一队列
rocketMQTemplate.syncSendOrderly(
    "order-topic",
    message,
    order.getId().toString()  // 使用订单ID作为分片键
);
```

---

## 参考资料

- RocketMQ 官方文档
- 《RocketMQ 实战与原理解析》
- Apache RocketMQ 源码
