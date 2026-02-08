# MyBatis 中 #{} 和 ${} 的区别

> 分类: MyBatis | 难度: ⭐⭐ | 频率: 高频

---

## 一、核心区别

```
┌───────────────────────────────────────────────────────────────────────────┐
│                      #{} vs ${} 对比                                       │
├─────────────────┬─────────────────────────┬───────────────────────────────┤
│                 │         #{}             │           ${}                  │
├─────────────────┼─────────────────────────┼───────────────────────────────┤
│   本质          │   预编译占位符           │   字符串替换                   │
│   SQL处理       │   PreparedStatement      │   Statement                   │
│   SQL注入       │   ✅ 安全               │   ❌ 有风险                    │
│   参数类型      │   自动处理类型转换       │   需手动处理                   │
│   性能          │   可复用预编译语句       │   每次重新编译                 │
│   适用场景      │   传递参数值             │   动态表名/列名                │
└─────────────────┴─────────────────────────┴───────────────────────────────┘
```

---

## 二、工作原理

### 2.1 #{} 预编译原理

```
用户输入: name = "张三"

MyBatis 处理过程:
┌────────────────────────────────────────────────────────────────────────┐
│  原始SQL:  SELECT * FROM user WHERE name = #{name}                      │
│                          ↓                                              │
│  预编译:   SELECT * FROM user WHERE name = ?                           │
│                          ↓                                              │
│  执行:     PreparedStatement.setString(1, "张三")                       │
│                          ↓                                              │
│  最终SQL:  SELECT * FROM user WHERE name = '张三'                       │
└────────────────────────────────────────────────────────────────────────┘

关键: 参数值在预编译之后绑定，无法改变SQL结构
```

### 2.2 ${} 字符串替换原理

```
用户输入: name = "张三"

MyBatis 处理过程:
┌────────────────────────────────────────────────────────────────────────┐
│  原始SQL:  SELECT * FROM user WHERE name = '${name}'                   │
│                          ↓                                              │
│  字符串替换: SELECT * FROM user WHERE name = '张三'                     │
│                          ↓                                              │
│  编译执行: Statement.execute("SELECT * FROM user WHERE name = '张三'") │
└────────────────────────────────────────────────────────────────────────┘

危险: 参数直接拼接到SQL，可能改变SQL结构
```

---

## 三、SQL 注入风险演示

### 3.1 使用 ${} 的危险示例

```xml
<!-- Mapper.xml -->
<select id="findByName" resultType="User">
    SELECT * FROM user WHERE name = '${name}'
</select>
```

```java
// 恶意输入
String name = "' OR '1'='1";

// 拼接后的SQL (返回所有用户!)
SELECT * FROM user WHERE name = '' OR '1'='1'

// 更危险的输入
String name = "'; DROP TABLE user; --";

// 拼接后的SQL (删除表!)
SELECT * FROM user WHERE name = ''; DROP TABLE user; --'
```

### 3.2 使用 #{} 安全处理

```xml
<!-- Mapper.xml -->
<select id="findByName" resultType="User">
    SELECT * FROM user WHERE name = #{name}
</select>
```

```java
// 恶意输入
String name = "' OR '1'='1";

// 预编译后的SQL
SELECT * FROM user WHERE name = ?

// 参数绑定 (恶意字符被作为普通字符串处理)
PreparedStatement.setString(1, "' OR '1'='1")

// 最终效果: 查找name等于 "' OR '1'='1" 的用户(找不到)
```

---

## 四、${} 的正确使用场景

### 4.1 动态表名

```xml
<!-- 分表查询，表名无法预编译 -->
<select id="findByTableName" resultType="Order">
    SELECT * FROM ${tableName} WHERE id = #{id}
</select>
```

```java
// 调用
orderMapper.findByTableName("order_202401", 12345L);

// 生成SQL
SELECT * FROM order_202401 WHERE id = ?
```

### 4.2 动态列名 (ORDER BY)

```xml
<!-- 排序字段动态传入 -->
<select id="findAllOrdered" resultType="User">
    SELECT * FROM user ORDER BY ${orderColumn} ${orderDirection}
</select>
```

```java
// 调用
userMapper.findAllOrdered("create_time", "DESC");

// 生成SQL
SELECT * FROM user ORDER BY create_time DESC
```

### 4.3 安全使用 ${} 的方式

```java
/**
 * 白名单校验，防止SQL注入
 */
public class SqlSafetyUtils {
    
    // 允许的排序字段白名单
    private static final Set<String> ALLOWED_ORDER_COLUMNS = Set.of(
        "id", "name", "create_time", "update_time"
    );
    
    // 允许的排序方向
    private static final Set<String> ALLOWED_DIRECTIONS = Set.of(
        "ASC", "DESC"
    );
    
    public static String validateOrderColumn(String column) {
        if (column == null || !ALLOWED_ORDER_COLUMNS.contains(column.toLowerCase())) {
            return "id"; // 默认值
        }
        return column;
    }
    
    public static String validateDirection(String direction) {
        if (direction == null || !ALLOWED_DIRECTIONS.contains(direction.toUpperCase())) {
            return "ASC";
        }
        return direction.toUpperCase();
    }
}
```

```java
// Service层调用前校验
public List<User> findAllOrdered(String orderColumn, String direction) {
    String safeColumn = SqlSafetyUtils.validateOrderColumn(orderColumn);
    String safeDirection = SqlSafetyUtils.validateDirection(direction);
    return userMapper.findAllOrdered(safeColumn, safeDirection);
}
```

---

## 五、源码分析

### 5.1 参数解析入口

```java
// org.apache.ibatis.scripting.xmltags.TextSqlNode
public class TextSqlNode implements SqlNode {
    
    // ${} 使用此方法解析
    private static class BindingTokenParser implements TokenHandler {
        @Override
        public String handleToken(String content) {
            // 直接从context获取值并转字符串
            Object value = OgnlCache.getValue(content, context.getBindings());
            return value == null ? "" : String.valueOf(value);
        }
    }
}
```

```java
// org.apache.ibatis.scripting.defaults.DefaultParameterHandler
public class DefaultParameterHandler implements ParameterHandler {
    
    // #{} 使用 PreparedStatement 设置参数
    @Override
    public void setParameters(PreparedStatement ps) {
        // 遍历参数映射
        for (int i = 0; i < parameterMappings.size(); i++) {
            ParameterMapping parameterMapping = parameterMappings.get(i);
            Object value = // 获取参数值...
            
            // 使用 TypeHandler 设置参数
            TypeHandler typeHandler = parameterMapping.getTypeHandler();
            typeHandler.setParameter(ps, i + 1, value, jdbcType);
        }
    }
}
```

---

## 六、面试回答

### 30秒版本

> `#{}` 是预编译占位符，使用 PreparedStatement，**防止 SQL 注入**，推荐使用。
> `${}` 是字符串替换，直接拼接 SQL，**有注入风险**，仅用于动态表名、列名等无法预编译的场景，且需做白名单校验。

### 1分钟版本

> 两者本质区别在于处理方式：
>
> **#{}（推荐）**
> - 使用 PreparedStatement 预编译
> - 参数值在编译后绑定，无法改变 SQL 结构
> - 自动处理类型转换和特殊字符转义
> - 可复用执行计划，性能更好
>
> **${}（谨慎使用）**
> - 纯字符串替换，直接拼接到 SQL
> - 有 SQL 注入风险
> - 仅用于动态表名、列名、ORDER BY 字段
> - 必须配合白名单校验使用
>
> 实际开发中，99% 场景用 `#{}`，只有动态表名列名等特殊场景才用 `${}`。

---

## 七、最佳实践

### ✅ 推荐做法

```xml
<!-- 1. 参数传递始终使用 #{} -->
<select id="findById" resultType="User">
    SELECT * FROM user WHERE id = #{id}
</select>

<!-- 2. 批量查询使用 foreach -->
<select id="findByIds" resultType="User">
    SELECT * FROM user WHERE id IN
    <foreach collection="ids" item="id" open="(" separator="," close=")">
        #{id}
    </foreach>
</select>

<!-- 3. 动态SQL使用<if>/<choose>而非${}拼接 -->
<select id="search" resultType="User">
    SELECT * FROM user
    <where>
        <if test="name != null">AND name = #{name}</if>
        <if test="age != null">AND age = #{age}</if>
    </where>
</select>
```

### ❌ 避免做法

```xml
<!-- ❌ 参数值使用 ${} -->
<select id="findByName" resultType="User">
    SELECT * FROM user WHERE name = '${name}'
</select>

<!-- ❌ 没有校验的动态表名 -->
<select id="findAll" resultType="Map">
    SELECT * FROM ${tableName}
</select>

<!-- ❌ LIKE查询中直接使用${} -->
<select id="search" resultType="User">
    SELECT * FROM user WHERE name LIKE '%${keyword}%'
</select>

<!-- ✅ LIKE正确写法 -->
<select id="search" resultType="User">
    SELECT * FROM user WHERE name LIKE CONCAT('%', #{keyword}, '%')
</select>
```

---

## 八、常见问题

### Q1: #{} 能用于 ORDER BY 吗?

```xml
<!-- ❌ 错误: #{} 会加引号 -->
<select id="findAll" resultType="User">
    SELECT * FROM user ORDER BY #{column}
</select>
<!-- 生成: ORDER BY 'create_time' (语法错误) -->

<!-- ✅ 正确: 使用 ${} + 白名单校验 -->
<select id="findAll" resultType="User">
    SELECT * FROM user ORDER BY ${column}
</select>
```

### Q2: 如何在 #{} 中使用 LIKE?

```xml
<!-- 方式1: CONCAT函数(推荐) -->
<select id="search" resultType="User">
    SELECT * FROM user WHERE name LIKE CONCAT('%', #{keyword}, '%')
</select>

<!-- 方式2: bind标签 -->
<select id="search" resultType="User">
    <bind name="pattern" value="'%' + keyword + '%'"/>
    SELECT * FROM user WHERE name LIKE #{pattern}
</select>
```
