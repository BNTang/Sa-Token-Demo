# 消息队列如何保证消息有序性

> 分类: 消息队列 | 难度: ⭐⭐⭐⭐ | 频率: 高频

---

## 一、消息乱序的原因

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          消息乱序的原因                                           │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  1. 多分区/多队列导致乱序                                                        │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │  Producer ─→ 消息1 ─→ Partition 0 ─→ Consumer 1                            │ │
│  │          ─→ 消息2 ─→ Partition 1 ─→ Consumer 2                            │ │
│  │          ─→ 消息3 ─→ Partition 0 ─→ Consumer 1                            │ │
│  │                                                                            │ │
│  │  消息2 可能比 消息1 更早被处理                                             │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  2. 消费者多线程处理导致乱序                                                     │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │  Queue ─→ 消息1,2,3 ─→ Consumer                                            │ │
│  │                           ├─→ Thread1 处理消息1 (耗时长)                    │ │
│  │                           ├─→ Thread2 处理消息2 (快速完成)                  │ │
│  │                           └─→ Thread3 处理消息3 (快速完成)                  │ │
│  │                                                                            │ │
│  │  消息2、3 可能比 消息1 更早处理完成                                        │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  3. 重试导致乱序                                                                 │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │  消息1 处理失败，进入重试队列                                               │ │
│  │  消息2、3 正常处理                                                          │ │
│  │  消息1 重试成功，但顺序已经在 2、3 之后了                                   │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 二、保证有序性的方案

### 2.1 全局有序 vs 局部有序

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                    全局有序 vs 局部有序                                           │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  全局有序: 所有消息严格按照发送顺序消费                                          │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │  • 只用单分区/单队列                                                        │ │
│  │  • 只有一个消费者                                                           │ │
│  │  • 性能极差，不推荐                                                         │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  局部有序: 同一业务ID的消息有序 (推荐)                                           │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │  • 同一订单的消息发送到同一分区                                             │ │
│  │  • 同一分区只有一个消费者消费                                               │ │
│  │  • 兼顾有序性和并发性能                                                     │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  大多数业务场景只需要局部有序!                                                   │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Kafka 顺序消息

```java
/**
 * Kafka 顺序消息
 * 核心: 同一 key 的消息发送到同一 partition
 */
public class KafkaOrderProducer {
    
    private KafkaProducer<String, String> producer;
    
    public void sendOrderMessage(String orderId, String message) {
        // 指定 key = orderId
        // Kafka 会对 key 做 hash，相同 key 一定进入同一 partition
        ProducerRecord<String, String> record = new ProducerRecord<>(
            "order-topic",
            orderId,    // key: 订单ID
            message     // value: 消息内容
        );
        
        producer.send(record);
    }
}

/**
 * Kafka 消费者配置
 */
Properties props = new Properties();
props.put("bootstrap.servers", "localhost:9092");
props.put("group.id", "order-group");
// 一个 partition 只能被同一消费组的一个消费者消费
// 保证了分区内消息的有序消费

// 注意: 消费者内部不要用多线程处理，否则会乱序
consumer.poll(Duration.ofMillis(100));
for (ConsumerRecord<String, String> record : records) {
    // 单线程顺序处理
    processMessage(record);
}
```

### 2.3 RocketMQ 顺序消息

```java
/**
 * RocketMQ 顺序消息生产者
 */
public class RocketMQOrderProducer {
    
    private DefaultMQProducer producer;
    
    public void sendOrderMessage(String orderId, String message) throws Exception {
        Message msg = new Message("OrderTopic", "TagA", message.getBytes());
        
        // 使用 MessageQueueSelector 选择队列
        // 相同 orderId 的消息进入相同队列
        SendResult result = producer.send(msg, new MessageQueueSelector() {
            @Override
            public MessageQueue select(List<MessageQueue> mqs, Message msg, Object arg) {
                String orderId = (String) arg;
                int index = Math.abs(orderId.hashCode()) % mqs.size();
                return mqs.get(index);
            }
        }, orderId);
    }
}

/**
 * RocketMQ 顺序消息消费者
 */
public class RocketMQOrderConsumer {
    
    public void consume() throws Exception {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("OrderConsumerGroup");
        consumer.setNamesrvAddr("localhost:9876");
        consumer.subscribe("OrderTopic", "*");
        
        // 使用 MessageListenerOrderly 保证顺序消费
        consumer.registerMessageListener(new MessageListenerOrderly() {
            @Override
            public ConsumeOrderlyStatus consumeMessage(
                    List<MessageExt> msgs, ConsumeOrderlyContext context) {
                for (MessageExt msg : msgs) {
                    // 顺序处理消息
                    processMessage(msg);
                }
                return ConsumeOrderlyStatus.SUCCESS;
            }
        });
        
        consumer.start();
    }
}
```

### 2.4 RabbitMQ 顺序消息

```java
/**
 * RabbitMQ 顺序消息
 * 方案: 单队列 + 单消费者
 */
public class RabbitMQOrderConsumer {
    
    @RabbitListener(queues = "order-queue", concurrency = "1")  // 单消费者
    public void handleMessage(Message message) {
        // 单线程顺序处理
        processMessage(message);
    }
}

/**
 * 或者: 按业务ID路由到不同队列
 */
@Bean
public DirectExchange orderExchange() {
    return new DirectExchange("order-exchange");
}

// 多个队列，每个队列处理特定订单范围
@Bean
public Queue orderQueue0() { return new Queue("order-queue-0"); }
@Bean
public Queue orderQueue1() { return new Queue("order-queue-1"); }
// ...

// 按订单ID hash 选择队列
public void sendOrderMessage(String orderId, String message) {
    int queueIndex = Math.abs(orderId.hashCode()) % QUEUE_COUNT;
    String routingKey = "order-" + queueIndex;
    rabbitTemplate.convertAndSend("order-exchange", routingKey, message);
}
```

---

## 三、消费端保证顺序

```java
/**
 * 消费端顺序处理 (内存队列 + 单线程)
 */
public class OrderedConsumer {
    
    // 每个业务ID一个队列
    private final Map<String, BlockingQueue<Message>> orderQueues = new ConcurrentHashMap<>();
    
    // 每个队列一个处理线程
    private final Map<String, Thread> processors = new ConcurrentHashMap<>();
    
    public void onMessage(Message message) {
        String orderId = message.getOrderId();
        
        // 获取或创建该订单的队列
        BlockingQueue<Message> queue = orderQueues.computeIfAbsent(
            orderId, k -> {
                BlockingQueue<Message> q = new LinkedBlockingQueue<>();
                // 启动处理线程
                Thread t = new Thread(() -> processQueue(q));
                t.start();
                processors.put(k, t);
                return q;
            }
        );
        
        // 消息入队，由专属线程顺序处理
        queue.offer(message);
    }
    
    private void processQueue(BlockingQueue<Message> queue) {
        while (true) {
            try {
                Message msg = queue.take();
                // 顺序处理
                handleMessage(msg);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
```

---

## 四、面试回答

### 30秒版本

> 消息乱序原因：多分区、多线程消费、重试。
>
> **解决方案**：
> 1. **发送端**：相同业务 key 的消息发送到同一分区/队列
> 2. **消费端**：同一分区只有一个消费者，单线程处理
>
> 大多数场景只需**局部有序**（同一订单有序），不需要全局有序。
>
> Kafka 用 key 路由；RocketMQ 用 MessageQueueSelector；RabbitMQ 用单队列或按 key 路由。

### 1分钟版本

> **消息乱序的原因**：
> - 多分区：消息分散到不同分区，消费顺序不确定
> - 多线程消费：同一消费者内多线程处理导致完成顺序不一致
> - 重试：失败消息重试后顺序靠后
>
> **全局有序 vs 局部有序**：
> - 全局有序：单分区 + 单消费者，性能极差
> - 局部有序：同一业务 ID 的消息有序即可，推荐
>
> **Kafka 方案**：
> - 发送时指定 key（如订单ID），相同 key 进入同一 partition
> - 同一 partition 只被一个消费者消费
> - 消费者内单线程处理
>
> **RocketMQ 方案**：
> - 使用 MessageQueueSelector 按业务 ID 选择队列
> - 使用 MessageListenerOrderly 顺序消费
>
> **消费端增强**：
> - 按业务 ID 建立内存队列
> - 每个队列一个处理线程
> - 保证同一业务 ID 的消息串行处理
