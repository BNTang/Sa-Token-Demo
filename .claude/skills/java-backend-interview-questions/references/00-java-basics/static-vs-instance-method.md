# Java 中静态方法和实例方法的区别是什么？

## 核心区别

```
┌─────────────────────────────────────────────────────────────┐
│                    静态方法 vs 实例方法                      │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ┌────────────────────────────────────────────────────┐   │
│   │                   类 (Class)                        │   │
│   │  ┌──────────────────────────────────────────────┐  │   │
│   │  │  static 成员 (类级别)                        │  │   │
│   │  │  • static 变量 (共享)                        │  │   │
│   │  │  • static 方法 (无需实例)                    │  │   │
│   │  └──────────────────────────────────────────────┘  │   │
│   │                                                     │   │
│   │  ┌──────────────────────────────────────────────┐  │   │
│   │  │  实例成员 (对象级别)                         │  │   │
│   │  │  • 实例变量 (每个对象独立)                   │  │   │
│   │  │  • 实例方法 (需要对象调用)                   │  │   │
│   │  └──────────────────────────────────────────────┘  │   │
│   └────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 对比表

```
┌─────────────────────────────────────────────────────────────┐
│                    对比总结                                  │
├──────────────────┬──────────────────┬───────────────────────┤
│   特性           │   静态方法       │   实例方法            │
├──────────────────┼──────────────────┼───────────────────────┤
│   关键字         │   static         │   无                  │
│   调用方式       │   类名.方法()    │   对象.方法()         │
│   是否需要实例   │   不需要         │   需要                │
│   能否访问 this  │   不能           │   能                  │
│   访问实例成员   │   不能直接访问   │   可以                │
│   访问静态成员   │   可以           │   可以                │
│   内存分配       │   类加载时       │   对象创建时          │
│   典型用途       │   工具方法       │   操作对象状态        │
└──────────────────┴──────────────────┴───────────────────────┘
```

## 代码示例

```java
public class Calculator {
    // 静态变量
    private static int count = 0;
    
    // 实例变量
    private String name;
    
    // 实例方法: 需要对象调用
    public void setName(String name) {
        this.name = name;  // 可以使用 this
        count++;           // 可以访问静态成员
    }
    
    public String getName() {
        return this.name;
    }
    
    // 静态方法: 不需要对象
    public static int add(int a, int b) {
        // this.name = "test";  // ❌ 不能使用 this
        // getName();           // ❌ 不能直接调用实例方法
        return a + b;
    }
    
    public static int getCount() {
        return count;  // 可以访问静态成员
    }
}

// 调用方式
// 静态方法: 类名直接调用
int sum = Calculator.add(1, 2);

// 实例方法: 需要创建对象
Calculator calc = new Calculator();
calc.setName("MyCalc");
String name = calc.getName();
```

## 静态方法的限制

```java
public class Demo {
    private int instanceVar = 10;
    private static int staticVar = 20;
    
    public static void staticMethod() {
        // ❌ 不能访问实例成员
        // System.out.println(instanceVar);
        // instanceMethod();
        
        // ❌ 不能使用 this 和 super
        // this.toString();
        
        // ✔ 可以访问静态成员
        System.out.println(staticVar);
        staticMethod2();
        
        // ✔ 可以创建对象访问实例成员
        Demo demo = new Demo();
        System.out.println(demo.instanceVar);
    }
    
    public static void staticMethod2() { }
    
    public void instanceMethod() {
        // ✔ 可以访问所有成员
        System.out.println(instanceVar);
        System.out.println(staticVar);
        staticMethod();
    }
}
```

## 使用场景

```
┌─────────────────────────────────────────────────────────────┐
│                    使用场景                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   静态方法适用场景:                                          │
│   ├── 工具类方法 (Math.abs(), Collections.sort())          │
│   ├── 工厂方法 (Integer.valueOf())                          │
│   ├── 不依赖对象状态的操作                                  │
│   └── 单例模式获取实例                                      │
│                                                             │
│   实例方法适用场景:                                          │
│   ├── 需要访问/修改对象状态                                 │
│   ├── 与具体对象相关的行为                                  │
│   └── 需要多态的方法                                        │
│                                                             │
│   ⚠️ 注意:                                                   │
│   • 静态方法不能被重写 (只能被隐藏)                         │
│   • 静态方法不支持多态                                      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 常见误区

```java
// ❌ 误区1: 用对象调用静态方法 (不推荐)
Calculator calc = new Calculator();
calc.add(1, 2);  // 实际调用 Calculator.add()
// 编译器会警告，应使用 Calculator.add()

// ❌ 误区2: 认为静态方法可以被重写
class Parent {
    public static void show() {
        System.out.println("Parent");
    }
}

class Child extends Parent {
    public static void show() {  // 这是隐藏，不是重写
        System.out.println("Child");
    }
}

Parent p = new Child();
p.show();  // 输出 "Parent"，不是多态!
```

## 面试回答

### 30秒版本

> **静态方法**用 `static` 修饰，通过类名调用，不需要实例，不能访问 `this` 和实例成员。**实例方法**通过对象调用，可访问所有成员。静态方法适合工具类、工厂方法；实例方法适合操作对象状态。注意：静态方法**不能被重写**，不支持多态。

### 1分钟版本

> **区别**：
> - 调用：静态用类名，实例用对象
> - this：静态不能用，实例可以用
> - 访问：静态只能访问静态，实例都可访问
>
> **使用场景**：
> - 静态：工具方法、工厂方法
> - 实例：操作对象状态
>
> **注意**：
> - 静态方法不能重写（只是隐藏）
> - 静态方法不支持多态
> - 不建议用对象调用静态方法

---

*关联文档：[java-inheritance.md](java-inheritance.md)*
