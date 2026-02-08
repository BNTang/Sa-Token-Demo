# Java 中 Exception 和 Error 有什么区别？

## 异常体系结构

```
┌─────────────────────────────────────────────────────────────┐
│                    Java 异常体系                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│                      Throwable                              │
│                          │                                  │
│            ┌─────────────┴─────────────┐                   │
│            │                           │                    │
│         Error                      Exception                │
│            │                           │                    │
│   ┌────────┴────────┐      ┌──────────┴──────────┐        │
│   │                 │      │                     │         │
│ OutOfMemoryError  StackOverflow  RuntimeException  IOException│
│ VirtualMachineError Error        NullPointerException ...  │
│ LinkageError                     IndexOutOfBounds          │
│                                  ClassCastException        │
│                                  IllegalArgumentException  │
│                                                             │
│   ────────────────────────────────────────────────────────  │
│   │  不可恢复，不应捕获  │  │  可恢复，应该处理  │          │
│   ────────────────────────────────────────────────────────  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 核心区别

```
┌─────────────────────────────────────────────────────────────┐
│                    Error vs Exception                        │
├──────────────────┬──────────────────┬───────────────────────┤
│   维度           │   Error          │   Exception           │
├──────────────────┼──────────────────┼───────────────────────┤
│   严重程度       │ 严重，不可恢复   │ 相对轻微，可恢复      │
│   产生原因       │ JVM/系统级问题   │ 程序逻辑/外部条件     │
│   是否应捕获     │ 不应该捕获       │ 应该捕获处理          │
│   是否可预防     │ 通常不可预防     │ 大部分可预防          │
│   常见示例       │ OOM、StackOverflow│ NPE、IOException     │
├──────────────────┴──────────────────┴───────────────────────┤
│                                                             │
│   共同点: 都继承自 Throwable                                 │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## Error 详解

```
┌─────────────────────────────────────────────────────────────┐
│                    常见 Error                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   OutOfMemoryError (OOM):                                   │
│   ├── Java heap space: 堆内存不足                           │
│   ├── Metaspace: 元空间不足                                 │
│   ├── GC overhead limit exceeded: GC 耗时过长               │
│   └── Unable to create native thread: 无法创建线程          │
│                                                             │
│   StackOverflowError:                                       │
│   └── 方法调用层级过深（通常是无限递归）                    │
│                                                             │
│   VirtualMachineError:                                      │
│   └── JVM 运行时内部错误                                    │
│                                                             │
│   LinkageError:                                             │
│   ├── NoClassDefFoundError: 类定义找不到                    │
│   └── ClassFormatError: 类文件格式错误                      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

```java
// StackOverflowError 示例
public void infiniteRecursion() {
    infiniteRecursion(); // 无限递归 → StackOverflowError
}

// OutOfMemoryError 示例
List<byte[]> list = new ArrayList<>();
while (true) {
    list.add(new byte[1024 * 1024]); // 不断分配内存 → OOM
}
```

## Exception 分类

```
┌─────────────────────────────────────────────────────────────┐
│                    Exception 分类                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   Exception                                                  │
│       │                                                     │
│       ├── Checked Exception (受检异常)                      │
│       │   ├── 编译时强制处理 (try-catch 或 throws)          │
│       │   ├── IOException                                   │
│       │   ├── SQLException                                  │
│       │   ├── ClassNotFoundException                        │
│       │   └── InterruptedException                          │
│       │                                                     │
│       └── Unchecked Exception (非受检异常)                  │
│           ├── RuntimeException 及其子类                     │
│           ├── 编译时不强制处理                              │
│           ├── NullPointerException                          │
│           ├── ArrayIndexOutOfBoundsException                │
│           ├── ClassCastException                            │
│           ├── IllegalArgumentException                      │
│           └── NumberFormatException                         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

```java
// Checked Exception - 必须处理
public void readFile() throws IOException {  // 声明抛出
    FileInputStream fis = new FileInputStream("file.txt");
}

public void readFileSafe() {
    try {
        FileInputStream fis = new FileInputStream("file.txt");
    } catch (IOException e) {  // 捕获处理
        e.printStackTrace();
    }
}

// Unchecked Exception - 可以不处理
public void divide(int a, int b) {
    int result = a / b;  // 可能 ArithmeticException，编译不报错
}
```

## 异常处理最佳实践

```
┌─────────────────────────────────────────────────────────────┐
│                    异常处理最佳实践                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ✅ 推荐做法:                                               │
│   • 捕获具体异常，而非 Exception                            │
│   • 异常信息要有意义                                        │
│   • 不要用异常控制流程                                      │
│   • finally 中不要 return                                   │
│   • 自定义异常继承合适的父类                                │
│                                                             │
│   ❌ 避免做法:                                               │
│   • catch 块为空（吞掉异常）                                │
│   • catch (Exception e) 一网打尽                            │
│   • 捕获 Error 或 Throwable                                 │
│   • 用 e.printStackTrace() 代替日志                        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

```java
// ❌ 错误示例
try {
    doSomething();
} catch (Exception e) {
    e.printStackTrace();  // 不推荐
}

// ✅ 正确示例
try {
    doSomething();
} catch (IOException e) {
    log.error("读取文件失败: {}", filename, e);
    throw new BusinessException("操作失败", e);
}
```

## 面试回答

### 30秒版本

> **Error** 是严重的、不可恢复的问题，由 JVM 或系统层面产生，如 `OutOfMemoryError`、`StackOverflowError`，**不应该捕获**。**Exception** 是程序可以处理的异常，分为受检异常（编译时必须处理，如 IOException）和非受检异常（RuntimeException，编译时不强制）。两者都继承自 `Throwable`。

### 1分钟版本

> **Exception vs Error**：
> - 都继承自 `Throwable`
> - Error：严重、不可恢复、JVM/系统级，不应捕获
> - Exception：可恢复、应该处理
>
> **常见 Error**：
> - `OutOfMemoryError`：内存不足
> - `StackOverflowError`：栈溢出（无限递归）
>
> **Exception 分类**：
> - **Checked**：编译时必须处理
>   - IOException、SQLException
> - **Unchecked**：RuntimeException
>   - NullPointerException、IndexOutOfBounds
>
> **最佳实践**：
> - 捕获具体异常
> - 不要 catch 块为空
> - 不要捕获 Error
> - 用日志记录，不要 printStackTrace

---

*关联文档：[jvm-oom-scenarios.md](../11-jvm/jvm-oom-scenarios.md) | [exception-handling.md](exception-handling.md)*
