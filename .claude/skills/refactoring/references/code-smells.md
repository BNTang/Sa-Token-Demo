# 代码坏味道识别指南

> 版本: 1.0 | 更新: 2026-02-05
>
> 基于《重构》Martin Fowler 著、熊节译
>
> 学会识别代码坏味道，是重构的第一步

---

## 概述

### 什么是代码坏味道

代码坏味道是代码中某些**可疑的结构**，它们暗示着可能存在重构的机会。

**关键**：
- 坏味道 != bug
- 坏味道不一定需要立即修复
- 但需要保持警惕，持续改进

### 为什么识别坏味道很重要

| 好处 | 说明 |
|------|------|
| **及早发现问题** | 在问题恶化前发现 |
| **指引重构方向** | 知道该重构什么 |
| **提升代码质量** | 持续改善代码 |
| **降低维护成本** | 减少技术债务 |

---

## 完整坏味道清单

### 1. 神秘命名 (Mysterious Name)

**【强制】命名必须表达意图**

代码的可读性 90% 取决于命名。

#### 检测方法

```java
// ❌ 存在问题
int d;  // 消逝的天数
List<Element> elemList;
public void get();
public void calc();

// ✅ 正确做法
int elapsedTimeInDays;
List<Element> elements;
public String getName();
public double calculatePrice();
```

#### 常见问题

| 问题 | 解决方案 |
|------|---------|
| 单字母变量 | 使用完整单词 |
| 缩写过多 | 使用完整单词 |
| 模糊名称 | 使用表达意图的名称 |
| 布尔值不清晰 | 使用 is/has/can 前缀 |

---

### 2. 重复代码 (Duplicated Code)

**【强制】消除重复代码**

重复是万恶之源，每次修改都要记得改所有地方。

#### 检测方法

```java
// ❌ 场景1：同一个类中重复
class Employee {
    void calculatePay() {
        // 重复的计算逻辑
        double basePay = 2000;
        double bonus = 500;
        double tax = (basePay + bonus) * 0.1;
    }

    void calculateSeverance() {
        // 相同的计算逻辑
        double basePay = 2000;
        double bonus = 500;
        double tax = (basePay + bonus) * 0.1;
    }
}

// ✅ 提取方法
class Employee {
    void calculatePay() {
        double tax = calculateTax();
        // ...
    }

    void calculateSeverance() {
        double tax = calculateTax();
        // ...
    }

    private double calculateTax() {
        double basePay = 2000;
        double bonus = 500;
        return (basePay + bonus) * 0.1;
    }
}
```

```java
// ❌ 场景2：不同类中重复
class Customer {
    double calculateBill() {
        double base = 100;
        double tax = base * 0.1;
        return base + tax;
    }
}

class Employee {
    double calculatePay() {
        double base = 2000;
        double tax = base * 0.1;
        return base + tax;
    }
}

// ✅ 提取到父类或工具类
class TaxCalculator {
    static double calculateTax(double amount) {
        return amount * 0.1;
    }
}

class Customer {
    double calculateBill() {
        double base = 100;
        double tax = TaxCalculator.calculateTax(base);
        return base + tax;
    }
}
```

#### 消除重复的方法

| 场景 | 重构手法 |
|------|---------|
| 同一类中重复 | 提炼函数 |
| 不同类中重复 | 提炼到父类 |
| 不同类中相似 | 提炼函数 + 参数化 |

---

### 3. 过长函数 (Long Function)

**【强制】函数不应超过 20 行**

函数越长，越难理解。

#### 检测方法

```java
// ❌ 60 行的函数
public void printReport() {
    // 1. 打印标题 (10 行)
    System.out.println("*************************");
    // ...

    // 2. 计算数据 (20 行)
    Enumeration e = _orders.elements();
    double outstanding = 0.0;
    while (e.hasMoreElements()) {
        Order each = (Order) e.nextElement();
        outstanding += each.getAmount();
    }

    // 3. 打印详情 (30 行)
    System.out.println("name: " + _name);
    System.out.println("amount: " + outstanding);
    // ...
}

// ✅ 拆分为小函数
public void printReport() {
    printHeader();
    double outstanding = calculateOutstanding();
    printDetails(outstanding);
}
```

#### 拆分策略

1. 找到函数中"在做什么"的注释
2. 将注释后的代码提取为函数
3. 函数名解释"做什么"

---

### 4. 过长参数列表 (Long Parameter List)

**【推荐】参数不应超过 3 个**

太长的参数列表难以理解，且容易导致前后不一致。

#### 检测方法

```java
// ❌ 参数过多
void createCustomer(String name, String email, String phone,
                     String address, String city, String zip) {
    // ...
}

// ✅ 使用参数对象
void createCustomer(CustomerData data) {
    // ...
}

class CustomerData {
    private String name;
    private String email;
    private String phone;
    private Address address;
}
```

#### 减少参数的方法

| 方法 | 适用场景 |
|------|---------|
| 使用参数对象 | 多个相关参数 |
| 通过对象传递 | 函数可以访问对象的数据 |
| 移除不需要的参数 | 参数未被使用 |

---

### 5. 过大的类 (Large Class)

**【推荐】类不应超过 300 行**

类越大，越难理解，越容易出现重复。

#### 检测方法

| 指标 | 阈值 | 说明 |
|------|------|------|
| 代码行数 | > 300 行 | 过大 |
| 方法数量 | > 10 个 | 职责过多 |
| 实例变量 | > 10 个 | 复杂度过高 |

#### 解决方案

```java
// ❌ 一个类做了太多事
class Customer {
    // 基础信息
    private String name;
    private String email;

    // 电话信息
    private String homePhone;
    private String workPhone;
    private String mobilePhone;

    // 地址信息
    private String street;
    private String city;
    private String zipCode;

    // ... 20 个方法
}

// ✅ 拆分为多个类
class Customer {
    private String name;
    private String email;
    private TelephoneNumber phone;
    private Address address;
}

class TelephoneNumber {
    private String home;
    private String work;
    private String mobile;
}

class Address {
    private String street;
    private String city;
    private String zipCode;
}
```

---

### 6. 过长的 switch/if

**【推荐】分支不应超过 3 个**

过长的条件逻辑难以理解和扩展。

#### 检测方法

```java
// ❌ 过长的 switch
String getTypeColor(String type) {
    switch (type) {
        case "RED": return "红色";
        case "GREEN": return "绿色";
        case "BLUE": return "蓝色";
        case "YELLOW": return "黄色";
        case "PURPLE": return "紫色";
        // ... 10 个分支
        default: return "未知";
    }
}

// ✅ 使用多态
interface Color {
    String getDisplayName();
}

class Red implements Color {
    String getDisplayName() {
        return "红色";
    }
}

String getTypeColor(Color color) {
    return color.getDisplayName();
}
```

#### 解决方案

| 场景 | 解决方案 |
|------|---------|
| 根据类型选择行为 | 多态 |
| 根据状态选择行为 | 状态模式 |
| 复杂条件 | 提炼函数 |

---

### 7. 基本类型偏执 (Primitive Obsession)

**【推荐】使用对象替代基本类型**

过度使用基本类型会丢失类型安全和业务含义。

#### 检测方法

```java
// ❌ 使用基本类型
class Order {
    private String customerName;
    private String customerPhone;
    private String customerAddress;

    void setCustomerInfo(String name, String phone, String address) {
        this.customerName = name;
        this.customerPhone = phone;
        this.customerAddress = address;
    }
}

// ✅ 使用对象
class Order {
    private Customer customer;

    void setCustomer(Customer customer) {
        this.customer = customer;
    }
}

class Customer {
    private Name name;
    private Telephone phone;
    private Address address;
}
```

---

### 8. 数据泥团 (Data Clumps)

**【推荐】相关数据应该组织在一起**

总是同时出现的数据应该被组织为一个对象。

#### 检测方法

```java
// ❌ 数据总是成对出现
void printAddress(String street, String city, String zip) {
    // ...
}

void shipTo(String street, String city, String zip) {
    // ...
}

// ✅ 组织为对象
void printAddress(Address address) {
    // ...
}

void shipTo(Address address) {
    // ...
}

class Address {
    private String street;
    private String city;
    private String zip;
}
```

---

### 9. 特性依恋 (Feature Envy)

**【推荐】函数应该访问自己对象的数据**

函数过度访问其他对象的数据，应该移动到那个对象。

#### 检测方法

```java
// ❌ 函数访问其他对象的数据
class Order {
    double calculatePrice(Product product) {
        double basePrice = product.getBasePrice();
        double discount = product.getDiscount();
        return basePrice - discount;
    }
}

// ✅ 让对象自己处理
class Order {
    double calculatePrice(Product product) {
        return product.calculatePrice();
    }
}

class Product {
    double calculatePrice() {
        return getBasePrice() - getDiscount();
    }
}
```

---

### 10. 发散式变化 (Divergent Change)

**【推荐】一个类应该只有一个变化的原因**

如果一个类因多个原因而变化，考虑拆分。

#### 检测方法

```java
// ❌ 一个类因多个原因变化
class Employee {
    // 因工资政策变化
    void calculatePay() { }

    // 因税务政策变化
    void calculateTax() { }

    // 因考勤政策变化
    void recordAttendance() { }
}

// ✅ 拆分为不同类
class Employee {
    Payroll payroll = new Payroll();
    Tax tax = new Tax();
    Attendance attendance = new Attendance();
}

class Payroll {
    void calculatePay() { }
}

class Tax {
    void calculateTax() { }
}

class Attendance {
    void recordAttendance() { }
}
```

---

## 坏味道优先级

### P0 - 必须立即修复

| 坏味道 | 影响 | 优先级 |
|--------|------|--------|
| 重复代码 | 修改遗漏、行为不一致 | ⭐⭐⭐⭐⭐ |
| 神秘命名 | 无法理解、维护困难 | ⭐⭐⭐⭐⭐ |
| 过长函数 | 难以理解、难以测试 | ⭐⭐⭐⭐ |

### P1 - 应该尽快修复

| 坏味道 | 影响 | 优先级 |
|--------|------|--------|
| 过大的类 | 职责不清、难以维护 | ⭐⭐⭐⭐ |
| 基本类型偏执 | 缺少类型安全 | ⭐⭐⭐⭐ |
| 数据泥团 | 数据不一致 | ⭐⭐⭐ |

### P2 - 可以逐步改进

| 坏味道 | 影响 | 优先级 |
|--------|------|--------|
| 过长的参数列表 | 可读性差 | ⭐⭐⭐ |
| 特性依恋 | 职责不清 | ⭐⭐⭐ |

---

## 检查清单

### 每次代码评审

| # | 检查项 | 说明 |
|---|--------|------|
| 1 | 有重复代码吗？ | 检查相同或相似的代码片段 |
| 2 | 函数超过 20 行吗？ | 检查函数长度 |
| 3 | 命名清晰吗？ | 检查变量、函数、类命名 |
| 4 | 参数超过 3 个吗？ | 检查参数列表长度 |
| 5 | 有过长的条件分支吗？ | 检查 switch/if 长度 |
| 6 | 有基本类型偏执吗？ | 检查是否过度使用基本类型 |
| 7 | 有数据泥团吗？ | 检查是否总是一起出现的数据 |

### 每周代码审查

| # | 检查项 | 说明 |
|---|--------|------|
| 1 | 最大的类有多大？ | 行数、方法数 |
| 2 | 重复代码集中在哪些地方？ | 记录待重构点 |
| 3 | 最长的函数有多长？ | 目标是缩短它 |
| 4 | 本周发现了多少坏味道？ | 跟踪技术债务 |

---

## 参考资料

- [《重构》第 3 章：代码的坏味道](https://gausszhou.github.io/refactoring2-zh/ch3.html)
- [代码坏味道完整列表](https://refactoring.com/catalog/refactorings-with-smells)
