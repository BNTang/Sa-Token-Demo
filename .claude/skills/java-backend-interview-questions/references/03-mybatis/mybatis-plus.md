# MyBatis-Plus 详解

> 分类: ORM框架 | 难度: ⭐⭐ | 频率: 高频

---

## 一、什么是 MyBatis-Plus

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          MyBatis-Plus 定义                                        │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  MyBatis-Plus (MP) 是 MyBatis 的增强工具，在 MyBatis 的基础上只做增强不做改变。  │
│  为简化开发、提高效率而生。                                                       │
│                                                                                  │
│  核心理念: 只做增强不做改变，引入后不影响原有的 MyBatis 代码                      │
│                                                                                  │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │                                                                            │ │
│  │       MyBatis-Plus                                                         │ │
│  │  ┌─────────────────────────────────────────────────────────────────────┐  │ │
│  │  │  • 通用 CRUD                                                         │  │ │
│  │  │  • 条件构造器 (Wrapper)                                              │  │ │
│  │  │  • 分页插件                                                          │  │ │
│  │  │  • 代码生成器                                                        │  │ │
│  │  │  • 乐观锁插件                                                        │  │ │
│  │  │  • 多租户插件                                                        │  │ │
│  │  └─────────────────────────────────────────────────────────────────────┘  │ │
│  │                          ↓ 增强                                            │ │
│  │  ┌─────────────────────────────────────────────────────────────────────┐  │ │
│  │  │                      MyBatis                                         │  │ │
│  │  └─────────────────────────────────────────────────────────────────────┘  │ │
│  │                          ↓                                                 │ │
│  │  ┌─────────────────────────────────────────────────────────────────────┐  │ │
│  │  │                      JDBC                                            │  │ │
│  │  └─────────────────────────────────────────────────────────────────────┘  │ │
│  │                                                                            │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 二、核心功能

### 2.1 通用 CRUD

```java
/**
 * 继承 BaseMapper 即可获得通用 CRUD 方法
 */
public interface UserMapper extends BaseMapper<User> {
    // 无需编写任何 CRUD 方法，BaseMapper 已提供
}

// 使用示例
@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;
    
    public void demo() {
        // 插入
        User user = new User("张三", 28);
        userMapper.insert(user);  // 自动填充ID
        
        // 根据ID查询
        User u = userMapper.selectById(1L);
        
        // 查询所有
        List<User> list = userMapper.selectList(null);
        
        // 根据ID更新
        user.setAge(30);
        userMapper.updateById(user);
        
        // 根据ID删除
        userMapper.deleteById(1L);
        
        // 批量操作
        userMapper.selectBatchIds(Arrays.asList(1L, 2L, 3L));
        userMapper.deleteBatchIds(Arrays.asList(1L, 2L, 3L));
    }
}
```

### 2.2 条件构造器

```java
/**
 * 使用 Wrapper 构建复杂查询条件
 */
public class WrapperExample {
    @Autowired
    private UserMapper userMapper;
    
    // QueryWrapper 查询条件构造器
    public List<User> queryExample() {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1)           // status = 1
               .like("name", "张")         // name LIKE '%张%'
               .ge("age", 18)              // age >= 18
               .lt("age", 60)              // age < 60
               .orderByDesc("create_time") // ORDER BY create_time DESC
               .select("id", "name", "age"); // 只查询指定字段
        
        return userMapper.selectList(wrapper);
    }
    
    // LambdaQueryWrapper (推荐，类型安全)
    public List<User> lambdaExample() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getStatus, 1)    // 使用方法引用，避免硬编码
               .like(User::getName, "张")
               .between(User::getAge, 18, 60);
        
        return userMapper.selectList(wrapper);
    }
    
    // UpdateWrapper 更新条件构造器
    public void updateExample() {
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(User::getId, 1L)
               .set(User::getName, "李四")
               .set(User::getAge, 30);
        
        userMapper.update(null, wrapper);
    }
}
```

### 2.3 分页插件

```java
/**
 * 配置分页插件
 */
@Configuration
public class MybatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 添加分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}

/**
 * 使用分页
 */
@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;
    
    public IPage<User> pageQuery(int pageNum, int pageSize) {
        // 创建分页对象
        Page<User> page = new Page<>(pageNum, pageSize);
        
        // 构建查询条件
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getStatus, 1);
        
        // 执行分页查询
        IPage<User> result = userMapper.selectPage(page, wrapper);
        
        // 获取分页信息
        long total = result.getTotal();      // 总记录数
        long pages = result.getPages();      // 总页数
        List<User> records = result.getRecords();  // 当前页数据
        
        return result;
    }
}
```

---

## 三、常用注解

```java
/**
 * MyBatis-Plus 常用注解
 */
@Data
@TableName("t_user")  // 指定表名
public class User {
    
    @TableId(type = IdType.ASSIGN_ID)  // 主键，雪花算法生成
    private Long id;
    
    @TableField("user_name")  // 指定字段名
    private String name;
    
    private Integer age;
    
    @TableField(fill = FieldFill.INSERT)  // 插入时自动填充
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)  // 插入和更新时自动填充
    private LocalDateTime updateTime;
    
    @TableLogic  // 逻辑删除字段
    private Integer deleted;
    
    @Version  // 乐观锁版本号
    private Integer version;
    
    @TableField(exist = false)  // 非数据库字段
    private String remark;
}
```

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          常用注解说明                                             │
├─────────────────────┬────────────────────────────────────────────────────────────┤
│  @TableName         │  指定实体类对应的表名                                       │
│  @TableId           │  标识主键，type指定生成策略                                 │
│  @TableField        │  指定字段名、自动填充策略、是否为数据库字段等               │
│  @TableLogic        │  逻辑删除字段，删除变更新                                   │
│  @Version           │  乐观锁版本号字段                                           │
└─────────────────────┴────────────────────────────────────────────────────────────┘

主键生成策略 (IdType):
• AUTO - 数据库自增
• ASSIGN_ID - 雪花算法 (推荐)
• ASSIGN_UUID - UUID
• INPUT - 手动输入
```

---

## 四、高级功能

### 4.1 自动填充

```java
/**
 * 自动填充处理器
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    
    @Override
    public void insertFill(MetaObject metaObject) {
        // 插入时自动填充
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
    
    @Override
    public void updateFill(MetaObject metaObject) {
        // 更新时自动填充
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
```

### 4.2 乐观锁

```java
/**
 * 配置乐观锁插件
 */
@Configuration
public class MybatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }
}

/**
 * 使用乐观锁
 */
@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;
    
    public void updateWithLock(Long id) {
        // 1. 先查询，获取version
        User user = userMapper.selectById(id);
        
        // 2. 更新
        user.setAge(30);
        int rows = userMapper.updateById(user);
        // UPDATE t_user SET age=30, version=version+1 
        // WHERE id=1 AND version=1
        
        if (rows == 0) {
            // 更新失败，可能被其他线程修改
            throw new RuntimeException("并发更新冲突");
        }
    }
}
```

### 4.3 逻辑删除

```yaml
# application.yml
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: deleted  # 全局逻辑删除字段
      logic-delete-value: 1        # 删除值
      logic-not-delete-value: 0    # 未删除值
```

```java
// 调用 delete 方法实际执行 update
userMapper.deleteById(1L);
// 实际SQL: UPDATE t_user SET deleted=1 WHERE id=1

// 查询自动过滤已删除数据
userMapper.selectList(null);
// 实际SQL: SELECT * FROM t_user WHERE deleted=0
```

---

## 五、MyBatis vs MyBatis-Plus

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                    MyBatis vs MyBatis-Plus                                        │
├─────────────────┬────────────────────────────┬───────────────────────────────────┤
│                 │       MyBatis              │        MyBatis-Plus               │
├─────────────────┼────────────────────────────┼───────────────────────────────────┤
│  CRUD           │  需要手写 SQL              │  通用方法，无需手写               │
│  条件查询       │  手写动态 SQL              │  Wrapper 构造器                   │
│  分页           │  需要PageHelper插件        │  内置分页插件                     │
│  代码生成       │  需要第三方工具            │  内置生成器                       │
│  乐观锁         │  需要手动实现              │  @Version 注解                    │
│  逻辑删除       │  需要手动实现              │  @TableLogic 注解                 │
│  学习成本       │  低                        │  略高（需学习 MP 特性）            │
│  灵活性         │  高                        │  高（可与 MyBatis 混用）           │
└─────────────────┴────────────────────────────┴───────────────────────────────────┘
```

---

## 六、面试回答

### 30秒版本

> MyBatis-Plus 是 MyBatis 的增强工具，在不改变 MyBatis 的基础上提供增强功能：
> - **通用 CRUD**：继承 BaseMapper 即可使用
> - **条件构造器**：Wrapper 链式构建查询条件
> - **分页插件**：内置分页，无需额外配置
> - **代码生成器**：一键生成 Entity、Mapper、Service
> - **注解支持**：@TableLogic 逻辑删除、@Version 乐观锁
>
> 核心理念是"只做增强不做改变"。

### 1分钟版本

> **什么是 MyBatis-Plus：**
> 是 MyBatis 的增强工具，在 MyBatis 基础上只做增强不做改变，完全兼容 MyBatis 的所有功能。
>
> **核心功能：**
> 1. **通用 CRUD**：继承 BaseMapper 接口即可获得 insert、delete、update、select 等方法
> 2. **条件构造器**：使用 QueryWrapper、LambdaQueryWrapper 链式构建复杂查询条件
> 3. **分页插件**：配置 PaginationInnerInterceptor 后使用 Page 对象即可分页
> 4. **自动填充**：实现 MetaObjectHandler 接口，自动填充创建时间、更新时间
> 5. **逻辑删除**：@TableLogic 注解标记，delete 变 update
> 6. **乐观锁**：@Version 注解，自动维护版本号
>
> **使用建议：**
> - 简单 CRUD 用 MP 自带方法
> - 复杂 SQL 仍可写 XML（与 MyBatis 混用）
> - 推荐使用 LambdaQueryWrapper 避免硬编码字段名

---

## 七、代码示例

### 7.1 完整使用示例

```java
/**
 * 实体类
 */
@Data
@TableName("t_user")
public class User {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private Integer age;
    private String email;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableLogic
    private Integer deleted;
}

/**
 * Mapper 接口
 */
public interface UserMapper extends BaseMapper<User> {
    // 继承 BaseMapper 即可，无需定义方法
    
    // 如需自定义 SQL，仍可使用 @Select 或 XML
    @Select("SELECT * FROM t_user WHERE age > #{age}")
    List<User> selectByAge(@Param("age") Integer age);
}

/**
 * Service 实现
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> 
        implements UserService {
    
    public PageResult<User> pageQuery(UserQueryDTO dto) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        
        // 条件判断，非空才加入条件
        wrapper.like(StringUtils.isNotBlank(dto.getName()), User::getName, dto.getName())
               .ge(dto.getMinAge() != null, User::getAge, dto.getMinAge())
               .le(dto.getMaxAge() != null, User::getAge, dto.getMaxAge())
               .orderByDesc(User::getCreateTime);
        
        Page<User> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        IPage<User> result = this.page(page, wrapper);
        
        return new PageResult<>(result.getTotal(), result.getRecords());
    }
}
```
