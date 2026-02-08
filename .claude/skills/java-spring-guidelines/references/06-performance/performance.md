# 性能优化

> Java/Spring Boot 编码规范 - 性能优化

---

## N+1 查询问题

### 问题示例

```java
// ❌ 错误：循环查询数据库（N+1 问题）
public List<OrderVO> getOrderList(List<Long> orderIds) {
    List<OrderVO> result = new ArrayList<>();
    for (Long orderId : orderIds) {
        Order order = orderMapper.selectById(orderId);        // 1 次查询
        User user = userMapper.selectById(order.getUserId());  // N 次查询
        OrderVO vo = new OrderVO();
        vo.setOrder(order);
        vo.setUser(user);
        result.add(vo);
    }
    return result;
}

// ✅ 正确：批量查询 + 内存关联
public List<OrderVO> getOrderList(List<Long> orderIds) {
    // 1. 批量查询订单
    List<Order> orders = orderMapper.selectBatchIds(orderIds);

    // 2. 提取用户ID
    Set<Long> userIds = orders.stream()
        .map(Order::getUserId)
        .collect(Collectors.toSet());

    // 3. 批量查询用户
    List<User> users = userMapper.selectBatchIds(userIds);
    Map<Long, User> userMap = users.stream()
        .collect(Collectors.toMap(User::getId, Function.identity()));

    // 4. 内存关联
    List<OrderVO> result = new ArrayList<>();
    for (Order order : orders) {
        OrderVO vo = new OrderVO();
        vo.setOrder(order);
        vo.setUser(userMap.get(order.getUserId()));
        result.add(vo);
    }
    return result;
}
```

### 使用 MyBatis 连接查询

```xml
<!-- 一次性查询订单和用户信息 -->
<select id="selectOrderWithUser" resultMap="OrderWithUserResultMap">
    SELECT
        o.id, o.order_no, o.user_id, o.amount,
        u.id AS user_id, u.name AS user_name, u.mobile AS user_mobile
    FROM `order` o
    LEFT JOIN `user` u ON o.user_id = u.id
    WHERE o.id IN
    <foreach collection="orderIds" item="id" open="(" separator="," close=")">
        #{id}
    </foreach>
</select>
```

---

## 深度分页问题

### 问题示例

```sql
-- ❌ 错误：深度分页性能差
SELECT * FROM product ORDER BY id LIMIT 100000, 10;
-- MySQL 需要扫描 100010 行，然后丢弃前 100000 行
```

### 解决方案：游标分页

```java
// DTO 设计
@Data
public class ProductPageReq {
    private Long lastId;    // 上次查询的最后一条记录ID
    private Integer pageSize = 10;
}

// Service
public List<ProductDTO> getPage(ProductPageReq req) {
    LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
        .gt(req.getLastId() != null, Product::getId, req.getLastId())
        .orderByAsc(Product::getId)
        .last("LIMIT " + req.getPageSize());

    return productMapper.selectList(wrapper).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
}

// SQL: SELECT * FROM product WHERE id > #{lastId} ORDER BY id LIMIT 10
```

---

## 批量处理

### 超过 1000 条必须分批

```java
// ❌ 错误：一次性处理大量数据，可能导致 OOM
public void processAllProducts() {
    List<Product> allProducts = productMapper.selectList(null);  // 可能有几十万条
    for (Product product : allProducts) {
        processProduct(product);
    }
}

// ✅ 正确：分批处理
public void processAllProducts() {
    int batchSize = 1000;
    long lastId = 0L;

    while (true) {
        List<Product> batch = productMapper.selectList(
            new LambdaQueryWrapper<Product>()
                .gt(Product::getId, lastId)
                .orderByAsc(Product::getId)
                .last("LIMIT " + batchSize)
        );

        if (CollectionUtils.isEmpty(batch)) {
            break;
        }

        for (Product product : batch) {
            processProduct(product);
        }

        lastId = batch.get(batch.size() - 1).getId();

        if (batch.size() < batchSize) {
            break;  // 最后一批
        }
    }
}

// ✅ 正确：使用 MyBatis Plus 分页
public void processAllProducts() {
    long current = 1;
    long size = 1000;

    while (true) {
        Page<Product> page = productMapper.selectPage(
            new Page<>(current, size),
            null
        );

        List<Product> records = page.getRecords();
        if (CollectionUtils.isEmpty(records)) {
            break;
        }

        for (Product product : records) {
            processProduct(product);
        }

        if (records.size() < size) {
            break;  // 最后一批
        }
        current++;
    }
}
```

### 批量插入

```java
// ❌ 错误：逐条插入
public void batchInsert(List<Product> products) {
    for (Product product : products) {
        productMapper.insert(product);  // N 次数据库交互
    }
}

// ✅ 正确：使用 MyBatis Plus 批量插入
public void batchInsert(List<Product> products) {
    if (CollectionUtils.isEmpty(products)) {
        return;
    }

    // 超过 1000 条分批
    int batchSize = 1000;
    for (int i = 0; i < products.size(); i += batchSize) {
        int end = Math.min(i + batchSize, products.size());
        List<Product> batch = products.subList(i, end);
        productService.saveBatch(batch);
    }
}

// ✅ 更高效：自定义批量插入 SQL
@Insert("<script>" +
        "INSERT INTO product (name, price, stock) VALUES " +
        "<foreach collection='list' item='item' separator=','>" +
        "(#{item.name}, #{item.price}, #{item.stock})" +
        "</foreach>" +
        "</script>")
int batchInsert(@Param("list") List<Product> products);
```

---

## 大数据量导出

### 使用流式查询

```java
/**
 * 流式查询导出
 */
public void exportProducts(HttpServletResponse response) {
    response.setContentType("application/vnd.ms-excel");
    response.setCharacterEncoding("utf-8");

    try (ExcelWriter writer = EasyExcel.write(response.getOutputStream(), ProductDTO.class).build()) {
        WriteSheet sheet = EasyExcel.writerSheet("商品").build();

        // 方式1：使用 MyBatis 流式查询
        try (Cursor<Product> cursor = productMapper.selectCursor(
                new LambdaQueryWrapper<Product>()
                    .eq(Product::getDeleted, false)
        )) {
            for (Product product : cursor) {
                ProductDTO dto = toDTO(product);
                writer.write(dto, sheet);
            }
        }

        // 方式2：使用 JDBC 流式查询
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT * FROM product WHERE deleted = false",
                 ResultSet.TYPE_FORWARD_ONLY,
                 ResultSet.CONCUR_READ_ONLY
             )) {
            ps.setFetchSize(1000);  // 每次从数据库获取 1000 条
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ProductDTO dto = extractFromResultSet(rs);
                    writer.write(dto, sheet);
                }
            }
        }
    } catch (IOException e) {
        throw exception(EXPORT_FAILED);
    }
}
```

---

## 缓存优化

### 多级缓存

```java
@Component
public class ProductService {

    // L1: 本地缓存
    private final LoadingCache<Long, Product> localCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build(id -> loadFromDb(id));

    // L2: Redis 缓存
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public Product getProductById(Long productId) {
        // L1: 本地缓存
        Product product = localCache.getIfPresent(productId);
        if (product != null) {
            return product;
        }

        // L2: Redis 缓存
        String key = "product:detail:" + productId;
        product = (Product) redisTemplate.opsForValue().get(key);
        if (product != null) {
            localCache.put(productId, product);
            return product;
        }

        // L3: 数据库
        product = loadFromDb(productId);
        if (product != null) {
            redisTemplate.opsForValue().set(key, product, 30, TimeUnit.MINUTES);
            localCache.put(productId, product);
        }

        return product;
    }
}
```

---

## 异步处理

### 使用 @Async

```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}

@Service
public class OrderService {

    @Async("asyncExecutor")
    public void sendOrderNotification(Long orderId) {
        // 异步发送通知
        emailService.sendOrderEmail(orderId);
    }

    public void createOrder(Order order) {
        // 同步创建订单
        orderMapper.insert(order);

        // 异步发送通知
        sendOrderNotification(order.getId());
    }
}
```

---

## 索引优化

### 覆盖索引

```sql
-- 假设有索引 idx_user_status_time (user_id, status, create_time)

-- ✅ 使用覆盖索引，无需回表
SELECT id, user_id, status, create_time
FROM `order`
WHERE user_id = 123 AND status = 1
ORDER BY create_time DESC
LIMIT 10;

-- ❌ 需要回表查询 amount
SELECT id, user_id, status, create_time, amount
FROM `order`
WHERE user_id = 123 AND status = 1
ORDER BY create_time DESC
LIMIT 10;

-- ✅ 优化：将 amount 加入索引
-- ALTER TABLE `order` ADD INDEX idx_user_status_time_amount (user_id, status, create_time, amount);
```

---

## 性能规范速查表

| 问题 | 解决方案 |
|------|---------|
| **N+1 查询** | 批量查询 + 内存关联 / JOIN 查询 |
| **深度分页** | 游标分页（WHERE id > lastId） |
| **大批量处理** | 分批处理，每批 1000 条 |
| **大数据导出** | 流式查询（Cursor / ResultSet） |
| **接口慢** | 添加缓存，使用多级缓存 |
| **并发高** | 异步处理 @Async |
| **索引失效** | 避免函数计算、隐式转换 |
| **慢 SQL** | 使用 EXPLAIN 分析，添加索引 |
