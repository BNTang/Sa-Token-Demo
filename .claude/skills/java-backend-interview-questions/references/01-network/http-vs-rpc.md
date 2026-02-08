# HTTP 与 RPC 之间的区别？

## 概念澄清

```
┌─────────────────────────────────────────────────────────────┐
│                    HTTP vs RPC 概念                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   常见误区：HTTP 和 RPC 是对立的                             │
│   正确理解：HTTP 是协议，RPC 是调用方式                       │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  RPC (Remote Procedure Call)                        │  │
│   │  └── 远程过程调用，调用方式的抽象                     │  │
│   │  └── 可以基于 HTTP，也可以基于 TCP 自定义协议         │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  HTTP                                               │  │
│   │  └── 应用层协议，定义请求/响应格式                    │  │
│   │  └── RESTful API 是基于 HTTP 的架构风格              │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   实际对比通常是：RESTful API vs RPC 框架                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## RESTful vs RPC 框架对比

```
┌─────────────────────────────────────────────────────────────┐
│                  RESTful vs RPC 框架                         │
├──────────────────┬──────────────────┬───────────────────────┤
│   特性            │    RESTful       │     RPC 框架          │
├──────────────────┼──────────────────┼───────────────────────┤
│   协议            │    HTTP          │   自定义/HTTP/TCP     │
│   序列化          │    JSON/XML      │   Protobuf/Thrift     │
│   传输效率        │    一般          │   高                  │
│   调用方式        │    资源导向      │   方法导向            │
│   接口定义        │    URL + Method  │   IDL (接口描述语言)  │
│   跨语言          │    天然支持      │   需要生成代码        │
│   浏览器访问      │    支持          │   不支持              │
│   服务发现        │    需要额外组件  │   内置                │
│   负载均衡        │    需要额外组件  │   内置                │
│   学习成本        │    低            │   较高                │
├──────────────────┴──────────────────┴───────────────────────┤
│   典型场景：                                                 │
│   • RESTful: 对外开放 API、前后端分离、跨组织调用            │
│   • RPC: 内部微服务通信、高性能要求场景                      │
└─────────────────────────────────────────────────────────────┘
```

## 协议栈对比

```
┌─────────────────────────────────────────────────────────────┐
│                    协议栈对比                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   RESTful (HTTP/JSON):                                      │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  应用层: JSON 数据                                   │  │
│   │  表示层: 文本编码                                    │  │
│   │  会话层: HTTP 协议                                   │  │
│   │  传输层: TCP                                         │  │
│   │  网络层: IP                                          │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   gRPC (HTTP/2 + Protobuf):                                 │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  应用层: Protobuf 二进制数据                         │  │
│   │  传输层: HTTP/2 多路复用                             │  │
│   │  传输层: TCP                                         │  │
│   │  网络层: IP                                          │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   Dubbo (自定义协议):                                        │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  应用层: Hessian2/Protobuf 序列化                    │  │
│   │  传输层: Dubbo 协议 (自定义二进制)                   │  │
│   │  传输层: TCP (Netty)                                 │  │
│   │  网络层: IP                                          │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 代码对比

### RESTful API

```java
// 服务端 - Spring Boot
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.findById(id);
    }
    
    @PostMapping
    public User createUser(@RequestBody User user) {
        return userService.save(user);
    }
}

// 客户端 - RestTemplate
@Service
public class UserClient {
    
    private RestTemplate restTemplate;
    
    public User getUser(Long id) {
        return restTemplate.getForObject(
            "http://user-service/api/users/{id}", 
            User.class, id);
    }
}
```

### gRPC

```protobuf
// user.proto - 接口定义
syntax = "proto3";

service UserService {
    rpc GetUser(UserRequest) returns (UserResponse);
    rpc CreateUser(User) returns (User);
}

message UserRequest {
    int64 id = 1;
}

message UserResponse {
    int64 id = 1;
    string name = 2;
}
```

```java
// 服务端
public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {
    
    @Override
    public void getUser(UserRequest request, StreamObserver<UserResponse> responseObserver) {
        User user = userDao.findById(request.getId());
        UserResponse response = UserResponse.newBuilder()
            .setId(user.getId())
            .setName(user.getName())
            .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}

// 客户端 - 像调用本地方法一样调用
public class UserClient {
    
    private UserServiceGrpc.UserServiceBlockingStub stub;
    
    public UserResponse getUser(long id) {
        UserRequest request = UserRequest.newBuilder().setId(id).build();
        return stub.getUser(request);  // 像本地方法调用
    }
}
```

### Dubbo

```java
// 接口定义 (API 模块)
public interface UserService {
    User getUser(Long id);
    User createUser(User user);
}

// 服务端 (Provider)
@DubboService
public class UserServiceImpl implements UserService {
    
    @Override
    public User getUser(Long id) {
        return userDao.findById(id);
    }
}

// 客户端 (Consumer) - 像本地方法一样调用
@Service
public class OrderService {
    
    @DubboReference
    private UserService userService;  // 远程服务
    
    public void createOrder(Long userId) {
        User user = userService.getUser(userId);  // 透明调用
        // ...
    }
}
```

## 性能对比

```
┌─────────────────────────────────────────────────────────────┐
│                    性能对比                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   序列化效率:                                                │
│   ┌────────────────┬───────────┬────────────────────────┐  │
│   │   格式          │  大小      │  序列化速度             │  │
│   ├────────────────┼───────────┼────────────────────────┤  │
│   │   JSON         │  100%     │  基准                   │  │
│   │   Protobuf     │  30-50%   │  3-10 倍快              │  │
│   │   Hessian2     │  50-70%   │  2-5 倍快               │  │
│   └────────────────┴───────────┴────────────────────────┘  │
│                                                             │
│   QPS 对比 (简单接口):                                       │
│   ┌────────────────┬───────────────────────────────────┐   │
│   │   框架          │  QPS                              │   │
│   ├────────────────┼───────────────────────────────────┤   │
│   │   HTTP/JSON    │  ~10,000                          │   │
│   │   gRPC         │  ~50,000                          │   │
│   │   Dubbo        │  ~30,000                          │   │
│   └────────────────┴───────────────────────────────────┘   │
│                                                             │
│   注: 实际性能取决于具体场景，仅供参考                        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 选型建议

```
┌─────────────────────────────────────────────────────────────┐
│                    选型建议                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   使用 RESTful API:                                          │
│   ├── 对外开放的公共 API                                    │
│   ├── 前后端分离的 Web 应用                                 │
│   ├── 跨组织/跨公司的系统集成                               │
│   ├── 需要浏览器直接访问                                    │
│   └── 团队对 HTTP 更熟悉                                    │
│                                                             │
│   使用 RPC 框架 (gRPC/Dubbo):                                │
│   ├── 内部微服务之间通信                                    │
│   ├── 对性能要求高的场景                                    │
│   ├── 需要服务治理能力（熔断、限流、负载均衡）               │
│   ├── 需要流式通信（gRPC）                                  │
│   └── 已有成熟的 RPC 基础设施                               │
│                                                             │
│   混合使用:                                                  │
│   ├── BFF 对外暴露 RESTful API                              │
│   └── 内部服务之间用 RPC                                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> HTTP 是协议，RPC 是调用方式，不是对立关系。通常比较的是 **RESTful vs RPC 框架**。RESTful 基于 HTTP+JSON，通用性好适合对外 API；RPC 框架（gRPC、Dubbo）使用二进制序列化+服务治理，性能高适合内部微服务通信。

### 1分钟版本

> **概念区分**：
> - HTTP：应用层协议
> - RPC：远程调用方式，可基于 HTTP 或 TCP
> - 实际对比：RESTful API vs RPC 框架
>
> **核心区别**：
>
> | 特性 | RESTful | RPC框架 |
> |------|---------|---------|
> | 序列化 | JSON文本 | Protobuf二进制 |
> | 性能 | 较低 | 高(3-10倍) |
> | 调用方式 | URL+Method | 像本地方法调用 |
> | 服务治理 | 需额外组件 | 内置 |
>
> **选型建议**：
> - 对外 API：RESTful（通用、易理解）
> - 内部微服务：RPC（高性能、服务治理）
> - 大厂实践：BFF 对外 REST，内部 gRPC/Dubbo

---

*关联文档：[dubbo-vs-gateway.md](../09-microservice/dubbo-vs-gateway.md) | [rpc-framework-design.md](../14-system-design/rpc-framework-design.md)*
