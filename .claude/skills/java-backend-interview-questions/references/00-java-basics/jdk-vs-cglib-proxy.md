# JDK 动态代理和 CGLIB 动态代理有什么区别？

## 核心区别

```
┌─────────────────────────────────────────────────────────────┐
│              JDK 动态代理 vs CGLIB 动态代理                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   JDK 动态代理:                                              │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  • 基于接口                                          │  │
│   │  • JDK 内置，无需引入依赖                            │  │
│   │  • 代理类实现目标接口                                │  │
│   │  • 只能代理有接口的类                                │  │
│   │  • InvocationHandler                                 │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   CGLIB 动态代理:                                            │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  • 基于继承                                          │  │
│   │  • 需要引入 cglib 依赖                               │  │
│   │  • 代理类继承目标类                                  │  │
│   │  • 可以代理没有接口的类                              │  │
│   │  • 不能代理 final 类和 final 方法                    │  │
│   │  • MethodInterceptor                                 │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 实现原理对比

```
┌─────────────────────────────────────────────────────────────┐
│                    实现原理                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   JDK 动态代理:                                              │
│   ┌──────────────────┐                                      │
│   │    Interface     │                                      │
│   │   UserService    │                                      │
│   └────────┬─────────┘                                      │
│            │ implements                                     │
│   ┌────────┴─────────┐     ┌─────────────────┐             │
│   │ UserServiceImpl  │     │   $Proxy0       │             │
│   │   (真实对象)     │     │   (代理类)      │             │
│   └──────────────────┘     │ implements接口  │             │
│                            │ 持有Handler引用 │             │
│                            └─────────────────┘             │
│                                                             │
│   CGLIB 动态代理:                                            │
│   ┌──────────────────┐                                      │
│   │ UserServiceImpl  │                                      │
│   │   (真实对象)     │                                      │
│   └────────┬─────────┘                                      │
│            │ extends                                        │
│   ┌────────┴─────────────────────────┐                     │
│   │ UserServiceImpl$$EnhancerByCGLIB │                     │
│   │   (代理类，继承真实类)           │                     │
│   │   重写父类方法                   │                     │
│   └──────────────────────────────────┘                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 代码对比

```java
// ================== JDK 动态代理 ==================
interface UserService {
    void save();
}

class UserServiceImpl implements UserService {
    public void save() { System.out.println("保存"); }
}

// 创建 JDK 代理
UserService proxy = (UserService) Proxy.newProxyInstance(
    UserService.class.getClassLoader(),
    new Class[]{UserService.class},
    (proxyObj, method, args) -> {
        System.out.println("前置");
        Object result = method.invoke(new UserServiceImpl(), args);
        System.out.println("后置");
        return result;
    }
);

// ================== CGLIB 动态代理 ==================
class UserServiceImpl {  // 无需接口
    public void save() { System.out.println("保存"); }
}

// 创建 CGLIB 代理
Enhancer enhancer = new Enhancer();
enhancer.setSuperclass(UserServiceImpl.class);
enhancer.setCallback((MethodInterceptor) (obj, method, args, proxy) -> {
    System.out.println("前置");
    Object result = proxy.invokeSuper(obj, args);  // 调用父类方法
    System.out.println("后置");
    return result;
});
UserServiceImpl proxy = (UserServiceImpl) enhancer.create();
```

## 详细对比表

```
┌─────────────────────────────────────────────────────────────┐
│                    详细对比                                  │
├──────────────────┬─────────────────┬────────────────────────┤
│   维度           │   JDK 动态代理  │   CGLIB 动态代理       │
├──────────────────┼─────────────────┼────────────────────────┤
│   实现方式       │ 基于接口        │ 基于继承               │
│   依赖           │ JDK 内置        │ 需要 cglib 库          │
│   代理对象要求   │ 必须有接口      │ 不能是 final 类        │
│   方法要求       │ 接口中的方法    │ 非 final 方法          │
│   核心类         │ Proxy           │ Enhancer               │
│   回调接口       │ InvocationHandler│ MethodInterceptor     │
│   调用方式       │ method.invoke() │ proxy.invokeSuper()    │
│   生成速度       │ 快              │ 慢（字节码生成）       │
│   执行速度       │ 反射调用，略慢  │ FastClass，较快        │
│   Spring 默认    │ 有接口时使用    │ 无接口时使用           │
└──────────────────┴─────────────────┴────────────────────────┘
```

## Spring 中的选择

```
┌─────────────────────────────────────────────────────────────┐
│                    Spring AOP 代理选择                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   Spring Boot 2.x 默认配置:                                  │
│   spring.aop.proxy-target-class=true  (默认使用 CGLIB)      │
│                                                             │
│   选择逻辑:                                                  │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  if (proxyTargetClass == true) {                     │  │
│   │      使用 CGLIB                                      │  │
│   │  } else if (目标类实现了接口) {                      │  │
│   │      使用 JDK 动态代理                               │  │
│   │  } else {                                            │  │
│   │      使用 CGLIB                                      │  │
│   │  }                                                   │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   强制使用 JDK 代理:                                         │
│   @EnableAspectJAutoProxy(proxyTargetClass = false)         │
│                                                             │
│   强制使用 CGLIB:                                            │
│   @EnableAspectJAutoProxy(proxyTargetClass = true)          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 性能对比

```
┌─────────────────────────────────────────────────────────────┐
│                    性能对比                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   创建代理对象:                                              │
│   • JDK: 较快 (反射生成)                                    │
│   • CGLIB: 较慢 (字节码生成)                                │
│                                                             │
│   方法调用:                                                  │
│   • JDK: 反射调用，每次都需要反射                           │
│   • CGLIB: FastClass 机制，直接调用，更快                   │
│                                                             │
│   结论:                                                      │
│   • 创建次数少、调用次数多: CGLIB 更优                      │
│   • 创建次数多、调用次数少: JDK 更优                        │
│   • 实际差距很小，不必过度关注                              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> **JDK 动态代理**基于接口，JDK 内置，使用 `InvocationHandler`，只能代理有接口的类。**CGLIB** 基于继承，通过 ASM 生成子类，可代理无接口的类，但不能代理 final 类/方法。Spring Boot 2.x 默认使用 CGLIB。性能上 CGLIB 方法调用更快（FastClass），JDK 创建代理更快。

### 1分钟版本

> **实现方式**：
> - JDK：基于接口（implements）
> - CGLIB：基于继承（extends）
>
> **依赖**：
> - JDK：内置，无需依赖
> - CGLIB：需要引入 cglib 库
>
> **限制**：
> - JDK：只能代理接口
> - CGLIB：不能代理 final 类/方法
>
> **核心类**：
> - JDK：Proxy + InvocationHandler
> - CGLIB：Enhancer + MethodInterceptor
>
> **Spring 选择**：
> - Spring Boot 2.x 默认 CGLIB
> - 可通过配置强制选择
>
> **性能**：
> - JDK 创建快，调用慢（反射）
> - CGLIB 创建慢，调用快（FastClass）

---

*关联文档：[dynamic-proxy.md](dynamic-proxy.md) | [spring-aop-proxy.md](../02-spring/spring-aop-proxy.md)*
