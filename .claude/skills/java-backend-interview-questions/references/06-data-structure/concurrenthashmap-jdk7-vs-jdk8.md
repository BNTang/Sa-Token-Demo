# ConcurrentHashMap JDK 1.7 和 1.8 的区别

> JDK 1.8 对 ConcurrentHashMap 进行了重大重构：分段锁变为桶级锁，数据结构引入红黑树

## 30秒速答

| 特性 | JDK 1.7 | JDK 1.8 |
|------|---------|---------|
| 数据结构 | Segment[] + HashEntry[] | Node[] + 链表/红黑树 |
| 锁机制 | 分段锁 ReentrantLock | CAS + synchronized 锁桶 |
| 锁粒度 | 锁一个 Segment | 锁一个桶(头节点) |
| 并发度 | 固定(默认16) | 桶数量(动态) |
| 查询复杂度 | O(n) | O(log n)(红黑树) |

## 一分钟详解

### JDK 1.7 结构：分段锁

```
ConcurrentHashMap (JDK 1.7)
┌──────────────────────────────────────────────────────┐
│  Segment[] (默认16个，每个Segment是一把锁)            │
│  ┌─────────┬─────────┬─────────┬─────────┐          │
│  │Segment0 │Segment1 │Segment2 │   ...   │          │
│  │(锁)     │(锁)     │(锁)     │         │          │
│  └────┬────┴────┬────┴────┬────┴─────────┘          │
│       ↓         ↓         ↓                          │
│  ┌─────────┐┌─────────┐┌─────────┐                   │
│  │HashEntry│HashEntry│HashEntry│  每个Segment内部    │
│  │数组     ││数组     ││数组     │  是小HashMap       │
│  └─────────┘└─────────┘└─────────┘                   │
└──────────────────────────────────────────────────────┘

put操作：先定位Segment，获取该Segment锁，再操作
最大并发度 = Segment数量 = 16（固定）
```

### JDK 1.8 结构：桶级锁

```
ConcurrentHashMap (JDK 1.8)
┌──────────────────────────────────────────────────────┐
│  Node[] 数组（每个桶独立加锁）                        │
│  ┌───┬───┬───┬───┬───┬───┬───┬───┬───┬───┐          │
│  │ 0 │ 1 │ 2 │ 3 │ 4 │ 5 │ 6 │ 7 │...│ n │          │
│  └─┬─┴─┬─┴───┴───┴─┬─┴───┴─┬─┴───┴───┴───┘          │
│    ↓   ↓           ↓       ↓                         │
│   [A] [C]        [红黑树]  [E]  锁头节点(synchronized) │
│    ↓   ↓                    ↓                        │
│   [B] [D]                  [F]                       │
│                                                      │
│  空桶：CAS 插入                                       │
│  非空桶：synchronized 锁头节点                        │
└──────────────────────────────────────────────────────┘

最大并发度 = 桶数量（动态扩展）
```

### 锁机制对比

```java
// JDK 1.7: Segment 继承 ReentrantLock
static class Segment<K,V> extends ReentrantLock {
    transient volatile HashEntry<K,V>[] table;
    
    final V put(K key, int hash, V value, boolean onlyIfAbsent) {
        lock();  // 获取分段锁
        try {
            // 操作 HashEntry
        } finally {
            unlock();
        }
    }
}

// JDK 1.8: CAS + synchronized
final V putVal(K key, V value, boolean onlyIfAbsent) {
    for (Node<K,V>[] tab = table;;) {
        Node<K,V> f; int n, i, fh;
        if (tab == null || (n = tab.length) == 0)
            tab = initTable();
        else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
            // 空桶：CAS 插入
            if (casTabAt(tab, i, null, new Node<K,V>(hash, key, value, null)))
                break;
        } else {
            // 非空桶：synchronized 锁头节点
            synchronized (f) {
                if (tabAt(tab, i) == f) {
                    // 链表或红黑树操作
                }
            }
        }
    }
}
```

### 详细对比表

| 对比维度 | JDK 1.7 | JDK 1.8 |
|---------|---------|---------|
| 数据结构 | Segment[] + HashEntry[] | Node[] + 链表/红黑树 |
| 锁类型 | ReentrantLock | CAS + synchronized |
| 锁粒度 | Segment(多个桶) | 单个桶(头节点) |
| 默认并发度 | 16 | 桶数量(16起步,动态扩展) |
| 初始化 | 初始化Segment[] | 懒加载(首次put) |
| 链表长度过长 | 保持链表 | 转红黑树(≥8) |
| 查询复杂度 | O(n) | O(log n) |
| 扩容 | Segment内部扩容 | 整体扩容 |
| size() 计算 | 遍历Segment累加 | CounterCell + baseCount |

### 关键改进点

```java
// 1. 锁粒度细化
// JDK 1.7: 16个Segment，最多16个线程并发写
// JDK 1.8: n个桶，n个线程可并发写不同桶

// 2. 红黑树优化查询
// JDK 1.7: 链表查找 O(n)
// JDK 1.8: 红黑树查找 O(log n)

// 3. size() 计算优化
// JDK 1.7: 遍历 Segment 累加，可能需要加锁重试
// JDK 1.8: 使用 LongAdder 思想
private transient volatile long baseCount;
private transient volatile CounterCell[] counterCells;

public int size() {
    long n = sumCount();  // baseCount + 所有CounterCell
    return ((n < 0L) ? 0 : (n > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int)n);
}
```

### 扩容对比

```java
// JDK 1.7: 每个 Segment 独立扩容
// 只扩容当前 Segment 内的 HashEntry[]

// JDK 1.8: 整个 table 扩容，支持并发扩容
// 多线程协助迁移数据
final Node<K,V>[] helpTransfer(Node<K,V>[] tab, Node<K,V> f) {
    // 其他线程可以帮助迁移
}
```

## 关键记忆点

```
┌────────────────────────────────────────────────────┐
│  JDK 1.7 vs 1.8 速记：                             │
│                                                    │
│  ┌──────────┬────────────────┬────────────────┐   │
│  │ 维度     │   JDK 1.7      │   JDK 1.8      │   │
│  ├──────────┼────────────────┼────────────────┤   │
│  │ 结构     │ Segment + 链表 │ Node + 链表/树 │   │
│  │ 锁       │ 分段锁(16个)   │ 桶锁(CAS+syn)  │   │
│  │ 粒度     │ 多桶共享锁     │ 一桶一锁       │   │
│  │ 树化     │ 无             │ 链表≥8转红黑树 │   │
│  │ 扩容     │ Segment内扩容  │ 全局并发扩容   │   │
│  └──────────┴────────────────┴────────────────┘   │
│                                                    │
│  1.8 优势：锁更细 + 红黑树 + 并发扩容              │
└────────────────────────────────────────────────────┘
```

## 面试追问

**Q: 为什么 JDK 1.8 放弃了分段锁？**

A: 
1. 分段锁并发度固定（16），无法动态调整
2. 分段锁实现复杂，内存开销大
3. synchronized 经过 JDK 优化，性能已接近 ReentrantLock
4. 配合红黑树，桶级锁更灵活高效
