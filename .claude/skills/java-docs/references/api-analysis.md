# 接口文档分析规范

> Java Spring Boot 项目文档生成器 - 接口分析模块

---

## 分析目标

分析项目获取以下信息用于生成接口设计文档：

| 信息项 | 来源 | 用途 |
|--------|------|------|
| 服务端口 | `application.yml` 中的 `server.port` | 生成访问地址 |
| 上下文路径 | `application.yml` 中的 `server.servlet.context-path` | 生成访问地址 |
| Swagger 路径 | 配置类或默认路径 | 生成文档访问地址 |
| API 分组 | Controller 类注解 | 生成接口分组 |
| 接口路径 | 方法注解 | 生成接口清单 |
| 请求/响应对象 | DTO/VO 类字段 | 生成参数说明 |
| 枚举类 | `enums` 包下的枚举 | 生成数据字典 |

---

## Swagger/Knife4j 检测

### 检测步骤

1. **检查依赖**

   在 `pom.xml` 或 `build.gradle` 中查找：

   ```xml
   <!-- Knife4j -->
   <dependency>
       <groupId>com.github.xiaoymin</groupId>
       <artifactId>knife4j-spring-boot-starter</artifactId>
   </dependency>

   <!-- 或 SpringDoc -->
   <dependency>
       <groupId>org.springdoc</groupId>
       <artifactId>springdoc-openapi-ui</artifactId>
   </dependency>
   ```

2. **检查配置类**

   查找 `config` 包下的配置类：

   ```java
   @Configuration
   @EnableKnife4j
   public class Knife4jConfig { }
   ```

3. **确定访问路径**

   | 框架 | 默认路径 |
   |------|---------|
   | Knife4j | `/doc.html` |
   | SpringDoc (Swagger UI) | `/swagger-ui.html` |
   | SpringDoc (OpenAPI JSON) | `/v3/api-docs` |

### 访问地址生成

根据配置生成访问地址：

```
本地开发环境: http://localhost:{port}{context-path}/doc.html
生产环境: http://{server}:{port}{context-path}/doc.html
```

---

## Controller 扫描

### 扫描策略

1. **查找 Controller 类**

   搜索所有包含以下注解的类：
   - `@RestController`
   - `@Controller` + `@ResponseBody`

2. **解析类级别注解**

   | 注解 | 用途 |
   |------|------|
   | `@RequestMapping("/xxx")` | 获取模块路径 |
   | `@Api(tags = "xxx")` | 获取分组名称（Swagger 2） |
   | `@Tag(name = "xxx")` | 获取分组名称（OpenAPI 3） |
   | Javadoc 类注释 | 获取模块说明 |

3. **解析方法级别注解**

   | 注解 | 用途 |
   |------|------|
   | `@GetMapping / @PostMapping` 等 | 获取 HTTP 方法和路径 |
   | `@ApiOperation` | 获取接口说明（Swagger 2） |
   | `@Operation` | 获取接口说明（OpenAPI 3） |
   | Javadoc 方法注释 | 获取接口说明 |

### 接口详情解析

对于每个接口方法，解析以下信息：

1. **请求参数**

   | 参数来源 | 解析方式 |
   |---------|---------|
   | `@RequestBody` | 请求体对象，解析 DTO 类字段 |
   | `@RequestParam` | URL 查询参数 |
   | `@PathVariable` | 路径变量 |
   | `@RequestHeader` | 请求头 |

2. **响应对象**

   | 解析方式 | 说明 |
   |---------|------|
   | 返回类型 | 解析 VO/DTO 类字段 |
   | `@ApiResponse` | 响应说明注解 |
   | Javadoc | 返回说明注释 |

3. **DTO/VO 字段解析**

   从 DTO/VO 类中提取字段信息：

   | 字段信息 | 来源 |
   |---------|------|
   | 字段名 | Java 字段名 |
   | 字段类型 | Java 类型 |
   | 说明 | Javadoc 注释 |
   | 是否必填 | `@NotNull` 等校验注解 |
   | 示例值 | `@mock` 注解 |

---

## 枚举类分析

### 扫描枚举类

1. **查找枚举位置**

   ```
   src/main/java/
   ├── ${basePackage}/
   │   ├── enums/              # 枚举类目录
   │   │   ├── TaskStatus.java
   │   │   ├── UserStatus.java
   │   │   └── ...
   │   └── constant/           # 常量类（也可能包含枚举）
   ```

2. **枚举类模式识别**

   常见枚举模式：

   ```java
   // 模式一：标准枚举
   public enum TaskStatus {
       PENDING(0, "待处理"),
       PROCESSING(1, "处理中"),
       COMPLETED(2, "已完成"),
       FAILED(3, "失败");

       private final Integer code;
       private final String desc;

       TaskStatus(Integer code, String desc) {
           this.code = code;
           this.desc = desc;
       }

       // getters...
   }

   // 模式二：简单枚举
   public enum UserStatus {
       ACTIVE("激活"),
       INACTIVE("未激活");

       private final String desc;

       UserStatus(String desc) {
           this.desc = desc;
       }
   }
   ```

### 枚举信息提取

| 信息项 | 提取方式 |
|--------|---------|
| 枚举名称 | 类名（去除 Status/Enum 等后缀） |
| 枚举项 | 枚举常量名 |
| 代码值 | `code` 字段值或 `ordinal()` |
| 说明 | `desc`/`description`/`message` 字段值 |

### 数据字典生成

根据枚举类生成数据字典：

```markdown
### 3.1 任务状态

| 序号 | 字典代码 | 字典值 |
|------|---------|--------|
| 1 | 0 | 待处理 |
| 2 | 1 | 处理中 |
| 3 | 2 | 已完成 |
| 4 | 3 | 失败 |
```

---

## 共用对象识别

### 识别规则

以下对象被识别为"共用参数"：

1. **统一返回对象**（常见命名）
   - `Result` / `CommonResult` / `Response`
   - `ApiResponse` / `BaseResponse`

2. **分页请求对象**
   - `PageRequest` / `PageQuery`
   - `BasePageReq` / `PageParam`

3. **分页响应对象**
   - `PageResult` / `PageInfo`
   - `IPage` / `PageData`

### 共用对象文档生成

```markdown
#### 1.2.3. 统一返回对象数据结构说明

所有接口统一使用此返回格式。

| 参数 | 参数类型 | 说明 |
|------|---------|------|
| code | int | 响应码，200表示成功 |
| message | string | 响应消息 |
| data | object | 响应数据 |
```

---

## 文档生成流程

### 步骤

```
1. 分析项目结构
   ├─ 读取配置文件（端口、context-path）
   ├─ 检测 Swagger/Knife4j 依赖
   └─ 确定基础 URL

2. 扫描 Controller
   ├─ 获取所有 Controller 类
   ├─ 解析每个接口方法
   ├─ 解析请求/响应对象
   └─ 生成接口清单

3. 分析枚举类
   ├─ 扫描 enums 包
   ├─ 提取枚举项
   └─ 生成数据字典

4. 识别共用对象
   ├─ 识别统一返回对象
   ├─ 识别分页对象
   └─ 生成共用参数说明

5. 填充模板
   ├─ 替换变量占位符
   ├─ 生成接口详情
   └─ 输出文档
```

---

## 输出文档结构

生成的 `docs/api-doc.md` 采用客户要求的格式：

```markdown
# 项目名称 接口设计说明书

## 目  录
1. 接口规范
   1.1. 调用说明
   1.2. 共用的参数
   1.3. 返回数据说明
2. 接口方法列表
   2.1. 本系统提供的接口
   2.2. 本系统需要访问的第三方接口
3. 数据字典

## 1. 接口规范
### 1.1. 调用说明
### 1.2. 共用的参数
### 1.3. 返回数据说明

## 2. 接口方法列表
### 2.1. 本系统提供的接口
#### 2.1.1. XXX查询接口
...

## 3. 数据字典
### 3.1. 任务状态
...

## 附录
### A. Swagger/Knife4j 在线文档
### B. 基础信息
### C. 分页参数说明
```

---

## Knife4j 导出说明

文档生成后，用户可以通过 Knife4j 导出更详细的接口文档：

### 导出步骤

1. 访问 `http://localhost:{port}/doc.html`
2. 点击「文档管理」→「离线文档」
3. 选择格式：
   - **Markdown**：适合版本控制
   - **Word**：适合正式文档
   - **HTML**：适合在线查看
   - **PDF**：适合打印分享
4. 点击「下载」

### 导出后合并

如果需要将 Knife4j 导出的内容合并到本文档：

1. 从 Knife4j 导出 Markdown 格式
2. 将接口详情部分复制到「2.1. 本系统提供的接口」章节
3. 保留文档结构和数据字典部分
