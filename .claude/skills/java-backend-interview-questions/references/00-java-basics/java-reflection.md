# Java 反射机制是什么？如何应用？

## 反射概述

```
┌─────────────────────────────────────────────────────────────┐
│                    Java 反射 (Reflection)                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   定义: 在运行时动态获取类的信息并操作类或对象的机制        │
│                                                             │
│   可以做什么:                                                │
│   1. 获取类的信息 (类名、方法、字段、注解等)                │
│   2. 创建对象 (不用 new)                                    │
│   3. 调用方法 (包括私有方法)                                │
│   4. 访问/修改字段 (包括私有字段)                           │
│                                                             │
│   核心类:                                                    │
│   ├── Class<T>      类的元信息                              │
│   ├── Field         字段                                    │
│   ├── Method        方法                                    │
│   ├── Constructor   构造器                                  │
│   └── Annotation    注解                                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 获取 Class 对象

```java
// 三种方式获取 Class 对象

// 1. 类名.class (编译时确定)
Class<User> clazz1 = User.class;

// 2. 对象.getClass() (运行时确定)
User user = new User();
Class<?> clazz2 = user.getClass();

// 3. Class.forName() (动态加载)
Class<?> clazz3 = Class.forName("com.example.User");

// 三种方式获取的是同一个 Class 对象
System.out.println(clazz1 == clazz2);  // true
System.out.println(clazz2 == clazz3);  // true
```

## 反射操作示例

```java
public class User {
    private String name;
    private int age;
    
    public User() {}
    public User(String name) { this.name = name; }
    
    private void privateMethod() {
        System.out.println("私有方法");
    }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
```

### 创建对象

```java
Class<User> clazz = User.class;

// 方式1: 无参构造
User user1 = clazz.getDeclaredConstructor().newInstance();

// 方式2: 有参构造
Constructor<User> constructor = clazz.getDeclaredConstructor(String.class);
User user2 = constructor.newInstance("Tom");
```

### 访问字段

```java
Class<User> clazz = User.class;
User user = new User();

// 获取私有字段
Field nameField = clazz.getDeclaredField("name");
nameField.setAccessible(true);  // 突破私有限制

// 设置值
nameField.set(user, "Tom");

// 获取值
String name = (String) nameField.get(user);
System.out.println(name);  // Tom
```

### 调用方法

```java
Class<User> clazz = User.class;
User user = new User();

// 获取公有方法
Method setNameMethod = clazz.getMethod("setName", String.class);
setNameMethod.invoke(user, "Jerry");

// 获取私有方法
Method privateMethod = clazz.getDeclaredMethod("privateMethod");
privateMethod.setAccessible(true);  // 突破私有限制
privateMethod.invoke(user);  // 输出: 私有方法
```

## 反射性能

```
┌─────────────────────────────────────────────────────────────┐
│                    反射性能问题                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   反射比直接调用慢的原因:                                    │
│   1. 需要动态解析类型                                       │
│   2. 无法进行 JIT 优化                                      │
│   3. 需要进行安全检查                                       │
│   4. 参数装箱拆箱                                           │
│                                                             │
│   性能对比 (相对值):                                         │
│   直接调用:   1x                                            │
│   反射调用:   5-20x (取决于场景)                            │
│                                                             │
│   优化方式:                                                  │
│   1. 缓存 Class/Method/Field 对象                           │
│   2. setAccessible(true) 跳过安全检查                       │
│   3. 使用 MethodHandle (Java 7+)                            │
│   4. 使用 LambdaMetafactory (Java 8+)                       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

```java
// 优化: 缓存 Method 对象
private static final Method SET_NAME_METHOD;

static {
    try {
        SET_NAME_METHOD = User.class.getMethod("setName", String.class);
        SET_NAME_METHOD.setAccessible(true);
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
}
```

## 应用场景

```
┌─────────────────────────────────────────────────────────────┐
│                    反射应用场景                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. Spring IOC                                              │
│      └── 通过反射创建 Bean 对象                             │
│                                                             │
│   2. Spring AOP                                              │
│      └── 动态代理中反射调用目标方法                         │
│                                                             │
│   3. MyBatis                                                 │
│      └── 反射设置实体对象属性值                             │
│                                                             │
│   4. Jackson/Gson                                            │
│      └── JSON 序列化/反序列化                               │
│                                                             │
│   5. JUnit                                                   │
│      └── 反射调用测试方法                                   │
│                                                             │
│   6. IDE 代码提示                                            │
│      └── 反射获取类的方法、字段信息                         │
│                                                             │
│   7. 配置文件加载                                            │
│      └── 根据类名字符串创建对象                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 反射与安全

```java
// Java 9+ 模块系统对反射的限制
// 需要在 module-info.java 中开放模块

module my.module {
    opens com.example to spring.core;  // 向 Spring 开放反射
}

// 或者使用 JVM 参数
// --add-opens java.base/java.lang=ALL-UNNAMED
```

## 面试回答

### 30秒版本

> 反射是在**运行时**动态获取类信息并操作对象的机制。核心类：`Class`、`Field`、`Method`、`Constructor`。可以创建对象、访问私有成员（`setAccessible(true)`）、调用方法。性能比直接调用慢 5-20 倍，需缓存 Method 对象优化。应用：Spring IOC/AOP、MyBatis、Jackson、JUnit。

### 1分钟版本

> **反射定义**：
> - 运行时动态获取类信息并操作对象
>
> **核心操作**：
> - 获取 Class：类名.class、getClass()、forName()
> - 创建对象：newInstance()
> - 访问字段：getDeclaredField() + setAccessible()
> - 调用方法：getMethod().invoke()
>
> **性能问题**：
> - 比直接调用慢 5-20 倍
> - 优化：缓存 Method/Field 对象
>
> **应用场景**：
> - Spring IOC：创建 Bean
> - Spring AOP：动态代理
> - MyBatis：设置属性
> - JSON 序列化
>
> **注意**：
> - Java 9+ 模块系统限制反射
> - 私有成员需 setAccessible(true)

---

*关联文档：[java-annotation.md](java-annotation.md) | [dynamic-proxy.md](dynamic-proxy.md)*
