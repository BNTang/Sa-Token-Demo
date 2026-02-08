# 代码坏味道与启发规则 (Smells and Heuristics)

> **核心理念**: 整洁代码不是通过遵循一套规则来编写的。你不能通过学习启发式规则列表就成为软件匠人。专业精神和工匠精神来自于驱动纪律的价值观。

本章汇总了 Martin Fowler《重构》中的代码坏味道，以及 Robert C. Martin 在实践中总结的启发规则。

---

## 一、注释 (Comments)

### C1: 不恰当的信息 (Inappropriate Information)

注释不应持有更适合放在其他系统中的信息，如源码控制、问题追踪等。

```java
// ❌ 修改历史不属于注释
/**
 * Changes (from 11-Oct-2001)
 * --------------------------
 * 11-Oct-2001 : Re-organised the class
 * 05-Nov-2001 : Added getDescription() method
 */

// ✅ 只保留技术性注释
/**
 * Represents a date independent of time.
 * Uses ordinal day count for internal representation.
 */
```

### C2: 废弃的注释 (Obsolete Comment)

过时、不相关、不正确的注释应立即更新或删除。

```java
// ❌ 代码已改变，注释未更新
// Returns -1 if not found
public Day parse(String s) {  // 实际抛出异常
    throw new IllegalArgumentException();
}

// ✅ 保持注释与代码同步或删除注释
public Day parse(String s) throws IllegalArgumentException {
    // 方法签名已自解释
}
```

### C3: 冗余的注释 (Redundant Comment)

描述自解释代码的注释是冗余的。

```java
// ❌ 冗余注释
i++; // increment i

// ❌ Javadoc比方法签名信息更少
/**
 * @param sellRequest
 * @return
 * @throws ManagedComponentException
 */
public SellResponse beginSellItem(SellRequest sellRequest)
    throws ManagedComponentException

// ✅ 注释应说出代码无法表达的内容
```

### C4: 糟糕的注释 (Poorly Written Comment)

值得写的注释值得写好。

```java
// ❌ 草率的注释
// does stuff with the thing

// ✅ 精心撰写
// Calculates the Fibonacci sequence up to n terms
// using memoization for O(n) time complexity
```

### C5: 注释掉的代码 (Commented-Out Code)

注释掉的代码是可憎的，应该删除。

```java
// ❌ 注释掉的代码 - 删除它！
// InputStreamResponse response = new InputStreamResponse();
// response.setBody(formatter.getResultStream(), formatter.getByteCount());
// return response;

// ✅ 如果需要，从版本控制系统中恢复
// 删除所有注释掉的代码
```

---

## 二、环境 (Environment)

### E1: 构建需要多个步骤 (Build Requires More Than One Step)

构建应该是单一简单操作。

```bash
# ❌ 复杂的构建过程
cd component1 && mvn install
cd ../component2 && mvn install
cd ../main && mvn package

# ✅ 一个命令构建整个项目
git clone mySystem
cd mySystem
mvn clean install
```

### E2: 测试需要多个步骤 (Tests Require More Than One Step)

运行所有单元测试应该只需一个命令。

```bash
# ❌ 需要多个命令
mvn test -pl module1
mvn test -pl module2
./run-integration-tests.sh

# ✅ 一个命令运行所有测试
mvn test

# 或在IDE中一键运行
```

---

## 三、函数 (Functions)

### F1: 过多的参数 (Too Many Arguments)

函数参数应该少。无参最好，其次是一个、两个、三个。超过三个应避免。

```java
// ❌ 参数过多
public void doSomething(String name, int age, String address, 
    String phone, String email, Date birthDate) { }

// ✅ 使用对象封装
public void doSomething(Person person) { }

// 或使用Builder模式
public void doSomething(PersonBuilder builder) { }
```

### F2: 输出参数 (Output Arguments)

输出参数违反直觉。读者期望参数是输入，不是输出。

```java
// ❌ 使用输出参数
public void appendFooter(StringBuilder report) {
    report.append("Footer");
}
appendFooter(report);

// ✅ 让对象修改自己的状态
report.appendFooter();

// 或返回新值
String result = createFooter(report);
```

### F3: 标识参数 (Flag Arguments)

布尔参数大声宣告函数做了不止一件事。

```java
// ❌ 标识参数
public void render(boolean isSuite) {
    if (isSuite) {
        // render suite
    } else {
        // render single test
    }
}

// ✅ 拆分为两个函数
public void renderForSuite() { }
public void renderForSingleTest() { }
```

### F4: 死函数 (Dead Function)

永远不会被调用的方法应该丢弃。

```java
// ❌ 永远不会被调用的方法
private void oldCalculation() {
    // 这个方法在代码库中没有任何地方调用
}

// ✅ 删除！版本控制系统还记得它
```

---

## 四、一般性问题 (General)

### G1: 一个源文件中有多种语言 (Multiple Languages in One Source File)

理想情况下，源文件应该只包含一种语言。

```java
// ❌ 混合多种语言
public String render() {
    return "<html>\n" +
           "  <body>\n" +
           "    <script>alert('Hello');</script>\n" +  // JavaScript
           "    <!-- Comment -->\n" +                    // HTML注释
           "  </body>\n" +
           "</html>";
}

// ✅ 分离关注点
// 使用模板引擎或单独的文件
public String render() {
    return templateEngine.render("page.html", model);
}
```

### G2: 明显的行为未实现 (Obvious Behavior Is Unimplemented)

遵循"最少惊讶原则"，函数应实现其他程序员合理期望的行为。

```java
// ❌ 行为不符合预期
Day day = DayDate.StringToDay("monday");  // 返回null，因为区分大小写

// ✅ 实现明显的行为
Day day = DayDate.StringToDay("monday");  // 返回Day.MONDAY
Day day = DayDate.StringToDay("MONDAY");  // 也返回Day.MONDAY
Day day = DayDate.StringToDay("Mon");     // 也返回Day.MONDAY
```

### G3: 边界行为不正确 (Incorrect Behavior at the Boundaries)

不要依赖直觉，为每个边界条件编写测试。

```java
// ❌ 未考虑边界条件
public int getFollowingDayOfWeek(int dayOfWeek, Date date) {
    // 假设边界情况会正常工作
}

// ✅ 测试所有边界条件
@Test
void testFollowingDayOfWeek_sameDay() {
    // 12月25日星期六后面的星期六是1月1日，不是12月25日
    Date saturday = createDate(2004, 12, 25);
    Date following = getFollowingDayOfWeek(SATURDAY, saturday);
    assertEquals(createDate(2005, 1, 1), following);
}
```

### G4: 覆盖安全措施 (Overridden Safeties)

覆盖安全措施是危险的（切尔诺贝利事故就是因为覆盖了安全机制）。

```java
// ❌ 忽略编译器警告
@SuppressWarnings("all")  // 危险！

// ❌ 关闭失败的测试
@Ignore("We'll fix this later")  // 永远不会

// ✅ 修复问题，不要绕过
```

### G5: 重复 (Duplication)

这是本书最重要的规则之一！DRY原则。

```java
// ❌ 复制粘贴代码
public void processAdminUser(User user) {
    validateUser(user);
    checkPermissions(user);
    logAction(user);
    sendNotification(user);
}

public void processNormalUser(User user) {
    validateUser(user);
    checkPermissions(user);  // 重复！
    logAction(user);          // 重复！
    sendNotification(user);   // 重复！
}

// ✅ 提取公共代码
public void processUser(User user, PermissionChecker checker) {
    validateUser(user);
    checker.check(user);
    logAction(user);
    sendNotification(user);
}
```

**重复的形式：**
1. **明显的重复**: 复制粘贴的代码 → 提取方法
2. **switch/case链**: 重复出现的条件判断 → 使用多态
3. **相似算法**: 结构相似但不完全相同 → 使用模板方法或策略模式

### G6: 错误的抽象层次 (Code at Wrong Level of Abstraction)

抽象类应只包含高层概念，派生类包含低层细节。

```java
// ❌ 抽象接口包含实现细节
public interface Stack {
    Object pop() throws EmptyException;
    void push(Object o) throws FullException;
    double percentFull();  // 不是所有栈都有"满"的概念！
}

// ✅ 分离抽象层次
public interface Stack {
    Object pop() throws EmptyException;
    void push(Object o);
}

public interface BoundedStack extends Stack {
    double percentFull();
    void push(Object o) throws FullException;
}
```

### G7: 基类依赖派生类 (Base Classes Depending on Their Derivatives)

基类不应该知道派生类的存在。

```java
// ❌ 基类引用派生类
public abstract class DayDate {
    public static DayDate createInstance() {
        return new SpreadsheetDate();  // 基类知道派生类！
    }
}

// ✅ 使用工厂模式
public abstract class DayDateFactory {
    public static DayDate makeDate(int ordinal) {
        return factory.createDate(ordinal);
    }
}
```

### G8: 信息过多 (Too Much Information)

良好的模块有小而精的接口，耦合度低。

```java
// ❌ 接口过于宽泛
public class DataManager {
    public void connect();
    public void disconnect();
    public void executeQuery();
    public void executeUpdate();
    public void beginTransaction();
    public void commit();
    public void rollback();
    public void setAutoCommit();
    // ... 20个更多方法
}

// ✅ 精简接口
public interface Repository<T> {
    T findById(Long id);
    List<T> findAll();
    T save(T entity);
    void delete(T entity);
}
```

**原则：**
- 类的方法越少越好
- 函数知道的变量越少越好
- 类的实例变量越少越好
- 隐藏数据、工具函数、常量和临时变量

### G9: 死代码 (Dead Code)

不会执行的代码应该删除。

```java
// ❌ 永远不会执行的代码
if (false) {
    doSomething();  // 死代码
}

try {
    // 不会抛出IOException的代码
} catch (IOException e) {
    handleError(e);  // 死代码
}

// ✅ 删除死代码
```

### G10: 垂直分离 (Vertical Separation)

变量和函数应在靠近使用它们的地方定义。

```java
// ❌ 变量声明距使用太远
public void process() {
    int count = 0;  // 声明在这里
    
    // ... 100行代码 ...
    
    count++;  // 使用在这里
}

// ✅ 就近声明
public void process() {
    // ... 其他代码 ...
    
    int count = 0;  // 声明靠近使用
    count++;
}
```

### G11: 不一致性 (Inconsistency)

如果你以某种方式做某事，所有类似的事都应该同样方式做。

```java
// ❌ 不一致的命名
public void processVerificationRequest() { }
public void handleDeletionReq() { }  // 不一致！
public void doDeletion() { }         // 不一致！

// ✅ 一致的命名
public void processVerificationRequest() { }
public void processDeletionRequest() { }
public void processUpdateRequest() { }
```

### G12: 杂乱 (Clutter)

没有用的东西都应该删除。

```java
// ❌ 杂乱
public class MyClass {
    private int unusedVariable;  // 未使用的变量
    
    public MyClass() { }  // 无实现的默认构造函数
    
    private void unusedMethod() { }  // 未调用的方法
    
    // 无信息的注释
    // This is a comment
}

// ✅ 清理干净
public class MyClass {
    // 只保留必要的代码
}
```

### G13: 人为耦合 (Artificial Coupling)

不相互依赖的东西不应该人为耦合。

```java
// ❌ 通用枚举放在特定类中
public class HourlyEmployee {
    public enum PayType { HOURLY, SALARY, COMMISSION }
    // ...
}

// ✅ 独立放置
public enum PayType { HOURLY, SALARY, COMMISSION }

public class HourlyEmployee {
    // ...
}
```

### G14: 特性依恋 (Feature Envy)

方法应对自己类的变量感兴趣，而非其他类的变量。

```java
// ❌ 特性依恋 - 过度使用另一个类的方法
public class HourlyPayCalculator {
    public Money calculateWeeklyPay(HourlyEmployee e) {
        int tenthRate = e.getTenthRate().getPennies();
        int tenthsWorked = e.getTenthsWorked();
        int straightTime = Math.min(400, tenthsWorked);
        int overTime = Math.max(0, tenthsWorked - straightTime);
        // ... 依恋 HourlyEmployee
    }
}

// ✅ 将计算移到数据所在的类
public class HourlyEmployee {
    public Money calculateWeeklyPay() {
        // 在这里计算，可以直接访问字段
    }
}
```

### G15: 选择器参数 (Selector Arguments)

避免用参数来选择函数行为。

```java
// ❌ 使用布尔参数选择行为
public int calculateWeeklyPay(boolean overtime) {
    double rate = overtime ? 1.5 : 1.0;
    // ...
}

// ✅ 拆分为独立函数
public int straightPay() {
    return getTenthsWorked() * getTenthRate();
}

public int overTimePay() {
    int overTimeTenths = Math.max(0, getTenthsWorked() - 400);
    return straightPay() + overTimeBonus(overTimeTenths);
}
```

### G16: 晦涩的意图 (Obscured Intent)

代码应尽可能表达清晰。

```java
// ❌ 晦涩的意图
public int m_otCalc() {
    return iThsWkd * iThsRte +
        (int) Math.round(0.5 * iThsRte *
            Math.max(0, iThsWkd - 400));
}

// ✅ 清晰的意图
public int calculateOvertimePay() {
    int straightTimePay = tenthsWorked * tenthRate;
    int overtimeTenths = Math.max(0, tenthsWorked - TENTHS_PER_WEEK);
    int overtimeBonus = (int) Math.round(OVERTIME_RATE * tenthRate * overtimeTenths);
    return straightTimePay + overtimeBonus;
}
```

### G17: 错位的责任 (Misplaced Responsibility)

代码应放在读者自然期望找到它的地方。

```java
// ❌ PI常量放在哪里？
public class Circle {
    public static final double PI = 3.14159;  // 不太对
}

// ✅ 放在数学/三角函数类中
public class Math {
    public static final double PI = 3.14159265358979;
}

// 或使用已有的 Math.PI
```

### G18: 不恰当的静态方法 (Inappropriate Static)

可能需要多态行为的方法不应该是静态的。

```java
// ❌ 可能需要多态的静态方法
HourlyPayCalculator.calculatePay(employee, overtimeRate);

// ✅ 实例方法，允许多态
employee.calculatePay();

// 或使用策略模式
payCalculator.calculatePay(employee);
```

**何时使用静态方法：**
- 不操作特定实例（如 `Math.max(a, b)`）
- 没有多态需求
- 所有数据来自参数

### G19: 使用解释性变量 (Use Explanatory Variables)

使用有意义的中间变量让程序可读。

```java
// ❌ 难以理解
Matcher match = headerPattern.matcher(line);
if (match.find()) {
    headers.put(match.group(1).toLowerCase(), match.group(2));
}

// ✅ 使用解释性变量
Matcher match = headerPattern.matcher(line);
if (match.find()) {
    String key = match.group(1);
    String value = match.group(2);
    headers.put(key.toLowerCase(), value);
}
```

### G20: 函数名应表达其行为 (Function Names Should Say What They Do)

```java
// ❌ 模糊的函数名
Date newDate = date.add(5);  // 加5天？5周？5小时？

// ✅ 清晰的函数名
Date newDate = date.plusDays(5);    // 明确是天
Date newDate = date.daysLater(5);   // 明确返回新对象
date.addDaysTo(5);                   // 明确修改原对象
```

### G21: 理解算法 (Understand the Algorithm)

在认为函数完成之前，确保你理解它是如何工作的。

```java
// ❌ 通过"摸索"让代码工作
public int calculate() {
    // 加了几个if，直到测试通过
    if (specialCase1) { ... }
    if (specialCase2) { ... }  // 不确定为什么需要这个
    // ...
}

// ✅ 理解后重构为清晰的实现
public int calculate() {
    // 明确的算法步骤
    int base = computeBase();
    int adjustment = computeAdjustment(base);
    return base + adjustment;
}
```

### G22: 使逻辑依赖变成物理依赖 (Make Logical Dependencies Physical)

依赖模块应该显式请求它所依赖的所有信息。

```java
// ❌ 逻辑依赖 - HourlyReporter假设页面大小
public class HourlyReporter {
    private final int PAGE_SIZE = 55;  // 假设formatter能处理55行
    
    public void generateReport(List<HourlyEmployee> employees) {
        if (page.size() == PAGE_SIZE)
            printAndClearItemList();
    }
}

// ✅ 物理依赖 - 显式询问
public class HourlyReporter {
    public void generateReport(List<HourlyEmployee> employees) {
        if (page.size() == formatter.getMaxPageSize())
            printAndClearItemList();
    }
}
```

### G23: 用多态替代 If/Else 或 Switch/Case (Prefer Polymorphism)

**"ONE SWITCH"规则**: 对于给定类型的选择，最多只能有一个switch语句。

```java
// ❌ 多处switch
public double calculatePay(Employee e) {
    switch (e.type) {
        case HOURLY: return calculateHourlyPay(e);
        case SALARY: return calculateSalaryPay(e);
    }
}

public boolean isPayday(Employee e) {
    switch (e.type) {  // 又一个switch！
        case HOURLY: return isHourlyPayday(e);
        case SALARY: return isSalaryPayday(e);
    }
}

// ✅ 使用多态
public abstract class Employee {
    public abstract double calculatePay();
    public abstract boolean isPayday();
}

public class HourlyEmployee extends Employee {
    public double calculatePay() { ... }
    public boolean isPayday() { ... }
}
```

### G24: 遵循标准约定 (Follow Standard Conventions)

团队应遵循基于行业规范的编码标准。

```java
// ✅ 一致的约定
// - 实例变量在类顶部
// - 公共方法在私有方法之前
// - 类名用PascalCase
// - 方法名用camelCase
// - 常量用UPPER_SNAKE_CASE
// - 大括号放在同一行或新行（团队统一即可）
```

### G25: 用命名常量替代魔法数字 (Replace Magic Numbers with Named Constants)

```java
// ❌ 魔法数字
double milesWalked = feetWalked / 5280.0;
if (age >= 18) { ... }
assertEquals(7777, employee.getId());

// ✅ 命名常量
double milesWalked = feetWalked / FEET_PER_MILE;
if (age >= LEGAL_ADULT_AGE) { ... }
assertEquals(HOURLY_EMPLOYEE_ID, 
    Employee.find(HOURLY_EMPLOYEE_NAME).getId());
```

**例外**: 自解释上下文中的常见数字
```java
// 可接受
double circumference = radius * Math.PI * 2;  // 2很明显
int dailyPay = hourlyRate * 8;  // 8小时工作日很明显
```

### G26: 精确 (Be Precise)

```java
// ❌ 不精确
// 期望查询只返回一条记录（没验证）
// 使用浮点数表示货币（精度问题）
// 避免锁因为"可能不会并发更新"（侥幸心理）
// 声明为 ArrayList 而不是 List（过度限制）

// ✅ 精确
// 检查查询是否真的只返回一条
User user = repository.findByEmail(email);
if (user == null) throw new NotFoundException();

// 使用整数或Money类处理货币
Money amount = Money.dollars(10, 50);  // $10.50

// 明确处理并发
synchronized (lock) {
    updateCounter();
}

// 使用接口类型
List<String> names = new ArrayList<>();
```

### G27: 结构优于约定 (Structure over Convention)

```java
// ❌ 依赖约定（可能被忘记）
// "所有实现都必须处理每种PayType"

// ✅ 用结构强制执行
public abstract class Employee {
    public abstract Money calculatePay();  // 必须实现
}
```

### G28: 封装条件 (Encapsulate Conditionals)

```java
// ❌ 裸露的复杂条件
if (timer.hasExpired() && !timer.isRecurrent()) {
    // ...
}

// ✅ 封装为描述性方法
if (shouldBeDeleted(timer)) {
    // ...
}

private boolean shouldBeDeleted(Timer timer) {
    return timer.hasExpired() && !timer.isRecurrent();
}
```

### G29: 避免否定条件 (Avoid Negative Conditionals)

```java
// ❌ 否定条件
if (!buffer.shouldNotCompact()) { }

// ✅ 肯定条件
if (buffer.shouldCompact()) { }
```

### G30: 函数只做一件事 (Functions Should Do One Thing)

```java
// ❌ 做多件事
public void pay() {
    for (Employee e : employees) {
        if (e.isPayday()) {
            Money pay = e.calculatePay();
            e.deliverPay(pay);
        }
    }
}

// ✅ 每个函数做一件事
public void pay() {
    for (Employee e : employees)
        payIfNecessary(e);
}

private void payIfNecessary(Employee e) {
    if (e.isPayday())
        calculateAndDeliverPay(e);
}

private void calculateAndDeliverPay(Employee e) {
    Money pay = e.calculatePay();
    e.deliverPay(pay);
}
```

### G31: 隐藏的时序耦合 (Hidden Temporal Couplings)

```java
// ❌ 隐藏的调用顺序
public class MoogDiver {
    public void dive(String reason) {
        saturateGradient();      // 必须先调用
        reticulateSplines();     // 然后调用这个
        diveForMoog(reason);     // 最后调用这个
    }
}

// ✅ 暴露时序耦合
public class MoogDiver {
    public void dive(String reason) {
        Gradient gradient = saturateGradient();
        List<Spline> splines = reticulateSplines(gradient);
        diveForMoog(splines, reason);
    }
}
```

### G32: 不要随意 (Don't Be Arbitrary)

代码结构应有理由，并通过结构传达这个理由。

```java
// ❌ 随意的嵌套类
public class AliasLinkWidget extends ParentWidget {
    public static class VariableExpandingWidgetRoot {
        // 这个类被其他不相关的类使用
        // 不应该嵌套在AliasLinkWidget中
    }
}

// ✅ 独立的顶级类
public class VariableExpandingWidgetRoot {
    // ...
}
```

### G33: 封装边界条件 (Encapsulate Boundary Conditions)

```java
// ❌ 边界条件散落各处
if (level + 1 < tags.length) {
    parts = new Parse(body, tags, level + 1, offset + endTag);
}

// ✅ 封装边界条件
int nextLevel = level + 1;
if (nextLevel < tags.length) {
    parts = new Parse(body, tags, nextLevel, offset + endTag);
}
```

### G34: 函数应只降一级抽象 (Functions Should Descend Only One Level of Abstraction)

```java
// ❌ 混合抽象层次
public String render() {
    StringBuffer html = new StringBuffer("<hr");
    if (size > 0)
        html.append(" size=\"").append(size + 1).append("\"");
    html.append(">");
    return html.toString();
}

// ✅ 分离抽象层次
public String render() {
    HtmlTag hr = new HtmlTag("hr");
    if (extraDashes > 0)
        hr.addAttribute("size", hrSize(extraDashes));
    return hr.html();
}

private String hrSize(int height) {
    int hrSize = height + 1;
    return String.format("%d", hrSize);
}
```

### G35: 配置数据放在高层 (Keep Configurable Data at High Levels)

```java
// ❌ 配置值埋在低层
public class LowLevelModule {
    public void process() {
        if (port == 0) port = 80;  // 默认值埋在这里
    }
}

// ✅ 配置值在高层
public class Arguments {
    public static final String DEFAULT_PATH = ".";
    public static final int DEFAULT_PORT = 80;
    public static final int DEFAULT_VERSION_DAYS = 14;
}

public static void main(String[] args) {
    Arguments arguments = parseCommandLine(args);
    // 配置值从这里传递下去
}
```

### G36: 避免传递导航 (Avoid Transitive Navigation)

迪米特法则 / 编写"害羞"代码。

```java
// ❌ 火车残骸
a.getB().getC().doSomething();

// ✅ 直接与协作者交流
myCollaborator.doSomething();
```

---

## 五、Java 特有 (Java)

### J1: 使用通配符避免长导入列表 (Avoid Long Import Lists by Using Wildcards)

```java
// ❌ 长导入列表
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// ✅ 通配符导入
import java.util.*;
```

### J2: 不要继承常量 (Don't Inherit Constants)

```java
// ❌ 继承常量接口
public abstract class Employee implements PayrollConstants {
    // TENTHS_PER_WEEK 从哪里来？
}

// ✅ 静态导入
import static PayrollConstants.*;

public class HourlyEmployee extends Employee {
    public Money calculatePay() {
        int straightTime = Math.min(tenthsWorked, TENTHS_PER_WEEK);
        // ...
    }
}
```

### J3: 使用枚举替代常量 (Constants versus Enums)

```java
// ❌ 使用 public static final int
public static final int HOURLY = 1;
public static final int SALARY = 2;

// ✅ 使用枚举
public enum PayType {
    HOURLY, SALARY, COMMISSION
}

// 枚举可以有方法和字段
public enum HourlyPayGrade {
    APPRENTICE { public double rate() { return 1.0; } },
    JOURNEYMAN { public double rate() { return 1.5; } },
    MASTER { public double rate() { return 2.0; } };
    
    public abstract double rate();
}
```

---

## 六、命名 (Names)

### N1: 选择描述性名称 (Choose Descriptive Names)

```java
// ❌ 无意义的名称
public int x() {
    int q = 0;
    int z = 0;
    for (int kk = 0; kk < 10; kk++) {
        if (l[z] == 10) {
            q += 10 + (l[z + 1] + l[z + 2]);
            z += 1;
        }
        // ...
    }
    return q;
}

// ✅ 描述性名称
public int score() {
    int score = 0;
    int frame = 0;
    for (int frameNumber = 0; frameNumber < 10; frameNumber++) {
        if (isStrike(frame)) {
            score += 10 + nextTwoBallsForStrike(frame);
            frame += 1;
        }
        // ...
    }
    return score;
}
```

### N2: 在适当抽象层次选择名称 (Choose Names at the Appropriate Level of Abstraction)

```java
// ❌ 名称暴露实现
public interface Modem {
    boolean dial(String phoneNumber);  // 不是所有modem都用电话
    String getConnectedPhoneNumber();
}

// ✅ 抽象层次的名称
public interface Modem {
    boolean connect(String connectionLocator);
    String getConnectedLocator();
}
```

### N3: 使用标准术语 (Use Standard Nomenclature Where Possible)

```java
// ✅ 使用设计模式名称
public class AutoHangupModemDecorator { }  // Decorator模式
public class UserFactory { }                // Factory模式
public class OrderBuilder { }               // Builder模式

// ✅ 使用项目的通用语言
public class Invoice { }      // 而不是 Bill, Receipt, etc.
public class Customer { }     // 而不是 Client, User, etc.
```

### N4: 无歧义的名称 (Unambiguous Names)

```java
// ❌ 歧义的名称
private String doRename() {
    // 里面还调用了 renamePage()
    // doRename 和 renamePage 有什么区别？
}

// ✅ 无歧义
private String renamePageAndOptionallyAllReferences() {
    // 名称长但清晰
}
```

### N5: 长作用域用长名称 (Use Long Names for Long Scopes)

```java
// ✅ 短作用域用短名称
private void rollMany(int n, int pins) {
    for (int i = 0; i < n; i++)  // i 很合适
        g.roll(pins);
}

// ✅ 长作用域用长名称
private int totalNumberOfProcessedRequests;  // 类字段
private UserAccountValidationService userAccountValidationService;
```

### N6: 避免编码 (Avoid Encodings)

```java
// ❌ 匈牙利命名法和前缀
private String m_name;        // m_ 前缀
private int iCount;           // 类型前缀
private String vis_imageUrl;  // 系统前缀

// ✅ 清晰的名称
private String name;
private int count;
private String imageUrl;
```

### N7: 名称应描述副作用 (Names Should Describe Side-Effects)

```java
// ❌ 名称隐藏副作用
public ObjectOutputStream getOos() throws IOException {
    if (m_oos == null) {
        m_oos = new ObjectOutputStream(m_socket.getOutputStream());
    }
    return m_oos;
}

// ✅ 名称揭示副作用
public ObjectOutputStream createOrReturnOos() throws IOException {
    if (m_oos == null) {
        m_oos = new ObjectOutputStream(m_socket.getOutputStream());
    }
    return m_oos;
}
```

---

## 七、测试 (Tests)

### T1: 测试不充分 (Insufficient Tests)

测试套件应测试所有可能出错的地方。

```java
// ❌ 测试不充分
@Test
void testAdd() {
    assertEquals(4, calculator.add(2, 2));
    // 只测试了一种情况
}

// ✅ 充分测试
@Test
void testAdd() {
    assertEquals(4, calculator.add(2, 2));
    assertEquals(0, calculator.add(0, 0));
    assertEquals(-1, calculator.add(-2, 1));
    assertEquals(Integer.MAX_VALUE, calculator.add(Integer.MAX_VALUE - 1, 1));
}
```

### T2: 使用覆盖率工具 (Use a Coverage Tool!)

```java
// ✅ 使用JaCoCo, Clover等工具
// 可视化未覆盖的代码
// 目标至少80%行覆盖率
```

### T3: 不要跳过简单测试 (Don't Skip Trivial Tests)

简单测试易于编写，其文档价值高于编写成本。

### T4: 被忽略的测试是对模糊性的质疑 (An Ignored Test Is a Question about an Ambiguity)

```java
// 不确定需求时
@Ignore("Unclear if negative values should be supported")
@Test
void testNegativeInput() {
    // 这是一个待确认的问题
}
```

### T5: 测试边界条件 (Test Boundary Conditions)

```java
@Test
void testBoundaryConditions() {
    // 空集合
    assertThrows(EmptyException.class, () -> emptyStack.pop());
    
    // 满容量
    fillStack(stack, MAX_SIZE);
    assertThrows(FullException.class, () -> stack.push(item));
    
    // 恰好在边界
    assertEquals(MAX_SIZE, stack.size());
}
```

### T6: 在Bug附近彻底测试 (Exhaustively Test Near Bugs)

Bug往往成群出现。发现一个bug后，彻底测试那个函数。

### T7: 失败模式能揭示问题 (Patterns of Failure Are Revealing)

观察测试失败的模式可以帮助诊断问题。

```java
// 如果所有输入超过5个字符的测试都失败
// 或者所有负数作为第二参数的测试都失败
// 这个模式就揭示了问题所在
```

### T8: 测试覆盖模式能揭示问题 (Test Coverage Patterns Can Be Revealing)

查看通过测试执行/未执行的代码，可以找出失败测试失败的原因。

### T9: 测试应该快 (Tests Should Be Fast)

慢测试不会被运行。保持测试快速。

```java
// ❌ 慢测试
@Test
void testWithRealDatabase() {
    // 连接真实数据库，执行5秒
}

// ✅ 快速测试
@Test
void testWithMockDatabase() {
    when(repository.findById(1L)).thenReturn(user);
    // 毫秒级完成
}
```

---

## 速查表

| 类别 | 规则编号 | 规则名称 |
|-----|---------|---------|
| **注释** | C1-C5 | 不恰当信息、废弃、冗余、糟糕、注释代码 |
| **环境** | E1-E2 | 构建/测试需多步 |
| **函数** | F1-F4 | 参数过多、输出参数、标识参数、死函数 |
| **一般** | G1-G36 | 多语言、明显行为未实现、边界错误... |
| **Java** | J1-J3 | 通配符导入、不继承常量、使用枚举 |
| **命名** | N1-N7 | 描述性、抽象层次、标准术语... |
| **测试** | T1-T9 | 测试不充分、覆盖率、边界条件... |

---

## 核心引用

> "Clean code is not written by following a set of rules. You don't become a software craftsman by learning a list of heuristics. Professionalism and craftsmanship come from values that drive disciplines."
> 
> — Robert C. Martin

> "Every time you see duplication in the code, it represents a missed opportunity for abstraction."
> 
> — Robert C. Martin

> "Find and eliminate duplication wherever you can."
> 
> — DRY Principle

---

## 技术栈
- **语言**: Java 17+
- **测试框架**: JUnit 5, Mockito
- **覆盖率工具**: JaCoCo, Clover
- **设计模式**: Template Method, Strategy, Factory, Decorator
