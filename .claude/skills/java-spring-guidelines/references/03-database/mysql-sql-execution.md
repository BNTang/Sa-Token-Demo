# SQL 执行过程详解

> Java/Spring Boot 编码规范 - 深入理解 SQL 语句在 MySQL 中的完整执行流程

---

## SQL 执行流程概览

```
┌─────────────────────────────────────────────────┐
│              MySQL 架构分层                      │
├─────────────────────────────────────────────────┤
│  客户端层（Client）                              │
│    ↓                                            │
│  连接器（Connector）           - 连接管理        │
│    ↓                                            │
│  查询缓存（Query Cache）       - MySQL 8.0 已移除│
│    ↓                                            │
│  分析器（Analyzer）            - 词法/语法分析   │
│    ↓                                            │
│  优化器（Optimizer）           - 执行计划优化    │
│    ↓                                            │
│  执行器（Executor）            - 调用存储引擎API │
│    ↓                                            │
│  存储引擎（Storage Engine）    - InnoDB/MyISAM  │
└─────────────────────────────────────────────────┘
```

---

## 完整执行流程

### 示例 SQL

```sql
SELECT id, username, age 
FROM user 
WHERE age > 25 
ORDER BY id 
LIMIT 10;
```

### 第1步：连接器（Connector）

**作用：**管理客户端连接、验证权限

```
客户端 → MySQL Server

1. 建立 TCP 连接（默认端口 3306）
2. 用户认证
   - 验证用户名和密码
   - 验证来源 IP（root@localhost, user@%）
3. 获取用户权限列表
   - 缓存权限信息（后续SQL执行时检查）
4. 连接成功，等待客户端发送SQL

连接状态：
- Sleep：空闲连接
- Query：正在执行SQL
- Locked：等待锁

查看连接：
SHOW PROCESSLIST;
```

**连接超时配置：**

```sql
-- 查看连接超时时间（默认 8 小时）
SHOW VARIABLES LIKE 'wait_timeout';

-- 查看当前连接数
SHOW STATUS LIKE 'Threads_connected';

-- 查看最大连接数
SHOW VARIABLES LIKE 'max_connections';
```

**Java 代码优化：**

```java
// ❌ 反例 - 频繁创建连接
public List<User> listUsers() {
    Connection conn = DriverManager.getConnection(url, user, password);
    // ... 查询
    conn.close();
    // 每次查询都建立新连接，开销大
}

// ✅ 正例 - 使用连接池
@Configuration
public class DataSourceConfig {
    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setMaximumPoolSize(20);  // 最大连接数
        config.setMinimumIdle(5);       // 最小空闲连接
        config.setMaxLifetime(1800000); // 连接最大存活时间 30分钟
        config.setConnectionTimeout(30000); // 连接超时 30秒
        return new HikariDataSource(config);
    }
}
```

---

### 第2步：查询缓存（Query Cache）

**⚠️ MySQL 8.0 已移除查询缓存**

> 原因：缓存失效频繁（任何表更新都会清空该表所有缓存），收益低。

```sql
-- MySQL 5.7 及之前

-- 第一次查询：未命中缓存
SELECT * FROM user WHERE id = 1;
-- 执行完整查询，结果缓存到 Query Cache

-- 第二次查询：命中缓存
SELECT * FROM user WHERE id = 1;
-- 直接返回缓存结果，跳过后续步骤

-- 表更新：清空缓存
UPDATE user SET age = 26 WHERE id = 1;
-- 清空 user 表的所有查询缓存
```

---

### 第3步：分析器（Analyzer）

**作用：**词法分析 + 语法分析

**3.1 词法分析（Lexical Analysis）**

```sql
SELECT id, username, age FROM user WHERE age > 25 ORDER BY id LIMIT 10;

词法分析：识别关键字和标识符
↓
SELECT    → 关键字
id        → 字段名
,         → 分隔符
username  → 字段名
age       → 字段名
FROM      → 关键字
user      → 表名
WHERE     → 关键字
age       → 字段名
>         → 比较运算符
25        → 常量
ORDER BY  → 关键字
id        → 字段名
LIMIT     → 关键字
10        → 常量
```

**3.2 语法分析（Syntax Analysis）**

```
语法分析：构建语法树（AST）

         SELECT
           ↓
    ┌──────┴──────┐
    │             │
 字段列表      FROM
  ↓             ↓
id,username  user表
  age          ↓
             WHERE
               ↓
            age > 25
               ↓
            ORDER BY
               ↓
              id
               ↓
             LIMIT 10

检查语法错误：
- 关键字拼写错误（SELCT → 报错）
- 表名/字段名不存在（user2 → 报错）
- 语法结构错误（SELECT FROM user → 报错）
```

**常见语法错误：**

```sql
-- ❌ 语法错误
SELCT * FROM user;  
-- ERROR: You have an error in your SQL syntax near 'SELCT'

-- ❌ 表不存在
SELECT * FROM user2;
-- ERROR: Table 'database.user2' doesn't exist

-- ❌ 字段不存在
SELECT id, username, email FROM user;
-- ERROR: Unknown column 'email' in 'field list'
```

---

### 第4步：优化器（Optimizer）

**作用：**生成最优执行计划

**4.1 选择索引**

```sql
-- 假设有索引：idx_age, idx_username

SELECT * FROM user WHERE age > 25 AND username = 'alice';

优化器分析：
1. 方案1：使用 idx_age
   - 扫描 age > 25 的记录（假设 1000 条）
   - 再过滤 username = 'alice'（假设 1 条）
   - 成本：扫描 1000 行

2. 方案2：使用 idx_username
   - 扫描 username = 'alice' 的记录（假设 1 条）
   - 再过滤 age > 25（假设 1 条）
   - 成本：扫描 1 行

优化器选择：idx_username（成本更低）
```

**4.2 决定JOIN顺序**

```sql
SELECT * FROM order o 
JOIN user u ON o.user_id = u.id 
WHERE o.status = 1;

优化器分析：
1. 小表驱动大表
   - user 表 1000 行
   - order 表 100万行
   - order WHERE status = 1 过滤后 1万行

2. JOIN 顺序
   - 方案1：order JOIN user（大表驱动小表，差）
   - 方案2：user JOIN order（小表驱动大表，优）

优化器选择：user JOIN order
```

**4.3 优化子查询**

```sql
-- 原始SQL（子查询）
SELECT * FROM user 
WHERE id IN (SELECT user_id FROM order WHERE status = 1);

-- 优化器改写为 JOIN
SELECT u.* FROM user u 
JOIN order o ON u.id = o.user_id 
WHERE o.status = 1;
```

**查看执行计划：**

```sql
EXPLAIN SELECT * FROM user WHERE age > 25;

+----+-------------+-------+-------+---------------+---------+---------+------+------+-------------+
| id | select_type | table | type  | possible_keys | key     | key_len | ref  | rows | Extra       |
+----+-------------+-------+-------+---------------+---------+---------+------+------+-------------+
|  1 | SIMPLE      | user  | range | idx_age       | idx_age | 4       | NULL | 100  | Using where |
+----+-------------+-------+-------+---------------+---------+---------+------+------+-------------+

关键字段：
- type: range（索引范围扫描）
- key: idx_age（使用的索引）
- rows: 100（预估扫描行数）
```

---

### 第5步：执行器（Executor）

**作用：**调用存储引擎 API，执行查询

**5.1 权限检查**

```sql
SELECT * FROM user WHERE age > 25;

执行器检查：
1. 用户是否有 user 表的 SELECT 权限
2. 用户是否有 age 字段的访问权限

权限不足：
ERROR: SELECT command denied to user 'guest'@'localhost' for table 'user'
```

**5.2 调用存储引擎**

```
执行器 → InnoDB 存储引擎

1. 打开表（open table）
2. 调用引擎接口读取第一行
   - 调用 InnoDB handler_read_first()
   - 判断 age > 25 是否满足
   - 满足则加入结果集，不满足则丢弃

3. 调用引擎接口读取下一行
   - 调用 InnoDB handler_read_next()
   - 判断 age > 25 是否满足
   - 重复执行，直到读取完所有行

4. 返回结果集给客户端
```

**全表扫描 vs 索引扫描：**

```sql
-- 全表扫描
SELECT * FROM user WHERE age > 25;
执行器：调用 handler_read_first() → handler_read_next() → ...
        逐行读取，逐行判断

-- 索引扫描
SELECT * FROM user WHERE age > 25;
执行器：调用 index_read() → index_next() → ...
        通过索引快速定位，只读取满足条件的行
```

---

### 第6步：存储引擎（Storage Engine）

**作用：**读写数据

**6.1 InnoDB 执行流程**

```
SELECT * FROM user WHERE id = 1;

InnoDB 执行：
1. 查询 Buffer Pool（内存缓存）
   - 命中：直接返回
   - 未命中：从磁盘加载

2. 从磁盘加载数据页
   - 根据 B+树索引定位数据页
   - 读取数据页到 Buffer Pool

3. 返回数据给执行器
```

**UPDATE 执行流程：**

```
UPDATE user SET age = 30 WHERE id = 1;

InnoDB 执行：
1. 查询数据（同 SELECT）
2. 加行锁（FOR UPDATE）
3. 写入 undo log（记录旧值 age=25）
4. 修改内存中的数据（Buffer Pool）
5. 写入 redo log buffer
6. 写入 binlog buffer
7. 事务提交时刷盘（redo log + binlog）
```

---

## 不同SQL类型的执行流程

### SELECT 查询流程

```sql
SELECT id, username FROM user WHERE age > 25 ORDER BY id LIMIT 10;

完整流程：
1. 连接器：验证权限
2. 分析器：词法/语法分析
3. 优化器：选择索引、生成执行计划
4. 执行器：调用存储引擎读取数据
5. 存储引擎：通过索引扫描 + 回表
6. 执行器：排序（ORDER BY）+ 限制结果（LIMIT）
7. 返回结果给客户端
```

### UPDATE 更新流程

```sql
UPDATE user SET age = 30 WHERE id = 1;

完整流程：
1. 连接器：验证权限
2. 分析器：词法/语法分析
3. 优化器：选择索引
4. 执行器：调用存储引擎查询数据
5. 存储引擎：加行锁（X锁）
6. 存储引擎：写入 undo log
7. 存储引擎：修改 Buffer Pool
8. 存储引擎：写入 redo log buffer
9. Server 层：写入 binlog buffer
10. 事务提交：redo log + binlog 两阶段提交
11. 返回影响行数给客户端
```

### INSERT 插入流程

```sql
INSERT INTO user (id, username, age) VALUES (1, 'alice', 25);

完整流程：
1-4. 同 UPDATE
5. 存储引擎：检查唯一索引冲突
6. 存储引擎：写入 undo log
7. 存储引擎：插入数据到 Buffer Pool
8. 存储引擎：更新索引 B+树
9-11. 同 UPDATE
```

---

## 性能优化要点

### 连接层优化

```java
// 使用连接池
HikariCP: 最大连接数 20-50
保持连接复用，避免频繁建立连接
```

### 分析器优化

```sql
-- 避免语法错误，使用参数化查询
PreparedStatement（预编译，复用执行计划）
```

### 优化器优化

```sql
-- 提供准确的索引
CREATE INDEX idx_age ON user(age);

-- 避免索引失效
WHERE age > 25  ✅
WHERE age + 1 > 26  ❌（索引失效）

-- 查看执行计划
EXPLAIN SELECT * FROM user WHERE age > 25;
```

### 执行器优化

```sql
-- 减少扫描行数
LIMIT 分页优化（避免深分页）
使用覆盖索引（避免回表）
```

### 存储引擎优化

```sql
-- 批量操作
INSERT INTO user VALUES (...), (...), (...);  -- 批量插入

-- 事务控制
合理拆分长事务，减少锁持有时间
```

---

## 检查清单

| 检查项 | 说明 | 优先级 |
|--------|------|--------|
| ✅ 使用连接池 | 避免频繁建立连接 | 🔴 必须 |
| ✅ 参数化查询 | 避免SQL注入、复用执行计划 | 🔴 必须 |
| ✅ 建立合理索引 | 提升查询性能 | 🔴 必须 |
| ✅ EXPLAIN 分析 | 上线前检查执行计划 | 🔴 必须 |
| ✅ 避免全表扫描 | 查询必须走索引 | 🔴 必须 |
| ✅ 批量操作 | 减少网络开销 | 🟡 推荐 |

---

## 参考资料

- MySQL 官方文档 - SQL Statement Execution
- 《MySQL 技术内幕》
- 《高性能 MySQL》
