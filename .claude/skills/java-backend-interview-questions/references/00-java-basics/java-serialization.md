# Java 中的序列化和反序列化是什么？

## 概念定义

```
┌─────────────────────────────────────────────────────────────┐
│                    序列化与反序列化                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   序列化 (Serialization):                                   │
│   将 Java 对象转换为字节序列的过程                          │
│   目的: 便于存储或网络传输                                  │
│                                                             │
│   反序列化 (Deserialization):                               │
│   将字节序列恢复为 Java 对象的过程                          │
│                                                             │
│   ┌──────────┐    序列化    ┌──────────┐                   │
│   │ Java对象 │ ──────────→ │ 字节序列 │                   │
│   └──────────┘              └──────────┘                   │
│        ↑                          │                         │
│        │        反序列化          │                         │
│        └──────────────────────────┘                         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 使用场景

```
┌─────────────────────────────────────────────────────────────┐
│                    序列化使用场景                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 网络传输                                                │
│      └── RPC 调用、分布式系统通信                           │
│                                                             │
│   2. 持久化存储                                              │
│      └── 对象保存到文件、数据库                             │
│                                                             │
│   3. 深拷贝                                                  │
│      └── 通过序列化+反序列化实现对象深拷贝                  │
│                                                             │
│   4. 缓存                                                    │
│      └── Redis 存储对象                                     │
│                                                             │
│   5. 分布式 Session                                         │
│      └── Session 复制到多个节点                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## JDK 原生序列化

```java
import java.io.*;

// 1. 实现 Serializable 接口
public class User implements Serializable {
    
    // 版本号，用于版本控制
    private static final long serialVersionUID = 1L;
    
    private String name;
    private int age;
    
    // transient 修饰的字段不会被序列化
    private transient String password;
    
    // static 字段不会被序列化（属于类，不属于对象）
    private static String company = "ABC";
    
    // 构造器、getter/setter 省略
}

// 2. 序列化
public void serialize(User user) throws IOException {
    try (ObjectOutputStream oos = new ObjectOutputStream(
            new FileOutputStream("user.dat"))) {
        oos.writeObject(user);
    }
}

// 3. 反序列化
public User deserialize() throws IOException, ClassNotFoundException {
    try (ObjectInputStream ois = new ObjectInputStream(
            new FileInputStream("user.dat"))) {
        return (User) ois.readObject();
    }
}
```

## serialVersionUID 作用

```
┌─────────────────────────────────────────────────────────────┐
│                    serialVersionUID                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   作用: 版本控制，确保序列化和反序列化的类版本一致          │
│                                                             │
│   不指定:                                                    │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  JVM 会根据类结构自动生成                            │  │
│   │  类修改后 UID 改变 → 反序列化失败                    │  │
│   │  抛出 InvalidClassException                          │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   显式指定:                                                  │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  private static final long serialVersionUID = 1L;    │  │
│   │  类修改后仍可反序列化（需要兼容）                    │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   最佳实践: 始终显式声明 serialVersionUID                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 常见序列化框架

```
┌─────────────────────────────────────────────────────────────┐
│                    序列化框架对比                            │
├──────────────┬──────────────────────────────────────────────┤
│   框架       │   特点                                       │
├──────────────┼──────────────────────────────────────────────┤
│   JDK 原生   │ 简单，但性能差、体积大、不跨语言             │
│   JSON       │ 可读性好、跨语言，性能一般 (Jackson/Gson)    │
│   Hessian    │ 二进制、跨语言、Dubbo 默认                   │
│   Protobuf   │ 高性能、体积小、跨语言、需要 IDL 定义        │
│   Kryo       │ Java 最快、体积小、不跨语言                  │
│   Avro       │ 大数据领域常用、动态 Schema                  │
└──────────────┴──────────────────────────────────────────────┘
```

```java
// JSON 序列化示例 (Jackson)
ObjectMapper mapper = new ObjectMapper();
String json = mapper.writeValueAsString(user);  // 序列化
User user = mapper.readValue(json, User.class); // 反序列化

// Protobuf 示例 (需要先定义 .proto 文件)
byte[] bytes = user.toByteArray();              // 序列化
User user = User.parseFrom(bytes);              // 反序列化
```

## 注意事项

```
┌─────────────────────────────────────────────────────────────┐
│                    序列化注意事项                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 安全问题                                                │
│      • 反序列化可能执行恶意代码                             │
│      • 不要反序列化不可信的数据                             │
│      • 使用白名单限制可反序列化的类                         │
│                                                             │
│   2. 性能问题                                                │
│      • JDK 原生序列化性能较差                               │
│      • 高性能场景用 Protobuf/Kryo                           │
│                                                             │
│   3. 兼容性问题                                              │
│      • 添加字段: 新字段使用默认值                           │
│      • 删除字段: 旧数据中的字段被忽略                       │
│      • 修改字段类型: 可能失败                               │
│                                                             │
│   4. 继承问题                                                │
│      • 父类也需要实现 Serializable                          │
│      • 否则父类字段不会被序列化                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> **序列化**是将 Java 对象转换为字节序列，**反序列化**是将字节序列恢复为对象。用于网络传输、持久化存储、深拷贝等场景。JDK 原生通过实现 `Serializable` 接口，需要声明 `serialVersionUID` 做版本控制。`transient` 修饰的字段不参与序列化。常用框架：JSON（可读性好）、Protobuf（高性能）、Kryo（Java 最快）。

### 1分钟版本

> **概念**：
> - 序列化：对象 → 字节序列
> - 反序列化：字节序列 → 对象
>
> **使用场景**：网络传输、持久化、深拷贝、缓存
>
> **JDK 原生序列化**：
> - 实现 `Serializable` 接口
> - 声明 `serialVersionUID` 版本控制
> - `transient` 字段不序列化
> - `static` 字段不序列化
>
> **常用框架**：
> - JSON (Jackson/Gson)：可读、跨语言
> - Protobuf：高性能、体积小
> - Kryo：Java 最快
>
> **注意**：
> - 安全问题：不反序列化不可信数据
> - 显式声明 serialVersionUID

---

*关联文档：[deep-copy.md](deep-copy.md) | [json-serialization.md](json-serialization.md)*
