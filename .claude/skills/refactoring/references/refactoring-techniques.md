# 重构手法详解

> 版本: 1.0 | 更新: 2026-02-05
>
> 基于《重构》Martin Fowler 著、熊节译
>
> 每个重构手法都按照"问题 → 解决方案 → 示例 → 动机"的结构组织

---

## 目录

- [整理代码](#整理代码)
- [在对象之间搬移特性](#在对象之间搬移特性)
- [重新组织数据](#重新组织数据)
- [简化条件逻辑](#简化条件逻辑)
- [简化函数调用](#简化函数调用)

---

## 整理代码

### 提炼函数 (Extract Function)

**问题**：一段代码可以独立出来，或者因为重复而需要整合。

**解决方案**：将这段代码放入一个独立函数中，并让函数名称解释该函数的用途。

```java
// ❌ 反例
void printOwing() {
    Enumeration e = _orders.elements();
    double outstanding = 0.0;

    while (e.hasMoreElements()) {
        Order each = (Order) e.nextElement();
        outstanding += each.getAmount();
    }

    System.out.println("name: " + _name);
    System.out.println("amount: " + outstanding);
}

// ✅ 正例
void printOwing() {
    double outstanding = calculateOutstanding();
    printBanner();
    printDetails(outstanding);
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

private void printBanner() {
    System.out.println("*************************");
    System.out.println("***** Customer Owes ******");
    System.out.println("*************************");
}

private void printDetails(double outstanding) {
    System.out.println("name: " + _name);
    System.out.println("amount: " + outstanding);
}
```

**动机**：
- 函数是细粒度的抽象
- 小函数更易于理解和复用
- 函数名应该表达"做什么"而不是"怎么做"

### 内联函数 (Inline Function)

**问题**：一个函数的本体与函数名称同样清晰易懂。

**解决方案**：在函数调用点插入函数本体，然后移除该函数。

```java
// ❌ 反例
int getRating() {
    return moreThanFiveLateDeliveries() ? 2 : 1;
}

boolean moreThanFiveLateDeliveries() {
    return _numberOfLateDeliveries > 5;
}

// ✅ 正例
int getRating() {
    return _numberOfLateDeliveries > 5 ? 2 : 1;
}
```

**动机**：
- 去除不必要的间接层
- 可能是"提炼函数"的逆向操作
- 当函数名称不再表达意图时，考虑内联

### 提炼变量 (Extract Variable)

**问题**：有一个复杂的表达式，难以理解。

**解决方案**：将该表达式（或其一部分）的结果存入一个临时变量，用变量名称解释表达式的用途。

```java
// ❌ 反例
void printPrice() {
    System.out.println("price: " + (_quantity * _itemPrice -
                          Math.max(0, _quantity - 500) * _itemPrice * 0.05 +
                          Math.min(_quantity * _itemPrice * 0.1, 100.0)));
}

// ✅ 正例
void printPrice() {
    double basePrice = _quantity * _itemPrice;
    double quantityDiscount = Math.max(0, _quantity - 500) * _itemPrice * 0.05;
    double shipping = Math.min(basePrice * 0.1, 100.0);
    double price = basePrice - quantityDiscount + shipping;

    System.out.println("price: " + price);
}
```

**动机**：
- 将复杂的表达式分解为可理解的部分
- 为每个部分命名，提高可读性
- 便于调试

### 内联变量 (Inline Variable)

**问题**：一个变量只被使用了一次，且不影响可读性。

**解决方案**：在该变量被使用的地方直接使用表达式。

```java
// ❌ 反例
boolean hasDiscount(Order order) {
    boolean basePrice = order.basePrice();
    return basePrice > 1000;
}

// ✅ 正例
boolean hasDiscount(Order order) {
    return order.basePrice() > 1000;
}
```

**动机**：
- 去除不必要的临时变量
- 可能是"提炼变量"的逆向操作

### 改变函数声明 (Change Function Declaration)

**问题**：函数名称不能表达意图，或参数过多。

**解决方案**：修改函数名称或参数。

```java
// ❌ 反例 - 函数名不清晰
void proc(Order order) {
    // ...
}

// ❌ 反例 - 参数过多
void createOrder(String customerId, String productCode,
                 int quantity, String address, String phone) {
    // ...
}

// ✅ 正例
void processOrder(Order order) {
    // ...
}

void createOrder(CreateOrderRequest request) {
    // ...
}
```

**动机**：
- 函数名应该表达"做什么"
- 参数列表应该尽可能短（不超过 3 个）
- 使用参数对象替代长参数列表

---

## 在对象之间搬移特性

### 搬移函数 (Move Method)

**问题**：在你的程序中，有个函数与其所驻类之外的另一个类进行更多的交流：调用后者，或被后者调用。

**解决方案**：在另一个类中建立一个有着类似行为的新函数，将旧函数变成一个单纯的委托函数，或者干脆将旧函数完全删除。

```java
// ❌ 反例 - 函数访问其他对象的数据
class Account {
    double overdraftCharge() {
        if (_type.isPremium()) {
            return 10;
        } else {
            return _type.overdraftCharge(this);
        }
    }
}

class AccountType {
    double overdraftCharge(Account account) {
        if (account.daysOverdrawn() > 7) {
            return account.overdraftCharge() * 1.2;
        } else {
            return account.overdraftCharge();
        }
    }
}

// ✅ 正例 - 将函数移到它使用的数据所在的类
class Account {
    double overdraftCharge() {
        return _type.overdraftCharge(this);
    }

    int daysOverdrawn() {
        // ...
    }

    double overdraftCharge() {
        // ...
    }
}

class AccountType {
    double overdraftCharge(Account account) {
        if (isPremium()) {
            return 10;
        } else {
            return account.overdraftChargeForType();
        }
    }
}
```

**动机**：
- 函数应该访问它所使用的数据
- 遵循"单一职责原则"

### 提炼类 (Extract Class)

**问题**：某个类做了应该由两个类做的事情。

**解决方案**：建立一个新类，将相关的字段和函数从旧类搬移到新类。

```java
// ❌ 反例 - 类职责过多
class Person {
    private String name;
    private String officeAreaCode;
    private String officeNumber;

    String getTelephoneNumber() {
        return "(" + officeAreaCode + ") " + officeNumber;
    }
}

// ✅ 正例 - 提取电话号码类
class Person {
    private String name;
    private TelephoneNumber officeTelephone;

    String getTelephoneNumber() {
        return officeTelephone.getTelephoneNumber();
    }
}

class TelephoneNumber {
    private String areaCode;
    private String number;

    String getTelephoneNumber() {
        return "(" + areaCode + ") " + number;
    }
}
```

**动机**：
- 遵循"单一职责原则"
- 一个类应该只有一个变化的原因
- 提高内聚性

---

## 重新组织数据

### 自封装字段 (Self Encapsulate Field)

**问题**：你直接访问一个字段，但与字段之间的耦合关系越来越笨拙。

**解决方案**：为该字段建立取值/设值函数，并且只以这些函数来访问字段。

```java
// ❌ 反例
class Range {
    private int low, high;

    boolean includes(int arg) {
        return arg >= low && arg <= high;
    }
}

// ✅ 正例
class Range {
    private int low, high;

    boolean includes(int arg) {
        return arg >= getLow() && arg <= getHigh();
    }

    int getLow() {
        return low;
    }

    int getHigh() {
        return high;
    }
}
```

**动机**：
- 允许子类覆写如何访问字段
- 便于在访问时添加额外逻辑
- 更好地支持封装

### 以对象取代基本类型 (Replace Primitive with Object)

**问题**：你有一个基本类型，但其表现行为不够丰富。

**解决方案**：将该基本类型替换为对象。

```java
// ❌ 反例
class Customer {
    private String name;
    private String priority;  // "LOW", "MEDIUM", "HIGH"
}

// ✅ 正例
class Customer {
    private String name;
    private Priority priority;
}

enum Priority {
    LOW, MEDIUM, HIGH
}
```

**动机**：
- 添加类型安全
- 便于扩展行为
- 消除"基本类型偏执"

---

## 简化条件逻辑

### 分解条件表达式 (Decompose Conditional)

**问题**：你有一个复杂的条件语句（if-then-else）。

**解决方案**：从条件表达式中的每个分支提炼出一个独立函数。

```java
// ❌ 反例
if (date.before(SUMMER_START) || date.after(SUMMER_END)) {
    charge = quantity * _winterRate + _winterServiceCharge;
} else {
    charge = quantity * _summerRate;
}

// ✅ 正例
if (notSummer(date)) {
    charge = winterCharge(quantity);
} else {
    charge = summerCharge(quantity);
}

private boolean notSummer(Date date) {
    return date.before(SUMMER_START) || date.after(SUMMER_END);
}

private double winterCharge(int quantity) {
    return quantity * _winterRate + _winterServiceCharge;
}

private double summerCharge(int quantity) {
    return quantity * _summerRate;
}
```

**动机**：
- 提高可读性
- 突出条件逻辑的业务含义
- 便于单独测试每个分支

### 合并条件表达式 (Consolidate Conditional)

**问题**：你有一系列条件测试，且得到相同结果。

**解决方案**：将这些测试合并为一个条件表达式，并将这个条件表达式提炼为一个独立函数。

```java
// ❌ 反例
double disabilityAmount() {
    if (_seniority < 2) return 0;
    if (_monthsDisabled > 12) return 0;
    if (_isPartTime) return 0;
    // ...
}

// ✅ 正例
double disabilityAmount() {
    if (isNotEligibleForDisability()) return 0;
    // ...
}

private boolean isNotEligibleForDisability() {
    return _seniority < 2 ||
           _monthsDisabled > 12 ||
           _isPartTime;
}
```

**动机**：
- 消除重复的条件检查
- 提高可读性
- 突出业务规则

---

## 简化函数调用

### 参数化方法 (Parameterize Method)

**问题**：若干函数做了类似的工作，但在函数本体中包含了不同的值。

**解决方案**：建立一个单一函数，以参数表达那些不同的值。

```java
// ❌ 反例
void tenPercentOff() {
    _price *= 0.9;
}

void fivePercentOff() {
    _price *= 0.95;
}

// ✅ 正例
void discount(double percentage) {
    _price *= (1 - percentage);
}

// 使用
tenPercentOff()  →  discount(0.10)
fivePercentOff()  →  discount(0.05)
```

**动机**：
- 消除重复代码
- 提高灵活性
- 便于添加新的折扣率

---

## 重构安全清单

### 重构前

- [ ] 是否有可靠的测试？
- [ ] 测试覆盖是否充分？
- [ ] 是否理解代码的作用？
- [ ] 是否有回退方案？

### 重构中

- [ ] 是否小步前进？
- [ ] 是否频繁运行测试？
- [ ] 测试是否全部通过？
- [ ] 是否保留了代码的行为？

### 重构后

- [ ] 代码是否更清晰？
- [ ] 是否消除了坏味道？
- [ ] 测试是否依然通过？
- [ ] 是否需要更新文档？

---

## 参考资料

- [《重构》第 2 版目录](https://gausszhou.github.io/refactoring2-zh/)
- [重构手法完整列表](https://refactoring.com/catalog/)
