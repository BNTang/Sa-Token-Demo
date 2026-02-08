# 数组与链表

> Java 后端面试知识点 - 数据结构

---

## 特性对比

| 特性 | 数组（Array） | 链表（LinkedList） |
|------|--------------|-------------------|
| **内存分配** | 连续内存 | 分散内存 |
| **大小** | 固定长度 | 动态扩展 |
| **随机访问** | O(1) | O(n) |
| **头部插入** | O(n) | O(1) |
| **尾部插入** | O(1) 摊销 | O(1) |
| **中间插入** | O(n) | O(1)（已定位） |
| **删除** | O(n) | O(1)（已定位） |
| **内存开销** | 低 | 高（额外指针） |
| **缓存友好** | 是 | 否 |

---

## 内存结构

### 数组

```
连续内存空间：

地址:  1000   1004   1008   1012   1016   1020
      ┌──────┬──────┬──────┬──────┬──────┬──────┐
      │  10  │  20  │  30  │  40  │  50  │  60  │
      └──────┴──────┴──────┴──────┴──────┴──────┘
索引:    0      1      2      3      4      5

访问 arr[3]：
地址 = 基地址 + 索引 × 元素大小
     = 1000 + 3 × 4 = 1012
时间复杂度：O(1)
```

### 链表

```
分散内存空间：

地址 1000         地址 2048         地址 1520         地址 3000
┌────┬────┐      ┌────┬────┐      ┌────┬────┐      ┌────┬────┐
│ 10 │2048│ ──>  │ 20 │1520│ ──>  │ 30 │3000│ ──>  │ 40 │null│
└────┴────┘      └────┴────┘      └────┴────┘      └────┴────┘
 data  next       data  next       data  next       data  next

访问第 3 个元素：
必须从头遍历：head → node1 → node2 → node3
时间复杂度：O(n)
```

---

## Java 中的实现

### 数组

```java
// 原生数组
int[] arr = new int[10];
String[] names = new String[100];

// ArrayList（动态数组）
List<Integer> list = new ArrayList<>();

// 底层结构
public class ArrayList<E> {
    transient Object[] elementData;  // 数组存储
    private int size;
    
    public E get(int index) {
        return (E) elementData[index];  // O(1)
    }
    
    public void add(int index, E element) {
        // O(n) - 需要移动元素
        System.arraycopy(elementData, index, 
                         elementData, index + 1, 
                         size - index);
        elementData[index] = element;
        size++;
    }
}
```

### 链表

```java
// LinkedList（双向链表）
List<Integer> list = new LinkedList<>();

// 底层结构
public class LinkedList<E> {
    transient Node<E> first;
    transient Node<E> last;
    transient int size;
    
    private static class Node<E> {
        E item;
        Node<E> next;
        Node<E> prev;
    }
    
    public E get(int index) {
        // O(n) - 需要遍历
        Node<E> node = first;
        for (int i = 0; i < index; i++) {
            node = node.next;
        }
        return node.item;
    }
    
    public void addFirst(E e) {
        // O(1)
        Node<E> newNode = new Node<>(null, e, first);
        if (first != null) first.prev = newNode;
        first = newNode;
        size++;
    }
}
```

---

## 操作复杂度详解

### 随机访问

```java
// 数组：O(1)
int value = arr[1000];  // 直接计算地址

// 链表：O(n)
int value = linkedList.get(1000);  // 遍历 1000 次
```

### 插入操作

```java
// 数组头部插入：O(n) - 需要移动所有元素
list.add(0, newElement);
// [1,2,3,4,5] → [_,1,2,3,4,5] → [0,1,2,3,4,5]
//                 ← 移动

// 链表头部插入：O(1) - 只需改变指针
linkedList.addFirst(newElement);
// head → [1] → [2] → ...
// newNode → head → [1] → [2] → ...
```

### 删除操作

```java
// 数组删除：O(n) - 需要移动元素
list.remove(0);
// [1,2,3,4,5] → [2,3,4,5,_]
//                 → 移动

// 链表删除：O(1)（已定位）
// prev → current → next
// prev ─────────── → next  (current 被移除)
```

---

## 使用场景

### 适合使用数组/ArrayList

```java
// 1. 频繁随机访问
List<User> users = new ArrayList<>();
User user = users.get(randomIndex);  // O(1)

// 2. 尾部插入为主
List<Log> logs = new ArrayList<>();
logs.add(newLog);  // O(1) 摊销

// 3. 需要遍历
for (User user : users) {
    // 数组遍历更快（缓存友好）
}

// 4. 内存敏感
// 数组开销更小
```

### 适合使用链表/LinkedList

```java
// 1. 频繁头部/中间插入删除
LinkedList<Task> queue = new LinkedList<>();
queue.addFirst(urgentTask);    // O(1)
queue.removeFirst();           // O(1)

// 2. 作为栈/队列
Deque<Integer> stack = new LinkedList<>();
stack.push(1);
stack.pop();

// 3. 不知道最终大小
LinkedList<Event> events = new LinkedList<>();
// 无需预分配，动态增长
```

---

## 性能实测

```java
public class PerformanceTest {
    
    public static void main(String[] args) {
        int n = 100000;
        
        // 测试随机访问
        testRandomAccess(n);
        
        // 测试头部插入
        testHeadInsert(n);
        
        // 测试尾部插入
        testTailInsert(n);
    }
    
    static void testRandomAccess(int n) {
        List<Integer> arrayList = new ArrayList<>();
        List<Integer> linkedList = new LinkedList<>();
        
        for (int i = 0; i < n; i++) {
            arrayList.add(i);
            linkedList.add(i);
        }
        
        // ArrayList: ~1ms
        long start = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            arrayList.get(n / 2);
        }
        System.out.println("ArrayList get: " + (System.nanoTime() - start) / 1e6 + "ms");
        
        // LinkedList: ~500ms
        start = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            linkedList.get(n / 2);
        }
        System.out.println("LinkedList get: " + (System.nanoTime() - start) / 1e6 + "ms");
    }
    
    static void testHeadInsert(int n) {
        List<Integer> arrayList = new ArrayList<>();
        List<Integer> linkedList = new LinkedList<>();
        
        // ArrayList: 很慢
        long start = System.nanoTime();
        for (int i = 0; i < n; i++) {
            arrayList.add(0, i);  // O(n) 每次
        }
        System.out.println("ArrayList addFirst: " + (System.nanoTime() - start) / 1e6 + "ms");
        
        // LinkedList: 很快
        start = System.nanoTime();
        for (int i = 0; i < n; i++) {
            linkedList.add(0, i);  // O(1)
        }
        System.out.println("LinkedList addFirst: " + (System.nanoTime() - start) / 1e6 + "ms");
    }
}
```

---

## 面试要点

### 核心答案

**问：数组和链表在 Java 中的区别是什么？**

答：

| 方面 | 数组（ArrayList） | 链表（LinkedList） |
|------|------------------|-------------------|
| **内存** | 连续分配 | 分散分配 |
| **随机访问** | O(1)，直接定位 | O(n)，需要遍历 |
| **头部插入** | O(n)，需要移动元素 | O(1)，改变指针 |
| **尾部插入** | O(1) 摊销 | O(1) |
| **内存开销** | 低 | 高（存储指针） |
| **缓存命中** | 高（连续内存） | 低（分散内存） |

**选择建议**：
- 随机访问多、遍历多 → ArrayList
- 频繁增删（尤其头部） → LinkedList
- 大多数场景 → ArrayList（更常用）

---

## 编码最佳实践

### ✅ 推荐做法

```java
// 1. 默认使用 ArrayList
List<User> users = new ArrayList<>();

// 2. 预估容量
List<Order> orders = new ArrayList<>(1000);

// 3. 需要队列/栈功能时用 LinkedList
Deque<Task> taskQueue = new LinkedList<>();
taskQueue.addLast(task);
taskQueue.removeFirst();

// 4. 不可变集合
List<String> constants = List.of("A", "B", "C");
```

### ❌ 避免做法

```java
// ❌ LinkedList 频繁随机访问
LinkedList<User> users = new LinkedList<>();
for (int i = 0; i < users.size(); i++) {
    User user = users.get(i);  // O(n²) 总时间
}

// ❌ ArrayList 频繁头部插入
ArrayList<Event> events = new ArrayList<>();
for (Event e : newEvents) {
    events.add(0, e);  // O(n) 每次
}

// ❌ 不指定初始容量
List<Data> hugeList = new ArrayList<>();  // 频繁扩容
for (int i = 0; i < 1000000; i++) {
    hugeList.add(new Data());
}
```
