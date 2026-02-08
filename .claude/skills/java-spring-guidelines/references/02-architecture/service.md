# Service 层规范

> Java/Spring Boot 编码规范 - Service 层

---

## 职责边界

**Service 层负责：**

1. 业务逻辑处理
2. 事务控制
3. 数据校验
4. 调用 Mapper/MQ/外部接口

```java
// ✅ 正确：Service 层包含业务逻辑
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements IOrderService {

    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final PaymentClient paymentClient;

    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(OrderCreateReq req) {
        // 1. 业务校验
        Product product = productMapper.selectById(req.getProductId());
        if (product == null) {
            throw exception(PRODUCT_NOT_EXISTS);
        }
        if (product.getStock() < req.getQuantity()) {
            throw exception(PRODUCT_STOCK_INSUFFICIENT);
        }

        // 2. 构建订单
        Order order = buildOrder(product, req);

        // 3. 扣减库存
        productMapper.reduceStock(product.getId(), req.getQuantity());

        // 4. 保存订单
        orderMapper.insert(order);

        // 5. 调用支付
        paymentClient.createPayment(order.getId(), order.getAmount());

        return order.getId();
    }
}
```

---

## 复杂度控制

### 方法长度

| 复杂度 | 限制 | 说明 |
|--------|------|------|
| 方法行数 | ≤ 50 行 | 超过考虑拆分 |
| if-else 嵌套 | ≤ 2 层 | 超过使用卫语句 |
| 分支数量 | ≤ 3 个 | 超过考虑策略模式 |

### 使用卫语句减少嵌套

```java
// ❌ 错误：多层嵌套
public void processOrder(Order order) {
    if (order != null) {
        if (order.getStatus() == OrderStatus.PENDING) {
            if (order.getAmount() > 0) {
                if (order.getUserId() != null) {
                    // 业务逻辑
                    doProcess(order);
                }
            }
        }
    }
}

// ✅ 正确：卫语句
public void processOrder(Order order) {
    if (order == null) {
        throw exception(ORDER_NOT_EXISTS);
    }
    if (order.getStatus() != OrderStatus.PENDING) {
        throw exception(ORDER_STATUS_ERROR);
    }
    if (order.getAmount() <= 0) {
        throw exception(ORDER_AMOUNT_ERROR);
    }
    if (order.getUserId() == null) {
        throw exception(USER_NOT_EXISTS);
    }
    doProcess(order);
}
```

---

## 策略模式替代多分支

超过 3 个分支时使用策略模式：

```java
// ❌ 差的做法：大量 if-else
public BigDecimal calculateDiscount(Order order, String couponType) {
    if ("FIXED".equals(couponType)) {
        return order.getAmount().subtract(new BigDecimal("10"));
    } else if ("PERCENTAGE".equals(couponType)) {
        return order.getAmount().multiply(new BigDecimal("0.9"));
    } else if ("FULL_REDUCTION".equals(couponType)) {
        if (order.getAmount().compareTo(new BigDecimal("100")) > 0) {
            return order.getAmount().subtract(new BigDecimal("20"));
        }
        return order.getAmount();
    }
    return order.getAmount();
}

// ✅ 好的做法：策略模式
public interface DiscountStrategy {
    BigDecimal calculate(Order order);
}

@Component("fixedDiscount")
public class FixedDiscountStrategy implements DiscountStrategy {
    @Override
    public BigDecimal calculate(Order order) {
        return order.getAmount().subtract(new BigDecimal("10"));
    }
}

@Component("percentageDiscount")
public class PercentageDiscountStrategy implements DiscountStrategy {
    @Override
    public BigDecimal calculate(Order order) {
        return order.getAmount().multiply(new BigDecimal("0.9"));
    }
}

@Service
@RequiredArgsConstructor
public class DiscountService {
    private final Map<String, DiscountStrategy> strategyMap;

    @PostConstruct
    public void init() {
        strategyMap = new HashMap<>();
        strategyMap.put("FIXED", fixedDiscount);
        strategyMap.put("PERCENTAGE", percentageDiscount);
    }

    public BigDecimal calculateDiscount(Order order, String couponType) {
        DiscountStrategy strategy = strategyMap.getOrDefault(couponType, order -> order.getAmount());
        return strategy.calculate(order);
    }
}
```

---

## 事务控制

### 基本规则

```java
// ✅ 正确：多表操作加事务
@Transactional(rollbackFor = Exception.class)
public void createOrderWithItems(Order order, List<OrderItem> items) {
    orderMapper.insert(order);
    items.forEach(item -> {
        item.setOrderId(order.getId());
        orderItemMapper.insert(item);
    });
}

// ⚠️ 注意：事务方法必须是 public
// private 方法上的 @Transactional 不生效

// ⚠️ 注意：避免同类方法内部调用
public void methodA() {
    this.methodB();  // methodB 的 @Transactional 不生效
}
```

### 同类调用代理失效

```java
// ❌ 错误：同类调用事务不生效
@Service
public class OrderService {
    public void createOrder(Order order) {
        this.saveOrder(order);  // @Transactional 不生效
        this.saveOrderItems(order.getItems());
    }

    @Transactional
    public void saveOrder(Order order) { }

    @Transactional
    public void saveOrderItems(List<OrderItem> items) { }
}

// ✅ 解决方案1：注入自身
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderService self;  // Spring 4.3+ 支持自注入

    public void createOrder(Order order) {
        self.saveOrder(order);  // 通过代理调用
        self.saveOrderItems(order.getItems());
    }

    @Transactional
    public void saveOrder(Order order) { }

    @Transactional
    public void saveOrderItems(List<OrderItem> items) { }
}

// ✅ 解决方案2：拆分到另一个 Service
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderItemService orderItemService;

    @Transactional
    public void createOrder(Order order) {
        saveOrder(order);
        orderItemService.saveItems(order.getItems());  // 跨服务调用，代理生效
    }
}
```

### 多数据源事务限制

`@Transactional` 只对主数据源生效，事务方法中禁止混用多个数据源：

```java
// ❌ 错误：事务中混用 MySQL 和 Doris
@Transactional(rollbackFor = Exception.class)
public void syncData() {
    // Doris 查询不在事务管理范围内
    List<Data> dorisData = dorisMapper.selectList();
    // MySQL 写入在事务中
    mysqlMapper.saveBatch(dorisData);
}

// ✅ 正确：拆分方法
public void syncData() {
    // 1. 非事务方法查询 Doris
    List<Data> dorisData = queryFromDoris();
    // 2. 事务方法写入 MySQL
    saveToMysql(dorisData);
}

@Transactional(rollbackFor = Exception.class)
public void saveToMysql(List<Data> data) {
    mysqlMapper.saveBatch(data);
}
```

---

## 对象转换

### Entity 与 DTO 转换

```java
// ✅ 正确：使用 BeanUtils.copyProperties
public ProductDTO toDTO(Product entity) {
    ProductDTO dto = new ProductDTO();
    BeanUtils.copyProperties(entity, dto);
    return dto;
}

// ✅ 正确：批量转换
public List<ProductDTO> toDTOList(List<Product> entities) {
    return entities.stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
}

// ⚠️ 注意：BeanUtils.copyProperties 是浅拷贝
// 嵌套对象需手动处理
public OrderDetailRsp toRsp(Order entity) {
    OrderDetailRsp rsp = new OrderDetailRsp();
    BeanUtils.copyProperties(entity, rsp);

    // 嵌套对象手动处理
    if (entity.getUser() != null) {
        UserInfoRsp userInfo = new UserInfoRsp();
        BeanUtils.copyProperties(entity.getUser(), userInfo);
        rsp.setUserInfo(userInfo);
    }

    if (CollectionUtils.isNotEmpty(entity.getItems())) {
        List<OrderItemRsp> items = entity.getItems().stream()
            .map(item -> {
                OrderItemRsp itemRsp = new OrderItemRsp();
                BeanUtils.copyProperties(item, itemRsp);
                return itemRsp;
            })
            .collect(Collectors.toList());
        rsp.setItems(items);
    }

    return rsp;
}
```

### 使用 MapStruct（推荐）

```java
@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductDTO toDTO(Product entity);

    List<ProductDTO> toDTOList(List<Product> entities);

    @Mapping(target = "createTime", format = "yyyy-MM-dd HH:mm:ss")
    ProductDetailRsp toDetailRsp(Product entity);
}
```

---

## 空安全处理

### 集合判空

```java
// ✅ 好的做法：使用工具类
if (CollectionUtils.isEmpty(list)) {
    return Collections.emptyList();
}

// ✅ 好的做法：Optional
List<OrderItem> items = Optional.ofNullable(order)
    .map(Order::getItems)
    .orElse(Collections.emptyList());
```

### 链式调用防空

```java
// ✅ 好的做法：Optional 链式调用
String city = Optional.ofNullable(user)
    .map(User::getAddress)
    .map(Address::getCity)
    .orElse("");

// ✅ 好的做法：多层防空
BigDecimal amount = Optional.ofNullable(order)
    .map(Order::getPayment)
    .map(Payment::getAmount)
    .orElse(BigDecimal.ZERO);
```

---

## Service 完整示例

```java
/**
 * 商品服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements IProductService {

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public IPage<ProductDTO> getPage(ProductPageReq req) {
        log.info("[商品查询]，分页查询，条件: {}", req);

        // 1. 构建查询条件
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
            .like(StringUtils.isNotBlank(req.getName()),
                Product::getName, req.getName())
            .eq(req.getStatus() != null, Product::getStatus, req.getStatus())
            .orderByDesc(Product::getCreateTime);

        // 2. 分页查询
        IPage<Product> page = productMapper.selectPage(
            new Page<>(req.getPageNum(), req.getPageSize()),
            wrapper
        );

        // 3. 转换 DTO
        List<ProductDTO> records = toDTOList(page.getRecords());

        // 4. 返回结果
        return new Page<>(records, page.getTotal(), page.getSize(), page.getCurrent());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addProduct(ProductAddReq req) {
        log.info("[商品新增]，开始新增，编码: {}，名称: {}", req.getCode(), req.getName());

        // 1. 校验编码唯一
        Product exists = productMapper.selectByCode(req.getCode());
        if (exists != null) {
            throw exception(PRODUCT_CODE_DUPLICATE);
        }

        // 2. 校验分类存在
        Category category = categoryMapper.selectById(req.getCategoryId());
        if (category == null) {
            throw exception(CATEGORY_NOT_EXISTS);
        }

        // 3. 构建实体
        Product product = new Product();
        BeanUtils.copyProperties(req, product);
        product.setStatus(1);  // 默认上架

        // 4. 保存
        productMapper.insert(product);

        // 5. 清除缓存
        clearProductCache(product.getId());

        log.info("[商品新增]，新增成功，商品ID: {}", product.getId());
        return product.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProduct(ProductUpdateReq req) {
        log.info("[商品修改]，开始修改，商品ID: {}", req.getId());

        // 1. 校验商品存在
        Product product = productMapper.selectById(req.getId());
        if (product == null) {
            throw exception(PRODUCT_NOT_EXISTS);
        }

        // 2. 更新
        Product update = new Product();
        BeanUtils.copyProperties(req, update);
        productMapper.updateById(update);

        // 3. 清除缓存
        clearProductCache(req.getId());

        log.info("[商品修改]，修改成功，商品ID: {}", req.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long productId) {
        log.info("[商品删除]，开始删除，商品ID: {}", productId);

        // 1. 校验商品存在
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw exception(PRODUCT_NOT_EXISTS);
        }

        // 2. 逻辑删除
        productMapper.deleteById(productId);

        // 3. 清除缓存
        clearProductCache(productId);

        log.info("[商品删除]，删除成功，商品ID: {}", productId);
    }

    /**
     * 清除商品缓存
     */
    private void clearProductCache(Long productId) {
        String key = "product:detail:" + productId;
        redisTemplate.delete(key);
    }

    /**
     * Entity 转 DTO
     */
    private ProductDTO toDTO(Product entity) {
        ProductDTO dto = new ProductDTO();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    private List<ProductDTO> toDTOList(List<Product> entities) {
        return entities.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
}
```

---

## Service 规范速查表

| 规范 | 要点 |
|------|------|
| **职责边界** | 业务逻辑、事务控制、数据校验 |
| **方法长度** | ≤ 50 行，超过考虑拆分 |
| **if-else 嵌套** | ≤ 2 层，超过使用卫语句 |
| **分支处理** | > 3 个分支使用策略模式 |
| **事务注解** | `@Transactional(rollbackFor = Exception.class)` |
| **同类调用** | 注入 self 或拆分 Service |
| **多数据源** | 事务方法禁止混用多数据源 |
| **对象转换** | Entity ↔ DTO，禁止直接返回 Entity |
| **空安全** | 使用 `CollectionUtils.isEmpty()` 和 `Optional` |
