# 异常处理

> Java/Spring Boot 编码规范 - 异常处理

> **注意**：本规范提供通用原则和示例代码，各项目应根据自身情况调整具体实现。

---

## 基本原则

### 1. 统一异常体系

项目应建立统一的异常体系，而不是随意抛出 `RuntimeException` 或 `Exception`：

```java
/**
 * 统一业务异常基类（示例，项目可自行定义）
 */
@Getter
public class BusinessException extends RuntimeException {

    private final String code;
    private final String message;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }
}

/**
 * 错误码接口（示例）
 */
public interface ErrorCode {
    String getCode();
    String getMessage();
}
```

### 2. 推荐的异常抛出方式

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    // 方式一：使用预定义错误码（推荐）
    public Order getOrderById(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(OrderErrorCode.ORDER_NOT_EXISTS);
        }
        return order;
    }

    // 方式二：错误码 + 自定义消息
    public void updateOrderStatus(Long orderId, Integer status) {
        if (status < 0 || status > 10) {
            throw new BusinessException(
                GlobalErrorCode.BAD_REQUEST,
                "订单状态不合法"
            );
        }
    }

    // 方式三：带参数的错误消息
    public void cancelOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(
                OrderErrorCode.ORDER_NOT_EXISTS.getCode(),
                String.format("订单不存在，订单ID: %s", orderId)
            );
        }
    }
}
```

---

## 禁止的异常处理

| ❌ 禁止 | ✅ 正确 | 原因 |
|--------|--------|------|
| `throw new RuntimeException()` | `throw new BusinessException(错误码)` | 无错误码，前端无法处理 |
| `throw new Exception("msg")` | `throw new BusinessException(错误码)` | 太泛，难以分类处理 |
| `catch (Exception e) {}` | 记录日志后处理/抛出 | 吞没异常，难以排查 |
| `e.printStackTrace()` | `log.error("msg", e)` | 日志系统管理 |

```java
// ❌ 错误：直接抛 RuntimeException
if (order == null) {
    throw new RuntimeException("订单不存在");
}

// ❌ 错误：捕获异常不处理
try {
    doSomething();
} catch (Exception e) {
    // 什么都不做
}

// ✅ 正确：使用项目定义的业务异常
if (order == null) {
    throw new BusinessException(OrderErrorCode.ORDER_NOT_EXISTS);
}

// ✅ 正确：捕获后处理
try {
    doSomething();
} catch (Exception e) {
    log.error("[操作]，执行失败", e);
    throw new BusinessException(SystemErrorCode.OPERATION_FAILED);
}
```

---

## 错误码定义规范

### 错误码设计原则

| 原则 | 说明 |
|------|------|
| 唯一性 | 每个错误码全局唯一 |
| 可分类 | 按模块/系统分段，便于识别 |
| 可扩展 | 预留码段，方便后续添加 |
| 国际化支持 | 错误码与错误消息分离 |

### 错误码分段建议

```
格式：{模块码}.{错误类型码}.{具体错误码}

示例：
- 1001：订单模块 - 业务错误 - 订单不存在
- 2001：商品模块 - 业务错误 - 商品不存在
- 3001：用户模块 - 业务错误 - 用户不存在
- 4001：支付模块 - 系统错误 - 支付超时
- 5001：网关模块 - 第三方错误 - 调用失败
```

### 示例实现（仅供参考）

```java
/**
 * 订单模块错误码（示例）
 */
public enum OrderErrorCode implements ErrorCode {

    ORDER_NOT_EXISTS("1001", "订单不存在"),
    ORDER_STATUS_ERROR("1002", "订单状态错误"),
    ORDER_ALREADY_PAID("1003", "订单已支付"),
    ORDER_CANNOT_CANCEL("1004", "订单不能取消");

    private final String code;
    private final String message;

    OrderErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}

/**
 * 全局错误码（示例）
 */
public enum GlobalErrorCode implements ErrorCode {

    SUCCESS("0", "成功"),
    BAD_REQUEST("400", "请求参数错误"),
    UNAUTHORIZED("401", "未授权"),
    FORBIDDEN("403", "无权限"),
    NOT_FOUND("404", "资源不存在"),
    INTERNAL_ERROR("500", "系统内部错误");

    private final String code;
    private final String message;

    GlobalErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
```

---

## 全局异常处理器

```java
/**
 * 全局异常处理器（示例，项目需根据自身返回结构调整）
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 业务异常
     * 说明：捕获项目自定义的业务异常，返回错误码和消息
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("[业务异常]，错误码: {}，错误消息: {}", e.getCode(), e.getMessage());
        // Result 为项目统一的返回对象，需自行实现
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("[参数校验]，校验失败: {}", message);
        return Result.error(GlobalErrorCode.BAD_REQUEST.getCode(), message);
    }

    /**
     * 缺少请求参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<?> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("[参数校验]，缺少参数: {}", e.getParameterName());
        return Result.error(GlobalErrorCode.BAD_REQUEST.getCode(),
            "缺少参数: " + e.getParameterName());
    }

    /**
     * 系统异常（兜底处理）
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("[系统异常]，未捕获异常", e);
        return Result.error(GlobalErrorCode.INTERNAL_ERROR);
    }
}
```

### 异常处理器处理顺序

```java
// 异常处理按优先级从高到低：
// 1. 具体业务异常（如 BusinessException）
// 2. 框架异常（如 MethodArgumentNotValidException）
// 3. 更通用的异常（如 RuntimeException）
// 4. 兜底异常（Exception）

// 示例：多个异常处理器
@ExceptionHandler({BusinessException.class, ServiceException.class})
public Result<?> handleBusiness(BusinessException e) { ... }

@ExceptionHandler(SQLException.class, DataAccessException.class)
public Result<?> handleDatabase(Exception e) { ... }

@ExceptionHandler(Exception.class)
public Result<?> handleDefault(Exception e) { ... }
```

---

## 异常处理原则

### 1. 早失败，早返回

```java
// ✅ 好的做法：卫语句
public void updateOrder(Long orderId, OrderUpdateReq req) {
    // 校验订单存在
    Order order = orderMapper.selectById(orderId);
    if (order == null) {
        throw new BusinessException(OrderErrorCode.ORDER_NOT_EXISTS);
    }

    // 校验订单状态
    if (!canUpdate(order.getStatus())) {
        throw new BusinessException(OrderErrorCode.ORDER_STATUS_ERROR);
    }

    // 执行更新
    doUpdate(order, req);
}

// ❌ 差的做法：深层嵌套
public void updateOrder(Long orderId, OrderUpdateReq req) {
    Order order = orderMapper.selectById(orderId);
    if (order != null) {
        if (canUpdate(order.getStatus())) {
            doUpdate(order, req);
        } else {
            throw new BusinessException(OrderErrorCode.ORDER_STATUS_ERROR);
        }
    } else {
        throw new BusinessException(OrderErrorCode.ORDER_NOT_EXISTS);
    }
}
```

### 2. 异常转换为业务错误码

```java
// ✅ 正确：捕获系统异常，转换为业务错误码
public void processPayment(Long orderId) {
    try {
        paymentClient.pay(orderId);
    } catch (TimeoutException e) {
        log.error("[支付]，超时，订单ID: {}", orderId, e);
        throw new BusinessException(PaymentErrorCode.PAYMENT_TIMEOUT);
    } catch (PaymentException e) {
        log.error("[支付]，失败，订单ID: {}，原因: {}", orderId, e.getMessage());
        throw new BusinessException(PaymentErrorCode.PAYMENT_FAILED);
    }
}
```

### 3. 资源清理使用 finally

```java
InputStream is = null;
try {
    is = new FileInputStream(file);
    // 处理文件
} catch (IOException e) {
    log.error("[文件处理]，读取失败", e);
    throw new BusinessException(FileErrorCode.FILE_READ_FAILED);
} finally {
    if (is != null) {
        try {
            is.close();
        } catch (IOException e) {
            log.error("[文件处理]，关闭流失败", e);
        }
    }
}

// 更好的写法：使用 try-with-resources
try (InputStream is = new FileInputStream(file)) {
    // 处理文件
} catch (IOException e) {
    log.error("[文件处理]，读取失败", e);
    throw new BusinessException(FileErrorCode.FILE_READ_FAILED);
}
```

---

## 常见异常场景处理

### 空指针异常（NPE）预防

```java
// ✅ 好的做法：使用 Optional
String city = Optional.ofNullable(user)
    .map(User::getAddress)
    .map(Address::getCity)
    .orElse("");

// ✅ 好的做法：先判空
if (order != null && order.getItems() != null) {
    order.getItems().forEach(item -> {
        // 处理订单项
    });
}

// ✅ 好的做法：使用工具类
List<OrderItem> items = CollectionUtils.isEmpty(order.getItems())
    ? Collections.emptyList()
    : order.getItems();
```

### 数据库操作异常

```java
// ✅ 正确处理
@Transactional(rollbackFor = Exception.class)
public void createOrder(Order order) {
    try {
        orderMapper.insert(order);
        insertOrderItems(order.getItems());
    } catch (DuplicateKeyException e) {
        log.warn("[订单创建]，订单号重复，orderNo: {}", order.getOrderNo());
        throw new BusinessException(OrderErrorCode.ORDER_NO_DUPLICATE);
    } catch (Exception e) {
        log.error("[订单创建]，数据库异常，orderNo: {}", order.getOrderNo(), e);
        throw new BusinessException(SystemErrorCode.DATABASE_ERROR);
    }
}
```

### 外部调用异常

```java
// ✅ 正确处理：重试 + 降级
@Retryable(value = {TimeoutException.class}, maxAttempts = 3)
public PaymentRsp callPayment(PaymentReq req) {
    try {
        return paymentClient.pay(req);
    } catch (TimeoutException e) {
        log.warn("[支付]，超时，订单ID: {}，将重试", req.getOrderId());
        throw e;  // 触发重试
    } catch (FeignException e) {
        log.error("[支付]，调用失败，订单ID: {}，状态: {}",
            req.getOrderId(), e.status());
        // 降级处理
        return createOfflinePayment(req);
    }
}

@Recover
public PaymentRsp recover(TimeoutException e, PaymentReq req) {
    log.error("[支付]，重试失败，订单ID: {}，降级处理", req.getOrderId());
    return createOfflinePayment(req);
}
```

---

## 异常日志规范

| 异常类型 | 日志级别 | 是否堆栈 | 示例 |
|---------|---------|---------|------|
| 业务异常 | WARN | 否 | `log.warn("[业务]，订单不存在，订单ID: {}", orderId)` |
| 参数校验 | WARN | 否 | `log.warn("[参数]，校验失败: {}", message)` |
| 系统异常 | ERROR | 是 | `log.error("[系统]，数据库异常", e)` |
| 外部调用 | ERROR | 是 | `log.error("[外部]，支付系统异常", e)` |

---

## 自定义异常体系示例

> 以下代码仅供参考，各项目应基于自身架构设计实现。

```java
/**
 * 业务异常基类
 */
@Getter
public class BusinessException extends RuntimeException {

    private final String code;
    private final String message;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.message = message;
    }
}

/**
 * 错误码接口
 */
public interface ErrorCode {
    String getCode();
    String getMessage();
}

/**
 * 系统错误码实现
 */
@AllArgsConstructor
@Getter
public enum SystemErrorCode implements ErrorCode {

    SUCCESS("0", "成功"),
    BAD_REQUEST("400", "请求参数错误"),
    UNAUTHORIZED("401", "未授权"),
    FORBIDDEN("403", "无权限"),
    NOT_FOUND("404", "资源不存在"),
    INTERNAL_ERROR("500", "系统内部错误"),
    DATABASE_ERROR("5001", "数据库异常");

    private final String code;
    private final String message;
}
```

---

## 异常规范总结

| 规范项 | 要求 |
|--------|------|
| 异常体系 | 项目统一定义，不使用原生 RuntimeException/Exception |
| 错误码 | 全局唯一、分段管理、支持国际化 |
| 异常日志 | 业务异常 WARN 级别，系统异常 ERROR 级别并记录堆栈 |
| 异常处理 | 早返回、不吞没异常、正确转换异常类型 |
| 资源清理 | 优先使用 try-with-resources |
