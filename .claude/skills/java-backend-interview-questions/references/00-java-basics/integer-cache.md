# 什么是 Java 的 Integer 缓存池？

## Integer 缓存池

```
┌─────────────────────────────────────────────────────────────┐
│                    Integer 缓存池 (Cache)                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   定义: Integer 类预先缓存 -128 到 127 之间的对象           │
│                                                             │
│   范围: [-128, 127]                                         │
│                                                             │
│   目的:                                                      │
│   • 减少对象创建                                            │
│   • 节省内存                                                │
│   • 提高性能                                                │
│                                                             │
│   相关类:                                                    │
│   • Byte: -128 ~ 127 (全部缓存)                             │
│   • Short: -128 ~ 127                                       │
│   • Integer: -128 ~ 127 (可调整上界)                        │
│   • Long: -128 ~ 127                                        │
│   • Character: 0 ~ 127                                      │
│   • Boolean: TRUE / FALSE                                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 源码分析

```java
public final class Integer {
    
    // 内部缓存类
    private static class IntegerCache {
        static final int low = -128;
        static final int high;  // 默认 127，可通过 JVM 参数调整
        static final Integer cache[];

        static {
            // 可通过 -XX:AutoBoxCacheMax=<size> 调整上界
            int h = 127;
            String integerCacheHighPropValue = 
                VM.getSavedProperty("java.lang.Integer.IntegerCache.high");
            if (integerCacheHighPropValue != null) {
                int i = parseInt(integerCacheHighPropValue);
                i = Math.max(i, 127);
                h = Math.min(i, Integer.MAX_VALUE - (-low) - 1);
            }
            high = h;

            // 创建缓存数组
            cache = new Integer[(high - low) + 1];
            int j = low;
            for (int k = 0; k < cache.length; k++) {
                cache[k] = new Integer(j++);
            }
        }
    }
    
    // valueOf 方法使用缓存
    public static Integer valueOf(int i) {
        if (i >= IntegerCache.low && i <= IntegerCache.high) {
            return IntegerCache.cache[i + (-IntegerCache.low)];
        }
        return new Integer(i);  // 超出范围创建新对象
    }
}
```

## 示例验证

```java
// 在缓存范围内 [-128, 127]
Integer a = 100;
Integer b = 100;
System.out.println(a == b);  // true (同一对象)

Integer c = Integer.valueOf(100);
System.out.println(a == c);  // true (同一对象)

// 超出缓存范围
Integer d = 128;
Integer e = 128;
System.out.println(d == e);  // false (不同对象!)

// new 永远创建新对象
Integer f = new Integer(100);  // 已废弃
Integer g = new Integer(100);
System.out.println(f == g);  // false (不同对象)
```

## 自动装箱与缓存

```
┌─────────────────────────────────────────────────────────────┐
│                    自动装箱与缓存                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   Integer i = 100;  // 自动装箱                             │
│                                                             │
│   编译后相当于:                                              │
│   Integer i = Integer.valueOf(100);  // 使用缓存            │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  100 在缓存范围内 → 返回缓存对象                     │  │
│   │  128 不在缓存范围 → new Integer(128)                 │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 常见面试陷阱

```java
// 陷阱 1: == 比较
Integer a = 127;
Integer b = 127;
System.out.println(a == b);  // true

Integer c = 128;
Integer d = 128;
System.out.println(c == d);  // false ⚠️

// 陷阱 2: 混合运算
Integer e = 100;
int f = 100;
System.out.println(e == f);  // true (自动拆箱比较值)

// 陷阱 3: 运算后的结果
Integer g = 100;
Integer h = 100;
Integer i = g + h;  // 200，超出缓存范围
Integer j = 200;
System.out.println(i == j);  // false

// 正确做法: 使用 equals 比较
System.out.println(c.equals(d));  // true ✅
```

## 调整缓存上界

```bash
# JVM 参数调整 Integer 缓存上界
java -XX:AutoBoxCacheMax=1000 MyApp

# 或者
java -Djava.lang.Integer.IntegerCache.high=1000 MyApp
```

```java
// 调整后验证
Integer a = 500;
Integer b = 500;
System.out.println(a == b);  // true (如果上界设为 1000)
```

## 面试回答

### 30秒版本

> Integer 缓存池预先缓存 **-128 到 127** 的对象。自动装箱时通过 `valueOf()` 使用缓存，范围内返回同一对象，范围外创建新对象。所以 `Integer a=127, b=127; a==b` 是 true，但 `a=128, b=128; a==b` 是 **false**！建议用 `equals()` 比较包装类。上界可通过 `-XX:AutoBoxCacheMax` 调整。

### 1分钟版本

> **缓存范围**：
> - Integer: -128 ~ 127（上界可调）
> - Byte/Short/Long: -128 ~ 127
> - Character: 0 ~ 127
>
> **工作原理**：
> - Integer.valueOf() 使用缓存
> - 自动装箱调用 valueOf()
> - new Integer() 不使用缓存
>
> **常见陷阱**：
> - 127 范围内 == true
> - 128 范围外 == false
> - 应使用 equals() 比较
>
> **调整方式**：
> - `-XX:AutoBoxCacheMax=N`
> - `-Djava.lang.Integer.IntegerCache.high=N`

---

*关联文档：[primitive-vs-wrapper.md](primitive-vs-wrapper.md) | [hashcode-equals.md](hashcode-equals.md)*
