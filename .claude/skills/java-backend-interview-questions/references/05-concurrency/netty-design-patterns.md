# Netty 采用了哪些设计模式？

## 设计模式概览

```
┌─────────────────────────────────────────────────────────────┐
│                    Netty 中的设计模式                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. Reactor 模式 (反应器模式)                              │
│   2. 责任链模式 (Chain of Responsibility)                   │
│   3. 工厂模式 (Factory)                                     │
│   4. 建造者模式 (Builder)                                   │
│   5. 策略模式 (Strategy)                                    │
│   6. 装饰器模式 (Decorator)                                 │
│   7. 观察者模式 (Observer)                                  │
│   8. 单例模式 (Singleton)                                   │
│   9. 适配器模式 (Adapter)                                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 1. Reactor 模式

```
┌─────────────────────────────────────────────────────────────┐
│                    Reactor 模式                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   Netty 的核心设计模式，用于处理高并发网络 I/O               │
│                                                             │
│   单 Reactor 单线程:                                        │
│   ┌─────────────────────────────────────────────────────┐  │
│   │              ┌─────────────┐                         │  │
│   │   Accept ───→│   Reactor   │───→ Read/Write/Decode  │  │
│   │              │  (单线程)   │                         │  │
│   │              └─────────────┘                         │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   主从 Reactor 多线程 (Netty 默认):                         │
│   ┌─────────────────────────────────────────────────────┐  │
│   │              ┌─────────────┐                         │  │
│   │   Accept ───→│ BossGroup   │                         │  │
│   │              │(MainReactor)│                         │  │
│   │              └──────┬──────┘                         │  │
│   │                     │ 分发连接                       │  │
│   │              ┌──────▼──────┐                         │  │
│   │              │ WorkerGroup │                         │  │
│   │              │(SubReactors)│──→ Read/Write/业务处理  │  │
│   │              │ 多个EventLoop│                        │  │
│   │              └─────────────┘                         │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   代码示例:                                                  │
│   EventLoopGroup bossGroup = new NioEventLoopGroup(1);     │
│   EventLoopGroup workerGroup = new NioEventLoopGroup();    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 2. 责任链模式

```
┌─────────────────────────────────────────────────────────────┐
│                    责任链模式                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ChannelPipeline 是典型的责任链                            │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │                   ChannelPipeline                    │  │
│   │   ┌───────┐   ┌───────┐   ┌───────┐   ┌───────┐    │  │
│   │   │Handler│→→→│Handler│→→→│Handler│→→→│Handler│    │  │
│   │   │  1    │   │  2    │   │  3    │   │  4    │    │  │
│   │   └───────┘   └───────┘   └───────┘   └───────┘    │  │
│   │   Decoder    LengthField  Business    Encoder      │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   入站方向: Head → Handler1 → Handler2 → ... → Tail        │
│   出站方向: Tail → ... → Handler2 → Handler1 → Head        │
│                                                             │
│   代码示例:                                                  │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  pipeline.addLast(new LengthFieldDecoder())          │  │
│   │          .addLast(new JsonDecoder())                 │  │
│   │          .addLast(new BusinessHandler())             │  │
│   │          .addLast(new JsonEncoder());                │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 3. 工厂模式

```java
/**
 * 工厂模式 - ChannelFactory
 */
public class FactoryPatternDemo {
    
    public void demo() {
        ServerBootstrap bootstrap = new ServerBootstrap();
        
        // 通过工厂创建 Channel
        bootstrap.channel(NioServerSocketChannel.class);  // 指定工厂类型
        
        // 内部实现:
        // ReflectiveChannelFactory<NioServerSocketChannel> factory 
        //     = new ReflectiveChannelFactory<>(NioServerSocketChannel.class);
        // Channel channel = factory.newChannel();
    }
}

// Netty 的 ChannelFactory 接口
public interface ChannelFactory<T extends Channel> {
    T newChannel();  // 工厂方法
}
```

## 4. 建造者模式

```java
/**
 * 建造者模式 - ServerBootstrap
 */
public class BuilderPatternDemo {
    
    public void demo() {
        ServerBootstrap bootstrap = new ServerBootstrap()
            .group(bossGroup, workerGroup)          // 设置线程组
            .channel(NioServerSocketChannel.class)  // 设置 Channel 类型
            .option(ChannelOption.SO_BACKLOG, 128)  // 设置选项
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ch.pipeline().addLast(new MyHandler());
                }
            });
        
        // 链式调用，一步步构建复杂对象
    }
}
```

## 5. 策略模式

```java
/**
 * 策略模式 - EventExecutorChooser
 */
public class StrategyPatternDemo {
    
    // Netty 选择 EventLoop 的策略
    // 根据 EventLoop 数量选择不同策略
    
    // 策略1: 2的幂次方时，用位运算
    private static final class PowerOfTwoEventExecutorChooser 
            implements EventExecutorChooser {
        @Override
        public EventExecutor next() {
            return executors[idx.getAndIncrement() & (executors.length - 1)];
        }
    }
    
    // 策略2: 非2的幂次方时，用取模
    private static final class GenericEventExecutorChooser 
            implements EventExecutorChooser {
        @Override
        public EventExecutor next() {
            return executors[(int) Math.abs(idx.getAndIncrement() % executors.length)];
        }
    }
}
```

## 6. 装饰器模式

```java
/**
 * 装饰器模式 - WrappedByteBuf
 */
public class DecoratorPatternDemo {
    
    // ByteBuf 的装饰器实现
    // UnpooledHeapByteBuf, PooledByteBuf, WrappedByteBuf 等
    
    // 例如 WrappedByteBuf 包装 ByteBuf 添加额外功能
    class WrappedByteBuf extends ByteBuf {
        protected final ByteBuf buf;  // 被装饰的对象
        
        protected WrappedByteBuf(ByteBuf buf) {
            this.buf = buf;
        }
        
        @Override
        public int readInt() {
            return buf.readInt();  // 委托给被装饰对象
        }
    }
}
```

## 7. 观察者模式

```java
/**
 * 观察者模式 - ChannelFuture 和 Listener
 */
public class ObserverPatternDemo {
    
    public void demo() {
        ChannelFuture future = bootstrap.connect("127.0.0.1", 8080);
        
        // 添加监听器 (观察者)
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture cf) {
                if (cf.isSuccess()) {
                    System.out.println("连接成功");
                } else {
                    System.out.println("连接失败");
                }
            }
        });
        
        // 使用 Lambda
        future.addListener(f -> {
            if (f.isSuccess()) {
                System.out.println("操作成功");
            }
        });
    }
}
```

## 8. 单例模式

```java
/**
 * 单例模式
 */
public class SingletonPatternDemo {
    
    // Netty 中的单例:
    // 1. ReadTimeoutException.INSTANCE
    // 2. WriteTimeoutException.INSTANCE
    // 3. DefaultEventExecutorChooserFactory.INSTANCE
    
    public static final ReadTimeoutException INSTANCE = 
        new ReadTimeoutException();
    
    private ReadTimeoutException() {
        // 私有构造
    }
}
```

## 9. 适配器模式

```java
/**
 * 适配器模式 - ChannelHandlerAdapter
 */
public class AdapterPatternDemo {
    
    // ChannelInboundHandlerAdapter
    // ChannelOutboundHandlerAdapter
    // 提供默认空实现，用户只需重写需要的方法
    
    public class MyHandler extends ChannelInboundHandlerAdapter {
        
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            // 只重写需要的方法
        }
        
        // 其他方法使用默认实现
    }
}
```

## 设计模式总结

```
┌─────────────────────────────────────────────────────────────┐
│                    设计模式总结                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   模式              应用场景                                 │
│   ─────────────────────────────────────────────────────     │
│   Reactor          EventLoop 事件处理                       │
│   责任链           ChannelPipeline Handler 链                │
│   工厂             ChannelFactory 创建 Channel               │
│   建造者           ServerBootstrap 链式配置                  │
│   策略             EventExecutorChooser 选择策略             │
│   装饰器           ByteBuf 包装类                            │
│   观察者           ChannelFuture + Listener                  │
│   单例             Exception 实例                            │
│   适配器           ChannelHandlerAdapter                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> Netty 用了很多设计模式：1）**Reactor 模式**：BossGroup 处理连接，WorkerGroup 处理 I/O；2）**责任链模式**：ChannelPipeline 中 Handler 链式处理；3）**建造者模式**：ServerBootstrap 链式配置；4）**工厂模式**：ChannelFactory 创建 Channel；5）**观察者模式**：ChannelFuture + Listener 异步回调；6）**适配器模式**：ChannelHandlerAdapter 提供默认实现。

### 1分钟版本

> **Netty 核心设计模式**：
>
> 1. **Reactor 模式**
>    - 主从 Reactor 多线程
>    - BossGroup 处理 Accept
>    - WorkerGroup 处理读写
>
> 2. **责任链模式**
>    - ChannelPipeline
>    - Handler 链式处理入站/出站事件
>
> 3. **建造者模式**
>    - ServerBootstrap
>    - 链式配置：group().channel().option()...
>
> 4. **工厂模式**
>    - ChannelFactory
>    - ReflectiveChannelFactory
>
> 5. **观察者模式**
>    - ChannelFuture + ChannelFutureListener
>    - 异步操作完成通知
>
> 6. **适配器模式**
>    - ChannelInboundHandlerAdapter
>    - 提供默认空实现
>
> 7. **策略模式**
>    - EventExecutorChooser
>    - 2 的幂次方用位运算，否则取模

---

*关联文档：[netty-performance.md](netty-performance.md) | [reactor-pattern.md](reactor-pattern.md)*
