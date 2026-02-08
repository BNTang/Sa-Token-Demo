# Java 运行时异常和编译时异常之间的区别是什么？

## 异常分类

```
┌─────────────────────────────────────────────────────────────┐
│                    Java 异常体系                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│                      Throwable                               │
│                     ┌────┴────┐                             │
│                     │         │                             │
│                  Error     Exception                         │
│                  (错误)    (异常)                            │
│                             │                               │
│              ┌──────────────┼──────────────┐               │
│              │                             │                │
│    RuntimeException                   其他 Exception        │
│    (运行时异常/非受检)               (编译时异常/受检)      │
│                                                             │
│    ┌─────────────────────┐    ┌─────────────────────┐      │
│    │ NullPointerException│    │ IOException        │       │
│    │ IndexOutOfBounds    │    │ SQLException       │       │
│    │ ClassCastException  │    │ ClassNotFoundException│    │
│    │ ArithmeticException │    │ FileNotFoundException │    │
│    │ IllegalArgumentException│ │ InterruptedException│    │
│    └─────────────────────┘    └─────────────────────┘      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 核心区别

```
┌─────────────────────────────────────────────────────────────┐
│              运行时异常 vs 编译时异常                        │
├──────────────────┬──────────────────┬───────────────────────┤
│   特性           │ 运行时异常       │ 编译时异常            │
├──────────────────┼──────────────────┼───────────────────────┤
│ 别名             │ 非受检异常       │ 受检异常              │
│                  │ Unchecked        │ Checked               │
├──────────────────┼──────────────────┼───────────────────────┤
│ 继承             │ RuntimeException │ Exception (非Runtime) │
├──────────────────┼──────────────────┼───────────────────────┤
│ 编译时检查       │ 不强制处理       │ 必须处理              │
├──────────────────┼──────────────────┼───────────────────────┤
│ 处理方式         │ 可以不 try-catch │ 必须 try-catch        │
│                  │ 也不用 throws    │ 或 throws 声明        │
├──────────────────┼──────────────────┼───────────────────────┤
│ 产生原因         │ 程序逻辑错误     │ 外部因素/可预见问题   │
├──────────────────┼──────────────────┼───────────────────────┤
│ 典型例子         │ NPE、数组越界    │ IO异常、SQL异常       │
└──────────────────┴──────────────────┴───────────────────────┘
```

## 代码示例

### 运行时异常

```java
// 运行时异常: 编译不强制处理
public void runtimeExample() {
    // NullPointerException
    String s = null;
    s.length();  // 运行时抛异常
    
    // ArrayIndexOutOfBoundsException
    int[] arr = new int[3];
    arr[5] = 10;  // 运行时抛异常
    
    // ArithmeticException
    int result = 10 / 0;  // 运行时抛异常
    
    // ClassCastException
    Object obj = "Hello";
    Integer num = (Integer) obj;  // 运行时抛异常
}
// 编译通过，但运行时可能抛异常
```

### 编译时异常

```java
// 编译时异常: 必须处理

// 方式1: try-catch 捕获
public void checkedExample1() {
    try {
        FileReader reader = new FileReader("file.txt");
        // ...
    } catch (FileNotFoundException e) {
        e.printStackTrace();
    }
}

// 方式2: throws 声明
public void checkedExample2() throws FileNotFoundException {
    FileReader reader = new FileReader("file.txt");
    // ...
}

// ❌ 不处理编译错误
public void wrongExample() {
    FileReader reader = new FileReader("file.txt");  // 编译错误!
    // Unhandled exception: java.io.FileNotFoundException
}
```

## 设计理念

```
┌─────────────────────────────────────────────────────────────┐
│                    设计理念                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   编译时异常 (Checked):                                      │
│   • 外部因素导致，程序无法避免                              │
│   • 文件不存在、网络中断、数据库连接失败                    │
│   • 强制处理，提醒开发者考虑异常情况                        │
│                                                             │
│   运行时异常 (Unchecked):                                    │
│   • 程序逻辑错误，可以通过代码避免                          │
│   • 空指针、数组越界、类型转换错误                          │
│   • 不强制处理，避免代码被异常处理淹没                      │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  争议: 很多人认为 Checked Exception 设计失败         │  │
│   │  • 过多的 try-catch 降低代码可读性                   │  │
│   │  • 新语言 (Kotlin、C#) 都没有 Checked Exception      │  │
│   │  • Spring 框架将大多数异常转为 Runtime               │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 最佳实践

```java
// 1. 自定义业务异常通常继承 RuntimeException
public class BusinessException extends RuntimeException {
    private int code;
    
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}

// 2. 将 Checked 异常转为 Runtime 异常
public void processFile(String path) {
    try {
        // ...
    } catch (IOException e) {
        throw new RuntimeException("文件处理失败", e);
    }
}

// 3. 使用 try-with-resources
try (FileReader reader = new FileReader("file.txt")) {
    // ...
} catch (IOException e) {
    // 处理
}
```

## 面试回答

### 30秒版本

> **编译时异常**（Checked）继承 Exception，必须 try-catch 或 throws 声明，代表外部因素（IO、SQL）。**运行时异常**（Unchecked）继承 RuntimeException，编译不强制处理，代表程序逻辑错误（NPE、越界）。实际开发中，自定义业务异常通常继承 RuntimeException。

### 1分钟版本

> **编译时异常 (Checked)**：
> - 继承 Exception（非 Runtime）
> - 必须 try-catch 或 throws
> - 外部因素：IO、SQL、网络
>
> **运行时异常 (Unchecked)**：
> - 继承 RuntimeException
> - 编译不强制处理
> - 程序错误：NPE、越界、类型转换
>
> **设计理念**：
> - Checked：强制处理外部异常
> - Unchecked：逻辑错误应修复代码
>
> **实践**：
> - 自定义异常继承 RuntimeException
> - 可将 Checked 包装为 Runtime

---

*关联文档：[exception-vs-error.md](exception-vs-error.md)*
