# Java 的 StringBuilder 是怎么实现的？

## 核心实现原理

```
┌─────────────────────────────────────────────────────────────┐
│                    StringBuilder 实现原理                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   底层数据结构:                                              │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  // Java 8                                           │  │
│   │  char[] value;                                       │  │
│   │                                                     │  │
│   │  // Java 9+ (Compact Strings)                        │  │
│   │  byte[] value;                                       │  │
│   │  byte coder;  // LATIN1=0 或 UTF16=1                 │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   关键属性:                                                  │
│   • value: 存储字符的数组                                   │
│   • count: 已使用的字符数                                   │
│   • capacity: 数组总容量 (value.length)                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 类继承结构

```
┌─────────────────────────────────────────────────────────────┐
│                    继承结构                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   Object                                                     │
│       │                                                     │
│       ▼                                                     │
│   AbstractStringBuilder  ← 核心实现                         │
│       │                     • char[]/byte[] value          │
│       │                     • int count                     │
│       │                     • append(), insert()...         │
│       │                                                     │
│       ├─── StringBuilder   (非线程安全)                     │
│       │                                                     │
│       └─── StringBuffer    (线程安全, synchronized)         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 源码分析

```java
// AbstractStringBuilder 核心源码 (简化)

abstract class AbstractStringBuilder {
    // 底层数组
    byte[] value;      // Java 9+
    int count;         // 已使用字符数
    byte coder;        // 编码: LATIN1 或 UTF16
    
    // 默认容量
    AbstractStringBuilder() {
        value = new byte[16];  // 默认容量 16
    }
    
    AbstractStringBuilder(int capacity) {
        value = new byte[capacity];
    }
    
    // append 方法
    public AbstractStringBuilder append(String str) {
        if (str == null) {
            return appendNull();
        }
        int len = str.length();
        // 确保容量足够
        ensureCapacityInternal(count + len);
        // 拷贝字符到数组
        str.getChars(0, len, value, count);
        count += len;
        return this;
    }
    
    // 确保容量
    private void ensureCapacityInternal(int minimumCapacity) {
        if (minimumCapacity > value.length) {
            // 扩容
            expandCapacity(minimumCapacity);
        }
    }
}
```

## 扩容机制

```
┌─────────────────────────────────────────────────────────────┐
│                    扩容机制                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   扩容公式:                                                  │
│   newCapacity = (oldCapacity << 1) + 2                      │
│   即: 新容量 = 旧容量 * 2 + 2                               │
│                                                             │
│   扩容过程:                                                  │
│   ┌───────────────────────────────────────────────────────┐│
│   │  初始容量: 16                                          ││
│   │  第1次扩容: 16 * 2 + 2 = 34                            ││
│   │  第2次扩容: 34 * 2 + 2 = 70                            ││
│   │  第3次扩容: 70 * 2 + 2 = 142                           ││
│   │  ...                                                   ││
│   └───────────────────────────────────────────────────────┘│
│                                                             │
│   如果 newCapacity 仍不够:                                   │
│   newCapacity = minimumCapacity  (使用需要的最小容量)       │
│                                                             │
│   扩容涉及数组拷贝，有性能开销                              │
│   → 预估容量，使用 new StringBuilder(capacity)              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

```java
// 扩容源码
private void expandCapacity(int minimumCapacity) {
    // 新容量 = 旧容量 * 2 + 2
    int newCapacity = (value.length << 1) + 2;
    
    if (newCapacity < minimumCapacity) {
        newCapacity = minimumCapacity;
    }
    
    if (newCapacity < 0) {
        // 溢出处理
        newCapacity = Integer.MAX_VALUE;
    }
    
    // 创建新数组并拷贝
    value = Arrays.copyOf(value, newCapacity);
}
```

## 常用方法实现

```java
// append - O(n)
public StringBuilder append(String str) {
    super.append(str);  // 调用父类
    return this;
}

// insert - O(n) 需要移动元素
public StringBuilder insert(int offset, String str) {
    // 1. 确保容量
    // 2. 将 offset 后的元素后移
    // 3. 插入新字符串
    System.arraycopy(value, offset, value, offset + str.length(), count - offset);
    str.getChars(value, offset);
    count += str.length();
    return this;
}

// delete - O(n) 需要移动元素
public StringBuilder delete(int start, int end) {
    // 将 end 后的元素前移
    System.arraycopy(value, end, value, start, count - end);
    count -= (end - start);
    return this;
}

// reverse - O(n)
public StringBuilder reverse() {
    // 交换首尾对应字符
    for (int i = 0, j = count - 1; i < j; i++, j--) {
        char temp = value[i];
        value[i] = value[j];
        value[j] = temp;
    }
    return this;
}

// toString - 创建新 String 对象
public String toString() {
    return new String(value, 0, count);
}
```

## 性能优化建议

```
┌─────────────────────────────────────────────────────────────┐
│                    性能优化建议                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 预估容量                                                │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  // ❌ 默认容量 16，可能多次扩容                     │  │
│   │  StringBuilder sb = new StringBuilder();             │  │
│   │                                                     │  │
│   │  // ✅ 预估容量，减少扩容                            │  │
│   │  StringBuilder sb = new StringBuilder(1000);         │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   2. 链式调用                                                │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  sb.append("a").append("b").append("c");             │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   3. 避免频繁 insert/delete                                  │
│      └── 需要移动元素，O(n) 复杂度                          │
│                                                             │
│   4. 复用 StringBuilder                                      │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  sb.setLength(0);  // 清空，复用                     │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> StringBuilder 底层用 **char[]**/byte[] 数组存储字符，初始容量 **16**。append 时检查容量，不足则**扩容为原来的 2 倍 + 2**，涉及数组拷贝。它继承 `AbstractStringBuilder`，与 StringBuffer 共享实现，区别是 StringBuffer 的方法加了 synchronized。优化建议：预估容量避免扩容，复用对象。

### 1分钟版本

> **底层结构**：
> - Java 8: char[] value
> - Java 9+: byte[] value (Compact Strings)
> - int count: 已使用长度
>
> **继承结构**：
> - 继承 AbstractStringBuilder
> - StringBuffer 也继承它（加 synchronized）
>
> **扩容机制**：
> - 默认容量 16
> - 扩容公式: 原容量 * 2 + 2
> - 涉及 Arrays.copyOf 数组拷贝
>
> **方法实现**：
> - append: O(n)，追加到末尾
> - insert/delete: O(n)，需移动元素
> - reverse: O(n)，首尾交换
> - toString: 创建新 String
>
> **优化建议**：
> - 预估容量 `new StringBuilder(1000)`
> - 复用 `sb.setLength(0)`
> - 避免频繁 insert/delete

---

*关联文档：[string-buffer-builder.md](string-buffer-builder.md) | [arraylist-implementation.md](../08-collection/arraylist-implementation.md)*
