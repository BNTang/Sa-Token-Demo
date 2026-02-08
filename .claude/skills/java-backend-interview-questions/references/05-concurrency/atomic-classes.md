# 你使用过Java中的哪些原子类?

## 回答

Java原子类位于`java.util.concurrent.atomic`包，基于CAS实现无锁线程安全：

```
┌─────────────────────────────────────────────────────────────┐
│                    原子类分类                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  基本类型                                                    │
│  ├── AtomicInteger        原子整数                          │
│  ├── AtomicLong           原子长整数                         │
│  └── AtomicBoolean        原子布尔                          │
│                                                             │
│  引用类型                                                    │
│  ├── AtomicReference<V>   原子引用                          │
│  ├── AtomicStampedReference  带版本号（解决ABA）             │
│  └── AtomicMarkableReference 带标记位                       │
│                                                             │
│  数组类型                                                    │
│  ├── AtomicIntegerArray   原子整数数组                       │
│  ├── AtomicLongArray      原子长整数数组                     │
│  └── AtomicReferenceArray 原子引用数组                       │
│                                                             │
│  字段更新器                                                  │
│  ├── AtomicIntegerFieldUpdater                              │
│  ├── AtomicLongFieldUpdater                                 │
│  └── AtomicReferenceFieldUpdater                            │
│                                                             │
│  累加器（JDK8+，高并发性能更好）                              │
│  ├── LongAdder            长整型累加器                       │
│  ├── DoubleAdder          双精度累加器                       │
│  ├── LongAccumulator      长整型累积器                       │
│  └── DoubleAccumulator    双精度累积器                       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 基本类型原子类

### AtomicInteger

```java
AtomicInteger counter = new AtomicInteger(0);

// 基本操作
counter.get();                    // 获取值
counter.set(10);                  // 设置值
counter.lazySet(10);              // 延迟设置（不保证立即可见）

// 原子更新
counter.incrementAndGet();        // ++i
counter.getAndIncrement();        // i++
counter.decrementAndGet();        // --i
counter.getAndDecrement();        // i--
counter.addAndGet(5);             // i += 5，返回新值
counter.getAndAdd(5);             // i += 5，返回旧值

// CAS操作
counter.compareAndSet(expect, update);  // 期望值匹配则更新

// 函数式更新（JDK8+）
counter.updateAndGet(x -> x * 2);       // 返回新值
counter.getAndUpdate(x -> x * 2);       // 返回旧值
counter.accumulateAndGet(5, (x, y) -> x + y);  // 累积
```

### AtomicLong

```java
AtomicLong counter = new AtomicLong(0L);

// 常用于计数器、序列号生成
long seq = counter.incrementAndGet();

// 与AtomicInteger API相同
```

### AtomicBoolean

```java
AtomicBoolean flag = new AtomicBoolean(false);

// 原子切换状态
if (flag.compareAndSet(false, true)) {
    // 只有一个线程能成功
    doSomething();
}

// 常用于一次性初始化
AtomicBoolean initialized = new AtomicBoolean(false);
public void init() {
    if (initialized.compareAndSet(false, true)) {
        // 初始化逻辑，只执行一次
    }
}
```

## 引用类型原子类

### AtomicReference

```java
AtomicReference<User> userRef = new AtomicReference<>(new User("张三"));

// 原子更新引用
User oldUser = userRef.get();
User newUser = new User("李四");
userRef.compareAndSet(oldUser, newUser);

// 函数式更新
userRef.updateAndGet(u -> new User(u.getName() + "_updated"));
```

### AtomicStampedReference（解决ABA问题）

```java
// 带版本号的原子引用
AtomicStampedReference<Integer> ref = 
    new AtomicStampedReference<>(100, 0);  // 初始值100，版本0

// 获取当前值和版本
int[] stampHolder = new int[1];
Integer value = ref.get(stampHolder);
int stamp = stampHolder[0];

// CAS更新（同时检查值和版本）
ref.compareAndSet(100, 200, stamp, stamp + 1);

// 解决ABA问题：即使值从A变B又变回A，版本号不同
```

### AtomicMarkableReference

```java
// 带标记位的原子引用
AtomicMarkableReference<Node> ref = 
    new AtomicMarkableReference<>(node, false);

// 获取标记
boolean[] markHolder = new boolean[1];
Node n = ref.get(markHolder);
boolean marked = markHolder[0];

// CAS更新
ref.compareAndSet(node, newNode, false, true);

// 常用于标记节点是否被逻辑删除
```

## 数组类型原子类

```java
// 原子整数数组
AtomicIntegerArray array = new AtomicIntegerArray(10);

// 操作指定索引位置
array.get(0);
array.set(0, 100);
array.incrementAndGet(0);
array.compareAndSet(0, 100, 200);

// 原子引用数组
AtomicReferenceArray<String> refArray = 
    new AtomicReferenceArray<>(new String[]{"a", "b", "c"});
refArray.compareAndSet(0, "a", "A");
```

## 字段更新器

```java
// 更新某个类的volatile字段，节省内存
public class Account {
    volatile int balance;  // 必须是volatile
}

// 创建更新器
AtomicIntegerFieldUpdater<Account> updater = 
    AtomicIntegerFieldUpdater.newUpdater(Account.class, "balance");

// 使用
Account account = new Account();
updater.incrementAndGet(account);
updater.compareAndSet(account, 0, 100);

// 优点：不需要为每个对象创建AtomicInteger
// 适合大量对象场景，节省内存
```

## 累加器（JDK8+）

### LongAdder（高并发首选）

```java
LongAdder counter = new LongAdder();

// 累加
counter.increment();  // +1
counter.add(10);      // +10

// 获取值
long sum = counter.sum();       // 获取当前和
counter.reset();                // 重置为0
long sumThenReset = counter.sumThenReset();  // 获取后重置
```

### LongAccumulator

```java
// 自定义累积函数
LongAccumulator accumulator = new LongAccumulator(
    (x, y) -> x + y,  // 累积函数
    0                  // 初始值
);

accumulator.accumulate(10);
long result = accumulator.get();

// 可以实现最大值、最小值等
LongAccumulator max = new LongAccumulator(Long::max, Long.MIN_VALUE);
max.accumulate(5);
max.accumulate(10);
max.accumulate(3);
System.out.println(max.get());  // 10
```

## AtomicInteger vs LongAdder

```
┌─────────────────────────────────────────────────────────────┐
│           AtomicLong vs LongAdder 高并发对比                 │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  AtomicLong:                                                │
│  ┌─────────┐                                               │
│  │  value  │ ← 所有线程CAS竞争同一个变量                     │
│  └─────────┘   高并发下大量CAS失败重试                       │
│                                                             │
│  LongAdder:                                                 │
│  ┌────┐ ┌────┐ ┌────┐ ┌────┐                              │
│  │Cell│ │Cell│ │Cell│ │Cell│ ← 分散到多个Cell，减少竞争     │
│  └────┘ └────┘ └────┘ └────┘                              │
│     ↓                                                       │
│  sum() 时汇总所有Cell                                        │
│                                                             │
│  性能对比（高并发）：                                         │
│  AtomicLong:  ████                                         │
│  LongAdder:   ████████████████████  更快                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 实际应用

### 计数器

```java
// 请求计数器
private final LongAdder requestCounter = new LongAdder();

public void handleRequest() {
    requestCounter.increment();
    // 处理请求
}

public long getRequestCount() {
    return requestCounter.sum();
}
```

### 无锁栈

```java
public class LockFreeStack<E> {
    private final AtomicReference<Node<E>> top = new AtomicReference<>();
    
    public void push(E item) {
        Node<E> newHead = new Node<>(item);
        Node<E> oldHead;
        do {
            oldHead = top.get();
            newHead.next = oldHead;
        } while (!top.compareAndSet(oldHead, newHead));
    }
    
    public E pop() {
        Node<E> oldHead;
        Node<E> newHead;
        do {
            oldHead = top.get();
            if (oldHead == null) return null;
            newHead = oldHead.next;
        } while (!top.compareAndSet(oldHead, newHead));
        return oldHead.item;
    }
}
```

## 面试回答

### 30秒版本
> 原子类基于CAS实现无锁线程安全。常用：①AtomicInteger/Long/Boolean基本类型；②AtomicReference原子引用；③AtomicStampedReference带版本号解决ABA；④LongAdder高并发累加器，分Cell减少竞争，比AtomicLong性能好。字段更新器可以更新volatile字段，节省内存。

### 1分钟版本
> Java原子类在`java.util.concurrent.atomic`包，基于CAS+volatile实现无锁线程安全。
> 
> **基本类型**：AtomicInteger、AtomicLong、AtomicBoolean，提供incrementAndGet()、compareAndSet()等原子操作。
> 
> **引用类型**：AtomicReference原子更新引用；AtomicStampedReference带版本号解决ABA问题，每次更新版本号加1。
> 
> **数组类型**：AtomicIntegerArray等，原子更新数组元素。
> 
> **字段更新器**：AtomicIntegerFieldUpdater更新对象的volatile字段，大量对象时节省内存。
> 
> **累加器（JDK8）**：LongAdder在高并发下性能远好于AtomicLong，原理是分散到多个Cell减少竞争，sum()时汇总。适合计数器场景。LongAccumulator支持自定义累积函数。

## 相关问题
- [[cas]] - CAS原理
- [[longadder-accumulator]] - 累加器详解
- [[aqs]] - AQS原理
