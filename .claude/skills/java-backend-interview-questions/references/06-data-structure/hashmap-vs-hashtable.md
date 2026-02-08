# Java 中的 HashMap 和 Hashtable 有什么区别？

## 核心区别

```
┌─────────────────────────────────────────────────────────────┐
│                 HashMap vs Hashtable                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ┌───────────────────────────────────────────────────────┐│
│   │  特性        │  HashMap          │  Hashtable         ││
│   ├───────────────────────────────────────────────────────┤│
│   │  线程安全    │  否               │  是(synchronized)  ││
│   │  null Key    │  允许 1 个        │  不允许            ││
│   │  null Value  │  允许多个         │  不允许            ││
│   │  继承关系    │  AbstractMap      │  Dictionary        ││
│   │  迭代器      │  fail-fast        │  fail-fast         ││
│   │  性能        │  高               │  低(全局锁)        ││
│   │  JDK版本     │  1.2              │  1.0               ││
│   │  推荐使用    │  是               │  否(已过时)        ││
│   └───────────────────────────────────────────────────────┘│
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 详细对比

### 1. 线程安全

```java
// Hashtable: 方法级 synchronized
public synchronized V put(K key, V value) { ... }
public synchronized V get(Object key) { ... }

// HashMap: 非线程安全
public V put(K key, V value) { ... }  // 无 synchronized

// 并发场景替代方案
// ❌ 不推荐
Map<String, Object> table = new Hashtable<>();

// ✅ 推荐: ConcurrentHashMap
Map<String, Object> map = new ConcurrentHashMap<>();

// ✅ 或同步包装
Map<String, Object> syncMap = Collections.synchronizedMap(new HashMap<>());
```

### 2. null 处理

```java
// HashMap: 允许 null
Map<String, Object> hashMap = new HashMap<>();
hashMap.put(null, "value");     // OK, null key
hashMap.put("key", null);       // OK, null value
hashMap.put(null, null);        // OK

// Hashtable: 不允许 null
Map<String, Object> hashtable = new Hashtable<>();
hashtable.put(null, "value");   // NullPointerException!
hashtable.put("key", null);     // NullPointerException!
```

### 3. 底层实现

```
┌─────────────────────────────────────────────────────────────┐
│                    底层实现对比                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   HashMap (JDK 8+):                                          │
│   • 数组 + 链表 + 红黑树                                    │
│   • 链表长度 ≥ 8 转红黑树                                   │
│   • 红黑树节点 ≤ 6 退化为链表                               │
│   • 扰动函数: hash ^ (hash >>> 16)                          │
│                                                             │
│   Hashtable:                                                 │
│   • 数组 + 链表                                             │
│   • 无红黑树优化                                            │
│   • 直接使用 hashCode()                                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 4. 扩容机制

```java
// HashMap
// 初始容量: 16
// 负载因子: 0.75
// 扩容: 2 倍
int newCapacity = oldCapacity << 1;  // 2倍

// Hashtable
// 初始容量: 11
// 负载因子: 0.75
// 扩容: 2倍 + 1
int newCapacity = (oldCapacity << 1) + 1;
```

### 5. 索引计算

```java
// HashMap: 位运算 (要求容量是2的幂)
int index = (n - 1) & hash;  // 效率高

// Hashtable: 取模运算
int index = (hash & 0x7FFFFFFF) % table.length;  // 效率低
```

## 性能对比

```
┌─────────────────────────────────────────────────────────────┐
│                    性能对比                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   操作               HashMap        Hashtable               │
│   ─────              ───────        ─────────               │
│   单线程 put         ~100ms          ~150ms                  │
│   单线程 get         ~50ms           ~80ms                   │
│   并发读写           ❌ 异常          ~500ms (全局锁)        │
│                                                             │
│   ConcurrentHashMap 并发性能最好 (~100ms)                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 代码示例

```java
// 单线程场景
Map<String, String> map = new HashMap<>();
map.put("key", "value");
String value = map.get("key");

// 多线程场景
// ❌ 不安全
Map<String, String> unsafeMap = new HashMap<>();

// ❌ 过时
Map<String, String> oldWay = new Hashtable<>();

// ✅ 推荐
Map<String, String> safeMap = new ConcurrentHashMap<>();

// ✅ 需要保持 HashMap 时
Map<String, String> syncMap = Collections.synchronizedMap(new HashMap<>());
```

## 选择建议

```
┌─────────────────────────────────────────────────────────────┐
│                    选择建议                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   单线程:                                                    │
│   └── HashMap (首选)                                        │
│                                                             │
│   多线程:                                                    │
│   ├── ConcurrentHashMap (首选，高性能)                      │
│   └── Collections.synchronizedMap (需要 HashMap 特性时)     │
│                                                             │
│   ❌ Hashtable: 不推荐使用                                   │
│      • 全局锁性能差                                         │
│      • 不支持 null                                          │
│      • 遗留类，无优化                                       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> **HashMap** 非线程安全，允许 null key/value，JDK8 用数组+链表+红黑树，性能高。**Hashtable** 线程安全（全局 synchronized），不允许 null，无红黑树优化。Hashtable 已过时，并发用 **ConcurrentHashMap**（分段锁/CAS，性能更好）。

### 1分钟版本

> **线程安全**：
> - HashMap：不安全
> - Hashtable：synchronized 全局锁
>
> **null 支持**：
> - HashMap：允许 null key/value
> - Hashtable：不允许
>
> **底层实现**：
> - HashMap：数组+链表+红黑树
> - Hashtable：数组+链表
>
> **性能**：
> - HashMap 更快（无锁、红黑树优化）
>
> **结论**：
> - 单线程用 HashMap
> - 多线程用 ConcurrentHashMap
> - Hashtable 已过时

---

*关联文档：[hashmap-principle.md](hashmap-principle.md) | [concurrenthashmap-vs-hashtable.md](concurrenthashmap-vs-hashtable.md)*
