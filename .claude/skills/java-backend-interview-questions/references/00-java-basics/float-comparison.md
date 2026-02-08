# Float 经过运算后如何判断与另一个数相等？

## 浮点数精度问题

```
┌─────────────────────────────────────────────────────────────┐
│                    浮点数精度问题                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   问题: 浮点数无法精确表示某些十进制数                      │
│                                                             │
│   示例:                                                      │
│   float a = 0.1f + 0.2f;                                    │
│   System.out.println(a);           // 0.30000001           │
│   System.out.println(a == 0.3f);   // false!               │
│                                                             │
│   原因: float/double 使用 IEEE 754 二进制浮点表示           │
│        0.1 的二进制是无限循环小数                           │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 正确比较方法

### 方法1: 误差范围比较 (推荐)

```java
// 定义一个很小的误差值 (epsilon)
private static final float EPSILON = 1e-6f;  // 0.000001

public static boolean equals(float a, float b) {
    return Math.abs(a - b) < EPSILON;
}

// 使用
float result = 0.1f + 0.2f;
boolean isEqual = Math.abs(result - 0.3f) < EPSILON;  // true
```

### 方法2: 相对误差比较 (数值范围大时)

```java
public static boolean relativeEquals(float a, float b, float epsilon) {
    // 处理特殊情况
    if (a == b) return true;
    if (Float.isNaN(a) || Float.isNaN(b)) return false;
    if (Float.isInfinite(a) || Float.isInfinite(b)) return a == b;
    
    // 相对误差
    float diff = Math.abs(a - b);
    float max = Math.max(Math.abs(a), Math.abs(b));
    return diff / max < epsilon;
}
```

### 方法3: BigDecimal (需要精确计算时)

```java
import java.math.BigDecimal;

// 用 String 构造器 (重要!)
BigDecimal a = new BigDecimal("0.1");
BigDecimal b = new BigDecimal("0.2");
BigDecimal c = new BigDecimal("0.3");

BigDecimal result = a.add(b);
boolean isEqual = result.compareTo(c) == 0;  // true

// ❌ 错误: 用 double 构造器
BigDecimal bad = new BigDecimal(0.1);  // 0.1000000000000000055511151...
```

### 方法4: Float.compare() (处理特殊值)

```java
// 处理 NaN 和正负零
float a = 0.1f + 0.2f;
float b = 0.3f;

// 仍然不能直接比较，但可以处理 NaN
if (Float.compare(a, b) == 0) {
    // 完全相等
}

// 最佳: 结合误差范围
public static boolean safeEquals(float a, float b, float epsilon) {
    if (Float.compare(a, b) == 0) return true;  // 处理 NaN/-0.0/+0.0
    return Math.abs(a - b) < epsilon;
}
```

## 完整工具类

```java
public class FloatUtils {
    private static final float FLOAT_EPSILON = 1e-6f;
    private static final double DOUBLE_EPSILON = 1e-9;
    
    /**
     * float 比较
     */
    public static boolean equals(float a, float b) {
        return equals(a, b, FLOAT_EPSILON);
    }
    
    public static boolean equals(float a, float b, float epsilon) {
        // 处理特殊值
        if (Float.isNaN(a) || Float.isNaN(b)) return false;
        if (Float.isInfinite(a) || Float.isInfinite(b)) return a == b;
        
        return Math.abs(a - b) < epsilon;
    }
    
    /**
     * double 比较
     */
    public static boolean equals(double a, double b) {
        return equals(a, b, DOUBLE_EPSILON);
    }
    
    public static boolean equals(double a, double b, double epsilon) {
        if (Double.isNaN(a) || Double.isNaN(b)) return false;
        if (Double.isInfinite(a) || Double.isInfinite(b)) return a == b;
        
        return Math.abs(a - b) < epsilon;
    }
    
    /**
     * 大于等于
     */
    public static boolean greaterOrEqual(float a, float b, float epsilon) {
        return a > b - epsilon;
    }
    
    /**
     * 小于等于
     */
    public static boolean lessOrEqual(float a, float b, float epsilon) {
        return a < b + epsilon;
    }
}
```

## 选择建议

```
┌─────────────────────────────────────────────────────────────┐
│                    方法选择建议                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   场景                           推荐方法                   │
│   ────────────                   ──────────                 │
│   普通业务计算                   误差范围比较 (epsilon)     │
│   金融/货币计算                  BigDecimal                 │
│   科学计算                       相对误差比较               │
│   需要精确相等                   避免浮点数，用整数         │
│                                                             │
│   epsilon 选择:                                              │
│   • float:  1e-6 到 1e-7                                    │
│   • double: 1e-9 到 1e-15                                   │
│                                                             │
│   ⚠️ 禁止:                                                   │
│   • 直接使用 == 比较浮点数                                  │
│   • 使用 BigDecimal(double) 构造器                          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> 浮点数因 IEEE 754 二进制表示无法精确存储某些十进制数，所以**不能用 == 直接比较**。正确方法：**误差范围比较** `Math.abs(a - b) < epsilon`，epsilon 通常取 1e-6f。金融计算用 **BigDecimal**（必须用 String 构造器）。也可用 Float.compare() 处理 NaN 等特殊值。

### 1分钟版本

> **问题**：0.1 + 0.2 ≠ 0.3（二进制无法精确表示）
>
> **方法**：
> 1. **误差比较**：`Math.abs(a - b) < epsilon`
>    - float 用 1e-6f
>    - double 用 1e-9
>
> 2. **BigDecimal**：金融计算
>    - 必须用 String 构造器
>    - 用 compareTo() 比较
>
> 3. **相对误差**：数值范围大时使用
>
> **禁止**：
> - 直接 == 比较
> - new BigDecimal(double)

---

*关联文档：[bigdecimal.md](bigdecimal.md)*
