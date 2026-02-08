# Java 中的 LinkedHashMap 是什么？

> LinkedHashMap 是 HashMap 的子类，通过双向链表维护元素的插入顺序或访问顺序

## 30秒速答

LinkedHashMap = **HashMap + 双向链表**：
- **有序性**: 维护插入顺序（默认）或访问顺序
- **结构**: 在 HashMap 基础上增加 before/after 指针
- **性能**: 与 HashMap 相当，略有开销
- **用途**: 实现 LRU 缓存、有序遍历

## 一分钟详解

### 数据结构

```
LinkedHashMap 结构：
┌──────────────────────────────────────────────────┐
│  HashMap 数组                                     │
│  ┌───┬───┬───┬───┬───┬───┬───┬───┐              │
│  │ 0 │ 1 │ 2 │ 3 │ 4 │ 5 │ 6 │ 7 │              │
│  └─┬─┴───┴─┬─┴───┴───┴─┬─┴───┴───┘              │
│    ↓       ↓           ↓                         │
│   [A]     [C]         [E]                        │
│    ↓       ↓                                     │
│   [B]     [D]                                    │
├──────────────────────────────────────────────────┤
│  双向链表 (维护顺序)                              │
│                                                  │
│  head ←→ A ←→ B ←→ C ←→ D ←→ E ←→ tail         │
│  (插入顺序: A, B, C, D, E)                       │
└──────────────────────────────────────────────────┘
```

### 核心源码

```java
// LinkedHashMap.Entry 继承 HashMap.Node
static class Entry<K,V> extends HashMap.Node<K,V> {
    Entry<K,V> before, after;  // 双向链表指针
    
    Entry(int hash, K key, V value, Node<K,V> next) {
        super(hash, key, value, next);
    }
}

// 链表头尾
transient LinkedHashMap.Entry<K,V> head;
transient LinkedHashMap.Entry<K,V> tail;

// 访问顺序标志
final boolean accessOrder;  // true=访问顺序, false=插入顺序(默认)
```

### 两种顺序模式

```java
// 1. 插入顺序（默认）
Map<String, Integer> insertOrder = new LinkedHashMap<>();
insertOrder.put("A", 1);
insertOrder.put("B", 2);
insertOrder.put("C", 3);
insertOrder.get("A");  // 访问不影响顺序
// 遍历顺序: A → B → C

// 2. 访问顺序（LRU 基础）
Map<String, Integer> accessOrder = new LinkedHashMap<>(16, 0.75f, true);
accessOrder.put("A", 1);
accessOrder.put("B", 2);
accessOrder.put("C", 3);
accessOrder.get("A");  // A 移到末尾
// 遍历顺序: B → C → A
```

### 实现 LRU 缓存

```java
public class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;
    
    public LRUCache(int capacity) {
        super(capacity, 0.75f, true);  // accessOrder = true
        this.capacity = capacity;
    }
    
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;  // 超出容量删除最老元素
    }
}

// 使用示例
LRUCache<String, Integer> cache = new LRUCache<>(3);
cache.put("A", 1);
cache.put("B", 2);
cache.put("C", 3);
cache.get("A");      // A 移到末尾
cache.put("D", 4);   // 容量超限，删除最老的 B

System.out.println(cache.keySet());  // [C, A, D]
```

### LinkedHashMap vs HashMap

| 特性 | HashMap | LinkedHashMap |
|------|---------|---------------|
| 遍历顺序 | 无序 | 有序（插入/访问） |
| 内存占用 | 较少 | 较多（额外指针） |
| 插入性能 | O(1) | O(1)（略慢） |
| 查询性能 | O(1) | O(1) |
| 实现 LRU | 不支持 | 天然支持 |

### 遍历顺序对比

```java
// HashMap - 无序
Map<String, Integer> hashMap = new HashMap<>();
hashMap.put("C", 3);
hashMap.put("A", 1);
hashMap.put("B", 2);
// 遍历顺序不确定，可能是 A, B, C 或其他

// LinkedHashMap - 插入顺序
Map<String, Integer> linkedHashMap = new LinkedHashMap<>();
linkedHashMap.put("C", 3);
linkedHashMap.put("A", 1);
linkedHashMap.put("B", 2);
// 遍历顺序固定: C → A → B
```

## 关键记忆点

```
┌─────────────────────────────────────────────────┐
│  LinkedHashMap = HashMap + 双向链表              │
│                                                 │
│  ┌──────────────┬───────────────────────────┐  │
│  │ 插入顺序     │ new LinkedHashMap<>()     │  │
│  │ (默认)       │ accessOrder = false       │  │
│  ├──────────────┼───────────────────────────┤  │
│  │ 访问顺序     │ new LinkedHashMap<>       │  │
│  │ (LRU缓存)    │ (cap, 0.75f, true)        │  │
│  └──────────────┴───────────────────────────┘  │
│                                                 │
│  LRU 实现: 继承 + 重写 removeEldestEntry()      │
└─────────────────────────────────────────────────┘
```

## 使用场景

1. **需要有序遍历**: 按插入顺序处理数据
2. **LRU 缓存**: 利用访问顺序 + removeEldestEntry
3. **去重保序**: 既要去重又要保持顺序
4. **JSON 序列化**: 保持字段顺序
