# Java 21 有哪些新特性？

## 核心新特性概览

```
┌─────────────────────────────────────────────────────────────┐
│                    Java 21 新特性 (2023 LTS)                 │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 虚拟线程 Virtual Threads - 正式版                      │
│   2. 模式匹配 switch - 正式版                               │
│   3. Record Patterns - 正式版                               │
│   4. 顺序集合 Sequenced Collections                         │
│   5. 分代 ZGC                                                │
│   6. String Templates (预览)                                 │
│   7. Unnamed Patterns (预览)                                 │
│                                                             │
│   Java 21 是 LTS (长期支持版本)                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 1. 虚拟线程 (Virtual Threads) ⭐重点

```java
// 传统线程 - 重量级，占用 OS 资源
Thread platformThread = new Thread(() -> {
    System.out.println("Platform thread");
});

// 虚拟线程 - 轻量级，由 JVM 管理
Thread virtualThread = Thread.ofVirtual().start(() -> {
    System.out.println("Virtual thread");
});

// 创建虚拟线程的方式
// 方式1: Thread.ofVirtual()
Thread vt1 = Thread.ofVirtual()
    .name("my-virtual-thread")
    .start(() -> doWork());

// 方式2: Executors.newVirtualThreadPerTaskExecutor()
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 10000; i++) {
        executor.submit(() -> {
            // 每个任务一个虚拟线程
            Thread.sleep(1000);
            return "Done";
        });
    }
}

// 方式3: Thread.startVirtualThread()
Thread.startVirtualThread(() -> doWork());
```

```
┌─────────────────────────────────────────────────────────────┐
│                    虚拟线程 vs 平台线程                      │
├──────────────────┬──────────────────┬───────────────────────┤
│   维度           │   平台线程       │   虚拟线程            │
├──────────────────┼──────────────────┼───────────────────────┤
│   实现           │ OS 原生线程      │ JVM 管理              │
│   资源消耗       │ ~1MB 栈空间      │ ~几 KB                │
│   创建成本       │ 昂贵             │ 廉价                  │
│   数量限制       │ 通常几千         │ 可达百万级            │
│   阻塞时         │ 占用 OS 线程     │ 自动切换，释放载体    │
│   适用场景       │ CPU 密集型       │ IO 密集型             │
└──────────────────┴──────────────────┴───────────────────────┘
```

## 2. 模式匹配 switch

```java
// 类型模式匹配
String format(Object obj) {
    return switch (obj) {
        case Integer i -> "Integer: " + i;
        case Long l    -> "Long: " + l;
        case Double d  -> "Double: " + d;
        case String s  -> "String: " + s;
        case null      -> "null";
        default        -> "Unknown";
    };
}

// 带条件的模式匹配
String describe(Object obj) {
    return switch (obj) {
        case String s when s.isEmpty() -> "Empty string";
        case String s -> "String: " + s;
        case Integer i when i > 0 -> "Positive: " + i;
        case Integer i when i < 0 -> "Negative: " + i;
        case Integer i -> "Zero";
        default -> "Other";
    };
}

// 密封类完全匹配 (无需 default)
sealed interface Shape permits Circle, Rectangle {}
record Circle(double radius) implements Shape {}
record Rectangle(double w, double h) implements Shape {}

double area(Shape shape) {
    return switch (shape) {
        case Circle c -> Math.PI * c.radius() * c.radius();
        case Rectangle r -> r.w() * r.h();
        // 密封类，编译器知道所有子类，无需 default
    };
}
```

## 3. Record Patterns

```java
record Point(int x, int y) {}
record Line(Point start, Point end) {}

// 解构 Record
void printPoint(Object obj) {
    if (obj instanceof Point(int x, int y)) {
        System.out.println("x=" + x + ", y=" + y);
    }
}

// 嵌套解构
void printLine(Object obj) {
    if (obj instanceof Line(Point(int x1, int y1), Point(int x2, int y2))) {
        System.out.println("From (" + x1 + "," + y1 + ") to (" + x2 + "," + y2 + ")");
    }
}

// 在 switch 中使用
String describe(Object obj) {
    return switch (obj) {
        case Point(int x, int y) -> "Point at " + x + "," + y;
        case Line(Point p1, Point p2) -> "Line from " + p1 + " to " + p2;
        default -> "Unknown";
    };
}
```

## 4. 顺序集合 (Sequenced Collections)

```java
// 新增接口，统一访问首尾元素
interface SequencedCollection<E> extends Collection<E> {
    E getFirst();           // 获取第一个
    E getLast();            // 获取最后一个
    void addFirst(E e);     // 添加到开头
    void addLast(E e);      // 添加到末尾
    E removeFirst();        // 移除第一个
    E removeLast();         // 移除最后一个
    SequencedCollection<E> reversed();  // 反转视图
}

// 使用
List<String> list = new ArrayList<>();
list.addFirst("first");
list.addLast("last");
String first = list.getFirst();
String last = list.getLast();

// 反转遍历
for (String s : list.reversed()) {
    System.out.println(s);
}
```

```
┌─────────────────────────────────────────────────────────────┐
│                    Sequenced Collections                     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   SequencedCollection                                        │
│       ├── List (ArrayList, LinkedList...)                   │
│       ├── Deque                                             │
│       └── SequencedSet                                      │
│               ├── SortedSet                                 │
│               └── LinkedHashSet                             │
│                                                             │
│   SequencedMap                                               │
│       ├── SortedMap                                         │
│       └── LinkedHashMap                                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 5. 分代 ZGC

```bash
# 启用分代 ZGC
java -XX:+UseZGC -XX:+ZGenerational MyApp

# 特点:
# - 将堆分为年轻代和老年代
# - 更高的吞吐量
# - 更低的内存占用
# - 保持低延迟 (<1ms)
```

## 6. String Templates (预览)

```java
// 预览特性，需要 --enable-preview

// 字符串模板
String name = "Tom";
int age = 18;
String message = STR."Hello, \{name}! You are \{age} years old.";
// 结果: "Hello, Tom! You are 18 years old."

// 支持表达式
String calc = STR."1 + 2 = \{1 + 2}";  // "1 + 2 = 3"

// 多行
String json = STR."""
    {
      "name": "\{name}",
      "age": \{age}
    }
    """;
```

## 面试回答

### 30秒版本

> Java 21 是 **LTS 版本**，最重要的是**虚拟线程**：轻量级线程（几 KB），JVM 管理，可创建百万级，解决 IO 密集型并发问题。其他特性：**switch 模式匹配**正式版（类型匹配 + when 条件），**Record Patterns**（解构 Record），**顺序集合**（getFirst/getLast），**分代 ZGC**。

### 1分钟版本

> **Java 21 是 LTS**
>
> **虚拟线程** ⭐：
> - 轻量级，几 KB vs 平台线程 1MB
> - JVM 调度，阻塞时自动切换
> - 可创建百万级
> - IO 密集型场景显著提升
> - `Executors.newVirtualThreadPerTaskExecutor()`
>
> **switch 模式匹配**：
> - 类型匹配 + when 条件
> - 密封类完全匹配
>
> **Record Patterns**：
> - 解构 Record 组件
> - `case Point(int x, int y)`
>
> **顺序集合**：
> - getFirst()/getLast()
> - addFirst()/addLast()
> - reversed()
>
> **分代 ZGC**：更高吞吐量

---

*关联文档：[java17-features.md](java17-features.md) | [thread-pool.md](../05-concurrency/thread-pool.md)*
