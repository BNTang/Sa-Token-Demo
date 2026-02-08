# 使用 HashMap 时，有哪些提升性能的技巧？

## 性能优化技巧

```
┌─────────────────────────────────────────────────────────────┐
│                    HashMap 性能优化                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 指定初始容量                                            │
│   2. 合理设置负载因子                                        │
│   3. 优化 hashCode() 实现                                   │
│   4. 选择合适的 Key 类型                                    │
│   5. 避免频繁扩容                                            │
│   6. 遍历方式选择                                            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 1. 指定初始容量 (最重要)

```java
// ❌ 糟糕: 不指定容量
Map<String, Object> map = new HashMap<>();  // 默认 16

// 如果要放 1000 个元素:
// 16 → 32 → 64 → 128 → 256 → 512 → 1024
// 触发 6 次扩容，每次 rehash 所有元素!

// ✅ 推荐: 指定初始容量
// 公式: (预期元素数 / 负载因子) + 1
int expectedSize = 1000;
int initialCapacity = (int) (expectedSize / 0.75f) + 1;  // 1334
Map<String, Object> map = new HashMap<>(initialCapacity);

// ✅ 更简单: 使用 Guava
Map<String, Object> map = Maps.newHashMapWithExpectedSize(1000);
```

## 2. 合理使用负载因子

```java
// 默认负载因子 0.75 (平衡空间和时间)

// 空间敏感场景 (牺牲时间换空间)
Map<String, Object> map = new HashMap<>(16, 0.9f);

// 时间敏感场景 (牺牲空间换时间)
Map<String, Object> map = new HashMap<>(16, 0.5f);

// ⚠️ 一般情况保持 0.75 即可
```

## 3. 优化 hashCode() 实现

```java
// ❌ 糟糕: 总是返回相同值 (全部冲突)
@Override
public int hashCode() {
    return 1;  // 所有元素都在一个桶，退化成链表
}

// ❌ 糟糕: 使用可变字段
@Override
public int hashCode() {
    return mutableField.hashCode();  // 字段变化后找不到
}

// ✅ 推荐: 使用 Objects.hash()
@Override
public int hashCode() {
    return Objects.hash(id, name);  // 基于不可变字段
}

// ✅ 高性能: 手动计算 (避免自动装箱)
@Override
public int hashCode() {
    int result = 17;
    result = 31 * result + id;
    result = 31 * result + (name != null ? name.hashCode() : 0);
    return result;
}
```

## 4. 选择合适的 Key 类型

```java
// ✅ 推荐: 使用不可变对象作为 Key
Map<String, Object> map1 = new HashMap<>();  // String 不可变
Map<Integer, Object> map2 = new HashMap<>(); // Integer 不可变

// ❌ 避免: 使用可变对象作为 Key
Map<List<String>, Object> map3 = new HashMap<>();  // 危险!
// List 内容变化后 hashCode 变化，无法找到元素

// ✅ 自定义 Key 必须正确实现 hashCode() 和 equals()
public class CacheKey {
    private final String module;
    private final long id;
    
    // 必须重写
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CacheKey cacheKey = (CacheKey) o;
        return id == cacheKey.id && Objects.equals(module, cacheKey.module);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(module, id);
    }
}
```

## 5. 避免频繁扩容

```java
// 批量添加前预估大小
List<User> users = getUserList();  // 假设 1000 个

// ✅ 预分配
Map<Long, User> userMap = new HashMap<>((int) (users.size() / 0.75f) + 1);
for (User user : users) {
    userMap.put(user.getId(), user);
}

// ✅ 使用 Stream 收集
Map<Long, User> userMap = users.stream()
    .collect(Collectors.toMap(
        User::getId, 
        Function.identity(),
        (o, n) -> n,  // 处理重复 key
        () -> new HashMap<>((int) (users.size() / 0.75f) + 1)  // 指定容量
    ));
```

## 6. 高效遍历方式

```java
Map<String, Object> map = new HashMap<>();

// ✅ 推荐: entrySet 遍历 (最高效)
for (Map.Entry<String, Object> entry : map.entrySet()) {
    String key = entry.getKey();
    Object value = entry.getValue();
}

// ✅ Java 8+ forEach
map.forEach((key, value) -> {
    // 处理
});

// ❌ 避免: keySet + get (两次查找)
for (String key : map.keySet()) {
    Object value = map.get(key);  // 额外的 hash 查找
}
```

## 7. 并发场景选择

```java
// ❌ 非线程安全
Map<String, Object> map = new HashMap<>();

// ✅ 高并发场景用 ConcurrentHashMap
Map<String, Object> map = new ConcurrentHashMap<>();

// ⚠️ 避免: Collections.synchronizedMap (全局锁，性能差)
Map<String, Object> map = Collections.synchronizedMap(new HashMap<>());
```

## 性能对比

```
┌─────────────────────────────────────────────────────────────┐
│                    性能对比                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   操作                不优化              优化后             │
│   ─────               ──────              ──────             │
│   放入 100万元素      800ms (6次扩容)     300ms (无扩容)     │
│   遍历 keySet+get     200ms               80ms (entrySet)    │
│   并发写入            异常/数据丢失       正常 (CHM)         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> 1. **指定初始容量**（最重要）：`(预期大小 / 0.75) + 1`，避免扩容
> 2. **正确实现 hashCode()**：均匀分布，使用不可变字段
> 3. **用不可变对象作 Key**：如 String、Integer
> 4. **entrySet 遍历**：避免 keySet + get 两次查找
> 5. **并发用 ConcurrentHashMap**

### 1分钟版本

> **初始容量**：
> - 公式: (元素数 / 0.75) + 1
> - 避免多次扩容 rehash
>
> **hashCode 优化**：
> - 均匀分布减少碰撞
> - 使用不可变字段
> - Objects.hash() 或手动计算
>
> **Key 选择**：
> - 不可变对象 (String/Integer)
> - 自定义类必须重写 equals/hashCode
>
> **遍历优化**：
> - entrySet 最高效
> - 避免 keySet + get
>
> **并发**：
> - ConcurrentHashMap

---

*关联文档：[hashmap-principle.md](hashmap-principle.md) | [hashmap-resize.md](hashmap-resize.md)*
