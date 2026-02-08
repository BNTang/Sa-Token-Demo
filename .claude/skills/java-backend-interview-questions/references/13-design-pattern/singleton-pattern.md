# 单例模式有哪几种实现？如何保证线程安全？

## 概念解析

**单例模式 (Singleton Pattern)** 确保一个类只有一个实例，并提供全局访问点。

```
┌─────────────────────────────────────────────────────────────┐
│                    单例模式核心要点                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 私有构造函数 ── 防止外部 new 实例                        │
│   2. 静态实例变量 ── 保存唯一实例                            │
│   3. 公共访问方法 ── 提供全局访问点                          │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │                    Singleton                        │  │
│   ├─────────────────────────────────────────────────────┤  │
│   │  - instance: Singleton (static)                     │  │
│   │  - Singleton() (private)                            │  │
│   │  + getInstance(): Singleton (static)                │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 实现方式对比

```
┌─────────────────────────────────────────────────────────────┐
│                   单例实现方式对比表                          │
├──────────────┬────────┬────────┬────────┬──────────────────┤
│   实现方式    │ 线程安全 │ 懒加载  │ 防反射  │ 推荐度           │
├──────────────┼────────┼────────┼────────┼──────────────────┤
│   饿汉式      │   ✅    │   ❌   │   ❌   │ ⭐⭐⭐ 简单可用    │
│   懒汉式(无锁) │   ❌    │   ✅   │   ❌   │ ⭐ 不推荐         │
│   懒汉式(同步) │   ✅    │   ✅   │   ❌   │ ⭐⭐ 性能差       │
│   双重检查锁  │   ✅    │   ✅   │   ❌   │ ⭐⭐⭐⭐ 常用     │
│   静态内部类  │   ✅    │   ✅   │   ❌   │ ⭐⭐⭐⭐⭐ 推荐  │
│   枚举单例    │   ✅    │   ❌   │   ✅   │ ⭐⭐⭐⭐⭐ 最佳  │
└──────────────┴────────┴────────┴────────┴──────────────────┘
```

## 各种实现详解

### 1. 饿汉式（静态常量）

```java
/**
 * 饿汉式 - 类加载时就创建实例
 * 优点：简单，线程安全（类加载机制保证）
 * 缺点：不支持懒加载，可能浪费资源
 */
public class EagerSingleton {
    
    // 类加载时创建实例
    private static final EagerSingleton INSTANCE = new EagerSingleton();
    
    // 私有构造函数
    private EagerSingleton() {}
    
    public static EagerSingleton getInstance() {
        return INSTANCE;
    }
}
```

### 2. 懒汉式（线程不安全）

```java
/**
 * 懒汉式 - 首次使用时创建
 * ❌ 线程不安全，不推荐使用
 */
public class LazySingletonUnsafe {
    
    private static LazySingletonUnsafe instance;
    
    private LazySingletonUnsafe() {}
    
    public static LazySingletonUnsafe getInstance() {
        if (instance == null) {
            // 多线程可能同时进入，创建多个实例！
            instance = new LazySingletonUnsafe();
        }
        return instance;
    }
}
```

### 3. 懒汉式（同步方法）

```java
/**
 * 懒汉式 - synchronized 保证线程安全
 * 缺点：每次获取都要同步，性能差
 */
public class LazySingletonSync {
    
    private static LazySingletonSync instance;
    
    private LazySingletonSync() {}
    
    // 整个方法同步，性能开销大
    public static synchronized LazySingletonSync getInstance() {
        if (instance == null) {
            instance = new LazySingletonSync();
        }
        return instance;
    }
}
```

### 4. 双重检查锁（DCL）⭐

```java
/**
 * 双重检查锁 (Double-Checked Locking)
 * 优点：懒加载 + 线程安全 + 高性能
 * 注意：必须使用 volatile！
 */
public class DCLSingleton {
    
    // volatile 防止指令重排序
    private static volatile DCLSingleton instance;
    
    private DCLSingleton() {}
    
    public static DCLSingleton getInstance() {
        if (instance == null) {                    // 第一次检查（无锁）
            synchronized (DCLSingleton.class) {    // 加锁
                if (instance == null) {            // 第二次检查（有锁）
                    instance = new DCLSingleton();
                }
            }
        }
        return instance;
    }
}

/*
 * 为什么需要 volatile？
 * 
 * new DCLSingleton() 不是原子操作，分三步：
 * 1. 分配内存空间
 * 2. 初始化对象
 * 3. 将 instance 指向内存地址
 * 
 * 没有 volatile 时，可能重排序为 1→3→2
 * 线程A执行到步骤3，线程B判断 instance != null，返回未初始化的对象
 * 
 * volatile 禁止重排序，保证可见性
 */
```

### 5. 静态内部类 ⭐⭐

```java
/**
 * 静态内部类方式
 * 优点：
 * - 懒加载（内部类按需加载）
 * - 线程安全（类加载机制保证）
 * - 无同步开销
 */
public class InnerClassSingleton {
    
    private InnerClassSingleton() {}
    
    // 静态内部类，只有调用 getInstance 时才会加载
    private static class Holder {
        private static final InnerClassSingleton INSTANCE = new InnerClassSingleton();
    }
    
    public static InnerClassSingleton getInstance() {
        return Holder.INSTANCE;
    }
}

/*
 * 原理：
 * - 外部类加载时，不会加载内部类
 * - 调用 getInstance() 时，才加载 Holder 类
 * - JVM 类加载过程是线程安全的（ClassLoader 加锁）
 */
```

### 6. 枚举单例 ⭐⭐⭐（最佳实践）

```java
/**
 * 枚举单例 - Effective Java 推荐
 * 优点：
 * - 线程安全（JVM 保证）
 * - 防止反射攻击
 * - 防止序列化破坏
 */
public enum EnumSingleton {
    
    INSTANCE;  // 唯一实例
    
    // 实例方法
    private int value;
    
    public int getValue() {
        return value;
    }
    
    public void setValue(int value) {
        this.value = value;
    }
    
    public void doSomething() {
        System.out.println("Enum Singleton doing something...");
    }
}

// 使用
EnumSingleton singleton = EnumSingleton.INSTANCE;
singleton.doSomething();
```

## 破坏单例的方式及防御

### 1. 反射攻击

```java
// 反射破坏单例
public class ReflectionAttack {
    public static void main(String[] args) throws Exception {
        DCLSingleton s1 = DCLSingleton.getInstance();
        
        // 反射获取构造函数
        Constructor<DCLSingleton> constructor = 
            DCLSingleton.class.getDeclaredConstructor();
        constructor.setAccessible(true);  // 绕过 private
        DCLSingleton s2 = constructor.newInstance();  // 创建新实例
        
        System.out.println(s1 == s2);  // false - 单例被破坏！
    }
}

// 防御方式：构造函数中检查
public class ReflectionSafeSingleton {
    private static volatile ReflectionSafeSingleton instance;
    
    private ReflectionSafeSingleton() {
        if (instance != null) {
            throw new RuntimeException("单例已存在，禁止反射创建！");
        }
    }
    // ...
}

// 枚举天然防反射（JVM 层面禁止反射创建枚举实例）
```

### 2. 序列化破坏

```java
// 序列化破坏单例
public class SerializationAttack {
    public static void main(String[] args) throws Exception {
        DCLSingleton s1 = DCLSingleton.getInstance();
        
        // 序列化
        ObjectOutputStream oos = new ObjectOutputStream(
            new FileOutputStream("singleton.ser"));
        oos.writeObject(s1);
        oos.close();
        
        // 反序列化
        ObjectInputStream ois = new ObjectInputStream(
            new FileInputStream("singleton.ser"));
        DCLSingleton s2 = (DCLSingleton) ois.readObject();
        ois.close();
        
        System.out.println(s1 == s2);  // false - 单例被破坏！
    }
}

// 防御方式：实现 readResolve 方法
public class SerializationSafeSingleton implements Serializable {
    private static final long serialVersionUID = 1L;
    private static volatile SerializationSafeSingleton instance;
    
    private SerializationSafeSingleton() {}
    
    public static SerializationSafeSingleton getInstance() {
        // DCL...
    }
    
    // 反序列化时调用，返回已有实例
    private Object readResolve() {
        return instance;
    }
}

// 枚举天然防序列化破坏（序列化机制特殊处理）
```

## Spring 中的单例

```java
/**
 * Spring 单例 Bean（默认作用域）
 * 注意：Spring 单例与 GoF 单例不同
 * - GoF 单例：整个 JVM 只有一个实例
 * - Spring 单例：每个 IoC 容器只有一个实例
 */
@Component
// @Scope("singleton")  // 默认就是 singleton
public class SpringSingleton {
    
    // Spring 使用 ConcurrentHashMap 存储 Bean
    // 线程安全由容器保证
}

// 查看 Spring 源码 DefaultSingletonBeanRegistry
public class DefaultSingletonBeanRegistry {
    
    // 一级缓存：存放完全初始化好的 Bean
    private final Map<String, Object> singletonObjects = 
        new ConcurrentHashMap<>(256);
    
    // 二级缓存：存放原始 Bean（解决循环依赖）
    private final Map<String, Object> earlySingletonObjects = 
        new ConcurrentHashMap<>(16);
    
    // 三级缓存：存放 Bean 工厂
    private final Map<String, ObjectFactory<?>> singletonFactories = 
        new HashMap<>(16);
}
```

## 最佳实践

### ✅ 推荐做法

```java
// 1. 一般场景：枚举单例（首选）
public enum ConfigManager {
    INSTANCE;
    
    private Properties config;
    
    ConfigManager() {
        config = loadConfig();
    }
    
    public String getProperty(String key) {
        return config.getProperty(key);
    }
}

// 2. 需要懒加载：静态内部类
public class ConnectionPool {
    private ConnectionPool() {}
    
    private static class Holder {
        private static final ConnectionPool INSTANCE = new ConnectionPool();
    }
    
    public static ConnectionPool getInstance() {
        return Holder.INSTANCE;
    }
}

// 3. Spring 环境：直接用 @Component
@Component
public class UserService {
    // Spring 管理的单例
}
```

### ❌ 避免做法

```java
// 1. 不要用懒汉式无锁版本
public static Singleton getInstance() {
    if (instance == null) {  // 线程不安全！
        instance = new Singleton();
    }
    return instance;
}

// 2. DCL 不要忘记 volatile
private static /*volatile*/ Singleton instance;  // 缺少 volatile，有问题

// 3. 不要在单例中存储可变状态（线程不安全）
public enum BadSingleton {
    INSTANCE;
    private List<String> data = new ArrayList<>();  // 非线程安全
}
```

## 面试回答

### 30秒版本

> 单例模式有六种实现：饿汉式、懒汉式（不安全/同步）、双重检查锁（DCL+volatile）、静态内部类、枚举单例。**推荐枚举单例**——线程安全、防反射、防序列化。DCL 必须加 volatile 防止指令重排序。Spring 的 @Component 默认就是单例。

### 1分钟版本

> **六种实现方式**：
> 1. **饿汉式**：类加载时创建，简单但不支持懒加载
> 2. **懒汉式同步**：性能差，每次都要同步
> 3. **双重检查锁 DCL**：懒加载+高性能，必须用 volatile（防止重排序）
> 4. **静态内部类**：懒加载+线程安全，利用类加载机制
> 5. **枚举单例**：最佳实践，防反射+防序列化
>
> **保证线程安全**：
> - 饿汉式/静态内部类：类加载机制保证（ClassLoader 加锁）
> - DCL：synchronized + volatile
> - 枚举：JVM 底层保证
>
> **破坏单例**：反射（setAccessible）、序列化反序列化
> **防御**：构造函数检查 / readResolve() / 用枚举
>
> **Spring 单例**：每个 IoC 容器一个实例，用三级缓存 Map 存储。

---

*关联文档：[design-pattern-overview.md](design-pattern-overview.md) | [java-memory-model.md](../05-concurrency/java-memory-model.md)*
