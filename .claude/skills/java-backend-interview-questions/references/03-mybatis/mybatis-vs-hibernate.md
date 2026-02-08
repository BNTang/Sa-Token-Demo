# MyBatis 与 Hibernate 的区别

> 分类: ORM框架 | 难度: ⭐⭐⭐ | 频率: 高频

---

## 一、核心区别

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                        MyBatis vs Hibernate                                       │
├─────────────────┬────────────────────────────┬───────────────────────────────────┤
│                 │        MyBatis             │         Hibernate                 │
├─────────────────┼────────────────────────────┼───────────────────────────────────┤
│   类型          │  半自动 ORM                 │  全自动 ORM                       │
│   SQL 控制      │  手写 SQL                   │  自动生成 SQL                     │
│   学习曲线      │  较低                       │  较高                             │
│   灵活性        │  高(完全控制SQL)            │  低(框架生成SQL)                  │
│   移植性        │  低(SQL依赖数据库)          │  高(HQL跨数据库)                  │
│   性能优化      │  容易(直接优化SQL)          │  困难(需理解生成的SQL)            │
│   适用场景      │  复杂查询多的业务系统        │  模型驱动的快速开发               │
└─────────────────┴────────────────────────────┴───────────────────────────────────┘
```

---

## 二、架构对比

### 2.1 MyBatis 架构

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          MyBatis 架构                                             │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│   ┌─────────────┐                                                               │
│   │   Java 对象 │                                                               │
│   └──────┬──────┘                                                               │
│          │                                                                       │
│          ↓                                                                       │
│   ┌─────────────────────────────────────────────────────────────────────────┐   │
│   │                        Mapper Interface                                  │   │
│   │                      (UserMapper.java)                                   │   │
│   └─────────────────────────────────────────────────────────────────────────┘   │
│          │                                                                       │
│          ↓                                                                       │
│   ┌─────────────────────────────────────────────────────────────────────────┐   │
│   │                         XML Mapper                                       │   │
│   │                      (UserMapper.xml)                                    │   │
│   │   <select id="findById">                                                 │   │
│   │       SELECT * FROM user WHERE id = #{id}    ← 手写SQL                  │   │
│   │   </select>                                                              │   │
│   └─────────────────────────────────────────────────────────────────────────┘   │
│          │                                                                       │
│          ↓                                                                       │
│   ┌─────────────┐                                                               │
│   │   Database  │                                                               │
│   └─────────────┘                                                               │
│                                                                                  │
│   特点: SQL 与 Java 代码分离，开发者完全控制 SQL                                  │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Hibernate 架构

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          Hibernate 架构                                           │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│   ┌─────────────┐                                                               │
│   │   Java 对象 │                                                               │
│   │   (Entity)  │                                                               │
│   └──────┬──────┘                                                               │
│          │ @Entity, @Table, @Column                                             │
│          ↓                                                                       │
│   ┌─────────────────────────────────────────────────────────────────────────┐   │
│   │                        SessionFactory                                    │   │
│   │                     (ORM映射、缓存管理)                                   │   │
│   └─────────────────────────────────────────────────────────────────────────┘   │
│          │                                                                       │
│          ↓                                                                       │
│   ┌─────────────────────────────────────────────────────────────────────────┐   │
│   │                        SQL Generator                                     │   │
│   │   session.get(User.class, 1)  →  SELECT * FROM user WHERE id = ?        │   │
│   │                               ↑ 框架自动生成SQL                          │   │
│   └─────────────────────────────────────────────────────────────────────────┘   │
│          │                                                                       │
│          ↓                                                                       │
│   ┌─────────────┐                                                               │
│   │   Database  │                                                               │
│   └─────────────┘                                                               │
│                                                                                  │
│   特点: 对象关系映射自动化，框架生成 SQL                                          │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 三、代码对比

### 3.1 实体类定义

```java
// MyBatis - 简单 POJO
public class User {
    private Long id;
    private String name;
    private String email;
    // getters and setters
}

// Hibernate - 需要 JPA 注解
@Entity
@Table(name = "user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "email", unique = true)
    private String email;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Order> orders;
    // getters and setters
}
```

### 3.2 基础 CRUD

```java
// ==================== MyBatis ====================

// UserMapper.java
public interface UserMapper {
    User findById(Long id);
    List<User> findByName(String name);
    int insert(User user);
    int update(User user);
    int delete(Long id);
}

// UserMapper.xml
<mapper namespace="com.example.mapper.UserMapper">
    <select id="findById" resultType="User">
        SELECT id, name, email FROM user WHERE id = #{id}
    </select>
    
    <select id="findByName" resultType="User">
        SELECT * FROM user WHERE name LIKE CONCAT('%', #{name}, '%')
    </select>
    
    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO user (name, email) VALUES (#{name}, #{email})
    </insert>
    
    <update id="update">
        UPDATE user SET name = #{name}, email = #{email} WHERE id = #{id}
    </update>
    
    <delete id="delete">
        DELETE FROM user WHERE id = #{id}
    </delete>
</mapper>

// ==================== Hibernate ====================

// UserRepository.java (Spring Data JPA)
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByNameContaining(String name);  // 自动生成 SQL
}

// 或者使用 Session
public class UserDao {
    @Autowired
    private SessionFactory sessionFactory;
    
    public User findById(Long id) {
        return sessionFactory.getCurrentSession().get(User.class, id);
    }
    
    public void save(User user) {
        sessionFactory.getCurrentSession().save(user);
    }
}
```

### 3.3 复杂查询对比

```java
// ==================== 复杂多表关联查询 ====================

// MyBatis - 直接写 SQL，完全控制
<select id="findUserOrderStats" resultType="UserOrderStats">
    SELECT 
        u.id,
        u.name,
        COUNT(o.id) as order_count,
        SUM(o.amount) as total_amount,
        MAX(o.create_time) as last_order_time
    FROM user u
    LEFT JOIN orders o ON u.id = o.user_id
    WHERE u.status = #{status}
      AND o.create_time BETWEEN #{startTime} AND #{endTime}
    GROUP BY u.id, u.name
    HAVING COUNT(o.id) > #{minOrderCount}
    ORDER BY total_amount DESC
    LIMIT #{limit}
</select>

// Hibernate - HQL 或 Criteria API
public List<Object[]> findUserOrderStats(Integer status, Date startTime, 
                                          Date endTime, int minOrderCount, int limit) {
    String hql = "SELECT u.id, u.name, COUNT(o.id), SUM(o.amount), MAX(o.createTime) " +
                 "FROM User u LEFT JOIN u.orders o " +
                 "WHERE u.status = :status " +
                 "AND o.createTime BETWEEN :startTime AND :endTime " +
                 "GROUP BY u.id, u.name " +
                 "HAVING COUNT(o.id) > :minOrderCount " +
                 "ORDER BY SUM(o.amount) DESC";
    
    return session.createQuery(hql)
            .setParameter("status", status)
            .setParameter("startTime", startTime)
            .setParameter("endTime", endTime)
            .setParameter("minOrderCount", minOrderCount)
            .setMaxResults(limit)
            .list();
}
```

---

## 四、详细对比

### 4.1 功能对比

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          功能对比                                                 │
├─────────────────────┬─────────────────────┬──────────────────────────────────────┤
│       功能          │       MyBatis       │           Hibernate                  │
├─────────────────────┼─────────────────────┼──────────────────────────────────────┤
│  一级缓存           │    ✅ Session级      │    ✅ Session级                      │
│  二级缓存           │    ✅ 需配置         │    ✅ 内置                           │
│  延迟加载           │    ✅ 支持           │    ✅ 支持                           │
│  级联操作           │    ❌ 不支持         │    ✅ 支持 (@OneToMany等)            │
│  对象状态管理       │    ❌ 不管理         │    ✅ 托管/脱管/删除                 │
│  脏检查             │    ❌ 无             │    ✅ 自动检测变更                   │
│  乐观锁             │    需手动实现        │    ✅ @Version                       │
│  数据库方言         │    ❌ SQL依赖数据库  │    ✅ 跨数据库                       │
└─────────────────────┴─────────────────────┴──────────────────────────────────────┘
```

### 4.2 性能对比

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          性能对比                                                 │
├─────────────────────┬─────────────────────┬──────────────────────────────────────┤
│       场景          │       MyBatis       │           Hibernate                  │
├─────────────────────┼─────────────────────┼──────────────────────────────────────┤
│  简单CRUD           │    快               │    快(有缓存优势)                    │
│  复杂查询           │    快(SQL可优化)    │    可能较慢(生成的SQL不一定最优)     │
│  批量操作           │    手动优化         │    需调优(避免N+1问题)               │
│  大数据量           │    容易优化         │    需要调优                          │
│  SQL调优空间        │    大(完全控制)     │    小(框架生成)                      │
└─────────────────────┴─────────────────────┴──────────────────────────────────────┘
```

---

## 五、N+1 问题对比

```java
// ==================== Hibernate N+1 问题 ====================

// 实体定义
@Entity
public class User {
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Order> orders;
}

// 查询
List<User> users = session.createQuery("FROM User").list();  // 1 次查询
for (User user : users) {
    user.getOrders().size();  // 每个用户触发 1 次查询 → N 次查询
}
// 总共 1 + N 次查询!

// 解决方案: JOIN FETCH
List<User> users = session.createQuery(
    "FROM User u JOIN FETCH u.orders"
).list();  // 1 次查询搞定


// ==================== MyBatis 处理方式 ====================

// 方式1: 直接 JOIN 查询
<select id="findUsersWithOrders" resultMap="userOrderMap">
    SELECT u.*, o.* FROM user u LEFT JOIN orders o ON u.id = o.user_id
</select>

// 方式2: 嵌套查询 (也有 N+1 问题)
<resultMap id="userMap" type="User">
    <collection property="orders" select="findOrdersByUserId" column="id"/>
</resultMap>

// 方式3: 批量查询 (推荐)
// 先查所有用户，再根据用户ID列表批量查订单
List<User> users = userMapper.findAll();
List<Long> userIds = users.stream().map(User::getId).collect(toList());
List<Order> orders = orderMapper.findByUserIds(userIds);  // WHERE user_id IN (...)
```

---

## 六、面试回答

### 30秒版本

> MyBatis 是**半自动 ORM**，需要手写 SQL，灵活可控，适合复杂查询多的系统；
> Hibernate 是**全自动 ORM**，自动生成 SQL，开发效率高但灵活性差，适合模型驱动开发。
>
> 主要区别：
> - SQL 控制：MyBatis 手写，Hibernate 自动生成
> - 学习成本：MyBatis 低，Hibernate 高
> - 移植性：Hibernate 好（跨数据库），MyBatis 差

### 1分钟版本

> **定位不同：**
> - MyBatis 是 SQL Mapper 框架，半自动 ORM，开发者写 SQL，框架负责 SQL 执行和结果映射
> - Hibernate 是全自动 ORM，通过对象关系映射自动生成 SQL
>
> **SQL 控制：**
> - MyBatis 可以直接写原生 SQL，方便优化和使用数据库特性
> - Hibernate 使用 HQL 或 Criteria，生成的 SQL 不一定最优
>
> **开发效率：**
> - Hibernate 简单 CRUD 更快，不需要写 SQL
> - MyBatis 需要写 XML/注解配置 SQL
>
> **适用场景：**
> - MyBatis：查询复杂、对 SQL 优化要求高、不需要跨数据库的系统
> - Hibernate：模型相对简单、需要快速开发、需要跨数据库移植的系统
>
> 国内互联网公司大多使用 MyBatis，因为灵活性更好，复杂业务场景可控。

---

## 七、选型建议

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          选型建议                                                 │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  选择 MyBatis:                                                                   │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │  ✓ 复杂的业务逻辑，多表关联查询多                                          │ │
│  │  ✓ 对性能要求高，需要精细化 SQL 调优                                       │ │
│  │  ✓ 团队 SQL 能力强                                                         │ │
│  │  ✓ 已有遗留数据库，表结构不规范                                            │ │
│  │  ✓ 需要使用数据库特定功能(存储过程、函数等)                                │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  选择 Hibernate/JPA:                                                             │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │  ✓ 新项目，可以从对象模型开始设计                                          │ │
│  │  ✓ 简单 CRUD 为主，复杂查询少                                              │ │
│  │  ✓ 需要跨数据库移植                                                        │ │
│  │  ✓ 使用领域驱动设计 (DDD)                                                  │ │
│  │  ✓ 团队熟悉 Hibernate 生态                                                 │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  现实选择:                                                                       │
│  • 国内互联网公司: 大多使用 MyBatis (或 MyBatis-Plus)                           │
│  • 国外/外企: JPA/Hibernate 使用较多                                            │
│  • Spring Boot 默认: Spring Data JPA (基于 Hibernate)                           │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```
