# JUnit 内部结构 (JUnit Internals)

> 基于《Clean Code》第15章 - Robert C. Martin

## 核心思想

本章是一个代码批评案例研究。展示了如何对已经相当整洁的代码应用童子军规则，使其变得更加整洁。

> JUnit 是所有 Java 框架中最著名的之一。作为框架，它概念简单、定义精确、实现优雅。

---

## 案例：ComparisonCompactor

### 功能说明

给定两个不同的字符串（如 `ABCDE` 和 `ABXDE`），它会通过生成字符串（如 `<…B[X]D…>`）来暴露差异。

### 测试用例（作为文档）

```java
public class ComparisonCompactorTest extends TestCase {

    public void testMessage() {
        String failure = new ComparisonCompactor(0, "b", "c").compact("a");
        assertTrue("a expected:<[b]> but was:<[c]>".equals(failure));
    }

    public void testStartSame() {
        String failure = new ComparisonCompactor(1, "ba", "bc").compact(null);
        assertEquals("expected:<b[a]> but was:<b[c]>", failure);
    }

    public void testEndSame() {
        String failure = new ComparisonCompactor(1, "ab", "cb").compact(null);
        assertEquals("expected:<[a]b> but was:<[c]b>", failure);
    }

    public void testSame() {
        String failure = new ComparisonCompactor(1, "ab", "ab").compact(null);
        assertEquals("expected:<ab> but was:<ab>", failure);
    }

    public void testNoContextStartAndEndSame() {
        String failure = new ComparisonCompactor(0, "abc", "adc").compact(null);
        assertEquals("expected:<…[b]…> but was:<…[d]…>", failure);
    }

    public void testStartAndEndContext() {
        String failure = new ComparisonCompactor(1, "abc", "adc").compact(null);
        assertEquals("expected:<a[b]c> but was:<a[d]c>", failure);
    }

    public void testStartAndEndContextWithEllipses() {
        String failure = new ComparisonCompactor(1, "abcde", "abfde").compact(null);
        assertEquals("expected:<…b[c]d…> but was:<…b[f]d…>", failure);
    }
    // ... 更多测试用例
}
```

**代码覆盖率**：100%——每行代码、每个 if 语句和 for 循环都被测试执行。

---

## 重构步骤

### 步骤 1：移除 f 前缀 [N6]

```java
// ❌ 之前：匈牙利命名法的作用域编码
private int fContextLength;
private String fExpected;
private String fActual;
private int fPrefix;
private int fSuffix;

// ✅ 之后：现代 IDE 使这种编码变得多余
private int contextLength;
private String expected;
private String actual;
private int prefix;
private int suffix;
```

### 步骤 2：封装条件判断 [G28]

```java
// ❌ 之前：未封装的条件
public String compact(String message) {
    if (expected == null || actual == null || areStringsEqual())
        return Assert.format(message, expected, actual);
    // ...
}

// ✅ 之后：提取方法使意图清晰
public String compact(String message) {
    if (shouldNotCompact())
        return Assert.format(message, expected, actual);
    // ...
}

private boolean shouldNotCompact() {
    return expected == null || actual == null || areStringsEqual();
}
```

### 步骤 3：消除歧义变量名 [N4]

```java
// ❌ 之前：局部变量与成员变量同名
String expected = compactString(this.expected);
String actual = compactString(this.actual);

// ✅ 之后：明确的名称
String compactExpected = compactString(expected);
String compactActual = compactString(actual);
```

### 步骤 4：正向表达条件 [G29]

```java
// ❌ 之前：否定更难理解
if (shouldNotCompact())
    return Assert.format(message, expected, actual);

// ✅ 之后：正向条件
if (canBeCompacted()) {
    findCommonPrefix();
    findCommonSuffix();
    String compactExpected = compactString(expected);
    String compactActual = compactString(actual);
    return Assert.format(message, compactExpected, compactActual);
} else {
    return Assert.format(message, expected, actual);
}

private boolean canBeCompacted() {
    return expected != null && actual != null && !areStringsEqual();
}
```

### 步骤 5：函数名应描述其行为 [N7]

```java
// ❌ 之前：compact 隐藏了副作用（可能不压缩）
public String compact(String message)

// ✅ 之后：名称清楚描述功能
public String formatCompactedComparison(String message)
```

### 步骤 6：函数只做一件事 [G30]

```java
// ❌ 之前：格式化和压缩混在一起
public String formatCompactedComparison(String message) {
    if (canBeCompacted()) {
        findCommonPrefix();
        findCommonSuffix();
        String compactExpected = compactString(expected);
        String compactActual = compactString(actual);
        return Assert.format(message, compactExpected, compactActual);
    } else {
        return Assert.format(message, expected, actual);
    }
}

// ✅ 之后：分离压缩和格式化
public String formatCompactedComparison(String message) {
    if (canBeCompacted()) {
        compactExpectedAndActual();
        return Assert.format(message, compactExpected, compactActual);
    } else {
        return Assert.format(message, expected, actual);
    }
}

private void compactExpectedAndActual() {
    findCommonPrefix();
    findCommonSuffix();
    compactExpected = compactString(expected);
    compactActual = compactString(actual);
}
```

### 步骤 7：一致的约定 [G11]

```java
// ❌ 之前：不一致——有的函数返回值，有的通过副作用设置
private void compactExpectedAndActual() {
    findCommonPrefix();       // 通过副作用设置 prefix
    findCommonSuffix();       // 通过副作用设置 suffix
    compactExpected = compactString(expected);  // 赋值
    compactActual = compactString(actual);      // 赋值
}

// ✅ 之后：一致地使用返回值
private void compactExpectedAndActual() {
    prefixIndex = findCommonPrefix();   // 返回值
    suffixIndex = findCommonSuffix();   // 返回值
    compactExpected = compactString(expected);
    compactActual = compactString(actual);
}
```

### 步骤 8：暴露时序耦合 [G31]

```java
// ❌ 之前：隐藏的时序耦合
// findCommonSuffix 依赖于 findCommonPrefix 先被调用
private void compactExpectedAndActual() {
    prefixIndex = findCommonPrefix();
    suffixIndex = findCommonSuffix();  // 如果顺序颠倒会出问题
    // ...
}

// ✅ 选项 1：通过参数暴露依赖
suffixIndex = findCommonSuffix(prefixIndex);

// ✅ 选项 2（更好）：合并为一个函数
private void findCommonPrefixAndSuffix() {
    findCommonPrefix();  // 显式调用顺序
    // suffix 计算代码...
}
```

### 步骤 9：精确命名 [N1]

```java
// ❌ 之前：suffixIndex 实际上是长度，不是索引
private int suffixIndex;
// 导致代码中出现奇怪的 +1

// ✅ 之后：名称反映真实含义
private int suffixLength;
// 消除了所有 +1
```

### 步骤 10：消除不必要的代码 [G9]

```java
// ❌ 之前：冗余的条件检查
private String compactString(String source) {
    String result = DELTA_START +
        source.substring(prefixLength, source.length() - suffixLength) +
        DELTA_END;
    if (prefixLength > 0)  // 这个检查是多余的！
        result = computeCommonPrefix() + result;
    if (suffixLength > 0)  // 这个检查也是多余的！
        result = result + computeCommonSuffix();
    return result;
}

// ✅ 之后：简化函数
private String compactString(String source) {
    return
        computeCommonPrefix() +
        DELTA_START +
        source.substring(prefixLength, source.length() - suffixLength) +
        DELTA_END +
        computeCommonSuffix();
}
```

---

## 最终版本

```java
package junit.framework;

public class ComparisonCompactor {
    private static final String ELLIPSIS = "…";
    private static final String DELTA_END = "]";
    private static final String DELTA_START = "[";

    private int contextLength;
    private String expected;
    private String actual;
    private int prefixLength;
    private int suffixLength;

    public ComparisonCompactor(int contextLength, String expected, String actual) {
        this.contextLength = contextLength;
        this.expected = expected;
        this.actual = actual;
    }

    public String formatCompactedComparison(String message) {
        String compactExpected = expected;
        String compactActual = actual;
        if (shouldBeCompacted()) {
            findCommonPrefixAndSuffix();
            compactExpected = compact(expected);
            compactActual = compact(actual);
        }
        return Assert.format(message, compactExpected, compactActual);
    }

    private boolean shouldBeCompacted() {
        return !shouldNotBeCompacted();
    }

    private boolean shouldNotBeCompacted() {
        return expected == null ||
               actual == null ||
               expected.equals(actual);
    }

    private void findCommonPrefixAndSuffix() {
        findCommonPrefix();
        suffixLength = 0;
        for (; !suffixOverlapsPrefix(); suffixLength++) {
            if (charFromEnd(expected, suffixLength) !=
                charFromEnd(actual, suffixLength))
                break;
        }
    }

    private char charFromEnd(String s, int i) {
        return s.charAt(s.length() - i - 1);
    }

    private boolean suffixOverlapsPrefix() {
        return actual.length() - suffixLength <= prefixLength ||
               expected.length() - suffixLength <= prefixLength;
    }

    private void findCommonPrefix() {
        prefixLength = 0;
        int end = Math.min(expected.length(), actual.length());
        for (; prefixLength < end; prefixLength++)
            if (expected.charAt(prefixLength) != actual.charAt(prefixLength))
                break;
    }

    // 使用 StringBuilder 清晰组合各部分
    private String compact(String s) {
        return new StringBuilder()
            .append(startingEllipsis())
            .append(startingContext())
            .append(DELTA_START)
            .append(delta(s))
            .append(DELTA_END)
            .append(endingContext())
            .append(endingEllipsis())
            .toString();
    }

    private String startingEllipsis() {
        return prefixLength > contextLength ? ELLIPSIS : "";
    }

    private String startingContext() {
        int contextStart = Math.max(0, prefixLength - contextLength);
        int contextEnd = prefixLength;
        return expected.substring(contextStart, contextEnd);
    }

    private String delta(String s) {
        int deltaStart = prefixLength;
        int deltaEnd = s.length() - suffixLength;
        return s.substring(deltaStart, deltaEnd);
    }

    private String endingContext() {
        int contextStart = expected.length() - suffixLength;
        int contextEnd = Math.min(contextStart + contextLength, expected.length());
        return expected.substring(contextStart, contextEnd);
    }

    private String endingEllipsis() {
        return suffixLength > contextLength ? ELLIPSIS : "";
    }
}
```

---

## 代码组织

### 最终版本的结构

```
┌─────────────────────────────────────────────────────────────┐
│  模块被分成两组函数：                                         │
│                                                             │
│  分析函数（先出现）：                                         │
│  • shouldBeCompacted()                                      │
│  • shouldNotBeCompacted()                                   │
│  • findCommonPrefixAndSuffix()                              │
│  • findCommonPrefix()                                       │
│  • charFromEnd()                                            │
│  • suffixOverlapsPrefix()                                   │
│                                                             │
│  合成函数（后出现）：                                         │
│  • compact()                                                │
│  • startingEllipsis()                                       │
│  • startingContext()                                        │
│  • delta()                                                  │
│  • endingContext()                                          │
│  • endingEllipsis()                                         │
│                                                             │
│  拓扑排序：每个函数的定义紧跟在其使用之后                      │
└─────────────────────────────────────────────────────────────┘
```

---

## 重构中应用的规则总结

| 规则编号 | 规则描述 | 应用 |
|---------|---------|------|
| **[N6]** | 避免编码（如 f 前缀） | 移除成员变量的 f 前缀 |
| **[G28]** | 封装条件 | 提取 `shouldNotCompact()` 方法 |
| **[N4]** | 无歧义的名称 | `compactExpected` 替代 `expected` 局部变量 |
| **[G29]** | 避免否定条件 | `canBeCompacted()` 替代 `shouldNotCompact()` |
| **[N7]** | 名称应描述副作用 | `formatCompactedComparison` 替代 `compact` |
| **[G30]** | 函数只做一件事 | 分离压缩和格式化 |
| **[G11]** | 一致的约定 | 所有函数都使用返回值 |
| **[N1]** | 选择描述性名称 | `suffixLength` 替代 `suffixIndex` |
| **[G31]** | 避免隐藏的时序耦合 | `findCommonPrefixAndSuffix()` |
| **[G33]** | 消除魔法数字 | 消除 +1 常量 |
| **[G9]** | 消除死代码 | 移除冗余的 if 检查 |

---

## 重构是迭代的

```
┌─────────────────────────────────────────────────────────────┐
│  如果仔细观察，你会注意到我撤销了本章早些时候做的几个决定       │
│                                                             │
│  例如：                                                     │
│  • 我把一些提取的方法内联回 formatCompactedComparison         │
│  • 我改变了 shouldNotBeCompacted 表达式的方向                 │
│                                                             │
│  这是典型的。一个重构常常导致另一个重构                        │
│  而那个又导致撤销第一个                                       │
│                                                             │
│  重构是一个充满试验和错误的迭代过程                           │
│  最终收敛到我们认为值得专业人士认可的东西                     │
└─────────────────────────────────────────────────────────────┘
```

---

## 代码审查清单

### 命名
- [ ] 是否移除了作用域编码（如 f、m_ 前缀）？
- [ ] 变量名是否精确描述其含义（length vs index）？
- [ ] 函数名是否描述了所有副作用？
- [ ] 局部变量是否与成员变量有歧义？

### 条件
- [ ] 复杂条件是否被封装成命名良好的方法？
- [ ] 是否使用正向条件而非否定条件？

### 函数
- [ ] 每个函数是否只做一件事？
- [ ] 函数是否使用一致的约定（返回值 vs 副作用）？
- [ ] 时序耦合是否显式暴露？

### 代码组织
- [ ] 函数是否按拓扑顺序排列？
- [ ] 分析函数和合成函数是否分组？

### 清理
- [ ] 是否消除了死代码？
- [ ] 是否消除了魔法数字和奇怪的 +1/-1？

---

## 结论

> **我们满足了童子军规则。我们让这个模块比发现时更整洁一些。**
> 
> **不是说它之前不整洁。作者们做得很出色。**
> 
> **但没有任何模块能免于改进，我们每个人都有责任让代码比发现时更整洁一些。**
