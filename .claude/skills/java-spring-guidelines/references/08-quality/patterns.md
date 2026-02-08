# 设计模式

> Java/Spring Boot 编码规范 - 设计模式

---

## 策略模式

### 适用场景

多分支业务逻辑，如支付方式、导出格式、计算规则等。

### 示例：支付方式

```java
/**
 * 支付策略接口
 */
public interface PaymentStrategy {

    /**
     * 创建支付
     */
    PaymentResult createPayment(PaymentRequest request);

    /**
     * 获取支付方式
     */
    PaymentType getType();
}

/**
 * 支付宝支付策略
 */
@Component("alipayStrategy")
public class AlipayStrategy implements PaymentStrategy {

    @Override
    public PaymentResult createPayment(PaymentRequest request) {
        // 调用支付宝接口
        return AlipayClient.create(request);
    }

    @Override
    public PaymentType getType() {
        return PaymentType.ALIPAY;
    }
}

/**
 * 微信支付策略
 */
@Component("wechatPayStrategy")
public class WechatPayStrategy implements PaymentStrategy {

    @Override
    public PaymentResult createPayment(PaymentRequest request) {
        // 调用微信支付接口
        return WechatPayClient.create(request);
    }

    @Override
    public PaymentType getType() {
        return PaymentType.WECHAT;
    }
}

/**
 * 支付策略工厂
 */
@Service
@RequiredArgsConstructor
public class PaymentStrategyFactory {

    private final Map<String, PaymentStrategy> strategyMap;

    @PostConstruct
    public void init() {
        // Spring 会自动注入所有 PaymentStrategy 实现
        strategyMap = new HashMap<>();
        strategyMap.put("ALIPAY", alipayStrategy);
        strategyMap.put("WECHAT", wechatPayStrategy);
    }

    public PaymentStrategy getStrategy(PaymentType type) {
        PaymentStrategy strategy = strategyMap.get(type.name());
        if (strategy == null) {
            throw exception(PAYMENT_TYPE_NOT_SUPPORTED);
        }
        return strategy;
    }
}

/**
 * 支付服务
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentStrategyFactory strategyFactory;

    public PaymentResult pay(PaymentRequest request) {
        // 根据支付方式获取策略
        PaymentStrategy strategy = strategyFactory.getStrategy(request.getType());
        return strategy.createPayment(request);
    }
}
```

---

## 模板方法模式

### 适用场景

流程固定但步骤可变，如导出流程、审批流程。

### 示例：数据导出

```java
/**
 * 导出模板
 */
public abstract class ExportTemplate {

    /**
     * 导出流程（模板方法）
     */
    public final void export(ExportRequest request, HttpServletResponse response) {
        // 1. 参数校验
        validateRequest(request);

        // 2. 查询数据
        List<?> data = queryData(request);

        // 3. 转换数据
        List<?> converted = convertData(data);

        // 4. 生成文件
        byte[] fileBytes = generateFile(converted);

        // 5. 写入响应
        writeFile(response, fileBytes);
    }

    /**
     * 参数校验（子类实现）
     */
    protected abstract void validateRequest(ExportRequest request);

    /**
     * 查询数据（子类实现）
     */
    protected abstract List<?> queryData(ExportRequest request);

    /**
     * 转换数据（子类可选）
     */
    protected List<?> convertData(List<?> data) {
        return data;  // 默认不转换
    }

    /**
     * 生成文件（子类实现）
     */
    protected abstract byte[] generateFile(List<?> data);

    /**
     * 写入响应（通用实现）
     */
    private void writeFile(HttpServletResponse response, byte[] bytes) {
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=export.xlsx");
        try {
            response.getOutputStream().write(bytes);
        } catch (IOException e) {
            throw exception(EXPORT_FAILED);
        }
    }
}

/**
 * 商品导出
 */
@Service
public class ProductExport extends ExportTemplate {

    @Autowired
    private ProductMapper productMapper;

    @Override
    protected void validateRequest(ExportRequest request) {
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw exception(EXPORT_DATE_RANGE_REQUIRED);
        }
    }

    @Override
    protected List<Product> queryData(ExportRequest request) {
        return productMapper.selectByDateRange(request.getStartDate(), request.getEndDate());
    }

    @Override
    protected byte[] generateFile(List<?> data) {
        return EasyExcel.write().build().excel(new ProductDTO());
    }
}

/**
 * 订单导出
 */
@Service
public class OrderExport extends ExportTemplate {

    @Autowired
    private OrderMapper orderMapper;

    @Override
    protected void validateRequest(ExportRequest request) {
        // 订单导出不需要日期范围
    }

    @Override
    protected List<Order> queryData(ExportRequest request) {
        return orderMapper.selectList(new LambdaQueryWrapper<Order>()
            .eq(Order::getUserId, request.getUserId())
        );
    }

    @Override
    protected byte[] generateFile(List<?> data) {
        return generateCsv(data);
    }
}
```

---

## 责任链模式

### 适用场景

多级审批、多步校验、处理器链。

### 示例：审批流程

```java
/**
 * 审批处理器接口
 */
public interface ApprovalHandler {

    /**
     * 处理审批
     */
    ApprovalResult handle(ApprovalRequest request);

    /**
     * 设置下一个处理器
     */
    ApprovalHandler setNext(ApprovalHandler handler);
}

/**
 * 抽象审批处理器
 */
public abstract class AbstractApprovalHandler implements ApprovalHandler {

    protected ApprovalHandler next;

    @Override
    public ApprovalHandler setNext(ApprovalHandler handler) {
        this.next = handler;
        return handler;
    }

    /**
     * 模板方法
     */
    @Override
    public ApprovalResult handle(ApprovalRequest request) {
        // 1. 当前处理
        ApprovalResult result = doHandle(request);

        // 2. 如果未完成且还有下一个处理器，继续
        if (!result.isCompleted() && next != null) {
            return next.handle(request);
        }

        return result;
    }

    /**
     * 子类实现具体处理逻辑
     */
    protected abstract ApprovalResult doHandle(ApprovalRequest request);
}

/**
 * 部门主管审批
 */
@Component
public class ManagerApprovalHandler extends AbstractApprovalHandler {

    @Override
    protected ApprovalResult doHandle(ApprovalRequest request) {
        if (request.getAmount() < new BigDecimal("5000")) {
            // 主管直接通过
            return ApprovalResult.approved("部门主管审批通过");
        }
        // 超过权限，传递给下一个
        return ApprovalResult.pending("待总监审批");
    }
}

/**
 * 总监审批
 */
@Component
public class DirectorApprovalHandler extends AbstractApprovalHandler {

    @Override
    protected ApprovalResult doHandle(ApprovalRequest request) {
        if (request.getAmount() < new BigDecimal("50000")) {
            return ApprovalResult.approved("总监审批通过");
        }
        return ApprovalResult.pending("待总经理审批");
    }
}

/**
 * 总经理审批
 */
@Component
public class GManagerApprovalHandler extends AbstractApprovalHandler {

    @Override
    protected ApprovalResult doHandle(ApprovalRequest request) {
        // 总经理最终审批
        return ApprovalResult.approved("总经理审批通过");
    }
}

/**
 * 审批服务
 */
@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final ManagerApprovalHandler managerHandler;
    private final DirectorApprovalHandler directorHandler;
    private final GManagerApprovalHandler gmHandler;

    @PostConstruct
    public void initChain() {
        // 构建责任链
        managerHandler.setNext(directorHandler).setNext(gmHandler);
    }

    public ApprovalResult approve(ApprovalRequest request) {
        return managerHandler.handle(request);
    }
}
```

---

## 工厂模式

### 示例：导出工厂

```java
/**
 * 导出类型枚举
 */
@Getter
@AllArgsConstructor
public enum ExportType {

    EXCEL("excel", "Excel 导出"),
    CSV("csv", "CSV 导出"),
    PDF("pdf", "PDF 导出");

    private final String code;
    private final String desc;

    public static ExportType getByCode(String code) {
        return Arrays.stream(values())
            .filter(e -> e.getCode().equals(code))
            .findFirst()
            .orElse(EXCEL);
    }
}

/**
 * 导出器接口
 */
public interface Exporter {

    byte[] export(List<?> data);

    String getContentType();
}

/**
 * Excel 导出器
 */
@Component("excelExporter")
public class ExcelExporter implements Exporter {

    @Override
    public byte[] export(List<?> data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EasyExcel.write(out).build().excel(data);
        return out.toByteArray();
    }

    @Override
    public String getContentType() {
        return "application/vnd.ms-excel";
    }
}

/**
 * 导出工厂
 */
@Component
@RequiredArgsConstructor
public class ExporterFactory {

    private final Map<String, Exporter> exporterMap;

    @PostConstruct
    public void init() {
        // Spring 自动注入所有 Exporter
        exporterMap = new HashMap<>();
        exporterMap.put("excel", excelExporter);
        exporterMap.put("csv", csvExporter);
        exporterMap.put("pdf", pdfExporter);
    }

    public Exporter getExporter(ExportType type) {
        Exporter exporter = exporterMap.get(type.getCode());
        if (exporter == null) {
            throw exception(EXPORT_TYPE_NOT_SUPPORTED);
        }
        return exporter;
    }
}
```

---

## 建造者模式

### 示例：复杂查询构建

```java
/**
 * 查询建造者
 */
public class QueryBuilder {

    private final LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();

    public static QueryBuilder create() {
        return new QueryBuilder();
    }

    public QueryBuilder name(String name) {
        if (StringUtils.isNotBlank(name)) {
            wrapper.like(Product::getName, name);
        }
        return this;
    }

    public QueryBuilder status(Integer status) {
        if (status != null) {
            wrapper.eq(Product::getStatus, status);
        }
        return this;
    }

    public QueryBuilder priceRange(BigDecimal min, BigDecimal max) {
        if (min != null) {
            wrapper.ge(Product::getPrice, min);
        }
        if (max != null) {
            wrapper.le(Product::getPrice, max);
        }
        return this;
    }

    public QueryBuilder orderByCreateTime(boolean desc) {
        if (desc) {
            wrapper.orderByDesc(Product::getCreateTime);
        } else {
            wrapper.orderByAsc(Product::getCreateTime);
        }
        return this;
    }

    public LambdaQueryWrapper<Product> build() {
        return wrapper;
    }
}

// 使用
public List<Product> search(String name, Integer status, BigDecimal minPrice, BigDecimal maxPrice) {
    LambdaQueryWrapper<Product> wrapper = QueryBuilder.create()
        .name(name)
        .status(status)
        .priceRange(minPrice, maxPrice)
        .orderByCreateTime(true)
        .build();

    return productMapper.selectList(wrapper);
}
```

---

## 观察者模式（Observer Pattern)

### 定义

定义对象间的一对多依赖关系，当一个对象状态改变时，所有依赖者都会收到通知并自动更新。

### 适用场景

- 事件驱动系统
- 消息订阅发布
- 状态变更通知
- 日志记录、监控

### Spring Event实现（推荐）

```java
/**
 * 订单创建事件
 */
@Getter
public class OrderCreatedEvent extends ApplicationEvent {
    
    private final Long orderId;
    private final Long userId;
    private final BigDecimal amount;
    
    public OrderCreatedEvent(Object source, Long orderId, Long userId, BigDecimal amount) {
        super(source);
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
    }
}

/**
 * 发布者（Subject）
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements IOrderService {
    
    private final OrderMapper orderMapper;
    private final ApplicationEventPublisher eventPublisher;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(OrderCreateDTO dto) {
        // 1. 创建订单
        OrderDO order = convertToEntity(dto);
        orderMapper.insert(order);
        
        // 2. 发布事件
        eventPublisher.publishEvent(new OrderCreatedEvent(
            this, order.getId(), order.getUserId(), order.getAmount()
        ));
        
        return order.getId();
    }
}

/**
 * 观察者 1：发送通知
 */
@Component
@Slf4j
public class OrderNotificationListener {
    
    @Async
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("[订单通知] 订单创建: orderId={}, userId={}", 
                 event.getOrderId(), event.getUserId());
        
        // 发送短信/邮件
        sendNotification(event);
    }
}

/**
 * 观察者 2：积分奖励
 */
@Component
@Slf4j
public class OrderPointsListener {
    
    @Async
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("[积分奖励] 订单创建: orderId={}, amount={}", 
                 event.getOrderId(), event.getAmount());
        
        // 计算并发放积分
        awardPoints(event.getUserId(), event.getAmount());
    }
}
```

---

## 代理模式（Proxy Pattern）

### 定义

为其他对象提供一个代理，以控制对这个对象的访问。

### 适用场景

- 远程代理（RPC）
- 虚拟代理（延迟加载）
- 保护代理（权限控制）
- 日志记录、性能监控
- AOP 切面

### AOP代理（推荐）

```java
/**
 * 日志切面（最常用的代理模式）
 */
@Aspect
@Component
@Slf4j
public class LoggingAspect {
    
    /**
     * 环绕通知（完全控制方法执行）
     */
    @Around("execution(* com.example.service..*.*(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 前置处理
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        
        log.info("[AOP代理] 调用方法: {}.{}, 参数: {}", className, methodName, args);
        long startTime = System.currentTimeMillis();
        
        try {
            // 执行目标方法
            Object result = joinPoint.proceed();
            
            // 后置处理
            long cost = System.currentTimeMillis() - startTime;
            log.info("[AOP代理] 方法返回: {}, 耗时: {}ms", result, cost);
            
            return result;
            
        } catch (Throwable ex) {
            // 异常处理
            long cost = System.currentTimeMillis() - startTime;
            log.error("[AOP代理] 方法异常, 耗时: {}ms", cost, ex);
            throw ex;
        }
    }
}
```

---

## 工厂模式对比

### 简单工厂模式

```java
/**
 * 简单工厂（静态工厂）
 */
public class PaymentChannelFactory {
    
    public static PaymentChannel create(String type) {
        switch (type) {
            case "alipay":
                return new AlipayChannel();
            case "wechat":
                return new WechatChannel();
            default:
                throw new IllegalArgumentException("不支持的支付方式: " + type);
        }
    }
}
```

### 工厂方法模式

```java
/**
 * 抽象工厂
 */
public interface PaymentChannelFactory {
    PaymentChannel create();
}

/**
 * 具体工厂
 */
public class AlipayChannelFactory implements PaymentChannelFactory {
    @Override
    public PaymentChannel create() {
        return new AlipayChannel();
    }
}
```

### 抽象工厂模式

```java
/**
 * 抽象工厂（产品族）
 */
public interface UIFactory {
    Button createButton();
    Dialog createDialog();
}

/**
 * 具体工厂
 */
public class WindowsUIFactory implements UIFactory {
    @Override
    public Button createButton() {
        return new WindowsButton();
    }
    
    @Override
    public Dialog createDialog() {
        return new WindowsDialog();
    }
}
```

### 三种工厂模式对比

| 模式 | 结构复杂度 | 扩展性 | 适用场景 |
|------|-----------|--------|---------|
| **简单工厂** | ⭐ | ⭐⭐ | 产品种类少且固定 |
| **工厂方法** | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 产品种类可能增加 |
| **抽象工厂** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | 多个产品族，产品族内部关联 |

**【推荐】选择原则**：
- 产品种类少（<5个）→ 简单工厂
- 产品种类可能增加 → 工厂方法
- 多个产品族，需要保证一致性 → 抽象工厂

---

## 设计模式速查表

| 模式 | 适用场景 | 优点 |
|------|----------|------|
| **策略模式** | 多分支业务逻辑 | 消除 if-else，易扩展 |
| **模板方法** | 流程固定，步骤可变 | 复用流程，灵活步骤 |
| **责任链** | 多级审批、多步校验 | 解耦处理流程 |
| **观察者** | 事件驱动 | 解耦发布订阅 |
| **代理** | AOP、权限、缓存、日志 | 增强功能，不修改原类 |
| **简单工厂** | 产品种类少且固定 | 简单易用 |
| **工厂方法** | 产品种类可能增加 | 符合开闭原则 |
| **抽象工厂** | 多个产品族 | 保证产品族一致性 |
| **建造者** | 复杂对象构建 | 流程清晰，易维护 |
