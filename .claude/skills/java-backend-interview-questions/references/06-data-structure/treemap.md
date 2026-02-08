# Java 中的 TreeMap 是什么？

> TreeMap 是基于红黑树实现的有序 Map，按照 Key 的自然顺序或自定义比较器排序

## 30秒速答

TreeMap 特点：
- **有序**: 按 Key 排序（自然顺序或 Comparator）
- **结构**: 底层是**红黑树**
- **性能**: 增删改查都是 O(log n)
- **不允许 null key**（需要比较）

## 一分钟详解

### 数据结构

```
TreeMap 红黑树结构：
          ┌───────┐
          │  50   │  (根节点-黑)
          └───┬───┘
        ┌─────┴─────┐
    ┌───┴───┐   ┌───┴───┐
    │  30   │   │  70   │  (红节点)
    └───┬───┘   └───┬───┘
  ┌─────┴─────┐     └─────┐
┌─┴─┐     ┌───┴───┐   ┌───┴───┐
│20 │     │  40   │   │  80   │ (黑节点)
└───┘     └───────┘   └───────┘

中序遍历(有序): 20 → 30 → 40 → 50 → 70 → 80
```

### 基本使用

```java
// 1. 自然顺序（Key 实现 Comparable）
TreeMap<Integer, String> map = new TreeMap<>();
map.put(30, "thirty");
map.put(10, "ten");
map.put(20, "twenty");

// 遍历顺序: 10 → 20 → 30 (升序)
for (Map.Entry<Integer, String> entry : map.entrySet()) {
    System.out.println(entry.getKey() + ": " + entry.getValue());
}

// 2. 自定义排序（降序）
TreeMap<Integer, String> descMap = new TreeMap<>(Comparator.reverseOrder());
descMap.put(30, "thirty");
descMap.put(10, "ten");
descMap.put(20, "twenty");
// 遍历顺序: 30 → 20 → 10 (降序)

// 3. 自定义对象排序
TreeMap<Person, String> personMap = new TreeMap<>(
    Comparator.comparing(Person::getAge)
              .thenComparing(Person::getName)
);
```

### 特有方法（导航功能）

```java
TreeMap<Integer, String> map = new TreeMap<>();
map.put(10, "A");
map.put(20, "B");
map.put(30, "C");
map.put(40, "D");
map.put(50, "E");

// 获取边界
map.firstKey();           // 10 (最小)
map.lastKey();            // 50 (最大)
map.firstEntry();         // 10=A
map.lastEntry();          // 50=E

// 查找临近值
map.lowerKey(30);         // 20 (小于30的最大key)
map.floorKey(30);         // 30 (小于等于30的最大key)
map.higherKey(30);        // 40 (大于30的最小key)
map.ceilingKey(30);       // 30 (大于等于30的最小key)

// 子Map视图
map.headMap(30);          // {10=A, 20=B} (key < 30)
map.tailMap(30);          // {30=C, 40=D, 50=E} (key >= 30)
map.subMap(20, 40);       // {20=B, 30=C} (20 <= key < 40)

// 逆序视图
map.descendingMap();      // {50=E, 40=D, 30=C, 20=B, 10=A}

// 弹出操作
map.pollFirstEntry();     // 弹出并返回最小 10=A
map.pollLastEntry();      // 弹出并返回最大 50=E
```

### TreeMap vs HashMap vs LinkedHashMap

| 特性 | HashMap | LinkedHashMap | TreeMap |
|------|---------|---------------|---------|
| 底层结构 | 数组+链表+红黑树 | HashMap+双向链表 | 红黑树 |
| 顺序 | 无序 | 插入/访问顺序 | Key排序 |
| 时间复杂度 | O(1) | O(1) | O(log n) |
| null key | 允许1个 | 允许1个 | 不允许 |
| null value | 允许 | 允许 | 允许 |
| 线程安全 | 否 | 否 | 否 |

### 实现原理

```java
// TreeMap 核心属性
public class TreeMap<K,V> extends AbstractMap<K,V>
    implements NavigableMap<K,V>, Cloneable, java.io.Serializable {
    
    private final Comparator<? super K> comparator;  // 比较器
    private transient Entry<K,V> root;               // 红黑树根节点
    private transient int size = 0;                  // 元素数量
    
    // 红黑树节点
    static final class Entry<K,V> implements Map.Entry<K,V> {
        K key;
        V value;
        Entry<K,V> left;    // 左子节点
        Entry<K,V> right;   // 右子节点
        Entry<K,V> parent;  // 父节点
        boolean color = BLACK;  // 颜色
    }
}
```

## 关键记忆点

```
┌────────────────────────────────────────────────┐
│  TreeMap 特性速记：                             │
│                                                │
│  「红」底层红黑树，自平衡                        │
│  「序」按 Key 排序（Comparable 或 Comparator）  │
│  「log」增删改查 O(log n)                       │
│  「导」NavigableMap 导航功能                    │
│  「禁」不允许 null key                          │
└────────────────────────────────────────────────┘
```

## 使用场景

```java
// 1. 需要按 Key 排序的场景
TreeMap<String, Integer> scoreMap = new TreeMap<>();
scoreMap.put("Alice", 90);
scoreMap.put("Bob", 85);
scoreMap.put("Charlie", 95);
// 按名字字母顺序排列

// 2. 范围查询
TreeMap<Long, String> timeEvents = new TreeMap<>();
// 查询某时间段的事件
SortedMap<Long, String> range = timeEvents.subMap(startTime, endTime);

// 3. 排行榜（按分数排序）
TreeMap<Integer, List<String>> ranking = new TreeMap<>(Comparator.reverseOrder());
ranking.computeIfAbsent(90, k -> new ArrayList<>()).add("Alice");
// 分数降序排列

// 4. 区间调度
TreeMap<Integer, Integer> intervals = new TreeMap<>();
Integer floor = intervals.floorKey(point);  // 找到包含point的区间
```

## 面试追问

**Q: TreeMap 和 TreeSet 的关系？**

A: TreeSet 底层就是 TreeMap，元素作为 Key，固定 PRESENT 作为 Value（类似 HashSet 和 HashMap 的关系）
