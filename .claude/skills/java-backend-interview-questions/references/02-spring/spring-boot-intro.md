# 什么是 Spring Boot

> Java 后端面试知识点 - Spring 生态

---

## Spring Boot 简介

Spring Boot 是基于 Spring 框架的快速开发工具，旨在简化 Spring 应用的创建、配置和部署。

### 核心理念

> **约定优于配置（Convention over Configuration）**

Spring Boot 提供了一套默认配置，让开发者专注于业务逻辑而非繁琐的配置。

---

## Spring Boot 与 Spring 的关系

```
┌────────────────────────────────────────────────────────────────┐
│                        Spring Boot                              │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  自动配置 + Starter + 嵌入式服务器 + Actuator            │  │
│  └──────────────────────────────────────────────────────────┘  │
│                            ↓ 构建于                            │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                    Spring Framework                       │  │
│  │      IoC容器 + AOP + 事务管理 + MVC + 数据访问             │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────┘
```

| 对比项 | Spring | Spring Boot |
|--------|--------|-------------|
| 配置方式 | 大量 XML/注解配置 | 自动配置 + 少量配置 |
| 服务器 | 需要外部 Tomcat | 内嵌 Tomcat/Jetty |
| 启动方式 | 部署 WAR 到服务器 | `java -jar` 直接运行 |
| 依赖管理 | 手动管理版本 | Starter 统一管理 |
| 项目创建 | 复杂 | Spring Initializr |

---

## 传统 Spring vs Spring Boot 对比

### 传统 Spring 配置

```xml
<!-- web.xml -->
<web-app>
    <servlet>
        <servlet-name>dispatcher</servlet-name>
        <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
    </servlet>
    <servlet-mapping>
        <servlet-name>dispatcher</servlet-name>
        <url-pattern>/</url-pattern>
    </servlet-mapping>
</web-app>

<!-- dispatcher-servlet.xml -->
<beans>
    <context:component-scan base-package="com.example"/>
    <mvc:annotation-driven/>
    <bean class="org.springframework.web.servlet.view.InternalResourceViewResolver">
        <property name="prefix" value="/WEB-INF/views/"/>
        <property name="suffix" value=".jsp"/>
    </bean>
</beans>

<!-- applicationContext.xml -->
<beans>
    <bean id="dataSource" class="...">
        <property name="driverClassName" value="..."/>
        <property name="url" value="..."/>
    </bean>
    <!-- 更多配置... -->
</beans>
```

### Spring Boot 方式

```java
// 一个注解 + 一个类 = 完整应用
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

// application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/db
    username: root
    password: 123456
```

---

## Spring Boot 解决的问题

### 1. 配置地狱

**问题**：Spring 需要大量 XML 配置
**解决**：自动配置，约定优于配置

### 2. 依赖版本冲突

**问题**：手动管理依赖，版本不兼容
**解决**：Starter POMs，版本统一管理

### 3. 部署复杂

**问题**：需要外部服务器，WAR 打包
**解决**：内嵌服务器，JAR 直接运行

### 4. 开发效率低

**问题**：项目初始化繁琐
**解决**：Spring Initializr，热部署

---

## 面试要点

### 核心答案

**问：什么是 Spring Boot？**

答：Spring Boot 是基于 Spring 框架的快速开发工具，核心理念是**约定优于配置**。

主要特点：
1. **简化配置**：自动配置，无需大量 XML
2. **快速启动**：内嵌 Tomcat，JAR 直接运行
3. **依赖管理**：Starter 统一管理依赖版本
4. **生产就绪**：Actuator 提供监控端点

### 与 Spring 的区别

Spring Boot 不是 Spring 的替代，而是 Spring 的增强：
- Spring 提供核心功能（IoC、AOP）
- Spring Boot 简化配置和开发流程

---

## 快速开始示例

```java
// 1. 创建主类
@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}

// 2. 创建 Controller
@RestController
@RequestMapping("/api")
public class HelloController {
    
    @GetMapping("/hello")
    public String hello() {
        return "Hello, Spring Boot!";
    }
}

// 3. 配置文件 application.yml
server:
  port: 8080
  
spring:
  application:
    name: demo-service
```

```xml
<!-- pom.xml -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.0</version>
</parent>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```

---

## 编码最佳实践

### ✅ 推荐做法

```java
// 1. 使用 @SpringBootApplication 而非分开的注解
@SpringBootApplication  // = @Configuration + @EnableAutoConfiguration + @ComponentScan
public class Application { }

// 2. 配置类使用 @ConfigurationProperties
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {
    private String name;
    private int timeout;
}

// 3. 使用 profiles 区分环境
// application-dev.yml, application-prod.yml
spring:
  profiles:
    active: dev
```

### ❌ 避免做法

```java
// ❌ 仍然使用大量 XML 配置
// Spring Boot 中应尽量使用 Java Config

// ❌ 硬编码配置值
@Value("localhost:3306")  // 应该用配置文件
private String dbHost;

// ❌ 不使用 Starter，手动添加依赖
// 应使用 spring-boot-starter-* 统一管理
```
