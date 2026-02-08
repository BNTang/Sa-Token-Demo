# 运维手册规范

> Java Spring Boot 项目文档生成器 - 运维内容规范

---

## 内容结构

运维手册应包含以下章节：

```markdown
# 运维手册

## 1. 服务管理
## 2. 日志管理
## 3. 监控与告警
## 4. 故障排查
## 5. 备份恢复
## 6. 性能调优
```

---

## 1. 服务管理

### 1.1 启动停止

| 操作 | 命令 |
|------|------|
| 启动 | `./scripts/start.sh` |
| 停止 | `./scripts/stop.sh` |
| 重启 | `./scripts/restart.sh` |
| 查看状态 | `ps -ef \| grep app.jar` |

### 1.2 进程管理

```bash
# 查找进程
ps -ef | grep ${projectName}.jar

# 查看进程详情
ps -fp <PID>

# 优雅停止（推荐）
kill -15 <PID>

# 强制停止
kill -9 <PID>
```

### 1.3 端口检查

```bash
# 检查端口是否占用
netstat -tuln | grep <port>

# 查看端口占用进程
lsof -i :<port>

# 或使用 ss 命令
ss -tuln | grep <port>
```

---

## 2. 日志管理

### 2.1 日志位置

| 日志类型 | 默认位置 |
|---------|---------|
| 应用日志 | `logs/app.log` |
| 错误日志 | `logs/error.log` |
| 访问日志 | `logs/access.log`（如果启用） |

### 2.2 日志配置

`application.yml` 中的日志配置：

```yaml
logging:
  file:
    name: logs/app.log           # 日志文件位置
  level:
    root: INFO                    # 全局日志级别
    com.example: DEBUG            # 包级别日志
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### 2.3 日志查看命令

```bash
# 实时查看日志
tail -f logs/app.log

# 查看最近 100 行
tail -n 100 logs/app.log

# 查看错误日志
grep ERROR logs/app.log

# 按时间过滤
grep "2026-01-28 10:" logs/app.log

# 查看特定模块日志
grep "\[订单\]" logs/app.log
```

### 2.4 日志轮转

```yaml
logging:
  logback:
    rollingpolicy:
      max-file-size: 100MB        # 单文件最大大小
      max-history: 30             # 保留天数
      total-size-cap: 10GB        # 总大小限制
```

---

## 3. 监控与告警

### 3.1 健康检查端点

Spring Boot Actuator 端点：

| 端点 | 路径 | 说明 |
|------|------|------|
| 健康检查 | `/actuator/health` | 服务健康状态 |
| 信息 | `/actuator/info` | 应用信息 |
| 指标 | `/actuator/metrics` | 性能指标 |
| 线程转储 | `/actuator/threaddump` | 线程信息 |

### 3.2 关键监控指标

| 指标 | 说明 | 告警阈值 |
|------|------|---------|
| JVM 内存使用率 | 堆内存使用比例 | > 80% |
| JVM GC 时间 | GC 耗时占比 | > 10% |
| 线程数 | 活跃线程数量 | > 200 |
| 响应时间 | 接口平均响应时间 | > 1000ms |
| 错误率 | 接口错误比例 | > 1% |
| QPS | 每秒请求数 | - |

### 3.3 Prometheus 配置（如果集成）

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    tags:
      application: ${spring.application.name}
```

### 3.4 告警配置建议

| 告警项 | 条件 | 级别 | 处理建议 |
|--------|------|------|---------|
| 服务下线 | 健康检查失败 | P0 | 立即检查服务状态 |
| 内存溢出 | JVM 内存 > 90% | P0 | 扩容或排查内存泄漏 |
| GC 频繁 | GC 时间 > 30% | P1 | 优化内存或扩容 |
| 接口超时 | 响应时间 > 3s | P1 | 检查依赖服务 |
| 错误率上升 | 错误率 > 5% | P2 | 查看日志排查问题 |

---

## 4. 故障排查

### 4.1 常见问题

| 问题 | 现象 | 排查步骤 | 解决方案 |
|------|------|---------|---------|
| 服务无法启动 | 启动报错 | 查看启动日志 | 检查端口占用、配置文件 |
| 接口超时 | 响应慢 | 查看日志、检查数据库 | 优化 SQL、增加连接池 |
| 内存溢出 | OOM 错误 | 查看 dump 文件 | 分析内存泄漏、扩容 |
| 连接池耗尽 | 获取连接超时 | 查看连接池监控 | 增加连接池大小 |
| 缓存失效 | Redis 连接失败 | 检查 Redis 状态 | 重启 Redis 或检查网络 |

### 4.2 问题诊断流程

```
1. 确认问题范围
   ├─ 单个用户 vs 全部用户
   ├─ 单个功能 vs 全部功能
   └─ 单个实例 vs 全部实例

2. 收集信息
   ├─ 查看应用日志
   ├─ 查看系统资源 (CPU/内存/磁盘/网络)
   ├─ 查看依赖服务状态
   └─ 查看监控指标

3. 定位问题
   ├─ 应用层：代码逻辑、配置
   ├─ 中间件：数据库、Redis、MQ
   ├─ 网络层：防火墙、负载均衡
   └─ 系统层：资源不足

4. 解决问题
   ├─ 紧急恢复：重启、回滚、扩容
   └─ 根本修复：代码修复、配置优化
```

### 4.3 日志关键词

| 关键词 | 含义 | 排查方向 |
|--------|------|---------|
| `OutOfMemoryError` | 内存溢出 | 内存泄漏、扩容 |
| `ConnectionTimeout` | 连接超时 | 网络检查、依赖服务 |
| `SQLException` | 数据库异常 | SQL 优化、连接池 |
| `TimeoutException` | 调用超时 | 接口优化、依赖服务 |

---

## 5. 备份恢复

### 5.1 数据备份

#### 数据库备份

```bash
# MySQL 备份
mysqldump -u root -p dbname > backup_$(date +%Y%m%d).sql

# MySQL 恢复
mysql -u root -p dbname < backup_20260128.sql

# 定时备份（crontab）
0 2 * * * mysqldump -u root -pdbname dbname > /backup/db_$(date +\%Y\%m\%d).sql
```

#### Redis 备份

```bash
# 手动触发 RDB 快照
redis-cli BGSAVE

# 检查备份文件
ls -lh /var/lib/redis/dump.rdb
```

### 5.2 配置备份

需要备份的配置文件：

| 文件 | 位置 |
|------|------|
| 应用配置 | `application-*.yml` |
| 日志配置 | `logback-spring.xml` |
| 启停脚本 | `scripts/*.sh` |

### 5.3 恢复流程

```
1. 停止应用服务
2. 恢复数据库数据
3. 恢复配置文件
4. 恢复 Redis 数据（如果需要）
5. 验证配置
6. 启动应用服务
7. 验证功能
```

---

## 6. 性能调优

### 6.1 JVM 参数建议

根据不同内存规格配置：

| 内存 | Xms | Xmx | MetaspaceSize | MaxMetaspaceSize |
|------|-----|-----|---------------|-----------------|
| 1G | 256m | 512m | 128m | 256m |
| 2G | 512m | 1024m | 128m | 256m |
| 4G | 1024m | 2048m | 256m | 512m |
| 8G | 2048m | 4096m | 256m | 512m |

推荐 JVM 参数：

```bash
java -Xms512m -Xmx1024m \
     -XX:MetaspaceSize=128m \
     -XX:MaxMetaspaceSize=256m \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/logs/heapdump.hprof \
     -jar app.jar
```

### 6.2 连接池配置

```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 10           # 最小空闲连接
      maximum-pool-size: 50       # 最大连接数
      connection-timeout: 30000   # 连接超时（毫秒）
      idle-timeout: 600000        # 空闲超时（10分钟）
      max-lifetime: 1800000       # 连接最大生命周期（30分钟）

  redis:
    lettuce:
      pool:
        min-idle: 5
        max-idle: 20
        max-active: 50
        max-wait: 3000
```

### 6.3 缓存配置

```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 3600000       # 默认缓存 1 小时
      cache-null-values: true     # 缓存空值防穿透
```

### 6.4 线程池配置

```yaml
spring:
  task:
    execution:
      pool:
        core-size: 8              # 核心线程数
        max-size: 16              # 最大线程数
        queue-capacity: 1000      # 队列容量
        keep-alive: 60s           # 线程存活时间
```

---

## 输出文档模板

生成的 `docs/operations.md` 按照以上章节结构组织，根据项目实际情况填写具体内容。
