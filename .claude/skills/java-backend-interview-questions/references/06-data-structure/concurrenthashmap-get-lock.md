# ConcurrentHashMap 的 get 方法是否需要加锁？

> get 方法不需要加锁，通过 volatile 保证可见性，实现无锁读取

## 30秒速答

**不需要加锁**！ConcurrentHashMap 的 get 操作是**无锁**的：
- 使用 **volatile** 保证 Node 数组和 Node.val 的可见性
- 使用 **Unsafe.getObjectVolatile()** 保证读取的原子性
- 读操作不会阻塞写操作，实现读写分离

## 一分钟详解

### get 方法源码分析（JDK 1.8）

```java
public V get(Object key) {
    Node<K,V>[] tab; Node<K,V> e, p; int n, eh; K ek;
    int h = spread(key.hashCode());  // 计算 hash
    
    if ((tab = table) != null && (n = tab.length) > 0 &&
        (e = tabAt(tab, (n - 1) & h)) != null) {  // volatile 读取桶
        
        if ((eh = e.hash) == h) {
            if ((ek = e.key) == key || (ek != null && key.equals(ek)))
                return e.val;  // 头节点命中
        }
        else if (eh < 0)  // 红黑树或 ForwardingNode
            return (p = e.find(h, key)) != null ? p.val : null;
        
        while ((e = e.next) != null) {  // 遍历链表
            if (e.hash == h &&
                ((ek = e.key) == key || (ek != null && key.equals(ek))))
                return e.val;
        }
    }
    return null;
}
```

### 为什么不需要加锁？

```
关键：volatile 保证可见性

┌──────────────────────────────────────────────────────┐
│  transient volatile Node<K,V>[] table;  // 数组volatile│
│                                                      │
│  static class Node<K,V> {                            │
│      final int hash;                                 │
│      final K key;                                    │
│      volatile V val;     // value 是 volatile        │
│      volatile Node<K,V> next; // next 是 volatile    │
│  }                                                   │
└──────────────────────────────────────────────────────┘

volatile 作用：
1. 写入后立即刷新到主内存
2. 读取时从主内存获取最新值
3. 禁止指令重排序
```

### tabAt 方法：安全读取

```java
// 使用 Unsafe 保证读取的原子性和可见性
static final <K,V> Node<K,V> tabAt(Node<K,V>[] tab, int i) {
    return (Node<K,V>)U.getObjectVolatile(tab, 
        ((long)i << ASHIFT) + ABASE);
}

// 等价于 volatile 读
// 即使正在扩容，也能读到正确的值
```

### 并发场景分析

```
场景1: 读写并发
┌─────────────────────────────────────────────────────┐
│  Thread A (写):        Thread B (读):                │
│  synchronized(f) {     get(key) {                   │
│      插入新节点           读取 table[i]              │
│      node.val = v;       遍历链表                    │
│  }                       返回 value                  │
│                                                     │
│  结果：B 要么读到旧值，要么读到新值（一致性）        │
│        不会读到中间状态或损坏数据                    │
└─────────────────────────────────────────────────────┘

场景2: 扩容时读取
┌─────────────────────────────────────────────────────┐
│  扩容线程:              读取线程:                    │
│  创建新数组              读取旧数组                  │
│  迁移节点到新数组         遇到 ForwardingNode        │
│                          → 去新数组查找              │
│                                                     │
│  结果：读取线程能正确找到数据                        │
└─────────────────────────────────────────────────────┘
```

### ForwardingNode 处理扩容

```java
// 扩容时占位节点，hash = MOVED = -1
static final class ForwardingNode<K,V> extends Node<K,V> {
    final Node<K,V>[] nextTable;  // 指向新表
    
    Node<K,V> find(int h, Object k) {
        // 从新表中查找
        outer: for (Node<K,V>[] tab = nextTable;;) {
            // ...在 nextTable 中查找
        }
    }
}

// get 遇到 ForwardingNode 时
if (eh < 0)  // hash < 0 表示特殊节点
    return (p = e.find(h, key)) != null ? p.val : null;
// 调用 ForwardingNode.find() 去新表查找
```

### 对比 JDK 1.7

```java
// JDK 1.7 的 get 也不需要加锁
V get(Object key, int hash) {
    if (count != 0) {  // volatile 读
        HashEntry<K,V> e = getFirst(hash);
        while (e != null) {
            if (e.hash == hash && key.equals(e.key)) {
                V v = e.value;  // volatile 读
                if (v != null)
                    return v;
                return readValueUnderLock(e);  // 极少情况
            }
            e = e.next;
        }
    }
    return null;
}

// 注意：JDK 1.7 有极少情况需要加锁读（value 为 null 时）
// JDK 1.8 完全无锁
```

## 关键记忆点

```
┌─────────────────────────────────────────────────────┐
│  ConcurrentHashMap get 无锁读取：                    │
│                                                     │
│  ┌─────────────────────────────────────────────┐   │
│  │  volatile table[]   → 数组引用可见性         │   │
│  │  volatile Node.val  → 值可见性               │   │
│  │  volatile Node.next → 链表可见性             │   │
│  │  Unsafe.getObjectVolatile → 原子读           │   │
│  └─────────────────────────────────────────────┘   │
│                                                     │
│  扩容时: ForwardingNode 转发到新表                  │
│                                                     │
│  结论: get 完全无锁，读写分离，高并发性能好         │
└─────────────────────────────────────────────────────┘
```

## 面试追问

**Q: volatile 能保证原子性吗？**

A: volatile 不保证复合操作的原子性（如 i++），但对于**单次读/写**操作是原子的。ConcurrentHashMap 的 get 只是读取操作，不涉及复合操作，所以 volatile 足够。

**Q: get 返回的值是最新的吗？**

A: 是**该时刻的最新值**。由于没有加锁，get 和 put 并发时可能读到旧值，但这是弱一致性的设计，符合 ConcurrentHashMap 的语义。如果需要强一致性，应该使用锁或其他同步机制。
