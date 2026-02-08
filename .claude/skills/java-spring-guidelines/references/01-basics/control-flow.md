# 控制语句规范

> Java/Spring Boot 编码规范 - 控制语句
> 参考：阿里巴巴 Java 开发手册

---

## if 语句规范

### 大括号规则

**【强制】if/else/for/while/do 语句必须使用大括号，即使只有一行代码。**

```java
// ❌ 反例 - 无大括号
if (condition)
    doSomething();

if (condition) doSomething();

// ✅ 正例
if (condition) {
    doSomething();
}
```

### 避免 if-else 嵌套

**【推荐】if-else 嵌套不超过 2 层，超过使用卫语句或策略模式。**

```java
// ❌ 反例 - 多层嵌套
public void process(Order order) {
    if (order != null) {
        if (order.getStatus() == 1) {
            if (order.getAmount() > 0) {
                if (order.getUserId() != null) {
                    // 业务逻辑
                }
            }
        }
    }
}

// ✅ 正例 - 卫语句（提前返回）
public void process(Order order) {
    if (order == null) {
        throw new BusinessException("订单不能为空");
    }
    if (order.getStatus() != 1) {
        throw new BusinessException("订单状态不正确");
    }
    if (order.getAmount() <= 0) {
        throw new BusinessException("订单金额不正确");
    }
    if (order.getUserId() == null) {
        throw new BusinessException("用户ID不能为空");
    }
    // 业务逻辑
    doProcess(order);
}
```

### 条件表达式

**【强制】不要在条件判断中执行复杂语句，应提取为布尔变量。**

```java
// ❌ 反例 - 条件过于复杂
if (user != null && user.getAge() > 18 && user.getStatus() == 1 
    && user.getRole() != null && "ADMIN".equals(user.getRole().getName())
    && user.getPermissions().contains("EDIT")) {
    // ...
}

// ✅ 正例 - 提取为有意义的变量
boolean isAdult = user != null && user.getAge() > 18;
boolean isActive = user != null && user.getStatus() == 1;
boolean isAdmin = user != null && user.getRole() != null 
    && "ADMIN".equals(user.getRole().getName());
boolean canEdit = user != null && user.getPermissions().contains("EDIT");

if (isAdult && isActive && isAdmin && canEdit) {
    // ...
}
```

### null 判断

**【推荐】对象判 null 在前，防止 NPE。**

```java
// ❌ 反例 - 可能 NPE
if (user.getName().equals("admin")) { }

// ✅ 正例 - 常量在前
if ("admin".equals(user.getName())) { }

// ✅ 正例 - 使用 Objects.equals
if (Objects.equals(user.getName(), "admin")) { }

// ✅ 正例 - null 检查在前
if (user != null && "admin".equals(user.getName())) { }
```

---

## switch 语句规范

### 必须有 default

**【强制】每个 switch 语句都必须有 default 分支。**

```java
// ❌ 反例 - 缺少 default
switch (status) {
    case 1:
        doA();
        break;
    case 2:
        doB();
        break;
}

// ✅ 正例
switch (status) {
    case 1:
        doA();
        break;
    case 2:
        doB();
        break;
    default:
        throw new IllegalArgumentException("未知状态: " + status);
}
```

### 必须有 break

**【强制】每个 case 要么用 break 结束，要么注释说明 fall through。**

```java
// ❌ 反例 - 缺少 break，意外 fall through
switch (type) {
    case 1:
        doA();
    case 2:      // 意外执行
        doB();
        break;
}

// ✅ 正例 - 明确 break
switch (type) {
    case 1:
        doA();
        break;
    case 2:
        doB();
        break;
    default:
        break;
}

// ✅ 正例 - 故意 fall through 需注释
switch (type) {
    case 1:
    case 2:
        // case 1 和 2 相同处理
        doAB();
        break;
    case 3:
        doC();
        // fall through - 故意继续执行 default
    default:
        doDefault();
        break;
}
```

### 使用枚举 switch

**【推荐】switch 优先使用枚举类型，而非整数或字符串。**

```java
// ✅ 正例 - 枚举 switch
public void processOrder(OrderStatus status) {
    switch (status) {
        case PENDING:
            handlePending();
            break;
        case PAID:
            handlePaid();
            break;
        case SHIPPED:
            handleShipped();
            break;
        default:
            throw new IllegalArgumentException("未处理的状态: " + status);
    }
}
```

### Java 14+ switch 表达式

**【推荐】Java 14+ 使用 switch 表达式，更简洁安全。**

```java
// ✅ 正例 - switch 表达式（Java 14+）
String statusText = switch (status) {
    case PENDING -> "待处理";
    case PAID -> "已支付";
    case SHIPPED -> "已发货";
    case COMPLETED -> "已完成";
};

// ✅ 正例 - 多行 case
int result = switch (day) {
    case MONDAY, FRIDAY, SUNDAY -> 6;
    case TUESDAY -> 7;
    default -> {
        int len = day.toString().length();
        yield len;
    }
};
```

---

## for 循环规范

### 循环变量

**【强制】循环中不要修改循环变量和条件表达式的值。**

```java
// ❌ 反例 - 修改循环变量
for (int i = 0; i < 10; i++) {
    if (condition) {
        i = i + 2;  // 禁止修改循环变量
    }
}

// ❌ 反例 - 修改集合大小
for (int i = 0; i < list.size(); i++) {
    if (condition) {
        list.remove(i);  // 禁止在循环中修改集合
    }
}
```

### 循环中避免复杂计算

**【推荐】循环条件中避免方法调用，应提前计算。**

```java
// ❌ 反例 - 每次循环都调用 size()
for (int i = 0; i < list.size(); i++) {
    // ...
}

// ✅ 正例 - 提前计算
int size = list.size();
for (int i = 0; i < size; i++) {
    // ...
}

// ✅ 正例 - 使用 foreach（推荐）
for (User user : users) {
    // ...
}
```

### 避免在循环中创建对象

**【推荐】避免在循环中创建大量临时对象。**

```java
// ❌ 反例 - 循环中创建 DateFormat
for (Order order : orders) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");  // 每次循环都创建
    String dateStr = sdf.format(order.getCreateTime());
}

// ✅ 正例 - 提到循环外
SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
for (Order order : orders) {
    String dateStr = sdf.format(order.getCreateTime());
}

// ✅ 正例 - 使用 DateTimeFormatter（线程安全）
DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
for (Order order : orders) {
    String dateStr = order.getCreateTime().format(formatter);
}
```

---

## while/do-while 规范

**【强制】while 循环必须有明确的终止条件，避免死循环。**

```java
// ❌ 反例 - 可能死循环
while (true) {
    // 没有 break 条件
}

// ✅ 正例 - 明确终止条件
int retryCount = 0;
int maxRetry = 3;
while (retryCount < maxRetry) {
    try {
        doSomething();
        break;
    } catch (Exception e) {
        retryCount++;
        log.warn("重试第{}次", retryCount);
    }
}

// ✅ 正例 - 有超时限制
long startTime = System.currentTimeMillis();
long timeout = 30000L;
while (System.currentTimeMillis() - startTime < timeout) {
    if (checkCondition()) {
        break;
    }
    Thread.sleep(1000);
}
```

---

## 三目运算符

**【推荐】三目运算符只用于简单条件，复杂条件用 if-else。**

```java
// ✅ 正例 - 简单条件
String status = isActive ? "启用" : "禁用";

// ❌ 反例 - 过于复杂
String result = a > b ? (c > d ? "A" : "B") : (e > f ? "C" : "D");

// ✅ 正例 - 复杂条件用 if-else
String result;
if (a > b) {
    result = c > d ? "A" : "B";
} else {
    result = e > f ? "C" : "D";
}
```

**【强制】三目运算符注意类型转换和拆箱。**

```java
// ❌ 反例 - 自动拆箱 NPE
Integer a = null;
Integer b = 2;
Integer result = true ? a : b;  // NPE！a 被拆箱

// ✅ 正例 - 避免 null
Integer result = (a != null) ? a : b;
```

---

## Optional 代替 null 判断

**【推荐】使用 Optional 简化 null 判断链。**

```java
// ❌ 反例 - 多层 null 判断
String cityName = null;
if (user != null) {
    Address address = user.getAddress();
    if (address != null) {
        City city = address.getCity();
        if (city != null) {
            cityName = city.getName();
        }
    }
}

// ✅ 正例 - 使用 Optional
String cityName = Optional.ofNullable(user)
    .map(User::getAddress)
    .map(Address::getCity)
    .map(City::getName)
    .orElse("未知");
```

---

## 禁则速查

| ❌ 禁止 | ✅ 正确 | 原因 |
|--------|--------|------|
| if 无大括号 | 必须有大括号 | 容易出错 |
| 超过 2 层嵌套 | 卫语句/策略模式 | 可读性 |
| switch 无 default | 必须有 default | 完整性 |
| case 无 break | break 或注释 fall through | 防止意外 |
| 循环中修改循环变量 | 禁止修改 | 逻辑混乱 |
| 循环中创建对象 | 提到循环外 | 性能 |
| while 无终止条件 | 明确终止条件 | 防止死循环 |
| 复杂三目嵌套 | if-else | 可读性 |
