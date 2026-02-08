# Java 中的 HashSet 和 HashMap 有什么区别？

## 核心区别

```
┌─────────────────────────────────────────────────────────────┐
│                 HashSet vs HashMap                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   本质关系: HashSet 底层就是 HashMap                         │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │                                                     │  │
│   │  HashSet:                                           │  │
│   │  ┌─────────────────────────────────────────────┐   │  │
│   │  │         HashMap<E, Object>                   │   │  │
│   │  │  Key = 元素                                  │   │  │
│   │  │  Value = PRESENT (固定占位符)                │   │  │
│   │  └─────────────────────────────────────────────┘   │  │
│   │                                                     │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 详细对比

```
┌──────────────────┬─────────────────────┬─────────────────────┐
│   特性           │   HashSet           │   HashMap           │
├──────────────────┼─────────────────────┼─────────────────────┤
│   实现接口       │   Set               │   Map               │
│   存储方式       │   对象 (元素)       │   键值对 (K-V)      │
│   添加方法       │   add(E e)          │   put(K k, V v)     │
│   获取方式       │   iterator 遍历     │   get(K key)        │
│   底层实现       │   HashMap           │   数组+链表+红黑树  │
│   null 元素      │   允许 1 个         │   允许 null key     │
│   重复判断       │   equals + hashCode │   equals + hashCode │
│   去重能力       │   自动去重          │   key 去重          │
└──────────────────┴─────────────────────┴─────────────────────┘
```

## 源码解析

```java
// HashSet 内部实现
public class HashSet<E> extends AbstractSet<E> implements Set<E> {
    
    // 底层使用 HashMap
    private transient HashMap<E, Object> map;
    
    // 占位值，所有 value 都用它
    private static final Object PRESENT = new Object();
    
    // 构造方法
    public HashSet() {
        map = new HashMap<>();
    }
    
    // add 实际调用 HashMap.put
    public boolean add(E e) {
        return map.put(e, PRESENT) == null;  // 元素作为 key
    }
    
    // remove 调用 HashMap.remove
    public boolean remove(Object o) {
        return map.remove(o) == PRESENT;
    }
    
    // contains 调用 HashMap.containsKey
    public boolean contains(Object o) {
        return map.containsKey(o);
    }
    
    // size
    public int size() {
        return map.size();
    }
    
    // iterator 返回 keySet 的迭代器
    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }
}
```

## 使用示例

```java
// HashSet - 存储单个对象
Set<String> set = new HashSet<>();
set.add("apple");
set.add("banana");
set.add("apple");  // 重复，不会添加
System.out.println(set.size());  // 2
System.out.println(set.contains("apple"));  // true

// HashMap - 存储键值对
Map<String, Integer> map = new HashMap<>();
map.put("apple", 1);
map.put("banana", 2);
map.put("apple", 3);  // 覆盖旧值
System.out.println(map.size());  // 2
System.out.println(map.get("apple"));  // 3
```

## 去重原理

```java
// HashSet 去重依赖 hashCode() 和 equals()
class User {
    String id;
    String name;
    
    // 必须正确重写
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }
}

Set<User> users = new HashSet<>();
users.add(new User("1", "Alice"));
users.add(new User("1", "Bob"));  // id 相同，不会添加
System.out.println(users.size());  // 1
```

## 选择场景

```
┌─────────────────────────────────────────────────────────────┐
│                    使用场景                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   使用 HashSet:                                              │
│   • 只需要存储元素，不需要关联值                            │
│   • 需要快速判断元素是否存在                                │
│   • 需要去重                                                │
│   • 集合运算 (交集、并集、差集)                             │
│                                                             │
│   使用 HashMap:                                              │
│   • 需要键值映射关系                                        │
│   • 需要根据 key 快速获取 value                             │
│   • 缓存场景                                                │
│   • 计数统计                                                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 代码示例

```java
// HashSet 场景: 去重
List<String> listWithDuplicates = Arrays.asList("a", "b", "a", "c", "b");
Set<String> uniqueSet = new HashSet<>(listWithDuplicates);
// 结果: [a, b, c]

// HashSet 场景: 快速查找
Set<String> blacklist = new HashSet<>();
blacklist.add("banned_user1");
blacklist.add("banned_user2");
if (blacklist.contains(userId)) {
    // 被封禁
}

// HashMap 场景: 计数
Map<String, Integer> wordCount = new HashMap<>();
for (String word : words) {
    wordCount.merge(word, 1, Integer::sum);
}

// HashMap 场景: 缓存
Map<Long, User> userCache = new HashMap<>();
userCache.put(userId, user);
User cached = userCache.get(userId);
```

## 面试回答

### 30秒版本

> **HashSet** 存储单个对象，**HashMap** 存储键值对。HashSet 底层就是 HashMap，元素作为 key，value 是固定占位对象 PRESENT。HashSet 用于去重和快速查找，HashMap 用于键值映射。两者都依赖 hashCode() 和 equals() 判断重复。

### 1分钟版本

> **关系**：HashSet 底层就是 HashMap
> - 元素存入 HashMap 的 key
> - value 是固定的 PRESENT 对象
>
> **区别**：
> - 接口：Set vs Map
> - 存储：单元素 vs 键值对
> - 方法：add vs put/get
>
> **共同点**：
> - 都用 hashCode + equals 判断重复
> - 都允许 null
> - 都是非线程安全
>
> **使用场景**：
> - HashSet：去重、快速查找
> - HashMap：键值映射、缓存

---

*关联文档：[hashmap-principle.md](hashmap-principle.md) | [java-collections.md](../00-java-basics/java-collections.md)*
