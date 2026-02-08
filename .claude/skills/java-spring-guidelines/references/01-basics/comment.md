# 注释规约

> Java/Spring Boot 编码规范 - 注释规范
> 参考：阿里巴巴 Java 开发手册

---

## 基本原则

**【强制】注释应解释"为什么"，而不是"做什么"。好代码本身就是最好的文档。**

```java
// ❌ 反例 - 解释做什么（代码已经说明）
// 设置用户名
user.setName(name);

// ✅ 正例 - 解释为什么
// 用户名需要去除首尾空格，防止登录时匹配失败
user.setName(name.trim());
```

---

## 类注释

**【强制】所有的类都必须添加创建者和创建日期。**

```java
/**
 * 订单服务实现类
 * <p>
 * 处理订单创建、支付、发货、取消等核心业务逻辑
 * </p>
 *
 * @author zhangsan
 * @since 2026-01-01
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements IOrderService {
    // ...
}
```

### 模板配置

**IDEA 类注释模板：**
File → Settings → Editor → File and Code Templates → Includes → File Header

```java
/**
 * ${description}
 *
 * @author ${USER}
 * @since ${DATE}
 */
```

---

## 方法注释

**【强制】所有的 public 方法必须添加 Javadoc 注释。**

### 标准格式

```java
/**
 * 根据用户ID获取用户信息
 * <p>
 * 优先从缓存获取，缓存未命中则查询数据库
 * </p>
 *
 * @param userId 用户ID，不能为空
 * @return 用户信息，不存在返回 null
 * @throws BusinessException 当用户ID为空时抛出
 */
public User getUserById(Long userId) {
    // ...
}

/**
 * 创建订单
 *
 * @param req 订单创建请求，包含商品ID、数量、收货地址等
 * @return 创建成功的订单ID
 * @throws BusinessException 当库存不足或商品不存在时抛出
 */
@Transactional(rollbackFor = Exception.class)
public Long createOrder(OrderCreateReq req) {
    // ...
}
```

### 常用 Javadoc 标签

| 标签 | 说明 | 示例 |
|------|------|------|
| `@param` | 参数说明 | `@param userId 用户ID` |
| `@return` | 返回值说明 | `@return 用户信息` |
| `@throws` | 异常说明 | `@throws BusinessException 当...时` |
| `@see` | 参见 | `@see UserService#getUser` |
| `@since` | 从哪个版本开始 | `@since 1.0.0` |
| `@deprecated` | 已废弃 | `@deprecated 使用 xxx 代替` |
| `@author` | 作者 | `@author zhangsan` |

### void 方法

```java
/**
 * 发送订单完成通知
 *
 * @param orderId 订单ID
 * @param userId 用户ID
 */
public void sendOrderCompleteNotification(Long orderId, Long userId) {
    // ...
}
```

### 废弃方法

```java
/**
 * 获取用户信息
 *
 * @param userId 用户ID
 * @return 用户信息
 * @deprecated 使用 {@link #getUserById(Long)} 代替，此方法将在 2.0 版本移除
 */
@Deprecated
public User getUser(Long userId) {
    return getUserById(userId);
}
```

---

## 字段注释

### 实体类字段

**【强制】实体类字段使用 Javadoc 注释，便于 IDE 提示。**

```java
@Data
@TableName("order")
public class Order extends BaseDO {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 订单号，唯一
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 订单金额（单位：元）
     */
    private BigDecimal amount;

    /**
     * 订单状态
     * @see OrderStatusEnum
     */
    private Integer status;

    /**
     * 支付时间
     */
    private LocalDateTime payTime;
}
```

### 常量注释

```java
public class OrderConstants {

    /**
     * 订单超时时间（分钟）
     */
    public static final int ORDER_TIMEOUT_MINUTES = 30;

    /**
     * 最大重试次数
     */
    public static final int MAX_RETRY_COUNT = 3;
}
```

---

## 行内注释

**【推荐】行内注释使用 `//`，放在代码上方或右侧。**

```java
public void processOrder(Order order) {
    // 1. 校验订单状态
    validateOrderStatus(order);

    // 2. 扣减库存
    reduceStock(order);

    // 3. 更新订单状态
    order.setStatus(OrderStatus.PAID.getCode());  // 修改为已支付
    orderMapper.updateById(order);

    // 4. 发送通知
    sendNotification(order);
}
```

### 复杂逻辑注释

```java
// 计算折扣：满100减20，满200减50，满300减100
// 优先使用大额优惠券
BigDecimal discount;
if (amount.compareTo(new BigDecimal("300")) >= 0) {
    discount = new BigDecimal("100");
} else if (amount.compareTo(new BigDecimal("200")) >= 0) {
    discount = new BigDecimal("50");
} else if (amount.compareTo(new BigDecimal("100")) >= 0) {
    discount = new BigDecimal("20");
} else {
    discount = BigDecimal.ZERO;
}
```

---

## TODO/FIXME 注释

**【推荐】待办事项使用 TODO，已知问题使用 FIXME。**

```java
// TODO: 后续需要添加缓存优化 - zhangsan 2026-01-15
public User getUserById(Long userId) {
    return userMapper.selectById(userId);
}

// FIXME: 并发情况下可能出现超卖，需要加分布式锁 - lisi 2026-01-20
public void reduceStock(Long productId, Integer quantity) {
    productMapper.reduceStock(productId, quantity);
}
```

### 格式规范

```java
// TODO: [描述] - [负责人] [日期]
// FIXME: [问题描述] - [负责人] [日期]
```

---

## 禁止的注释

### 无意义注释

```java
// ❌ 反例 - 无意义注释
// getter
public String getName() {
    return name;
}

// setter
public void setName(String name) {
    this.name = name;
}

// 构造方法
public User() {
}
```

### 过时注释

```java
// ❌ 反例 - 注释与代码不一致
// 获取用户名
public String getEmail() {  // 方法名已改，注释未更新
    return email;
}
```

### 注释掉的代码

**【强制】不要在代码中保留注释掉的代码，直接删除。版本控制系统会保留历史。**

```java
// ❌ 反例 - 注释掉的代码
public void process() {
    doA();
    // doB();  // 2025-12-01 暂时注释
    // doC();
    // if (condition) {
    //     doD();
    // }
    doE();
}

// ✅ 正例 - 删除无用代码
public void process() {
    doA();
    doE();
}
```

---

## 接口注释

**【推荐】接口方法使用 Javadoc，实现类可省略（@Override 已表明）。**

```java
public interface IOrderService {

    /**
     * 创建订单
     *
     * @param req 订单创建请求
     * @return 订单ID
     * @throws BusinessException 当库存不足时抛出
     */
    Long createOrder(OrderCreateReq req);

    /**
     * 取消订单
     *
     * @param orderId 订单ID
     * @param reason 取消原因
     */
    void cancelOrder(Long orderId, String reason);
}

@Service
public class OrderServiceImpl implements IOrderService {

    @Override
    public Long createOrder(OrderCreateReq req) {
        // 实现类不需要重复注释
    }

    @Override
    public void cancelOrder(Long orderId, String reason) {
        // ...
    }
}
```

---

## 枚举注释

```java
/**
 * 订单状态枚举
 *
 * @author zhangsan
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum OrderStatusEnum {

    /**
     * 待支付 - 订单创建后的初始状态
     */
    PENDING_PAYMENT(0, "待支付"),

    /**
     * 已支付 - 用户完成支付
     */
    PAID(1, "已支付"),

    /**
     * 已发货 - 商家已发货
     */
    SHIPPED(2, "已发货"),

    /**
     * 已完成 - 用户确认收货
     */
    COMPLETED(3, "已完成"),

    /**
     * 已取消 - 用户或系统取消订单
     */
    CANCELLED(4, "已取消");

    private final Integer code;
    private final String desc;
}
```

---

## 注释格式检查

### Checkstyle 配置

```xml
<!-- 类必须有 Javadoc -->
<module name="JavadocType">
    <property name="scope" value="public"/>
</module>

<!-- public 方法必须有 Javadoc -->
<module name="JavadocMethod">
    <property name="scope" value="public"/>
</module>

<!-- Javadoc 必须有描述 -->
<module name="JavadocStyle"/>
```

---

## 禁则速查

| ❌ 禁止 | ✅ 正确 | 原因 |
|--------|--------|------|
| 无类注释 | 添加作者和日期 | 便于追溯 |
| public 方法无注释 | 添加 Javadoc | API 文档化 |
| 注释解释"做什么" | 注释解释"为什么" | 代码自解释 |
| 注释与代码不一致 | 及时更新注释 | 误导维护者 |
| 保留注释代码 | 直接删除 | Git 有历史 |
| `//注释` | `// 注释` | 格式规范 |
