# 什么是 BIO、NIO、AIO？

## I/O 模型概述

```
┌─────────────────────────────────────────────────────────────┐
│                    Java I/O 模型演进                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   BIO (Blocking I/O)           JDK 1.0                      │
│   └── 同步阻塞 I/O                                          │
│                                                             │
│   NIO (Non-blocking I/O)       JDK 1.4                      │
│   └── 同步非阻塞 I/O (多路复用)                             │
│                                                             │
│   AIO (Asynchronous I/O)       JDK 1.7                      │
│   └── 异步非阻塞 I/O                                        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 三种 I/O 模型对比

```
┌─────────────────────────────────────────────────────────────┐
│                    BIO - 同步阻塞                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   客户端                    服务端                          │
│   ┌─────┐                  ┌─────────────────────────┐     │
│   │  C1 │ ───────────────→ │ Thread-1 (阻塞等待)    │     │
│   └─────┘                  └─────────────────────────┘     │
│   ┌─────┐                  ┌─────────────────────────┐     │
│   │  C2 │ ───────────────→ │ Thread-2 (阻塞等待)    │     │
│   └─────┘                  └─────────────────────────┘     │
│   ┌─────┐                  ┌─────────────────────────┐     │
│   │  C3 │ ───────────────→ │ Thread-3 (阻塞等待)    │     │
│   └─────┘                  └─────────────────────────┘     │
│                                                             │
│   特点: 一个连接一个线程，线程阻塞等待数据                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    NIO - 同步非阻塞 (多路复用)               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   客户端                    服务端                          │
│   ┌─────┐                  ┌─────────────────────────┐     │
│   │  C1 │ ──┐              │                         │     │
│   └─────┘   │              │                         │     │
│   ┌─────┐   │  ┌────────┐  │    ┌────────────────┐  │     │
│   │  C2 │ ──┼─→│Selector│──┼───→│ 单线程/少量线程 │  │     │
│   └─────┘   │  └────────┘  │    └────────────────┘  │     │
│   ┌─────┐   │              │                         │     │
│   │  C3 │ ──┘              │                         │     │
│   └─────┘                  └─────────────────────────┘     │
│                                                             │
│   特点: 一个线程处理多个连接，Selector轮询就绪事件          │
│                                                             │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    AIO - 异步非阻塞                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   客户端                    服务端                          │
│   ┌─────┐                  ┌─────────────────────────┐     │
│   │  C1 │ ──┐              │   操作系统内核          │     │
│   └─────┘   │              │   ┌─────────────────┐   │     │
│   ┌─────┐   │              │   │  异步I/O完成后  │   │     │
│   │  C2 │ ──┼─────────────→│   │  回调通知应用   │   │     │
│   └─────┘   │              │   └─────────────────┘   │     │
│   ┌─────┐   │              │                         │     │
│   │  C3 │ ──┘              └─────────────────────────┘     │
│   └─────┘                                                   │
│                                                             │
│   特点: 发起I/O后立即返回，完成后OS回调通知                 │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 详细对比

```
┌──────────────┬──────────────────┬──────────────────┬──────────────────┐
│   特性       │   BIO            │   NIO            │   AIO            │
├──────────────┼──────────────────┼──────────────────┼──────────────────┤
│   I/O模型    │   同步阻塞       │   同步非阻塞     │   异步非阻塞     │
│   编程复杂度 │   简单           │   复杂           │   复杂           │
│   连接数     │   1:1 线程       │   多路复用       │   回调模式       │
│   吞吐量     │   低             │   高             │   高             │
│   资源消耗   │   高(线程多)     │   低             │   低             │
│   适用场景   │   连接数少       │   高并发         │   高并发         │
│   JDK版本    │   1.0            │   1.4            │   1.7            │
│   核心类     │   Stream         │   Channel+Buffer │   Async Channel  │
│              │   Socket         │   +Selector      │   +Callback      │
└──────────────┴──────────────────┴──────────────────┴──────────────────┘
```

## 代码示例

### BIO

```java
// BIO 服务端
ServerSocket serverSocket = new ServerSocket(8080);
while (true) {
    Socket socket = serverSocket.accept();  // 阻塞等待连接
    new Thread(() -> {
        try {
            InputStream is = socket.getInputStream();
            byte[] buffer = new byte[1024];
            int len = is.read(buffer);  // 阻塞等待数据
            // 处理数据...
        } catch (IOException e) {
            e.printStackTrace();
        }
    }).start();
}
```

### NIO

```java
// NIO 服务端
Selector selector = Selector.open();
ServerSocketChannel serverChannel = ServerSocketChannel.open();
serverChannel.bind(new InetSocketAddress(8080));
serverChannel.configureBlocking(false);
serverChannel.register(selector, SelectionKey.OP_ACCEPT);

while (true) {
    selector.select();  // 阻塞等待就绪事件
    Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
    while (keys.hasNext()) {
        SelectionKey key = keys.next();
        keys.remove();
        if (key.isAcceptable()) {
            // 处理连接
            SocketChannel client = serverChannel.accept();
            client.configureBlocking(false);
            client.register(selector, SelectionKey.OP_READ);
        } else if (key.isReadable()) {
            // 处理读取
            SocketChannel client = (SocketChannel) key.channel();
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            client.read(buffer);
        }
    }
}
```

### AIO

```java
// AIO 服务端
AsynchronousServerSocketChannel server = 
    AsynchronousServerSocketChannel.open().bind(new InetSocketAddress(8080));

server.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
    @Override
    public void completed(AsynchronousSocketChannel client, Void attachment) {
        server.accept(null, this);  // 继续接受下一个连接
        
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        client.read(buffer, buffer, new CompletionHandler<Integer, ByteBuffer>() {
            @Override
            public void completed(Integer result, ByteBuffer buf) {
                // 异步读取完成，处理数据
            }
            @Override
            public void failed(Throwable exc, ByteBuffer buf) { }
        });
    }
    @Override
    public void failed(Throwable exc, Void attachment) { }
});
```

## 使用场景

```
┌─────────────────────────────────────────────────────────────┐
│                    使用场景                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   BIO:                                                       │
│   • 连接数少且固定                                          │
│   • 简单的请求/响应模式                                     │
│   • 低并发场景                                              │
│                                                             │
│   NIO:                                                       │
│   • 高并发场景 (聊天服务器、游戏服务器)                     │
│   • 连接多但请求轻量                                        │
│   • Netty、Mina 等框架底层                                  │
│                                                             │
│   AIO:                                                       │
│   • 连接数多且重操作 (大文件传输)                           │
│   • Windows IOCP 性能好                                     │
│   • Linux 上优势不明显 (epoll 模拟)                         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> **BIO**（同步阻塞）：一个连接一个线程，read/write 阻塞，适合低并发。**NIO**（同步非阻塞）：一个线程处理多个连接，Selector 多路复用，适合高并发。**AIO**（异步非阻塞）：发起 I/O 立即返回，完成后 OS 回调通知，Linux 优势不大。实际生产用 **Netty**（基于 NIO）。

### 1分钟版本

> **BIO**：
> - 同步阻塞，一连接一线程
> - 简单但并发能力差
>
> **NIO**：
> - 同步非阻塞，多路复用
> - Channel + Buffer + Selector
> - 一个线程处理多个连接
>
> **AIO**：
> - 异步非阻塞，回调模式
> - 发起 I/O 立即返回
> - Linux 用 epoll 模拟，优势不大
>
> **生产选择**：
> - 推荐 Netty（封装 NIO）
> - 解决了 NIO 的复杂性和 bug

---

*关联文档：[nio-channel.md](nio-channel.md) | [nio-selector.md](nio-selector.md)*
