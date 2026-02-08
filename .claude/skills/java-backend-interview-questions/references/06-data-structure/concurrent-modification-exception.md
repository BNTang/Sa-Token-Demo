# ConcurrentModificationException 是如何产生的？

> 在遍历集合时同时修改集合内容，fail-fast 机制通过 modCount 检测并抛出此异常

## 30秒速答

ConcurrentModificationException 产生原因：
- **触发时机**: 遍历过程中修改了集合结构（添加/删除元素）
- **检测机制**: 迭代器的 `expectedModCount != modCount`
- **本质**: fail-fast 快速失败机制
- **解决方案**: 使用 Iterator.remove()、CopyOnWriteArrayList 或 并发安全集合

## 一分钟详解

### 典型错误场景

```java
// ❌ 错误示例1: foreach 中删除元素
List<String> list = new ArrayList<>(Arrays.asList("A", "B", "C"));
for (String s : list) {
    if ("B".equals(s)) {
        list.remove(s);  // 抛出 ConcurrentModificationException
    }
}

// ❌ 错误示例2: 迭代器遍历中直接修改集合
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    String s = it.next();
    if ("B".equals(s)) {
        list.remove(s);  // 抛出 ConcurrentModificationException
    }
}
```

### fail-fast 机制原理

```java
// ArrayList 源码
public class ArrayList<E> {
    protected transient int modCount = 0;  // 修改计数器
    
    public boolean add(E e) {
        modCount++;  // 添加时 +1
        // ...
    }
    
    public E remove(int index) {
        modCount++;  // 删除时 +1
        // ...
    }
}

// 迭代器源码
private class Itr implements Iterator<E> {
    int expectedModCount = modCount;  // 创建时记录
    
    public E next() {
        checkForComodification();  // 每次调用都检查
        // ...
    }
    
    final void checkForComodification() {
        if (modCount != expectedModCount)
            throw new ConcurrentModificationException();
    }
}
```

### 检测过程图示

```
创建迭代器：
┌────────────────────────────────────────────────────┐
│  ArrayList.modCount = 3                            │
│  Iterator.expectedModCount = 3  (复制)             │
└────────────────────────────────────────────────────┘

遍历过程中调用 list.remove()：
┌────────────────────────────────────────────────────┐
│  ArrayList.modCount = 4  (++了)                    │
│  Iterator.expectedModCount = 3  (没变)             │
│                                                    │
│  调用 next() 时检查：                               │
│  4 != 3 → 抛出 ConcurrentModificationException    │
└────────────────────────────────────────────────────┘
```

### 正确的解决方案

```java
List<String> list = new ArrayList<>(Arrays.asList("A", "B", "C", "D"));

// ✅ 方案1: 使用 Iterator.remove()
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    String s = it.next();
    if ("B".equals(s)) {
        it.remove();  // 安全删除，会同步更新 expectedModCount
    }
}

// ✅ 方案2: 使用 removeIf() (Java 8+)
list.removeIf(s -> "B".equals(s));

// ✅ 方案3: 使用 CopyOnWriteArrayList
List<String> cowList = new CopyOnWriteArrayList<>(list);
for (String s : cowList) {
    if ("B".equals(s)) {
        cowList.remove(s);  // 不会抛异常（写时复制）
    }
}

// ✅ 方案4: 倒序遍历删除
for (int i = list.size() - 1; i >= 0; i--) {
    if ("B".equals(list.get(i))) {
        list.remove(i);  // 倒序删除不影响未遍历元素
    }
}

// ✅ 方案5: 收集后统一删除
List<String> toRemove = new ArrayList<>();
for (String s : list) {
    if ("B".equals(s)) {
        toRemove.add(s);
    }
}
list.removeAll(toRemove);
```

### 多线程场景

```java
// 多线程并发修改也会触发
List<String> list = new ArrayList<>(Arrays.asList("A", "B", "C"));

// 线程1: 遍历
new Thread(() -> {
    for (String s : list) {
        System.out.println(s);
        Thread.sleep(100);  // 模拟慢操作
    }
}).start();

// 线程2: 修改
new Thread(() -> {
    list.add("D");  // 可能导致线程1抛异常
}).start();

// ✅ 解决: 使用并发安全集合
List<String> safeList = new CopyOnWriteArrayList<>(list);
// 或 Collections.synchronizedList(list) + 手动同步遍历
```

### 哪些集合有 fail-fast？

| 集合类型 | fail-fast | 备注 |
|---------|-----------|------|
| ArrayList | ✅ 是 | 单线程也会触发 |
| LinkedList | ✅ 是 | 同上 |
| HashMap | ✅ 是 | 遍历时修改 |
| HashSet | ✅ 是 | 底层是 HashMap |
| CopyOnWriteArrayList | ❌ 否 | 遍历快照 |
| ConcurrentHashMap | ❌ 否 | 弱一致性 |
| Vector | ✅ 是 | 虽然同步但仍有 |

## 关键记忆点

```
┌─────────────────────────────────────────────────────┐
│  ConcurrentModificationException 速记：             │
│                                                     │
│  「改」遍历时结构修改（add/remove）                  │
│  「数」modCount != expectedModCount                 │
│  「快」fail-fast 快速失败机制                        │
│                                                     │
│  解决方案：                                         │
│  ┌───────────────────────────────────────────┐     │
│  │ 1. Iterator.remove()   → 同步更新计数     │     │
│  │ 2. removeIf()          → Java 8 简洁写法  │     │
│  │ 3. CopyOnWriteArrayList→ 遍历快照不报错   │     │
│  │ 4. 倒序遍历删除        → 索引不错乱       │     │
│  │ 5. 收集后统一删除      → 分离遍历和修改   │     │
│  └───────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────┘
```

## 面试追问

**Q: fail-fast 和 fail-safe 的区别？**

| 机制 | 行为 | 代表集合 |
|------|------|---------|
| fail-fast | 检测到修改立即报错 | ArrayList, HashMap |
| fail-safe | 遍历副本，不报错 | CopyOnWriteArrayList, ConcurrentHashMap |

**Q: 为什么删除倒数第二个元素不报错？**

A: 这是一个特殊情况。删除后 size 减小，`hasNext()` 返回 false，循环提前结束，没有调用 `next()` 的检查逻辑。
