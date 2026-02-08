# Java 中的 List 接口有哪些实现类？

## List 实现类概览

```
┌─────────────────────────────────────────────────────────────┐
│                    List 接口实现类                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│                         List<E>                             │
│                            │                                │
│           ┌────────────────┼────────────────┐              │
│           │                │                │              │
│       ArrayList        LinkedList        Vector            │
│           │                                  │              │
│           │                               Stack            │
│           │                                                │
│   CopyOnWriteArrayList                                      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 实现类对比

```
┌─────────────────────────────────────────────────────────────┐
│                    List 实现类详解                           │
├───────────────────┬─────────────────────────────────────────┤
│   实现类          │   特点                                   │
├───────────────────┼─────────────────────────────────────────┤
│   ArrayList       │   • 动态数组实现                        │
│                   │   • 随机访问快 O(1)                     │
│                   │   • 增删慢 O(n)                         │
│                   │   • 非线程安全                          │
│                   │   • 最常用                              │
├───────────────────┼─────────────────────────────────────────┤
│   LinkedList      │   • 双向链表实现                        │
│                   │   • 随机访问慢 O(n)                     │
│                   │   • 头尾增删快 O(1)                     │
│                   │   • 实现了 Deque 接口                   │
│                   │   • 非线程安全                          │
├───────────────────┼─────────────────────────────────────────┤
│   Vector          │   • 动态数组实现                        │
│                   │   • 线程安全 (synchronized)             │
│                   │   • 已过时，不推荐使用                  │
│                   │   • 用 ArrayList + Collections.sync     │
├───────────────────┼─────────────────────────────────────────┤
│   Stack           │   • 继承自 Vector                       │
│                   │   • 栈结构 (LIFO)                       │
│                   │   • 已过时，用 ArrayDeque 替代          │
├───────────────────┼─────────────────────────────────────────┤
│   CopyOnWrite     │   • 写时复制                            │
│   ArrayList       │   • 线程安全                            │
│                   │   • 读多写少场景                        │
│                   │   • 迭代安全                            │
└───────────────────┴─────────────────────────────────────────┘
```

## 性能对比

```
┌─────────────────────────────────────────────────────────────┐
│                    性能对比                                  │
├───────────────┬───────────┬───────────┬───────────┬─────────┤
│   操作        │ ArrayList │ LinkedList │ Vector   │ COWAL   │
├───────────────┼───────────┼───────────┼───────────┼─────────┤
│   get(index)  │  O(1)     │  O(n)     │  O(1)     │  O(1)   │
│   add(末尾)   │  O(1)*    │  O(1)     │  O(1)*    │  O(n)   │
│   add(index)  │  O(n)     │  O(n)     │  O(n)     │  O(n)   │
│   remove      │  O(n)     │  O(n)     │  O(n)     │  O(n)   │
│   contains    │  O(n)     │  O(n)     │  O(n)     │  O(n)   │
├───────────────┼───────────┼───────────┼───────────┼─────────┤
│   线程安全    │  否       │  否       │  是       │  是     │
│   * 扩容时 O(n)                                             │
└───────────────┴───────────┴───────────┴───────────┴─────────┘
```

## 代码示例

```java
// ArrayList - 最常用
List<String> arrayList = new ArrayList<>();
arrayList.add("A");
arrayList.get(0);        // O(1)
arrayList.add(0, "B");   // O(n)

// LinkedList - 队列/栈场景
LinkedList<String> linkedList = new LinkedList<>();
linkedList.addFirst("A");  // O(1)
linkedList.addLast("B");   // O(1)
linkedList.removeFirst();  // O(1)
linkedList.get(5);         // O(n)

// Vector - 已过时
Vector<String> vector = new Vector<>();  // 不推荐

// 替代方案: 同步包装
List<String> syncList = Collections.synchronizedList(new ArrayList<>());

// CopyOnWriteArrayList - 读多写少
List<String> cowList = new CopyOnWriteArrayList<>();
cowList.add("A");  // 写操作复制整个数组

// Stack 替代方案
Deque<String> stack = new ArrayDeque<>();
stack.push("A");
stack.pop();
```

## 选择指南

```
┌─────────────────────────────────────────────────────────────┐
│                    选择指南                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   默认选择: ArrayList                                        │
│   • 随机访问多                                              │
│   • 遍历操作多                                              │
│   • 大多数场景                                              │
│                                                             │
│   选择 LinkedList:                                           │
│   • 频繁头部操作                                            │
│   • 需要队列功能                                            │
│   • 很少随机访问                                            │
│                                                             │
│   选择 CopyOnWriteArrayList:                                 │
│   • 高并发读                                                │
│   • 写操作很少                                              │
│   • 需要安全迭代                                            │
│                                                             │
│   避免使用:                                                  │
│   • Vector (用 ArrayList + synchronized)                    │
│   • Stack (用 ArrayDeque)                                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 内部实现

```java
// ArrayList 核心
public class ArrayList<E> {
    Object[] elementData;  // 存储数据的数组
    int size;              // 实际元素个数
    
    // 扩容: 1.5 倍
    private void grow(int minCapacity) {
        int newCapacity = oldCapacity + (oldCapacity >> 1);
        elementData = Arrays.copyOf(elementData, newCapacity);
    }
}

// LinkedList 核心
public class LinkedList<E> {
    Node<E> first;  // 头节点
    Node<E> last;   // 尾节点
    int size;
    
    private static class Node<E> {
        E item;
        Node<E> next;
        Node<E> prev;
    }
}
```

## 面试回答

### 30秒版本

> List 主要实现类：**ArrayList**（动态数组，随机访问 O(1)，最常用）、**LinkedList**（双向链表，头尾操作 O(1)，也是 Deque）、**Vector**（同步数组，已过时）、**CopyOnWriteArrayList**（写时复制，读多写少）。默认选 ArrayList，需要队列用 LinkedList，高并发读用 CopyOnWriteArrayList。

### 1分钟版本

> **ArrayList**：
> - 动态数组，1.5 倍扩容
> - 随机访问 O(1)
> - 增删 O(n)
> - 最常用
>
> **LinkedList**：
> - 双向链表
> - 头尾操作 O(1)
> - 实现 Deque 接口
>
> **CopyOnWriteArrayList**：
> - 写时复制，线程安全
> - 读无锁，写复制数组
>
> **已过时**：
> - Vector → ArrayList + synchronized
> - Stack → ArrayDeque

---

*关联文档：[arraylist-vs-linkedlist.md](arraylist-vs-linkedlist.md) | [java-collections.md](java-collections.md)*
