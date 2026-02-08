# 什么是循环依赖？Spring 如何解决？

## 什么是循环依赖

**循环依赖 (Circular Dependency)** 是指两个或多个 Bean 相互依赖，形成闭环，导致无法确定初始化顺序。

```
┌─────────────────────────────────────────────────────────────┐
│                    循环依赖示意图                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   两个 Bean 互相依赖:                                        │
│   ┌─────────┐           ┌─────────┐                        │
│   │ Bean A  │ ───────→  │ Bean B  │                        │
│   │         │ ←───────  │         │                        │
│   └─────────┘           └─────────┘                        │
│                                                             │
│   三个 Bean 循环依赖:                                        │
│   ┌─────────┐           ┌─────────┐                        │
│   │ Bean A  │ ────────→ │ Bean B  │                        │
│   └─────────┘           └─────────┘                        │
│        ↑                      │                            │
│        │                      ↓                            │
│        │               ┌─────────┐                         │
│        └───────────────│ Bean C  │                         │
│                        └─────────┘                         │
│                                                             │
│   问题：创建 A 需要 B，创建 B 需要 A，死循环！                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 代码示例

```java
// 循环依赖示例
@Service
public class ServiceA {
    @Autowired
    private ServiceB serviceB;  // A 依赖 B
    
    public void doSomething() {
        serviceB.doWork();
    }
}

@Service
public class ServiceB {
    @Autowired
    private ServiceA serviceA;  // B 依赖 A
    
    public void doWork() {
        serviceA.doSomething();
    }
}

// 创建流程:
// 1. 创建 A → 发现需要 B
// 2. 创建 B → 发现需要 A
// 3. 创建 A → 需要 B ... (死循环)
```

## Spring 三级缓存解决方案

```
┌─────────────────────────────────────────────────────────────┐
│                    Spring 三级缓存                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  一级缓存 (singletonObjects)                         │  │
│   │  ├── 存放: 完全初始化好的单例 Bean                    │  │
│   │  └── 状态: 可以直接使用                               │  │
│   └─────────────────────────────────────────────────────┘  │
│                           ↑                                 │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  二级缓存 (earlySingletonObjects)                    │  │
│   │  ├── 存放: 提前曝光的 Bean (可能是代理对象)           │  │
│   │  └── 状态: 未完成属性填充                             │  │
│   └─────────────────────────────────────────────────────┘  │
│                           ↑                                 │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  三级缓存 (singletonFactories)                       │  │
│   │  ├── 存放: Bean 的 ObjectFactory (工厂对象)          │  │
│   │  └── 作用: 解决 AOP 代理问题                         │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 三级缓存源码

```java
// DefaultSingletonBeanRegistry.java
public class DefaultSingletonBeanRegistry {
    
    // 一级缓存：完整的单例 Bean
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);
    
    // 二级缓存：提前曝光的 Bean (半成品)
    private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>(16);
    
    // 三级缓存：Bean 工厂
    private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16);
    
    // 获取单例 Bean
    protected Object getSingleton(String beanName, boolean allowEarlyReference) {
        // 1. 先从一级缓存获取
        Object singletonObject = this.singletonObjects.get(beanName);
        
        if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
            // 2. 从二级缓存获取
            singletonObject = this.earlySingletonObjects.get(beanName);
            
            if (singletonObject == null && allowEarlyReference) {
                synchronized (this.singletonObjects) {
                    // 双重检查
                    singletonObject = this.singletonObjects.get(beanName);
                    if (singletonObject == null) {
                        singletonObject = this.earlySingletonObjects.get(beanName);
                        if (singletonObject == null) {
                            // 3. 从三级缓存获取工厂，创建早期对象
                            ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
                            if (singletonFactory != null) {
                                singletonObject = singletonFactory.getObject();
                                // 升级到二级缓存
                                this.earlySingletonObjects.put(beanName, singletonObject);
                                // 移除三级缓存
                                this.singletonFactories.remove(beanName);
                            }
                        }
                    }
                }
            }
        }
        return singletonObject;
    }
}
```

## 解决循环依赖的流程

```
┌─────────────────────────────────────────────────────────────┐
│                解决循环依赖流程 (A 依赖 B，B 依赖 A)          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   步骤 1: 创建 A                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  1.1 实例化 A (new ServiceA())                       │  │
│   │  1.2 将 A 的工厂放入三级缓存                          │  │
│   │      singletonFactories.put("A", () -> getEarly(A))  │  │
│   │  1.3 填充属性，发现需要 B                             │  │
│   └─────────────────────────────────────────────────────┘  │
│                           │                                 │
│                           ▼                                 │
│   步骤 2: 创建 B                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  2.1 实例化 B (new ServiceB())                       │  │
│   │  2.2 将 B 的工厂放入三级缓存                          │  │
│   │  2.3 填充属性，发现需要 A                             │  │
│   │  2.4 从三级缓存获取 A 的工厂，执行获取早期 A          │  │
│   │  2.5 将早期 A 放入二级缓存                            │  │
│   │  2.6 B 持有 A 的引用（虽然 A 未完成初始化）           │  │
│   │  2.7 B 初始化完成，放入一级缓存                       │  │
│   └─────────────────────────────────────────────────────┘  │
│                           │                                 │
│                           ▼                                 │
│   步骤 3: 继续创建 A                                         │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  3.1 从一级缓存获取 B (已完成)                        │  │
│   │  3.2 A 持有 B 的引用                                 │  │
│   │  3.3 A 初始化完成，放入一级缓存                       │  │
│   │  3.4 清理二级、三级缓存                               │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   结果: A 和 B 都创建成功，互相持有对方引用                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 为什么需要三级缓存？

```
┌─────────────────────────────────────────────────────────────┐
│                  三级缓存的必要性                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   问题：如果 A 是 AOP 代理对象怎么办？                        │
│                                                             │
│   两级缓存的问题:                                            │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  创建 A → 放入二级缓存 (原始 A)                       │  │
│   │  创建 B → 获取二级缓存的 A (原始 A)                   │  │
│   │  A 初始化完成 → 生成代理 A'                          │  │
│   │                                                      │  │
│   │  问题: B 持有的是原始 A，不是代理 A'                  │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   三级缓存解决:                                              │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  三级缓存存的是 ObjectFactory                        │  │
│   │  getEarlyBeanReference() 判断是否需要生成代理        │  │
│   │                                                      │  │
│   │  如果需要代理:                                        │  │
│   │    工厂会提前创建代理对象，放入二级缓存                │  │
│   │  如果不需要代理:                                      │  │
│   │    工厂直接返回原始对象                               │  │
│   │                                                      │  │
│   │  结果: B 获取到的一定是最终对象 (代理或原始)          │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

```java
// AbstractAutowireCapableBeanFactory.java
protected Object getEarlyBeanReference(String beanName, RootBeanDefinition mbd, Object bean) {
    Object exposedObject = bean;
    // 如果有 AOP，这里会创建代理对象
    for (BeanPostProcessor bp : getBeanPostProcessors()) {
        if (bp instanceof SmartInstantiationAwareBeanPostProcessor) {
            SmartInstantiationAwareBeanPostProcessor ibp = 
                (SmartInstantiationAwareBeanPostProcessor) bp;
            exposedObject = ibp.getEarlyBeanReference(exposedObject, beanName);
        }
    }
    return exposedObject;
}
```

## 无法解决的循环依赖

```java
/**
 * 1. 构造器注入 - 无法解决
 */
@Service
public class ServiceA {
    private final ServiceB serviceB;
    
    @Autowired
    public ServiceA(ServiceB serviceB) {  // 构造器注入
        this.serviceB = serviceB;
    }
}

@Service
public class ServiceB {
    private final ServiceA serviceA;
    
    @Autowired
    public ServiceB(ServiceA serviceA) {
        this.serviceA = serviceA;
    }
}

// 原因：实例化时就需要依赖，无法提前曝光
// 解决：改用 @Lazy 延迟加载
@Autowired
public ServiceA(@Lazy ServiceB serviceB) {
    this.serviceB = serviceB;
}

/**
 * 2. prototype 作用域 - 无法解决
 */
@Scope("prototype")
@Service
public class PrototypeA {
    @Autowired
    private PrototypeB prototypeB;
}

// 原因：每次都创建新实例，不放入缓存
// 解决：改用单例或 @Lazy

/**
 * 3. @DependsOn 导致的循环 - 无法解决
 */
@DependsOn("serviceB")
@Service
public class ServiceA { ... }

@DependsOn("serviceA")
@Service
public class ServiceB { ... }

// 原因：明确指定依赖顺序，形成死循环
```

## Spring Boot 2.6+ 的变化

```yaml
# Spring Boot 2.6 默认禁止循环依赖
spring:
  main:
    allow-circular-references: true  # 需要手动开启

# 推荐：重构代码，消除循环依赖
```

## 解决循环依赖的最佳实践

```java
/**
 * 方案1：@Lazy 延迟加载
 */
@Service
public class ServiceA {
    @Autowired
    @Lazy  // 延迟加载 B
    private ServiceB serviceB;
}

/**
 * 方案2：Setter 注入替代构造器注入
 */
@Service
public class ServiceA {
    private ServiceB serviceB;
    
    @Autowired
    public void setServiceB(ServiceB serviceB) {
        this.serviceB = serviceB;
    }
}

/**
 * 方案3：重构代码，消除循环依赖（推荐）
 */
// 提取公共逻辑到第三个服务
@Service
public class CommonService {
    // 公共逻辑
}

@Service
public class ServiceA {
    @Autowired
    private CommonService commonService;
}

@Service
public class ServiceB {
    @Autowired
    private CommonService commonService;
}
```

## 面试回答

### 30秒版本

> 循环依赖是指两个 Bean 相互依赖。Spring 通过**三级缓存**解决：一级缓存（完整 Bean）、二级缓存（早期 Bean）、三级缓存（Bean 工厂）。创建时先将工厂放入三级缓存，填充属性时从缓存获取早期对象打破循环。**构造器注入**的循环依赖无法解决。

### 1分钟版本

> **什么是循环依赖**：A 依赖 B，B 依赖 A，形成闭环。
>
> **Spring 解决方案**：三级缓存
> - 一级缓存（singletonObjects）：完整 Bean
> - 二级缓存（earlySingletonObjects）：提前曝光的 Bean
> - 三级缓存（singletonFactories）：Bean 工厂
>
> **解决流程**：
> 1. 实例化 A，将工厂放入三级缓存
> 2. 填充属性需要 B，去创建 B
> 3. B 从三级缓存获取 A 的工厂，得到早期 A，放入二级缓存
> 4. B 完成，返回继续创建 A
> 5. A 完成，放入一级缓存
>
> **为什么三级**：处理 AOP 代理，保证获取的是最终对象。
>
> **无法解决**：构造器注入、prototype 作用域。
>
> **推荐**：重构代码消除循环依赖，或使用 @Lazy。

---

*关联文档：[spring-ioc.md](spring-ioc.md) | [spring-aop.md](spring-aop.md) | [spring-bean-lifecycle.md](spring-bean-lifecycle.md)*
