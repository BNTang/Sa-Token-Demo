# Swagger 2.x 注解参考

> Swagger 2.x 注解完整参考手册

---

## Maven 依赖

```xml
<!-- Spring Boot 2.x -->
<dependency>
    <groupId>io.springfox</groupId>
    <artifactId>springfox-swagger2</artifactId>
    <version>3.0.0</version>
</dependency>
<dependency>
    <groupId>io.springfox</groupId>
    <artifactId>springfox-swagger-ui</artifactId>
    <version>3.0.0</version>
</dependency>

<!-- Knife4j（基于 Swagger 2.x 增强） -->
<dependency>
    <groupId>com.github.xiaoymin</groupId>
    <artifactId>knife4j-spring-boot-starter</artifactId>
    <version>2.0.9</version>
</dependency>
```

---

## 核心注解清单

### @Api - 类级别

用于标注 Controller 类，说明该类的功能。

```java
@Api(tags = "用户管理", description = "用户增删改查接口")
@RestController
@RequestMapping("/api/user")
public class UserController {
    // ...
}
```

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| tags | String[] | 是 | 分组标签，用于文档导航 |
| description | String | 否 | 模块详细描述 |
| hidden | boolean | 否 | 是否隐藏该类 |
| produces | String | 否 | 返回类型，如 "application/json" |
| consumes | String | 否 | 接收类型，如 "application/json" |

---

### @ApiOperation - 方法级别

用于标注接口方法，说明接口功能。

```java
@ApiOperation(
    value = "获取用户列表",
    notes = "支持分页查询，可根据用户名、手机号等条件筛选"
)
@GetMapping("/list")
public Result<List<UserVO>> list() {
    // ...
}
```

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| value | String | 是 | 接口功能简述 |
| notes | String | 否 | 接口详细说明 |
| httpMethod | String | 否 | HTTP 方法（自动识别） |
| response | Class<?> | 否 | 响应类型 |
| responseContainer | String | 否 | 响应容器类型（List、Map等） |
| code | int | 否 | 成功响应状态码 |
| hidden | boolean | 否 | 是否隐藏该接口 |

---

### @ApiResponses / @ApiResponse - 响应说明

说明接口可能的响应状态。

```java
@ApiResponses({
    @ApiResponse(code = 200, message = "请求成功", response = UserVO.class),
    @ApiResponse(code = 400, message = "请求参数错误"),
    @ApiResponse(code = 401, message = "未授权，请先登录"),
    @ApiResponse(code = 403, message = "无权限访问"),
    @ApiResponse(code = 404, message = "用户不存在"),
    @ApiResponse(code = 500, message = "服务器内部错误")
})
@GetMapping("/{id}")
public Result<UserVO> getById(@PathVariable Long id) {
    // ...
}
```

| 属性 | 类型 | 说明 |
|------|------|------|
| code | int | HTTP 状态码 |
| message | String | 状态描述 |
| response | Class<?> | 响应类型 |
| responseContainer | String | 响应容器类型 |
| examples | Example | 示例 |
| reference | String | 引用其他定义 |

---

### @ApiImplicitParams / @ApiImplicitParam - Query 参数

用于说明 URL 查询参数。

```java
@ApiImplicitParams({
    @ApiImplicitParam(
        name = "keyword",
        value = "搜索关键词（支持用户名、手机号模糊匹配）",
        paramType = "query",
        dataType = "string",
        required = false,
        example = "张三"
    ),
    @ApiImplicitParam(
        name = "status",
        value = "用户状态：1-正常，2-冻结",
        paramType = "query",
        dataType = "int",
        required = false,
        example = "1"
    ),
    @ApiImplicitParam(
        name = "pageNum",
        value = "页码",
        paramType = "query",
        dataType = "int",
        required = true,
        example = "1"
    ),
    @ApiImplicitParam(
        name = "pageSize",
        value = "每页数量",
        paramType = "query",
        dataType = "int",
        required = true,
        example = "10"
    )
})
@GetMapping("/search")
public Result<IPage<UserVO>> search(
    @RequestParam(required = false) String keyword,
    @RequestParam(required = false) Integer status,
    @RequestParam Integer pageNum,
    @RequestParam Integer pageSize
) {
    // ...
}
```

| 属性 | 类型 | 说明 |
|------|------|------|
| name | String | 参数名 |
| value | String | 参数说明 |
| paramType | String | 参数位置：query、path、body、header |
| dataType | String | 数据类型：string、int、long、boolean等 |
| dataTypeClass | Class<?> | 数据类型类（更精确） |
| required | boolean | 是否必填 |
| example | String | 示例值 |
| defaultValue | String | 默认值 |
| allowMultiple | boolean | 是否为数组参数 |
| allowableValues | String | 允许的值范围 |

---

### @ApiParam - 方法参数

用于说明方法参数（Path 变量、Body 参数等）。

```java
// Path 变量
@GetMapping("/{id}")
public Result<UserVO> getById(
    @ApiParam(value = "用户ID", example = "1001", required = true)
    @PathVariable Long id
) {
    // ...
}

// Body 参数
@PostMapping("/add")
public Result<Long> add(
    @ApiParam(value = "用户信息", required = true)
    @Valid @RequestBody UserAddReq req
) {
    // ...
}
```

| 属性 | 类型 | 说明 |
|------|------|------|
| name | String | 参数名（可选） |
| value | String | 参数说明 |
| required | boolean | 是否必填 |
| example | String | 示例值 |
| defaultValue | String | 默认值 |
| hidden | boolean | 是否隐藏 |
| type | String | 数据类型 |
| allowableValues | String | 允许的值范围 |

---

### @ApiModel / @ApiModelProperty - 模型类

用于说明 VO/DTO 类及其字段。

```java
@Data
@ApiModel(value = "UserVO", description = "用户视图对象")
public class UserVO {

    @ApiModelProperty(value = "用户ID", example = "1001", required = true)
    private Long id;

    @ApiModelProperty(value = "用户名", example = "zhangsan", required = true)
    private String username;

    @ApiModelProperty(value = "手机号", example = "13800138000", required = true)
    private String phone;

    @ApiModelProperty(value = "用户状态：1-正常，2-冻结", example = "1")
    private Integer status;

    @ApiModelProperty(value = "创建时间", example = "2025-01-30 10:30:00")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "最后登录IP", hidden = true)
    private String lastLoginIp;
}
```

**@ApiModel 属性：**

| 属性 | 类型 | 说明 |
|------|------|------|
| value | String | 模型名称 |
| description | String | 模型描述 |
| parent | Class<?> | 父类 |
| subTypes | Class<?>[] | 子类 |
| reference | String | 引用其他定义 |

**@ApiModelProperty 属性：**

| 属性 | 类型 | 说明 |
|------|------|------|
| value | String | 字段说明 |
| name | String | 字段名（可选，默认使用字段名） |
| required | boolean | 是否必填 |
| example | String | 示例值 |
| hidden | boolean | 是否隐藏 |
| dataType | String | 数据类型 |
| notes | String | 补充说明 |
| allowableValues | String | 允许的值范围 |
| access | String | 访问权限 |
| position | int | 在模型中的排序位置 |

---

## Knife4j 2.x 增强注解

### @ApiSupport - 类级别增强

```java
@ApiSupport(author = "张三", order = 1)
@Api(tags = "用户管理")
@RestController
@RequestMapping("/api/user")
public class UserController {
    // ...
}
```

| 属性 | 类型 | 说明 |
|------|------|------|
| author | String | 作者名称 |
| order | int | 接口排序 |

### @ApiOperationSupport - 方法级别增强

```java
@ApiOperationSupport(
    author = "张三",
    order = 1,
    ignoreParameters = {"token", "password"},
    responses = @IoResponse(code = 200, description = "成功", responseClass = "com.xxx.vo.UserVO")
)
@ApiOperation(value = "获取用户列表")
@GetMapping("/list")
public Result<List<UserVO>> list() {
    // ...
}
```

| 属性 | 类型 | 说明 |
|------|------|------|
| author | String | 作者名称 |
| order | int | 接口排序 |
| ignoreParameters | String[] | 忽略的参数（不显示在文档中） |
| includeParameters | String[] | 包含的参数（只显示指定参数） |
| responses | IoResponse[] | 响应说明 |

---

## 常见问题

### Q1：泛型响应类型无法解析

```java
// 问题：Result<List<UserVO>> 中的 UserVO 无法被识别
public Result<List<UserVO>> list() {
    // ...
}

// 解决方案1：在 @ApiResponse 中指定
@ApiResponse(code = 200, message = "成功", response = UserVO.class, responseContainer = "List")

// 解决方案2：使用 Knife4j 增强注解
@ApiOperationSupport(responses = @IoResponse(responseClass = "com.xxx.vo.UserVO"))
```

### Q2：枚举类型如何显示说明

```java
public enum UserStatus {
    NORMAL(1, "正常"),
    FROZEN(2, "冻结");

    private Integer code;
    private String desc;
}

// 在 VO 中使用注解说明
@ApiModelProperty(value = "用户状态：1-正常，2-冻结", example = "1")
private Integer status;
```

### Q3：如何隐藏某个接口

```java
@ApiOperation(value = "内部接口", hidden = true)
@GetMapping("/internal")
public Result<String> internal() {
    // ...
}
```

### Q4：如何处理文件上传

```java
@ApiOperation(value = "上传头像")
@ApiImplicitParams({
    @ApiImplicitParam(
        name = "file",
        value = "头像文件",
        paramType = "form",
        dataType = "file",
        required = true
    )
})
@PostMapping("/avatar")
public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
    // ...
}
```
