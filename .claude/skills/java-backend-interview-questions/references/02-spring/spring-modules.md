# Spring 由哪些重要的模块组成？

## 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                    Spring Framework 模块                     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │                    Test                             │  │
│   │            spring-test (单元测试、集成测试)           │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   ┌───────────────────────┬─────────────────────────────┐  │
│   │        Data Access    │         Web                 │  │
│   │  ┌─────────────────┐  │  ┌───────────────────────┐  │  │
│   │  │  spring-jdbc    │  │  │  spring-web           │  │  │
│   │  │  spring-tx      │  │  │  spring-webmvc        │  │  │
│   │  │  spring-orm     │  │  │  spring-webflux       │  │  │
│   │  │  spring-oxm     │  │  │  spring-websocket     │  │  │
│   │  └─────────────────┘  │  └───────────────────────┘  │  │
│   └───────────────────────┴─────────────────────────────┘  │
│                                                             │
│   ┌───────────────────────┬─────────────────────────────┐  │
│   │         AOP           │       Messaging             │  │
│   │  ┌─────────────────┐  │  ┌───────────────────────┐  │  │
│   │  │  spring-aop     │  │  │  spring-messaging     │  │  │
│   │  │  spring-aspects │  │  │  spring-jms           │  │  │
│   │  └─────────────────┘  │  └───────────────────────┘  │  │
│   └───────────────────────┴─────────────────────────────┘  │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │                      Core                           │  │
│   │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────┐  │  │
│   │  │  Beans   │ │   Core   │ │ Context  │ │  SpEL  │  │  │
│   │  └──────────┘ └──────────┘ └──────────┘ └────────┘  │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 核心模块详解

### 1. Core Container（核心容器）

```
┌─────────────────────────────────────────────────────────────┐
│                   Core Container 模块                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  spring-core                                        │  │
│   │  • 框架基础工具类                                    │  │
│   │  • 资源访问 (Resource)                              │  │
│   │  • 类型转换 (ConversionService)                      │  │
│   │  • 环境抽象 (Environment)                            │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  spring-beans                                       │  │
│   │  • BeanFactory (IoC 容器基础)                        │  │
│   │  • Bean 定义 (BeanDefinition)                        │  │
│   │  • Bean 生命周期管理                                 │  │
│   │  • 依赖注入实现                                      │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  spring-context                                     │  │
│   │  • ApplicationContext (高级容器)                     │  │
│   │  • 事件发布 (ApplicationEvent)                       │  │
│   │  • 国际化 (MessageSource)                            │  │
│   │  • 注解驱动 (@Component, @Autowired...)              │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  spring-expression (SpEL)                           │  │
│   │  • Spring 表达式语言                                 │  │
│   │  • #{...} 表达式                                     │  │
│   │  • 属性访问、方法调用、运算符                         │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

```java
// spring-context 核心类示例
@Configuration
@ComponentScan("com.example")
public class AppConfig {
    
    @Bean
    public DataSource dataSource() {
        // ...
    }
}

// ApplicationContext 使用
ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
UserService userService = context.getBean(UserService.class);

// SpEL 表达式
@Value("#{systemProperties['user.home']}")
private String userHome;

@Value("#{T(java.lang.Math).random() * 100}")
private double randomNumber;
```

### 2. AOP 模块

```
┌─────────────────────────────────────────────────────────────┐
│                      AOP 模块                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  spring-aop                                         │  │
│   │  • 代理机制 (JDK / CGLIB)                            │  │
│   │  • 切点表达式 (Pointcut)                             │  │
│   │  • 通知类型 (Advice)                                 │  │
│   │  • 切面定义 (Advisor)                                │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  spring-aspects                                     │  │
│   │  • AspectJ 集成                                      │  │
│   │  • @Aspect 注解支持                                  │  │
│   │  • 编译时/加载时织入                                 │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

```java
// AOP 使用示例
@Aspect
@Component
public class LogAspect {
    
    @Around("execution(* com.example.service.*.*(..))")
    public Object logAround(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return pjp.proceed();
        } finally {
            long duration = System.currentTimeMillis() - start;
            log.info("方法 {} 执行耗时 {}ms", pjp.getSignature().getName(), duration);
        }
    }
}
```

### 3. Data Access 模块

```
┌─────────────────────────────────────────────────────────────┐
│                    Data Access 模块                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  spring-jdbc                                        │  │
│   │  • JdbcTemplate                                     │  │
│   │  • NamedParameterJdbcTemplate                       │  │
│   │  • DataSource 管理                                   │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  spring-tx                                          │  │
│   │  • 事务抽象 (PlatformTransactionManager)             │  │
│   │  • 声明式事务 (@Transactional)                       │  │
│   │  • 编程式事务 (TransactionTemplate)                  │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  spring-orm                                         │  │
│   │  • JPA 集成                                          │  │
│   │  • Hibernate 集成                                    │  │
│   │  • 事务管理器集成                                    │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

```java
// JdbcTemplate 使用
@Repository
public class UserRepository {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    public User findById(Long id) {
        return jdbcTemplate.queryForObject(
            "SELECT * FROM users WHERE id = ?",
            new BeanPropertyRowMapper<>(User.class),
            id
        );
    }
}

// 声明式事务
@Service
public class OrderService {
    
    @Transactional(rollbackFor = Exception.class)
    public void createOrder(Order order) {
        // 事务内操作
    }
}
```

### 4. Web 模块

```
┌─────────────────────────────────────────────────────────────┐
│                       Web 模块                               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  spring-web                                         │  │
│   │  • Web 基础设施                                      │  │
│   │  • HTTP 客户端 (RestTemplate, WebClient)             │  │
│   │  • 文件上传、过滤器                                  │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  spring-webmvc                                      │  │
│   │  • DispatcherServlet (前端控制器)                    │  │
│   │  • @Controller, @RequestMapping                      │  │
│   │  • 视图解析、参数绑定                                │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  spring-webflux                                     │  │
│   │  • 响应式 Web 框架                                   │  │
│   │  • 非阻塞 I/O                                        │  │
│   │  • Mono/Flux 响应式流                                │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  spring-websocket                                   │  │
│   │  • WebSocket 支持                                    │  │
│   │  • STOMP 协议                                        │  │
│   │  • SockJS 回退                                       │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

```java
// Spring MVC
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }
}

// Spring WebFlux (响应式)
@RestController
public class ReactiveUserController {
    
    @GetMapping("/users/{id}")
    public Mono<User> getUser(@PathVariable Long id) {
        return userService.findById(id);
    }
    
    @GetMapping("/users")
    public Flux<User> getAllUsers() {
        return userService.findAll();
    }
}
```

### 5. 其他重要模块

```
┌─────────────────────────────────────────────────────────────┐
│                     其他重要模块                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  spring-test                                        │  │
│   │  • @SpringBootTest                                   │  │
│   │  • MockMvc 测试 Web 层                               │  │
│   │  • @MockBean, @SpyBean                               │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  spring-messaging                                   │  │
│   │  • 消息抽象                                          │  │
│   │  • STOMP 消息处理                                    │  │
│   │  • 与 RabbitMQ、Kafka 集成                           │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  spring-context-support                             │  │
│   │  • 缓存抽象 (@Cacheable)                             │  │
│   │  • 任务调度 (@Scheduled)                             │  │
│   │  • 邮件发送 (JavaMailSender)                         │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## Spring Boot 常用 Starter

```
┌─────────────────────────────────────────────────────────────┐
│                  Spring Boot Starter                         │
├──────────────────────────────┬──────────────────────────────┤
│   Starter                     │   功能                       │
├──────────────────────────────┼──────────────────────────────┤
│   spring-boot-starter-web    │   Web 开发，内嵌 Tomcat      │
│   spring-boot-starter-data-jpa│   JPA + Hibernate           │
│   spring-boot-starter-data-redis│ Redis 客户端              │
│   spring-boot-starter-security│  Spring Security           │
│   spring-boot-starter-actuator│  监控端点                   │
│   spring-boot-starter-test   │   测试套件                   │
│   spring-boot-starter-validation│ 参数校验                  │
│   spring-boot-starter-cache  │   缓存抽象                   │
│   spring-boot-starter-amqp   │   RabbitMQ                   │
│   spring-boot-starter-webflux│   响应式 Web                 │
└──────────────────────────────┴──────────────────────────────┘
```

## 核心源码包

```java
// 重要的包和类
org.springframework.beans      // Bean 工厂、定义、后置处理器
org.springframework.context    // 应用上下文、事件、国际化
org.springframework.aop        // AOP 实现
org.springframework.tx         // 事务管理
org.springframework.web        // Web 核心
org.springframework.jdbc       // JDBC 模板

// 关键类
BeanFactory                    // IoC 容器基础接口
ApplicationContext             // 高级容器
BeanPostProcessor              // Bean 后置处理器
BeanFactoryPostProcessor       // BeanFactory 后置处理器
DispatcherServlet              // 前端控制器
HandlerMapping                 // URL 映射
ViewResolver                   // 视图解析
PlatformTransactionManager     // 事务管理器
```

## 面试回答

### 30秒版本

> Spring 核心模块包括：**Core Container**（Beans、Core、Context、SpEL）负责 IoC/DI；**AOP** 提供切面编程；**Data Access**（JDBC、TX、ORM）处理数据访问和事务；**Web**（MVC、WebFlux）支持 Web 开发；**Test** 支持测试。其中 Context 是最核心的，提供 ApplicationContext 容器。

### 1分钟版本

> Spring 主要模块：
>
> **1. Core Container（核心）**：
> - **spring-beans**：BeanFactory，Bean 生命周期管理
> - **spring-core**：工具类、资源访问、类型转换
> - **spring-context**：ApplicationContext，注解驱动，事件机制
> - **spring-expression (SpEL)**：表达式语言
>
> **2. AOP**：spring-aop（代理实现）、spring-aspects（AspectJ 集成）
>
> **3. Data Access**：
> - spring-jdbc：JdbcTemplate
> - spring-tx：声明式事务 @Transactional
> - spring-orm：JPA/Hibernate 集成
>
> **4. Web**：
> - spring-webmvc：传统 MVC（DispatcherServlet）
> - spring-webflux：响应式（Mono/Flux）
>
> **5. Test**：单元测试、集成测试支持
>
> **核心类**：ApplicationContext、BeanFactory、DispatcherServlet、PlatformTransactionManager

---

*关联文档：[spring-ioc.md](spring-ioc.md) | [spring-aop.md](spring-aop.md) | [spring-circular-dependency.md](spring-circular-dependency.md)*
