---
name: java-spring-guidelines
description: Java 和 Spring Boot 编码规范最佳实践。涵盖命名约定、依赖注入、日志规范、异常处理、Controller/Service/Mapper 层设计、缓存策略、事务控制、并发安全、性能优化、数据库设计、安全规范等。当编写 Java 代码、Spring Boot 控制器/服务/映射器、数据库设计或进行代码审查时使用。
compatibility: Spring Boot 2.7+ / 3.x, MyBatis Plus 3.5+, Redisson 3.x
metadata:
  author: senior-java-team
  version: "5.0"
---

# Java/Spring Boot 编码规范

> 版本: 5.0 | 更新: 2026-01-28
>
> 本规范由 10 年+ 经验的 Java 资深开发者总结，涵盖企业级应用开发的最佳实践。

---

## 概述

本规范适用于基于 **Spring Boot + MyBatis Plus** 架构的企业级 Java 应用开发，旨在确保代码质量、可维护性和系统稳定性。

### 技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 2.7+ / 3.x | 应用框架 |
| MyBatis Plus | 3.5+ | ORM 框架 |
| Lombok | 最新版 | 简化代码 |
| Redisson | 3.x | 分布式锁 |
| RocketMQ | 5.x | 消息队列/分布式事务 |

---

## 何时使用此 Skill

当用户进行以下操作时激活此技能：

| 场景 | 触发词 |
|------|--------|
| 编写代码 | 写 Java、写 Controller、写 Service、写 Mapper、新建接口 |
| 命名咨询 | 怎么命名、类名、方法名、变量名、命名规范 |
| 代码审查 | 审查代码、code review、PR review、检查代码 |
| 架构设计 | Service 怎么写、事务怎么处理、缓存怎么设计 |
| 问题解决 | 并发问题、N+1 查询、事务失效、锁问题 |
| 最佳实践 | 最佳实践、规范、标准写法 |
| 数据库 | 建表、索引设计、字段命名 |

---

## 快速参考

### 核心规范速查

| 规范 | 要点 |
|------|------|
| **依赖注入** | `@RequiredArgsConstructor` + `private final` |
| **日志** | `@Slf4j` + `{}` 占位符 + `[业务名]` |
| **异常** | `ServiceExceptionUtil.exception()` |
| **Controller** | 只接收参数、调用 Service、返回结果 |
| **Mapper** | 简单查询用 Lambda API，复杂用 XML |
| **事务** | `@Transactional(rollbackFor = Exception.class)` |
| **校验** | `@Valid @RequestBody` + 字段校验注解 |
| **缓存** | Key 格式 `{业务}:{模块}:{标识}` + 必须 TTL |

### 禁止项速查

| ❌ 禁止 | ✅ 正确做法 |
|--------|-----------|
| `@Autowired` 字段注入 | 构造器注入 |
| 字符串拼接日志 | `{}` 占位符 |
| `throw new RuntimeException()` | `exception(错误码)` |
| Controller 写业务逻辑 | 业务逻辑放 Service |
| 循环查数据库 | 批量查询 + 内存关联 |
| `${}` SQL 拼接 | `#{}` 预编译 |
| 事务混用多数据源 | 拆分方法 |
| 永不过期缓存 | 必须设置 TTL |

---

## 详细规范目录

完整规范内容按主题组织，点击查看详情：

### 📁 01-基础编码规范

| 主题 | 文件 | 内容概要 |
|------|------|---------|
| **命名规范** | [naming.md](references/01-basics/naming.md) | 类命名、变量命名、表命名、接口路径命名、禁止拼音中文 |
| **常量定义** | [constants.md](references/01-basics/constants.md) | 魔法值禁止、常量分类、枚举代替常量、复用层次 |
| **代码格式** | [code-format.md](references/01-basics/code-format.md) | 大括号、空格、缩进、换行、注释格式、文件编码 |
| **OOP 规约** | [oop.md](references/01-basics/oop.md) | 静态访问、equals、包装类、构造方法、覆写规则 |
| **控制语句** | [control-flow.md](references/01-basics/control-flow.md) | if/switch/for 规范、卫语句、Optional |
| **注释规约** | [comment.md](references/01-basics/comment.md) | Javadoc、类注释、方法注释、TODO/FIXME |
| **代码风格** | [coding-style.md](references/01-basics/coding-style.md) | Import 规则、依赖注入、对象转换、空安全 |

### 📁 02-架构与分层

| 主题 | 文件 | 内容概要 |
|------|------|---------|
| **工程结构** | [project-structure.md](references/02-architecture/project-structure.md) | 应用分层、目录规范、多模块、依赖管理、DO/DTO/VO 转换 |
| **Spring 框架核心** | [spring-framework.md](references/02-architecture/spring-framework.md) | Spring/SpringBoot 启动流程、Bean 生命周期、事务传播行为、设计模式应用 |
| **分布式系统** | [distributed-system.md](references/02-architecture/distributed-system.md) | 无状态化、分布式锁、分布式事务、服务治理、中间件集群配置 |
| **Controller 层** | [controller.md](references/02-architecture/controller.md) | RESTful 规范、参数校验、返回类型、职责边界 |
| **Service 层** | [service.md](references/02-architecture/service.md) | 复杂度控制、卫语句、策略模式、事务边界 |
| **Mapper 层** | [mapper.md](references/02-architecture/mapper.md) | Lambda API、XML 查询、SQL 安全、动态排序 |

### 📁 03-数据库

| 主题 | 文件 | 内容概要 |
|------|------|---------|
| **数据库设计** | [database.md](references/03-database/database.md) | 表设计、字段规范、索引规范、建表模板 |
| **事务管理** | [transaction.md](references/03-database/transaction.md) | 事务规则、隔离级别详解（脏读/不可重复读/幻读）、多数据源限制、MVCC、Read View |
| **MySQL 锁机制** | [mysql-locks.md](references/03-database/mysql-locks.md) | 乐观锁、悲观锁、行锁机制、死锁排查与解决 |
| **MySQL 索引优化** | [mysql-index.md](references/03-database/mysql-index.md) | InnoDB 引擎、索引设计、索引失效场景、查询优化、EXPLAIN 分析 |
| **MySQL 索引原理** | [mysql-btree.md](references/03-database/mysql-btree.md) | B+树结构、聚簇索引、非聚簇索引、回表、索引下推、三层B+树存储量计算 |
| **MySQL 事务实现** | [mysql-transaction-impl.md](references/03-database/mysql-transaction-impl.md) | redo/undo log、二阶段提交、锁机制（行锁/间隙锁/Next-Key）、MVCC、长事务问题 |
| **MySQL 性能调优** | [mysql-optimization.md](references/03-database/mysql-optimization.md) | EXPLAIN 分析、count 函数对比、深度分页优化、SQL 调优步骤 |
| **SQL 执行过程** | [mysql-sql-execution.md](references/03-database/mysql-sql-execution.md) | 连接器、分析器、优化器、执行器、存储引擎、完整执行流程详解 |

### 📁 04-并发与集合

| 主题 | 文件 | 内容概要 |
|------|------|---------|
| **并发控制** | [concurrency.md](references/04-concurrency/concurrency.md) | 乐观锁、分布式锁、幂等性设计 |
| **线程编程** | [thread.md](references/04-concurrency/thread.md) | 线程池、ThreadLocal、锁、volatile、并发集合 |
| **集合处理** | [collection.md](references/04-concurrency/collection.md) | ArrayList/HashMap、遍历删除、toArray、集合转Map |
| **HashMap 线程安全** | [hashmap-concurrent.md](references/04-concurrency/hashmap-concurrent.md) | HashMap vs ConcurrentHashMap、JDK 1.7 vs 1.8、线程安全集合选择 |

### 📁 05-消息队列

| 主题 | 文件 | 内容概要 |
|------|------|---------|
| **异步与消息** | [async-mq.md](references/05-messaging/async-mq.md) | @Async 规范、RocketMQ 事务消息 |
| **RabbitMQ 规范** | [rabbitmq.md](references/05-messaging/rabbitmq.md) | RabbitMQ 配置、消息发送确认、无法路由消息处理、死信队列 |
| **RocketMQ 架构** | [rocketmq-architecture.md](references/05-messaging/rocketmq-architecture.md) | NameServer 原理、vs Zookeeper、事务消息实现、缺点与替代方案 |

### 📁 06-性能优化

| 主题 | 文件 | 内容概要 |
|------|------|---------|
| **性能优化** | [performance.md](references/06-performance/performance.md) | N+1 查询、深度分页、批量处理、大数导出 |
| **缓存规范** | [cache.md](references/06-performance/cache.md) | Key 命名、TTL 设置、缓存穿透、更新策略 |
| **Redis 集群** | [redis-cluster.md](references/06-performance/redis-cluster.md) | 集群原理、脑裂问题、分布式锁实现、Redisson 使用 |

### 📁 07-异常与日志

| 主题 | 文件 | 内容概要 |
|------|------|---------|
| **异常处理** | [exception.md](references/07-exception-logging/exception.md) | 业务异常、全局异常、异常堆栈处理 |
| **日志规范** | [logging.md](references/07-exception-logging/logging.md) | 日志格式、日志级别、必须打印节点、敏感信息脱敏 |

### 📁 08-代码质量

| 主题 | 文件 | 内容概要 |
|------|------|---------|
| **测试规范** | [testing.md](references/08-quality/testing.md) | Mock 测试、集成测试、命名规范、覆盖要求 |
| **安全规范** | [security.md](references/08-quality/security.md) | XSS 防护、SQL 注入、配置安全、敏感数据脱敏 |
| **设计模式** | [patterns.md](references/08-quality/patterns.md) | 策略模式、模板方法、责任链 |
| **接口文档** | [api-doc.md](references/08-quality/api-doc.md) | Javadoc 规范、Apifox 集成、字段注释 |

---

## 代码评审 Checklist

### 必查项

| 检查点 | 说明 | 检查方式 |
|--------|------|---------|
| **命名规范** | 类名、方法名、变量名是否符合规范 | 查看 [naming.md](references/01-basics/naming.md) |
| **日志规范** | 是否有业务标识，是否使用占位符 | 查看 [logging.md](references/07-exception-logging/logging.md) |
| **异常处理** | 是否使用统一异常，是否有兜底处理 | 查看 [exception.md](references/07-exception-logging/exception.md) |
| **参数校验** | Controller 是否有 @Valid，Service 是否有业务校验 | 查看 [controller.md](references/02-architecture/controller.md) |
| **SQL 安全** | 是否使用 #{}，是否有 SQL 注入风险 | 查看 [mapper.md](references/02-architecture/mapper.md) |
| **事务边界** | 多表操作是否有事务，是否混用数据源 | 查看 [transaction.md](references/03-database/transaction.md) |
| **空指针** | 是否有 NPE 风险，集合是否判空 | 查看 [coding-style.md](references/01-basics/coding-style.md) |
| **幂等性** | 写接口是否保证幂等 | 查看 [concurrency.md](references/04-concurrency/concurrency.md) |

### 性能检查

| 检查点 | 说明 | 检查方式 |
|--------|------|---------|
| **N+1 查询** | 是否有循环查询数据库 | 查看 [performance.md](references/06-performance/performance.md) |
| **深度分页** | 是否使用游标分页 | 查看 [performance.md](references/06-performance/performance.md) |
| **批量处理** | 超过 1000 条是否分批 | 查看 [performance.md](references/06-performance/performance.md) |
| **缓存策略** | 是否有缓存、TTL 是否合理 | 查看 [cache.md](references/06-performance/cache.md) |

---

## 快速修复常见问题

### 问题 1：事务不生效

```java
// ❌ 原因：同类内部调用，代理失效
@Service
public class OrderService {
    public void createOrder() {
        this.saveOrder();  // @Transactional 不生效
    }

    @Transactional
    public void saveOrder() { }
}

// ✅ 解决：注入自身
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderService self;  // Spring 4.3+ 支持自注入

    public void createOrder() {
        self.saveOrder();  // 通过代理调用
    }

    @Transactional
    public void saveOrder() { }
}
```

### 问题 2：N+1 查询

```java
// ❌ 错误：循环查询
for (Order order : orders) {
    User user = userMapper.selectById(order.getUserId());
}

// ✅ 正确：批量查询
Set<Long> userIds = orders.stream()
    .map(Order::getUserId)
    .collect(Collectors.toSet());
Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
    .collect(Collectors.toMap(User::getId, Function.identity()));
```

### 问题 3：缓存穿透

```java
// ❌ 问题：缓存不存在时直接查库，高并发穿透
String data = redis.get(key);
if (data == null) {
    data = db.select(id);  // 大量请求直达数据库
}

// ✅ 解决：空值也缓存
String data = redis.get(key);
if (data == null) {
    data = db.select(id);
    redis.setex(key, 300, data == null ? "" : data);  // 空值缓存 5 分钟
}
```

---

## 版本历史

| 版本 | 日期 | 变更 |
|------|------|------|
| 5.0 | 2026-01-28 | 重构为 Agent Skills 格式，拆分详细规范到 references |
| 4.1 | 2026-01-21 | 新增设计模式章节 |
| 4.0 | 2025-12-15 | 新增分布式事务规范 |
| 3.5 | 2025-11-01 | 新增性能优化章节 |
| 3.0 | 2025-09-10 | 新增缓存规范 |
| 2.0 | 2025-08-01 | 新增 Controller/Service/Mapper 分层规范 |
| 1.0 | 2025-06-01 | 初始版本 |
