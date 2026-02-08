# 分库分表

> 分类: 数据库 | 难度: ⭐⭐⭐⭐ | 频率: 高频

---

## 一、什么是分库分表

分库分表是解决**单库性能瓶颈**和**数据量过大**问题的数据库架构方案。

### 1.1 为什么需要分库分表

```
┌─────────────────────────────────────────────────────────┐
│                    单库瓶颈                              │
├─────────────────────────────────────────────────────────┤
│ 1. 数据量瓶颈: 单表超过 2000万行，查询性能下降           │
│ 2. 连接数瓶颈: MySQL 最大连接数有限(通常 2000-5000)      │
│ 3. 磁盘IO瓶颈: 单机磁盘读写能力有限                      │
│ 4. 内存瓶颈:   热数据无法完全放入内存                    │
└─────────────────────────────────────────────────────────┘
```

### 1.2 核心概念

```
┌─────────────────────────────────────────────────────────────────────┐
│                           分库分表示意图                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│   原始单库单表                     分库分表后                          │
│  ┌──────────┐                ┌──────────┐  ┌──────────┐              │
│  │   DB_0   │                │   DB_0   │  │   DB_1   │              │
│  ├──────────┤                ├──────────┤  ├──────────┤              │
│  │ order表  │    ────>       │ order_0  │  │ order_0  │              │
│  │(5000万行)│                │ order_1  │  │ order_1  │              │
│  └──────────┘                └──────────┘  └──────────┘              │
│                               每表约1250万行                          │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 二、分库分表类型(策略)

### 2.1 水平拆分 vs 垂直拆分

```
┌──────────────────────────────────────────────────────────────────────────┐
│                         拆分方式对比                                       │
├───────────────┬─────────────────────────┬────────────────────────────────┤
│               │      垂直拆分            │        水平拆分                 │
├───────────────┼─────────────────────────┼────────────────────────────────┤
│   拆分维度    │    按业务/字段拆分       │      按数据行拆分               │
│   表结构      │    各表结构不同          │      各表结构相同               │
│   解决问题    │    业务解耦、字段过多    │      数据量过大                 │
│   复杂度      │    相对简单              │      相对复杂                   │
└───────────────┴─────────────────────────┴────────────────────────────────┘
```

### 2.2 垂直拆分详解

#### 垂直分库

```
按业务拆分到不同数据库:

原始:                          垂直分库后:
┌─────────────┐               ┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│  电商系统   │               │  订单库     │  │  用户库     │  │  商品库     │
├─────────────┤    ────>      ├─────────────┤  ├─────────────┤  ├─────────────┤
│ - 用户表    │               │ - 订单表    │  │ - 用户表    │  │ - 商品表    │
│ - 订单表    │               │ - 订单明细  │  │ - 地址表    │  │ - 库存表    │
│ - 商品表    │               └─────────────┘  └─────────────┘  └─────────────┘
│ - 库存表    │
└─────────────┘
```

#### 垂直分表

```
按字段拆分(冷热分离):

原始用户表:                           垂直分表后:
┌─────────────────────────┐          ┌──────────────────┐  ┌──────────────────┐
│      user_info          │          │   user_base      │  │   user_detail    │
├─────────────────────────┤          ├──────────────────┤  ├──────────────────┤
│ id                      │   ───>   │ id               │  │ user_id (FK)     │
│ username  (热)          │          │ username         │  │ bio              │
│ password  (热)          │          │ password         │  │ avatar           │
│ phone     (热)          │          │ phone            │  │ hometown         │
│ bio       (冷)          │          └──────────────────┘  │ create_time      │
│ avatar    (冷/大)       │                                └──────────────────┘
│ hometown  (冷)          │
└─────────────────────────┘
```

### 2.3 水平拆分详解

#### 水平分库

```java
// 按用户ID分库 (2个库)
public String getDatabase(Long userId) {
    return "db_" + (userId % 2);  // db_0 或 db_1
}
```

#### 水平分表

```java
// 按用户ID分表 (4张表)
public String getTableName(Long userId) {
    return "order_" + (userId % 4);  // order_0, order_1, order_2, order_3
}
```

### 2.4 分片策略

```
┌────────────────────────────────────────────────────────────────────────────┐
│                          常用分片策略                                        │
├─────────────────┬──────────────────────────────────────────────────────────┤
│  Hash取模       │  hash(shardKey) % N                                      │
│                 │  优点: 数据分布均匀                                       │
│                 │  缺点: 扩容需要数据迁移                                   │
├─────────────────┼──────────────────────────────────────────────────────────┤
│  Range范围      │  按时间/ID区间划分                                        │
│                 │  优点: 扩容简单，范围查询友好                             │
│                 │  缺点: 数据分布不均(热点问题)                             │
├─────────────────┼──────────────────────────────────────────────────────────┤
│  一致性Hash     │  环形Hash空间，虚拟节点                                   │
│                 │  优点: 扩容只需迁移部分数据                               │
│                 │  缺点: 实现复杂                                           │
├─────────────────┼──────────────────────────────────────────────────────────┤
│  基因法         │  将分片信息编入主键                                       │
│                 │  优点: 无需路由查询                                       │
│                 │  缺点: 需定制ID生成                                       │
└─────────────────┴──────────────────────────────────────────────────────────┘
```

---

## 三、代码示例

### 3.1 ShardingSphere 配置示例

```yaml
# application.yml - ShardingSphere 分库分表配置
spring:
  shardingsphere:
    datasource:
      names: ds0,ds1
      ds0:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://localhost:3306/db_0
        username: root
        password: root
      ds1:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://localhost:3306/db_1
        username: root
        password: root
    
    rules:
      sharding:
        tables:
          t_order:
            actual-data-nodes: ds$->{0..1}.t_order_$->{0..3}
            # 分库策略
            database-strategy:
              standard:
                sharding-column: user_id
                sharding-algorithm-name: db-mod
            # 分表策略
            table-strategy:
              standard:
                sharding-column: order_id
                sharding-algorithm-name: table-mod
            # 主键生成
            key-generate-strategy:
              column: order_id
              key-generator-name: snowflake
        
        sharding-algorithms:
          db-mod:
            type: MOD
            props:
              sharding-count: 2
          table-mod:
            type: MOD
            props:
              sharding-count: 4
        
        key-generators:
          snowflake:
            type: SNOWFLAKE
```

### 3.2 自定义分片算法

```java
/**
 * 自定义Hash分片算法
 */
@Component
public class CustomShardingAlgorithm implements StandardShardingAlgorithm<Long> {
    
    @Override
    public String doSharding(Collection<String> availableTargetNames, 
                             PreciseShardingValue<Long> shardingValue) {
        Long userId = shardingValue.getValue();
        // 使用CRC32保证分布均匀
        CRC32 crc32 = new CRC32();
        crc32.update(userId.toString().getBytes());
        long hash = crc32.getValue();
        
        int index = (int) (Math.abs(hash) % availableTargetNames.size());
        return new ArrayList<>(availableTargetNames).get(index);
    }
    
    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames,
                                         RangeShardingValue<Long> shardingValue) {
        // 范围查询时返回所有分片
        return availableTargetNames;
    }
    
    @Override
    public Properties getProps() {
        return new Properties();
    }
    
    @Override
    public void init(Properties props) {
    }
}
```

### 3.3 分布式ID生成(雪花算法)

```java
/**
 * 雪花算法ID生成器
 * 结构: 1位符号位 + 41位时间戳 + 10位机器ID + 12位序列号
 */
public class SnowflakeIdGenerator {
    
    private final long workerId;
    private final long datacenterId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;
    
    // 各部分位数
    private final long workerIdBits = 5L;
    private final long datacenterIdBits = 5L;
    private final long sequenceBits = 12L;
    
    // 最大值
    private final long maxWorkerId = ~(-1L << workerIdBits);
    private final long maxDatacenterId = ~(-1L << datacenterIdBits);
    private final long sequenceMask = ~(-1L << sequenceBits);
    
    // 位移
    private final long workerIdShift = sequenceBits;
    private final long datacenterIdShift = sequenceBits + workerIdBits;
    private final long timestampShift = sequenceBits + workerIdBits + datacenterIdBits;
    
    private final long epoch = 1704067200000L; // 2024-01-01 00:00:00
    
    public SnowflakeIdGenerator(long workerId, long datacenterId) {
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException("Worker ID out of range");
        }
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException("Datacenter ID out of range");
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }
    
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards");
        }
        
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        
        lastTimestamp = timestamp;
        
        return ((timestamp - epoch) << timestampShift)
                | (datacenterId << datacenterIdShift)
                | (workerId << workerIdShift)
                | sequence;
    }
    
    private long waitNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
```

---

## 四、分库分表带来的问题

### 4.1 常见问题与解决方案

```
┌─────────────────┬───────────────────────────────────────────────────────┐
│     问题        │                    解决方案                            │
├─────────────────┼───────────────────────────────────────────────────────┤
│  跨库JOIN       │  1. 业务层代码组装                                    │
│                 │  2. 数据冗余                                          │
│                 │  3. 绑定表(ShardingSphere)                            │
├─────────────────┼───────────────────────────────────────────────────────┤
│  跨库事务       │  1. 分布式事务(Seata)                                 │
│                 │  2. 最终一致性(消息队列)                              │
│                 │  3. TCC模式                                           │
├─────────────────┼───────────────────────────────────────────────────────┤
│  全局排序分页   │  1. 业务限制(只能按分片键查询)                        │
│                 │  2. 归并排序(性能差)                                  │
│                 │  3. 二次查询(游标分页)                                │
├─────────────────┼───────────────────────────────────────────────────────┤
│  全局唯一ID     │  1. 雪花算法                                          │
│                 │  2. 号段模式(美团Leaf)                                │
│                 │  3. UUID(不推荐)                                      │
├─────────────────┼───────────────────────────────────────────────────────┤
│  扩容困难       │  1. 一致性Hash                                        │
│                 │  2. 提前规划分片数(2的幂次)                           │
│                 │  3. 双写迁移                                          │
└─────────────────┴───────────────────────────────────────────────────────┘
```

---

## 五、面试回答

### 30秒版本

> 分库分表是解决单库性能瓶颈的方案。**垂直拆分**按业务或字段拆分，**水平拆分**按数据行拆分。常用分片策略有 Hash 取模、Range 范围和一致性 Hash。主流中间件有 ShardingSphere 和 MyCat。

### 1分钟版本

> 分库分表用于解决单库数据量过大、连接数不足等问题。
>
> 拆分方式分两种：
> - **垂直拆分**：按业务拆分到不同库（如用户库、订单库），或按字段冷热分离
> - **水平拆分**：结构相同的表按规则分散到多库多表
>
> 分片策略主要有：
> - **Hash 取模**：分布均匀但扩容麻烦
> - **Range 范围**：扩容简单但易有热点
> - **一致性 Hash**：扩容只需迁移部分数据
>
> 带来的问题包括跨库 JOIN、分布式事务、全局 ID 等，需要配合 ShardingSphere、Seata 等中间件解决。

---

## 六、最佳实践

### ✅ 推荐做法

```java
// 1. 预估数据量，提前规划分片数(2的幂次方便扩容)
int shardCount = 16;  // 16 -> 32 -> 64 扩容方便

// 2. 选择合适的分片键(高频查询条件)
// 订单表用 user_id 分片，支持"查询用户订单"场景

// 3. 使用雪花算法或Leaf生成分布式ID
@Resource
private SnowflakeIdGenerator idGenerator;

public Order createOrder(Order order) {
    order.setOrderId(idGenerator.nextId());
    return orderMapper.insert(order);
}

// 4. 绑定表避免跨库JOIN
// 订单表和订单明细表使用相同分片键，保证在同一库
```

### ❌ 避免做法

```java
// ❌ 使用UUID作为主键(无序，B+树频繁分裂)
String id = UUID.randomUUID().toString();

// ❌ 随意选择分片键
// 错误: 按create_time分片，但查询都用user_id
sharding-column: create_time

// ❌ 忽视数据倾斜
// 如果90%订单属于10%用户，Hash分片仍会不均

// ❌ 分片数量太少
int shardCount = 2;  // 后期扩容代价大
```

---

## 七、主流分库分表中间件

| 中间件 | 类型 | 特点 |
|--------|------|------|
| **ShardingSphere** | JDBC代理 | Apache顶级项目，生态完善，社区活跃 |
| **MyCat** | 数据库代理 | 独立部署，支持多种数据库 |
| **TDDL** | JDBC代理 | 阿里内部使用，不开源 |
| **Vitess** | 数据库代理 | YouTube 开源，适合超大规模 |
