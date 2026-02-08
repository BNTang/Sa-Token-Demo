# ConcurrentHashMap 和 Hashtable 的区别是什么？

## 核心区别

```
┌─────────────────────────────────────────────────────────────┐
│            ConcurrentHashMap vs Hashtable                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   Hashtable: 全局锁                                          │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  ┌─────────────────────────────────────────────┐    │  │
│   │  │           synchronized (全表锁)              │    │  │
│   │  │  ┌───┬───┬───┬───┬───┬───┬───┬───┐         │    │  │
│   │  │  │ 0 │ 1 │ 2 │ 3 │ 4 │ 5 │ 6 │ 7 │         │    │  │
│   │  │  └───┴───┴───┴───┴───┴───┴───┴───┘         │    │  │
│   │  └─────────────────────────────────────────────┘    │  │
│   └─────────────────────────────────────────────────────┘  │
│   问题: 任何操作都要竞争同一把锁，并发性能差                 │
│                                                             │
│   ConcurrentHashMap (JDK 8+): CAS + synchronized            │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  ┌───┐┌───┐┌───┐┌───┐┌───┐┌───┐┌───┐┌───┐         │  │
│   │  │锁1││锁2││锁3││锁4││锁5││锁6││锁7││锁8│         │  │
│   │  │ 0 ││ 1 ││ 2 ││ 3 ││ 4 ││ 5 ││ 6 ││ 7 │         │  │
│   │  └───┘└───┘└───┘└───┘└───┘└───┘└───┘└───┘         │  │
│   └─────────────────────────────────────────────────────┘  │
│   优势: 锁粒度细，只锁单个桶，并发性能高                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 详细对比

```
┌──────────────────┬─────────────────────┬─────────────────────┐
│   特性           │ ConcurrentHashMap   │ Hashtable           │
├──────────────────┼─────────────────────┼─────────────────────┤
│   锁机制         │ CAS + synchronized  │ synchronized (全局) │
│                  │ (锁单个桶)          │                     │
│   并发度         │ 高 (桶级别)         │ 低 (表级别)         │
│   null key/value │ 不允许              │ 不允许              │
│   底层结构       │ 数组+链表+红黑树    │ 数组+链表           │
│   size() 实现    │ baseCount + 分段计数│ 遍历整个表          │
│   迭代器         │ 弱一致性            │ fail-fast           │
│   扩容           │ 并发扩容            │ 单线程扩容          │
│   JDK 版本       │ 1.5 (优化于 JDK8)   │ 1.0                 │
│   推荐使用       │ ✓ 是                │ ✗ 否                │
└──────────────────┴─────────────────────┴─────────────────────┘
```

## ConcurrentHashMap 演进

```
┌─────────────────────────────────────────────────────────────┐
│                    版本演进                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   JDK 1.7: 分段锁 (Segment)                                  │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  Segment[] (默认16段)                                │  │
│   │  ┌──────────┐ ┌──────────┐ ┌──────────┐            │  │
│   │  │Segment 0 │ │Segment 1 │ │Segment 2 │ ...        │  │
│   │  │  锁 + 桶 │ │  锁 + 桶 │ │  锁 + 桶 │            │  │
│   │  └──────────┘ └──────────┘ └──────────┘            │  │
│   │  最多 16 个线程并发写                                │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   JDK 1.8+: CAS + synchronized (桶级别锁)                    │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  Node[] table                                        │  │
│   │  ┌───┬───┬───┬───┬───┬───┬───┬───┐                 │  │
│   │  │   │   │   │   │   │   │   │   │                 │  │
│   │  └───┴───┴───┴───┴───┴───┴───┴───┘                 │  │
│   │  • 空桶: CAS 插入                                    │  │
│   │  • 非空桶: synchronized 锁头节点                     │  │
│   │  • 锁粒度更细，并发度更高                            │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## JDK 8 put 源码

```java
final V putVal(K key, V value, boolean onlyIfAbsent) {
    // 不允许 null
    if (key == null || value == null) throw new NullPointerException();
    
    int hash = spread(key.hashCode());
    int binCount = 0;
    
    for (Node<K,V>[] tab = table;;) {
        Node<K,V> f; int n, i, fh;
        
        // 1. 表未初始化，初始化
        if (tab == null || (n = tab.length) == 0)
            tab = initTable();
        
        // 2. 桶为空，CAS 插入
        else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
            if (casTabAt(tab, i, null, new Node<K,V>(hash, key, value, null)))
                break;  // CAS 成功，跳出
        }
        
        // 3. 正在扩容，帮助扩容
        else if ((fh = f.hash) == MOVED)
            tab = helpTransfer(tab, f);
        
        // 4. 桶不为空，synchronized 锁住头节点
        else {
            V oldVal = null;
            synchronized (f) {  // 只锁当前桶的头节点
                if (tabAt(tab, i) == f) {
                    if (fh >= 0) {
                        // 链表操作
                        binCount = 1;
                        for (Node<K,V> e = f;; ++binCount) {
                            // ... 链表插入/更新
                        }
                    }
                    else if (f instanceof TreeBin) {
                        // 红黑树操作
                    }
                }
            }
            // 链表过长转红黑树
            if (binCount != 0) {
                if (binCount >= TREEIFY_THRESHOLD)
                    treeifyBin(tab, i);
            }
        }
    }
    addCount(1L, binCount);
    return null;
}
```

## 性能对比

```
┌─────────────────────────────────────────────────────────────┐
│                    性能对比 (100万次操作)                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   场景              ConcurrentHashMap    Hashtable          │
│   ─────             ─────────────────    ─────────          │
│   单线程写          ~200ms               ~300ms             │
│   4线程并发写       ~150ms               ~800ms             │
│   8线程并发写       ~120ms               ~1500ms            │
│   16线程并发写      ~100ms               ~3000ms            │
│                                                             │
│   结论: 并发越高，ConcurrentHashMap 优势越明显              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 迭代器差异

```java
// ConcurrentHashMap: 弱一致性迭代器
ConcurrentHashMap<String, String> chm = new ConcurrentHashMap<>();
for (String key : chm.keySet()) {
    // 迭代过程中可以修改，不抛异常
    // 但不保证看到所有修改
    chm.put("newKey", "value");  // OK
}

// Hashtable: fail-fast 迭代器
Hashtable<String, String> ht = new Hashtable<>();
for (String key : ht.keySet()) {
    ht.put("newKey", "value");  // ConcurrentModificationException!
}
```

## 面试回答

### 30秒版本

> **Hashtable** 用全局 synchronized，所有操作竞争同一把锁，并发性能差。**ConcurrentHashMap** JDK8 用 CAS + synchronized 锁单个桶，空桶 CAS 插入，非空锁头节点，并发度高很多。都不允许 null。Hashtable 已过时，多线程用 ConcurrentHashMap。

### 1分钟版本

> **锁机制差异**：
> - Hashtable：synchronized 全表锁
> - ConcurrentHashMap：CAS + synchronized 桶级锁
>
> **JDK 8 ConcurrentHashMap**：
> - 空桶：CAS 无锁插入
> - 非空桶：synchronized 锁头节点
> - 支持并发扩容
>
> **性能**：
> - 并发场景 ConcurrentHashMap 快 10 倍以上
>
> **其他区别**：
> - 迭代器：弱一致 vs fail-fast
> - 结构：红黑树优化 vs 纯链表
>
> **结论**：Hashtable 过时，用 ConcurrentHashMap

---

*关联文档：[hashmap-vs-hashtable.md](hashmap-vs-hashtable.md) | [hashmap-principle.md](hashmap-principle.md)*
