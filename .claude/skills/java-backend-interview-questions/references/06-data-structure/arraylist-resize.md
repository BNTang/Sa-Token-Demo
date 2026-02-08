# Java ArrayList 的扩容机制是什么？

## 扩容机制概述

```
┌─────────────────────────────────────────────────────────────┐
│                    ArrayList 扩容机制                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   核心: 新容量 = 旧容量 × 1.5 (右移1位 + 原值)              │
│                                                             │
│   int newCapacity = oldCapacity + (oldCapacity >> 1);       │
│                                                             │
│   扩容过程:                                                  │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  10 → 15 → 22 → 33 → 49 → 73 → 109 → ...           │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 扩容流程

```
┌─────────────────────────────────────────────────────────────┐
│                    扩容流程图                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   add(element)                                              │
│       │                                                     │
│       ↓                                                     │
│   ensureCapacityInternal(size + 1)                         │
│       │                                                     │
│       ↓                                                     │
│   是否需要扩容? (minCapacity > elementData.length)         │
│       │                                                     │
│   ┌───┴───┐                                                │
│   │       │                                                │
│   ↓       ↓                                                │
│  否      是 → grow(minCapacity)                            │
│   │           │                                            │
│   │           ↓                                            │
│   │      newCapacity = oldCapacity + (oldCapacity >> 1)    │
│   │           │                                            │
│   │           ↓                                            │
│   │      Arrays.copyOf(elementData, newCapacity)           │
│   │           │                                            │
│   └─────┬─────┘                                            │
│         ↓                                                   │
│   elementData[size++] = element                            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 源码解析

```java
// 初始容量
private static final int DEFAULT_CAPACITY = 10;

// 空数组
private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {};

// 存储元素的数组
transient Object[] elementData;

// add 方法
public boolean add(E e) {
    // 确保容量足够
    ensureCapacityInternal(size + 1);
    // 添加元素
    elementData[size++] = e;
    return true;
}

// 确保容量
private void ensureCapacityInternal(int minCapacity) {
    ensureExplicitCapacity(calculateCapacity(elementData, minCapacity));
}

// 计算容量
private static int calculateCapacity(Object[] elementData, int minCapacity) {
    // 首次添加，返回 DEFAULT_CAPACITY (10)
    if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
        return Math.max(DEFAULT_CAPACITY, minCapacity);
    }
    return minCapacity;
}

// 判断是否需要扩容
private void ensureExplicitCapacity(int minCapacity) {
    modCount++;
    // 需要的容量 > 当前数组长度，扩容
    if (minCapacity - elementData.length > 0)
        grow(minCapacity);
}

// 核心扩容逻辑
private void grow(int minCapacity) {
    int oldCapacity = elementData.length;
    
    // 新容量 = 旧容量 × 1.5
    int newCapacity = oldCapacity + (oldCapacity >> 1);
    
    // 新容量不够，使用需要的最小容量
    if (newCapacity - minCapacity < 0)
        newCapacity = minCapacity;
    
    // 超过最大限制
    if (newCapacity - MAX_ARRAY_SIZE > 0)
        newCapacity = hugeCapacity(minCapacity);
    
    // 复制到新数组
    elementData = Arrays.copyOf(elementData, newCapacity);
}
```

## 扩容示例

```java
List<String> list = new ArrayList<>();  // capacity = 0 (懒加载)

list.add("1");  // 首次添加，capacity = 10
list.add("2");  // size=2, capacity=10
...
list.add("10"); // size=10, capacity=10
list.add("11"); // 触发扩容! capacity = 10 + 10/2 = 15

// 扩容计算
// oldCapacity = 10
// newCapacity = 10 + (10 >> 1) = 10 + 5 = 15
```

## 性能优化

```java
// ❌ 不推荐: 频繁扩容
List<String> list = new ArrayList<>();
for (int i = 0; i < 10000; i++) {
    list.add("item" + i);  // 多次扩容
}

// ✅ 推荐: 预分配容量
List<String> list = new ArrayList<>(10000);  // 指定初始容量
for (int i = 0; i < 10000; i++) {
    list.add("item" + i);  // 无扩容
}

// 公式: 初始容量 = (预期元素数 / 负载因子) + 1
// 对于 ArrayList，直接用预期元素数即可
```

## 扩容次数计算

```
┌─────────────────────────────────────────────────────────────┐
│         存储 N 个元素需要扩容多少次？                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   元素数量         扩容次数         容量变化                 │
│   ─────────        ─────────        ─────────                │
│   1-10             1 (初始化)       0 → 10                   │
│   11-15            2                10 → 15                  │
│   16-22            3                15 → 22                  │
│   23-33            4                22 → 33                  │
│   34-49            5                33 → 49                  │
│   ...                                                        │
│   100              8                                         │
│   1000             18                                        │
│   10000            25                                        │
│                                                             │
│   公式: 扩容次数 ≈ log₁.₅(N/10) + 1                          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 与 Vector 对比

```
┌─────────────────────────────────────────────────────────────┐
│              ArrayList vs Vector 扩容                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ArrayList:                                                 │
│   • 扩容 1.5 倍: newCapacity = oldCapacity + (old >> 1)     │
│   • 更节省空间                                              │
│   • 非线程安全                                              │
│                                                             │
│   Vector:                                                    │
│   • 扩容 2 倍: newCapacity = oldCapacity + oldCapacity      │
│   • 空间浪费更多                                            │
│   • 线程安全 (synchronized)                                 │
│   • 已过时                                                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> ArrayList 扩容为**原容量的 1.5 倍**：`newCapacity = oldCapacity + (oldCapacity >> 1)`。首次添加时初始化为 10，之后 10→15→22→33...。扩容时用 `Arrays.copyOf` 创建新数组并复制元素。性能优化：**预分配容量**避免多次扩容。

### 1分钟版本

> **扩容时机**：size + 1 > 当前容量
>
> **扩容大小**：1.5 倍
> ```java
> newCapacity = oldCapacity + (oldCapacity >> 1)
> ```
>
> **扩容过程**：
> 1. 计算新容量
> 2. Arrays.copyOf 创建新数组
> 3. 复制原数组元素
>
> **初始容量**：
> - 空构造：首次添加时分配 10
> - 指定容量：直接分配
>
> **优化建议**：
> - 预估大小，初始化时指定
> - 减少扩容复制开销

---

*关联文档：[arraylist-vs-linkedlist.md](arraylist-vs-linkedlist.md) | [list-implementations.md](list-implementations.md)*
