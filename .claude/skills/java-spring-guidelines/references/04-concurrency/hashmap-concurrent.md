# HashMap 与集合线程安全规范

> Java/Spring Boot 编码规范 - HashMap 与集合类线程安全深度指南

---

## HashMap 线程安全问题

### HashMap 不是线程安全的

**【强制】在多线程环境下，禁止使用 HashMap，必须使用 ConcurrentHashMap。**

> 说明：HashMap 在并发环境下可能导致：
> - **JDK 1.7**：死循环（扩容时链表成环）
> - **JDK 1.8**：数据丢失、覆盖

```java
// ❌ 反例 - 多线程使用 HashMap
public class UserCache {
    // 线程不安全！
    private static Map<Long, User> userCache = new HashMap<>();
    
    public void put(Long id, User user) {
        userCache.put(id, user); // 多线程可能丢失数据
    }
}

// ✅ 正例 - 使用 ConcurrentHashMap
public class UserCache {
    private static Map<Long, User> userCache = new ConcurrentHashMap<>();
    
    public void put(Long id, User user) {
        userCache.put(id, user); // 线程安全
    }
}
```

---

## JDK 1.7 vs 1.8 HashMap 区别

### JDK 1.7 HashMap 问题

**扩容时的死循环问题**

> 在 JDK 1.7 中，HashMap 扩容采用头插法，多线程并发扩容时可能形成环形链表，导致 CPU 100%。

```java
// JDK 1.7 扩容伪代码（头插法）
void transfer(Entry[] newTable) {
    Entry[] src = table;
    for (int j = 0; j < src.length; j++) {
        Entry<K,V> e = src[j];
        while (e != null) {
            Entry<K,V> next = e.next;
            int i = indexFor(e.hash, newTable.length);
            e.next = newTable[i]; // 头插法
            newTable[i] = e;
            e = next;
        }
    }
}
```

**并发场景示例：**

```java
// 线程 1 和线程 2 同时扩容
// 线程 1: A -> B
// 线程 2: B -> A
// 结果：A <-> B 形成环，get() 时死循环
```

### JDK 1.8 HashMap 改进

**1. 使用尾插法，避免死循环**

```java
// JDK 1.8 扩容伪代码（尾插法）
final Node<K,V>[] resize() {
    // ... 扩容逻辑
    if (loHead != null) {
        loTail.next = null;
        newTab[j] = loHead; // 尾插法
    }
}
```

**2. 链表转红黑树优化**

> 当链表长度 ≥ 8 且数组容量 ≥ 64 时，链表转红黑树，查询时间复杂度从 O(n) 降到 O(log n)。

```java
// JDK 1.8 结构
// 数组 + 链表 + 红黑树
// 链表长度 < 8: 使用链表
// 链表长度 ≥ 8 且容量 ≥ 64: 转红黑树
// 红黑树节点 < 6: 退化为链表
```

**3. 仍然不是线程安全**

> 说明：虽然 JDK 1.8 避免了死循环，但并发 put 仍然会导致数据丢失。

```java
// ❌ JDK 1.8 仍然不安全
Map<String, Integer> map = new HashMap<>();
// 多线程并发 put，可能丢失数据
IntStream.range(0, 1000).parallel().forEach(i -> 
    map.put("key" + i, i)
);
// map.size() 可能 < 1000
```

---

## ConcurrentHashMap 线程安全原理

### JDK 1.7 ConcurrentHashMap

**分段锁（Segment）**

```java
// JDK 1.7 结构
// Segment[] segments (默认 16 个 Segment)
// 每个 Segment 是一个 ReentrantLock
// 并发度 = Segment 数量

// 优点：不同 Segment 可并发访问
// 缺点：Segment 数量固定，扩容复杂
```

### JDK 1.8 ConcurrentHashMap

**CAS + synchronized**

```java
// JDK 1.8 结构
// Node[] table + 链表/红黑树
// put: CAS 插入头节点，失败则 synchronized 锁住头节点
// get: 无锁读取

// 优点：锁粒度更细（锁单个节点），并发度更高
```

**使用示例：**

```java
// ✅ 正例 - ConcurrentHashMap 线程安全
public class OrderCache {
    private final ConcurrentHashMap<Long, Order> cache = new ConcurrentHashMap<>();
    
    public void put(Long orderId, Order order) {
        cache.put(orderId, order);
    }
    
    public Order get(Long orderId) {
        return cache.get(orderId);
    }
    
    // putIfAbsent 是原子操作
    public Order putIfAbsent(Long orderId, Order order) {
        return cache.putIfAbsent(orderId, order);
    }
    
    // computeIfAbsent 保证只执行一次
    public Order getOrCreate(Long orderId) {
        return cache.computeIfAbsent(orderId, id -> {
            // 只有一个线程会执行这里
            return orderService.getById(id);
        });
    }
}
```

---

## HashMap vs ConcurrentHashMap vs Hashtable

### 对比表

| 特性 | HashMap | ConcurrentHashMap | Hashtable |
|------|---------|-------------------|-----------|
| **线程安全** | ❌ 否 | ✅ 是 | ✅ 是 |
| **null key** | ✅ 允许1个 | ❌ 不允许 | ❌ 不允许 |
| **null value** | ✅ 允许多个 | ❌ 不允许 | ❌ 不允许 |
| **锁粒度** | - | 节点级锁 | 方法级锁 |
| **迭代器** | fail-fast | 弱一致性 | fail-fast |
| **性能** | 🟢 最快 | 🟡 较快 | 🔴 最慢 |
| **JDK 1.8 优化** | ✅ 红黑树 | ✅ 红黑树 | ❌ 无 |

### 选择建议

```java
// 单线程环境
Map<String, String> map = new HashMap<>();

// 多线程环境
Map<String, String> map = new ConcurrentHashMap<>();

// ❌ 禁止使用 Hashtable（已过时）
Map<String, String> map = new Hashtable<>(); // 性能差

// ❌ 禁止使用 Collections.synchronizedMap（性能差）
Map<String, String> map = Collections.synchronizedMap(new HashMap<>());
```

---

## ConcurrentHashMap 最佳实践

### putIfAbsent 避免重复插入

```java
// ❌ 反例 - 非原子操作，有并发问题
if (!cache.containsKey(userId)) {
    cache.put(userId, loadUser(userId)); // 可能重复加载
}

// ✅ 正例 - putIfAbsent 是原子操作
cache.putIfAbsent(userId, loadUser(userId));

// ✅ 更好 - computeIfAbsent 只在需要时计算
cache.computeIfAbsent(userId, id -> loadUser(id));
```

### computeIfAbsent 懒加载

```java
// ❌ 反例 - 每次都加载
public User getUser(Long userId) {
    if (!cache.containsKey(userId)) {
        User user = userMapper.selectById(userId);
        cache.put(userId, user);
    }
    return cache.get(userId);
}

// ✅ 正例 - computeIfAbsent 保证只加载一次
public User getUser(Long userId) {
    return cache.computeIfAbsent(userId, id -> 
        userMapper.selectById(id)
    );
}
```

### merge 合并操作

```java
// ❌ 反例 - 需要加锁
public synchronized void increment(String key) {
    Integer count = cache.get(key);
    cache.put(key, count == null ? 1 : count + 1);
}

// ✅ 正例 - merge 原子操作
public void increment(String key) {
    cache.merge(key, 1, Integer::sum);
}
```

---

## Java 集合类线程安全总结

### 线程不安全的集合

```java
// ❌ 以下集合在多线程环境下不安全
List<String> list = new ArrayList<>();
List<String> list = new LinkedList<>();
Set<String> set = new HashSet<>();
Set<String> set = new TreeSet<>();
Map<String, String> map = new HashMap<>();
Map<String, String> map = new TreeMap<>();
```

### 线程安全的集合

```java
// ✅ 高性能并发集合（推荐）
List<String> list = new CopyOnWriteArrayList<>();
Set<String> set = new CopyOnWriteArraySet<>();
Map<String, String> map = new ConcurrentHashMap<>();
Queue<String> queue = new ConcurrentLinkedQueue<>();
Deque<String> deque = new ConcurrentLinkedDeque<>();

// ✅ 阻塞队列
BlockingQueue<String> queue = new LinkedBlockingQueue<>();
BlockingQueue<String> queue = new ArrayBlockingQueue<>(100);
BlockingDeque<String> deque = new LinkedBlockingDeque<>();

// ❌ 不推荐（性能差）
List<String> list = Collections.synchronizedList(new ArrayList<>());
Set<String> set = Collections.synchronizedSet(new HashSet<>());
Map<String, String> map = Collections.synchronizedMap(new HashMap<>());
Map<String, String> map = new Hashtable<>();
List<String> list = new Vector<>();
```

---

## CopyOnWriteArrayList 使用场景

**【推荐】读多写少场景使用 CopyOnWriteArrayList。**

> 原理：写时复制（Copy-On-Write），写操作时复制整个数组，读操作无锁。

```java
// ✅ 适用场景：配置列表、监听器列表（读多写少）
public class ConfigManager {
    private final List<ConfigListener> listeners = new CopyOnWriteArrayList<>();
    
    // 写操作：很少执行
    public void addListener(ConfigListener listener) {
        listeners.add(listener); // 复制整个数组
    }
    
    // 读操作：频繁执行
    public void notifyListeners(ConfigEvent event) {
        listeners.forEach(listener -> listener.onConfigChange(event)); // 无锁读取
    }
}

// ❌ 不适用场景：写多读少（性能差）
public class MessageQueue {
    // 错误：频繁写入会导致大量数组复制
    private final List<Message> messages = new CopyOnWriteArrayList<>();
    
    public void add(Message message) {
        messages.add(message); // 每次写入都复制整个数组！
    }
}
```

---

## 集合初始化容量最佳实践

### HashMap 初始容量计算

**【推荐】HashMap 初始容量设置为：预期元素数量 / 0.75 + 1。**

> 原因：HashMap 默认负载因子 0.75，超过容量 * 0.75 会触发扩容。

```java
// ❌ 反例 - 不指定容量，多次扩容
Map<Long, User> userMap = new HashMap<>();
for (int i = 0; i < 100; i++) {
    userMap.put((long) i, new User());
}

// ✅ 正例 - 预计算容量
int expectedSize = 100;
int initialCapacity = (int) (expectedSize / 0.75) + 1; // 134
Map<Long, User> userMap = new HashMap<>(initialCapacity);

// ✅ 更好 - 使用 Guava
Map<Long, User> userMap = Maps.newHashMapWithExpectedSize(100);
```

### ConcurrentHashMap 初始容量

```java
// ✅ 正例 - 指定初始容量
Map<Long, Order> orderMap = new ConcurrentHashMap<>(256);

// ✅ 正例 - 指定初始容量和并发度
Map<Long, Order> orderMap = new ConcurrentHashMap<>(256, 0.75f, 16);
```

---

## 面试常见问题解答

### 1. 为什么 HashMap 线程不安全？

**JDK 1.7：扩容时头插法导致死循环**
- 多线程并发扩容，链表可能形成环
- get() 时死循环，CPU 100%

**JDK 1.8：并发 put 导致数据丢失**
- 两个线程同时 put 到同一位置
- 后执行的线程覆盖前一个的数据

### 2. ConcurrentHashMap 和 Hashtable 的区别？

| 对比项 | ConcurrentHashMap | Hashtable |
|--------|-------------------|-----------|
| 锁粒度 | 节点级锁（JDK 1.8） | 方法级锁 |
| 性能 | 高并发性能好 | 性能差 |
| null | 不允许 | 不允许 |
| 推荐 | ✅ 推荐 | ❌ 已过时 |

### 3. JDK 1.8 HashMap 为什么引入红黑树？

**解决链表过长问题：**
- 链表长度 ≥ 8 时，查询时间复杂度 O(n) → O(log n)
- 防止 Hash 碰撞攻击导致性能下降

### 4. ConcurrentHashMap 的 size() 方法准确吗？

**JDK 1.7：不准确（分段统计）**
```java
// 多次统计 Segment，可能不一致
```

**JDK 1.8：弱一致性**
```java
// 使用 baseCount + counterCells，弱一致性
// 高并发下可能不准确，但性能好
```

---

## 检查清单

| 检查项 | 说明 | 优先级 |
|--------|------|--------|
| ✅ 多线程使用 ConcurrentHashMap | 避免数据丢失 | 🔴 必须 |
| ✅ 避免使用 Hashtable | 性能差 | 🔴 必须 |
| ✅ 避免使用 Collections.synchronizedMap | 性能差 | 🔴 必须 |
| ✅ HashMap 指定初始容量 | 避免扩容 | 🟡 推荐 |
| ✅ 使用 computeIfAbsent 懒加载 | 保证原子性 | 🟡 推荐 |
| ✅ 读多写少用 CopyOnWriteArrayList | 无锁读取 | 🟡 推荐 |
| ✅ 了解 JDK 1.7 vs 1.8 差异 | 避免踩坑 | 🟡 推荐 |

---

## 参考资料

- 阿里巴巴 Java 开发手册 - 集合处理
- Java Concurrent Programming in Practice
- JDK 1.8 HashMap 源码
- JDK 1.8 ConcurrentHashMap 源码
