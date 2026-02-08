# 说说 Java 中 HashMap 的原理？

## HashMap 结构

```
┌─────────────────────────────────────────────────────────────┐
│                    HashMap 底层结构                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   JDK 1.8+: 数组 + 链表 + 红黑树                            │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  table[] 数组                                        │  │
│   ├─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────────┤  │
│   │  0  │  1  │  2  │  3  │  4  │  5  │  6  │   ...   │  │
│   ├─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────────┤  │
│   │ null│Node │ null│Node │Node │ null│ null│   ...   │  │
│   └─────┴──┬──┴─────┴──┬──┴──┬──┴─────┴─────┴─────────┘  │
│            │           │     │                            │
│            ↓           ↓     ↓                            │
│         ┌────┐      ┌────┐ ┌────┐                        │
│         │Node│      │Node│ │Node│  链表 (< 8)            │
│         └──┬─┘      └──┬─┘ └──┬─┘                        │
│            │           │     │                            │
│            ↓           ↓     ↓                            │
│         ┌────┐      ┌────┐ ┌────────────────────┐        │
│         │Node│      │Node│ │   Red-Black Tree   │ ≥8     │
│         └────┘      └────┘ │   红黑树           │        │
│                            └────────────────────┘        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 核心参数

```java
// 默认初始容量 16
static final int DEFAULT_INITIAL_CAPACITY = 1 << 4;

// 最大容量 2^30
static final int MAXIMUM_CAPACITY = 1 << 30;

// 默认负载因子 0.75
static final float DEFAULT_LOAD_FACTOR = 0.75f;

// 链表转红黑树阈值
static final int TREEIFY_THRESHOLD = 8;

// 红黑树退化为链表阈值
static final int UNTREEIFY_THRESHOLD = 6;

// 树化时数组最小长度 (否则优先扩容)
static final int MIN_TREEIFY_CAPACITY = 64;
```

## put 流程

```
┌─────────────────────────────────────────────────────────────┐
│                    put(key, value) 流程                      │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 计算 hash                                              │
│      hash = (h = key.hashCode()) ^ (h >>> 16)              │
│      (高16位参与运算，减少碰撞)                             │
│                                                             │
│   2. 定位桶位置                                             │
│      index = (n - 1) & hash  (n是数组长度)                 │
│                                                             │
│   3. 判断桶是否为空                                         │
│      ├── 空: 直接插入新 Node                               │
│      └── 非空: 处理冲突                                    │
│                                                             │
│   4. 处理冲突                                               │
│      ├── key 相等: 覆盖旧值                                │
│      ├── 是树节点: 红黑树插入                              │
│      └── 是链表: 尾插法插入                                │
│          └── 链表长度 ≥ 8 且 数组长度 ≥ 64: 转红黑树       │
│                                                             │
│   5. 检查扩容                                               │
│      if (++size > threshold) resize();                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 源码解析

```java
public V put(K key, V value) {
    return putVal(hash(key), key, value, false, true);
}

// 扰动函数：让 hash 更均匀
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}

final V putVal(int hash, K key, V value, boolean onlyIfAbsent, boolean evict) {
    Node<K,V>[] tab; Node<K,V> p; int n, i;
    
    // 1. table 为空则初始化
    if ((tab = table) == null || (n = tab.length) == 0)
        n = (tab = resize()).length;
    
    // 2. 计算索引，桶为空直接插入
    if ((p = tab[i = (n - 1) & hash]) == null)
        tab[i] = newNode(hash, key, value, null);
    else {
        Node<K,V> e; K k;
        
        // 3. key 相同，覆盖
        if (p.hash == hash && 
            ((k = p.key) == key || (key != null && key.equals(k))))
            e = p;
        
        // 4. 红黑树
        else if (p instanceof TreeNode)
            e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
        
        // 5. 链表
        else {
            for (int binCount = 0; ; ++binCount) {
                if ((e = p.next) == null) {
                    p.next = newNode(hash, key, value, null);
                    // 链表长度 ≥ 8，转红黑树
                    if (binCount >= TREEIFY_THRESHOLD - 1)
                        treeifyBin(tab, hash);
                    break;
                }
                if (e.hash == hash && 
                    ((k = e.key) == key || (key != null && key.equals(k))))
                    break;
                p = e;
            }
        }
        
        // 覆盖旧值
        if (e != null) {
            V oldValue = e.value;
            if (!onlyIfAbsent || oldValue == null)
                e.value = value;
            return oldValue;
        }
    }
    
    ++modCount;
    // 6. 检查扩容
    if (++size > threshold)
        resize();
    return null;
}
```

## get 流程

```java
public V get(Object key) {
    Node<K,V> e;
    return (e = getNode(hash(key), key)) == null ? null : e.value;
}

final Node<K,V> getNode(int hash, Object key) {
    Node<K,V>[] tab; Node<K,V> first, e; int n; K k;
    
    if ((tab = table) != null && (n = tab.length) > 0 &&
        (first = tab[(n - 1) & hash]) != null) {
        
        // 检查首节点
        if (first.hash == hash && 
            ((k = first.key) == key || (key != null && key.equals(k))))
            return first;
        
        if ((e = first.next) != null) {
            // 红黑树查找
            if (first instanceof TreeNode)
                return ((TreeNode<K,V>)first).getTreeNode(hash, key);
            // 链表遍历
            do {
                if (e.hash == hash && 
                    ((k = e.key) == key || (key != null && key.equals(k))))
                    return e;
            } while ((e = e.next) != null);
        }
    }
    return null;
}
```

## 时间复杂度

```
┌─────────────────────────────────────────────────────────────┐
│                    时间复杂度                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   理想情况 (无冲突):      O(1)                               │
│   链表冲突:              O(n)                               │
│   红黑树冲突:            O(log n)                           │
│                                                             │
│   平均情况:              O(1) (负载因子控制)                 │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> HashMap 底层是**数组+链表+红黑树**（JDK8+）。put 时先计算 hash（扰动函数），用 `(n-1) & hash` 定位桶。冲突时链表**尾插法**，链表 ≥8 且数组 ≥64 转红黑树。扩容时容量翻倍，重新 hash 分布。时间复杂度 O(1)，负载因子 0.75 平衡空间和时间。

### 1分钟版本

> **结构**：数组 + 链表 + 红黑树
>
> **put 流程**：
> 1. hash(key) 扰动函数
> 2. (n-1) & hash 定位桶
> 3. 空桶直接插入，否则处理冲突
> 4. 链表尾插法，≥8 转红黑树
> 5. size > threshold 扩容
>
> **核心参数**：
> - 初始容量 16
> - 负载因子 0.75
> - 树化阈值 8
>
> **时间复杂度**：
> - 平均 O(1)
> - 链表 O(n)，红黑树 O(log n)

---

*关联文档：[hashmap-resize.md](../06-data-structure/hashmap-resize.md) | [hash-collision.md](hash-collision.md)*
