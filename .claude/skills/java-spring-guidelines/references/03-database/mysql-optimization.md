# MySQL 性能优化与调优

> Java/Spring Boot 编码规范 - MySQL 性能优化实战

---

## 一、EXPLAIN 查询分析

### 基本语法

```sql
EXPLAIN SELECT * FROM t_order WHERE user_id = 1001;
```

### EXPLAIN 输出字段详解

| 字段 | 说明 | 重要性 |
|------|------|--------|
| **id** | 查询序列号 | ⭐⭐⭐ |
| **select_type** | 查询类型 | ⭐⭐⭐ |
| **table** | 表名 | ⭐⭐ |
| **partitions** | 分区信息 | ⭐ |
| **type** | 访问类型 | ⭐⭐⭐⭐⭐ |
| **possible_keys** | 可能使用的索引 | ⭐⭐⭐ |
| **key** | 实际使用的索引 | ⭐⭐⭐⭐⭐ |
| **key_len** | 索引长度 | ⭐⭐⭐⭐ |
| **ref** | 索引的引用 | ⭐⭐⭐ |
| **rows** | 扫描行数 | ⭐⭐⭐⭐⭐ |
| **filtered** | 过滤百分比 | ⭐⭐⭐ |
| **Extra** | 额外信息 | ⭐⭐⭐⭐⭐ |

---

### 1. type（访问类型）- 最重要

**性能从好到坏排序**：

```
system > const > eq_ref > ref > range > index > ALL
```

| type | 说明 | 示例 | 性能 |
|------|------|------|------|
| **system** | 表只有一行（系统表） | - | ⭐⭐⭐⭐⭐ |
| **const** | 主键或唯一索引查询 | `WHERE id = 1` | ⭐⭐⭐⭐⭐ |
| **eq_ref** | 唯一索引扫描（JOIN） | `ON a.id = b.id` | ⭐⭐⭐⭐⭐ |
| **ref** | 非唯一索引查询 | `WHERE user_id = 1` | ⭐⭐⭐⭐ |
| **range** | 范围查询 | `WHERE id BETWEEN 1 AND 100` | ⭐⭐⭐ |
| **index** | 全索引扫描 | `SELECT id FROM t_order` | ⭐⭐ |
| **ALL** | 全表扫描 | `WHERE name = 'xxx'` 无索引 | ⭐ |

**【强制】type 至少要达到 range 级别，最好是 ref 或 const**

```sql
-- ❌ ALL：全表扫描
EXPLAIN SELECT * FROM t_order WHERE order_no = 'O123456789';

-- ✅ ref：使用普通索引
ALTER TABLE t_order ADD INDEX idx_order_no(order_no);
EXPLAIN SELECT * FROM t_order WHERE order_no = 'O123456789';

-- ✅ const：使用主键
EXPLAIN SELECT * FROM t_order WHERE id = 1001;
```

---

### 2. key（实际使用的索引）

```sql
-- ❌ key = NULL：没有使用索引
EXPLAIN SELECT * FROM t_order WHERE create_time > '2024-01-01';

-- ✅ key = idx_create_time：使用了索引
ALTER TABLE t_order ADD INDEX idx_create_time(create_time);
EXPLAIN SELECT * FROM t_order WHERE create_time > '2024-01-01';
```

---

### 3. rows（扫描行数）

**【推荐】rows 应尽可能小（< 1000）**

```sql
-- ❌ rows = 100000：扫描 10 万行
EXPLAIN SELECT * FROM t_order WHERE status = 1;

-- ✅ rows = 10：扫描 10 行
EXPLAIN SELECT * FROM t_order WHERE user_id = 1001;
```

---

### 4. Extra（额外信息）- 重要

| Extra 值 | 说明 | 是否良好 |
|----------|------|---------|
| **Using index** | 覆盖索引（只读索引，不回表） | ✅ 优秀 |
| **Using where** | WHERE 过滤 | ✅ 正常 |
| **Using index condition** | 索引条件下推 | ✅ 良好 |
| **Using filesort** | 文件排序（内存或磁盘） | ❌ 需优化 |
| **Using temporary** | 临时表 | ❌ 需优化 |
| **Using join buffer** | JOIN 缓冲 | ⚠️ 注意 |

```sql
-- ✅ Using index：覆盖索引（最优）
EXPLAIN SELECT id, user_id FROM t_order WHERE user_id = 1001;

-- ❌ Using filesort：排序未走索引
EXPLAIN SELECT * FROM t_order ORDER BY create_time DESC;

-- ✅ Using index：排序走了索引
ALTER TABLE t_order ADD INDEX idx_create_time(create_time);
EXPLAIN SELECT * FROM t_order ORDER BY create_time DESC;

-- ❌ Using temporary：分组未走索引
EXPLAIN SELECT user_id, COUNT(*) FROM t_order GROUP BY user_id;

-- ✅ 优化：添加索引
ALTER TABLE t_order ADD INDEX idx_user_id(user_id);
EXPLAIN SELECT user_id, COUNT(*) FROM t_order GROUP BY user_id;
```

---

### EXPLAIN 实战案例

#### 案例 1：慢查询优化

```sql
-- 慢查询
EXPLAIN SELECT * FROM t_order 
WHERE user_id = 1001 
  AND status = 1 
ORDER BY create_time DESC;

-- 结果：
-- type = ALL
-- rows = 500000
-- Extra = Using where; Using filesort
```

**优化步骤：**

```sql
-- 1. 添加联合索引
ALTER TABLE t_order 
ADD INDEX idx_user_status_time(user_id, status, create_time);

-- 2. 再次 EXPLAIN
EXPLAIN SELECT * FROM t_order 
WHERE user_id = 1001 
  AND status = 1 
ORDER BY create_time DESC;

-- 结果：
-- type = ref
-- key = idx_user_status_time
-- rows = 100
-- Extra = Using index condition
```

#### 案例 2：JOIN 优化

```sql
-- 慢查询
EXPLAIN SELECT o.*, u.username 
FROM t_order o
LEFT JOIN t_user u ON o.user_id = u.id
WHERE o.status = 1;

-- 结果：
-- type = ALL (t_order 表)
-- type = eq_ref (t_user 表)
-- rows = 100000
```

**优化：**

```sql
-- 添加索引
ALTER TABLE t_order ADD INDEX idx_status(status);

-- 再次 EXPLAIN
-- type = ref (t_order 表)
-- rows = 1000
```

---

## 二、count(*) vs count(1) vs count(字段)

### 性能对比

| 写法 | 是否统计 NULL | 性能 | 推荐 |
|------|--------------|------|------|
| **count(\*)** | 是 | ⭐⭐⭐⭐⭐ | ✅ 推荐 |
| **count(1)** | 是 | ⭐⭐⭐⭐⭐ | ✅ 可以 |
| **count(主键)** | 是 | ⭐⭐⭐⭐ | ⚠️ 次优 |
| **count(字段)** | 否（跳过 NULL） | ⭐⭐⭐ | ❌ 不推荐 |

### 详细说明

#### 1. count(\*) - 推荐

```sql
SELECT COUNT(*) FROM t_order;
```

**特点**：
- MySQL 优化器会选择最优的索引（通常是二级索引）
- InnoDB 会统计所有行（包括 NULL）
- **性能最优**

**底层原理**：
```
1. 优化器选择最小的二级索引
2. 扫描索引树，统计叶子节点数量
3. 不需要回表
```

#### 2. count(1)

```sql
SELECT COUNT(1) FROM t_order;
```

**特点**：
- 与 `count(*)` 性能几乎相同
- InnoDB 优化器会将 `count(1)` 转换为 `count(*)`

```sql
-- MySQL 5.7+ 中，count(1) 和 count(*) 完全等价
EXPLAIN SELECT COUNT(*) FROM t_order;
EXPLAIN SELECT COUNT(1) FROM t_order;

-- 执行计划相同
```

#### 3. count(主键)

```sql
SELECT COUNT(id) FROM t_order;
```

**特点**：
- 需要取出主键值进行判断
- 性能略低于 `count(*)`

#### 4. count(字段) - 不推荐

```sql
SELECT COUNT(username) FROM t_user;
```

**特点**：
- **跳过 NULL 值**（与其他不同）
- 需要取出字段值并判断
- 性能最差

```sql
-- 示例：字段包含 NULL
INSERT INTO t_user (id, username) VALUES 
(1, 'user1'),
(2, NULL),
(3, 'user3');

SELECT COUNT(*) FROM t_user;        -- 结果：3
SELECT COUNT(1) FROM t_user;        -- 结果：3
SELECT COUNT(id) FROM t_user;       -- 结果：3
SELECT COUNT(username) FROM t_user; -- 结果：2（跳过 NULL）
```

### 实战案例

```java
/**
 * ✅ 推荐：使用 count(*)
 */
@Mapper
public interface OrderMapper extends BaseMapper<OrderDO> {
    
    @Select("SELECT COUNT(*) FROM t_order WHERE user_id = #{userId}")
    Long countByUserId(@Param("userId") Long userId);
}

/**
 * ❌ 不推荐：使用 count(字段)
 */
@Select("SELECT COUNT(id) FROM t_order WHERE user_id = #{userId}")
Long countByUserId(@Param("userId") Long userId);
```

### 大表 count(*) 优化

**问题**：千万级数据表，`count(*)` 很慢

```sql
-- 慢查询
SELECT COUNT(*) FROM t_order WHERE status = 1;
-- 耗时：5 秒
```

**优化方案：**

#### 方案 1：使用 Redis 缓存

```java
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements IOrderService {
    
    private final OrderMapper orderMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 获取订单总数（带缓存）
     */
    @Override
    public Long getOrderCount(Long userId, Integer status) {
        String cacheKey = String.format("order:count:%s:%s", userId, status);
        
        // 1. 查缓存
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return Long.valueOf(cached.toString());
        }
        
        // 2. 查数据库
        Long count = orderMapper.countByUserIdAndStatus(userId, status);
        
        // 3. 写缓存（5 分钟过期）
        redisTemplate.opsForValue().set(cacheKey, count, 5, TimeUnit.MINUTES);
        
        return count;
    }
    
    /**
     * 创建订单时更新计数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createOrder(OrderCreateDTO dto) {
        // 插入订单
        OrderDO order = convertToEntity(dto);
        orderMapper.insert(order);
        
        // 清除缓存
        String cacheKey = String.format("order:count:%s:%s", 
                                        order.getUserId(), order.getStatus());
        redisTemplate.delete(cacheKey);
    }
}
```

#### 方案 2：使用单独的计数表

```sql
-- 创建计数表
CREATE TABLE t_order_count (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    status TINYINT NOT NULL,
    count BIGINT DEFAULT 0,
    UNIQUE KEY uk_user_status(user_id, status)
);

-- 插入订单时更新计数
INSERT INTO t_order_count (user_id, status, count)
VALUES (1001, 1, 1)
ON DUPLICATE KEY UPDATE count = count + 1;

-- 删除订单时减少计数
UPDATE t_order_count 
SET count = count - 1 
WHERE user_id = 1001 AND status = 1;
```

---

## 三、深度分页优化

### 问题场景

```sql
-- 慢查询：深度分页
SELECT * FROM t_order 
ORDER BY create_time DESC 
LIMIT 1000000, 20;

-- 耗时：10 秒
-- 原因：MySQL 需要扫描 1000020 行，然后丢弃前 1000000 行
```

### 优化方案

#### 方案 1：子查询优化（推荐）

**核心思想**：先用覆盖索引查出 ID，再回表查询完整数据

```sql
-- ❌ 慢查询
SELECT * FROM t_order 
ORDER BY create_time DESC 
LIMIT 1000000, 20;

-- ✅ 优化后（覆盖索引 + 子查询）
SELECT * FROM t_order 
WHERE id >= (
    SELECT id FROM t_order 
    ORDER BY create_time DESC 
    LIMIT 1000000, 1
)
ORDER BY create_time DESC 
LIMIT 20;
```

```java
/**
 * 深度分页优化
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements IOrderService {
    
    private final OrderMapper orderMapper;
    
    /**
     * ✅ 优化：子查询方式
     */
    @Override
    public List<OrderDTO> pageOrders(Integer pageNum, Integer pageSize) {
        // 1. 计算 offset
        int offset = (pageNum - 1) * pageSize;
        
        // 2. 子查询优化
        List<OrderDO> orders = orderMapper.selectPageOptimized(offset, pageSize);
        
        return convertToDTOList(orders);
    }
}
```

```xml
<!-- Mapper XML -->
<select id="selectPageOptimized" resultType="OrderDO">
    SELECT * FROM t_order 
    WHERE id >= (
        SELECT id FROM t_order 
        ORDER BY create_time DESC 
        LIMIT #{offset}, 1
    )
    ORDER BY create_time DESC 
    LIMIT #{pageSize}
</select>
```

**性能对比**：

| 方案 | LIMIT 10, 20 | LIMIT 100000, 20 | LIMIT 1000000, 20 |
|------|--------------|------------------|-------------------|
| **直接查询** | 10ms | 500ms | 10s |
| **子查询优化** | 10ms | 50ms | 100ms |

#### 方案 2：游标分页（Cursor-based Pagination）

**核心思想**：使用上一页的最后一条数据的 ID 作为起点

```sql
-- 第一页
SELECT * FROM t_order 
WHERE status = 1 
ORDER BY id DESC 
LIMIT 20;

-- 第二页（假设第一页最后一条 ID = 999980）
SELECT * FROM t_order 
WHERE status = 1 
  AND id < 999980 
ORDER BY id DESC 
LIMIT 20;
```

```java
/**
 * 游标分页
 */
@Data
public class CursorPageDTO {
    private Long cursor;  // 上一页最后一条记录的 ID
    private Integer pageSize;
}

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements IOrderService {
    
    private final OrderMapper orderMapper;
    
    /**
     * ✅ 游标分页
     */
    @Override
    public CursorPageResult<OrderDTO> cursorPage(CursorPageDTO pageDTO) {
        // 查询数据
        List<OrderDO> orders = orderMapper.selectByCursor(
            pageDTO.getCursor(), 
            pageDTO.getPageSize()
        );
        
        // 构造结果
        CursorPageResult<OrderDTO> result = new CursorPageResult<>();
        result.setList(convertToDTOList(orders));
        
        // 设置下一页游标
        if (!orders.isEmpty()) {
            result.setNextCursor(orders.get(orders.size() - 1).getId());
        }
        
        return result;
    }
}
```

```xml
<select id="selectByCursor" resultType="OrderDO">
    SELECT * FROM t_order 
    WHERE status = 1 
    <if test="cursor != null">
        AND id &lt; #{cursor}
    </if>
    ORDER BY id DESC 
    LIMIT #{pageSize}
</select>
```

**优点**：
- ✅ 性能稳定（不受页码影响）
- ✅ 适合移动端下拉刷新

**缺点**：
- ❌ 不支持跳页
- ❌ 不能显示总页数

#### 方案 3：延迟关联（Deferred Join）

```sql
-- ✅ 延迟关联
SELECT o.* 
FROM t_order o
INNER JOIN (
    SELECT id FROM t_order 
    ORDER BY create_time DESC 
    LIMIT 1000000, 20
) AS tmp ON o.id = tmp.id;
```

#### 方案 4：使用 Elasticsearch

```java
/**
 * ✅ 推荐：大数据量使用 ES
 */
@Service
@RequiredArgsConstructor
public class OrderSearchService {
    
    private final ElasticsearchRestTemplate esTemplate;
    
    public PageResult<OrderDTO> searchOrders(OrderQueryDTO query) {
        // ES 分页性能稳定
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.matchAllQuery())
            .withPageable(PageRequest.of(query.getPageNum(), query.getPageSize()))
            .build();
        
        SearchHits<OrderDocument> hits = esTemplate.search(searchQuery, OrderDocument.class);
        
        // 转换结果
        List<OrderDTO> list = hits.stream()
            .map(hit -> convertToDTO(hit.getContent()))
            .collect(Collectors.toList());
        
        return PageResult.of(list, hits.getTotalHits());
    }
}
```

---

## 四、SQL 调优通用步骤

### 第 1 步：开启慢查询日志

```sql
-- 查看慢查询配置
SHOW VARIABLES LIKE 'slow_query%';
SHOW VARIABLES LIKE 'long_query_time';

-- 开启慢查询日志
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 1;  -- 1 秒

-- 日志路径
SHOW VARIABLES LIKE 'slow_query_log_file';
```

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/db?slowQueryThresholdMillis=1000&logSlowQueries=true
```

### 第 2 步：使用 EXPLAIN 分析

```sql
EXPLAIN SELECT * FROM t_order WHERE user_id = 1001;
```

**检查项**：
1. type 是否是 ALL？
2. key 是否是 NULL？
3. rows 是否很大？
4. Extra 是否有 Using filesort 或 Using temporary？

### 第 3 步：添加索引

```sql
-- 添加索引
ALTER TABLE t_order ADD INDEX idx_user_id(user_id);

-- 再次 EXPLAIN
EXPLAIN SELECT * FROM t_order WHERE user_id = 1001;
```

### 第 4 步：使用 SHOW PROFILE

```sql
-- 开启 profiling
SET profiling = 1;

-- 执行查询
SELECT * FROM t_order WHERE user_id = 1001;

-- 查看耗时
SHOW PROFILES;

-- 查看详细信息
SHOW PROFILE FOR QUERY 1;
```

### 第 5 步：优化 SQL

**常见优化技巧：**

```sql
-- 1. 避免 SELECT *
-- ❌ 
SELECT * FROM t_order WHERE user_id = 1001;
-- ✅ 
SELECT id, order_no, status FROM t_order WHERE user_id = 1001;

-- 2. 使用覆盖索引
-- ❌ 需要回表
SELECT * FROM t_order WHERE user_id = 1001;
-- ✅ 覆盖索引
SELECT id, user_id, status FROM t_order WHERE user_id = 1001;

-- 3. 避免函数操作
-- ❌ 索引失效
SELECT * FROM t_order WHERE DATE(create_time) = '2024-01-01';
-- ✅ 
SELECT * FROM t_order 
WHERE create_time >= '2024-01-01 00:00:00' 
  AND create_time < '2024-01-02 00:00:00';

-- 4. 避免隐式转换
-- ❌ order_no 是 VARCHAR，传入 INT 导致索引失效
SELECT * FROM t_order WHERE order_no = 123456;
-- ✅ 
SELECT * FROM t_order WHERE order_no = '123456';

-- 5. 使用 UNION ALL 替代 UNION
-- ❌ UNION 会去重（Using temporary）
SELECT * FROM t_order WHERE status = 1
UNION
SELECT * FROM t_order WHERE status = 2;
-- ✅ UNION ALL 不去重
SELECT * FROM t_order WHERE status = 1
UNION ALL
SELECT * FROM t_order WHERE status = 2;
```

---

## 五、监控与诊断

### 1. 慢查询统计

```java
/**
 * 慢查询拦截器
 */
@Slf4j
@Component
@Intercepts({
    @Signature(type = Executor.class, method = "query", 
               args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
    @Signature(type = Executor.class, method = "update", 
               args = {MappedStatement.class, Object.class})
})
public class SlowQueryInterceptor implements Interceptor {
    
    private static final long SLOW_QUERY_THRESHOLD = 1000; // 1 秒
    
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        try {
            return invocation.proceed();
        } finally {
            long endTime = System.currentTimeMillis();
            long cost = endTime - startTime;
            
            if (cost > SLOW_QUERY_THRESHOLD) {
                MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
                String sqlId = ms.getId();
                
                log.warn("[慢查询] SQL ID: {}, 耗时: {}ms", sqlId, cost);
                
                // 发送告警
                sendSlowQueryAlert(sqlId, cost);
            }
        }
    }
}
```

### 2. 连接池监控

```yaml
# application.yml
spring:
  datasource:
    hikari:
      minimum-idle: 10
      maximum-pool-size: 50
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      # 开启 JMX 监控
      register-mbeans: true
```

```java
/**
 * 连接池监控
 */
@Component
@Slf4j
public class DataSourceMonitor {
    
    @Resource
    private HikariDataSource dataSource;
    
    @Scheduled(fixedRate = 60000)  // 每分钟
    public void monitor() {
        HikariPoolMXBean pool = dataSource.getHikariPoolMXBean();
        
        log.info("[连接池监控] 活跃连接: {}, 空闲连接: {}, 等待线程: {}, 总连接: {}", 
                 pool.getActiveConnections(),
                 pool.getIdleConnections(),
                 pool.getThreadsAwaitingConnection(),
                 pool.getTotalConnections());
        
        // 告警
        if (pool.getThreadsAwaitingConnection() > 10) {
            log.error("[连接池告警] 等待连接的线程过多: {}", 
                      pool.getThreadsAwaitingConnection());
        }
    }
}
```

---

## 六、速查表

### EXPLAIN type 性能

| type | 性能 | 说明 |
|------|------|------|
| **const** | ⭐⭐⭐⭐⭐ | 主键/唯一索引 |
| **ref** | ⭐⭐⭐⭐ | 普通索引 |
| **range** | ⭐⭐⭐ | 范围查询 |
| **index** | ⭐⭐ | 全索引扫描 |
| **ALL** | ⭐ | 全表扫描（需优化） |

### count 函数性能

| 函数 | 性能 | 推荐 |
|------|------|------|
| **count(\*)** | ⭐⭐⭐⭐⭐ | ✅ |
| **count(1)** | ⭐⭐⭐⭐⭐ | ✅ |
| **count(主键)** | ⭐⭐⭐⭐ | ⚠️ |
| **count(字段)** | ⭐⭐⭐ | ❌ |

### 分页方案选择

| 场景 | 推荐方案 | 原因 |
|------|---------|------|
| **浅分页（前100页）** | LIMIT offset, size | 性能可接受 |
| **深度分页（后100页）** | 子查询优化 | 避免扫描大量数据 |
| **移动端下拉刷新** | 游标分页 | 性能稳定 |
| **大数据量搜索** | Elasticsearch | 专业搜索引擎 |
