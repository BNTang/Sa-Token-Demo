# PO、VO、BO、DTO、DAO、POJO 有什么区别？

## 对象类型概览

```
┌─────────────────────────────────────────────────────────────┐
│                    Java 对象分层模型                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   前端/客户端                                                │
│       ↓ VO (展示)                                           │
│   ┌─────────────────────────────────────────────────────┐  │
│   │                    Controller 层                     │  │
│   └─────────────────────────────────────────────────────┘  │
│       ↓ DTO (传输)                                          │
│   ┌─────────────────────────────────────────────────────┐  │
│   │                    Service 层                        │  │
│   │                    (BO 业务对象)                     │  │
│   └─────────────────────────────────────────────────────┘  │
│       ↓ PO (持久化)                                         │
│   ┌─────────────────────────────────────────────────────┐  │
│   │                    DAO 层                            │  │
│   └─────────────────────────────────────────────────────┘  │
│       ↓                                                     │
│   ┌─────────────────────────────────────────────────────┐  │
│   │                    数据库                            │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 各对象类型详解

```
┌─────────────────────────────────────────────────────────────┐
│                    对象类型详解                              │
├───────────┬─────────────────────────────────────────────────┤
│   类型    │   说明                                          │
├───────────┼─────────────────────────────────────────────────┤
│   POJO    │   Plain Old Java Object                        │
│           │   普通 Java 对象，只有属性和 getter/setter      │
│           │   是所有其他对象类型的统称/基础                 │
├───────────┼─────────────────────────────────────────────────┤
│   PO      │   Persistent Object (持久化对象)               │
│           │   与数据库表一一对应                           │
│           │   也叫 Entity、DO (Domain Object)              │
├───────────┼─────────────────────────────────────────────────┤
│   VO      │   View Object (视图对象)                       │
│           │   用于前端展示                                 │
│           │   可能聚合多个 PO 的数据                       │
├───────────┼─────────────────────────────────────────────────┤
│   DTO     │   Data Transfer Object (数据传输对象)          │
│           │   用于层间/服务间数据传输                      │
│           │   屏蔽内部实现细节                             │
├───────────┼─────────────────────────────────────────────────┤
│   BO      │   Business Object (业务对象)                   │
│           │   封装业务逻辑                                 │
│           │   可能包含多个 PO                              │
├───────────┼─────────────────────────────────────────────────┤
│   DAO     │   Data Access Object (数据访问对象)            │
│           │   封装数据库操作                               │
│           │   提供 CRUD 方法                               │
└───────────┴─────────────────────────────────────────────────┘
```

## 代码示例

### PO (持久化对象)

```java
// PO: 对应数据库表
@Entity
@Table(name = "user")
public class UserPO {
    @Id
    private Long id;
    private String username;
    private String password;  // 敏感字段
    private String email;
    private Date createTime;
    private Date updateTime;
    // getter/setter
}
```

### DTO (数据传输对象)

```java
// DTO: 接口请求/响应
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    // 不包含 password 等敏感字段
    // getter/setter
}

// 注册请求 DTO
public class UserRegisterDTO {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    @Email
    private String email;
}
```

### VO (视图对象)

```java
// VO: 前端展示
public class UserVO {
    private Long id;
    private String username;
    private String email;
    private String roleName;      // 关联查询的角色名
    private List<String> permissions;  // 权限列表
    private String createTimeStr;  // 格式化后的时间
}
```

### BO (业务对象)

```java
// BO: 封装业务逻辑
public class OrderBO {
    private OrderPO order;
    private List<OrderItemPO> items;
    private UserPO user;
    
    // 业务方法
    public BigDecimal calculateTotal() {
        return items.stream()
            .map(item -> item.getPrice().multiply(new BigDecimal(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public boolean canCancel() {
        return order.getStatus() == OrderStatus.PENDING;
    }
}
```

### DAO (数据访问对象)

```java
// DAO: 数据库操作
@Repository
public interface UserDAO extends JpaRepository<UserPO, Long> {
    UserPO findByUsername(String username);
    List<UserPO> findByEmailLike(String email);
}

// MyBatis 风格
@Mapper
public interface UserMapper {
    UserPO selectById(Long id);
    int insert(UserPO user);
    int update(UserPO user);
    int deleteById(Long id);
}
```

## 转换关系

```
┌─────────────────────────────────────────────────────────────┐
│                    对象转换流程                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   请求流程:                                                  │
│   ┌───────┐      ┌───────┐      ┌───────┐      ┌───────┐  │
│   │  DTO  │ ───→ │  BO   │ ───→ │  PO   │ ───→ │  DB   │  │
│   │ 请求  │      │ 业务  │      │ 持久化 │      │ 数据库 │  │
│   └───────┘      └───────┘      └───────┘      └───────┘  │
│                                                             │
│   响应流程:                                                  │
│   ┌───────┐      ┌───────┐      ┌───────┐      ┌───────┐  │
│   │  VO   │ ←─── │  BO   │ ←─── │  PO   │ ←─── │  DB   │  │
│   │ 展示  │      │ 业务  │      │ 持久化 │      │ 数据库 │  │
│   └───────┘      └───────┘      └───────┘      └───────┘  │
│                                                             │
│   转换工具: MapStruct、BeanUtils、ModelMapper               │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

```java
// MapStruct 转换示例
@Mapper
public interface UserConverter {
    UserConverter INSTANCE = Mappers.getMapper(UserConverter.class);
    
    UserVO poToVo(UserPO po);
    UserPO dtoToPo(UserDTO dto);
    List<UserVO> poListToVoList(List<UserPO> poList);
}

// 使用
UserVO vo = UserConverter.INSTANCE.poToVo(userPO);
```

## 面试回答

### 30秒版本

> - **POJO**: 普通 Java 对象，其他类型的统称
> - **PO/Entity**: 对应数据库表，持久化对象
> - **DTO**: 层间数据传输，屏蔽敏感字段
> - **VO**: 前端展示，可能聚合多个数据
> - **BO**: 封装业务逻辑
> - **DAO**: 封装数据库 CRUD 操作
>
> 转换用 MapStruct 或 BeanUtils。

### 1分钟版本

> **POJO**：普通对象，只有属性和 getter/setter
>
> **PO (Entity)**：
> - 对应数据库表
> - 包含所有字段
>
> **DTO**：
> - 层间传输对象
> - 屏蔽敏感字段（如密码）
>
> **VO**：
> - 前端展示对象
> - 聚合多个 PO 数据
> - 格式化展示
>
> **BO**：
> - 业务对象
> - 封装业务逻辑方法
>
> **DAO**：
> - 数据访问对象
> - 封装 CRUD 操作

---

*关联文档：[java-encapsulation.md](java-encapsulation.md)*
