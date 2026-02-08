# 什么是 Java 的网络编程？

## 网络编程概述

```
┌─────────────────────────────────────────────────────────────┐
│                    Java 网络编程                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   定义: 使用 Java 进行网络通信的程序设计                    │
│                                                             │
│   核心类 (java.net 包):                                      │
│   • Socket / ServerSocket      TCP 通信                     │
│   • DatagramSocket / DatagramPacket   UDP 通信              │
│   • URL / URLConnection        HTTP 通信                    │
│   • InetAddress               IP 地址处理                   │
│                                                             │
│   通信模式:                                                  │
│   ┌──────────────┐                    ┌──────────────┐      │
│   │   Client     │ ←────网络────→    │   Server     │      │
│   │   客户端     │                    │   服务端     │      │
│   └──────────────┘                    └──────────────┘      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## TCP 编程

### 服务端

```java
// TCP 服务端
public class TcpServer {
    public static void main(String[] args) throws Exception {
        // 1. 创建 ServerSocket，监听端口
        ServerSocket serverSocket = new ServerSocket(8080);
        System.out.println("服务器启动，等待连接...");
        
        while (true) {
            // 2. 接受客户端连接 (阻塞)
            Socket socket = serverSocket.accept();
            System.out.println("客户端连接: " + socket.getRemoteSocketAddress());
            
            // 3. 处理请求 (建议用线程池)
            new Thread(() -> handleClient(socket)).start();
        }
    }
    
    private static void handleClient(Socket socket) {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(
                socket.getOutputStream(), true)) {
            
            // 读取请求
            String request = in.readLine();
            System.out.println("收到: " + request);
            
            // 发送响应
            out.println("Hello, " + request);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

### 客户端

```java
// TCP 客户端
public class TcpClient {
    public static void main(String[] args) throws Exception {
        // 1. 创建 Socket，连接服务器
        try (Socket socket = new Socket("localhost", 8080);
             BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(
                socket.getOutputStream(), true)) {
            
            // 2. 发送请求
            out.println("World");
            
            // 3. 读取响应
            String response = in.readLine();
            System.out.println("响应: " + response);
        }
    }
}
```

## UDP 编程

```java
// UDP 服务端
public class UdpServer {
    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket(8080);
        byte[] buffer = new byte[1024];
        
        while (true) {
            // 接收数据
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            
            String message = new String(packet.getData(), 0, packet.getLength());
            System.out.println("收到: " + message);
            
            // 发送响应
            byte[] response = ("Echo: " + message).getBytes();
            DatagramPacket reply = new DatagramPacket(
                response, response.length, 
                packet.getAddress(), packet.getPort());
            socket.send(reply);
        }
    }
}

// UDP 客户端
public class UdpClient {
    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket();
        
        // 发送数据
        byte[] data = "Hello UDP".getBytes();
        DatagramPacket packet = new DatagramPacket(
            data, data.length, 
            InetAddress.getByName("localhost"), 8080);
        socket.send(packet);
        
        // 接收响应
        byte[] buffer = new byte[1024];
        DatagramPacket response = new DatagramPacket(buffer, buffer.length);
        socket.receive(response);
        
        System.out.println("响应: " + 
            new String(response.getData(), 0, response.getLength()));
        socket.close();
    }
}
```

## HTTP 编程

```java
// Java 11+ HttpClient (推荐)
HttpClient client = HttpClient.newHttpClient();

// GET 请求
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("https://api.example.com/data"))
    .GET()
    .build();

HttpResponse<String> response = client.send(
    request, HttpResponse.BodyHandlers.ofString());

System.out.println(response.statusCode());
System.out.println(response.body());

// POST 请求
HttpRequest postRequest = HttpRequest.newBuilder()
    .uri(URI.create("https://api.example.com/data"))
    .header("Content-Type", "application/json")
    .POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"test\"}"))
    .build();
```

## TCP vs UDP

```
┌─────────────────────────────────────────────────────────────┐
│                    TCP vs UDP                                │
├──────────────────┬──────────────────────────────────────────┤
│   TCP            │   UDP                                    │
├──────────────────┼──────────────────────────────────────────┤
│ 面向连接         │ 无连接                                   │
│ 可靠传输         │ 不可靠                                   │
│ 有序到达         │ 可能乱序                                 │
│ 速度较慢         │ 速度快                                   │
│ Socket/ServerSocket │ DatagramSocket                       │
├──────────────────┼──────────────────────────────────────────┤
│ 应用: HTTP、FTP  │ 应用: DNS、视频流、游戏                  │
└──────────────────┴──────────────────────────────────────────┘
```

## NIO 网络编程

```java
// NIO 非阻塞服务器 (简化版)
Selector selector = Selector.open();
ServerSocketChannel serverChannel = ServerSocketChannel.open();
serverChannel.bind(new InetSocketAddress(8080));
serverChannel.configureBlocking(false);
serverChannel.register(selector, SelectionKey.OP_ACCEPT);

while (true) {
    selector.select();  // 阻塞等待事件
    Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
    
    while (keys.hasNext()) {
        SelectionKey key = keys.next();
        keys.remove();
        
        if (key.isAcceptable()) {
            // 处理连接
        } else if (key.isReadable()) {
            // 处理读取
        }
    }
}
```

## 面试回答

### 30秒版本

> Java 网络编程使用 **java.net** 包。**TCP** 用 Socket/ServerSocket，面向连接可靠传输。**UDP** 用 DatagramSocket，无连接快速传输。**HTTP** 用 Java 11+ HttpClient。高性能场景用 **NIO**（Selector + Channel）或 **Netty** 框架。

### 1分钟版本

> **核心类**：
> - TCP：Socket / ServerSocket
> - UDP：DatagramSocket / DatagramPacket
> - HTTP：HttpClient (Java 11+)
>
> **TCP 编程流程**：
> - 服务端：ServerSocket.accept() 接受连接
> - 客户端：Socket 连接服务器
> - 通过 InputStream/OutputStream 通信
>
> **TCP vs UDP**：
> - TCP：可靠、有序、慢
> - UDP：不可靠、快、适合实时
>
> **高性能方案**：
> - NIO：Selector 多路复用
> - Netty：封装 NIO，更易用

---

*关联文档：[tcp-vs-udp.md](../01-network/tcp-vs-udp.md) | [netty-performance.md](../05-concurrency/netty-performance.md)*
