# 格式规范 (Clean Code Chapter 5)

> 代码格式关乎沟通，而沟通是专业开发者的头等大事。

---

## 5.1 格式的目的

### 为什么格式很重要？

- **代码格式不可忽略，必须严肃对待**
- 你今天编写的功能，极有可能在下一版本中被修改
- **代码的可读性会对以后可能发生的修改行为产生深远影响**
- 原始代码修改之后很久，其代码风格和可读性仍会影响到可维护性和扩展性
- **即便代码已不复存在，你的风格和律条仍存活下来**

---

## 5.2 垂直格式

### 文件大小建议

| 指标 | 建议值 |
|------|-------|
| 典型文件长度 | 200 行左右 |
| 文件上限 | 500 行 |
| 理想情况 | 短文件通常比长文件易于理解 |

> 有可能用大多数为 200 行、最长 500 行的单个文件构造出色的系统（FitNesse 总长约 50000 行）

---

### 5.2.1 向报纸学习 (The Newspaper Metaphor)

源文件应该像报纸文章那样组织：

| 位置 | 内容 |
|------|------|
| **顶部（标题）** | 名称应简单且一目了然，告诉我们是否在正确的模块中 |
| **第一段（大纲）** | 高层次概念和算法 |
| **往下阅读** | 细节渐次展开 |
| **底部** | 最底层的函数和细节 |

```java
// ✅ 像报纸一样组织代码
public class OrderService {
    // 高层次：公共API
    public Order createOrder(OrderRequest request) { ... }
    public Order getOrder(Long id) { ... }
    
    // 中层次：私有业务方法
    private void validateOrder(OrderRequest request) { ... }
    private void calculateTotal(Order order) { ... }
    
    // 底层次：工具方法
    private boolean isValidAmount(BigDecimal amount) { ... }
}
```

---

### 5.2.2 概念间垂直方向上的区隔

**用空白行隔开不同的概念：**

```java
// ✅ 正确：用空白行区隔概念
package fitnesse.wikitext.widgets;

import java.util.regex.*;

public class BoldWidget extends ParentWidget {
    public static final String REGEXP = "'''.+?'''";
    private static final Pattern pattern = Pattern.compile("'''(.+?)'''",
        Pattern.MULTILINE + Pattern.DOTALL);

    public BoldWidget(ParentWidget parent, String text) throws Exception {
        super(parent);
        Matcher match = pattern.matcher(text);
        match.find();
        addChildWidgets(match.group(1));
    }

    public String render() throws Exception {
        StringBuffer html = new StringBuffer("<b>");
        html.append(childHtml()).append("</b>");
        return html.toString();
    }
}
```

```java
// ❌ 错误：没有空白行区隔
package fitnesse.wikitext.widgets;
import java.util.regex.*;
public class BoldWidget extends ParentWidget {
    public static final String REGEXP = "'''.+?'''";
    private static final Pattern pattern = Pattern.compile("'''(.+?)'''",
        Pattern.MULTILINE + Pattern.DOTALL);
    public BoldWidget(ParentWidget parent, String text) throws Exception {
        super(parent);
        Matcher match = pattern.matcher(text);
        match.find();
        addChildWidgets(match.group(1));}
    public String render() throws Exception {
        StringBuffer html = new StringBuffer("<b>");
        html.append(childHtml()).append("</b>");
        return html.toString();
    }
}
```

---

### 5.2.3 垂直方向上的靠近

**紧密相关的代码应该互相靠近：**

```java
// ❌ 错误：无用注释割断了变量间的联系
public class ReporterConfig {
    /**
     * The class name of the reporter listener
     */
    private String m_className;

    /**
     * The properties of the reporter listener
     */
    private List<Property> m_properties = new ArrayList<Property>();

    public void addProperty(Property property) {
        m_properties.add(property);
    }
}

// ✅ 正确：紧密相关的代码靠在一起
public class ReporterConfig {
    private String m_className;
    private List<Property> m_properties = new ArrayList<Property>();

    public void addProperty(Property property) {
        m_properties.add(property);
    }
}
```

---

### 5.2.4 垂直距离

#### 变量声明

**变量声明应尽可能靠近其使用位置：**

```java
// ✅ 本地变量在函数顶部
private static void readPreferences() {
    InputStream is = null;  // 声明靠近使用位置
    try {
        is = new FileInputStream(getPreferencesFile());
        setPreferences(new Properties(getPreferences()));
        getPreferences().load(is);
    } catch (IOException e) {
        // ...
    }
}

// ✅ 循环控制变量在循环语句中声明
public int countTestCases() {
    int count = 0;
    for (Test each : tests)  // each 在循环中声明
        count += each.countTestCases();
    return count;
}
```

#### 实体变量

**实体变量应该在类的顶部声明：**

```java
// ✅ 正确：实体变量在类顶部
public class WikiPageResponder implements SecureResponder {
    protected WikiPage page;
    protected PageData pageData;
    protected String pageTitle;
    protected Request request;
    protected PageCrawler crawler;

    public Response makeResponse(...) { ... }
    // ...
}

// ❌ 错误：实体变量藏在类中间
public class TestSuite implements Test {
    static public Test createTest(...) { ... }
    public static Constructor getTestConstructor(...) { ... }
    
    private String fName;  // ⚠️ 藏在这里很难发现
    private Vector<Test> fTests = new Vector<Test>(10);
    
    public TestSuite() { ... }
}
```

#### 相关函数

**调用者应该放在被调用者上面：**

```java
// ✅ 正确：调用者在上，被调用者在下
public class WikiPageResponder implements SecureResponder {
    
    public Response makeResponse(FitNesseContext context, Request request) {
        String pageName = getPageNameOrDefault(request, "FrontPage");
        loadPage(pageName, context);
        if (page == null)
            return notFoundResponse(context, request);
        else
            return makePageResponse(context);
    }

    private String getPageNameOrDefault(Request request, String defaultPageName) {
        String pageName = request.getResource();
        if (StringUtil.isBlank(pageName))
            pageName = defaultPageName;
        return pageName;
    }

    protected void loadPage(String resource, FitNesseContext context) {
        // ...
    }

    private Response notFoundResponse(FitNesseContext context, Request request) {
        return new NotFoundResponder().makeResponse(context, request);
    }

    private SimpleResponse makePageResponse(FitNesseContext context) {
        // ...
    }
}
```

#### 概念相关

**执行相似操作的函数应该放在一起：**

```java
// ✅ 概念相关的函数放在一起
public class Assert {
    static public void assertTrue(String message, boolean condition) {
        if (!condition)
            fail(message);
    }

    static public void assertTrue(boolean condition) {
        assertTrue(null, condition);
    }

    static public void assertFalse(String message, boolean condition) {
        assertTrue(message, !condition);
    }

    static public void assertFalse(boolean condition) {
        assertFalse(null, condition);
    }
}
```

---

### 5.2.5 垂直顺序

| 原则 | 说明 |
|------|------|
| 自上向下 | 被调用的函数应该放在执行调用的函数下面 |
| 高层在前 | 最重要的概念先出来，包括最少细节 |
| 细节在后 | 底层细节最后出来 |

**好处**：能扫过源代码文件，自最前面的几个函数获知要旨，而不至于沉溺到细节中。

---

## 5.3 横向格式

### 代码行宽度建议

| 指标 | 建议值 |
|------|-------|
| 常见宽度 | 45 字符左右 |
| 个人上限 | 120 字符 |
| 硬性上限 | 不超过 120-150 字符 |

**原则**：无需拖动滚动条到右边。

---

### 5.3.1 水平方向上的区隔与靠近

**使用空格字符表示关联性：**

```java
// ✅ 赋值操作符周围加空格（强调左右两边）
private void measureLine(String line) {
    lineCount++;
    int lineSize = line.length();
    totalChars += lineSize;
    lineWidthHistogram.addLine(lineSize, lineCount);
    recordWidestLine(lineSize);
}

// ✅ 函数名和括号之间不加空格（表示紧密关系）
// ✅ 参数之间用空格隔开（强调分离）
lineWidthHistogram.addLine(lineSize, lineCount);
```

**运算符优先级用空格表示：**

```java
// ✅ 乘法因子之间不加空格（高优先级）
// ✅ 加减法运算项之间用空格隔开（低优先级）
public class Quadratic {
    public static double root1(double a, double b, double c) {
        double determinant = determinant(a, b, c);
        return (-b + Math.sqrt(determinant)) / (2*a);
    }

    private static double determinant(double a, double b, double c) {
        return b*b - 4*a*c;
    }
}
```

---

### 5.3.2 水平对齐

**不推荐水平对齐：**

```java
// ❌ 不推荐：水平对齐
public class FitNesseExpediter implements ResponseSender {
    private   Socket          socket;
    private   InputStream     input;
    private   OutputStream    output;
    private   Request         request;
    private   Response        response;
    protected long            requestParsingTimeLimit;
}

// ✅ 推荐：不对齐，如果列表太长说明类应该被拆分
public class FitNesseExpediter implements ResponseSender {
    private Socket socket;
    private InputStream input;
    private OutputStream output;
    private Request request;
    private Response response;
    protected long requestParsingTimeLimit;
}
```

**原因**：
- 对齐会强调不重要的东西
- 目光会从类型移开，只看变量名
- 自动格式化工具通常会消除这类对齐
- 如果需要对齐的列表太长，问题在于列表本身

---

### 5.3.3 缩进

**缩进让层级结构可见：**

| 层级 | 缩进 |
|------|------|
| 文件顶层（类声明） | 不缩进 |
| 类中的方法 | 缩进一个层级 |
| 方法的实现 | 缩进一个层级 |
| 代码块 | 缩进一个层级 |

```java
// ❌ 无缩进：几乎无法阅读
public class FitNesseServer implements SocketServer { private FitNesseContext context; public FitNesseServer(FitNesseContext context) { this.context = context; } public void serve(Socket s) { serve(s, 10000); } }

// ✅ 有缩进：清晰可读
public class FitNesseServer implements SocketServer {
    private FitNesseContext context;

    public FitNesseServer(FitNesseContext context) {
        this.context = context;
    }

    public void serve(Socket s) {
        serve(s, 10000);
    }

    public void serve(Socket s, long requestTimeout) {
        try {
            FitNesseExpediter sender = new FitNesseExpediter(s, context);
            sender.setRequestParsingTimeLimit(requestTimeout);
            sender.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

#### 不要违反缩进规则

```java
// ❌ 错误：把范围坍塌到一行
public class CommentWidget extends TextWidget {
    public static final String REGEXP = "^#[^\r\n]*(?:(?:\r\n)|\n|\r)?";
    public CommentWidget(ParentWidget parent, String text){super(parent, text);}
    public String render() throws Exception {return ""; }
}

// ✅ 正确：展开并缩进
public class CommentWidget extends TextWidget {
    public static final String REGEXP = "^#[^\r\n]*(?:(?:\r\n)|\n|\r)?";

    public CommentWidget(ParentWidget parent, String text) {
        super(parent, text);
    }

    public String render() throws Exception {
        return "";
    }
}
```

---

### 5.3.4 空范围

```java
// ❌ 危险：分号容易被忽略
while (dis.read(buf, 0, readBufferSize) != -1);

// ✅ 正确：空范围体单独成行并缩进
while (dis.read(buf, 0, readBufferSize) != -1)
    ;
```

---

## 5.4 团队规则

> 每个程序员都有自己喜欢的格式规则，但如果在一个团队中工作，就是团队说了算。

### 团队规则要点

1. **达成共识**：一组开发者应当认同一种格式风格
2. **统一执行**：每个成员都应该采用那种风格
3. **一致性**：软件应该拥有一以贯之的风格
4. **自动化**：把规则编写进 IDE 的代码格式功能

### FitNesse 团队规则制定

| 决策项 | 说明 |
|-------|------|
| 括号位置 | 放在哪里 |
| 缩进大小 | 几个字符 |
| 命名规范 | 类、变量、方法如何命名 |

**花费时间**：约 10 分钟

**关键点**：这些规则并非全是个人喜爱的，但它们是团队决定的规则。作为团队一员，要遵循这些规则。

---

## 格式检查清单

### 垂直格式 ✅

| # | 检查项 |
|---|-------|
| 1 | 文件长度控制在 200-500 行 |
| 2 | 像报纸一样组织：高层在前，细节在后 |
| 3 | 用空白行隔开不同概念 |
| 4 | 紧密相关的代码靠在一起 |
| 5 | 变量声明靠近使用位置 |
| 6 | 实体变量在类顶部声明 |
| 7 | 调用者在被调用者上面 |
| 8 | 概念相关的函数放在一起 |

### 横向格式 ✅

| # | 检查项 |
|---|-------|
| 1 | 代码行宽度不超过 120 字符 |
| 2 | 赋值操作符周围加空格 |
| 3 | 函数名和括号之间不加空格 |
| 4 | 参数之间用空格隔开 |
| 5 | 不使用水平对齐 |
| 6 | 正确使用缩进表示层级 |
| 7 | 不把代码块坍塌到一行 |
| 8 | 空范围体单独成行 |

### 团队规则 ✅

| # | 检查项 |
|---|-------|
| 1 | 团队统一格式风格 |
| 2 | 规则编入 IDE 格式化工具 |
| 3 | 所有成员遵循团队规则 |

---

## 总结

| 原则 | 说明 |
|------|------|
| **格式关乎沟通** | 专业开发者的头等大事 |
| **向报纸学习** | 高层概念在前，细节在后 |
| **空白行隔开概念** | 紧密相关的靠近 |
| **自上向下** | 调用者在上，被调用者在下 |
| **短代码行** | 不超过 120 字符 |
| **正确缩进** | 让层级结构可见 |
| **团队规则优先** | 个人风格服从团队 |
