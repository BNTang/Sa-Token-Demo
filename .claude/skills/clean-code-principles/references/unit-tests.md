# 单元测试 (Unit Tests)

> 基于《Clean Code》第9章 - Robert C. Martin

## 核心思想

**测试代码和生产代码一样重要。** 它不是二等公民。它需要被思考、被设计、被维护。它必须像生产代码一样保持整洁。

**如果测试代码腐化了，生产代码也会腐化。保持测试整洁！**

---

## TDD 三定律 (The Three Laws of TDD)

```
┌─────────────────────────────────────────────────────────────┐
│  第一定律：在编写不能通过的单元测试前，不可编写生产代码        │
│                                                             │
│  第二定律：只可编写刚好无法通过的单元测试，不能编译也算不通过   │
│                                                             │
│  第三定律：只可编写刚好足以通过当前失败测试的生产代码          │
└─────────────────────────────────────────────────────────────┘
```

### 循环周期
- 这三条定律将你锁定在约 **30秒** 的循环中
- 测试和生产代码一起编写
- 测试只比生产代码早几秒钟

### 结果
- 每天写几十个测试
- 每月写几百个测试
- 每年写几千个测试
- 测试覆盖几乎全部生产代码

---

## 规则 1：保持测试整洁 (Keeping Tests Clean)

### 脏测试等于没有测试

> 有些团队决定测试代码不需要与生产代码保持同样的质量标准。"快而脏"是座右铭。

**后果链：**

```
脏测试 → 测试难以修改 → 修改生产代码时旧测试失败 
      → 测试成为负担 → 维护成本上升 
      → 开发者抱怨 → 最终丢弃测试套件
```

**没有测试套件后：**
- 无法确保代码修改按预期工作
- 无法确保系统某部分的修改不会破坏其他部分
- 缺陷率开始上升
- 害怕做出修改
- 停止清理生产代码
- 生产代码开始腐烂

### 测试让代码可维护

```
┌─────────────────────────────────────────────────────────────┐
│  有测试 → 不怕修改代码 → 可以改进架构和设计                   │
│                                                             │
│  没测试 → 每次修改都可能是 bug → 害怕修改 → 代码腐烂          │
└─────────────────────────────────────────────────────────────┘
```

> 单元测试让代码**可扩展、可维护、可复用**。测试覆盖率越高，恐惧越少。

---

## 规则 2：整洁的测试 (Clean Tests)

### 什么是整洁的测试？
三个词：**可读性、可读性、可读性**。

在测试中，可读性比在生产代码中更重要。要用尽可能少的表达式说清楚尽可能多的内容。

### ❌ 错误示例：难以理解的测试

```java
public void testGetPageHieratchyAsXml() throws Exception {
    crawler.addPage(root, PathParser.parse("PageOne"));
    crawler.addPage(root, PathParser.parse("PageOne.ChildOne"));
    crawler.addPage(root, PathParser.parse("PageTwo"));

    request.setResource("root");
    request.addInput("type", "pages");
    Responder responder = new SerializedPageResponder();
    SimpleResponse response =
        (SimpleResponse) responder.makeResponse(
            new FitNesseContext(root), request);
    String xml = response.getContent();

    assertEquals("text/xml", response.getContentType());
    assertSubString("<name>PageOne</name>", xml);
    assertSubString("<name>PageTwo</name>", xml);
    assertSubString("<name>ChildOne</name>", xml);
}
```

**问题**：
- 大量重复代码
- 充满干扰测试表达力的细节
- `PathParser` 转换与测试无关
- 创建 responder 和处理 response 的细节是噪音

### ✅ 正确示例：BUILD-OPERATE-CHECK 模式

```java
public void testGetPageHierarchyAsXml() throws Exception {
    makePages("PageOne", "PageOne.ChildOne", "PageTwo");

    submitRequest("root", "type:pages");

    assertResponseIsXML();
    assertResponseContains(
        "<name>PageOne</name>", "<name>PageTwo</name>", "<name>ChildOne</name>"
    );
}

public void testSymbolicLinksAreNotInXmlPageHierarchy() throws Exception {
    WikiPage page = makePage("PageOne");
    makePages("PageOne.ChildOne", "PageTwo");

    addLinkTo(page, "PageTwo", "SymPage");

    submitRequest("root", "type:pages");

    assertResponseIsXML();
    assertResponseContains(
        "<name>PageOne</name>", "<name>PageTwo</name>", "<name>ChildOne</name>"
    );
    assertResponseDoesNotContain("SymPage");
}

public void testGetDataAsXml() throws Exception {
    makePageWithContent("TestPageOne", "test page");

    submitRequest("TestPageOne", "type:data");

    assertResponseIsXML();
    assertResponseContains("test page", "<Test");
}
```

### BUILD-OPERATE-CHECK 模式

```
┌─────────────────────────────────────────────────────────────┐
│  BUILD    - 构造测试数据                                     │
│  OPERATE  - 操作测试数据                                     │
│  CHECK    - 检查操作产生的预期结果                            │
└─────────────────────────────────────────────────────────────┘
```

---

## 规则 3：领域特定测试语言 (Domain-Specific Testing Language)

### 原则
为测试构建一套领域特定语言，而非使用程序员操作系统的 API。

### 示例

```java
// 测试专用 API
makePages("PageOne", "PageOne.ChildOne", "PageTwo");
submitRequest("root", "type:pages");
assertResponseIsXML();
assertResponseContains("<name>PageOne</name>");
```

### 好处
- 测试更方便编写
- 测试更容易阅读
- 帮助编写测试的人
- 帮助后来阅读测试的人

### 演进方式
测试 API 不是预先设计的，而是从持续重构测试代码中演进而来。

---

## 规则 4：双重标准 (A Dual Standard)

### 原则
测试代码有不同的工程标准，但仍需**简单、精炼、富有表达力**。

测试代码不必像生产代码那样高效，因为它运行在测试环境而非生产环境。

### ❌ 原始版本

```java
@Test
public void turnOnLoTempAlarmAtThreshold() throws Exception {
    hw.setTemp(WAY_TOO_COLD);
    controller.tic();
    assertTrue(hw.heaterState());
    assertTrue(hw.blowerState());
    assertFalse(hw.coolerState());
    assertFalse(hw.hiTempAlarm());
    assertTrue(hw.loTempAlarm());
}
```

**问题**：眼睛需要在状态名和断言之间来回跳转

### ✅ 改进版本

```java
@Test
public void turnOnLoTempAlarmAtThreshold() throws Exception {
    wayTooCold();
    assertEquals("HBchL", hw.getState());
}

@Test
public void turnOnCoolerAndBlowerIfTooHot() throws Exception {
    tooHot();
    assertEquals("hBChl", hw.getState());
}

@Test
public void turnOnHeaterAndBlowerIfTooCold() throws Exception {
    tooCold();
    assertEquals("HBchl", hw.getState());
}

@Test
public void turnOnHiTempAlarmAtThreshold() throws Exception {
    wayTooHot();
    assertEquals("hBCHl", hw.getState());
}
```

### 编码约定
- 大写 = "开"，小写 = "关"
- 字母顺序：`{heater, blower, cooler, hi-temp-alarm, lo-temp-alarm}`
- `"HBchL"` = heater 开, blower 开, cooler 关, hi-temp 关, lo-temp 开

### getState() 实现

```java
public String getState() {
    String state = "";
    state += heater ? "H" : "h";
    state += blower ? "B" : "b";
    state += cooler ? "C" : "c";
    state += hiTempAlarm ? "H" : "h";
    state += loTempAlarm ? "L" : "l";
    return state;
}
```

> 这段代码效率不高（应该用 StringBuffer），但在测试环境中完全可以接受。

### 双重标准

| 方面 | 生产代码 | 测试代码 |
|------|---------|---------|
| **内存效率** | 重要 | 可以放宽 |
| **CPU效率** | 重要 | 可以放宽 |
| **整洁性** | 必须 | 同样必须 |
| **可读性** | 重要 | 更加重要 |

---

## 规则 5：每个测试一个断言？(One Assert per Test?)

### 争议
有一种观点认为每个测试函数只应有一个断言语句。

### Given-When-Then 模式

```java
public void testGetPageHierarchyAsXml() throws Exception {
    givenPages("PageOne", "PageOne.ChildOne", "PageTwo");

    whenRequestIsIssued("root", "type:pages");

    thenResponseShouldBeXML();
}

public void testGetPageHierarchyHasRightTags() throws Exception {
    givenPages("PageOne", "PageOne.ChildOne", "PageTwo");

    whenRequestIsIssued("root", "type:pages");

    thenResponseShouldContain(
        "<name>PageOne</name>", "<name>PageTwo</name>", "<name>ChildOne</name>"
    );
}
```

### 实际建议

> 单一断言是个好准则，但不必害怕在测试中使用多个断言。**最好的规则是最小化每个概念的断言数量，每个测试函数只测试一个概念。**

---

## 规则 6：每个测试一个概念 (Single Concept per Test)

### ❌ 错误示例：测试多个概念

```java
/**
 * Miscellaneous tests for the addMonths() method.
 */
public void testAddMonths() {
    SerialDate d1 = SerialDate.createInstance(31, 5, 2004);

    SerialDate d2 = SerialDate.addMonths(1, d1);
    assertEquals(30, d2.getDayOfMonth());
    assertEquals(6, d2.getMonth());
    assertEquals(2004, d2.getYYYY());

    SerialDate d3 = SerialDate.addMonths(2, d1);
    assertEquals(31, d3.getDayOfMonth());
    assertEquals(7, d3.getMonth());
    assertEquals(2004, d3.getYYYY());

    SerialDate d4 = SerialDate.addMonths(1, SerialDate.addMonths(1, d1));
    assertEquals(30, d4.getDayOfMonth());
    assertEquals(7, d4.getMonth());
    assertEquals(2004, d4.getYYYY());
}
```

### 应该拆分为

1. **测试从31天月份加一个月到30天月份**
   - 5月31日 + 1个月 = 6月30日（不是31日）

2. **测试从31天月份加两个月到31天月份**
   - 5月31日 + 2个月 = 7月31日

3. **测试从30天月份加一个月到31天月份**
   - 6月30日 + 1个月 = 7月30日（不是31日）

### 发现遗漏的测试

> 这暗示了一个通用规则：增加月份时，日期不能大于该月最后一天。这意味着 2月28日加一个月应该是3月28日。这个测试是缺失的！

---

## F.I.R.S.T. 原则

整洁测试遵循五条规则：

### **F - Fast（快速）**

```
测试应该快速运行。
  ↓
测试慢 → 不想频繁运行 → 不能及早发现问题 → 不敢清理代码 → 代码腐烂
```

### **I - Independent（独立）**

```
测试之间不应相互依赖。
  • 一个测试不应为下一个测试设置条件
  • 应能独立运行每个测试
  • 应能以任何顺序运行测试

依赖后果：第一个失败导致级联失败，难以诊断，隐藏下游缺陷
```

### **R - Repeatable（可重复）**

```
测试应在任何环境中可重复。
  • 生产环境
  • QA环境
  • 没有网络的笔记本电脑上

不可重复后果：总有借口解释为什么失败，环境不可用时无法运行测试
```

### **S - Self-Validating（自足验证）**

```
测试应有布尔值输出 —— 要么通过，要么失败。
  • 不应需要阅读日志文件来判断是否通过
  • 不应需要手动比较两个文本文件

不自验证后果：失败变得主观，运行测试需要长时间的手动评估
```

### **T - Timely（及时）**

```
测试应及时编写。
  • 单元测试应该恰好在使其通过的生产代码之前编写
  
之后写的后果：
  • 发现生产代码难以测试
  • 认为某些代码太难测试
  • 没有把生产代码设计成可测试的
```

---

## 测试模式总结

### BUILD-OPERATE-CHECK (构建-操作-检查)

```java
@Test
public void testOrderTotal() {
    // BUILD
    Order order = new Order();
    order.addItem(new Item("Widget", 10.00));
    order.addItem(new Item("Gadget", 25.00));
    
    // OPERATE
    double total = order.calculateTotal();
    
    // CHECK
    assertEquals(35.00, total, 0.001);
}
```

### GIVEN-WHEN-THEN (假设-当-那么)

```java
@Test
public void shouldCalculateOrderTotal() {
    // GIVEN
    givenOrderWithItems("Widget:10.00", "Gadget:25.00");
    
    // WHEN
    whenCalculatingTotal();
    
    // THEN
    thenTotalShouldBe(35.00);
}
```

### ARRANGE-ACT-ASSERT (安排-执行-断言)

```java
@Test
public void testUserAuthentication() {
    // Arrange
    User user = new User("john", "password123");
    AuthService authService = new AuthService();
    
    // Act
    boolean result = authService.authenticate(user);
    
    // Assert
    assertTrue(result);
}
```

---

## 代码审查清单

### 测试质量
- [ ] 测试代码是否与生产代码同等对待？
- [ ] 测试是否可读、简单、富有表达力？
- [ ] 是否避免了脏测试（快而脏）？

### 测试结构
- [ ] 是否遵循 BUILD-OPERATE-CHECK 或 GIVEN-WHEN-THEN 模式？
- [ ] 每个测试是否只测试一个概念？
- [ ] 是否最小化了每个概念的断言数量？

### 领域特定语言
- [ ] 是否构建了测试专用 API？
- [ ] 测试是否使用领域语言而非底层 API？
- [ ] 测试 API 是否随时间演进和重构？

### F.I.R.S.T. 原则
- [ ] **Fast**: 测试是否快速运行？
- [ ] **Independent**: 测试之间是否独立？
- [ ] **Repeatable**: 测试是否在任何环境可重复？
- [ ] **Self-Validating**: 测试是否有明确的通过/失败结果？
- [ ] **Timely**: 测试是否在生产代码之前编写？

---

## 核心箴言

> **测试代码和生产代码一样重要。它不是二等公民。**

> **有测试套件覆盖生产代码，是保持设计和架构尽可能整洁的关键。测试带来可变化性。**

> **如果测试代码腐化了，生产代码也会腐化。保持测试整洁！**

> **什么是整洁的测试？三个词：可读性、可读性、可读性。**
