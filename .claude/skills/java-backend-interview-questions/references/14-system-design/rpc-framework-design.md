# 让你设计一个 RPC 框架，怎么设计？

## RPC 框架架构

```
┌─────────────────────────────────────────────────────────────┐
│                    RPC 框架核心组件                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │                    Consumer (客户端)                 │  │
│   │  ┌─────────────────────────────────────────────┐   │  │
│   │  │              动态代理 (Proxy)                │   │  │
│   │  │         像调用本地方法一样调用远程服务        │   │  │
│   │  └─────────────────────────────────────────────┘   │  │
│   │                        ↓                           │  │
│   │  ┌─────────────────────────────────────────────┐   │  │
│   │  │             序列化 (Serialization)           │   │  │
│   │  │         Java对象 → 二进制数据                │   │  │
│   │  └─────────────────────────────────────────────┘   │  │
│   │                        ↓                           │  │
│   │  ┌─────────────────────────────────────────────┐   │  │
│   │  │             网络传输 (Transport)             │   │  │
│   │  │               Netty / HTTP                   │   │  │
│   │  └─────────────────────────────────────────────┘   │  │
│   └─────────────────────────────────────────────────────┘  │
│                            │                               │
│                            ▼                               │
│   ┌─────────────────────────────────────────────────────┐  │
│   │                    注册中心                          │  │
│   │         Zookeeper / Nacos / Consul                  │  │
│   │     • 服务注册    • 服务发现    • 健康检查          │  │
│   └─────────────────────────────────────────────────────┘  │
│                            │                               │
│                            ▼                               │
│   ┌─────────────────────────────────────────────────────┐  │
│   │                    Provider (服务端)                 │  │
│   │  ┌─────────────────────────────────────────────┐   │  │
│   │  │             网络传输 (Transport)             │   │  │
│   │  └─────────────────────────────────────────────┘   │  │
│   │                        ↓                           │  │
│   │  ┌─────────────────────────────────────────────┐   │  │
│   │  │             反序列化 (Deserialization)       │   │  │
│   │  └─────────────────────────────────────────────┘   │  │
│   │                        ↓                           │  │
│   │  ┌─────────────────────────────────────────────┐   │  │
│   │  │             业务处理 + 反射调用               │   │  │
│   │  └─────────────────────────────────────────────┘   │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 核心流程

```
┌─────────────────────────────────────────────────────────────┐
│                    RPC 调用流程                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. Consumer 调用                                          │
│      UserService.getUser(123)                               │
│              ↓                                              │
│   2. 动态代理拦截                                           │
│      封装 RpcRequest: 服务名、方法名、参数                   │
│              ↓                                              │
│   3. 从注册中心获取服务地址列表                              │
│              ↓                                              │
│   4. 负载均衡选择一个节点                                   │
│              ↓                                              │
│   5. 序列化 RpcRequest                                      │
│              ↓                                              │
│   6. 网络传输发送请求                                        │
│              ↓                                              │
│   7. Provider 接收，反序列化                                 │
│              ↓                                              │
│   8. 反射调用目标方法                                        │
│              ↓                                              │
│   9. 序列化 RpcResponse                                     │
│              ↓                                              │
│   10. 返回 Consumer，反序列化得到结果                       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 一、RPC 协议设计

```
┌─────────────────────────────────────────────────────────────┐
│                    RPC 协议格式                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │        魔数     │ 版本 │ 序列化 │ 消息类型 │ 状态     │  │
│   │       4 byte   │  1   │   1    │    1     │   1     │  │
│   ├─────────────────────────────────────────────────────┤  │
│   │             请求ID              │     数据长度       │  │
│   │              8 byte             │      4 byte       │  │
│   ├─────────────────────────────────────────────────────┤  │
│   │                      数据体                          │  │
│   │                   (可变长度)                         │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   • 魔数：标识 RPC 协议，用于快速识别                       │
│   • 版本：协议版本，用于兼容升级                            │
│   • 序列化：1=JSON, 2=Protobuf, 3=Hessian                  │
│   • 消息类型：1=请求, 2=响应, 3=心跳                        │
│   • 请求ID：用于匹配请求和响应                              │
│   • 数据长度：解决粘包拆包问题                              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

```java
/**
 * RPC 协议消息
 */
@Data
public class RpcMessage {
    // 魔数
    public static final int MAGIC_NUMBER = 0xCAFEBABE;
    
    private byte version = 1;
    private byte serializationType;
    private byte messageType;
    private byte status;
    private long requestId;
    private Object data;
}

/**
 * RPC 请求
 */
@Data
public class RpcRequest {
    private String requestId;
    private String interfaceName;  // 接口全限定名
    private String methodName;     // 方法名
    private Class<?>[] paramTypes; // 参数类型
    private Object[] params;       // 参数值
    private String version;        // 服务版本
}

/**
 * RPC 响应
 */
@Data
public class RpcResponse {
    private String requestId;
    private int code;          // 状态码
    private String message;    // 错误信息
    private Object data;       // 返回数据
}
```

## 二、服务注册与发现

```java
/**
 * 服务注册接口
 */
public interface ServiceRegistry {
    void register(ServiceInfo serviceInfo);
    void unregister(ServiceInfo serviceInfo);
}

/**
 * 服务发现接口
 */
public interface ServiceDiscovery {
    List<ServiceInfo> discover(String serviceName);
    void subscribe(String serviceName, ServiceChangeListener listener);
}

/**
 * ZooKeeper 实现
 */
@Component
public class ZkServiceRegistry implements ServiceRegistry {
    
    private CuratorFramework zkClient;
    private static final String ROOT_PATH = "/rpc";
    
    @Override
    public void register(ServiceInfo serviceInfo) {
        String servicePath = ROOT_PATH + "/" + serviceInfo.getName();
        String instancePath = servicePath + "/" + serviceInfo.getAddress();
        
        try {
            // 创建临时节点（服务下线自动删除）
            if (zkClient.checkExists().forPath(servicePath) == null) {
                zkClient.create().creatingParentsIfNeeded()
                    .withMode(CreateMode.PERSISTENT)
                    .forPath(servicePath);
            }
            
            zkClient.create().withMode(CreateMode.EPHEMERAL)
                .forPath(instancePath, JSON.toJSONBytes(serviceInfo));
                
        } catch (Exception e) {
            throw new RpcException("服务注册失败", e);
        }
    }
}

@Component
public class ZkServiceDiscovery implements ServiceDiscovery {
    
    // 本地缓存
    private Map<String, List<ServiceInfo>> serviceCache = new ConcurrentHashMap<>();
    
    @Override
    public List<ServiceInfo> discover(String serviceName) {
        // 优先从缓存获取
        List<ServiceInfo> cached = serviceCache.get(serviceName);
        if (cached != null) {
            return cached;
        }
        
        // 从 ZK 获取并缓存
        String servicePath = ROOT_PATH + "/" + serviceName;
        List<String> children = zkClient.getChildren().forPath(servicePath);
        
        List<ServiceInfo> services = children.stream()
            .map(child -> parseServiceInfo(servicePath + "/" + child))
            .collect(Collectors.toList());
        
        serviceCache.put(serviceName, services);
        
        // 订阅变化
        watchService(serviceName);
        
        return services;
    }
}
```

## 三、动态代理

```java
/**
 * JDK 动态代理
 */
public class RpcClientProxy implements InvocationHandler {
    
    private ServiceDiscovery serviceDiscovery;
    private LoadBalancer loadBalancer;
    private RpcClient rpcClient;
    
    @SuppressWarnings("unchecked")
    public <T> T createProxy(Class<T> interfaceClass) {
        return (T) Proxy.newProxyInstance(
            interfaceClass.getClassLoader(),
            new Class<?>[]{interfaceClass},
            this
        );
    }
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 1. 构建 RPC 请求
        RpcRequest request = RpcRequest.builder()
            .requestId(UUID.randomUUID().toString())
            .interfaceName(method.getDeclaringClass().getName())
            .methodName(method.getName())
            .paramTypes(method.getParameterTypes())
            .params(args)
            .build();
        
        // 2. 服务发现
        String serviceName = method.getDeclaringClass().getName();
        List<ServiceInfo> services = serviceDiscovery.discover(serviceName);
        
        if (services.isEmpty()) {
            throw new RpcException("No service available: " + serviceName);
        }
        
        // 3. 负载均衡
        ServiceInfo selected = loadBalancer.select(services, request);
        
        // 4. 发送请求
        RpcResponse response = rpcClient.send(request, selected);
        
        // 5. 处理响应
        if (response.getCode() != 200) {
            throw new RpcException(response.getMessage());
        }
        
        return response.getData();
    }
}

/**
 * 使用示例
 */
@Service
public class OrderService {
    
    @RpcReference  // 自定义注解，注入代理对象
    private UserService userService;
    
    public Order createOrder(Long userId) {
        // 透明调用远程服务
        User user = userService.getById(userId);
        // ...
    }
}
```

## 四、序列化

```java
/**
 * 序列化接口
 */
public interface Serializer {
    byte[] serialize(Object obj);
    <T> T deserialize(byte[] data, Class<T> clazz);
}

/**
 * Protobuf 序列化（推荐）
 */
public class ProtobufSerializer implements Serializer {
    
    private static final RuntimeSchema<RpcRequest> requestSchema = 
        RuntimeSchema.createFrom(RpcRequest.class);
    
    @Override
    public byte[] serialize(Object obj) {
        LinkedBuffer buffer = LinkedBuffer.allocate(512);
        try {
            return ProtostuffIOUtil.toByteArray(obj, requestSchema, buffer);
        } finally {
            buffer.clear();
        }
    }
    
    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) {
        RuntimeSchema<T> schema = RuntimeSchema.createFrom(clazz);
        T obj = schema.newMessage();
        ProtostuffIOUtil.mergeFrom(data, obj, schema);
        return obj;
    }
}

/**
 * 序列化类型枚举
 */
public enum SerializationType {
    JSON(1, new JsonSerializer()),
    PROTOBUF(2, new ProtobufSerializer()),
    HESSIAN(3, new HessianSerializer());
    
    private int code;
    private Serializer serializer;
}
```

## 五、网络通信 (Netty)

```java
/**
 * Netty RPC 服务端
 */
public class NettyRpcServer {
    
    private int port;
    private Map<String, Object> serviceMap = new ConcurrentHashMap<>();
    
    public void start() {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                            .addLast(new RpcDecoder())        // 解码
                            .addLast(new RpcEncoder())        // 编码
                            .addLast(new IdleStateHandler(30, 0, 0))  // 心跳
                            .addLast(new RpcServerHandler(serviceMap)); // 业务处理
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childOption(ChannelOption.SO_KEEPALIVE, true);
            
            ChannelFuture future = bootstrap.bind(port).sync();
            future.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}

/**
 * 服务端处理器
 */
@ChannelHandler.Sharable
public class RpcServerHandler extends SimpleChannelInboundHandler<RpcRequest> {
    
    private Map<String, Object> serviceMap;
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequest request) {
        // 异步处理
        CompletableFuture.supplyAsync(() -> handleRequest(request))
            .thenAccept(response -> ctx.writeAndFlush(response));
    }
    
    private RpcResponse handleRequest(RpcRequest request) {
        try {
            // 1. 获取服务实现
            Object service = serviceMap.get(request.getInterfaceName());
            
            // 2. 反射调用
            Method method = service.getClass().getMethod(
                request.getMethodName(), request.getParamTypes());
            Object result = method.invoke(service, request.getParams());
            
            // 3. 构建响应
            return RpcResponse.success(request.getRequestId(), result);
        } catch (Exception e) {
            return RpcResponse.fail(request.getRequestId(), e.getMessage());
        }
    }
}
```

## 六、负载均衡

```java
/**
 * 负载均衡接口
 */
public interface LoadBalancer {
    ServiceInfo select(List<ServiceInfo> services, RpcRequest request);
}

/**
 * 随机
 */
public class RandomLoadBalancer implements LoadBalancer {
    private Random random = new Random();
    
    @Override
    public ServiceInfo select(List<ServiceInfo> services, RpcRequest request) {
        return services.get(random.nextInt(services.size()));
    }
}

/**
 * 加权轮询
 */
public class WeightedRoundRobinLoadBalancer implements LoadBalancer {
    private AtomicInteger index = new AtomicInteger(0);
    
    @Override
    public ServiceInfo select(List<ServiceInfo> services, RpcRequest request) {
        // 简单轮询
        int i = index.getAndIncrement() % services.size();
        return services.get(i);
    }
}

/**
 * 一致性哈希
 */
public class ConsistentHashLoadBalancer implements LoadBalancer {
    private TreeMap<Long, ServiceInfo> virtualNodes = new TreeMap<>();
    private static final int VIRTUAL_NODE_NUM = 100;
    
    @Override
    public ServiceInfo select(List<ServiceInfo> services, RpcRequest request) {
        buildHashRing(services);
        
        // 根据请求参数计算 hash
        long hash = hash(request.getInterfaceName() + request.getMethodName());
        
        // 顺时针找第一个节点
        Map.Entry<Long, ServiceInfo> entry = virtualNodes.ceilingEntry(hash);
        if (entry == null) {
            entry = virtualNodes.firstEntry();
        }
        return entry.getValue();
    }
}
```

## 框架组件总结

```
┌─────────────────────────────────────────────────────────────┐
│                    RPC 框架核心模块                          │
├──────────────────┬──────────────────────────────────────────┤
│   模块            │   实现技术                               │
├──────────────────┼──────────────────────────────────────────┤
│   动态代理        │   JDK Proxy / CGLIB / Javassist         │
│   序列化          │   Protobuf / Hessian / JSON             │
│   网络通信        │   Netty / HTTP/2                        │
│   服务注册发现    │   ZooKeeper / Nacos / Consul            │
│   负载均衡        │   随机 / 轮询 / 一致性哈希               │
│   容错机制        │   重试 / 熔断 / 降级                     │
│   协议设计        │   自定义二进制协议                       │
│   扩展机制        │   SPI 机制                              │
└──────────────────┴──────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> RPC 框架核心模块：1）**动态代理**透明调用；2）**序列化**（Protobuf）；3）**网络传输**（Netty）；4）**服务注册发现**（ZooKeeper）；5）**负载均衡**；6）**容错**（重试/熔断）。调用流程：代理拦截 → 服务发现 → 负载均衡 → 序列化 → 网络发送 → 服务端反射调用 → 返回结果。

### 1分钟版本

> **RPC 框架设计**：
>
> **核心组件**：
> - **协议**：魔数 + 版本 + 序列化类型 + 消息类型 + 请求ID + 数据长度 + 数据体
> - **动态代理**：JDK/CGLIB，拦截方法调用封装 RpcRequest
> - **序列化**：Protobuf（高性能）/ Hessian（跨语言）
> - **网络通信**：Netty（高性能NIO）
> - **服务注册发现**：ZooKeeper 临时节点 + 本地缓存
> - **负载均衡**：随机、轮询、一致性哈希
>
> **调用流程**：
> 1. 代理拦截 → 封装 RpcRequest
> 2. 从注册中心发现服务列表
> 3. 负载均衡选择节点
> 4. 序列化 → Netty 发送
> 5. 服务端反序列化 → 反射调用
> 6. 返回结果
>
> **扩展**：容错重试、熔断降级、链路追踪、SPI 扩展点

---

*关联文档：[http-vs-rpc.md](../01-network/http-vs-rpc.md) | [nio-vs-netty.md](../05-concurrency/nio-vs-netty.md)*
