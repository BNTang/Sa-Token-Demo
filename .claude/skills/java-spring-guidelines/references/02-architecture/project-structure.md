# 工程结构规范

> Java/Spring Boot 编码规范 - 工程结构
> 参考：阿里巴巴 Java 开发手册

---

## 应用分层

### 标准分层架构

```
┌────────────────────────────────────────────────────────────┐
│                    终端显示层 (View)                        │
│            Web/App/小程序/开放接口等                        │
├────────────────────────────────────────────────────────────┤
│                   开放接口层 (Controller)                   │
│     封装 Service 方法，进行参数校验、权限校验               │
├────────────────────────────────────────────────────────────┤
│                   业务逻辑层 (Service)                      │
│     核心业务逻辑，事务控制                                   │
├────────────────────────────────────────────────────────────┤
│                   数据访问层 (Mapper/DAO)                   │
│     与数据库交互，ER 模型交互                                │
├────────────────────────────────────────────────────────────┤
│                   外部接口层 (Client/Manager)               │
│     调用第三方服务、消息队列、缓存等                         │
└────────────────────────────────────────────────────────────┘
```

### 各层职责

| 层 | 职责 | 命名规范 |
|---|------|---------|
| **Controller** | 接收请求、参数校验、调用 Service、返回结果 | `*Controller` |
| **Service** | 业务逻辑、事务控制、数据组装 | `I*Service` / `*ServiceImpl` |
| **Mapper/DAO** | 数据库 CRUD | `*Mapper` |
| **Manager** | 通用业务处理、第三方接口封装、缓存操作 | `*Manager` |
| **Client** | RPC 调用封装、HTTP 调用 | `*Client` |

---

## 项目目录结构

### 方案一：按技术分层（适合小型项目）

```
project/
├── src/
│   ├── main/
│   │   ├── java/com/company/project/
│   │   │   │
│   │   │   ├── 📁 web/                     # ========== Web 层 ==========
│   │   │   │   ├── controller/            # RESTful 接口
│   │   │   │   ├── filter/                # 过滤器
│   │   │   │   ├── interceptor/           # 拦截器
│   │   │   │   └── advice/                # 全局异常处理、响应增强
│   │   │   │
│   │   │   ├── 📁 service/                # ========== 业务层 ==========
│   │   │   │   ├── I*Service.java         # 服务接口
│   │   │   │   └── impl/                  # 服务实现
│   │   │   │       └── *ServiceImpl.java
│   │   │   │
│   │   │   ├── 📁 manager/                # ========== 通用业务层 ==========
│   │   │   │   └── *Manager.java          # 通用业务处理、缓存、第三方封装
│   │   │   │
│   │   │   ├── 📁 repository/             # ========== 数据访问层 ==========
│   │   │   │   ├── mapper/                # MyBatis Mapper 接口
│   │   │   │   │   └── *Mapper.java
│   │   │   │   └── entity/                # 数据库实体（DO）
│   │   │   │       ├── *DO.java           # 表对应实体
│   │   │   │       └── base/              # 基类（BaseEntity）
│   │   │   │
│   │   │   ├── 📁 integration/            # ========== 外部集成层 ==========
│   │   │   │   ├── client/                # RPC/HTTP 客户端
│   │   │   │   │   └── *Client.java
│   │   │   │   ├── mq/                    # 消息队列
│   │   │   │   │   ├── producer/          # 消息生产者
│   │   │   │   │   └── consumer/          # 消息消费者
│   │   │   │   └── schedule/              # 定时任务
│   │   │   │       └── *Job.java
│   │   │   │
│   │   │   ├── 📁 model/                  # ========== 数据模型 ==========
│   │   │   │   ├── dto/                   # 数据传输对象
│   │   │   │   │   ├── req/               # 请求参数
│   │   │   │   │   │   ├── *AddReq.java
│   │   │   │   │   │   ├── *UpdateReq.java
│   │   │   │   │   │   └── *QueryReq.java
│   │   │   │   │   └── rsp/               # 响应对象
│   │   │   │   │       ├── *DetailRsp.java
│   │   │   │   │       └── *PageRsp.java
│   │   │   │   ├── vo/                    # 视图对象（可选）
│   │   │   │   ├── bo/                    # 业务对象（可选）
│   │   │   │   └── query/                 # 查询条件对象
│   │   │   │
│   │   │   ├── 📁 common/                 # ========== 公共模块 ==========
│   │   │   │   ├── config/                # 配置类
│   │   │   │   │   ├── MybatisConfig.java
│   │   │   │   │   ├── RedisConfig.java
│   │   │   │   │   └── WebMvcConfig.java
│   │   │   │   ├── constants/             # 常量
│   │   │   │   │   └── *Constants.java
│   │   │   │   ├── enums/                 # 枚举
│   │   │   │   │   └── *Enum.java
│   │   │   │   ├── exception/             # 自定义异常
│   │   │   │   │   ├── BusinessException.java
│   │   │   │   │   └── ErrorCode.java
│   │   │   │   ├── utils/                 # 工具类
│   │   │   │   │   └── *Utils.java
│   │   │   │   ├── aspect/                # AOP 切面
│   │   │   │   │   └── *Aspect.java
│   │   │   │   ├── annotation/            # 自定义注解
│   │   │   │   │   └── @*.java
│   │   │   │   └── validator/             # 自定义校验器
│   │   │   │
│   │   │   └── Application.java           # 启动类
│   │   │
│   │   └── resources/
│   │       ├── mapper/                    # MyBatis XML 映射
│   │       │   └── *.xml
│   │       ├── static/                    # 静态资源
│   │       ├── templates/                 # 模板文件
│   │       ├── application.yml            # 主配置
│   │       ├── application-dev.yml        # 开发环境
│   │       ├── application-test.yml       # 测试环境
│   │       ├── application-staging.yml    # 预发环境
│   │       ├── application-prod.yml       # 生产环境
│   │       ├── logback-spring.xml         # 日志配置
│   │       └── banner.txt                 # 启动横幅
│   │
│   └── test/
│       └── java/                          # 单元测试
│           └── com/company/project/
│               ├── service/               # Service 测试
│               ├── controller/            # Controller 测试
│               └── mapper/                # Mapper 测试
│
└── pom.xml
```

### 方案二：按业务模块分包（推荐，适合中大型项目）

```
project/
└── src/main/java/com/company/project/
    │
    ├── 📦 common/                         # ========== 公共基础模块 ==========
    │   ├── base/                          # 基类
    │   │   ├── BaseController.java        # Controller 基类
    │   │   ├── BaseService.java           # Service 基类
    │   │   └── BaseEntity.java            # Entity 基类
    │   ├── config/                        # 全局配置
    │   ├── constants/                     # 全局常量
    │   ├── enums/                         # 全局枚举
    │   ├── exception/                     # 全局异常
    │   ├── utils/                         # 工具类
    │   ├── aspect/                        # 切面
    │   ├── annotation/                    # 注解
    │   └── model/                         # 通用模型
    │       └── CommonResult.java          # 统一返回结果
    │
    ├── 📦 module1/                        # ========== 业务模块1：商品 ==========
    │   ├── controller/
    │   │   └── ProductController.java     # GET    /products/{id}
    │   │                                  # POST   /products
    │   │                                  # PUT    /products/{id}
    │   │                                  # DELETE /products/{id}
    │   ├── service/
    │   │   ├── IProductService.java
    │   │   └── impl/
    │   │       └── ProductServiceImpl.java
    │   ├── manager/
    │   │   └── ProductCacheManager.java   # 缓存处理
    │   ├── mapper/
    │   │   └── ProductMapper.java
    │   ├── entity/
    │   │   └── ProductDO.java
    │   ├── model/
    │   │   ├── req/
    │   │   │   ├── ProductAddReq.java
    │   │   │   ├── ProductUpdateReq.java
    │   │   │   └── ProductQueryReq.java
    │   │   ├── rsp/
    │   │   │   ├── ProductDetailRsp.java
    │   │   │   └── ProductPageRsp.java
    │   │   └── dto/
    │   │       └── ProductDTO.java
    │   ├── enums/
    │   │   ├── ProductStatus.java
    │   │   └── ProductType.java
    │   └── constants/
    │       └── ProductConstants.java
    │
    ├── 📦 module2/                        # ========== 业务模块2：订单 ==========
    │   ├── controller/
    │   │   └── OrderController.java       # RESTful 风格
    │   ├── service/
    │   │   ├── IOrderService.java
    │   │   └── impl/
    │   ├── manager/
    │   │   └── OrderManager.java          # 订单通用处理
    │   ├── mapper/
    │   │   ├── OrderMapper.java
    │   │   └── OrderItemMapper.java
    │   ├── entity/
    │   │   ├── OrderDO.java
    │   │   └── OrderItemDO.java
    │   ├── model/
    │   │   ├── req/
    │   │   ├── rsp/
    │   │   └── dto/
    │   ├── enums/
    │   │   ├── OrderStatus.java
    │   │   └── PaymentType.java
    │   └── mq/                            # 模块内消息队列
    │       ├── producer/
    │       │   └── OrderEventProducer.java
    │       └── consumer/
    │           └── OrderPaymentConsumer.java
    │
    ├── 📦 module3/                        # ========== 业务模块3：用户 ==========
    │   ├── controller/
    │   ├── service/
    │   ├── mapper/
    │   ├── entity/
    │   └── model/
    │
    ├── 📦 integration/                    # ========== 外部集成（跨模块） ==========
    │   ├── client/                        # RPC/HTTP 调用
    │   │   ├── PaymentClient.java         # 支付服务
    │   │   └── LogisticsClient.java       # 物流服务
    │   └── schedule/                      # 定时任务（跨模块）
    │       └── DataSyncJob.java
    │
    ├── 📁 web/                            # ========== Web 配置（跨模块） ==========
    │   ├── filter/
    │   │   └── AuthFilter.java
    │   ├── interceptor/
    │   │   └── LoginInterceptor.java
    │   └── advice/
    │       ├── GlobalExceptionHandler.java
    │       └── ResponseAdvice.java
    │
    └── Application.java                   # 启动类
```

### 目录设计原则

| 原则 | 说明 | 示例 |
|------|------|------|
| **按职责分层** | 每层职责单一明确 | Controller 只做参数校验和调用 |
| **按业务聚合** | 相关功能放在一起 | 商品相关的代码在 product 包下 |
| **依赖方向单一** | 高层依赖低层，不能反向 | Controller → Service → Mapper |
| **公共下沉** | 公共代码抽取到 common | 工具类、基类、常量 |
| **模块隔离** | 业务模块之间不直接依赖 | 通过接口或事件通信 |
| **清晰命名** | 包名见名知意 | web/service/repository/integration |

---

## 多模块项目（Maven）

### 标准 Maven 多模块结构

```
project-parent/                            # 父工程
├── pom.xml                                # 父 POM（聚合所有模块）
│
├── 📦 project-api/                        # ========== API 接口模块 ==========
│   ├── pom.xml                            # 对外提供的接口定义
│   └── src/main/java/.../api/
│       ├── dto/                           # 传输对象
│       │   ├── req/
│       │   └── rsp/
│       ├── enums/                         # 枚举
│       ├── constants/                     # 常量
│       └── IRemoteService.java            # Feign 接口
│
├── 📦 project-common/                     # ========== 公共模块 ==========
│   ├── pom.xml                            # 公共工具和基础设施
│   └── src/main/java/.../common/
│       ├── base/                          # 基类
│       │   ├── BaseController.java
│       │   ├── BaseService.java
│       │   └── BaseEntity.java
│       ├── exception/                     # 异常定义
│       │   ├── BusinessException.java
│       │   └── ErrorCode.java
│       ├── utils/                         # 工具类
│       │   ├── DateUtils.java
│       │   ├── JsonUtils.java
│       │   └── StringUtils.java
│       ├── config/                        # 通用配置
│       │   ├── RedisConfig.java
│       │   └── MybatisConfig.java
│       ├── aspect/                        # 切面
│       ├── annotation/                    # 注解
│       └── model/                         # 通用模型
│           └── CommonResult.java
│
├── 📦 project-domain/                     # ========== 领域模型模块（可选） ==========
│   ├── pom.xml                            # 核心业务实体和领域逻辑
│   └── src/main/java/.../domain/
│       ├── entity/                        # 业务实体（DO）
│       ├── vo/                            # 值对象
│       └── repository/                    # 仓储接口
│
├── 📦 project-service/                    # ========== 业务服务模块 ==========
│   ├── pom.xml                            # 核心业务逻辑实现
│   └── src/main/java/.../service/
│       ├── product/                       # 商品业务
│       │   ├── controller/
│       │   ├── service/
│       │   ├── manager/
│       │   ├── mapper/
│       │   ├── entity/
│       │   └── model/
│       ├── order/                         # 订单业务
│       │   ├── controller/
│       │   ├── service/
│       │   ├── mapper/
│       │   ├── entity/
│       │   └── model/
│       ├── integration/                   # 外部集成
│       │   ├── client/
│       │   ├── mq/
│       │   └── schedule/
│       └── web/                           # Web 层配置
│           ├── filter/
│           ├── interceptor/
│           └── advice/
│
├── 📦 project-job/                        # ========== 定时任务模块（可选） ==========
│   ├── pom.xml                            # 独立的定时任务应用
│   └── src/main/java/.../job/
│       ├── task/                          # 任务定义
│       ├── config/                        # 调度配置
│       └── JobApplication.java            # 任务启动类
│
├── 📦 project-mq/                         # ========== 消息消费模块（可选） ==========
│   ├── pom.xml                            # 独立的消息消费应用
│   └── src/main/java/.../mq/
│       ├── consumer/                      # 消费者
│       ├── config/                        # MQ 配置
│       └── MqApplication.java             # 消费者启动类
│
└── 📦 project-web/                        # ========== Web 启动模块 ==========
    ├── pom.xml                            # Web 应用入口
    └── src/main/
        ├── java/
        │   └── Application.java           # 主启动类
        └── resources/
            ├── application.yml            # 配置文件
            ├── application-dev.yml
            ├── application-test.yml
            ├── application-prod.yml
            └── logback-spring.xml
```

### Maven 模块依赖关系

```
                   project-web (启动)
                        │
                        ├─────────────────┐
                        │                 │
                        ▼                 ▼
                 project-service     project-job
                        │                 │
        ┌───────────────┼─────────────┐   │
        │               │             │   │
        ▼               ▼             ▼   ▼
   project-api   project-domain  project-common
        │               │             │
        └───────────────┴─────────────┘
                        │
                        ▼
              第三方依赖（Spring Boot、MyBatis 等）
```

### 父 POM 配置示例

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.company</groupId>
    <artifactId>project-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <!-- 聚合所有子模块 -->
    <modules>
        <module>project-common</module>
        <module>project-api</module>
        <module>project-domain</module>
        <module>project-service</module>
        <module>project-job</module>
        <module>project-mq</module>
        <module>project-web</module>
    </modules>

    <!-- 统一版本管理 -->
    <properties>
        <java.version>17</java.version>
        <spring-boot.version>3.2.0</spring-boot.version>
        <mybatis-plus.version>3.5.5</mybatis-plus.version>
        <hutool.version>5.8.24</hutool.version>
        <project.version>1.0.0-SNAPSHOT</project.version>
    </properties>

    <!-- 依赖版本管理（不会实际引入） -->
    <dependencyManagement>
        <dependencies>
            <!-- Spring Boot BOM -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <!-- 内部模块 -->
            <dependency>
                <groupId>com.company</groupId>
                <artifactId>project-common</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.company</groupId>
                <artifactId>project-api</artifactId>
                <version>${project.version}</version>
            </dependency>

            <!-- 第三方依赖 -->
            <dependency>
                <groupId>com.baomidou</groupId>
                <artifactId>mybatis-plus-boot-starter</artifactId>
                <version>${mybatis-plus.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <!-- 所有子模块通用的依赖 -->
    <dependencies>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
    </dependencies>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <version>${spring-boot.version}</version>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

### 子模块 POM 示例

```xml
<!-- project-service/pom.xml -->
<project>
    <parent>
        <groupId>com.company</groupId>
        <artifactId>project-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>project-service</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <!-- 依赖内部模块（版本继承自 parent） -->
        <dependency>
            <groupId>com.company</groupId>
            <artifactId>project-common</artifactId>
        </dependency>
        <dependency>
            <groupId>com.company</groupId>
            <artifactId>project-api</artifactId>
        </dependency>

        <!-- 依赖第三方（版本继承自 parent） -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
        </dependency>
    </dependencies>
</project>
```

### 多模块拆分原则

| 模块 | 职责 | 依赖关系 | 是否可运行 |
|------|------|---------|-----------|
| **project-api** | 对外接口定义、DTO | 无依赖或依赖 common | ❌ 否 |
| **project-common** | 公共工具、基础设施 | 第三方工具包 | ❌ 否 |
| **project-domain** | 领域模型、核心业务实体 | 依赖 common | ❌ 否 |
| **project-service** | 业务逻辑实现 | 依赖 api、domain、common | ❌ 否 |
| **project-web** | Web 应用入口 | 依赖 service | ✅ 是 |
| **project-job** | 定时任务独立应用 | 依赖 service 或 common | ✅ 是 |
| **project-mq** | 消息消费独立应用 | 依赖 service 或 common | ✅ 是 |

**【强制】依赖方向必须是单向的，禁止循环依赖。**
**【推荐】可独立运行的模块（web、job、mq）应使用 Spring Boot 插件打成可执行 JAR。**

---

## 分层领域模型规约

### 数据对象分类

| 对象类型 | 英文全称 | 中文说明 | 使用场景 | 位置 | 命名示例 |
|---------|---------|---------|---------|------|---------|
| **DO** | Data Object | 数据库对象 | 与数据库表一一对应 | entity/ | `ProductDO.java` |
| **DTO** | Data Transfer Object | 数据传输对象 | 服务间、层间传输 | dto/ | `ProductDTO.java` |
| **BO** | Business Object | 业务对象 | 业务逻辑封装 | bo/ | `OrderBO.java` |
| **VO** | View Object | 视图对象 | 前端展示 | vo/ | `ProductVO.java` |
| **Req** | Request Object | 请求对象 | Controller 接收参数 | dto/req/ | `ProductAddReq.java` |
| **Rsp** | Response Object | 响应对象 | Controller 返回数据 | dto/rsp/ | `ProductDetailRsp.java` |
| **Query** | Query Object | 查询对象 | 封装查询条件 | query/ | `ProductQuery.java` |
| **Command** | Command Object | 命令对象 | 封装操作指令（CQRS） | command/ | `CreateOrderCommand.java` |

### 数据流转规则

```
┌─────────────────────────────────────────────────────────────┐
│                    前端 / 客户端                             │
└─────────────────────────────────────────────────────────────┘
                             │ JSON
                             ▼
┌─────────────────────────────────────────────────────────────┐
│  Controller 层              Req/Rsp                          │
│  ├─ 接收参数：ProductAddReq                                  │
│  ├─ 参数校验：@Valid                                         │
│  ├─ 调用服务：productService.add(req)                        │
│  └─ 返回结果：ProductDetailRsp                               │
└─────────────────────────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│  Service 层                 DTO/BO                           │
│  ├─ Req → DTO：接收 Controller 参数                          │
│  ├─ DTO → DO：准备数据库操作                                 │
│  ├─ DO → DTO：查询结果转换                                   │
│  └─ DTO → Rsp：返回给 Controller                            │
└─────────────────────────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│  Mapper 层                  DO                               │
│  ├─ 插入：insert(ProductDO)                                  │
│  ├─ 更新：update(ProductDO)                                  │
│  ├─ 查询：selectById() → ProductDO                           │
│  └─ 删除：deleteById()                                       │
└─────────────────────────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                       数据库（MySQL）                         │
└─────────────────────────────────────────────────────────────┘
```

### 对象转换代码示例

```java
/**
 * Controller 层：处理 Req 和 Rsp
 */
@RestController
@RequestMapping("/products")
public class ProductController {

    private final IProductService productService;

    // 接收 Req，返回 Rsp
    @PostMapping
    public CommonResult<Long> create(@Valid @RequestBody ProductAddReq req) {
        Long productId = productService.addProduct(req);
        return CommonResult.success(productId);
    }

    @GetMapping("/{id}")
    public CommonResult<ProductDetailRsp> getById(@PathVariable Long id) {
        ProductDetailRsp rsp = productService.getProductDetail(id);
        return CommonResult.success(rsp);
    }
}

/**
 * Service 层：处理 DTO 和 DO 的转换
 */
@Service
public class ProductServiceImpl implements IProductService {

    private final ProductMapper productMapper;

    @Override
    public Long addProduct(ProductAddReq req) {
        // 1. Req/DTO → DO（准备入库）
        ProductDO productDO = new ProductDO();
        BeanUtils.copyProperties(req, productDO);
        productDO.setCreateTime(LocalDateTime.now());
        
        // 2. 调用 Mapper 插入
        productMapper.insert(productDO);
        
        return productDO.getId();
    }

    @Override
    public ProductDetailRsp getProductDetail(Long id) {
        // 1. 查询 DO
        ProductDO productDO = productMapper.selectById(id);
        if (productDO == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        
        // 2. DO → DTO/Rsp（返回前端）
        ProductDetailRsp rsp = new ProductDetailRsp();
        BeanUtils.copyProperties(productDO, rsp);
        
        // 3. 补充额外信息（如关联查询）
        rsp.setCategoryName(getCategoryName(productDO.getCategoryId()));
        
        return rsp;
    }

    @Override
    public ProductDTO getProductDTO(Long id) {
        // 服务间传输使用 DTO
        ProductDO productDO = productMapper.selectById(id);
        
        ProductDTO dto = new ProductDTO();
        BeanUtils.copyProperties(productDO, dto);
        
        return dto;
    }
}

/**
 * Mapper 层：只处理 DO
 */
@Mapper
public interface ProductMapper extends BaseMapper<ProductDO> {
    
    // MyBatis-Plus 提供的基础方法
    // - insert(ProductDO entity)
    // - updateById(ProductDO entity)
    // - selectById(Long id)
    // - deleteById(Long id)
    
    /**
     * 自定义复杂查询（返回 DO）
     */
    List<ProductDO> selectByCondition(@Param("query") ProductQuery query);
}
```

### 对象设计规范

#### DO（Data Object）- 数据库对象

```java
/**
 * 商品表实体
 * 表名：t_product
 */
@Data
@TableName("t_product")
public class ProductDO extends BaseEntity {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 商品编码
     */
    private String code;
    
    /**
     * 商品名称
     */
    private String name;
    
    /**
     * 价格（单位：分）
     */
    private Long price;
    
    /**
     * 状态：0-下架，1-上架
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    @TableLogic
    private Integer deleted;
}
```

#### DTO（Data Transfer Object）- 服务间传输

```java
/**
 * 商品传输对象（服务间调用）
 */
@Data
public class ProductDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private String code;
    private String name;
    private BigDecimal price;      // 转换为元
    private Integer status;
    private String statusDesc;     // 状态描述
    private String categoryName;   // 关联分类名称
}
```

#### Req（Request Object）- 请求对象

```java
/**
 * 新增商品请求
 */
@Data
public class ProductAddReq {
    
    @NotBlank(message = "商品编码不能为空")
    private String code;
    
    @NotBlank(message = "商品名称不能为空")
    @Length(max = 100, message = "商品名称最长100字符")
    private String name;
    
    @NotNull(message = "商品价格不能为空")
    @DecimalMin(value = "0.01", message = "商品价格必须大于0")
    private BigDecimal price;
    
    @NotNull(message = "分类ID不能为空")
    private Long categoryId;
}

/**
 * 商品查询请求
 */
@Data
public class ProductQueryReq {
    
    /**
     * 商品名称（模糊查询）
     */
    private String name;
    
    /**
     * 状态
     */
    private Integer status;
    
    /**
     * 分类ID
     */
    private Long categoryId;
    
    /**
     * 价格区间-最小值
     */
    private BigDecimal minPrice;
    
    /**
     * 价格区间-最大值
     */
    private BigDecimal maxPrice;
}
```

#### Rsp（Response Object）- 响应对象

```java
/**
 * 商品详情响应
 */
@Data
public class ProductDetailRsp {
    
    private Long id;
    private String code;
    private String name;
    private BigDecimal price;
    private Integer status;
    private String statusDesc;
    
    /**
     * 关联信息
     */
    private Long categoryId;
    private String categoryName;
    
    /**
     * 库存信息
     */
    private Integer stock;
    private Integer sales;
    
    /**
     * 时间信息
     */
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

### 转换工具推荐

```java
/**
 * 对象转换工具（推荐使用 MapStruct）
 */
@Mapper(componentModel = "spring")
public interface ProductConverter {
    
    ProductConverter INSTANCE = Mappers.getMapper(ProductConverter.class);
    
    /**
     * Req → DO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", expression = "java(java.time.LocalDateTime.now())")
    ProductDO reqToDO(ProductAddReq req);
    
    /**
     * DO → DTO
     */
    @Mapping(target = "price", expression = "java(convertPrice(source.getPrice()))")
    ProductDTO doToDTO(ProductDO source);
    
    /**
     * DO → Rsp
     */
    ProductDetailRsp doToRsp(ProductDO source);
    
    /**
     * 价格转换：分 → 元
     */
    default BigDecimal convertPrice(Long priceInCent) {
        return priceInCent == null ? null : 
               BigDecimal.valueOf(priceInCent).divide(BigDecimal.valueOf(100));
    }
}
```

### 对象使用规范

| 规范 | 说明 |
|------|------|
| **DO 不出 Mapper** | DO 只在 Mapper 层使用，不向上传递 |
| **Controller 不接收 DO** | Controller 接收 Req，返回 Rsp |
| **Service 返回 DTO** | 服务间调用使用 DTO，不暴露 DO |
| **VO 可选** | 如果 Rsp 足够用，无需额外定义 VO |
| **Req/Rsp 必须校验** | Req 必须加 `@Valid`，字段必须加校验注解 |
| **DO 必须序列化** | 如果使用缓存，DO 必须实现 Serializable |
| **转换统一封装** | 使用 MapStruct 或工具类统一转换，避免散落各处 |

---

## 二方库依赖

### 版本管理

**【强制】二方库版本号命名：主版本号.次版本号.修订号**

| 版本号 | 变更情况 |
|--------|---------|
| 主版本号 | 产品方向改变，或大规模 API 不兼容 |
| 次版本号 | 保持兼容，增加主要功能 |
| 修订号 | 保持兼容，修复 bug 或小功能 |

```xml
<!-- ✅ 正例 - 使用 dependencyManagement 统一管理版本 -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.company</groupId>
            <artifactId>common-utils</artifactId>
            <version>1.2.3</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 依赖原则

**【强制】禁止在子项目的 pom 中出现相同 groupId、artifactId 但不同 version。**

```xml
<!-- ❌ 反例 - 版本冲突 -->
<!-- 模块 A -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-lang3</artifactId>
    <version>3.12.0</version>
</dependency>

<!-- 模块 B -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-lang3</artifactId>
    <version>3.14.0</version>
</dependency>

<!-- ✅ 正例 - 父 pom 统一管理 -->
<!-- parent pom.xml -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
            <version>3.14.0</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

**【推荐】所有 pom 文件中的依赖声明放在 `<dependencies>` 语句块中，所有版本号放在 `<properties>` 中。**

```xml
<properties>
    <spring-boot.version>3.2.0</spring-boot.version>
    <mybatis-plus.version>3.5.5</mybatis-plus.version>
    <hutool.version>5.8.24</hutool.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <version>${spring-boot.version}</version>
    </dependency>
</dependencies>
```

---

## 配置文件规范

### 多环境配置

```
resources/
├── application.yml           # 公共配置
├── application-dev.yml       # 开发环境
├── application-test.yml      # 测试环境
├── application-staging.yml   # 预发环境
└── application-prod.yml      # 生产环境
```

### 配置分离原则

```yaml
# application.yml - 公共配置
spring:
  application:
    name: order-service
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}

# 非敏感配置放在配置文件
server:
  port: 8080

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

```yaml
# application-prod.yml - 敏感配置用环境变量
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  redis:
    host: ${REDIS_HOST}
    password: ${REDIS_PASSWORD}
```

---

## 启动类规范

```java
/**
 * 订单服务启动类
 *
 * @author zhangsan
 * @since 2026-01-01
 */
@SpringBootApplication
@MapperScan("com.company.order.mapper")
@EnableFeignClients(basePackages = "com.company.order.client")
@EnableScheduling
public class OrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
```

---

## 禁则速查

| ❌ 禁止 | ✅ 正确 | 原因 |
|--------|--------|------|
| Controller 调用 Mapper | Controller → Service → Mapper | 分层规范 |
| Service 返回 DO | Service 返回 DTO | 隔离数据库结构 |
| 循环依赖 | 单向依赖 | 模块解耦 |
| 版本号散落各处 | properties 统一管理 | 版本一致性 |
| 敏感配置写死 | 环境变量/配置中心 | 安全性 |
| 公共代码复制粘贴 | 抽取到 common 模块 | DRY 原则 |
