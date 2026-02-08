# 并发控制

> Java/Spring Boot 编码规范 - 并发控制与幂等性

---

## 幂等性设计

写接口必须保证幂等，防止重复提交导致数据异常。

### 幂等性方案

| 方案 | 适用场景 | 实现方式 |
|------|---------|---------|
| **唯一索引** | 防止重复插入 | 数据库层面唯一约束 |
| **Token 机制** | 表单重复提交 | 提交前获取 token，使用后失效 |
| **状态机** | 状态流转操作 | 只允许特定状态转换 |
| **业务单号去重** | 支付、扣款等 | 基于 orderNo 判断是否已处理 |

---

### 方案一：唯一索引

最简单可靠的幂等方式，在数据库层面保证：

```sql
-- 订单表唯一索引
CREATE UNIQUE INDEX `uk_order_order_no` ON `order` (`order_no`);

-- 用户投票唯一索引（防止重复投票）
CREATE UNIQUE INDEX `uk_vote_user_activity` ON `vote` (`user_id`, `activity_id`);
```

```java
// ✅ 重复插入会抛出 DuplicateKeyException
@Transactional(rollbackFor = Exception.class)
public void createOrder(Order order) {
    try {
        orderMapper.insert(order);
    } catch (DuplicateKeyException e) {
        throw exception(ORDER_NO_DUPLICATE);
    }
}
```

---

### 方案二：Token 机制

防止表单重复提交：

```java
@RestController
@RequiredArgsConstructor
public class FormController {

    private final StringRedisTemplate redisTemplate;

    /**
     * 获取提交 Token
     */
    @GetMapping("/form/token")
    public CommonResult<String> getFormToken() {
        String token = UUID.randomUUID().toString();
        String key = "form:token:" + token;
        redisTemplate.opsForValue().set(key, "1", 5, TimeUnit.MINUTES);
        return CommonResult.success(token);
    }

    /**
     * 提交表单（需要 Token）
     */
    @PostMapping("/form/submit")
    public CommonResult<Void> submit(
        @RequestHeader("X-Form-Token") String token,
        @RequestBody FormData data
    ) {
        String key = "form:token:" + token;

        // 删除 Token（原子操作）
        Boolean deleted = redisTemplate.delete(key);

        if (!deleted) {
            throw exception(FORM_TOKEN_INVALID_OR_USED);
        }

        // 处理表单提交
        processFormData(data);

        return CommonResult.success();
    }
}
```

---

### 方案三：状态机

只允许特定的状态转换：

```java
/**
 * 订单状态流转
 */
public enum OrderStatus {

    PENDING_PAYMENT(1, "待支付", Arrays.asList()),
    PAID(2, "已支付", Arrays.asList(PENDING_PAYMENT)),
    SHIPPED(3, "已发货", Arrays.asList(PAID)),
    COMPLETED(4, "已完成", Arrays.asList(SHIPPED)),
    CANCELLED(5, "已取消", Arrays.asList(PENDING_PAYMENT)),
    REFUNDED(6, "已退款", Arrays.asList(PAID, SHIPPED, COMPLETED));

    private final Integer code;
    private final String desc;
    private final List<OrderStatus> allowedFrom;

    /**
     * 检查状态转换是否合法
     */
    public boolean canTransitionFrom(OrderStatus from) {
        return allowedFrom.contains(from);
    }
}

// Service 层使用
@Transactional(rollbackFor = Exception.class)
public void updateOrderStatus(Long orderId, OrderStatus newStatus) {
    Order order = orderMapper.selectById(orderId);
    if (order == null) {
        throw exception(ORDER_NOT_EXISTS);
    }

    OrderStatus currentStatus = OrderStatus.getByCode(order.getStatus());
    if (!newStatus.canTransitionFrom(currentStatus)) {
        throw exception(ORDER_STATUS_TRANSITION_NOT_ALLOWED,
            currentStatus.getDesc() + " -> " + newStatus.getDesc());
    }

    order.setStatus(newStatus.getCode());
    orderMapper.updateById(order);
}
```

---

### 方案四：业务单号去重

基于 Redis 或数据库记录处理状态：

```java
@Transactional(rollbackFor = Exception.class)
public void processPayment(String orderNo) {
    String key = "payment:processed:" + orderNo;

    // 1. 检查是否已处理
    Boolean processed = (Boolean) redisTemplate.opsForValue().get(key);
    if (Boolean.TRUE.equals(processed)) {
        log.warn("[支付]，订单已处理，orderNo: {}", orderNo);
        return;  // 幂等返回
    }

    // 2. 设置处理标记
    redisTemplate.opsForValue().set(key, true, 24, TimeUnit.HOURS);

    // 3. 执行支付逻辑
    doProcessPayment(orderNo);

    log.info("[支付]，订单处理完成，orderNo: {}", orderNo);
}
```

---

## 并发锁

### 乐观锁（推荐）

适用于冲突较少的场景：

```java
// Entity 增加版本号
@Data
@TableName("product")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private Integer stock;

    @Version
    private Integer version;  // 乐观锁版本号
}

// 更新时自动检查版本号
@Transactional(rollbackFor = Exception.class)
public void reduceStock(Long productId, Integer quantity) {
    Product product = productMapper.selectById(productId);
    if (product.getStock() < quantity) {
        throw exception(PRODUCT_STOCK_INSUFFICIENT);
    }

    product.setStock(product.getStock() - quantity);
    int rows = productMapper.updateById(product);  // MyBatis Plus 自动处理版本号

    if (rows == 0) {
        // 更新失败，版本号冲突
        throw exception(PRODUCT_STOCK_UPDATE_CONFLICT);
    }
}
```

---

### 分布式锁

使用 Redisson `RLock`：

```java
@Service
@RequiredArgsConstructor
public class ProductService {

    private final RedissonClient redissonClient;

    public void reduceStock(Long productId, Integer quantity) {
        String lockKey = "lock:product:stock:" + productId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试加锁：最多等待 5 秒，锁 10 秒后自动释放
            if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                try {
                    doReduceStock(productId, quantity);
                } finally {
                    // 只释放自己持有的锁
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } else {
                throw exception(PRODUCT_STOCK_LOCK_TIMEOUT);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw exception(PRODUCT_STOCK_LOCK_INTERRUPTED);
        }
    }

    private void doReduceStock(Long productId, Integer quantity) {
        Product product = productMapper.selectById(productId);
        if (product.getStock() < quantity) {
            throw exception(PRODUCT_STOCK_INSUFFICIENT);
        }
        product.setStock(product.getStock() - quantity);
        productMapper.updateById(product);
    }
}
```

### 分布式锁封装

```java
/**
 * 分布式锁工具类
 */
@Component
@RequiredArgsConstructor
public class DistributedLockService {

    private final RedissonClient redissonClient;

    /**
     * 执行带锁的操作
     *
     * @param lockKey 锁的 key
     * @param waitTime 等待时间
     * @param leaseTime 锁超时时间
     * @param action 要执行的操作
     */
    public <T> T executeWithLock(
        String lockKey,
        long waitTime,
        long leaseTime,
        Supplier<T> action
    ) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS)) {
                try {
                    return action.get();
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            }
            throw exception(LOCK_ACQUIRE_TIMEOUT);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw exception(LOCK_INTERRUPTED);
        }
    }

    /**
     * 无返回值的带锁操作
     */
    public void executeWithLock(
        String lockKey,
        long waitTime,
        long leaseTime,
        Runnable action
    ) {
        executeWithLock(lockKey, waitTime, leaseTime, (Supplier<Void>) () -> {
            action.run();
            return null;
        });
    }
}

// 使用示例
@Service
@RequiredArgsConstructor
public class OrderService {

    private final DistributedLockService lockService;

    public void cancelOrder(Long orderId) {
        String lockKey = "lock:order:" + orderId;

        lockService.executeWithLock(lockKey, 5, 10, () -> {
            // 执行取消订单逻辑
            doCancelOrder(orderId);
        });
    }
}
```

---

## 并发问题场景处理

### 场景一：超卖问题

```java
// ❌ 错误：先查后更新，并发不安全
public void reduceStock(Long productId, Integer quantity) {
    Product product = productMapper.selectById(productId);
    if (product.getStock() >= quantity) {
        product.setStock(product.getStock() - quantity);
        productMapper.updateById(product);  // 可能超卖
    }
}

// ✅ 正确：使用乐观锁
@Version
private Integer version;

// ✅ 正确：使用数据库原子操作
@Update("UPDATE product SET stock = stock - #{quantity} WHERE id = #{productId} AND stock >= #{quantity}")
int reduceStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

// ✅ 正确：使用分布式锁
public void reduceStock(Long productId, Integer quantity) {
    String lockKey = "lock:product:stock:" + productId;
    // ... 加锁后执行
}
```

### 场景二：缓存击穿

```java
// ❌ 错误：大量请求同时查询缓存未命中的 key
public Product getProductById(Long productId) {
    String key = "product:detail:" + productId;
    Product product = (Product) redisTemplate.opsForValue().get(key);
    if (product == null) {
        // 大量请求同时到这里
        product = productMapper.selectById(productId);
        redisTemplate.opsForValue().set(key, product, 30, TimeUnit.MINUTES);
    }
    return product;
}

// ✅ 正确：使用分布式锁
public Product getProductById(Long productId) {
    String key = "product:detail:" + productId;
    Product product = (Product) redisTemplate.opsForValue().get(key);
    if (product != null) {
        return product;
    }

    String lockKey = "lock:product:query:" + productId;
    RLock lock = redissonClient.getLock(lockKey);

    try {
        if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
            try {
                // 双重检查
                product = (Product) redisTemplate.opsForValue().get(key);
                if (product != null) {
                    return product;
                }

                product = productMapper.selectById(productId);
                if (product != null) {
                    redisTemplate.opsForValue().set(key, product, 30, TimeUnit.MINUTES);
                }
                return product;
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }

    // 降级：直接查数据库
    return productMapper.selectById(productId);
}
```

---

## 并发规范速查表

| 场景 | 方案 | 说明 |
|------|------|------|
| **防重复插入** | 唯一索引 | 数据库层面保证 |
| **防重复提交** | Token 机制 | 提交前获取 token |
| **状态流转** | 状态机 | 只允许特定转换 |
| **支付幂等** | 业务单号去重 | Redis/DB 记录处理状态 |
| **低冲突更新** | 乐观锁 | @Version 注解 |
| **高冲突更新** | 分布式锁 | Redisson RLock |
| **缓存击穿** | 分布式锁 | 只有一个线程查库 |
| **超卖问题** | 原子操作/锁 | UPDATE ... AND stock >= quantity |
