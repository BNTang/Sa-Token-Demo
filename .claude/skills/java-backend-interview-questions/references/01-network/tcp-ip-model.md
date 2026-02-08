# TCP/IP 四层模型

> Java 后端面试知识点 - 网络协议基础

---

## 四层模型概述

TCP/IP 模型是互联网的实际架构标准，比 OSI 模型更简洁实用。

```
┌─────────────────────────────────────────────────────────────────────────┐
│        TCP/IP 四层模型              │         对应 OSI 层级             │
├─────────────────────────────────────┼───────────────────────────────────┤
│  应用层 (Application)               │  应用层 + 表示层 + 会话层          │
│  HTTP, FTP, SMTP, DNS, SSH          │                                   │
├─────────────────────────────────────┼───────────────────────────────────┤
│  传输层 (Transport)                 │  传输层                           │
│  TCP, UDP                           │                                   │
├─────────────────────────────────────┼───────────────────────────────────┤
│  网络层 (Internet/Network)          │  网络层                           │
│  IP, ICMP, ARP, RARP                │                                   │
├─────────────────────────────────────┼───────────────────────────────────┤
│  网络接口层 (Network Interface)     │  数据链路层 + 物理层               │
│  Ethernet, Wi-Fi, PPP               │                                   │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 各层详解

### 应用层

**职责**：为用户应用程序提供网络服务

```java
// ✅ HTTP 客户端最佳实践
@Configuration
public class HttpClientConfig {
    
    @Bean
    public WebClient webClient() {
        // 使用连接池，提高性能
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .responseTimeout(Duration.ofSeconds(10))
            .doOnConnected(conn -> conn
                .addHandlerLast(new ReadTimeoutHandler(10))
                .addHandlerLast(new WriteTimeoutHandler(10)));
        
        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }
}
```

### 传输层

**职责**：提供端到端通信

| 特性 | TCP | UDP |
|------|-----|-----|
| 连接性 | 面向连接 | 无连接 |
| 可靠性 | 可靠（重传机制） | 不可靠 |
| 顺序性 | 保证顺序 | 不保证 |
| 速度 | 较慢 | 较快 |
| 头部开销 | 20字节 | 8字节 |
| 适用场景 | HTTP、文件传输 | 直播、游戏 |

**TCP 三次握手**：
```
客户端                          服务端
   │                               │
   │─────── SYN (seq=x) ──────────>│  第1次：客户端发起连接
   │                               │
   │<──── SYN+ACK (seq=y,ack=x+1) ─│  第2次：服务端确认并回应
   │                               │
   │─────── ACK (ack=y+1) ────────>│  第3次：客户端确认
   │                               │
   │       连接建立完成             │
```

**TCP 四次挥手**：
```
客户端                          服务端
   │                               │
   │─────── FIN (seq=u) ──────────>│  第1次：客户端请求关闭
   │                               │
   │<──── ACK (ack=u+1) ───────────│  第2次：服务端确认
   │                               │
   │<──── FIN (seq=v) ─────────────│  第3次：服务端请求关闭
   │                               │
   │─────── ACK (ack=v+1) ────────>│  第4次：客户端确认
   │                               │
   │    TIME_WAIT (2MSL)           │
```

### 网络层

**职责**：路由选择、逻辑寻址

```java
// ✅ 获取本机 IP 地址
public class NetworkUtils {
    
    public static String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = 
                NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            log.error("获取本机IP失败", e);
        }
        return "127.0.0.1";
    }
}
```

### 网络接口层

**职责**：物理传输、帧封装

---

## 数据封装与解封装

### 发送数据（封装）

```
应用层      │ HTTP请求数据                          │
            ↓
传输层      │ TCP头 │ HTTP请求数据                  │ → 段(Segment)
            ↓
网络层      │ IP头 │ TCP头 │ HTTP请求数据           │ → 包(Packet)
            ↓
网络接口层  │ 帧头 │ IP头 │ TCP头 │ 数据 │ 帧尾    │ → 帧(Frame)
            ↓
物理传输    │ 0101010101010101...                   │ → 比特流(Bits)
```

---

## 面试要点

### 高频问题

1. **为什么 TCP 握手是三次，挥手是四次？**
   - 握手：三次可以确认双方收发能力正常
   - 挥手：TCP 是全双工，双方都需要单独关闭各自的发送通道

2. **TCP 和 UDP 的区别？**
   - TCP：可靠、有序、面向连接、慢
   - UDP：不可靠、无序、无连接、快

3. **什么时候用 UDP？**
   - 对实时性要求高，容忍丢包：视频直播、语音通话、游戏
   - 简单查询：DNS 查询

---

## 编码最佳实践

### ✅ 网络编程推荐

```java
// 1. 使用 NIO 处理高并发连接
Selector selector = Selector.open();
ServerSocketChannel serverChannel = ServerSocketChannel.open();
serverChannel.configureBlocking(false);
serverChannel.register(selector, SelectionKey.OP_ACCEPT);

// 2. 使用 Netty 简化网络编程
EventLoopGroup bossGroup = new NioEventLoopGroup(1);
EventLoopGroup workerGroup = new NioEventLoopGroup();

ServerBootstrap bootstrap = new ServerBootstrap();
bootstrap.group(bossGroup, workerGroup)
    .channel(NioServerSocketChannel.class)
    .childHandler(new ChannelInitializer<SocketChannel>() {
        @Override
        protected void initChannel(SocketChannel ch) {
            ch.pipeline().addLast(new MyHandler());
        }
    });
```

### ❌ 避免做法

```java
// ❌ 同步阻塞处理大量连接
ServerSocket server = new ServerSocket(8080);
while (true) {
    Socket client = server.accept(); // 阻塞
    handleClient(client);            // 串行处理，性能差
}
```
