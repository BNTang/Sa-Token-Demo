# 数据库设计规范

> Java/Spring Boot 编码规范 - 数据库设计

---

## 基础字段要求

实体类继承 `BaseDO` 时，建表语句**必须包含**以下基础字段：

| 字段名 | 类型 | 说明 | 必须 |
|--------|------|------|------|
| `id` | `bigint` | 主键，自增 | ✅ |
| `create_time` | `datetime` | 创建时间 | ✅ |
| `update_time` | `datetime` | 更新时间 | ✅ |
| `creator` | `varchar(64)` | 创建者 | ✅ |
| `updater` | `varchar(64)` | 更新者 | ✅ |
| `deleted` | `bit(1)` | 逻辑删除标记 | ✅ |

---

## 建表语句模板

```sql
CREATE TABLE `table_name` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',

    -- 业务字段示例
    `name` varchar(100) NOT NULL COMMENT '名称',
    `code` varchar(50) NOT NULL COMMENT '编码',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',

    -- 基础字段（必须）
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='表注释';
```

---

## 字段类型规范

| 类型 | MySQL 类型 | Java 类型 | 说明 |
|------|-----------|----------|------|
| 主键 | `bigint` | `Long` | 自增主键 |
| 金额 | `decimal(19,2)` | `BigDecimal` | 禁止使用 float/double |
| 枚举/状态 | `tinyint` | `Integer` | 0-127 |
| 日期 | `datetime` | `LocalDateTime` | 精确到秒 |
| 日期 | `date` | `LocalDate` | 精确到天 |
| 文本短 | `varchar(n)` | `String` | n ≤ 500 |
| 文本长 | `text` | `String` | > 500 字符 |
| 布尔 | `bit(1)` | `Boolean` | true/false |

### 字段长度规范

| 字段类型 | 推荐长度 | 说明 |
|---------|---------|------|
| 用户名 | `varchar(50)` | 用户名 |
| 真实姓名 | `varchar(30)` | 真实姓名 |
| 手机号 | `varchar(20)` | 考虑区号 |
| 邮箱 | `varchar(100)` | 邮箱地址 |
| 身份证 | `varchar(18)` | 大陆身份证 |
| 编码 | `varchar(50)` | 业务编码 |
| 名称 | `varchar(100)` | 通用名称 |
| 备注 | `varchar(500)` | 短备注 |
| 描述 | `text` | 长描述 |
| URL | `varchar(500)` | 链接地址 |

---

## 字段命名规范

### 命名规则

| 规则 | 示例 | 说明 |
|------|------|------|
| 全小写下划线 | `user_name`, `create_time` | 表名、字段名 |
| 布尔字段 | `is_active`, `deleted` | `is_` 前缀或动词 |
| 时间字段 | `create_time`, `effect_time` | `_time` 后缀 |
| 日期字段 | `birth_date`, `order_date` | `_date` 后缀 |
| 状态字段 | `order_status`, `approval_status` | `_status` 后缀 |
| 金额字段 | `total_amount`, `pay_amount` | `_amount` 后缀 |
| 数量字段 | `order_count`, `stock` | `_count` 后缀 |
| 外键字段 | `user_id`, `product_id` | `关联表_id` 格式 |

### 禁用命名

| ❌ 禁止 | ✅ 正确 | 原因 |
|--------|--------|------|
| `userName` | `user_name` | 应使用下划线 |
| `createtime` | `create_time` | 应使用下划线分隔 |
| `status` (布尔) | `is_active` | 布尔应有 `is_` 前缀 |
| `money` | `amount` | 应使用 `amount` |
| `count` | `order_count` | 应明确是哪种计数 |

---

## 索引规范

### 索引命名规则

| 类型 | 命名规则 | 示例 |
|------|----------|------|
| 主键 | `PRIMARY KEY` | `PRIMARY KEY (id)` |
| 唯一索引 | `uk_表名_字段` | `uk_order_order_no` |
| 普通索引 | `idx_表名_字段` | `idx_order_user_id` |
| 联合索引 | `idx_表名_字段1_字段2` | `idx_order_user_id_status` |

### 索引创建示例

```sql
-- 主键（自动创建）
PRIMARY KEY (`id`)

-- 唯一索引
CREATE UNIQUE INDEX `uk_order_order_no` ON `order` (`order_no`);
CREATE UNIQUE INDEX `uk_user_mobile` ON `user` (`mobile`);

-- 单列索引
CREATE INDEX `idx_order_user_id` ON `order` (`user_id`);
CREATE INDEX `idx_order_status` ON `order` (`status`);
CREATE INDEX `idx_product_create_time` ON `product` (`create_time`);

-- 联合索引（注意字段顺序）
CREATE INDEX `idx_order_user_status` ON `order` (`user_id`, `status`);
CREATE INDEX `idx_order_user_time` ON `order` (`user_id`, `create_time`);
```

### 索引设计原则

1. **选择性高的字段优先建索引**
   - 如：手机号、订单号、用户 ID

2. **联合索引遵循最左前缀原则**
   - `idx(a, b, c)` 支持：`a`, `a,b`, `a,b,c`
   - 不支持：`b`, `c`, `b,c`

3. **区分度低的字段不建单独索引**
   - 如：性别、状态（可与其他字段组合）

4. **频繁排序的字段考虑索引**
   - 如：`create_time DESC`

5. **避免冗余索引**
   - 有 `idx(a,b)` 就不需要 `idx(a)`

```sql
-- ❌ 冗余：idx(a) 被 idx(a,b) 覆盖
CREATE INDEX `idx_a` ON `table` (`a`);
CREATE INDEX `idx_a_b` ON `table` (`a`, `b`);

-- ✅ 正确：只需 idx(a,b)
CREATE INDEX `idx_a_b` ON `table` (`a`, `b`);
```

---

## 实体类映射

### 继承 BaseDO

```java
/**
 * 商品实体
 */
@Data
@TableName("product")
@EqualsAndHashCode(callSuper = true)
public class Product extends BaseDO {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 商品编码
     */
    @TableField("code")
    private String code;

    /**
     * 商品名称
     */
    @TableField("name")
    private String name;

    /**
     * 价格
     */
    @TableField("price")
    private BigDecimal price;

    /**
     * 状态：0-下架，1-上架
     */
    @TableField("status")
    private Integer status;

    // BaseDO 已包含：
    // private String creator;
    // private LocalDateTime createTime;
    // private String updater;
    // private LocalDateTime updateTime;
    // private Boolean deleted;
}
```

### 字段注解

```java
@Data
@TableName("product")
public class Product extends BaseDO {

    // 主键自增
    @TableId(type = IdType.AUTO)
    private Long id;

    // 主键手动赋值
    @TableId(type = IdType.INPUT)
    private String orderNo;

    // 字段不存在于数据库
    @TableField(exist = false)
    private String tempField;

    // 字段加密存储
    @TableField(value = "mobile", typeHandler = EncryptTypeHandler.class)
    private String mobile;

    // 逻辑删除字段
    @TableLogic
    @TableField("deleted")
    private Boolean deleted;

    // 自动填充
    @TableField(fill = FieldFill.INSERT)
    private String creator;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updater;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

---

## 常见建表示例

### 用户表

```sql
CREATE TABLE `user` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` varchar(50) NOT NULL COMMENT '用户名',
    `password` varchar(100) NOT NULL COMMENT '密码（加密）',
    `real_name` varchar(30) DEFAULT NULL COMMENT '真实姓名',
    `mobile` varchar(20) DEFAULT NULL COMMENT '手机号',
    `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
    `avatar` varchar(500) DEFAULT NULL COMMENT '头像URL',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_mobile` (`mobile`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
```

### 订单表

```sql
CREATE TABLE `order` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `order_no` varchar(32) NOT NULL COMMENT '订单号',
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `total_amount` decimal(19,2) NOT NULL COMMENT '订单总金额',
    `pay_amount` decimal(19,2) DEFAULT NULL COMMENT '实付金额',
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0-待支付，1-已支付，2-已发货，3-已完成，4-已取消',
    `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';
```

### 商品表

```sql
CREATE TABLE `product` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    `code` varchar(50) NOT NULL COMMENT '商品编码',
    `name` varchar(100) NOT NULL COMMENT '商品名称',
    `category_id` bigint NOT NULL COMMENT '分类ID',
    `price` decimal(19,2) NOT NULL COMMENT '价格',
    `stock` int NOT NULL DEFAULT 0 COMMENT '库存',
    `sales` int NOT NULL DEFAULT 0 COMMENT '销量',
    `image` varchar(500) DEFAULT NULL COMMENT '主图',
    `images` text COMMENT '图片列表（JSON）',
    `description` text COMMENT '商品描述',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0-下架，1-上架',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';
```

---

## 数据库规范速查表

| 规范 | 要点 |
|------|------|
| **引擎** | 统一使用 InnoDB |
| **字符集** | utf8mb4，支持 emoji |
| **主键** | bigint 自增 |
| **金额** | decimal(19,2)，禁止 float/double |
| **时间** | datetime / date |
| **布尔** | bit(1) |
| **文本** | varchar(n) ≤ 500，text > 500 |
| **基础字段** | id, create_time, update_time, creator, updater, deleted |
| **索引命名** | uk_唯一, idx_普通 |
| **字段命名** | snake_case，全小写 |
