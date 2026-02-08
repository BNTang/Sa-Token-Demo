# Java 中的 WeakHashMap 是什么？

> WeakHashMap 的 key 是弱引用，当 key 没有其他强引用时会被 GC 回收，对应 entry 自动移除

## 30秒速答

WeakHashMap 特点：
- **弱引用 Key**: key 被 WeakReference 包装
- **自动清理**: key 无强引用时，entry 自动删除
- **适用场景**: 缓存、监听器管理、对象元数据存储
- **与内存泄漏**: 防止因缓存导致的内存泄漏

## 一分钟详解

### 弱引用与强引用对比

```
强引用 (HashMap):
┌──────────────┐     强引用      ┌─────────┐
│   HashMap    │ ────────────→  │  Key    │ ← 其他强引用可能存在
└──────────────┘                └─────────┘
即使外部不再使用 Key，HashMap 仍持有强引用，Key 不会被 GC

弱引用 (WeakHashMap):
┌──────────────┐     弱引用      ┌─────────┐
│ WeakHashMap  │ ────- - - -→   │  Key    │ ← 无其他强引用
└──────────────┘                └─────────┘
当外部无强引用指向 Key 时，Key 可被 GC，entry 自动清除
```

### 基本使用

```java
// 示例: WeakHashMap 自动清理
WeakHashMap<Object, String> cache = new WeakHashMap<>();

Object key1 = new Object();
Object key2 = new Object();

cache.put(key1, "value1");
cache.put(key2, "value2");
System.out.println(cache.size());  // 2

key1 = null;  // 移除强引用
System.gc();  // 触发 GC
Thread.sleep(100);

System.out.println(cache.size());  // 1 (key1 对应的 entry 被清除)
```

### 与 HashMap 对比

```java
// HashMap: 即使外部不再引用，也不会自动清理
HashMap<Object, String> hashMap = new HashMap<>();
Object key = new Object();
hashMap.put(key, "value");
key = null;
System.gc();
System.out.println(hashMap.size());  // 仍是 1，可能导致内存泄漏

// WeakHashMap: key 无强引用时自动清理
WeakHashMap<Object, String> weakMap = new WeakHashMap<>();
Object key2 = new Object();
weakMap.put(key2, "value");
key2 = null;
System.gc();
Thread.sleep(100);
System.out.println(weakMap.size());  // 变成 0
```

### 实现原理

```java
// WeakHashMap.Entry 继承 WeakReference
private static class Entry<K,V> extends WeakReference<Object> 
    implements Map.Entry<K,V> {
    
    V value;
    final int hash;
    Entry<K,V> next;
    
    Entry(Object key, V value, ReferenceQueue<Object> queue,
          int hash, Entry<K,V> next) {
        super(key, queue);  // key 作为弱引用
        this.value = value;
        this.hash = hash;
        this.next = next;
    }
}

// 清理机制: 每次操作时检查 ReferenceQueue
private void expungeStaleEntries() {
    for (Object x; (x = queue.poll()) != null; ) {
        // 从 map 中移除已被 GC 的 entry
    }
}
```

### 典型使用场景

```java
// 场景1: 缓存（自动过期）
class ImageCache {
    private WeakHashMap<String, Image> cache = new WeakHashMap<>();
    
    public Image getImage(String path) {
        Image image = cache.get(path);
        if (image == null) {
            image = loadImage(path);
            cache.put(path, image);
        }
        return image;
    }
    // 当内存紧张时，未被使用的图片会被 GC
}

// 场景2: 对象元数据存储
class ObjectMetadata {
    // 存储对象的额外信息，对象销毁时自动清理
    private WeakHashMap<Object, Metadata> metadataMap = new WeakHashMap<>();
    
    public void setMetadata(Object obj, Metadata meta) {
        metadataMap.put(obj, meta);
    }
    // 当 obj 被 GC 时，其元数据也会被清理
}

// 场景3: 监听器管理（防止内存泄漏）
class EventBus {
    private WeakHashMap<Object, List<Listener>> listeners = new WeakHashMap<>();
    
    public void register(Object subscriber, Listener listener) {
        listeners.computeIfAbsent(subscriber, k -> new ArrayList<>()).add(listener);
    }
    // subscriber 被销毁时，自动取消注册
}
```

### 注意事项

```java
// ⚠️ 注意1: 字符串字面量不会被回收
WeakHashMap<String, Integer> map = new WeakHashMap<>();
map.put("hello", 1);  // 字符串常量池持有强引用，永不回收
String key = new String("world");
map.put(key, 2);
key = null;
System.gc();
// "hello" 对应的 entry 仍在，"world" 对应的被清理

// ⚠️ 注意2: Value 仍是强引用
// 如果 Value 引用了 Key，会阻止 Key 被回收
class BadExample {
    Object key;  // Value 引用 Key
}
WeakHashMap<Object, BadExample> bad = new WeakHashMap<>();
Object k = new Object();
BadExample v = new BadExample();
v.key = k;  // Value 持有 Key 的强引用
bad.put(k, v);
k = null;
// k 不会被回收，因为 v.key 仍持有强引用！
```

## 关键记忆点

```
┌─────────────────────────────────────────────────┐
│  WeakHashMap 速记：                              │
│                                                 │
│  「弱」key 是 WeakReference                      │
│  「清」key 无强引用时自动清理 entry               │
│  「防」防止缓存导致的内存泄漏                    │
│  「查」每次操作检查 ReferenceQueue               │
│                                                 │
│  适用场景：                                      │
│  ✓ 缓存（允许被 GC）                            │
│  ✓ 对象元数据存储                               │
│  ✓ 监听器注册管理                               │
│                                                 │
│  注意：                                          │
│  ✗ 字符串字面量不会被回收                       │
│  ✗ Value 引用 Key 会阻止回收                    │
└─────────────────────────────────────────────────┘
```

## 面试追问

**Q: WeakHashMap 和 SoftReference 区别？**

| 类型 | 回收时机 | 适用场景 |
|------|---------|---------|
| WeakReference | 下次 GC 即回收 | 对象元数据、监听器 |
| SoftReference | 内存不足时回收 | 缓存（尽量保留） |

**Q: 如何实现 SoftReference 版本的缓存？**

A: 使用 Guava 的 `CacheBuilder.softValues()` 或自己封装 SoftReference + ReferenceQueue
