# 日志规范

> Java/Spring Boot 编码规范 - 日志记录

---

## 基本要求

### 使用 SLF4J

使用 Lombok 的 `@Slf4j` 注解简化日志声明：

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    public void createOrder(OrderCreateReq req) {
        log.info("[订单创建]，开始创建订单，请求参数: {}", req);
    }
}
```

### 日志占位符

✅ 使用 `{}` 占位符，❌ 禁止字符串拼接：

```java
// ✅ 正确：使用占位符
log.info("[订单创建]，订单ID: {}，用户ID: {}", orderId, userId);

// ❌ 错误：字符串拼接
log.info("[订单创建]，订单ID: " + orderId + "，用户ID: " + userId);
```

**原因**：字符串拼接会无条件执行，包括字符串拼接操作；占位符只在日志级别启用时才格式化。

### 业务标识

日志必须包含业务名称，用中括号 `[]` 标识：

```java
log.info("[促销目录生成]，计划ID: {}，分区数量: {}", planId, zoneCount);
log.info("[订单支付]，订单号: {}，支付金额: {}", orderNo, amount);
log.info("[用户登录]，用户ID: {}，IP: {}", userId, ip);
```

### 敏感信息脱敏

敏感信息必须脱敏，禁止明文打印：

| 敏感信息 | 脱敏方式 | 示例 |
|---------|---------|------|
| 手机号 | 保留前3后4 | `138****5678` |
| 身份证 | 保留前6后4 | `110101199001****1234` |
| 银行卡 | 保留前4后4 | `6222****5678` |
| 密码 | 全部隐藏 | `******` |
| Token | 保留前8后8 | `eyJhbGc***OiJVTyJ9` |

```java
// ✅ 正确：脱敏
log.info("[用户注册]，手机号: {}，姓名: {}",
    DesensitizedUtil.mobilePhone(mobile),
    DesensitizedUtil.chineseName(name));

// ❌ 错误：明文打印
log.info("[用户注册]，手机号: {}，身份证: {}", mobile, idCard);
```

---

## 必须打印的节点

以下节点必须打印日志：

| 节点 | 级别 | 内容 |
|------|------|------|
| **业务方法入口** | INFO | 打印关键参数 |
| **数据库查询结果** | INFO | 特别是查询为空时 |
| **外部系统调用前** | INFO | Feign、HTTP、MQ |
| **外部系统调用后** | INFO | 返回结果或异常 |
| **数据库写操作** | INFO | 插入、更新、删除 |
| **异常情况** | ERROR | 必须包含堆栈 |

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final PaymentClient paymentClient;

    /**
     * 创建订单
     */
    public Long createOrder(OrderCreateReq req) {
        // 1. 方法入口
        log.info("[订单创建]，开始创建订单，用户ID: {}，商品ID: {}，数量: {}",
            req.getUserId(), req.getProductId(), req.getQuantity());

        // 2. 业务校验
        Order order = buildOrder(req);
        log.info("[订单创建]，订单信息: {}", order);

        // 3. 数据库写操作
        orderMapper.insert(order);
        log.info("[订单创建]，订单插入成功，订单ID: {}", order.getId());

        // 4. 调用外部系统前
        log.info("[订单创建]，准备调用支付系统，订单ID: {}，金额: {}",
            order.getId(), order.getAmount());

        // 5. 调用外部系统
        try {
            PaymentRsp rsp = paymentClient.createPayment(order.getId(), order.getAmount());
            log.info("[订单创建]，支付系统响应: {}", rsp);
        } catch (Exception e) {
            // 6. 异常情况
            log.error("[订单创建]，调用支付系统失败，订单ID: {}", order.getId(), e);
            throw exception(PAYMENT_CALL_FAILED);
        }

        return order.getId();
    }
}
```

---

## 日志级别

| 级别 | 使用场景 | 示例 |
|------|---------|------|
| **ERROR** | 错误异常，必须包含堆栈 | `log.error("msg", e)` |
| **WARN** | 警告信息，不影响业务 | 配置缺失使用默认值 |
| **INFO** | 关键业务流程 | 订单创建、支付成功 |
| **DEBUG** | 详细调试信息 | 方法入参、中间变量 |

### ERROR 规范

ERROR 级别日志**必须包含异常堆栈**：

```java
// ✅ 正确：包含异常
log.error("[订单支付]，支付失败，订单ID: {}", orderId, e);

// ❌ 错误：无异常堆栈
log.error("[订单支付]，支付失败，订单ID: {}，异常: {}", orderId, e.getMessage());
```

### WARN 规范

WARN 用于警告但不影响业务的情况：

```java
// 配置缺失使用默认值
String timeout = config.getTimeout();
if (StringUtils.isBlank(timeout)) {
    log.warn("[配置加载]，超时时间配置为空，使用默认值 30 秒");
    timeout = "30";
}

// 数据查询为空（不一定是错误）
List<Order> orders = orderMapper.selectByStatus(DELETED);
if (CollectionUtils.isEmpty(orders)) {
    log.warn("[数据查询]，查询已删除订单，结果为空");
}
```

---

## 日志格式

### 标准格式

```
[业务名称]，操作描述，关键信息: {}
```

### 常见场景

```java
// 查询操作
log.info("[商品查询]，商品ID: {}，查询结果: {}", productId, product);
log.info("[商品查询]，商品ID: {}，商品不存在", productId);

// 新增操作
log.info("[商品新增]，商品编码: {}，商品名称: {}", req.getCode(), req.getName());
log.info("[商品新增]，新增成功，商品ID: {}", productId);

// 修改操作
log.info("[商品修改]，商品ID: {}，修改内容: {}", productId, updateReq);
log.info("[商品修改]，修改成功，商品ID: {}", productId);

// 删除操作
log.info("[商品删除]，商品ID: {}", productId);
log.info("[商品删除]，删除成功，商品ID: {}", productId);

// 审批操作
log.info("[订单审批]，订单ID: {}，审批人: {}，审批结果: {}",
    orderId, approver, approved);
log.info("[订单审批]，审批失败，订单ID: {}，原因: {}", orderId, reason);
```

---

## 异常日志

### 业务异常

业务异常使用 WARN 级别，不需要堆栈：

```java
if (order == null) {
    log.warn("[订单查询]，订单不存在，订单ID: {}", orderId);
    throw exception(ORDER_NOT_EXISTS);
}
```

### 系统异常

系统异常使用 ERROR 级别，必须包含堆栈：

```java
try {
    // 调用外部系统
    paymentClient.pay(order);
} catch (Exception e) {
    log.error("[订单支付]，调用支付系统异常，订单ID: {}", orderId, e);
    throw exception(PAYMENT_SYSTEM_ERROR);
}
```

---

## 性能日志

### 耗时记录

对于可能耗时的操作，记录执行时间：

```java
long startTime = System.currentTimeMillis();
try {
    // 执行操作
    doSomething();
    long cost = System.currentTimeMillis() - startTime;
    log.info("[数据同步]，同步完成，耗时: {} ms", cost);
} catch (Exception e) {
    long cost = System.currentTimeMillis() - startTime;
    log.error("[数据同步]，同步失败，耗时: {} ms", cost, e);
}
```

### 慢查询预警

```java
long startTime = System.currentTimeMillis();
List<Order> orders = orderMapper.selectComplex(query);
long cost = System.currentTimeMillis() - startTime;

if (cost > 1000) {
    log.warn("[订单查询]，慢查询预警，耗时: {} ms，SQL: {}", cost, query);
} else {
    log.info("[订单查询]，查询完成，耗时: {} ms，结果数: {}", cost, orders.size());
}
```

---

## JSON 日志（可选）

对于复杂对象，使用 JSON 格式输出：

```java
log.info("[订单创建]，订单信息: {}", JsonUtils.toJson(order));
log.info("[请求参数]，请求内容: {}", JsonUtils.toJson(req));
```

---

## 日志配置建议

### Logback 配置示例

```xml
<configuration>
    <!-- 控制台输出 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- 文件输出 -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/app.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/app.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- 错误日志单独输出 -->
    <appender name="ERROR_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/error.log</file>
        <filter class="ch.qos.logback.classic.filter.LevelFilter">
            <level>ERROR</level>
            <onMatch>ACCEPT</onMatch>
            <onMismatch>DENY</onMismatch>
        </filter>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/error.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>90</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE" />
        <appender-ref ref="FILE" />
        <appender-ref ref="ERROR_FILE" />
    </root>
</configuration>
```

---

## 日志规范速查表

| 场景 | 级别 | 格式 | 备注 |
|------|------|------|------|
| 方法入口 | INFO | `[业务]，操作描述，参数: {}` | 关键参数 |
| 查询为空 | INFO/WARN | `[业务]，查询结果为空，条件: {}` | 记录查询条件 |
| DB 写操作 | INFO | `[业务]，插入/更新/删除成功，ID: {}` | 记录主键 |
| 外部调用前 | INFO | `[业务]，调用XXX，请求: {}` | 记录请求 |
| 外部调用后 | INFO | `[业务]，XXX响应: {}` | 记录响应 |
| 业务异常 | WARN | `[业务]，业务异常，原因: {}` | 不需要堆栈 |
| 系统异常 | ERROR | `[业务]，系统异常，描述: {}` | 必须有堆栈 |
| 慢查询 | WARN | `[业务]，慢查询预警，耗时: {} ms` | 超过阈值 |
