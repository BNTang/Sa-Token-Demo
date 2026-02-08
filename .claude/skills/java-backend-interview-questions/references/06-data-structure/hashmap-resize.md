# HashMap 扩容机制

> Java 后端面试知识点 - 数据结构

---

## HashMap 基本结构

```
HashMap 底层结构（JDK 8+）：

┌──────────────────────────────────────────────────────────────┐
│                         数组                                  │
├────┬────┬────┬────┬────┬────┬────┬────┬────┬────┬────┬────┤
│ 0  │ 1  │ 2  │ 3  │ 4  │ 5  │ 6  │ 7  │ 8  │ 9  │... │ n  │
└──┬─┴────┴──┬─┴────┴────┴──┬─┴────┴────┴────┴────┴────┴────┘
   │         │              │
   ↓         ↓              ↓
┌──────┐  ┌──────┐      ┌──────┐
│Node1 │  │Node2 │      │Node5 │
└──┬───┘  └──┬───┘      └──┬───┘
   ↓         ↓              ↓
┌──────┐  ┌──────┐      红黑树
│Node1b│  │Node2b│      (链表长度 > 8 且数组长度 >= 64)
└──────┘  └──┬───┘
             ↓
         ┌──────┐
         │Node2c│
         └──────┘
```

---

## 为什么容量是 2 的 n 次方

### 核心原因：高效取模

```java
// HashMap 计算数组索引
static int indexFor(int hash, int length) {
    return hash & (length - 1);  // 位运算取模
}
```

**当 length = 2^n 时**：

```
length = 16 (2^4) = 00010000
length - 1 = 15   = 00001111

hash = 12345678 (任意 hash 值)
index = hash & 15

    12345678  (二进制: ...01011100001110)
  &       15  (二进制:         00001111)
  ──────────
=        14   (只保留低 4 位)
```

**位运算 vs 取模运算**：
- `hash % length`：取模运算，较慢
- `hash & (length - 1)`：位运算，极快
- **两者等价的前提**：length 是 2 的幂

### 均匀分布

当 `length = 2^n` 时，`length - 1` 的二进制全是 1：

```
length = 16, length - 1 = 15 = 1111
length = 32, length - 1 = 31 = 11111
length = 64, length - 1 = 63 = 111111
```

这样 hash 值的低位能完全参与运算，分布更均匀。

**如果 length 不是 2 的幂**：

```
length = 15, length - 1 = 14 = 1110

hash & 14：
    任意hash  & 1110
    
最低位永远是 0！索引永远是偶数，奇数位置浪费。
```

---

## 扩容流程

### 触发条件

```java
// 元素数量 > 容量 × 负载因子
if (size > threshold) {
    resize();
}

// threshold = capacity * loadFactor
// 默认：threshold = 16 * 0.75 = 12
```

### 扩容步骤

```java
final Node<K,V>[] resize() {
    Node<K,V>[] oldTab = table;
    int oldCap = (oldTab == null) ? 0 : oldTab.length;
    int oldThr = threshold;
    int newCap, newThr = 0;
    
    // 1. 计算新容量（翻倍）
    if (oldCap > 0) {
        newCap = oldCap << 1;  // 容量翻倍
        newThr = oldThr << 1;  // 阈值翻倍
    }
    
    // 2. 创建新数组
    Node<K,V>[] newTab = new Node[newCap];
    table = newTab;
    
    // 3. 重新分配元素
    if (oldTab != null) {
        for (int j = 0; j < oldCap; ++j) {
            Node<K,V> e = oldTab[j];
            if (e != null) {
                oldTab[j] = null;  // 帮助 GC
                
                if (e.next == null) {
                    // 单个节点，直接计算新位置
                    newTab[e.hash & (newCap - 1)] = e;
                } else if (e instanceof TreeNode) {
                    // 红黑树，拆分
                    ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);
                } else {
                    // 链表，巧妙拆分
                    Node<K,V> loHead = null, loTail = null;  // 低位链表
                    Node<K,V> hiHead = null, hiTail = null;  // 高位链表
                    
                    do {
                        // 关键：hash & oldCap 判断高位是 0 还是 1
                        if ((e.hash & oldCap) == 0) {
                            // 位置不变
                            if (loTail == null) loHead = e;
                            else loTail.next = e;
                            loTail = e;
                        } else {
                            // 位置 = 原位置 + oldCap
                            if (hiTail == null) hiHead = e;
                            else hiTail.next = e;
                            hiTail = e;
                        }
                    } while ((e = e.next) != null);
                    
                    // 放置链表
                    if (loTail != null) {
                        loTail.next = null;
                        newTab[j] = loHead;
                    }
                    if (hiTail != null) {
                        hiTail.next = null;
                        newTab[j + oldCap] = hiHead;
                    }
                }
            }
        }
    }
    return newTab;
}
```

### 链表拆分原理

```
扩容前：capacity = 16
扩容后：capacity = 32

原索引 = hash & (16-1) = hash & 0b1111

扩容后：
新索引 = hash & (32-1) = hash & 0b11111

关键位是第 5 位（原来的最高位+1）：
- 如果第 5 位 = 0：新索引 = 原索引
- 如果第 5 位 = 1：新索引 = 原索引 + 16

判断方法：hash & oldCap（hash & 16）
- 结果为 0：位置不变
- 结果为 16：位置 = 原位置 + oldCap

示例：
oldCap = 16, index = 5

元素A: hash = 0b...00101 (5)
       hash & 16 = 0b00101 & 0b10000 = 0
       新位置 = 5

元素B: hash = 0b...10101 (21)
       hash & 16 = 0b10101 & 0b10000 = 16 (非0)
       新位置 = 5 + 16 = 21
```

---

## 初始化容量建议

```java
// ✅ 预估容量，减少扩容
int expectedSize = 100;
int initialCapacity = (int) (expectedSize / 0.75) + 1;
Map<String, Object> map = new HashMap<>(initialCapacity);

// 或使用 Guava
Map<String, Object> map = Maps.newHashMapWithExpectedSize(100);
```

**计算公式**：`initialCapacity = expectedSize / loadFactor + 1`

---

## 面试要点

### 核心答案

**问：为什么 HashMap 在 Java 中扩容时采用 2 的 n 次方倍？**

答：HashMap 容量采用 2 的 n 次方主要有两个原因：

1. **高效计算索引**
   - 当 `length = 2^n` 时，`hash % length` 等价于 `hash & (length - 1)`
   - 位运算比取模运算快得多

2. **元素分布均匀**
   - `length - 1` 的二进制全是 1
   - hash 值的低位能完全参与运算
   - 避免某些位置永远不会被使用

**扩容时的优化**：
- 扩容为原来的 2 倍（`newCap = oldCap << 1`）
- 链表拆分只需判断 `hash & oldCap` 是否为 0
- 元素要么在原位置，要么在 `原位置 + oldCap`
- 无需重新计算 hash，效率高

---

## 编码最佳实践

### ✅ 推荐做法

```java
// 1. 预估容量，避免扩容
int size = 1000;
Map<String, User> map = new HashMap<>((int)(size / 0.75) + 1);

// 2. 使用 Guava
Map<String, User> map = Maps.newHashMapWithExpectedSize(1000);

// 3. 不可变 Map（无扩容问题）
Map<String, Integer> map = Map.of("a", 1, "b", 2);

// 4. 并发场景用 ConcurrentHashMap
Map<String, User> map = new ConcurrentHashMap<>(expectedSize);
```

### ❌ 避免做法

```java
// ❌ 不指定初始容量
Map<String, User> map = new HashMap<>();  // 默认 16，频繁扩容

// ❌ 初始容量太小
Map<String, User> map = new HashMap<>(10);  // 存 1000 个元素要扩容多次

// ❌ 多线程使用 HashMap
new Thread(() -> map.put("a", 1)).start();
new Thread(() -> map.put("b", 2)).start();
// JDK 7 可能死循环，JDK 8+ 数据丢失
```

---

## JDK 7 vs JDK 8 扩容区别

| 特性 | JDK 7 | JDK 8 |
|------|-------|-------|
| 数据结构 | 数组 + 链表 | 数组 + 链表 + 红黑树 |
| 链表插入 | 头插法 | 尾插法 |
| 扩容顺序 | 链表逆序 | 保持顺序 |
| 并发问题 | 可能死循环 | 数据丢失（仍不安全） |

**JDK 7 死循环原因**：
- 多线程扩容时，头插法可能形成环形链表
- get() 时进入死循环

**JDK 8 改进**：
- 使用尾插法，保持链表顺序
- 不会形成环形链表，但仍线程不安全
