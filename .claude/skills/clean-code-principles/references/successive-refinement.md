# 逐步改进 (Successive Refinement)

> 基于《Clean Code》第14章 - Robert C. Martin

## 核心思想

> 要编写整洁代码，你必须先写脏代码，然后再清理它。

本章是一个逐步改进的案例研究。展示了一个开始良好但无法扩展的模块如何被重构和清理。

---

## 核心原则

### 写作的真理同样适用于编程

```
┌─────────────────────────────────────────────────────────────┐
│  我们在小学就学过这个真理：                                   │
│                                                             │
│  应该先写草稿，然后写第二稿，然后写后续几稿                    │
│  直到我们得到最终版本                                        │
│                                                             │
│  编写整洁代码是一个逐步改进的过程                             │
└─────────────────────────────────────────────────────────────┘
```

### 新手程序员的问题

```
大多数新手程序员认为：主要目标是让程序工作
                    ↓
一旦"工作"了，就转向下一个任务
                    ↓
把"工作"的程序留在最终让它"工作"的状态
                    ↓
大多数资深程序员知道：这是职业自杀
```

---

## 案例：Args 命令行参数解析器

### 最终整洁版本

```java
// 简单易用的 API
public static void main(String[] args) {
    try {
        Args arg = new Args("l,p#,d*", args);
        boolean logging = arg.getBoolean('l');
        int port = arg.getInt('p');
        String directory = arg.getString('d');
        executeApplication(logging, port, directory);
    } catch (ArgsException e) {
        System.out.printf("Argument error: %s\n", e.errorMessage());
    }
}

// 格式字符串说明：
// l   - 布尔参数
// p#  - 整数参数
// d*  - 字符串参数
```

### 添加新参数类型的步骤

添加新类型（如 double）只需要：
1. 新的 `ArgumentMarshaler` 派生类
2. 新的 `getXXX` 函数
3. `parseSchemaElement` 中新的 case 语句
4. 可能需要新的 `ErrorCode` 和错误消息

---

## 演进过程

### 阶段 1：仅布尔参数（整洁）

```java
public class Args {
    private String schema;
    private String[] args;
    private boolean valid;
    private Set<Character> unexpectedArguments = new TreeSet<>();
    private Map<Character, Boolean> booleanArgs = new HashMap<>();
    private int numberOfArguments = 0;
    
    // 简单、紧凑、易于理解
}
```

### 阶段 2：添加 String 和 Integer（开始混乱）

```java
public class Args {
    private String schema;
    private String[] args;
    private boolean valid = true;
    private Set<Character> unexpectedArguments = new TreeSet<>();
    private Map<Character, Boolean> booleanArgs = new HashMap<>();
    private Map<Character, String> stringArgs = new HashMap<>();  // 新增
    private Map<Character, Integer> intArgs = new HashMap<>();    // 新增
    private Set<Character> argsFound = new HashSet<>();
    private int currentArgument;
    private char errorArgumentId = '\0';
    private String errorParameter = "TILT";
    private ErrorCode errorCode = ErrorCode.OK;
    
    // 实例变量数量令人生畏
    // 奇怪的字符串如 "TILT"
    // try-catch-catch 块堆积
    // 开始腐烂发酵...
}
```

### 问题识别

```
┌─────────────────────────────────────────────────────────────┐
│  仅增加两个参数类型就产生了巨大的负面影响                      │
│                                                             │
│  每种参数类型需要在三个主要位置添加新代码：                     │
│  1. 解析 schema 元素的方式（选择该类型的 HashMap）             │
│  2. 解析命令行字符串并转换为真实类型                          │
│  3. getXXX 方法返回真实类型给调用者                          │
│                                                             │
│  多种类型，都有相似方法 → 这听起来像一个类！                   │
│  于是 ArgumentMarshaler 概念诞生了                           │
└─────────────────────────────────────────────────────────────┘
```

---

## 重构方法论

### 关于增量主义

```
┌─────────────────────────────────────────────────────────────┐
│  毁掉程序的最好方式之一是                                     │
│  以改进的名义对其结构进行大规模修改                           │
│                                                             │
│  有些程序永远无法从这种"改进"中恢复                           │
│  问题是很难让程序像"改进"之前一样工作                         │
└─────────────────────────────────────────────────────────────┘
```

### TDD 的纪律

```
使用 TDD，我不允许做出破坏系统的修改
每个修改都必须保持系统像以前一样工作

                    ↓
                    
需要一套可以随时运行的自动化测试
验证系统的行为没有改变

                    ↓

进行大量非常小的修改
每个修改都保持系统工作
```

### 重构步骤示例

```java
// 步骤 1：添加 ArgumentMarshaler 骨架
private class ArgumentMarshaler {
    private boolean booleanValue = false;
    public void setBoolean(boolean value) { booleanValue = value; }
    public boolean getBoolean() { return booleanValue; }
}

private class BooleanArgumentMarshaler extends ArgumentMarshaler {}
private class StringArgumentMarshaler extends ArgumentMarshaler {}
private class IntegerArgumentMarshaler extends ArgumentMarshaler {}

// 步骤 2：修改 HashMap 使用 ArgumentMarshaler
private Map<Character, ArgumentMarshaler> booleanArgs =
    new HashMap<Character, ArgumentMarshaler>();

// 步骤 3：修复被破坏的语句
private void parseBooleanSchemaElement(char elementId) {
    booleanArgs.put(elementId, new BooleanArgumentMarshaler());
}

// 步骤 4：继续重构...每一步都运行测试
```

### 类似魔方

```
┌─────────────────────────────────────────────────────────────┐
│  重构很像解魔方                                              │
│                                                             │
│  需要很多小步骤才能实现大目标                                 │
│  每一步都使下一步成为可能                                    │
│                                                             │
│  放入一些东西然后再取出是重构中很常见的                       │
│  步骤的小和保持测试运行的需要意味着你会经常移动东西            │
└─────────────────────────────────────────────────────────────┘
```

---

## 最终架构

### Args.java（整洁版）

```java
public class Args {
    private String schema;
    private Map<Character, ArgumentMarshaler> marshalers = new HashMap<>();
    private Set<Character> argsFound = new HashSet<>();
    private Iterator<String> currentArgument;
    private List<String> argsList;

    public Args(String schema, String[] args) throws ArgsException {
        this.schema = schema;
        argsList = Arrays.asList(args);
        parse();
    }

    private void parse() throws ArgsException {
        parseSchema();
        parseArguments();
    }

    private void parseSchemaElement(String element) throws ArgsException {
        char elementId = element.charAt(0);
        String elementTail = element.substring(1);
        validateSchemaElementId(elementId);
        if (elementTail.length() == 0)
            marshalers.put(elementId, new BooleanArgumentMarshaler());
        else if (elementTail.equals("*"))
            marshalers.put(elementId, new StringArgumentMarshaler());
        else if (elementTail.equals("#"))
            marshalers.put(elementId, new IntegerArgumentMarshaler());
        else if (elementTail.equals("##"))
            marshalers.put(elementId, new DoubleArgumentMarshaler());
        else
            throw new ArgsException(ArgsException.ErrorCode.INVALID_FORMAT,
                                    elementId, elementTail);
    }

    private boolean setArgument(char argChar) throws ArgsException {
        ArgumentMarshaler m = marshalers.get(argChar);
        if (m == null)
            return false;
        try {
            m.set(currentArgument);  // 多态调用
            return true;
        } catch (ArgsException e) {
            e.setErrorArgumentId(argChar);
            throw e;
        }
    }
    // ...
}
```

### ArgumentMarshaler 接口

```java
public interface ArgumentMarshaler {
    void set(Iterator<String> currentArgument) throws ArgsException;
    Object get();
}
```

### 具体实现

```java
public class BooleanArgumentMarshaler implements ArgumentMarshaler {
    private boolean booleanValue = false;

    public void set(Iterator<String> currentArgument) throws ArgsException {
        booleanValue = true;
    }

    public Object get() {
        return booleanValue;
    }
}

public class StringArgumentMarshaler implements ArgumentMarshaler {
    private String stringValue = "";

    public void set(Iterator<String> currentArgument) throws ArgsException {
        try {
            stringValue = currentArgument.next();
        } catch (NoSuchElementException e) {
            throw new ArgsException(MISSING_STRING);
        }
    }

    public Object get() {
        return stringValue;
    }
}

public class IntegerArgumentMarshaler implements ArgumentMarshaler {
    private int intValue = 0;

    public void set(Iterator<String> currentArgument) throws ArgsException {
        String parameter = null;
        try {
            parameter = currentArgument.next();
            intValue = Integer.parseInt(parameter);
        } catch (NoSuchElementException e) {
            throw new ArgsException(MISSING_INTEGER);
        } catch (NumberFormatException e) {
            throw new ArgsException(INVALID_INTEGER, parameter);
        }
    }

    public Object get() {
        return intValue;
    }
}
```

### ArgsException 类

```java
public class ArgsException extends Exception {
    private char errorArgumentId = '\0';
    private String errorParameter = null;
    private ErrorCode errorCode = ErrorCode.OK;

    public enum ErrorCode {
        OK, INVALID_FORMAT, UNEXPECTED_ARGUMENT, INVALID_ARGUMENT_NAME,
        MISSING_STRING, MISSING_INTEGER, INVALID_INTEGER,
        MISSING_DOUBLE, INVALID_DOUBLE
    }

    public String errorMessage() throws Exception {
        switch (errorCode) {
            case UNEXPECTED_ARGUMENT:
                return String.format("Argument -%c unexpected.", errorArgumentId);
            case MISSING_STRING:
                return String.format("Could not find string parameter for -%c.",
                                     errorArgumentId);
            case INVALID_INTEGER:
                return String.format("Argument -%c expects an integer but was '%s'.",
                                     errorArgumentId, errorParameter);
            // ...
        }
        return "";
    }
}
```

---

## 关键设计决策

### 关注点分离

```
┌─────────────────────────────────────────────────────────────┐
│  好的软件设计很大程度上是关于分区                             │
│  创建适当的位置来放置不同类型的代码                           │
│                                                             │
│  这种关注点分离使代码更容易理解和维护                         │
└─────────────────────────────────────────────────────────────┘
```

| 关注点 | 放置位置 |
|-------|---------|
| 参数处理 | Args 类 |
| 错误消息格式化 | ArgsException 类 |
| 类型转换 | ArgumentMarshaler 派生类 |
| 测试 | ArgsTest、ArgsExceptionTest |

### 类型转换（从 type-case 到多态）

```java
// ❌ 之前：type-case（不好）
private boolean setArgument(char argChar) throws ArgsException {
    ArgumentMarshaler m = marshalers.get(argChar);
    if (m instanceof BooleanArgumentMarshaler)
        setBooleanArg(m);
    else if (m instanceof StringArgumentMarshaler)
        setStringArg(m);
    else if (m instanceof IntegerArgumentMarshaler)
        setIntArg(m);
    else
        return false;
    return true;
}

// ✅ 之后：多态调用（好）
private boolean setArgument(char argChar) throws ArgsException {
    ArgumentMarshaler m = marshalers.get(argChar);
    if (m == null)
        return false;
    try {
        m.set(currentArgument);  // 多态！
        return true;
    } catch (ArgsException e) {
        e.setErrorArgumentId(argChar);
        throw e;
    }
}
```

---

## 结论

### 代码工作是不够的

```
┌─────────────────────────────────────────────────────────────┐
│  代码工作是不够的                                            │
│  工作的代码往往是严重破损的                                   │
│                                                             │
│  程序员如果仅仅满足于代码工作，就是不专业的                   │
│                                                             │
│  他们可能担心没有时间改进代码的结构和设计                     │
│  但我不同意                                                 │
│                                                             │
│  没有什么比糟糕的代码对开发项目有更深刻、更长期的恶化影响      │
└─────────────────────────────────────────────────────────────┘
```

### 糟糕代码的代价

```
糟糕的时间表可以重做
糟糕的需求可以重新定义
糟糕的团队动态可以修复

但是...

糟糕的代码会腐烂发酵
成为拖垮团队的无法逃脱的重量

我一次又一次地看到团队停滞不前
因为在匆忙中，他们创造了恶性的代码泥潭
永远主宰着他们的命运
```

### 清理代码的时机

| 时机 | 难度 |
|-----|------|
| 刚刚（5分钟前）弄脏 | 非常容易清理 |
| 今天早上弄脏 | 下午容易清理 |
| 代码已经腐烂 | 昂贵且困难 |

### 解决方案

```
┌─────────────────────────────────────────────────────────────┐
│  解决方案是持续保持代码尽可能整洁和简单                       │
│                                                             │
│  永远不要让腐烂开始                                          │
└─────────────────────────────────────────────────────────────┘
```

---

## 代码审查清单

### 重构过程
- [ ] 是否使用 TDD 保持系统在每一步都工作？
- [ ] 修改是否足够小，可以在失败时快速回滚？
- [ ] 是否避免了大规模结构变更？

### 架构
- [ ] 是否使用多态替代了 type-case？
- [ ] 是否适当分离了关注点？
- [ ] 添加新类型是否只需要最小的修改？

### 代码健康
- [ ] 是否在混乱变大之前就停下来重构？
- [ ] 是否保持代码尽可能整洁和简单？
- [ ] 是否永远不让腐烂开始？

---

## 核心箴言

> **我没有从头到尾以当前形式编写这个程序。更重要的是，我并不期望你能在一次尝试中写出整洁优雅的程序。**

> **要编写整洁代码，你必须先写脏代码，然后再清理它。**

> **毁掉程序的最好方式之一是以改进的名义对其结构进行大规模修改。**

> **代码工作是不够的。仅仅满足于代码工作的程序员是不专业的。**

> **持续保持代码尽可能整洁和简单。永远不要让腐烂开始。**
