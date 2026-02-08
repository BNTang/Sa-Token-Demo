# Knife4j 增强注解参考

> Knife4j 增强 Swagger 功能的注解使用指南

---

## 概述

Knife4j 是 Swagger 的增强解决方案，提供了更丰富的功能和更美观的 UI 界面。

- **Knife4j 2.x**：基于 Swagger 2.x，适用于 Spring Boot 2.x
- **Knife4j 4.x**：基于 OpenAPI 3.x，适用于 Spring Boot 3.x

---

## Maven 依赖

```xml
<!-- Knife4j 2.x (Spring Boot 2.x) -->
<dependency>
    <groupId>com.github.xiaoymin</groupId>
    <artifactId>knife4j-spring-boot-starter</artifactId>
    <version>2.0.9</version>
</dependency>

<!-- Knife4j 4.x (Spring Boot 3.x) -->
<dependency>
    <groupId>com.github.xiaoymin</groupId>
    <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
    <version>4.4.0</version>
</dependency>
```

---

## 核心增强注解

### @ApiSupport - 类级别增强

为整个 Controller 类添加额外信息。

```java
@ApiSupport(author = "张三", order = 1)
@Api(tags = "用户管理")  // Swagger 2.x
// 或
@Tag(name = "用户管理")  // OpenAPI 3.x
@RestController
@RequestMapping("/api/user")
public class UserController {
    // ...
}
```

**属性说明：**

| 属性 | 类型 | 说明 |
|------|------|------|
| author | String | 作者名称 |
| order | int | 分组排序，数值越小越靠前 |

---

### @ApiOperationSupport - 方法级别增强

为接口方法添加额外信息，增强文档展示。

```java
@ApiOperationSupport(
    author = "张三",
    order = 1,
    ignoreParameters = {"token", "password", "confirmPassword"},
    includeParameters = {"id", "name", "phone"}
)
@ApiOperation(value = "更新用户信息")
@PostMapping("/update")
public Result<Void> update(@Valid @RequestBody UserUpdateReq req) {
    // ...
}
```

**属性说明：**

| 属性 | 类型 | 说明 |
|------|------|------|
| author | String | 作者名称 |
| order | int | 接口排序（同类内排序） |
| ignoreParameters | String[] | 要忽略的参数（不显示在文档中） |
| includeParameters | String[] | 要包含的参数（只显示指定参数） |
| responses | IoResponse[] | 响应说明 |

**参数匹配规则：**

- 精确匹配：`"fieldName"` 匹配完全相同的字段名
- 嵌套对象：`"req.user"` 匹配 `req.user` 字段
- 通配符：`"req.*"` 匹配 `req` 下的所有字段

---

### 响应增强说明

使用 `@ApiOperationSupport` 增强 API 响应文档。

```java
// Swagger 2.x
@ApiOperationSupport(
    responses = {
        @IoResponse(code = 200, description = "请求成功", responseClass = "com.xxx.vo.UserVO"),
        @IoResponse(code = 400, description = "参数错误")
    }
)
@ApiOperation(value = "获取用户信息")
@GetMapping("/{id}")
public Result<UserVO> getById(@PathVariable Long id) {
    // ...
}
```

**@IoResponse 属性：**

| 属性 | 类型 | 说明 |
|------|------|------|
| code | int | HTTP 状态码 |
| description | String | 响应描述 |
| responseClass | String | 响应类完整路径 |
| headers | IoResponseHeader[] | 响应头 |

---

## 文档分组与排序

### 分组配置

```java
@Configuration
public class Knife4jConfig {

    @Bean(value = "defaultApi2")
    public Docket defaultApi2() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(new ApiInfoBuilder()
                        .title("用户管理 API")
                        .description("用户相关接口")
                        .version("1.0")
                        .build())
                .groupName("用户分组")
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.xxx.controller.user"))
                .paths(PathSelectors.any())
                .build();
    }
}
```

### 排序示例

```java
@ApiSupport(order = 1)
@Api(tags = "用户管理")
public class UserController {
    // order=1, 该分组排在最前
}

@ApiSupport(order = 2)
@Api(tags = "订单管理")
public class OrderController {
    // order=2, 该分组排在第二
}

// 同一分组内排序
@ApiOperationSupport(order = 1)
@ApiOperation(value = "查询用户")
public Result<UserVO> get() { }

@ApiOperationSupport(order = 2)
@ApiOperation(value = "创建用户")
public Result<Long> add() { }
```

---

## 参数过滤示例

### 忽略敏感参数

```java
@ApiOperationSupport(
    ignoreParameters = {
        "req.password",        // 忽略请求对象中的密码字段
        "req.confirmPassword",  // 忽略确认密码字段
        "token"                 // 忽略 token 参数
    }
)
@ApiOperation(value = "注册用户")
@PostMapping("/register")
public Result<Long> register(@Valid @RequestBody RegisterReq req) {
    // 文档中不会显示 password 和 confirmPassword 字段
}
```

### 只显示必要参数

```java
@ApiOperationSupport(
    includeParameters = {
        "req.username",
        "req.phone",
        "req.email"
    }
)
@ApiOperation(value = "更新用户资料")
@PostMapping("/update")
public Result<Void> update(@Valid @RequestBody UserUpdateReq req) {
    // 文档中只显示指定字段
}
```

---

## 动态注解（高级）

Knife4j 支持通过配置动态控制接口显示。

```yaml
# application.yml
knife4j:
  enable-aggregation: true
  cors: true
  production: false  # 生产环境关闭文档
  basic:
    enable: true     # 开启 Basic 认证
    username: admin
    password: 123456
```

---

## 界面增强功能

Knife4j 提供的额外功能：

| 功能 | 说明 |
|------|------|
| **离线文档** | 支持 Markdown、Html、Word 等格式导出 |
| **全局参数** | 设置全局请求头（如 token） |
| **搜索过滤** | 接口名称、标签搜索 |
| **调试模式** | 在线调试接口 |
| **资源下载** | 下载 Swagger JSON/YAML |
| **个性化配置** | 主题、语言等配置 |

---

## 访问地址

| 版本 | UI 地址 | Doc 地址 |
|------|---------|----------|
| Knife4j 2.x | `/doc.html` | `/docs.html` |
| Knife4j 4.x | `/doc.html` | `/doc.html` |

---

## 最佳实践

### 1. 敏感参数处理

```java
// 忽略所有包含 "password", "secret", "token" 的字段
@ApiOperationSupport(ignoreParameters = {"*.password", "*.secret", "token"})
@ApiOperation(value = "用户登录")
@PostMapping("/login")
public Result<String> login(@Valid @RequestBody LoginReq req) {
    // ...
}
```

### 2. 统一响应格式

```java
@Data
@ApiModel("统一响应结果")
public class Result<T> {

    @ApiModelProperty("状态码")
    private Integer code;

    @ApiModelProperty("消息")
    private String message;

    @ApiModelProperty("数据")
    private T data;
}

// 使用 Knife4j 增强指定具体响应类型
@ApiOperationSupport(
    responses = @IoResponse(code = 200, responseClass = "com.xxx.vo.UserVO")
)
```

### 3. 接口版本管理

```java
@ApiSupport(author = "张三", order = 1)
@Api(tags = "用户管理 V1")
@RequestMapping("/api/v1/user")
public class UserV1Controller { }

@ApiSupport(author = "李四", order = 2)
@Api(tags = "用户管理 V2")
@RequestMapping("/api/v2/user")
public class UserV2Controller { }
```

---

## 注意事项

1. **生产环境关闭**：生产环境建议关闭 Knife4j 文档
2. **认证授权**：建议开启 Basic 认证保护文档
3. **性能影响**：Knife4j 会扫描所有注解，启动时会略慢
4. **版本兼容**：注意 Swagger 2.x 和 OpenAPI 3.x 的注解区别
