# Spring Boot 核心特性

> Java 后端面试知识点 - Spring 生态

---

## 核心特性概览

| 特性 | 说明 |
|------|------|
| **自动配置** | 根据依赖自动配置 Spring 应用 |
| **Starter POMs** | 预定义依赖集合，简化依赖管理 |
| **嵌入式服务器** | 内嵌 Tomcat/Jetty/Undertow |
| **Actuator** | 生产级监控和管理端点 |
| **外部化配置** | 支持多种配置源和 profile |
| **无代码生成** | 无 XML，无代码生成 |

---

## 1. 自动配置（Auto-Configuration）

### 原理

Spring Boot 根据类路径中的依赖，自动配置 Spring 应用。

```
@SpringBootApplication
        ↓
@EnableAutoConfiguration
        ↓
spring.factories / AutoConfiguration.imports
        ↓
加载所有自动配置类
        ↓
@Conditional* 条件过滤
        ↓
生效的配置注入容器
```

### 核心注解

```java
@SpringBootApplication  // 组合注解
= @SpringBootConfiguration  // 配置类
+ @EnableAutoConfiguration  // 开启自动配置
+ @ComponentScan            // 组件扫描
```

### 自动配置原理

```java
// META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration
org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration

// 自动配置类示例
@AutoConfiguration
@ConditionalOnClass(DataSource.class)  // 条件：类路径存在 DataSource
@ConditionalOnMissingBean(DataSource.class)  // 条件：容器中没有 DataSource
@EnableConfigurationProperties(DataSourceProperties.class)
public class DataSourceAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public DataSource dataSource(DataSourceProperties properties) {
        return createDataSource(properties);
    }
}
```

### @Conditional 系列注解

| 注解 | 条件 |
|------|------|
| `@ConditionalOnClass` | 类路径存在指定类 |
| `@ConditionalOnMissingClass` | 类路径不存在指定类 |
| `@ConditionalOnBean` | 容器中存在指定 Bean |
| `@ConditionalOnMissingBean` | 容器中不存在指定 Bean |
| `@ConditionalOnProperty` | 配置属性满足条件 |
| `@ConditionalOnWebApplication` | Web 应用环境 |

### 查看自动配置报告

```yaml
# application.yml
debug: true  # 启动时打印自动配置报告

# 或启动参数
java -jar app.jar --debug
```

```
============================
CONDITIONS EVALUATION REPORT
============================

Positive matches:  # 生效的自动配置
-----------------
   DataSourceAutoConfiguration matched:
      - @ConditionalOnClass found required class 'javax.sql.DataSource'

Negative matches:  # 未生效的配置
-----------------
   RedisAutoConfiguration:
      Did not match:
         - @ConditionalOnClass did not find required class 'redis.clients.jedis.Jedis'
```

---

## 2. Starter POMs

### 常用 Starter

| Starter | 包含功能 |
|---------|---------|
| `spring-boot-starter-web` | Web 应用（Tomcat, MVC） |
| `spring-boot-starter-data-jpa` | JPA 数据访问 |
| `spring-boot-starter-data-redis` | Redis 支持 |
| `spring-boot-starter-security` | 安全框架 |
| `spring-boot-starter-test` | 测试支持 |
| `spring-boot-starter-actuator` | 监控端点 |
| `spring-boot-starter-validation` | 参数校验 |

### Starter 依赖管理

```xml
<!-- 父 POM 管理版本 -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.0</version>
</parent>

<!-- 引入 Starter，无需指定版本 -->
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
</dependencies>
```

### 自定义 Starter

```java
// 1. 创建自动配置类
@AutoConfiguration
@ConditionalOnClass(MyService.class)
@EnableConfigurationProperties(MyProperties.class)
public class MyAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public MyService myService(MyProperties properties) {
        return new MyService(properties.getConfig());
    }
}

// 2. 配置属性类
@ConfigurationProperties(prefix = "my.service")
public class MyProperties {
    private String config;
    // getter/setter
}

// 3. 注册自动配置
// resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
com.example.MyAutoConfiguration
```

---

## 3. 嵌入式服务器

### 支持的服务器

| 服务器 | 特点 |
|--------|------|
| **Tomcat** | 默认，成熟稳定 |
| **Jetty** | 轻量级，适合长连接 |
| **Undertow** | 高性能，适合高并发 |

### 切换服务器

```xml
<!-- 排除 Tomcat -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- 使用 Undertow -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-undertow</artifactId>
</dependency>
```

### 服务器配置

```yaml
server:
  port: 8080
  tomcat:
    threads:
      max: 200       # 最大线程数
      min-spare: 10  # 最小空闲线程
    max-connections: 10000
    accept-count: 100
    connection-timeout: 20000
```

---

## 4. Actuator 监控

### 启用 Actuator

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### 常用端点

| 端点 | 说明 |
|------|------|
| `/actuator/health` | 健康检查 |
| `/actuator/info` | 应用信息 |
| `/actuator/metrics` | 指标数据 |
| `/actuator/env` | 环境配置 |
| `/actuator/beans` | 所有 Bean |
| `/actuator/mappings` | 请求映射 |
| `/actuator/threaddump` | 线程转储 |
| `/actuator/heapdump` | 堆转储 |

### 配置

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: always
  health:
    redis:
      enabled: true
    db:
      enabled: true
```

### 自定义健康检查

```java
@Component
public class CustomHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        // 检查外部服务
        boolean serviceUp = checkExternalService();
        
        if (serviceUp) {
            return Health.up()
                .withDetail("service", "available")
                .build();
        }
        return Health.down()
            .withDetail("service", "unavailable")
            .build();
    }
}
```

---

## 5. 外部化配置

### 配置优先级（由高到低）

1. 命令行参数
2. JNDI 属性
3. Java 系统属性
4. 操作系统环境变量
5. `application-{profile}.yml`
6. `application.yml`
7. `@PropertySource` 注解
8. 默认属性

### 使用配置

```java
// 方式1: @Value
@Value("${app.name:默认值}")
private String appName;

// 方式2: @ConfigurationProperties（推荐）
@ConfigurationProperties(prefix = "app")
@Data
public class AppConfig {
    private String name;
    private int timeout;
    private List<String> servers;
    private Map<String, String> headers;
}

// 方式3: Environment
@Autowired
private Environment env;

public void demo() {
    String value = env.getProperty("app.name");
}
```

### Profile 配置

```yaml
# application.yml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}

---
# application-dev.yml
server:
  port: 8080
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/dev_db

---
# application-prod.yml  
server:
  port: 80
spring:
  datasource:
    url: jdbc:mysql://prod-server:3306/prod_db
```

---

## 面试要点

### 核心答案

**问：Spring Boot 的核心特性有哪些？**

答：Spring Boot 有六大核心特性：

1. **自动配置**
   - 根据类路径依赖自动配置 Spring 应用
   - 使用 `@Conditional` 条件化装配

2. **Starter POMs**
   - 预定义依赖集合，统一版本管理
   - 开箱即用，引入即可使用

3. **嵌入式服务器**
   - 内嵌 Tomcat/Jetty/Undertow
   - 无需外部服务器，JAR 直接运行

4. **Actuator**
   - 提供健康检查、指标监控、环境信息等端点
   - 生产级运维支持

5. **外部化配置**
   - 支持多种配置源（YAML, Properties, 环境变量）
   - Profile 支持多环境配置

6. **无代码生成**
   - 无需 XML 配置
   - 纯注解驱动

---

## 编码最佳实践

### ✅ 推荐做法

```java
// 1. 使用 @ConfigurationProperties 绑定配置
@ConfigurationProperties(prefix = "app.datasource")
@Validated  // 开启校验
@Data
public class DataSourceConfig {
    @NotEmpty
    private String url;
    private int poolSize = 10;  // 提供默认值
}

// 2. 使用 Actuator 健康检查
@Component
public class DatabaseHealthIndicator extends AbstractHealthIndicator {
    @Override
    protected void doHealthCheck(Health.Builder builder) {
        // 检查数据库连接
        builder.up().withDetail("database", "MySQL");
    }
}

// 3. Profile 区分环境
@Profile("prod")
@Configuration
public class ProdConfig { }
```

### ❌ 避免做法

```java
// ❌ 硬编码配置
private String dbUrl = "jdbc:mysql://localhost:3306/db";

// ❌ 不使用 Starter，手动配置
// 应使用 spring-boot-starter-data-redis

// ❌ 暴露所有 Actuator 端点
management:
  endpoints:
    web:
      exposure:
        include: "*"  # 生产环境危险
```
