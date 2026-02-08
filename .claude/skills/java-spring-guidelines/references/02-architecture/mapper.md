# Mapper 层规范

> Java/Spring Boot 编码规范 - Mapper 层

---

## 查询方式选择

| 场景 | 方式 | 说明 |
|------|------|------|
| 简单 CRUD | MyBatis Plus Lambda API | 类型安全，重构友好 |
| 复杂查询 | MyBatis XML | 灵活，支持复杂 SQL |
| 动态排序 | XML `<choose>` | 防止 SQL 注入 |

```java
// ✅ 简单查询使用 Lambda API
public List<Product> listByStatus(Integer status) {
    return productMapper.selectList(new LambdaQueryWrapper<Product>()
        .eq(Product::getStatus, status)
        .orderByDesc(Product::getCreateTime)
    );
}

// ✅ 复杂查询使用 XML
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    List<OrderStatisticsVO> queryStatistics(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("statusList") List<Integer> statusList
    );
}
```

---

## Lambda API 规范

### 基本查询

```java
// = 等于
.eq(Product::getId, id)

// != 不等于
.ne(Product::getStatus, 0)

// > 大于
.gt(Product::getPrice, new BigDecimal("100"))

// >= 大于等于
.ge(Product::getStock, 10)

// < 小于
.lt(Product::getCreateTime, now)

// <= 小于等于
.le(Product::getUpdateTime, now)

// LIKE 模糊查询
.like(Product::getName, keyword)
.likeLeft(Product::getMobile, "138")    // 左模糊 %138
.likeRight(Product::getNo, "ORD")       // 右模糊 ORD%

// IN 查询
.in(Product::getId, Arrays.asList(1L, 2L, 3L))
.in(Product::getStatus, Arrays.asList(1, 2))

// NOT IN
.notIn(Product::getStatus, Arrays.asList(0, 3))

// IS NULL
.isNull(Product::getDeleteTime)

// IS NOT NULL
.isNotNull(Product::getDeleteTime)

// BETWEEN
.between(Product::getCreateTime, startDate, endDate)

// 排序
.orderByAsc(Product::getCreateTime)
.orderByDesc(Product::getId)
```

### 条件拼接

```java
// ✅ 好的做法：条件只在为真时拼接
LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
    .eq(StringUtils.isNotBlank(name), Product::getName, name)
    .eq(status != null, Product::getStatus, status)
    .gt(minPrice != null, Product::getPrice, minPrice)
    .lt(maxPrice != null, Product::getPrice, maxPrice)
    .like(StringUtils.isNotBlank(keyword), Product::getName, keyword)
    .in(CollectionUtils.isNotEmpty(statusList), Product::getStatus, statusList);
```

### 分页查询

```java
public IPage<Product> getPage(ProductPageReq req) {
    // 1. 构建查询条件
    LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
        .like(StringUtils.isNotBlank(req.getName()), Product::getName, req.getName())
        .eq(req.getStatus() != null, Product::getStatus, req.getStatus())
        .orderByDesc(Product::getCreateTime);

    // 2. 分页查询
    return productMapper.selectPage(
        new Page<>(req.getPageNum(), req.getPageSize()),
        wrapper
    );
}
```

---

## SQL 安全

### 使用 #{} 防止 SQL 注入

```java
// ✅ 正确：使用 #{} 预编译
@Select("SELECT * FROM product WHERE id = #{id}")
Product selectById(Long id);

// ❌ 错误：使用 ${} 直接拼接，有 SQL 注入风险
@Select("SELECT * FROM product WHERE id = ${id}")
Product selectByIdUnsafe(Long id);
```

### 动态排序使用白名单

```xml
<!-- ✅ 正确：使用 choose 白名单 -->
SELECT * FROM product
<where>
    <if test="name != null and name != ''">
        AND name LIKE CONCAT('%', #{name}, '%')
    </if>
    <if test="status != null">
        AND status = #{status}
    </if>
</where>
ORDER BY
<choose>
    <when test="orderColumn == 'create_time'">create_time</when>
    <when test="orderColumn == 'update_time'">update_time</when>
    <when test="orderColumn == 'price'">price</when>
    <when test="orderColumn == 'name'">name</when>
    <otherwise>id</otherwise>
</choose>
<choose>
    <when test="orderDirection == 'asc'">ASC</when>
    <otherwise>DESC</otherwise>
</choose>
```

```java
// ❌ 错误：直接拼接字段名
String sql = "SELECT * FROM product ORDER BY " + orderColumn + " " + orderDirection;
```

---

## XML 映射规范

### ResultMap 定义

```xml
<!-- 基础 ResultMap -->
<resultMap id="BaseResultMap" type="com.company.entity.Product">
    <id column="id" property="id"/>
    <result column="name" property="name"/>
    <result column="price" property="price"/>
    <result column="status" property="status"/>
    <result column="create_time" property="createTime"/>
    <result column="update_time" property="updateTime"/>
</resultMap>

<!-- 嵌套对象 ResultMap -->
<resultMap id="OrderDetailResultMap" type="com.company.dto.OrderDetailDTO">
    <id column="order_id" property="orderId"/>
    <result column="order_no" property="orderNo"/>
    <!-- 一对一关联 -->
    <association property="user" javaType="com.company.dto.UserDTO">
        <id column="user_id" property="userId"/>
        <result column="user_name" property="userName"/>
    </association>
    <!-- 一对多关联 -->
    <collection property="items" ofType="com.company.dto.OrderItemDTO">
        <id column="item_id" property="itemId"/>
        <result column="product_name" property="productName"/>
        <result column="quantity" property="quantity"/>
    </collection>
</resultMap>
```

### 动态 SQL

```xml
<select id="queryByCondition" resultMap="BaseResultMap">
    SELECT * FROM product
    <where>
        <!-- if 条件 -->
        <if test="name != null and name != ''">
            AND name LIKE CONCAT('%', #{name}, '%')
        </if>

        <!-- choose when otherwise -->
        <choose>
            <when test="status != null">
                AND status = #{status}
            </when>
            <when test="statusList != null and statusList.size() > 0">
                AND status IN
                <foreach collection="statusList" item="status" open="(" separator="," close=")">
                    #{status}
                </foreach>
            </when>
        </choose>

        <!-- 时间范围 -->
        <if test="startTime != null">
            AND create_time &gt;= #{startTime}
        </if>
        <if test="endTime != null">
            AND create_time &lt;= #{endTime}
        </if>
    </where>

    <!-- 排序 -->
    <choose>
        <when test="orderColumn == 'create_time'">ORDER BY create_time</when>
        <when test="orderColumn == 'price'">ORDER BY price</when>
        <otherwise>ORDER BY id</otherwise>
    </choose>
    <choose>
        <when test="orderDirection == 'asc'">ASC</when>
        <otherwise>DESC</otherwise>
    </choose>

    <!-- 分页 -->
    LIMIT #{offset}, #{pageSize}
</select>
```

### 批量插入

```xml
<insert id="insertBatch">
    INSERT INTO order_item (order_id, product_id, quantity, price)
    VALUES
    <foreach collection="list" item="item" separator=",">
        (#{item.orderId}, #{item.productId}, #{item.quantity}, #{item.price})
    </foreach>
</insert>
```

### 批量更新

```xml
<update id="updateBatch">
    <foreach collection="list" item="item" separator=";">
        UPDATE product
        SET name = #{item.name},
            price = #{item.price},
            update_time = NOW()
        WHERE id = #{item.id}
    </foreach>
</update>

<!-- 注意：需要在 JDBC URL 中添加 allowMultiQueries=true -->
```

---

## 复杂查询示例

### 统计查询

```xml
<select id="queryStatistics" resultType="com.company.dto.OrderStatisticsVO">
    SELECT
        DATE(create_time) AS orderDate,
        COUNT(*) AS orderCount,
        SUM(amount) AS totalAmount,
        AVG(amount) AS avgAmount
    FROM `order`
    <where>
        <if test="startDate != null">
            AND DATE(create_time) &gt;= #{startDate}
        </if>
        <if test="endDate != null">
            AND DATE(create_time) &lt;= #{endDate}
        </if>
        <if test="statusList != null and statusList.size() > 0">
            AND status IN
            <foreach collection="statusList" item="status" open="(" separator="," close=")">
                #{status}
            </foreach>
        </if>
    </where>
    GROUP BY DATE(create_time)
    ORDER BY orderDate
</select>
```

### 关联查询

```xml
<select id="queryOrderWithUser" resultMap="OrderDetailResultMap">
    SELECT
        o.id AS order_id,
        o.order_no AS order_no,
        u.id AS user_id,
        u.name AS user_name,
        oi.id AS item_id,
        p.name AS product_name,
        oi.quantity AS quantity
    FROM `order` o
    LEFT JOIN `user` u ON o.user_id = u.id
    LEFT JOIN order_item oi ON o.id = oi.order_id
    LEFT JOIN product p ON oi.product_id = p.id
    <where>
        <if test="orderId != null">
            AND o.id = #{orderId}
        </if>
        <if test="orderNo != null and orderNo != ''">
            AND o.order_no = #{orderNo}
        </if>
    </where>
</select>
```

---

## Mapper 接口示例

```java
/**
 * 商品 Mapper
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 根据编码查询
     */
    Product selectByCode(@Param("code") String code);

    /**
     * 查询库存不足的商品
     */
    List<Product> selectLowStock(@Param("threshold") Integer threshold);

    /**
     * 批量更新状态
     */
    int updateStatusByIds(@Param("ids") List<Long> ids, @Param("status") Integer status);

    /**
     * 查询商品统计
     */
    ProductStatisticsVO queryStatistics(@Param("categoryId") Long categoryId);
}
```

---

## Mapper 规范速查表

| 规范 | 要点 |
|------|------|
| **简单查询** | 使用 MyBatis Plus Lambda API |
| **复杂查询** | 使用 MyBatis XML |
| **SQL 安全** | 使用 `#{}` 预编译，禁止 `${}` 拼接 |
| **动态排序** | XML `<choose>` 白名单，不直接拼接字段 |
| **参数校验** | XML 中使用 `<if test="xxx != null">` |
| **批量操作** | 使用 `<foreach>` |
| **命名规范** | 方法名清晰表达查询意图 |
