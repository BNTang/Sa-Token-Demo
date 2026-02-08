# 事务管理

> Java/Spring Boot 编码规范 - 事务控制

---

## 基本规则

| 规则 | 说明 |
|------|------|
| 多表操作 | 必须加 `@Transactional(rollbackFor = Exception.class)` |
| 事务方法 | 必须是 `public`（private 不生效） |
| 同类调用 | 避免同类方法内部调用（代理失效） |
| 长事务 | 拆分长事务，避免锁表时间过长 |
| 多数据源 | 事务方法中禁止混用多个数据源 |

---

## 标准事务注解

```java
// ✅ 正确：指定 rollbackFor
@Transactional(rollbackFor = Exception.class)
public void createOrder(Order order) {
    orderMapper.insert(order);
    orderItemMapper.insertBatch(order.getItems());
}

// ⚠️ 注意：默认只回滚 RuntimeException
@Transactional
public void method() { }

// ❌ 错误：只回滚 Error
@Transactional(rollbackFor = Error.class)
public void method() { }
```

---

## 事务失效场景

### 场景一：方法不是 public

```java
// ❌ 错误：private 方法事务不生效
@Transactional
private void createOrder(Order order) {
    // 事务不生效
}

// ✅ 正确：使用 public 方法
@Transactional
public void createOrder(Order order) {
    // 事务生效
}
```

### 场景二：同类内部调用

```java
// ❌ 错误：同类调用，事务不生效
@Service
public class OrderService {

    public void processOrder(Order order) {
        this.createOrder(order);      // createOrder 的 @Transactional 不生效
        this.updateInventory(order);  // updateInventory 的 @Transactional 不生效
    }

    @Transactional
    public void createOrder(Order order) { }

    @Transactional
    public void updateInventory(Order order) { }
}

// ✅ 解决方案1：注入自身
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderService self;  // Spring 4.3+ 支持自注入

    public void processOrder(Order order) {
        self.createOrder(order);      // 通过代理调用，事务生效
        self.updateInventory(order);
    }

    @Transactional
    public void createOrder(Order order) { }

    @Transactional
    public void updateInventory(Order order) { }
}

// ✅ 解决方案2：拆分到另一个 Service
@Service
@RequiredArgsConstructor
public class OrderService {

    private final InventoryService inventoryService;

    @Transactional
    public void processOrder(Order order) {
        createOrder(order);
        inventoryService.updateInventory(order);  // 跨服务调用，事务生效
    }

    public void createOrder(Order order) { }
}

@Service
public class InventoryService {

    @Transactional
    public void updateInventory(Order order) { }
}

// ✅ 解决方案3：使用 ApplicationContext 获取代理对象
@Service
public class OrderService implements ApplicationContextAware {

    private OrderService self;

    @Override
    public void setApplicationContext(ApplicationContext ctx) {
        self = ctx.getBean(OrderService.class);
    }

    public void processOrder(Order order) {
        self.createOrder(order);  // 通过代理调用
    }

    @Transactional
    public void createOrder(Order order) { }
}
```

### 场景三：异常被捕获

```java
// ❌ 错误：异常被捕获，事务不回滚
@Transactional(rollbackFor = Exception.class)
public void createOrder(Order order) {
    try {
        orderMapper.insert(order);
    } catch (Exception e) {
        log.error("插入失败", e);
        // 异常被捕获，事务不回滚
    }
}

// ✅ 正确：抛出异常或手动回滚
@Transactional(rollbackFor = Exception.class)
public void createOrder(Order order) {
    try {
        orderMapper.insert(order);
    } catch (Exception e) {
        log.error("插入失败", e);
        throw exception(ORDER_CREATE_FAILED);  // 抛出异常
    }
}

// ✅ 正确：手动回滚
@Transactional(rollbackFor = Exception.class)
public void createOrder(Order order) {
    try {
        orderMapper.insert(order);
    } catch (Exception e) {
        log.error("插入失败", e);
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        throw exception(ORDER_CREATE_FAILED);
    }
}
```

### 场景四：多数据源混用

```java
// ❌ 错误：事务中混用 MySQL 和 Doris
@Transactional(rollbackFor = Exception.class)
public void syncData() {
    // Doris 查询不在事务管理范围内
    List<Data> dorisData = dorisMapper.selectList();

    // MySQL 写入在事务中
    // 如果 MySQL 回滚，Doris 的数据无法回滚
    mysqlMapper.saveBatch(dorisData);
}

// ✅ 正确：拆分方法
public void syncData() {
    // 1. 非事务方法查询 Doris
    List<Data> dorisData = queryFromDoris();

    // 2. 事务方法写入 MySQL
    saveToMysql(dorisData);
}

public List<Data> queryFromDoris() {
    return dorisMapper.selectList();
}

@Transactional(rollbackFor = Exception.class)
public void saveToMysql(List<Data> data) {
    mysqlMapper.saveBatch(data);
}
```

---

## 事务传播行为

```java
// REQUIRED（默认）：加入现有事务，无则创建
@Transactional(propagation = Propagation.REQUIRED)
public void methodA() {
    methodB();  // methodB 会加入 methodA 的事务
}

@Transactional(propagation = Propagation.REQUIRED)
public void methodB() { }

// REQUIRES_NEW：创建新事务，挂起当前事务
@Transactional(propagation = Propagation.REQUIRED)
public void methodA() {
    methodB();  // methodB 会创建新事务，独立提交/回滚
}

@Transactional(propagation = Propagation.REQUIRES_NEW)
public void methodB() { }

// NESTED：嵌套事务，回滚点
@Transactional(propagation = Propagation.REQUIRED)
public void methodA() {
    try {
        methodB();  // methodB 是嵌套事务，失败可单独回滚
    } catch (Exception e) {
        // methodB 回滚，methodA 不受影响
    }
}

@Transactional(propagation = Propagation.NESTED)
public void methodB() { }
```

---

## 事务隔离级别

### 隔离级别概述

**事务的 ACID 特性：**

- **A (Atomicity)** 原子性：事务要么全部成功，要么全部失败
- **C (Consistency)** 一致性：事务前后数据保持一致
- **I (Isolation)** 隔离性：多个事务之间互不干扰
- **D (Durability)** 持久性：事务提交后永久保存

**隔离级别解决的问题：**

| 问题 | 说明 | 场景示例 |
|------|------|---------|
| **脏读** | 读到其他事务未提交的数据 | 事务A修改但未提交，事务B读到了 |
| **不可重复读** | 同一事务内多次读取结果不同 | 事务A两次读取之间，事务B修改并提交 |
| **幻读** | 同一事务内多次查询记录数不同 | 事务A两次查询之间，事务B插入新记录 |

### 四种隔离级别

| 隔离级别 | 脏读 | 不可重复读 | 幻读 | 性能 | 适用场景 |
|---------|------|-----------|------|------|---------|
| **READ_UNCOMMITTED** | ❌ 可能 | ❌ 可能 | ❌ 可能 | 🟢 最高 | 几乎不用 |
| **READ_COMMITTED** | ✅ 避免 | ❌ 可能 | ❌ 可能 | 🟡 较高 | Oracle 默认 |
| **REPEATABLE_READ** | ✅ 避免 | ✅ 避免 | ❌ 可能 | 🟡 中等 | MySQL 默认 |
| **SERIALIZABLE** | ✅ 避免 | ✅ 避免 | ✅ 避免 | 🔴 最低 | 严格场景 |

### 脏读（Dirty Read）

**问题描述：**事务 A 读到了事务 B **未提交**的数据，如果事务 B 回滚，事务 A 读到的就是脏数据。

**示例场景：**

```sql
-- 时间线：事务 A 和事务 B 并发执行

-- 事务 A（查询余额）
START TRANSACTION;
-- T1: 查询余额
SELECT balance FROM account WHERE id = 1;  
-- 结果：balance = 1000

-- 事务 B（转账）
START TRANSACTION;
-- T2: 修改余额但未提交
UPDATE account SET balance = 500 WHERE id = 1;  

-- 事务 A（再次查询）
-- T3: 再次查询余额（READ_UNCOMMITTED 隔离级别）
SELECT balance FROM account WHERE id = 1;  
-- 结果：balance = 500  ← 读到了事务 B 未提交的数据（脏读）

-- 事务 B（回滚）
-- T4: 回滚事务
ROLLBACK;

-- 问题：事务 A 读到的 500 是脏数据！实际余额是 1000
```

**解决方案：**使用 **READ_COMMITTED** 或更高隔离级别。

### 不可重复读（Non-Repeatable Read）

**问题描述：**事务 A 在同一事务内多次读取同一数据，但每次读取的结果不同（其他事务修改并提交了数据）。

**示例场景：**

```sql
-- 事务 A（统计报表）
START TRANSACTION;
-- T1: 第一次查询
SELECT balance FROM account WHERE id = 1;  
-- 结果：balance = 1000

-- 事务 B（转账）
START TRANSACTION;
-- T2: 修改余额并提交
UPDATE account SET balance = 500 WHERE id = 1;
COMMIT;

-- 事务 A（再次查询）
-- T3: 第二次查询（READ_COMMITTED 隔离级别）
SELECT balance FROM account WHERE id = 1;  
-- 结果：balance = 500  ← 读到了事务 B 已提交的数据

-- 问题：同一事务内两次读取结果不同（不可重复读）
COMMIT;
```

**解决方案：**使用 **REPEATABLE_READ** 或更高隔离级别。

### 幻读（Phantom Read）

**问题描述：**事务 A 在同一事务内多次查询，发现查询结果的**记录数量**不同（其他事务插入或删除了数据）。

**示例场景：**

```sql
-- 事务 A（统计订单）
START TRANSACTION;
-- T1: 第一次查询订单数量
SELECT COUNT(*) FROM `order` WHERE user_id = 1;  
-- 结果：count = 10

-- 事务 B（创建订单）
START TRANSACTION;
-- T2: 插入新订单并提交
INSERT INTO `order` (user_id, amount) VALUES (1, 100);
COMMIT;

-- 事务 A（再次查询）
-- T3: 第二次查询订单数量（REPEATABLE_READ 隔离级别）
SELECT COUNT(*) FROM `order` WHERE user_id = 1;  
-- 结果：count = 11  ← 读到了事务 B 插入的新记录

-- 问题：同一事务内两次查询记录数不同（幻读）
COMMIT;
```

**MySQL InnoDB 的幻读解决：**

> MySQL InnoDB 在 REPEATABLE_READ 隔离级别下，通过 **MVCC（多版本并发控制）+ Next-Key Lock** 机制，基本解决了幻读问题。

**解决方案：**
- 使用 **SERIALIZABLE** 隔离级别（性能差）
- MySQL InnoDB 默认 **REPEATABLE_READ** + MVCC 已基本解决

### Spring 隔离级别配置

```java
// ✅ 使用数据库默认隔离级别（推荐）
@Transactional(isolation = Isolation.DEFAULT)
public void createOrder() { }

// 读未提交（几乎不用，允许脏读）
@Transactional(isolation = Isolation.READ_UNCOMMITTED)
public void queryData() { }

// 读已提交（Oracle 默认，避免脏读）
@Transactional(isolation = Isolation.READ_COMMITTED)
public void queryData() { }

// 可重复读（MySQL 默认，避免脏读和不可重复读）
@Transactional(isolation = Isolation.REPEATABLE_READ)
public void queryData() { }

// 串行化（最高隔离，性能最差）
@Transactional(isolation = Isolation.SERIALIZABLE)
public void criticalOperation() { }
```

### 隔离级别选择建议

```java
// ✅ 推荐：使用数据库默认隔离级别
@Transactional(rollbackFor = Exception.class)  // 不指定 isolation
public void normalOperation() {
    // MySQL: REPEATABLE_READ
    // Oracle: READ_COMMITTED
}

// ❌ 不推荐：随意改变隔离级别
@Transactional(isolation = Isolation.READ_UNCOMMITTED)  // 可能脏读
public void queryData() { }

// ✅ 特殊场景：报表统计需要严格一致性
@Transactional(isolation = Isolation.SERIALIZABLE, readOnly = true)
public BigDecimal calculateTotalAmount() {
    // 完全串行化，避免幻读
}
```

### MySQL InnoDB MVCC 机制

**MVCC（Multi-Version Concurrency Control）：**

> InnoDB 通过保存数据的多个版本，实现高并发下的读写不阻塞。

**工作原理：**

```sql
-- 表结构（隐藏字段）
CREATE TABLE `account` (
    `id` bigint,
    `balance` decimal(10,2),
    `DB_TRX_ID` bigint,    -- 隐藏字段：事务ID
    `DB_ROLL_PTR` bigint   -- 隐藏字段：回滚指针
);

-- 事务执行过程
-- T1: 事务1插入数据
INSERT INTO account VALUES (1, 1000);
-- 记录版本：balance=1000, TRX_ID=100

-- T2: 事务2更新数据
UPDATE account SET balance = 500 WHERE id = 1;
-- 新版本：balance=500, TRX_ID=101
-- 旧版本：balance=1000, TRX_ID=100（保留在 undo log）

-- T3: 事务3查询（REPEATABLE_READ）
SELECT balance FROM account WHERE id = 1;
-- 如果事务3开始于事务2之前，读取旧版本 balance=1000
-- 如果事务3开始于事务2之后，读取新版本 balance=500
```

---

## 长事务拆分

```java
// ❌ 错误：长事务持有锁时间过长
@Transactional(rollbackFor = Exception.class)
public void processBatchOrders(List<Long> orderIds) {
    for (Long orderId : orderIds) {
        Order order = orderMapper.selectById(orderId);
        // 复杂业务处理
        processOrder(order);
        orderMapper.updateById(order);
        // 调用外部接口
        externalApi.notify(order);
    }
}

// ✅ 正确：拆分事务
public void processBatchOrders(List<Long> orderIds) {
    for (Long orderId : orderIds) {
        processSingleOrder(orderId);  // 每个订单独立事务
    }
}

@Transactional(rollbackFor = Exception.class)
public void processSingleOrder(Long orderId) {
    Order order = orderMapper.selectById(orderId);
    processOrder(order);
    orderMapper.updateById(order);
    // 外部调用移到事务外
}

// 外部调用异步执行
@Async("asyncExecutor")
public void notifyExternal(Order order) {
    externalApi.notify(order);
}
```

---

## 编程式事务

复杂场景可使用编程式事务：

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final TransactionTemplate transactionTemplate;

    public void complexProcess() {
        // 事务1：创建订单
        Order order = transactionTemplate.execute(status -> {
            Order newOrder = createOrder();
            return newOrder;
        });

        // 非事务：外部调用
        externalApi.validate(order);

        // 事务2：扣减库存
        transactionTemplate.executeWithoutResult(status -> {
            reduceStock(order);
        });

        // 事务3：创建支付
        transactionTemplate.executeWithoutResult(status -> {
            createPayment(order);
        });
    }
}
```

---

## 事务规范速查表

| 规范 | 要点 |
|------|------|
| **注解** | `@Transactional(rollbackFor = Exception.class)` |
| **方法可见性** | 必须是 public |
| **同类调用** | 注入 self 或拆分 Service |
| **异常处理** | 不要捕获后不抛出 |
| **多数据源** | 事务中禁止混用 |
| **长事务** | 拆分为多个短事务 |
| **外部调用** | 移到事务外部 |
