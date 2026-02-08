# XXL-Job 分布式任务调度方案详解

> 版本: 1.0 | 更新: 2026-02-05
>
> 使用 XXL-Job 解决分布式定时任务重复执行问题

---

## 概述

XXL-Job 是一个分布式任务调度平台，其核心特性之一就是通过调度中心统一调度，从源头上避免定时任务重复执行。

### 核心原理

```
┌─────────────────────────────────────────────────────────────────┐
│                    XXL-Job 架构图                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────┐                                           │
│  │   调度中心        │                                           │
│  │  (Admin Server)  │                                           │
│  │                  │                                           │
│  │  - 任务管理      │                                           │
│  │  - 调度策略      │                                           │
│  │  - 日志监控      │                                           │
│  └────────┬─────────┘                                           │
│           │                                                     │
│           │ 触发任务（只选择一个执行器）                          │
│           ↓                                                     │
│      ┌────┴────┐                                               │
│      │         │                                               │
│  ┌───┴───┐ ┌──┴───┐ ┌───┐                                    │
│  │执行器A │ │执行器B │ │... │  (自动选主，只有一个执行)          │
│  └───────┘ └───────┘ └───┘                                    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 快速开始

### 1. 部署调度中心

#### 下载并初始化数据库

```bash
# 下载 XXL-Job 源码
git clone https://gitee.com/xuxueli0323/xxl-job.git
cd xxl-job/doc/db

# 初始化数据库
mysql -u root -p < tables_xxl_job.sql
```

#### 修改配置

```properties
# application.properties
server.port=8080
spring.datasource.url=jdbc:mysql://localhost:3306/xxl_job?useUnicode=true&characterEncoding=UTF-8
spring.datasource.username=root
spring.datasource.password=your_password
```

#### 启动调度中心

```bash
mvn clean package
java -jar xxl-job-admin/target/xxl-job-admin-2.4.0.jar
```

访问：http://localhost:8080/xxl-job-admin
默认账号：admin/123456

---

### 2. 接入应用

#### 添加依赖

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.xuxueli</groupId>
    <artifactId>xxl-job-core</artifactId>
    <version>2.4.0</version>
</dependency>
```

#### 配置

```yaml
# application.yml
xxl:
  job:
    admin:
      addresses: http://localhost:8080/xxl-job-admin
    executor:
      appname: my-service  # 执行器名称
      address:
      ip:
      port: 9999
      logpath: /data/applogs/xxl-job/jobhandler
      logretentiondays: 30
```

#### 配置类

```java
@Configuration
public class XxlJobConfig {

    @Value("${xxl.job.admin.addresses}")
    private String adminAddresses;

    @Value("${xxl.job.executor.appname}")
    private String appname;

    @Value("${xxl.job.executor.port}")
    private int port;

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        XxlJobSpringExecutor xxlJobExecutor = new XxlJobSpringExecutor();
        xxlJobExecutor.setAdminAddresses(adminAddresses);
        xxlJobExecutor.setAppname(appname);
        xxlJobExecutor.setPort(port);
        return xxlJobExecutor;
    }
}
```

#### 编写任务

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class SampleXxlJob {

    @XxlJob("syncDataJob")
    public void syncData() {
        XxlJobHelper.log("任务开始执行");

        try {
            // 业务逻辑
            doSyncData();

            XxlJobHelper.handleSuccess();
            XxlJobHelper.log("任务执行成功");

        } catch (Exception e) {
            XxlJobHelper.handleFailure("任务执行失败: " + e.getMessage());
            XxlJobHelper.log("任务执行失败", e);
            throw e;
        }
    }

    private void doSyncData() {
        log.info("执行数据同步任务");
        // 实际业务逻辑
    }
}
```

#### 在调度中心配置任务

1. 登录调度中心
2. 执行器管理 → 新增执行器
   - Appname：`my-service`
   - 名称：`我的服务`
3. 任务管理 → 新增任务
   - 执行器：`我的服务`
   - 任务描述：`数据同步任务`
   - 调度类型：`CRON`
   - Cron：`0 */5 * * * ?`
   - 运行模式：`BEAN`
   - JobHandler：`syncDataJob`

---

## 核心功能详解

### 1. 路由策略

XXL-Job 提供多种路由策略，用于选择执行任务的具体实例。

| 路由策略 | 说明 | 适用场景 |
|---------|------|---------|
| **FIRST** | 第一个 | 固定使用第一个机器 |
| **LAST** | 最后一个 | 固定使用最后一个机器 |
| **ROUND** | 轮询 | 平均分配到每个机器 |
| **RANDOM** | 随机 | 随机选择一个机器 |
| **CONSISTENT_HASH** | 一致性哈希 | 相同参数的任务始终路由到同一机器 |
| **LEAST_FREQUENTLY_USED** | 最不经常使用 | 优先使用执行次数少的机器 |
| **LEAST_RECENTLY_USED** | 最最近不经常使用 | 优先使用最久未使用的机器 |
| **FAILOVER** | 故障转移 | 自动转移，失败后重试其他机器 |
| **BUSYOVER** | 忙碌转移 | 转向空闲机器 |

**配置示例**：

```java
@XxlJob("syncDataJob")
public void syncData() {
    // 路由策略在调度中心配置
    // 任务管理 → 任务详情 → 路由策略
    doTask();
}
```

### 2. 任务分片

适用于大数据量场景，将任务拆分到多个实例并行执行。

```java
@Component
@Slf4j
public class ShardingXxlJob {

    @XxlJob("batchProcessJob")
    public void batchProcess() {
        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex(); // 当前分片索引
        int shardTotal = XxlJobHelper.getShardTotal(); // 总分片数

        XxlJobHelper.log("当前分片: {}/{}", shardIndex, shardTotal);

        // 查询数据时使用分片参数
        List<Data> dataList = fetchDataWithSharding(shardIndex, shardTotal);

        for (Data data : dataList) {
            process(data);
        }

        XxlJobHelper.log("分片 {} 处理完成，共处理 {} 条数据", shardIndex, dataList.size());
    }

    private List<Data> fetchDataWithSharding(int shardIndex, int shardTotal) {
        // 使用分片参数查询数据
        // 例如：根据 ID 取模
        // WHERE MOD(id, #{shardTotal}) = #{shardIndex}
        return dataMapper.selectBySharding(shardIndex, shardTotal);
    }
}
```

### 3. 任务依赖

配置任务之间的依赖关系，顺序执行。

```java
@Component
@Slf4j
public class DependentXxlJob {

    @XxlJob("taskA")
    public void taskA() {
        XxlJobHelper.log("执行任务 A");
        // 任务 A 的逻辑
    }

    @XxlJob("taskB")
    public void taskB() {
        XxlJobHelper.log("执行任务 B（依赖任务 A）");
        // 任务 B 的逻辑
    }

    @XxlJob("taskC")
    public void taskC() {
        XxlJobHelper.log("执行任务 C（依赖任务 B）");
        // 任务 C 的逻辑
    }
}
```

**在调度中心配置**：
```
任务管理 → 新增任务 → 子任务ID
- 任务 B 的子任务ID：taskA
- 任务 C 的子任务ID：taskB
```

### 4. 失败重试

```java
@Component
@Slf4j
public class RetryXxlJob {

    @XxlJob("retryJob")
    public void retryJob() {
        XxlJobHelper.log("任务开始执行");

        try {
            doTask();

            // 标记成功
            XxlJobHelper.handleSuccess();

        } catch (Exception e) {
            // 标记失败（会触发重试）
            XxlJobHelper.handleFailure("任务执行失败: " + e.getMessage());

            // 在调度中心配置：任务管理 → 最大重试次数
            throw e;
        }
    }
}
```

**配置重试次数**：
```
任务管理 → 任务详情 → 最大重试次数：3
```

### 5. 阻塞处理策略

当任务执行时间过长，下次调度时间已到时的处理策略。

| 策略 | 说明 | 适用场景 |
|------|------|---------|
| **单机串行** | 等待当前任务完成后，再执行下次调度 | 任务执行时间可预测 |
| **丢弃后续调度** | 后续调度丢弃，直到当前任务完成 | 避免任务堆积 |
| **覆盖之前调度** | 停止当前任务，执行新的调度 | 允许任务中断 |

**配置**：
```
任务管理 → 任务详情 → 阻塞处理策略
```

---

## 高级功能

### 1. 动态参数

```java
@Component
@Slf4j
public class DynamicParamXxlJob {

    @XxlJob("dynamicParamJob")
    public void dynamicParamJob() {
        // 获取任务参数
        String param = XxlJobHelper.getJobParam();
        XxlJobHelper.log("任务参数: {}", param);

        // 解析 JSON 参数
        JSONObject paramJson = JSON.parseObject(param);
        String date = paramJson.getString("date");
        int limit = paramJson.getIntValue("limit");

        // 使用参数执行任务
        doTask(date, limit);
    }
}
```

**在调度中心配置参数**：
```
任务管理 → 任务详情 → 任务参数
{
  "date": "2026-02-05",
  "limit": 1000
}
```

### 2. 任务日志

```java
@Component
@Slf4j
public class LogXxlJob {

    @XxlJob("logJob")
    public void logJob() {
        // 普通日志（会记录到调度中心）
        XxlJobHelper.log("任务开始");

        // 携带参数的日志
        XxlJobHelper.log("处理用户: {}, 数量: {}", "user123", 100);

        // 异常日志
        try {
            doTask();
        } catch (Exception e) {
            XxlJobHelper.log("任务失败", e);
            throw e;
        }

        // 记录执行结果
        XxlJobHelper.log("任务完成，处理了 {} 条数据", 100);
    }
}
```

**查看日志**：
```
调度中心 → 任务管理 → 操作 → 查看日志
```

### 3. 邮件告警

```java
@Component
@Slf4j
public class AlertXxlJob {

    @XxlJob("alertJob")
    public void alertJob() {
        XxlJobHelper.log("任务开始");

        try {
            doTask();
            XxlJobHelper.handleSuccess();

        } catch (Exception e) {
            XxlJobHelper.handleFailure("任务失败: " + e.getMessage());

            // 在调度中心配置：任务管理 → 告警邮件
            // 配置收件人列表，任务失败时自动发送邮件
            throw e;
        }
    }
}
```

**配置告警**：
```
调度中心 → 任务管理 → 任务详情 → 告警邮件
勾选：任务失败邮件告警
配置：邮件收件人列表（多个用逗号分隔）
```

### 4. 执行器注册

```java
@Component
@Slf4j
public class RegistryXxlJob {

    @XxlJob("registryJob")
    public void registryJob() {
        // 获取执行器信息
        String appName = XxlJobHelper.getJobParam();
        XxlJobHelper.log("执行器名称: {}", appName);

        // 获取当前执行器列表
        // 调度中心 → 执行器管理 → 查看在线机器
    }
}
```

---

## 最佳实践

### 1. 任务幂等性设计

即使 XXL-Job 保证了调度层面的单实例执行，任务本身仍应设计为幂等。

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotentXxlJob {

    private final TaskRecordMapper taskRecordMapper;

    @XxlJob("idempotentJob")
    public void idempotentJob() {
        String taskId = "task-" + LocalDate.now();

        try {
            // 双重保险：数据库幂等
            TaskRecord record = new TaskRecord();
            record.setTaskId(taskId);
            taskRecordMapper.insert(record);

            doTask();

        } catch (DuplicateKeyException e) {
            XxlJobHelper.log("任务已执行，跳过");
        }
    }
}
```

### 2. 任务超时处理

```java
@Component
@Slf4j
public class TimeoutXxlJob {

    @XxlJob("timeoutJob")
    public void timeoutJob() {
        long startTime = System.currentTimeMillis();
        XxlJobHelper.log("任务开始");

        try {
            // 执行任务
            Future<?> future = CompletableFuture.runAsync(this::doTask);

            // 设置超时
            future.get(10, TimeUnit.MINUTES);

            long duration = System.currentTimeMillis() - startTime;
            XxlJobHelper.log("任务完成，耗时: {}ms", duration);

        } catch (TimeoutException e) {
            XxlJobHelper.log("任务超时", e);
            XxlJobHelper.handleFailure("任务超时");

        } catch (Exception e) {
            XxlJobHelper.log("任务失败", e);
            XxlJobHelper.handleFailure(e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
```

### 3. 分片任务最佳实践

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class ShardingBestPractice {

    private final DataMapper dataMapper;
    private final DataProcessor dataProcessor;

    @XxlJob("shardingJob")
    public void shardingJob() {
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();

        XxlJobHelper.log("分片 {}/{} 开始", shardIndex, shardTotal);

        long startTime = System.currentTimeMillis();
        int processedCount = 0;

        try {
            // 1. 分批查询，避免 OOM
            int batchSize = 1000;
            int offset = 0;

            while (true) {
                List<Data> batch = dataMapper.selectByShardingWithLimit(
                    shardIndex, shardTotal, offset, batchSize
                );

                if (batch.isEmpty()) {
                    break;
                }

                // 2. 批量处理
                for (Data data : batch) {
                    dataProcessor.process(data);
                    processedCount++;

                    // 3. 进度日志
                    if (processedCount % 100 == 0) {
                        XxlJobHelper.log("分片 {} 已处理 {} 条", shardIndex, processedCount);
                    }
                }

                offset += batchSize;
            }

            long duration = System.currentTimeMillis() - startTime;
            XxlJobHelper.handleSuccess();
            XxlJobHelper.log("分片 {} 完成，共处理 {} 条，耗时 {}ms",
                shardIndex, processedCount, duration);

        } catch (Exception e) {
            XxlJobHelper.log("分片 {} 失败", shardIndex, e);
            XxlJobHelper.handleFailure(e.getMessage());
            throw e;
        }
    }
}
```

### 4. 监控指标收集

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class MetricsXxlJob {

    private final MetricsService metricsService;

    @XxlJob("metricsJob")
    public void metricsJob() {
        String taskName = "metricsJob";
        long startTime = System.currentTimeMillis();

        try {
            // 执行任务
            int result = doTask();

            // 记录成功指标
            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordSuccess(taskName, duration);
            metricsService.recordProcessedCount(taskName, result);

            XxlJobHelper.log("任务成功，耗时: {}ms, 处理: {} 条", duration, result);

        } catch (Exception e) {
            // 记录失败指标
            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordFailure(taskName, duration);

            XxlJobHelper.log("任务失败", e);
            XxlJobHelper.handleFailure(e.getMessage());
            throw e;
        }
    }
}
```

---

## 运维管理

### 1. 任务管理 API

XXL-Job 提供了 REST API，可以通过 API 管理任务。

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class XxlJobApiClient {

    public void triggerJob(int jobId, String param) {
        String url = "http://localhost:8080/xxl-job-admin/jobinfo/trigger";

        Map<String, Object> params = new HashMap<>();
        params.put("id", jobId);
        params.put("executorParam", param);

        // 发送 HTTP 请求
        RestTemplate restTemplate = new RestTemplate();
        String result = restTemplate.postForObject(url, params, String.class);

        log.info("触发任务结果: {}", result);
    }

    public void stopJob(int jobId) {
        String url = "http://localhost:8080/xxl-job-admin/jobinfo/stop";

        Map<String, Object> params = new HashMap<>();
        params.put("id", jobId);

        RestTemplate restTemplate = new RestTemplate();
        String result = restTemplate.postForObject(url, params, String.class);

        log.info("停止任务结果: {}", result);
    }
}
```

### 2. 任务执行监控

```java
@Component
@Slf4j
public class JobMonitorService {

    @Scheduled(cron = "0 */5 * * * ?")
    public void monitorJobs() {
        // 查询执行中的任务
        List<RunningTask> runningTasks = getRunningTasks();

        for (RunningTask task : runningTasks) {
            long runningMinutes = Duration.between(
                task.getStartTime(),
                LocalDateTime.now()
            ).toMinutes();

            if (runningMinutes > 60) {
                log.warn("任务执行时间过长: {}, 已运行: {} 分钟",
                    task.getJobName(), runningMinutes);

                // 发送告警
                sendAlert(task);
            }
        }
    }

    private List<RunningTask> getRunningTasks() {
        // 从 XXL-Job API 获取
        // GET /jobinfo?pageNum=1&pageSize=100&triggerStatus=1
        return Collections.emptyList();
    }

    private void sendAlert(RunningTask task) {
        // 发送告警通知
    }
}
```

---

## 优缺点总结

### 优点

| 优点 | 说明 |
|------|------|
| ✅ 调度层面避免重复 | 调度中心统一调度，源头上保证单实例执行 |
| ✅ 可视化管理 | Web 界面管理任务，查看日志 |
| ✅ 功能完善 | 支持任务依赖、失败重试、邮件告警 |
| ✅ 支持任务分片 | 大数据量场景下并行处理 |
| ✅ 路由策略丰富 | 多种路由策略适应不同场景 |
| ✅ 高可用 | 调度中心集群部署，执行器故障转移 |
| ✅ 易于监控 | 统一的日志查看和性能监控 |

### 缺点

| 缺点 | 解决方案 |
|------|---------|
| ❌ 需要独立部署调度中心 | 使用 Docker 部署简化运维 |
| ❌ 有一定学习成本 | 阅读官方文档，参考示例 |
| ❌ 依赖外部系统 | 调度中心高可用部署 |

---

## 适用场景

| 场景 | 是否推荐 | 原因 |
|------|---------|------|
| 企业级应用 | ✅ 强烈推荐 | 功能完善，易于管理 |
| 定时任务数量多（>10个） | ✅ 强烈推荐 | 统一管理，降低维护成本 |
| 需要可视化管理 | ✅ 强烈推荐 | Web 界面友好 |
| 需要任务监控告警 | ✅ 强烈推荐 | 内置告警功能 |
| 大数据量处理 | ✅ 强烈推荐 | 支持任务分片 |
| 简单的定时任务 | ⚠️ 可选 | 可能过度设计 |
| 单体应用 | ⚠️ 可选 | 可用简单方案 |

---

## 参考资料

- XXL-Job 官方文档：https://www.xuxueli.com/xxl-job
- XXL-Job GitHub：https://github.com/xuxueli/xxl-job
