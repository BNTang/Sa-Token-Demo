# Java 中的注解原理是什么？

## 注解概述

```
┌─────────────────────────────────────────────────────────────┐
│                    Java 注解 (Annotation)                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   定义: 一种元数据，用于为代码添加额外信息                  │
│                                                             │
│   作用:                                                      │
│   1. 编译检查: @Override、@Deprecated                       │
│   2. 编译时处理: Lombok、MapStruct                          │
│   3. 运行时处理: Spring @Autowired、@Transactional          │
│                                                             │
│   本质: 注解是一个继承了 Annotation 接口的特殊接口          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 元注解

```java
// 元注解: 用于定义注解的注解

@Target(ElementType.METHOD)      // 注解可以用在哪里
@Retention(RetentionPolicy.RUNTIME)  // 注解保留到什么时候
@Documented                      // 是否包含在 JavaDoc 中
@Inherited                       // 是否可被子类继承
public @interface MyAnnotation {
    String value() default "";
}
```

```
┌─────────────────────────────────────────────────────────────┐
│                    元注解详解                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   @Target - 注解可以应用的位置                              │
│   ├── TYPE          类、接口、枚举                          │
│   ├── FIELD         字段                                    │
│   ├── METHOD        方法                                    │
│   ├── PARAMETER     方法参数                                │
│   ├── CONSTRUCTOR   构造方法                                │
│   ├── LOCAL_VARIABLE 局部变量                               │
│   ├── ANNOTATION_TYPE 注解类型                              │
│   └── PACKAGE       包                                      │
│                                                             │
│   @Retention - 注解的生命周期                               │
│   ├── SOURCE        只在源码中，编译时丢弃                  │
│   │                 例: @Override、@SuppressWarnings        │
│   ├── CLASS         保留到字节码，运行时丢弃 (默认)         │
│   │                 例: @Deprecated (早期)                  │
│   └── RUNTIME       保留到运行时，可通过反射获取            │
│                     例: @Autowired、@Transactional          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 自定义注解

```java
// 定义注解
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Log {
    String value() default "";           // 注解属性
    boolean printArgs() default true;    // 是否打印参数
    LogLevel level() default LogLevel.INFO;  // 枚举类型
}

// 使用注解
@Log(value = "用户服务", printArgs = false)
public class UserService {
    
    @Log("保存用户")
    public void save(User user) {
        // ...
    }
}
```

## 注解原理

```
┌─────────────────────────────────────────────────────────────┐
│                    注解的本质                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   编译后，注解变成一个接口:                                  │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  // @Log 编译后的样子                                │  │
│   │  public interface Log extends Annotation {           │  │
│   │      String value();                                 │  │
│   │      boolean printArgs();                            │  │
│   │      LogLevel level();                               │  │
│   │  }                                                   │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   运行时获取注解:                                            │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  JVM 会动态生成注解的代理类                          │  │
│   │  代理类实现注解接口，从 AnnotationInvocationHandler  │  │
│   │  的 memberValues Map 中获取属性值                    │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 运行时处理注解

```java
// 通过反射获取并处理注解
public class AnnotationProcessor {
    
    public void process(Object obj) throws Exception {
        Class<?> clazz = obj.getClass();
        
        // 获取类上的注解
        if (clazz.isAnnotationPresent(Log.class)) {
            Log log = clazz.getAnnotation(Log.class);
            System.out.println("类注解: " + log.value());
        }
        
        // 获取方法上的注解
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Log.class)) {
                Log log = method.getAnnotation(Log.class);
                System.out.println("方法: " + method.getName());
                System.out.println("注解值: " + log.value());
                System.out.println("打印参数: " + log.printArgs());
            }
        }
    }
}

// AOP 中使用注解
@Aspect
@Component
public class LogAspect {
    
    @Around("@annotation(log)")  // 拦截带 @Log 的方法
    public Object around(ProceedingJoinPoint pjp, Log log) throws Throwable {
        System.out.println("日志: " + log.value());
        return pjp.proceed();
    }
}
```

## 编译时处理注解

```
┌─────────────────────────────────────────────────────────────┐
│                    编译时注解处理 (APT)                      │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   APT (Annotation Processing Tool):                         │
│   在编译期扫描和处理注解，生成额外的代码                    │
│                                                             │
│   典型应用:                                                  │
│   ├── Lombok: @Data、@Getter 生成代码                       │
│   ├── MapStruct: 生成对象映射代码                           │
│   ├── Dagger: 依赖注入代码生成                              │
│   └── AutoService: 生成 SPI 配置文件                        │
│                                                             │
│   实现方式:                                                  │
│   继承 AbstractProcessor，重写 process() 方法               │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

```java
// 编译时注解处理器示例
@SupportedAnnotationTypes("com.example.MyAnnotation")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class MyAnnotationProcessor extends AbstractProcessor {
    
    @Override
    public boolean process(Set<? extends TypeElement> annotations, 
                          RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(MyAnnotation.class)) {
            // 生成代码
            generateCode(element);
        }
        return true;
    }
}
```

## 面试回答

### 30秒版本

> 注解本质是继承 `Annotation` 接口的特殊接口。通过**元注解**定义：`@Target` 指定位置，`@Retention` 指定生命周期（SOURCE/CLASS/RUNTIME）。运行时注解通过**反射**获取和处理，JVM 动态生成代理类存储属性值。编译时注解通过 **APT** 处理，如 Lombok、MapStruct。

### 1分钟版本

> **注解本质**：
> - 继承 Annotation 接口的特殊接口
> - 编译后成为普通接口
>
> **元注解**：
> - @Target：可应用位置
> - @Retention：生命周期
>   - SOURCE：编译时丢弃
>   - CLASS：运行时丢弃
>   - RUNTIME：运行时可反射获取
>
> **处理方式**：
> - 运行时：反射获取 `getAnnotation()`
> - 编译时：APT (AbstractProcessor)
>
> **原理**：
> - JVM 生成注解代理类
> - 属性值存在 AnnotationInvocationHandler 的 Map 中
>
> **应用**：
> - Spring：@Autowired、@Transactional
> - Lombok：@Data、@Getter
> - 自定义：日志、权限、缓存等

---

*关联文档：[java-reflection.md](java-reflection.md) | [spring-aop.md](../02-spring/spring-aop.md)*
