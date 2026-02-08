# Sa-Token 注解鉴权演示模块

本模块演示如何使用 Sa-Token 的注解鉴权功能，彻底分离鉴权代码和业务逻辑。

## 核心概念

### 问题：鉴权代码和业务逻辑混在一起

```java
// 传统方式 - 鉴权代码和业务逻辑混在一起
@RequestMapping("add")
public Result add() {
    if (currentUser == null) {
        return Result.fail("请先登录");
    }
    if (!currentUser.hasRole("admin")) {
        return Result.fail("无权限");
    }
    if (currentUser.isDisabled()) {
        return Result.fail("账号已被封禁");
    }
    // 真正的业务逻辑从这里才开始...
    return Result.success("添加用户成功");
}
```

### 解决方案：使用 Sa-Token 注解

```java
// 注解方式 - 鉴权逻辑清晰分离
@SaCheckRole("admin")
@RequestMapping("add")
public Result add() {
    // 直接写业务逻辑，代码简洁明了
    return Result.success("添加用户成功");
}
```

## 项目结构

```
sa-token-demo-annotation
├── src/main/java/com/it666/annotation
│   ├── AnnotationApplication.java      # 启动类
│   ├── config
│   │   └── SaTokenConfigure.java      # Sa-Token 拦截器配置
│   └── controller
│       ├── BasicAnnotationController.java      # 基本注解演示
│       ├── AdvancedAnnotationController.java   # 高级注解演示
│       ├── CombinedAnnotationController.java   # 组合注解演示
│       └── AuthController.java                # 认证辅助接口
└── src/main/resources
    └── application.yml
```

## 启动方式

```bash
# 编译项目
mvn clean compile

# 运行项目
mvn spring-boot:run -pl sa-token-demo-annotation
```

启动成功后访问：http://localhost:8083

## 核心配置

### 1. 注册拦截器（必须）

```java
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor()).addPathPatterns("/**");
    }
}
```

**注意**：不注册拦截器的话，所有注解都不会生效！

## 注解列表

### 基本注解

| 注解 | 说明 | 示例 |
|------|------|------|
| @SaCheckLogin | 登录校验 | `@SaCheckLogin` |
| @SaCheckRole | 角色校验 | `@SaCheckRole("admin")` |
| @SaCheckPermission | 权限校验 | `@SaCheckPermission("user:add")` |
| @SaCheckSafe | 二级认证校验 | `@SaCheckSafe()` |
| @SaCheckHttpBasic | HttpBasic 认证 | `@SaCheckHttpBasic(account = "sa:123456")` |
| @SaCheckHttpDigest | HttpDigest 认证 | `@SaCheckHttpDigest(value = "sa:123456")` |
| @SaCheckDisable | 账号服务封禁校验 | `@SaCheckDisable("comment")` |
| @SaIgnore | 忽略校验 | `@SaIgnore` |
| @SaCheckOr | 组合校验（OR） | `@SaCheckOr(login = @SaCheckLogin, ...)` |

### 高级用法

#### OR 模式（满足任一条件）

```java
@SaCheckPermission(value = {"user-add", "user-all"}, mode = SaMode.OR)
```

#### AND 模式（同时满足所有条件，默认）

```java
@SaCheckPermission(value = {"user-add", "user-update"}, mode = SaMode.AND)
```

#### 角色和权限混合校验

```java
// 有 user.add 权限 或 admin 角色均可
@SaCheckPermission(value = "user.add", orRole = "admin")
```

#### 叠加注解实现 AND 效果

```java
@SaCheckLogin
@SaCheckRole("admin")
@SaCheckPermission("user.add")
// 以上三个必须同时满足
```

## API 接口说明

### 认证辅助接口（/auth/**）

| 接口 | 说明 |
|------|------|
| POST /auth/login | 登录（普通用户） |
| POST /auth/loginAdmin | 登录（管理员） |
| POST /auth/loginUser | 登录（普通用户，有部分权限） |
| POST /auth/loginSuperAdmin | 登录（超级管理员） |
| GET /auth/getInfo | 获取当前登录用户信息 |
| POST /auth/logout | 登出 |
| POST /auth/openSafe | 开启二级认证 |
| POST /auth/closeSafe | 关闭二级认证 |
| GET /auth/checkSafe | 检查二级认证状态 |
| POST /auth/disableService | 封禁账号服务 |
| POST /auth/untieDisableService | 解封账号服务 |
| GET /auth/checkDisable | 检查服务封禁状态 |

### 基本注解演示（/basic/**）

| 接口 | 说明 | 所需条件 |
|------|------|----------|
| GET /basic/info | 查询用户信息 | 需要登录 |
| GET /basic/add | 用户增加 | 需要 super-admin 角色 |
| GET /basic/userAdd | 用户增加 | 需要 user-add 权限 |
| GET /basic/updatePwd | 修改密码 | 需要二级认证 |
| GET /basic/dashboard | 管理后台 | 需要 HttpBasic 认证 |
| GET /basic/report | 报表数据 | 需要 HttpDigest 认证 |
| GET /basic/send | 发表评论 | comment 服务未被封禁 |

### 高级注解演示（/advanced/**）

| 接口 | 说明 | 所需条件 |
|------|------|----------|
| GET /advanced/atJurOr | OR模式权限 | user-add 或 user-all 或 user-delete |
| GET /advanced/atJurAnd | AND模式权限 | 同时具备 user-add 和 user-update |
| GET /advanced/userAdd | 用户增加 | user.add 权限 或 admin 角色 |
| GET /advanced/multiRoleOr | 多角色OR | admin 或 manager 或 staff |
| GET /advanced/multiRoleAnd | 多角色AND | 同时具备 admin、manager、staff |
| GET /advanced/multiCheckAnd | 多重校验AND | 登录 + admin角色 + user.add权限 |

### 组合注解演示（/combined/**）

| 接口 | 说明 | 所需条件 |
|------|------|----------|
| GET /combined/health | 健康检查 | 无需登录（@SaIgnore） |
| GET /combined/normal | 正常接口 | 需要登录 |
| GET /combined/test | 组合校验 | 满足多种校验中的任意一种 |
| GET /combined/multiAccount | 多账号体系 | 后台登录 或 用户登录 |
| GET /combined/adminOnly | 管理员专属 | 登录 + admin角色 |
| POST /combined/changePassword | 修改密码 | 登录 + 二级认证 |

## 测试示例

### 1. 测试登录校验

```bash
# 1. 先登录
curl -X POST "http://localhost:8083/auth/login?username=test"

# 2. 访问需要登录的接口（使用返回的 token）
curl -H "satoken: xxx" "http://localhost:8083/basic/info"
```

### 2. 测试角色校验

```bash
# 1. 以管理员身份登录
curl -X POST "http://localhost:8083/auth/loginAdmin"

# 2. 访问需要管理员角色的接口
curl -H "satoken: xxx" "http://localhost:8083/basic/add"
```

### 3. 测试二级认证

```bash
# 1. 登录
curl -X POST "http://localhost:8083/auth/login"

# 2. 开启二级认证
curl -X POST "http://localhost:8083/auth/openSafe?password=123456" \
  -H "satoken: xxx"

# 3. 访问需要二级认证的接口
curl "http://localhost:8083/basic/updatePwd" -H "satoken: xxx"
```

### 4. 测试服务封禁

```bash
# 1. 登录
curl -X POST "http://localhost:8083/auth/login"

# 2. 封禁 comment 服务
curl -X POST "http://localhost:8083/auth/disableService?service=comment" \
  -H "satoken: xxx"

# 3. 尝试发表评论（会失败）
curl "http://localhost:8083/basic/send" -H "satoken: xxx"

# 4. 解封服务
curl -X POST "http://localhost:8083/auth/untieDisableService?service=comment" \
  -H "satoken: xxx"
```

### 5. 测试 @SaIgnore

```bash
# 无需登录即可访问
curl "http://localhost:8083/combined/health"
```

## 注意事项

1. **必须注册拦截器**：否则所有注解都不会生效
2. **@SaIgnore 优先级最高**：即使同时写了 @SaCheckLogin 和 @SaIgnore，结果也是不鉴权
3. **orRole 的两种写法**：
   - `orRole = {"admin", "manager"}` - 有其中一个角色即可
   - `orRole = {"admin, manager"}` - 必须同时具备这两个角色（字符串逗号分隔）
4. **注解可以加在类上**：整个 Controller 的所有方法统一生效

## 参考资料

- [Sa-Token 官方文档](https://sa-token.cc)
- [AOP 注解鉴权](https://sa-token.cc/doc.html#/plugin/aop-at)
- [自定义注解](https://sa-token.cc/doc.html#/fun/custom-annotations)
