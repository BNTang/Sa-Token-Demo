# TCP 超时重传机制

> Java 后端面试知识点 - TCP 可靠性保障

---

## 什么是超时重传

超时重传是 TCP 保证可靠传输的核心机制之一。当发送方在规定时间内没有收到确认 ACK，就会重新发送数据。

```
正常情况：
发送方                          接收方
   │                               │
   │─────── 数据包 ───────────────>│
   │                               │
   │<────── ACK ───────────────────│  ✅ 收到确认
   │                               │

丢包情况（需要重传）：
发送方                          接收方
   │                               │
   │─────── 数据包 ───X (丢失)     │
   │                               │
   │  等待 ACK...                  │
   │  超时！                        │
   │                               │
   │─────── 数据包（重传）────────>│  ✅ 重传成功
   │                               │
   │<────── ACK ───────────────────│
   │                               │
```

---

## 超时重传解决的问题

### 1. 数据包丢失

网络不稳定、拥塞、路由器丢弃等原因导致数据包丢失。

### 2. ACK 丢失

数据到达了，但确认响应丢失，发送方以为没送到。

### 3. 网络延迟过大

数据在网络中延迟时间超过预期，被误判为丢失。

---

## RTO 计算（超时时间）

RTO（Retransmission Timeout）是超时重传的等待时间。

### 动态 RTO 算法

```
SRTT = (1 - α) × SRTT + α × RTT    // 平滑 RTT
RTTVAR = (1 - β) × RTTVAR + β × |SRTT - RTT|  // RTT 偏差
RTO = SRTT + 4 × RTTVAR            // 超时时间

其中：
- α = 1/8
- β = 1/4
- SRTT: 平滑后的 RTT
- RTTVAR: RTT 变化值
```

### 指数退避

连续重传时，RTO 会翻倍，避免网络拥塞：

```
第1次重传: RTO
第2次重传: 2 × RTO
第3次重传: 4 × RTO
...
```

---

## 快速重传机制

除了超时重传，TCP 还有**快速重传**优化。

```
发送方                          接收方
   │                               │
   │─── Seq=1 ────────────────────>│
   │─── Seq=2 ─X (丢失)            │
   │─── Seq=3 ────────────────────>│  收到 3，发现 2 没收到
   │<── ACK=2 (重复确认1) ─────────│
   │─── Seq=4 ────────────────────>│
   │<── ACK=2 (重复确认2) ─────────│
   │─── Seq=5 ────────────────────>│
   │<── ACK=2 (重复确认3) ─────────│  三次重复确认
   │                               │
   │   收到 3 个重复 ACK            │
   │   触发快速重传（不等超时）      │
   │                               │
   │─── Seq=2 (快速重传) ─────────>│
   │                               │
```

**规则**：收到 3 个重复 ACK 后立即重传，不等待超时。

---

## 编码实践

### Socket 超时配置

```java
// ✅ 设置合理的超时时间
public class TcpClientConfig {
    
    public Socket createSocket(String host, int port) throws IOException {
        Socket socket = new Socket();
        
        // 连接超时：3秒
        socket.connect(new InetSocketAddress(host, port), 3000);
        
        // 读取超时：10秒
        socket.setSoTimeout(10000);
        
        // TCP KeepAlive
        socket.setKeepAlive(true);
        
        // 关闭时立即释放端口（谨慎使用）
        // socket.setReuseAddress(true);
        
        return socket;
    }
}
```

### HTTP 客户端超时配置

```java
// ✅ Apache HttpClient 超时配置
@Configuration
public class HttpClientConfig {
    
    @Bean
    public CloseableHttpClient httpClient() {
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(3000)         // 连接超时
            .setSocketTimeout(10000)         // 读取超时
            .setConnectionRequestTimeout(1000) // 从连接池获取连接超时
            .build();
        
        return HttpClients.custom()
            .setDefaultRequestConfig(requestConfig)
            .setRetryHandler(new DefaultHttpRequestRetryHandler(3, true))
            .build();
    }
}

// ✅ Spring WebClient 超时配置
@Bean
public WebClient webClient() {
    HttpClient httpClient = HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
        .responseTimeout(Duration.ofSeconds(10));
    
    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
}
```

### 重试机制实现

```java
// ✅ 使用 Resilience4j 实现重试
@Configuration
public class RetryConfig {
    
    @Bean
    public Retry retry() {
        RetryConfig config = RetryConfig.custom()
            .maxAttempts(3)                           // 最大重试次数
            .waitDuration(Duration.ofMillis(500))     // 重试间隔
            .retryOnException(e -> e instanceof IOException)
            .build();
        
        return Retry.of("httpRetry", config);
    }
}

@Service
public class HttpService {
    
    private final Retry retry;
    private final WebClient webClient;
    
    public String fetchData(String url) {
        return Retry.decorateSupplier(retry, () -> {
            return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        }).get();
    }
}

// ✅ 简单重试实现
public <T> T executeWithRetry(Supplier<T> action, int maxRetries) {
    int attempt = 0;
    while (true) {
        try {
            return action.get();
        } catch (Exception e) {
            attempt++;
            if (attempt >= maxRetries) {
                throw e;
            }
            // 指数退避
            long waitTime = (long) Math.pow(2, attempt) * 100;
            Thread.sleep(waitTime);
            log.warn("重试第{}次，等待{}ms", attempt, waitTime);
        }
    }
}
```

---

## 面试要点

### 核心答案

**问：TCP 超时重传机制是为了解决什么问题？**

答：超时重传机制是为了解决**网络传输中的丢包问题**，保证 TCP 的可靠传输。

具体场景：
1. **数据包丢失**：网络拥塞、路由器丢弃
2. **ACK 丢失**：确认响应在网络中丢失
3. **延迟过大**：数据包延迟超过预期

工作原理：
- 发送数据后启动定时器
- 超时未收到 ACK 则重传数据
- RTO 动态计算，连续重传时指数退避

### 延伸问题

**问：什么是快速重传？**
答：收到 3 个重复 ACK 后立即重传，不等待超时。比超时重传更快速。

**问：RTO 设置太大或太小有什么影响？**
- 太大：丢包后等待时间长，传输效率低
- 太小：频繁误判重传，浪费带宽

---

## 编码最佳实践

### ✅ 推荐做法

```java
// 1. 始终设置超时
RestTemplate restTemplate = new RestTemplate();
HttpComponentsClientHttpRequestFactory factory = 
    new HttpComponentsClientHttpRequestFactory();
factory.setConnectTimeout(3000);
factory.setReadTimeout(10000);
restTemplate.setRequestFactory(factory);

// 2. 使用重试框架
@Retryable(value = IOException.class, maxAttempts = 3, 
           backoff = @Backoff(delay = 1000, multiplier = 2))
public String callRemoteService() {
    // 远程调用
}

// 3. 设置合理的超时层级
// 连接超时 < 读取超时 < 请求总超时
```

### ❌ 避免做法

```java
// ❌ 不设置超时，可能无限等待
Socket socket = new Socket(host, port);  // 无超时
InputStream is = socket.getInputStream();
is.read();  // 可能永久阻塞

// ❌ 无限重试
while (true) {
    try {
        return callService();
    } catch (Exception e) {
        // 永远重试，可能导致雪崩
    }
}

// ❌ 重试间隔固定不增长
for (int i = 0; i < 3; i++) {
    try { return call(); } 
    catch (Exception e) { Thread.sleep(1000); }  // 固定间隔
}
```
