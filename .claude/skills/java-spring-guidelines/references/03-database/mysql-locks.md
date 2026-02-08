# MySQL 锁机制详解

> Java/Spring Boot 编码规范 - MySQL 锁与并发控制

---

## 一、锁的分类

### 按锁的粒度分类

| 锁类型 | 作用范围 | 性能 | 并发度 | 适用场景 |
|--------|---------|------|--------|---------|
| **表锁** | 整张表 | ⭐⭐⭐⭐⭐ | ⭐ | 全表更新 |
| **行锁** | 单行记录 | ⭐⭐ | ⭐⭐⭐⭐⭐ | 高并发 OLTP |
| **页锁** | 数据页 | ⭐⭐⭐ | ⭐⭐⭐ | 很少使用 |

### 按锁的实现方式分类

| 锁类型 | 实现方式 | 冲突检测 | 适用场景 |
|--------|---------|---------|---------|
| **乐观锁** | 版本号/CAS | 提交时检测 | 读多写少 |
| **悲观锁** | 数据库锁 | 加锁时阻塞 | 写多读少 |

---

## 二、乐观锁 vs 悲观锁

### 乐观锁（Optimistic Lock）

**核心思想**：假设不会发生冲突，只在提交时检测冲突。

#### 实现方式一：Version 字段

```sql
-- 表结构
CREATE TABLE t_product (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100),
    stock INT,
    version INT DEFAULT 0,  -- 版本号
    INDEX idx_version(version)
);
```

```java
/**
 * 实体类
 */
@Data
@TableName("t_product")
public class ProductDO {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String name;
    
    private Integer stock;
    
    @Version  // MyBatis-Plus 乐观锁注解
    private Integer version;
}

/**
 * 乐观锁配置（MyBatis-Plus）
 */
@Configuration
public class MybatisPlusConfig {
    
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 添加乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }
}

/**
 * Service 层使用
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements IProductService {
    
    private final ProductMapper productMapper;
    
    /**
     * 扣减库存（乐观锁，失败重试）
     */
    @Override
    public void deductStock(Long productId, Integer quantity) {
        int maxRetries = 3;
        
        for (int i = 0; i < maxRetries; i++) {
            // 1. 查询商品
            ProductDO product = productMapper.selectById(productId);
            
            if (product == null) {
                throw exception(ErrorCode.PRODUCT_NOT_FOUND);
            }
            
            // 2. 检查库存
            if (product.getStock() < quantity) {
                throw exception(ErrorCode.STOCK_NOT_ENOUGH);
            }
            
            // 3. 扣减库存（version 自动 +1）
            product.setStock(product.getStock() - quantity);
            int updated = productMapper.updateById(product);
            
            // 4. 更新成功
            if (updated > 0) {
                log.info("[库存扣减] 成功，productId: {}, quantity: {}, 剩余: {}", 
                         productId, quantity, product.getStock() - quantity);
                return;
            }
            
            // 5. 失败重试
            log.warn("[库存扣减] 乐观锁冲突，第 {} 次重试", i + 1);
            
            // 随机休眠 10-50ms
            try {
                Thread.sleep(10 + new Random().nextInt(40));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        throw exception(ErrorCode.STOCK_DEDUCT_FAILED, "库存扣减失败，请重试");
    }
}
```

**底层 SQL（MyBatis-Plus 自动生成）：**

```sql
-- 查询
SELECT id, name, stock, version FROM t_product WHERE id = 1;

-- 更新（WHERE 条件自动加上 version）
UPDATE t_product 
SET stock = 99, version = version + 1 
WHERE id = 1 AND version = 0;

-- 如果 version 不匹配，affected rows = 0
```

#### 实现方式二：CAS（Compare And Swap）

```sql
-- 不需要 version 字段
UPDATE t_product 
SET stock = stock - 1 
WHERE id = 1 AND stock >= 1;
```

```java
/**
 * CAS 方式扣减库存
 */
@Override
public void deductStockByCAS(Long productId, Integer quantity) {
    int updated = productMapper.deductStock(productId, quantity);
    
    if (updated == 0) {
        throw exception(ErrorCode.STOCK_NOT_ENOUGH);
    }
}
```

```xml
<!-- Mapper XML -->
<update id="deductStock">
    UPDATE t_product 
    SET stock = stock - #{quantity}
    WHERE id = #{productId} 
      AND stock >= #{quantity}
</update>
```

### 悲观锁（Pessimistic Lock）

**核心思想**：假设会发生冲突，先加锁再操作。

#### 共享锁（S 锁，读锁）

**语法**：`SELECT ... LOCK IN SHARE MODE`（MySQL 8.0 推荐 `FOR SHARE`）

```sql
-- 加共享锁
SELECT * FROM t_product WHERE id = 1 LOCK IN SHARE MODE;
-- 或者（MySQL 8.0+）
SELECT * FROM t_product WHERE id = 1 FOR SHARE;
```

**特点**：
- 多个事务可以同时持有 S 锁（读读不互斥）
- S 锁与 X 锁互斥（读写互斥）

```java
/**
 * 共享锁示例
 */
@Override
@Transactional(rollbackFor = Exception.class)
public ProductDTO getProductWithLock(Long productId) {
    // 加共享锁（其他事务可以读，但不能写）
    ProductDO product = productMapper.selectByIdWithShareLock(productId);
    
    // 业务处理...
    
    return convertToDTO(product);
}
```

```xml
<select id="selectByIdWithShareLock" resultType="ProductDO">
    SELECT * FROM t_product WHERE id = #{id} FOR SHARE
</select>
```

#### 排他锁（X 锁，写锁）

**语法**：`SELECT ... FOR UPDATE`

```sql
-- 加排他锁
SELECT * FROM t_product WHERE id = 1 FOR UPDATE;
```

**特点**：
- X 锁与任何锁都互斥（读写都阻塞）
- 其他事务必须等待锁释放

```java
/**
 * 排他锁扣减库存
 */
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements IProductService {
    
    private final ProductMapper productMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductStockWithLock(Long productId, Integer quantity) {
        // 1. 加排他锁查询
        ProductDO product = productMapper.selectByIdForUpdate(productId);
        
        if (product == null) {
            throw exception(ErrorCode.PRODUCT_NOT_FOUND);
        }
        
        // 2. 检查库存
        if (product.getStock() < quantity) {
            throw exception(ErrorCode.STOCK_NOT_ENOUGH);
        }
        
        // 3. 扣减库存
        product.setStock(product.getStock() - quantity);
        productMapper.updateById(product);
        
        // 4. 事务提交时自动释放锁
    }
}
```

```xml
<!-- Mapper XML -->
<select id="selectByIdForUpdate" resultType="ProductDO">
    SELECT * FROM t_product WHERE id = #{id} FOR UPDATE
</select>
```

### 乐观锁 vs 悲观锁对比

| 维度 | 乐观锁 | 悲观锁 |
|------|--------|--------|
| **冲突假设** | 假设不会冲突 | 假设会冲突 |
| **锁定时机** | 提交时检测 | 读取时加锁 |
| **性能** | 冲突少时性能好 | 冲突多时避免重试 |
| **实现方式** | Version / CAS | FOR UPDATE |
| **失败处理** | 需要重试 | 等待锁释放 |
| **适用场景** | 读多写少 | 写多读少 |
| **并发度** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| **死锁风险** | 无 | 有 |

**【推荐】选择原则：**

```java
// ✅ 读多写少 → 乐观锁
// 示例：商品详情、用户信息

// ✅ 写多读少 → 悲观锁
// 示例：秒杀库存、账户余额

// ✅ 冲突率低 → 乐观锁
// ✅ 冲突率高 → 悲观锁
```

---

## 三、InnoDB 行锁机制

### 行锁类型

| 锁类型 | 英文名 | 作用 |
|--------|--------|------|
| **记录锁** | Record Lock | 锁定单行记录 |
| **间隙锁** | Gap Lock | 锁定索引之间的间隙 |
| **临键锁** | Next-Key Lock | 记录锁 + 间隙锁 |

### 记录锁（Record Lock）

```sql
-- 精确匹配唯一索引
SELECT * FROM t_product WHERE id = 1 FOR UPDATE;

-- 只锁定 id=1 这一行
```

### 间隙锁（Gap Lock）

**作用**：防止幻读（Phantom Read）

```sql
-- 假设表中有 id: 1, 5, 10
SELECT * FROM t_product WHERE id BETWEEN 3 AND 8 FOR UPDATE;

-- 锁定范围：(1, 10)
-- 其他事务不能插入 id=3,4,6,7,8,9 的记录
```

### 临键锁（Next-Key Lock）

**默认锁类型**：Record Lock + Gap Lock

```sql
-- 假设表中有 id: 1, 5, 10
SELECT * FROM t_product WHERE id > 3 FOR UPDATE;

-- 锁定：
-- - 记录锁：id=5, id=10
-- - 间隙锁：(3, 5), (5, 10), (10, +∞)
```

---

## 四、死锁问题

### 什么是死锁？

**定义**：两个或多个事务互相等待对方释放锁，形成循环等待。

```sql
-- 事务 A
START TRANSACTION;
UPDATE t_product SET stock = stock - 1 WHERE id = 1;  -- 锁定 id=1
-- 等待...
UPDATE t_product SET stock = stock - 1 WHERE id = 2;  -- 等待 id=2 的锁
COMMIT;

-- 事务 B
START TRANSACTION;
UPDATE t_product SET stock = stock - 1 WHERE id = 2;  -- 锁定 id=2
-- 等待...
UPDATE t_product SET stock = stock - 1 WHERE id = 1;  -- 等待 id=1 的锁
COMMIT;

-- 结果：死锁！
```

### 查看死锁日志

```sql
-- 查看最近一次死锁
SHOW ENGINE INNODB STATUS\G

-- 关键信息：
-- *** (1) TRANSACTION
-- *** (2) TRANSACTION
-- *** WE ROLL BACK TRANSACTION (1)
```

### 死锁解决方案

#### 1. 统一加锁顺序

```java
// ❌ 错误：加锁顺序不一致
@Transactional
public void transferMoney(Long fromId, Long toId, BigDecimal amount) {
    Account from = accountMapper.selectByIdForUpdate(fromId);
    Account to = accountMapper.selectByIdForUpdate(toId);
    // 可能死锁
}

// ✅ 正确：按 ID 大小排序加锁
@Transactional
public void transferMoney(Long fromId, Long toId, BigDecimal amount) {
    Long minId = Math.min(fromId, toId);
    Long maxId = Math.max(fromId, toId);
    
    Account account1 = accountMapper.selectByIdForUpdate(minId);
    Account account2 = accountMapper.selectByIdForUpdate(maxId);
    
    // 确定转出转入
    Account from = account1.getId().equals(fromId) ? account1 : account2;
    Account to = account1.getId().equals(toId) ? account1 : account2;
    
    // 转账逻辑
    from.setBalance(from.getBalance().subtract(amount));
    to.setBalance(to.getBalance().add(amount));
    
    accountMapper.updateById(from);
    accountMapper.updateById(to);
}
```

#### 2. 缩小锁范围

```java
// ❌ 错误：锁范围过大
@Transactional
public void processOrder(Long orderId) {
    OrderDO order = orderMapper.selectByIdForUpdate(orderId);
    
    // 长时间业务处理（网络调用、复杂计算）
    callPaymentService(order);
    callInventoryService(order);
    
    orderMapper.updateById(order);
}

// ✅ 正确：缩小锁范围
@Transactional
public void processOrder(Long orderId) {
    // 先不加锁查询
    OrderDO order = orderMapper.selectById(orderId);
    
    // 业务处理
    callPaymentService(order);
    callInventoryService(order);
    
    // 最后加锁更新
    OrderDO lockedOrder = orderMapper.selectByIdForUpdate(orderId);
    lockedOrder.setStatus(order.getStatus());
    orderMapper.updateById(lockedOrder);
}
```

#### 3. 设置锁等待超时

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/db?innodb_lock_wait_timeout=5
```

```sql
-- 全局设置
SET GLOBAL innodb_lock_wait_timeout = 5;

-- 会话级别设置
SET SESSION innodb_lock_wait_timeout = 5;
```

#### 4. 使用乐观锁替代悲观锁

```java
// ✅ 推荐：高并发场景使用乐观锁
@Override
public void deductStock(Long productId, Integer quantity) {
    for (int i = 0; i < 3; i++) {
        ProductDO product = productMapper.selectById(productId);
        product.setStock(product.getStock() - quantity);
        
        int updated = productMapper.updateById(product);  // version 自动 +1
        if (updated > 0) {
            return;
        }
    }
    throw exception(ErrorCode.STOCK_DEDUCT_FAILED);
}
```

#### 5. 死锁监控与告警

```java
/**
 * 死锁监控
 */
@Component
@Slf4j
public class DeadlockMonitor {
    
    @Scheduled(fixedRate = 60000)  // 每分钟检查一次
    public void checkDeadlock() {
        try {
            // 执行 SHOW ENGINE INNODB STATUS
            String status = jdbcTemplate.queryForObject(
                "SHOW ENGINE INNODB STATUS", String.class
            );
            
            // 解析死锁信息
            if (status.contains("LATEST DETECTED DEADLOCK")) {
                log.error("[死锁告警] 检测到死锁: {}", extractDeadlockInfo(status));
                // 发送告警
                sendAlert("检测到数据库死锁");
            }
            
        } catch (Exception e) {
            log.error("[死锁监控] 异常", e);
        }
    }
}
```

---

## 五、锁使用最佳实践

### 1. 尽量使用索引

```java
// ❌ 错误：无索引，升级为表锁
SELECT * FROM t_product WHERE name = '商品A' FOR UPDATE;

// ✅ 正确：有索引，使用行锁
SELECT * FROM t_product WHERE id = 1 FOR UPDATE;
```

### 2. 避免长事务

```java
// ❌ 错误：事务时间过长
@Transactional
public void processOrder(Long orderId) {
    OrderDO order = orderMapper.selectByIdForUpdate(orderId);
    
    // ❌ RPC 调用
    paymentClient.pay(order);
    
    // ❌ 复杂计算
    complexCalculation();
    
    orderMapper.updateById(order);
}

// ✅ 正确：只在必要时加锁
public void processOrder(Long orderId) {
    OrderDO order = orderMapper.selectById(orderId);
    
    // 业务处理
    paymentClient.pay(order);
    complexCalculation();
    
    // 最后加锁更新
    updateOrderWithLock(orderId, order.getStatus());
}

@Transactional
public void updateOrderWithLock(Long orderId, Integer status) {
    OrderDO order = orderMapper.selectByIdForUpdate(orderId);
    order.setStatus(status);
    orderMapper.updateById(order);
}
```

### 3. 合理设置隔离级别

```yaml
# application.yml
spring:
  datasource:
    hikari:
      transaction-isolation: TRANSACTION_READ_COMMITTED  # RC 级别（推荐）
```

| 隔离级别 | 幻读 | 间隙锁 | 性能 | 推荐场景 |
|---------|------|--------|------|---------|
| **READ COMMITTED** | ✅ 可能 | ❌ 无 | ⭐⭐⭐⭐⭐ | 高并发 OLTP |
| **REPEATABLE READ** | ❌ 避免 | ✅ 有 | ⭐⭐⭐ | 默认级别 |

### 4. 监控锁等待

```sql
-- 查看当前锁等待
SELECT * FROM information_schema.INNODB_LOCKS;

-- 查看锁等待关系
SELECT * FROM information_schema.INNODB_LOCK_WAITS;

-- 查看事务
SELECT * FROM information_schema.INNODB_TRX;
```

---

## 六、锁问题排查

### 问题 1：锁等待超时

```
ERROR 1205 (HY000): Lock wait timeout exceeded; try restarting transaction
```

**排查步骤：**

```sql
-- 1. 查看锁等待
SELECT 
    r.trx_id AS waiting_trx_id,
    r.trx_mysql_thread_id AS waiting_thread,
    r.trx_query AS waiting_query,
    b.trx_id AS blocking_trx_id,
    b.trx_mysql_thread_id AS blocking_thread,
    b.trx_query AS blocking_query
FROM information_schema.innodb_lock_waits w
JOIN information_schema.innodb_trx b ON b.trx_id = w.blocking_trx_id
JOIN information_schema.innodb_trx r ON r.trx_id = w.requesting_trx_id;

-- 2. 杀掉阻塞线程
KILL <blocking_thread>;
```

### 问题 2：死锁频繁

**排查步骤：**

1. 查看死锁日志：`SHOW ENGINE INNODB STATUS`
2. 分析加锁顺序
3. 优化业务逻辑
4. 考虑使用乐观锁

---

## 七、速查表

### 锁类型选择

| 场景 | 推荐锁类型 | 原因 |
|------|-----------|------|
| 读多写少 | 乐观锁 | 避免锁等待 |
| 写多读少 | 悲观锁 | 避免重试 |
| 秒杀场景 | 悲观锁 + Redis | 保证准确性 |
| 账户转账 | 悲观锁 | 强一致性 |
| 商品浏览 | 无锁 | 性能优先 |
| 订单查询 | 无锁 | 读操作 |

### SQL 锁语法

| 操作 | SQL 语法 |
|------|---------|
| 共享锁 | `SELECT ... FOR SHARE` |
| 排他锁 | `SELECT ... FOR UPDATE` |
| 跳过锁等待 | `SELECT ... FOR UPDATE NOWAIT` |
| 等待指定时间 | `SELECT ... FOR UPDATE WAIT 3` |

### 乐观锁 vs 悲观锁

| 维度 | 乐观锁 | 悲观锁 |
|------|--------|--------|
| **实现** | Version 字段 | FOR UPDATE |
| **性能** | 冲突少时好 | 冲突多时好 |
| **死锁** | 无风险 | 有风险 |
| **重试** | 需要 | 不需要 |
| **推荐场景** | 读多写少 | 写多读少 |
