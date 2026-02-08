# MySQL 索引优化规范

> Java/Spring Boot 编码规范 - MySQL 索引优化与查询性能
> 参考：阿里巴巴 Java 开发手册、MySQL 最佳实践

---

## 索引基础

### 索引类型选择

**【推荐】优先使用 InnoDB 存储引擎，支持事务和行级锁。**

| 存储引擎 | 事务 | 锁粒度 | 适用场景 |
|---------|------|--------|---------|
| **InnoDB** | ✅ | 行级锁 | OLTP，高并发读写 |
| MyISAM | ❌ | 表级锁 | OLAP，读多写少（已淘汰）|
| Memory | ❌ | 表级锁 | 临时表，缓存 |

```sql
-- ✅ 正例 - 使用 InnoDB
CREATE TABLE `order` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `order_no` varchar(64) NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ❌ 反例 - 不要使用 MyISAM
CREATE TABLE `order` (...) ENGINE=MyISAM;
```

---

## 索引设计规范

### 主键索引

**【强制】表必须有主键，优先使用 bigint 自增主键。**

```sql
-- ✅ 正例 - bigint 自增主键
CREATE TABLE `user` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB;

-- ❌ 反例 - 使用 UUID 作为主键（导致页分裂）
CREATE TABLE `user` (
    `id` varchar(36) NOT NULL,
    PRIMARY KEY (`id`)
);

-- ❌ 反例 - 使用业务字段作为主键（不灵活）
CREATE TABLE `user` (
    `username` varchar(50) NOT NULL,
    PRIMARY KEY (`username`)
);
```

> 说明：
> - 自增主键保证 B+树顺序插入，避免页分裂
> - UUID 随机性导致频繁页分裂，性能差
> - 业务字段作主键，后续变更困难

### 唯一索引

**【推荐】业务唯一字段建立唯一索引，提高查询性能并防止重复。**

```sql
-- ✅ 正例 - 唯一索引
CREATE TABLE `user` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `username` varchar(50) NOT NULL,
    `mobile` varchar(20) NOT NULL,
    `email` varchar(100),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_mobile` (`mobile`)
) ENGINE=InnoDB;
```

### 联合索引

**【推荐】遵循最左前缀原则设计联合索引。**

```sql
-- 索引：idx_user_status_time (user_id, status, create_time)

-- ✅ 能使用索引
SELECT * FROM order WHERE user_id = 1;
SELECT * FROM order WHERE user_id = 1 AND status = 1;
SELECT * FROM order WHERE user_id = 1 AND status = 1 AND create_time > '2024-01-01';

-- ❌ 不能使用索引（跳过了 user_id）
SELECT * FROM order WHERE status = 1;
SELECT * FROM order WHERE status = 1 AND create_time > '2024-01-01';
```

**【推荐】区分度高的字段放在联合索引左侧。**

```sql
-- ❌ 反例 - status 区分度低（只有 0/1），放在左侧
CREATE INDEX idx_status_user ON order (status, user_id);

-- ✅ 正例 - user_id 区分度高，放在左侧
CREATE INDEX idx_user_status ON order (user_id, status);
```

---

## 索引失效场景

### 函数/计算导致索引失效

**【强制】WHERE 条件不要对索引字段使用函数或计算。**

```sql
-- ❌ 反例 - 索引失效
SELECT * FROM user WHERE YEAR(create_time) = 2024;
SELECT * FROM user WHERE age + 1 = 30;
SELECT * FROM user WHERE CONCAT(first_name, last_name) = 'John Doe';

-- ✅ 正例 - 索引有效
SELECT * FROM user WHERE create_time >= '2024-01-01' AND create_time < '2025-01-01';
SELECT * FROM user WHERE age = 29;
SELECT * FROM user WHERE first_name = 'John' AND last_name = 'Doe';
```

### 隐式类型转换导致索引失效

**【强制】WHERE 条件避免隐式类型转换。**

```sql
-- 假设 mobile 字段是 varchar(20)

-- ❌ 反例 - 数字类型导致索引失效
SELECT * FROM user WHERE mobile = 13800138000;

-- ✅ 正例 - 使用字符串
SELECT * FROM user WHERE mobile = '13800138000';

-- 假设 status 字段是 tinyint

-- ❌ 反例 - 字符串导致索引失效
SELECT * FROM order WHERE status = '1';

-- ✅ 正例 - 使用数字
SELECT * FROM order WHERE status = 1;
```

> 规则：字符串字段用数字查询会失效，数字字段用字符串查询也会失效

### LIKE 模糊查询索引失效

**【推荐】LIKE 查询避免左侧通配符。**

```sql
-- ❌ 反例 - 左侧 % 导致索引失效
SELECT * FROM user WHERE username LIKE '%john%';
SELECT * FROM user WHERE username LIKE '%john';

-- ✅ 正例 - 右侧 % 可以使用索引
SELECT * FROM user WHERE username LIKE 'john%';

-- ✅ 如果必须模糊搜索，使用全文索引
CREATE FULLTEXT INDEX idx_fulltext_name ON user(username);
SELECT * FROM user WHERE MATCH(username) AGAINST('john');
```

### OR 条件索引失效

**【推荐】OR 条件两侧字段都要有索引，否则索引失效。**

```sql
-- 假设有索引 idx_username 和 idx_mobile

-- ✅ 正例 - 两侧都有索引，走索引合并
SELECT * FROM user WHERE username = 'john' OR mobile = '13800138000';

-- ❌ 反例 - email 没有索引，导致全表扫描
SELECT * FROM user WHERE username = 'john' OR email = 'john@example.com';

-- ✅ 正例 - 改用 UNION
SELECT * FROM user WHERE username = 'john'
UNION
SELECT * FROM user WHERE email = 'john@example.com';
```

### NULL 判断索引失效

**【推荐】尽量设置字段 NOT NULL，使用默认值。**

```sql
-- ❌ 反例 - IS NULL / IS NOT NULL 可能导致索引失效
SELECT * FROM user WHERE mobile IS NULL;
SELECT * FROM user WHERE mobile IS NOT NULL;

-- ✅ 正例 - 设计时避免 NULL
CREATE TABLE `user` (
    `mobile` varchar(20) NOT NULL DEFAULT '' COMMENT '手机号',
    `remark` varchar(500) NOT NULL DEFAULT '' COMMENT '备注'
);

-- 查询空值使用 = ''
SELECT * FROM user WHERE mobile = '';
```

### 不等于操作索引失效

**【推荐】避免使用 != 或 <> 操作符。**

```sql
-- ❌ 反例 - != 可能导致索引失效
SELECT * FROM order WHERE status != 0;

-- ✅ 正例 - 使用 IN 或范围查询
SELECT * FROM order WHERE status IN (1, 2, 3, 4);
SELECT * FROM order WHERE status > 0;
```

---

## 查询优化规范

### SELECT 字段优化

**【强制】禁止使用 SELECT *，明确指定需要的字段。**

```java
// ❌ 反例 - SELECT *
@Select("SELECT * FROM user WHERE id = #{id}")
User selectById(Long id);

// ✅ 正例 - 指定字段
@Select("SELECT id, username, mobile, email FROM user WHERE id = #{id}")
User selectById(Long id);

// ✅ 如果字段太多，使用 MyBatis Plus BaseMapper
User user = userMapper.selectById(id);
```

> 优点：
> - 减少网络传输
> - 可能走覆盖索引，避免回表
> - 提升查询性能

### 覆盖索引优化

**【推荐】查询字段尽量使用覆盖索引，避免回表。**

```sql
-- 假设有联合索引 idx_user_status_time (user_id, status, create_time)

-- ❌ 反例 - 查询其他字段，需要回表
SELECT id, username, mobile FROM order 
WHERE user_id = 1 AND status = 1;

-- ✅ 正例 - 只查询索引字段，覆盖索引，无需回表
SELECT user_id, status, create_time FROM order 
WHERE user_id = 1 AND status = 1;

-- ✅ 正例 - 在索引中包含 id（主键自动包含）
SELECT id, user_id, status, create_time FROM order 
WHERE user_id = 1 AND status = 1;
```

### 分页查询优化

**【推荐】深分页使用"延迟关联"或"子查询"优化。**

```java
// ❌ 反例 - 深分页性能差（需要扫描并丢弃大量数据）
@Select("SELECT * FROM order WHERE user_id = #{userId} LIMIT 100000, 10")
List<Order> listByPage(Long userId);

// ✅ 正例 - 使用 id 范围查询
@Select("SELECT * FROM order WHERE user_id = #{userId} AND id > #{lastId} ORDER BY id LIMIT 10")
List<Order> listByLastId(@Param("userId") Long userId, @Param("lastId") Long lastId);

// ✅ 正例 - 延迟关联（先查主键，再关联）
@Select("SELECT o.* FROM order o " +
        "INNER JOIN (SELECT id FROM order WHERE user_id = #{userId} LIMIT 100000, 10) t " +
        "ON o.id = t.id")
List<Order> listByPageOptimized(Long userId);
```

### JOIN 查询优化

**【推荐】关联查询使用小表驱动大表，关联字段加索引。**

```sql
-- ❌ 反例 - 大表驱动小表
SELECT o.*, u.username FROM order o
LEFT JOIN user u ON o.user_id = u.id
WHERE u.vip_level = 5;

-- ✅ 正例 - 小表驱动大表
SELECT o.*, u.username FROM user u
INNER JOIN order o ON u.id = o.user_id
WHERE u.vip_level = 5;

-- ✅ 确保关联字段有索引
CREATE INDEX idx_user_id ON order(user_id);
```

**【强制】超过 3 个表禁止 JOIN，考虑业务层关联。**

```java
// ❌ 反例 - 多表 JOIN
@Select("SELECT o.*, u.*, p.*, a.* FROM `order` o " +
        "JOIN user u ON o.user_id = u.id " +
        "JOIN product p ON o.product_id = p.id " +
        "JOIN address a ON o.address_id = a.id")
List<OrderVO> listOrders();

// ✅ 正例 - 业务层关联
public List<OrderVO> listOrders() {
    // 1. 查询订单
    List<Order> orders = orderMapper.selectList(null);
    
    // 2. 批量查询关联数据
    List<Long> userIds = orders.stream().map(Order::getUserId).collect(Collectors.toList());
    Map<Long, User> userMap = userMapper.selectBatchIds(userIds)
        .stream().collect(Collectors.toMap(User::getId, u -> u));
    
    // 3. 组装 VO
    return orders.stream().map(order -> {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        vo.setUser(userMap.get(order.getUserId()));
        return vo;
    }).collect(Collectors.toList());
}
```

---

## IN 查询优化

**【推荐】IN 条件数量控制在 1000 以内。**

```java
// ❌ 反例 - IN 条件过多
List<Long> userIds = ...; // 10000 个 ID
List<Order> orders = orderMapper.selectList(
    new LambdaQueryWrapper<Order>().in(Order::getUserId, userIds)
);

// ✅ 正例 - 分批查询
List<Long> userIds = ...; // 10000 个 ID
List<Order> orders = new ArrayList<>();
Lists.partition(userIds, 1000).forEach(batch -> {
    orders.addAll(orderMapper.selectList(
        new LambdaQueryWrapper<Order>().in(Order::getUserId, batch)
    ));
});
```

---

## COUNT 查询优化

**【推荐】避免使用 COUNT(*)，改用 COUNT(1) 或 COUNT(主键)。**

> 说明：在 InnoDB 引擎下，COUNT(1) 和 COUNT(主键) 性能相当，略优于 COUNT(*)

```sql
-- ❌ 不推荐
SELECT COUNT(*) FROM order WHERE status = 1;

-- ✅ 推荐
SELECT COUNT(1) FROM order WHERE status = 1;
SELECT COUNT(id) FROM order WHERE status = 1;
```

**【推荐】大表 COUNT 使用近似值或缓存。**

```java
// ❌ 反例 - 大表 COUNT 慢
public long countOrders() {
    return orderMapper.selectCount(null);
}

// ✅ 正例 - 使用 Redis 缓存计数
@Cacheable(value = "order:count", key = "'all'")
public long countOrders() {
    return orderMapper.selectCount(null);
}

// ✅ 正例 - 使用近似值
@Select("SELECT table_rows FROM information_schema.tables " +
        "WHERE table_schema = DATABASE() AND table_name = 'order'")
long countApprox();
```

---

## 索引监控与优化

### EXPLAIN 分析

**【推荐】上线前使用 EXPLAIN 分析慢查询。**

```sql
EXPLAIN SELECT * FROM order WHERE user_id = 1 AND status = 1;
```

| 关键字段 | 说明 | 优化目标 |
|---------|------|---------|
| `type` | 访问类型 | 达到 `ref` 或 `range` |
| `key` | 实际使用的索引 | 不为 NULL |
| `rows` | 扫描行数 | 越小越好 |
| `Extra` | 额外信息 | 避免 `Using filesort` |

### 慢查询日志

**【推荐】开启慢查询日志，定期分析优化。**

```sql
-- 查看慢查询配置
SHOW VARIABLES LIKE 'slow_query%';
SHOW VARIABLES LIKE 'long_query_time';

-- 开启慢查询日志
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 1; -- 1 秒
```

---

## 索引设计检查清单

| 检查项 | 说明 | 优先级 |
|--------|------|--------|
| ✅ 主键使用 bigint 自增 | 避免页分裂 | 🔴 必须 |
| ✅ WHERE/ORDER BY 字段加索引 | 提升查询性能 | 🔴 必须 |
| ✅ 唯一字段建唯一索引 | 防止重复数据 | 🟡 推荐 |
| ✅ 联合索引遵循最左前缀 | 索引复用 | 🔴 必须 |
| ✅ 区分度高的字段在左侧 | 提升过滤效率 | 🟡 推荐 |
| ✅ 避免索引字段使用函数 | 防止索引失效 | 🔴 必须 |
| ✅ 避免隐式类型转换 | 防止索引失效 | 🔴 必须 |
| ✅ LIKE 避免左侧通配符 | 防止索引失效 | 🔴 必须 |
| ✅ 字段设置 NOT NULL | 提升性能 | 🟡 推荐 |
| ✅ 禁止 SELECT * | 减少传输 | 🔴 必须 |
| ✅ 使用覆盖索引 | 避免回表 | 🟡 推荐 |
| ✅ 深分页优化 | 避免大量扫描 | 🔴 必须 |
| ✅ JOIN 表加索引 | 提升关联性能 | 🔴 必须 |
| ✅ IN 条件 < 1000 | 避免SQL过长 | 🔴 必须 |

---

## 参考资料

- 阿里巴巴 Java 开发手册 - MySQL 数据库
- MySQL 官方文档 - Optimization
- InnoDB 存储引擎
