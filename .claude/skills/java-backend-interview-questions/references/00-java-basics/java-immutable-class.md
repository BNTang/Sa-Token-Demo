# 什么是 Java 中的不可变类？

## 不可变类定义

```
┌─────────────────────────────────────────────────────────────┐
│                    不可变类 (Immutable Class)                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   定义: 对象一旦创建，其状态（属性值）就不能被修改          │
│                                                             │
│   JDK 中的不可变类:                                          │
│   ├── String                                                │
│   ├── Integer/Long/Double 等包装类                          │
│   ├── BigInteger / BigDecimal                               │
│   ├── LocalDate / LocalDateTime (Java 8)                    │
│   └── Optional                                              │
│                                                             │
│   特点:                                                      │
│   • 线程安全 (无需同步)                                     │
│   • 可以被安全共享和缓存                                    │
│   • 适合作为 Map 的 key                                     │
│   • 适合作为常量                                            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 如何创建不可变类

```
┌─────────────────────────────────────────────────────────────┐
│                    创建不可变类的规则                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 类用 final 修饰，防止被继承                            │
│   2. 所有字段用 private final 修饰                          │
│   3. 不提供 setter 方法                                     │
│   4. 通过构造方法初始化所有字段                             │
│   5. 对可变对象字段进行深拷贝 (防御性拷贝)                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

```java
// 标准的不可变类
public final class ImmutableUser {
    private final String name;
    private final int age;
    private final List<String> hobbies;  // 可变对象
    
    public ImmutableUser(String name, int age, List<String> hobbies) {
        this.name = name;
        this.age = age;
        // 深拷贝：防止外部修改影响内部状态
        this.hobbies = new ArrayList<>(hobbies);
    }
    
    public String getName() {
        return name;
    }
    
    public int getAge() {
        return age;
    }
    
    public List<String> getHobbies() {
        // 返回副本：防止外部修改
        return new ArrayList<>(hobbies);
        // 或者返回不可修改视图
        // return Collections.unmodifiableList(hobbies);
    }
    
    // 不提供 setter 方法
    
    // 如需修改，返回新对象
    public ImmutableUser withAge(int newAge) {
        return new ImmutableUser(this.name, newAge, this.hobbies);
    }
}
```

## 防御性拷贝的重要性

```java
// ❌ 错误示例: 未进行防御性拷贝
public final class BadImmutable {
    private final List<String> items;
    
    public BadImmutable(List<String> items) {
        this.items = items;  // 直接引用
    }
    
    public List<String> getItems() {
        return items;  // 直接返回
    }
}

// 问题演示
List<String> list = new ArrayList<>();
list.add("A");
BadImmutable obj = new BadImmutable(list);

list.add("B");  // 修改原 list
System.out.println(obj.getItems());  // [A, B] 内部状态被修改了!

obj.getItems().add("C");  // 通过 getter 修改
System.out.println(obj.getItems());  // [A, B, C] 又被修改了!
```

## Java 14+ Record

```java
// Java 14+ Record 自动实现不可变类
public record User(String name, int age) {
    // 自动生成:
    // - private final 字段
    // - 全参构造器
    // - getter (name(), age())
    // - equals, hashCode, toString
}

// 使用
User user = new User("Tom", 18);
String name = user.name();  // getter 方法
// user.name = "Jerry";  // 编译错误，没有 setter

// 注意: 如果 Record 包含可变对象，仍需手动进行防御性拷贝
public record Team(String name, List<String> members) {
    public Team {
        // 紧凑构造器中进行深拷贝
        members = List.copyOf(members);  // 返回不可变 List
    }
}
```

## 不可变类的优缺点

```
┌─────────────────────────────────────────────────────────────┐
│                    优缺点对比                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ✅ 优点:                                                   │
│   • 线程安全，无需同步                                      │
│   • 可以安全共享和缓存                                      │
│   • 作为 Map key 不会出问题                                 │
│   • 简单可靠，状态可预测                                    │
│   • 天然的 hashCode 缓存                                    │
│                                                             │
│   ❌ 缺点:                                                   │
│   • 每次修改都创建新对象                                    │
│   • 频繁修改时性能较差                                      │
│   • 大对象的深拷贝成本高                                    │
│                                                             │
│   适用场景:                                                  │
│   • 值对象 (Value Object)                                   │
│   • DTO (数据传输对象)                                      │
│   • 配置信息                                                │
│   • 常量                                                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## String 不可变原理

```java
public final class String {
    // final 数组，引用不可变
    private final char[] value;  // Java 8
    // private final byte[] value;  // Java 9+
    
    // 不提供修改 value 的方法
    // 所有"修改"方法都返回新 String 对象
    
    public String concat(String str) {
        // 创建新数组，返回新 String
        return new String(...);
    }
    
    public String substring(int beginIndex) {
        // 返回新 String
        return new String(...);
    }
}
```

## 面试回答

### 30秒版本

> 不可变类是对象创建后状态不可修改的类，如 String、Integer、LocalDate。创建规则：**final 类**、**private final 字段**、**无 setter**、**可变对象深拷贝**。优点：线程安全、可安全共享缓存。Java 14+ 可用 **Record** 简化创建。缺点是每次修改创建新对象。

### 1分钟版本

> **定义**：对象状态不可修改
>
> **JDK 示例**：String、Integer、BigDecimal、LocalDate
>
> **创建规则**：
> 1. final 类（防止继承）
> 2. private final 字段
> 3. 无 setter 方法
> 4. 可变对象深拷贝（构造器和 getter）
>
> **优点**：
> - 线程安全
> - 可安全共享缓存
> - 适合做 Map key
>
> **Java 14+ Record**：
> - 自动生成不可变类结构
> - 可变字段仍需深拷贝
>
> **缺点**：
> - 修改需创建新对象
> - 频繁修改性能差

---

*关联文档：[string-buffer-builder.md](string-buffer-builder.md) | [java17-features.md](java17-features.md)*
