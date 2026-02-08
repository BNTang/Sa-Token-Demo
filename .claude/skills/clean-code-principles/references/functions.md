# 函数编写规范 (Clean Code Chapter 3)

> 函数是所有程序中的第一组代码。大师级程序员把系统当作故事来讲，而不是当作程序来写。

---

## 1. 短小 (Small!)

### 核心规则

- **第一规则**：函数应该短小
- **第二规则**：函数还要更短小
- **理想长度**：20 行封顶最佳
- **极致目标**：每个函数只有 2-4 行

### 代码块和缩进

```java
// ✅ 正确：代码块只有一行，且是函数调用
public void processOrder(Order order) {
    if (isValidOrder(order)) {
        executeOrder(order);
    }
}

// ❌ 错误：代码块过长，嵌套过深
public void processOrder(Order order) {
    if (order != null) {
        if (order.getItems() != null) {
            for (Item item : order.getItems()) {
                if (item.getPrice() > 0) {
                    // 更多嵌套...
                }
            }
        }
    }
}
```

### 缩进规则

- 函数的缩进层级不该多于一层或两层
- if/else/while 语句中的代码块应该只有一行（函数调用）
- 函数不应该大到足以容纳嵌套结构

---

## 2. 只做一件事 (Do One Thing)

### 黄金法则

> **函数应该做一件事。做好这件事。只做这一件事。**

### 如何判断函数是否只做一件事

1. **抽象层级测试**：函数中的语句都在同一抽象层级
2. **TO 段落测试**：能用简洁的 TO 起头段落描述
3. **提取测试**：无法再拆出一个非重新诠释实现的函数

```java
// ✅ 只做一件事：将设置和拆解包纳到测试页面
public static String renderPageWithSetupsAndTeardowns(
    PageData pageData, boolean isSuite) throws Exception {
    if (isTestPage(pageData)) {
        includeSetupAndTeardownPages(pageData, isSuite);
    }
    return pageData.getHtml();
}
```

### 函数中的区段

- 如果函数能被切分为 declarations、initializations、sieve 等区段
- 这是函数做事太多的明显征兆
- **只做一件事的函数无法被合理地切分为多个区段**

---

## 3. 每个函数一个抽象层级

### 抽象层级混杂的问题

```java
// ❌ 混杂不同抽象层级
public void process() {
    getHtml();                                    // 高层抽象
    String path = PathParser.render(pagePath);   // 中层抽象
    buffer.append("\n");                          // 低层抽象
}
```

### 向下规则 (The Stepdown Rule)

- 代码应该像自顶向下的叙事
- 每个函数后面都跟着下一抽象层级的函数
- 像一系列 TO 起头的段落

```
TO includeSetupAndTeardownPages：
    我们先容纳设置步骤，然后纳入测试页面内容，再纳入分拆步骤。

TO includeSetupPages：
    如果是套件，就纳入套件设置步骤，然后再纳入普通设置步骤。

TO includeSuiteSetupPage：
    先搜索"SuiteSetUp"页面的上级继承关系，再添加一个包括该页面路径的语句。
```

---

## 4. Switch 语句

### 问题分析

Switch 语句天生存在以下问题：
1. **太长**：新类型增加时会更长
2. **做多件事**：违反单一职责
3. **违反 SRP**：有多个修改理由
4. **违反 OCP**：添加新类型时必须修改

```java
// ❌ 有问题的 switch
public Money calculatePay(Employee e) throws InvalidEmployeeType {
    switch (e.type) {
        case COMMISSIONED: return calculateCommissionedPay(e);
        case HOURLY: return calculateHourlyPay(e);
        case SALARIED: return calculateSalariedPay(e);
        default: throw new InvalidEmployeeType(e.type);
    }
}
```

### 解决方案：使用多态

```java
// ✅ 使用抽象工厂 + 多态
public abstract class Employee {
    public abstract boolean isPayday();
    public abstract Money calculatePay();
    public abstract void deliverPay(Money pay);
}

public interface EmployeeFactory {
    Employee makeEmployee(EmployeeRecord r) throws InvalidEmployeeType;
}

public class EmployeeFactoryImpl implements EmployeeFactory {
    public Employee makeEmployee(EmployeeRecord r) throws InvalidEmployeeType {
        switch (r.type) {  // switch 只出现一次，隐藏在工厂中
            case COMMISSIONED: return new CommissionedEmployee(r);
            case HOURLY: return new HourlyEmployee(r);
            case SALARIED: return new SalariedEmployee(r);
            default: throw new InvalidEmployeeType(r.type);
        }
    }
}
```

### Switch 使用规则

Switch 语句可以容忍的条件：
- 只出现一次
- 用于创建多态对象
- 隐藏在继承关系中
- 系统其他部分看不到

---

## 5. 使用描述性的名称

### 命名原则

| 原则 | 说明 |
|-----|------|
| 长名称优于短名称 | 长而具有描述性 > 短而令人费解 |
| 长名称优于长注释 | 好名称可以替代注释 |
| 花时间取名 | 尝试不同名称，测试阅读效果 |
| 保持一致 | 使用相同的短语、名词和动词 |

### 一致性命名示例

```java
// ✅ 一脉相承的命名
includeSetupAndTeardownPages()
includeSetupPages()
includeSuiteSetupPage()
includeSetupPage()
includeTeardownPages()
includeSuiteTeardownPage()
includeTeardownPage()
```

---

## 6. 函数参数

### 参数数量规则

| 参数数量 | 评价 | 建议 |
|---------|------|------|
| 零参数 (niladic) | 最理想 | 首选 |
| 一参数 (monadic) | 好 | 推荐 |
| 二参数 (dyadic) | 可接受 | 需小心 |
| 三参数 (triadic) | 应避免 | 尽量不用 |
| 多参数 (polyadic) | 禁止 | 绝不使用 |

### 一元函数的普遍形式

```java
// 1. 询问型：问关于参数的问题
boolean fileExists("MyFile")

// 2. 转换型：转换参数并返回
InputStream fileOpen("MyFile")

// 3. 事件型：使用参数修改系统状态（无返回值）
void passwordAttemptFailedNtimes(int attempts)
```

### 标识参数 (Flag Arguments)

**标识参数丑陋不堪！向函数传入布尔值是骇人听闻的做法。**

```java
// ❌ 使用标识参数
render(true)
render(boolean isSuite)

// ✅ 拆分为两个函数
renderForSuite()
renderForSingleTest()
```

### 二元函数的问题

```java
// ❌ 二元函数：需要暂停思考
writeField(outputStream, name)

// ✅ 转为一元函数
outputStream.writeField(name)

// ✅ 自然的二元函数（有序组成部分）
Point p = new Point(0, 0)  // 笛卡儿点天生拥有两个参数
```

### 参数对象

```java
// ❌ 过多参数
Circle makeCircle(double x, double y, double radius)

// ✅ 封装为对象
Circle makeCircle(Point center, double radius)
```

### 动词与关键字

```java
// 动词/名词对形式
write(name)
writeField(name)  // 更好：告诉我们 name 是一个 field

// 关键字形式：把参数名编码进函数名
assertExpectedEqualsActual(expected, actual)  // 减轻记忆参数顺序的负担
```

---

## 7. 无副作用

### 副作用是谎言

函数承诺只做一件事，但还是会做其他被藏起来的事：
- 对自己类中的变量做出未能预期的改动
- 修改传递的参数
- 修改系统全局变量

```java
// ❌ 有副作用的函数
public boolean checkPassword(String userName, String password) {
    User user = UserGateway.findByName(userName);
    if (user != User.NULL) {
        String codedPhrase = user.getPhraseEncodedByPassword();
        String phrase = cryptographer.decrypt(codedPhrase, password);
        if ("Valid Password".equals(phrase)) {
            Session.initialize();  // ⚠️ 副作用！
            return true;
        }
    }
    return false;
}
```

**问题**：`checkPassword` 函数名暗示只检查密码，但它还会初始化会话，造成时序性耦合。

**修正**：如果一定要时序性耦合，就应该在函数名称中说明：
```java
checkPasswordAndInitializeSession()  // 虽然违反"只做一件事"
```

### 输出参数

```java
// ❌ 令人困惑：s 是输入还是输出？
appendFooter(s)

// ✅ 使用 this 替代输出参数
report.appendFooter()
```

**规则**：如果函数必须修改某对象的状态，让它修改其所属对象的状态。

---

## 8. 分隔指令与询问 (Command Query Separation)

**函数要么做什么事（Command），要么回答什么事（Query），但二者不可得兼。**

```java
// ❌ 混淆指令与询问
public boolean set(String attribute, String value)

if (set("username", "unclebob"))  // 是在问还是在设？

// ✅ 分隔指令与询问
if (attributeExists("username")) {    // Query
    setAttribute("username", "unclebob");  // Command
}
```

---

## 9. 使用异常替代返回错误码

### 错误码的问题

```java
// ❌ 错误码导致深层嵌套
if (deletePage(page) == E_OK) {
    if (registry.deleteReference(page.name) == E_OK) {
        if (configKeys.deleteKey(page.name.makeKey()) == E_OK) {
            logger.log("page deleted");
        } else {
            logger.log("configKey not deleted");
        }
    } else {
        logger.log("deleteReference from registry failed");
    }
} else {
    logger.log("delete failed");
    return E_ERROR;
}
```

### 使用异常

```java
// ✅ 异常让代码更清晰
try {
    deletePage(page);
    registry.deleteReference(page.name);
    configKeys.deleteKey(page.name.makeKey());
} catch (Exception e) {
    logger.log(e.getMessage());
}
```

### 抽离 Try/Catch 代码块

```java
// ✅ 抽离错误处理
public void delete(Page page) {
    try {
        deletePageAndAllReferences(page);
    } catch (Exception e) {
        logError(e);
    }
}

private void deletePageAndAllReferences(Page page) throws Exception {
    deletePage(page);
    registry.deleteReference(page.name);
    configKeys.deleteKey(page.name.makeKey());
}

private void logError(Exception e) {
    logger.log(e.getMessage());
}
```

### 错误处理就是一件事

- 处理错误的函数不该做其他事
- 如果关键字 `try` 在函数中存在，它应该是第一个单词
- `catch/finally` 代码块后面不该有其他内容

### 避免 Error.java 依赖磁铁

```java
// ❌ Error 枚举是依赖磁铁
public enum Error {
    OK, INVALID, NO_SUCH, LOCKED, OUT_OF_RESOURCES, WAITING_FOR_EVENT;
}
// 修改 Error 枚举时，所有使用它的类都需要重新编译和部署

// ✅ 使用异常类，可以从异常类派生新异常
// 无需重新编译或重新部署（符合 OCP）
```

---

## 10. 别重复自己 (Don't Repeat Yourself - DRY)

> **重复可能是软件中一切邪恶的根源。**

### 消除重复的策略

| 技术 | 用途 |
|-----|------|
| 提取方法 | 将重复代码抽取为独立函数 |
| 数据库范式 | 消灭数据重复 |
| 面向对象 | 将代码集中到基类 |
| 面向方面编程 (AOP) | 横切关注点的重复消除 |
| 面向组件编程 | 模块级别的重复消除 |

---

## 11. 结构化编程

### Dijkstra 规则

- 每个函数只有一个入口、一个出口
- 只有一个 return 语句
- 循环中不能有 break 或 continue
- 永远不能有 goto

### 对于小函数

**只要函数保持短小：**
- 偶尔的 return、break、continue 没有坏处
- 可能比单入单出原则更具表达力
- goto 只在大函数中才有道理，应该尽量避免

---

## 12. 如何写出这样的函数

### 写作流程

1. **初稿**：冗长而复杂，有太多缩进和嵌套循环，过长的参数列表，随意的名称，重复的代码
2. **单元测试**：配上一套单元测试，覆盖每行丑陋的代码
3. **打磨**：分解函数、修改名称、消除重复
4. **重构**：缩短和重新安置方法，有时拆散类
5. **保持测试通过**：同时保持测试通过

> 我并不从一开始就按照规则写函数。我想没人做得到。

---

## 函数编写检查清单

| # | 检查项 | 说明 |
|---|-------|------|
| 1 | 函数长度 ≤ 20 行 | 理想情况 2-4 行 |
| 2 | 缩进层级 ≤ 2 | 不要深层嵌套 |
| 3 | 只做一件事 | 同一抽象层级的步骤 |
| 4 | 抽象层级一致 | 不混杂高/中/低层抽象 |
| 5 | 参数数量 ≤ 2 | 零参数最佳 |
| 6 | 无标识参数 | 不传入布尔值 |
| 7 | 无副作用 | 不做隐藏的事 |
| 8 | 指令与询问分离 | 不同时做事和回答 |
| 9 | 使用异常而非错误码 | 分离主路径和错误处理 |
| 10 | 无重复代码 | DRY 原则 |
| 11 | 描述性命名 | 名称说明函数做什么 |
| 12 | 命名一致 | 使用相同的短语和动词 |

---

## 示例：重构后的代码

```java
public class SetupTeardownIncluder {
    private PageData pageData;
    private boolean isSuite;
    private WikiPage testPage;
    private StringBuffer newPageContent;
    private PageCrawler pageCrawler;

    public static String render(PageData pageData) throws Exception {
        return render(pageData, false);
    }

    public static String render(PageData pageData, boolean isSuite) throws Exception {
        return new SetupTeardownIncluder(pageData).render(isSuite);
    }

    private SetupTeardownIncluder(PageData pageData) {
        this.pageData = pageData;
        testPage = pageData.getWikiPage();
        pageCrawler = testPage.getPageCrawler();
        newPageContent = new StringBuffer();
    }

    private String render(boolean isSuite) throws Exception {
        this.isSuite = isSuite;
        if (isTestPage())
            includeSetupAndTeardownPages();
        return pageData.getHtml();
    }

    private boolean isTestPage() throws Exception {
        return pageData.hasAttribute("Test");
    }

    private void includeSetupAndTeardownPages() throws Exception {
        includeSetupPages();
        includePageContent();
        includeTeardownPages();
        updatePageContent();
    }

    private void includeSetupPages() throws Exception {
        if (isSuite) includeSuiteSetupPage();
        includeSetupPage();
    }

    private void includeTeardownPages() throws Exception {
        includeTeardownPage();
        if (isSuite) includeSuiteTeardownPage();
    }
    
    // ... 每个函数都只做一件事，引出下一个函数
}
```

**注意每个函数如何：**
- 保持在同一抽象层上
- 引出下一个函数
- 讲述自己那个小故事
