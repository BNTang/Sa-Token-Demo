# 接口和抽象类有什么区别？

## 核心区别

```
┌─────────────────────────────────────────────────────────────┐
│                接口 (Interface) vs 抽象类 (Abstract Class)   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   抽象类:                                                    │
│   ├── "是什么" (is-a)                                       │
│   ├── 表示事物的本质                                        │
│   ├── 单继承                                                │
│   └── 例: Animal 是抽象概念，Dog 是具体 Animal              │
│                                                             │
│   接口:                                                      │
│   ├── "能做什么" (can-do)                                   │
│   ├── 表示能力、行为                                        │
│   ├── 多实现                                                │
│   └── 例: Flyable 表示能飞的能力，Bird 实现 Flyable         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 详细对比

```
┌─────────────────────────────────────────────────────────────┐
│                    详细对比表                                │
├──────────────────────┬───────────────────┬──────────────────┤
│   特性               │   抽象类          │   接口           │
├──────────────────────┼───────────────────┼──────────────────┤
│   关键字             │ abstract class    │ interface        │
│   继承/实现          │ extends (单继承)  │ implements (多)  │
│   构造方法           │ 有                │ 无               │
│   成员变量           │ 任意              │ public static final│
│   普通方法           │ 可以有            │ 无 (Java 8 前)   │
│   抽象方法           │ 可以有            │ 全是 (默认)      │
│   静态方法           │ 可以有            │ Java 8+          │
│   默认方法           │ 无                │ Java 8+ default  │
│   设计层面           │ 模板 (is-a)       │ 能力 (can-do)    │
│   访问修饰符         │ 任意              │ public (隐式)    │
└──────────────────────┴───────────────────┴──────────────────┘
```

## 代码示例

```java
// 抽象类: 表示"是什么"
public abstract class Animal {
    protected String name;  // 可以有成员变量
    
    public Animal(String name) {  // 可以有构造方法
        this.name = name;
    }
    
    // 普通方法
    public void sleep() {
        System.out.println(name + " is sleeping");
    }
    
    // 抽象方法
    public abstract void speak();
}

// 接口: 表示"能做什么"
public interface Flyable {
    // 常量 (隐式 public static final)
    int MAX_ALTITUDE = 10000;
    
    // 抽象方法 (隐式 public abstract)
    void fly();
    
    // 默认方法 (Java 8+)
    default void land() {
        System.out.println("Landing...");
    }
    
    // 静态方法 (Java 8+)
    static void checkWeather() {
        System.out.println("Weather is good for flying");
    }
}

// 使用
public class Bird extends Animal implements Flyable {
    public Bird(String name) {
        super(name);
    }
    
    @Override
    public void speak() {
        System.out.println(name + " chirps");
    }
    
    @Override
    public void fly() {
        System.out.println(name + " is flying");
    }
}
```

## Java 8+ 接口新特性

```java
public interface ModernInterface {
    // 抽象方法
    void abstractMethod();
    
    // 默认方法 - 有实现，子类可选重写
    default void defaultMethod() {
        System.out.println("Default implementation");
        privateMethod();  // 可调用私有方法
    }
    
    // 静态方法 - 属于接口
    static void staticMethod() {
        System.out.println("Static method");
    }
    
    // 私有方法 (Java 9+) - 用于默认方法内部复用
    private void privateMethod() {
        System.out.println("Private helper");
    }
}
```

```
┌─────────────────────────────────────────────────────────────┐
│                    接口方法演化                              │
├─────────────────────────────────────────────────────────────┤
│   Java 7 之前:                                               │
│   • 只能有抽象方法和常量                                    │
│                                                             │
│   Java 8:                                                    │
│   • 默认方法 (default)                                      │
│   • 静态方法 (static)                                       │
│                                                             │
│   Java 9:                                                    │
│   • 私有方法 (private)                                      │
│   • 私有静态方法 (private static)                           │
│                                                             │
│   Java 17:                                                   │
│   • 密封接口 (sealed interface)                             │
└─────────────────────────────────────────────────────────────┘
```

## 设计原则

```
┌─────────────────────────────────────────────────────────────┐
│                    何时使用抽象类                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 需要共享代码（模板方法模式）                           │
│   2. 需要非 static、非 final 的成员变量                     │
│   3. 子类之间有明确的 "is-a" 关系                           │
│   4. 需要非 public 的成员                                   │
│   5. 需要构造方法                                           │
│                                                             │
│   例: Template Method 模式                                   │
│   abstract class Game {                                      │
│       final void play() {  // 模板方法                      │
│           initialize();                                      │
│           start();                                           │
│           end();                                             │
│       }                                                      │
│       abstract void initialize();                            │
│       abstract void start();                                 │
│       abstract void end();                                   │
│   }                                                          │
│                                                             │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    何时使用接口                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 定义能力、行为规范                                     │
│   2. 需要多重继承能力                                       │
│   3. 不相关的类需要共同的行为                               │
│   4. 面向接口编程                                           │
│   5. 策略模式、回调                                         │
│                                                             │
│   例:                                                        │
│   interface Comparable<T> { int compareTo(T o); }            │
│   interface Runnable { void run(); }                         │
│   interface Serializable { } // 标记接口                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> **抽象类**表示"是什么"（is-a），单继承，可以有构造方法、成员变量、普通方法。**接口**表示"能做什么"（can-do），多实现，成员变量默认 public static final，Java 8+ 可以有默认方法和静态方法。抽象类用于共享代码和模板方法；接口用于定义能力规范和多重继承。

### 1分钟版本

> **语法区别**：
> - 抽象类：单继承，有构造器，任意成员
> - 接口：多实现，无构造器，常量 + 方法
>
> **设计语义**：
> - 抽象类："是什么" (is-a)
> - 接口："能做什么" (can-do)
>
> **Java 8+ 接口**：
> - default 方法：默认实现
> - static 方法：属于接口
> - Java 9: private 方法
>
> **使用场景**：
> - 抽象类：模板方法模式，共享代码
> - 接口：定义能力，策略模式，面向接口编程
>
> **推荐**：
> - 优先使用接口
> - 需要共享代码时用抽象类
> - 可以组合使用

---

*关联文档：[java-polymorphism.md](java-polymorphism.md) | [java-no-multiple-inheritance.md](java-no-multiple-inheritance.md)*
