# Java Spring 编码规范 - 详细规范索引

> 📚 所有规范文档已按主题分类组织，方便快速查找

---

## 📂 目录结构

```
references/
├── 📁 01-basics/              # 基础编码规范（7个文件）
├── 📁 02-architecture/         # 架构与分层（4个文件）
├── 📁 03-database/            # 数据库相关（6个文件）
├── 📁 04-concurrency/         # 并发与集合（4个文件）
├── 📁 05-messaging/           # 消息队列（3个文件）
├── 📁 06-performance/         # 性能优化（2个文件）
├── 📁 07-exception-logging/   # 异常与日志（2个文件）
└── 📁 08-quality/             # 代码质量（4个文件）
```

---

## 📁 01-基础编码规范

| 文件 | 说明 | 关键内容 |
|------|------|---------|
| [naming.md](01-basics/naming.md) | 命名规范 | 类名、变量名、表名、接口路径命名、禁止拼音中文 |
| [constants.md](01-basics/constants.md) | 常量定义 | 魔法值禁止、常量分类、枚举代替常量 |
| [code-format.md](01-basics/code-format.md) | 代码格式 | 大括号、空格、缩进、换行、注释格式 |
| [oop.md](01-basics/oop.md) | OOP 规约 | 静态访问、equals、包装类、构造方法 |
| [control-flow.md](01-basics/control-flow.md) | 控制语句 | if/switch/for 规范、卫语句、Optional |
| [comment.md](01-basics/comment.md) | 注释规约 | Javadoc、类注释、方法注释、TODO/FIXME |
| [coding-style.md](01-basics/coding-style.md) | 代码风格 | Import、依赖注入、对象转换、空安全 |

---

## 📁 02-架构与分层

| 文件 | 说明 | 关键内容 |
|------|------|---------|
| [project-structure.md](02-architecture/project-structure.md) | 工程结构 | 应用分层、目录规范、多模块、DO/DTO/VO 转换 || [spring-framework.md](02-architecture/spring-framework.md) | Spring 框架核心 | Spring/SpringBoot 启动流程、Bean 生命周期、事务传播行为 || [distributed-system.md](02-architecture/distributed-system.md) | 分布式系统 | 无状态化、分布式锁、分布式事务、Nacos、Sentinel、XXL-Job |
| [controller.md](02-architecture/controller.md) | Controller 层 | RESTful 规范、参数校验、返回类型 |
| [service.md](02-architecture/service.md) | Service 层 | 复杂度控制、卫语句、策略模式、事务边界 |
| [mapper.md](02-architecture/mapper.md) | Mapper 层 | Lambda API、XML 查询、SQL 安全 |

---

## 📁 03-数据库

| 文件 | 说明 | 关键内容 |
|------|------|---------|
| [database.md](03-database/database.md) | 数据库设计 | 表设计、字段规范、索引规范、建表模板 |
| [transaction.md](03-database/transaction.md) | 事务管理 | 事务规则、隔离级别、MVCC、Read View |
| [mysql-locks.md](03-database/mysql-locks.md) | MySQL 锁机制 | 乐观锁、悲观锁、行锁机制、死锁排查与解决 |
| [mysql-index.md](03-database/mysql-index.md) | MySQL 索引优化 | 索引设计、索引失效、查询优化、EXPLAIN |
| [mysql-btree.md](03-database/mysql-btree.md) | MySQL 索引原理 | B+树结构、聚簇索引、索引下推、存储量计算 |
| [mysql-transaction-impl.md](03-database/mysql-transaction-impl.md) | MySQL 事务实现 | redo/undo log、二阶段提交、锁机制、MVCC |
| [mysql-optimization.md](03-database/mysql-optimization.md) | MySQL 性能调优 | EXPLAIN 分析、count 对比、深度分页优化、SQL 调优 |
| [mysql-sql-execution.md](03-database/mysql-sql-execution.md) | SQL 执行过程 | 连接器、分析器、优化器、执行器、存储引擎 |

---

## 📁 04-并发与集合

| 文件 | 说明 | 关键内容 |
|------|------|---------|
| [concurrency.md](04-concurrency/concurrency.md) | 并发控制 | 乐观锁、分布式锁、幂等性设计 |
| [thread.md](04-concurrency/thread.md) | 线程编程 | 线程池、ThreadLocal、锁、volatile |
| [collection.md](04-concurrency/collection.md) | 集合处理 | ArrayList/HashMap、遍历删除、集合转Map |
| [hashmap-concurrent.md](04-concurrency/hashmap-concurrent.md) | HashMap 线程安全 | HashMap vs ConcurrentHashMap、JDK 1.7 vs 1.8 |

---

## 📁 05-消息队列

| 文件 | 说明 | 关键内容 |
|------|------|---------|
| [async-mq.md](05-messaging/async-mq.md) | 异步与消息 | @Async 规范、RocketMQ 事务消息 |
| [rabbitmq.md](05-messaging/rabbitmq.md) | RabbitMQ 规范 | 消息确认、无法路由消息、死信队列 |
| [rocketmq-architecture.md](05-messaging/rocketmq-architecture.md) | RocketMQ 架构 | NameServer、事务消息、替代方案 |

---

## 📁 06-性能优化

| 文件 | 说明 | 关键内容 |
|------|------|---------|
| [performance.md](06-performance/performance.md) | 性能优化 | N+1 查询、深度分页、批量处理、大数导出 |
| [cache.md](06-performance/cache.md) | 缓存规范 | Key 命名、TTL 设置、缓存穿透、更新策略 || [redis-cluster.md](06-performance/redis-cluster.md) | Redis 集群 | 集群原理、脑裂问题、分布式锁、Redisson 使用 |
---

## 📁 07-异常与日志

| 文件 | 说明 | 关键内容 |
|------|------|---------|
| [exception.md](07-exception-logging/exception.md) | 异常处理 | 业务异常、全局异常、异常堆栈处理 |
| [logging.md](07-exception-logging/logging.md) | 日志规范 | 日志格式、日志级别、敏感信息脱敏 |

---

## 📁 08-代码质量

| 文件 | 说明 | 关键内容 |
|------|------|---------|
| [testing.md](08-quality/testing.md) | 测试规范 | Mock 测试、集成测试、命名规范 |
| [security.md](08-quality/security.md) | 安全规范 | XSS 防护、SQL 注入、配置安全 |
| [patterns.md](08-quality/patterns.md) | 设计模式 | 策略模式、模板方法、责任链 |
| [api-doc.md](08-quality/api-doc.md) | 接口文档 | Javadoc 规范、Apifox 集成 |

---

## 🔍 快速查找

### 按问题场景查找

| 遇到的问题 | 查看文件 |
|-----------|---------|
| 不知道怎么命名 | [01-basics/naming.md](01-basics/naming.md) |
| Controller 怎么写 | [02-architecture/controller.md](02-architecture/controller.md) |
| 分布式部署问题 | [02-architecture/distributed-system.md](02-architecture/distributed-system.md) |
| SQL 慢查询优化 | [03-database/mysql-index.md](03-database/mysql-index.md) |
| 事务不生效 | [03-database/transaction.md](03-database/transaction.md) |
| 并发安全问题 | [04-concurrency/concurrency.md](04-concurrency/concurrency.md) |
| 消息队列选型 | [05-messaging/async-mq.md](05-messaging/async-mq.md) |
| 接口响应慢 | [06-performance/performance.md](06-performance/performance.md) |
| 异常怎么抛 | [07-exception-logging/exception.md](07-exception-logging/exception.md) |
| 日志怎么打 | [07-exception-logging/logging.md](07-exception-logging/logging.md) |
| 代码重复太多 | [08-quality/patterns.md](08-quality/patterns.md) |

### 按技术栈查找

| 技术 | 相关文件 |
|------|---------|
| **Spring Boot** | controller.md, service.md, exception.md, async-mq.md, distributed-system.md |
| **MyBatis Plus** | mapper.md, database.md, performance.md |
| **MySQL** | database.md, mysql-*.md, transaction.md |
| **Redis** | cache.md, concurrency.md, distributed-system.md |
| **消息队列** | async-mq.md, rabbitmq.md, rocketmq-architecture.md, distributed-system.md |
| **并发编程** | thread.md, concurrency.md, hashmap-concurrent.md |
| **分布式系统** | distributed-system.md, concurrency.md, async-mq.md, cache.md |
| **Nacos** | distributed-system.md |
| **Sentinel** | distributed-system.md |
| **XXL-Job** | distributed-system.md |

---

## 📖 使用建议

1. **新项目启动**：按顺序阅读 01-02-03 目录的所有文件
2. **日常开发**：遇到问题时根据"快速查找"表格定位
3. **代码审查**：重点查看 [SKILL.md](../SKILL.md) 中的"代码评审 Checklist"
4. **性能优化**：重点阅读 03-database 和 06-performance 目录
5. **架构设计**：重点阅读 02-architecture 目录

---

## 📌 推荐阅读顺序

### 新手必读（按优先级）
1. [naming.md](01-basics/naming.md) - 命名规范
2. [controller.md](02-architecture/controller.md) - Controller 层规范
3. [service.md](02-architecture/service.md) - Service 层规范
4. [exception.md](07-exception-logging/exception.md) - 异常处理
5. [logging.md](07-exception-logging/logging.md) - 日志规范

### 进阶阅读
1. [distributed-system.md](02-architecture/distributed-system.md) - 分布式系统开发
2. [mysql-index.md](03-database/mysql-index.md) - MySQL 索引优化
3. [transaction.md](03-database/transaction.md) - 事务管理
4. [concurrency.md](04-concurrency/concurrency.md) - 并发控制
5. [performance.md](06-performance/performance.md) - 性能优化
6. [cache.md](06-performance/cache.md) - 缓存规范

### 深入研究
1. [mysql-btree.md](03-database/mysql-btree.md) - B+树原理
2. [mysql-transaction-impl.md](03-database/mysql-transaction-impl.md) - 事务实现原理
3. [mysql-sql-execution.md](03-database/mysql-sql-execution.md) - SQL 执行流程
4. [hashmap-concurrent.md](04-concurrency/hashmap-concurrent.md) - HashMap 线程安全
5. [rocketmq-architecture.md](05-messaging/rocketmq-architecture.md) - RocketMQ 架构

---

**返回主文档**: [SKILL.md](../SKILL.md)
