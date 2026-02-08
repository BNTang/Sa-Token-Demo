# 边界 (Boundaries)

> 基于《Clean Code》第8章 - Robert C. Martin (by James Grenning)

## 核心思想

我们很少控制系统中的全部软件。有时我们购买第三方程序包或使用开放源代码，有时我们依靠公司中其他团队打造组件或子系统。不管是哪种情况，我们都得将外来代码干净利落地整合进自己的代码中。

**依靠你能控制的东西，好过依靠你控制不了的东西，免得日后受它控制。**

---

## 规则 1：封装第三方代码 (Encapsulate Third-Party Code)

### 问题
在接口提供者和使用者之间，存在与生俱来的张力：
- **提供者**：追求普适性，吸引广泛用户
- **使用者**：想要集中满足特定需求的接口

### 以 Map 为例

#### ❌ 直接使用 Map（有风险）

```java
// 原始方式 - 需要类型转换
Map sensors = new HashMap();
Sensor s = (Sensor) sensors.get(sensorId);

// 使用泛型 - 稍好但仍有问题
Map<String, Sensor> sensors = new HashMap<String, Sensor>();
Sensor s = sensors.get(sensorId);
```

**问题**：
- `Map` 提供了超出所需的功能（如 `clear()` 方法）
- 无法约束存入的对象类型
- 当 Map 接口变动时，所有使用处都要修改
- Java 5 加入泛型支持时，确实发生了大量修改

#### ✅ 封装边界接口

```java
public class Sensors {
    private Map sensors = new HashMap();

    public Sensor getById(String id) {
        return (Sensor) sensors.get(id);
    }

    public void add(String id, Sensor sensor) {
        sensors.put(id, sensor);
    }
    
    // 不暴露 clear()、remove() 等危险方法
}
```

### 封装的好处
- 边界接口（Map）是隐藏的
- 能随应用程序其他部分的极小影响而变动
- 泛型使用不再是大问题，转换在内部处理
- 接口经过修整以适应应用程序需要
- 代码易于理解、难以被误用
- 可以强制执行设计和业务规则

### 核心原则

```
┌─────────────────────────────────────────────────────────────┐
│  不要将 Map（或任何边界接口）在系统中传递                     │
│                                                             │
│  ✓ 把边界接口保留在类或近亲类中                              │
│  ✓ 避免从公共 API 中返回边界接口                            │
│  ✓ 避免将边界接口作为参数传递给公共 API                      │
└─────────────────────────────────────────────────────────────┘
```

---

## 规则 2：学习性测试 (Learning Tests)

### 问题
- 学习第三方代码很难
- 整合第三方代码也很难
- 同时做这两件事难上加难

### 解决方案
不要在生产代码中试验新东西，而是编写测试来遍览和理解第三方代码。

### 学习 log4j 的例子

```java
// Step 1: 第一次尝试
@Test
public void testLogCreate() {
    Logger logger = Logger.getLogger("MyLogger");
    logger.info("hello");
}
// 结果：错误 - 需要 Appender

// Step 2: 添加 Appender
@Test
public void testLogAddAppender() {
    Logger logger = Logger.getLogger("MyLogger");
    ConsoleAppender appender = new ConsoleAppender();
    logger.addAppender(appender);
    logger.info("hello");
}
// 结果：错误 - Appender 没有输出流

// Step 3: 完整配置
@Test
public void testLogAddAppender() {
    Logger logger = Logger.getLogger("MyLogger");
    logger.removeAllAppenders();
    logger.addAppender(new ConsoleAppender(
        new PatternLayout("%p %t %m%n"),
        ConsoleAppender.SYSTEM_OUT));
    logger.info("hello");
}
// 结果：成功！
```

### 最终的学习性测试

```java
public class LogTest {
    private Logger logger;

    @Before
    public void initialize() {
        logger = Logger.getLogger("logger");
        logger.removeAllAppenders();
        Logger.getRootLogger().removeAllAppenders();
    }

    @Test
    public void basicLogger() {
        BasicConfigurator.configure();
        logger.info("basicLogger");
    }

    @Test
    public void addAppenderWithStream() {
        logger.addAppender(new ConsoleAppender(
            new PatternLayout("%p %t %m%n"),
            ConsoleAppender.SYSTEM_OUT));
        logger.info("addAppenderWithStream");
    }

    @Test
    public void addAppenderWithoutStream() {
        logger.addAppender(new ConsoleAppender(
            new PatternLayout("%p %t %m%n")));
        logger.info("addAppenderWithoutStream");
    }
}
```

---

## 规则 3：学习性测试的价值 (Learning Tests Are Better Than Free)

### 好处

| 方面 | 价值 |
|------|------|
| **零成本** | 无论如何都要学习 API，测试只是更好的学习方式 |
| **精确试验** | 帮助增进对 API 的理解 |
| **正面投资回报** | 第三方升级时，运行测试检查行为变化 |
| **兼容性验证** | 确保第三方包按我们想要的方式工作 |
| **变更预警** | 第三方修改与测试不兼容时，马上发现 |
| **迁移支持** | 有边界测试支持，更容易升级版本 |

### 边界测试策略

```
┌─────────────────────────────────────────────────────────────┐
│  边界测试 = 与生产代码调用方式一致的输出测试                  │
│                                                             │
│  作用：                                                     │
│  • 验证 API 行为符合预期                                    │
│  • 第三方升级时快速发现不兼容                                │
│  • 减轻迁移劳力，避免长久绑在旧版本                          │
└─────────────────────────────────────────────────────────────┘
```

---

## 规则 4：使用尚不存在的代码 (Using Code That Does Not Yet Exist)

### 场景
另一种边界：将已知和未知分隔开的边界。边界那边是未知的（至少目前未知）。

### 案例：Transmitter（发送机）

子系统开发者还没有定义接口，但我们不想被阻碍。

### 解决方案：定义自己想要的接口

```java
// 我们定义自己想要的接口
public interface Transmitter {
    void transmit(Frequency frequency, DataStream stream);
}

// CommunicationsController 使用我们的接口
public class CommunicationsController {
    private Transmitter transmitter;
    
    public void sendData(Frequency freq, DataStream data) {
        transmitter.transmit(freq, data);
    }
}
```

### 当真正的 API 定义后

```java
// 编写 Adapter 桥接
public class TransmitterAdapter implements Transmitter {
    private RealTransmitterAPI realApi;
    
    public TransmitterAdapter(RealTransmitterAPI api) {
        this.realApi = api;
    }
    
    @Override
    public void transmit(Frequency frequency, DataStream stream) {
        // 转换为真正 API 的调用方式
        realApi.configure(frequency.toHz());
        realApi.send(stream.getBytes());
    }
}
```

### 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  CommunicationsController                                   │
│         │                                                   │
│         ▼                                                   │
│    Transmitter (我们定义的接口)                             │
│         │                                                   │
│         ▼                                                   │
│  TransmitterAdapter (ADAPTER 模式)                          │
│         │                                                   │
│         ▼                                                   │
│  RealTransmitterAPI (第三方/其他团队的 API)                 │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 好处
1. **接口在我们控制之下**
2. **客户代码更可读**，集中于该完成的工作
3. **隔离变化**，API 变动时只需修改 Adapter
4. **便于测试**，使用 `FakeTransmitter` 测试 Controller

---

## 规则 5：整洁的边界 (Clean Boundaries)

### 核心原则

**边界上会发生有趣的事。改动是其中之一。**

良好的软件设计，无需巨大投入和重写即可进行修改。

### 边界管理策略

| 策略 | 说明 | 示例 |
|------|------|------|
| **包装 (Wrap)** | 将边界接口封装在自己的类中 | `Sensors` 包装 `Map` |
| **适配 (Adapt)** | 用 Adapter 转换接口 | `TransmitterAdapter` |
| **最小化引用** | 代码中尽量少处引用第三方 | 集中在一个包/模块 |
| **测试保护** | 定义期望的边界测试 | 学习性测试 |

### 边界代码需要

```
┌─────────────────────────────────────────────────────────────┐
│  1. 清晰的分割                                               │
│  2. 定义期望的测试                                           │
│  3. 避免代码过多了解第三方特定信息                           │
│  4. 依靠能控制的东西                                         │
└─────────────────────────────────────────────────────────────┘
```

---

## 实践模式总结

### 模式 1：封装模式 (Wrapper Pattern)

```java
// 第三方接口功能过多
public class EmailService {
    private ThirdPartyEmailClient client;  // 边界接口隐藏

    public void sendEmail(String to, String subject, String body) {
        // 只暴露需要的功能
        client.send(new Email(to, subject, body));
    }
    
    // 不暴露 client.deleteAllEmails() 等危险方法
}
```

### 模式 2：适配器模式 (Adapter Pattern)

```java
// 我们的接口
public interface PaymentGateway {
    PaymentResult charge(Money amount, CreditCard card);
}

// 适配第三方
public class StripeAdapter implements PaymentGateway {
    private StripeClient stripe;
    
    @Override
    public PaymentResult charge(Money amount, CreditCard card) {
        StripeCharge charge = stripe.createCharge(
            amount.toCents(),
            card.getToken()
        );
        return new PaymentResult(charge.isSuccessful());
    }
}
```

### 模式 3：门面模式 (Facade Pattern)

```java
// 简化复杂的第三方 API
public class CloudStorageFacade {
    private S3Client s3;
    private CloudFrontClient cdn;
    private IAMClient iam;
    
    public String uploadAndPublish(File file) {
        String key = s3.upload(file);
        cdn.invalidate(key);
        return cdn.getPublicUrl(key);
    }
}
```

---

## 代码审查清单

### 第三方代码使用
- [ ] 是否封装了边界接口，而非直接传递？
- [ ] 是否只暴露应用程序需要的功能？
- [ ] 是否避免从公共 API 返回第三方类型？

### 学习性测试
- [ ] 是否为新的第三方库编写了学习性测试？
- [ ] 是否有边界测试验证第三方行为？
- [ ] 第三方升级时能否快速验证兼容性？

### 接口设计
- [ ] 对于未定义的外部接口，是否定义了自己想要的接口？
- [ ] 是否使用 Adapter 模式隔离第三方变化？
- [ ] 第三方变动时，修改点是否最小化？

### 依赖管理
- [ ] 是否尽量少处引用第三方边界接口？
- [ ] 边界代码是否有清晰的分割？
- [ ] 是否依靠能控制的接口而非第三方接口？

---

## 核心箴言

> **依靠你能控制的东西，好过依靠你控制不了的东西，免得日后受它控制。**

> **边界上的代码需要清晰的分割和定义了期望的测试。**

> **应该避免我们的代码过多地了解第三方代码中的特定信息。**

> **学习性测试毫无成本。无论如何我们都得学习要使用的 API，而编写测试则是获得这些知识的容易而不会影响其他工作的途径。**
