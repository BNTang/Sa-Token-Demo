# Spring 框架核心

> Java/Spring Boot 编码规范 - Spring 框架原理与设计模式

---

## 一、Spring 启动过程

### Spring 容器启动流程

```
1. 加载配置文件/注解类
2. 创建 BeanFactory（容器）
3. Bean 定义加载与注册
4. Bean 实例化前的后置处理
5. Bean 实例化
6. 属性注入（依赖注入）
7. Aware 接口回调
8. Bean 后置处理器前置方法
9. InitializingBean.afterPropertiesSet()
10. @PostConstruct / init-method
11. Bean 后置处理器后置方法
12. Bean 完全初始化
```

### 核心代码流程

```java
/**
 * AbstractApplicationContext.refresh() - 核心方法
 */
@Override
public void refresh() throws BeansException, IllegalStateException {
    synchronized (this.startupShutdownMonitor) {
        // 1. 准备刷新上下文环境
        prepareRefresh();
        
        // 2. 初始化 BeanFactory
        ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();
        
        // 3. BeanFactory 预处理配置
        prepareBeanFactory(beanFactory);
        
        try {
            // 4. BeanFactory 后置处理
            postProcessBeanFactory(beanFactory);
            
            // 5. 调用 BeanFactory 后置处理器
            invokeBeanFactoryPostProcessors(beanFactory);
            
            // 6. 注册 Bean 后置处理器
            registerBeanPostProcessors(beanFactory);
            
            // 7. 初始化消息源（国际化）
            initMessageSource();
            
            // 8. 初始化事件多播器
            initApplicationEventMulticaster();
            
            // 9. 刷新（子类实现）
            onRefresh();
            
            // 10. 注册监听器
            registerListeners();
            
            // 11. 实例化所有单例 Bean
            finishBeanFactoryInitialization(beanFactory);
            
            // 12. 完成刷新，发布事件
            finishRefresh();
            
        } catch (BeansException ex) {
            // 销毁已创建的 Bean
            destroyBeans();
            cancelRefresh(ex);
            throw ex;
        } finally {
            resetCommonCaches();
        }
    }
}
```

### Bean 生命周期详解

#### 1. 实例化阶段

```java
/**
 * AbstractAutowireCapableBeanFactory.createBean()
 */
@Override
protected Object createBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args) {
    // 1. 实例化前处理
    Object bean = resolveBeforeInstantiation(beanName, mbd);
    if (bean != null) {
        return bean;
    }
    
    // 2. 创建 Bean 实例
    Object beanInstance = doCreateBean(beanName, mbd, args);
    return beanInstance;
}
```

#### 2. 属性注入阶段

```java
/**
 * 属性注入（依赖注入）
 */
protected void populateBean(String beanName, RootBeanDefinition mbd, @Nullable BeanWrapper bw) {
    // 1. 后置处理器前置方法
    for (BeanPostProcessor bp : getBeanPostProcessors()) {
        if (bp instanceof InstantiationAwareBeanPostProcessor) {
            InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
            if (!ibp.postProcessAfterInstantiation(bw.getWrappedInstance(), beanName)) {
                return;
            }
        }
    }
    
    // 2. 属性值处理
    PropertyValues pvs = mbd.getPropertyValues();
    
    // 3. 自动装配（@Autowired）
    if (mbd.getResolvedAutowireMode() == AUTOWIRE_BY_NAME || 
        mbd.getResolvedAutowireMode() == AUTOWIRE_BY_TYPE) {
        autowireByName(beanName, mbd, bw, newPvs);
        autowireByType(beanName, mbd, bw, newPvs);
    }
    
    // 4. 应用属性值
    applyPropertyValues(beanName, mbd, bw, pvs);
}
```

#### 3. 初始化阶段

```java
/**
 * 初始化 Bean
 */
protected Object initializeBean(String beanName, Object bean, @Nullable RootBeanDefinition mbd) {
    // 1. Aware 接口回调
    invokeAwareMethods(beanName, bean);
    
    // 2. BeanPostProcessor 前置方法
    Object wrappedBean = applyBeanPostProcessorsBeforeInitialization(bean, beanName);
    
    // 3. 调用初始化方法
    invokeInitMethods(beanName, wrappedBean, mbd);
    
    // 4. BeanPostProcessor 后置方法
    wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
    
    return wrappedBean;
}

/**
 * Aware 接口回调
 */
private void invokeAwareMethods(String beanName, Object bean) {
    if (bean instanceof Aware) {
        if (bean instanceof BeanNameAware) {
            ((BeanNameAware) bean).setBeanName(beanName);
        }
        if (bean instanceof BeanClassLoaderAware) {
            ((BeanClassLoaderAware) bean).setBeanClassLoader(getBeanClassLoader());
        }
        if (bean instanceof BeanFactoryAware) {
            ((BeanFactoryAware) bean).setBeanFactory(this);
        }
    }
}
```

### 实战示例：Bean 生命周期监控

```java
/**
 * Bean 生命周期演示
 */
@Component
@Slf4j
public class LifecycleBean implements BeanNameAware, BeanFactoryAware,
        ApplicationContextAware, InitializingBean, DisposableBean {
    
    private String beanName;
    
    public LifecycleBean() {
        log.info("1. 构造函数调用");
    }
    
    @Override
    public void setBeanName(String name) {
        this.beanName = name;
        log.info("2. BeanNameAware.setBeanName: {}", name);
    }
    
    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        log.info("3. BeanFactoryAware.setBeanFactory");
    }
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        log.info("4. ApplicationContextAware.setApplicationContext");
    }
    
    @PostConstruct
    public void postConstruct() {
        log.info("5. @PostConstruct 注解方法");
    }
    
    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("6. InitializingBean.afterPropertiesSet");
    }
    
    @PreDestroy
    public void preDestroy() {
        log.info("7. @PreDestroy 注解方法");
    }
    
    @Override
    public void destroy() throws Exception {
        log.info("8. DisposableBean.destroy");
    }
}

/**
 * 自定义 BeanPostProcessor
 */
@Component
@Slf4j
public class CustomBeanPostProcessor implements BeanPostProcessor {
    
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof LifecycleBean) {
            log.info("4.5. BeanPostProcessor.postProcessBeforeInitialization: {}", beanName);
        }
        return bean;
    }
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof LifecycleBean) {
            log.info("6.5. BeanPostProcessor.postProcessAfterInitialization: {}", beanName);
        }
        return bean;
    }
}
```

**输出顺序**：

```
1. 构造函数调用
2. BeanNameAware.setBeanName: lifecycleBean
3. BeanFactoryAware.setBeanFactory
4. ApplicationContextAware.setApplicationContext
4.5. BeanPostProcessor.postProcessBeforeInitialization: lifecycleBean
5. @PostConstruct 注解方法
6. InitializingBean.afterPropertiesSet
6.5. BeanPostProcessor.postProcessAfterInitialization: lifecycleBean
7. @PreDestroy 注解方法
8. DisposableBean.destroy
```

---

## 二、SpringBoot 启动流程

### 启动流程概览

```java
/**
 * SpringApplication.run() - 入口方法
 */
public ConfigurableApplicationContext run(String... args) {
    // 1. 创建 StopWatch（计时器）
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    
    // 2. 创建 BootstrapContext
    DefaultBootstrapContext bootstrapContext = createBootstrapContext();
    ConfigurableApplicationContext context = null;
    
    // 3. 配置 Headless 模式
    configureHeadlessProperty();
    
    // 4. 获取 SpringApplicationRunListeners
    SpringApplicationRunListeners listeners = getRunListeners(args);
    listeners.starting(bootstrapContext, this.mainApplicationClass);
    
    try {
        // 5. 封装命令行参数
        ApplicationArguments applicationArguments = new DefaultApplicationArguments(args);
        
        // 6. 准备环境
        ConfigurableEnvironment environment = prepareEnvironment(listeners, bootstrapContext, applicationArguments);
        configureIgnoreBeanInfo(environment);
        
        // 7. 打印 Banner
        Banner printedBanner = printBanner(environment);
        
        // 8. 创建 ApplicationContext
        context = createApplicationContext();
        context.setApplicationStartup(this.applicationStartup);
        
        // 9. 准备 Context
        prepareContext(bootstrapContext, context, environment, listeners, applicationArguments, printedBanner);
        
        // 10. 刷新 Context（核心）
        refreshContext(context);
        
        // 11. 刷新后处理
        afterRefresh(context, applicationArguments);
        
        // 12. 停止计时
        stopWatch.stop();
        if (this.logStartupInfo) {
            new StartupInfoLogger(this.mainApplicationClass).logStarted(getApplicationLog(), stopWatch);
        }
        
        // 13. 发布启动完成事件
        listeners.started(context);
        
        // 14. 调用 Runners
        callRunners(context, applicationArguments);
        
    } catch (Throwable ex) {
        handleRunFailure(context, ex, listeners);
        throw new IllegalStateException(ex);
    }
    
    try {
        // 15. 发布运行中事件
        listeners.running(context);
    } catch (Throwable ex) {
        handleRunFailure(context, ex, null);
        throw new IllegalStateException(ex);
    }
    
    return context;
}
```

### 核心步骤详解

#### 1. 自动配置原理

```java
/**
 * @SpringBootApplication 组合注解
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan
public @interface SpringBootApplication {
    // ...
}

/**
 * @EnableAutoConfiguration 核心注解
 */
@AutoConfigurationPackage
@Import(AutoConfigurationImportSelector.class)
public @interface EnableAutoConfiguration {
    // ...
}

/**
 * AutoConfigurationImportSelector - 自动配置导入
 */
public class AutoConfigurationImportSelector implements DeferredImportSelector {
    
    @Override
    public String[] selectImports(AnnotationMetadata annotationMetadata) {
        if (!isEnabled(annotationMetadata)) {
            return NO_IMPORTS;
        }
        
        // 1. 加载自动配置类
        AutoConfigurationEntry autoConfigurationEntry = getAutoConfigurationEntry(annotationMetadata);
        return StringUtils.toStringArray(autoConfigurationEntry.getConfigurations());
    }
    
    protected AutoConfigurationEntry getAutoConfigurationEntry(AnnotationMetadata annotationMetadata) {
        if (!isEnabled(annotationMetadata)) {
            return EMPTY_ENTRY;
        }
        
        // 2. 从 spring.factories 加载配置类
        List<String> configurations = getCandidateConfigurations(annotationMetadata, attributes);
        
        // 3. 去重
        configurations = removeDuplicates(configurations);
        
        // 4. 排除
        Set<String> exclusions = getExclusions(annotationMetadata, attributes);
        configurations.removeAll(exclusions);
        
        // 5. 过滤（@ConditionalOnClass 等）
        configurations = getConfigurationClassFilter().filter(configurations);
        
        return new AutoConfigurationEntry(configurations, exclusions);
    }
}
```

**META-INF/spring.factories**（部分）：

```properties
# Auto Configure
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,\
org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration,\
org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
```

#### 2. 条件注解原理

```java
/**
 * 数据源自动配置示例
 */
@Configuration
@ConditionalOnClass({DataSource.class, EmbeddedDatabaseType.class})
@ConditionalOnMissingBean(DataSource.class)
@EnableConfigurationProperties(DataSourceProperties.class)
public class DataSourceAutoConfiguration {
    
    @Bean
    @ConditionalOnProperty(prefix = "spring.datasource", name = "url")
    public DataSource dataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }
}
```

**常用条件注解**：

| 注解 | 说明 |
|------|------|
| **@ConditionalOnClass** | Classpath 中存在指定类 |
| **@ConditionalOnMissingClass** | Classpath 中不存在指定类 |
| **@ConditionalOnBean** | 容器中存在指定 Bean |
| **@ConditionalOnMissingBean** | 容器中不存在指定 Bean |
| **@ConditionalOnProperty** | 配置文件中存在指定属性 |
| **@ConditionalOnWebApplication** | 是 Web 应用 |

---

## 三、Spring 事务传播行为

### 7 种传播行为

| 传播行为 | 说明 | 使用场景 |
|---------|------|---------|
| **REQUIRED**（默认） | 如果有事务则加入，没有则创建新事务 | 最常用 |
| **REQUIRES_NEW** | 总是创建新事务，挂起当前事务 | 日志记录 |
| **SUPPORTS** | 如果有事务则加入，没有则非事务执行 | 查询操作 |
| **NOT_SUPPORTED** | 总是非事务执行，挂起当前事务 | 不需要事务的操作 |
| **MANDATORY** | 必须在事务中执行，否则抛异常 | 严格要求事务 |
| **NEVER** | 不能在事务中执行，否则抛异常 | 禁止事务 |
| **NESTED** | 嵌套事务（使用 Savepoint） | 部分回滚 |

### REQUIRED（默认）

**定义**：如果当前存在事务，则加入该事务；如果不存在，则创建新事务。

```java
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements IOrderService {
    
    private final OrderMapper orderMapper;
    private final OrderItemService orderItemService;
    
    /**
     * 外层事务
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void createOrder(OrderCreateDTO dto) {
        // 插入订单
        OrderDO order = convertToEntity(dto);
        orderMapper.insert(order);
        
        // 插入订单明细（加入当前事务）
        orderItemService.createItems(order.getId(), dto.getItems());
        
        // 任何一个方法抛异常，整个事务都回滚
    }
}

@Service
@RequiredArgsConstructor
public class OrderItemServiceImpl implements IOrderItemService {
    
    private final OrderItemMapper orderItemMapper;
    
    /**
     * 内层事务（加入外层事务）
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void createItems(Long orderId, List<OrderItemDTO> items) {
        for (OrderItemDTO item : items) {
            OrderItemDO orderItem = convertToEntity(orderId, item);
            orderItemMapper.insert(orderItem);
        }
    }
}
```

**特点**：
- ✅ 内外层方法共享同一个事务
- ✅ 任何一个方法抛异常，整个事务回滚

---

### REQUIRES_NEW

**定义**：总是创建新事务，如果当前存在事务，则挂起当前事务。

```java
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements IOrderService {
    
    private final OrderMapper orderMapper;
    private final LogService logService;
    
    /**
     * 外层事务
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void createOrder(OrderCreateDTO dto) {
        // 插入订单
        OrderDO order = convertToEntity(dto);
        orderMapper.insert(order);
        
        // 记录日志（独立事务）
        logService.log("创建订单: " + order.getId());
        
        // 模拟异常
        if (order.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw exception(ErrorCode.INVALID_AMOUNT);
        }
        
        // 结果：订单回滚，但日志已提交
    }
}

@Service
@RequiredArgsConstructor
public class LogServiceImpl implements ILogService {
    
    private final LogMapper logMapper;
    
    /**
     * 独立事务（不受外层事务影响）
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String message) {
        LogDO log = new LogDO();
        log.setMessage(message);
        log.setCreateTime(LocalDateTime.now());
        logMapper.insert(log);
        
        // 即使外层事务回滚，日志也已提交
    }
}
```

**特点**：
- ✅ 内层方法有独立事务
- ✅ 外层事务回滚不影响内层事务
- ✅ 适用场景：日志记录、审计记录

---

### NESTED（嵌套事务）

**定义**：如果当前存在事务，则在嵌套事务中执行（使用 Savepoint）。

```java
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements IOrderService {
    
    private final OrderMapper orderMapper;
    private final CouponService couponService;
    
    /**
     * 外层事务
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void createOrder(OrderCreateDTO dto) {
        // 插入订单
        OrderDO order = convertToEntity(dto);
        orderMapper.insert(order);
        
        try {
            // 扣减优惠券（嵌套事务）
            couponService.deductCoupon(dto.getUserId(), dto.getCouponId());
        } catch (Exception e) {
            // 优惠券扣减失败，只回滚嵌套事务
            log.warn("[优惠券扣减失败] 继续创建订单", e);
        }
        
        // 订单创建成功
    }
}

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements ICouponService {
    
    private final CouponMapper couponMapper;
    
    /**
     * 嵌套事务（外层事务的子事务）
     */
    @Override
    @Transactional(propagation = Propagation.NESTED)
    public void deductCoupon(Long userId, Long couponId) {
        CouponDO coupon = couponMapper.selectById(couponId);
        
        if (coupon == null || coupon.getStatus() != 1) {
            throw exception(ErrorCode.COUPON_INVALID);
        }
        
        coupon.setStatus(2);
        couponMapper.updateById(coupon);
        
        // 如果抛异常，只回滚到 Savepoint
    }
}
```

**特点**：
- ✅ 内层方法可以单独回滚（回滚到 Savepoint）
- ✅ 外层事务回滚，内层事务也回滚
- ✅ 适用场景：部分失败可容忍

---

### 传播行为对比

| 场景 | REQUIRED | REQUIRES_NEW | NESTED |
|------|----------|--------------|--------|
| **外层无事务** | 创建新事务 | 创建新事务 | 创建新事务 |
| **外层有事务** | 加入事务 | 创建新事务（挂起外层） | 嵌套事务（Savepoint） |
| **内层异常** | 外层回滚 | 外层不受影响 | 可回滚到 Savepoint |
| **外层异常** | 内层回滚 | 内层不受影响 | 内层回滚 |

---

## 四、Spring 中的设计模式

### 1. 工厂模式（Factory Pattern）

**应用**：BeanFactory、ApplicationContext

```java
/**
 * BeanFactory - 简单工厂
 */
public interface BeanFactory {
    Object getBean(String name) throws BeansException;
    <T> T getBean(Class<T> requiredType) throws BeansException;
}

// 使用
ApplicationContext context = new ClassPathXmlApplicationContext("spring.xml");
UserService userService = context.getBean(UserService.class);
```

### 2. 单例模式（Singleton Pattern）

**应用**：Spring Bean 默认单例

```java
/**
 * DefaultSingletonBeanRegistry - 单例注册表
 */
public class DefaultSingletonBeanRegistry {
    
    // 一级缓存：单例对象缓存
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);
    
    @Nullable
    protected Object getSingleton(String beanName) {
        return this.singletonObjects.get(beanName);
    }
}
```

### 3. 代理模式（Proxy Pattern）

**应用**：AOP、@Transactional、@Async

```java
/**
 * JdkDynamicAopProxy - JDK 动态代理
 */
public class JdkDynamicAopProxy implements AopProxy, InvocationHandler {
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 1. 获取拦截器链
        List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);
        
        // 2. 执行拦截器链
        MethodInvocation invocation = new ReflectiveMethodInvocation(proxy, target, method, args, targetClass, chain);
        Object retVal = invocation.proceed();
        
        return retVal;
    }
}
```

### 4. 模板方法模式（Template Method Pattern）

**应用**：JdbcTemplate、RedisTemplate、RestTemplate

```java
/**
 * JdbcTemplate - 模板方法
 */
public class JdbcTemplate {
    
    @Nullable
    public <T> T execute(StatementCallback<T> action) throws DataAccessException {
        // 1. 获取连接
        Connection con = DataSourceUtils.getConnection(obtainDataSource());
        Statement stmt = null;
        
        try {
            // 2. 创建 Statement
            stmt = con.createStatement();
            
            // 3. 执行回调（子类实现）
            T result = action.doInStatement(stmt);
            
            return result;
            
        } catch (SQLException ex) {
            // 4. 异常处理
            throw translateException("StatementCallback", sql, ex);
        } finally {
            // 5. 释放资源
            JdbcUtils.closeStatement(stmt);
            DataSourceUtils.releaseConnection(con, getDataSource());
        }
    }
}
```

### 5. 观察者模式（Observer Pattern）

**应用**：Spring Event、ApplicationListener

```java
/**
 * ApplicationEventMulticaster - 事件多播器
 */
public interface ApplicationEventMulticaster {
    void multicastEvent(ApplicationEvent event);
}

/**
 * 使用示例见"设计模式"章节
 */
```

### 6. 策略模式（Strategy Pattern）

**应用**：Resource 接口、InstantiationStrategy

```java
/**
 * Resource - 资源加载策略
 */
public interface Resource extends InputStreamSource {
    boolean exists();
    boolean isReadable();
    InputStream getInputStream() throws IOException;
}

// 具体策略
public class ClassPathResource implements Resource { }
public class FileSystemResource implements Resource { }
public class UrlResource implements Resource { }
```

### 7. 适配器模式（Adapter Pattern）

**应用**：HandlerAdapter、MessageConverter

```java
/**
 * HandlerAdapter - 处理器适配器
 */
public interface HandlerAdapter {
    boolean supports(Object handler);
    ModelAndView handle(HttpServletRequest request, 
                        HttpServletResponse response, 
                        Object handler) throws Exception;
}

// 具体适配器
public class RequestMappingHandlerAdapter implements HandlerAdapter { }
public class SimpleControllerHandlerAdapter implements HandlerAdapter { }
```

---

## 五、速查表

### Spring Bean 作用域

| 作用域 | 说明 |
|--------|------|
| **singleton** | 单例（默认） |
| **prototype** | 每次获取创建新实例 |
| **request** | 每个 HTTP 请求一个实例 |
| **session** | 每个 HTTP Session 一个实例 |
| **application** | 每个 ServletContext 一个实例 |

### Spring 事务传播行为

| 传播行为 | 适用场景 |
|---------|---------|
| **REQUIRED** | 常规业务（默认） |
| **REQUIRES_NEW** | 日志记录、审计 |
| **NESTED** | 部分失败可容忍 |
| **SUPPORTS** | 查询操作 |

### Spring 设计模式

| 设计模式 | 应用 |
|---------|------|
| **工厂模式** | BeanFactory |
| **单例模式** | Spring Bean |
| **代理模式** | AOP、@Transactional |
| **模板方法** | JdbcTemplate |
| **观察者模式** | Spring Event |
| **策略模式** | Resource |
| **适配器模式** | HandlerAdapter |
