# 说下 Spring Bean 的生命周期？

## 生命周期概述

```
┌─────────────────────────────────────────────────────────────┐
│                    Bean 生命周期全流程                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 实例化 (Instantiation)                                 │
│      ↓                                                      │
│   2. 属性填充 (Populate Properties)                         │
│      ↓                                                      │
│   3. Aware 接口回调                                         │
│      ↓                                                      │
│   4. BeanPostProcessor 前置处理                             │
│      ↓                                                      │
│   5. 初始化 (Initialization)                                │
│      ↓                                                      │
│   6. BeanPostProcessor 后置处理                             │
│      ↓                                                      │
│   7. 使用中...                                              │
│      ↓                                                      │
│   8. 销毁 (Destruction)                                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 详细流程

```
┌─────────────────────────────────────────────────────────────┐
│                    Bean 生命周期详解                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  1. 实例化 Bean                                      │  │
│   │     - 通过构造函数或工厂方法创建 Bean 实例           │  │
│   │     - 此时只是一个空对象                             │  │
│   └─────────────────────────────────────────────────────┘  │
│                         ↓                                   │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  2. 属性填充 (依赖注入)                              │  │
│   │     - @Autowired、@Value 注入                        │  │
│   │     - setter 方法注入                                │  │
│   │     - 解决循环依赖 (三级缓存)                        │  │
│   └─────────────────────────────────────────────────────┘  │
│                         ↓                                   │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  3. Aware 接口回调                                   │  │
│   │     - BeanNameAware.setBeanName()                    │  │
│   │     - BeanFactoryAware.setBeanFactory()              │  │
│   │     - ApplicationContextAware.setApplicationContext()│  │
│   └─────────────────────────────────────────────────────┘  │
│                         ↓                                   │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  4. BeanPostProcessor.postProcessBeforeInitialization│  │
│   │     - 所有 Bean 都会经过                             │  │
│   │     - @PostConstruct 在这里处理                      │  │
│   └─────────────────────────────────────────────────────┘  │
│                         ↓                                   │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  5. 初始化                                           │  │
│   │     - InitializingBean.afterPropertiesSet()          │  │
│   │     - 自定义 init-method                             │  │
│   └─────────────────────────────────────────────────────┘  │
│                         ↓                                   │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  6. BeanPostProcessor.postProcessAfterInitialization │  │
│   │     - AOP 代理在这里创建                             │  │
│   │     - 返回的可能是代理对象                           │  │
│   └─────────────────────────────────────────────────────┘  │
│                         ↓                                   │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  7. Bean 就绪，可以使用                              │  │
│   └─────────────────────────────────────────────────────┘  │
│                         ↓                                   │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  8. 销毁                                             │  │
│   │     - @PreDestroy                                    │  │
│   │     - DisposableBean.destroy()                       │  │
│   │     - 自定义 destroy-method                          │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 代码示例

```java
@Component
public class LifecycleBean implements BeanNameAware, BeanFactoryAware, 
        ApplicationContextAware, InitializingBean, DisposableBean {
    
    private String beanName;
    
    // 1. 实例化 - 构造函数
    public LifecycleBean() {
        System.out.println("1. 构造函数 - 实例化");
    }
    
    // 2. 属性填充
    @Autowired
    public void setDependency(SomeDependency dependency) {
        System.out.println("2. 属性填充 - 依赖注入");
    }
    
    // 3. Aware 接口回调
    @Override
    public void setBeanName(String name) {
        this.beanName = name;
        System.out.println("3.1 BeanNameAware.setBeanName: " + name);
    }
    
    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        System.out.println("3.2 BeanFactoryAware.setBeanFactory");
    }
    
    @Override
    public void setApplicationContext(ApplicationContext ctx) {
        System.out.println("3.3 ApplicationContextAware.setApplicationContext");
    }
    
    // 4. BeanPostProcessor 前置处理 (在外部类中)
    
    // 5.1 @PostConstruct
    @PostConstruct
    public void postConstruct() {
        System.out.println("5.1 @PostConstruct");
    }
    
    // 5.2 InitializingBean.afterPropertiesSet
    @Override
    public void afterPropertiesSet() {
        System.out.println("5.2 InitializingBean.afterPropertiesSet");
    }
    
    // 5.3 自定义 init-method (在 @Bean 中指定)
    public void customInit() {
        System.out.println("5.3 自定义 init-method");
    }
    
    // 6. BeanPostProcessor 后置处理 (在外部类中)
    // AOP 代理在这里创建
    
    // 7. 使用中...
    
    // 8.1 @PreDestroy
    @PreDestroy
    public void preDestroy() {
        System.out.println("8.1 @PreDestroy");
    }
    
    // 8.2 DisposableBean.destroy
    @Override
    public void destroy() {
        System.out.println("8.2 DisposableBean.destroy");
    }
    
    // 8.3 自定义 destroy-method (在 @Bean 中指定)
    public void customDestroy() {
        System.out.println("8.3 自定义 destroy-method");
    }
}
```

```java
/**
 * BeanPostProcessor 示例
 */
@Component
public class CustomBeanPostProcessor implements BeanPostProcessor {
    
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        System.out.println("4. BeanPostProcessor.postProcessBeforeInitialization: " + beanName);
        return bean;
    }
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        System.out.println("6. BeanPostProcessor.postProcessAfterInitialization: " + beanName);
        // AOP 代理在这里创建，返回的可能是代理对象
        return bean;
    }
}
```

## 初始化方法执行顺序

```
┌─────────────────────────────────────────────────────────────┐
│                    初始化方法顺序                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   执行顺序:                                                  │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  1. @PostConstruct                                   │  │
│   │     ↓                                                │  │
│   │  2. InitializingBean.afterPropertiesSet()            │  │
│   │     ↓                                                │  │
│   │  3. init-method (自定义)                             │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   销毁顺序:                                                  │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  1. @PreDestroy                                      │  │
│   │     ↓                                                │  │
│   │  2. DisposableBean.destroy()                         │  │
│   │     ↓                                                │  │
│   │  3. destroy-method (自定义)                          │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   推荐使用: @PostConstruct / @PreDestroy (简洁、通用)       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 常见面试追问

### BeanPostProcessor 的作用？

```
┌─────────────────────────────────────────────────────────────┐
│                    BeanPostProcessor                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   作用: 在 Bean 初始化前后进行增强处理                       │
│                                                             │
│   应用场景:                                                  │
│   • AOP 代理创建 (AbstractAutoProxyCreator)                 │
│   • @Autowired 注入 (AutowiredAnnotationBeanPostProcessor)  │
│   • @Value 解析 (AutowiredAnnotationBeanPostProcessor)      │
│   • @PostConstruct 处理 (CommonAnnotationBeanPostProcessor) │
│                                                             │
│   注意: 所有 Bean 都会经过所有 BeanPostProcessor            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Aware 接口的作用？

```
┌─────────────────────────────────────────────────────────────┐
│                    Aware 接口                               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   作用: 让 Bean 获取 Spring 容器的资源                       │
│                                                             │
│   常用 Aware 接口:                                           │
│   • BeanNameAware - 获取 Bean 名称                          │
│   • BeanFactoryAware - 获取 BeanFactory                     │
│   • ApplicationContextAware - 获取 ApplicationContext       │
│   • EnvironmentAware - 获取 Environment                     │
│   • ResourceLoaderAware - 获取资源加载器                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> Bean 生命周期：1）**实例化**（构造函数）；2）**属性填充**（依赖注入）；3）**Aware回调**（注入容器资源）；4）**BeanPostProcessor前置处理**；5）**初始化**（@PostConstruct → afterPropertiesSet → init-method）；6）**BeanPostProcessor后置处理**（AOP代理在这里创建）；7）**使用**；8）**销毁**（@PreDestroy → destroy → destroy-method）。

### 1分钟版本

> **Bean 生命周期四大阶段**：
>
> 1. **实例化**：构造函数创建对象
>
> 2. **属性填充**：@Autowired、@Value 注入依赖
>
> 3. **初始化**：
>    - Aware 接口回调（获取容器资源）
>    - BeanPostProcessor.postProcessBeforeInitialization
>    - @PostConstruct
>    - InitializingBean.afterPropertiesSet()
>    - 自定义 init-method
>    - BeanPostProcessor.postProcessAfterInitialization（AOP代理创建）
>
> 4. **销毁**：
>    - @PreDestroy
>    - DisposableBean.destroy()
>    - 自定义 destroy-method
>
> **关键点**：
> - BeanPostProcessor 对所有 Bean 生效
> - AOP 代理在后置处理器中创建
> - 推荐使用 @PostConstruct/@PreDestroy（简洁）

---

*关联文档：[spring-ioc.md](spring-ioc.md) | [spring-circular-dependency.md](spring-circular-dependency.md)*
