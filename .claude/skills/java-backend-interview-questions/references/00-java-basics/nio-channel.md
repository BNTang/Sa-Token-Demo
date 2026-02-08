# 什么是 Channel？

## Channel 概述

```
┌─────────────────────────────────────────────────────────────┐
│                    NIO Channel                               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   定义: 数据传输的双向通道，类似于流但更强大                │
│                                                             │
│   与 Stream 的区别:                                          │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  Stream (流)              Channel (通道)            │  │
│   │  ─────────────            ──────────────            │  │
│   │  单向 (读或写)            双向 (可读可写)           │  │
│   │  阻塞                     可配置非阻塞              │  │
│   │  直接操作字节             必须配合 Buffer           │  │
│   │  不支持 Selector          支持 Selector             │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   数据流向:                                                  │
│   ┌──────────┐    read()     ┌──────────┐                  │
│   │  Channel │ ────────────→ │  Buffer  │                  │
│   │  通道    │ ←──────────── │  缓冲区  │                  │
│   └──────────┘    write()    └──────────┘                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 主要 Channel 类型

```
┌─────────────────────────────────────────────────────────────┐
│                    Channel 实现类                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   文件通道:                                                  │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  FileChannel          文件读写、内存映射、锁        │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   网络通道:                                                  │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  SocketChannel        TCP 客户端                     │  │
│   │  ServerSocketChannel  TCP 服务端                     │  │
│   │  DatagramChannel      UDP 通信                       │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   管道通道:                                                  │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  Pipe.SourceChannel   管道读端                       │  │
│   │  Pipe.SinkChannel     管道写端                       │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 代码示例

### FileChannel

```java
// 读取文件
try (FileChannel channel = FileChannel.open(
        Path.of("input.txt"), StandardOpenOption.READ)) {
    
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    while (channel.read(buffer) != -1) {
        buffer.flip();  // 切换到读模式
        while (buffer.hasRemaining()) {
            System.out.print((char) buffer.get());
        }
        buffer.clear();  // 清空准备下次写入
    }
}

// 写入文件
try (FileChannel channel = FileChannel.open(
        Path.of("output.txt"), 
        StandardOpenOption.CREATE, 
        StandardOpenOption.WRITE)) {
    
    ByteBuffer buffer = ByteBuffer.wrap("Hello NIO".getBytes());
    channel.write(buffer);
}

// 文件复制 (零拷贝)
try (FileChannel src = FileChannel.open(Path.of("src.txt"), READ);
     FileChannel dest = FileChannel.open(Path.of("dest.txt"), CREATE, WRITE)) {
    
    src.transferTo(0, src.size(), dest);  // 零拷贝传输
}
```

### SocketChannel

```java
// 客户端
SocketChannel client = SocketChannel.open();
client.configureBlocking(false);  // 非阻塞模式
client.connect(new InetSocketAddress("localhost", 8080));

while (!client.finishConnect()) {
    // 等待连接完成
}

ByteBuffer buffer = ByteBuffer.wrap("Hello Server".getBytes());
client.write(buffer);

buffer.clear();
client.read(buffer);
client.close();

// 服务端
ServerSocketChannel server = ServerSocketChannel.open();
server.bind(new InetSocketAddress(8080));
server.configureBlocking(false);

SocketChannel clientChannel = server.accept();  // 非阻塞，可能返回 null
if (clientChannel != null) {
    clientChannel.configureBlocking(false);
    // 处理连接...
}
```

## Buffer 配合使用

```java
// Channel 必须配合 Buffer 使用
ByteBuffer buffer = ByteBuffer.allocate(1024);

// 从 Channel 读数据到 Buffer
int bytesRead = channel.read(buffer);

// 切换 Buffer 模式
buffer.flip();  // 写模式 → 读模式

// 从 Buffer 写数据到 Channel
int bytesWritten = channel.write(buffer);

// Buffer 状态切换
buffer.clear();    // 清空，准备写入
buffer.compact();  // 压缩已读数据，保留未读数据
buffer.rewind();   // 重读，position 归零
```

## 常用方法

```
┌─────────────────────────────────────────────────────────────┐
│                    Channel 常用方法                          │
├───────────────────────┬─────────────────────────────────────┤
│   方法                │   说明                               │
├───────────────────────┼─────────────────────────────────────┤
│   read(Buffer)        │   从通道读取数据到 Buffer            │
│   write(Buffer)       │   从 Buffer 写入数据到通道           │
│   open()              │   打开通道                           │
│   close()             │   关闭通道                           │
│   isOpen()            │   判断通道是否打开                   │
│   configureBlocking() │   设置阻塞/非阻塞模式                │
│   register()          │   注册到 Selector                    │
├───────────────────────┴─────────────────────────────────────┤
│   FileChannel 特有:                                          │
├───────────────────────┬─────────────────────────────────────┤
│   transferTo()        │   零拷贝传输到另一通道               │
│   transferFrom()      │   零拷贝从另一通道接收               │
│   map()               │   内存映射文件                       │
│   lock()              │   文件锁定                           │
└───────────────────────┴─────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> **Channel** 是 NIO 的数据传输通道，与 Stream 相比是**双向的**，可配置**非阻塞**，必须配合 **Buffer** 使用。主要类型：FileChannel（文件）、SocketChannel（TCP客户端）、ServerSocketChannel（TCP服务端）、DatagramChannel（UDP）。FileChannel 支持**零拷贝**（transferTo）和内存映射。

### 1分钟版本

> **定义**：NIO 中数据传输的双向通道
>
> **与 Stream 区别**：
> - 双向 vs 单向
> - 可非阻塞 vs 阻塞
> - 配合 Buffer vs 直接操作
> - 支持 Selector vs 不支持
>
> **主要类型**：
> - FileChannel：文件读写、零拷贝
> - SocketChannel：TCP 客户端
> - ServerSocketChannel：TCP 服务端
> - DatagramChannel：UDP
>
> **常用操作**：
> - read(buffer) / write(buffer)
> - configureBlocking(false)
> - transferTo() 零拷贝

---

*关联文档：[bio-nio-aio.md](bio-nio-aio.md) | [nio-selector.md](nio-selector.md)*
