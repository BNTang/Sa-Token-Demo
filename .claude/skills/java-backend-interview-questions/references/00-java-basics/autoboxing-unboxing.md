# 什么是 Java 中的自动装箱和拆箱？

## 概念

```
┌─────────────────────────────────────────────────────────────┐
│                    自动装箱与拆箱                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   自动装箱 (Autoboxing):                                     │
│   基本类型 → 包装类型                                       │
│   int → Integer                                             │
│                                                             │
│   自动拆箱 (Unboxing):                                       │
│   包装类型 → 基本类型                                       │
│   Integer → int                                             │
│                                                             │
│   引入版本: Java 5                                          │
│                                                             │
│   ┌───────────┐   装箱   ┌───────────┐                     │
│   │    int    │ ───────→ │  Integer  │                     │
│   │   基本类型 │ ←─────── │  包装类型  │                     │
│   └───────────┘   拆箱   └───────────┘                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 代码示例

```java
// 自动装箱
Integer a = 100;           // 等同于 Integer.valueOf(100)
Long b = 100L;             // 等同于 Long.valueOf(100L)
Double c = 3.14;           // 等同于 Double.valueOf(3.14)

// 自动拆箱
int x = a;                 // 等同于 a.intValue()
long y = b;                // 等同于 b.longValue()
double z = c;              // 等同于 c.doubleValue()

// 混合运算时自动拆箱
Integer num = 100;
int result = num + 50;     // num 先拆箱再运算

// 集合中的装箱
List<Integer> list = new ArrayList<>();
list.add(1);               // 自动装箱
list.add(2);
int sum = list.get(0) + list.get(1);  // 自动拆箱
```

## 底层实现

```java
// 编译器自动转换

// 装箱: 调用 valueOf()
Integer a = 100;
// 编译后: Integer a = Integer.valueOf(100);

// 拆箱: 调用 xxxValue()
int x = a;
// 编译后: int x = a.intValue();
```

## 常见陷阱

### 1. 缓存陷阱

```java
Integer a = 127;
Integer b = 127;
System.out.println(a == b);  // true (缓存范围内)

Integer c = 128;
Integer d = 128;
System.out.println(c == d);  // false (超出缓存)

// 原因: Integer.valueOf() 对 -128~127 有缓存
// 应使用 equals() 比较
System.out.println(c.equals(d));  // true
```

### 2. NPE 陷阱

```java
Integer num = null;
int value = num;  // NullPointerException!

// 拆箱时如果包装类为 null，会抛 NPE
// 安全写法
int value = (num != null) ? num : 0;
// 或 Java 8+
int value = Optional.ofNullable(num).orElse(0);
```

### 3. 性能陷阱

```java
// ❌ 糟糕: 循环中频繁装箱
Long sum = 0L;
for (int i = 0; i < 1000000; i++) {
    sum += i;  // 每次都拆箱再装箱
}

// ✅ 正确: 使用基本类型
long sum = 0L;
for (int i = 0; i < 1000000; i++) {
    sum += i;
}
```

### 4. == 比较陷阱

```java
// 基本类型与包装类型比较
int a = 100;
Integer b = 100;
System.out.println(a == b);  // true (b 拆箱后比较值)

// 两个包装类型比较
Integer c = 100;
Integer d = 100;
System.out.println(c == d);  // true (缓存)

Integer e = 200;
Integer f = 200;
System.out.println(e == f);  // false (不同对象)
```

## 最佳实践

```
┌─────────────────────────────────────────────────────────────┐
│                    最佳实践                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ✅ 推荐:                                                   │
│   • 优先使用基本类型 (性能好)                               │
│   • 包装类型比较用 equals()                                 │
│   • 注意 null 检查防止 NPE                                  │
│   • 循环中避免频繁装拆箱                                    │
│                                                             │
│   ❌ 避免:                                                   │
│   • 不要用 == 比较包装类型                                  │
│   • 不要在循环中使用包装类型累加                            │
│   • 不要假设包装类型非 null                                 │
│                                                             │
│   何时用包装类型:                                            │
│   • 集合元素 (List<Integer>)                                │
│   • 允许 null 的场景                                        │
│   • 泛型参数                                                │
│   • 调用需要 Object 的方法                                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> **自动装箱**是基本类型→包装类型，底层调用 `valueOf()`；**自动拆箱**是包装类型→基本类型，调用 `xxxValue()`。常见陷阱：**缓存**（-128~127 用 == 是 true，超出是 false）、**NPE**（null 拆箱抛异常）、**性能**（循环中频繁装拆箱）。建议用基本类型，比较用 equals()。

### 1分钟版本

> **装箱**：基本类型 → 包装类型，调用 valueOf()
> **拆箱**：包装类型 → 基本类型，调用 intValue() 等
>
> **陷阱**：
> - 缓存：-128~127 用 == 是 true
> - NPE：null 拆箱抛异常
> - 性能：循环装拆箱性能差
> - ==：比较对象地址不是值
>
> **最佳实践**：
> - 优先用基本类型
> - 比较用 equals()
> - 检查 null 防 NPE
> - 循环用基本类型

---

*关联文档：[primitive-vs-wrapper.md](primitive-vs-wrapper.md) | [integer-cache.md](integer-cache.md)*
