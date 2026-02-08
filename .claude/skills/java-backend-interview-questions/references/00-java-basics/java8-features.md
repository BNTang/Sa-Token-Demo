# Java 8 有哪些新特性？

## 核心新特性概览

```
┌─────────────────────────────────────────────────────────────┐
│                    Java 8 新特性 (2014)                      │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. Lambda 表达式                                           │
│   2. Stream API                                              │
│   3. 函数式接口 @FunctionalInterface                        │
│   4. 接口默认方法 / 静态方法                                 │
│   5. Optional 类                                             │
│   6. 新的日期时间 API (java.time)                           │
│   7. 方法引用 ::                                             │
│   8. CompletableFuture                                       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 1. Lambda 表达式

```java
// 之前: 匿名内部类
Runnable r1 = new Runnable() {
    @Override
    public void run() {
        System.out.println("Hello");
    }
};

// Java 8: Lambda
Runnable r2 = () -> System.out.println("Hello");

// 带参数
Comparator<String> c1 = (s1, s2) -> s1.compareTo(s2);

// 多行
Comparator<String> c2 = (s1, s2) -> {
    System.out.println("Comparing");
    return s1.compareTo(s2);
};
```

## 2. Stream API

```java
List<String> names = Arrays.asList("Tom", "Jerry", "Alice", "Bob");

// 过滤 + 转换 + 收集
List<String> result = names.stream()
    .filter(name -> name.length() > 3)    // 过滤
    .map(String::toUpperCase)             // 转换
    .sorted()                             // 排序
    .collect(Collectors.toList());        // 收集

// 聚合操作
long count = names.stream().count();
Optional<String> first = names.stream().findFirst();

// 并行流
names.parallelStream()
    .forEach(System.out::println);
```

```
┌─────────────────────────────────────────────────────────────┐
│                    Stream 常用操作                           │
├─────────────────────────────────────────────────────────────┤
│   中间操作 (惰性):                                           │
│   • filter()  - 过滤                                        │
│   • map()     - 转换                                        │
│   • flatMap() - 扁平化                                      │
│   • sorted()  - 排序                                        │
│   • distinct()- 去重                                        │
│   • limit()   - 限制数量                                    │
│                                                             │
│   终止操作:                                                  │
│   • collect() - 收集到集合                                  │
│   • forEach() - 遍历                                        │
│   • count()   - 计数                                        │
│   • reduce()  - 归约                                        │
│   • findFirst() / findAny()                                 │
└─────────────────────────────────────────────────────────────┘
```

## 3. 函数式接口

```java
// 内置函数式接口
Function<String, Integer> strToInt = Integer::parseInt;
Predicate<String> isEmpty = String::isEmpty;
Consumer<String> printer = System.out::println;
Supplier<String> supplier = () -> "Hello";

// 自定义
@FunctionalInterface
interface MyFunction {
    int apply(int a, int b);
    // 只能有一个抽象方法
}

MyFunction add = (a, b) -> a + b;
```

## 4. 接口默认方法

```java
interface MyInterface {
    // 默认方法
    default void defaultMethod() {
        System.out.println("Default implementation");
    }
    
    // 静态方法
    static void staticMethod() {
        System.out.println("Static method");
    }
    
    // 抽象方法
    void abstractMethod();
}
```

## 5. Optional

```java
// 避免 NullPointerException
Optional<String> opt = Optional.ofNullable(getName());

// 使用
String name = opt.orElse("default");
String name2 = opt.orElseGet(() -> "default");
opt.ifPresent(System.out::println);

// 链式操作
String upper = Optional.ofNullable(user)
    .map(User::getName)
    .map(String::toUpperCase)
    .orElse("UNKNOWN");
```

## 6. 新日期时间 API

```java
// 不可变，线程安全
LocalDate date = LocalDate.now();
LocalTime time = LocalTime.now();
LocalDateTime dateTime = LocalDateTime.now();

// 时区
ZonedDateTime zoned = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));

// 操作
LocalDate tomorrow = date.plusDays(1);
LocalDate nextMonth = date.plusMonths(1);

// 格式化
DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
String formatted = date.format(formatter);
```

## 7. 方法引用

```java
// 静态方法引用
Function<String, Integer> f1 = Integer::parseInt;

// 实例方法引用
String str = "hello";
Supplier<Integer> f2 = str::length;

// 任意对象实例方法引用
Function<String, Integer> f3 = String::length;

// 构造方法引用
Supplier<ArrayList<String>> f4 = ArrayList::new;
```

## 面试回答

### 30秒版本

> Java 8 最重要的特性：1）**Lambda 表达式**：简化匿名内部类；2）**Stream API**：集合的函数式操作，支持 filter/map/collect 链式调用；3）**Optional**：避免 NPE；4）**接口默认方法**：接口可以有实现；5）**新日期 API**：LocalDate/LocalDateTime，不可变线程安全；6）**方法引用 `::`**：简化 Lambda。

### 1分钟版本

> **Lambda 表达式**：
> - `() -> expression`
> - 简化匿名内部类
>
> **Stream API**：
> - 函数式处理集合
> - filter/map/sorted/collect
> - 并行流 parallelStream
>
> **函数式接口**：
> - @FunctionalInterface
> - Function/Predicate/Consumer/Supplier
>
> **Optional**：
> - 包装可能为 null 的值
> - map/orElse/ifPresent
>
> **接口默认方法**：
> - default 方法有实现
> - 支持接口演化
>
> **新日期 API**：
> - LocalDate/LocalTime/LocalDateTime
> - 不可变、线程安全
>
> **方法引用**：`类::方法`

---

*关联文档：[java11-features.md](java11-features.md) | [java17-features.md](java17-features.md)*
