# Java 的 Optional 类是什么？它有什么用？

## Optional 概述

```
┌─────────────────────────────────────────────────────────────┐
│                    Optional 类                               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   定义: 一个容器类，表示值可能存在也可能不存在              │
│                                                             │
│   引入版本: Java 8                                          │
│                                                             │
│   目的:                                                      │
│   • 避免 NullPointerException                               │
│   • 强制开发者处理空值情况                                  │
│   • 使代码更具可读性                                        │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  Optional<T>                                         │  │
│   │  ├── 有值: Optional.of(value)                        │  │
│   │  └── 无值: Optional.empty()                          │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 创建 Optional

```java
// 1. 创建包含值的 Optional
Optional<String> opt1 = Optional.of("Hello");  // 值不能为 null

// 2. 创建可能为空的 Optional
Optional<String> opt2 = Optional.ofNullable(null);  // 值可以为 null
Optional<String> opt3 = Optional.ofNullable("World");

// 3. 创建空的 Optional
Optional<String> opt4 = Optional.empty();

// ❌ 错误: of() 传 null 会抛 NullPointerException
Optional<String> opt5 = Optional.of(null);  // 抛异常!
```

## 常用方法

```java
Optional<String> opt = Optional.ofNullable(getValue());

// 1. 判断是否有值
if (opt.isPresent()) {
    System.out.println(opt.get());
}

// 2. 如果有值则执行操作 (推荐)
opt.ifPresent(value -> System.out.println(value));

// 3. 获取值，无值返回默认值
String result = opt.orElse("default");

// 4. 获取值，无值通过 Supplier 生成默认值
String result = opt.orElseGet(() -> computeDefault());

// 5. 获取值，无值抛出异常
String result = opt.orElseThrow(() -> new RuntimeException("No value"));

// Java 10+
String result = opt.orElseThrow();  // 抛 NoSuchElementException
```

## 链式操作

```java
// map: 转换值
Optional<String> name = Optional.of("  John  ");
Optional<String> upper = name.map(String::trim)
                             .map(String::toUpperCase);
// 结果: Optional["JOHN"]

// flatMap: 用于返回 Optional 的函数
Optional<User> user = findUserById(1);
Optional<String> email = user.flatMap(User::getEmail);  // getEmail 返回 Optional

// filter: 过滤
Optional<Integer> age = Optional.of(25);
Optional<Integer> adult = age.filter(a -> a >= 18);  // 有值
Optional<Integer> child = age.filter(a -> a < 18);   // empty
```

## 正确使用方式

```java
// ❌ 错误: 使用 get() 前不检查
String value = optional.get();  // 可能抛 NoSuchElementException

// ❌ 错误: 用 Optional 作为字段
public class User {
    private Optional<String> email;  // 不推荐
}

// ❌ 错误: 用 Optional 作为方法参数
public void process(Optional<String> input) {  // 不推荐
}

// ✅ 正确: 用作方法返回值
public Optional<User> findById(Long id) {
    User user = userMap.get(id);
    return Optional.ofNullable(user);
}

// ✅ 正确: 链式处理
String city = getUserById(1)
    .map(User::getAddress)
    .map(Address::getCity)
    .orElse("Unknown");
```

## 替代 null 检查

```java
// ❌ 传统 null 检查 (繁琐)
public String getCity(User user) {
    if (user != null) {
        Address address = user.getAddress();
        if (address != null) {
            return address.getCity();
        }
    }
    return "Unknown";
}

// ✅ 使用 Optional (简洁)
public String getCity(User user) {
    return Optional.ofNullable(user)
        .map(User::getAddress)
        .map(Address::getCity)
        .orElse("Unknown");
}
```

## 最佳实践

```
┌─────────────────────────────────────────────────────────────┐
│                    Optional 最佳实践                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ✅ 推荐:                                                   │
│   • 用作方法返回值表示可能为空                              │
│   • 用 orElse/orElseGet/orElseThrow 获取值                 │
│   • 用 map/flatMap/filter 进行链式操作                     │
│   • Java 9+ 用 ifPresentOrElse                             │
│                                                             │
│   ❌ 避免:                                                   │
│   • 不要用 Optional 作为字段类型                            │
│   • 不要用 Optional 作为方法参数                            │
│   • 不要用 Optional 包装集合 (空集合即可)                   │
│   • 不要直接调用 get() 不检查                               │
│   • 不要用 Optional.of(null)                               │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> **Optional** 是 Java 8 引入的容器类，表示值可能存在或不存在，用于**避免 NullPointerException**。创建用 `of()`/`ofNullable()`/`empty()`，获取值用 `orElse()`/`orElseGet()`/`orElseThrow()`，链式操作用 `map()`/`flatMap()`/`filter()`。适合作方法返回值，不建议作字段或参数。

### 1分钟版本

> **定义**：容器类，表示值可能存在或不存在
>
> **目的**：
> - 避免 NPE
> - 强制处理空值
> - 代码更清晰
>
> **创建**：
> - of(value)：值不能为 null
> - ofNullable(value)：值可以为 null
> - empty()：空 Optional
>
> **获取值**：
> - orElse(default)：返回默认值
> - orElseGet(supplier)：延迟计算默认值
> - orElseThrow()：无值抛异常
>
> **最佳实践**：
> - 用作方法返回值
> - 不要作字段或参数
> - 不要直接 get()

---

*关联文档：[java8-features.md](java8-features.md)*
