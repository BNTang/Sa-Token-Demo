# 什么是 Java 内部类？它有什么作用？

## 内部类概述

```
┌─────────────────────────────────────────────────────────────┐
│                    Java 内部类                               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   定义: 定义在另一个类内部的类                              │
│                                                             │
│   内部类分类:                                                │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  1. 成员内部类 (Member Inner Class)                  │  │
│   │  2. 静态内部类 (Static Nested Class)                 │  │
│   │  3. 局部内部类 (Local Inner Class)                   │  │
│   │  4. 匿名内部类 (Anonymous Inner Class)               │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 1. 成员内部类

```java
public class Outer {
    private String name = "Outer";
    private int value = 10;
    
    // 成员内部类
    public class Inner {
        private String name = "Inner";
        
        public void display() {
            // 访问外部类私有成员
            System.out.println(value);  // 10
            
            // 同名变量访问
            System.out.println(name);             // Inner
            System.out.println(this.name);        // Inner
            System.out.println(Outer.this.name);  // Outer
        }
    }
}

// 使用
Outer outer = new Outer();
Outer.Inner inner = outer.new Inner();  // 需要外部类实例
inner.display();
```

```
┌─────────────────────────────────────────────────────────────┐
│                    成员内部类特点                            │
├─────────────────────────────────────────────────────────────┤
│   • 可以访问外部类所有成员（包括 private）                  │
│   • 创建时需要外部类实例: outer.new Inner()                 │
│   • 持有外部类引用: Outer.this                              │
│   • 不能有 static 成员（除了 static final 常量）            │
└─────────────────────────────────────────────────────────────┘
```

## 2. 静态内部类

```java
public class Outer {
    private static String staticName = "Static Outer";
    private String name = "Outer";
    
    // 静态内部类
    public static class StaticInner {
        public void display() {
            // 只能访问外部类的静态成员
            System.out.println(staticName);  // OK
            // System.out.println(name);     // 编译错误!
        }
    }
}

// 使用 - 不需要外部类实例
Outer.StaticInner inner = new Outer.StaticInner();
inner.display();
```

```
┌─────────────────────────────────────────────────────────────┐
│                    静态内部类特点                            │
├─────────────────────────────────────────────────────────────┤
│   • 只能访问外部类的静态成员                                │
│   • 创建时不需要外部类实例: new Outer.StaticInner()         │
│   • 不持有外部类引用                                        │
│   • 可以有静态成员                                          │
│   • 常用于: Builder 模式、工具类                            │
└─────────────────────────────────────────────────────────────┘
```

## 3. 局部内部类

```java
public class Outer {
    public void method() {
        final int localVar = 10;  // 必须是 final 或 effectively final
        
        // 局部内部类 - 定义在方法内
        class LocalInner {
            public void display() {
                System.out.println(localVar);
            }
        }
        
        LocalInner inner = new LocalInner();
        inner.display();
    }
}
```

## 4. 匿名内部类

```java
public class AnonymousDemo {
    
    public void demo() {
        // 匿名内部类 - 实现接口
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                System.out.println("Running...");
            }
        };
        
        // 匿名内部类 - 继承类
        Thread thread = new Thread() {
            @Override
            public void run() {
                System.out.println("Thread running...");
            }
        };
        
        // Java 8+ Lambda 替代（函数式接口）
        Runnable lambda = () -> System.out.println("Lambda running...");
    }
}
```

```
┌─────────────────────────────────────────────────────────────┐
│                    匿名内部类特点                            │
├─────────────────────────────────────────────────────────────┤
│   • 没有类名，直接 new 接口/父类                            │
│   • 只能使用一次                                            │
│   • 只能有一个父类或实现一个接口                            │
│   • 不能有构造方法                                          │
│   • Java 8 函数式接口可用 Lambda 替代                       │
└─────────────────────────────────────────────────────────────┘
```

## 内部类作用

```
┌─────────────────────────────────────────────────────────────┐
│                    内部类的作用                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 封装性                                                  │
│      └── 隐藏实现细节，只对外部类可见                       │
│                                                             │
│   2. 访问外部类私有成员                                      │
│      └── 内部类可以直接访问外部类的 private 成员            │
│                                                             │
│   3. 回调机制                                                │
│      └── 匿名内部类实现事件监听器                           │
│                                                             │
│   4. 多继承的变通                                            │
│      └── 内部类可以独立继承，不受外部类影响                 │
│                                                             │
│   5. 代码组织                                                │
│      └── 逻辑相关的类放在一起                               │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 实际应用

```java
// 1. Builder 模式 - 静态内部类
public class User {
    private final String name;
    private final int age;
    
    private User(Builder builder) {
        this.name = builder.name;
        this.age = builder.age;
    }
    
    public static class Builder {
        private String name;
        private int age;
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder age(int age) {
            this.age = age;
            return this;
        }
        
        public User build() {
            return new User(this);
        }
    }
}

// 使用
User user = new User.Builder()
    .name("Tom")
    .age(18)
    .build();

// 2. 迭代器 - 成员内部类
public class MyList<E> {
    private Object[] elements;
    
    public Iterator<E> iterator() {
        return new MyIterator();
    }
    
    private class MyIterator implements Iterator<E> {
        private int index = 0;
        
        @Override
        public boolean hasNext() {
            return index < elements.length;
        }
        
        @Override
        public E next() {
            return (E) elements[index++];
        }
    }
}
```

## 对比总结

```
┌─────────────────────────────────────────────────────────────┐
│                    内部类对比                                │
├──────────────┬───────────────────────────────────────────────┤
│   类型       │   特点                                        │
├──────────────┼───────────────────────────────────────────────┤
│   成员内部类 │ 持有外部类引用，可访问所有成员                │
│   静态内部类 │ 不持有外部引用，只能访问静态成员              │
│   局部内部类 │ 定义在方法内，作用域仅在方法内                │
│   匿名内部类 │ 无类名，一次性使用，可用 Lambda 替代          │
└──────────────┴───────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> 内部类是定义在另一个类内部的类，有四种：**成员内部类**（可访问外部类所有成员，持有外部类引用）、**静态内部类**（只能访问静态成员，不持有引用，常用于 Builder 模式）、**局部内部类**（定义在方法内）、**匿名内部类**（无类名，常用于回调，Java 8 可用 Lambda 替代）。作用：封装、访问私有成员、回调机制。

### 1分钟版本

> **四种内部类**：
> 1. **成员内部类**：
>    - 持有外部类引用
>    - 可访问所有成员
>    - 创建需要外部类实例
>
> 2. **静态内部类**：
>    - 不持有外部类引用
>    - 只能访问静态成员
>    - Builder 模式常用
>
> 3. **局部内部类**：定义在方法内
>
> 4. **匿名内部类**：
>    - 无类名，一次性使用
>    - Java 8 可用 Lambda 替代
>
> **作用**：
> - 封装：隐藏实现
> - 访问私有成员
> - 回调机制
> - 代码组织

---

*关联文档：[singleton-pattern.md](../14-design-pattern/singleton-pattern.md) | [java8-features.md](java8-features.md)*
