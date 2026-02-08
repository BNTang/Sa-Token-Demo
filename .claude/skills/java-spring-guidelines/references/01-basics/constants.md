# 常量定义规范

> Java/Spring Boot 编码规范 - 常量定义
> 参考：阿里巴巴 Java 开发手册

---

## 魔法值禁止（强制）

**【强制】不允许任何魔法值（即未经定义的常量）直接出现在代码中。**

```java
// ❌ 反例 - 魔法值直接出现
String key = "Id#taobao_" + tradeId;
cache.put(key, value);

if (status == 1) {                    // 1 代表什么？
    // ...
}

if ("admin".equals(role)) {           // 字符串魔法值
    // ...
}

// ✅ 正例 - 使用常量定义
public class CacheKeyConstants {
    public static final String TRADE_KEY_PREFIX = "Id#taobao_";
}

public class OrderStatusConstants {
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_PAID = 1;
    public static final int STATUS_SHIPPED = 2;
}

public class RoleConstants {
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_USER = "user";
}

// 使用
String key = CacheKeyConstants.TRADE_KEY_PREFIX + tradeId;
if (status == OrderStatusConstants.STATUS_PAID) {
    // ...
}
if (RoleConstants.ROLE_ADMIN.equals(role)) {
    // ...
}
```

---

## 常量命名规则

### 基本规则

| 规则 | 说明 | 示例 |
|------|------|------|
| **全大写** | 常量命名全部大写 | `MAX_STOCK_COUNT` |
| **下划线分隔** | 单词间用下划线隔开 | `DEFAULT_TIMEOUT_SECONDS` |
| **语义完整** | 力求语义表达完整清楚，不要嫌名字长 | ✅ `MAX_STOCK_COUNT` ❌ `MAX_COUNT` |

```java
// ❌ 反例 - 语义不完整
public static final int MAX_COUNT = 100;          // 什么的最大数量？
public static final String PREFIX = "order_";     // 什么的前缀？
public static final long TIMEOUT = 3000;          // 什么单位？秒？毫秒？

// ✅ 正例 - 语义清晰
public static final int MAX_STOCK_COUNT = 100;
public static final String ORDER_CACHE_KEY_PREFIX = "order:";
public static final long DEFAULT_TIMEOUT_MILLIS = 3000L;
public static final long DEFAULT_TIMEOUT_SECONDS = 3L;
```

---

## 常量分类管理

**【推荐】不要使用一个常量类维护所有常量，按常量功能进行归类，分开维护。**

```java
// ❌ 反例 - 大而全的常量类
public class Constants {
    public static final String REDIS_KEY_USER = "user:";
    public static final String REDIS_KEY_ORDER = "order:";
    public static final int ORDER_STATUS_PENDING = 0;
    public static final int ORDER_STATUS_PAID = 1;
    public static final String ROLE_ADMIN = "admin";
    public static final long CACHE_TTL = 3600L;
    // ... 几百个常量
}

// ✅ 正例 - 按功能分类
public class RedisKeyConstants {
    public static final String USER_INFO = "user:info:%s";
    public static final String ORDER_DETAIL = "order:detail:%s";
}

public class OrderStatusConstants {
    public static final int PENDING = 0;
    public static final int PAID = 1;
    public static final int SHIPPED = 2;
}

public class RoleConstants {
    public static final String ADMIN = "admin";
    public static final String USER = "user";
}

public class CacheTtlConstants {
    public static final long TTL_5_MIN = 5 * 60L;
    public static final long TTL_30_MIN = 30 * 60L;
    public static final long TTL_1_HOUR = 60 * 60L;
    public static final long TTL_1_DAY = 24 * 60 * 60L;
}
```

---

## 常量复用层次

**【推荐】常量的复用层次有五层：**

| 层次 | 说明 | 位置 |
|------|------|------|
| **跨应用共享常量** | 多个应用共用 | 二方库 `client.jar` 的 `constant` 目录 |
| **应用内共享常量** | 单个应用内共用 | 一方库 `modules` 的 `constant` 目录 |
| **子工程内共享常量** | 子工程内共用 | 当前子工程的 `constant` 目录 |
| **包内共享常量** | 包内共用 | 当前包下的 `constant` 目录 |
| **类内共享常量** | 类内使用 | 类内 `private static final` 定义 |

```java
// ❌ 反例 - 两个开发者定义了不同的 "是"
// A 类
public static final String YES = "yes";
// B 类  
public static final String YES = "y";
// A.YES.equals(B.YES) 预期 true，实际 false

// ✅ 正例 - 统一定义
public class BooleanConstants {
    public static final String YES = "Y";
    public static final String NO = "N";
}
```

---

## 枚举代替常量

**【推荐】如果变量值仅在一个范围内变化，且带有名称之外的延伸属性，定义为枚举类。**

```java
// ✅ 正例 - 使用枚举类
@Getter
@AllArgsConstructor
public enum WeekdayEnum {
    MONDAY(1, "星期一"),
    TUESDAY(2, "星期二"),
    WEDNESDAY(3, "星期三"),
    THURSDAY(4, "星期四"),
    FRIDAY(5, "星期五"),
    SATURDAY(6, "星期六"),
    SUNDAY(7, "星期日");

    private final Integer code;
    private final String desc;

    public static WeekdayEnum getByCode(Integer code) {
        return Arrays.stream(values())
                .filter(e -> Objects.equals(e.getCode(), code))
                .findFirst()
                .orElse(null);
    }
}

// ✅ 正例 - 订单状态枚举
@Getter
@AllArgsConstructor
public enum OrderStatusEnum {
    PENDING_PAYMENT(0, "待支付"),
    PAID(1, "已支付"),
    SHIPPED(2, "已发货"),
    COMPLETED(3, "已完成"),
    CANCELLED(4, "已取消");

    private final Integer code;
    private final String desc;
}
```

---

## 常量类示例

### Redis Key 常量

```java
/**
 * Redis Key 常量
 * 格式：{业务}:{模块}:{标识}
 */
public class RedisKeyConstants {

    private RedisKeyConstants() {}

    // ==================== 用户相关 ====================
    /** 用户信息 */
    public static final String USER_INFO = "user:info:%s";
    /** 用户 Token */
    public static final String USER_TOKEN = "user:token:%s";
    /** 用户权限 */
    public static final String USER_PERMISSION = "user:permission:%s";

    // ==================== 商品相关 ====================
    /** 商品详情 */
    public static final String PRODUCT_DETAIL = "product:detail:%s";
    /** 商品库存 */
    public static final String PRODUCT_STOCK = "product:stock:%s";

    // ==================== 订单相关 ====================
    /** 订单信息 */
    public static final String ORDER_INFO = "order:info:%s";
    /** 订单超时（分布式锁） */
    public static final String ORDER_TIMEOUT_LOCK = "order:timeout:lock:%s";
}
```

### 业务状态常量

```java
/**
 * 通用状态常量
 */
public class CommonStatusConstants {

    private CommonStatusConstants() {}

    /** 禁用 */
    public static final Integer STATUS_DISABLE = 0;
    /** 启用 */
    public static final Integer STATUS_ENABLE = 1;
}

/**
 * 删除状态常量
 */
public class DeletedConstants {

    private DeletedConstants() {}

    /** 未删除 */
    public static final Boolean NOT_DELETED = false;
    /** 已删除 */
    public static final Boolean DELETED = true;
}
```

### TTL 常量

```java
/**
 * 缓存 TTL 常量（单位：秒）
 */
public class CacheTtlConstants {

    private CacheTtlConstants() {}

    /** 5 分钟 */
    public static final long TTL_5_MIN = 5 * 60L;
    /** 10 分钟 */
    public static final long TTL_10_MIN = 10 * 60L;
    /** 30 分钟 */
    public static final long TTL_30_MIN = 30 * 60L;
    /** 1 小时 */
    public static final long TTL_1_HOUR = 60 * 60L;
    /** 1 天 */
    public static final long TTL_1_DAY = 24 * 60 * 60L;
    /** 7 天 */
    public static final long TTL_7_DAYS = 7 * 24 * 60 * 60L;
    /** 30 天 */
    public static final long TTL_30_DAYS = 30 * 24 * 60 * 60L;
}
```

---

## 常量禁则速查

| ❌ 禁止 | ✅ 正确 | 原因 |
|--------|--------|------|
| 魔法值 `1`, `"admin"` | 定义常量使用 | 可读性、可维护性 |
| 小写 `l` 后缀 `2l` | 大写 `L` 后缀 `2L` | 避免与 1 混淆 |
| 语义不完整 `MAX_COUNT` | 语义完整 `MAX_STOCK_COUNT` | 清晰表达含义 |
| 全局大常量类 | 按功能分类常量类 | 便于查找和维护 |
| 常量定义重复 | 统一提取到共享层 | 避免不一致 |
