# I/O 模型详解

> 分类: 操作系统/网络编程 | 难度: ⭐⭐⭐⭐ | 频率: 高频

---

## 一、五种 I/O 模型

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                         五种 I/O 模型概览                                         │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  1. 阻塞 I/O (Blocking I/O)                                                      │
│  2. 非阻塞 I/O (Non-blocking I/O)                                                │
│  3. I/O 多路复用 (I/O Multiplexing)                                              │
│  4. 信号驱动 I/O (Signal-driven I/O)                                             │
│  5. 异步 I/O (Asynchronous I/O)                                                  │
│                                                                                  │
│  核心区别: 等待数据准备和拷贝数据两个阶段是否阻塞                                 │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 二、阻塞 I/O (BIO)

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          阻塞 I/O 模型                                            │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│   应用程序                                       内核                            │
│       │                                           │                              │
│       │ ─────── read() 系统调用 ─────────────────→│                              │
│       │                                           │ 等待数据准备                 │
│       │                                           │     (网卡→内核缓冲区)        │
│   阻塞等待...                                     │          ↓                   │
│       │                                           │ 拷贝数据到用户空间           │
│       │                                           │     (内核缓冲区→用户空间)    │
│       │ ←────────── 返回数据 ─────────────────────│                              │
│       │                                           │                              │
│       ↓                                                                          │
│   处理数据                                                                       │
│                                                                                  │
│   特点: 两个阶段都阻塞                                                           │
│   优点: 简单直观                                                                 │
│   缺点: 一个线程只能处理一个连接                                                 │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// Java BIO 示例
public class BioServer {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(8080);
        
        while (true) {
            Socket socket = serverSocket.accept();  // 阻塞等待连接
            
            // 每个连接一个线程
            new Thread(() -> {
                try {
                    InputStream in = socket.getInputStream();
                    byte[] buffer = new byte[1024];
                    int len = in.read(buffer);  // 阻塞等待数据
                    System.out.println(new String(buffer, 0, len));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
```

---

## 三、非阻塞 I/O (NIO)

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          非阻塞 I/O 模型                                          │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│   应用程序                                       内核                            │
│       │                                           │                              │
│       │ ─────── read() ──────────────────────────→│ 数据未准备好                 │
│       │ ←────── 返回 EAGAIN ─────────────────────│                              │
│       │                                           │                              │
│       │ ─────── read() ──────────────────────────→│ 数据未准备好                 │
│       │ ←────── 返回 EAGAIN ─────────────────────│                              │
│       │                                           │                              │
│   轮询...                                         │                              │
│       │                                           │                              │
│       │ ─────── read() ──────────────────────────→│ 数据准备好                   │
│       │                                           │ 拷贝数据到用户空间           │
│       │ ←────────── 返回数据 ─────────────────────│                              │
│       │                                           │                              │
│                                                                                  │
│   特点: 第一阶段不阻塞(轮询)，第二阶段阻塞                                        │
│   优点: 不会阻塞在等待数据上                                                     │
│   缺点: 需要不断轮询，浪费 CPU                                                   │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 四、I/O 多路复用 (Select/Poll/Epoll)

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          I/O 多路复用模型                                         │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│   应用程序                                       内核                            │
│       │                                           │                              │
│       │ ─────── select(fds) ─────────────────────→│                              │
│       │          监听多个fd                        │ 等待任一fd就绪              │
│   阻塞等待...                                     │                              │
│       │ ←────── 返回就绪fd数量 ───────────────────│                              │
│       │                                           │                              │
│       │ ─────── read(ready_fd) ──────────────────→│                              │
│       │                                           │ 拷贝数据                     │
│       │ ←────────── 返回数据 ─────────────────────│                              │
│       │                                           │                              │
│                                                                                  │
│   特点: 一个线程可以同时监听多个文件描述符                                        │
│   优点: 单线程处理大量连接，高效                                                 │
│   缺点: 需要两次系统调用 (select + read)                                         │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// Java NIO 示例 (使用 Selector)
public class NioServer {
    public static void main(String[] args) throws IOException {
        Selector selector = Selector.open();
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(8080));
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        
        while (true) {
            selector.select();  // 阻塞直到有事件就绪
            
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectedKeys.iterator();
            
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                
                if (key.isAcceptable()) {
                    // 处理新连接
                    SocketChannel clientChannel = serverChannel.accept();
                    clientChannel.configureBlocking(false);
                    clientChannel.register(selector, SelectionKey.OP_READ);
                } else if (key.isReadable()) {
                    // 读取数据
                    SocketChannel clientChannel = (SocketChannel) key.channel();
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    clientChannel.read(buffer);
                    buffer.flip();
                    System.out.println(new String(buffer.array(), 0, buffer.limit()));
                }
            }
        }
    }
}
```

---

## 五、信号驱动 I/O

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          信号驱动 I/O 模型                                        │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│   应用程序                                       内核                            │
│       │                                           │                              │
│       │ ─────── sigaction(SIGIO) ────────────────→│ 注册信号处理                │
│       │ ←────── 立即返回 ────────────────────────│                              │
│       │                                           │                              │
│   继续执行其他任务...                              │ 等待数据准备                │
│       │                                           │                              │
│       │ ←──────── SIGIO 信号 ─────────────────────│ 数据准备好                  │
│       │                                           │                              │
│       │ ─────── read() ──────────────────────────→│                              │
│       │                                           │ 拷贝数据                     │
│       │ ←────────── 返回数据 ─────────────────────│                              │
│       │                                           │                              │
│                                                                                  │
│   特点: 数据准备好后内核发送信号通知应用                                          │
│   优点: 第一阶段非阻塞                                                           │
│   缺点: 信号处理复杂，很少使用                                                   │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 六、异步 I/O (AIO)

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          异步 I/O 模型                                            │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│   应用程序                                       内核                            │
│       │                                           │                              │
│       │ ─────── aio_read() ──────────────────────→│                              │
│       │ ←────── 立即返回 ────────────────────────│                              │
│       │                                           │                              │
│   继续执行其他任务...                              │ 等待数据准备                │
│       │                                           │ 拷贝数据到用户空间          │
│       │                                           │                              │
│       │ ←──────── 完成通知 ───────────────────────│ 全部完成                    │
│       │           (信号/回调)                      │                              │
│       │                                           │                              │
│   处理数据 (数据已在用户空间)                                                     │
│                                                                                  │
│   特点: 两个阶段都不阻塞，内核完成所有工作后通知应用                              │
│   优点: 真正的异步，应用程序无需等待                                             │
│   缺点: 实现复杂，Linux 支持有限                                                 │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// Java AIO 示例
public class AioServer {
    public static void main(String[] args) throws Exception {
        AsynchronousServerSocketChannel serverChannel = 
            AsynchronousServerSocketChannel.open().bind(new InetSocketAddress(8080));
        
        serverChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
            @Override
            public void completed(AsynchronousSocketChannel clientChannel, Void attachment) {
                // 继续接受新连接
                serverChannel.accept(null, this);
                
                // 异步读取数据
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                clientChannel.read(buffer, buffer, new CompletionHandler<Integer, ByteBuffer>() {
                    @Override
                    public void completed(Integer result, ByteBuffer buf) {
                        buf.flip();
                        System.out.println(new String(buf.array(), 0, result));
                    }
                    
                    @Override
                    public void failed(Throwable exc, ByteBuffer buf) {
                        exc.printStackTrace();
                    }
                });
            }
            
            @Override
            public void failed(Throwable exc, Void attachment) {
                exc.printStackTrace();
            }
        });
        
        Thread.sleep(Long.MAX_VALUE);  // 保持运行
    }
}
```

---

## 七、模型对比

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          I/O 模型对比                                             │
├───────────────────┬────────────────┬────────────────┬──────────────────────────┤
│       模型        │  数据准备阶段  │  数据拷贝阶段  │         使用场景          │
├───────────────────┼────────────────┼────────────────┼──────────────────────────┤
│    阻塞 I/O       │     阻塞       │     阻塞       │ 连接数少，编程简单        │
│    非阻塞 I/O     │   非阻塞(轮询) │     阻塞       │ 很少单独使用              │
│    I/O 多路复用   │   阻塞(select) │     阻塞       │ 高并发服务器(Nginx/Redis) │
│    信号驱动 I/O   │   非阻塞(信号) │     阻塞       │ 很少使用                  │
│    异步 I/O       │     非阻塞     │     非阻塞     │ 需要完全异步的场景        │
├───────────────────┴────────────────┴────────────────┴──────────────────────────┤
│                                                                                  │
│  同步 vs 异步:                                                                   │
│  - 同步: 数据拷贝阶段需要应用程序参与 (前4种)                                    │
│  - 异步: 数据拷贝由内核完成，完成后通知应用 (仅 AIO)                             │
│                                                                                  │
│  阻塞 vs 非阻塞:                                                                 │
│  - 阻塞: 调用会等待直到完成                                                      │
│  - 非阻塞: 调用立即返回，可能需要轮询                                            │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 八、面试回答

### 30秒版本

> 常见的 I/O 模型有5种：
> 1. **阻塞 I/O**：两阶段都阻塞，简单但性能差
> 2. **非阻塞 I/O**：轮询检查，浪费 CPU
> 3. **I/O 多路复用**：select/poll/epoll，一个线程监听多个连接，主流方案
> 4. **信号驱动 I/O**：数据就绪发信号，很少用
> 5. **异步 I/O**：内核完成所有工作后通知，真正的异步
>
> 前4种是同步 I/O，只有 AIO 是真正的异步 I/O。

### 1分钟版本

> I/O 操作分为两个阶段：**等待数据准备**和**拷贝数据到用户空间**。
>
> **阻塞 I/O**：两个阶段都阻塞，线程无法做其他事，一个连接一个线程，并发能力差。
>
> **非阻塞 I/O**：第一阶段非阻塞，应用程序需要不断轮询检查数据是否就绪，浪费 CPU。
>
> **I/O 多路复用**：使用 select/poll/epoll 一个线程同时监听多个连接，有事件就绪才处理，是 Nginx、Redis、Netty 等高性能框架的基础。
>
> **异步 I/O**：两个阶段都非阻塞，应用程序发起读请求后立即返回，内核完成数据准备和拷贝后通知应用，是真正的异步。
>
> Java 中 BIO 对应阻塞 I/O，NIO 基于 I/O 多路复用，AIO 对应异步 I/O。

---

## 九、最佳实践

### ✅ 推荐做法

```java
// 1. 高并发服务器使用 NIO + Selector 或 Netty
EventLoopGroup bossGroup = new NioEventLoopGroup(1);
EventLoopGroup workerGroup = new NioEventLoopGroup();

// 2. 文件 I/O 可考虑 AIO
AsynchronousFileChannel.open(path, StandardOpenOption.READ)
    .read(buffer, 0, buffer, handler);

// 3. 简单场景可以使用 BIO + 线程池
ExecutorService executor = Executors.newFixedThreadPool(100);
```

### ❌ 避免做法

```java
// ❌ 高并发场景使用 BIO + 每连接一个线程
while (true) {
    Socket socket = serverSocket.accept();
    new Thread(() -> handle(socket)).start();  // 线程数爆炸
}

// ❌ 非阻塞 I/O 忙轮询
while (true) {
    if (channel.read(buffer) > 0) {
        // 处理数据
    }
    // 没有 sleep，CPU 100%
}
```
