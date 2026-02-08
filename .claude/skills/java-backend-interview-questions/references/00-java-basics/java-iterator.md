# 什么是 Java 中的迭代器 (Iterator)？

## 迭代器概述

```
┌─────────────────────────────────────────────────────────────┐
│                    Iterator 迭代器                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   定义: 用于遍历集合元素的接口                              │
│                                                             │
│   包: java.util.Iterator                                    │
│                                                             │
│   核心方法:                                                  │
│   • hasNext()  判断是否还有元素                             │
│   • next()     返回下一个元素                               │
│   • remove()   删除当前元素 (可选操作)                      │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │   Collection                                         │  │
│   │   ┌─────┬─────┬─────┬─────┬─────┐                   │  │
│   │   │  A  │  B  │  C  │  D  │  E  │                   │  │
│   │   └─────┴─────┴─────┴─────┴─────┘                   │  │
│   │         ↑                                            │  │
│   │      Iterator (游标)                                 │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 基本用法

```java
List<String> list = Arrays.asList("A", "B", "C", "D");

// 获取迭代器
Iterator<String> iterator = list.iterator();

// 遍历
while (iterator.hasNext()) {
    String element = iterator.next();
    System.out.println(element);
}

// 简化: for-each 循环 (底层使用迭代器)
for (String element : list) {
    System.out.println(element);
}
```

## 安全删除元素

```java
List<String> list = new ArrayList<>(Arrays.asList("A", "B", "C", "D"));

// ❌ 错误: for-each 中删除会抛 ConcurrentModificationException
for (String s : list) {
    if (s.equals("B")) {
        list.remove(s);  // 抛异常!
    }
}

// ✅ 正确: 使用迭代器的 remove()
Iterator<String> iterator = list.iterator();
while (iterator.hasNext()) {
    String s = iterator.next();
    if (s.equals("B")) {
        iterator.remove();  // 安全删除
    }
}

// ✅ Java 8+: removeIf()
list.removeIf(s -> s.equals("B"));
```

## fail-fast 机制

```
┌─────────────────────────────────────────────────────────────┐
│                    fail-fast 快速失败                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   原理:                                                      │
│   • 每个集合维护 modCount (修改次数)                        │
│   • 迭代器创建时记录 expectedModCount                       │
│   • 每次 next() 检查两者是否相等                            │
│   • 不相等则抛出 ConcurrentModificationException            │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  iterator = list.iterator()  // 记录 modCount = 3    │  │
│   │  list.add("X")               // modCount = 4         │  │
│   │  iterator.next()             // 检查失败，抛异常     │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   注意: iterator.remove() 会同步更新 expectedModCount       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## ListIterator

```java
// ListIterator: List 专用的双向迭代器
List<String> list = new ArrayList<>(Arrays.asList("A", "B", "C"));
ListIterator<String> li = list.listIterator();

// 正向遍历
while (li.hasNext()) {
    int index = li.nextIndex();
    String element = li.next();
    System.out.println(index + ": " + element);
}

// 反向遍历
while (li.hasPrevious()) {
    String element = li.previous();
    System.out.println(element);
}

// 修改元素
li = list.listIterator();
while (li.hasNext()) {
    String s = li.next();
    if (s.equals("B")) {
        li.set("BB");  // 替换当前元素
    }
}

// 插入元素
li.add("D");  // 在当前位置插入
```

## Iterator vs for-each vs for

```
┌─────────────────────────────────────────────────────────────┐
│                    三种遍历方式对比                          │
├──────────────────┬──────────────────────────────────────────┤
│   方式           │   特点                                   │
├──────────────────┼──────────────────────────────────────────┤
│   for 循环       │ 可用索引、可修改元素、ArrayList 性能好   │
│   for-each       │ 简洁、底层用迭代器、不能删除元素         │
│   Iterator       │ 可安全删除、通用性强                     │
├──────────────────┴──────────────────────────────────────────┤
│   选择建议:                                                  │
│   • 只遍历: for-each (最简洁)                               │
│   • 需删除: Iterator (最安全)                               │
│   • 需索引: for 循环                                        │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> **Iterator** 是遍历集合的接口，核心方法：`hasNext()`、`next()`、`remove()`。优势是**遍历时可安全删除元素**，避免 ConcurrentModificationException。for-each 底层使用迭代器但不能删除。**fail-fast** 机制通过 modCount 检测并发修改。ListIterator 支持双向遍历和修改。

### 1分钟版本

> **定义**：遍历集合元素的接口
>
> **核心方法**：
> - hasNext()：有无下一个
> - next()：获取下一个
> - remove()：删除当前元素
>
> **优势**：
> - 遍历时安全删除
> - 统一遍历接口
>
> **fail-fast**：
> - 检测集合被修改
> - 抛 ConcurrentModificationException
>
> **ListIterator**：
> - 双向遍历
> - set() 修改元素
> - add() 插入元素
>
> **for-each**：
> - 底层用迭代器
> - 不能删除元素

---

*关联文档：[java8-features.md](java8-features.md)*
