# 什么是 Java 的封装特性？

## 封装概述

```
┌─────────────────────────────────────────────────────────────┐
│                    Java 封装                                 │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   定义: 将数据和操作数据的方法绑定在一起，                  │
│        对外隐藏内部实现细节                                 │
│                                                             │
│   核心思想: "隐藏信息，暴露接口"                            │
│                                                             │
│   实现方式:                                                  │
│   • private 修饰属性 (隐藏数据)                             │
│   • public getter/setter (控制访问)                         │
│                                                             │
│   ┌───────────────────────────────────────────┐            │
│   │              封装的类                      │            │
│   │  ┌─────────────────────────────────────┐  │            │
│   │  │  private 属性 (内部隐藏)            │  │            │
│   │  │  ─────────────────────────────────  │  │            │
│   │  │  - name                             │  │            │
│   │  │  - age                              │  │            │
│   │  │  - salary                           │  │            │
│   │  └─────────────────────────────────────┘  │            │
│   │                   ↑↓                      │            │
│   │  ┌─────────────────────────────────────┐  │            │
│   │  │  public 方法 (对外接口)             │  │ ← 外部访问 │
│   │  │  ─────────────────────────────────  │  │            │
│   │  │  + getName()                        │  │            │
│   │  │  + setAge(int age)                  │  │            │
│   │  │  + getSalary()                      │  │            │
│   │  └─────────────────────────────────────┘  │            │
│   └───────────────────────────────────────────┘            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 代码示例

```java
// 封装示例
public class Person {
    // private 修饰属性，外部不能直接访问
    private String name;
    private int age;
    private double salary;
    
    // 构造器
    public Person(String name, int age) {
        this.name = name;
        setAge(age);  // 使用 setter 验证
    }
    
    // getter: 获取属性值
    public String getName() {
        return name;
    }
    
    // setter: 设置属性值，可加入验证逻辑
    public void setAge(int age) {
        if (age < 0 || age > 150) {
            throw new IllegalArgumentException("Invalid age: " + age);
        }
        this.age = age;
    }
    
    public int getAge() {
        return age;
    }
    
    // 只读属性: 只提供 getter
    public double getSalary() {
        return salary;
    }
    
    // 内部计算方法
    public double getAnnualSalary() {
        return salary * 12;
    }
}

// 使用
Person person = new Person("John", 25);
person.setAge(30);          // 通过 setter 修改
System.out.println(person.getAge());  // 通过 getter 获取

// person.age = -1;  // ❌ 编译错误，不能直接访问 private
// person.setAge(-1); // ❌ 运行时抛异常，验证失败
```

## 封装的好处

```
┌─────────────────────────────────────────────────────────────┐
│                    封装的好处                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 数据安全                                                │
│      └── 通过 setter 验证，防止非法数据                     │
│                                                             │
│   2. 隐藏实现细节                                            │
│      └── 内部修改不影响外部调用                             │
│                                                             │
│   3. 控制访问                                                │
│      └── 可设置只读、只写、可读写                           │
│                                                             │
│   4. 便于维护                                                │
│      └── 修改内部逻辑无需改变接口                           │
│                                                             │
│   5. 增加灵活性                                              │
│      └── getter/setter 中可加入日志、缓存等逻辑            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 访问控制级别

```java
// 从严格到宽松
private   // 仅本类可访问
default   // 同包可访问 (不写修饰符)
protected // 同包 + 子类可访问
public    // 所有类可访问

// 封装建议
// 1. 属性用 private
// 2. getter/setter 用 public
// 3. 内部方法用 private
// 4. 继承需要的方法用 protected
```

## 封装最佳实践

```java
// 1. 防御性拷贝 (可变对象)
public class Employee {
    private Date hireDate;
    
    public Date getHireDate() {
        return new Date(hireDate.getTime());  // 返回副本
    }
    
    public void setHireDate(Date date) {
        this.hireDate = new Date(date.getTime());  // 存储副本
    }
}

// 2. 不可变类
public final class ImmutablePerson {
    private final String name;
    private final int age;
    
    public ImmutablePerson(String name, int age) {
        this.name = name;
        this.age = age;
    }
    
    public String getName() { return name; }
    public int getAge() { return age; }
    // 无 setter
}

// 3. Builder 模式 (多参数)
Person person = Person.builder()
    .name("John")
    .age(25)
    .build();
```

## 面试回答

### 30秒版本

> 封装是将数据和操作数据的方法绑定，**隐藏内部实现**。实现方式：**private 修饰属性**，通过 **public getter/setter** 控制访问。好处：数据安全（setter 验证）、隐藏实现细节（内部修改不影响外部）、控制访问权限（只读/只写）。是面向对象三大特性之一。

### 1分钟版本

> **定义**：隐藏数据，暴露接口
>
> **实现方式**：
> - private 修饰属性
> - public getter/setter 方法
>
> **好处**：
> - 数据安全：setter 加验证
> - 隐藏实现：内部修改不影响外部
> - 控制访问：只读、只写、读写
> - 便于维护：接口稳定
>
> **最佳实践**：
> - 可变对象做防御性拷贝
> - 不可变类更安全
> - 多参数用 Builder 模式

---

*关联文档：[java-access-modifiers.md](java-access-modifiers.md) | [java-inheritance.md](java-inheritance.md)*
