# 什么是 AOP？

## 概念解析

**AOP (Aspect-Oriented Programming)** 面向切面编程是一种编程范式，用于将**横切关注点**与业务逻辑分离。

```
┌─────────────────────────────────────────────────────────────┐
│                     横切关注点 (Cross-Cutting Concerns)       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│    日志记录     权限校验     事务管理     性能监控     异常处理  │
│       │          │           │           │           │      │
│       ▼          ▼           ▼           ▼           ▼      │
│   ┌──────────────────────────────────────────────────────┐  │
│   │                                                      │  │
│   │   OrderService   UserService   PaymentService  ...   │  │
│   │                                                      │  │
│   │              核心业务逻辑 (Core Business Logic)        │  │
│   │                                                      │  │
│   └──────────────────────────────────────────────────────┘  │
│                                                             │
│   AOP 将横切关注点抽取出来，统一管理，避免代码重复              │
└─────────────────────────────────────────────────────────────┘
```

## AOP 核心概念

```
┌─────────────────────────────────────────────────────────────┐
│                        AOP 术语表                            │
├──────────────┬──────────────────────────────────────────────┤
│   术语        │   说明                                       │
├──────────────┼──────────────────────────────────────────────┤
│   Aspect     │   切面 = 切点 + 通知，模块化的横切关注点         │
│   JoinPoint  │   连接点，程序执行的某个点（如方法调用）          │
│   Pointcut   │   切点，匹配连接点的表达式                      │
│   Advice     │   通知，在切点执行的动作                        │
│   Weaving    │   织入，将切面应用到目标对象的过程               │
│   Target     │   目标对象，被代理的原始对象                    │
│   Proxy      │   代理对象，AOP创建的增强对象                   │
└──────────────┴──────────────────────────────────────────────┘
```

## 五种通知类型

```
┌─────────────────────────────────────────────────────────────┐
│                     通知执行顺序                              │
│                                                             │
│   @Around (前半部分)                                         │
│        │                                                    │
│        ▼                                                    │
│   @Before ───────────────────┐                              │
│        │                     │                              │
│        ▼                     │                              │
│   ┌─────────────────────┐    │                              │
│   │   目标方法执行        │    │ 正常                         │
│   └─────────────────────┘    │                              │
│        │                     │                              │
│    成功 │ 异常               │                              │
│        ▼                     ▼                              │
│   @AfterReturning      @AfterThrowing                       │
│        │                     │                              │
│        └─────────┬───────────┘                              │
│                  ▼                                          │
│              @After (finally)                               │
│                  │                                          │
│                  ▼                                          │
│   @Around (后半部分)                                         │
└─────────────────────────────────────────────────────────────┘
```

## 代码示例

### 定义切面

```java
@Aspect
@Component
@Order(1)  // 多个切面时指定顺序
public class LoggingAspect {
    
    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);
    
    // ============ 切点定义 ============
    
    // 匹配 service 包下所有方法
    @Pointcut("execution(* com.example.service.*.*(..))")
    public void serviceLayer() {}
    
    // 匹配带有 @Transactional 注解的方法
    @Pointcut("@annotation(org.springframework.transaction.annotation.Transactional)")
    public void transactionalMethod() {}
    
    // 组合切点
    @Pointcut("serviceLayer() && transactionalMethod()")
    public void transactionalService() {}
    
    // ============ 通知定义 ============
    
    @Before("serviceLayer()")
    public void logBefore(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        log.info(">>> 调用方法: {}，参数: {}", methodName, Arrays.toString(args));
    }
    
    @AfterReturning(pointcut = "serviceLayer()", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Object result) {
        log.info("<<< 方法返回: {}，结果: {}", 
            joinPoint.getSignature().getName(), result);
    }
    
    @AfterThrowing(pointcut = "serviceLayer()", throwing = "ex")
    public void logAfterThrowing(JoinPoint joinPoint, Exception ex) {
        log.error("!!! 方法异常: {}，异常: {}", 
            joinPoint.getSignature().getName(), ex.getMessage());
    }
    
    @After("serviceLayer()")
    public void logAfter(JoinPoint joinPoint) {
        log.info("--- 方法结束: {}", joinPoint.getSignature().getName());
    }
    
    // 环绕通知 - 功能最强大
    @Around("serviceLayer()")
    public Object logAround(ProceedingJoinPoint pjp) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = pjp.getSignature().getName();
        
        try {
            log.info(">>> [Around] 方法开始: {}", methodName);
            Object result = pjp.proceed();  // 执行目标方法
            log.info("<<< [Around] 方法成功: {}", methodName);
            return result;
        } catch (Throwable ex) {
            log.error("!!! [Around] 方法异常: {}", ex.getMessage());
            throw ex;  // 重新抛出异常
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            log.info("--- [Around] 耗时: {}ms", duration);
        }
    }
}
```

### 切点表达式语法

```java
// execution 表达式结构
// execution(修饰符? 返回类型 类路径?方法名(参数) 异常?)

// 常用示例
@Pointcut("execution(public * *(..))")                    // 所有 public 方法
@Pointcut("execution(* set*(..))")                        // 所有 set 开头的方法
@Pointcut("execution(* com.example.service.*.*(..))")     // service 包下所有方法
@Pointcut("execution(* com.example.service..*.*(..))")    // service 包及子包
@Pointcut("execution(* com.example.service.UserService.*(..))") // 指定类

// within 表达式
@Pointcut("within(com.example.service.*)")                // service 包下的类
@Pointcut("within(com.example.service..*)")               // 包含子包

// @annotation 表达式
@Pointcut("@annotation(com.example.annotation.Log)")      // 带有 @Log 注解
@Pointcut("@within(org.springframework.stereotype.Service)") // 类上有 @Service

// args 表达式
@Pointcut("args(java.lang.String)")                       // 单个 String 参数
@Pointcut("args(java.lang.String, ..)")                   // 第一个参数是 String

// 组合表达式
@Pointcut("serviceLayer() && !transactionalMethod()")     // 与、非
@Pointcut("serviceLayer() || repositoryLayer()")          // 或
```

### 自定义注解 + AOP

```java
// 1. 定义注解
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int value() default 100;  // 每秒限制次数
    String key() default "";   // 限流 key
}

// 2. 定义切面
@Aspect
@Component
public class RateLimitAspect {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        String key = "rate_limit:" + (rateLimit.key().isEmpty() 
            ? pjp.getSignature().toShortString() 
            : rateLimit.key());
        
        Long count = redisTemplate.opsForValue().increment(key, 1);
        if (count == 1) {
            redisTemplate.expire(key, 1, TimeUnit.SECONDS);
        }
        
        if (count > rateLimit.value()) {
            throw new RuntimeException("请求过于频繁，请稍后重试");
        }
        
        return pjp.proceed();
    }
}

// 3. 使用注解
@Service
public class OrderService {
    
    @RateLimit(value = 10, key = "createOrder")
    public Order createOrder(OrderRequest request) {
        // 业务逻辑
    }
}
```

## AOP 实现原理

```
┌─────────────────────────────────────────────────────────────┐
│                    Spring AOP 实现方式                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ┌─────────────────────┐    ┌─────────────────────────┐   │
│   │   JDK 动态代理       │    │     CGLIB 代理          │   │
│   ├─────────────────────┤    ├─────────────────────────┤   │
│   │ • 基于接口            │    │ • 基于继承              │   │
│   │ • 只能代理接口方法     │    │ • 可代理普通类          │   │
│   │ • 通过反射调用        │    │ • 生成子类字节码        │   │
│   │ • 性能稍低           │    │ • 性能较好              │   │
│   └─────────────────────┘    └─────────────────────────┘   │
│                                                             │
│   Spring Boot 2.x 默认使用 CGLIB                            │
│   配置: spring.aop.proxy-target-class=true (CGLIB)          │
│         spring.aop.proxy-target-class=false (JDK)           │
└─────────────────────────────────────────────────────────────┘
```

## AOP 常见应用场景

| 场景 | 说明 | 示例 |
|------|------|------|
| **日志记录** | 方法调用前后记录日志 | 操作日志、访问日志 |
| **性能监控** | 统计方法执行时间 | 慢查询监控、接口耗时 |
| **事务管理** | @Transactional 实现 | 声明式事务 |
| **权限校验** | 方法调用前检查权限 | @PreAuthorize |
| **缓存处理** | @Cacheable 实现 | 方法结果缓存 |
| **异常处理** | 统一异常捕获处理 | @ControllerAdvice |
| **限流熔断** | 接口调用限制 | 自定义 @RateLimit |
| **数据校验** | 参数合法性检查 | @Validated |

## 最佳实践

### ✅ 推荐做法

```java
// 1. 切面类加 @Order 控制执行顺序
@Aspect
@Component
@Order(1)  // 数字越小优先级越高
public class SecurityAspect { }

// 2. 使用组合切点，提高可维护性
@Pointcut("execution(* com.example.service.*.*(..))")
public void serviceLayer() {}

@Pointcut("@annotation(com.example.annotation.Secured)")
public void securedMethod() {}

@Before("serviceLayer() && securedMethod()")
public void checkSecurity() { }

// 3. Around 通知中正确处理异常
@Around("serviceLayer()")
public Object around(ProceedingJoinPoint pjp) throws Throwable {
    try {
        return pjp.proceed();
    } catch (Throwable ex) {
        // 处理后必须重新抛出，否则会吞掉异常
        throw ex;
    }
}

// 4. 避免在切面中做重逻辑，保持轻量
```

### ❌ 避免做法

```java
// 1. 避免切点范围过大
@Pointcut("execution(* *.*(..))")  // 匹配所有方法，性能差

// 2. 避免在切面中修改方法参数
@Before("serviceLayer()")
public void before(JoinPoint jp) {
    Object[] args = jp.getArgs();
    args[0] = "modified";  // 不推荐，可能导致意外行为
}

// 3. 避免 AOP 失效的情况
@Service
public class UserService {
    public void methodA() {
        this.methodB();  // 内部调用，AOP 失效！
    }
    
    @Transactional
    public void methodB() { }
}
```

## 面试回答

### 30秒版本

> AOP（面向切面编程）是将**日志、事务、权限**等横切关注点从业务逻辑中分离出来，通过**切面**统一管理。核心概念包括：切点（匹配哪些方法）、通知（执行什么逻辑）、织入（应用到目标对象）。Spring AOP 基于**动态代理**实现，JDK 代理接口，CGLIB 代理类。

### 1分钟版本

> AOP 面向切面编程，用于将**横切关注点**与业务逻辑分离。
>
> **核心概念**：
> - **Aspect**：切面，模块化的横切关注点
> - **Pointcut**：切点，匹配连接点的表达式
> - **Advice**：通知，Before/After/Around 等
>
> **五种通知**：@Before、@After、@AfterReturning、@AfterThrowing、@Around（最强大）
>
> **实现原理**：Spring AOP 基于动态代理，有接口用 JDK 代理，无接口用 CGLIB（Spring Boot 2.x 默认）。
>
> **应用场景**：日志记录、事务管理（@Transactional）、权限校验、性能监控、缓存（@Cacheable）、限流熔断。
>
> **注意事项**：同类内部调用会导致 AOP 失效，需通过代理对象调用或使用 `AopContext.currentProxy()`。

---

*关联文档：[spring-aop-proxy.md](spring-aop-proxy.md) | [spring-ioc.md](spring-ioc.md) | [spring-modules.md](spring-modules.md)*
