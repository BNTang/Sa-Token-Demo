# Java 17 有哪些新特性？

## 核心新特性概览

```
┌─────────────────────────────────────────────────────────────┐
│                    Java 17 新特性 (2021 LTS)                 │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. Sealed Classes (密封类) - 正式版                       │
│   2. Pattern Matching for instanceof - 正式版               │
│   3. Record 类型 - 正式版                                    │
│   4. Switch 表达式 - 正式版                                  │
│   5. 文本块 Text Blocks - 正式版                            │
│   6. 新的随机数生成器 API                                    │
│   7. 移除实验性 AOT 和 JIT 编译器                           │
│   8. 弃用 Security Manager                                   │
│                                                             │
│   Java 17 是 LTS (长期支持版本)                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 1. Sealed Classes (密封类)

```java
// 限制哪些类可以继承
public sealed class Shape 
    permits Circle, Rectangle, Triangle {
}

// 必须是 final、sealed 或 non-sealed
public final class Circle extends Shape {
    private double radius;
}

public sealed class Rectangle extends Shape 
    permits Square {
}

// non-sealed 放开限制
public non-sealed class Triangle extends Shape {
}

// Square 可以有任意子类
public final class Square extends Rectangle {
}
```

```
┌─────────────────────────────────────────────────────────────┐
│                    密封类规则                                │
├─────────────────────────────────────────────────────────────┤
│   • sealed: 必须用 permits 指定允许的子类                   │
│   • 子类必须声明为 final、sealed 或 non-sealed              │
│   • 子类必须与父类在同一模块（或同一包）                    │
│   • 好处: 精确控制继承，配合模式匹配更安全                  │
└─────────────────────────────────────────────────────────────┘
```

## 2. Pattern Matching for instanceof

```java
// 之前
if (obj instanceof String) {
    String s = (String) obj;  // 需要强制转换
    System.out.println(s.length());
}

// Java 17 (Java 16 正式)
if (obj instanceof String s) {
    // s 直接可用，自动转换
    System.out.println(s.length());
}

// 还可以在条件中使用
if (obj instanceof String s && s.length() > 5) {
    System.out.println(s);
}
```

## 3. Record 类型

```java
// 之前: 需要写很多样板代码
public class Point {
    private final int x;
    private final int y;
    
    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    // getter, equals, hashCode, toString...
}

// Java 17: 一行搞定
public record Point(int x, int y) { }

// 使用
Point p = new Point(1, 2);
System.out.println(p.x());    // 1
System.out.println(p.y());    // 2
System.out.println(p);        // Point[x=1, y=2]

// 自定义构造器
public record User(String name, int age) {
    public User {
        // 紧凑构造器，可加验证
        if (age < 0) throw new IllegalArgumentException();
    }
}
```

```
┌─────────────────────────────────────────────────────────────┐
│                    Record 特点                               │
├─────────────────────────────────────────────────────────────┤
│   • 自动生成: 构造器、getter、equals、hashCode、toString    │
│   • 不可变 (final 字段)                                     │
│   • 不能被继承 (隐式 final)                                 │
│   • 可以实现接口                                            │
│   • 适合: DTO、值对象                                       │
└─────────────────────────────────────────────────────────────┘
```

## 4. Switch 表达式

```java
// 传统 switch
int numLetters;
switch (day) {
    case MONDAY:
    case FRIDAY:
    case SUNDAY:
        numLetters = 6;
        break;
    case TUESDAY:
        numLetters = 7;
        break;
    default:
        numLetters = 0;
}

// Java 17: 箭头语法 + 表达式
int numLetters = switch (day) {
    case MONDAY, FRIDAY, SUNDAY -> 6;
    case TUESDAY -> 7;
    case THURSDAY, SATURDAY -> 8;
    case WEDNESDAY -> 9;
};

// yield 返回值
String result = switch (day) {
    case MONDAY -> "Start";
    case FRIDAY -> {
        System.out.println("Weekend coming!");
        yield "Almost there";  // 多行用 yield
    }
    default -> "Regular day";
};
```

## 5. 文本块 (Text Blocks)

```java
// 之前: 字符串拼接
String json = "{\n" +
    "  \"name\": \"Tom\",\n" +
    "  \"age\": 18\n" +
    "}";

// Java 17: 文本块
String json = """
    {
      "name": "Tom",
      "age": 18
    }
    """;

// SQL
String sql = """
    SELECT id, name
    FROM users
    WHERE age > 18
    ORDER BY name
    """;

// HTML
String html = """
    <html>
        <body>
            <h1>Hello</h1>
        </body>
    </html>
    """;
```

## 6. 新的随机数 API

```java
// 新的 RandomGenerator 接口
RandomGenerator generator = RandomGenerator.getDefault();

// 多种实现
RandomGenerator random = RandomGenerator.of("L64X128MixRandom");

// 生成随机数
int n = generator.nextInt(100);
double d = generator.nextDouble();

// 生成流
generator.ints(10, 0, 100)
    .forEach(System.out::println);
```

## 面试回答

### 30秒版本

> Java 17 是 **LTS 版本**，主要正式化特性：1）**Sealed Classes**：限制类的继承层次；2）**instanceof 模式匹配**：类型检查 + 转换一步完成；3）**Record**：不可变数据类，自动生成构造器/getter/equals/hashCode；4）**Switch 表达式**：箭头语法、返回值；5）**文本块**：多行字符串用 `"""`。

### 1分钟版本

> **Java 17 是 LTS**
>
> **Sealed Classes**：
> - `sealed class X permits A, B`
> - 限制子类，配合模式匹配
>
> **instanceof 模式匹配**：
> - `if (obj instanceof String s)`
> - 类型检查 + 变量绑定
>
> **Record**：
> - `record Point(int x, int y) {}`
> - 自动生成 getter/equals/hashCode
> - 不可变，适合 DTO
>
> **Switch 表达式**：
> - 箭头语法 `case X ->`
> - 可以返回值
> - yield 用于多行
>
> **文本块**：
> - `"""多行字符串"""`
> - 保留格式，适合 JSON/SQL/HTML
>
> **推荐升级**：生产环境从 Java 8/11 升到 17

---

*关联文档：[java11-features.md](java11-features.md) | [java21-features.md](java21-features.md)*
