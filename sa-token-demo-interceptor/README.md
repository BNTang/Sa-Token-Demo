# Sa-Token 拦断器路由鉴权演示

## 项目简介

本项目演示了 Sa-Token 拦断器的各种用法，包括：

1. **路由匹配规则** - 使用 `match()` 方法指定需要拦截的路径
2. **路由排除规则** - 使用 `notMatch()` 方法排除不需要拦截的路径
3. **登录校验** - 使用 `StpUtil.checkLogin()` 校验用户是否登录
4. **角色校验** - 使用 `StpUtil.checkRoleOr()` 校验用户是否拥有指定角色
5. **权限校验** - 使用 `StpUtil.checkPermission()` 校验用户是否拥有指定权限
6. **连缀写法** - 演示 Sa-Token 的连缀写法

## 启动项目

```bash
mvn spring-boot:run
```

启动成功后访问：http://localhost:8083

## 测试账号

| 用户名 | 密码 | 角色 | 权限 |
|--------|------|------|------|
| user | 123456 | user | user |
| admin | 123456 | admin | admin, user, goods, orders |
| super-admin | 123456 | super-admin | 所有权限 |
| goods-admin | 123456 | admin | goods |

## 模块说明

### 1. 认证模块 (`/auth`)
- `POST /auth/doLogin` - 用户登录
- `POST /auth/register` - 用户注册
- `GET /auth/isLogin` - 检查登录状态
- `GET /auth/userInfo` - 获取当前用户信息
- `POST /auth/logout` - 退出登录

### 2. 用户模块 (`/user`)
- 需要 `user` 权限
- `GET /user/list` - 获取用户列表
- `GET /user/info/{username}` - 获取用户详情
- `POST /user/create` - 创建用户
- `PUT /user/update` - 更新用户
- `DELETE /user/delete` - 删除用户

### 3. 管理员模块 (`/admin`)
- 需要 `admin` 或 `super-admin` 角色
- 需要 `admin` 权限
- `GET /admin/dashboard` - 管理员首页
- `GET /admin/settings` - 系统设置
- `PUT /admin/settings` - 更新系统设置
- `GET /admin/users` - 用户管理

### 4. 商品模块 (`/goods`)
- 需要 `goods` 权限
- `GET /goods/list` - 获取商品列表
- `GET /goods/info/{goodsId}` - 获取商品详情
- `POST /goods/add` - 添加商品
- `PUT /goods/update` - 更新商品
- `DELETE /goods/delete/{goodsId}` - 删除商品

### 5. 订单模块 (`/orders`)
- 需要 `orders` 权限
- `GET /orders/list` - 获取订单列表
- `GET /orders/info/{orderId}` - 获取订单详情
- `POST /orders/create` - 创建订单
- `PUT /orders/cancel/{orderId}` - 取消订单

### 6. 通知模块 (`/notice`)
- 需要 `notice` 权限
- `GET /notice/list` - 获取通知列表
- `GET /notice/info/{noticeId}` - 获取通知详情
- `POST /notice/publish` - 发布通知
- `DELETE /notice/delete/{noticeId}` - 删除通知

### 7. 评论模块 (`/comment`)
- 需要 `comment` 权限
- `GET /comment/list` - 获取评论列表
- `GET /comment/info/{commentId}` - 获取评论详情
- `POST /comment/add` - 添加评论
- `DELETE /comment/delete/{commentId}` - 删除评论

## 测试流程

### 1. 登录测试

使用 `user` 账号登录：
```bash
curl -X POST http://localhost:8083/auth/doLogin \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"123456"}'
```

返回结果会包含 `token`，后续请求需要携带此 token。

### 2. 权限测试

使用返回的 token 访问需要权限的接口：
```bash
# 访问用户模块 - 成功（user 有 user 权限）
curl http://localhost:8083/user/list \
  -H "satoken: YOUR_TOKEN_HERE"

# 访问管理员模块 - 失败（user 没有 admin 权限）
curl http://localhost:8083/admin/dashboard \
  -H "satoken: YOUR_TOKEN_HERE"
```

### 3. 角色测试

使用 `admin` 账号登录后访问管理员模块：
```bash
# 先登录获取 token
curl -X POST http://localhost:8083/auth/doLogin \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'

# 使用 admin 的 token 访问管理员模块 - 成功（admin 有 admin 角色）
curl http://localhost:8083/admin/dashboard \
  -H "satoken: ADMIN_TOKEN_HERE"
```

## 核心代码说明

### SaTokenConfigure.java

这是 Sa-Token 拦断器配置类，定义了路由匹配和鉴权规则：

```java
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> {
            // 1. 基础登录校验
            SaRouter
                .match("/**")
                .notMatch("/auth/doLogin", "/auth/register")
                .check(r -> StpUtil.checkLogin());

            // 2. 角色校验
            SaRouter.match("/admin/**", r -> StpUtil.checkRoleOr("admin", "super-admin"));

            // 3. 权限校验
            SaRouter.match("/user/**", r -> StpUtil.checkPermission("user"));
            SaRouter.match("/goods/**", r -> StpUtil.checkPermission("goods"));

        })).addPathPatterns("/**");
    }
}
```

## 知识点总结

1. **SaRouter.match()** - 指定要匹配的路由规则
2. **SaRouter.notMatch()** - 指定要排除的路由规则
3. **SaRouter.check()** - 指定校验逻辑
4. **StpUtil.checkLogin()** - 校验是否登录
5. **StpUtil.checkRoleOr()** - 校验是否拥有指定角色之一
6. **StpUtil.checkPermission()** - 校验是否拥有指定权限
7. **连缀写法** - 可以连续调用多个方法

## 注意事项

1. 拦断器配置中的路由匹配是按顺序执行的
2. 如果前面的规则已经通过校验，后面的规则不会再执行
3. `notMatch()` 的优先级高于 `match()`
4. 角色和权限信息需要在 `StpInterface` 实现类中提供
