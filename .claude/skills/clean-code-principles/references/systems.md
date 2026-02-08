# 系统 (Systems)

> 基于《Clean Code》第11章 - Robert C. Martin (by Dr. Kevin Dean Wampler)

## 核心思想

> "复杂性会杀死人。它让开发者的生活变得困难，让产品难以计划、构建和测试。"
> — Ray Ozzie, CTO, Microsoft Corporation

**如何建造一座城市？** 你能自己管理所有细节吗？可能不行。城市之所以能运转，是因为有不同的团队管理城市的不同部分——水系统、电力系统、交通、执法、建筑规范等。城市也因为有适当的抽象层次和模块化而运转。

软件系统也应如此。整洁代码帮助我们在较低抽象层次实现这一点。本章讨论如何在更高抽象层次——系统层次——保持整洁。

---

## 规则 1：将构造与使用分离 (Separate Constructing from Using)

### 核心原则

```
┌─────────────────────────────────────────────────────────────┐
│  构造是一个与使用完全不同的过程                               │
│                                                             │
│  软件系统应该将启动过程（对象构造和依赖装配）                  │
│  与启动后的运行时逻辑分离开来                                 │
└─────────────────────────────────────────────────────────────┘
```

### ❌ 问题示例：延迟初始化

```java
public Service getService() {
    if (service == null)
        service = new MyServiceImpl(…); // 大多数情况下够用的默认值?
    return service;
}
```

**优点**：
- 不使用对象就不产生构造开销
- 启动时间可能更快
- 确保不会返回 null

**问题**：
- 硬编码依赖于 `MyServiceImpl` 及其构造器所需的一切
- 即使运行时从不使用该类型的对象，也无法编译
- 测试困难——需要在单元测试期间分配测试替身
- 构造逻辑与运行时处理混合，需要测试所有执行路径
- 违反单一职责原则
- 不知道 `MyServiceImpl` 是否在所有情况下都是正确的对象

### 解决方案 1：分离 Main

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│    main                    │              应用程序           │
│  ─────────────────────────│────────────────────────────────│
│                           │                                │
│  构造对象                   │       使用对象                  │
│  装配依赖                   │       执行业务逻辑              │
│                           │                                │
│         ──────────────────→                                │
│               依赖方向                                       │
└─────────────────────────────────────────────────────────────┘
```

- main 函数构建系统所需的对象，然后传递给应用程序
- 应用程序只是使用它们
- **所有依赖箭头都从 main 指向应用程序**
- 应用程序不知道 main 或构造过程

### 解决方案 2：工厂模式

当应用程序需要控制对象**何时**被创建时：

```java
// 接口在应用程序侧
public interface LineItemFactory {
    LineItem makeLineItem();
}

// 实现在 main 侧
public class LineItemFactoryImplementation implements LineItemFactory {
    public LineItem makeLineItem() {
        return new LineItem();  // 构造细节
    }
}

// 应用程序使用工厂
public class OrderProcessing {
    private LineItemFactory factory;
    
    public void addLineItem(Order order) {
        LineItem item = factory.makeLineItem();  // 应用程序控制何时创建
        order.addItem(item);
    }
}
```

### 解决方案 3：依赖注入 (DI)

```
┌─────────────────────────────────────────────────────────────┐
│  依赖注入 (DI) = 控制反转 (IoC) 应用于依赖管理                 │
│                                                             │
│  对象不应负责实例化自己的依赖                                  │
│  而应将此责任传递给另一个"权威"机制                            │
└─────────────────────────────────────────────────────────────┘
```

#### JNDI 查找（部分 DI）

```java
MyService myService = (MyService)(jndiContext.lookup("NameOfMyService"));
```

调用对象不控制返回什么对象，但仍然**主动**解析依赖。

#### 真正的依赖注入

```java
// 类完全被动，不采取任何步骤解析依赖
public class MyComponent {
    private MyService service;
    
    // 通过构造器注入
    public MyComponent(MyService service) {
        this.service = service;
    }
    
    // 或通过 setter 注入
    public void setService(MyService service) {
        this.service = service;
    }
}
```

DI 容器在构造过程中：
1. 实例化所需对象（通常按需）
2. 使用构造器参数或 setter 方法装配依赖
3. 通过配置文件或专用构造模块指定实际使用哪些依赖

---

## 规则 2：扩展系统 (Scaling Up)

### 城市类比

```
城镇 → 小城市 → 大城市
  ↓
道路从窄变宽
  ↓
小建筑被大建筑取代
  ↓
服务（电力、水、网络）随人口增加而添加
```

> "为什么他们第一次不把路修宽一点？" 
> 
> 因为没法预先证明在小镇中心修一条六车道高速公路的费用是合理的。

### 软件系统可以增量成长

```
┌─────────────────────────────────────────────────────────────┐
│  神话：我们能第一次就把系统做对                               │
│                                                             │
│  现实：应该只实现今天的故事，然后重构和扩展系统                │
│       以实现明天的新故事                                     │
│                                                             │
│  这就是迭代和增量式敏捷的精髓                                 │
└─────────────────────────────────────────────────────────────┘
```

**关键条件**：如果我们保持适当的关注点分离，软件架构可以增量成长。

---

## 规则 3：横切关注点 (Cross-Cutting Concerns)

### 什么是横切关注点？

像持久化、事务、安全这样的关注点往往**跨越**领域对象的自然边界。

```
┌─────────────────────────────────────────────────────────────┐
│  你想让所有对象使用相同的持久化策略：                          │
│  • 使用特定的 DBMS vs 文件                                  │
│  • 遵循表和列的命名约定                                      │
│  • 使用一致的事务语义                                        │
│                                                             │
│  结果：相同的代码分散在多个对象中                             │
└─────────────────────────────────────────────────────────────┘
```

### 面向切面编程 (AOP)

AOP 中，称为**切面**的模块化构造指定系统中哪些点应该以一致的方式修改行为以支持特定关注点。

---

## 规则 4：使用 POJO (Plain Old Java Objects)

### ❌ EJB2 的问题

```java
// EJB2 实体 Bean - 业务逻辑与容器紧密耦合
public abstract class Bank implements javax.ejb.EntityBean {
    // 业务逻辑
    public abstract String getStreetAddr1();
    public abstract void setStreetAddr1(String street1);
    public abstract Collection getAccounts();
    
    // 必须实现的容器逻辑
    public abstract void setId(Integer id);
    public abstract Integer getId();
    public Integer ejbCreate(Integer id) { … }
    public void ejbPostCreate(Integer id) { … }
    public void setEntityContext(EntityContext ctx) {}
    public void unsetEntityContext() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void ejbLoad() {}
    public void ejbStore() {}
    public void ejbRemove() {}
}
```

**问题**：
- 必须继承容器类型
- 必须提供容器要求的多个生命周期方法
- 隔离单元测试困难
- 在 EJB2 架构外无法复用
- 面向对象编程被削弱

### ✅ Spring/EJB3 的 POJO 方式

```java
// EJB3 使用注解的 POJO
@Entity
@Table(name = "BANKS")
public class Bank implements java.io.Serializable {
    @Id @GeneratedValue(strategy=GenerationType.AUTO)
    private int id;

    @Embeddable
    public class Address {
        protected String streetAddr1;
        protected String streetAddr2;
        protected String city;
        protected String state;
        protected String zipCode;
    }
    
    @Embedded
    private Address address;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy="bank")
    private Collection<Account> accounts = new ArrayList<Account>();

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public void addAccount(Account account) {
        account.setBank(this);
        accounts.add(account);
    }
    
    public Collection<Account> getAccounts() { return accounts; }
    public void setAccounts(Collection<Account> accounts) { this.accounts = accounts; }
}
```

**优势**：
- 代码比 EJB2 干净得多
- 实体细节包含在注解中
- 注解外的代码干净、清晰
- 易于测试驱动和维护

### Spring 配置示例

```xml
<beans>
    <bean id="appDataSource"
          class="org.apache.commons.dbcp.BasicDataSource"
          destroy-method="close"
          p:driverClassName="com.mysql.jdbc.Driver"
          p:url="jdbc:mysql://localhost:3306/mydb"
          p:username="me"/>

    <bean id="bankDataAccessObject"
          class="com.example.banking.persistence.BankDataAccessObject"
          p:dataSource-ref="appDataSource"/>

    <bean id="bank"
          class="com.example.banking.model.Bank"
          p:dataAccessObject-ref="bankDataAccessObject"/>
</beans>
```

### 俄罗斯套娃式装饰器

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│    客户端调用 bank.getAccounts()                            │
│              ↓                                              │
│    ┌─────────────────────────────────────┐                 │
│    │  JDBC 数据源代理                     │                 │
│    │  ┌─────────────────────────────┐    │                 │
│    │  │  DAO 代理                    │    │                 │
│    │  │  ┌─────────────────────┐    │    │                 │
│    │  │  │  Bank POJO          │    │    │                 │
│    │  │  └─────────────────────┘    │    │                 │
│    │  └─────────────────────────────┘    │                 │
│    └─────────────────────────────────────┘                 │
│                                                             │
│  可以添加其他装饰器：事务、缓存等                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 规则 5：测试驱动系统架构

### 核心观点

```
┌─────────────────────────────────────────────────────────────┐
│  如果能用 POJO 编写应用程序的领域逻辑                         │
│  在代码层面与任何架构关注点解耦                               │
│  那么就可能真正地测试驱动架构                                 │
│                                                             │
│  可以根据需要从简单演进到复杂                                 │
│  按需采用新技术                                              │
└─────────────────────────────────────────────────────────────┘
```

### 不需要大设计优先 (BDUF)

**BDUF 有害**：
- 抑制适应变化
- 因为不愿丢弃先前的努力而产生心理阻力
- 架构选择影响后续设计思考

**软件 vs 建筑**：
- 建筑师必须做 BDUF，因为施工开始后无法做出激进的架构变更
- 软件可以在结构适当分离关注点的情况下进行经济可行的激进变更

### 简单开始，增量扩展

```
从"天真简单"但良好解耦的架构开始
         ↓
快速交付可工作的用户故事
         ↓
根据规模增长添加更多基础设施
         ↓
在每个抽象层次和范围保持适当简单
```

---

## 规则 6：优化决策制定

### 模块化使分散决策成为可能

```
┌─────────────────────────────────────────────────────────────┐
│  在足够大的系统中，无论是城市还是软件项目                      │
│  没有一个人能做出所有决策                                     │
│                                                             │
│  我们都知道最好把责任交给最有资格的人                         │
│  我们常常忘记：最好把决策推迟到最后可能的时刻                   │
└─────────────────────────────────────────────────────────────┘
```

### 为什么推迟决策？

| 过早决策 | 延迟决策 |
|---------|---------|
| 用次优知识做出 | 用最佳信息做出知情选择 |
| 更少的客户反馈 | 更多客户反馈 |
| 更少的项目反思 | 更多项目思考时间 |
| 更少的实施经验 | 更多实施选择经验 |

> 这不是懒惰或不负责任；这让我们能用最佳信息做出知情选择。

---

## 规则 7：明智使用标准

### 标准的价值

- 更容易复用想法和组件
- 更容易招聘有相关经验的人
- 封装好的想法
- 更容易连接组件

### 标准的问题

```
┌─────────────────────────────────────────────────────────────┐
│  许多团队使用 EJB2 架构因为它是标准                           │
│  即使更轻量、更直接的设计就足够了                             │
│                                                             │
│  问题：                                                     │
│  • 创建标准的过程有时太长                                    │
│  • 某些标准与实际需求脱节                                    │
│  • 团队可能过度沉迷于被大肆宣传的标准                         │
│  • 失去对为客户实现价值的关注                                │
└─────────────────────────────────────────────────────────────┘
```

---

## 规则 8：领域特定语言 (DSL)

### 什么是 DSL？

独立的小型脚本语言或标准语言中的 API，允许代码以领域专家可能编写的结构化散文形式书写。

### DSL 的价值

```
┌─────────────────────────────────────────────────────────────┐
│  好的 DSL 最小化领域概念与实现代码之间的"沟通鸿沟"            │
│                                                             │
│  如果你用领域专家使用的相同语言实现领域逻辑                    │
│  错误翻译领域到实现的风险就会降低                             │
│                                                             │
│  DSL 将抽象层次提升到代码惯用法和设计模式之上                  │
│  允许开发者在适当的抽象层次揭示代码意图                       │
└─────────────────────────────────────────────────────────────┘
```

---

## Java 代理示例

### JDK 动态代理

```java
// 接口
public interface Bank {
    Collection<Account> getAccounts();
    void setAccounts(Collection<Account> accounts);
}

// POJO 实现
public class BankImpl implements Bank {
    private List<Account> accounts;
    
    public Collection<Account> getAccounts() { 
        return accounts; 
    }
    
    public void setAccounts(Collection<Account> accounts) { 
        this.accounts = new ArrayList<Account>(); 
        for (Account account: accounts) {
            this.accounts.add(account);
        }
    }
}

// 调用处理器
public class BankProxyHandler implements InvocationHandler {
    private Bank bank;
    
    public BankHandler(Bank bank) {
        this.bank = bank;
    }
    
    public Object invoke(Object proxy, Method method, Object[] args) 
            throws Throwable {
        String methodName = method.getName();
        if (methodName.equals("getAccounts")) {
            bank.setAccounts(getAccountsFromDatabase());
            return bank.getAccounts();
        } else if (methodName.equals("setAccounts")) {
            bank.setAccounts((Collection<Account>) args[0]);
            setAccountsToDatabase(bank.getAccounts());
            return null;
        } else {
            // ...
        }
    }
    
    protected Collection<Account> getAccountsFromDatabase() { /* ... */ }
    protected void setAccountsToDatabase(Collection<Account> accounts) { /* ... */ }
}

// 创建代理
Bank bank = (Bank) Proxy.newProxyInstance(
    Bank.class.getClassLoader(), 
    new Class[] { Bank.class },
    new BankProxyHandler(new BankImpl())
);
```

**问题**：代码量大、复杂，难以创建整洁代码。

**解决方案**：使用 Spring AOP、JBoss AOP 或 AspectJ 等框架。

---

## 最优系统架构

```
┌─────────────────────────────────────────────────────────────┐
│  最优系统架构由模块化的关注点领域组成                          │
│  每个领域用普通老式 Java（或其他）对象实现                     │
│  不同领域用最小侵入性的切面或类切面工具集成在一起               │
│  这种架构可以像代码一样被测试驱动                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 代码审查清单

### 构造与使用分离
- [ ] 启动过程是否与运行时逻辑分离？
- [ ] 是否避免了延迟初始化与业务逻辑混合？
- [ ] 是否使用工厂或 DI 管理对象创建？

### 关注点分离
- [ ] 横切关注点（持久化、事务、安全）是否使用切面处理？
- [ ] 业务逻辑是否作为纯 POJO 实现？
- [ ] 领域对象是否与基础设施关注点解耦？

### 可扩展性
- [ ] 架构是否允许增量成长？
- [ ] 是否避免了不必要的大设计优先？
- [ ] 新功能是否可以通过扩展而非修改添加？

### 决策制定
- [ ] 决策是否尽可能推迟？
- [ ] 是否基于最新知识做出决策？
- [ ] 标准的使用是否增加了可证明的价值？

### 测试
- [ ] 系统架构是否可以测试驱动？
- [ ] POJO 是否可以独立于框架测试？
- [ ] 依赖是否可以轻松替换为测试替身？

---

## 核心箴言

> **软件系统应该将启动过程（对象构造和依赖装配）与启动后的运行时逻辑分离开来。**

> **最优系统架构由模块化的关注点领域组成，每个领域用 POJO 实现。**

> **我们无法第一次就把系统做对。应该只实现今天的故事，然后重构和扩展系统以实现明天的新故事。**

> **最好把决策推迟到最后可能的时刻。这不是懒惰或不负责任；这让我们能用最佳信息做出知情选择。**

> **系统也必须整洁。侵入性架构会淹没领域逻辑并影响敏捷性。永远不要忘记使用能工作的最简单方案。**
