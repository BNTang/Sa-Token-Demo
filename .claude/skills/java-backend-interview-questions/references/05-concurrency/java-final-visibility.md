# Java final 关键字与可见性

> 分类: Java 并发 | 难度: ⭐⭐⭐ | 频率: 中频

---

## 一、核心问题

**问: Java 中的 final 关键字能保证变量的可见性吗?**

**答: 可以，但有条件限制。**

---

## 二、final 的可见性保证

### 2.1 final 语义保证

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                         final 可见性规则                                      │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  前提条件: 对象引用必须正确发布(未逃逸)                                        │
│                                                                              │
│  JMM 保证:                                                                   │
│  ┌─────────────────────────────────────────────────────────────────────────┐ │
│  │  在构造函数中对 final 字段的写入                                         │ │
│  │                   ↓                                                      │ │
│  │  与随后把对象引用赋值给其他变量                                           │ │
│  │                   ↓                                                      │ │
│  │  这两个操作之间存在 happens-before 关系                                   │ │
│  └─────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│  结论: 其他线程看到对象引用时，必定能看到 final 字段的正确值                   │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 正确使用示例

```java
public class FinalFieldExample {
    private final int value;
    private final List<String> list;
    
    public FinalFieldExample() {
        this.value = 42;
        this.list = new ArrayList<>();
        this.list.add("item");  // 构造函数内的操作也被保证可见
    }
    
    public int getValue() {
        return value;  // 任何线程读取时保证看到 42
    }
    
    public List<String> getList() {
        return list;  // 任何线程读取时保证看到非空且包含"item"
    }
}

// 线程A
FinalFieldExample obj = new FinalFieldExample();
sharedRef = obj;  // 正确发布

// 线程B
FinalFieldExample ref = sharedRef;
if (ref != null) {
    int v = ref.getValue();  // 保证读到 42，不会读到 0
}
```

---

## 三、final 不能保证可见性的情况

### 3.1 构造函数中 this 逃逸

```java
public class UnsafeFinalExample {
    private final int value;
    
    public UnsafeFinalExample(Listener listener) {
        // ❌ 危险: this 逃逸，其他线程可能看到未初始化的对象
        listener.onCreated(this);
        this.value = 42;  // 此时其他线程可能已经在访问 value
    }
}

interface Listener {
    void onCreated(UnsafeFinalExample obj);
}

// 线程B通过listener可能看到 value = 0
```

### 3.2 final 引用可变对象

```java
public class FinalMutableExample {
    private final List<String> list = new ArrayList<>();
    
    // 构造函数完成后，list 引用可见性有保证
    // 但 list 内容的后续修改没有可见性保证!
    
    public void add(String item) {
        list.add(item);  // ❌ 这个操作对其他线程不保证可见
    }
    
    public List<String> getList() {
        return list;
    }
}

// 线程A
FinalMutableExample obj = new FinalMutableExample();
obj.add("item1");  // 修改 list 内容

// 线程B
List<String> list = obj.getList();
list.size();  // 可能看到 0 或 1，不确定
```

---

## 四、final vs volatile vs synchronized

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                    可见性保证对比                                             │
├──────────────┬─────────────────┬─────────────────┬───────────────────────────┤
│              │     final       │    volatile     │      synchronized         │
├──────────────┼─────────────────┼─────────────────┼───────────────────────────┤
│  可见性保证  │  ✅ 有(有限制) │    ✅ 完全保证   │      ✅ 完全保证           │
│  可修改性    │  ❌ 不可修改    │    ✅ 可修改     │      ✅ 可修改             │
│  原子性      │  ❌ 不保证      │    ❌ 不保证     │      ✅ 保证               │
│  使用场景    │  不可变字段     │    状态标志      │      临界区保护            │
│  性能开销    │  无             │    较低          │      较高                  │
└──────────────┴─────────────────┴─────────────────┴───────────────────────────┘
```

---

## 五、JMM 原理分析

### 5.1 final 字段的重排序规则

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                     final 字段重排序规则                                      │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  写 final 字段的重排序规则:                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐ │
│  │  1. 构造函数内对 final 字段的写入                                        │ │
│  │  2. 构造函数 return 之前插入 StoreStore 屏障                             │ │
│  │  3. 对象引用赋值给其他变量                                               │ │
│  │                                                                          │ │
│  │  JVM 保证: 步骤1 不会被重排序到 步骤3 之后                               │ │
│  └─────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│  读 final 字段的重排序规则:                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐ │
│  │  1. 读对象引用                                                           │ │
│  │  2. 读对象引用后插入 LoadLoad 屏障                                       │ │
│  │  3. 读对象的 final 字段                                                  │ │
│  │                                                                          │ │
│  │  JVM 保证: 步骤3 不会被重排序到 步骤1 之前                               │ │
│  └─────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 5.2 内存屏障示意

```java
public class FinalExample {
    final int x;
    int y;
    static FinalExample obj;
    
    public FinalExample() {
        x = 1;  // final 字段写入
        y = 2;  // 普通字段写入
        // [StoreStore 屏障] - JVM 自动插入
    }
    
    public static void writer() {
        obj = new FinalExample();
        // 执行顺序保证:
        // 1. x = 1
        // 2. [StoreStore 屏障]
        // 3. obj = new FinalExample()
    }
    
    public static void reader() {
        FinalExample ref = obj;
        // [LoadLoad 屏障] - JVM 自动插入
        if (ref != null) {
            int a = ref.x;  // 保证读到 1
            int b = ref.y;  // 可能读到 0 或 2
        }
    }
}
```

---

## 六、面试回答

### 30秒版本

> **final 可以保证有限的可见性**。JMM 保证：在构造函数中对 final 字段的写入，在对象引用被正确发布后，对其他线程可见。但前提是构造函数中 this 不能逃逸，且 final 只保证引用不变，引用对象的内容变化不保证可见。

### 1分钟版本

> final 的可见性保证基于 JMM 的 happens-before 规则：
>
> **保证的情况：**
> - 构造函数内对 final 字段的写入，在构造函数结束后对其他线程可见
> - JVM 会在构造函数 return 前插入 StoreStore 屏障
>
> **不保证的情况：**
> - 构造函数中 this 逃逸（其他线程可能看到半初始化对象）
> - final 引用的可变对象，其内容的后续修改不保证可见
>
> **与 volatile 对比：**
> - final 只保证初始化时的可见性，volatile 保证任何时刻的可见性
> - final 不可修改，volatile 可修改
> - final 适合不可变对象，volatile 适合状态标志

---

## 七、最佳实践

### ✅ 推荐做法

```java
// 1. 不可变对象使用 final
public final class ImmutableConfig {
    private final String host;
    private final int port;
    private final List<String> servers;  // 防御性拷贝
    
    public ImmutableConfig(String host, int port, List<String> servers) {
        this.host = host;
        this.port = port;
        this.servers = Collections.unmodifiableList(new ArrayList<>(servers));
    }
    
    public String getHost() { return host; }
    public int getPort() { return port; }
    public List<String> getServers() { return servers; }
}

// 2. 常量使用 static final
public class Constants {
    public static final int MAX_RETRY = 3;
    public static final String DEFAULT_CHARSET = "UTF-8";
}

// 3. 需要可见性且可修改用 volatile
public class StatusFlag {
    private volatile boolean running = true;
    
    public void stop() {
        running = false;  // 对其他线程立即可见
    }
    
    public boolean isRunning() {
        return running;
    }
}
```

### ❌ 避免做法

```java
// ❌ 构造函数中 this 逃逸
public class BadExample {
    private final int value;
    
    public BadExample() {
        // 危险: 其他线程可能看到 value = 0
        GlobalRegistry.register(this);
        this.value = 42;
    }
}

// ❌ 依赖 final 保证可变对象内容的可见性
public class BadMutableFinal {
    private final Map<String, String> cache = new HashMap<>();
    
    public void put(String key, String value) {
        cache.put(key, value);  // 不保证对其他线程可见
    }
}

// ✅ 修复: 使用线程安全的集合
public class GoodMutableFinal {
    private final Map<String, String> cache = new ConcurrentHashMap<>();
}
```

---

## 八、实战场景

### 8.1 单例模式中的 final

```java
// 静态内部类单例 - 利用 final 和类加载机制保证可见性
public class Singleton {
    private Singleton() {}
    
    private static class Holder {
        // final 保证初始化完成后可见
        private static final Singleton INSTANCE = new Singleton();
    }
    
    public static Singleton getInstance() {
        return Holder.INSTANCE;
    }
}
```

### 8.2 不可变配置对象

```java
/**
 * 配置对象 - 利用 final 保证线程安全
 */
public final class DatabaseConfig {
    private final String url;
    private final String username;
    private final int maxPoolSize;
    
    private DatabaseConfig(Builder builder) {
        this.url = builder.url;
        this.username = builder.username;
        this.maxPoolSize = builder.maxPoolSize;
    }
    
    // 线程安全: 所有字段都是 final，对象发布后所有线程可见
    public String getUrl() { return url; }
    public String getUsername() { return username; }
    public int getMaxPoolSize() { return maxPoolSize; }
    
    public static class Builder {
        private String url;
        private String username;
        private int maxPoolSize = 10;
        
        public Builder url(String url) { this.url = url; return this; }
        public Builder username(String username) { this.username = username; return this; }
        public Builder maxPoolSize(int size) { this.maxPoolSize = size; return this; }
        public DatabaseConfig build() { return new DatabaseConfig(this); }
    }
}
```
