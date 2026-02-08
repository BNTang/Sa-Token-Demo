# Spring AOP 动态代理

> 分类: Spring | 难度: ⭐⭐⭐ | 频率: 高频

---

## 一、Spring AOP 默认代理方式

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                    Spring AOP 代理选择规则                                        │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  Spring Boot 2.x 默认: CGLIB                                                     │
│  Spring Framework: 根据目标类是否实现接口决定                                     │
│                                                                                  │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │                                                                            │ │
│  │                     目标类实现了接口?                                       │ │
│  │                           │                                                │ │
│  │              Yes ─────────┼───────── No                                    │ │
│  │               ↓                       ↓                                    │ │
│  │         JDK 动态代理              CGLIB 代理                               │ │
│  │                                                                            │ │
│  │  Spring Boot 2.x 默认开启 proxyTargetClass=true                            │ │
│  │  所以默认都是 CGLIB                                                        │ │
│  │                                                                            │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  配置强制使用 CGLIB:                                                             │
│  spring.aop.proxy-target-class=true  (Spring Boot 2.x 默认值)                    │
│                                                                                  │
│  配置优先使用 JDK 代理:                                                          │
│  spring.aop.proxy-target-class=false                                             │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 二、JDK 动态代理

```java
/**
 * JDK 动态代理
 * 基于接口，目标类必须实现接口
 */
public interface UserService {
    void save(User user);
}

public class UserServiceImpl implements UserService {
    @Override
    public void save(User user) {
        System.out.println("保存用户: " + user.getName());
    }
}

/**
 * JDK 代理实现
 */
public class JdkProxyDemo {
    
    public static void main(String[] args) {
        UserService target = new UserServiceImpl();
        
        UserService proxy = (UserService) Proxy.newProxyInstance(
            target.getClass().getClassLoader(),
            target.getClass().getInterfaces(),  // 必须有接口
            new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) 
                        throws Throwable {
                    System.out.println("前置处理");
                    Object result = method.invoke(target, args);  // 调用目标方法
                    System.out.println("后置处理");
                    return result;
                }
            }
        );
        
        proxy.save(new User("张三"));
    }
}

// 原理:
// 1. 运行时动态生成代理类，实现目标接口
// 2. 代理类持有 InvocationHandler
// 3. 调用接口方法时，转发给 InvocationHandler.invoke()
// 4. 通过反射调用目标对象的方法
```

---

## 三、CGLIB 代理

```java
/**
 * CGLIB 代理
 * 基于继承，目标类无需实现接口
 */
public class OrderService {  // 无接口
    public void create(Order order) {
        System.out.println("创建订单: " + order.getId());
    }
}

/**
 * CGLIB 代理实现
 */
public class CglibProxyDemo {
    
    public static void main(String[] args) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(OrderService.class);  // 设置父类
        enhancer.setCallback(new MethodInterceptor() {
            @Override
            public Object intercept(Object obj, Method method, Object[] args, 
                    MethodProxy proxy) throws Throwable {
                System.out.println("前置处理");
                Object result = proxy.invokeSuper(obj, args);  // 调用父类方法
                System.out.println("后置处理");
                return result;
            }
        });
        
        OrderService proxy = (OrderService) enhancer.create();
        proxy.create(new Order("001"));
    }
}

// 原理:
// 1. 运行时生成目标类的子类
// 2. 重写父类方法，加入拦截逻辑
// 3. 通过 MethodProxy 调用父类原始方法
// 4. 不使用反射，性能更好
```

---

## 四、两种代理方式对比

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                    JDK 代理 vs CGLIB 代理                                         │
├───────────────────┬──────────────────────────┬───────────────────────────────────┤
│       特性         │       JDK 动态代理        │           CGLIB 代理              │
├───────────────────┼──────────────────────────┼───────────────────────────────────┤
│  实现方式          │  基于接口                 │  基于继承                          │
├───────────────────┼──────────────────────────┼───────────────────────────────────┤
│  目标类要求        │  必须实现接口             │  不能是 final 类                   │
├───────────────────┼──────────────────────────┼───────────────────────────────────┤
│  方法要求          │  只能代理接口方法         │  不能代理 final 方法               │
├───────────────────┼──────────────────────────┼───────────────────────────────────┤
│  调用方式          │  反射调用                 │  FastClass 机制，直接调用          │
├───────────────────┼──────────────────────────┼───────────────────────────────────┤
│  生成速度          │  快                       │  慢 (需生成子类)                   │
├───────────────────┼──────────────────────────┼───────────────────────────────────┤
│  执行速度          │  较慢 (反射开销)          │  较快 (无反射)                     │
├───────────────────┼──────────────────────────┼───────────────────────────────────┤
│  依赖              │  JDK 内置                 │  需要引入 cglib 库                 │
│                   │  java.lang.reflect.Proxy  │  Spring 已内置                     │
└───────────────────┴──────────────────────────┴───────────────────────────────────┘
```

---

## 五、Spring AOP 示例

```java
/**
 * Spring AOP 切面
 */
@Aspect
@Component
public class LogAspect {
    
    @Pointcut("execution(* com.example.service.*.*(..))")
    public void servicePointcut() {}
    
    @Around("servicePointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            return result;
        } finally {
            long cost = System.currentTimeMillis() - start;
            System.out.println(joinPoint.getSignature() + " 耗时: " + cost + "ms");
        }
    }
}

/**
 * 被代理的 Service
 */
@Service
public class UserService {  // Spring Boot 2.x 默认用 CGLIB 代理
    
    public User getById(Long id) {
        return userMapper.selectById(id);
    }
}

// 获取代理类型
@Autowired
private UserService userService;

System.out.println(AopUtils.isJdkDynamicProxy(userService));  // false
System.out.println(AopUtils.isCglibProxy(userService));       // true
```

---

## 六、面试回答

### 30秒版本

> **Spring Boot 2.x 默认使用 CGLIB 代理**。
>
> **JDK 动态代理**：
> - 基于接口，目标类必须实现接口
> - 运行时生成实现接口的代理类
> - 通过反射调用目标方法
>
> **CGLIB 代理**：
> - 基于继承，生成目标类的子类
> - 目标类不能是 final，方法不能是 final
> - 通过 FastClass 直接调用，性能更好

### 1分钟版本

> **Spring AOP 代理选择**：
> - Spring Boot 2.x 默认 `proxyTargetClass=true`，使用 CGLIB
> - Spring Framework 默认：有接口用 JDK，无接口用 CGLIB
>
> **JDK 动态代理**：
> - 基于接口，目标类必须实现接口
> - 使用 `Proxy.newProxyInstance()` 动态生成代理类
> - 代理类实现目标接口，持有 InvocationHandler
> - 调用方法时通过反射调用目标对象
> - 优点：JDK 内置，生成速度快
>
> **CGLIB 代理**：
> - 基于继承，生成目标类的子类
> - 使用 ASM 字节码框架动态生成
> - 目标类不能是 final，方法不能是 final
> - 使用 FastClass 机制，不用反射，执行更快
> - 缺点：生成子类速度较慢
>
> **性能对比**：
> - 生成速度：JDK > CGLIB
> - 执行速度：CGLIB > JDK（JDK 8 后差距缩小）
>
> **为什么 Spring Boot 2.x 默认 CGLIB**：
> - 避免开发者必须定义接口
> - CGLIB 执行性能更好
