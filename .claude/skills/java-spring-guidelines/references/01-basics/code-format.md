# 代码格式规范

> Java/Spring Boot 编码规范 - 代码格式
> 参考：阿里巴巴 Java 开发手册

---

## 大括号规则（强制）

### 非空代码块

**【强制】左大括号不换行，右大括号按结构换行，因为结构一致更易读。**

```java
// ✅ 正例 - 标准格式
public static void main(String[] args) {
    if (flag == 0) {
        System.out.println(say);
    } else {
        System.out.println("ok");
    }
}

// ❌ 反例
public static void main(String[] args)
{
    if (flag == 0) {
        System.out.println(say);
    }
    else {
        System.out.println("ok");
    }
}
```

### 空代码块

**【推荐】空方法可用 `{}`，需要说明时用独立行注释，因为简洁且语义明确。**

```java
// ✅ 正例 - 空代码块简洁写法
public void doNothing() {}

// ✅ 正例 - 空方法可以加注释
@Override
public void callback() {
    // 空实现
}
```

---

## 空格规则（强制）

### 括号空格

**【强制】左小括号和字符之间不出现空格；右小括号和字符之间也不出现空格，因为括号内更紧凑可读。**

```java
// ✅ 正例
if (flag == 0) {
    method(param1, param2);
}

// ❌ 反例
// 括号内出现空格
if ( flag == 0 ) {
    method( param1, param2 );
}
```

### 关键字空格

**【强制】if/for/while/switch/do 等保留字与括号之间必须加空格，因为结构更清晰易读。**

```java
// ✅ 正例
if (condition) { }
for (int i = 0; i < 10; i++) { }
while (running) { }
switch (status) { }
do { } while (condition);

// ❌ 反例
// 关键字后缺少空格
if(condition) { }
for(int i = 0; i < 10; i++) { }
while(running) { }
```

### 运算符空格

**【强制】任何二目、三目运算符的左右两边都需要加一个空格，因为边界清晰更易读。**

```java
// ✅ 正例
int a = 3;
boolean result = a > 0 && b < 10;
int sum = a + b;
String name = flag ? "yes" : "no";

// ❌ 反例
int a=3;
boolean result=a>0&&b<10;
int sum=a+b;
String name=flag?"yes":"no";
```

### 方法参数空格

**【强制】方法参数在定义和传入时，多个参数逗号后边必须加空格，因为列表更易读。**

```java
// ✅ 正例
public void method(String a, String b, String c) { }
method("a", "b", "c");

// ❌ 反例
public void method(String a,String b,String c) { }
method("a","b","c");
```

---

## 缩进规则（强制）

**【强制】采用 4 个空格缩进，禁止使用 tab 字符，因为不同环境 tab 宽度不一致。**

> IDE 设置：
> - IDEA：Settings → Editor → Code Style → Java → 取消勾选 "Use tab character"
> - Eclipse：勾选 "insert spaces for tabs"

```java
// ✅ 正例 - 4 个空格缩进
public class Example {
    private String name;

    public void method() {
        if (condition) {
            doSomething();
        }
    }
}
```

---

## 注释规则（强制）

**【强制】禁止行尾注释，因为会遮挡代码并影响对齐与 diff。**

**【强制】单行注释 `//` 后必须保留一个空格，因为可读性更好。**

```java
// ✅ 正例
// 这是正确的注释格式
int count = 0;

// ❌ 反例 - 行尾注释（禁止）
// 例子：int count = 0; <行尾注释>

// ❌ 反例 - 缺少空格
//这是错误的注释格式
```

---

## 行长度规则

**【强制】单行字符数限制不超过 120 个，超出需要换行，因为过长会降低可读性。**

### 换行原则

1. 第二行相对第一行缩进 4 个空格，从第三行开始不再继续缩进
2. 运算符与下文一起换行
3. 方法调用的点符号与下文一起换行
4. 方法调用时，多个参数需要换行时，在逗号后进行
5. 在括号前不要换行

```java
// ✅ 正例 - 链式调用换行
StringBuilder sb = new StringBuilder();
// 点号与方法一起换行，缩进 4 空格
sb.append("hello")
    .append("world")
    .append("java")
    .append("spring");

// ✅ 正例 - 参数换行
// 逗号后换行
public void longMethodName(String param1, String param2,
    String param3, String param4) {
    // ...
}

// ✅ 正例 - 条件表达式换行
// 运算符与下文一起换行
if (condition1 && condition2
    && condition3 && condition4) {
    // ...
}

// ❌ 反例 - 括号前换行
// 不要在括号前换行
sb.append("hello").append
    ("world");

// ❌ 反例 - 逗号前换行
// 不要在逗号前换行
method(param1, param2, param3
    , param4);
```

---

## 空行规则

**【推荐】方法体内的执行语句组、变量定义语句组、不同业务逻辑之间插入一个空行，因为分组更清晰。相同业务逻辑和语义之间不需要插入空行。**

```java
// ✅ 正例
public void processOrder(OrderReq req) {
    // 1. 参数校验
    validateParams(req);

    // 2. 查询数据
    Order order = orderMapper.selectById(req.getId());
    User user = userMapper.selectById(order.getUserId());

    // 3. 业务处理
    order.setStatus(OrderStatus.COMPLETED);
    order.setCompleteTime(LocalDateTime.now());

    // 4. 保存数据
    orderMapper.updateById(order);

    // 5. 发送通知
    notificationService.sendOrderCompleteNotice(user, order);
}

// ❌ 反例 - 无空行分隔，逻辑混乱
public void processOrder(OrderReq req) {
    validateParams(req);
    Order order = orderMapper.selectById(req.getId());
    User user = userMapper.selectById(order.getUserId());
    order.setStatus(OrderStatus.COMPLETED);
    order.setCompleteTime(LocalDateTime.now());
    orderMapper.updateById(order);
    notificationService.sendOrderCompleteNotice(user, order);
}

// ❌ 反例 - 过多空行
public void processOrder(OrderReq req) {
    validateParams(req);


    Order order = orderMapper.selectById(req.getId());


    User user = userMapper.selectById(order.getUserId());
}
```

---

## 变量对齐规则

**【推荐】没有必要增加若干空格来使某一行的字符与上一行对应位置的字符对齐，因为对齐会增加维护成本。**

```java
// ✅ 正例 - 不需要刻意对齐
int a = 3;
long b = 4L;
float c = 5F;
StringBuilder sb = new StringBuilder();

// ⚠️ 不推荐 - 刻意对齐增加维护成本
int           a  = 3;
long          b  = 4L;
float         c  = 5F;
StringBuilder sb = new StringBuilder();
// 增加新变量时需要调整所有对齐
```

---

## 文件编码规则

**【强制】IDE 的 text file encoding 设置为 UTF-8；IDE 中文件的换行符使用 Unix 格式（LF），不要使用 Windows 格式（CRLF），因为跨平台一致性更好。**

### IDE 设置

**IDEA 设置：**
- File → Settings → Editor → File Encodings
  - Global Encoding: UTF-8
  - Project Encoding: UTF-8
  - Default encoding for properties files: UTF-8
- File → Settings → Editor → Code Style
  - Line separator: Unix and macOS (\n)

**VS Code 设置：**
```json
{
    "files.encoding": "utf8",
    "files.eol": "\n"
}
```

---

## 完整示例

```java
package com.example.service;

import static com.example.exception.ServiceExceptionUtil.exception;

import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.entity.Order;
import com.example.mapper.OrderMapper;

/**
 * 订单服务实现
 *
 * @author developer
 * @since 2026-01-28
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements IOrderService {

    private final OrderMapper orderMapper;
    private final UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createOrder(OrderCreateReq req) {
        // 1. 参数校验
        log.info("[createOrder] 开始创建订单, userId={}", req.getUserId());
        validateParams(req);

        // 2. 构建订单实体
        Order order = new Order();
        order.setUserId(req.getUserId());
        order.setProductId(req.getProductId());
        order.setQuantity(req.getQuantity());
        order.setStatus(OrderStatus.PENDING);
        order.setCreateTime(LocalDateTime.now());

        // 3. 保存订单
        orderMapper.insert(order);
        log.info("[createOrder] 订单创建成功, orderId={}", order.getId());
    }

    private void validateParams(OrderCreateReq req) {
        if (req.getUserId() == null) {
            throw exception(ErrorCode.USER_ID_REQUIRED);
        }
        if (req.getProductId() == null) {
            throw exception(ErrorCode.PRODUCT_ID_REQUIRED);
        }
        if (req.getQuantity() == null || req.getQuantity() <= 0) {
            throw exception(ErrorCode.QUANTITY_INVALID);
        }
    }
}
```

---

## 格式禁则速查

| ❌ 禁止 | ✅ 正确 | 原因 |
|--------|--------|------|
| `if(x)` | `if (x)` | 关键字后加空格 |
| `a=b+c` | `a = b + c` | 运算符两侧加空格 |
| `method(a,b,c)` | `method(a, b, c)` | 逗号后加空格 |
| Tab 缩进 | 4 空格缩进 | 统一格式 |
| `//注释` | `// 注释` | 双斜线后加空格 |
| 行尾注释 | 独立行注释 | 行尾注释影响可读性 |
| 120 字符超长 | 换行处理 | 可读性 |
| Windows 换行 (CRLF) | Unix 换行 (LF) | 跨平台兼容 |
| 括号前换行 | 逗号后换行 | 代码可读性 |
