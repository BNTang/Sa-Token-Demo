# MyBatis 插件机制

> Java 后端面试知识点 - MyBatis 深入

---

## 插件概述

MyBatis 允许在执行过程中拦截特定方法调用，实现自定义功能。

### 可拦截的对象

| 对象 | 可拦截方法 | 说明 |
|------|-----------|------|
| **Executor** | update, query, commit, rollback | SQL 执行器 |
| **StatementHandler** | prepare, parameterize, batch, update, query | SQL 语法构建 |
| **ParameterHandler** | getParameterObject, setParameters | 参数处理 |
| **ResultSetHandler** | handleResultSets, handleOutputParameters | 结果集处理 |

---

## 插件运行原理

### 核心机制：动态代理

```
┌─────────────────────────────────────────────────────────────────┐
│                      MyBatis 执行流程                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Executor ──> StatementHandler ──> ParameterHandler ──> ResultSetHandler
│     ↑              ↑                    ↑                  ↑    │
│     │              │                    │                  │    │
│  ┌──┴──────────────┴────────────────────┴──────────────────┴──┐ │
│  │                    Plugin Chain (代理链)                   │ │
│  │  Plugin1 → Plugin2 → Plugin3 → ... → 原始对象              │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### 源码分析

```java
// 1. Plugin 类 - 使用 JDK 动态代理
public class Plugin implements InvocationHandler {
    
    private final Object target;           // 被代理对象
    private final Interceptor interceptor; // 拦截器
    private final Map<Class<?>, Set<Method>> signatureMap; // 拦截方法签名
    
    // 创建代理对象
    public static Object wrap(Object target, Interceptor interceptor) {
        Map<Class<?>, Set<Method>> signatureMap = getSignatureMap(interceptor);
        Class<?> type = target.getClass();
        Class<?>[] interfaces = getAllInterfaces(type, signatureMap);
        if (interfaces.length > 0) {
            return Proxy.newProxyInstance(
                type.getClassLoader(),
                interfaces,
                new Plugin(target, interceptor, signatureMap));
        }
        return target;
    }
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Set<Method> methods = signatureMap.get(method.getDeclaringClass());
        if (methods != null && methods.contains(method)) {
            // 执行拦截器逻辑
            return interceptor.intercept(new Invocation(target, method, args));
        }
        // 不拦截，直接执行原方法
        return method.invoke(target, args);
    }
}

// 2. InterceptorChain - 拦截器链
public class InterceptorChain {
    
    private final List<Interceptor> interceptors = new ArrayList<>();
    
    // 为目标对象创建代理链
    public Object pluginAll(Object target) {
        for (Interceptor interceptor : interceptors) {
            target = interceptor.plugin(target);  // 层层包装
        }
        return target;
    }
}
```

---

## 编写自定义插件

### 步骤

1. 实现 `Interceptor` 接口
2. 使用 `@Intercepts` 注解指定拦截目标
3. 注册插件

### 示例1：SQL 执行时间统计

```java
@Intercepts({
    @Signature(
        type = StatementHandler.class,
        method = "query",
        args = {Statement.class, ResultHandler.class}
    ),
    @Signature(
        type = StatementHandler.class,
        method = "update",
        args = {Statement.class}
    )
})
@Slf4j
public class SqlExecutionTimePlugin implements Interceptor {
    
    /** 慢 SQL 阈值（毫秒） */
    private long slowSqlThreshold = 1000;
    
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        try {
            // 执行原方法
            return invocation.proceed();
        } finally {
            long costTime = System.currentTimeMillis() - startTime;
            
            // 获取 SQL 语句
            StatementHandler handler = (StatementHandler) invocation.getTarget();
            BoundSql boundSql = handler.getBoundSql();
            String sql = boundSql.getSql().replaceAll("\\s+", " ");
            
            if (costTime > slowSqlThreshold) {
                log.warn("[慢SQL] 耗时: {}ms, SQL: {}", costTime, sql);
            } else {
                log.debug("[SQL] 耗时: {}ms, SQL: {}", costTime, sql);
            }
        }
    }
    
    @Override
    public Object plugin(Object target) {
        // 使用 Plugin.wrap 创建代理
        return Plugin.wrap(target, this);
    }
    
    @Override
    public void setProperties(Properties properties) {
        // 读取配置参数
        String threshold = properties.getProperty("slowSqlThreshold", "1000");
        this.slowSqlThreshold = Long.parseLong(threshold);
    }
}
```

### 示例2：自动分页插件

```java
@Intercepts({
    @Signature(
        type = Executor.class,
        method = "query",
        args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}
    )
})
public class SimplePagePlugin implements Interceptor {
    
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        Object parameter = args[1];
        RowBounds rowBounds = (RowBounds) args[2];
        
        // 检查是否需要分页
        if (rowBounds == RowBounds.DEFAULT) {
            return invocation.proceed();
        }
        
        // 获取原始 SQL
        BoundSql boundSql = ms.getBoundSql(parameter);
        String originalSql = boundSql.getSql();
        
        // 构建分页 SQL
        String pageSql = originalSql + " LIMIT " + rowBounds.getOffset() 
                        + ", " + rowBounds.getLimit();
        
        // 反射修改 SQL
        Field sqlField = BoundSql.class.getDeclaredField("sql");
        sqlField.setAccessible(true);
        sqlField.set(boundSql, pageSql);
        
        // 重置 RowBounds
        args[2] = RowBounds.DEFAULT;
        
        return invocation.proceed();
    }
    
    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }
    
    @Override
    public void setProperties(Properties properties) {
    }
}
```

### 示例3：数据权限插件

```java
@Intercepts({
    @Signature(
        type = StatementHandler.class,
        method = "prepare",
        args = {Connection.class, Integer.class}
    )
})
public class DataPermissionPlugin implements Interceptor {
    
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler handler = (StatementHandler) invocation.getTarget();
        
        // 获取原始对象（可能被多层代理）
        MetaObject metaObject = SystemMetaObject.forObject(handler);
        while (metaObject.hasGetter("h")) {
            Object obj = metaObject.getValue("h");
            metaObject = SystemMetaObject.forObject(obj);
        }
        while (metaObject.hasGetter("target")) {
            Object obj = metaObject.getValue("target");
            metaObject = SystemMetaObject.forObject(obj);
        }
        
        // 获取 SQL
        BoundSql boundSql = handler.getBoundSql();
        String originalSql = boundSql.getSql();
        
        // 获取当前用户的数据权限
        Long deptId = SecurityUtils.getCurrentUserDeptId();
        
        // 追加数据权限条件
        if (deptId != null && originalSql.toLowerCase().contains("where")) {
            String permissionSql = originalSql + " AND dept_id = " + deptId;
            metaObject.setValue("delegate.boundSql.sql", permissionSql);
        }
        
        return invocation.proceed();
    }
    
    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }
    
    @Override
    public void setProperties(Properties properties) {
    }
}
```

---

## 注册插件

### 方式1：mybatis-config.xml

```xml
<plugins>
    <plugin interceptor="com.example.plugin.SqlExecutionTimePlugin">
        <property name="slowSqlThreshold" value="1000"/>
    </plugin>
</plugins>
```

### 方式2：Spring Boot 配置

```java
@Configuration
public class MyBatisConfig {
    
    @Bean
    public SqlExecutionTimePlugin sqlExecutionTimePlugin() {
        SqlExecutionTimePlugin plugin = new SqlExecutionTimePlugin();
        Properties props = new Properties();
        props.setProperty("slowSqlThreshold", "1000");
        plugin.setProperties(props);
        return plugin;
    }
}
```

### 方式3：MyBatis Plus

```java
@Configuration
public class MyBatisPlusConfig {
    
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        // 乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }
}
```

---

## 面试要点

### 核心答案

**问：简述 MyBatis 的插件运行原理，以及如何编写一个插件？**

答：

**运行原理**：
1. MyBatis 使用 **JDK 动态代理** 实现插件机制
2. 可拦截四大对象：Executor、StatementHandler、ParameterHandler、ResultSetHandler
3. 多个插件形成**代理链**，按配置顺序包装
4. 执行时从外层代理依次调用到原始对象

**编写步骤**：
1. 实现 `Interceptor` 接口的三个方法：
   - `intercept()`：拦截逻辑
   - `plugin()`：创建代理（通常用 `Plugin.wrap()`）
   - `setProperties()`：读取配置参数
2. 使用 `@Intercepts` + `@Signature` 指定拦截目标
3. 注册插件到 MyBatis 配置

**常见应用**：
- SQL 执行时间统计
- 分页处理
- 数据权限过滤
- 乐观锁处理

---

## 编码最佳实践

### ✅ 推荐做法

```java
// 1. 使用 MetaObject 安全获取属性
MetaObject metaObject = SystemMetaObject.forObject(target);
String sql = (String) metaObject.getValue("delegate.boundSql.sql");

// 2. 正确处理代理链
while (metaObject.hasGetter("h")) {
    Object obj = metaObject.getValue("h");
    metaObject = SystemMetaObject.forObject(obj);
}

// 3. 使用 MyBatis Plus 内置插件
@Bean
public MybatisPlusInterceptor interceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
    return interceptor;
}
```

### ❌ 避免做法

```java
// ❌ 直接反射修改 final 字段
Field field = BoundSql.class.getDeclaredField("sql");
field.setAccessible(true);
field.set(boundSql, newSql);  // 可能失败

// ❌ 忘记调用 invocation.proceed()
@Override
public Object intercept(Invocation invocation) throws Throwable {
    // 处理逻辑...
    // 忘记调用原方法！
}

// ❌ 插件顺序不当
// 分页插件应放在最后，先于其他插件执行
```
