# Java 泛型擦除是什么？

## 类型擦除概念

```
┌─────────────────────────────────────────────────────────────┐
│                    类型擦除 (Type Erasure)                   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   定义: 泛型信息只在编译期存在，运行时被擦除                │
│                                                             │
│   编译前:                                                    │
│   List<String> list = new ArrayList<>();                    │
│                                                             │
│   编译后 (字节码):                                           │
│   List list = new ArrayList();  // 泛型信息被擦除           │
│                                                             │
│   原因:                                                      │
│   • 兼容 Java 5 之前的代码                                  │
│   • 保持字节码向后兼容                                      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 擦除过程

```
┌─────────────────────────────────────────────────────────────┐
│                    类型擦除过程                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   无界类型参数 → Object                                     │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  class Box<T> {            class Box {               │  │
│   │      private T item;  →        private Object item;  │  │
│   │  }                         }                         │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   有界类型参数 → 上界类型                                   │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  class Box<T extends Number> {                       │  │
│   │      private T item;                                 │  │
│   │  }                                                   │  │
│   │  擦除后:                                              │  │
│   │  class Box {                                         │  │
│   │      private Number item;  // 擦除为上界             │  │
│   │  }                                                   │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 代码验证

```java
// 验证类型擦除
List<String> stringList = new ArrayList<>();
List<Integer> intList = new ArrayList<>();

// 运行时类型相同
System.out.println(stringList.getClass() == intList.getClass());  // true

// 获取泛型信息
System.out.println(stringList.getClass());  // class java.util.ArrayList

// 无法通过反射获取泛型参数 (局部变量)
```

## 类型擦除的影响

```java
// 1. 不能创建泛型数组
T[] array = new T[10];  // 编译错误!
// 替代方案
T[] array = (T[]) new Object[10];  // 需要强转，有警告

// 2. 不能使用 instanceof 检查泛型类型
if (obj instanceof List<String>) { }  // 编译错误!
// 可以检查原始类型
if (obj instanceof List<?>) { }  // OK

// 3. 不能捕获泛型异常
try {
    // ...
} catch (GenericException<String> e) { }  // 编译错误!

// 4. 不能重载泛型方法 (擦除后签名相同)
void process(List<String> list) { }   // 擦除后: void process(List list)
void process(List<Integer> list) { }  // 擦除后: void process(List list) 冲突!
```

## 桥接方法

```java
// 泛型继承时，编译器会生成桥接方法

interface Comparable<T> {
    int compareTo(T o);
}

class MyInt implements Comparable<Integer> {
    @Override
    public int compareTo(Integer o) {
        return 0;
    }
}

// 编译后，编译器生成桥接方法:
class MyInt implements Comparable {
    // 原方法
    public int compareTo(Integer o) {
        return 0;
    }
    
    // 桥接方法 (编译器生成)
    public int compareTo(Object o) {
        return compareTo((Integer) o);  // 调用原方法
    }
}
```

## 如何保留泛型信息

```java
// 1. 通过匿名子类 (TypeReference 技巧)
abstract class TypeReference<T> {
    Type getType() {
        Type superClass = getClass().getGenericSuperclass();
        return ((ParameterizedType) superClass).getActualTypeArguments()[0];
    }
}

TypeReference<List<String>> ref = new TypeReference<List<String>>() {};
System.out.println(ref.getType());  // java.util.List<java.lang.String>

// 2. Jackson 使用此技巧
objectMapper.readValue(json, new TypeReference<List<User>>() {});

// 3. 字段和方法参数的泛型信息可以通过反射获取
class MyClass {
    private List<String> names;  // 字段的泛型信息会保留
}

Field field = MyClass.class.getDeclaredField("names");
ParameterizedType type = (ParameterizedType) field.getGenericType();
Type arg = type.getActualTypeArguments()[0];  // String
```

## 面试回答

### 30秒版本

> **类型擦除**是指泛型信息只在编译期存在，运行时被擦除为原始类型。无界类型参数擦除为 Object，有界参数擦除为上界。这是为了兼容 Java 5 之前的代码。影响：不能创建泛型数组、不能 instanceof 泛型类型、不能重载泛型方法（擦除后签名相同）。

### 1分钟版本

> **定义**：
> - 泛型只在编译期存在
> - 运行时被擦除为原始类型
>
> **擦除规则**：
> - 无界 `<T>` → Object
> - 有界 `<T extends Number>` → Number
>
> **影响**：
> - 不能 `new T[]`
> - 不能 `instanceof List<String>`
> - 不能重载泛型方法
>
> **桥接方法**：
> - 泛型继承时编译器生成
> - 保证多态正确性
>
> **保留泛型信息**：
> - 字段/方法参数可通过反射获取
> - TypeReference 匿名子类技巧

---

*关联文档：[generics-intro.md](generics-intro.md) | [generics-bounds.md](generics-bounds.md)*
