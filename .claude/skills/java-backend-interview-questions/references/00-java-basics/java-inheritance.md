# 什么是 Java 中的继承机制？

## 继承概述

```
┌─────────────────────────────────────────────────────────────┐
│                    Java 继承机制                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   定义: 子类获取父类的属性和方法的机制                      │
│                                                             │
│   关键字: extends                                            │
│                                                             │
│   特点:                                                      │
│   • Java 单继承 (一个类只能继承一个父类)                    │
│   • 支持多层继承 (A → B → C)                                │
│   • 所有类默认继承 Object                                   │
│                                                             │
│   ┌───────────────────┐                                     │
│   │      Animal       │  父类/基类/超类                     │
│   │  ─────────────────│                                     │
│   │  - name           │                                     │
│   │  + eat()          │                                     │
│   └─────────┬─────────┘                                     │
│             │ extends                                        │
│   ┌─────────┴─────────┐                                     │
│   │       Dog         │  子类/派生类                        │
│   │  ─────────────────│                                     │
│   │  - breed          │  子类特有属性                       │
│   │  + bark()         │  子类特有方法                       │
│   │  + eat() 重写     │  可以重写父类方法                   │
│   └───────────────────┘                                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 代码示例

```java
// 父类
public class Animal {
    protected String name;
    
    public Animal(String name) {
        this.name = name;
    }
    
    public void eat() {
        System.out.println(name + " is eating");
    }
    
    public void sleep() {
        System.out.println(name + " is sleeping");
    }
}

// 子类继承父类
public class Dog extends Animal {
    private String breed;
    
    public Dog(String name, String breed) {
        super(name);  // 调用父类构造器
        this.breed = breed;
    }
    
    // 子类特有方法
    public void bark() {
        System.out.println(name + " is barking");
    }
    
    // 重写父类方法
    @Override
    public void eat() {
        System.out.println(name + " is eating dog food");
    }
}

// 使用
Dog dog = new Dog("Buddy", "Golden");
dog.eat();    // Buddy is eating dog food (重写的方法)
dog.sleep();  // Buddy is sleeping (继承的方法)
dog.bark();   // Buddy is barking (子类方法)
```

## 继承的关键点

### super 关键字

```java
public class Dog extends Animal {
    
    public Dog(String name) {
        super(name);  // 调用父类构造器，必须是第一行
    }
    
    @Override
    public void eat() {
        super.eat();  // 调用父类方法
        System.out.println("And some bones");
    }
    
    public void showName() {
        System.out.println(super.name);  // 访问父类属性
    }
}
```

### 方法重写规则

```
┌─────────────────────────────────────────────────────────────┐
│                    方法重写规则                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 方法名、参数列表必须相同                               │
│   2. 返回类型相同或是子类型 (协变返回)                      │
│   3. 访问权限不能更小 (public → private ❌)                 │
│   4. 不能抛出更大的异常                                     │
│   5. final 方法不能重写                                     │
│   6. static 方法不能重写 (只是隐藏)                         │
│   7. private 方法不能重写 (子类不可见)                      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 构造器调用链

```java
class A {
    A() { System.out.println("A"); }
}

class B extends A {
    B() { 
        super();  // 隐式调用
        System.out.println("B"); 
    }
}

class C extends B {
    C() { 
        super();  // 隐式调用
        System.out.println("C"); 
    }
}

new C();
// 输出: A B C (从父到子依次执行)
```

## 继承的限制

```
┌─────────────────────────────────────────────────────────────┐
│                    继承的限制                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   不能继承:                                                  │
│   • final 类不能被继承                                      │
│   • 构造器不能被继承 (但可以调用)                           │
│   • private 成员不能被继承 (不可见)                         │
│                                                             │
│   Java 单继承原因:                                           │
│   • 避免菱形继承问题                                        │
│   • 简化语言设计                                            │
│   • 可通过接口实现多继承效果                                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 继承 vs 组合

```java
// 继承: is-a 关系
class Dog extends Animal { }  // 狗是动物

// 组合: has-a 关系
class Car {
    private Engine engine;  // 汽车有引擎
}

// 组合更灵活，优先考虑组合
// "组合优于继承" 原则
```

## 面试回答

### 30秒版本

> 继承是子类获取父类属性和方法的机制，用 `extends` 关键字。Java **单继承**，支持多层继承。子类可用 `super` 调用父类构造器和方法，可 `@Override` 重写方法。重写规则：方法签名相同、访问权限不能更小、final/private/static 方法不能重写。

### 1分钟版本

> **定义**：子类获取父类属性和方法
>
> **特点**：
> - 单继承（只能 extends 一个类）
> - 多层继承（A→B→C）
> - 默认继承 Object
>
> **关键字**：
> - extends：声明继承
> - super：调用父类构造器/方法
> - @Override：标记重写
>
> **重写规则**：
> - 方法名、参数相同
> - 访问权限不能更小
> - final/static 不能重写
>
> **设计原则**：
> - 继承: is-a 关系
> - 组合优于继承

---

*关联文档：[java-polymorphism.md](java-polymorphism.md) | [java-encapsulation.md](java-encapsulation.md)*
