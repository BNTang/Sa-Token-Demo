# Java 中的 CopyOnWriteArrayList 是什么？

> CopyOnWriteArrayList 是线程安全的 ArrayList，采用写时复制策略：写操作复制新数组，读操作无锁

## 30秒速答

CopyOnWriteArrayList 特点：
- **写时复制**: 每次修改都复制整个数组
- **读无锁**: 读操作直接访问，无需同步
- **弱一致性**: 读到的可能是旧数据
- **适用场景**: 读多写少（如监听器列表、配置缓存）

## 一分钟详解

### 写时复制原理

```
写操作（Copy-On-Write）：
┌──────────────────────────────────────────────────────┐
│  原数组: [A, B, C]                                   │
│                                                      │
│  add(D) 操作：                                       │
│  1. 加锁 (ReentrantLock)                             │
│  2. 复制新数组: [A, B, C, D]                         │
│  3. 替换引用指向新数组                               │
│  4. 释放锁                                           │
│                                                      │
│  原数组 → [A, B, C]      (正在读的线程继续使用)      │
│  新数组 → [A, B, C, D]   (新的读操作使用)            │
└──────────────────────────────────────────────────────┘

读操作（无锁）：
┌──────────────────────────────────────────────────────┐
│  直接读取数组，不加锁                                 │
│  get(i): return array[i]                             │
│                                                      │
│  读到的是快照数据，可能不是最新的                     │
└──────────────────────────────────────────────────────┘
```

### 核心源码

```java
public class CopyOnWriteArrayList<E> {
    // 使用 volatile 保证可见性
    private transient volatile Object[] array;
    
    // 读操作：无锁
    public E get(int index) {
        return (E) getArray()[index];  // 直接读，不加锁
    }
    
    final Object[] getArray() {
        return array;  // volatile 读，保证可见性
    }
    
    // 写操作：加锁 + 复制
    public boolean add(E e) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            // 复制新数组
            Object[] newElements = Arrays.copyOf(elements, len + 1);
            newElements[len] = e;
            // 替换引用
            setArray(newElements);
            return true;
        } finally {
            lock.unlock();
        }
    }
    
    // 遍历：使用快照
    public Iterator<E> iterator() {
        return new COWIterator<E>(getArray(), 0);  // 快照数组
    }
}
```

### 遍历的特殊行为

```java
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
list.add("A");
list.add("B");

// 遍历期间修改不会影响当前遍历
for (String s : list) {
    list.add("C");  // 不会抛 ConcurrentModificationException
    System.out.println(s);  // 只打印 A, B
}

// 遍历结束后，list 变成 [A, B, C, C]

// 普通 ArrayList 遍历时修改会抛异常
ArrayList<String> arrayList = new ArrayList<>(Arrays.asList("A", "B"));
for (String s : arrayList) {
    arrayList.add("C");  // 抛出 ConcurrentModificationException
}
```

### 与其他线程安全 List 对比

| 特性 | CopyOnWriteArrayList | synchronizedList | Vector |
|------|---------------------|-----------------|--------|
| 锁机制 | 写锁(ReentrantLock) | 全局synchronized | 全局synchronized |
| 读操作 | 无锁 | 加锁 | 加锁 |
| 写操作 | 加锁+复制数组 | 加锁 | 加锁 |
| 遍历时修改 | 允许(不报错) | 可能报错 | 可能报错 |
| 内存占用 | 高(写时复制) | 低 | 低 |
| 适用场景 | 读多写少 | 通用 | 过时,不推荐 |

### 使用场景

```java
// 场景1: 监听器列表
public class EventSource {
    private final CopyOnWriteArrayList<EventListener> listeners = 
        new CopyOnWriteArrayList<>();
    
    public void addListener(EventListener l) {
        listeners.add(l);  // 很少调用
    }
    
    public void fireEvent(Event e) {
        for (EventListener l : listeners) {  // 频繁调用
            l.onEvent(e);
        }
    }
}

// 场景2: 配置缓存
public class ConfigCache {
    private final CopyOnWriteArrayList<Config> configs = 
        new CopyOnWriteArrayList<>();
    
    public void refresh() {
        // 偶尔刷新配置
        configs.clear();
        configs.addAll(loadFromDB());
    }
    
    public List<Config> getConfigs() {
        return configs;  // 频繁读取
    }
}

// 场景3: 黑白名单
CopyOnWriteArrayList<String> blacklist = new CopyOnWriteArrayList<>();
// 添加黑名单很少，查询黑名单很频繁
```

### 缺点与注意事项

```java
// ❌ 不适合：写多的场景
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
for (int i = 0; i < 100000; i++) {
    list.add("item" + i);  // 每次都复制数组，性能极差
}

// ❌ 不适合：大数组频繁修改
CopyOnWriteArrayList<byte[]> largeList = new CopyOnWriteArrayList<>();
// 100万元素，每次修改复制100万个引用

// ⚠️ 弱一致性
// 读到的可能是旧数据，不保证实时性
Thread writerThread = new Thread(() -> list.add("new"));
Thread readerThread = new Thread(() -> System.out.println(list.size()));
// reader 可能读到修改前的 size
```

## 关键记忆点

```
┌─────────────────────────────────────────────────────┐
│  CopyOnWriteArrayList 速记：                        │
│                                                     │
│  「写」写时加锁 + 复制新数组 + 替换引用              │
│  「读」读时无锁，直接访问 volatile 数组             │
│  「弱」弱一致性，读到的可能是旧快照                  │
│  「少」适合读多写少场景（监听器、配置）              │
│                                                     │
│  ┌───────────────────────────────────────────┐     │
│  │ 读操作 → 无锁，高性能                      │     │
│  │ 写操作 → 复制整个数组，代价高              │     │
│  │ 遍历   → 使用快照，不抛异常                │     │
│  └───────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────┘
```

## 面试追问

**Q: CopyOnWriteArrayList 和 synchronizedList 怎么选？**

| 场景 | 选择 |
|------|------|
| 读多写少 | CopyOnWriteArrayList |
| 写多读少 | synchronizedList |
| 需要实时一致性 | synchronizedList |
| 遍历时可能修改 | CopyOnWriteArrayList |
