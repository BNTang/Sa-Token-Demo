# Java 的 I/O 流是什么？

## I/O 流概述

```
┌─────────────────────────────────────────────────────────────┐
│                    Java I/O 流概述                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   定义: 数据从源到目的地的传输通道                          │
│                                                             │
│   流的方向:                                                  │
│   • 输入流 (Input): 数据从外部读入程序                      │
│   • 输出流 (Output): 数据从程序写到外部                     │
│                                                             │
│   ┌───────────┐          ┌──────────┐                       │
│   │   数据源   │ ──────→ │   程序   │  输入流 (读)          │
│   │(文件/网络) │          │          │                       │
│   └───────────┘          └──────────┘                       │
│                                                             │
│   ┌───────────┐          ┌──────────┐                       │
│   │   程序    │ ──────→ │  目的地  │  输出流 (写)          │
│   │           │          │(文件/网络)│                       │
│   └───────────┘          └──────────┘                       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 流的分类

```
┌─────────────────────────────────────────────────────────────┐
│                    流的分类                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   按数据单位:                                                │
│   ├── 字节流 (Byte Stream): 处理二进制数据                  │
│   │   └── InputStream / OutputStream                        │
│   └── 字符流 (Character Stream): 处理文本数据               │
│       └── Reader / Writer                                   │
│                                                             │
│   按功能:                                                    │
│   ├── 节点流: 直接连接数据源                                │
│   │   └── FileInputStream、FileReader                       │
│   └── 处理流: 包装节点流，增强功能                          │
│       └── BufferedInputStream、BufferedReader               │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 核心类层次

```
┌─────────────────────────────────────────────────────────────┐
│                    I/O 类层次结构                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   字节流:                                                    │
│   InputStream (抽象类)                                       │
│   ├── FileInputStream        文件输入                       │
│   ├── ByteArrayInputStream   字节数组输入                   │
│   ├── BufferedInputStream    缓冲输入                       │
│   ├── DataInputStream        读取基本类型                   │
│   └── ObjectInputStream      读取对象                       │
│                                                             │
│   OutputStream (抽象类)                                      │
│   ├── FileOutputStream       文件输出                       │
│   ├── ByteArrayOutputStream  字节数组输出                   │
│   ├── BufferedOutputStream   缓冲输出                       │
│   ├── DataOutputStream       写入基本类型                   │
│   └── ObjectOutputStream     写入对象                       │
│                                                             │
│   字符流:                                                    │
│   Reader (抽象类)                                            │
│   ├── FileReader             文件字符输入                   │
│   ├── BufferedReader         缓冲字符输入                   │
│   ├── InputStreamReader      字节转字符                     │
│   └── StringReader           字符串输入                     │
│                                                             │
│   Writer (抽象类)                                            │
│   ├── FileWriter             文件字符输出                   │
│   ├── BufferedWriter         缓冲字符输出                   │
│   ├── OutputStreamWriter     字符转字节                     │
│   └── StringWriter           字符串输出                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 代码示例

### 字节流

```java
// 读取文件 (字节流)
try (FileInputStream fis = new FileInputStream("input.txt")) {
    byte[] buffer = new byte[1024];
    int len;
    while ((len = fis.read(buffer)) != -1) {
        System.out.write(buffer, 0, len);
    }
}

// 写入文件 (字节流)
try (FileOutputStream fos = new FileOutputStream("output.txt")) {
    byte[] data = "Hello World".getBytes();
    fos.write(data);
}

// 带缓冲 (性能更好)
try (BufferedInputStream bis = new BufferedInputStream(
        new FileInputStream("input.txt"))) {
    // ...
}
```

### 字符流

```java
// 读取文本文件 (字符流)
try (BufferedReader br = new BufferedReader(
        new FileReader("input.txt"))) {
    String line;
    while ((line = br.readLine()) != null) {
        System.out.println(line);
    }
}

// 写入文本文件 (字符流)
try (BufferedWriter bw = new BufferedWriter(
        new FileWriter("output.txt"))) {
    bw.write("Hello World");
    bw.newLine();
}

// 指定编码
try (BufferedReader br = new BufferedReader(
        new InputStreamReader(
            new FileInputStream("input.txt"), "UTF-8"))) {
    // ...
}
```

## 字节流 vs 字符流

```
┌─────────────────────────────────────────────────────────────┐
│                    字节流 vs 字符流                          │
├──────────────────┬──────────────────────────────────────────┤
│   特性           │   说明                                   │
├──────────────────┼──────────────────────────────────────────┤
│   字节流         │ 处理二进制数据 (图片、视频、压缩包)      │
│   字符流         │ 处理文本数据 (自动处理编码)              │
├──────────────────┼──────────────────────────────────────────┤
│   选择原则:                                                  │
│   • 纯文本文件 → 字符流 (Reader/Writer)                     │
│   • 二进制文件 → 字节流 (InputStream/OutputStream)          │
│   • 不确定 → 字节流 (更通用)                                │
└──────────────────┴──────────────────────────────────────────┘
```

## Java 7+ NIO.2

```java
// Java 7+ Files 工具类 (推荐)
// 读取所有内容
String content = Files.readString(Path.of("file.txt"));

// 读取所有行
List<String> lines = Files.readAllLines(Path.of("file.txt"));

// 写入
Files.writeString(Path.of("file.txt"), "content");

// 复制
Files.copy(Path.of("src.txt"), Path.of("dest.txt"));
```

## 面试回答

### 30秒版本

> I/O 流是数据传输的通道。按数据单位分：**字节流**（InputStream/OutputStream）处理二进制，**字符流**（Reader/Writer）处理文本。按功能分：节点流（直连数据源）、处理流（包装增强，如 Buffered）。使用时需 **try-with-resources** 自动关闭。Java 7+ 推荐用 **Files** 工具类。

### 1分钟版本

> **定义**：数据从源到目的地的传输通道
>
> **分类**：
> - 按单位：字节流(二进制) / 字符流(文本)
> - 按功能：节点流(直连) / 处理流(增强)
>
> **核心类**：
> - 字节：InputStream / OutputStream
> - 字符：Reader / Writer
> - 缓冲：BufferedXxx（性能更好）
>
> **选择**：
> - 文本 → 字符流
> - 二进制 → 字节流
>
> **最佳实践**：
> - try-with-resources 自动关闭
> - 使用 Buffered 提高性能
> - Java 7+ 用 Files 工具类

---

*关联文档：[java-encoding-issue.md](java-encoding-issue.md) | [java-network.md](java-network.md)*
