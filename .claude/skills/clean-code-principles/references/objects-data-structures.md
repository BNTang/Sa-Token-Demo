# 对象和数据结构 (Objects and Data Structures)

> 基于《Clean Code》第6章 - Robert C. Martin

## 核心思想

将变量设置为私有（private）有一个理由：我们不想其他人依赖这些变量。我们还想在心血来潮时能自由修改其类型或实现。那么，为什么还是有那么多程序员给对象自动添加赋值器和取值器，将私有变量公之于众、如同它们根本就是公共变量一般呢？

---

## 规则 1：数据抽象 (Data Abstraction)

### 原则
隐藏实现并非只是在变量之间放上一个函数层那么简单。**隐藏实现关乎抽象！**

类并不简单地用取值器和赋值器将其变量推向外间，而是曝露抽象接口，以便用户无需了解数据的实现就能操作数据本体。

### ❌ 错误示例：具象点（暴露实现）

```java
// 具象点 - 曝露了其实现
public class Point {
    public double x;
    public double y;
}
```

### ✅ 正确示例：抽象点（隐藏实现）

```java
// 抽象点 - 完全隐藏实现
public interface Point {
    double getX();
    double getY();
    void setCartesian(double x, double y);
    double getR();
    double getTheta();
    void setPolar(double r, double theta);
}
```

> 你不知道该实现会是在矩形坐标系中还是在极坐标系中。接口明白无误地呈现了一种数据结构，同时固定了一套存取策略。

### ❌ 错误示例：具象机动车

```java
// 曝露数据细节
public interface Vehicle {
    double getFuelTankCapacityInGallons();
    double getGallonsOfGasoline();
}
```

### ✅ 正确示例：抽象机动车

```java
// 以抽象形态表述数据
public interface Vehicle {
    double getPercentFuelRemaining();
}
```

### 核心要点
- 我们不愿曝露数据细节，更愿意以抽象形态表述数据
- 傻乐着乱加取值器和赋值器，是最坏的选择
- 要以最好的方式呈现某个对象包含的数据，需要做严肃的思考

---

## 规则 2：数据/对象的反对称性 (Data/Object Anti-Symmetry)

### 核心定义

| 类型 | 特征 | 优势 | 劣势 |
|------|------|------|------|
| **对象** | 隐藏数据，曝露操作函数 | 便于添加新类型 | 难以添加新函数 |
| **数据结构** | 曝露数据，没有有意义的函数 | 便于添加新函数 | 难以添加新类型 |

### 过程式代码示例

```java
public class Square {
    public Point topLeft;
    public double side;
}

public class Rectangle {
    public Point topLeft;
    public double height;
    public double width;
}

public class Circle {
    public Point center;
    public double radius;
}

public class Geometry {
    public final double PI = 3.141592653589793;

    public double area(Object shape) throws NoSuchShapeException {
        if (shape instanceof Square) {
            Square s = (Square) shape;
            return s.side * s.side;
        } else if (shape instanceof Rectangle) {
            Rectangle r = (Rectangle) shape;
            return r.height * r.width;
        } else if (shape instanceof Circle) {
            Circle c = (Circle) shape;
            return PI * c.radius * c.radius;
        }
        throw new NoSuchShapeException();
    }
}
```

**优点**：添加 `perimeter()` 函数不影响形状类  
**缺点**：添加新形状需修改所有函数

### 面向对象代码示例

```java
public class Square implements Shape {
    private Point topLeft;
    private double side;

    public double area() {
        return side * side;
    }
}

public class Rectangle implements Shape {
    private Point topLeft;
    private double height;
    private double width;

    public double area() {
        return height * width;
    }
}

public class Circle implements Shape {
    private Point center;
    private double radius;
    public final double PI = 3.141592653589793;

    public double area() {
        return PI * radius * radius;
    }
}
```

**优点**：添加新形状不影响既有函数  
**缺点**：添加新函数需修改所有类

### 选择原则

```
┌─────────────────────────────────────────────────────────────┐
│                    如何选择？                                │
├─────────────────────────────────────────────────────────────┤
│  需要添加新数据类型？ → 使用对象和面向对象                    │
│  需要添加新行为/函数？ → 使用数据结构和过程式代码             │
└─────────────────────────────────────────────────────────────┘
```

> 成熟的程序员知道"一切都是对象"只是一个神话。有时候确实需要简单的数据结构和操作它们的过程。

---

## 规则 3：得墨忒耳律 (The Law of Demeter)

### 定义
模块不应了解它所操作对象的内部情形。**只跟朋友谈话，不与陌生人谈话。**

### 规则说明
类 C 的方法 f 只应该调用以下对象的方法：

1. **C 自身**
2. **由 f 创建的对象**
3. **作为参数传递给 f 的对象**
4. **由 C 的实体变量持有的对象**

### ❌ 违反示例：火车失事 (Train Wrecks)

```java
// 违反得墨忒耳律 - 像一列火车
final String outputDir = ctxt.getOptions().getScratchDir().getAbsolutePath();
```

### ⚠️ 稍好的写法

```java
// 拆分调用链，但仍可能违反
Options opts = ctxt.getOptions();
File scratchDir = opts.getScratchDir();
final String outputDir = scratchDir.getAbsolutePath();
```

### 判断标准

```
┌────────────────────────────────────────────────────────────────┐
│  如果 ctxt、Options、ScratchDir 是对象：                       │
│    → 内部结构应当隐藏，上述代码违反得墨忒耳律                   │
│                                                                │
│  如果它们只是数据结构（没有行为）：                            │
│    → 自然会曝露内部结构，得墨忒耳律不适用                      │
└────────────────────────────────────────────────────────────────┘
```

### ✅ 正确做法：让对象做事

```java
// 思考：为什么需要临时目录的绝对路径？
// 答：为了创建临时文件

// 错误：询问内部情形
String outFile = outputDir + "/" + className.replace('.', '/') + ".class";
FileOutputStream fout = new FileOutputStream(outFile);
BufferedOutputStream bos = new BufferedOutputStream(fout);

// 正确：让对象做事
BufferedOutputStream bos = ctxt.createScratchFileStream(classFileName);
```

> 如果 ctxt 是个对象，就应该要求它做点什么，不该要求它给出内部情形。

---

## 规则 4：避免混杂结构 (Avoid Hybrids)

### 什么是混杂结构？
一半是对象，一半是数据结构的混合体。

### 特征
- 拥有执行操作的函数
- 同时有公共变量或公共访问器/改值器
- 诱导外部函数以过程式方式使用变量

### ❌ 错误示例

```java
// 混杂结构 - 两面不讨好
public class UserProfile {
    // 像数据结构一样暴露数据
    public String name;
    public int age;
    
    // 又像对象一样有行为
    public void updateProfile(String name, int age) {
        this.name = name;
        this.age = age;
        saveToDatabase();
    }
    
    public void sendNotification() {
        // 发送通知逻辑
    }
}
```

### 后果
- 增加了添加新函数的难度
- 也增加了添加新数据结构的难度
- 展示了乱七八糟的设计

---

## 规则 5：数据传送对象 (DTO)

### 定义
最为精练的数据结构 —— 只有公共变量、没有函数的类。

### 使用场景
- 与数据库通信
- 解析套接字传递的消息
- 原始数据转换的排头兵

### 标准 DTO 示例

```java
public class AddressDTO {
    public String street;
    public String city;
    public String state;
    public String zip;
}
```

### Bean 形式

```java
public class Address {
    private String street;
    private String streetExtra;
    private String city;
    private String state;
    private String zip;

    public Address(String street, String streetExtra,
                   String city, String state, String zip) {
        this.street = street;
        this.streetExtra = streetExtra;
        this.city = city;
        this.state = state;
        this.zip = zip;
    }

    public String getStreet() { return street; }
    public String getStreetExtra() { return streetExtra; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getZip() { return zip; }
}
```

---

## 规则 6：Active Record 的正确使用

### 什么是 Active Record？
特殊的 DTO 形式，拥有公共变量和类似 `save`、`find` 这样的可浏览方法。通常是对数据库表的直接翻译。

### ❌ 错误做法

```java
// 不要往 Active Record 中塞业务逻辑
public class User extends ActiveRecord {
    public String name;
    public String email;
    
    // 导航方法 - OK
    public void save() { /* ... */ }
    public static User find(Long id) { /* ... */ }
    
    // ❌ 业务逻辑 - 不应放这里
    public void sendWelcomeEmail() { /* ... */ }
    public boolean canAccessResource(Resource r) { /* ... */ }
}
```

### ✅ 正确做法

```java
// Active Record 只作为数据结构
public class User extends ActiveRecord {
    public String name;
    public String email;
    
    public void save() { /* ... */ }
    public static User find(Long id) { /* ... */ }
}

// 独立的业务对象包含规则
public class UserService {
    public void sendWelcomeEmail(User user) {
        // 业务逻辑
    }
    
    public boolean canAccessResource(User user, Resource resource) {
        // 访问控制逻辑
    }
}
```

---

## 总结对照表

| 概念 | 对象 (Objects) | 数据结构 (Data Structures) |
|------|---------------|---------------------------|
| **数据** | 隐藏 | 曝露 |
| **行为** | 曝露 | 无/极少 |
| **添加新类型** | 容易 | 困难 |
| **添加新行为** | 困难 | 容易 |
| **得墨忒耳律** | 适用 | 不适用 |
| **典型代表** | 业务对象 | DTO、Entity |

---

## 代码审查清单

### 数据抽象
- [ ] 是否通过抽象接口表述数据，而非直接暴露实现？
- [ ] 取值器/赋值器是否真正隐藏了实现细节？
- [ ] 是否避免了"傻乐着乱加 getter/setter"？

### 对象 vs 数据结构
- [ ] 是否根据需求（添加类型 vs 添加行为）选择了正确的方式？
- [ ] 对象是否隐藏数据并曝露操作？
- [ ] 数据结构是否只曝露数据而无行为？

### 得墨忒耳律
- [ ] 是否避免了火车失事式的链式调用？
- [ ] 方法是否只调用"朋友"的方法？
- [ ] 对象是否被要求"做事"而非"给出内部信息"？

### 避免混杂
- [ ] 是否避免了半对象半数据结构的混杂体？
- [ ] Active Record 是否只用作数据结构？
- [ ] 业务逻辑是否在独立的对象中？

---

## 核心箴言

> **对象曝露行为，隐藏数据。数据结构曝露数据，没有明显的行为。**

> **优秀的软件开发者不带成见地了解这种情形，并依据手边工作的性质选择其中一种手段。**

> **只跟朋友谈话，不与陌生人谈话。**
