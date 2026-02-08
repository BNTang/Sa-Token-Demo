---
name: refactoring
description: 代码重构最佳实践。基于《重构》Martin Fowler 著、熊节译。涵盖重构原则、坏味道识别、重构手法、测试驱动重构。包括何时重构、重构步骤、安全策略、常见陷阱。当需要改善代码设计、消除代码坏味道、优化代码结构时使用。
metadata:
  author: skill-hub
  version: "1.0"
  compatibility: 所有编程语言
  reference: "《重构》- Martin Fowler（熊节 译）"
---

# 代码重构

> 版本: 1.0 | 更新: 2026-02-05
>
> 改善现有代码的设计，而不改变其行为
>
> "重构是对软件内部结构的一种调整，目的是在不改变软件可观察行为的前提下，提高其可理解性、降低其修改成本。" —— Martin Fowler

---

## 概述

### 做了什么

提供一套系统的代码改善方法，通过小幅步骤安全地修改代码结构，提升代码质量而不改变功能。

### 为什么需要

| 痛点 | 后果 |
|------|------|
| 代码难以理解 | 修改耗时，容易引入 bug |
| 代码充满坏味道 | 技术债务累积，开发速度变慢 |
| 不敢修改旧代码 | 系统腐化，最终推倒重来 |
| 重复代码遍布 | 一处修改，多处遗漏 |

### 什么时候必须用

- 代码评审发现坏味道时
- 添加功能前（先重构，再添加）
- 修复 bug 前（先重构，让 bug 显露）
- 代码变复杂时（持续小步重构）

---

## 核心原则

### 两顶帽子

> "重构和添加功能是两个独立的活动，任何时候都应该只戴其中一顶帽子。"

| 活动 | 目标 | 何时 |
|------|------|------|
| **添加功能** | 增加新能力 | 需求驱动 |
| **重构** | 调整结构 | 坏味道驱动 |

**关键**：不要同时做这两件事。

### 重构的定义

```
重构 = 修改代码结构 + 不改变行为 + 通过测试验证
```

### 为什么要重构

| 理由 | 说明 |
|------|------|
| **保持代码整洁** | 技术债务会像破窗效应一样扩散 |
| **提高编程速度** | 好的代码更容易理解和修改 |
| **理解代码** | 重构是理解代码的最佳方式 |
| **修复 bug** | 让 bug 藏不住 |

---

## 何时重构

### 规则一：三次法则

> "事不过三，三则重构。"

| 次数 | 行动 |
|------|------|
| 第一次 | 尽管去做 |
| 第二次 | 虽有反感，但还是去做 |
| 第三次 | 重构 |

### 规则二：添加功能时

```
准备开发 → 看代码结构 → 发现设计有问题 → 重构 → 添加功能
```

### 规则三：修复 bug 时

```
发现 bug → 看代码 → 难以理解 → 重构 → bug 显露 → 修复
```

### 规则四：代码评审时

每次代码评审都应该问："能不能重构让代码更好？"

---

## 重构的前提

### 必须有可靠的测试

> "没有测试，不要重构。"

```
测试覆盖 → 小步修改 → 运行测试 → 确认通过 → 继续下一步
```

### 测试覆盖标准

| 场景 | 覆盖要求 |
|------|---------|
| 关键业务逻辑 | 100% |
| 一般业务逻辑 | 80%+ |
| 工具类 | 90%+ |
| 边界条件 | 必须覆盖 |

### 小步前进

```
重构安全链：小步修改 → 频繁运行测试 → 发现问题立即回退
```

---

## 代码坏味道识别

### 神秘命名 (Mysterious Name)

**【强制】命名必须表达意图**

```java
// ❌ 反例 - 命名不清晰
int d;  // 消逝的时间
List<Element> elemList;

// ✅ 正例 - 命名清晰
int elapsedTimeInDays;
List<Element> elements;
```

| 问题 | 解决方案 |
|------|---------|
| 变量名不清晰 | 改为能表达意图的名称 |
| 函数名不清晰 | 改为动词+名词的形式 |
| 单字母滥用 | 仅用于循环变量 |

### 重复代码 (Duplicated Code)

**【强制】消除重复代码**

```java
// ❌ 反例 - 重复代码
if (order.getAmount() > 1000) {
    discount = order.getAmount() * 0.9;
}
if (invoice.getAmount() > 1000) {
    discount = invoice.getAmount() * 0.9;
}

// ✅ 正例 - 提取公共方法
private BigDecimal calculateDiscount(BigDecimal amount) {
    return amount.compareTo(BigDecimal.valueOf(1000)) > 0
        ? amount.multiply(BigDecimal.valueOf(0.9))
        : amount;
}
```

| 场景 | 解决方案 |
|------|---------|
| 同一类中重复 | 提取方法 |
| 不同类中重复 | 提取到父类或工具类 |
| 相似但有差异 | 提取方法 + 参数化 |

### 过长函数 (Long Function)

**【强制】函数不应该超过 20 行**

```java
// ❌ 反例 - 函数过长
public void printOwing() {
    Enumeration e = _orders.elements();
    double outstanding = 0.0;

    // 打印 banner
    System.out.println("*************************");
    System.out.println("***** Customer Owes ******");
    System.out.println("*************************");

    // 计算金额
    while (e.hasMoreElements()) {
        Order each = (Order) e.nextElement();
        outstanding += each.getAmount();
    }

    // 打印详情
    System.out.println("name: " + _name);
    System.out.println("amount: " + outstanding);
}

// ✅ 正例 - 拆分为小函数
public void printOwing() {
    printBanner();
    double outstanding = calculateOutstanding();
    printDetails(outstanding);
}

private void printBanner() {
    System.out.println("*************************");
    System.out.println("***** Customer Owes ******");
    System.out.println("*************************");
}

private double calculateOutstanding() {
    Enumeration e = _orders.elements();
    double result = 0.0;
    while (e.hasMoreElements()) {
        Order each = (Order) e.nextElement();
        result += each.getAmount();
    }
    return result;
}
```

### 过长参数列表 (Long Parameter List)

**【推荐】参数不应该超过 3 个**

```java
// ❌ 反例 - 参数过多
void createOrder(String customerId, String productCode, int quantity,
                 String address, String phone, String email) {
    // ...
}

// ✅ 正例 - 使用参数对象
void createOrder(CreateOrderRequest request) {
    // ...
}

@Data
class CreateOrderRequest {
    private String customerId;
    private String productCode;
    private int quantity;
    private Address address;
    private Contact contact;
}
```

### 特性依恋 (Feature Envy)

**【推荐】函数应该访问自己的数据**

```java
// ❌ 反例 - 过度访问其他对象
class Order {
    void calculatePrice(Product product) {
        double basePrice = product.getBasePrice();
        double discount = product.getDiscount();
        // ...
    }
}

// ✅ 正例 - 让对象自己处理
class Order {
    void calculatePrice(Product product) {
        return product.calculatePrice();
    }
}
```

### 数据泥团 (Data Clumps)

**【推荐】相关数据应该组织在一起**

```java
// ❌ 反例 - 数据总是成对出现
void printInfo(String firstName, String lastName, String street,
                String city, String zipCode) {
    // ...
}

// ✅ 正例 - 组织成对象
void printInfo(Person person, Address address) {
    // ...
}
```

### 基本类型偏执 (Primitive Obsession)

**【推荐】使用小对象替代基本类型**

```java
// ❌ 反例 - 使用基本类型
void connect(String host, int port, String protocol, boolean secure) {
    // ...
}

// ✅ 正例 - 使用对象
void connect(ConnectionConfig config) {
    // ...
}

class ConnectionConfig {
    private String host;
    private int port;
    private Protocol protocol;
    private boolean secure;
}
```

### 其他坏味道

| 坏味道 | 检测方法 | 重构手法 |
|--------|---------|---------|
| 过大的类 | 方法数量 > 10 或代码行数 > 500 | 提取类 |
| 过长的 switch/if | 分支 > 3 个 | 多态取代条件式 |
| 发散式变化 | 一个类因多个原因变化 | 拆分类 |
| 霰弹式修改 | 一个变化导致多个类修改 | 移动方法 |
| 依恋情结 | 函数访问其他对象的数据 | 移动方法 |
| 冗余的注释 | 代码需要注释才能理解 | 重命名、提取方法 |

---

## 重构手法速查

### 整理代码

| 手法 | 说明 | 适用场景 |
|------|------|---------|
| **提炼函数** | 将代码段提取为独立函数 | 函数过长、重复代码 |
| **内联函数** | 将函数体直接嵌入调用处 | 函数过于简单、间接层过多 |
| **提炼变量** | 将表达式提取为变量 | 表达式复杂、重复 |
| **改变函数声明** | 修改函数名或参数 | 命名不清晰、参数过多 |
| **封装变量** | 将变量设为只读 | 防止意外修改 |

### 在对象之间搬移特性

| 手法 | 说明 | 适用场景 |
|------|------|---------|
| **提炼函数** | 将函数从一个类移到另一个类 | 函数更属于另一个类 |
| **搬移函数** | 将函数从一个类移到另一个类 | 特性依恋 |
| **提炼类** | 将相关特性提取为新类 | 类职责过多 |
| **内联类** | 将类内联到另一个类 | 类不再承担责任 |

### 重新组织数据

| 手法 | 说明 | 适用场景 |
|------|------|---------|
| **自封装字段** | 将字段设为私有，提供访问器 | 直接访问字段 |
| **以对象取代基本类型** | 用类替代基本类型 | 基本类型偏执 |
| **提炼值对象** | 将相关数据组织成类 | 数据泥团 |
| **以查询取代派生变量** | 动态计算而非存储 | 数据同步问题 |

---

## 重构清单

### 编码时自查

| # | 检查项 | 通过 |
|---|--------|------|
| 1 | 函数是否超过 20 行？ | ☐ |
| 2 | 是否有重复代码？ | ☐ |
| 3 | 命名是否表达意图？ | ☐ |
| 4 | 参数是否超过 3 个？ | ☐ |
| 5 | 是否有深层嵌套？ | ☐ |
| 6 | 注释是否在解释"是什么"？ | ☐ |

### 重构前检查

| # | 检查项 | 通过 |
|---|--------|------|
| 1 | 是否有可靠的测试？ | ☐ |
| 2 | 是否理解代码的作用？ | ☐ |
| 3 | 是否能小步前进？ | ☐ |
| 4 | 是否有回退方案？ | ☐ |

---

## 记忆锚点

| 原则 | 一句话 |
|------|--------|
| **两顶帽子** | 重构和添加功能分开 |
| **小步前进** | 频繁提交、频繁测试 |
| **事不过三** | 三次重复，必须重构 |
| **测试保护** | 没有测试，不要重构 |
| **表达意图** | 好代码不需要注释 |

---

## 参考资料

| 来源 | 说明 |
|------|------|
| [《重构》第 2 版](https://gausszhou.github.io/refactoring2-zh/) | Martin Fowler 著，熊节译 |
| [《重构》GitHub 仓库](https://github.com/JustAGhost/Refactoring-2nd-Edition-CN) | 中文翻译项目 |
| [《分析模式》](https://book.douban.com/subject/3058456/) | Martin Fowler 著 |
| [《领域特定语言》](https://book.douban.com/subject/2285263/) | Martin Fowler 著 |
