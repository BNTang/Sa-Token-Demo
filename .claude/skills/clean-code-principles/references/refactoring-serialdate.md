# 重构 SerialDate (Refactoring SerialDate)

> **核心理念**: 代码审查是专业实践，我们应当欢迎对自己代码的批评。通过这种批评我们才能学习。医生这样做，飞行员这样做，律师这样做，程序员也需要学会这样做。

## 重构方法论

### 1. 先让它工作 (First, Make It Work)

#### 测试覆盖率检查
```java
// ❌ 原始代码: 只有50%测试覆盖率
// Clover报告: 185个可执行语句中只测试了91个

// ✅ 重构后: 提升到92%覆盖率
// 170个语句被测试覆盖
```

#### 发现并修复Bug
```java
// ❌ 边界条件错误 [T5]
// getFollowingDayOfWeek 在边界情况下返回错误结果
if (baseDOW > targetWeekday) {  // 原始代码

// ✅ 修复边界条件
if (baseDOW >= targetWeekday) {  // 正确处理相等情况
```

```java
// ❌ 算法错误: adjust变量永远为负，条件永远为false [T8]
if (adjust >= 4)  // 这行永远不会执行！

// ✅ 正确算法
int delta = targetDOW - base.getDayOfWeek();
int positiveDelta = delta + 7;
int adjust = positiveDelta % 7;
if (adjust > 3)
    adjust -= 7;
return SerialDate.addDays(adjust, base);
```

---

### 2. 再让它正确 (Then Make It Right)

## 规则清单与示例

### 规则 1: 删除过时注释 [C1]
```java
// ❌ 变更历史注释 - 源码管理工具已经做这件事
/**
 * Changes (from 11-Oct-2001)
 * --------------------------
 * 11-Oct-2001 : Re-organised the class and moved it to new package
 * ...
 */

// ✅ 使用版本控制系统 (Git) 管理变更历史
// 只保留必要的版权和许可证信息
```

### 规则 2: 简化导入 [J1]
```java
// ❌ 冗长的导入列表
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

// ✅ 使用通配符导入
import java.text.*;
import java.util.*;
```

### 规则 3: 避免多语言混用 [G1]
```java
// ❌ 一个注释中混用四种语言: Java, English, Javadoc, HTML
/**
 * <ul>
 *   <li>This is item 1</li>
 *   <li>This is item 2</li>
 * </ul>
 */

// ✅ 使用 <pre> 保持格式
/**
 * <pre>
 * - This is item 1
 * - This is item 2
 * </pre>
 */
```

### 规则 4: 使用描述性名称 [N1][N2]
```java
// ❌ SerialDate - 暗示了实现细节（序列号）
// 抽象类不应该暗示任何实现

// ✅ DayDate - 描述概念而非实现
// 因为已有太多Date类，选择DayDate作为折中

// ❌ toSerial() - 不够描述性
// ✅ getOrdinalDay() - 更精确地描述返回值

// ❌ createInstance() - 通用名称
// ✅ makeDate() - 更具描述性
```

### 规则 5: 用枚举替代常量继承 [J2]
```java
// ❌ 继承常量类 - 只是为了避免写 MonthConstants.JANUARY
public abstract class DayDate extends MonthConstants {
    // ...
}

// ✅ 使用枚举
public abstract class DayDate implements Comparable, Serializable {
    public static enum Month {
        JANUARY(1), FEBRUARY(2), MARCH(3), APRIL(4),
        MAY(5), JUNE(6), JULY(7), AUGUST(8),
        SEPTEMBER(9), OCTOBER(10), NOVEMBER(11), DECEMBER(12);
        
        Month(int index) {
            this.index = index;
        }
        
        public static Month make(int monthIndex) {
            for (Month m : Month.values()) {
                if (m.index == monthIndex)
                    return m;
            }
            throw new IllegalArgumentException("Invalid month index " + monthIndex);
        }
        
        public final int index;
    }
}
```

### 规则 6: 移除冗余验证 [G5]
```java
// ❌ 有了Month枚举后，这个验证方法变得多余
public static boolean isValidMonthCode(int monthCode) {
    return monthCode >= 1 && monthCode <= 12;
}

// ✅ 枚举本身就提供了类型安全
// 直接删除 isValidMonthCode 方法
```

### 规则 7: 将实现细节下推到派生类 [G6]
```java
// ❌ 抽象类包含实现相关的常量
public abstract class DayDate {
    public static final int EARLIEST_DATE_ORDINAL = 2;     // Excel相关
    public static final int LATEST_DATE_ORDINAL = 2958465;
    public static final int MINIMUM_YEAR_SUPPORTED = 1900;
    public static final int MAXIMUM_YEAR_SUPPORTED = 9999;
}

// ✅ 将实现细节移到具体类
public class SpreadsheetDate extends DayDate {
    public static final int EARLIEST_DATE_ORDINAL = 2;
    public static final int LATEST_DATE_ORDINAL = 2958465;
    public static final int MINIMUM_YEAR_SUPPORTED = 1900;
    public static final int MAXIMUM_YEAR_SUPPORTED = 9999;
}
```

### 规则 8: 使用抽象工厂模式 [G7]
```java
// ❌ 基类直接创建派生类实例 - 违反依赖倒置原则
public abstract class DayDate {
    public static DayDate createInstance() {
        return new SpreadsheetDate();  // 基类知道派生类！
    }
}

// ✅ 使用抽象工厂
public abstract class DayDateFactory {
    private static DayDateFactory factory = new SpreadsheetDateFactory();
    
    public static void setInstance(DayDateFactory factory) {
        DayDateFactory.factory = factory;
    }
    
    protected abstract DayDate _makeDate(int ordinal);
    protected abstract DayDate _makeDate(int day, DayDate.Month month, int year);
    protected abstract int _getMinimumYear();
    protected abstract int _getMaximumYear();
    
    public static DayDate makeDate(int ordinal) {
        return factory._makeDate(ordinal);
    }
    
    public static int getMinimumYear() {
        return factory._getMinimumYear();
    }
    
    public static int getMaximumYear() {
        return factory._getMaximumYear();
    }
}
```

### 规则 9: 将数据放在使用它的地方附近 [G10]
```java
// ❌ 表格定义在DayDate中，但只在SpreadsheetDate中使用
public abstract class DayDate {
    static final int[] AGGREGATE_DAYS_TO_END_OF_PRECEDING_MONTH = {...};
}

// ✅ 将表格移到使用它的类中
public class SpreadsheetDate extends DayDate {
    private static final int[] AGGREGATE_DAYS_TO_END_OF_PRECEDING_MONTH = {...};
}
```

### 规则 10: 删除死代码 [G9]
```java
// ❌ 未使用的方法和变量
private String description;  // 没有人使用

public static int[] AGGREGATE_DAYS_TO_END_OF_MONTH = {...};  // 从未调用

// ✅ 直接删除！不要留着"以防万一"
```

### 规则 11: 使用数学术语命名 [N3]
```java
// ❌ 不清晰的常量名
public static final int INCLUDE_NONE = 0;
public static final int INCLUDE_FIRST = 1;
public static final int INCLUDE_SECOND = 2;
public static final int INCLUDE_BOTH = 3;

// ✅ 使用数学术语
public enum DateInterval {
    OPEN,         // 开区间 (a, b)
    CLOSED_LEFT,  // 左闭右开 [a, b)
    CLOSED_RIGHT, // 左开右闭 (a, b]
    CLOSED        // 闭区间 [a, b]
}
```

### 规则 12: 将静态方法转为实例方法 [G18]
```java
// ❌ 操作DayDate变量的静态方法
public static DayDate addDays(int days, DayDate date) {
    return DayDateFactory.makeDate(date.toOrdinal() + days);
}

// ✅ 转换为实例方法
public DayDate addDays(int days) {
    return DayDateFactory.makeDate(toOrdinal() + days);
}
```

### 规则 13: 避免歧义的方法名 [N4][G20]
```java
// ❌ 容易误解：是修改date还是返回新对象？
DayDate date = DateFactory.makeDate(5, Month.DECEMBER, 1952);
date.addDays(7);  // 读者可能认为date被修改了

// ✅ 使用plus前缀明确表示返回新对象
DayDate date = oldDate.plusDays(5);  // 清晰表明返回新对象

// 以下写法读起来不通顺，暗示不会修改原对象
date.plusDays(5);  // 这样写没有意义，暗示应该接收返回值
```

### 规则 14: 使用解释性临时变量 [G19]
```java
// ❌ 复杂算法难以理解
public DayDate addMonths(int months) {
    return DayDateFactory.makeDate(
        Math.min(getDayOfMonth(), lastDayOfMonth(...)),
        Month.make((12 * getYear() + getMonth().index - 1 + months) % 12 + 1),
        (12 * getYear() + getMonth().index - 1 + months) / 12
    );
}

// ✅ 使用解释性变量
public DayDate addMonths(int months) {
    int thisMonthAsOrdinal = 12 * getYear() + getMonth().index - 1;
    int resultMonthAsOrdinal = thisMonthAsOrdinal + months;
    int resultYear = resultMonthAsOrdinal / 12;
    Month resultMonth = Month.make(resultMonthAsOrdinal % 12 + 1);
    
    int lastDayOfResultMonth = lastDayOfMonth(resultMonth, resultYear);
    int resultDay = Math.min(getDayOfMonth(), lastDayOfResultMonth);
    return DayDateFactory.makeDate(resultDay, resultMonth, resultYear);
}
```

### 规则 15: 使用解释性表达式 [G16]
```java
// ❌ 难以理解的闰年判断
public static boolean isLeapYear(int year) {
    return (year % 4 == 0) && ((year % 100 != 0) || (year % 400 == 0));
}

// ✅ 使用解释性变量
public static boolean isLeapYear(int year) {
    boolean fourth = year % 4 == 0;
    boolean hundredth = year % 100 == 0;
    boolean fourHundredth = year % 400 == 0;
    return fourth && (!hundredth || fourHundredth);
}
```

### 规则 16: 理解算法本质 [G21]
```java
// ❌ 复杂难懂的算法
public DayDate getPreviousDayOfWeek(Day targetDayOfWeek) {
    int adjust;
    int baseDOW = getDayOfWeek().index;
    int targetDOW = targetDayOfWeek.index;
    if (baseDOW > targetDOW) {
        adjust = Math.min(0, targetDOW - baseDOW);
    } else {
        adjust = -7 + Math.max(0, targetDOW - baseDOW);
    }
    return plusDays(adjust);
}

// ✅ 理解本质后简化
public DayDate getPreviousDayOfWeek(Day targetDayOfWeek) {
    int offsetToTarget = targetDayOfWeek.index - getDayOfWeek().index;
    if (offsetToTarget >= 0)
        offsetToTarget -= 7;
    return plusDays(offsetToTarget);
}
```

### 规则 17: 消除逻辑依赖 [G22]
```java
// ❌ getDayOfWeek 方法隐式依赖第0天是星期几
public Day getDayOfWeek() {
    // 这个算法依赖于ordinal 0是星期六
    return Day.make((getOrdinalDay() + 6) % 7 + 1);
}

// ✅ 显式声明依赖
public abstract Day getDayOfWeekForOrdinalZero();

public Day getDayOfWeek() {
    Day startingDay = getDayOfWeekForOrdinalZero();
    int startingOffset = startingDay.index - Day.SUNDAY.index;
    return Day.make((getOrdinalDay() + startingOffset) % 7 + 1);
}

// SpreadsheetDate中实现
protected Day getDayOfWeekForOrdinalZero() {
    return Day.SATURDAY;
}
```

### 规则 18: 用多态替代switch [G23]
```java
// ❌ switch语句
public boolean isInRange(DayDate d1, DayDate d2, int include) {
    switch (include) {
        case INCLUDE_NONE:
            return d > left && d < right;
        case INCLUDE_FIRST:
            return d >= left && d < right;
        case INCLUDE_SECOND:
            return d > left && d <= right;
        case INCLUDE_BOTH:
            return d >= left && d <= right;
    }
}

// ✅ 使用枚举多态
public enum DateInterval {
    OPEN {
        public boolean isIn(int d, int left, int right) {
            return d > left && d < right;
        }
    },
    CLOSED_LEFT {
        public boolean isIn(int d, int left, int right) {
            return d >= left && d < right;
        }
    },
    CLOSED_RIGHT {
        public boolean isIn(int d, int left, int right) {
            return d > left && d <= right;
        }
    },
    CLOSED {
        public boolean isIn(int d, int left, int right) {
            return d >= left && d <= right;
        }
    };
    
    public abstract boolean isIn(int d, int left, int right);
}

public boolean isInRange(DayDate d1, DayDate d2, DateInterval interval) {
    int left = Math.min(d1.getOrdinalDay(), d2.getOrdinalDay());
    int right = Math.max(d1.getOrdinalDay(), d2.getOrdinalDay());
    return interval.isIn(getOrdinalDay(), left, right);
}
```

### 规则 19: 将枚举移到独立文件 [G13]
```java
// ❌ 枚举嵌套在类中，变得过大
public abstract class DayDate {
    public enum Month { ... }  // 很多行
    public enum Day { ... }    // 很多行
    // ...
}

// ✅ 独立的枚举文件
// Month.java
public enum Month {
    JANUARY(1), FEBRUARY(2), ...;
    // ...
}

// Day.java  
public enum Day {
    MONDAY(Calendar.MONDAY), ...;
    // ...
}
```

### 规则 20: 消除魔法数字 [G25]
```java
// ❌ 魔法数字
int firstMonth = 1;
int sunday = 1;

// ✅ 使用符号常量
int firstMonth = Month.JANUARY.toInt();
int sunday = Day.SUNDAY.toInt();
```

---

## 应用的规则速查表

| 规则编号 | 规则名称 | 应用场景 |
|---------|---------|---------|
| [C1] | 过时注释 | 删除变更历史，使用版本控制 |
| [C2] | 冗余注释 | 删除只重复代码的注释 |
| [C3] | 描述不足的Javadoc | 精简或删除无价值的Javadoc |
| [J1] | 通配符导入 | 简化导入语句 |
| [J2] | 避免常量继承 | 使用枚举代替继承常量接口 |
| [J3] | 使用枚举替代常量 | 类型安全的常量定义 |
| [G1] | 避免多语言混用 | 一个源文件一种语言 |
| [G4] | 自动序列化 | 让编译器管理serialVersionUID |
| [G5] | 消除重复 | 合并重复的if语句和方法 |
| [G6] | 在正确层次放置代码 | 实现细节下推到派生类 |
| [G7] | 基类不了解派生类 | 使用抽象工厂模式 |
| [G8] | 限制可见性 | 表格应为private |
| [G9] | 删除死代码 | 删除未使用的变量和方法 |
| [G10] | 就近原则 | 数据靠近使用它的代码 |
| [G11] | 保持一致 | 相似的方法使用相似的结构 |
| [G12] | 删除杂乱 | 删除无用的默认构造函数、final等 |
| [G13] | 单独的类文件 | 大的枚举应有自己的文件 |
| [G14] | 特性依赖 | 方法应属于它操作的类 |
| [G15] | 避免布尔参数 | 不要传flag来选择输出格式 |
| [G16] | 表达式要表意 | 使用解释性变量 |
| [G17] | 数据与行为共存 | LAST_DAY_OF_MONTH应在Month枚举中 |
| [G18] | 适当使用实例方法 | 操作对象状态的方法不应是static |
| [G19] | 解释性临时变量 | 分解复杂表达式 |
| [G20] | 函数名表达意图 | plusDays vs addDays |
| [G21] | 理解算法 | 先理解再重构 |
| [G22] | 消除逻辑依赖 | 显式声明隐式依赖 |
| [G23] | 多态替代switch | 使用枚举多态 |
| [G24] | 抽象方法在上面 | 按重要性排列类成员 |
| [G25] | 消除魔法数字 | 使用符号常量 |
| [N1] | 选择描述性名称 | toOrdinal → getOrdinalDay |
| [N2] | 名称在正确抽象层次 | SerialDate → DayDate |
| [N3] | 使用标准术语 | 开区间、闭区间等数学术语 |
| [N4] | 无歧义的名称 | plusDays明确返回新对象 |
| [T1] | 测试不充分 | 发现未测试的代码路径 |
| [T2] | 使用覆盖率工具 | Clover发现50%未覆盖 |
| [T5] | 边界条件测试 | 发现边界条件错误 |
| [T6] | 在Bug附近深入测试 | Bug往往成群出现 |
| [T7] | 测试失败模式揭示问题 | 分析哪些测试失败 |
| [T8] | 覆盖率模式揭示问题 | 永不执行的代码意味着Bug |
| [F4] | 未使用的功能 | 删除未调用的方法 |

---

## 代码审查检查清单

### 测试覆盖率
- [ ] 使用覆盖率工具（如Clover/JaCoCo）检查
- [ ] 覆盖率至少达到85%以上
- [ ] 关注未覆盖代码，可能隐藏Bug

### 命名规范
- [ ] 抽象类名不暗示实现细节
- [ ] 方法名清晰表达是否修改对象
- [ ] 使用标准术语命名

### 类设计
- [ ] 基类不了解派生类
- [ ] 实现细节在派生类中
- [ ] 使用工厂模式创建实例

### 代码清理
- [ ] 删除未使用的代码
- [ ] 删除冗余注释
- [ ] 使用枚举替代常量

### 算法清晰度
- [ ] 使用解释性临时变量
- [ ] 复杂条件抽取为方法
- [ ] 理解算法本质后再简化

---

## 重构前后对比

| 指标 | 重构前 | 重构后 |
|-----|-------|-------|
| 测试覆盖率 | 50% | 84.9% |
| 可执行语句 | 185 | 53 |
| Bug数量 | 多个边界条件错误 | 已修复 |
| 类职责 | 混杂 | 清晰分离 |
| 命名 | SerialDate (暗示实现) | DayDate (描述概念) |

---

## 核心引用

> "It is only through critiques like these that we will learn. Doctors do it. Pilots do it. Lawyers do it. And we programmers need to learn how to do it too."
> 
> — Robert C. Martin

> "So once again we've followed the Boy Scout Rule. We've checked the code in a bit cleaner than when we checked it out."
> 
> — Robert C. Martin

---

## 技术栈
- **语言**: Java 17+
- **设计模式**: Abstract Factory, Singleton, Decorator
- **测试工具**: JUnit, Clover (覆盖率)
- **重构工具**: IDE内置重构功能
