# 为什么 Spring 循环依赖需要三级缓存，二级不够吗？

## 回顾三级缓存

```
┌─────────────────────────────────────────────────────────────┐
│                    Spring 三级缓存                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   一级缓存 (singletonObjects)                               │
│   └── 存放完全初始化好的 Bean                               │
│                                                             │
│   二级缓存 (earlySingletonObjects)                          │
│   └── 存放提前暴露的 Bean (未完成属性注入)                  │
│                                                             │
│   三级缓存 (singletonFactories)                             │
│   └── 存放 Bean 工厂 (ObjectFactory)                        │
│       用于生成早期 Bean 引用                                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 核心问题：为什么需要三级缓存？

```
┌─────────────────────────────────────────────────────────────┐
│                    核心答案                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   三级缓存的关键作用：解决 AOP 代理对象的循环依赖           │
│                                                             │
│   如果只用二级缓存：                                         │
│   • 简单对象的循环依赖可以解决 ✓                            │
│   • 但 AOP 代理对象会有问题 ✗                               │
│                                                             │
│   问题在于：                                                 │
│   代理对象应该在什么时候创建？                               │
│   • 正常情况：Bean 初始化完成后创建代理                     │
│   • 循环依赖：需要提前创建代理                               │
│                                                             │
│   三级缓存（ObjectFactory）延迟决定：                        │
│   • 被循环依赖时 → 提前创建代理                             │
│   • 没有循环依赖 → 正常时机创建代理                         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 场景分析：没有 AOP

```
┌─────────────────────────────────────────────────────────────┐
│                    无 AOP 场景（二级缓存足够）               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   A 依赖 B，B 依赖 A（都没有 AOP）                           │
│                                                             │
│   二级缓存方案：                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  1. 创建 A 实例 → 放入二级缓存 (A原始对象)           │  │
│   │  2. A 填充属性，发现需要 B                           │  │
│   │  3. 创建 B 实例 → 放入二级缓存                       │  │
│   │  4. B 填充属性，发现需要 A                           │  │
│   │  5. 从二级缓存获取 A (原始对象) ✓                    │  │
│   │  6. B 初始化完成 → 移到一级缓存                      │  │
│   │  7. A 填充 B 完成 → 移到一级缓存                     │  │
│   │                                                     │  │
│   │  这个场景下，二级缓存确实够用！                      │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 场景分析：有 AOP

```
┌─────────────────────────────────────────────────────────────┐
│                    有 AOP 场景（必须三级缓存）               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   A 依赖 B，B 依赖 A，A 有 AOP 代理                          │
│                                                             │
│   问题：如果只用二级缓存                                     │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  1. 创建 A 实例 → 放入二级缓存 (A原始对象)           │  │
│   │  2. A 填充属性，发现需要 B                           │  │
│   │  3. 创建 B 实例                                      │  │
│   │  4. B 填充属性，从二级缓存获取 A (原始对象)          │  │
│   │     ❌ B 拿到的是原始 A，不是代理 A！                │  │
│   │  5. B 初始化完成                                     │  │
│   │  6. A 初始化完成，创建代理 A'                        │  │
│   │     ❌ 此时 B 持有的还是原始 A！                     │  │
│   │                                                     │  │
│   │  结果：B.a != 容器中的 A (代理对象)                  │  │
│   │  违反了单例原则！                                    │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   三级缓存解决方案：                                         │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  1. 创建 A 实例                                      │  │
│   │  2. 放入三级缓存: ObjectFactory (可生成代理)         │  │
│   │  3. A 填充属性，发现需要 B                           │  │
│   │  4. 创建 B 实例                                      │  │
│   │  5. B 填充属性，发现需要 A                           │  │
│   │  6. 从三级缓存获取 ObjectFactory                     │  │
│   │  7. 调用 ObjectFactory.getObject()                   │  │
│   │     → 判断需要 AOP → 创建代理 A' → 放入二级缓存      │  │
│   │  8. B 拿到代理 A' ✓                                  │  │
│   │  9. B 初始化完成                                     │  │
│   │  10. A 初始化完成，发现二级缓存已有代理，直接使用    │  │
│   │                                                     │  │
│   │  结果：B.a == 容器中的 A (都是代理对象)              │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 源码分析

```java
// DefaultSingletonBeanRegistry.java

// 三级缓存定义
private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);          // 一级
private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>(16);      // 二级
private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16);         // 三级

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
                        // 3. 从三级缓存获取 ObjectFactory
                        ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
                        if (singletonFactory != null) {
                            // 调用工厂方法（可能创建代理）
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
```

```java
// AbstractAutowireCapableBeanFactory.java

// 创建 Bean 时添加到三级缓存
protected Object doCreateBean(String beanName, RootBeanDefinition mbd, Object[] args) {
    // 创建实例
    BeanWrapper instanceWrapper = createBeanInstance(beanName, mbd, args);
    Object bean = instanceWrapper.getWrappedInstance();
    
    // 判断是否需要提前暴露
    boolean earlySingletonExposure = (mbd.isSingleton() && 
            this.allowCircularReferences &&
            isSingletonCurrentlyInCreation(beanName));
    
    if (earlySingletonExposure) {
        // 添加到三级缓存，传入 ObjectFactory
        addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
    }
    
    // 填充属性
    populateBean(beanName, mbd, instanceWrapper);
    
    // 初始化（包括 AOP 代理）
    Object exposedObject = initializeBean(beanName, exposedObject, mbd);
    
    return exposedObject;
}

// 获取早期 Bean 引用（关键：这里决定是否创建代理）
protected Object getEarlyBeanReference(String beanName, RootBeanDefinition mbd, Object bean) {
    Object exposedObject = bean;
    
    // 调用 SmartInstantiationAwareBeanPostProcessor
    for (BeanPostProcessor bp : getBeanPostProcessors()) {
        if (bp instanceof SmartInstantiationAwareBeanPostProcessor) {
            SmartInstantiationAwareBeanPostProcessor ibp = 
                (SmartInstantiationAwareBeanPostProcessor) bp;
            // 这里会判断是否需要 AOP 代理
            exposedObject = ibp.getEarlyBeanReference(exposedObject, beanName);
        }
    }
    return exposedObject;
}
```

## 流程图解

```
┌─────────────────────────────────────────────────────────────┐
│                    完整流程                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   创建 A (有AOP):                                            │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  1. 实例化 A                                         │  │
│   │     ↓                                                │  │
│   │  2. A 加入三级缓存 (ObjectFactory)                   │  │
│   │     ↓                                                │  │
│   │  3. 填充 A 的属性，需要 B                            │  │
│   │     ↓                                                │  │
│   │  4. 实例化 B                                         │  │
│   │     ↓                                                │  │
│   │  5. B 加入三级缓存                                   │  │
│   │     ↓                                                │  │
│   │  6. 填充 B 的属性，需要 A                            │  │
│   │     ↓                                                │  │
│   │  7. getSingleton(A):                                 │  │
│   │     一级缓存: 无                                      │  │
│   │     二级缓存: 无                                      │  │
│   │     三级缓存: 有 ObjectFactory                        │  │
│   │     ↓                                                │  │
│   │  8. ObjectFactory.getObject()                        │  │
│   │     → getEarlyBeanReference()                        │  │
│   │     → 检测到需要 AOP                                 │  │
│   │     → 创建代理对象 A'                                │  │
│   │     → A' 放入二级缓存                                │  │
│   │     ↓                                                │  │
│   │  9. B.a = A' (代理对象)                              │  │
│   │     ↓                                                │  │
│   │  10. B 初始化完成 → 一级缓存                         │  │
│   │     ↓                                                │  │
│   │  11. A.b = B                                         │  │
│   │     ↓                                                │  │
│   │  12. A 初始化时检查二级缓存有代理                    │  │
│   │      使用已有的 A' (不重复创建代理)                  │  │
│   │     ↓                                                │  │
│   │  13. A' 放入一级缓存                                 │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 为什么不能只用二级缓存？

```
┌─────────────────────────────────────────────────────────────┐
│                    根本原因                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   Spring 设计原则：尽量保持 Bean 创建的正常流程              │
│                                                             │
│   正常流程：                                                 │
│   实例化 → 填充属性 → 初始化 → 后置处理(AOP代理)             │
│                                                             │
│   如果用二级缓存：                                           │
│   • 必须在实例化后立即创建代理（不知道会不会循环依赖）       │
│   • 打破了 Bean 生命周期                                    │
│   • 每个 Bean 都要提前判断是否需要代理                      │
│                                                             │
│   三级缓存的优雅之处：                                       │
│   • 只存工厂方法，延迟到真正需要时才调用                    │
│   • 没有循环依赖：正常流程，后置处理时创建代理              │
│   • 有循环依赖：被依赖时，通过工厂提前创建代理              │
│                                                             │
│   总结：三级缓存是为了兼顾"不破坏生命周期"和"解决AOP循环"   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> 三级缓存的核心作用是**解决 AOP 代理对象的循环依赖**。二级缓存存对象，三级缓存存 ObjectFactory 工厂。有循环依赖时，通过工厂提前创建代理；没有循环依赖时，保持正常生命周期在后置处理器创建代理。这是一种延迟决策的设计。

### 1分钟版本

> **问题本质**：
> - 二级缓存可以解决简单对象的循环依赖
> - 但 AOP 代理对象会有问题
>
> **AOP 的问题**：
> - 正常情况：代理在 Bean 初始化完成后创建
> - 循环依赖时：B 依赖 A，此时 A 还没初始化完，代理还没创建
> - 如果 B 拿到原始 A，后来 A 变成代理 A'，单例就不一致了
>
> **三级缓存的解决**：
> - 三级缓存存的是 `ObjectFactory`，不是对象
> - 被依赖时调用 `getObject()`，此时判断是否需要代理
> - 需要代理 → 提前创建代理，存入二级缓存
> - 不需要 → 返回原始对象
>
> **设计优势**：
> - 延迟决策：只有被循环依赖时才提前创建代理
> - 保持生命周期：没有循环依赖时正常流程
> - 单例一致：保证所有引用都是同一个对象

---

*关联文档：[spring-circular-dependency.md](spring-circular-dependency.md) | [spring-aop.md](spring-aop.md)*
