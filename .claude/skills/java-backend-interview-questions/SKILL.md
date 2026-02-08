---
name: java-backend-interview-questions
description: Java 后端开发面试知识点及最佳编码实践。涵盖Java基础(序列化、Exception vs Error、多态、参数传递、重载vs重写、内部类、接口vs抽象类、包装类vs基本类型、String/StringBuilder/StringBuffer、Java8/11/17/21/25新特性、JDK vs JRE、JDK工具、hashCode与equals、动态代理、JDK代理vs CGLIB、注解原理、反射机制、不可变类、SPI机制、泛型/类型擦除/上下界、深拷贝vs浅拷贝、Integer缓存池、类加载过程、双亲委派模型、BigDecimal精度、new String创建对象、final/finally/finalize、乱码问题、String底层char改byte、调用外部程序ProcessBuilder、线程start两次、栈vs队列、Optional类、I/O流、网络编程Socket、基本数据类型、自动装箱拆箱、迭代器Iterator、运行时异常vs编译时异常、继承extends、封装private、访问修饰符、静态方法vs实例方法、for vs foreach、wait vs sleep、Java vs Go、Object类方法、字节码、BIO/NIO/AIO、Channel通道、Selector选择器、Float浮点数比较、POJO/VO/DTO/DAO/BO)、数据结构与集合(HashMap原理/扩容/性能优化、Hash碰撞、ArrayList/LinkedList区别、ArrayList扩容机制、HashSet vs HashMap、HashMap vs Hashtable、ConcurrentHashMap vs Hashtable、CopyOnWriteArrayList、List实现类、数组vs链表、负载因子0.75、2的n次方)、网络协议(TCP/IP、OSI、HTTP版本、HTTP vs HTTPS、HTTP vs RPC、TCP vs UDP、粘包拆包)、Spring(IOC、AOP、循环依赖、三级缓存原因、模块、Bean生命周期、MVC工作原理、DI依赖注入)、MyBatis、MySQL(索引注意事项、不建索引场景)、Redis(过期/淘汰策略、Lua脚本、Pipeline、BigKey、热点Key、持久化、OOM排查、缓存穿透/击穿/雪崩、分布式锁、使用场景、哨兵机制、主从复制、BGSAVE请求处理、机器爆了优化)、Java 并发编程(线程池原理/拒绝策略/线程数设置、锁优化、synchronized实现、NIO/Netty性能原因、并发工具类、volatile关键字、线程生命周期、ABA问题、Netty设计模式)、消息队列(幂等性、有序性、可靠性、堆积处理)、微服务(熔断、降级、雪崩、限流算法、负载均衡、分布式vs微服务、Seata分布式事务)、数据库(分库分表、大表查询)、设计模式(单例)、系统设计(秒杀、短链、分布式ID、点赞系统、RPC框架设计)、JVM(组成、GC调优、GC参数、GC算法、垃圾收集器、OOM场景、内存分析)、问题排查(CPU飙高)等核心知识点。当需要理解原理、面试准备或编写相关代码时使用。
metadata:
  author: senior-java-team
  version: "7.5"
  compatibility: Java 8+, Spring Boot 2.7+/3.x, Redis 6+, MyBatis 3.5+, MyBatis-Plus 3.5+, Kafka, RocketMQ, RabbitMQ, Seata, Netty
---

# Java 后端面试知识点与最佳实践

> 版本: 7.5 | 更新: 2026-02-03
>
> 本知识库由 10 年+ 经验的 Java 资深开发者总结，涵盖面试高频考点及实际开发最佳实践。

---

## 概述

本 Skill 旨在帮助开发者：
1. **理解原理** - 深入理解 Java 后端核心技术的底层原理
2. **面试准备** - 掌握高频面试题的标准答案
3. **最佳实践** - 将理论知识转化为实际编码规范

---

## 何时使用此 Skill

| 场景 | 触发词 |
|------|--------|
| Java基础 | 序列化、反序列化、Exception vs Error、Error、Java优势、多态、参数传递、按值传递、多重继承、OOP、面向对象、面向过程、重载、重写、Overload、Override、内部类、Java8、Java11、Java17、Java21、Java25、Lambda、Stream、虚拟线程、Record、String、StringBuilder、StringBuffer、包装类、基本类型、装箱、拆箱、接口、抽象类、JDK、JRE、JVM工具、jstack、jmap、jstat、hashCode、equals、==操作符、动态代理、JDK代理、CGLIB代理、注解、Annotation、反射、Reflection、不可变类、Immutable、SPI、Service Provider Interface、泛型、Generics、类型擦除、上界、下界、extends、super、深拷贝、浅拷贝、Integer缓存、类加载、ClassLoader、双亲委派、BigDecimal、精度、new String对象、final、finally、finalize、乱码、编码、UTF-8、GBK、char数组、byte数组、Runtime.exec、ProcessBuilder、外部程序、系统命令、线程start两次、IllegalThreadStateException、栈、队列、Stack、Queue、Deque、LIFO、FIFO、Optional、空值处理、NPE、I/O流、InputStream、OutputStream、Reader、Writer、字节流、字符流、网络编程、Socket、TCP编程、UDP编程、HttpClient、基本数据类型、8种、int、long、float、double、char、boolean、自动装箱、自动拆箱、Autoboxing、Unboxing、迭代器、Iterator、fail-fast、ListIterator、运行时异常、编译时异常、Checked Exception、Unchecked Exception、RuntimeException、继承、extends、super、重写规则、封装、private、getter、setter、访问修饰符、public、protected、default、静态方法、实例方法、static方法、for循环、foreach、增强for、双亲委派模型、Parent Delegation、打破双亲委派、wait、sleep、wait vs sleep、释放锁、Java vs Go、Goroutine、并发模型、Object类、11个方法、toString、getClass、clone、notify、notifyAll、字节码、bytecode、.class文件、JIT、javap、BIO、NIO、AIO、同步阻塞、同步非阻塞、异步非阻塞、Channel、通道、Selector、选择器、多路复用、ByteBuffer、Float比较、浮点数比较、epsilon、POJO、VO、DTO、DAO、BO、PO、Entity、数据传输对象、视图对象、Java集合、Collection、Set、List、Map、Queue |
| 网络协议 | TCP、UDP、OSI、四层模型、滑动窗口、TIME_WAIT、超时重传、URL到页面、TCP连接、HTTP版本、HTTP/2、HTTP/3、QUIC、HTTPS、TLS、SSL、HTTP vs RPC、gRPC、Dubbo、TCP作用、TCP vs UDP、粘包、拆包、Netty粘包 |
| Spring | Spring Boot 特性、自动配置、Starter、IOC、DI、依赖注入、AOP、动态代理、CGLIB、循环依赖、三级缓存、为什么三级缓存、Spring模块、Bean生命周期、MVC工作原理、DispatcherServlet |
| MyBatis | MyBatis 缓存、插件、拦截器、#{}和${}、MyBatis vs Hibernate、MyBatis-Plus |
| MySQL | MySQL索引、建索引注意事项、不建索引、索引失效、联合索引、最左前缀、索引优化 |
| Redis | Redis 数据类型、跳表、性能优化、客户端、Hash、事务、Memcached、过期删除、内存淘汰、LRU、LFU、Lua脚本、Pipeline、BigKey、热点Key、HotKey、使用场景、持久化、RDB、AOF、混合持久化、内存溢出、OOM、缓存穿透、缓存击穿、缓存雪崩、分布式锁、Redisson、哨兵、Sentinel、主从复制、BGSAVE、COW、Redis机器爆了 |
| 并发编程 | 线程池、AQS、多线程创建、Reactor、CAS、JMM、volatile、原子性、可见性、有序性、ThreadLocal、final、死锁、Netty、Netty性能、拒绝策略、线程数设置、并发工具类、CountDownLatch、Semaphore、锁优化、synchronized实现、ReentrantLock、NIO、线程生命周期、ABA问题、Netty设计模式 |
| 消息队列 | MQ、Kafka、RocketMQ、RabbitMQ、幂等性、消息有序、消息可靠、消息堆积、推拉模式 |
| 数据结构 | HashMap原理、HashMap扩容、HashMap性能、HashMap负载因子、负载因子0.75、为什么2的n次方、Hash碰撞、哈希冲突、链地址法、开放地址法、ArrayList扩容、ArrayList vs LinkedList、数组vs链表、List实现类、HashSet vs HashMap、HashMap vs Hashtable、ConcurrentHashMap vs Hashtable、ConcurrentHashMap、CopyOnWriteArrayList、synchronizedList、集合框架、Collection |
| 会话管理 | Cookie、Session、Token、JWT |
| 微服务 | API 网关、Dubbo、Spring Cloud Gateway、服务熔断、Sentinel、Resilience4j、服务降级、服务雪崩、限流、限流算法、令牌桶、漏桶、负载均衡、轮询、一致性哈希、分布式vs微服务、Seata、分布式事务、AT模式、TCC |
| 操作系统 | 进程、线程、IO模型、select、poll、epoll、物理地址、逻辑地址 |
| JVM | 编译执行、解释执行、JIT、JVM组成、运行时数据区、GC调优、垃圾回收、GC算法、标记清除、复制算法、标记整理、垃圾收集器、G1、ZGC、CMS、JVM参数、OOM场景、内存分析、jstat、jmap、MAT |
| 问题排查 | CPU飙高、线上排查、top、jstack、Arthas |
| 数据库 | 分库分表、ShardingSphere、大表查询、SELECT * 内存、流式查询 |
| 设计模式 | 设计模式、单例、工厂、策略、代理、SOLID原则、单例模式实现 |
| 系统设计 | 秒杀系统、短链系统、分布式ID、雪花算法、点赞系统、RPC框架设计 |

---

## 知识点目录

### 📁 00-Java 基础

| 主题 | 文件 | 内容概要 |
|------|------|---------|
| **序列化与反序列化** | [java-serialization.md](references/00-java-basics/java-serialization.md) | 序列化概念、Serializable、transient、常用框架 |
| **Exception vs Error** | [exception-vs-error.md](references/00-java-basics/exception-vs-error.md) | Throwable体系、Checked/Unchecked异常、处理最佳实践 |
| **Java优势** | [java-advantages.md](references/00-java-basics/java-advantages.md) | 跨平台、GC、生态、多线程、强类型 |
| **Java多态** | [java-polymorphism.md](references/00-java-basics/java-polymorphism.md) | 多态三条件、运行时多态、编译时多态 |
| **参数传递** | [java-parameter-passing.md](references/00-java-basics/java-parameter-passing.md) | 按值传递、基本类型vs引用类型、常见陷阱 |
| **不支持多重继承** | [java-no-multiple-inheritance.md](references/00-java-basics/java-no-multiple-inheritance.md) | 菱形继承问题、接口多实现、默认方法冲突 |
| **OOP vs POP** | [oop-vs-pop.md](references/00-java-basics/oop-vs-pop.md) | 面向对象vs面向过程、三大特性、适用场景 |
| **重载 vs 重写** | [overload-vs-override.md](references/00-java-basics/overload-vs-override.md) | 两同两小一大、编译时vs运行时多态 |
| **内部类** | [java-inner-class.md](references/00-java-basics/java-inner-class.md) | 成员内部类、静态内部类、匿名内部类、作用 |
| **Java 8 新特性** | [java8-features.md](references/00-java-basics/java8-features.md) | Lambda、Stream、Optional、接口默认方法、新日期API |
| **Java 11 新特性** | [java11-features.md](references/00-java-basics/java11-features.md) | var、String新方法、HttpClient、ZGC(实验) |
| **Java 17 新特性** | [java17-features.md](references/00-java-basics/java17-features.md) | Sealed Classes、Record、Switch表达式、文本块 |
| **Java 21 新特性** | [java21-features.md](references/00-java-basics/java21-features.md) | 虚拟线程、模式匹配switch、Record Patterns、顺序集合 |
| **Java 25 新特性** | [java25-features.md](references/00-java-basics/java25-features.md) | 结构化并发、Scoped Values、Stream Gatherers |
| **String/Buffer/Builder** | [string-buffer-builder.md](references/00-java-basics/string-buffer-builder.md) | 可变性、线程安全、性能对比、使用场景 |
| **StringBuilder实现** | [stringbuilder-implementation.md](references/00-java-basics/stringbuilder-implementation.md) | 底层数组、扩容机制、源码分析 |
| **基本类型vs包装类型** | [primitive-vs-wrapper.md](references/00-java-basics/primitive-vs-wrapper.md) | 8种类型、自动装拆箱、缓存机制、常见陷阱 |
| **接口vs抽象类** | [interface-vs-abstract.md](references/00-java-basics/interface-vs-abstract.md) | is-a vs can-do、Java8+接口新特性、设计选择 |
| **JDK vs JRE** | [jdk-vs-jre.md](references/00-java-basics/jdk-vs-jre.md) | JVM/JRE/JDK关系、Java9+模块化变化 |
| **JDK工具** | [jdk-tools.md](references/00-java-basics/jdk-tools.md) | jps/jstack/jmap/jstat/jcmd、可视化工具、Arthas |
| **hashCode与equals** | [hashcode-equals.md](references/00-java-basics/hashcode-equals.md) | ==操作符、equals方法、hashCode约定、HashMap应用 |
| **动态代理** | [dynamic-proxy.md](references/00-java-basics/dynamic-proxy.md) | 静态vs动态代理、JDK代理、CGLIB代理、应用场景 |
| **JDK代理vs CGLIB** | [jdk-vs-cglib-proxy.md](references/00-java-basics/jdk-vs-cglib-proxy.md) | 接口vs继承、性能对比、Spring选择策略 |
| **注解原理** | [java-annotation.md](references/00-java-basics/java-annotation.md) | 元注解、注解本质、运行时/编译时处理、APT |
| **反射机制** | [java-reflection.md](references/00-java-basics/java-reflection.md) | Class对象、获取字段/方法、性能优化、应用场景 |
| **不可变类** | [java-immutable-class.md](references/00-java-basics/java-immutable-class.md) | 创建规则、防御性拷贝、Record类型 |
| **SPI机制** | [java-spi.md](references/00-java-basics/java-spi.md) | ServiceLoader、META-INF/services、Dubbo/Spring SPI |
| **泛型作用** | [generics-intro.md](references/00-java-basics/generics-intro.md) | 类型安全、消除强转、代码复用、命名约定 |
| **泛型擦除** | [generics-erasure.md](references/00-java-basics/generics-erasure.md) | 类型擦除原理、桥接方法、保留泛型信息 |
| **泛型上下界** | [generics-bounds.md](references/00-java-basics/generics-bounds.md) | extends上界、super下界、PECS原则 |
| **深拷贝vs浅拷贝** | [deep-shallow-copy.md](references/00-java-basics/deep-shallow-copy.md) | 区别图示、clone方法、序列化深拷贝 |
| **Integer缓存池** | [integer-cache.md](references/00-java-basics/integer-cache.md) | -128~127缓存、自动装箱、==陷阱 |
| **类加载过程** | [classloader.md](references/00-java-basics/classloader.md) | 加载→链接→初始化、双亲委派模型 |
| **BigDecimal精度** | [bigdecimal.md](references/00-java-basics/bigdecimal.md) | 为什么精确、内部结构、舍入模式、陷阱 |
| **new String对象数量** | [string-new-objects.md](references/00-java-basics/string-new-objects.md) | 1或2个对象、常量池、intern方法 |
| **final/finally/finalize** | [final-finally-finalize.md](references/00-java-basics/final-finally-finalize.md) | 三者区别、final用法、finally执行、finalize废弃 |
| **乱码问题** | [java-encoding-issue.md](references/00-java-basics/java-encoding-issue.md) | 编码解码不一致、UTF-8统一、常见场景 |
| **String底层byte数组** | [string-byte-array.md](references/00-java-basics/string-byte-array.md) | JDK9变更、Compact Strings、节省内存 |
| **调用外部程序** | [java-process-exec.md](references/00-java-basics/java-process-exec.md) | Runtime.exec、ProcessBuilder、跨平台执行 |
| **线程start两次** | [thread-start-twice.md](references/00-java-basics/thread-start-twice.md) | IllegalThreadStateException、线程状态 |
| **栈vs队列** | [stack-vs-queue.md](references/00-java-basics/stack-vs-queue.md) | LIFO vs FIFO、ArrayDeque、LinkedList |
| **Optional类** | [java-optional.md](references/00-java-basics/java-optional.md) | 避免NPE、orElse、map/flatMap链式操作 |
| **I/O流** | [java-io-stream.md](references/00-java-basics/java-io-stream.md) | 字节流、字符流、缓冲流、NIO.2 Files |
| **网络编程** | [java-network.md](references/00-java-basics/java-network.md) | Socket、TCP/UDP编程、HttpClient |
| **基本数据类型** | [java-primitive-types.md](references/00-java-basics/java-primitive-types.md) | 8种类型、大小范围、类型转换 |
| **自动装箱拆箱** | [autoboxing-unboxing.md](references/00-java-basics/autoboxing-unboxing.md) | valueOf、xxxValue、缓存陷阱、NPE陷阱 |
| **迭代器Iterator** | [java-iterator.md](references/00-java-basics/java-iterator.md) | hasNext/next/remove、fail-fast、安全删除 |
| **运行时vs编译时异常** | [runtime-vs-compile-exception.md](references/00-java-basics/runtime-vs-compile-exception.md) | Checked vs Unchecked、处理方式 |
| **继承机制** | [java-inheritance.md](references/00-java-basics/java-inheritance.md) | extends、super、方法重写规则、单继承 |
| **封装特性** | [java-encapsulation.md](references/00-java-basics/java-encapsulation.md) | private属性、getter/setter、最佳实践 |
| **访问修饰符** | [java-access-modifiers.md](references/00-java-basics/java-access-modifiers.md) | public/protected/default/private、最小权限 |
| **静态vs实例方法** | [static-vs-instance-method.md](references/00-java-basics/static-vs-instance-method.md) | static特点、this访问、不能重写 |
| **for vs foreach** | [for-vs-foreach.md](references/00-java-basics/for-vs-foreach.md) | 语法区别、底层实现、修改删除元素 |
| **双亲委派模型** | [parent-delegation.md](references/00-java-basics/parent-delegation.md) | 加载流程、安全性、打破双亲委派 |
| **wait vs sleep** | [wait-vs-sleep.md](references/00-java-basics/wait-vs-sleep.md) | 释放锁、所属类、唤醒方式 |
| **Java vs Go** | [java-vs-go.md](references/00-java-basics/java-vs-go.md) | 语言特性、并发模型、适用场景 |
| **Object类方法** | [java-object-methods.md](references/00-java-basics/java-object-methods.md) | 11个方法、equals/hashCode、wait/notify |
| **字节码** | [java-bytecode.md](references/00-java-basics/java-bytecode.md) | .class结构、指令、JIT编译、跨语言 |
| **BIO/NIO/AIO** | [bio-nio-aio.md](references/00-java-basics/bio-nio-aio.md) | 同步阻塞、同步非阻塞、异步非阻塞、Selector多路复用 |
| **NIO Channel** | [nio-channel.md](references/00-java-basics/nio-channel.md) | 双向通道、FileChannel、SocketChannel、Buffer交互 |
| **NIO Selector** | [nio-selector.md](references/00-java-basics/nio-selector.md) | 多路复用器、SelectionKey事件、一线程多连接 |
| **Float比较** | [float-comparison.md](references/00-java-basics/float-comparison.md) | epsilon精度、BigDecimal精确比较、Float.compare |
| **POJO/VO/DTO/DAO** | [pojo-vo-dto-dao.md](references/00-java-basics/pojo-vo-dto-dao.md) | 6种对象类型、层间转换、MapStruct |
| **Java集合类** | [java-collections.md](references/00-java-basics/java-collections.md) | Collection/Map体系、List/Set/Queue对比 |

### 📁 01-网络协议

| 主题 | 文件 | 内容概要 |
|------|------|---------|
| **OSI 七层模型** | [osi-model.md](references/01-network/osi-model.md) | 七层职责、协议分布、面试要点 |
| **TCP/IP 四层模型** | [tcp-ip-model.md](references/01-network/tcp-ip-model.md) | 四层架构、与 OSI 对应关系 |
| **TCP TIME_WAIT** | [tcp-time-wait.md](references/01-network/tcp-time-wait.md) | 存在原因、2MSL、编码实践 |
| **TCP 超时重传** | [tcp-retransmission.md](references/01-network/tcp-retransmission.md) | RTO 计算、重传机制、编码考量 |
| **TCP 滑动窗口** | [tcp-sliding-window.md](references/01-network/tcp-sliding-window.md) | 流量控制、拥塞控制、窗口调优 |
| **URL到页面全过程** | [url-to-page.md](references/01-network/url-to-page.md) | DNS解析、TCP连接、HTTP请求、页面渲染 |
| **TCP 连接本质** | [tcp-connection.md](references/01-network/tcp-connection.md) | 四元组、三次握手、四次挥手、连接状态 |
| **HTTP 版本区别** | [http-versions.md](references/01-network/http-versions.md) | HTTP/1.0、1.1、2.0 对比，多路复用、头部压缩 |
| **HTTP/2 vs HTTP/3** | [http-versions-advanced.md](references/01-network/http-versions-advanced.md) | QUIC协议、0-RTT、连接迁移、队头阻塞 |
| **HTTP vs HTTPS** | [http-vs-https.md](references/01-network/http-vs-https.md) | TLS握手、加密方式、CA证书体系 |
| **HTTP vs RPC** | [http-vs-rpc.md](references/01-network/http-vs-rpc.md) | RESTful vs RPC框架、gRPC/Dubbo代码示例 |
| **TCP 解决什么问题** | [tcp-purpose.md](references/01-network/tcp-purpose.md) | 可靠传输、流量控制、拥塞控制、三次握手四次挥手 |
| **TCP vs UDP 区别** | [tcp-vs-udp.md](references/01-network/tcp-vs-udp.md) | 面向连接vs无连接、可靠vs不可靠、适用场景 |
| **TCP 粘包拆包** | [tcp-sticky-packet.md](references/01-network/tcp-sticky-packet.md) | 粘包原因、3种解决方案、Netty解码器 |

### 📁 02-Spring 生态

| 主题 | 文件 | 内容概要 |
|------|------|---------|
| **Spring Boot 简介** | [spring-boot-intro.md](references/02-spring/spring-boot-intro.md) | 什么是 Spring Boot、核心理念 |
| **Spring Boot 核心特性** | [spring-boot-features.md](references/02-spring/spring-boot-features.md) | 自动配置、Starter、Actuator |
| **Spring IOC** | [spring-ioc.md](references/02-spring/spring-ioc.md) | 控制反转、依赖注入、Bean生命周期 |
| **Spring AOP 动态代理** | [spring-aop-proxy.md](references/02-spring/spring-aop-proxy.md) | JDK代理 vs CGLIB、代理选择规则 |
| **Spring AOP** | [spring-aop.md](references/02-spring/spring-aop.md) | AOP概念、5种通知类型、切点表达式、自定义注解 |
| **Spring 模块组成** | [spring-modules.md](references/02-spring/spring-modules.md) | Core Container、AOP、Data Access、Web、Test |
| **Spring 循环依赖** | [spring-circular-dependency.md](references/02-spring/spring-circular-dependency.md) | 三级缓存、解决流程、构造器注入限制 |
| **三级缓存原因** | [spring-three-level-cache-why.md](references/02-spring/spring-three-level-cache-why.md) | 为什么需要三级缓存、AOP代理对象处理、二级不够 |
| **Spring Bean生命周期** | [spring-bean-lifecycle.md](references/02-spring/spring-bean-lifecycle.md) | Bean生命周期8阶段、实例化→属性填充→初始化→销毁 |
| **Spring MVC工作原理** | [spring-mvc-workflow.md](references/02-spring/spring-mvc-workflow.md) | DispatcherServlet、HandlerMapping、ViewResolver |
| **Spring DI依赖注入** | [spring-di.md](references/02-spring/spring-di.md) | 构造器/Setter/字段注入、@Autowired vs @Resource |

### 📁 03-MyBatis

| 主题 | 文件 | 内容概要 |
|------|------|---------|
| **MyBatis 缓存机制** | [mybatis-cache.md](references/03-mybatis/mybatis-cache.md) | 一级缓存、二级缓存、最佳实践 |
| **MyBatis 插件机制** | [mybatis-plugin.md](references/03-mybatis/mybatis-plugin.md) | 插件原理、自定义插件开发 |
| **#{} 和 ${} 区别** | [mybatis-placeholder.md](references/03-mybatis/mybatis-placeholder.md) | 预编译、SQL注入、使用场景 |
| **MyBatis vs Hibernate** | [mybatis-vs-hibernate.md](references/03-mybatis/mybatis-vs-hibernate.md) | 半自动vs全自动ORM、N+1问题 |
| **MyBatis-Plus** | [mybatis-plus.md](references/03-mybatis/mybatis-plus.md) | 通用CRUD、Wrapper、分页插件 |

### 📁 03-MySQL

| 主题 | 文件 | 内容概要 |
|------|------|---------|
| **MySQL索引注意事项** | [mysql-index-tips.md](references/03-mysql/mysql-index-tips.md) | 适合建索引场景、联合索引顺序、索引失效 |
| **MySQL不建索引场景** | [mysql-when-not-index.md](references/03-mysql/mysql-when-not-index.md) | 小表、选择性低、频繁更新、大字段 |

### 📁 04-Redis

| 主题 | 文件 | 内容概要 |
|------|------|---------|
| **Redis 数据类型** | [redis-data-types.md](references/04-redis/redis-data-types.md) | 五种基本类型 + 高级类型、使用场景 |
| **Redis 跳表原理** | [redis-skiplist.md](references/04-redis/redis-skiplist.md) | 跳表结构、查询效率、ZSet 实现 |
| **Redis 客户端选型** | [redis-clients.md](references/04-redis/redis-clients.md) | Jedis、Lettuce、Redisson 对比 |
| **Redis 性能优化** | [redis-performance.md](references/04-redis/redis-performance.md) | 瓶颈分析、优化方案、最佳实践 |
| **Redis Hash 类型** | [redis-hash.md](references/04-redis/redis-hash.md) | Hash结构、编码方式、购物车案例 |
| **Redis vs Memcached** | [redis-vs-memcached.md](references/04-redis/redis-vs-memcached.md) | 数据结构、持久化、集群对比 |
| **Redis 事务** | [redis-transaction.md](references/04-redis/redis-transaction.md) | MULTI/EXEC、WATCH乐观锁、Lua脚本 |
| **Redis 过期删除策略** | [redis-expiration.md](references/04-redis/redis-expiration.md) | 惰性删除、定期删除、主从复制 |
| **Redis 内存淘汰策略** | [redis-eviction.md](references/04-redis/redis-eviction.md) | 8种策略、LRU vs LFU、配置方法 |
| **Redis Lua 脚本** | [redis-lua-script.md](references/04-redis/redis-lua-script.md) | Lua语法、EVAL/EVALSHA、分布式锁、限流器 |
| **Redis Pipeline** | [redis-pipeline.md](references/04-redis/redis-pipeline.md) | 批量操作、减少RTT、与事务区别 |
| **Redis 使用场景** | [redis-use-cases.md](references/04-redis/redis-use-cases.md) | 缓存、分布式锁、排行榜、消息队列、社交关系 |
| **Redis BigKey** | [redis-bigkey.md](references/04-redis/redis-bigkey.md) | 发现方法、危害分析、拆分与删除方案 |
| **Redis 热点Key** | [redis-hotkey.md](references/04-redis/redis-hotkey.md) | 本地缓存、Key分片、JD HotKey框架 |
| **Redis 分布式锁** | [distributed-lock.md](references/04-redis/distributed-lock.md) | Redis/ZooKeeper/MySQL实现、Redisson看门狗 |
| **Redis 持久化** | [redis-persistence.md](references/04-redis/redis-persistence.md) | RDB快照、AOF日志、混合持久化 |
| **Redis 内存溢出** | [redis-oom.md](references/04-redis/redis-oom.md) | OOM排查思路、BigKey检测、淘汰策略配置 |
| **缓存穿透/击穿/雪崩** | [redis-cache-problems.md](references/04-redis/redis-cache-problems.md) | 布隆过滤器、互斥锁、过期时间随机化 |
| **Redis 哨兵机制** | [redis-sentinel.md](references/04-redis/redis-sentinel.md) | 监控、通知、自动故障转移、主观/客观下线 |
| **Redis 主从复制** | [redis-replication.md](references/04-redis/redis-replication.md) | 全量复制、增量复制、PSYNC、复制积压缓冲区 |
| **Redis BGSAVE请求处理** | [redis-bgsave-request-handling.md](references/04-redis/redis-bgsave-request-handling.md) | fork、COW写时复制、快照期间正常响应 |
| **Redis 机器爆了优化** | [redis-crash-optimization.md](references/04-redis/redis-crash-optimization.md) | 内存/CPU/连接数排查、BigKey拆分、读写分离 |

### 📁 05-Java 并发

| 主题 | 文件 | 内容概要 |
|------|------|---------|
| **多线程创建方式** | [thread-creation.md](references/05-concurrency/thread-creation.md) | 四种方式对比、最佳实践 |
| **线程池核心参数** | [thread-pool.md](references/05-concurrency/thread-pool.md) | 核心参数、动态调整、监控 |
| **AQS 原理** | [aqs.md](references/05-concurrency/aqs.md) | 核心原理、CLH 队列、锁实现 |
| **Reactor 模型** | [reactor-model.md](references/05-concurrency/reactor-model.md) | 三种模型、Netty 实现 |
| **final 关键字与可见性** | [java-final-visibility.md](references/05-concurrency/java-final-visibility.md) | final语义、内存屏障、不可变对象 |
| **原子性、可见性、有序性** | [java-atomicity-visibility-ordering.md](references/05-concurrency/java-atomicity-visibility-ordering.md) | 三大特性、volatile、synchronized |
| **Java 内存模型 (JMM)** | [java-memory-model.md](references/05-concurrency/java-memory-model.md) | happens-before、内存屏障、DCL |
| **CAS 操作** | [java-cas.md](references/05-concurrency/java-cas.md) | 原理、ABA问题、Atomic类 |
| **ThreadLocal 弱引用** | [threadlocal-weakreference.md](references/05-concurrency/threadlocal-weakreference.md) | 内存泄漏、remove最佳实践 |
| **Java 死锁** | [java-deadlock.md](references/05-concurrency/java-deadlock.md) | 四个必要条件、预防方法、jstack检测 |
| **Netty 应用场景** | [netty-usage.md](references/05-concurrency/netty-usage.md) | RPC、MQ、IM、游戏服务器 |
| **Netty 高性能原因** | [netty-performance.md](references/05-concurrency/netty-performance.md) | Reactor模型、零拷贝、内存池、串行化设计 |
| **线程池原理** | [thread-pool-principle.md](references/05-concurrency/thread-pool-principle.md) | 7个核心参数、执行流程、状态转换 |
| **线程池拒绝策略** | [thread-pool-rejection.md](references/05-concurrency/thread-pool-rejection.md) | 4种内置策略、自定义处理器 |
| **线程池线程数设置** | [thread-pool-sizing.md](references/05-concurrency/thread-pool-sizing.md) | CPU密集型、IO密集型、动态调优 |
| **并发工具类** | [concurrent-tools.md](references/05-concurrency/concurrent-tools.md) | CountDownLatch、CyclicBarrier、Semaphore |
| **synchronized vs ReentrantLock** | [synchronized-vs-reentrantlock.md](references/05-concurrency/synchronized-vs-reentrantlock.md) | 功能对比、使用场景、公平锁 |
| **synchronized 实现原理** | [synchronized-implementation.md](references/05-concurrency/synchronized-implementation.md) | Mark Word、锁升级、偏向锁/轻量级锁/重量级锁 |
| **NIO vs Netty** | [nio-vs-netty.md](references/05-concurrency/nio-vs-netty.md) | NIO问题、Netty优势、ByteBuf、Reactor模型 |
| **锁优化** | [lock-optimization.md](references/05-concurrency/lock-optimization.md) | 减少锁粒度、读写锁、无锁CAS、ThreadLocal |
| **volatile 关键字** | [volatile-keyword.md](references/05-concurrency/volatile-keyword.md) | 可见性、禁止指令重排、不保证原子性 |
| **线程生命周期** | [thread-lifecycle.md](references/05-concurrency/thread-lifecycle.md) | 6种状态、NEW/RUNNABLE/BLOCKED/WAITING/TIMED_WAITING/TERMINATED |
| **ABA 问题** | [aba-problem.md](references/05-concurrency/aba-problem.md) | CAS的ABA问题、AtomicStampedReference解决 |
| **Netty 设计模式** | [netty-design-patterns.md](references/05-concurrency/netty-design-patterns.md) | Reactor、责任链、建造者、工厂、观察者模式 |
| **线程同步** | [thread-synchronization.md](references/05-concurrency/thread-synchronization.md) | synchronized、Lock、volatile、原子类 |
| **线程安全** | [thread-safety.md](references/05-concurrency/thread-safety.md) | 三要素、实现方式、安全级别 |
| **协程与虚拟线程** | [coroutine-virtual-thread.md](references/05-concurrency/coroutine-virtual-thread.md) | 协程概念、Java 21虚拟线程、线程vs协程 |
| **线程通信** | [thread-communication.md](references/05-concurrency/thread-communication.md) | wait/notify、Condition、BlockingQueue、CountDownLatch |
| **创建多线程** | [create-thread.md](references/05-concurrency/create-thread.md) | 4种方式、Thread/Runnable/Callable/线程池 |
| **线程池线程数设置** | [thread-pool-size.md](references/05-concurrency/thread-pool-size.md) | CPU密集N+1、IO密集2N、压测调优 |
| **Executors线程池实现** | [executors-thread-pools.md](references/05-concurrency/executors-thread-pools.md) | Fixed/Cached/Single/Scheduled/WorkStealing、阿里规范禁用 |
| **线程池动态配置** | [thread-pool-dynamic-config.md](references/05-concurrency/thread-pool-dynamic-config.md) | setCorePoolSize/setMaximumPoolSize、配置中心集成 |
| **线程池shutdown** | [thread-pool-shutdown.md](references/05-concurrency/thread-pool-shutdown.md) | shutdown vs shutdownNow、SHUTDOWN/STOP状态、优雅关闭 |
| **线程池异常追踪** | [thread-pool-exception.md](references/05-concurrency/thread-pool-exception.md) | afterExecute钩子、Future.get()、UncaughtExceptionHandler |
| **DelayQueue vs ScheduledPool** | [delayqueue-vs-scheduledpool.md](references/05-concurrency/delayqueue-vs-scheduledpool.md) | Delayed接口、DelayedWorkQueue、定时任务选型 |
| **Java Timer** | [timer.md](references/05-concurrency/timer.md) | 单线程缺陷、异常终止问题、ScheduledThreadPoolExecutor替代 |
| **时间轮算法** | [time-wheel.md](references/05-concurrency/time-wheel.md) | 环形数组+链表、O(1)添加、Netty/Kafka时间轮 |
| **并发工具类概览** | [concurrent-utils.md](references/05-concurrency/concurrent-utils.md) | 6大类工具分类、同步/锁/容器/队列/原子/线程池 |
| **Semaphore信号量** | [semaphore.md](references/05-concurrency/semaphore.md) | AQS共享模式、限流控制、公平非公平 |
| **CyclicBarrier** | [cyclic-barrier.md](references/05-concurrency/cyclic-barrier.md) | 循环屏障、互相等待、barrierAction回调 |
| **CountDownLatch** | [countdownlatch.md](references/05-concurrency/countdownlatch.md) | 倒计时门闩、一次性、一等多/多等一模式 |
| **StampedLock** | [stamped-lock.md](references/05-concurrency/stamped-lock.md) | 乐观读tryOptimisticRead、validate验证、锁升级 |
| **CompletableFuture** | [completable-future.md](references/05-concurrency/completable-future.md) | 链式调用、thenApply/thenCompose/allOf、异常处理 |
| **ForkJoinPool** | [fork-join-pool.md](references/05-concurrency/fork-join-pool.md) | 分治fork/join、工作窃取算法、RecursiveTask |
| **线程执行顺序控制** | [thread-execution-order.md](references/05-concurrency/thread-execution-order.md) | join/CountDownLatch/Semaphore/Condition控制顺序 |
| **阻塞队列** | [blocking-queues.md](references/05-concurrency/blocking-queues.md) | 7种阻塞队列对比、核心方法、线程池选择 |
| **原子类** | [atomic-classes.md](references/05-concurrency/atomic-classes.md) | 6类原子类、AtomicStampedReference、字段更新器 |
| **LongAdder累加器** | [longadder-accumulator.md](references/05-concurrency/longadder-accumulator.md) | 分Cell减少竞争、base+Cells、@Contended |
| **CAS原理** | [cas.md](references/05-concurrency/cas.md) | Compare-And-Swap、Unsafe、CMPXCHG指令、ABA问题 |

### 📁 06-数据结构

| 主题 | 文件 | 内容概要 |
|------|------|---------|
| **HashMap 扩容机制** | [hashmap-resize.md](references/06-data-structure/hashmap-resize.md) | 2^n 原因、扩容流程、并发问题 |
| **数组与链表** | [array-vs-linkedlist.md](references/06-data-structure/array-vs-linkedlist.md) | 特性对比、选型指南 |
| **HashMap原理** | [hashmap-principle.md](references/06-data-structure/hashmap-principle.md) | 数组+链表+红黑树、put/get流程、扰动函数 |
| **HashMap性能优化** | [hashmap-performance.md](references/06-data-structure/hashmap-performance.md) | 初始容量、hashCode优化、遍历方式 |
| **Hash碰撞** | [hash-collision.md](references/06-data-structure/hash-collision.md) | 链地址法、开放地址法、再哈希法 |
| **CopyOnWriteArrayList vs synchronizedList** | [copyonwrite-vs-synchronized.md](references/06-data-structure/copyonwrite-vs-synchronized.md) | 写时复制、全局锁、适用场景对比 |
| **List实现类** | [list-implementations.md](references/06-data-structure/list-implementations.md) | ArrayList、LinkedList、Vector、CopyOnWriteArrayList |
| **ArrayList vs LinkedList** | [arraylist-vs-linkedlist.md](references/06-data-structure/arraylist-vs-linkedlist.md) | 数组vs双向链表、时间复杂度、使用场景 |
| **ArrayList扩容** | [arraylist-resize.md](references/06-data-structure/arraylist-resize.md) | 1.5倍扩容、grow源码、性能优化 |
| **HashMap vs Hashtable** | [hashmap-vs-hashtable.md](references/06-data-structure/hashmap-vs-hashtable.md) | 线程安全、null支持、红黑树优化 |
| **ConcurrentHashMap vs Hashtable** | [concurrenthashmap-vs-hashtable.md](references/06-data-structure/concurrenthashmap-vs-hashtable.md) | 桶级锁vs全表锁、CAS+synchronized |
| **HashSet vs HashMap** | [hashset-vs-hashmap.md](references/06-data-structure/hashset-vs-hashmap.md) | HashSet底层用HashMap、PRESENT占位 |
| **HashMap 2^n原因** | [hashmap-2n-size.md](references/06-data-structure/hashmap-2n-size.md) | 位运算取模、扩容高位判断、均匀分布 |
| **HashMap负载因子0.75** | [hashmap-load-factor.md](references/06-data-structure/hashmap-load-factor.md) | 时间空间平衡、泊松分布验证 |
| **HashMap红黑树改动** | [hashmap-red-black-tree.md](references/06-data-structure/hashmap-red-black-tree.md) | 为什么引入红黑树、转换条件、阈值8 |
| **HashMap JDK8改动** | [hashmap-jdk8-changes.md](references/06-data-structure/hashmap-jdk8-changes.md) | 尾插法、扩容优化、扰动简化 |
| **LinkedHashMap** | [linkedhashmap.md](references/06-data-structure/linkedhashmap.md) | 插入顺序/访问顺序、LRU缓存实现 |
| **TreeMap** | [treemap.md](references/06-data-structure/treemap.md) | 红黑树、排序Map、NavigableMap |
| **IdentityHashMap** | [identityhashmap.md](references/06-data-structure/identityhashmap.md) | ==比较、引用相等、序列化场景 |
| **WeakHashMap** | [weakhashmap.md](references/06-data-structure/weakhashmap.md) | 弱引用key、自动GC、缓存场景 |
| **ConcurrentHashMap 1.7vs1.8** | [concurrenthashmap-jdk7-vs-jdk8.md](references/06-data-structure/concurrenthashmap-jdk7-vs-jdk8.md) | 分段锁vs桶锁、CAS+synchronized |
| **ConcurrentHashMap get加锁** | [concurrenthashmap-get-lock.md](references/06-data-structure/concurrenthashmap-get-lock.md) | 无锁读取、volatile可见性 |
| **ConcurrentHashMap null问题** | [concurrenthashmap-null-key-value.md](references/06-data-structure/concurrenthashmap-null-key-value.md) | 不支持null key/value原因 |
| **CopyOnWriteArrayList** | [copyonwritearraylist.md](references/06-data-structure/copyonwritearraylist.md) | 写时复制原理、读无锁、适用场景 |
| **ConcurrentModificationException** | [concurrent-modification-exception.md](references/06-data-structure/concurrent-modification-exception.md) | fail-fast机制、modCount检查 |

### 📁 07-会话管理

| 主题 | 文件 | 内容概要 |
|------|------|---------|
| **Cookie/Session/Token** | [session-management.md](references/07-session/session-management.md) | 三者区别、JWT、最佳实践 |

### 📁 08-数据库

| 主题 | 文件 | 内容概要 |
|------|------|---------|
| **分库分表** | [sharding.md](references/08-database/sharding.md) | 垂直/水平拆分、分片策略、ShardingSphere |
| **MySQL 大表查询** | [mysql-big-query.md](references/08-database/mysql-big-query.md) | 流式查询、游标查询、分页批量处理 |

### 📁 09-微服务

| 主题 | 文件 | 内容概要 |
|------|------|---------|
| **Dubbo vs Gateway** | [dubbo-vs-gateway.md](references/09-microservice/dubbo-vs-gateway.md) | RPC框架vs API网关、层次区别 |
| **API 网关** | [api-gateway.md](references/09-microservice/api-gateway.md) | 核心功能、Spring Cloud Gateway |
| **服务熔断** | [circuit-breaker.md](references/09-microservice/circuit-breaker.md) | 三态模型、Resilience4j、Sentinel |
| **服务降级** | [service-degradation.md](references/09-microservice/service-degradation.md) | 降级策略、Sentinel/Resilience4j、多级降级 |
| **服务雪崩** | [service-avalanche.md](references/09-microservice/service-avalanche.md) | 雪崩成因、预防方案、超时/熔断/隔离 |
| **限流算法** | [rate-limiting.md](references/09-microservice/rate-limiting.md) | 固定窗口、滑动窗口、漏桶、令牌桶、Guava RateLimiter |
| **负载均衡算法** | [load-balancing.md](references/09-microservice/load-balancing.md) | 轮询、加权轮询、随机、最少连接、一致性哈希 |
| **分布式vs微服务** | [distributed-vs-microservice.md](references/09-microservice/distributed-vs-microservice.md) | 分布式关注部署、微服务关注架构、技术栈对比 |
| **Seata分布式事务** | [seata.md](references/09-microservice/seata.md) | TC/TM/RM三组件、AT/TCC/SAGA/XA四种模式 |

### 📁 10-操作系统

| 主题 | 文件 | 内容概要 |
|------|------|---------|
| **进程与线程** | [thread-vs-process.md](references/10-os/thread-vs-process.md) | 区别对比、上下文切换、虚拟线程 |
| **I/O 模型** | [io-models.md](references/10-os/io-models.md) | BIO/NIO/多路复用/AIO |
| **Select/Poll/Epoll** | [select-poll-epoll.md](references/10-os/select-poll-epoll.md) | 三种多路复用对比、Epoll优势 |
| **物理地址与逻辑地址** | [physical-logical-address.md](references/10-os/physical-logical-address.md) | 页表、MMU、TLB |

### 📁 11-JVM

| 主题 | 文件 | 内容概要 |
|------|------|---------|
| **编译执行与解释执行** | [compile-vs-interpret.md](references/11-jvm/compile-vs-interpret.md) | JIT编译器、热点代码、分层编译 |
| **JVM 组成** | [jvm-components.md](references/11-jvm/jvm-components.md) | 类加载子系统、运行时数据区、执行引擎 |
| **JVM GC 调优** | [jvm-gc-tuning.md](references/11-jvm/jvm-gc-tuning.md) | 调优目标、GC选择、常用参数、调优流程 |
| **JVM 常用参数** | [jvm-parameters.md](references/11-jvm/jvm-parameters.md) | 堆内存、GC配置、诊断参数、生产配置示例 |
| **垃圾收集器** | [jvm-garbage-collectors.md](references/11-jvm/jvm-garbage-collectors.md) | Serial、Parallel、CMS、G1、ZGC、Shenandoah |
| **GC 算法** | [jvm-gc-algorithms.md](references/11-jvm/jvm-gc-algorithms.md) | 标记-清除、复制算法、标记-整理、分代收集 |
| **JVM OOM 场景** | [jvm-oom-scenarios.md](references/11-jvm/jvm-oom-scenarios.md) | Heap Space、Metaspace、GC Overhead、Native Thread等8种 |
| **JVM 内存分析** | [jvm-memory-analysis.md](references/11-jvm/jvm-memory-analysis.md) | jps、jstat、jmap、MAT分析dump |

### 📁 12-问题排查

| 主题 | 文件 | 内容概要 |
|------|------|---------|
| **CPU飙高排查** | [cpu-high-troubleshoot.md](references/12-troubleshoot/cpu-high-troubleshoot.md) | top定位进程/线程、jstack分析、Arthas快速排查 |

### 📁 13-消息队列

| 主题 | 文件 | 内容概要 |
|------|------|---------|
| **消息幂等性** | [message-idempotency.md](references/13-mq/message-idempotency.md) | 去重表、Redis SETNX、业务幂等 |
| **消息有序性** | [message-ordering.md](references/13-mq/message-ordering.md) | 局部/全局有序、Kafka分区、RocketMQ队列 |
| **消息堆积处理** | [message-backlog.md](references/13-mq/message-backlog.md) | 紧急扩容、消费者优化、预防措施 |
| **消息可靠性** | [message-reliability.md](references/13-mq/message-reliability.md) | 生产端/Broker/消费端防丢策略 |
| **推拉模式** | [push-pull-mode.md](references/13-mq/push-pull-mode.md) | Push vs Pull对比、长轮询原理 |

### 📁 14-设计模式

| 主题 | 文件 | 内容概要 |
|------|------|---------|
| **设计模式概述** | [design-pattern-overview.md](references/14-design-pattern/design-pattern-overview.md) | 23种GoF模式分类、SOLID原则 |
| **单例模式** | [singleton-pattern.md](references/14-design-pattern/singleton-pattern.md) | 6种实现、线程安全、DCL+volatile、枚举单例 |

### 📁 15-系统设计

| 主题 | 文件 | 内容概要 |
|------|------|---------|
| **秒杀系统设计** | [seckill-design.md](references/15-system-design/seckill-design.md) | 架构设计、Redis预减库存、MQ异步、防超卖 |
| **分布式ID生成器** | [distributed-id-generator.md](references/15-system-design/distributed-id-generator.md) | 雪花算法、号段模式、UUID、Redis自增 |
| **短链系统设计** | [short-url-design.md](references/15-system-design/short-url-design.md) | 短码生成、62进制、302重定向、缓存策略 |
| **点赞系统设计** | [like-system-design.md](references/15-system-design/like-system-design.md) | Redis Set去重、异步持久化、热门排行ZSet |
| **RPC 框架设计** | [rpc-framework-design.md](references/15-system-design/rpc-framework-design.md) | 协议设计、服务发现、动态代理、序列化、Netty通信 |

---

## 快速参考表

### Java 基础速查

| 问题 | 核心答案 |
|------|---------|
| **序列化作用** | 对象→字节序列，用于网络传输、持久化、深拷贝 |
| **transient 作用** | 修饰的字段不参与序列化 |
| **Exception vs Error** | Exception可恢复应处理，Error不可恢复不应捕获 |
| **Checked vs Unchecked** | Checked编译时必须处理，Unchecked(RuntimeException)不强制 |
| **Java 三大特性** | 封装、继承、多态 |
| **多态三条件** | 继承、方法重写、父类引用指向子类对象 |
| **参数传递方式** | 永远是按值传递(引用类型传的是地址的拷贝) |
| **为什么不支持多继承** | 菱形继承问题，用接口多实现替代 |
| **重载 vs 重写** | 重载同类不同参数，重写子父类同签名 |
| **重写规则** | 两同两小一大(方法签名同、返回值异常≤、权限≥) |
| **内部类类型** | 成员内部类、静态内部类、局部内部类、匿名内部类 |
| **Java 8 核心** | Lambda、Stream、Optional、接口默认方法、新日期API |
| **Java 11 核心** | var局部变量、String新方法、HttpClient、ZGC(实验) |
| **Java 17 核心** | Sealed Classes、Record、Switch表达式、文本块 |
| **Java 21 核心** | 虚拟线程、模式匹配switch、Record Patterns |
| **虚拟线程优势** | 轻量(几KB)、百万级、IO密集型性能提升 |
| **String不可变原因** | final char[]、线程安全、常量池共享、哈希缓存 |
| **StringBuilder vs StringBuffer** | StringBuilder非线程安全性能高，StringBuffer线程安全 |
| **StringBuilder扩容** | 原容量×2+2，涉及数组拷贝 |
| **Integer缓存范围** | -128 ~ 127 |
| **装拆箱陷阱** | 缓存外==是false、null拆箱NPE、循环装箱性能差 |
| **接口 vs 抽象类** | 接口can-do多实现，抽象类is-a单继承有状态 |
| **JDK vs JRE** | JDK=JRE+开发工具，JRE=JVM+类库 |
| **常用诊断工具** | jps进程、jstack线程栈、jmap内存、jstat GC统计 |
| **hashCode与equals约定** | equals相等→hashCode必须相等；重写equals必须重写hashCode |
| **==与equals区别** | ==比地址(基本类型比值)，equals比内容(需重写) |
| **JDK动态代理** | 基于接口，Proxy+InvocationHandler，反射调用 |
| **CGLIB代理** | 基于继承(生成子类)，不能代理final类/方法 |
| **Spring AOP代理选择** | Spring Boot 2.x默认CGLIB，有接口可用JDK |
| **注解本质** | 继承Annotation的接口，运行时通过反射获取 |
| **@Retention作用** | SOURCE(编译丢弃)、CLASS(运行丢弃)、RUNTIME(可反射) |
| **反射性能** | 比直接调用慢5-20x，需缓存Method对象优化 |
| **反射应用** | Spring IOC、AOP、MyBatis、JSON序列化 |
| **不可变类规则** | final类、private final字段、无setter、深拷贝 |
| **SPI机制** | META-INF/services/接口全名，ServiceLoader加载 |
| **泛型作用** | 类型安全、消除强转、代码复用 |
| **类型擦除** | 泛型编译时存在，运行时擦除为Object或上界 |
| **PECS原则** | Producer Extends(读)，Consumer Super(写) |
| **深拷贝vs浅拷贝** | 浅拷贝共享引用对象，深拷贝完全独立 |
| **Integer缓存陷阱** | 127内==true，128外==false，用equals比较 |
| **类加载过程** | 加载→链接(验证/准备/解析)→初始化 |
| **双亲委派作用** | 避免重复加载，保护核心类不被篡改 |
| **BigDecimal精度原因** | 用BigInteger+scale存储，非二进制浮点 |
| **new String创建对象** | 最多2个(常量池+堆)，常量池已有则1个 |
| **final/finally/finalize** | final不变、finally必执行、finalize已废弃 |
| **乱码原因** | 编码和解码使用不同字符集 |
| **JDK9 String改byte[]** | Compact Strings，纯ASCII节省50%内存 |
| **调用外部程序** | ProcessBuilder推荐，必须读取输出流，设置超时 |
| **线程start两次** | 抛IllegalThreadStateException，只能NEW状态调用start |
| **栈vs队列** | 栈LIFO后进先出(push/pop)，队列FIFO先进先出(offer/poll) |
| **Java中栈实现** | ArrayDeque推荐，Stack类已过时 |
| **Optional作用** | 避免NPE，强制处理空值，用作方法返回值 |
| **Optional获取值** | orElse(默认值)、orElseGet(supplier)、orElseThrow() |
| **I/O流分类** | 字节流(二进制)、字符流(文本)；节点流、处理流 |
| **字节流vs字符流** | 文本用字符流(Reader/Writer)，二进制用字节流 |
| **TCP vs UDP编程** | TCP用Socket/ServerSocket，UDP用DatagramSocket |
| **Java 8种基本类型** | byte/short/int/long/float/double/char/boolean |
| **自动装箱** | 基本→包装，调用valueOf()，有缓存(-128~127) |
| **自动拆箱** | 包装→基本，调用xxxValue()，null会NPE |
| **Iterator优势** | 遍历时可安全删除元素，用iterator.remove() |
| **fail-fast机制** | modCount检测并发修改，抛ConcurrentModificationException |
| **Checked异常** | 编译时必须处理(try-catch或throws)，如IOException |
| **Unchecked异常** | RuntimeException子类，编译不强制处理，如NPE |
| **继承关键字** | extends继承类，super调用父类，单继承多层继承 |
| **方法重写规则** | 签名同、返回类型≤、权限≥、异常≤、不能final/private/static |
| **封装实现** | private属性+public getter/setter，隐藏实现暴露接口 |
| **4种访问修饰符** | private(本类)、default(同包)、protected(同包+子类)、public |
| **静态方法特点** | 类名调用、不能访问this和实例成员、不能重写只能隐藏 |
| **for vs foreach** | foreach简洁底层用迭代器，for有索引可修改删除元素 |
| **双亲委派模型** | 先委托父加载器，父加载不了才自己加载 |
| **打破双亲委派** | JDBC SPI、Tomcat应用隔离、热部署 |
| **wait vs sleep** | wait释放锁用于通信(Object)，sleep不释放锁(Thread) |
| **Java vs Go并发** | Java线程1MB重量级，Go goroutine 2KB轻量级 |
| **Object类方法数** | 11个：equals/hashCode/toString/getClass/clone/wait×3/notify×2/finalize |
| **字节码** | .class文件，0xCAFEBABE开头，JVM解释或JIT编译执行 |
| **BIO/NIO/AIO** | BIO同步阻塞、NIO同步非阻塞(多路复用)、AIO异步非阻塞 |
| **NIO核心组件** | Channel(双向通道)、Buffer(缓冲区)、Selector(多路复用器) |
| **Selector作用** | 一个线程监控多个Channel，事件驱动减少线程数 |
| **Float比较** | 禁止==，用Math.abs(a-b)<1e-6f或BigDecimal |
| **POJO/VO/DTO/DAO** | POJO基础、PO对应表、DTO传输、VO展示、DAO访问 |
| **Java集合框架** | Collection(List/Set/Queue)和Map两大体系 |

### 数据结构速查

| 问题 | 核心答案 |
|------|---------|
| **HashMap底层结构** | 数组+链表+红黑树(JDK8+链表≥8转红黑树) |
| **HashMap put流程** | hash扰动→定位桶→空则创建→链表/树→扩容检查 |
| **HashMap扩容时机** | size > capacity × loadFactor(默认0.75) |
| **HashMap 2^n原因** | hash & (n-1) 位运算代替取模，高效均匀分布 |
| **负载因子0.75** | 时间空间平衡，泊松分布λ=0.5链表达8概率极低 |
| **Hash碰撞解决** | 链地址法(HashMap)、开放地址法(ThreadLocalMap)、再哈希 |
| **ArrayList底层** | 动态数组，随机访问O(1)，增删O(n) |
| **ArrayList扩容** | 原容量×1.5，oldCapacity + (oldCapacity >> 1) |
| **LinkedList底层** | 双向链表，头尾O(1)，随机访问O(n)，实现Deque |
| **ArrayList vs LinkedList** | ArrayList随机访问快，LinkedList头尾操作快 |
| **HashSet实现** | 底层HashMap，元素作Key，PRESENT作Value |
| **HashMap vs Hashtable** | HashMap非线程安全允许null，Hashtable全表锁不允许null |
| **ConcurrentHashMap锁** | JDK7分段锁、JDK8 CAS+synchronized锁桶 |
| **CopyOnWriteArrayList** | 写时复制，适合读多写少，读无锁写加锁 |
| **synchronizedList** | 全局synchronized，所有操作互斥 |
| **LinkedHashMap** | HashMap+双向链表，插入/访问顺序，实现LRU |
| **TreeMap** | 红黑树，按key排序，O(log n) |
| **WeakHashMap** | 弱引用key，自动GC清理，缓存场景 |
| **IdentityHashMap** | ==判断key相等，对象图遍历/序列化 |
| **HashMap红黑树条件** | 链表≥8 且 数组≥64，退化阈值6 |
| **JDK8 HashMap改进** | 尾插法、扩容位判断、扰动简化 |
| **ConcurrentModificationException** | 遍历时修改，modCount!=expectedModCount |

### 网络协议速查

| 问题 | 核心答案 |
|------|---------|
| **TIME_WAIT 存在原因** | 1. 确保最后 ACK 到达 2. 让旧连接数据包消失 |
| **滑动窗口作用** | 流量控制 + 提高传输效率 |
| **超时重传目的** | 解决丢包问题，保证可靠传输 |
| **URL到页面过程** | DNS→TCP→TLS→HTTP→解析→渲染 |
| **TCP连接本质** | 四元组 (源IP:端口, 目的IP:端口) 唯一标识 |
| **HTTP/2 vs HTTP/1.1** | 多路复用、头部压缩、二进制分帧、服务器推送 |
| **HTTP/2 vs HTTP/3** | HTTP/3用QUIC协议(基于UDP)，0-RTT、连接迁移 |
| **HTTP vs HTTPS** | HTTPS = HTTP + TLS，加密传输、CA证书认证 |
| **HTTP vs RPC** | HTTP通用易用，RPC高性能适合内部调用 |
| **TCP解决什么问题** | 丢包(ACK重传)、乱序(序列号)、流控(滑动窗口) |
| **TCP vs UDP** | TCP面向连接可靠，UDP无连接快速，看场景选型 |
| **粘包拆包解决** | 固定长度、分隔符、长度字段(推荐)、Netty解码器 |

### Java 并发速查

| 问题 | 核心答案 |
|------|---------|
| **线程池参数可否动态修改** | 可以，`setCorePoolSize()` / `setMaximumPoolSize()` |
| **AQS 核心** | state 变量 + CLH 双向队列 + CAS |
| **创建线程方式** | Thread、Runnable、Callable、线程池（推荐） |
| **CAS 问题** | ABA问题、自旋开销、只能操作单变量 |
| **JMM 三大特性** | 原子性、可见性、有序性 |
| **ThreadLocal 内存泄漏** | 用完必须 remove() |
| **死锁四个条件** | 互斥、持有等待、不可剥夺、循环等待 |
| **线程池7参数** | 核心线程、最大线程、存活时间、时间单位、队列、工厂、拒绝策略 |
| **线程池拒绝策略** | Abort(抛异常)、Caller(调用者执行)、Discard(丢弃)、DiscardOldest |
| **CPU密集型线程数** | N + 1 (N为CPU核心数) |
| **IO密集型线程数** | N × (1 + 等待时间/计算时间) |
| **CountDownLatch vs CyclicBarrier** | 一次性 vs 可重用，单向等待 vs 互相等待 |
| **synchronized vs ReentrantLock** | synchronized简单，ReentrantLock功能多(超时/公平/Condition) |
| **synchronized实现** | Mark Word、锁升级(偏向锁→轻量级锁→重量级锁) |
| **锁优化策略** | 减少锁粒度、锁分离、无锁CAS、ThreadLocal |
| **NIO vs Netty** | Netty解决了NIO的Epoll bug、API复杂、ByteBuffer限制 |
| **Netty高性能原因** | Reactor模型、零拷贝、内存池、串行化设计 |
| **volatile作用** | 可见性(MESI)、禁止重排(内存屏障)、不保证原子性 |
| **线程6种状态** | NEW、RUNNABLE、BLOCKED、WAITING、TIMED_WAITING、TERMINATED |
| **ABA问题解决** | AtomicStampedReference(版本号)、AtomicMarkableReference(标记) |
| **Netty设计模式** | Reactor、责任链(Pipeline)、建造者(Bootstrap)、工厂、观察者 |
| **线程同步** | synchronized、Lock、volatile、Atomic原子类 |
| **线程安全定义** | 多线程执行结果与单线程一致，无需额外同步 |
| **线程安全三要素** | 原子性、可见性、有序性 |
| **协程/虚拟线程** | 用户态轻量级线程，Java 21 Virtual Thread |
| **虚拟线程优势** | 几KB内存，百万级，IO密集型性能提升 |
| **线程通信方式** | volatile、wait/notify、Condition、BlockingQueue |
| **创建线程4种方式** | Thread、Runnable、Callable、线程池(推荐) |
| **Executors 5种线程池** | FixedThreadPool/CachedThreadPool/SingleThread/ScheduledThreadPool/WorkStealingPool |
| **Executors缺陷** | 阿里规范禁用，无界队列/无限线程可能OOM |
| **shutdown vs shutdownNow** | shutdown等待任务完成(SHUTDOWN)，shutdownNow中断所有(STOP) |
| **线程池异常追踪** | afterExecute钩子、try-catch任务、Future.get()、UncaughtExceptionHandler |
| **DelayQueue vs ScheduledPool** | DelayQueue纯队列、ScheduledPool=线程池+DelayedWorkQueue |
| **Timer缺陷** | 单线程阻塞、异常终止整个Timer、不推荐使用 |
| **时间轮算法** | 环形数组+链表，O(1)添加定时任务，Netty/Kafka使用 |
| **Semaphore作用** | 信号量限流，AQS共享模式，acquire/release |
| **CyclicBarrier vs CountDownLatch** | CyclicBarrier互相等待可重用，CountDownLatch一等多一次性 |
| **StampedLock乐观读** | tryOptimisticRead()无锁读，validate()验证，比RRWL高效 |
| **CompletableFuture优势** | 链式调用、组合异步、异常处理、非阻塞 |
| **ForkJoinPool工作窃取** | 每个线程有独立双端队列，空闲线程从别的队列尾部窃取任务 |
| **线程执行顺序控制** | join/CountDownLatch/Semaphore/Condition/CompletableFuture |
| **7种阻塞队列** | ArrayBQ/LinkedBQ/PriorityBQ/DelayQueue/SynchronousQueue/LinkedTransferQueue/LinkedBlockingDeque |
| **6类原子类** | 基本/数组/引用/字段更新器/累加器/FieldUpdater |
| **AtomicStampedReference** | 解决ABA问题，带版本号的CAS |
| **LongAdder优势** | 分Cell减少竞争，高并发比AtomicLong快5-10倍 |
| **CAS三大问题** | ABA问题、自旋开销、只能操作单变量 |
| **AQS两种模式** | 独占模式(ReentrantLock)、共享模式(Semaphore/CountDownLatch) |

### Spring 速查

| 问题 | 核心答案 |
|------|---------|
| **AOP 5种通知** | Before、After、AfterReturning、AfterThrowing、Around |
| **Spring 模块** | Core Container、AOP、Data Access、Web、Test |
| **循环依赖解决** | 三级缓存(singletonObjects/earlySingletonObjects/singletonFactories) |
| **循环依赖限制** | 构造器注入、prototype作用域无法解决 |
| **为什么三级缓存** | 解决AOP代理对象的循环依赖，延迟决策是否创建代理 |
| **Bean生命周期** | 实例化→属性填充→Aware→BeanPostProcessor→初始化→销毁 |
| **MVC工作原理** | DispatcherServlet→HandlerMapping→HandlerAdapter→ViewResolver |
| **DI三种方式** | 构造器注入(推荐)、Setter注入、字段注入(不推荐) |

### Redis 速查

| 问题 | 核心答案 |
|------|---------|
| **基本数据类型** | String、List、Hash、Set、ZSet |
| **跳表时间复杂度** | O(log n) 查询/插入/删除 |
| **推荐客户端** | Redisson（功能全）/ Lettuce（高性能） |
| **Redis vs Memcached** | 数据结构丰富、支持持久化、原生集群 |
| **Redis 事务** | MULTI/EXEC，不支持回滚，可用 Lua 脚本 |
| **过期删除策略** | 惰性删除 + 定期删除 (100ms 随机检查) |
| **8种内存淘汰策略** | noeviction、volatile-lru/lfu/ttl/random、allkeys-lru/lfu/random |
| **LRU vs LFU** | 最近最少使用 vs 最不常使用 |
| **Lua脚本作用** | 原子性执行多命令、分布式锁、限流器 |
| **Pipeline作用** | 批量发送命令、减少RTT、不保证原子性 |
| **BigKey定义** | String>10KB 或 集合>5000元素 |
| **BigKey解决** | 拆分、压缩、UNLINK异步删除、SCAN替代全量命令 |
| **Redis常见场景** | 缓存、分布式锁、排行榜、消息队列、会话、社交关系 |
| **热点Key解决** | 本地缓存、Key分片、读写分离、JD HotKey |
| **分布式锁实现** | Redis(SETNX+Lua)、Redisson、ZooKeeper |
| **RDB vs AOF** | RDB快照恢复快、AOF数据更安全，推荐混合持久化 |
| **Redis OOM排查** | INFO memory、--bigkeys、检查淘汰策略 |
| **缓存穿透** | 查不存在数据，用布隆过滤器或缓存空值 |
| **缓存击穿** | 热点Key过期，用互斥锁或逻辑过期 |
| **缓存雪崩** | 大量Key同时过期，用随机过期时间+多级缓存 |
| **哨兵机制** | 监控、通知、自动故障转移；主观下线→客观下线 |
| **主从复制** | 全量复制(BGSAVE+RDB)、增量复制(复制积压缓冲区) |
| **BGSAVE请求处理** | fork+COW，主进程继续响应，子进程写快照 |
| **Redis机器爆了** | 查内存(--bigkeys)、查CPU(SLOWLOG)、查连接数(CLIENT LIST) |

### MySQL 速查

| 问题 | 核心答案 |
|------|---------|
| **建索引注意** | 选择性高的列、WHERE/JOIN列、联合索引最左前缀 |
| **不建索引场景** | 小表、选择性低(性别)、频繁更新、大字段 |
| **联合索引顺序** | 选择性高放前面、范围查询放最后 |
| **索引失效** | 函数操作、隐式转换、LIKE %开头、OR连接非索引列 |

### 消息队列速查

| 问题 | 核心答案 |
|------|---------|
| **消息幂等方案** | 去重表、Redis SETNX、唯一约束、乐观锁 |
| **消息有序方案** | 同Key同分区(Kafka)、MessageQueueSelector(RocketMQ) |
| **消息堆积处理** | 紧急扩容分区/消费者、快速转存、限流降级 |
| **消息可靠性** | 生产端确认 + Broker持久化 + 消费端手动ACK |
| **Push vs Pull** | Push实时但易压垮消费者，Pull可控但有延迟 |
| **长轮询原理** | 无消息时服务端Hold请求，有消息立即返回 |

### 微服务速查

| 问题 | 核心答案 |
|------|---------|
| **API 网关作用** | 路由、认证、限流、监控 |
| **Dubbo vs Gateway** | RPC框架 vs API网关，不同层次 |
| **服务熔断三态** | CLOSED(正常)→OPEN(熔断)→HALF_OPEN(试探) |
| **熔断框架** | Resilience4j (轻量)、Sentinel (阿里全家桶) |
| **服务降级 vs 熔断** | 降级=主动返回兜底数据，熔断=故障后自动切断调用 |
| **服务雪崩** | 一个服务故障引发连锁反应，整个系统崩溃 |
| **雪崩预防** | 超时控制、服务熔断、服务降级、资源隔离、限流 |
| **限流算法** | 固定窗口、滑动窗口、漏桶、令牌桶(最常用) |
| **令牌桶 vs 漏桶** | 令牌桶允许突发流量，漏桶严格限速 |
| **负载均衡算法** | 轮询、加权轮询、随机、最少连接、一致性哈希 |
| **分布式vs微服务** | 分布式关注部署分散，微服务关注业务拆分 |
| **Seata组件** | TC(事务协调者)、TM(事务管理器)、RM(资源管理器) |
| **Seata模式** | AT(无侵入推荐)、TCC(业务侵入)、SAGA(长事务)、XA(强一致) |

### JVM 速查

| 问题 | 核心答案 |
|------|---------|
| **JVM三大组成** | 类加载子系统、运行时数据区、执行引擎 |
| **运行时数据区** | 堆、方法区(共享) + 栈、PC、本地方法栈(私有) |
| **GC调优目标** | 低延迟、高吞吐量、低内存占用(三者权衡) |
| **GC选择** | 低延迟用ZGC，高吞吐用Parallel，均衡用G1 |
| **JVM核心参数** | -Xms/-Xmx(堆)、-XX:+UseG1GC(GC)、-XX:+HeapDumpOnOutOfMemoryError |
| **常见垃圾收集器** | Serial、Parallel、CMS(废弃)、G1(默认)、ZGC、Shenandoah |
| **GC算法** | 标记-清除(有碎片)、复制(年轻代)、标记-整理(老年代) |
| **分代收集** | 年轻代用复制算法，老年代用标记-整理 |
| **OOM 8种场景** | Heap Space、Metaspace、GC Overhead、Direct Buffer、Unable to create native thread等 |
| **内存分析工具** | jps(进程)、jstat(GC统计)、jmap(dump)、MAT(分析) |
| **CPU飙高排查** | top→top -Hp→printf %x→jstack→分析线程栈 |

### 系统设计速查

| 问题 | 核心答案 |
|------|---------|
| **秒杀防超卖** | Redis Lua原子扣减库存 + MQ异步创建订单 |
| **分布式ID方案** | 雪花算法(推荐)、号段模式、Redis INCR |
| **雪花算法结构** | 64位 = 1符号 + 41时间戳 + 10机器ID + 12序列号 |
| **短链生成** | 分布式ID转62进制，6位支持56亿 |
| **短链重定向** | 302临时重定向(可统计)，301会被浏览器缓存 |
| **点赞系统设计** | Redis Set去重 + 异步持久化 + ZSet热门排行 |
| **RPC框架核心** | 协议设计、服务发现、动态代理、序列化、网络通信 |

### 操作系统速查

| 问题 | 核心答案 |
|------|---------|
| **进程 vs 线程** | 资源分配单位 vs 调度执行单位 |
| **Select vs Epoll** | O(n)遍历 vs O(1)事件驱动 |
| **物理 vs 逻辑地址** | 真实内存地址 vs 程序虚拟地址 |

---

## 编码最佳实践总结

### ✅ 推荐做法

| 场景 | 最佳实践 |
|------|---------|
| 创建线程 | 使用线程池，禁止裸创建 Thread |
| Redis 客户端 | 生产环境用 Redisson 或 Lettuce |
| MyBatis 缓存 | 慎用二级缓存，配合 Redis |
| HashMap 初始化 | 指定初始容量 `(expectedSize / 0.75) + 1` |
| 会话管理 | 分布式系统用 Token/JWT |
| ThreadLocal | 用完必须在 finally 中 remove() |
| MyBatis 占位符 | 使用 #{} 防止 SQL 注入 |
| CAS 高并发 | 使用 LongAdder 代替 AtomicLong |
| 分库分表 | 使用雪花算法生成分布式 ID |
| API 网关 | 只做路由和横切功能，不放业务逻辑 |
| 死锁预防 | 统一资源获取顺序 + tryLock超时 |
| 线程池配置 | 根据任务类型选择：CPU密集型N+1，IO密集型N×2 |
| 消息幂等 | 业务唯一ID + 去重表/Redis |
| 消息消费 | 手动ACK，消费成功后再提交 |
| 服务熔断 | 设置合理的失败率阈值和恢复策略 |
| 大表查询 | 使用流式/游标查询或分页批量处理 |
| Redis内存 | 生产环境配置 maxmemory-policy 为 allkeys-lru |
| IOC 依赖注入 | 首选构造器注入，次选 Setter 注入 |
| Spring AOP | 理解 CGLIB 代理限制，避免 final 类/方法 |
| 单例模式 | 推荐枚举单例或静态内部类，DCL需要volatile |
| 秒杀系统 | Redis预减库存+MQ异步创建订单 |
| 分布式ID | 使用雪花算法，注意时钟回拨问题 |
| 短链设计 | 62进制编码，302重定向方便统计 |
| Redis BigKey | 拆分、压缩、UNLINK删除、定期监控 |
| 服务雪崩 | 超时+熔断+降级+隔离组合使用 |
| JVM GC | 根据场景选择GC，开启GC日志分析 |

### ❌ 避免做法

| 场景 | 禁止做法 |
|------|---------|
| 并发编程 | `new Thread().start()` 裸创建线程 |
| Redis 操作 | 同步阻塞客户端处理高并发 |
| HashMap | 不指定容量导致频繁扩容 |
| Session | 分布式系统依赖服务器 Session |
| ThreadLocal | 不调用 remove() 导致内存泄漏 |
| MyBatis | 用 ${} 拼接用户输入 (SQL注入风险) |
| Redis 事务 | 依赖事务回滚 (Redis不支持回滚) |
| 死锁风险 | 嵌套锁不释放、持锁做IO操作 |
| 消息消费 | 自动ACK模式处理重要业务消息 |
| 线程池 | 使用无界队列 (OOM风险) |
| 大表查询 | 一次性 SELECT * FROM 千万级表 (OOM) |
| 消息重试 | 不做幂等性校验的无限重试 |
| IOC 注入 | 过度使用 @Autowired 字段注入 |
| Redis 操作 | KEYS/HGETALL/SMEMBERS等全量命令(用SCAN替代) |
| BigKey 删除 | 直接DEL大Key(用UNLINK异步删除) |
| 循环依赖 | 构造器注入形成循环(用@Lazy或重构) |
