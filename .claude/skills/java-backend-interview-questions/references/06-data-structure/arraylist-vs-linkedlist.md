# Java 中 ArrayList 和 LinkedList 有什么区别？

## 结构对比

```
┌─────────────────────────────────────────────────────────────┐
│                ArrayList vs LinkedList                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ArrayList (动态数组):                                      │
│   ┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐        │
│   │  0  │  1  │  2  │  3  │  4  │  5  │  6  │ ... │        │
│   │ 元素 │ 元素 │ 元素 │ 元素 │ 元素 │ null │ null │ ... │        │
│   └─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┘        │
│   size = 5              capacity = 10                       │
│                                                             │
│   LinkedList (双向链表):                                     │
│       first                                    last         │
│         ↓                                        ↓          │
│   ┌──────────┐    ┌──────────┐    ┌──────────┐             │
│   │prev│A│next│←→│prev│B│next│←→│prev│C│next│             │
│   │null│ │  ──│─→│  ─│ │  ──│─→│  ─│ │null │             │
│   └──────────┘    └──────────┘    └──────────┘             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 详细对比

```
┌──────────────────┬─────────────────────┬─────────────────────┐
│   特性           │   ArrayList         │   LinkedList        │
├──────────────────┼─────────────────────┼─────────────────────┤
│   底层结构       │   动态数组          │   双向链表          │
│   随机访问       │   O(1) ✓            │   O(n)              │
│   头部插入       │   O(n)              │   O(1) ✓            │
│   尾部插入       │   O(1) 平均         │   O(1) ✓            │
│   中间插入       │   O(n)              │   O(n)              │
│   内存占用       │   紧凑              │   每节点额外指针    │
│   缓存友好       │   是                │   否                │
│   实现接口       │   List, RandomAccess │  List, Deque        │
│   初始容量       │   10                │   无                │
│   扩容           │   1.5 倍            │   无需扩容          │
└──────────────────┴─────────────────────┴─────────────────────┘
```

## 时间复杂度

```
┌─────────────────────────────────────────────────────────────┐
│                    时间复杂度对比                            │
├───────────────────┬─────────────────┬───────────────────────┤
│   操作            │   ArrayList     │   LinkedList          │
├───────────────────┼─────────────────┼───────────────────────┤
│   get(index)      │   O(1) ✓        │   O(n)                │
│   add(E)          │   O(1)*         │   O(1)                │
│   add(0, E)       │   O(n)          │   O(1) ✓              │
│   add(index, E)   │   O(n)          │   O(n)                │
│   remove(index)   │   O(n)          │   O(n)                │
│   remove(0)       │   O(n)          │   O(1) ✓              │
│   contains        │   O(n)          │   O(n)                │
│   iterator.next   │   O(1)          │   O(1)                │
├───────────────────┴─────────────────┴───────────────────────┤
│   * ArrayList 扩容时为 O(n)                                  │
└─────────────────────────────────────────────────────────────┘
```

## 代码示例

```java
// ArrayList
List<String> arrayList = new ArrayList<>();

// 随机访问 - ArrayList 优势
String element = arrayList.get(1000);  // O(1)

// 尾部添加
arrayList.add("element");  // O(1) 平均

// 头部添加 - 需要移动所有元素
arrayList.add(0, "first");  // O(n)

// LinkedList
LinkedList<String> linkedList = new LinkedList<>();

// 随机访问 - 需要遍历
String element = linkedList.get(1000);  // O(n)

// 头尾操作 - LinkedList 优势
linkedList.addFirst("first");  // O(1)
linkedList.addLast("last");    // O(1)
linkedList.removeFirst();      // O(1)
linkedList.removeLast();       // O(1)

// 队列操作 (Deque)
linkedList.offer("element");   // 入队
linkedList.poll();             // 出队
linkedList.peek();             // 查看队首

// 栈操作
linkedList.push("element");    // 入栈
linkedList.pop();              // 出栈
```

## 内存分析

```
┌─────────────────────────────────────────────────────────────┐
│                    内存对比 (存储 1000 个 Integer)           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ArrayList:                                                 │
│   • 数组: 1000 × 4 bytes (引用) = 4 KB                      │
│   • Integer 对象: 1000 × 16 bytes = 16 KB                   │
│   • 对象头等: ~100 bytes                                     │
│   • 总计: ~20 KB                                             │
│                                                             │
│   LinkedList:                                                │
│   • Node: 1000 × (16对象头 + 8prev + 8next + 8item) = 40 KB │
│   • Integer 对象: 1000 × 16 bytes = 16 KB                   │
│   • LinkedList 头: ~40 bytes                                 │
│   • 总计: ~56 KB                                             │
│                                                             │
│   结论: LinkedList 内存约为 ArrayList 的 2.5 倍             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 使用场景

```
┌─────────────────────────────────────────────────────────────┐
│                    使用场景选择                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   选择 ArrayList:                                            │
│   • 频繁随机访问 (按索引)                                    │
│   • 主要在尾部添加                                          │
│   • 遍历操作多                                              │
│   • 内存敏感                                                │
│   • 大多数场景首选                                          │
│                                                             │
│   选择 LinkedList:                                           │
│   • 频繁头部添加删除                                        │
│   • 需要队列功能 (FIFO)                                     │
│   • 需要双端队列 (Deque)                                    │
│   • 很少随机访问                                            │
│                                                             │
│   ⚠️ 实际生产: 90%+ 场景用 ArrayList                         │
│      LinkedList 只在明确需要队列时使用                       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 常见误区

```java
// ❌ 误区1: LinkedList 中间插入是 O(1)
// 实际上定位需要 O(n)，插入才是 O(1)
linkedList.add(500, "element");  // 实际是 O(n)

// ❌ 误区2: LinkedList 一定比 ArrayList 快
// 由于缓存不友好，很多场景 ArrayList 更快

// ❌ 误区3: 频繁插入用 LinkedList
// 如果是尾部插入，ArrayList 同样是 O(1)

// ✓ 正确使用: 明确需要头部操作
Deque<String> queue = new LinkedList<>();  // 队列场景
queue.offer("a");
queue.poll();
```

## 面试回答

### 30秒版本

> **ArrayList**：动态数组，随机访问 O(1)，尾部添加 O(1)，头部添加 O(n)，内存紧凑缓存友好。**LinkedList**：双向链表，随机访问 O(n)，头尾操作 O(1)，实现了 Deque 接口可当队列用。大多数场景选 ArrayList，只有明确需要队列或频繁头部操作时用 LinkedList。

### 1分钟版本

> **底层结构**：
> - ArrayList：动态数组
> - LinkedList：双向链表
>
> **性能差异**：
> - 随机访问：ArrayList O(1)，LinkedList O(n)
> - 头部操作：ArrayList O(n)，LinkedList O(1)
> - 尾部添加：都是 O(1)
>
> **其他区别**：
> - LinkedList 实现 Deque，可当队列
> - ArrayList 内存更紧凑
> - ArrayList 缓存友好
>
> **选择**：
> - 默认 ArrayList
> - 队列场景用 LinkedList

---

*关联文档：[list-implementations.md](list-implementations.md) | [arraylist-resize.md](arraylist-resize.md)*
