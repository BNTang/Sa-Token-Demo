# 什么是 Java 的 SPI 机制？

## SPI 概述

```
┌─────────────────────────────────────────────────────────────┐
│              SPI (Service Provider Interface)                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   定义: 服务提供者接口，一种服务发现机制                    │
│                                                             │
│   核心思想:                                                  │
│   • 接口在调用方定义                                        │
│   • 实现由第三方提供                                        │
│   • 运行时动态加载实现                                      │
│                                                             │
│   ┌─────────────┐    定义接口    ┌─────────────────────┐   │
│   │   JDK/框架   │ ───────────→ │    Interface        │   │
│   │   (调用方)   │               │  (服务接口)         │   │
│   └─────────────┘               └─────────────────────┘   │
│                                           ▲                 │
│                                           │ 实现            │
│                                 ┌─────────┴─────────┐      │
│                                 │                   │       │
│                           ┌──────────┐        ┌──────────┐ │
│                           │  实现A   │        │  实现B   │ │
│                           │ (MySQL)  │        │ (Oracle) │ │
│                           └──────────┘        └──────────┘ │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## SPI 使用步骤

```
┌─────────────────────────────────────────────────────────────┐
│                    SPI 使用步骤                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 定义服务接口                                            │
│                                                             │
│   2. 创建实现类                                              │
│                                                             │
│   3. 在 META-INF/services/ 目录下创建配置文件               │
│      文件名 = 接口全限定名                                  │
│      文件内容 = 实现类全限定名 (每行一个)                   │
│                                                             │
│   4. 使用 ServiceLoader 加载                                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

```java
// 1. 定义接口
package com.example;

public interface PayService {
    void pay(BigDecimal amount);
}

// 2. 实现类 A
package com.example.impl;

public class AlipayService implements PayService {
    @Override
    public void pay(BigDecimal amount) {
        System.out.println("支付宝支付: " + amount);
    }
}

// 3. 实现类 B
package com.example.impl;

public class WechatPayService implements PayService {
    @Override
    public void pay(BigDecimal amount) {
        System.out.println("微信支付: " + amount);
    }
}
```

```
// 4. 创建配置文件
// 文件路径: META-INF/services/com.example.PayService
// 文件内容:
com.example.impl.AlipayService
com.example.impl.WechatPayService
```

```java
// 5. 加载服务
ServiceLoader<PayService> loader = ServiceLoader.load(PayService.class);

for (PayService service : loader) {
    service.pay(new BigDecimal("100"));
}
// 输出:
// 支付宝支付: 100
// 微信支付: 100
```

## JDK 中的 SPI 应用

```
┌─────────────────────────────────────────────────────────────┐
│                    JDK SPI 应用                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. JDBC                                                    │
│      ├── 接口: java.sql.Driver                              │
│      ├── 实现: com.mysql.cj.jdbc.Driver                     │
│      └── MySQL 驱动 jar 包含配置文件                        │
│                                                             │
│   2. 日志门面                                                │
│      ├── 接口: org.slf4j.spi.SLF4JServiceProvider          │
│      └── 实现: logback、log4j2                              │
│                                                             │
│   3. Servlet 容器                                            │
│      └── javax.servlet.ServletContainerInitializer          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 框架中的 SPI

```
┌─────────────────────────────────────────────────────────────┐
│                    框架 SPI 应用                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   Dubbo SPI:                                                 │
│   ├── 增强了 JDK SPI                                        │
│   ├── 配置文件: META-INF/dubbo/                             │
│   ├── 支持 key=value 形式                                   │
│   ├── 支持自适应扩展 @Adaptive                              │
│   └── 支持依赖注入                                          │
│                                                             │
│   Spring SPI:                                                │
│   ├── 配置文件: META-INF/spring.factories                   │
│   ├── 用于自动配置                                          │
│   └── Spring Boot 的 @EnableAutoConfiguration               │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

```java
// Dubbo SPI 示例
// 配置文件: META-INF/dubbo/com.example.PayService
// alipay=com.example.impl.AlipayService
// wechat=com.example.impl.WechatPayService

ExtensionLoader<PayService> loader = 
    ExtensionLoader.getExtensionLoader(PayService.class);

PayService alipay = loader.getExtension("alipay");  // 按 key 获取
```

## SPI 与 API 对比

```
┌─────────────────────────────────────────────────────────────┐
│                    SPI vs API                                │
├──────────────────┬──────────────────┬───────────────────────┤
│   维度           │   API            │   SPI                 │
├──────────────────┼──────────────────┼───────────────────────┤
│   调用方         │ 第三方           │ 框架/JDK              │
│   实现方         │ 框架/JDK         │ 第三方                │
│   目的           │ 提供功能给调用者 │ 扩展框架功能          │
│   典型场景       │ 工具类、SDK      │ 驱动、插件            │
├──────────────────┴──────────────────┴───────────────────────┤
│   API: 实现方提供接口和实现                                  │
│   SPI: 实现方提供接口，调用方提供实现                        │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> **SPI** 是服务提供者接口，一种服务发现机制。框架定义接口，第三方提供实现，运行时动态加载。配置文件放在 `META-INF/services/接口全限定名`，内容是实现类。使用 `ServiceLoader.load()` 加载。JDK 应用：JDBC Driver。框架扩展：Dubbo SPI、Spring Factories。

### 1分钟版本

> **定义**：
> - 服务提供者接口
> - 框架定义接口，第三方提供实现
>
> **使用步骤**：
> 1. 定义接口
> 2. 创建实现类
> 3. 配置文件：META-INF/services/接口全名
> 4. ServiceLoader.load() 加载
>
> **JDK 应用**：
> - JDBC：Driver 加载
> - 日志门面
>
> **框架扩展**：
> - Dubbo SPI：支持 key=value、依赖注入
> - Spring Factories：自动配置
>
> **SPI vs API**：
> - API：框架提供实现给调用者
> - SPI：框架定义接口，调用者提供实现

---

*关联文档：[java-reflection.md](java-reflection.md) | [classloader.md](classloader.md)*
