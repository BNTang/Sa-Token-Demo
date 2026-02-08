---
name: swagger-annotations
description: Swagger/Knife4j 接口注解补充工具。自动分析 Controller 类，补充完整的 Swagger 2.x 和 OpenAPI 3.x 注解，包括类级别、方法级别、参数注解和 VO/DTO 模型注解。支持 Knife4j 增强注解。当用户要求补充 swagger 注解、添加接口文档、完善 API 文档、添加 @Api/@ApiOperation 注解时使用。
metadata:
  author: java-team
  version: "1.0"
  compatibility: Spring Boot 2.x/3.x, Swagger 2.x, OpenAPI 3.x, Knife4j 2.x/4.x
---

# Swagger/Knife4j 接口注解补充

> 版本: 1.0 | 更新: 2026-01-30
>
> 本 Skill 帮助为 Spring Boot 项目补充完整的接口文档注解。

---

## 概述

本 Skill 用于为 Java Spring Boot 项目的 Controller 层补充 Swagger/Knife4j 接口文档注解，确保生成的 API 文档完整、准确、易读。

### 支持的注解版本

| 版本 | 常用依赖 | 注解包路径 |
|------|---------|-----------|
| **Swagger 2.x** | `springfox-swagger2` | `io.swagger.annotations.*` |
| **OpenAPI 3.x** | `springdoc-openapi` | `io.swagger.v3.oas.annotations.*` |
| **Knife4j 2.x** | 基于 Swagger 2.x | `io.swagger.annotations.*` + `com.github.xiaoymin.knife4j.annotations.*` |
| **Knife4j 4.x** | 基于 OpenAPI 3.x | `io.swagger.v3.oas.annotations.*` + `com.github.xiaoymin.knife4j.annotations.*` |

---

## 何时使用

当用户进行以下操作时激活此技能：

| 触发词 | 说明 |
|--------|------|
| 补充 swagger 注解 | 为现有接口添加 Swagger 注解 |
| 添加接口文档 | 完善接口文档注解 |
| 完善 API 文档 | 补充接口文档说明 |
| 添加 @Api 注解 | 补充类级别注解 |
| knife4j 注解 | Knife4j 增强注解 |
| 接口文档不完整 | 补充缺失的注解 |

---

## 工作流程

### 第一步：检查项目配置

1. **确认项目使用的 Swagger 版本**

```bash
# 搜索依赖
grep -r "swagger" pom.xml
grep -r "springdoc" pom.xml
grep -r "knife4j" pom.xml
```

2. **确认项目注解风格**

查看现有 Controller 使用的注解包：
- Swagger 2.x: `io.swagger.annotations.Api`
- OpenAPI 3.x: `io.swagger.v3.oas.annotations.tags.Tag`

3. **检查统一响应类**

确认项目使用的响应类（`Result`、`CommonResult`、`Response` 等）及其位置。

---

### 第二步：分析目标 Controller

1. **读取 Controller 文件**
2. **分析现有注解情况**
3. **识别需要补充的注解**
4. **查找关联的 VO/DTO 类**

---

### 第三步：补充注解

根据项目使用的 Swagger 版本，按以下顺序补充注解：

#### 3.1 类级别注解

**Swagger 2.x / Knife4j 2.x：**

```java
@Api(tags = "模块名称", description = "模块功能描述")
@RestController
@RequestMapping("/api/xxx")
public class XxxController {
    // ...
}
```

**OpenAPI 3.x / Knife4j 4.x：**

```java
@Tag(name = "模块名称", description = "模块功能描述")
@RestController
@RequestMapping("/api/xxx")
public class XxxController {
    // ...
}
```

**Knife4j 增强注解（可选）：**

```java
@ApiSupport(author = "作者名", order = 1)
```

#### 3.2 方法级别注解

**Swagger 2.x / Knife4j 2.x：**

```java
@ApiOperation(value = "接口功能名称", notes = "接口用途详细说明")
@ApiResponses({
    @ApiResponse(code = 200, message = "请求成功"),
    @ApiResponse(code = 400, message = "请求参数错误"),
    @ApiResponse(code = 401, message = "未授权，请先登录"),
    @ApiResponse(code = 403, message = "无权限访问"),
    @ApiResponse(code = 404, message = "请求资源不存在"),
    @ApiResponse(code = 500, message = "服务器内部错误")
})
@GetMapping("/xxx")
public Result<List<XxxVO>> getXxx() {
    // ...
}
```

**OpenAPI 3.x / Knife4j 4.x：**

```java
@Operation(summary = "接口功能名称", description = "接口用途详细说明")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "请求成功"),
    @ApiResponse(responseCode = "400", description = "请求参数错误"),
    @ApiResponse(responseCode = "401", description = "未授权，请先登录"),
    @ApiResponse(responseCode = "403", description = "无权限访问"),
    @ApiResponse(responseCode = "404", description = "请求资源不存在"),
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
})
@GetMapping("/xxx")
public Result<List<XxxVO>> getXxx() {
    // ...
}
```

**Knife4j 增强注解（推荐）：**

```java
@ApiOperationSupport(
    author = "作者名",
    order = 1,
    ignoreParameters = {"token", "password"}
)
```

#### 3.3 参数注解

**Query 参数（@RequestParam）：**

```java
// Swagger 2.x
@ApiImplicitParams({
    @ApiImplicitParam(
        name = "keyword",
        value = "搜索关键词（支持模糊匹配）",
        paramType = "query",
        dataType = "string",
        required = false,
        example = "测试商品"
    ),
    @ApiImplicitParam(
        name = "status",
        value = "状态：0-下架，1-上架",
        paramType = "query",
        dataType = "int",
        required = false,
        example = "1"
    )
})
@GetMapping("/search")
public Result<List<XxxVO>> search(
    @RequestParam(required = false) String keyword,
    @RequestParam(required = false) Integer status
) {
    // ...
}
```

**路径参数（@PathVariable）：**

```java
// Swagger 2.x
@GetMapping("/{id}")
public Result<XxxVO> getById(
    @ApiParam(value = "商品ID", example = "1001", required = true)
    @PathVariable Long id
) {
    // ...
}

// OpenAPI 3.x
@GetMapping("/{id}")
public Result<XxxVO> getById(
    @Parameter(name = "id", description = "商品ID", example = "1001", required = true)
    @PathVariable Long id
) {
    // ...
}
```

**Body 参数（@RequestBody）：**

```java
// Swagger 2.x
@PostMapping("/add")
public Result<Long> add(
    @ApiParam(value = "商品信息", required = true)
    @Valid @RequestBody XxxAddReq req
) {
    // ...
}

// OpenAPI 3.x（通常不需要额外注解，VO类注解即可）
@PostMapping("/add")
@RequestBody(description = "商品信息", required = true, content = @Content(schema = @Schema(implementation = XxxAddReq.class)))
public Result<Long> add(@Valid @RequestBody XxxAddReq req) {
    // ...
}
```

**数组参数：**

```java
// Swagger 2.x
@ApiImplicitParams({
    @ApiImplicitParam(
        name = "ids",
        value = "商品ID列表",
        paramType = "query",
        dataType = "array",
        allowMultiple = true,
        example = "1001,1002,1003"
    )
})
@GetMapping("/batch")
public Result<List<XxxVO>> batch(@RequestParam List<Long> ids) {
    // ...
}
```

---

### 第四步：补充 VO/DTO 注解

**Swagger 2.x：**

```java
@Data
@ApiModel(value = "XxxVO", description = "商品视图对象")
public class XxxVO {

    @ApiModelProperty(value = "商品ID", example = "1001", required = true)
    private Long id;

    @ApiModelProperty(value = "商品名称", example = "阿莫西林", required = true)
    private String name;

    @ApiModelProperty(value = "商品价格（元）", example = "25.50", required = true)
    private BigDecimal price;

    @ApiModelProperty(value = "状态：0-下架，1-上架", example = "1")
    private Integer status;

    @ApiModelProperty(value = "创建时间", example = "2025-01-30 10:30:00")
    private LocalDateTime createTime;
}
```

**OpenAPI 3.x：**

```java
@Data
@Schema(description = "商品视图对象")
public class XxxVO {

    @Schema(description = "商品ID", example = "1001", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "商品名称", example = "阿莫西林", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "商品价格（元）", example = "25.50")
    private BigDecimal price;

    @Schema(description = "状态：0-下架，1-上架", example = "1", allowableValues = {"0", "1"})
    private Integer status;
}
```

**字段隐藏（不显示在文档中）：**

```java
// Swagger 2.x
@ApiModelProperty(hidden = true)
private String internalField;

// OpenAPI 3.x
@Schema(hidden = true)
private String internalField;
```

---

### 第五步：检查统一响应类注解

确保项目的 `Result` / `CommonResult` 类有完整的注解：

```java
@Data
@ApiModel(value = "统一响应结果", description = "所有接口的统一响应格式")
public class Result<T> {

    @ApiModelProperty(value = "响应状态码", example = "200", notes = "200-成功，400-参数错误，500-服务器错误")
    private Integer code;

    @ApiModelProperty(value = "响应消息", example = "操作成功")
    private String message;

    @ApiModelProperty(value = "响应数据", notes = "成功时返回具体业务数据，失败时为null")
    private T data;

    @ApiModelProperty(value = "响应时间戳（毫秒）", example = "1738252800000")
    private Long timestamp;
}
```

---

### 第六步：处理泛型响应

当返回类型为 `Result<List<T>>` 时，Swagger 可能无法完全解析嵌套泛型。

**解决方案：**

1. **方法级别指定响应类型（推荐）**

```java
// Swagger 2.x
@ApiResponse(code = 200, message = "请求成功", response = Result.class, responseContainer = "List")
```

2. **在 notes 中说明响应结构**

```java
@ApiOperation(
    value = "获取商品列表",
    notes = "响应格式：{code: 200, message: '成功', data: [{id: 1, name: '商品1'}, ...]}"
)
```

3. **使用Knife4j增强注解**

```java
@ApiOperationSupport(
    responses = @IoResponse(code = 200, description = "请求成功", responseClass = "com.xxx.vo.XxxVO")
)
```

---

## 导入语句清单

**Swagger 2.x：**

```java
// 类级别
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

// 方法响应
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

// 参数
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;

// 模型
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

// Knife4j 增强（可选）
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import com.github.xiaoymin.knife4j.annotations.ApiSupport;
```

**OpenAPI 3.x：**

```java
// 类级别
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

// 方法响应
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

// 参数
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

// 模型
import io.swagger.v3.oas.annotations.media.Schema;

// Knife4j 增强（可选）
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import com.github.xiaoymin.knife4j.annotations.ApiSupport;
```

---

## 注解属性速查表

### Swagger 2.x 常用属性

| 注解 | 属性 | 类型 | 说明 | 示例 |
|------|------|------|------|------|
| @Api | tags | String | 分组标签 | `"用户管理"` |
| @Api | description | String | 模块描述 | `"用户增删改查"` |
| @ApiOperation | value | String | 接口名称 | `"获取用户列表"` |
| @ApiOperation | notes | String | 接口说明 | `"分页查询用户数据"` |
| @ApiResponse | code | int | 状态码 | `200` |
| @ApiResponse | message | String | 状态说明 | `"请求成功"` |
| @ApiImplicitParam | name | String | 参数名 | `"userId"` |
| @ApiImplicitParam | value | String | 参数说明 | `"用户ID"` |
| @ApiImplicitParam | paramType | String | 参数位置 | `"query"/"path"/"body"` |
| @ApiImplicitParam | dataType | String | 数据类型 | `"string"/"int"/"long"` |
| @ApiImplicitParam | required | boolean | 是否必填 | `true/false` |
| @ApiImplicitParam | example | String | 示例值 | `"123"` |
| @ApiImplicitParam | allowMultiple | boolean | 是否数组 | `true/false` |
| @ApiModelProperty | value | String | 字段说明 | `"用户名"` |
| @ApiModelProperty | example | String | 示例值 | `"张三"` |
| @ApiModelProperty | required | boolean | 是否必填 | `true/false` |
| @ApiModelProperty | hidden | boolean | 是否隐藏 | `true/false` |
| @ApiModelProperty | notes | String | 补充说明 | `"唯一标识"` |

### OpenAPI 3.x 常用属性

| 注解 | 属性 | 类型 | 说明 | 示例 |
|------|------|------|------|------|
| @Tag | name | String | 分组标签 | `"用户管理"` |
| @Tag | description | String | 模块描述 | `"用户增删改查"` |
| @Operation | summary | String | 接口名称 | `"获取用户列表"` |
| @Operation | description | String | 接口说明 | `"分页查询用户数据"` |
| @ApiResponse | responseCode | String | 状态码 | `"200"` |
| @ApiResponse | description | String | 状态说明 | `"请求成功"` |
| @Parameter | name | String | 参数名 | `"userId"` |
| @Parameter | description | String | 参数说明 | `"用户ID"` |
| @Parameter | in | String | 参数位置 | `"query"/"path"/"body"` |
| @Schema | type | String | 数据类型 | `"string"/"integer"` |
| @Schema | example | String | 示例值 | `"123"` |
| @Schema | requiredMode | RequiredMode | 是否必填 | `REQUIRED` |

### Knife4j 增强注解属性

| 注解 | 属性 | 类型 | 说明 | 示例 |
|------|------|------|------|------|
| @ApiOperationSupport | author | String | 作者 | `"张三"` |
| @ApiOperationSupport | order | int | 排序 | `1` |
| @ApiOperationSupport | ignoreParameters | String[] | 忽略参数 | `{"token"}` |
| @ApiSupport | author | String | 作者 | `"张三"` |
| @ApiSupport | order | int | 排序 | `1` |

---

## 注意事项

### 1. 接口描述简洁明确

- `value` / `summary`：简洁的功能名称
- `notes` / `description`：详细的用途说明

```java
// ✅ 好的示例
@ApiOperation(value = "获取商品详情", notes = "根据商品ID查询商品详细信息")

// ❌ 不好的示例
@ApiOperation(value = "商品", notes = "获取")
```

### 2. 参数说明完整

- 包含取值范围
- 包含含义说明
- 提供真实示例值

```java
// ✅ 好的示例
@ApiImplicitParam(
    name = "status",
    value = "商品状态：0-下架，1-上架，2-已售罄",
    paramType = "query",
    dataType = "int",
    example = "1"
)

// ❌ 不好的示例
@ApiImplicitParam(name = "status", value = "状态", dataType = "int")
```

### 3. 响应状态使用中文

```java
@ApiResponses({
    @ApiResponse(code = 200, message = "请求成功"),
    @ApiResponse(code = 400, message = "请求参数错误"),
    @ApiResponse(code = 401, message = "未授权，请先登录"),
    @ApiResponse(code = 403, message = "无权限访问"),
    @ApiResponse(code = 404, message = "请求资源不存在"),
    @ApiResponse(code = 500, message = "服务器内部错误")
})
```

### 4. 示例数据真实

- example 值要符合实际业务场景
- 日期格式使用 `yyyy-MM-dd HH:mm:ss`
- 金额使用真实数值

```java
// ✅ 好的示例
@ApiModelProperty(value = "创建时间", example = "2025-01-30 10:30:00")
private LocalDateTime createTime;

@ApiModelProperty(value = "商品价格", example = "25.50")
private BigDecimal price;

// ❌ 不好的示例
@ApiModelProperty(value = "创建时间", example = "xxx")
private LocalDateTime createTime;
```

### 5. 敏感信息脱敏

- 禁止在示例中出现真实手机号、身份证号
- 使用脱敏后的示例值

```java
// ✅ 好的示例
@ApiModelProperty(value = "手机号", example = "13800138000")
private String phone;

@ApiModelProperty(value = "身份证号", example = "110101199001011234")
private String idCard;

// ❌ 不好的示例（包含真实敏感信息）
@ApiModelProperty(value = "手机号", example = "13812345678")
private String phone;
```

### 6. 保持项目风格一致

- 参考项目中已有 Controller 的注解风格
- 统一使用 Swagger 2.x 或 OpenAPI 3.x
- 统一使用项目的响应类命名

---

## 快速检查清单

补充注解完成后，检查以下项目：

| 检查项 | Swagger 2.x | OpenAPI 3.x |
|--------|-------------|-------------|
| 类级别注解 | `@Api` | `@Tag` |
| 方法名称注解 | `@ApiOperation` | `@Operation` |
| 方法响应注解 | `@ApiResponses` | `@ApiResponses` |
| Query 参数注解 | `@ApiImplicitParam` | `@Parameter` |
| Path 参数注解 | `@ApiParam` | `@Parameter` |
| Body 参数说明 | `@ApiParam` | `@RequestBody/@Schema` |
| VO 类注解 | `@ApiModel` | `@Schema` |
| VO 字段注解 | `@ApiModelProperty` | `@Schema` |
| 数组参数处理 | `allowMultiple = true` | `array` schema |

---

## 相关参考

- [Java/Spring Boot 编码规范 - Controller 层](../java/references/controller.md)
- [Java/Spring Boot 编码规范 - 接口文档](../java/references/api-doc.md)
- [Swagger 2.x 注解文档](references/swagger2-annotations.md)
- [OpenAPI 3.x 注解文档](references/openapi3-annotations.md)
- [Knife4j 增强注解](references/knife4j-annotations.md)
