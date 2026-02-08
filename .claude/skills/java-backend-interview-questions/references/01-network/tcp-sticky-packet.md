# TCP 的粘包和拆包能说说吗？

## 什么是粘包和拆包

```
┌─────────────────────────────────────────────────────────────┐
│                    粘包与拆包                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   TCP 是字节流协议，没有消息边界！                           │
│                                                             │
│   发送端发送:                                                │
│   ┌─────────┐  ┌─────────┐  ┌─────────┐                    │
│   │ 消息1   │  │ 消息2   │  │ 消息3   │                    │
│   └─────────┘  └─────────┘  └─────────┘                    │
│                                                             │
│   接收端可能收到:                                            │
│                                                             │
│   正常情况:                                                  │
│   ┌─────────┐  ┌─────────┐  ┌─────────┐                    │
│   │ 消息1   │  │ 消息2   │  │ 消息3   │                    │
│   └─────────┘  └─────────┘  └─────────┘                    │
│                                                             │
│   粘包: (多个消息合并成一个包)                               │
│   ┌───────────────────────────────────┐                    │
│   │ 消息1 + 消息2 + 消息3             │                    │
│   └───────────────────────────────────┘                    │
│                                                             │
│   拆包: (一个消息被拆成多个包)                               │
│   ┌─────────┐  ┌─────────┐                                 │
│   │ 消息1前半│  │消息1后半│                                 │
│   └─────────┘  └─────────┘                                 │
│                                                             │
│   粘包+拆包: (组合情况)                                      │
│   ┌─────────────────────┐  ┌───────────────┐               │
│   │ 消息1 + 消息2前半    │  │消息2后半+消息3│               │
│   └─────────────────────┘  └───────────────┘               │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 产生原因

```
┌─────────────────────────────────────────────────────────────┐
│                    产生原因                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   粘包原因:                                                  │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  1. Nagle 算法 (发送端)                              │  │
│   │     - 小数据包合并发送，减少网络开销                 │  │
│   │     - 等待一定时间或数据量后一起发送                 │  │
│   │                                                     │  │
│   │  2. 接收缓冲区累积 (接收端)                          │  │
│   │     - 应用程序读取不及时                             │  │
│   │     - 多个数据包堆积在缓冲区                         │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   拆包原因:                                                  │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  1. 数据包超过 MSS (最大报文段长度)                  │  │
│   │     - TCP 分段传输                                   │  │
│   │                                                     │  │
│   │  2. 数据包超过 MTU (最大传输单元)                    │  │
│   │     - IP 层分片                                      │  │
│   │                                                     │  │
│   │  3. 发送缓冲区满                                     │  │
│   │     - 先发送部分数据                                 │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   根本原因: TCP 是字节流协议，没有消息边界                   │
│   UDP 不会有粘包问题，因为 UDP 是数据报协议，有边界          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 解决方案

### 方案1：固定长度

```java
/**
 * 固定长度方案
 * 每个消息固定 100 字节，不足补空格
 */
public class FixedLengthProtocol {
    private static final int MESSAGE_LENGTH = 100;
    
    // 发送
    public void send(OutputStream out, String message) throws IOException {
        byte[] bytes = new byte[MESSAGE_LENGTH];
        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(data, 0, bytes, 0, Math.min(data.length, MESSAGE_LENGTH));
        out.write(bytes);
    }
    
    // 接收
    public String receive(InputStream in) throws IOException {
        byte[] bytes = new byte[MESSAGE_LENGTH];
        int read = 0;
        while (read < MESSAGE_LENGTH) {
            int len = in.read(bytes, read, MESSAGE_LENGTH - read);
            if (len == -1) throw new EOFException();
            read += len;
        }
        return new String(bytes, StandardCharsets.UTF_8).trim();
    }
}
```

### 方案2：分隔符

```java
/**
 * 分隔符方案
 * 使用特殊字符分隔消息
 */
public class DelimiterProtocol {
    private static final String DELIMITER = "\r\n";
    
    // 发送
    public void send(OutputStream out, String message) throws IOException {
        out.write((message + DELIMITER).getBytes(StandardCharsets.UTF_8));
        out.flush();
    }
    
    // 接收
    public String receive(BufferedReader reader) throws IOException {
        return reader.readLine();  // 读到 \r\n 结束
    }
}
```

### 方案3：长度字段（推荐）

```java
/**
 * 长度字段方案（最常用）
 * 消息格式: [4字节长度][消息体]
 */
public class LengthFieldProtocol {
    
    // 发送
    public void send(OutputStream out, String message) throws IOException {
        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        // 写入长度 (4字节)
        out.write((data.length >> 24) & 0xFF);
        out.write((data.length >> 16) & 0xFF);
        out.write((data.length >> 8) & 0xFF);
        out.write(data.length & 0xFF);
        // 写入消息体
        out.write(data);
        out.flush();
    }
    
    // 接收
    public String receive(InputStream in) throws IOException {
        // 读取长度
        byte[] lengthBytes = new byte[4];
        readFully(in, lengthBytes);
        int length = ((lengthBytes[0] & 0xFF) << 24)
                   | ((lengthBytes[1] & 0xFF) << 16)
                   | ((lengthBytes[2] & 0xFF) << 8)
                   | (lengthBytes[3] & 0xFF);
        
        // 读取消息体
        byte[] data = new byte[length];
        readFully(in, data);
        return new String(data, StandardCharsets.UTF_8);
    }
    
    private void readFully(InputStream in, byte[] buffer) throws IOException {
        int read = 0;
        while (read < buffer.length) {
            int len = in.read(buffer, read, buffer.length - read);
            if (len == -1) throw new EOFException();
            read += len;
        }
    }
}
```

## Netty 解决方案

```java
/**
 * Netty 提供的解码器
 */
public class NettyDecoders {
    
    public void configureFixedLength(ChannelPipeline pipeline) {
        // 固定长度解码器
        pipeline.addLast(new FixedLengthFrameDecoder(100));
    }
    
    public void configureDelimiter(ChannelPipeline pipeline) {
        // 分隔符解码器
        ByteBuf delimiter = Unpooled.copiedBuffer("\r\n".getBytes());
        pipeline.addLast(new DelimiterBasedFrameDecoder(1024, delimiter));
    }
    
    public void configureLengthField(ChannelPipeline pipeline) {
        // 长度字段解码器（最常用）
        pipeline.addLast(new LengthFieldBasedFrameDecoder(
            65535,  // maxFrameLength
            0,      // lengthFieldOffset
            4,      // lengthFieldLength
            0,      // lengthAdjustment
            4       // initialBytesToStrip
        ));
        // 对应的编码器
        pipeline.addLast(new LengthFieldPrepender(4));
    }
}
```

```
┌─────────────────────────────────────────────────────────────┐
│                    Netty 解码器                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   LengthFieldBasedFrameDecoder 参数说明:                     │
│                                                             │
│   ┌──────────────────────────────────────────────────────┐ │
│   │  offset=0, length=4                                  │ │
│   │  ┌────────────┬─────────────────────────────────┐   │ │
│   │  │  长度(4B)   │         消息体                  │   │ │
│   │  └────────────┴─────────────────────────────────┘   │ │
│   │                                                      │ │
│   │  offset=2, length=4 (有Header)                       │ │
│   │  ┌──────────┬────────────┬─────────────────────┐    │ │
│   │  │Header(2B)│  长度(4B)   │       消息体        │    │ │
│   │  └──────────┴────────────┴─────────────────────┘    │ │
│   └──────────────────────────────────────────────────────┘ │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 方案对比

```
┌─────────────────────────────────────────────────────────────┐
│                    方案对比                                  │
├──────────────┬──────────────────────────────────────────────┤
│   方案        │   优缺点                                     │
├──────────────┼──────────────────────────────────────────────┤
│   固定长度    │ ✓ 简单                                       │
│              │ ✗ 浪费空间，不适合变长消息                   │
├──────────────┼──────────────────────────────────────────────┤
│   分隔符      │ ✓ 简单，适合文本协议                        │
│              │ ✗ 消息内容不能包含分隔符                     │
│              │ ✗ 需要扫描全部内容                           │
├──────────────┼──────────────────────────────────────────────┤
│   长度字段    │ ✓ 灵活，支持变长消息                        │
│   (推荐)      │ ✓ 解析高效，直接跳到消息体                  │
│              │ ✗ 实现相对复杂                               │
├──────────────┴──────────────────────────────────────────────┤
│                                                             │
│   生产建议：使用 长度字段 方案 + Netty LengthFieldDecoder    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> **粘包**：多个消息合并成一个包；**拆包**：一个消息被拆成多个包。原因是 TCP 是字节流协议，没有消息边界。解决方案：1）**固定长度**；2）**分隔符**；3）**长度字段**（推荐）。Netty 提供 `LengthFieldBasedFrameDecoder` 解决。

### 1分钟版本

> **问题本质**：
> - TCP 是字节流协议，没有消息边界
> - UDP 是数据报协议，不会有粘包问题
>
> **产生原因**：
> - 粘包：Nagle算法合并小包、接收方读取不及时
> - 拆包：数据超过MSS/MTU、发送缓冲区满
>
> **解决方案**：
> 1. **固定长度**：每个消息固定字节数，简单但浪费空间
> 2. **分隔符**：如 `\r\n`，适合文本协议，内容不能含分隔符
> 3. **长度字段**（推荐）：`[4字节长度][消息体]`，灵活高效
>
> **Netty 实现**：
> - `FixedLengthFrameDecoder` - 固定长度
> - `DelimiterBasedFrameDecoder` - 分隔符
> - `LengthFieldBasedFrameDecoder` - 长度字段（推荐）

---

*关联文档：[tcp-vs-udp.md](tcp-vs-udp.md) | [netty-performance.md](../05-concurrency/netty-performance.md)*
