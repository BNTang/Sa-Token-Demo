# TCP 滑动窗口

> Java 后端面试知识点 - TCP 流量控制与效率优化

---

## 什么是滑动窗口

滑动窗口是 TCP 实现**流量控制**和**提高传输效率**的核心机制。

```
没有滑动窗口（停等协议）：
─────────────────────────
发送方                          接收方
   │                               │
   │─── 数据1 ────────────────────>│
   │<── ACK1 ──────────────────────│  等待确认后才发下一个
   │─── 数据2 ────────────────────>│
   │<── ACK2 ──────────────────────│
   │─── 数据3 ────────────────────>│
   │<── ACK3 ──────────────────────│
   
   效率低！每次只能发一个包

有滑动窗口：
─────────────────────────
发送方                          接收方
   │                               │
   │─── 数据1 ────────────────────>│
   │─── 数据2 ────────────────────>│  窗口内可以连续发送
   │─── 数据3 ────────────────────>│
   │<── ACK1 ──────────────────────│
   │─── 数据4 ────────────────────>│  收到 ACK 后窗口滑动
   │<── ACK2 ──────────────────────│
   │─── 数据5 ────────────────────>│
   
   效率高！管道化传输
```

---

## 滑动窗口的作用

### 1. 流量控制

防止发送方发送速度超过接收方处理能力。

```
接收方通告窗口大小：
─────────────────────────

接收方缓冲区：[已处理|待处理空间|空闲空间]
                        ↑
                   可接收窗口 = 空闲空间

发送方                          接收方
   │                               │
   │<── ACK, Window=5000 ──────────│  接收方告知还能接收 5000 字节
   │                               │
   │─── 发送 3000 字节 ───────────>│
   │                               │
   │<── ACK, Window=2000 ──────────│  窗口缩小
   │                               │
   │   发送方最多再发 2000 字节     │
```

### 2. 提高传输效率

不用等待每个包的确认，可以连续发送多个包。

```
窗口大小 = 4 的发送过程：

发送窗口状态：
┌───┬───┬───┬───┬───┬───┬───┬───┬───┬───┐
│ 1 │ 2 │ 3 │ 4 │ 5 │ 6 │ 7 │ 8 │ 9 │10 │
└───┴───┴───┴───┴───┴───┴───┴───┴───┴───┘
  ↑───────────↑
     窗口=4

收到 ACK1 后窗口滑动：
┌───┬───┬───┬───┬───┬───┬───┬───┬───┬───┐
│ 1 │ 2 │ 3 │ 4 │ 5 │ 6 │ 7 │ 8 │ 9 │10 │
└───┴───┴───┴───┴───┴───┴───┴───┴───┴───┘
  ✓   ↑───────────↑
         窗口=4（右移）
```

---

## 发送方窗口分区

```
┌─────────────┬─────────────────────────┬──────────────────────┐
│  已发送已确认  │    已发送未确认           │    可发送未发送        │  不可发送
│  (可回收)     │    (在途)                │    (窗口内)           │  (窗口外)
└─────────────┴─────────────────────────┴──────────────────────┘
              ↑                         ↑
           窗口起点                   窗口终点
              
              ←──────── 发送窗口 ────────→
```

---

## 拥塞控制与滑动窗口

滑动窗口配合拥塞控制算法工作：

### 拥塞窗口（cwnd）

```
实际发送窗口 = min(接收窗口 rwnd, 拥塞窗口 cwnd)

拥塞控制四个阶段：
1. 慢启动：cwnd 指数增长（1→2→4→8...）
2. 拥塞避免：cwnd 线性增长（到达阈值后）
3. 拥塞发生：检测到丢包，cwnd 减半
4. 快速恢复：快速重传后的恢复阶段
```

---

## 编码实践

### Socket 缓冲区配置

```java
// ✅ 配置 Socket 缓冲区大小
public class SocketConfig {
    
    public Socket configureSocket(Socket socket) throws SocketException {
        // 发送缓冲区（影响发送窗口）
        socket.setSendBufferSize(64 * 1024);  // 64KB
        
        // 接收缓冲区（影响接收窗口）
        socket.setReceiveBufferSize(64 * 1024);  // 64KB
        
        // TCP_NODELAY：禁用 Nagle 算法
        // 小包立即发送，减少延迟（适合实时应用）
        socket.setTcpNoDelay(true);
        
        return socket;
    }
}
```

### Netty 配置

```java
// ✅ Netty 服务端配置
ServerBootstrap bootstrap = new ServerBootstrap();
bootstrap.group(bossGroup, workerGroup)
    .channel(NioServerSocketChannel.class)
    // 服务端接收缓冲区
    .option(ChannelOption.SO_RCVBUF, 64 * 1024)
    // 客户端连接的缓冲区
    .childOption(ChannelOption.SO_SNDBUF, 64 * 1024)
    .childOption(ChannelOption.SO_RCVBUF, 64 * 1024)
    // 禁用 Nagle 算法
    .childOption(ChannelOption.TCP_NODELAY, true)
    // 开启 TCP KeepAlive
    .childOption(ChannelOption.SO_KEEPALIVE, true);
```

### 高吞吐量场景优化

```java
// ✅ 批量发送数据，减少系统调用
public class BatchSender {
    
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final int batchSize = 4096;
    
    public void send(OutputStream out, byte[] data) throws IOException {
        buffer.write(data);
        
        // 达到批量大小时发送
        if (buffer.size() >= batchSize) {
            flush(out);
        }
    }
    
    public void flush(OutputStream out) throws IOException {
        if (buffer.size() > 0) {
            out.write(buffer.toByteArray());
            out.flush();
            buffer.reset();
        }
    }
}
```

### Linux 内核参数调优

```bash
# /etc/sysctl.conf

# TCP 接收/发送缓冲区（最小值、默认值、最大值）
net.ipv4.tcp_rmem = 4096 87380 6291456
net.ipv4.tcp_wmem = 4096 65536 4194304

# 开启窗口缩放（支持大于 64KB 的窗口）
net.ipv4.tcp_window_scaling = 1

# TCP 内存使用限制
net.ipv4.tcp_mem = 786432 1048576 1572864
```

---

## 面试要点

### 核心答案

**问：TCP 滑动窗口的作用是什么？**

答：滑动窗口有两个主要作用：

1. **流量控制**
   - 接收方通告窗口大小告知发送方自己的处理能力
   - 发送方根据窗口大小限制发送速率
   - 防止发送过快导致接收方缓冲区溢出

2. **提高传输效率**
   - 允许发送多个数据包后再等待确认（管道化）
   - 无需停等每个包的 ACK
   - 充分利用网络带宽

### 延伸问题

**问：滑动窗口和拥塞窗口的区别？**
- **滑动窗口（rwnd）**：接收方告知的窗口，用于流量控制
- **拥塞窗口（cwnd）**：发送方维护的窗口，用于拥塞控制
- **实际窗口** = min(rwnd, cwnd)

**问：什么是零窗口？**
答：接收方缓冲区满时通告窗口为 0，发送方暂停发送。发送方会定期发送窗口探测包，检查窗口是否恢复。

---

## 编码最佳实践

### ✅ 推荐做法

```java
// 1. 根据场景配置缓冲区大小
// 大文件传输：增大缓冲区
socket.setSendBufferSize(256 * 1024);
socket.setReceiveBufferSize(256 * 1024);

// 2. 实时应用禁用 Nagle
socket.setTcpNoDelay(true);

// 3. 使用 NIO 提高并发能力
Selector selector = Selector.open();
channel.configureBlocking(false);
channel.register(selector, SelectionKey.OP_READ);

// 4. 批量写入减少系统调用
ByteBuffer[] buffers = ...; 
channel.write(buffers);  // Gathering Write
```

### ❌ 避免做法

```java
// ❌ 逐字节发送
for (byte b : data) {
    out.write(b);  // 每次一个字节，效率极低
}

// ❌ 不配置 TCP_NODELAY 导致延迟
// 小包会等待合并，增加延迟

// ❌ 缓冲区设置过小
socket.setReceiveBufferSize(1024);  // 太小，限制吞吐量
```
