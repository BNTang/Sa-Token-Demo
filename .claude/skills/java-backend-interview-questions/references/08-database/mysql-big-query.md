# MySQL SELECT * 大表查询内存问题

> 分类: MySQL | 难度: ⭐⭐⭐⭐ | 频率: 高频

---

## 一、问题分析

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          问题: SELECT * FROM 大表 会导致内存飙升吗?               │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  答案: 不一定，取决于客户端的读取方式                                            │
│                                                                                  │
│  MySQL 服务端:                                                                   │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │  • MySQL 不会一次性把 1000 万行加载到内存                                   │ │
│  │  • 边查询边发送给客户端                                                     │ │
│  │  • 使用 net_buffer (默认 16KB) 作为发送缓冲区                               │ │
│  │  • 缓冲区满就 flush 给客户端                                                │ │
│  │  • 服务端内存是可控的                                                       │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  客户端 (关键!):                                                                 │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │  默认行为: 客户端会把全部结果缓存到内存!                                    │ │
│  │                                                                             │ │
│  │  JDBC 默认: 使用 ResultSet 时，默认把全部结果加载到内存                     │ │
│  │  这是客户端内存飙升的真正原因!                                              │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 二、数据传输流程

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          MySQL 查询数据流                                         │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌─────────────┐      ┌─────────────┐      ┌─────────────┐                       │
│  │   MySQL     │      │   网络      │      │   客户端    │                       │
│  │   Server    │ ──→  │   传输      │ ──→  │   (Java)    │                       │
│  └─────────────┘      └─────────────┘      └─────────────┘                       │
│                                                                                  │
│  1. Server 执行查询                                                              │
│  2. 从存储引擎读取一行                                                           │
│  3. 放入 net_buffer (默认16KB)                                                   │
│  4. net_buffer 满了就发送给客户端                                                │
│  5. 循环直到所有行发送完毕                                                       │
│                                                                                  │
│  Server 端内存占用:                                                              │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │  • net_buffer: 16KB                                                         │ │
│  │  • InnoDB Buffer Pool: 用于缓存数据页                                       │ │
│  │  • 不会因为查询 1000 万行而 OOM                                             │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  客户端内存占用:                                                                 │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │  默认 JDBC: 把所有结果缓存到 ResultSet                                      │ │
│  │  1000 万行 × 每行 1KB = 10GB 内存!                                          │ │
│  │  → OOM!                                                                     │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 三、解决方案

### 3.1 流式查询 (Streaming)

```java
/**
 * 方案1: JDBC 流式查询
 */
public void streamQuery() throws SQLException {
    Connection conn = dataSource.getConnection();
    
    // 关键配置
    Statement stmt = conn.createStatement(
        ResultSet.TYPE_FORWARD_ONLY,      // 只能向前滚动
        ResultSet.CONCUR_READ_ONLY        // 只读
    );
    stmt.setFetchSize(Integer.MIN_VALUE);  // MySQL 流式读取的标志
    
    ResultSet rs = stmt.executeQuery("SELECT * FROM big_table");
    
    while (rs.next()) {
        // 每次只读取一行到内存
        processRow(rs);
    }
    
    rs.close();
    stmt.close();
}

/**
 * MyBatis 流式查询
 */
@Select("SELECT * FROM big_table")
@Options(resultSetType = ResultSetType.FORWARD_ONLY, fetchSize = Integer.MIN_VALUE)
@ResultType(Record.class)
void selectBigTable(ResultHandler<Record> handler);

// 使用
mapper.selectBigTable(context -> {
    Record record = context.getResultObject();
    processRecord(record);
});
```

### 3.2 游标查询 (Cursor)

```java
/**
 * 方案2: 游标查询 (MySQL 5.0+)
 */
public void cursorQuery() throws SQLException {
    // JDBC URL 加参数
    String url = "jdbc:mysql://localhost:3306/test?useCursorFetch=true";
    
    Connection conn = DriverManager.getConnection(url, user, password);
    PreparedStatement stmt = conn.prepareStatement("SELECT * FROM big_table");
    stmt.setFetchSize(1000);  // 每次获取 1000 行
    
    ResultSet rs = stmt.executeQuery();
    while (rs.next()) {
        processRow(rs);
    }
}

/**
 * MyBatis 游标查询
 */
@Select("SELECT * FROM big_table")
Cursor<Record> selectBigTableCursor();

// 使用
try (Cursor<Record> cursor = mapper.selectBigTableCursor()) {
    cursor.forEach(record -> {
        processRecord(record);
    });
}
```

### 3.3 分页查询

```java
/**
 * 方案3: 分页查询 (推荐)
 */
public void pageQuery() {
    int pageSize = 1000;
    long lastId = 0;  // 游标分页，避免深分页问题
    
    while (true) {
        // 使用主键游标分页，避免 OFFSET 性能问题
        List<Record> records = jdbcTemplate.query(
            "SELECT * FROM big_table WHERE id > ? ORDER BY id LIMIT ?",
            new Object[]{lastId, pageSize},
            rowMapper
        );
        
        if (records.isEmpty()) {
            break;
        }
        
        for (Record record : records) {
            processRecord(record);
        }
        
        lastId = records.get(records.size() - 1).getId();
    }
}
```

---

## 四、注意事项

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          注意事项                                                 │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  流式查询的限制:                                                                 │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │  1. 必须读取完所有数据才能关闭连接                                          │ │
│  │     查询期间连接被占用，不能执行其他 SQL                                    │ │
│  │                                                                             │ │
│  │  2. 不能使用连接池自动归还                                                  │ │
│  │     需要手动管理连接                                                        │ │
│  │                                                                             │ │
│  │  3. 如果处理很慢，可能超时                                                  │ │
│  │     Server 端等待客户端接收                                                 │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  推荐做法:                                                                       │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │  1. 优先使用分页查询                                                        │ │
│  │     • 用主键游标分页避免深分页                                              │ │
│  │     • 每页处理完就释放内存                                                  │ │
│  │                                                                             │ │
│  │  2. 确实需要全量扫描时用流式/游标                                           │ │
│  │     • 如数据导出、数据迁移                                                  │ │
│  │                                                                             │ │
│  │  3. 禁止在 OLTP 系统执行全表扫描                                            │ │
│  │     • 影响正常业务                                                          │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 五、面试回答

### 30秒版本

> **MySQL 服务端不会 OOM**：边查边发，用 net_buffer（16KB）缓冲。
>
> **客户端可能 OOM**：JDBC 默认把全部结果缓存到内存。
>
> **解决方案**：
> 1. **流式查询**：`setFetchSize(Integer.MIN_VALUE)`，一行一行读
> 2. **游标查询**：`useCursorFetch=true`，每次读 N 行
> 3. **分页查询**：用主键游标分页，避免深分页问题
>
> **推荐分页查询**，流式查询会长时间占用连接。

### 1分钟版本

> **服务端内存**：
> MySQL 不会一次性加载 1000 万行到内存。执行时边查边通过 net_buffer（默认 16KB）发送给客户端，缓冲区满就 flush。所以服务端内存是可控的。
>
> **客户端内存（关键）**：
> JDBC 默认行为是把全部结果缓存到 ResultSet，这才是 OOM 的原因！1000 万行可能占用几 GB 内存。
>
> **解决方案**：
>
> 1. **流式查询**
>    - `setFetchSize(Integer.MIN_VALUE)` + `TYPE_FORWARD_ONLY`
>    - 一次只读一行到内存
>    - 缺点：查询期间连接被占用
>
> 2. **游标查询**
>    - URL 加 `useCursorFetch=true`，设置 `fetchSize=1000`
>    - 每次读取 1000 行
>
> 3. **分页查询（推荐）**
>    - 用主键游标分页：`WHERE id > #{lastId} LIMIT 1000`
>    - 避免 `OFFSET` 深分页问题
>    - 每页处理完释放内存
>
> **最佳实践**：
> - OLTP 系统禁止全表扫描
> - 数据导出/迁移用流式或游标
> - 一般场景用分页查询
