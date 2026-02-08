# Java 中 hashCode 和 equals 方法是什么？与 == 有什么区别？

## 三者概念

```
┌─────────────────────────────────────────────────────────────┐
│                == vs equals() vs hashCode()                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   == 操作符:                                                 │
│   ├── 基本类型: 比较值是否相等                              │
│   └── 引用类型: 比较内存地址是否相同                        │
│                                                             │
│   equals() 方法:                                             │
│   ├── Object 默认实现: 等同于 ==                            │
│   ├── 重写后: 比较对象内容是否相等                          │
│   └── String/Integer 等已重写                               │
│                                                             │
│   hashCode() 方法:                                           │
│   ├── 返回对象的哈希码 (int)                                │
│   ├── 用于 HashMap/HashSet 等哈希表                         │
│   └── 与 equals 必须保持一致性                              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 代码示例

```java
// == 比较
String s1 = new String("hello");
String s2 = new String("hello");
String s3 = "hello";
String s4 = "hello";

System.out.println(s1 == s2);      // false (不同对象)
System.out.println(s3 == s4);      // true  (常量池同一对象)
System.out.println(s1 == s3);      // false (堆对象 vs 常量池)

// equals 比较
System.out.println(s1.equals(s2)); // true  (内容相同)
System.out.println(s1.equals(s3)); // true  (内容相同)

// hashCode
System.out.println(s1.hashCode()); // 相同
System.out.println(s2.hashCode()); // 相同 (内容相同，hashCode相同)
```

## equals 和 hashCode 的约定

```
┌─────────────────────────────────────────────────────────────┐
│                    equals 和 hashCode 约定                   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. equals 相等 → hashCode 必须相等                        │
│      ┌─────────────────────────────────────────────────┐   │
│      │  if (a.equals(b)) {                              │   │
│      │      assert a.hashCode() == b.hashCode();  // 必须│   │
│      │  }                                               │   │
│      └─────────────────────────────────────────────────┘   │
│                                                             │
│   2. hashCode 相等 → equals 不一定相等 (哈希冲突)           │
│      ┌─────────────────────────────────────────────────┐   │
│      │  a.hashCode() == b.hashCode()                    │   │
│      │  // 不能推出 a.equals(b)                         │   │
│      └─────────────────────────────────────────────────┘   │
│                                                             │
│   3. 重写 equals 必须同时重写 hashCode                      │
│      否则 HashMap/HashSet 无法正常工作                      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 正确重写示例

```java
public class User {
    private String name;
    private int age;
    
    @Override
    public boolean equals(Object o) {
        // 1. 同一对象
        if (this == o) return true;
        // 2. null 或类型不同
        if (o == null || getClass() != o.getClass()) return false;
        // 3. 比较属性
        User user = (User) o;
        return age == user.age && Objects.equals(name, user.name);
    }
    
    @Override
    public int hashCode() {
        // 必须和 equals 使用相同的属性
        return Objects.hash(name, age);
    }
}
```

## 在 HashMap 中的应用

```
┌─────────────────────────────────────────────────────────────┐
│                    HashMap 查找流程                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   put(key, value):                                          │
│   1. 计算 key.hashCode()                                    │
│   2. 定位桶位置: index = hash & (length - 1)                │
│   3. 遍历桶中元素，用 equals 判断 key 是否存在              │
│   4. 存在则更新，不存在则插入                               │
│                                                             │
│   如果只重写 equals 不重写 hashCode:                        │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  User u1 = new User("Tom", 18);                      │  │
│   │  User u2 = new User("Tom", 18);                      │  │
│   │  u1.equals(u2) == true  // 逻辑相等                  │  │
│   │  u1.hashCode() != u2.hashCode()  // 默认不同!        │  │
│   │                                                     │  │
│   │  map.put(u1, "value1");                              │  │
│   │  map.get(u2);  // null! 因为 hashCode 不同           │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 对比总结

```
┌─────────────────────────────────────────────────────────────┐
│                    三者对比                                  │
├──────────────┬──────────────────────────────────────────────┤
│   比较方式   │   说明                                       │
├──────────────┼──────────────────────────────────────────────┤
│   ==         │ 基本类型比值，引用类型比地址                 │
│   equals()   │ 比较内容是否相等（需重写）                   │
│   hashCode() │ 返回哈希码，用于哈希表定位                   │
├──────────────┴──────────────────────────────────────────────┤
│   重要规则:                                                  │
│   • 重写 equals 必须重写 hashCode                           │
│   • equals 相等 → hashCode 必须相等                         │
│   • hashCode 相等 → equals 不一定相等                       │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> **==** 基本类型比值，引用类型比内存地址。**equals()** 默认等同于 ==，重写后比较对象内容。**hashCode()** 返回哈希码用于 HashMap 定位。重要约定：**重写 equals 必须重写 hashCode**，equals 相等则 hashCode 必须相等，否则 HashMap/HashSet 无法正常工作。

### 1分钟版本

> **== 操作符**：
> - 基本类型：比较值
> - 引用类型：比较内存地址
>
> **equals()**：
> - Object 默认实现等同于 ==
> - 重写后比较对象内容
> - String/Integer 已重写
>
> **hashCode()**：
> - 返回对象哈希码
> - 用于 HashMap/HashSet 定位桶
>
> **约定**：
> - 重写 equals 必须重写 hashCode
> - equals 相等 → hashCode 必须相等
> - hashCode 相等不代表 equals 相等
>
> **不遵守约定后果**：
> - HashMap 无法正确查找
> - HashSet 无法正确去重

---

*关联文档：[hashmap-resize.md](../06-data-structure/hashmap-resize.md)*
