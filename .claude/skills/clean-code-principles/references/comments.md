# 注释规范 (Clean Code Chapter 4)

> "别给糟糕的代码加注释——重新写吧。" —— Brian W. Kernighan 与 P. J. Plaugher

---

## 核心理念

### 注释的本质

- **注释是一种必须的恶**：若编程语言足够有表达力，就不需要注释
- **注释总是一种失败**：无法找到不用注释就能表达自我的方法
- **真实只在一处地方有：代码**：只有代码能忠实地告诉你它做的事

### 为什么贬低注释？

1. **注释会撒谎**：存在的时间越久，离其所描述的代码越远，越来越变得全然错误
2. **程序员不能坚持维护注释**：代码在变动，注释并不总是随之变动
3. **不准确的注释要比没注释坏得多**：它们满口胡言，预期的东西永不能实现

---

## 4.1 注释不能美化糟糕的代码

**写注释的常见动机之一是糟糕的代码的存在。**

```java
// ❌ 错误做法：用注释解释糟糕的代码
// 我们告诉自己："喔，最好写点注释！"

// ✅ 正确做法：把代码弄干净！
// 与其花时间编写解释糟糕代码的注释，不如花时间清洁那堆糟糕的代码
```

**核心原则**：
- 带有少量注释的整洁而有表达力的代码 > 带有大量注释的零碎而复杂的代码

---

## 4.2 用代码来阐述

```java
// ❌ 需要注释来解释
// Check to see if the employee is eligible for full benefits
if ((employee.flags & HOURLY_FLAG) && (employee.age > 65))

// ✅ 代码自解释
if (employee.isEligibleForFullBenefits())
```

**只要想上那么几秒钟，就能用代码解释你大部分的意图。**

很多时候，简单到只需要创建一个描述与注释所言同一事物的函数即可。

---

## 4.3 好注释

> 唯一真正好的注释是你想办法不去写的注释。

### 4.3.1 法律信息

```java
// ✅ 版权及著作权声明
// Copyright (C) 2003,2004,2005 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the GNU General Public License version 2 or later.
```

**建议**：只要有可能，就指向一份标准许可或其他外部文档，而不要把所有条款放到注释中。

---

### 4.3.2 提供信息的注释

```java
// ✅ 解释抽象方法的返回值
// Returns an instance of the Responder being tested.
protected abstract Responder responderInstance();

// ✅ 更好的方式：用函数名传达信息
protected abstract Responder responderBeingTested();

// ✅ 解释正则表达式的格式
// format matched kk:mm:ss EEE, MMM dd, yyyy
Pattern timeMatcher = Pattern.compile(
    "\\d*:\\d*:\\d* \\w*, \\w* \\d*, \\d*");
```

**最佳实践**：如果能把代码移到某个转换日期和时间格式的类中，注释就变得多此一举了。

---

### 4.3.3 对意图的解释

```java
// ✅ 解释某个决定后面的意图
public int compareTo(Object o) {
    if (o instanceof WikiPagePath) {
        // ... 比较逻辑
    }
    return 1; // we are greater because we are the right type.
}

// ✅ 解释测试意图
// This is our best attempt to get a race condition
// by creating large number of threads.
for (int i = 0; i < 25000; i++) {
    Thread thread = new Thread(widgetBuilderThread);
    thread.start();
}
```

---

### 4.3.4 阐释

```java
// ✅ 翻译晦涩难明的参数或返回值
assertTrue(a.compareTo(a) == 0);    // a == a
assertTrue(a.compareTo(b) != 0);    // a != b
assertTrue(a.compareTo(b) == -1);   // a < b
assertTrue(b.compareTo(a) == 1);    // b > a
```

**⚠️ 风险**：阐释性注释本身可能不正确。在写这类注释之前：
1. 考虑是否还有更好的办法
2. 加倍小心地确认注释正确性

---

### 4.3.5 警示

```java
// ✅ 警告运行时间过长
// Don't run unless you have some time to kill.
public void _testWithReallyBigFile() {
    writeLinesToFile(10000000);
    // ...
}

// ✅ 警告线程安全问题
public static SimpleDateFormat makeStandardHttpDateFormat() {
    // SimpleDateFormat is not thread safe,
    // so we need to create each instance independently.
    SimpleDateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
    df.setTimeZone(TimeZone.getTimeZone("GMT"));
    return df;
}
```

---

### 4.3.6 TODO 注释

```java
// ✅ 解释为什么函数实现部分无所作为
// TODO-MdM these are not needed
// We expect this to go away when we do the checkout model
protected VersionInfo makeVersion() throws Exception {
    return null;
}
```

**TODO 的用途**：
- 提醒删除某个不必要的特性
- 要求他人注意某个问题
- 恳请别人取个好名字
- 提示对依赖于某个计划事件的修改

**⚠️ 注意**：
- TODO 不是在系统中留下糟糕代码的借口
- 要定期查看，删除不再需要的 TODO

---

### 4.3.7 放大

```java
// ✅ 放大某种看来不合理之物的重要性
String listItemContent = match.group(3).trim();
// the trim is real important. It removes the starting
// spaces that could cause the item to be recognized
// as another list.
new ListItemWidget(this, listItemContent, this.level + 1);
```

---

### 4.3.8 公共 API 中的 Javadoc

```java
// ✅ 为公共 API 编写良好的 Javadoc
/**
 * Creates a new user with the specified name and email.
 * 
 * @param name the user's full name, must not be null
 * @param email the user's email address, must be valid
 * @return the newly created User object
 * @throws IllegalArgumentException if name or email is invalid
 */
public User createUser(String name, String email) {
    // ...
}
```

**提醒**：Javadoc 也可能误导、不适用或者提供错误信息。

---

## 4.4 坏注释

> 大多数注释都属此类。通常，坏注释都是糟糕代码的支撑或借口，基本上等于程序员自说自话。

### 4.4.1 喃喃自语

```java
// ❌ 含糊不清的注释
public void loadProperties() {
    try {
        String propertiesPath = propertiesLocation + "/" + PROPERTIES_FILE;
        FileInputStream propertiesStream = new FileInputStream(propertiesPath);
        loadedProperties.load(propertiesStream);
    } catch (IOException e) {
        // No properties files means all defaults are loaded
        // ⚠️ 谁来装载默认设置？在哪里装载？
    }
}
```

**问题**：任何迫使读者查看其他模块的注释，都没能与读者沟通好，不值所费。

---

### 4.4.2 多余的注释

```java
// ❌ 多余：读注释花的时间比读代码还长
// Utility method that returns when this.closed is true. 
// Throws an exception if the timeout is reached.
public synchronized void waitForClose(final long timeoutMillis) throws Exception {
    if (!closed) {
        wait(timeoutMillis);
        if (!closed)
            throw new Exception("MockResponseSender could not be closed");
    }
}

// ❌ 多余的 Javadoc（Tomcat 示例）
/**
 * The processor delay for this component.
 */
protected int backgroundProcessorDelay = -1;

/**
 * The container event listeners for this Container.
 */
protected ArrayList listeners = new ArrayList();
```

---

### 4.4.3 误导性注释

```java
// ❌ 误导：注释说"returns when this.closed becomes true"
// 实际上：只在判断到 this.closed 为 true 时返回，否则等待超时后抛异常
// Utility method that returns when this.closed is true.
public synchronized void waitForClose(final long timeoutMillis) throws Exception {
    if (!closed) {
        wait(timeoutMillis);
        if (!closed)
            throw new Exception("MockResponseSender could not be closed");
    }
}
```

**危害**：可能导致其他程序员快活地调用这个函数，期望在条件满足时立即返回，结果陷于调试困境。

---

### 4.4.4 循规式注释

```java
// ❌ 愚蠢的规矩：每个函数都要有 Javadoc
/**
 * @param title The title of the CD
 * @param author The author of the CD
 * @param tracks The number of tracks on the CD
 * @param durationInMinutes The duration of the CD in minutes
 */
public void addCD(String title, String author, int tracks, int durationInMinutes) {
    CD cd = new CD();
    cd.title = title;
    cd.author = author;
    cd.tracks = tracks;
    cd.duration = duration;
    cdList.add(cd);
}
```

**问题**：这类注释徒然让代码变得散乱，满口胡言，令人迷惑不解。

---

### 4.4.5 日志式注释

```java
// ❌ 在模块开头记录每次修改
/**
 * Changes (from 11-Oct-2001)
 * --------------------------
 * 11-Oct-2001 : Re-organised the class and moved it to new package (DG);
 * 05-Nov-2001 : Added a getDescription() method (DG);
 * 12-Nov-2001 : IBD requires setDescription() method (DG);
 * 05-Dec-2001 : Fixed bug in SpreadsheetDate class (DG);
 * ...
 */
```

**解决方案**：使用源代码控制系统（Git）记录历史，删除这类注释。

---

### 4.4.6 废话注释

```java
// ❌ 废话：对显然之事喋喋不休
/**
 * Default constructor.
 */
protected AnnualDateRule() {
}

/** The day of the month. */
private int dayOfMonth;

/**
 * Returns the day of the month.
 * @return the day of the month.
 */
public int getDayOfMonth() {
    return dayOfMonth;
}
```

---

### 4.4.7 可怕的废话

```java
// ❌ 剪切-粘贴错误
/** The name. */
private String name;

/** The version. */
private String version;

/** The licenceName. */
private String licenceName;

/** The version. */  // ⚠️ 复制粘贴错误！应该是 info
private String info;
```

**教训**：如果作者在写注释时都没花心思，怎么能指望读者从中获益？

---

### 4.4.8 能用函数或变量时就别用注释

```java
// ❌ 用注释解释复杂表达式
// does the module from the global list <mod> depend on the
// subsystem we are part of?
if (smodule.getDependSubsystems().contains(subSysMod.getSubSystem()))

// ✅ 用变量替代注释
ArrayList moduleDependees = smodule.getDependSubsystems();
String ourSubSystem = subSysMod.getSubSystem();
if (moduleDependees.contains(ourSubSystem))
```

---

### 4.4.9 位置标记

```java
// ❌ 无用的位置标记
// Actions //////////////////////////////////

// ✅ 规则：尽量少用标记栏，只在特别有价值的时候用
// 如果滥用标记栏，就会沉没在背景噪音中，被忽略掉
```

---

### 4.4.10 括号后面的注释

```java
// ❌ 在括号后面放置注释
public class wc {
    public static void main(String[] args) {
        try {
            while ((line = in.readLine()) != null) {
                lineCount++;
                // ...
            } //while
            System.out.println("wordCount = " + wordCount);
        } // try
        catch (IOException e) {
            System.err.println("Error:" + e.getMessage());
        } //catch
    } //main
}
```

**解决方案**：如果你发现自己想标记右括号，其实应该做的是**缩短函数**。

---

### 4.4.11 归属与署名

```java
// ❌ 用注释署名
/* Added by Rick */

// ✅ 使用源代码控制系统
// Git 非常善于记住是谁在何时添加了什么
```

---

### 4.4.12 注释掉的代码

```java
// ❌ 直接把代码注释掉
InputStreamResponse response = new InputStreamResponse();
response.setBody(formatter.getResultStream(), formatter.getByteCount());
// InputStream resultsStream = formatter.getResultStream();
// StreamReader reader = new StreamReader(resultsStream);
// response.setContent(reader.read(formatter.getByteCount()));
```

**问题**：其他人不敢删除注释掉的代码，认为它一定有其原因。

**解决方案**：源代码控制系统可以为我们记住不要的代码，**删掉即可，它们丢不了**。

---

### 4.4.13 HTML 注释

```java
// ❌ 在源代码注释中使用 HTML 标记
/**
 * Task to run fit tests.
 * <p/>
 * <pre>
 * Usage:
 * &lt;taskdef name=&quot;execute-fitnesse-tests&quot;
 *     classname=&quot;fitnesse.ant.ExecuteFitnesseTestsTask&quot;
 *     classpathref=&quot;classpath&quot; /&gt;
 * </pre>
 */
```

**问题**：HTML 标记使得注释在编辑器/IDE 中难以卒读。

**解决方案**：该是工具（Javadoc）而非程序员来负责给注释加上合适的 HTML 标签。

---

### 4.4.14 非本地信息

```java
// ❌ 注释描述的是系统其他地方的信息
/**
 * Port on which fitnesse would run. Defaults to 8082.
 * ⚠️ 该函数完全没控制到那个所谓默认值！
 * @param fitnessePort
 */
public void setFitnessePort(int fitnessePort) {
    this.fitnessePort = fitnessePort;
}
```

**规则**：请确保注释描述了离它最近的代码。

---

### 4.4.15 信息过多

```java
// ❌ 添加无关的历史性话题
/*
    RFC 2045 - Multipurpose Internet Mail Extensions (MIME)
    Part One: Format of Internet Message Bodies
    section 6.8. Base64 Content-Transfer-Encoding
    The encoding process represents 24-bit groups of input bits as output
    strings of 4 encoded characters. Proceeding from left to right, a
    24-bit input group is formed by concatenating 3 8-bit input groups.
    These 24 bits are then treated as 4 concatenated 6-bit groups, each
    of which is translated into a single digit in the base64 alphabet.
    ...
*/
```

---

### 4.4.16 不明显的联系

```java
// ❌ 注释与代码的联系不明显
/*
 * start with an array that is big enough to hold all the pixels
 * (plus filter bytes), and an extra 200 bytes for header info
 */
this.pngBytes = new byte[((this.width + 1) * this.height * 3) + 200];
// ⚠️ 过滤器字节是什么？与 +1 有关系吗？还是与 *3 有关？为什么用 200？
```

**规则**：注释的作用是解释未能自行解释的代码。如果注释本身还需要解释，就太遗憾了。

---

### 4.4.17 函数头

```java
// ❌ 为短函数写函数头注释
/**
 * Gets the name of the user.
 * @return the name
 */
public String getName() {
    return name;
}
```

**规则**：短函数不需要太多描述。为只做一件事的短函数选个好名字，通常要比写函数头注释要好。

---

### 4.4.18 非公共代码中的 Javadoc

```java
// ❌ 为内部代码写 Javadoc
/**
 * Internal helper method to calculate the sum.
 * @param a first number
 * @param b second number
 * @return the sum
 */
private int add(int a, int b) {
    return a + b;
}
```

**规则**：Javadoc 对于公共 API 非常有用，但对于不打算作公共用途的代码就令人厌恶了。

---

## 注释检查清单

### 好注释 ✅

| 类型 | 使用场景 |
|------|----------|
| 法律信息 | 版权声明，指向标准许可 |
| 提供信息 | 解释正则表达式格式等 |
| 对意图的解释 | 解释决定背后的原因 |
| 阐释 | 翻译晦涩的参数或返回值 |
| 警示 | 警告后果（线程安全、运行时间等） |
| TODO | 提醒未完成的工作 |
| 放大 | 强调某些看似不重要但实际重要的代码 |
| 公共 API 的 Javadoc | 为外部使用者提供文档 |

### 坏注释 ❌

| 类型 | 说明 |
|------|------|
| 喃喃自语 | 含糊不清，需要查看其他代码才能理解 |
| 多余的注释 | 读注释比读代码还费时 |
| 误导性注释 | 不够精确，可能导致错误使用 |
| 循规式注释 | 为了规矩而写，没有实际价值 |
| 日志式注释 | 记录修改历史（应使用版本控制） |
| 废话注释 | 对显然之事喋喋不休 |
| 可怕的废话 | 复制粘贴错误 |
| 能用代码替代的注释 | 应该用变量或函数替代 |
| 位置标记 | 滥用会被忽略 |
| 括号后面的注释 | 应该缩短函数 |
| 归属与署名 | 应使用版本控制 |
| 注释掉的代码 | 直接删除 |
| HTML 注释 | 让工具处理格式 |
| 非本地信息 | 描述的不是附近的代码 |
| 信息过多 | 无关的细节描述 |
| 不明显的联系 | 注释本身还需要解释 |
| 函数头 | 短函数用好名字替代 |
| 非公共代码中的 Javadoc | 内部代码不需要 |

---

## 总结

| 原则 | 说明 |
|------|------|
| **代码自解释** | 用代码表达意图，不用注释 |
| **清洁代码 > 写注释** | 与其写注释解释糟糕代码，不如重写代码 |
| **注释会撒谎** | 代码变动，注释不变动，就会误导 |
| **好注释很少** | 唯一真正好的注释是想办法不去写的注释 |
| **删除无用注释** | 多余、过时、误导的注释要删除 |
| **使用版本控制** | 历史记录、署名、删除的代码交给 Git |
