# Java 11 有哪些新特性？

## 核心新特性概览

```
┌─────────────────────────────────────────────────────────────┐
│                    Java 11 新特性 (2018 LTS)                 │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. var 局部变量类型推断 (Lambda 中)                        │
│   2. 新的 String 方法                                        │
│   3. 新的 HttpClient API                                     │
│   4. 单文件源代码执行                                        │
│   5. Optional 新方法                                         │
│   6. Collection.toArray()                                    │
│   7. Files 新方法                                            │
│   8. ZGC 垃圾收集器 (实验性)                                 │
│   9. 移除 Java EE 和 CORBA 模块                             │
│                                                             │
│   Java 11 是 LTS (长期支持版本)                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 1. Lambda 中使用 var

```java
// Java 10: var 用于局部变量
var list = new ArrayList<String>();  // 推断为 ArrayList<String>

// Java 11: var 可以用于 Lambda 参数
list.stream()
    .map((@Nonnull var s) -> s.toUpperCase())  // 可以加注解
    .collect(Collectors.toList());
```

## 2. 新的 String 方法

```java
// isBlank() - 判断是否为空白
"   ".isBlank();    // true
"".isBlank();       // true
"abc".isBlank();    // false

// lines() - 按行分割
String text = "line1\nline2\nline3";
text.lines().forEach(System.out::println);

// strip() - 去除首尾空白 (支持 Unicode)
"  hello  ".strip();       // "hello"
"  hello  ".stripLeading();  // "hello  "
"  hello  ".stripTrailing(); // "  hello"

// repeat() - 重复字符串
"ab".repeat(3);  // "ababab"
```

```
┌─────────────────────────────────────────────────────────────┐
│                    String 新方法对比                         │
├──────────────────┬───────────────────────────────────────────┤
│   方法           │   说明                                    │
├──────────────────┼───────────────────────────────────────────┤
│   isBlank()      │ 空或只有空白字符返回 true                 │
│   lines()        │ 按行分割，返回 Stream<String>             │
│   strip()        │ 去除首尾空白（支持 Unicode）              │
│   stripLeading() │ 去除开头空白                              │
│   stripTrailing()│ 去除结尾空白                              │
│   repeat(n)      │ 重复 n 次                                 │
├──────────────────┴───────────────────────────────────────────┤
│   strip() vs trim(): strip 支持 Unicode 空白字符            │
└─────────────────────────────────────────────────────────────┘
```

## 3. 新的 HttpClient API

```java
// 同步请求
HttpClient client = HttpClient.newHttpClient();

HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("https://api.example.com/users"))
    .header("Content-Type", "application/json")
    .GET()
    .build();

HttpResponse<String> response = client.send(
    request, 
    HttpResponse.BodyHandlers.ofString()
);
System.out.println(response.body());

// 异步请求
client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
    .thenApply(HttpResponse::body)
    .thenAccept(System.out::println)
    .join();
```

```
┌─────────────────────────────────────────────────────────────┐
│                    HttpClient 特点                           │
├─────────────────────────────────────────────────────────────┤
│   • 支持 HTTP/2 和 WebSocket                                │
│   • 同步和异步 API                                          │
│   • 链式 Builder 模式                                       │
│   • 替代老旧的 HttpURLConnection                            │
└─────────────────────────────────────────────────────────────┘
```

## 4. 单文件源代码执行

```bash
# 以前需要先编译再运行
javac Hello.java
java Hello

# Java 11 直接运行源文件
java Hello.java
```

## 5. Optional 新方法

```java
// isEmpty() - 与 isPresent() 相反
Optional<String> opt = Optional.empty();
opt.isEmpty();     // true (Java 11)
opt.isPresent();   // false

// or() - 提供另一个 Optional
Optional<String> result = opt.or(() -> Optional.of("default"));
```

## 6. Collection.toArray()

```java
// 之前
String[] arr1 = list.toArray(new String[0]);
String[] arr2 = list.toArray(new String[list.size()]);

// Java 11
String[] arr3 = list.toArray(String[]::new);  // 更简洁
```

## 7. Files 新方法

```java
// 读写文件更简洁
Path path = Path.of("test.txt");

// 读取为字符串
String content = Files.readString(path);

// 写入字符串
Files.writeString(path, "Hello World");

// 写入并追加
Files.writeString(path, "More", StandardOpenOption.APPEND);
```

## 8. ZGC 垃圾收集器

```
┌─────────────────────────────────────────────────────────────┐
│                    ZGC (实验性)                              │
├─────────────────────────────────────────────────────────────┤
│   • 低延迟 GC，停顿时间 < 10ms                              │
│   • 支持 TB 级堆内存                                        │
│   • 启用: -XX:+UseZGC                                       │
│   • Java 15 正式发布                                        │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> Java 11 是 **LTS 版本**，主要特性：1）Lambda 参数可用 **var**；2）**String 新方法**：isBlank/lines/strip/repeat；3）标准化的 **HttpClient**：支持 HTTP/2 和异步；4）**单文件执行**：java Hello.java；5）**Optional.isEmpty()**；6）Files.readString/writeString 简化 IO；7）**ZGC** 低延迟垃圾收集器（实验性）。

### 1分钟版本

> **Java 11 是 LTS**（长期支持版本）
>
> **Lambda 中 var**：
> - 可以给参数加注解
>
> **String 新方法**：
> - `isBlank()`：空或空白
> - `lines()`：按行分割为 Stream
> - `strip()`：去空白（支持 Unicode）
> - `repeat(n)`：重复 n 次
>
> **HttpClient**：
> - 正式 API（替代 HttpURLConnection）
> - 支持 HTTP/2、WebSocket
> - 同步/异步
>
> **其他**：
> - 单文件执行 `java Hello.java`
> - `Optional.isEmpty()`
> - `Files.readString()/writeString()`
>
> **ZGC**：
> - 低延迟 GC（<10ms）
> - Java 11 实验，Java 15 正式

---

*关联文档：[java8-features.md](java8-features.md) | [java17-features.md](java17-features.md)*
