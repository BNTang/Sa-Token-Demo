# 什么是 Java 的 BigDecimal？为什么能保证精度？

## BigDecimal 概述

```
┌─────────────────────────────────────────────────────────────┐
│                    BigDecimal 概述                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   定义: 任意精度的十进制数，用于高精度计算                  │
│                                                             │
│   为什么需要:                                                │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  double d = 0.1 + 0.2;                               │  │
│   │  System.out.println(d);  // 0.30000000000000004 ❌   │  │
│   │                                                     │  │
│   │  BigDecimal b1 = new BigDecimal("0.1");              │  │
│   │  BigDecimal b2 = new BigDecimal("0.2");              │  │
│   │  System.out.println(b1.add(b2));  // 0.3 ✅          │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   应用场景:                                                  │
│   • 金融系统 (货币计算)                                     │
│   • 精确科学计算                                            │
│   • 需要控制精度和舍入的场景                                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 为什么 double 不精确

```
┌─────────────────────────────────────────────────────────────┐
│                    double 精度问题                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   IEEE 754 浮点数表示:                                       │
│   • double 使用 64 位二进制存储                             │
│   • 符号位(1) + 指数位(11) + 尾数位(52)                     │
│                                                             │
│   问题: 很多十进制小数无法用二进制精确表示                  │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  0.1 (十进制) = 0.0001100110011... (二进制，无限循环)│  │
│   │  存储时必须截断，导致精度丢失                        │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   类比: 1/3 无法用有限十进制小数表示                        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## BigDecimal 内部结构

```java
public class BigDecimal extends Number {
    // 核心字段
    private final BigInteger intVal;   // 无缩放值 (整数部分)
    private final int scale;           // 小数位数
    private transient int precision;   // 精度
    
    // 123.45 的存储方式:
    // intVal = 12345
    // scale = 2
    // 实际值 = intVal / 10^scale = 12345 / 100 = 123.45
}
```

```
┌─────────────────────────────────────────────────────────────┐
│                    BigDecimal 存储原理                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   值 = 无缩放值 (unscaledValue) / 10^scale                  │
│                                                             │
│   ┌────────────────────────────────────────────────────┐   │
│   │  BigDecimal("123.456")                              │   │
│   │  • unscaledValue = 123456                           │   │
│   │  • scale = 3                                        │   │
│   │  • 值 = 123456 / 10³ = 123.456                      │   │
│   └────────────────────────────────────────────────────┘   │
│                                                             │
│   BigInteger 可以表示任意大的整数                           │
│   → BigDecimal 可以精确表示任意精度的小数                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 正确使用方式

```java
// ✅ 正确: 使用 String 构造
BigDecimal b1 = new BigDecimal("0.1");

// ❌ 错误: 使用 double 构造 (精度已丢失)
BigDecimal b2 = new BigDecimal(0.1);
System.out.println(b2);  // 0.1000000000000000055511151231257827021181583404541015625

// ✅ 正确: 使用 valueOf (内部转 String)
BigDecimal b3 = BigDecimal.valueOf(0.1);

// 四则运算
BigDecimal a = new BigDecimal("10");
BigDecimal b = new BigDecimal("3");

a.add(b);        // 加法
a.subtract(b);   // 减法
a.multiply(b);   // 乘法
a.divide(b, 2, RoundingMode.HALF_UP);  // 除法，保留2位小数，四舍五入
```

## 舍入模式

```
┌─────────────────────────────────────────────────────────────┐
│                    舍入模式 (RoundingMode)                   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   HALF_UP      四舍五入 (常用)                              │
│   HALF_DOWN    五舍六入                                     │
│   HALF_EVEN    银行家舍入 (四舍六入五成双)                  │
│   UP           远离零方向舍入                               │
│   DOWN         向零方向舍入 (截断)                          │
│   CEILING      向正无穷方向舍入                             │
│   FLOOR        向负无穷方向舍入                             │
│                                                             │
│   示例 (保留1位小数):                                        │
│   值        HALF_UP   HALF_DOWN  HALF_EVEN  DOWN            │
│   2.55      2.6       2.5        2.6        2.5             │
│   2.45      2.5       2.4        2.4        2.4             │
│   -2.55     -2.6      -2.5       -2.6       -2.5            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 常见陷阱

```java
// 陷阱 1: 除法必须指定精度
BigDecimal a = new BigDecimal("10");
BigDecimal b = new BigDecimal("3");
a.divide(b);  // ArithmeticException: 除不尽!
// 正确: a.divide(b, 2, RoundingMode.HALF_UP);

// 陷阱 2: 使用 equals 比较
new BigDecimal("1.0").equals(new BigDecimal("1.00"));  // false!
// equals 比较 value 和 scale
// 正确: 使用 compareTo
new BigDecimal("1.0").compareTo(new BigDecimal("1.00")) == 0;  // true

// 陷阱 3: 使用 double 构造
new BigDecimal(0.1);  // 精度已丢失
// 正确: new BigDecimal("0.1");
```

## 面试回答

### 30秒版本

> **BigDecimal** 用于高精度计算，解决 double 的精度问题（二进制无法精确表示某些十进制小数）。内部通过 **BigInteger + scale** 存储：值 = 无缩放整数 / 10^scale。使用时必须用 **String 构造器**，除法要指定精度和舍入模式，比较用 **compareTo** 而非 equals。

### 1分钟版本

> **为什么需要**：
> - double 用二进制存储，无法精确表示 0.1 等
> - 金融系统需要精确计算
>
> **内部原理**：
> - unscaledValue (BigInteger) + scale
> - 值 = unscaledValue / 10^scale
> - BigInteger 可表示任意大整数
>
> **正确使用**：
> - 用 String 构造器
> - 除法指定精度和舍入模式
> - compareTo 比较值
>
> **常见陷阱**：
> - new BigDecimal(0.1) 精度丢失
> - divide 不指定精度会异常
> - equals 比较 scale，用 compareTo

---

*关联文档：[primitive-vs-wrapper.md](primitive-vs-wrapper.md) | [java-immutable-class.md](java-immutable-class.md)*
