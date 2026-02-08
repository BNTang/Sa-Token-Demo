# 命名规范

> Java/Spring Boot 编码规范 - 命名约定
> 参考：阿里巴巴 Java 开发手册

---

## 基本规则（强制）

### 命名禁则

| 规则 | 说明 | 示例 |
|------|------|------|
| **禁止下划线/美元符开头或结尾** | 代码命名均不能以 `_` 或 `$` 开始或结束 | ❌ `_name` / `name_` / `$Object` / `Object$` |
| **禁止拼音与英文混合** | 严禁使用拼音与英文混合，更不允许中文 | ❌ `DaZhePromotion` / `getPingfenByName()` |
| **禁止完全不规范缩写** | 避免望文不知义的缩写 | ❌ `AbsClass`(AbstractClass) / `condi`(condition) |

```java
// ❌ 反例
String _name;
String name_;
Object $Object;
int 某变量 = 3;                        // 中文变量名
void DaZhePromotion();                // 拼音命名
String getPingfenByName();            // 拼音英文混用

// ✅ 正例 - 国际通用名称可视同英文
String alibaba;
String taobao;
String hangzhou;
```

### POJO 布尔类型规则

**【强制】POJO 类中布尔类型变量，不要加 `is` 前缀**，否则部分框架解析会引起序列化错误。

```java
// ❌ 反例 - 会导致 RPC 框架序列化问题
public class UserDO {
    private Boolean isDeleted;        // getter 为 isDeleted()
                                      // 框架误认为属性名是 deleted
}

// ✅ 正例
public class UserDO {
    private Boolean deleted;          // getter 为 getDeleted() 或 isDeleted()
    private Boolean active;
    private Boolean enabled;
}
```

### Long 类型赋值

**【强制】** long 或 Long 赋值时，使用大写 `L`，禁止小写 `l`，避免与数字 `1` 混淆。

```java
// ❌ 反例
Long a = 2l;                          // 是 21 还是 2L？

// ✅ 正例
Long a = 2L;
long b = 100L;
```

---

## 类命名

| 类型 | 规则 | 示例 |
|-----|------|-----|
| 实体类 | 驼峰命名，业务名称 | `PromotionPlan`, `OrderItem`, `UserAccount` |
| Controller | `*Controller` | `ProductController`, `OrderController` |
| Service 接口 | `I*Service` | `IProductService`, `IOrderService` |
| Service 实现 | `*ServiceImpl` | `ProductServiceImpl`, `OrderServiceImpl` |
| Mapper | `*Mapper` | `ProductMapper`, `OrderMapper` |
| DTO | `*DTO` | `ProductDTO`, `UserDTO` |
| 请求对象 | `*Req` / `*Request` | `ProductPageReq`, `AddOrderRequest` |
| 响应对象 | `*Rsp` / `*Response` | `ProductDetailRsp`, `OrderInfoResponse` |
| 枚举类 | `*Enum` / `*Status` | `OrderStatusEnum`, `PaymentStatus` |
| 配置类 | `*Config` / `*Configuration` | `RedisConfig`, `WebMvcConfig` |
| 工具类 | `*Utils` / `*Util` | `DateUtils`, `StringUtil` |
| 常量类 | `*Constants` / `*Constant` | `SystemConstants`, `RedisKeyConstant` |
| 异常类 | `*Exception` | `BusinessException`, `PaymentException` |
| AOP 切面 | `*Aspect` | `LogAspect`, `PermissionAspect` |

### 命名原则

1. **见名知意**：名称应清晰表达其职责
2. **统一后缀**：同类组件使用统一后缀
3. **业务前缀**：多业务模块时加业务前缀区分

```java
// ✅ 好的命名
public class ProductController { }
public class ProductServiceImpl implements IProductService { }
public class ProductPageReq { }
public class ProductDetailRsp { }

// ❌ 差的命名
public class ProductCtrl { }              // 缩写不清晰
public class ProductImpl { }              // 缺少 Service 后缀
public class ProductRequest { }           // 未区分请求类型
```

---

## 变量命名

### 基本规则

| 类型 | 规则 | 示例 |
|-----|------|-----|
| 方法/字段 | `lowerCamelCase` | `getUserById`, `userName`, `maxSize` |
| 常量 | `UPPER_SNAKE_CASE` | `MAX_BATCH_SIZE`, `DEFAULT_TIMEOUT` |
| 数据库表 | `lower_snake_case` | `promotion_plan`, `order_item` |
| 接口路径 | `kebab-case` | `/product-catalog/page`, `/order-detail` |
| 包名 | 全小写，点分隔 | `com.company.project.module` |

### 集合变量命名

集合变量应使用复数或带 `List`/`Map` 后缀：

```java
// ✅ 好的命名
List<Product> products;
List<Product> productList;
Set<Long> userIds;
Map<Long, User> userMap;

// ❌ 差的命名
List<Product> product;          // 单数容易误解
List<Product> list;             // 太泛
Map<Long, User> map;            // 太泛
```

### 布尔变量命名

布尔变量应以 `is`/`has`/`can`/`should` 开头，或使用动词：

```java
// ✅ 好的命名
boolean isActive;
boolean hasPermission;
boolean canExecute;
boolean shouldRetry;
boolean deleted;                // 动词形式
boolean enabled;                // 形容词形式

// ❌ 差的命名
boolean flag;                   // 不明确
boolean status;                 // 应该用枚举
boolean check;                  // 动词不清晰
```

---

## 方法命名

### 常用前缀

| 前缀 | 含义 | 示例 |
|------|------|-----|
| `get` | 获取单个对象 | `getUserById`, `getProduct` |
| `list` | 获取列表 | `listProducts`, `listActiveOrders` |
| `count` | 统计数量 | `countByStatus`, `countActiveUsers` |
| `save` | 保存（新增/更新） | `saveOrder`, `saveProduct` |
| `insert` | 新增 | `insertOrder`, `insertUser` |
| `update` | 更新 | `updateStatus`, `updatePassword` |
| `delete` | 删除 | `deleteById`, `deleteOrder` |
| `remove` | 移除 | `removeFromCache`, `removeItem` |
| `check` | 检查（返回布尔） | `checkPermission`, `checkExists` |
| `validate` | 验证（抛异常） | `validateInput`, `validateParams` |
| `is/has/can` | 判断（返回布尔） | `isValid`, `hasPermission`, `canExecute` |
| `convert/to` | 转换 | `convertToDTO`, `toEntity`, `toResponse` |
| `parse/from` | 解析 | `parseDate`, `fromJson`, `fromDTO` |
| `build/create` | 构建/创建 | `buildResponse`, `createOrder` |
| `process/handle` | 处理 | `processPayment`, `handleCallback` |

### Controller 方法命名

```java
// ✅ 好的命名 - 清晰表达操作
@PostMapping("/add")
public CommonResult<Void> addProduct(@Valid @RequestBody ProductAddReq req)

@PostMapping("/update")
public CommonResult<Void> updateProduct(@Valid @RequestBody ProductUpdateReq req)

@PostMapping("/delete")
public CommonResult<Void> deleteProduct(@Valid @RequestBody ProductDeleteReq req)

@GetMapping("/detail")
public CommonResult<ProductDetailRsp> getDetail(@RequestParam Long id)

@PostMapping("/page")
public CommonResult<IPage<ProductPageDTO>> getPage(@Valid @RequestBody ProductPageReq req)

@PostMapping("/list")
public CommonResult<List<ProductDTO>> listProducts(@Valid @RequestBody ProductQueryReq req)
```

---

## 数据库命名

### 表命名

| 规则 | 说明 | 示例 |
|------|------|-----|
| 全小写下划线 | 表名使用 `snake_case` | `promotion_plan`, `order_item` |
| 业务前缀 | 多业务时加业务前缀 | `mall_product`, `mall_order` |
| 关系表 | 双表名下划线连接 | `user_role`, `order_product` |
| 树形表 | 父子关系统一后缀 | `category`, `department` |

```sql
-- ✅ 好的命名
CREATE TABLE `product` ( ... );
CREATE TABLE `order_item` ( ... );
CREATE TABLE `user_role` ( ... );

-- ❌ 差的命名
CREATE TABLE `Product` ( ... );           -- 大写
CREATE TABLE `orderItem` ( ... );          -- 驼峰
CREATE TABLE `order-items` ( ... );        -- 连字符
```

### 字段命名

| 规则 | 说明 | 示例 |
|------|------|-----|
| 全小写下划线 | 字段名使用 `snake_case` | `user_name`, `create_time` |
| 布尔字段 | `is_` 前缀或动词 | `is_active`, `deleted`, `enabled` |
| 时间字段 | `_time` / `_date` 后缀 | `create_time`, `effect_date` |
| 状态字段 | `_status` 后缀 | `order_status`, `approval_status` |
| 外键字段 | `关联表_id` 格式 | `user_id`, `product_id` |
| 金额字段 | `_amount` 后缀，用 `decimal` | `total_amount`, `pay_amount` |

```sql
-- ✅ 好的命名
CREATE TABLE `user` (
    `id` bigint NOT NULL,
    `user_name` varchar(64) NOT NULL COMMENT '用户名',
    `is_active` bit(1) DEFAULT b'1' COMMENT '是否激活',
    `create_time` datetime NOT NULL COMMENT '创建时间',
    `order_count` int DEFAULT 0 COMMENT '订单数量',
    PRIMARY KEY (`id`)
);

-- ❌ 差的命名
CREATE TABLE `user` (
    `id` bigint NOT NULL,
    `userName` varchar(64) NOT NULL,       -- 驼峰
    `active` bit(1) DEFAULT b'1',         -- 布尔无前缀
    `createtime` datetime NOT NULL,        -- 无下划线
    `ordercount` int DEFAULT 0             -- 无下划线
);
```

---

## 包命名

### 标准分层结构

```
com.company.project
├── controller          # 控制器层
├── service             # 服务层
│   ├── impl           # 服务实现
│   └── ...            # 服务接口（可选放父目录）
├── mapper              # 数据访问层
├── entity              # 实体类
├── dto                 # 数据传输对象
│   ├── req            # 请求对象
│   └── rsp            # 响应对象
├── enums               # 枚举类
├── config              # 配置类
├── utils               # 工具类
├── constants           # 常量类
├── exception           # 异常类
├── aspects             # AOP 切面
├── listener            # 监听器
└── common              # 公共模块
```

### 按业务模块分包（推荐）

```
com.company.project
├── product             # 商品模块
│   ├── controller
│   ├── service
│   ├── mapper
│   ├── entity
│   └── dto
├── order               # 订单模块
│   ├── controller
│   ├── service
│   └── ...
├── user                # 用户模块
└── common              # 公共模块
    ├── config
    ├── utils
    └── exception
```

---

## 常量命名

### 常量类组织

```java
/**
 * Redis Key 常量
 */
public class RedisKeyConstants {

    // 用户相关
    public static final String USER_INFO = "user:info:%s";
    public static final String USER_TOKEN = "user:token:%s";

    // 商品相关
    public static final String PRODUCT_DETAIL = "product:detail:%s";
    public static final String PRODUCT_LIST = "product:list:%s";

    // 订单相关
    public static final String ORDER_INFO = "order:info:%s";

    // 通用 TTL
    public static final long TTL_5_MIN = 5 * 60;
    public static final long TTL_30_MIN = 30 * 60;
    public static final long TTL_1_HOUR = 60 * 60;
}

/**
 * 业务状态常量
 */
public class BusinessStatusConstants {

    public static final Integer STATUS_DRAFT = 0;       // 草稿
    public static final Integer STATUS_ACTIVE = 1;      // 启用
    public static final Integer STATUS_INACTIVE = 2;    // 停用
    public static final Integer STATUS_DELETED = 3;     // 已删除
}
```

---

## 枚举命名

```java
/**
 * 订单状态枚举
 */
@Getter
@AllArgsConstructor
public enum OrderStatusEnum {

    PENDING_PAYMENT(1, "待支付"),
    PAID(2, "已支付"),
    SHIPPED(3, "已发货"),
    COMPLETED(4, "已完成"),
    CANCELLED(5, "已取消"),
    REFUNDED(6, "已退款");

    private final Integer code;
    private final String desc;

    public static OrderStatusEnum getByCode(Integer code) {
        return Arrays.stream(values())
                .filter(e -> Objects.equals(e.getCode(), code))
                .findFirst()
                .orElse(null);
    }
}
```

---

## 通用命名禁则

| ❌ 禁止 | ✅ 正确 | 原因 |
|--------|--------|------|
| `data`, `info`, `manager` | 具体业务名称 | 太泛，不明确 |
| `handle()`, `process()` | `handlePayment()`, `processOrder()` | 太泛，不明确 |
| `flag`, `status` (布尔) | `isValid`, `isActive` | 布尔应用 is/has |
| `a`, `b`, `tmp` | 具体含义名称 | 无意义 |
| 中文拼音 | `userName` 而非 `yongHuMing` | 不专业 |
| 单字母（循环除外） | `index`, `item` | 不清晰 |
