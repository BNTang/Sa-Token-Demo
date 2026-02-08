# 什么是 Java 中的动态代理？

## 代理模式概述

```
┌─────────────────────────────────────────────────────────────┐
│                    代理模式                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   定义: 为其他对象提供一种代理以控制对这个对象的访问        │
│                                                             │
│   ┌──────────┐      ┌───────────┐      ┌──────────────┐    │
│   │  Client  │ ───→ │   Proxy   │ ───→ │ RealSubject  │    │
│   │  调用方  │      │   代理类   │      │  真实对象    │    │
│   └──────────┘      └───────────┘      └──────────────┘    │
│                           │                                 │
│                     增强功能:                               │
│                     • 日志记录                              │
│                     • 权限校验                              │
│                     • 事务管理                              │
│                     • 性能监控                              │
│                                                             │
│   静态代理: 编译时生成代理类                                │
│   动态代理: 运行时生成代理类                                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 静态代理

```java
// 接口
interface UserService {
    void save(User user);
}

// 真实实现
class UserServiceImpl implements UserService {
    public void save(User user) {
        System.out.println("保存用户: " + user);
    }
}

// 静态代理类 - 需要手动编写
class UserServiceProxy implements UserService {
    private UserService target;
    
    public UserServiceProxy(UserService target) {
        this.target = target;
    }
    
    public void save(User user) {
        System.out.println("开始事务");  // 前置增强
        target.save(user);               // 调用真实对象
        System.out.println("提交事务");  // 后置增强
    }
}

// 缺点: 每个接口都需要写一个代理类，代码冗余
```

## JDK 动态代理

```java
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

// InvocationHandler 实现
public class LoggingHandler implements InvocationHandler {
    private Object target;  // 被代理对象
    
    public LoggingHandler(Object target) {
        this.target = target;
    }
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) 
            throws Throwable {
        // 前置增强
        System.out.println("调用方法: " + method.getName());
        long start = System.currentTimeMillis();
        
        // 调用真实方法
        Object result = method.invoke(target, args);
        
        // 后置增强
        long end = System.currentTimeMillis();
        System.out.println("方法耗时: " + (end - start) + "ms");
        
        return result;
    }
}

// 创建代理对象
UserService realService = new UserServiceImpl();
UserService proxy = (UserService) Proxy.newProxyInstance(
    UserService.class.getClassLoader(),    // 类加载器
    new Class[]{UserService.class},        // 接口数组
    new LoggingHandler(realService)        // 调用处理器
);

// 使用代理
proxy.save(new User("Tom"));  // 自动添加日志
```

```
┌─────────────────────────────────────────────────────────────┐
│                    JDK 动态代理原理                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. Proxy.newProxyInstance() 动态生成代理类                │
│   2. 代理类实现指定接口                                     │
│   3. 代理类持有 InvocationHandler 引用                      │
│   4. 调用代理方法时，转发给 handler.invoke()                │
│                                                             │
│   生成的代理类结构 (简化):                                   │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  public class $Proxy0 implements UserService {       │  │
│   │      private InvocationHandler h;                    │  │
│   │                                                     │  │
│   │      public void save(User user) {                   │  │
│   │          h.invoke(this, saveMethod, new Object[]{user});│  │
│   │      }                                               │  │
│   │  }                                                   │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   限制: 只能代理接口，不能代理类                            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## CGLIB 动态代理

```java
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

// 被代理类 (无需接口)
public class UserServiceImpl {
    public void save(User user) {
        System.out.println("保存用户: " + user);
    }
}

// MethodInterceptor 实现
public class LoggingInterceptor implements MethodInterceptor {
    @Override
    public Object intercept(Object obj, Method method, Object[] args, 
            MethodProxy proxy) throws Throwable {
        System.out.println("调用方法: " + method.getName());
        
        // 调用父类方法 (原方法)
        Object result = proxy.invokeSuper(obj, args);
        
        System.out.println("方法执行完成");
        return result;
    }
}

// 创建代理对象
Enhancer enhancer = new Enhancer();
enhancer.setSuperclass(UserServiceImpl.class);  // 设置父类
enhancer.setCallback(new LoggingInterceptor()); // 设置回调
UserServiceImpl proxy = (UserServiceImpl) enhancer.create();

proxy.save(new User("Tom"));
```

```
┌─────────────────────────────────────────────────────────────┐
│                    CGLIB 原理                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   • 通过继承实现代理 (生成目标类的子类)                     │
│   • 使用 ASM 字节码操作框架                                 │
│   • 可以代理没有接口的类                                    │
│   • 不能代理 final 类和 final 方法                          │
│                                                             │
│   生成的代理类结构:                                          │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  public class UserServiceImpl$$EnhancerByCGLIB      │  │
│   │          extends UserServiceImpl {                   │  │
│   │                                                     │  │
│   │      private MethodInterceptor callback;             │  │
│   │                                                     │  │
│   │      @Override                                       │  │
│   │      public void save(User user) {                   │  │
│   │          callback.intercept(this, method, args, proxy);│  │
│   │      }                                               │  │
│   │  }                                                   │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 应用场景

```
┌─────────────────────────────────────────────────────────────┐
│                    动态代理应用场景                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. Spring AOP                                              │
│      └── 事务管理、日志、权限等切面                         │
│                                                             │
│   2. RPC 框架                                                │
│      └── Dubbo、gRPC 的服务调用代理                         │
│                                                             │
│   3. MyBatis Mapper                                          │
│      └── 接口方法映射到 SQL 执行                            │
│                                                             │
│   4. 延迟加载                                                │
│      └── Hibernate 懒加载代理                               │
│                                                             │
│   5. 监控埋点                                                │
│      └── 方法耗时统计                                       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> **动态代理**是在运行时动态生成代理类，无需手动编写。**JDK 动态代理**基于接口，通过 `Proxy.newProxyInstance()` + `InvocationHandler` 实现。**CGLIB** 通过继承实现，可代理没有接口的类。Spring AOP 默认有接口用 JDK 代理，无接口用 CGLIB。应用场景：AOP、RPC、MyBatis Mapper。

### 1分钟版本

> **静态代理 vs 动态代理**：
> - 静态：编译时生成，每个接口写一个
> - 动态：运行时生成，一套代码代理所有
>
> **JDK 动态代理**：
> - 基于接口
> - Proxy.newProxyInstance()
> - InvocationHandler.invoke()
> - 限制：只能代理接口
>
> **CGLIB 代理**：
> - 基于继承（生成子类）
> - MethodInterceptor
> - 可代理没有接口的类
> - 限制：不能代理 final 类/方法
>
> **Spring AOP 选择**：
> - 有接口：JDK 代理
> - 无接口：CGLIB
>
> **应用**：AOP、RPC、MyBatis、延迟加载

---

*关联文档：[jdk-vs-cglib-proxy.md](jdk-vs-cglib-proxy.md) | [spring-aop-proxy.md](../02-spring/spring-aop-proxy.md)*
