# Java 中的 IdentityHashMap 是什么？

> IdentityHashMap 使用 `==` 判断 key 相等性，而非 `equals()` 方法

## 30秒速答

IdentityHashMap 特点：
- **相等性判断**: 用 `==`（引用相等），不用 `equals()`
- **哈希计算**: 用 `System.identityHashCode()`，不用 `hashCode()`
- **应用场景**: 对象图遍历、序列化、缓存对象引用
- **底层结构**: 线性探测法的开放地址表（非链地址法）

## 一分钟详解

### 与 HashMap 的关键区别

```java
// HashMap: 使用 equals() 和 hashCode()
Map<String, Integer> hashMap = new HashMap<>();
String s1 = new String("hello");
String s2 = new String("hello");
hashMap.put(s1, 1);
hashMap.put(s2, 2);
System.out.println(hashMap.size());  // 1 (s1.equals(s2) = true，覆盖)

// IdentityHashMap: 使用 == 和 identityHashCode
Map<String, Integer> identityMap = new IdentityHashMap<>();
identityMap.put(s1, 1);
identityMap.put(s2, 2);
System.out.println(identityMap.size());  // 2 (s1 != s2，不同key)
```

### 相等性判断源码

```java
// HashMap 的 key 比较
if (e.hash == hash && ((k = e.key) == key || key.equals(k)))

// IdentityHashMap 的 key 比较
if (item == key)  // 只用 == 比较引用
```

### 底层结构差异

```
HashMap: 数组 + 链表/红黑树（链地址法）
┌───┬───┬───┬───┐
│ 0 │ 1 │ 2 │ 3 │
└─┬─┴───┴─┬─┴───┘
  ↓       ↓
 K1→V1   K3→V3
  ↓
 K2→V2

IdentityHashMap: 线性探测数组（开放地址法）
┌────┬────┬────┬────┬────┬────┬────┬────┐
│ K1 │ V1 │ K2 │ V2 │null│null│ K3 │ V3 │
└────┴────┴────┴────┴────┴────┴────┴────┘
  key和value相邻存储，冲突时线性探测下一位置
```

### 典型使用场景

```java
// 场景1: 对象图遍历（防止循环引用）
public class ObjectGraphTraverser {
    private IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();
    
    public void traverse(Object obj) {
        if (visited.containsKey(obj)) {
            return;  // 已访问过这个对象（同一引用）
        }
        visited.put(obj, true);
        // 继续遍历对象的字段...
    }
}

// 场景2: 序列化时跟踪对象引用
public class Serializer {
    private IdentityHashMap<Object, Integer> objectIds = new IdentityHashMap<>();
    private int nextId = 0;
    
    public int getObjectId(Object obj) {
        Integer id = objectIds.get(obj);
        if (id == null) {
            id = nextId++;
            objectIds.put(obj, id);
        }
        return id;
    }
}

// 场景3: 代理/装饰器场景
// 需要区分原始对象和代理对象（equals可能相等但引用不同）
IdentityHashMap<Object, Object> proxyMap = new IdentityHashMap<>();
Object original = new MyClass();
Object proxy = createProxy(original);
proxyMap.put(original, proxy);  // original.equals(proxy)可能为true，但这里仍是两个key
```

### 对比总结

| 特性 | HashMap | IdentityHashMap |
|------|---------|-----------------|
| key相等判断 | equals() | == |
| hash计算 | hashCode() | identityHashCode() |
| 底层结构 | 链地址法 | 开放地址法(线性探测) |
| null key | 支持 | 支持 |
| 线程安全 | 否 | 否 |
| 常见场景 | 通用Map | 对象图遍历、序列化 |

### 注意事项

```java
// 字符串字面量共享同一引用
String a = "hello";
String b = "hello";
IdentityHashMap<String, Integer> map = new IdentityHashMap<>();
map.put(a, 1);
map.put(b, 2);
System.out.println(map.size());  // 1！因为 a == b (字符串常量池)

// new 创建的是不同引用
String c = new String("hello");
String d = new String("hello");
map.put(c, 3);
map.put(d, 4);
System.out.println(map.size());  // 3！c != d
```

## 关键记忆点

```
┌─────────────────────────────────────────────────┐
│  IdentityHashMap 速记：                          │
│                                                 │
│  「==」用引用相等，不用 equals()                 │
│  「identity」System.identityHashCode()           │
│  「线性」开放地址法，key-value 相邻存储          │
│  「场景」对象图遍历、序列化、代理映射            │
│                                                 │
│  核心区别：                                      │
│  HashMap  → equals() + hashCode()               │
│  Identity → ==  + identityHashCode()            │
└─────────────────────────────────────────────────┘
```

## 面试加分

**Q: 什么时候用 IdentityHashMap 而不是 HashMap？**

A: 当你需要区分**同一个对象的多个引用**而非**逻辑相等的对象**时：
1. 序列化框架跟踪对象引用
2. 编译器/解释器的符号表
3. 防止循环引用的图遍历
4. 缓存原始对象和代理对象的映射
