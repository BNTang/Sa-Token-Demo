# 什么是 Selector？

## Selector 概述

```
┌─────────────────────────────────────────────────────────────┐
│                    NIO Selector 选择器                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   定义: I/O 多路复用器，一个线程监控多个 Channel 的事件     │
│                                                             │
│   核心思想:                                                  │
│   ┌─────────────────────────────────────────────────────┐  │
│   │                                                     │  │
│   │   Channel-1 ──┐                                     │  │
│   │   Channel-2 ──┼──→ Selector ──→ 单线程处理          │  │
│   │   Channel-3 ──┤        │                            │  │
│   │   Channel-N ──┘        ↓                            │  │
│   │                    轮询就绪事件                      │  │
│   │                                                     │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   优势: 减少线程数量，降低系统开销                          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 四种事件类型

```
┌─────────────────────────────────────────────────────────────┐
│                    SelectionKey 事件类型                     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ┌─────────────────┬───────────────────────────────────┐  │
│   │  常量           │  说明                             │  │
│   ├─────────────────┼───────────────────────────────────┤  │
│   │  OP_ACCEPT (16) │  有新连接可接受                   │  │
│   │                 │  适用于 ServerSocketChannel       │  │
│   ├─────────────────┼───────────────────────────────────┤  │
│   │  OP_CONNECT (8) │  连接建立完成                     │  │
│   │                 │  适用于 SocketChannel             │  │
│   ├─────────────────┼───────────────────────────────────┤  │
│   │  OP_READ (1)    │  有数据可读                       │  │
│   │                 │  适用于 SocketChannel             │  │
│   ├─────────────────┼───────────────────────────────────┤  │
│   │  OP_WRITE (4)   │  可以写入数据                     │  │
│   │                 │  适用于 SocketChannel             │  │
│   └─────────────────┴───────────────────────────────────┘  │
│                                                             │
│   可以组合: OP_READ | OP_WRITE                              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 工作流程

```
┌─────────────────────────────────────────────────────────────┐
│                    Selector 工作流程                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 创建 Selector                                          │
│      Selector selector = Selector.open();                   │
│                                                             │
│   2. Channel 注册到 Selector                                │
│      channel.configureBlocking(false);  // 必须非阻塞       │
│      channel.register(selector, SelectionKey.OP_READ);      │
│                                                             │
│   3. 轮询就绪事件                                           │
│      while (true) {                                         │
│          int readyCount = selector.select();  // 阻塞       │
│          if (readyCount == 0) continue;                     │
│                                                             │
│   4. 处理就绪的 Channel                                     │
│          Set<SelectionKey> keys = selector.selectedKeys();  │
│          Iterator<SelectionKey> it = keys.iterator();       │
│          while (it.hasNext()) {                             │
│              SelectionKey key = it.next();                  │
│              it.remove();  // 必须移除                      │
│              if (key.isReadable()) { ... }                  │
│          }                                                  │
│      }                                                      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 完整代码示例

```java
public class NioServer {
    public static void main(String[] args) throws IOException {
        // 1. 创建 Selector
        Selector selector = Selector.open();
        
        // 2. 创建 ServerSocketChannel
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(8080));
        serverChannel.configureBlocking(false);  // 必须非阻塞
        
        // 3. 注册到 Selector，监听 ACCEPT 事件
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        
        System.out.println("服务器启动，监听端口 8080...");
        
        // 4. 轮询事件
        while (true) {
            int readyChannels = selector.select();  // 阻塞等待
            if (readyChannels == 0) continue;
            
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectedKeys.iterator();
            
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();  // 必须手动移除
                
                if (key.isAcceptable()) {
                    // 处理新连接
                    handleAccept(key, selector);
                } else if (key.isReadable()) {
                    // 处理读取
                    handleRead(key);
                }
            }
        }
    }
    
    private static void handleAccept(SelectionKey key, Selector selector) 
            throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);
        System.out.println("新连接: " + clientChannel.getRemoteAddress());
    }
    
    private static void handleRead(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        
        int bytesRead = clientChannel.read(buffer);
        if (bytesRead == -1) {
            key.cancel();
            clientChannel.close();
            return;
        }
        
        buffer.flip();
        String message = StandardCharsets.UTF_8.decode(buffer).toString();
        System.out.println("收到: " + message);
        
        // 回复客户端
        buffer.clear();
        buffer.put(("Echo: " + message).getBytes());
        buffer.flip();
        clientChannel.write(buffer);
    }
}
```

## Selector 方法

```
┌─────────────────────────────────────────────────────────────┐
│                    Selector 常用方法                         │
├───────────────────────┬─────────────────────────────────────┤
│   方法                │   说明                               │
├───────────────────────┼─────────────────────────────────────┤
│   open()              │   创建 Selector                      │
│   select()            │   阻塞等待，直到有就绪事件           │
│   select(timeout)     │   阻塞等待，最多 timeout 毫秒        │
│   selectNow()         │   非阻塞，立即返回                   │
│   wakeup()            │   唤醒阻塞的 select()                │
│   close()             │   关闭 Selector                      │
│   keys()              │   返回所有注册的 SelectionKey        │
│   selectedKeys()      │   返回就绪的 SelectionKey            │
└───────────────────────┴─────────────────────────────────────┘
```

## 注意事项

```
┌─────────────────────────────────────────────────────────────┐
│                    使用注意事项                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. Channel 必须是非阻塞模式                               │
│      channel.configureBlocking(false);                      │
│                                                             │
│   2. 处理后必须从 selectedKeys 中移除                       │
│      iterator.remove();  // 否则下次还会处理                │
│                                                             │
│   3. FileChannel 不能注册到 Selector                        │
│      只有网络 Channel 可以                                  │
│                                                             │
│   4. 空轮询 bug (JDK NIO bug)                               │
│      select() 无事件却返回 0，导致 CPU 100%                 │
│      Netty 已解决此问题                                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> **Selector** 是 NIO 的多路复用器，一个线程通过 Selector 监控多个 Channel 的事件。四种事件：**ACCEPT**（新连接）、**CONNECT**（连接完成）、**READ**（可读）、**WRITE**（可写）。Channel 必须**非阻塞**才能注册，处理后需手动从 selectedKeys 移除。底层用 epoll/select 系统调用。

### 1分钟版本

> **定义**：I/O 多路复用器，一个线程监控多个 Channel
>
> **四种事件**：
> - OP_ACCEPT：新连接到达
> - OP_CONNECT：连接建立完成
> - OP_READ：有数据可读
> - OP_WRITE：可以写入
>
> **使用流程**：
> 1. Selector.open() 创建
> 2. channel.register() 注册
> 3. selector.select() 阻塞等待
> 4. 遍历 selectedKeys 处理
>
> **注意事项**：
> - Channel 必须非阻塞
> - 处理后要 iterator.remove()
> - 有空轮询 bug，Netty 已解决

---

*关联文档：[bio-nio-aio.md](bio-nio-aio.md) | [nio-channel.md](nio-channel.md)*
