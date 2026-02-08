# 什么是 Java 泛型的上下界限定符？

## 上下界概念

```
┌─────────────────────────────────────────────────────────────┐
│                    泛型上下界限定符                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   上界通配符 (Upper Bound):                                  │
│   <? extends T>  表示 T 或 T 的子类                         │
│                                                             │
│   下界通配符 (Lower Bound):                                  │
│   <? super T>    表示 T 或 T 的父类                         │
│                                                             │
│   无界通配符:                                                │
│   <?>            表示任意类型                               │
│                                                             │
│   PECS 原则:                                                 │
│   Producer Extends, Consumer Super                          │
│   生产者用 extends，消费者用 super                          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 上界通配符 extends

```java
// 类型层次: Object > Number > Integer/Double
//                          > Long

// 上界: 只能读取，不能写入 (协变)
List<? extends Number> list = new ArrayList<Integer>();

// ✅ 可以读取 (作为 Number)
Number num = list.get(0);
Object obj = list.get(0);

// ❌ 不能写入 (除了 null)
list.add(1);        // 编译错误!
list.add(1.0);      // 编译错误!
list.add(null);     // OK (null 可以)

// 原因: 编译器不知道具体是 Integer 还是 Double 的 List
// 如果是 List<Integer>，放 Double 就错了
```

```
┌─────────────────────────────────────────────────────────────┐
│                    extends 示意图                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│           ┌─────────┐                                       │
│           │  Number │  ← 上界                               │
│           └────┬────┘                                       │
│         ┌──────┴──────┐                                     │
│         │             │                                     │
│   ┌─────────┐   ┌─────────┐                                │
│   │ Integer │   │ Double  │                                 │
│   └─────────┘   └─────────┘                                │
│                                                             │
│   List<? extends Number> 可以是:                            │
│   • List<Number>                                            │
│   • List<Integer>                                           │
│   • List<Double>                                            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 下界通配符 super

```java
// 下界: 只能写入，读取只能是 Object (逆变)
List<? super Integer> list = new ArrayList<Number>();

// ✅ 可以写入 Integer 及其子类
list.add(1);        // OK
list.add(123);      // OK

// ❌ 读取只能是 Object
Object obj = list.get(0);  // OK
Integer num = list.get(0); // 编译错误!

// 原因: 只知道是 Integer 的父类，但不知道具体是哪个
// 可能是 List<Number> 或 List<Object>
```

```
┌─────────────────────────────────────────────────────────────┐
│                    super 示意图                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│           ┌─────────┐                                       │
│           │  Object │                                       │
│           └────┬────┘                                       │
│                │                                            │
│           ┌────┴────┐                                       │
│           │  Number │                                       │
│           └────┬────┘                                       │
│                │                                            │
│           ┌────┴────┐                                       │
│           │ Integer │  ← 下界                               │
│           └─────────┘                                       │
│                                                             │
│   List<? super Integer> 可以是:                             │
│   • List<Integer>                                           │
│   • List<Number>                                            │
│   • List<Object>                                            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## PECS 原则

```java
// PECS: Producer Extends, Consumer Super

// Producer (生产者): 只读取数据，用 extends
public void printNumbers(List<? extends Number> list) {
    for (Number n : list) {  // 只读
        System.out.println(n);
    }
}

// Consumer (消费者): 只写入数据，用 super
public void addIntegers(List<? super Integer> list) {
    list.add(1);  // 只写
    list.add(2);
}

// 实际应用: Collections.copy
public static <T> void copy(List<? super T> dest,    // 消费者
                            List<? extends T> src) { // 生产者
    for (T item : src) {
        dest.add(item);
    }
}
```

## 对比总结

```
┌─────────────────────────────────────────────────────────────┐
│                    通配符对比                                │
├──────────────────┬──────────────────┬───────────────────────┤
│   通配符         │   可读           │   可写               │
├──────────────────┼──────────────────┼───────────────────────┤
│ <? extends T>    │ ✅ 作为 T       │ ❌ 只能写 null       │
│ <? super T>      │ ❌ 只能读 Object │ ✅ 写 T 及其子类     │
│ <?>              │ ❌ 只能读 Object │ ❌ 只能写 null       │
│ <T>              │ ✅ 作为 T       │ ✅ 写 T              │
├──────────────────┴──────────────────┴───────────────────────┤
│   记忆口诀:                                                  │
│   extends 读取 (生产者) | super 写入 (消费者)               │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> **上界 `<? extends T>`**：T 或 T 的子类，只能读取不能写入，用于生产者场景。**下界 `<? super T>`**：T 或 T 的父类，只能写入不能读取，用于消费者场景。**PECS 原则**：Producer Extends, Consumer Super。如 `Collections.copy(dest, src)`，dest 用 super，src 用 extends。

### 1分钟版本

> **上界 extends**：
> - `<? extends Number>` = Number 或子类
> - 只能读取（作为 Number）
> - 不能写入（不知道具体类型）
> - 用于生产者（提供数据）
>
> **下界 super**：
> - `<? super Integer>` = Integer 或父类
> - 只能写入（Integer 及子类）
> - 读取只能是 Object
> - 用于消费者（接收数据）
>
> **PECS 原则**：
> - Producer Extends
> - Consumer Super
>
> **典型应用**：
> - `Collections.copy(List<? super T> dest, List<? extends T> src)`

---

*关联文档：[generics-intro.md](generics-intro.md) | [generics-erasure.md](generics-erasure.md)*
