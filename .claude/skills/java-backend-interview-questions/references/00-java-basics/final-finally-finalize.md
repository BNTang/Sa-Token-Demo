# Java 中 final、finally 和 finalize 各有什么区别？

## 三者概述

```
┌─────────────────────────────────────────────────────────────┐
│                    final vs finally vs finalize             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   final:     关键字，用于修饰类、方法、变量                 │
│   finally:   关键字，try-catch-finally 中的代码块           │
│   finalize:  Object 类的方法，GC 前回调 (已废弃)            │
│                                                             │
│   三者除了名字相似，没有任何关系!                           │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## final 关键字

```java
// 1. final 修饰类: 不能被继承
public final class String { }
public class MyString extends String { }  // 编译错误!

// 2. final 修饰方法: 不能被重写
public class Parent {
    public final void show() { }
}
public class Child extends Parent {
    @Override
    public void show() { }  // 编译错误!
}

// 3. final 修饰变量: 只能赋值一次
final int x = 10;
x = 20;  // 编译错误!

// final 修饰引用类型
final List<String> list = new ArrayList<>();
list = new ArrayList<>();  // 编译错误! 引用不能变
list.add("hello");         // OK! 内容可以变
```

```
┌─────────────────────────────────────────────────────────────┐
│                    final 详解                                │
├──────────────────┬──────────────────────────────────────────┤
│   修饰对象       │   作用                                   │
├──────────────────┼──────────────────────────────────────────┤
│   类             │ 不能被继承 (如 String、Integer)          │
│   方法           │ 不能被子类重写                           │
│   成员变量       │ 必须初始化，且只能赋值一次               │
│   局部变量       │ 只能赋值一次                             │
│   方法参数       │ 方法内不能修改                           │
├──────────────────┴──────────────────────────────────────────┤
│   注意: final 修饰引用类型，引用不可变，但对象内容可变      │
└─────────────────────────────────────────────────────────────┘
```

## finally 代码块

```java
// finally 用于资源释放，无论是否异常都会执行
public void readFile() {
    FileInputStream fis = null;
    try {
        fis = new FileInputStream("test.txt");
        // 读取文件
    } catch (IOException e) {
        e.printStackTrace();
    } finally {
        // 无论是否异常，都会执行
        if (fis != null) {
            try {
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

// Java 7+ try-with-resources (推荐)
try (FileInputStream fis = new FileInputStream("test.txt")) {
    // 自动关闭资源
}
```

```
┌─────────────────────────────────────────────────────────────┐
│                    finally 特殊情况                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   finally 不执行的情况:                                      │
│   1. System.exit(0) 强制退出 JVM                            │
│   2. 线程被杀死                                             │
│   3. 机器断电                                               │
│                                                             │
│   finally 与 return:                                         │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  public int test() {                                 │  │
│   │      try {                                           │  │
│   │          return 1;                                   │  │
│   │      } finally {                                     │  │
│   │          return 2;  // ⚠️ finally 的 return 覆盖     │  │
│   │      }                                               │  │
│   │  }                                                   │  │
│   │  // 返回 2，不推荐在 finally 中 return                │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## finalize 方法 (已废弃)

```java
// Object 类的 finalize 方法
protected void finalize() throws Throwable { }

// 作用: 对象被 GC 回收前调用
// 用途: 释放非 Java 资源 (如文件句柄、Socket)

public class Resource {
    @Override
    protected void finalize() throws Throwable {
        try {
            // 释放资源
            System.out.println("finalize 被调用");
        } finally {
            super.finalize();
        }
    }
}
```

```
┌─────────────────────────────────────────────────────────────┐
│                    finalize 问题 (已废弃)                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ❌ 问题:                                                   │
│   1. 不保证一定执行                                         │
│   2. 执行时机不确定                                         │
│   3. 影响 GC 性能 (需要额外处理)                            │
│   4. 可能导致对象复活                                       │
│   5. 异常会被忽略                                           │
│                                                             │
│   ✅ 替代方案:                                               │
│   1. try-with-resources (Java 7+)                           │
│   2. Cleaner (Java 9+)                                      │
│   3. 手动调用 close() 方法                                  │
│                                                             │
│   Java 9: @Deprecated(since="9")                            │
│   Java 18: @Deprecated(since="9", forRemoval=true)          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 三者对比

```
┌─────────────────────────────────────────────────────────────┐
│                    三者对比                                  │
├──────────────┬──────────────────────────────────────────────┤
│   关键词     │   说明                                       │
├──────────────┼──────────────────────────────────────────────┤
│   final      │ 关键字，修饰类/方法/变量，表示不可变         │
│   finally    │ 关键字，异常处理，保证代码执行               │
│   finalize   │ Object 方法，GC 回调，已废弃                 │
├──────────────┴──────────────────────────────────────────────┤
│   三者除了名字相似，没有任何关系!                           │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> 三者除了名字相似没有任何关系！**final** 是关键字：修饰类不能继承、方法不能重写、变量只能赋值一次。**finally** 是 try-catch 代码块，无论是否异常都执行，用于资源释放。**finalize** 是 Object 的方法，GC 前调用，**已废弃**（不保证执行、影响性能），用 try-with-resources 替代。

### 1分钟版本

> **final**：
> - 修饰类：不能继承（String）
> - 修饰方法：不能重写
> - 修饰变量：只能赋值一次
> - 引用类型：引用不变，内容可变
>
> **finally**：
> - try-catch-finally 代码块
> - 无论是否异常都执行
> - 用于资源释放
> - 不执行：System.exit、线程杀死
>
> **finalize**：
> - Object 类方法
> - GC 前回调
> - **已废弃**（Java 9）
> - 问题：不保证执行、影响性能
> - 替代：try-with-resources、Cleaner

---

*关联文档：[exception-vs-error.md](exception-vs-error.md) | [java-immutable-class.md](java-immutable-class.md)*
