---
name: meaningful-names
description: 有意义的命名规范。涵盖变量、函数、类、包的命名原则与最佳实践。当编写代码、代码评审或重构命名时使用。
metadata:
  author: skill-hub
  version: "1.0"
  compatibility: 所有编程语言
  reference: "《Clean Code》Chapter 2 - Tim Ottinger"
---

# 有意义的命名

> 版本: 1.0 | 更新: 2026-02-04
>
> 选个好名字要花时间，但省下来的时间比花掉的多

---

## 概述

### 做了什么
提供一套命名的核心原则和具体规则，帮助你给变量、函数、类取出清晰、准确、有意义的名称。

### 为什么需要
- 软件中随处可见命名：变量、函数、参数、类、包、文件、目录……
- 好名称让代码自解释，不需要注释
- 糟糕的名称让读者在脑中做翻译，浪费时间

### 什么时候必须用
- 创建任何新的变量、函数、类时
- 代码评审时检查命名质量
- 重构代码时改进命名

---

## 何时使用此 Skill

| 场景 | 触发词 |
|------|--------|
| 变量命名 | 变量名、怎么命名、取名 |
| 函数命名 | 函数名、方法名 |
| 类命名 | 类名、接口名 |
| 重构命名 | 改名、重命名、命名规范 |

---

## 核心原则

### 一句话总结

> **名称应该告诉你：它为什么存在、它做什么事、应该怎么用。如果名称需要注释来补充，那就不算名副其实。**

---

## 1. 名副其实（Use Intention-Revealing Names）

### 问题空间
变量名不能表达意图，读者必须猜测代码的目的。

### 核心语义
**名称应该回答所有大问题：为什么存在、做什么事、怎么用。**

### 【强制】规则

| 规则 | 说明 |
|------|------|
| 名称必须表达意图 | 不需要注释就能理解 |
| 名称必须说明计量对象和单位 | `elapsedTimeInDays` 而非 `d` |
| 名称必须揭示业务含义 | `getFlaggedCells()` 而非 `getThem()` |

### 反例与正例

```java
// ❌ 反例 - 名称什么都没说明
int d; // elapsed time in days

// ✅ 正例 - 名称自我说明
int elapsedTimeInDays;
int daysSinceCreation;
int daysSinceModification;
int fileAgeInDays;
```

```java
// ❌ 反例 - 读者无法理解代码目的
public List<int[]> getThem() {
    List<int[]> list1 = new ArrayList<int[]>();
    for (int[] x : theList)
        if (x[0] == 4)
            list1.add(x);
    return list1;
}

// ✅ 正例 - 名称揭示意图
public List<Cell> getFlaggedCells() {
    List<Cell> flaggedCells = new ArrayList<Cell>();
    for (Cell cell : gameBoard)
        if (cell.isFlagged())
            flaggedCells.add(cell);
    return flaggedCells;
}
```

### 记忆锚点
> **如果名称需要注释，那就是烂名称。**

---

## 2. 避免误导（Avoid Disinformation）

### 问题空间
名称传递了错误的信息，让读者产生错误的理解。

### 核心语义
**名称不能包含与本意相悖的线索。**

### 【强制】规则

| 规则 | 说明 |
|------|------|
| 不用平台/语言保留词 | `hp`、`aix`、`sco` 是 Unix 平台名 |
| 不用 List 除非真是 List | `accountList` 必须是真正的 List 类型 |
| 不用外形相似的名称 | 难以区分 `XYZControllerForEfficientHandlingOfStrings` 和 `XYZControllerForEfficientStorageOfStrings` |
| 不用 l 和 O 做变量名 | 容易与数字 1 和 0 混淆 |

### 反例与正例

```java
// ❌ 反例 - 误导：不是 List 却叫 accountList
Set<Account> accountList = new HashSet<>();

// ✅ 正例 - 准确表达类型
Set<Account> accounts = new HashSet<>();
Set<Account> accountGroup = new HashSet<>();
```

```java
// ❌ 反例 - l 和 O 容易与 1 和 0 混淆
int a = l;
if (O == l)
    a = O1;
else
    l = 01;

// ✅ 正例 - 使用清晰的名称
int result = length;
if (zero == length)
    result = zeroOne;
else
    length = one;
```

### 记忆锚点
> **名称不能骗人。**

---

## 3. 做有意义的区分（Make Meaningful Distinctions）

### 问题空间
为了让编译器通过而随意改名，导致名称无法区分。

### 核心语义
**如果名称必须不同，那意思也应该不同。**

### 【强制】规则

| 规则 | 说明 |
|------|------|
| 不用数字系列命名 | `a1, a2, a3` 毫无意义 |
| 不用废话区分 | `ProductInfo` vs `ProductData` 无法区分 |
| 不用冗余词 | `nameString`（Name 不可能是浮点数）、`customerObject` |

### 废话词列表

| ❌ 废话词 | 说明 |
|---------|------|
| Info | `ProductInfo` vs `Product` 无区别 |
| Data | `AccountData` vs `Account` 无区别 |
| a, an, the | 除非有明确约定 |
| variable | 变量名中不该有 variable |
| table | 表名中不该有 table |
| String | `nameString` 多余 |
| Object | `customerObject` 多余 |

### 反例与正例

```java
// ❌ 反例 - 数字系列，毫无意义
public static void copyChars(char a1[], char a2[]) {
    for (int i = 0; i < a1.length; i++) {
        a2[i] = a1[i];
    }
}

// ✅ 正例 - 有意义的区分
public static void copyChars(char source[], char destination[]) {
    for (int i = 0; i < source.length; i++) {
        destination[i] = source[i];
    }
}
```

```java
// ❌ 反例 - 无法区分该调用哪个
getActiveAccount();
getActiveAccounts();
getActiveAccountInfo();

// ✅ 正例 - 清晰区分
getActiveAccount();          // 获取单个账户
getActiveAccountList();      // 获取账户列表
getActiveAccountSummary();   // 获取账户摘要
```

### 记忆锚点
> **区分名称要让读者知道不同之处是什么。**

---

## 4. 使用读得出来的名称（Use Pronounceable Names）

### 问题空间
名称读不出来，无法在团队中讨论。

### 核心语义
**编程是社会活动，名称必须能读出来讨论。**

### 【强制】规则

| 规则 | 说明 |
|------|------|
| 名称必须可发音 | 方便口头讨论 |
| 不用自造缩写 | `genymdhms` 读不出来 |
| 使用完整单词 | `generationTimestamp` 而非 `gentstmp` |

### 反例与正例

```java
// ❌ 反例 - 读不出来，讨论时像傻子
class DtaRcrd102 {
    private Date genymdhms;      // generation year month day hour minute second
    private Date modymdhms;
    private final String pszqint = "102";
}

// ✅ 正例 - 像人话一样可以讨论
class Customer {
    private Date generationTimestamp;
    private Date modificationTimestamp;
    private final String recordId = "102";
}
```

### 对话示例

```
// ❌ 用烂名称讨论
"嘿，这儿，鼻涕阿三喜摁踢上头，有个皮挨死极翘整数，看见没？"

// ✅ 用好名称讨论
"嘿，Mikey，看看这条记录！生成时间戳被设置为明天了！不能这样吧？"
```

### 记忆锚点
> **能读出来才能讨论。**

---

## 5. 使用可搜索的名称（Use Searchable Names）

### 问题空间
单字母和魔法数字在代码中无法搜索定位。

### 核心语义
**名称长短应与作用域大小相对应。作用域越大，名称应越长、越可搜索。**

### 【强制】规则

| 规则 | 说明 |
|------|------|
| 常量必须命名 | `WORK_DAYS_PER_WEEK` 而非 `5` |
| 全局变量用长名称 | 便于搜索 |
| 局部变量可用短名称 | 仅在短方法内使用 |
| 循环变量可用 i, j, k | 但绝不用 l（容易与 1 混淆） |

### 反例与正例

```java
// ❌ 反例 - 无法搜索，魔法数字含义不明
for (int j = 0; j < 34; j++) {
    s += (t[j] * 4) / 5;
}

// ✅ 正例 - 可搜索，含义清晰
int realDaysPerIdealDay = 4;
final int WORK_DAYS_PER_WEEK = 5;
int sum = 0;
for (int j = 0; j < NUMBER_OF_TASKS; j++) {
    int realTaskDays = taskEstimate[j] * realDaysPerIdealDay;
    int realTaskWeeks = realTaskDays / WORK_DAYS_PER_WEEK;
    sum += realTaskWeeks;
}
```

### 记忆锚点
> **grep 能找到的名称才是好名称。**

---

## 6. 避免使用编码（Avoid Encodings）

### 问题空间
名称中嵌入类型或作用域信息，增加解码负担。

### 核心语义
**现代 IDE 和强类型语言让编码变得多余。**

### 【强制】规则

| 规则 | 说明 |
|------|------|
| 不用匈牙利命名法 | `strName`、`iCount` 已过时 |
| 不用成员前缀 | `m_description` 多余 |
| 接口不加 I 前缀 | `ShapeFactory` 而非 `IShapeFactory` |

### 反例与正例

```java
// ❌ 反例 - 匈牙利命名法（已过时）
PhoneNumber phoneString;  // 类型改了，名字忘改了！
String strName;
int iCount;

// ✅ 正例 - 不编码类型
PhoneNumber phone;
String name;
int count;
```

```java
// ❌ 反例 - 成员前缀
public class Part {
    private String m_dsc;  // The textual description
    
    void setName(String name) {
        m_dsc = name;
    }
}

// ✅ 正例 - 无前缀，用 this 区分
public class Part {
    private String description;
    
    void setDescription(String description) {
        this.description = description;
    }
}
```

```java
// ❌ 反例 - 接口加 I 前缀
public interface IShapeFactory { }
public class ShapeFactory implements IShapeFactory { }

// ✅ 正例 - 接口不加前缀，实现加后缀
public interface ShapeFactory { }
public class ShapeFactoryImpl implements ShapeFactory { }
```

### 记忆锚点
> **编译器和 IDE 记得类型，你不需要在名称里重复。**

---

## 7. 避免思维映射（Avoid Mental Mapping）

### 问题空间
读者必须在脑中把你的名称翻译成他们熟知的概念。

### 核心语义
**明确是王道。专业程序员写其他人能理解的代码。**

### 【强制】规则

| 规则 | 说明 |
|------|------|
| 不用单字母变量（循环除外） | `r` 代表 url？需要翻译 |
| 不炫耀聪明 | 用 `url` 而非用 `r` 显得自己聪明 |
| 名称直接表达概念 | 不需要读者"翻译" |

### 反例与正例

```java
// ❌ 反例 - 需要思维映射
// r 是不包含主机名和图式的小写版 url...你能记住吗？
String r = url.toLowerCase().replaceFirst("https?://[^/]+", "");

// ✅ 正例 - 直接表达
String urlPathWithoutHost = url.toLowerCase()
    .replaceFirst("https?://[^/]+", "");
```

### 记忆锚点
> **聪明程序员炫技，专业程序员写易懂代码。**

---

## 8. 类名与方法名（Class Names & Method Names）

### 【强制】类名规则

| 规则 | 说明 |
|------|------|
| 类名用名词或名词短语 | `Customer`、`WikiPage`、`Account`、`AddressParser` |
| 避免 Manager、Processor、Data、Info | 这些词太宽泛 |
| 类名不用动词 | 动词是方法的事 |

### 【强制】方法名规则

| 规则 | 说明 |
|------|------|
| 方法名用动词或动词短语 | `postPayment`、`deletePage`、`save` |
| 访问器用 get 前缀 | `getName()` |
| 修改器用 set 前缀 | `setName()` |
| 断言用 is 前缀 | `isPosted()` |

### 反例与正例

```java
// ❌ 反例 - 类名用动词
class ProcessPayment { }
class HandleUser { }

// ✅ 正例 - 类名用名词
class PaymentProcessor { }
class UserHandler { }
```

```java
// ✅ 正例 - 方法命名
String name = employee.getName();
customer.setName("mike");
if (paycheck.isPosted()) { }
```

### 静态工厂方法

```java
// ❌ 不够清晰
Complex fulcrumPoint = new Complex(23.0);

// ✅ 更清晰 - 静态工厂方法描述参数含义
Complex fulcrumPoint = Complex.fromRealNumber(23.0);
```

---

## 9. 别扮可爱（Don't Be Cute）

### 问题空间
名称太俏皮，只有作者能理解。

### 核心语义
**言到意到，意到言到。宁可明确，毋为好玩。**

### 【强制】规则

| 规则 | 说明 |
|------|------|
| 不用俚语 | `whack()` → `kill()` |
| 不用文化梗 | `eatMyShorts()` → `abort()` |
| 不用只有自己懂的笑话 | `HolyHandGrenade` → `deleteItems()` |

### 反例与正例

```java
// ❌ 反例 - 谁知道这是干嘛的？
void whack() { }
void holyHandGrenade() { }
void eatMyShorts() { }

// ✅ 正例 - 清晰明确
void kill() { }
void deleteItems() { }
void abort() { }
```

### 记忆锚点
> **代码是给人看的，不是给自己逗乐的。**

---

## 10. 每个概念对应一个词（Pick One Word per Concept）

### 问题空间
同一概念用不同的词，读者要记住是哪个库用哪个词。

### 核心语义
**一个抽象概念对应一个词，全项目一以贯之。**

### 【强制】规则

| 规则 | 说明 |
|------|------|
| 获取数据统一用词 | 不要混用 `fetch`、`retrieve`、`get` |
| 控制器统一用词 | 不要混用 `controller`、`manager`、`driver` |
| 团队约定术语表 | 统一全项目命名 |

### 反例与正例

```java
// ❌ 反例 - 同一概念用不同词
class UserController { }
class AccountManager { }
class DeviceDriver { }

// ✅ 正例 - 统一用词
class UserController { }
class AccountController { }
class DeviceController { }
```

```java
// ❌ 反例 - 获取数据用三种词
userService.fetchUser();
orderService.retrieveOrder();
productService.getProduct();

// ✅ 正例 - 统一用 get
userService.getUser();
orderService.getOrder();
productService.getProduct();
```

### 记忆锚点
> **一个概念一个词，全项目保持一致。**

---

## 11. 别用双关语（Don't Pun）

### 问题空间
同一个词用于不同语义的操作。

### 核心语义
**一词一义。如果语义不同，就用不同的词。**

### 【强制】规则

| 规则 | 说明 |
|------|------|
| add 只用于相加或连接 | 两个值合并成新值 |
| insert/append 用于放入集合 | 把一个值放入容器 |
| 语义不同就用不同词 | 不要为了"一致"而用错词 |

### 反例与正例

```java
// 场景1：add 用于两个值相加
public int add(int a, int b) {
    return a + b;
}

// 场景2：把元素放入集合
// ❌ 反例 - 为了"一致"也叫 add，但语义不同
public void add(Element element) {
    collection.add(element);
}

// ✅ 正例 - 语义不同就用不同词
public void insert(Element element) {
    collection.add(element);
}

public void append(Element element) {
    collection.add(element);
}
```

### 记忆锚点
> **同一个词只能有一个意思。**

---

## 12. 使用领域名称（Use Domain Names）

### 解决方案领域 vs 问题领域

| 领域 | 说明 | 示例 |
|------|------|------|
| 解决方案领域 | 技术术语，程序员熟悉 | `Queue`、`Visitor`、`Factory`、`Iterator` |
| 问题领域 | 业务术语，需要领域专家 | `Invoice`、`Policy`、`Claim` |

### 【推荐】规则

| 规则 | 说明 |
|------|------|
| 优先用技术术语 | 程序员不需要问客户 |
| 技术术语不够就用业务术语 | 维护者可以问领域专家 |
| 分离技术代码和业务代码 | 各用各的术语 |

### 示例

```java
// ✅ 解决方案领域名称 - 程序员一看就懂
class AccountVisitor { }      // 访问者模式
class JobQueue { }            // 队列数据结构
class UserFactory { }         // 工厂模式

// ✅ 问题领域名称 - 与业务概念对应
class InsurancePolicy { }     // 保险单
class ClaimProcessor { }      // 理赔处理
class PremiumCalculator { }   // 保费计算
```

### 记忆锚点
> **技术代码用技术名，业务代码用业务名。**

---

## 13. 添加有意义的语境（Add Meaningful Context）

### 问题空间
名称脱离上下文无法理解。

### 核心语义
**用类、函数或命名空间给名称提供语境。**

### 【推荐】规则

| 规则 | 说明 |
|------|------|
| 用类提供语境 | 把相关变量放入类中 |
| 用函数提供语境 | 把相关变量放入函数参数 |
| 前缀是最后手段 | 能用类就别用前缀 |

### 反例与正例

```java
// ❌ 反例 - 孤立的变量，语境不明
String firstName;
String lastName;
String street;
String houseNumber;
String city;
String state;      // 这是地址的州？还是状态？
String zipcode;

// ✅ 正例 - 用类提供语境
class Address {
    String firstName;
    String lastName;
    String street;
    String houseNumber;
    String city;
    String state;      // 明确是地址的州
    String zipcode;
}
```

```java
// ❌ 反例 - 变量语境不明
private void printGuessStatistics(char candidate, int count) {
    String number;
    String verb;
    String pluralModifier;
    // ... 很长的方法
}

// ✅ 正例 - 用类提供语境
class GuessStatisticsMessage {
    private String number;
    private String verb;
    private String pluralModifier;
    
    public String make(char candidate, int count) {
        createPluralDependentMessageParts(count);
        return String.format("There %s %s %s%s", 
            verb, number, candidate, pluralModifier);
    }
    
    private void createPluralDependentMessageParts(int count) {
        if (count == 0) {
            thereAreNoLetters();
        } else if (count == 1) {
            thereIsOneLetter();
        } else {
            thereAreManyLetters(count);
        }
    }
    // ...
}
```

### 记忆锚点
> **类是最好的语境提供者。**

---

## 14. 不要添加没用的语境（Don't Add Gratuitous Context）

### 问题空间
给所有名称加上无意义的前缀，搞得 IDE 自动补全毫无用处。

### 核心语义
**短名称只要足够清楚，就比长名称好。**

### 【强制】规则

| 规则 | 说明 |
|------|------|
| 不给所有类加项目前缀 | `GSDAccountAddress` 太长 |
| 不给变量加类型后缀 | `addressString` 多余 |
| 精确但不冗余 | `PostalAddress` 而非 `GSDPostalAddress` |

### 反例与正例

```java
// ❌ 反例 - 项目前缀泛滥
// "Gas Station Deluxe" 项目
class GSDAccount { }
class GSDAddress { }
class GSDCustomer { }
// 输入 G，自动补全列出一英里长的列表...

// ✅ 正例 - 只在需要时加前缀
class Account { }
class Address { }
class Customer { }
```

```java
// ❌ 反例 - 不必要的语境
class Address {
    String addressStreet;
    String addressCity;
    String addressState;
}

// ✅ 正例 - 类名已提供语境
class Address {
    String street;
    String city;
    String state;
}
```

### 区分不同类型的地址

```java
// ✅ 用精确的类名区分，而非前缀
class PostalAddress { }     // 邮寄地址
class MacAddress { }        // MAC 地址
class WebAddress { }        // 网址
```

### 记忆锚点
> **不要让 IDE 的自动补全变成噩梦。**

---

## 命名 Checklist

### 创建新名称时检查

| # | 检查项 | 通过 |
|---|--------|------|
| 1 | 名称表达了意图吗？ | ☐ |
| 2 | 不需要注释就能理解吗？ | ☐ |
| 3 | 名称读得出来吗？ | ☐ |
| 4 | 名称能搜索到吗？ | ☐ |
| 5 | 没有误导信息吗？ | ☐ |
| 6 | 没有编码类型吗？ | ☐ |
| 7 | 与同类概念保持一致吗？ | ☐ |

### 代码评审时检查

| # | 检查项 | 通过 |
|---|--------|------|
| 1 | 变量名是名词吗？ | ☐ |
| 2 | 方法名是动词吗？ | ☐ |
| 3 | 类名是名词短语吗？ | ☐ |
| 4 | 没有双关语吗？ | ☐ |
| 5 | 有足够的语境吗？ | ☐ |
| 6 | 没有多余的语境吗？ | ☐ |

---

## 禁则速查表

| ❌ 禁止 | ✅ 正确做法 | 原因 |
|--------|-----------|------|
| `int d;` | `int elapsedTimeInDays;` | 名副其实 |
| `accountList`（非 List） | `accounts` | 避免误导 |
| `a1, a2, a3` | `source, destination` | 有意义的区分 |
| `genymdhms` | `generationTimestamp` | 读得出来 |
| `5` | `WORK_DAYS_PER_WEEK` | 可搜索 |
| `strName`, `m_desc` | `name`, `description` | 不编码 |
| 单字母变量（循环外） | 完整单词 | 避免思维映射 |
| `HolyHandGrenade()` | `deleteItems()` | 别扮可爱 |
| 混用 fetch/get/retrieve | 统一用 get | 一概念一词 |
| `GSDAddress` | `Address` | 不加无用语境 |

---

## 最后的话

> "取好名字最难的地方在于需要良好的描述技巧和共有文化背景。"

**别怕重命名**：
- 现代 IDE 让重命名几乎零成本
- 如果发现更好的名称，立即改
- 改名可能让某人吃惊，但效果立竿见影

---

## 参考资料

| 来源 | 说明 |
|------|------|
| 《Clean Code》Chapter 2 | Tim Ottinger 著，命名规范原文 |
| 《代码大全》| 命名最佳实践 |
| Google Style Guide | 各语言命名规范 |
