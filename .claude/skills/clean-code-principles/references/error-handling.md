# 错误处理 (Error Handling)

> 基于《Clean Code》第7章 - Robert C. Martin (by Michael Feathers)

## 核心思想

错误处理很重要，但如果它搞乱了代码逻辑，就是错误的做法。

**整洁代码是可读的，但也要强固。** 可读与强固并不冲突。如果将错误处理隔离看待，独立于主要逻辑之外，就能写出强固而整洁的代码。

---

## 规则 1：使用异常而非返回码 (Use Exceptions Rather Than Return Codes)

### 问题
返回码会搞乱调用者代码，调用者必须在调用之后即刻检查错误，这个步骤很容易被遗忘。

### ❌ 错误示例：使用返回码

```java
public class DeviceController {
    public void sendShutDown() {
        DeviceHandle handle = getHandle(DEV1);
        // 检查设备状态
        if (handle != DeviceHandle.INVALID) {
            // 保存设备状态到记录域
            retrieveDeviceRecord(handle);
            // 如果没有挂起，则关闭
            if (record.getStatus() != DEVICE_SUSPENDED) {
                pauseDevice(handle);
                clearDeviceWorkQueue(handle);
                closeDevice(handle);
            } else {
                logger.log("Device suspended. Unable to shut down");
            }
        } else {
            logger.log("Invalid handle for: " + DEV1.toString());
        }
    }
}
```

### ✅ 正确示例：使用异常

```java
public class DeviceController {
    public void sendShutDown() {
        try {
            tryToShutDown();
        } catch (DeviceShutDownError e) {
            logger.log(e);
        }
    }

    private void tryToShutDown() throws DeviceShutDownError {
        DeviceHandle handle = getHandle(DEV1);
        DeviceRecord record = retrieveDeviceRecord(handle);
        
        pauseDevice(handle);
        clearDeviceWorkQueue(handle);
        closeDevice(handle);
    }

    private DeviceHandle getHandle(DeviceID id) {
        // ...
        throw new DeviceShutDownError("Invalid handle for: " + id.toString());
    }
}
```

### 核心优势
- 代码更整洁
- 设备关闭算法和错误处理被隔离
- 可以分别理解每个关注点

---

## 规则 2：先写 Try-Catch-Finally 语句 (Write Your Try-Catch-Finally Statement First)

### 原则
- `try` 代码块就像是事务
- `catch` 代码块将程序维持在一种持续状态
- 先写 `try-catch-finally` 帮助定义代码用户应该期待什么

### TDD 方式

```java
// Step 1: 先写测试
@Test(expected = StorageException.class)
public void retrieveSectionShouldThrowOnInvalidFileName() {
    sectionStore.retrieveSection("invalid - file");
}

// Step 2: 创建占位代码
public List<RecordedGrip> retrieveSection(String sectionName) {
    return new ArrayList<RecordedGrip>();
}

// Step 3: 添加异常处理
public List<RecordedGrip> retrieveSection(String sectionName) {
    try {
        FileInputStream stream = new FileInputStream(sectionName);
    } catch (Exception e) {
        throw new StorageException("retrieval error", e);
    }
    return new ArrayList<RecordedGrip>();
}

// Step 4: 缩小异常范围
public List<RecordedGrip> retrieveSection(String sectionName) {
    try {
        FileInputStream stream = new FileInputStream(sectionName);
        stream.close();
    } catch (FileNotFoundException e) {
        throw new StorageException("retrieval error", e);
    }
    return new ArrayList<RecordedGrip>();
}
```

> 尝试编写强行抛出异常的测试，再往处理器中添加行为，使之满足测试要求。

---

## 规则 3：使用不可控异常 (Use Unchecked Exceptions)

### 核心论点
可控异常（Checked Exception）违反**开放/闭合原则**。

### 问题说明

```
┌─────────────────────────────────────────────────────────────┐
│  调用层级：                                                  │
│                                                             │
│  TopLevel.method()                                          │
│       ↓                                                     │
│  MiddleLevel.method()  ← 必须声明 throws                    │
│       ↓                                                     │
│  LowLevel.method()     ← 必须声明 throws                    │
│       ↓                                                     │
│  BottomLevel.method()  ← 抛出新的 checked exception         │
│                                                             │
│  结果：底层修改导致整条链路的签名都要修改！                   │
└─────────────────────────────────────────────────────────────┘
```

### 代价
- 底层修改波及高层签名
- 修改好的模块必须重新构建、发布
- 封装被打破：抛出路径中的每个函数都要了解底层异常细节

### 建议
- C#、Python、Ruby 都不支持可控异常，但仍能写出强固软件
- 对于关键代码库，可控异常有时有用
- 对于一般应用开发，依赖成本高于收益

---

## 规则 4：给出异常发生的环境说明 (Provide Context with Exceptions)

### 原则
抛出的每个异常，都应当提供足够的环境说明，以便判断错误的来源和处所。

### 应包含信息
- **失败的操作**
- **失败类型**
- 足够的信息用于日志记录

### 示例

```java
// ❌ 信息不足
throw new RuntimeException("Error occurred");

// ✅ 提供上下文
throw new OrderProcessingException(
    "Failed to process order #" + orderId + 
    " for customer " + customerId + 
    ": insufficient inventory for product " + productId,
    originalException
);
```

> 堆栈踪迹（stack trace）无法告诉你该失败操作的初衷，需要通过异常消息补充。

---

## 规则 5：依调用者需要定义异常类 (Define Exception Classes in Terms of a Caller's Needs)

### 原则
定义异常类时，最重要的考虑应该是**它们如何被捕获**。

### ❌ 错误示例：大量重复的异常处理

```java
ACMEPort port = new ACMEPort(12);

try {
    port.open();
} catch (DeviceResponseException e) {
    reportPortError(e);
    logger.log("Device response exception", e);
} catch (ATM1212UnlockedException e) {
    reportPortError(e);
    logger.log("Unlock exception", e);
} catch (GMXError e) {
    reportPortError(e);
    logger.log("Device response exception");
} finally {
    // ...
}
```

### ✅ 正确示例：打包第三方 API

```java
// 使用包装类
LocalPort port = new LocalPort(12);
try {
    port.open();
} catch (PortDeviceFailure e) {
    reportError(e);
    logger.log(e.getMessage(), e);
} finally {
    // ...
}

// 包装类定义
public class LocalPort {
    private ACMEPort innerPort;

    public LocalPort(int portNumber) {
        innerPort = new ACMEPort(portNumber);
    }

    public void open() {
        try {
            innerPort.open();
        } catch (DeviceResponseException e) {
            throw new PortDeviceFailure(e);
        } catch (ATM1212UnlockedException e) {
            throw new PortDeviceFailure(e);
        } catch (GMXError e) {
            throw new PortDeviceFailure(e);
        }
    }
}
```

### 打包第三方 API 的好处
1. **降低依赖**：未来可以不太痛苦地改用其他代码库
2. **便于测试**：容易模拟第三方调用
3. **定义舒适的 API**：不必绑死在特定厂商的 API 设计上

---

## 规则 6：定义常规流程 (Define the Normal Flow)

### 特例模式 (Special Case Pattern)

创建一个类或配置一个对象，用来处理特例。客户代码就不用应付异常行为了。

### ❌ 错误示例：异常打断业务逻辑

```java
try {
    MealExpenses expenses = expenseReportDAO.getMeals(employee.getID());
    m_total += expenses.getTotal();
} catch (MealExpensesNotFound e) {
    m_total += getMealPerDiem();
}
```

### ✅ 正确示例：使用特例模式

```java
// 简洁的业务逻辑
MealExpenses expenses = expenseReportDAO.getMeals(employee.getID());
m_total += expenses.getTotal();

// 特例对象处理异常情况
public class PerDiemMealExpenses implements MealExpenses {
    public int getTotal() {
        // 返回餐食补贴默认值
        return PER_DIEM_AMOUNT;
    }
}

// DAO 返回特例对象
public MealExpenses getMeals(Long employeeId) {
    MealExpenses expenses = findMeals(employeeId);
    if (expenses == null) {
        return new PerDiemMealExpenses();
    }
    return expenses;
}
```

---

## 规则 7：别返回 null 值 (Don't Return Null)

### 问题
返回 null 值是在给自己增加工作量，也是在给调用者添乱。只要有一处没检查 null 值，应用程序就会失控。

### ❌ 错误示例：到处检查 null

```java
public void registerItem(Item item) {
    if (item != null) {
        ItemRegistry registry = persistentStore.getItemRegistry();
        if (registry != null) {
            Item existing = registry.getItem(item.getID());
            if (existing.getBillingPeriod().hasRetailOwner()) {
                existing.register(item);
            }
        }
    }
}
```

### 解决方案

#### 方案 1：抛出异常

```java
public Item getItem(String id) {
    Item item = repository.findById(id);
    if (item == null) {
        throw new ItemNotFoundException("Item not found: " + id);
    }
    return item;
}
```

#### 方案 2：返回特例对象

```java
public Item getItem(String id) {
    Item item = repository.findById(id);
    if (item == null) {
        return Item.NULL_ITEM; // Null Object Pattern
    }
    return item;
}
```

#### 方案 3：返回空集合

```java
// ❌ 返回 null
public List<Employee> getEmployees() {
    if (noEmployees) {
        return null;  // 调用者必须检查 null
    }
    return employees;
}

// 调用代码必须检查
List<Employee> employees = getEmployees();
if (employees != null) {
    for (Employee e : employees) {
        totalPay += e.getPay();
    }
}

// ✅ 返回空集合
public List<Employee> getEmployees() {
    if (noEmployees) {
        return Collections.emptyList();
    }
    return employees;
}

// 调用代码更简洁
List<Employee> employees = getEmployees();
for (Employee e : employees) {
    totalPay += e.getPay();
}
```

---

## 规则 8：别传递 null 值 (Don't Pass Null)

### 原则
在方法中返回 null 值是糟糕的做法，但将 null 值传递给其他方法就更糟糕了。

### 问题

```java
public class MetricsCalculator {
    public double xProjection(Point p1, Point p2) {
        return (p2.x - p1.x) * 1.5;
    }
}

// 如果传入 null
calculator.xProjection(null, new Point(12, 13));
// 得到 NullPointerException
```

### 应对方案

#### 方案 1：抛出自定义异常

```java
public double xProjection(Point p1, Point p2) {
    if (p1 == null || p2 == null) {
        throw new InvalidArgumentException(
            "Invalid argument for MetricsCalculator.xProjection");
    }
    return (p2.x - p1.x) * 1.5;
}
```

#### 方案 2：使用断言

```java
public double xProjection(Point p1, Point p2) {
    assert p1 != null : "p1 should not be null";
    assert p2 != null : "p2 should not be null";
    return (p2.x - p1.x) * 1.5;
}
```

### 最佳实践
> 在大多数编程语言中，没有良好的方法能对付由调用者意外传入的 null 值。恰当的做法就是**禁止传入 null 值**。

---

## 总结对照表

| 规则 | 要点 |
|------|------|
| **使用异常而非返回码** | 异常分离业务逻辑和错误处理 |
| **先写 Try-Catch-Finally** | 定义事务范围，TDD 驱动 |
| **使用不可控异常** | 可控异常违反开放/闭合原则 |
| **给出环境说明** | 包含失败操作和失败类型 |
| **依调用者需要定义异常类** | 打包第三方 API，统一异常类型 |
| **定义常规流程** | 使用特例模式替代异常 |
| **别返回 null** | 返回空集合或特例对象 |
| **别传递 null** | 禁止传入 null 值 |

---

## 代码审查清单

### 异常使用
- [ ] 是否使用异常而非返回码处理错误？
- [ ] 是否先写 try-catch-finally 定义事务范围？
- [ ] 是否优先使用不可控异常（RuntimeException）？

### 异常信息
- [ ] 异常消息是否包含足够的上下文信息？
- [ ] 是否包含失败的操作和失败类型？

### 异常设计
- [ ] 是否根据调用者需要定义异常类？
- [ ] 是否打包了第三方 API 的异常？
- [ ] 是否使用特例模式处理边界情况？

### Null 处理
- [ ] 方法是否避免返回 null？
- [ ] 是否返回空集合而非 null？
- [ ] 是否使用特例对象替代 null？
- [ ] 是否禁止传入 null 参数？

---

## 核心箴言

> **错误处理很重要，但如果它搞乱了代码逻辑，就是错误的做法。**

> **整洁代码是可读的，但也要强固。可读与强固并不冲突。**

> **如果将错误处理隔离看待，独立于主要逻辑之外，就能写出强固而整洁的代码。**
