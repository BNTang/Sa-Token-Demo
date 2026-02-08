# 为什么在 Java 中编写代码时会遇到乱码问题？

## 乱码原因

```
┌─────────────────────────────────────────────────────────────┐
│                    乱码问题根本原因                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   核心原因: 编码和解码使用了不同的字符集                    │
│                                                             │
│   ┌──────────┐   编码    ┌──────────┐   解码    ┌────────┐ │
│   │  "中文"  │ ───────→ │ 字节序列  │ ───────→ │  ???   │ │
│   └──────────┘   UTF-8   └──────────┘   GBK    └────────┘ │
│                                                             │
│   编码: 字符 → 字节                                         │
│   解码: 字节 → 字符                                         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 常见字符编码

```
┌─────────────────────────────────────────────────────────────┐
│                    常见字符编码                              │
├──────────────┬──────────────────────────────────────────────┤
│   编码       │   说明                                       │
├──────────────┼──────────────────────────────────────────────┤
│   ASCII      │ 7 位，128 字符，英文、数字、符号             │
│   ISO-8859-1 │ 8 位，256 字符，西欧语言                     │
│   GBK/GB2312 │ 双字节，中文简体                             │
│   Big5       │ 双字节，中文繁体                             │
│   UTF-8      │ 变长，1-4 字节，兼容 ASCII，推荐             │
│   UTF-16     │ 2 或 4 字节，Java 内部使用                   │
└──────────────┴──────────────────────────────────────────────┘
```

## 乱码场景

### 1. 文件读写乱码

```java
// ❌ 未指定编码
FileReader reader = new FileReader("test.txt");  // 使用系统默认编码

// ✅ 指定编码
BufferedReader reader = new BufferedReader(
    new InputStreamReader(new FileInputStream("test.txt"), "UTF-8")
);

// Java 11+ 更简单
Files.readString(Path.of("test.txt"), StandardCharsets.UTF_8);
```

### 2. 网络传输乱码

```java
// HTTP 请求
// ❌ 未设置编码
request.getParameter("name");  // 可能乱码

// ✅ 设置请求编码
request.setCharacterEncoding("UTF-8");

// ✅ 设置响应编码
response.setContentType("text/html; charset=UTF-8");
response.setCharacterEncoding("UTF-8");
```

### 3. 数据库乱码

```
┌─────────────────────────────────────────────────────────────┐
│                    数据库乱码排查                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   需要统一的地方:                                            │
│   1. 数据库字符集: CREATE DATABASE db CHARACTER SET utf8mb4 │
│   2. 表字符集:     CREATE TABLE t (...) CHARSET=utf8mb4     │
│   3. 连接字符集:   jdbc:mysql://...?characterEncoding=UTF-8 │
│   4. 客户端编码:   SET NAMES utf8mb4                        │
│                                                             │
│   MySQL 注意: utf8 只支持 3 字节，用 utf8mb4 (4 字节，表情符)│
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 4. String 编码转换

```java
String str = "中文";

// 编码: String → byte[]
byte[] bytes = str.getBytes("UTF-8");

// 解码: byte[] → String
String decoded = new String(bytes, "UTF-8");

// ❌ 常见错误: 编解码不一致
byte[] utf8Bytes = str.getBytes("UTF-8");
String wrong = new String(utf8Bytes, "GBK");  // 乱码!

// ✅ 推荐使用 StandardCharsets 常量
byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
String decoded = new String(bytes, StandardCharsets.UTF_8);
```

## 如何避免乱码

```
┌─────────────────────────────────────────────────────────────┐
│                    避免乱码的最佳实践                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 统一使用 UTF-8                                          │
│      • 源代码文件                                           │
│      • 配置文件                                             │
│      • 数据库                                               │
│      • HTTP 请求响应                                        │
│                                                             │
│   2. 显式指定编码，不依赖默认值                             │
│      • new InputStreamReader(is, "UTF-8")                   │
│      • request.setCharacterEncoding("UTF-8")                │
│                                                             │
│   3. 使用 StandardCharsets 常量                             │
│      • StandardCharsets.UTF_8 (避免拼写错误)                │
│                                                             │
│   4. IDE 设置                                                │
│      • File Encoding: UTF-8                                 │
│      • Console Encoding: UTF-8                              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## JVM 默认编码

```java
// 获取系统默认编码
System.out.println(Charset.defaultCharset());  // 如 UTF-8

// JVM 启动参数设置默认编码
// -Dfile.encoding=UTF-8

// Windows 中文版默认 GBK，Linux/Mac 默认 UTF-8
```

## 面试回答

### 30秒版本

> 乱码的根本原因是**编码和解码使用了不同的字符集**。常见场景：文件读写、网络传输、数据库交互。避免方法：**统一使用 UTF-8**、显式指定编码不依赖默认值、使用 `StandardCharsets.UTF_8` 常量。数据库注意用 **utf8mb4** 而非 utf8。

### 1分钟版本

> **根本原因**：
> - 编码（字符→字节）和解码（字节→字符）使用不同字符集
>
> **常见场景**：
> - 文件读写：未指定编码
> - HTTP：未设置 Content-Type
> - 数据库：连接编码不一致
> - String 转换：getBytes 和 new String 编码不同
>
> **解决方案**：
> - 统一使用 UTF-8
> - 显式指定编码
> - 使用 StandardCharsets.UTF_8
> - 数据库用 utf8mb4
>
> **注意**：
> - Windows 默认 GBK
> - JVM 参数 `-Dfile.encoding=UTF-8`

---

*关联文档：[string-byte-array.md](string-byte-array.md) | [string-buffer-builder.md](string-buffer-builder.md)*
