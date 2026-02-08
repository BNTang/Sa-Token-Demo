# OpenAPI 3.x 注解参考

> OpenAPI 3.x / Springdoc 注解完整参考手册

---

## Maven 依赖

```xml
<!-- Springdoc OpenAPI (Spring Boot 3.x) -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>

<!-- Knife4j 4.x (基于 OpenAPI 3.x) -->
<dependency>
    <groupId>com.github.xiaoymin</groupId>
    <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
    <version>4.4.0</version>
</dependency>
```

---

## 核心注解清单

### @Tag - 类级别

用于标注 Controller 类，说明该类的功能。

```java
@Tag(name = "用户管理", description = "用户增删改查接口")
@RestController
@RequestMapping("/api/user")
public class UserController {
    // ...
}
```

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 是 | 分组标签，用于文档导航 |
| description | String | 否 | 模块详细描述 |
| externalDocs | ExternalDocumentation | 外部文档 |

---

### @Operation - 方法级别

用于标注接口方法，说明接口功能。

```java
@Operation(
    summary = "获取用户列表",
    description = "支持分页查询，可根据用户名、手机号等条件筛选"
)
@GetMapping("/list")
public Result<List<UserVO>> list() {
    // ...
}
```

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| summary | String | 是 | 接口功能简述 |
| description | String | 否 | 接口详细说明 |
| operationId | String | 否 | 操作唯一标识 |
| tags | String[] | 否 | 所属标签 |
| requestBody | RequestBody | 否 | 请求体说明 |
| responses | ApiResponses | 否 | 响应说明 |
| parameters | Parameter[] | 否 | 参数说明 |
| deprecated | boolean | 否 | 是否已废弃 |
| hidden | boolean | 否 | 是否隐藏 |

---

### @ApiResponses / @ApiResponse - 响应说明

说明接口可能的响应状态。

```java
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "请求成功",
        content = @Content(schema = @Schema(implementation = UserVO.class))),
    @ApiResponse(responseCode = "400", description = "请求参数错误"),
    @ApiResponse(responseCode = "401", description = "未授权，请先登录"),
    @ApiResponse(responseCode = "403", description = "无权限访问"),
    @ApiResponse(responseCode = "404", description = "用户不存在"),
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
})
@GetMapping("/{id}")
public Result<UserVO> getById(@PathVariable Long id) {
    // ...
}
```

| 属性 | 类型 | 说明 |
|------|------|------|
| responseCode | String | HTTP 状态码 |
| description | String | 状态描述 |
| content | Content[] | 响应内容 |
| headers | Header[] | 响应头 |

---

### @Parameter - 参数说明

用于说明方法参数（Query、Path、Header 参数等）。

```java
@GetMapping("/search")
public Result<IPage<UserVO>> search(
    @Parameter(
        name = "keyword",
        description = "搜索关键词（支持用户名、手机号模糊匹配）",
        example = "张三",
        required = false
    )
    @RequestParam(required = false) String keyword,

    @Parameter(
        name = "status",
        description = "用户状态：1-正常，2-冻结",
        example = "1",
        schema = @Schema(allowableValues = {"1", "2"})
    )
    @RequestParam(required = false) Integer status,

    @Parameter(
        name = "pageNum",
        description = "页码",
        example = "1",
        required = true
    )
    @RequestParam Integer pageNum,

    @Parameter(
        name = "pageSize",
        description = "每页数量",
        example = "10",
        required = true
    )
    @RequestParam Integer pageSize
) {
    // ...
}
```

**@Parameter 属性：**

| 属性 | 类型 | 说明 |
|------|------|------|
| name | String | 参数名 |
| description | String | 参数说明 |
| in | ParameterIn | 参数位置：QUERY、PATH、HEADER、COOKIE |
| required | boolean | 是否必填 |
| example | String | 示例值 |
| deprecated | boolean | 是否已废弃 |
| hidden | boolean | 是否隐藏 |
| schema | Schema | 数据类型定义 |
| array | ArraySchema | 数组类型定义 |
| content | Content[] | 参数内容 |

---

### @RequestBody - 请求体说明

用于说明 POST/PUT 请求体。

```java
@PostMapping("/add")
@Operation(summary = "新增用户")
public Result<Long> add(
    @RequestBody(
        description = "用户信息",
        required = true,
        content = @Content(
            schema = @Schema(implementation = UserAddReq.class),
            examples = @ExampleObject(
                name = "示例",
                value = "{\"username\":\"zhangsan\",\"phone\":\"13800138000\"}"
            )
        )
    )
    @Valid @RequestBody UserAddReq req
) {
    // ...
}
```

**@RequestBody 属性：**

| 属性 | 类型 | 说明 |
|------|------|------|
| description | String | 请求体说明 |
| required | boolean | 是否必填 |
| content | Content[] | 请求体内容 |

---

### @Schema - 模型类

用于说明 VO/DTO 类及其字段。

```java
@Data
@Schema(description = "用户视图对象")
public class UserVO {

    @Schema(description = "用户ID", example = "1001", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "用户名", example = "zhangsan", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Schema(description = "手机号", example = "13800138000", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;

    @Schema(description = "用户状态：1-正常，2-冻结", example = "1", allowableValues = {"1", "2"})
    private Integer status;

    @Schema(description = "创建时间", example = "2025-01-30T10:30:00", type = "string")
    private LocalDateTime createTime;

    @Schema(description = "最后登录IP", hidden = true)
    private String lastLoginIp;
}
```

**@Schema 属性：**

| 属性 | 类型 | 说明 |
|------|------|------|
| description | String | 字段说明 |
| name | String | 字段名（可选） |
| example | String | 示例值 |
| requiredMode | RequiredMode | 是否必填：REQUIRED、NOT_REQUIRED、AUTO |
| type | String | 数据类型：string、integer、number、boolean等 |
| format | String | 格式：date、date-time、int32、int64等 |
| hidden | boolean | 是否隐藏 |
| allowableValues | String[] | 允许的值范围 |
| minimum | String | 最小值 |
| maximum | String | 最大值 |
| pattern | String | 正则表达式 |
| minLength | int | 最小长度 |
| maxLength | int | 最大长度 |
| ref | String | 引用其他定义 |
| implementation | Class<?> | 实际类型 |

---

### @ArraySchema - 数组类型

用于说明数组类型的参数或字段。

```java
@ArraySchema(
    schema = @Schema(description = "用户ID", example = "1001"),
    arraySchema = @Schema(description = "用户ID列表"),
    minItems = 1,
    maxItems = 100
)
private List<Long> userIds;
```

**@ArraySchema 属性：**

| 属性 | 类型 | 说明 |
|------|------|------|
| schema | Schema | 数组元素类型 |
| arraySchema | Schema | 数组本身说明 |
| minItems | int | 最小元素个数 |
| maxItems | int | 最大元素个数 |
| uniqueItems | boolean | 元素是否唯一 |

---

## Knife4j 4.x 增强注解

### @ApiSupport - 类级别增强

```java
@ApiSupport(author = "张三", order = 1)
@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/user")
public class UserController {
    // ...
}
```

### @ApiOperationSupport - 方法级别增强

```java
@ApiOperationSupport(
    author = "张三",
    order = 1,
    ignoreParameters = {"token", "password"}
)
@Operation(summary = "获取用户列表")
@GetMapping("/list")
public Result<List<UserVO>> list() {
    // ...
}
```

---

## 注解对比：Swagger 2.x vs OpenAPI 3.x

| 功能 | Swagger 2.x | OpenAPI 3.x |
|------|-------------|-------------|
| 类标签 | `@Api(tags = "...")` | `@Tag(name = "...")` |
| 接口名称 | `@ApiOperation(value = "...")` | `@Operation(summary = "...")` |
| 接口描述 | `@ApiOperation(notes = "...")` | `@Operation(description = "...")` |
| 响应状态 | `@ApiResponse(code = 200, ...)` | `@ApiResponse(responseCode = "200", ...)` |
| Query 参数 | `@ApiImplicitParam` | `@Parameter(in = ParameterIn.QUERY)` |
| Path 参数 | `@ApiParam` | `@Parameter(in = ParameterIn.PATH)` |
| 模型类 | `@ApiModel` | `@Schema` |
| 字段说明 | `@ApiModelProperty` | `@Schema` |
| 隐藏字段 | `hidden = true` | `hidden = true` |
| 必填字段 | `required = true` | `requiredMode = REQUIRED` |

---

## 配置示例

### Swagger 2.x 配置

```java
@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.xxx.controller"))
                .paths(PathSelectors.any())
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("项目 API 文档")
                .description("项目接口文档说明")
                .version("1.0")
                .build();
    }
}
```

### OpenAPI 3.x 配置

```java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("项目 API 文档")
                        .description("项目接口文档说明")
                        .version("1.0"))
                .externalDocs(new ExternalDocumentation()
                        .description("项目文档")
                        .url("https://docs.example.com"));
    }
}
```

---

## 常见问题

### Q1：如何处理枚举类型

```java
public enum UserStatus {
    NORMAL, FROZEN
}

// 在 VO 中使用注解说明
@Schema(
    description = "用户状态",
    example = "NORMAL",
    allowableValues = {"NORMAL", "FROZEN"}
)
private UserStatus status;

// 或者使用字符串
@Schema(description = "用户状态：NORMAL-正常，FROZEN-冻结", example = "NORMAL")
private String status;
```

### Q2：如何处理文件上传

```java
@Operation(summary = "上传头像")
@Parameter(
    name = "file",
    description = "头像文件",
    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
)
@PostMapping("/avatar")
public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
    // ...
}
```

### Q3：如何处理分页参数复用

```java
@Schema(description = "分页参数")
@Data
public class PageReq {

    @Schema(description = "页码", example = "1", minValue = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页数量", example = "10", minValue = "1", maxValue = "100")
    private Integer pageSize = 10;
}
```
