# 迭进 (Emergence)

> 基于《Clean Code》第12章 - Robert C. Martin (by Jeff Langr)

## 核心思想

> 如果有四条简单规则能帮助你在工作中创造良好设计呢？
> 如果遵循这些规则能让你洞察代码的结构和设计，更容易应用 SRP 和 DIP 等原则呢？
> 如果这四条规则能促进良好设计的**涌现**呢？

---

## Kent Beck 的简单设计四规则

```
┌─────────────────────────────────────────────────────────────┐
│  Kent Beck 的简单设计四规则（按重要性排序）：                  │
│                                                             │
│  1. 运行所有测试                                             │
│  2. 不包含重复代码                                           │
│  3. 表达程序员的意图                                         │
│  4. 最小化类和方法的数量                                     │
└─────────────────────────────────────────────────────────────┘
```

---

## 规则 1：运行所有测试 (Runs All the Tests)

### 核心原则

设计首先必须产生一个**按预期运行**的系统。

```
┌─────────────────────────────────────────────────────────────┐
│  一个被全面测试且始终通过所有测试的系统 = 可测试的系统         │
│                                                             │
│  无法验证的系统永远不应该被部署                               │
└─────────────────────────────────────────────────────────────┘
```

### 测试如何改善设计

| 测试的作用 | 设计的改善 |
|-----------|-----------|
| 推动我们创建小而单一职责的类 | 符合 SRP 的类更容易测试 |
| 紧耦合使测试难写 | 推动使用 DIP、依赖注入、接口和抽象 |
| 写越多测试，越追求易测试的代码 | 设计持续改进 |

> **显著的结论**：遵循一条简单明了的规则——需要有测试并持续运行——会影响系统对低耦合和高内聚这两个主要 OO 目标的遵守。**编写测试会导向更好的设计。**

---

## 规则 2-4：重构 (Refactoring)

### 测试赋予的能力

```
有了测试，我们就有能力保持代码和类的整洁

         添加几行代码
              ↓
         暂停并反思新设计
              ↓
    设计是否退化了？ ─→ 是 ─→ 清理它
              ↓ 否
         运行测试确认没有破坏任何东西
              ↓
         继续下一个任务
```

**消除恐惧**：因为有测试，清理代码不会破坏它！

### 重构时可以做什么

- 增加内聚性
- 降低耦合度
- 分离关注点
- 模块化系统关注点
- 缩小函数和类
- 选择更好的名称
- 应用设计模式
- 应用后三条规则

---

## 规则 2：消除重复 (No Duplication)

### 重复是良好设计系统的首要敌人

```
┌─────────────────────────────────────────────────────────────┐
│  重复代表：                                                  │
│  • 额外的工作                                                │
│  • 额外的风险                                                │
│  • 额外不必要的复杂性                                        │
└─────────────────────────────────────────────────────────────┘
```

### 重复的多种形式

1. **完全相同的代码行**
2. **相似的代码行**（可以重构成相同）
3. **实现的重复**

### 示例 1：实现重复

```java
// ❌ 重复的实现
int size() { /* 追踪计数器 */ }
boolean isEmpty() { /* 追踪布尔值 */ }

// ✅ 消除重复
boolean isEmpty() {
    return 0 == size();
}
```

### 示例 2：代码行重复

```java
// ❌ 包含重复的代码
public void scaleToOneDimension(float desiredDimension, float imageDimension) {
    if (Math.abs(desiredDimension - imageDimension) < errorThreshold)
        return;
    float scalingFactor = desiredDimension / imageDimension;
    scalingFactor = (float)(Math.floor(scalingFactor * 100) * 0.01f);
    
    RenderedOp newImage = ImageUtilities.getScaledImage(
        image, scalingFactor, scalingFactor);
    image.dispose();      // 重复
    System.gc();          // 重复
    image = newImage;     // 重复
}

public synchronized void rotate(int degrees) {
    RenderedOp newImage = ImageUtilities.getRotatedImage(image, degrees);
    image.dispose();      // 重复
    System.gc();          // 重复
    image = newImage;     // 重复
}
```

```java
// ✅ 消除重复
public void scaleToOneDimension(float desiredDimension, float imageDimension) {
    if (Math.abs(desiredDimension - imageDimension) < errorThreshold)
        return;
    float scalingFactor = desiredDimension / imageDimension;
    scalingFactor = (float)(Math.floor(scalingFactor * 100) * 0.01f);
    replaceImage(ImageUtilities.getScaledImage(image, scalingFactor, scalingFactor));
}

public synchronized void rotate(int degrees) {
    replaceImage(ImageUtilities.getRotatedImage(image, degrees));
}

private void replaceImage(RenderedOp newImage) {
    image.dispose();
    System.gc();
    image = newImage;
}
```

### 提取公共代码的连锁效应

```
提取极小级别的公共代码
         ↓
开始识别 SRP 违规
         ↓
可能将新提取的方法移到另一个类
         ↓
提高其可见性
         ↓
团队其他人识别进一步抽象的机会
         ↓
在不同上下文中复用
         ↓
系统复杂性大幅降低
```

> **"小范围复用"的理解对于"大范围复用"至关重要。**

### 示例 3：模板方法模式消除高层重复

```java
// ❌ 高层重复
public class VacationPolicy {
    public void accrueUSDivisionVacation() {
        // 计算基于工作小时数的假期
        // ...
        // 确保符合美国最低标准
        // ...
        // 应用到工资记录
        // ...
    }
    
    public void accrueEUDivisionVacation() {
        // 计算基于工作小时数的假期  ← 重复
        // ...
        // 确保符合欧盟最低标准       ← 不同
        // ...
        // 应用到工资记录            ← 重复
        // ...
    }
}
```

```java
// ✅ 使用模板方法模式
abstract public class VacationPolicy {
    public void accrueVacation() {
        calculateBaseVacationHours();  // 共用
        alterForLegalMinimums();       // 抽象方法
        applyToPayroll();              // 共用
    }
    
    private void calculateBaseVacationHours() { /* ... */ }
    abstract protected void alterForLegalMinimums();  // 子类填充
    private void applyToPayroll() { /* ... */ }
}

public class USVacationPolicy extends VacationPolicy {
    @Override 
    protected void alterForLegalMinimums() {
        // 美国特定逻辑
    }
}

public class EUVacationPolicy extends VacationPolicy {
    @Override 
    protected void alterForLegalMinimums() {
        // 欧盟特定逻辑
    }
}
```

子类填充 `accrueVacation` 算法中的"洞"，提供唯一不重复的信息。

---

## 规则 3：表达力 (Expressive)

### 为什么表达力重要？

```
┌─────────────────────────────────────────────────────────────┐
│  软件项目的大部分成本在于长期维护                             │
│                                                             │
│  为了在引入变更时最小化缺陷的可能性                           │
│  我们必须能够理解系统做什么                                   │
│                                                             │
│  系统越复杂，开发者理解需要的时间越多                         │
│  误解的机会也越大                                            │
└─────────────────────────────────────────────────────────────┘
```

### 如何表达自己

| 方式 | 说明 |
|-----|------|
| **选择好名称** | 听到类或函数名时，不应对其职责感到惊讶 |
| **保持函数和类小** | 小类和函数通常易于命名、编写和理解 |
| **使用标准命名法** | 使用设计模式名称如 COMMAND、VISITOR 描述设计 |
| **编写良好的单元测试** | 测试作为示例文档，帮助快速理解类的作用 |
| **尝试** | 花时间让代码易于阅读 |

### 最重要的表达方式

```
┌─────────────────────────────────────────────────────────────┐
│  最重要的表达方式是：尝试                                    │
│                                                             │
│  我们太常让代码运行后就转向下一个问题                         │
│  没有充分考虑让代码对下一个读者来说易于阅读                   │
│                                                             │
│  记住，下一个读代码的人最可能是你自己                         │
└─────────────────────────────────────────────────────────────┘
```

> **对你的工作成果感到一些骄傲吧。花一点时间在每个函数和类上。选择更好的名称，把大函数拆分成小函数，总之用心对待你创造的东西。用心是宝贵的资源。**

---

## 规则 4：最少的类和方法 (Minimal Classes and Methods)

### 避免过度设计

即使是消除重复、代码表达力和 SRP 这样基本的概念也可能走得太远。

```
┌─────────────────────────────────────────────────────────────┐
│  问题示例（无意义的教条主义）：                               │
│                                                             │
│  • 坚持为每个类创建一个接口                                  │
│  • 坚持字段和行为必须分离到数据类和行为类                     │
│                                                             │
│  这种教条应该被抵制，采用更务实的方法                         │
└─────────────────────────────────────────────────────────────┘
```

### 平衡

```
目标：保持整体系统小
      同时保持函数和类小

          ↓

但是：这条规则优先级最低

          ↓

虽然保持类和函数数量低很重要
更重要的是：有测试、消除重复、表达自己
```

---

## 四规则优先级

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│    优先级 1  ▶  运行所有测试                                │
│         │                                                   │
│         ▼                                                   │
│    优先级 2  ▶  不包含重复代码                              │
│         │                                                   │
│         ▼                                                   │
│    优先级 3  ▶  表达程序员的意图                            │
│         │                                                   │
│         ▼                                                   │
│    优先级 4  ▶  最小化类和方法的数量                        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 代码审查清单

### 测试
- [ ] 是否有全面的测试覆盖？
- [ ] 所有测试是否持续通过？
- [ ] 代码是否可测试？

### 重复
- [ ] 是否存在完全相同的代码行？
- [ ] 是否存在相似的代码可以重构？
- [ ] 是否存在实现上的重复？
- [ ] 高层算法重复是否使用模板方法模式处理？

### 表达力
- [ ] 类和函数名是否清晰表达其职责？
- [ ] 函数和类是否足够小？
- [ ] 是否使用设计模式标准名称？
- [ ] 单元测试是否作为文档清晰表达类的作用？
- [ ] 代码是否经过精心打磨？

### 最小化
- [ ] 是否避免了无意义的接口？
- [ ] 是否避免了不必要的类分离？
- [ ] 整体系统是否保持小巧？

---

## 核心箴言

> **编写测试会导向更好的设计。**

> **重复是良好设计系统的首要敌人。**

> **代码应该清晰表达作者的意图。作者让代码越清晰，其他人理解它需要的时间就越少。**

> **下一个读代码的人最可能是你自己。对你的工作成果感到一些骄傲吧。**

> **遵循简单设计的实践可以并且确实能鼓励和促使开发者遵守那些本来需要多年才能学会的良好原则和模式。**
