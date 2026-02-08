# OSI 七层模型

> Java 后端面试知识点 - 网络协议基础

---

## 七层模型概述

OSI（Open Systems Interconnection）七层模型是国际标准化组织提出的网络通信标准架构。

```
┌─────────────────────────────────────────────────────────────────┐
│  第7层  │  应用层 (Application)    │  HTTP, FTP, SMTP, DNS      │
├─────────┼──────────────────────────┼────────────────────────────┤
│  第6层  │  表示层 (Presentation)   │  SSL/TLS, JPEG, ASCII      │
├─────────┼──────────────────────────┼────────────────────────────┤
│  第5层  │  会话层 (Session)        │  NetBIOS, RPC              │
├─────────┼──────────────────────────┼────────────────────────────┤
│  第4层  │  传输层 (Transport)      │  TCP, UDP                  │
├─────────┼──────────────────────────┼────────────────────────────┤
│  第3层  │  网络层 (Network)        │  IP, ICMP, ARP             │
├─────────┼──────────────────────────┼────────────────────────────┤
│  第2层  │  数据链路层 (Data Link)  │  Ethernet, PPP, MAC        │
├─────────┼──────────────────────────┼────────────────────────────┤
│  第1层  │  物理层 (Physical)       │  光纤, 双绞线, 无线电波     │
└─────────────────────────────────────────────────────────────────┘
```

---

## 各层详解

### 第7层 - 应用层

**职责**：为应用程序提供网络服务的接口

| 协议 | 端口 | 用途 |
|------|------|------|
| HTTP/HTTPS | 80/443 | Web 访问 |
| FTP | 21 | 文件传输 |
| SMTP | 25 | 邮件发送 |
| DNS | 53 | 域名解析 |
| SSH | 22 | 安全远程登录 |

**编码实践**：
```java
// ✅ 使用 RestTemplate/WebClient 进行 HTTP 通信
@Service
public class HttpClientService {
    
    private final WebClient webClient;
    
    public HttpClientService(WebClient.Builder builder) {
        this.webClient = builder
            .baseUrl("https://api.example.com")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }
    
    public Mono<User> getUser(Long id) {
        return webClient.get()
            .uri("/users/{id}", id)
            .retrieve()
            .bodyToMono(User.class);
    }
}
```

### 第4层 - 传输层

**职责**：提供端到端的可靠/不可靠传输

| 协议 | 特性 | 适用场景 |
|------|------|---------|
| TCP | 可靠、有序、面向连接 | HTTP、文件传输 |
| UDP | 不可靠、无序、无连接 | 视频直播、DNS 查询 |

**编码实践**：
```java
// ✅ TCP Socket 服务端示例
public class TcpServer {
    public void start(int port) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            log.info("TCP Server started on port {}", port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                // 使用线程池处理客户端连接
                executorService.submit(() -> handleClient(clientSocket));
            }
        }
    }
}

// ✅ UDP 服务端示例
public class UdpServer {
    public void start(int port) throws IOException {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            while (true) {
                socket.receive(packet);
                processPacket(packet);
            }
        }
    }
}
```

### 第3层 - 网络层

**职责**：路由选择、IP 寻址

**核心协议**：
- **IP**：网络寻址
- **ICMP**：网络诊断（ping）
- **ARP**：IP 地址到 MAC 地址映射

---

## 面试要点

### 高频问题

1. **OSI 与 TCP/IP 模型的区别？**
   - OSI 是理论模型（7层），TCP/IP 是实际实现（4层）
   - OSI 将应用层拆分为 应用/表示/会话 三层

2. **为什么要分层？**
   - 各层独立，易于标准化
   - 灵活性好，某层变化不影响其他层
   - 易于实现和维护

3. **数据封装过程？**
   ```
   应用层数据 → 加TCP头 → 加IP头 → 加帧头帧尾 → 比特流
   ```

### 记忆口诀

> **"应表会传网数物"** = 应用层、表示层、会话层、传输层、网络层、数据链路层、物理层

---

## 编码最佳实践

### ✅ 推荐做法

```java
// 1. 使用高层抽象，避免直接操作 Socket
@Configuration
public class WebClientConfig {
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
            .codecs(config -> config.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();
    }
}

// 2. 合理设置超时
@Bean
public RestTemplate restTemplate() {
    HttpComponentsClientHttpRequestFactory factory = 
        new HttpComponentsClientHttpRequestFactory();
    factory.setConnectTimeout(3000);   // 连接超时 3s
    factory.setReadTimeout(5000);      // 读取超时 5s
    return new RestTemplate(factory);
}
```

### ❌ 避免做法

```java
// ❌ 不设置超时，可能导致连接挂起
RestTemplate restTemplate = new RestTemplate(); // 默认无超时

// ❌ 直接使用低层 Socket 而不封装
Socket socket = new Socket("host", 80); // 难以维护
```
