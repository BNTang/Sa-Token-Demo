# Spring 中的 DI 是什么？

## DI 概述

```
┌─────────────────────────────────────────────────────────────┐
│                    DI (Dependency Injection)                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   什么是 DI？                                                │
│   依赖注入 - 将对象的依赖关系由容器在运行时注入              │
│   而不是由对象自己创建或查找依赖                            │
│                                                             │
│   DI 是 IoC 的具体实现方式:                                  │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  IoC (控制反转) = 设计思想                          │  │
│   │  DI (依赖注入) = 实现手段                           │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   核心思想:                                                  │
│   • 对象不再自己 new 依赖                                   │
│   • 容器负责创建和注入依赖                                  │
│   • 降低耦合，便于测试                                      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 没有 DI vs 使用 DI

```
┌─────────────────────────────────────────────────────────────┐
│                    没有 DI (传统方式)                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   public class UserService {                                │
│       // 对象自己创建依赖，强耦合！                         │
│       private UserDao userDao = new UserDaoImpl();          │
│       private EmailService email = new EmailServiceImpl();  │
│                                                             │
│       public void register(User user) {                     │
│           userDao.save(user);                               │
│           email.sendWelcome(user);                          │
│       }                                                     │
│   }                                                         │
│                                                             │
│   问题:                                                      │
│   • UserService 依赖具体实现类，无法替换                    │
│   • 单元测试困难，无法 mock                                 │
│   • 改变依赖需要修改代码                                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

```
┌─────────────────────────────────────────────────────────────┐
│                    使用 DI                                   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   @Service                                                  │
│   public class UserService {                                │
│       // 依赖由容器注入，依赖接口不依赖实现                 │
│       @Autowired                                            │
│       private UserDao userDao;                              │
│                                                             │
│       @Autowired                                            │
│       private EmailService email;                           │
│                                                             │
│       public void register(User user) {                     │
│           userDao.save(user);                               │
│           email.sendWelcome(user);                          │
│       }                                                     │
│   }                                                         │
│                                                             │
│   优点:                                                      │
│   • 面向接口编程，低耦合                                    │
│   • 易于测试，可以注入 mock 对象                            │
│   • 配置灵活，运行时可替换实现                              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## DI 的三种方式

### 1. 构造器注入（推荐）

```java
@Service
public class UserService {
    private final UserDao userDao;
    private final EmailService emailService;
    
    // 构造器注入 - 推荐方式
    @Autowired  // Spring 4.3+ 可省略
    public UserService(UserDao userDao, EmailService emailService) {
        this.userDao = userDao;
        this.emailService = emailService;
    }
}
```

```
┌─────────────────────────────────────────────────────────────┐
│                    构造器注入优点                            │
├─────────────────────────────────────────────────────────────┤
│   • 依赖不可变 (final)                                      │
│   • 依赖不为 null (必须传入)                                │
│   • 避免循环依赖 (编译期发现)                               │
│   • 方便单元测试 (不需要反射)                               │
│   • Spring 官方推荐                                         │
└─────────────────────────────────────────────────────────────┘
```

### 2. Setter 注入

```java
@Service
public class UserService {
    private UserDao userDao;
    
    // Setter 注入
    @Autowired
    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }
}
```

### 3. 字段注入（不推荐）

```java
@Service
public class UserService {
    // 字段注入 - 不推荐
    @Autowired
    private UserDao userDao;
    
    @Autowired
    private EmailService emailService;
}
```

```
┌─────────────────────────────────────────────────────────────┐
│                    字段注入缺点                              │
├─────────────────────────────────────────────────────────────┤
│   • 依赖可变、可能为 null                                   │
│   • 无法在构造时确保依赖完整                                │
│   • 隐藏依赖关系                                            │
│   • 单元测试必须用反射                                      │
│   • 掩盖了类承担过多职责的问题                              │
└─────────────────────────────────────────────────────────────┘
```

## 注入相关注解

```
┌─────────────────────────────────────────────────────────────┐
│                    注入注解对比                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   @Autowired (Spring)                                       │
│   ├── 按类型匹配                                            │
│   ├── 可用于字段、构造器、setter                            │
│   ├── 默认必须 (required=true)                              │
│   └── 多个实现时配合 @Qualifier                             │
│                                                             │
│   @Resource (JSR-250, JavaEE)                               │
│   ├── 默认按名称匹配                                        │
│   ├── 找不到再按类型匹配                                    │
│   └── @Resource(name = "userDaoImpl")                       │
│                                                             │
│   @Inject (JSR-330)                                         │
│   ├── 类似 @Autowired                                       │
│   ├── 按类型匹配                                            │
│   └── 需要 javax.inject 依赖                                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 多个实现的处理

```java
// 有多个 UserDao 实现
@Repository("mysqlUserDao")
public class MysqlUserDao implements UserDao { }

@Repository("mongoUserDao")
public class MongoUserDao implements UserDao { }

// 方式1: @Qualifier 指定
@Service
public class UserService {
    @Autowired
    @Qualifier("mysqlUserDao")
    private UserDao userDao;
}

// 方式2: @Primary 标记默认
@Repository
@Primary
public class MysqlUserDao implements UserDao { }

// 方式3: 按名称注入
@Service
public class UserService {
    @Autowired
    private UserDao mysqlUserDao;  // 变量名匹配 Bean 名
}
```

## DI 的好处

```
┌─────────────────────────────────────────────────────────────┐
│                    DI 的优势                                 │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 解耦                                                    │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  • 依赖接口，不依赖具体实现                          │  │
│   │  • 更换实现只需修改配置                              │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   2. 易于测试                                                │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  @Test                                               │  │
│   │  void testRegister() {                               │  │
│   │      UserDao mockDao = mock(UserDao.class);          │  │
│   │      EmailService mockEmail = mock(EmailService.class);│  │
│   │      UserService service = new UserService(mockDao, mockEmail);│  │
│   │      // 测试...                                      │  │
│   │  }                                                   │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   3. 配置灵活                                                │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  • 通过 Profile 切换实现                             │  │
│   │  • 通过配置文件控制行为                              │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   4. 单一职责                                                │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  • 对象不负责创建依赖                                │  │
│   │  • 专注于业务逻辑                                    │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> **DI（依赖注入）**是 IoC 的实现方式，将对象的依赖由 Spring 容器在运行时注入，而不是对象自己创建。注入方式有三种：1）**构造器注入**（推荐，依赖不可变、不为 null）；2）**Setter 注入**；3）**字段注入**（不推荐）。常用注解 @Autowired（按类型）、@Resource（按名称）。好处：解耦、易于测试、配置灵活。

### 1分钟版本

> **DI 是什么**：
> - Dependency Injection，依赖注入
> - IoC（控制反转）的具体实现
> - 对象的依赖由容器注入，不是自己 new
>
> **三种注入方式**：
> 1. **构造器注入**（推荐）
>    - 依赖 final，不可变
>    - 不会为 null
>    - 避免循环依赖
>    - 方便测试
>
> 2. **Setter 注入**：可选依赖时使用
>
> 3. **字段注入**：不推荐，测试不便
>
> **相关注解**：
> - @Autowired：按类型注入
> - @Qualifier：多实现时指定
> - @Resource：按名称注入
> - @Primary：标记默认实现
>
> **好处**：解耦、易测试、配置灵活

---

*关联文档：[spring-ioc.md](spring-ioc.md) | [spring-bean-lifecycle.md](spring-bean-lifecycle.md)*
