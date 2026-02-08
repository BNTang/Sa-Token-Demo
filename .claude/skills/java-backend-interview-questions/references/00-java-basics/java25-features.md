# Java 25 有哪些新特性？

## Java 25 概述

```
┌─────────────────────────────────────────────────────────────┐
│                    Java 25 新特性 (2025 LTS)                 │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   Java 25 是继 Java 21 之后的 LTS 版本                      │
│   发布时间: 2025 年 9 月                                    │
│                                                             │
│   主要特性 (正式版/预览):                                    │
│   1. Primitive Types in Patterns (正式)                     │
│   2. Flexible Constructor Bodies (正式)                     │
│   3. Structured Concurrency (正式)                          │
│   4. Scoped Values (正式)                                    │
│   5. Stream Gatherers (正式)                                │
│   6. 向量 API (正式)                                         │
│   7. Foreign Function & Memory API                          │
│   8. Compact Object Headers (实验性)                        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 1. 基本类型模式匹配

```java
// 支持在 switch 中匹配基本类型
int value = 42;

String result = switch (value) {
    case 0 -> "zero";
    case int i when i > 0 && i < 10 -> "single digit: " + i;
    case int i when i >= 10 && i < 100 -> "double digit: " + i;
    case int i -> "large number: " + i;
};

// 自动类型转换匹配
Object obj = 100;
switch (obj) {
    case int i -> System.out.println("int: " + i);
    case long l -> System.out.println("long: " + l);
    case double d -> System.out.println("double: " + d);
    default -> System.out.println("other");
}
```

## 2. 灵活的构造方法体

```java
public class User {
    private final String name;
    private final int age;
    
    public User(String name, int age) {
        // Java 25: 可以在 super()/this() 之前执行代码
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Name required");
        }
        if (age < 0) {
            throw new IllegalArgumentException("Age must be positive");
        }
        
        this.name = name;
        this.age = age;
    }
}

// 子类构造器
public class Admin extends User {
    private final String role;
    
    public Admin(String name, int age, String role) {
        // 可以先验证再调用 super
        validateRole(role);
        super(name, age);  // 之前必须是第一行
        this.role = role;
    }
    
    private static void validateRole(String role) {
        if (!role.equals("ADMIN")) throw new IllegalArgumentException();
    }
}
```

## 3. 结构化并发 (Structured Concurrency)

```java
import java.util.concurrent.StructuredTaskScope;

// 结构化并发：子任务生命周期绑定到父任务
Response handle(Request request) throws Exception {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
        // 并行执行多个任务
        Subtask<User> userTask = scope.fork(() -> findUser(request.userId()));
        Subtask<Order> orderTask = scope.fork(() -> findOrder(request.orderId()));
        
        // 等待所有任务完成
        scope.join();
        
        // 检查是否有失败
        scope.throwIfFailed();
        
        // 获取结果
        return new Response(userTask.get(), orderTask.get());
    }
    // scope 结束时，未完成的任务自动取消
}

// ShutdownOnSuccess: 任一成功就返回
String fetchFromAnySource() throws Exception {
    try (var scope = new StructuredTaskScope.ShutdownOnSuccess<String>()) {
        scope.fork(() -> fetchFromPrimary());
        scope.fork(() -> fetchFromBackup());
        
        scope.join();
        return scope.result();  // 返回第一个成功的结果
    }
}
```

```
┌─────────────────────────────────────────────────────────────┐
│                    结构化并发优势                            │
├─────────────────────────────────────────────────────────────┤
│   • 清晰的生命周期：子任务不能超过父任务                    │
│   • 自动取消：一个失败，自动取消其他                        │
│   • 可观测性：线程转储显示任务关系                          │
│   • 异常处理：结构化的异常传播                              │
└─────────────────────────────────────────────────────────────┘
```

## 4. Scoped Values (作用域值)

```java
// 替代 ThreadLocal，更安全、更高效
private static final ScopedValue<User> CURRENT_USER = ScopedValue.newInstance();

void handleRequest(User user) {
    // 绑定值到当前作用域
    ScopedValue.runWhere(CURRENT_USER, user, () -> {
        processRequest();  // 此作用域内可访问 CURRENT_USER
    });
    // 作用域结束，值自动清除
}

void processRequest() {
    // 获取作用域值
    User user = CURRENT_USER.get();
    // 如果不在作用域内调用，抛出异常
}

// 支持虚拟线程继承
```

```
┌─────────────────────────────────────────────────────────────┐
│                ScopedValue vs ThreadLocal                    │
├──────────────────┬──────────────────┬───────────────────────┤
│   维度           │   ThreadLocal    │   ScopedValue         │
├──────────────────┼──────────────────┼───────────────────────┤
│   可变性         │ 可变             │ 不可变                │
│   生命周期       │ 需要手动清理     │ 自动绑定作用域        │
│   内存泄漏       │ 常见问题         │ 不会                  │
│   虚拟线程支持   │ 性能问题         │ 原生支持              │
│   继承           │ InheritableXxx   │ 自动继承              │
└──────────────────┴──────────────────┴───────────────────────┘
```

## 5. Stream Gatherers

```java
// 自定义流中间操作
import java.util.stream.Gatherers;

// 滑动窗口
List<List<Integer>> windows = Stream.of(1, 2, 3, 4, 5)
    .gather(Gatherers.windowSliding(3))
    .toList();
// [[1,2,3], [2,3,4], [3,4,5]]

// 固定窗口
List<List<Integer>> fixed = Stream.of(1, 2, 3, 4, 5)
    .gather(Gatherers.windowFixed(2))
    .toList();
// [[1,2], [3,4], [5]]

// 扫描（累积）
List<Integer> sums = Stream.of(1, 2, 3, 4)
    .gather(Gatherers.scan(() -> 0, Integer::sum))
    .toList();
// [1, 3, 6, 10]

// 自定义 Gatherer
Gatherer<String, ?, String> myGatherer = Gatherer.of(
    () -> new StringBuilder(),
    (state, element, downstream) -> {
        state.append(element);
        if (state.length() >= 10) {
            downstream.push(state.toString());
            state.setLength(0);
        }
        return true;
    },
    (state, downstream) -> {
        if (state.length() > 0) {
            downstream.push(state.toString());
        }
    }
);
```

## 6. 向量 API (Vector API)

```java
import jdk.incubator.vector.*;

// SIMD 并行计算
void vectorAdd(float[] a, float[] b, float[] c) {
    var species = FloatVector.SPECIES_256;  // 256位向量
    int i = 0;
    
    // 向量化循环
    for (; i < species.loopBound(a.length); i += species.length()) {
        var va = FloatVector.fromArray(species, a, i);
        var vb = FloatVector.fromArray(species, b, i);
        var vc = va.add(vb);  // SIMD 并行加法
        vc.intoArray(c, i);
    }
    
    // 处理剩余元素
    for (; i < a.length; i++) {
        c[i] = a[i] + b[i];
    }
}
```

## 面试回答

### 30秒版本

> Java 25 是 **LTS 版本**（2025.9），主要特性：1）**结构化并发**正式版：StructuredTaskScope 管理子任务生命周期；2）**Scoped Values**：替代 ThreadLocal，作用域自动管理；3）**灵活构造方法**：super() 之前可以执行代码；4）**Stream Gatherers**：自定义流中间操作；5）基本类型模式匹配；6）向量 API 用于 SIMD 计算。

### 1分钟版本

> **Java 25 是 LTS**（2025 年 9 月）
>
> **结构化并发**：
> - StructuredTaskScope
> - 子任务绑定父任务生命周期
> - 自动取消，异常传播
>
> **Scoped Values**：
> - 替代 ThreadLocal
> - 不可变，作用域自动管理
> - 虚拟线程友好
>
> **灵活构造方法**：
> - super()/this() 之前可以执行代码
> - 支持参数验证
>
> **Stream Gatherers**：
> - 自定义流中间操作
> - windowSliding/windowFixed/scan
>
> **其他**：
> - 基本类型模式匹配
> - 向量 API (SIMD)
> - Compact Object Headers

---

*关联文档：[java21-features.md](java21-features.md) | [thread-pool.md](../05-concurrency/thread-pool.md)*
