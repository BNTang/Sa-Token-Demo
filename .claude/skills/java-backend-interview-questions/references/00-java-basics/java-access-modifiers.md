# Java 中的访问修饰符有哪些？

## 四种访问修饰符

```
┌─────────────────────────────────────────────────────────────┐
│                    Java 访问修饰符                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ┌───────────┬───────┬───────┬─────────┬────────┐         │
│   │  修饰符   │ 本类  │ 同包  │ 子类    │ 其他包 │         │
│   ├───────────┼───────┼───────┼─────────┼────────┤         │
│   │ private   │  ✔    │  ✘    │  ✘      │  ✘     │         │
│   │ (default) │  ✔    │  ✔    │  ✘(异包)│  ✘     │         │
│   │ protected │  ✔    │  ✔    │  ✔      │  ✘     │         │
│   │ public    │  ✔    │  ✔    │  ✔      │  ✔     │         │
│   └───────────┴───────┴───────┴─────────┴────────┘         │
│                                                             │
│   访问范围: private < default < protected < public          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 详细说明

### private

```java
public class Person {
    private String name;     // 只有本类能访问
    private int age;
    
    private void secret() {  // 私有方法
        // ...
    }
    
    public void show() {
        System.out.println(name);  // 本类可以访问
        secret();                   // 本类可以调用
    }
}

// 其他类
Person p = new Person();
// p.name = "John";  // ❌ 编译错误
// p.secret();       // ❌ 编译错误
```

### default (包级私有)

```java
// com.example.a 包
class DefaultClass {           // 默认修饰符，不写
    String name;               // 默认访问级别
    
    void show() {              // 默认访问级别
        // ...
    }
}

// 同包的类可以访问
// com.example.a.Other
class Other {
    void test() {
        DefaultClass dc = new DefaultClass();
        dc.name = "test";      // ✔ 同包可访问
    }
}

// 不同包不能访问
// com.example.b.Another
// DefaultClass dc = new DefaultClass();  // ❌ 编译错误
```

### protected

```java
// com.example.parent 包
public class Parent {
    protected String name;
    
    protected void show() {
        // ...
    }
}

// 同包可以访问
// com.example.parent.SamePackage
class SamePackage {
    void test() {
        Parent p = new Parent();
        p.name = "test";  // ✔ 同包可访问
    }
}

// 不同包的子类可以访问
// com.example.child.Child
public class Child extends Parent {
    void test() {
        this.name = "test";  // ✔ 子类可访问
        this.show();         // ✔ 子类可访问
    }
}
```

### public

```java
// 任何地方都可以访问
public class PublicClass {
    public String name;
    
    public void show() {
        // ...
    }
}

// 其他包
import com.example.PublicClass;

PublicClass pc = new PublicClass();
pc.name = "test";  // ✔ 任何地方都可以
pc.show();         // ✔ 任何地方都可以
```

## 可修饰的目标

```
┌─────────────────────────────────────────────────────────────┐
│                    可修饰的目标                              │
├───────────────────┬─────────────────────────────────────────┤
│   修饰符          │   可修饰的目标                          │
├───────────────────┼─────────────────────────────────────────┤
│   private         │   成员变量、成员方法、构造器、内部类    │
│   (default)       │   类、成员变量、成员方法、构造器、内部类│
│   protected       │   成员变量、成员方法、构造器、内部类    │
│   public          │   类、成员变量、成员方法、构造器、内部类│
├───────────────────┴─────────────────────────────────────────┤
│   注意:                                                      │
│   • 外部类只能用 public 或 default                          │
│   • 局部变量不能用访问修饰符                                │
└─────────────────────────────────────────────────────────────┘
```

## 使用建议

```
┌─────────────────────────────────────────────────────────────┐
│                    使用建议                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 属性: 优先使用 private                                 │
│      └── 封装原则，通过 getter/setter 访问                  │
│                                                             │
│   2. 方法: 根据需要选择                                     │
│      └── API 方法用 public                                  │
│      └── 工具方法用 private                                 │
│      └── 子类需要用 protected                               │
│                                                             │
│   3. 类: 工具类用 default，API 类用 public                  │
│                                                             │
│   4. 最小权限原则                                            │
│      └── 能用 private 不用 default                          │
│      └── 能用 default 不用 protected                        │
│      └── 能用 protected 不用 public                         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> Java 有 **4 种访问修饰符**：**private**（仅本类）、**default**（同包）、**protected**（同包+子类）、**public**（所有）。范围从小到大。遵循**最小权限原则**：属性用 private，API 方法用 public，内部方法用 private，子类扩展用 protected。

### 1分钟版本

> **四种修饰符**：
> - private：仅本类可访问
> - default：同包可访问
> - protected：同包 + 不同包子类
> - public：任何地方
>
> **可修饰目标**：
> - 外部类：只能 public 或 default
> - 成员/方法/构造器：四种都可以
>
> **使用原则**：
> - 属性用 private（封装）
> - API 用 public
> - 内部方法用 private
> - 继承扩展用 protected
> - 最小权限原则

---

*关联文档：[java-encapsulation.md](java-encapsulation.md)*
