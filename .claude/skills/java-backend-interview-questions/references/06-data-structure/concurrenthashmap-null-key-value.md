# 为什么 ConcurrentHashMap 不支持 null key 或 value？

> 在并发环境下，null 会导致歧义：无法区分"key不存在"还是"key存在但value为null"

## 30秒速答

ConcurrentHashMap 不允许 null key/value 的原因：
1. **歧义问题**: `get(key) == null` 无法区分 key 不存在还是 value 本身是 null
2. **并发检查困难**: `containsKey()` + `get()` 不是原子操作，两者之间可能被修改
3. **设计哲学**: Doug Lea 认为并发容器允许 null 是错误的设计

## 一分钟详解

### 歧义问题演示

```java
// HashMap 允许 null，但存在歧义
HashMap<String, String> hashMap = new HashMap<>();
hashMap.put("key1", null);  // value 为 null
hashMap.put(null, "value"); // key 为 null

String value = hashMap.get("key1");  // 返回 null
String value2 = hashMap.get("key2"); // 也返回 null

// 问题：如何区分这两种情况？
// 需要额外调用 containsKey() 判断

// ConcurrentHashMap 的问题更严重
ConcurrentHashMap<String, String> concurrentMap = new ConcurrentHashMap<>();
// concurrentMap.put("key", null);  // 抛出 NullPointerException
```

### 并发环境下的问题

```java
// 假设 ConcurrentHashMap 允许 null value

// Thread A
if (map.containsKey(key)) {    // 时刻1: 返回 true
    // 此时 Thread B 删除了 key
    String v = map.get(key);   // 时刻2: 返回 null
    v.length();                // NPE!
}

// 问题分析：
// containsKey() 和 get() 之间不是原子的
// 在并发环境下，null 的语义是模糊的
```

### 源码验证

```java
// ConcurrentHashMap.putVal() 方法
final V putVal(K key, V value, boolean onlyIfAbsent) {
    if (key == null || value == null) throw new NullPointerException();
    // ...
}

// ConcurrentHashMap.containsValue() 方法
public boolean containsValue(Object value) {
    if (value == null) throw new NullPointerException();
    // ...
}
```

### Doug Lea 的解释

```
引用自 Doug Lea（ConcurrentHashMap 作者）：

"The main reason that nulls aren't allowed in ConcurrentMaps 
 (ConcurrentHashMaps, ConcurrentSkipListMaps) is that 
 ambiguities that may be just barely tolerable in non-concurrent 
 maps can't be accommodated."

翻译：
不允许 null 的主要原因是：
在非并发 Map 中勉强可以容忍的歧义，
在并发 Map 中无法处理。
```

### 如何解决需要 null 的场景？

```java
// 方案1: 使用特殊占位符
public static final String NULL_VALUE = new String("NULL_PLACEHOLDER");

ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
map.put("key", NULL_VALUE);  // 代替 null

String value = map.get("key");
if (value == NULL_VALUE) {
    // 原本是 null
}

// 方案2: 使用 Optional（Java 8+）
ConcurrentHashMap<String, Optional<String>> optionalMap = new ConcurrentHashMap<>();
optionalMap.put("key", Optional.empty());  // 表示 null
optionalMap.put("key2", Optional.of("value"));

Optional<String> opt = optionalMap.get("key");
if (opt != null && !opt.isPresent()) {
    // key 存在但 value 为 null
}

// 方案3: 使用 computeIfAbsent 避免二次检查
map.computeIfAbsent("key", k -> computeValue(k));  // 原子操作
```

### 各 Map 对 null 的支持

| Map 类型 | null key | null value | 原因 |
|---------|----------|------------|------|
| HashMap | ✅ 允许 | ✅ 允许 | 单线程，可用 containsKey 区分 |
| Hashtable | ❌ 禁止 | ❌ 禁止 | 古老设计，强制非空 |
| ConcurrentHashMap | ❌ 禁止 | ❌ 禁止 | 并发歧义 |
| TreeMap | ❌ 禁止 | ✅ 允许 | key 需要比较，null 无法比较 |
| LinkedHashMap | ✅ 允许 | ✅ 允许 | 继承 HashMap |

## 关键记忆点

```
┌─────────────────────────────────────────────────────┐
│  ConcurrentHashMap 禁止 null 的三个原因：           │
│                                                     │
│  1. 「歧义」get()=null 是不存在还是value本身null？   │
│                                                     │
│  2. 「非原子」containsKey() + get() 不是原子操作    │
│      两者之间可能被其他线程修改                      │
│                                                     │
│  3. 「设计」Doug Lea: 并发容器不应容忍 null 歧义    │
│                                                     │
│  解决方案：                                         │
│  - 使用占位符对象代替 null                          │
│  - 使用 Optional 包装                               │
│  - 使用 computeIfAbsent 等原子操作                  │
└─────────────────────────────────────────────────────┘
```

## 面试追问

**Q: HashMap 为什么允许 null？**

A: HashMap 是单线程设计，可以通过 `containsKey()` 来区分：
```java
if (map.containsKey(key)) {
    String v = map.get(key);  // 单线程下两行之间不会被修改
    // v 可能是 null，但确定 key 存在
}
```

**Q: TreeMap 为什么允许 null value 但禁止 null key？**

A: TreeMap 基于红黑树，需要对 key 进行比较排序。`null` 无法与其他对象比较（会抛 NPE），所以禁止 null key。但 value 不参与比较，可以为 null。
