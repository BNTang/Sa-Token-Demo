# Reactor 线程模型

> Java 后端面试知识点 - 并发编程与网络编程

---

## 什么是 Reactor 模型

Reactor 是一种事件驱动的设计模式，用于处理高并发 I/O 请求。核心思想是：**多路复用 + 事件分发**。

---

## 三种 Reactor 模型

### 1. 单 Reactor 单线程

```
┌─────────────────────────────────────────────────────────────────┐
│                     单 Reactor 单线程                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  客户端1 ──┐                                                    │
│            │     ┌───────────┐     ┌──────────────────────┐    │
│  客户端2 ──┼────>│  Reactor  │────>│  Handler (处理业务)   │    │
│            │     │  (单线程)  │     │  (同一线程)           │    │
│  客户端3 ──┘     └───────────┘     └──────────────────────┘    │
│                                                                 │
│  特点：一个线程完成所有工作（accept、read、业务处理、write）       │
│  缺点：无法利用多核，业务阻塞影响其他连接                         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**特点**：
- 一个线程处理所有 I/O 和业务逻辑
- 实现简单

**缺点**：
- 无法利用多核 CPU
- 一个 Handler 阻塞会影响所有连接

### 2. 单 Reactor 多线程

```
┌─────────────────────────────────────────────────────────────────┐
│                     单 Reactor 多线程                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  客户端1 ──┐                                                    │
│            │     ┌───────────┐     ┌──────────────────────┐    │
│  客户端2 ──┼────>│  Reactor  │────>│     Worker Pool      │    │
│            │     │  (单线程)  │     │  ┌────┐ ┌────┐ ┌────┐ │    │
│  客户端3 ──┘     │  accept   │     │  │ W1 │ │ W2 │ │ W3 │ │    │
│                  │  read/write     │  └────┘ └────┘ └────┘ │    │
│                  └───────────┘     │  (业务处理)           │    │
│                                    └──────────────────────┘    │
│                                                                 │
│  特点：Reactor 处理 I/O，Worker 线程池处理业务                    │
│  缺点：Reactor 是单线程，高并发时成为瓶颈                         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**特点**：
- Reactor 线程处理所有 I/O
- 业务逻辑交给线程池处理

**缺点**：
- Reactor 单线程可能成为瓶颈
- 海量连接时 accept/read/write 压力大

### 3. 主从 Reactor 多线程（推荐）

```
┌─────────────────────────────────────────────────────────────────┐
│                   主从 Reactor 多线程（Netty 模型）              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│                  ┌───────────────┐                              │
│  客户端1 ──┐     │  Main Reactor │                              │
│            │     │  (Boss 线程)   │                              │
│  客户端2 ──┼────>│  只负责 accept │                              │
│            │     └───────┬───────┘                              │
│  客户端3 ──┘             │                                      │
│                          ↓                                      │
│         ┌────────────────┼────────────────┐                    │
│         ↓                ↓                ↓                    │
│  ┌────────────┐   ┌────────────┐   ┌────────────┐             │
│  │Sub Reactor1│   │Sub Reactor2│   │Sub Reactor3│             │
│  │(Worker线程) │   │(Worker线程) │   │(Worker线程) │             │
│  │ read/write │   │ read/write │   │ read/write │             │
│  └──────┬─────┘   └──────┬─────┘   └──────┬─────┘             │
│         ↓                ↓                ↓                    │
│  ┌───────────────────────────────────────────────────┐         │
│  │                  业务线程池                        │         │
│  │              (处理耗时业务逻辑)                     │         │
│  └───────────────────────────────────────────────────┘         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**特点**：
- **Main Reactor**：只负责 accept 新连接
- **Sub Reactor**：处理已建立连接的 read/write
- **业务线程池**：处理耗时业务逻辑

**优点**：
- 职责分离，高性能
- 充分利用多核 CPU
- Netty 采用此模型

---

## Netty 实现

```java
public class NettyServer {
    
    public void start(int port) throws Exception {
        // 主 Reactor：处理 accept
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        // 从 Reactor：处理 read/write
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        // 业务线程池
        EventExecutorGroup businessGroup = new DefaultEventExecutorGroup(16);
        
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        
                        // 编解码器（在 Worker 线程执行）
                        pipeline.addLast(new StringDecoder());
                        pipeline.addLast(new StringEncoder());
                        
                        // 业务处理（在业务线程池执行，不阻塞 I/O 线程）
                        pipeline.addLast(businessGroup, new BusinessHandler());
                    }
                });
            
            ChannelFuture future = bootstrap.bind(port).sync();
            log.info("Netty Server started on port {}", port);
            future.channel().closeFuture().sync();
            
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            businessGroup.shutdownGracefully();
        }
    }
}

@Slf4j
public class BusinessHandler extends SimpleChannelInboundHandler<String> {
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        log.info("收到消息: {}, 线程: {}", msg, Thread.currentThread().getName());
        // 业务处理
        String response = processMessage(msg);
        ctx.writeAndFlush(response);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("异常", cause);
        ctx.close();
    }
}
```

### Netty 线程模型配置

```java
// 1. 推荐配置：主从 Reactor
EventLoopGroup bossGroup = new NioEventLoopGroup(1);      // 1 个 boss 线程
EventLoopGroup workerGroup = new NioEventLoopGroup();     // 默认 CPU 核数 * 2

// 2. 性能调优
EventLoopGroup workerGroup = new NioEventLoopGroup(
    Runtime.getRuntime().availableProcessors() * 2);

// 3. 业务线程池（处理耗时操作）
EventExecutorGroup businessGroup = new DefaultEventExecutorGroup(16);

// 4. 自定义线程工厂
EventLoopGroup workerGroup = new NioEventLoopGroup(8, 
    new ThreadFactoryBuilder()
        .setNameFormat("netty-worker-%d")
        .build());
```

---

## Spring WebFlux 中的 Reactor

```java
// WebFlux 使用 Reactor 模式
@RestController
@RequestMapping("/api")
public class ReactiveController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/users/{id}")
    public Mono<User> getUser(@PathVariable Long id) {
        return userService.findById(id);
    }
    
    @GetMapping("/users")
    public Flux<User> getAllUsers() {
        return userService.findAll();
    }
}

@Service
public class UserService {
    
    @Autowired
    private ReactiveMongoRepository<User, Long> repository;
    
    public Mono<User> findById(Long id) {
        return repository.findById(id)
            .doOnNext(user -> log.info("Found user: {}", user))
            .switchIfEmpty(Mono.error(new NotFoundException()));
    }
    
    public Flux<User> findAll() {
        return repository.findAll();
    }
}
```

---

## 面试要点

### 核心答案

**问：介绍一下 Reactor 线程模型？**

答：

Reactor 是一种事件驱动的设计模式，核心是**多路复用 + 事件分发**。

**三种模型**：

1. **单 Reactor 单线程**
   - 一个线程处理所有 I/O 和业务
   - 简单但无法利用多核

2. **单 Reactor 多线程**
   - Reactor 线程处理 I/O
   - 业务交给线程池
   - Reactor 可能成为瓶颈

3. **主从 Reactor 多线程**（Netty 采用）
   - **Boss 线程**：只负责 accept
   - **Worker 线程**：处理 read/write
   - **业务线程池**：处理耗时逻辑
   - 高性能，职责分离

**Netty 实现**：
```java
EventLoopGroup bossGroup = new NioEventLoopGroup(1);    // Boss
EventLoopGroup workerGroup = new NioEventLoopGroup();   // Worker
bootstrap.group(bossGroup, workerGroup)...
```

---

## 编码最佳实践

### ✅ 推荐做法

```java
// 1. Boss 线程数设为 1（accept 操作很快）
EventLoopGroup bossGroup = new NioEventLoopGroup(1);

// 2. Worker 线程数根据 CPU 核数设置
EventLoopGroup workerGroup = new NioEventLoopGroup(
    Runtime.getRuntime().availableProcessors() * 2);

// 3. 耗时操作使用独立线程池
EventExecutorGroup businessGroup = new DefaultEventExecutorGroup(16);
pipeline.addLast(businessGroup, new BusinessHandler());

// 4. 不要在 I/O 线程执行阻塞操作
@Override
protected void channelRead0(ChannelHandlerContext ctx, String msg) {
    // ✅ 交给业务线程池
    businessExecutor.submit(() -> {
        String result = slowOperation(msg);
        ctx.writeAndFlush(result);
    });
}
```

### ❌ 避免做法

```java
// ❌ Boss 线程数过多
EventLoopGroup bossGroup = new NioEventLoopGroup(10);  // 浪费

// ❌ 在 I/O 线程执行阻塞操作
@Override
protected void channelRead0(ChannelHandlerContext ctx, String msg) {
    Thread.sleep(1000);  // 阻塞 I/O 线程！
    ctx.writeAndFlush(response);
}

// ❌ 忘记关闭资源
bossGroup.shutdownGracefully();  // 应该在 finally 中
```
