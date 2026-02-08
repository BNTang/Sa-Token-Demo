# 运维手册模板

> 项目名称：${projectName}
> 生成时间：${timestamp}

---

## 1. 服务管理

### 1.1 启动停止

| 操作 | 命令 | 说明 |
|------|------|------|
| 启动 | `./scripts/start.sh` | 后台启动服务 |
| 停止 | `./scripts/stop.sh` | 优雅停止服务 |
| 重启 | `./scripts/restart.sh` | 重启服务 |
| 查看状态 | `ps -ef \| grep ${projectName}.jar` | 查看进程状态 |

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
netstat -tuln | grep ${port}

# 查看端口占用进程
lsof -i :${port}

# 或使用 ss 命令
ss -tuln | grep ${port}
```

---

## 2. 日志管理

### 2.1 日志位置

| 日志类型 | 位置 | 说明 |
|---------|------|------|
| 应用日志 | `logs/app.log` | 应用运行日志 |
| 错误日志 | `logs/error.log` | ERROR 级别日志 |
| 访问日志 | `logs/access.log` | HTTP 请求日志（如果启用） |

### 2.2 日志配置

当前日志配置（`application.yml`）：

```yaml
logging:
  file:
    name: logs/app.log
  level:
    root: INFO
    ${package}: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### 2.3 日志级别

| 级别 | 用途 |
|------|------|
| ERROR | 错误信息，需要关注 |
| WARN | 警告信息，可能存在问题 |
| INFO | 关键业务流程信息 |
| DEBUG | 调试信息，开发环境使用 |

**调整日志级别**：

```yaml
# 临时调整（通过启动参数）
java -jar app.jar --logging.level.com.example=DEBUG

# 永久调整（修改配置文件）
# 编辑 application.yml
logging:
  level:
    com.example: DEBUG
```

### 2.4 日志查看命令

```bash
# 实时查看日志
tail -f logs/app.log

# 查看最近 100 行
tail -n 100 logs/app.log

# 查看错误日志
grep ERROR logs/app.log

# 按时间过滤
grep "2026-01-28 10:" logs/app.log

# 查看特定模块日志（带业务标识）
grep "\[${businessTag}\]" logs/app.log

# 统计错误数量
grep -c ERROR logs/app.log

# 查看最近的异常堆栈
grep -A 20 "Exception" logs/app.log | tail -n 50
```

### 2.5 日志轮转

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

| 端点 | 路径 | 说明 |
|------|------|------|
| 健康检查 | `/actuator/health` | 服务健康状态 |
| 信息 | `/actuator/info` | 应用信息 |
| 指标 | `/actuator/metrics` | 性能指标 |
| 线程转储 | `/actuator/threaddump` | 线程信息 |
| 环境变量 | `/actuator/env` | 环境配置 |

**访问示例**：

```bash
# 健康检查
curl http://localhost:${port}/actuator/health

# 查看指标列表
curl http://localhost:${port}/actuator/metrics

# 查看具体指标
curl http://localhost:${port}/actuator/metrics/jvm.memory.used
```

### 3.2 关键监控指标

| 指标 | 说明 | 告警阈值 | 获取方式 |
|------|------|---------|---------|
| JVM 内存使用率 | 堆内存使用比例 | > 80% | `/actuator/metrics/jvm.memory.used` |
| JVM GC 时间 | GC 耗时占比 | > 10% | `/actuator/metrics/jvm.gc.pause` |
| 线程数 | 活跃线程数量 | > 200 | `/actuator/metrics/jvm.threads.live` |
| 响应时间 | 接口平均响应时间 | > 1000ms | 应用监控 |
| 错误率 | 接口错误比例 | > 1% | 应用监控 |
| QPS | 每秒请求数 | - | 应用监控 |

### 3.3 Prometheus 配置

如果项目集成了 Prometheus，可通过以下方式暴露指标：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    tags:
      application: ${projectName}
```

Prometheus 访问地址：
```
http://localhost:${port}/actuator/prometheus
```

### 3.4 告警配置建议

| 告警项 | 条件 | 级别 | 处理建议 |
|--------|------|------|---------|
| 服务下线 | 健康检查失败连续 3 次 | P0 | 立即检查服务状态、日志 |
| 内存溢出 | JVM 内存使用率 > 90% | P0 | 分析内存泄漏、扩容 |
| GC 频繁 | GC 时间占比 > 30% | P1 | 优化内存分配、扩容 |
| 接口超时 | 响应时间 > 3s 连续 1 分钟 | P1 | 检查依赖服务、优化代码 |
| 错误率上升 | 错误率 > 5% 连续 5 分钟 | P2 | 查看日志排查问题 |

---

## 4. 故障排查

### 4.1 常见问题诊断

| 问题 | 现象 | 排查步骤 | 解决方案 |
|------|------|---------|---------|
| **服务无法启动** | 启动报错 | 1. 查看启动日志<br>2. 检查端口占用<br>3. 检查配置文件 | 释放端口、修正配置 |
| **接口响应慢** | 响应时间过长 | 1. 查看应用日志<br>2. 检查数据库慢查询<br>3. 检查依赖服务 | 优化 SQL、增加缓存 |
| **内存溢出** | OOM 错误 | 1. 查看 heap dump<br>2. 分析大对象 | 分析内存泄漏、扩容 |
| **连接池耗尽** | 获取连接超时 | 1. 查看连接池监控<br>2. 检查是否有连接未释放 | 增加连接池、修复代码 |
| **缓存失效** | Redis 连接失败 | 1. 检查 Redis 状态<br>2. 检查网络 | 重启 Redis、检查网络 |

### 4.2 问题诊断流程

```
1. 确认问题范围
   ├─ 单个用户 vs 全部用户
   ├─ 单个功能 vs 全部功能
   └─ 单个实例 vs 全部实例

2. 收集信息
   ├─ 查看应用日志 (logs/app.log)
   ├─ 查看系统资源 (top、vmstat、iostat)
   ├─ 查看依赖服务状态 (数据库、Redis)
   └─ 查看监控指标 (Prometheus、Grafana)

3. 定位问题
   ├─ 应用层：代码逻辑、配置
   ├─ 中间件：数据库、Redis、MQ
   ├─ 网络层：防火墙、负载均衡
   └─ 系统层：CPU、内存、磁盘、网络

4. 解决问题
   ├─ 紧急恢复：重启、回滚、扩容
   └─ 根本修复：代码修复、配置优化
```

### 4.3 日志关键词排查

| 关键词 | 含义 | 排查方向 |
|--------|------|---------|
| `OutOfMemoryError` | 内存溢出 | 内存泄漏分析、扩容 |
| `ConnectionTimeout` | 连接超时 | 网络检查、依赖服务状态 |
| `SQLException` | 数据库异常 | SQL 优化、连接池配置 |
| `TimeoutException` | 调用超时 | 接口性能优化、依赖服务 |
| `NullPointerException` | 空指针异常 | 代码 bug 修复 |

### 4.4 常用诊断命令

```bash
# 查看 CPU 使用率
top -p <PID>

# 查看内存使用
free -h

# 查看磁盘使用
df -h

# 查看磁盘 IO
iostat -x 1

# 查看网络连接
netstat -an | grep ${port}

# 查看 JVM 堆内存
jmap -heap <PID>

# 导出线程堆栈
jstack <PID> > thread_dump.txt

# 导出堆转储
jmap -dump:format=b,file=heap_dump.hprof <PID>
```

---

## 5. 备份恢复

### 5.1 数据备份

#### 数据库备份

```bash
# MySQL 备份
mysqldump -u ${dbUser} -p ${dbName} > backup_$(date +%Y%m%d_%H%M%S).sql

# MySQL 恢复
mysql -u ${dbUser} -p ${dbName} < backup_20260128.sql

# 定时备份（添加到 crontab）
# 每天凌晨 2 点备份
0 2 * * * mysqldump -u ${dbUser} -p${dbPassword} ${dbName} > /backup/db_$(date +\%Y\%m\%d).sql
```

#### Redis 备份

```bash
# 手动触发 RDB 快照
redis-cli -a ${redisPassword} BGSAVE

# 检查备份文件
ls -lh /var/lib/redis/dump.rdb

# 查看 Redis 备份目录
redis-cli CONFIG GET dir
```

### 5.2 配置备份

需要备份的配置文件：

| 文件 | 位置 | 说明 |
|------|------|------|
| 应用配置 | `application-*.yml` | 环境配置 |
| 日志配置 | `logback-spring.xml` | 日志配置 |
| 启停脚本 | `scripts/*.sh` | 服务脚本 |

### 5.3 恢复流程

```
1. 停止应用服务
   ./scripts/stop.sh

2. 恢复数据库数据
   mysql -u ${dbUser} -p ${dbName} < backup_xxx.sql

3. 恢复配置文件
   cp backup/application-prod.yml config/

4. 恢复 Redis 数据（如果需要）
   # 将备份的 dump.rdb 复制到 Redis 数据目录
   cp backup/dump.rdb /var/lib/redis/
   # 重启 Redis
   systemctl restart redis

5. 验证配置
   cat config/application-prod.yml

6. 启动应用服务
   ./scripts/start.sh

7. 验证功能
   curl http://localhost:${port}/actuator/health
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

**推荐 JVM 参数**：

```bash
# 基础配置
java -Xms512m -Xmx1024m \
     -XX:MetaspaceSize=128m \
     -XX:MaxMetaspaceSize=256m \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -jar app.jar

# 生产环境配置
java -server \
     -Xms1024m -Xmx2048m \
     -XX:MetaspaceSize=256m \
     -XX:MaxMetaspaceSize=512m \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=100 \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/logs/heapdump.hprof \
     -XX:+PrintGCDetails \
     -XX:+PrintGCDateStamps \
     -Xloggc:/logs/gc.log \
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
```

### 6.3 缓存配置

```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 3600000       # 默认缓存 1 小时
      cache-null-values: true     # 缓存空值防穿透
  data:
    redis:
      lettuce:
        pool:
          min-idle: 5
          max-idle: 20
          max-active: 50
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

## 7. 联系方式

| 角色 | 姓名 | 联系方式 |
|------|------|---------|
| 开发负责人 | ${devOwner} | ${devContact} |
| 运维负责人 | ${opsOwner} | ${opsContact} |
| 值班电话 | ${supportPhone} | - |

---

## 8. 附录

### 8.1 相关文档

- [接口设计文档](api-doc.md)
- [部署手册](deployment.md)

### 8.2 相关链接

- Spring Boot 官方文档: https://spring.io/projects/spring-boot
- Knife4j 文档: https://doc.xiaominfo.com/
