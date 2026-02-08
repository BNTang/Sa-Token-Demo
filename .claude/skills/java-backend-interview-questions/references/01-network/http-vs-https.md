# HTTP 和 HTTPS 有什么区别？

## 核心区别

```
┌─────────────────────────────────────────────────────────────┐
│                    HTTP vs HTTPS 对比                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   HTTP (HyperText Transfer Protocol)                        │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  客户端 ←────── 明文传输 ──────→ 服务器              │  │
│   │                 可被窃听/篡改                        │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   HTTPS (HTTP Secure = HTTP + TLS/SSL)                      │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  客户端 ←────── 加密传输 ──────→ 服务器              │  │
│   │                 安全可靠                             │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 详细对比

```
┌─────────────────────────────────────────────────────────────┐
│                    HTTP vs HTTPS 详细对比                    │
├──────────────────┬─────────────────┬────────────────────────┤
│   特性            │      HTTP       │        HTTPS           │
├──────────────────┼─────────────────┼────────────────────────┤
│   端口            │      80         │        443             │
│   传输方式        │      明文       │        加密            │
│   安全性          │      不安全     │        安全            │
│   证书            │      不需要     │        需要 CA 证书    │
│   连接建立        │      3 次握手   │        3次握手+TLS握手 │
│   性能            │      较快       │        略慢(加密开销)  │
│   SEO             │      较低权重   │        较高权重        │
│   URL 前缀        │      http://    │        https://        │
├──────────────────┴─────────────────┴────────────────────────┤
│   HTTPS = HTTP + TLS (Transport Layer Security)             │
│   TLS 是 SSL 的升级版本                                      │
└─────────────────────────────────────────────────────────────┘
```

## HTTPS 工作原理

### TLS 握手过程

```
┌─────────────────────────────────────────────────────────────┐
│                    TLS 握手过程                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   客户端                                        服务器       │
│      │                                            │         │
│      │ ──── 1. ClientHello ─────────────────────→ │         │
│      │      (支持的TLS版本、加密套件、随机数)      │         │
│      │                                            │         │
│      │ ←─── 2. ServerHello ─────────────────────  │         │
│      │      (选定的TLS版本、加密套件、随机数)      │         │
│      │                                            │         │
│      │ ←─── 3. Certificate ─────────────────────  │         │
│      │      (服务器证书，包含公钥)                 │         │
│      │                                            │         │
│      │ ←─── 4. ServerHelloDone ─────────────────  │         │
│      │                                            │         │
│      │      (验证证书有效性)                       │         │
│      │                                            │         │
│      │ ──── 5. ClientKeyExchange ───────────────→ │         │
│      │      (用公钥加密的预主密钥)                 │         │
│      │                                            │         │
│      │ ──── 6. ChangeCipherSpec ────────────────→ │         │
│      │      (通知开始使用加密通信)                 │         │
│      │                                            │         │
│      │ ──── 7. Finished ────────────────────────→ │         │
│      │                                            │         │
│      │ ←─── 8. ChangeCipherSpec ────────────────  │         │
│      │ ←─── 9. Finished ────────────────────────  │         │
│      │                                            │         │
│      │ ════ 加密通信开始 ═════════════════════════ │         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 加密方式

```
┌─────────────────────────────────────────────────────────────┐
│                    HTTPS 加密方式                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 非对称加密 (握手阶段)                                   │
│      ├── RSA / ECDHE 算法                                   │
│      ├── 用于交换对称密钥                                   │
│      └── 计算开销大，只用于密钥交换                          │
│                                                             │
│   2. 对称加密 (数据传输阶段)                                 │
│      ├── AES-128 / AES-256                                  │
│      ├── 用于加密实际数据                                   │
│      └── 速度快，适合大量数据                                │
│                                                             │
│   3. 数字签名 (身份验证)                                     │
│      ├── SHA-256 + RSA                                      │
│      └── 验证数据完整性和来源                                │
│                                                             │
│   综合使用：非对称加密交换密钥 → 对称加密传输数据             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## HTTPS 解决的问题

```
┌─────────────────────────────────────────────────────────────┐
│                  HTTP 的安全问题                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 窃听风险 (Eavesdropping)                               │
│      ├── HTTP 明文传输，数据可被抓包                        │
│      └── HTTPS 加密解决 ✓                                   │
│                                                             │
│   2. 篡改风险 (Tampering)                                   │
│      ├── 中间人可以修改传输内容                              │
│      └── HTTPS 数字签名验证完整性 ✓                         │
│                                                             │
│   3. 冒充风险 (Impersonation)                               │
│      ├── 无法验证服务器身份，可能访问钓鱼网站                │
│      └── HTTPS CA证书验证身份 ✓                             │
│                                                             │
│   中间人攻击 (MITM):                                         │
│   客户端 ←→ 攻击者 ←→ 服务器                                 │
│   攻击者可窃听、篡改、伪造通信                               │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 证书体系

```
┌─────────────────────────────────────────────────────────────┐
│                    CA 证书信任链                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │                  根证书 (Root CA)                    │  │
│   │            内置于操作系统/浏览器                      │  │
│   │            DigiCert, Let's Encrypt, GlobalSign       │  │
│   └─────────────────────────────────────────────────────┘  │
│                           │ 签发                           │
│                           ▼                                │
│   ┌─────────────────────────────────────────────────────┐  │
│   │                 中间证书 (Intermediate CA)           │  │
│   └─────────────────────────────────────────────────────┘  │
│                           │ 签发                           │
│                           ▼                                │
│   ┌─────────────────────────────────────────────────────┐  │
│   │                 网站证书 (End Entity)                │  │
│   │               example.com 的证书                     │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   验证过程：网站证书 → 中间证书 → 根证书 (逐级验证签名)      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 代码示例

### Java HTTPS 请求

```java
// 1. 使用 HttpsURLConnection
public String httpsGet(String url) throws Exception {
    URL httpsUrl = new URL(url);
    HttpsURLConnection conn = (HttpsURLConnection) httpsUrl.openConnection();
    conn.setRequestMethod("GET");
    
    try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(conn.getInputStream()))) {
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        return response.toString();
    }
}

// 2. 使用 RestTemplate (Spring)
@Bean
public RestTemplate restTemplate() throws Exception {
    SSLContext sslContext = SSLContexts.custom()
        .loadTrustMaterial(null, (chain, authType) -> true) // 信任所有证书(仅测试用)
        .build();
    
    HttpClient client = HttpClients.custom()
        .setSSLContext(sslContext)
        .build();
    
    return new RestTemplate(new HttpComponentsClientHttpRequestFactory(client));
}

// 3. 使用 OkHttp
OkHttpClient client = new OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .build();

Request request = new Request.Builder()
    .url("https://api.example.com/data")
    .build();

Response response = client.newCall(request).execute();
```

### Nginx HTTPS 配置

```nginx
server {
    listen 443 ssl http2;
    server_name example.com;
    
    # 证书配置
    ssl_certificate     /etc/nginx/ssl/example.com.crt;
    ssl_certificate_key /etc/nginx/ssl/example.com.key;
    
    # TLS 版本 (禁用不安全的版本)
    ssl_protocols TLSv1.2 TLSv1.3;
    
    # 加密套件
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256;
    ssl_prefer_server_ciphers on;
    
    # HSTS (强制 HTTPS)
    add_header Strict-Transport-Security "max-age=31536000" always;
    
    location / {
        proxy_pass http://backend;
    }
}

# HTTP 重定向到 HTTPS
server {
    listen 80;
    server_name example.com;
    return 301 https://$server_name$request_uri;
}
```

## 性能优化

```
┌─────────────────────────────────────────────────────────────┐
│                  HTTPS 性能优化                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. TLS 会话复用                                           │
│      ├── Session ID: 服务端缓存会话                         │
│      └── Session Ticket: 客户端存储加密票据                 │
│                                                             │
│   2. OCSP Stapling                                          │
│      └── 服务端缓存证书状态，减少客户端验证延迟              │
│                                                             │
│   3. HTTP/2                                                 │
│      └── 多路复用，减少连接数                                │
│                                                             │
│   4. TLS 1.3                                                │
│      └── 1-RTT 握手，比 TLS 1.2 减少一次往返                │
│                                                             │
│   5. 证书链优化                                              │
│      └── 减少中间证书数量                                   │
│                                                             │
│   6. 硬件加速                                               │
│      └── 使用支持 AES-NI 的 CPU                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> HTTP 和 HTTPS 的核心区别：HTTPS = HTTP + TLS 加密。HTTP 明文传输（端口 80），有窃听、篡改、冒充风险；HTTPS 加密传输（端口 443），通过 **非对称加密交换密钥 + 对称加密传输数据 + CA 证书验证身份**，解决了安全问题。

### 1分钟版本

> **主要区别**：
> - **传输方式**：HTTP 明文，HTTPS 加密
> - **端口**：HTTP 80，HTTPS 443
> - **证书**：HTTPS 需要 CA 签发的证书
> - **性能**：HTTPS 有加密开销，但 TLS 1.3 + HTTP/2 已大幅优化
>
> **HTTPS 原理**：
> 1. TLS 握手：非对称加密交换密钥
> 2. 数据传输：对称加密（AES）加密内容
> 3. 身份验证：CA 证书链验证服务器身份
>
> **解决问题**：
> - 窃听 → 加密解决
> - 篡改 → 数字签名验证完整性
> - 冒充 → CA 证书验证身份
>
> **优化**：TLS 会话复用、OCSP Stapling、TLS 1.3

---

*关联文档：[http-versions.md](http-versions.md) | [http-versions-advanced.md](http-versions-advanced.md) | [tcp-connection.md](tcp-connection.md)*
