# Java Object 类中有什么方法，有什么作用？

## Object 类概述

```
┌─────────────────────────────────────────────────────────────┐
│                    Object 类                                 │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   定义: Java 中所有类的根类                                 │
│                                                             │
│   特点:                                                      │
│   • 所有类都直接或间接继承 Object                           │
│   • 提供通用方法供所有对象使用                              │
│   • 位于 java.lang 包                                       │
│                                                             │
│   Object 类方法 (11个):                                      │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  1. public boolean equals(Object obj)               │  │
│   │  2. public int hashCode()                           │  │
│   │  3. public String toString()                        │  │
│   │  4. public final Class<?> getClass()                │  │
│   │  5. protected Object clone()                        │  │
│   │  6. public final void notify()                      │  │
│   │  7. public final void notifyAll()                   │  │
│   │  8. public final void wait()                        │  │
│   │  9. public final void wait(long timeout)            │  │
│   │ 10. public final void wait(long timeout, int nanos) │  │
│   │ 11. protected void finalize() [已废弃]              │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 方法详解

### equals() 和 hashCode()

```java
// equals(): 判断两个对象是否相等
// 默认实现: 比较对象地址 (==)
public boolean equals(Object obj) {
    return (this == obj);
}

// 重写示例
@Override
public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    Person person = (Person) obj;
    return Objects.equals(name, person.name) && age == person.age;
}

// hashCode(): 返回对象的哈希码
// 用于 HashMap、HashSet 等
@Override
public int hashCode() {
    return Objects.hash(name, age);
}

// ⚠️ 重写规则:
// 1. equals 相等 → hashCode 必须相等
// 2. hashCode 相等 → equals 不一定相等
```

### toString()

```java
// toString(): 返回对象的字符串表示
// 默认实现: 类名@哈希码的十六进制
public String toString() {
    return getClass().getName() + "@" + Integer.toHexString(hashCode());
}

// 重写示例
@Override
public String toString() {
    return "Person{name='" + name + "', age=" + age + "}";
}
```

### getClass()

```java
// getClass(): 返回对象的运行时类
// final 方法，不能重写
Object obj = "Hello";
Class<?> clazz = obj.getClass();  // class java.lang.String

// 常用于:
// 1. 获取类信息
System.out.println(clazz.getName());  // java.lang.String

// 2. 反射
Method[] methods = clazz.getMethods();

// 3. equals 中比较类型
if (obj.getClass() != this.getClass()) return false;
```

### clone()

```java
// clone(): 创建对象的副本
// protected 方法，需要:
// 1. 类实现 Cloneable 接口
// 2. 重写 clone() 方法

public class Person implements Cloneable {
    private String name;
    private Address address;  // 引用类型
    
    @Override
    public Person clone() throws CloneNotSupportedException {
        Person cloned = (Person) super.clone();  // 浅拷贝
        cloned.address = address.clone();        // 深拷贝引用
        return cloned;
    }
}
```

### wait()、notify()、notifyAll()

```java
// 用于线程间通信，必须在 synchronized 块中调用

synchronized (lock) {
    // wait(): 当前线程等待，释放锁
    while (!condition) {
        lock.wait();
    }
    
    // notify(): 唤醒一个等待的线程
    lock.notify();
    
    // notifyAll(): 唤醒所有等待的线程
    lock.notifyAll();
}
```

### finalize() (已废弃)

```java
// finalize(): 对象被垃圾回收前调用
// ❌ Java 9 已废弃，不建议使用
// 问题:
// 1. 执行时机不确定
// 2. 可能导致对象复活
// 3. 性能开销大

// ✅ 替代方案:
// 1. try-with-resources
// 2. Cleaner (Java 9+)
```

## 方法分类

```
┌─────────────────────────────────────────────────────────────┐
│                    方法分类                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   对象比较:                                                  │
│   ├── equals()   比较对象内容                               │
│   └── hashCode() 返回哈希码                                 │
│                                                             │
│   对象信息:                                                  │
│   ├── toString() 字符串表示                                 │
│   └── getClass() 获取运行时类                               │
│                                                             │
│   对象复制:                                                  │
│   └── clone()    创建对象副本                               │
│                                                             │
│   线程通信 (3个):                                            │
│   ├── wait()     线程等待                                   │
│   ├── notify()   唤醒单个线程                               │
│   └── notifyAll() 唤醒所有线程                              │
│                                                             │
│   垃圾回收 (已废弃):                                         │
│   └── finalize() GC 前调用                                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> Object 类有 **11 个方法**：
> - **equals/hashCode**：比较对象、返回哈希码（重写必须一起）
> - **toString**：字符串表示
> - **getClass**：获取运行时类（final）
> - **clone**：对象复制（需实现 Cloneable）
> - **wait/notify/notifyAll**：线程通信（需在 synchronized 中）
> - **finalize**：已废弃

### 1分钟版本

> **对象比较**：
> - equals()：比较内容，默认比较地址
> - hashCode()：返回哈希码，用于 HashMap
> - 重写 equals 必须重写 hashCode
>
> **对象信息**：
> - toString()：字符串表示
> - getClass()：获取 Class 对象（final）
>
> **对象复制**：
> - clone()：需实现 Cloneable 接口
> - 默认浅拷贝
>
> **线程通信**：
> - wait()：等待并释放锁
> - notify()/notifyAll()：唤醒线程
> - 必须在 synchronized 块中
>
> **已废弃**：
> - finalize()：用 Cleaner 替代

---

*关联文档：[hashcode-equals.md](hashcode-equals.md) | [wait-vs-sleep.md](wait-vs-sleep.md) | [deep-shallow-copy.md](deep-shallow-copy.md)*
