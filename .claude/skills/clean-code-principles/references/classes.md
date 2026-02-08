# 类 (Classes)

> 基于《Clean Code》第10章 - Robert C. Martin (with Jeff Langr)

## 核心思想

我们已经关注了如何编写好的代码行和代码块，探讨了函数的组织方式。但如果不关注更高层次的代码组织，我们仍然没有整洁的代码。让我们谈谈整洁的类。

---

## 类的组织 (Class Organization)

### Java 类组织约定

```java
public class MyClass {
    // 1. 公共静态常量
    public static final String CONSTANT = "value";
    
    // 2. 私有静态变量
    private static Logger logger = LoggerFactory.getLogger(MyClass.class);
    
    // 3. 私有实例变量
    private String name;
    private int count;
    
    // 4. 公共函数
    public void doSomething() {
        helperMethod();
    }
    
    // 5. 私有工具函数（紧跟调用它的公共函数之后）
    private void helperMethod() {
        // ...
    }
}
```

### 封装原则
- 优先保持变量和工具函数私有
- 有时需要将变量或函数设为 protected 以便测试访问
- 测试优先：如果测试需要调用函数或访问变量，可以设为 protected 或包作用域
- **放松封装是最后的手段**

---

## 规则 1：类应该短小 (Classes Should Be Small!)

### 核心规则

```
┌─────────────────────────────────────────────────────────────┐
│  类的第一条规则：应该短小                                     │
│  类的第二条规则：还要更短小                                   │
└─────────────────────────────────────────────────────────────┘
```

### 度量方式
- 函数：用代码行数度量
- 类：用**职责数量**度量

### ❌ 错误示例：God Class

```java
public class SuperDashboard extends JFrame implements MetaDataUser {
    public String getCustomizerLanguagePath()
    public void setSystemConfigPath(String systemConfigPath)
    public String getSystemConfigDocument()
    public void setSystemConfigDocument(String systemConfigDocument)
    public boolean getGuruState()
    public boolean getNoviceState()
    public boolean getOpenSourceState()
    public void showObject(MetaObject object)
    public void showProgress(String s)
    public boolean isMetadataDirty()
    public void setIsMetadataDirty(boolean isMetadataDirty)
    public Component getLastFocusedComponent()
    public void setLastFocused(Component lastFocused)
    // ... 还有60多个方法
}
```

### 方法少就够了吗？

```java
// 只有5个方法，但仍然太大！
public class SuperDashboard extends JFrame implements MetaDataUser {
    public Component getLastFocusedComponent()
    public void setLastFocused(Component lastFocused)
    public int getMajorVersionNumber()
    public int getMinorVersionNumber()
    public int getBuildNumber()
}
```

**问题**：尽管方法数量少，SuperDashboard 仍有太多职责：
1. 追踪焦点组件
2. 管理版本信息

### 命名启示

| 命名特征 | 说明 |
|---------|------|
| 无法取简洁名称 | 类可能太大 |
| 名称越模糊 | 职责越可能太多 |
| 包含 Processor、Manager、Super | 暗示职责聚合过多 |

### 25词规则

> 应该能用约25个词描述类的职责，且不使用"如果"、"和"、"或"、"但是"。

**反例**：
> "SuperDashboard 提供对最后拥有焦点的组件的访问，**并且**允许我们追踪版本和构建号。"

第一个"并且"就暗示了 SuperDashboard 有太多职责。

---

## 规则 2：单一职责原则 (Single Responsibility Principle - SRP)

### 定义

```
┌─────────────────────────────────────────────────────────────┐
│  类或模块应该有且只有一个修改的理由                           │
└─────────────────────────────────────────────────────────────┘
```

### 示例：提取 Version 类

```java
// ❌ 两个修改理由
public class SuperDashboard extends JFrame implements MetaDataUser {
    public Component getLastFocusedComponent()
    public void setLastFocused(Component lastFocused)
    public int getMajorVersionNumber()   // 理由1: 版本信息变化
    public int getMinorVersionNumber()
    public int getBuildNumber()
}

// ✅ 单一职责
public class Version {
    public int getMajorVersionNumber()
    public int getMinorVersionNumber()
    public int getBuildNumber()
}
```

`Version` 类具有很高的复用潜力！

### 为什么 SRP 常被违反？

```
让软件工作 ≠ 让软件整洁
         ↓
很多人认为程序能工作就完成了
         ↓
没有切换到组织和整洁的关注点
         ↓
继续处理下一个问题，而非拆分臃肿的类
```

### 小类太多会难以理解吗？

> 你希望工具放在有许多小抽屉、每个抽屉里都是定义清晰、标记明确的组件的工具箱里？还是希望只有几个大抽屉，什么都往里扔？

**事实**：
- 多个小类的系统与少数大类的系统有**同样多的移动部件**
- 管理复杂性的主要目标是**组织它**，让开发者知道去哪里找东西
- 每个小类封装单一职责、单一修改理由，与少数其他类协作

---

## 规则 3：内聚性 (Cohesion)

### 定义
- 类应该有少量实例变量
- 每个方法应该操作一个或多个这些变量
- 方法操作的变量越多，该方法对类越内聚
- 每个方法都使用每个变量的类是**最大内聚**的

### ✅ 高内聚示例：Stack

```java
public class Stack {
    private int topOfStack = 0;
    List<Integer> elements = new LinkedList<Integer>();

    public int size() {
        return topOfStack;
    }

    public void push(int element) {
        topOfStack++;
        elements.add(element);
    }

    public int pop() throws PoppedWhenEmpty {
        if (topOfStack == 0)
            throw new PoppedWhenEmpty();
        int element = elements.get(--topOfStack);
        elements.remove(topOfStack);
        return element;
    }
}
```

只有 `size()` 没有使用两个变量，这是一个非常内聚的类。

### 内聚性降低的信号

```
保持函数短小 + 保持参数列表短小
          ↓
可能导致实例变量增多（只被部分方法使用）
          ↓
这几乎总是意味着至少有另一个类想要从大类中分离出来
          ↓
应该将变量和方法分离到两个或更多类中，使新类更内聚
```

### 维持内聚性会产生许多小类

```
拆分大函数为小函数
      ↓
提取的代码使用了多个变量
      ↓
可以将这些变量提升为实例变量（不用传参）
      ↓
但这会降低内聚性
      ↓
如果有些函数想共享某些变量，它们就应该是一个独立的类
      ↓
当类失去内聚性时，就拆分它！
```

---

## 规则 4：为修改而组织 (Organizing for Change)

### 问题：必须打开才能修改的类

```java
// ❌ 添加 update 需要修改这个类
public class Sql {
    public Sql(String table, Column[] columns)
    public String create()
    public String insert(Object[] fields)
    public String selectAll()
    public String findByKey(String keyColumn, String keyValue)
    public String select(Column column, String pattern)
    public String select(Criteria criteria)
    public String preparedInsert()
    private String columnList(Column[] columns)
    private String valuesList(Object[] fields, final Column[] columns)
    private String selectWithCriteria(String criteria)
    private String placeholderList(Column[] columns)
}
```

**两个修改理由**：
1. 添加新的语句类型（如 update）
2. 修改单个语句类型的细节（如 select 支持子查询）

### ✅ 正确示例：封闭的类集合

```java
abstract public class Sql {
    public Sql(String table, Column[] columns)
    abstract public String generate();
}

public class CreateSql extends Sql {
    public CreateSql(String table, Column[] columns)
    @Override public String generate()
}

public class SelectSql extends Sql {
    public SelectSql(String table, Column[] columns)
    @Override public String generate()
}

public class InsertSql extends Sql {
    public InsertSql(String table, Column[] columns, Object[] fields)
    @Override public String generate()
    private String valuesList(Object[] fields, final Column[] columns)
}

public class SelectWithCriteriaSql extends Sql {
    public SelectWithCriteriaSql(String table, Column[] columns, Criteria criteria)
    @Override public String generate()
}

public class FindByKeySql extends Sql {
    public FindByKeySql(String table, Column[] columns, String keyColumn, String keyValue)
    @Override public String generate()
}

public class PreparedInsertSql extends Sql {
    public PreparedInsertSql(String table, Column[] columns)
    @Override public String generate()
    private String placeholderList(Column[] columns)
}

public class Where {
    public Where(String criteria)
    public String generate()
}

public class ColumnList {
    public ColumnList(Column[] columns)
    public String generate()
}
```

### 好处

| 方面 | 优势 |
|------|------|
| **理解时间** | 理解任何类只需极短时间 |
| **风险** | 一个函数破坏另一个的风险几乎为零 |
| **测试** | 更容易验证所有逻辑（类彼此隔离） |
| **扩展** | 添加 update 无需修改任何现有类 |

### 开放-封闭原则 (OCP)

```
┌─────────────────────────────────────────────────────────────┐
│  类应该对扩展开放，对修改关闭                                 │
└─────────────────────────────────────────────────────────────┘
```

重构后的 Sql 类：
- **对扩展开放**：可以通过子类添加新功能
- **对修改关闭**：添加 UpdateSql 时不需要修改任何其他类

---

## 规则 5：隔离修改 (Isolating from Change)

### 问题
依赖于具体细节的客户类，当这些细节改变时会面临风险。

### 示例：Portfolio 依赖 TokyoStockExchange

```java
// ❌ 直接依赖具体实现
public class Portfolio {
    private TokyoStockExchange exchange;
    
    public Portfolio() {
        this.exchange = new TokyoStockExchange();
    }
    
    public Money value() {
        // 直接调用 exchange，难以测试
    }
}
```

**问题**：股票价格每5分钟变化一次，测试时会得到不同答案！

### ✅ 正确示例：依赖抽象

```java
// 定义接口
public interface StockExchange {
    Money currentPrice(String symbol);
}

// Portfolio 依赖接口
public class Portfolio {
    private StockExchange exchange;
    
    public Portfolio(StockExchange exchange) {
        this.exchange = exchange;
    }
    
    public Money value() {
        // 使用接口，可以注入测试替身
    }
}

// 真实实现
public class TokyoStockExchange implements StockExchange {
    @Override
    public Money currentPrice(String symbol) {
        // 真实的股票查询
    }
}
```

### 测试代码

```java
public class PortfolioTest {
    private FixedStockExchangeStub exchange;
    private Portfolio portfolio;

    @Before
    protected void setUp() throws Exception {
        exchange = new FixedStockExchangeStub();
        exchange.fix("MSFT", 100);  // 固定微软股价为100
        portfolio = new Portfolio(exchange);
    }
    
    @Test
    public void GivenFiveMSFTTotalShouldBe500() throws Exception {
        portfolio.add(5, "MSFT");
        Assert.assertEquals(500, portfolio.value());
    }
}
```

### 依赖倒置原则 (DIP)

```
┌─────────────────────────────────────────────────────────────┐
│  类应该依赖于抽象，而非具体细节                               │
└─────────────────────────────────────────────────────────────┘
```

**效果**：
- 系统解耦足以被这样测试
- 也更加灵活，促进更多复用
- 缺少耦合意味着系统元素彼此隔离，也与变化隔离
- 隔离使得更容易理解系统的每个元素

---

## 重构示例：PrintPrimes

### ❌ 重构前：单一大函数

```java
public class PrintPrimes {
    public static void main(String[] args) {
        final int M = 1000;
        final int RR = 50;
        final int CC = 4;
        final int WW = 10;
        final int ORDMAX = 30;
        int P[] = new int[M + 1];
        int PAGENUMBER;
        int PAGEOFFSET;
        int ROWOFFSET;
        int C;
        int J;
        int K;
        boolean JPRIME;
        int ORD;
        int SQUARE;
        int N;
        int MULT[] = new int[ORDMAX + 1];
        // ... 几十行深度嵌套的逻辑
    }
}
```

### ✅ 重构后：三个单一职责的类

```java
// 1. 主程序类 - 处理执行环境
public class PrimePrinter {
    public static void main(String[] args) {
        final int NUMBER_OF_PRIMES = 1000;
        int[] primes = PrimeGenerator.generate(NUMBER_OF_PRIMES);

        final int ROWS_PER_PAGE = 50;
        final int COLUMNS_PER_PAGE = 4;
        RowColumnPagePrinter tablePrinter =
            new RowColumnPagePrinter(ROWS_PER_PAGE,
                                     COLUMNS_PER_PAGE,
                                     "The First " + NUMBER_OF_PRIMES +
                                     " Prime Numbers");
        tablePrinter.print(primes);
    }
}

// 2. 打印类 - 知道如何格式化输出
public class RowColumnPagePrinter {
    private int rowsPerPage;
    private int columnsPerPage;
    private int numbersPerPage;
    private String pageHeader;
    private PrintStream printStream;

    public RowColumnPagePrinter(int rowsPerPage, int columnsPerPage, String pageHeader) {
        // ...
    }

    public void print(int data[]) {
        // 分页打印逻辑
    }

    private void printPage(...) { }
    private void printRow(...) { }
    private void printPageHeader(...) { }
}

// 3. 生成器类 - 知道如何生成质数
public class PrimeGenerator {
    private static int[] primes;
    private static ArrayList<Integer> multiplesOfPrimeFactors;

    protected static int[] generate(int n) {
        primes = new int[n];
        multiplesOfPrimeFactors = new ArrayList<Integer>();
        set2AsFirstPrime();
        checkOddNumbersForSubsequentPrimes();
        return primes;
    }

    private static void set2AsFirstPrime() { }
    private static void checkOddNumbersForSubsequentPrimes() { }
    private static boolean isPrime(int candidate) { }
    // ...
}
```

### 重构说明

| 类 | 职责 | 修改理由 |
|---|------|---------|
| PrimePrinter | 处理执行环境 | 调用方式改变时 |
| RowColumnPagePrinter | 格式化数字列表 | 输出格式改变时 |
| PrimeGenerator | 生成质数 | 算法改变时 |

**这不是重写！**
- 没有从头开始
- 使用相同的算法和机制
- 先写测试验证行为
- 一次做一个微小的改变
- 每次改变后运行程序确保行为不变

---

## 代码审查清单

### 类组织
- [ ] 变量和方法是否按正确顺序排列？
- [ ] 是否优先保持变量和方法私有？
- [ ] 放松封装是否是最后的手段？

### 类大小
- [ ] 类是否足够短小？
- [ ] 是否能用25个词描述类的职责（不含 if/and/or/but）？
- [ ] 类名是否简洁且描述其职责？

### 单一职责原则 (SRP)
- [ ] 类是否只有一个修改的理由？
- [ ] 是否避免了 Processor、Manager、Super 等命名？
- [ ] 是否将不同职责拆分到不同类？

### 内聚性
- [ ] 类是否有少量实例变量？
- [ ] 每个方法是否操作多个实例变量？
- [ ] 当内聚性降低时是否拆分类？

### 为修改而组织
- [ ] 是否遵循开放-封闭原则 (OCP)？
- [ ] 添加新功能是否不需要修改现有类？
- [ ] 是否使用继承/多态替代 switch/if？

### 隔离修改
- [ ] 是否遵循依赖倒置原则 (DIP)？
- [ ] 类是否依赖于抽象而非具体细节？
- [ ] 是否可以轻松注入测试替身？

---

## 核心箴言

> **类的第一条规则：应该短小。类的第二条规则：还要更短小。**

> **单一职责原则 (SRP)：类或模块应该有且只有一个修改的理由。**

> **开放-封闭原则 (OCP)：类应该对扩展开放，对修改关闭。**

> **依赖倒置原则 (DIP)：类应该依赖于抽象，而非具体细节。**

> **我们希望系统由许多小类组成，而非少数大类。每个小类封装单一职责，有单一修改理由，与少数其他类协作以实现期望的系统行为。**
