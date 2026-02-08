# 消息队列如何保证消息不丢失

> 分类: 消息队列 | 难度: ⭐⭐⭐⭐ | 频率: 高频

---

## 一、消息丢失的三个环节

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          消息传递全链路                                           │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│   Producer ────────→ Broker ────────→ Consumer                                   │
│       │                   │                │                                     │
│       ↓                   ↓                ↓                                     │
│  ① 发送丢失         ② 存储丢失         ③ 消费丢失                               │
│                                                                                  │
│  ① 发送丢失:                                                                     │
│  • 网络故障，消息未到达 Broker                                                   │
│  • Producer 没等到 ACK 就认为发送成功                                            │
│                                                                                  │
│  ② 存储丢失:                                                                     │
│  • Broker 收到消息后未持久化就宕机                                               │
│  • 主从切换时未同步的消息丢失                                                    │
│                                                                                  │
│  ③ 消费丢失:                                                                     │
│  • 消费者拿到消息后，还没处理完就宕机                                            │
│  • 消费者先提交 offset，后处理失败                                               │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 二、Kafka 保证消息不丢失

### 2.1 生产端

```java
/**
 * Kafka 生产端配置
 */
Properties props = new Properties();
props.put("bootstrap.servers", "localhost:9092");

// 关键配置1: acks
// acks=0: 不等待确认 (可能丢失)
// acks=1: Leader 写入即确认 (主挂了可能丢失)
// acks=all/-1: 所有 ISR 副本写入才确认 (最安全)
props.put("acks", "all");

// 关键配置2: 重试
props.put("retries", Integer.MAX_VALUE);
props.put("retry.backoff.ms", 100);

// 关键配置3: 幂等
props.put("enable.idempotence", "true");

// 关键配置4: 异步发送需要回调处理失败
producer.send(record, (metadata, exception) -> {
    if (exception != null) {
        // 发送失败，记录日志或重试
        log.error("发送失败: {}", exception.getMessage());
        // 可以存入本地，后续补发
    }
});
```

### 2.2 Broker 端

```properties
# Kafka Broker 配置

# 副本数 >= 2
default.replication.factor=3

# ISR 最少副本数
min.insync.replicas=2

# 禁止非 ISR 副本选举为 Leader
unclean.leader.election.enable=false
```

### 2.3 消费端

```java
/**
 * Kafka 消费端 - 手动提交 offset
 */
Properties props = new Properties();
props.put("enable.auto.commit", "false");  // 关闭自动提交

KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
consumer.subscribe(Collections.singletonList("my-topic"));

while (true) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
    for (ConsumerRecord<String, String> record : records) {
        try {
            // 1. 先处理消息
            processMessage(record);
            
            // 2. 处理成功后再提交
            consumer.commitSync();
        } catch (Exception e) {
            // 处理失败，不提交，下次重新消费
            log.error("处理失败: {}", e.getMessage());
        }
    }
}
```

---

## 三、RocketMQ 保证消息不丢失

### 3.1 生产端

```java
/**
 * RocketMQ 生产端 - 同步发送
 */
DefaultMQProducer producer = new DefaultMQProducer("producer-group");
producer.setNamesrvAddr("localhost:9876");
producer.setRetryTimesWhenSendFailed(3);  // 失败重试次数
producer.start();

Message msg = new Message("TopicA", "TagA", "消息内容".getBytes());

// 同步发送，等待 Broker 确认
SendResult result = producer.send(msg);
if (result.getSendStatus() == SendStatus.SEND_OK) {
    // 发送成功
} else {
    // 发送失败，处理
}

/**
 * 事务消息 (强一致)
 */
TransactionMQProducer transactionProducer = new TransactionMQProducer("tx-group");
transactionProducer.setTransactionListener(new TransactionListener() {
    @Override
    public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        // 执行本地事务
        try {
            doLocalTransaction();
            return LocalTransactionState.COMMIT_MESSAGE;
        } catch (Exception e) {
            return LocalTransactionState.ROLLBACK_MESSAGE;
        }
    }
    
    @Override
    public LocalTransactionState checkLocalTransaction(MessageExt msg) {
        // 回查本地事务状态
        return checkTransactionStatus(msg.getTransactionId());
    }
});
```

### 3.2 Broker 端

```properties
# RocketMQ Broker 配置

# 同步刷盘 (默认异步)
flushDiskType=SYNC_FLUSH

# 主从同步复制 (默认异步)
brokerRole=SYNC_MASTER
```

### 3.3 消费端

```java
/**
 * RocketMQ 消费端
 */
DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("consumer-group");
consumer.setNamesrvAddr("localhost:9876");
consumer.subscribe("TopicA", "*");

consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
    try {
        for (MessageExt msg : msgs) {
            // 处理消息
            processMessage(msg);
        }
        // 处理成功才返回 CONSUME_SUCCESS
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    } catch (Exception e) {
        // 返回 RECONSUME_LATER，消息会重新投递
        return ConsumeConcurrentlyStatus.RECONSUME_LATER;
    }
});

consumer.start();
```

---

## 四、RabbitMQ 保证消息不丢失

```java
/**
 * RabbitMQ 生产端 - 确认机制
 */
@Configuration
public class RabbitConfig {
    
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        
        // 开启发送确认
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                // 消息未到达交换机，重发或记录
                log.error("消息发送失败: {}", cause);
            }
        });
        
        // 开启返回确认 (消息未路由到队列)
        template.setReturnsCallback(returned -> {
            log.error("消息未路由到队列: {}", returned.getMessage());
        });
        
        return template;
    }
}

/**
 * Broker 端 - 持久化
 */
@Bean
public Queue durableQueue() {
    // durable=true 队列持久化
    return new Queue("my-queue", true);
}

// 消息持久化
MessageProperties props = new MessageProperties();
props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);

/**
 * 消费端 - 手动 ACK
 */
@RabbitListener(queues = "my-queue")
public void consume(Message message, Channel channel) throws IOException {
    try {
        processMessage(message);
        // 处理成功，手动 ACK
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    } catch (Exception e) {
        // 处理失败，拒绝消息，重新入队
        channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
    }
}
```

---

## 五、面试回答

### 30秒版本

> 消息可能在三个环节丢失：**生产端、Broker、消费端**。
>
> **解决方案**：
> - **生产端**：同步发送 + 发送确认 + 失败重试
> - **Broker**：持久化 + 主从同步复制
> - **消费端**：手动提交 offset，处理成功才确认
>
> Kafka：acks=all + 手动提交 offset；  
> RocketMQ：SYNC_FLUSH + SYNC_MASTER；  
> RabbitMQ：confirm + 持久化 + 手动 ACK。

### 1分钟版本

> **三个丢失环节**：
> 1. 生产端：网络故障、没等确认
> 2. Broker：未持久化就宕机、主从未同步
> 3. 消费端：处理前宕机、先提交后处理失败
>
> **Kafka 配置**：
> - 生产端：`acks=all`，等所有 ISR 副本确认
> - Broker：`min.insync.replicas=2`，`unclean.leader.election.enable=false`
> - 消费端：关闭自动提交，处理成功再 commitSync
>
> **RocketMQ 配置**：
> - 生产端：同步发送，事务消息
> - Broker：`SYNC_FLUSH` 同步刷盘，`SYNC_MASTER` 同步复制
> - 消费端：返回 `CONSUME_SUCCESS` 才确认
>
> **RabbitMQ 配置**：
> - 生产端：confirm 确认机制
> - Broker：队列和消息都持久化
> - 消费端：手动 ACK，处理失败 basicNack 重新入队
>
> **最佳实践**：
> - 生产端开启重试、记录发送日志
> - 消费端保证幂等性（消息可能重复）
> - 关键消息使用事务消息
