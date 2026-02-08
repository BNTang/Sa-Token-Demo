# MySQL 索引深度原理

> Java/Spring Boot 编码规范 - MySQL 索引底层原理与实现机制
> 深入理解 B+树、聚簇索引、回表、索引下推等核心概念

---

## B+树索引结构

### 为什么 MySQL 选择 B+树？

**对比其他数据结构：**

| 数据结构 | 时间复杂度 | 问题 |
|---------|----------|------|
| **数组** | O(n) | 查询慢 |
| **二叉搜索树** | O(log n) ~ O(n) | 可能退化成链表 |
| **AVL 树** | O(log n) | 高度太高，IO 次数多 |
| **红黑树** | O(log n) | 高度太高，IO 次数多 |
| **B 树** | O(log n) | 非叶子节点存数据，浪费空间 |
| **B+树** | O(log n) | ✅ 最优选择 |

**B+树优势：**

1. **IO 次数少**：多叉树，高度低（千万级数据仅 3-4 层）
2. **磁盘预读友好**：非叶子节点只存索引，一次加载更多索引
3. **范围查询高效**：叶子节点链表连接，支持顺序遍历
4. **稳定性能**：所有数据在叶子节点，查询路径长度一致

### B+树结构示意图

```
                    [15, 30]              ← 根节点（索引）
                   /    |    \
                 /      |      \
            [5,10]  [15,20,25] [30,40,50] ← 非叶子节点（索引）
             / | \    /  |  \     / | \
           /   |   \  /   |   \   /  |  \
      [1,2,3] [5,7] [10,12] ... [50,52] ← 叶子节点（数据+索引）
         ↓      ↓      ↓          ↓
      完整行  完整行  完整行    完整行
         ←-------------→-------------→   双向链表
```

**关键特点：**

- **非叶子节点**：只存储索引（主键值），不存数据
- **叶子节点**：存储索引 + 完整行数据（聚簇索引）或主键值（非聚簇索引）
- **链表结构**：叶子节点通过双向链表连接，支持范围查询

### B+树查询过程详解

**示例：查询 `id = 25` 的数据**

```sql
SELECT * FROM user WHERE id = 25;
```

**查询步骤：**

```
1. 磁盘 IO 1：读取根节点 [15, 30]
   → 25 在 15 和 30 之间，走中间分支

2. 磁盘 IO 2：读取非叶子节点 [15, 20, 25]
   → 25 在 20 和 25 之间，走最右分支

3. 磁盘 IO 3：读取叶子节点 [25, 26, 27]
   → 找到 id=25 的完整行数据，返回结果

总共 3 次磁盘 IO，查询完成
```

**千万级数据查询：**

```
假设：
- 每个节点 16KB（InnoDB 页大小）
- 主键 bigint 8字节，指针 6字节
- 非叶子节点可存：16KB / 14B ≈ 1170 个索引

层数计算：
- 第 1 层（根节点）：1170 个分支
- 第 2 层：1170 × 1170 = 136万个分支
- 第 3 层：1170 × 1170 × 1170 = 16亿条数据

结论：千万级数据只需要 3 次 IO！
```

### MySQL 三层 B+树能存多少数据？

**详细计算过程：**

**假设条件：**
- InnoDB 页大小：16KB = 16384 字节
- 主键类型：bigint = 8 字节
- 指针大小：6 字节
- 一行数据大小：1KB = 1024 字节（假设）

**非叶子节点：**
```
每个索引项大小 = 主键(8B) + 指针(6B) = 14B
每页可存索引数 = 16384B / 14B ≈ 1170 个
```

**叶子节点：**
```
每行数据大小 = 1KB
每页可存数据行 = 16KB / 1KB = 16 行
```

**三层B+树计算：**
```
第 1 层（根节点）：
  1 个节点，1170 个指针

第 2 层（非叶子节点）：
  1170 个节点，每个节点 1170 个指针
  总指针数：1170 × 1170 = 1,368,900

第 3 层（叶子节点）：
  1,368,900 个节点，每个节点 16 行数据
  总数据行：1,368,900 × 16 = 21,902,400 行

答案：三层 B+树约能存储 **2190 万行数据**
```

**不同数据大小的存储量：**

| 每行数据大小 | 每页存储行数 | 三层B+树存储量 |
|-------------|-------------|---------------|
| 0.5KB | 32 行 | 4380 万行 |
| 1KB | 16 行 | 2190 万行 |
| 2KB | 8 行 | 1095 万行 |

**结论：**
- 数据行越小，存储量越大
- 三层B+树足以支撑千万级甚至亿级数据
- 查询只需 3 次磁盘 IO，性能极高

---

## 聚簇索引 vs 非聚簇索引

### InnoDB 聚簇索引（主键索引）

**【强制】InnoDB 必须有主键，主键索引是聚簇索引。**

```
聚簇索引（Clustered Index）
- 叶子节点存储：完整行数据
- 一个表只有一个聚簇索引（主键索引）
- 数据按主键顺序物理存储
```

**示例：主键索引结构**

```
表结构：user (id, name, age, city)

主键索引（id）B+树：
                  [50]
                 /    \
               /        \
          [10,20,30]  [60,70,80]
           /  |  \      /  |  \
         /    |    \  /    |    \
    [1,2,3] [10,12] ... [60,65] [70,75]
      ↓       ↓           ↓       ↓
   完整行   完整行      完整行   完整行
   (id=1,   (id=10,    (id=60,  (id=70,
    name,    name,      name,    name,
    age,     age,       age,     age,
    city)    city)      city)    city)
```

### InnoDB 非聚簇索引（二级索引）

**非聚簇索引（Secondary Index）：**

```
非聚簇索引（Non-Clustered Index）
- 叶子节点存储：索引字段 + 主键值
- 一个表可以有多个非聚簇索引
- 查询数据需要"回表"（先查二级索引，再查主键索引）
```

**示例：普通索引结构**

```
普通索引（name）B+树：
                ['Mike']
                /      \
              /          \
      ['Alice','Bob']  ['Tom','Zack']
        /      \          /      \
      /          \      /          \
  ['Alice']  ['Bob']  ['Tom']  ['Zack']
    ↓          ↓        ↓        ↓
   id=2       id=5     id=8     id=10  ← 只存主键值！
```

---

## 回表查询

### 什么是回表？

**回表（Table Lookup）：**

> 通过非聚簇索引查询数据时，先从二级索引 B+树找到主键值，再从主键索引 B+树查询完整数据的过程。

**示例：回表查询过程**

```sql
-- 假设 name 字段有普通索引
SELECT id, name, age, city FROM user WHERE name = 'Alice';
```

**查询步骤：**

```
1. 查询 name 索引 B+树
   → 找到 name='Alice' 的叶子节点
   → 获取主键值 id=2

2. 回表：查询主键索引 B+树（聚簇索引）
   → 根据 id=2 查找
   → 获取完整行数据 (id=2, name='Alice', age=25, city='Beijing')

3. 返回结果

总共需要查询 2 棵 B+树！
```

### 如何避免回表？

**【推荐】使用覆盖索引（Covering Index）。**

```sql
-- ❌ 反例 - 需要回表（查询 age, city 字段）
SELECT id, name, age, city FROM user WHERE name = 'Alice';

-- ✅ 正例 - 覆盖索引，无需回表
SELECT id, name FROM user WHERE name = 'Alice';

-- ✅ 正例 - 创建联合索引
CREATE INDEX idx_name_age_city ON user(name, age, city);
-- 查询字段都在索引中，无需回表
SELECT id, name, age, city FROM user WHERE name = 'Alice';
```

**覆盖索引示意图：**

```
联合索引 idx_name_age_city (name, age, city) B+树：
                ['Mike']
                /      \
              /          \
  ['Alice',25,'BJ']  ['Tom',30,'SH']
        ↓                  ↓
    id=2, Alice, 25, BJ   id=8, Tom, 30, SH

查询 SELECT id, name, age, city WHERE name='Alice'
→ 直接从索引获取所有字段，无需回表！
```

---

## 索引下推（Index Condition Pushdown, ICP）

### 什么是索引下推？

**索引下推（MySQL 5.6+）：**

> 将部分 WHERE 条件的过滤操作下推到存储引擎层，减少回表次数。

**示例：有无索引下推的区别**

```sql
-- 假设有联合索引 idx_name_age (name, age)
SELECT * FROM user WHERE name LIKE 'A%' AND age = 25;
```

**无索引下推（MySQL 5.5）：**

```
1. 通过 name 索引找到所有 name LIKE 'A%' 的记录（如 100 条）
2. 对这 100 条记录都进行回表，获取完整数据
3. 在 Server 层过滤 age=25 的数据（如 10 条）
4. 返回结果

问题：回表 100 次，但只有 10 条符合条件
```

**有索引下推（MySQL 5.6+）：**

```
1. 通过 name 索引找到所有 name LIKE 'A%' 的记录
2. 在存储引擎层，利用索引中的 age 字段过滤 age=25（剩 10 条）
3. 只对这 10 条记录回表
4. 返回结果

优化：回表次数从 100 次降到 10 次！
```

### 索引下推生效条件

**【推荐】利用联合索引的多个字段进行过滤。**

```sql
-- ✅ 索引下推生效
-- 联合索引 idx_name_age (name, age)
SELECT * FROM user WHERE name LIKE 'A%' AND age > 20;
-- age 条件在存储引擎层过滤

-- ✅ 索引下推生效
-- 联合索引 idx_city_age (city, age)
SELECT * FROM user WHERE city = 'Beijing' AND age BETWEEN 20 AND 30;

-- ❌ 索引下推不生效（age 没有索引）
SELECT * FROM user WHERE name = 'Alice' AND age > 20;
```

### 如何查看是否使用索引下推？

```sql
EXPLAIN SELECT * FROM user WHERE name LIKE 'A%' AND age = 25;

-- 查看 Extra 列：
-- Using index condition  ← 使用了索引下推
```

---

## 索引数量是否越多越好？

### 索引的代价

**【推荐】索引不是越多越好，需要权衡查询和写入性能。**

| 影响 | 说明 |
|------|------|
| ✅ 提升查询性能 | 加速 WHERE、ORDER BY、JOIN |
| ❌ 降低写入性能 | INSERT/UPDATE/DELETE 需要维护索引 B+树 |
| ❌ 占用磁盘空间 | 每个索引都是一棵 B+树 |
| ❌ 增加优化器负担 | 索引太多，选择索引的成本增加 |

### 索引数量建议

```sql
-- ❌ 反例 - 索引过多
CREATE TABLE `user` (
    `id` bigint PRIMARY KEY,
    `username` varchar(50),
    `mobile` varchar(20),
    `email` varchar(100),
    `age` int,
    `city` varchar(50),
    `status` tinyint,
    INDEX idx_username (username),
    INDEX idx_mobile (mobile),
    INDEX idx_email (email),
    INDEX idx_age (age),
    INDEX idx_city (city),
    INDEX idx_status (status),
    INDEX idx_username_age (username, age),
    INDEX idx_city_age (city, age),
    INDEX idx_status_age (status, age)
    -- 9 个索引！写入性能差
);

-- ✅ 正例 - 合理索引
CREATE TABLE `user` (
    `id` bigint PRIMARY KEY,
    `username` varchar(50),
    `mobile` varchar(20),
    `email` varchar(100),
    `age` int,
    `city` varchar(50),
    `status` tinyint,
    UNIQUE INDEX uk_username (username),
    UNIQUE INDEX uk_mobile (mobile),
    INDEX idx_city_status_age (city, status, age)  -- 联合索引复用
    -- 4 个索引，合理
);
```

**建议：**

- 单表索引数量控制在 **5 个以内**
- 优先使用联合索引，利用最左前缀原则
- 低频查询字段不建索引
- 区分度低的字段（如性别）不建索引

---

## MyISAM vs InnoDB 索引区别

| 对比项 | InnoDB | MyISAM |
|--------|--------|--------|
| **聚簇索引** | ✅ 主键索引是聚簇索引 | ❌ 无聚簇索引 |
| **主键索引** | 叶子节点存完整行数据 | 叶子节点存数据物理地址 |
| **二级索引** | 叶子节点存主键值 | 叶子节点存数据物理地址 |
| **回表** | ✅ 需要回表 | ❌ 不需要回表（直接访问地址）|
| **数据存储** | 索引和数据在一起 | 索引和数据分离 |
| **事务支持** | ✅ 支持 | ❌ 不支持 |
| **锁粒度** | 行级锁 | 表级锁 |

**MyISAM 索引结构：**

```
MyISAM 主键索引：
        [50]
       /    \
  [10,20]  [60,70]
    ↓        ↓
  0x001    0x005  ← 存储数据物理地址，直接访问，无需回表

InnoDB 主键索引：
        [50]
       /    \
  [10,20]  [60,70]
    ↓        ↓
  完整行   完整行  ← 存储完整数据
```

---

## 实战问题解答

### Q1: 为什么主键推荐自增？

```sql
-- ❌ 反例 - UUID 主键
CREATE TABLE `order` (
    `id` varchar(36) PRIMARY KEY,  -- UUID
    ...
);

问题：
1. UUID 是无序的，插入时导致 B+树页分裂
2. 页分裂需要移动数据，性能差
3. UUID 36 字节，占用空间大

-- ✅ 正例 - 自增主键
CREATE TABLE `order` (
    `id` bigint PRIMARY KEY AUTO_INCREMENT,
    ...
);

优点：
1. 顺序插入，B+树只在最右侧插入，无页分裂
2. bigint 8 字节，空间小
3. 性能高
```

### Q2: 联合索引字段顺序如何确定？

```sql
-- 原则：区分度高的字段在前

-- ❌ 反例
CREATE INDEX idx_status_user ON order(status, user_id);
-- status 只有 0/1/2 三个值，区分度低

-- ✅ 正例
CREATE INDEX idx_user_status ON order(user_id, status);
-- user_id 区分度高，放在前面
```

### Q3: 什么时候会发生回表？

```sql
-- ✅ 不会回表（覆盖索引）
SELECT id, name FROM user WHERE name = 'Alice';

-- ❌ 会回表（查询非索引字段）
SELECT id, name, age FROM user WHERE name = 'Alice';

-- ✅ 不会回表（主键查询）
SELECT * FROM user WHERE id = 1;

-- ❌ 会回表（非聚簇索引 + 查询其他字段）
SELECT * FROM user WHERE name = 'Alice';
```

---

## 索引监控与优化

### 查看索引使用情况

```sql
-- 查看表索引
SHOW INDEX FROM user;

-- 查看索引统计信息
SELECT * FROM information_schema.STATISTICS 
WHERE table_schema = 'your_db' AND table_name = 'user';

-- 查看未使用的索引（MySQL 8.0+）
SELECT * FROM sys.schema_unused_indexes;

-- 查看索引选择性（区分度）
SELECT 
    COUNT(DISTINCT column_name) / COUNT(*) AS selectivity
FROM table_name;
-- selectivity > 0.1 建议建索引
```

### EXPLAIN 关键字段解读

```sql
EXPLAIN SELECT * FROM user WHERE name = 'Alice';
```

| 字段 | 重点值 | 说明 |
|------|--------|------|
| **type** | system > const > eq_ref > ref > range > index > ALL | 访问类型 |
| **key** | idx_name | 实际使用的索引 |
| **key_len** | 152 | 索引长度（越小越好）|
| **rows** | 1 | 扫描行数（越小越好）|
| **Extra** | Using index | 使用覆盖索引 |
| **Extra** | Using index condition | 使用索引下推 |
| **Extra** | Using filesort | ❌ 使用文件排序（需优化）|
| **Extra** | Using temporary | ❌ 使用临时表（需优化）|

---

## 最佳实践检查清单

| 检查项 | 说明 | 优先级 |
|--------|------|--------|
| ✅ 理解 B+树结构 | 知道为什么查询快 | 🟡 推荐 |
| ✅ 区分聚簇索引和非聚簇索引 | InnoDB 特性 | 🔴 必须 |
| ✅ 理解回表过程 | 知道性能损耗 | 🔴 必须 |
| ✅ 使用覆盖索引 | 避免回表 | 🟡 推荐 |
| ✅ 理解索引下推 | MySQL 5.6+ 优化 | 🟡 推荐 |
| ✅ 主键使用自增 | 避免页分裂 | 🔴 必须 |
| ✅ 控制索引数量 | 单表 ≤ 5 个 | 🟡 推荐 |
| ✅ EXPLAIN 分析查询 | 上线前检查 | 🔴 必须 |

---

## 参考资料

- MySQL 官方文档 - InnoDB Storage Engine
- 《高性能 MySQL》
- 《MySQL 技术内幕：InnoDB 存储引擎》
