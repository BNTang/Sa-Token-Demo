# Java 和 Go 的区别

## 语言特性对比

```
┌─────────────────────────────────────────────────────────────┐
│                    Java vs Go                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   Java (1995)                    Go (2009)                  │
│   ──────────                     ────────                   │
│   • 面向对象                     • 面向过程 + 简化OOP       │
│   • JVM 字节码                   • 编译成机器码             │
│   • 成熟生态                     • 云原生生态               │
│   • 企业级应用                   • 微服务/云/工具           │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 详细对比

```
┌─────────────────────────────────────────────────────────────┐
│                    详细对比                                  │
├──────────────────┬──────────────────┬───────────────────────┤
│   特性           │   Java           │   Go                  │
├──────────────────┼──────────────────┼───────────────────────┤
│   类型系统       │   静态强类型     │   静态强类型          │
│   编译方式       │   JIT (字节码)   │   AOT (机器码)        │
│   运行时         │   JVM            │   无虚拟机            │
│   内存管理       │   GC             │   GC                  │
│   OOP            │   类、继承       │   结构体、组合        │
│   并发模型       │   线程 + 锁      │   Goroutine + Channel │
│   异常处理       │   try-catch      │   error 返回值        │
│   泛型           │   支持           │   Go 1.18+ 支持       │
│   包管理         │   Maven/Gradle   │   Go Modules          │
│   启动速度       │   较慢           │   极快                │
│   可执行文件     │   需要 JVM       │   单文件独立部署      │
└──────────────────┴──────────────────┴───────────────────────┘
```

## 代码对比

### Hello World

```java
// Java
public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }
}
```

```go
// Go
package main

import "fmt"

func main() {
    fmt.Println("Hello, World!")
}
```

### 面向对象

```java
// Java: 类 + 继承
public class Animal {
    protected String name;
    public void speak() { }
}

public class Dog extends Animal {
    @Override
    public void speak() {
        System.out.println("Woof!");
    }
}
```

```go
// Go: 结构体 + 组合 + 接口
type Animal struct {
    Name string
}

type Dog struct {
    Animal  // 组合
}

func (d Dog) Speak() {
    fmt.Println("Woof!")
}

// 接口隐式实现
type Speaker interface {
    Speak()
}
```

### 并发

```java
// Java: 线程
new Thread(() -> {
    System.out.println("Hello from thread");
}).start();

// 或使用线程池
ExecutorService executor = Executors.newFixedThreadPool(10);
executor.submit(() -> System.out.println("Task"));
```

```go
// Go: Goroutine (极轻量)
go func() {
    fmt.Println("Hello from goroutine")
}()

// Channel 通信
ch := make(chan int)
go func() {
    ch <- 42  // 发送
}()
result := <-ch  // 接收
```

### 错误处理

```java
// Java: 异常
try {
    File file = new File("test.txt");
    // ...
} catch (IOException e) {
    e.printStackTrace();
}
```

```go
// Go: 返回 error
file, err := os.Open("test.txt")
if err != nil {
    log.Fatal(err)
}
defer file.Close()
```

## 并发模型对比

```
┌─────────────────────────────────────────────────────────────┐
│                    并发模型对比                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   Java 线程模型:                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  Thread (1:1 映射 OS 线程)                          │  │
│   │  • 创建成本高 (~1MB 栈)                             │  │
│   │  • 上下文切换开销大                                 │  │
│   │  • 共享内存 + 锁                                    │  │
│   │  • 需要线程池管理                                   │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   Go Goroutine 模型:                                        │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  Goroutine (M:N 映射)                               │  │
│   │  • 创建成本低 (~2KB 栈，动态增长)                   │  │
│   │  • 调度器管理，切换快                               │  │
│   │  • Channel 通信 (CSP 模型)                          │  │
│   │  • 可创建百万级 Goroutine                           │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   Java 21+ Virtual Threads (虚拟线程):                      │
│   • 类似 Goroutine 的轻量级线程                             │
│   • 大幅降低线程成本                                        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 适用场景

```
┌─────────────────────────────────────────────────────────────┐
│                    适用场景                                  │
├──────────────────┬──────────────────────────────────────────┤
│   Java           │   Go                                     │
├──────────────────┼──────────────────────────────────────────┤
│ • 企业级应用     │ • 微服务                                 │
│ • 大型后端系统   │ • 云原生应用 (K8s, Docker)               │
│ • Android 开发   │ • 命令行工具                             │
│ • 大数据处理     │ • 高并发网络服务                         │
│ • 金融系统       │ • DevOps 工具                            │
│ • Spring 生态    │ • 区块链                                 │
└──────────────────┴──────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> **Java** 是面向对象语言，运行在 JVM 上，生态成熟，适合企业级应用。**Go** 编译成机器码，启动快，部署简单。Go 的 **Goroutine** 比 Java 线程更轻量（2KB vs 1MB），支持百万级并发。Go 用 **error 返回值**而非异常，用**组合代替继承**。Java 适合大型系统，Go 适合微服务、云原生。

### 1分钟版本

> **语言特性**：
> - Java：面向对象、JVM、成熟生态
> - Go：面向过程+简化OOP、编译机器码、云原生生态
>
> **并发模型**：
> - Java：Thread (1MB)、锁、线程池
> - Go：Goroutine (2KB)、Channel、可创建百万级
>
> **错误处理**：
> - Java：try-catch 异常
> - Go：error 返回值
>
> **OOP**：
> - Java：类、继承
> - Go：结构体、组合、隐式接口
>
> **适用场景**：
> - Java：企业应用、大型系统、Android
> - Go：微服务、云原生、CLI工具

---

*关联文档：[jvm-intro.md](../02-jvm/jvm-intro.md)*
