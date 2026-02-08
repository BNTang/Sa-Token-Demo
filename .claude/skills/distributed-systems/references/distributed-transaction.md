# 分布式事务完整方案

> 版本: 1.0 | 更新: 2026-02-05
>
* 分布式事务用于保证跨多个服务/数据库的数据一致性

---

## 概述

分布式事务是指在分布式系统中涉及多个服务或数据库的事务，需要保证这些操作要么全部成功，要么全部失败。

### 问题场景

```
┌────────────────────────────────────────────────────────────┐
│                    分布式事务问题                            │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  用户下单服务                                                │
│      │                                                     │
│      ├─> 扣除库存（库存服务）                               │
│      ├─> 创建订单（订单服务）                               │
│      └─> 扣减余额（账户服务）                               │
│                                                            │
│  问题：任何一个服务失败，都需要回滚所有服务                   │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

---

## 方案对比

| 方案 | 一致性 | 可用性 | 复杂度 | 性能 | 适用场景 |
|------|-------|--------|--------|------|---------|
| **2PC/XA** | 强 | 低 | 低 | 低 | 传统单体应用 |
| **TCC** | 最终 | 高 | 高 | 高 | 核心业务 |
| **SAGA** | 最终 | 高 | 中 | 高 | 长事务 |
| **本地消息表** | 最终 | 高 | 中 | 高 | 异步场景 |
| **MQ 事务消息** | 最终 | 高 | 中 | 高 | 高并发 |
| **Seata AT** | 最终 | 高 | 低 | 中 | 一般业务 |

---

## 方案一：Seata AT 模式（推荐）

### 依赖

```xml
<dependency>
    <groupId>io.seata</groupId>
    <artifactId>seata-spring-boot-starter</artifactId>
    <version>1.7.0</version>
</dependency>
```

### 配置

```yaml
seata:
  enabled: true
  application-id: my-service
  tx-service-group: my_tx_group
  service:
    vgroup-mapping:
      my_tx_group: default
    grouplist:
      default: localhost:8091
  registry:
    type: nacos
    nacos:
      server-addr: localhost:8848
      namespace: seata
      group: SEATA_GROUP
  config:
    type: nacos
    nacos:
      server-addr: localhost:8848
      namespace: seata
      group: SEATA_GROUP
```

### 使用

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final InventoryService inventoryService;
    private final AccountService accountService;
    private final OrderMapper orderMapper;

    @GlobalTransactional(name = "create-order", rollbackFor = Exception.class)
    public void createOrder(OrderDTO orderDTO) {
        // 1. 扣除库存
        inventoryService.deduct(orderDTO.getProductId(), orderDTO.getCount());

        // 2. 扣减余额
        accountService.deduct(orderDTO.getUserId(), orderDTO.getAmount());

        // 3. 创建订单
        Order order = new Order();
        order.setUserId(orderDTO.getUserId());
        order.setProductId(orderDTO.getProductId());
        order.setCount(orderDTO.getCount());
        order.setAmount(orderDTO.getAmount());
        orderMapper.insert(order);

        // 任何步骤失败，Seata 会自动回滚所有操作
    }
}
```

---

## 方案二：TCC 模式

### 定义接口

```java
public interface InventoryTccService {

    /**
     * Try：预留资源
     */
    @TwoPhaseBusinessAction(name = "inventoryDeduct", commitMethod = "commit", rollbackMethod = "rollback")
    boolean deduct(@BusinessActionContextParameter(paramName = "productId") Long productId,
                   @BusinessActionContextParameter(paramName = "count") Integer count);

    /**
     * Confirm：确认扣减
     */
    boolean commit(BusinessActionContext context);

    /**
     * Cancel：取消预留
     */
    boolean rollback(BusinessActionContext context);
}
```

### 实现

```java
@Service
public class InventoryTccServiceImpl implements InventoryTccService {

    @Autowired
    private InventoryMapper inventoryMapper;

    @Autowired
    private InventoryFreezeMapper freezeMapper;

    @Override
    @Transactional
    public boolean deduct(Long productId, Integer count) {
        // 1. 检查库存
        Inventory inventory = inventoryMapper.selectById(productId);
        if (inventory.getStock() < count) {
            throw new RuntimeException("库存不足");
        }

        // 2. 冻结库存
        InventoryFreeze freeze = new InventoryFreeze();
        freeze.setProductId(productId);
        freeze.setCount(count);
        freezeMapper.insert(freeze);

        return true;
    }

    @Override
    @Transactional
    public boolean commit(BusinessActionContext context) {
        Long productId = context.getActionContext("productId", Long.class);
        Integer count = context.getActionContext("count", Integer.class);

        // 1. 扣减真实库存
        inventoryMapper.deduct(productId, count);

        // 2. 删除冻结记录
        freezeMapper.deleteByProductId(productId);

        return true;
    }

    @Override
    @Transactional
    public boolean rollback(BusinessActionContext context) {
        Long productId = context.getActionContext("productId", Long.class);

        // 释放冻结库存
        freezeMapper.deleteByProductId(productId);

        return true;
    }
}
```

### 使用

```java
@Service
public class OrderService {

    @Autowired
    private InventoryTccService inventoryTccService;

    @GlobalTransactional
    public void createOrder(OrderDTO orderDTO) {
        // TCC 调用
        inventoryTccService.deduct(orderDTO.getProductId(), orderDTO.getCount());

        // 其他业务...
    }
}
```

---

## 方案三：本地消息表

### 建表

```sql
CREATE TABLE local_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id VARCHAR(64) NOT NULL UNIQUE,
    topic VARCHAR(64) NOT NULL,
    content JSON NOT NULL,
    status VARCHAR(16) DEFAULT 'PENDING',
    retry_times INT DEFAULT 0,
    next_retry_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status_retry (status, next_retry_time)
) COMMENT='本地消息表';
```

### 发送消息

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final LocalMessageMapper messageMapper;

    @Transactional
    public void createOrder(OrderDTO orderDTO) {
        // 1. 创建订单
        Order order = new Order();
        order.setUserId(orderDTO.getUserId());
        order.setAmount(orderDTO.getAmount());
        orderMapper.insert(order);

        // 2. 保存本地消息（同一事务）
        LocalMessage message = new LocalMessage();
        message.setMessageId(UUID.randomUUID().toString());
        message.setTopic("order.created");
        message.setContent(JSON.toJSONString(Map.of(
            "orderId", order.getId(),
            "userId", order.getUserId(),
            "amount", order.getAmount()
        )));
        message.setStatus("PENDING");
        messageMapper.insert(message);
    }
}
```

### 消息发送任务

```java
@Service
@RequiredArgsConstructor
public class MessageSendService {

    private final LocalMessageMapper messageMapper;
    private final RocketMQTemplate rocketMQTemplate;

    @Scheduled(fixedDelay = 5000)
    public void sendPendingMessages() {
        List<LocalMessage> messages = messageMapper.selectPendingMessages(100);

        for (LocalMessage message : messages) {
            try {
                // 发送消息
                rocketMQTemplate.syncSend(message.getTopic(), message.getContent());

                // 更新状态
                message.setStatus("SENT");
                messageMapper.updateById(message);

            } catch (Exception e) {
                // 更新重试次数和下次重试时间
                message.setRetryTimes(message.getRetryTimes() + 1);
                message.setNextRetryTime(calculateNextRetryTime(message.getRetryTimes()));
                messageMapper.updateById(message);
            }
        }
    }

    private LocalDateTime calculateNextRetryTime(int retryTimes) {
        // 指数退避
        long delay = (long) Math.pow(2, Math.min(retryTimes, 10)) * 1000;
        return LocalDateTime.now().plusSeconds(delay / 1000);
    }
}
```

---

## 方案四：SAGA 模式

### 定义状态

```java
public enum OrderSagaStatus {
    CREATED,         // 订单已创建
    INVENTORY_DEDUCTED,  // 库存已扣减
    ACCOUNT_DEDUCTED,    // 余额已扣减
    COMPLETED,       // 已完成
    COMPENSATING     // 补偿中
}
```

### SAGA 编排

```java
@Service
@RequiredArgsConstructor
public class OrderSagaService {

    private final InventoryService inventoryService;
    private final AccountService accountService;
    private final OrderSagaMapper sagaMapper;

    public void createOrder(OrderDTO orderDTO) {
        // 1. 创建 SAGA 实例
        OrderSaga saga = new OrderSaga();
        saga.setOrderId(UUID.randomUUID().toString());
        saga.setStatus(OrderSagaStatus.CREATED.name());
        sagaMapper.insert(saga);

        try {
            // 2. 扣除库存
            inventoryService.deduct(orderDTO.getProductId(), orderDTO.getCount());
            saga.setStatus(OrderSagaStatus.INVENTORY_DEDUCTED.name());
            sagaMapper.updateById(saga);

            // 3. 扣减余额
            accountService.deduct(orderDTO.getUserId(), orderDTO.getAmount());
            saga.setStatus(OrderSagaStatus.ACCOUNT_DEDUCTED.name());
            sagaMapper.updateById(saga);

            // 4. 完成
            saga.setStatus(OrderSagaStatus.COMPLETED.name());
            sagaMapper.updateById(saga);

        } catch (Exception e) {
            // 补偿
            compensate(saga);
            throw e;
        }
    }

    private void compensate(OrderSaga saga) {
        saga.setStatus(OrderSagaStatus.COMPENSATING.name());
        sagaMapper.updateById(saga);

        // 根据当前状态进行补偿
        if (saga.getStatus().equals(OrderSagaStatus.ACCOUNT_DEDUCTED.name())) {
            accountService.refund(saga.getUserId(), saga.getAmount());
        }

        if (saga.getStatus().equals(OrderSagaStatus.INVENTORY_DEDUCTED.name())) {
            inventoryService.refund(saga.getProductId(), saga.getCount());
        }
    }
}
```

---

## 方案选择建议

| 场景 | 推荐方案 | 理由 |
|------|---------|------|
| 一般业务 | Seata AT | 简单易用，代码侵入性小 |
| 核心业务 | TCC | 一致性强，性能好 |
| 长事务 | SAGA | 支持长事务，可补偿 |
| 异步场景 | 本地消息表 | 可靠性高，实现简单 |
| 高并发 | MQ 事务消息 | 性能好，解耦 |

---

## 参考资料

- Seata 官方文档：https://seata.io
- RocketMQ 事务消息：https://rocketmq.apache.org/zh/docs/transactionMessage
