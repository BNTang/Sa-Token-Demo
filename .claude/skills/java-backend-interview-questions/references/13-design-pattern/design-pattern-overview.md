# 设计模式概述

> 分类: 设计模式 | 难度: ⭐⭐⭐ | 频率: 高频

---

## 一、什么是设计模式

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          设计模式定义                                             │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  设计模式是软件开发中针对常见问题的可复用解决方案模板。                           │
│                                                                                  │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │                                                                            │ │
│  │  "每一个模式描述了一个在我们周围不断重复发生的问题，                        │ │
│  │   以及该问题的解决方案的核心。"                                             │ │
│  │                                       —— GoF《设计模式》                   │ │
│  │                                                                            │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  设计模式的作用:                                                                 │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │  1. 提高代码复用性 - 避免重复造轮子                                         │ │
│  │  2. 提高可维护性 - 统一的设计语言和结构                                     │ │
│  │  3. 提高可扩展性 - 符合开闭原则，易于扩展                                   │ │
│  │  4. 提高沟通效率 - 开发者之间的通用语言                                     │ │
│  │  5. 降低耦合度 - 面向接口编程                                               │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 二、设计模式分类

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          23种设计模式分类                                         │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  创建型模式 (5种) - 对象创建                                                     │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │  单例模式 (Singleton)     确保一个类只有一个实例                            │ │
│  │  工厂方法 (Factory Method) 定义创建对象的接口                               │ │
│  │  抽象工厂 (Abstract Factory) 创建相关对象的家族                             │ │
│  │  建造者模式 (Builder)     分步骤构建复杂对象                                │ │
│  │  原型模式 (Prototype)     通过复制创建对象                                  │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  结构型模式 (7种) - 类/对象的组合                                                │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │  代理模式 (Proxy)         控制对象访问                                      │ │
│  │  适配器模式 (Adapter)     接口转换                                          │ │
│  │  装饰器模式 (Decorator)   动态添加功能                                      │ │
│  │  外观模式 (Facade)        简化复杂系统接口                                  │ │
│  │  桥接模式 (Bridge)        分离抽象和实现                                    │ │
│  │  组合模式 (Composite)     树形结构                                          │ │
│  │  享元模式 (Flyweight)     共享对象减少内存                                  │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  行为型模式 (11种) - 对象间的通信                                                │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │  策略模式 (Strategy)      算法可替换                                        │ │
│  │  模板方法 (Template Method) 定义算法骨架                                    │ │
│  │  观察者模式 (Observer)    事件通知                                          │ │
│  │  责任链模式 (Chain of Responsibility) 请求传递                              │ │
│  │  命令模式 (Command)       封装请求                                          │ │
│  │  状态模式 (State)         状态驱动行为                                      │ │
│  │  迭代器模式 (Iterator)    遍历集合                                          │ │
│  │  中介者模式 (Mediator)    对象间解耦                                        │ │
│  │  备忘录模式 (Memento)     保存/恢复状态                                     │ │
│  │  访问者模式 (Visitor)     分离数据结构和操作                                │ │
│  │  解释器模式 (Interpreter) 语言解释器                                        │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 三、常用设计模式示例

### 3.1 单例模式

```java
/**
 * 单例模式 - 双重检查锁
 */
public class Singleton {
    private static volatile Singleton instance;
    
    private Singleton() {}
    
    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class) {
                if (instance == null) {
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}

// 应用: Spring Bean 默认单例、线程池、配置管理器
```

### 3.2 工厂模式

```java
/**
 * 工厂方法模式
 */
public interface PaymentFactory {
    Payment createPayment();
}

public class AlipayFactory implements PaymentFactory {
    @Override
    public Payment createPayment() {
        return new Alipay();
    }
}

public class WechatPayFactory implements PaymentFactory {
    @Override
    public Payment createPayment() {
        return new WechatPay();
    }
}

// 应用: Spring BeanFactory、日志工厂、连接池
```

### 3.3 策略模式

```java
/**
 * 策略模式
 */
public interface DiscountStrategy {
    double calculate(double price);
}

public class VipDiscount implements DiscountStrategy {
    @Override
    public double calculate(double price) {
        return price * 0.8;
    }
}

public class NormalDiscount implements DiscountStrategy {
    @Override
    public double calculate(double price) {
        return price * 0.95;
    }
}

public class OrderService {
    private DiscountStrategy discountStrategy;
    
    public void setDiscountStrategy(DiscountStrategy strategy) {
        this.discountStrategy = strategy;
    }
    
    public double pay(double price) {
        return discountStrategy.calculate(price);
    }
}

// 应用: 支付方式选择、排序算法、登录验证
```

### 3.4 代理模式

```java
/**
 * 代理模式 - 静态代理
 */
public interface UserService {
    void save(User user);
}

public class UserServiceImpl implements UserService {
    @Override
    public void save(User user) {
        System.out.println("保存用户");
    }
}

public class UserServiceProxy implements UserService {
    private UserService target;
    
    public UserServiceProxy(UserService target) {
        this.target = target;
    }
    
    @Override
    public void save(User user) {
        System.out.println("开始事务");
        target.save(user);
        System.out.println("提交事务");
    }
}

// 应用: Spring AOP、MyBatis Mapper、RPC 调用
```

---

## 四、设计原则

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          SOLID 原则                                               │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  S - 单一职责原则 (Single Responsibility)                                        │
│      一个类只负责一件事                                                          │
│                                                                                  │
│  O - 开闭原则 (Open/Closed)                                                      │
│      对扩展开放，对修改关闭                                                      │
│                                                                                  │
│  L - 里氏替换原则 (Liskov Substitution)                                          │
│      子类可以替换父类                                                            │
│                                                                                  │
│  I - 接口隔离原则 (Interface Segregation)                                        │
│      多个专门接口优于一个通用接口                                                │
│                                                                                  │
│  D - 依赖倒置原则 (Dependency Inversion)                                         │
│      依赖抽象，不依赖具体实现                                                    │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 五、面试回答

### 30秒版本

> **设计模式**是针对常见问题的可复用解决方案模板。
>
> **作用**：提高代码复用性、可维护性、可扩展性，降低耦合，是开发者的通用语言。
>
> **分类**（23种）：
> - 创建型（5种）：单例、工厂、建造者等
> - 结构型（7种）：代理、适配器、装饰器等
> - 行为型（11种）：策略、观察者、模板方法等
>
> 常用的有单例、工厂、策略、代理、模板方法、观察者模式。

### 1分钟版本

> **什么是设计模式**：
> 是软件开发中针对常见问题的可复用解决方案，是前人经验的总结。
>
> **作用**：
> - 提高代码复用性和可维护性
> - 提高可扩展性（符合开闭原则）
> - 降低耦合度（面向接口编程）
> - 提高沟通效率（通用语言）
>
> **分类**：
> - **创建型**（5种）：对象创建，如单例、工厂、建造者
> - **结构型**（7种）：类/对象组合，如代理、适配器、装饰器
> - **行为型**（11种）：对象通信，如策略、观察者、责任链
>
> **常用模式**：
> - 单例：全局唯一实例（Spring Bean）
> - 工厂：对象创建解耦（BeanFactory）
> - 策略：算法可替换（支付方式选择）
> - 代理：增强功能（Spring AOP）
> - 模板方法：定义骨架（JdbcTemplate）
> - 观察者：事件通知（Spring Event）
>
> **设计原则**：SOLID（单一职责、开闭、里氏替换、接口隔离、依赖倒置）
