# 低级 BUG 预防规范

> 避免常见的低级错误，减少代码评审返工和线上事故

---

## 概述

### 做了什么
提供一套低级 BUG 预防清单，覆盖空指针、边界条件、资源泄漏等高频问题。

### 为什么需要
低级 BUG 会让领导觉得团队不专业，且往往在生产环境才暴露，修复成本是开发阶段的 10-100 倍。

### 什么时候必须用
- 写任何新代码之前过一遍清单
- 代码自测时逐项检查
- Code Review 时作为评审依据

---

## 低级 BUG 分类速查

| 类型 | 典型场景 | 危害等级 |
|------|---------|---------|
| 空指针 | 对象未初始化就使用 | 🔴 高 |
| 边界问题 | 数组越界、分页边界 | 🔴 高 |
| 资源泄漏 | 连接/流/锁未释放 | 🔴 高 |
| 并发问题 | 线程不安全的操作 | 🔴 高 |
| 逻辑漏洞 | 条件判断遗漏 | 🟡 中 |
| 拼写错误 | 变量名/字段名写错 | 🟡 中 |
| 硬编码 | 魔法数字/固定URL | 🟡 中 |
| 格式问题 | 日期格式、数字精度 | 🟡 中 |

---

## 1. 空指针/空值问题

### 问题空间
程序运行时访问了 null/nil/None/undefined 对象，导致崩溃。

### 核心语义
**任何对象在使用前，必须确认它不为空。**

### 【强制】规则清单

| 规则 | 说明 |
|------|------|
| 外部输入必须判空 | 所有从外部获取的数据（API参数、数据库查询、配置读取）都可能为空 |
| 链式调用必须判空 | `a.getB().getC()` 每一层都可能返回 null |
| 集合元素必须判空 | 从 List/Map 取出的元素可能为 null |
| 可选类型优先 | 使用 Optional/Maybe 等类型包装可能为空的值 |

### 反例与正例

```java
// ❌ 反例 - 直接使用外部输入
public void process(User user) {
    String name = user.getName().toUpperCase();
}

// ✅ 正例 - 先判空再使用
public void process(User user) {
    if (user == null || user.getName() == null) {
        throw new IllegalArgumentException("用户或用户名不能为空");
    }
    String name = user.getName().toUpperCase();
}
```

```python
# ❌ 反例 - 直接访问字典
def get_user_name(data):
    return data["user"]["name"].upper()

# ✅ 正例 - 安全访问
def get_user_name(data):
    user = data.get("user")
    if user is None:
        return None
    name = user.get("name")
    return name.upper() if name else None
```

### 决策规则

| 条件 | 建议 | 强度 |
|------|------|------|
| 数据来自外部（API/DB/配置） | 必须判空 | 强制 |
| 数据来自内部且确定初始化 | 可不判空，但加注释说明 | 推荐 |
| 不确定数据来源 | 一律判空 | 强制 |

### 记忆锚点
> **用之前，问一句：它会不会是空的？**

---

## 2. 边界条件问题

### 问题空间
程序只考虑了"正常情况"，忽略了边界值导致异常。

### 核心语义
**代码必须处理最小值、最大值、空集合、零值等边界情况。**

### 【强制】检查清单

| 边界类型 | 必须考虑 |
|---------|---------|
| 数组/列表 | 空列表、单元素、首尾元素 |
| 分页查询 | 第一页、最后一页、超出范围 |
| 数值计算 | 0、负数、最大值、溢出 |
| 字符串 | 空串、空白串、超长串 |
| 日期时间 | 跨天、跨月、跨年、闰年 |
| 金额 | 0元、负数、小数精度 |

### 反例与正例

```java
// ❌ 反例 - 未处理空列表
public User getFirst(List<User> users) {
    return users.get(0);
}

// ✅ 正例 - 处理边界
public User getFirst(List<User> users) {
    if (users == null || users.isEmpty()) {
        return null;
    }
    return users.get(0);
}
```

```javascript
// ❌ 反例 - 分页未处理边界
function getPage(list, page, size) {
    const start = page * size;
    return list.slice(start, start + size);
}

// ✅ 正例 - 处理分页边界
function getPage(list, page, size) {
    if (!list || list.length === 0) return [];
    if (page < 0) page = 0;
    if (size <= 0) size = 10;
    
    const start = page * size;
    if (start >= list.length) return [];
    
    return list.slice(start, start + size);
}
```

### 记忆锚点
> **正常流程写完后，问：空的呢？零呢？负数呢？超大呢？**

---

## 3. 资源泄漏问题

### 问题空间
打开的资源（连接、文件、锁）没有正确关闭，导致资源耗尽。

### 核心语义
**谁申请谁释放，用完必须关，异常也得关。**

### 【强制】规则清单

| 资源类型 | 必须操作 |
|---------|---------|
| 数据库连接 | 使用连接池，用完归还 |
| 文件句柄 | 使用 try-with-resources / with 语句 |
| HTTP 连接 | 关闭 response body |
| 锁 | finally 中释放 |
| 线程池 | 程序退出时 shutdown |

### 反例与正例

```java
// ❌ 反例 - 异常时资源未关闭
public void readFile(String path) throws IOException {
    FileInputStream fis = new FileInputStream(path);
    byte[] data = fis.readAllBytes();
    fis.close();
}

// ✅ 正例 - 使用 try-with-resources
public void readFile(String path) throws IOException {
    try (FileInputStream fis = new FileInputStream(path)) {
        byte[] data = fis.readAllBytes();
    }
}
```

```python
# ❌ 反例 - 手动关闭可能遗漏
def read_file(path):
    f = open(path, 'r')
    content = f.read()
    f.close()
    return content

# ✅ 正例 - 使用 with 语句
def read_file(path):
    with open(path, 'r') as f:
        return f.read()
```

### 记忆锚点
> **打开了就要关，用 try-finally 或语法糖。**

---

## 4. 并发安全问题

### 问题空间
多线程访问共享资源，导致数据不一致或死锁。

### 核心语义
**共享可变状态 = 危险区域，必须加锁或改用不可变。**

### 【强制】规则清单

| 场景 | 必须操作 |
|------|---------|
| 共享计数器 | 使用原子类或加锁 |
| 共享集合 | 使用线程安全集合 |
| 懒加载单例 | 使用双重检查或静态内部类 |
| 跨方法状态 | 避免使用成员变量存临时状态 |

### 反例与正例

```java
// ❌ 反例 - 非线程安全的计数器
private int count = 0;
public void increment() {
    count++;
}

// ✅ 正例 - 使用原子类
private AtomicInteger count = new AtomicInteger(0);
public void increment() {
    count.incrementAndGet();
}
```

```go
// ❌ 反例 - 并发写 map
var cache = make(map[string]string)
func Set(k, v string) {
    cache[k] = v
}

// ✅ 正例 - 使用 sync.Map
var cache sync.Map
func Set(k, v string) {
    cache.Store(k, v)
}
```

### 记忆锚点
> **多个人同时改一个东西，必出事。**

---

## 5. 逻辑遗漏问题

### 问题空间
条件分支没覆盖所有情况，导致某些场景无法正确处理。

### 核心语义
**if 有几个分支，else 是什么，switch 要有 default。**

### 【强制】规则清单

| 规则 | 说明 |
|------|------|
| switch 必须有 default | 处理意外值 |
| if-else 必须覆盖 | else 分支不能省略 |
| 枚举必须穷举 | 新增枚举值时编译器会提醒 |
| 状态机要完整 | 每个状态的每个事件都要处理 |

### 反例与正例

```java
// ❌ 反例 - 缺少 default
public String getStatusText(int status) {
    switch (status) {
        case 1: return "待处理";
        case 2: return "处理中";
        case 3: return "已完成";
    }
    // status=4 时返回 null
}

// ✅ 正例 - 有 default
public String getStatusText(int status) {
    switch (status) {
        case 1: return "待处理";
        case 2: return "处理中";
        case 3: return "已完成";
        default: return "未知状态";
    }
}
```

### 记忆锚点
> **else 和 default 是你的安全网。**

---

## 6. 拼写与复制错误

### 问题空间
变量名写错、复制粘贴后忘改，导致数据错乱。

### 核心语义
**复制代码后，必须逐行检查变量名和字段名。**

### 【强制】规则清单

| 规则 | 说明 |
|------|------|
| 禁止手写 JSON 字段名 | 使用常量或注解生成 |
| 复制后必检查 | 复制代码后逐行确认变量名 |
| 使用 IDE 重构 | 改名用 IDE 的 Rename 功能 |
| 统一术语表 | 团队维护统一的命名词汇表 |

### 反例与正例

```java
// ❌ 反例 - 复制粘贴后未改变量
public void copyUser(User source, User target) {
    target.setName(source.getName());
    target.setAge(source.getAge());
    target.setEmail(source.getName());  // 复制后忘改
}

// ✅ 正例 - 使用工具或仔细检查
public void copyUser(User source, User target) {
    target.setName(source.getName());
    target.setAge(source.getAge());
    target.setEmail(source.getEmail());
}
```

### 记忆锚点
> **复制一时爽，检查不能忘。**

---

## 7. 硬编码问题

### 问题空间
魔法数字/固定值散落在代码中，修改困难且容易遗漏。

### 核心语义
**所有可能变化的值，都要抽取为常量或配置。**

### 【强制】规则清单

| 类型 | 处理方式 |
|------|---------|
| 状态码/类型码 | 定义为枚举或常量 |
| URL/端口 | 放入配置文件 |
| 超时时间 | 配置化，支持动态调整 |
| 业务阈值 | 配置化，便于运营调整 |

### 反例与正例

```java
// ❌ 反例 - 魔法数字
if (user.getType() == 1) {
    // 普通用户
} else if (user.getType() == 2) {
    // VIP 用户
}

// ✅ 正例 - 使用常量或枚举
public enum UserType {
    NORMAL(1), VIP(2);
    private final int code;
}

if (user.getType() == UserType.NORMAL.getCode()) {
    // 普通用户
}
```

### 记忆锚点
> **数字藏代码里，三月后你也不认识。**

---

## 8. 时间与精度问题

### 问题空间
日期格式不统一、浮点数精度丢失，导致数据错误。

### 核心语义
**日期用标准格式，金额用 BigDecimal，时区要明确。**

### 【强制】规则清单

| 类型 | 规则 |
|------|------|
| 日期存储 | 统一使用 UTC 或带时区 |
| 日期格式 | 统一使用 ISO 8601 |
| 金额计算 | 禁止使用 float/double |
| 比例计算 | 注意小数点位数和舍入规则 |

### 反例与正例

```java
// ❌ 反例 - 浮点数算金额
double price = 0.1 + 0.2;
// price = 0.30000000000000004

// ✅ 正例 - BigDecimal 算金额
BigDecimal price = new BigDecimal("0.1")
    .add(new BigDecimal("0.2"));
// price = 0.3
```

### 记忆锚点
> **钱的事，别用浮点数。**

---

## 9. 数据库必填字段遗漏

### 问题空间
数据库表的某些字段是 NOT NULL（必填），但 Java 实体类中对应字段没有赋值就执行 insert，导致数据库报错。

### 核心语义
**数据库必填字段，代码层必须有校验兜底，不能等到 SQL 执行才报错。**

### 失败场景分析

| 场景 | 问题 |
|------|------|
| 前端漏传字段 | 后端没校验，直接入库报错 |
| 业务逻辑遗漏赋值 | 某条分支没给必填字段赋值 |
| 默认值为 null | 实体类字段没有默认值 |
| 批量导入 | 部分数据缺失必填字段 |

### 【强制】规则清单

| 规则 | 说明 |
|------|------|
| 实体类加校验注解 | 必填字段加 @NotNull / @NotBlank / @NotEmpty |
| Controller 层开启校验 | 入参加 @Valid 或 @Validated |
| Service 层二次校验 | 关键业务逻辑中再次确认必填字段 |
| 建表时设默认值 | 能有默认值的字段设置 DEFAULT |
| 单元测试覆盖 | 测试必填字段为空的场景 |

### 反例与正例

```java
// ❌ 反例 - 实体类无校验，直接入库
@Data
public class User {
    private Long id;
    private String name;      // 数据库 NOT NULL，但这里没标注
    private String email;     // 数据库 NOT NULL
    private Integer status;   // 数据库 NOT NULL
}

@Service
public class UserService {
    public void createUser(User user) {
        // 没有任何校验，直接插入
        userMapper.insert(user);
        // 如果 name/email/status 为 null，数据库报错
    }
}
```

```java
// ✅ 正例 - 多层校验保护

// 1. 实体类/DTO 加校验注解
@Data
public class UserCreateDTO {
    
    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名最长50字符")
    private String name;
    
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @NotNull(message = "状态不能为空")
    private Integer status;
}

// 2. Controller 层开启校验
@RestController
public class UserController {
    
    @PostMapping("/users")
    public Result createUser(@Valid @RequestBody UserCreateDTO dto) {
        // @Valid 会自动校验，不通过直接返回 400
        userService.createUser(dto);
        return Result.success();
    }
}

// 3. Service 层防御性校验（可选，用于非 Controller 调用场景）
@Service
public class UserService {
    
    public void createUser(UserCreateDTO dto) {
        // 防御性校验，防止内部调用绕过 Controller
        Assert.hasText(dto.getName(), "用户名不能为空");
        Assert.hasText(dto.getEmail(), "邮箱不能为空");
        Assert.notNull(dto.getStatus(), "状态不能为空");
        
        User user = convertToEntity(dto);
        userMapper.insert(user);
    }
}
```

### 常用校验注解速查

| 注解 | 适用类型 | 说明 |
|------|---------|------|
| `@NotNull` | 任意类型 | 不能为 null |
| `@NotBlank` | String | 不能为 null 且 trim 后长度 > 0 |
| `@NotEmpty` | String/集合 | 不能为 null 且长度/大小 > 0 |
| `@Size(min, max)` | String/集合 | 长度/大小范围 |
| `@Min` / `@Max` | 数字 | 最小值/最大值 |
| `@Email` | String | 邮箱格式 |
| `@Pattern` | String | 正则匹配 |

### 全局异常处理

```java
// 统一处理校验失败的异常
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining("; "));
        
        return Result.fail(400, message);
    }
}
```

### 决策规则

| 条件 | 建议 | 强度 |
|------|------|------|
| 数据库字段 NOT NULL | 实体类/DTO 必须加 @NotNull 或 @NotBlank | 强制 |
| 外部 API 入参 | Controller 必须加 @Valid | 强制 |
| 内部服务调用 | Service 层加 Assert 校验 | 推荐 |
| 批量导入场景 | 先校验整批数据，再批量入库 | 强制 |

### 数据库层兜底

```sql
-- 能有默认值的字段，建表时设置 DEFAULT
CREATE TABLE user (
    id BIGINT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    status INT NOT NULL DEFAULT 1,          -- 有默认值
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,  -- 有默认值
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### 记忆锚点
> **数据库说必填，代码层就要拦，别等 SQL 报错才发现。**

---

## 代码自测 Checklist

提交代码前，逐项检查：

### 必查项（10 秒快速扫描）

| # | 检查项 | 通过 |
|---|--------|------|
| 1 | 外部输入都判空了吗？ | ☐ |
| 2 | 列表/数组访问前检查长度了吗？ | ☐ |
| 3 | 资源都用 try-finally 关闭了吗？ | ☐ |
| 4 | switch 有 default 吗？ | ☐ |
| 5 | 复制的代码变量名都改对了吗？ | ☐ |
| 6 | 没有魔法数字吗？ | ☐ |
| 7 | 金额用 BigDecimal 了吗？ | ☐ |
| 8 | 日期处理考虑时区了吗？ | ☐ |
| 9 | 数据库必填字段加 @NotNull 了吗？ | ☐ |
| 10 | Controller 入参加 @Valid 了吗？ | ☐ |

### 进阶项（并发相关）

| # | 检查项 | 通过 |
|---|--------|------|
| 9 | 共享变量有并发保护吗？ | ☐ |
| 10 | 锁的获取顺序一致吗？ | ☐ |

---

## 低级 BUG 预防工具

| 工具类型 | 推荐工具 | 能发现的问题 |
|---------|---------|-------------|
| 静态分析 | SonarQube, SpotBugs, ESLint | 空指针、资源泄漏、代码异味 |
| 代码格式 | Prettier, gofmt, Black | 格式问题 |
| 类型检查 | TypeScript, mypy | 类型错误 |
| 单元测试 | JUnit, pytest, Jest | 边界条件、逻辑错误 |
| 代码评审 | Pull Request + Checklist | 所有类型 |

---

## 团队实践建议

### 1. 建立代码评审清单
把本文的 Checklist 加入团队的 PR 模板。

### 2. 配置静态分析
在 CI 中集成 SonarQube 或类似工具，阻断高危问题合入。

### 3. 编写边界测试
单元测试必须覆盖：空值、零值、边界值、异常值。

### 4. 复盘低级 BUG
每次线上问题复盘，提炼成规则加入 Checklist。

---

## 禁则速查表

| ❌ 禁止 | ✅ 正确做法 | 原因 |
|--------|-----------|------|
| 不判空就使用外部数据 | 先判空再使用 | 防 NPE |
| 不检查长度就取元素 | 先检查 isEmpty | 防越界 |
| 手动 close 资源 | 用 try-with-resources | 防泄漏 |
| 省略 else/default | 始终写完整分支 | 防遗漏 |
| 复制代码后不检查 | 逐行确认变量名 | 防笔误 |
| 代码中写死数字 | 抽取为常量/配置 | 便维护 |
| float/double 算金额 | 用 BigDecimal | 防精度丢失 |
| 不加锁操作共享变量 | 用原子类或锁 | 防并发问题 |
| 必填字段不加校验注解 | 加 @NotNull/@NotBlank | 防入库报错 |
| Controller 不加 @Valid | 入参加 @Valid 校验 | 提前拦截非法数据 |

---

## 参考资料

| 来源 | 说明 |
|------|------|
| 阿里巴巴 Java 开发手册 | 大量低级 BUG 预防经验 |
| Effective Java | 编程最佳实践 |
| Clean Code | 代码质量提升方法论 |
