# 数据库唯一约束方案详解

> 版本: 1.0 | 更新: 2026-02-05
>
> 使用数据库唯一约束解决定时任务重复执行问题

---

## 概述

数据库唯一约束是解决定时任务重复执行最简单、最可靠的方案之一，特别适合任务本身涉及数据库操作的场景。

### 核心原理

```
┌────────────────────────────────────────────────────────────┐
│          数据库唯一约束工作原理                             │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  实例A      实例B      实例C                               │
│    │          │          │                                │
│    └─ INSERT ─┴─ INSERT ─┘                                │
│         (同时插入)                                         │
│                ↓                                          │
│         数据库唯一索引                                      │
│                ↓                                          │
│         只有第一个 INSERT 成功                              │
│         其他抛出 DuplicateKeyException                     │
│                ↓                                          │
│         实例A 执行任务                                      │
│         实例B/C 捕获异常，跳过                               │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

---

## 方案一：基础唯一约束

### 1. 建表语句

```sql
CREATE TABLE task_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    task_id VARCHAR(128) NOT NULL COMMENT '任务ID',
    task_date DATE NOT NULL COMMENT '任务日期',
    status VARCHAR(32) DEFAULT 'SUCCESS' COMMENT '状态：SUCCESS/FAILED',
    start_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    duration_ms BIGINT COMMENT '执行耗时(毫秒)',
    error_msg TEXT COMMENT '错误信息',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_task (task_date, task_id) COMMENT '任务唯一键',
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务执行记录';
```

**关键设计点**：
- **复合唯一键**：`task_date + task_id`，避免日期冲突
- **状态记录**：记录任务执行状态
- **耗时记录**：便于监控和优化
- **错误信息**：便于问题排查

### 2. 基础实现

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class DbUniqueTaskService {

    private final TaskRecordMapper taskRecordMapper;

    @Scheduled(cron = "0 0 2 * * ?")
    public void dailyReport() {
        String taskId = "daily-report";
        LocalDate today = LocalDate.now();
        String uniqueTaskId = today + ":" + taskId;

        try {
            // 插入任务记录（唯一索引保证幂等）
            TaskRecord record = new TaskRecord();
            record.setTaskId(uniqueTaskId);
            record.setTaskDate(today);
            record.setStatus("RUNNING");
            record.setStartTime(LocalDateTime.now());
            taskRecordMapper.insert(record);

            // 执行任务
            log.info("开始执行每日报表任务");
            doDailyReport();

            // 更新状态
            record.setStatus("SUCCESS");
            record.setEndTime(LocalDateTime.now());
            record.setDurationMs(Duration.between(record.getStartTime(), record.getEndTime()).toMillis());
            taskRecordMapper.updateById(record);

        } catch (DuplicateKeyException e) {
            log.info("任务已在其他实例执行: {}", uniqueTaskId);
        } catch (Exception e) {
            // 任务执行失败
            log.error("任务执行失败: {}", uniqueTaskId, e);
            updateTaskStatus(uniqueTaskId, "FAILED", e.getMessage());
            throw e;
        }
    }

    private void doDailyReport() {
        // 实际业务逻辑
        log.info("生成每日报表...");
    }

    private void updateTaskStatus(String taskId, String status, String errorMsg) {
        TaskRecord record = taskRecordMapper.selectByTaskId(taskId);
        if (record != null) {
            record.setStatus(status);
            record.setEndTime(LocalDateTime.now());
            record.setErrorMsg(errorMsg);
            taskRecordMapper.updateById(record);
        }
    }
}
```

---

## 方案二：状态机模式

更完善的实现，支持任务状态跟踪。

### 1. 枚举定义

```java
@Getter
@AllArgsConstructor
public enum TaskStatus {
    PENDING("待执行"),
    RUNNING("执行中"),
    SUCCESS("成功"),
    FAILED("失败"),
    TIMEOUT("超时");

    private final String desc;
}
```

### 2. 实体类

```java
@Data
@TableName("task_record")
public class TaskRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskId;
    private LocalDate taskDate;
    private String status;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;

    private String errorMsg;
    private String metadata; // JSON 格式的额外信息

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // 判断是否超时
    public boolean isExpired(long timeoutMinutes) {
        return TaskStatus.RUNNING.name().equals(status)
            && startTime != null
            && Duration.between(startTime, LocalDateTime.now()).toMinutes() > timeoutMinutes;
    }
}
```

### 3. 完整实现

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class StateMachineTaskService {

    private final TaskRecordMapper taskRecordMapper;
    private static final long TASK_TIMEOUT_MINUTES = 30;

    @Scheduled(cron = "0 0 2 * * ?")
    public void dailyReport() {
        String taskId = "daily-report-" + LocalDate.now();

        // 1. 检查是否已有任务记录
        TaskRecord existingRecord = taskRecordMapper.selectByTaskId(taskId);

        if (existingRecord != null) {
            handleExistingRecord(existingRecord);
            return;
        }

        // 2. 创建新任务记录
        TaskRecord newRecord = createTaskRecord(taskId);
        try {
            taskRecordMapper.insert(newRecord);

            // 3. 执行任务
            executeTask(newRecord);

        } catch (DuplicateKeyException e) {
            log.info("任务已在其他实例执行: {}", taskId);
        } catch (Exception e) {
            log.error("任务执行失败: {}", taskId, e);
            updateTaskStatus(newRecord.getId(), TaskStatus.FAILED, e.getMessage());
        }
    }

    private void handleExistingRecord(TaskRecord record) {
        // 检查是否超时
        if (record.isExpired(TASK_TIMEOUT_MINUTES)) {
            log.warn("任务超时，准备重试: {}", record.getTaskId());
            updateTaskStatus(record.getId(), TaskStatus.TIMEOUT, "任务超时");

            // 可以选择重新执行
            retryTask(record);
            return;
        }

        // 检查状态
        if (TaskStatus.SUCCESS.name().equals(record.getStatus())) {
            log.info("任务已成功执行: {}", record.getTaskId());
        } else if (TaskStatus.RUNNING.name().equals(record.getStatus())) {
            log.info("任务正在执行中: {}", record.getTaskId());
        } else {
            log.warn("任务状态异常: {}, 状态: {}", record.getTaskId(), record.getStatus());
        }
    }

    private TaskRecord createTaskRecord(String taskId) {
        TaskRecord record = new TaskRecord();
        record.setTaskId(taskId);
        record.setTaskDate(LocalDate.now());
        record.setStatus(TaskStatus.PENDING.name());
        return record;
    }

    private void executeTask(TaskRecord record) {
        // 更新为执行中
        updateTaskStatus(record.getId(), TaskStatus.RUNNING, null);
        record.setStartTime(LocalDateTime.now());

        try {
            log.info("开始执行任务: {}", record.getTaskId());
            doDailyReport();

            // 执行成功
            record.setEndTime(LocalDateTime.now());
            record.setDurationMs(Duration.between(record.getStartTime(), record.getEndTime()).toMillis());
            updateTaskStatus(record.getId(), TaskStatus.SUCCESS, null);

        } catch (Exception e) {
            throw e;
        }
    }

    private void retryTask(TaskRecord oldRecord) {
        // 删除旧记录
        taskRecordMapper.deleteById(oldRecord.getId());

        // 重新执行
        dailyReport();
    }

    private void updateTaskStatus(Long id, TaskStatus status, String errorMsg) {
        TaskRecord record = new TaskRecord();
        record.setId(id);
        record.setStatus(status.name());
        record.setErrorMsg(errorMsg);
        taskRecordMapper.updateById(record);
    }

    private void doDailyReport() {
        // 实际业务逻辑
    }
}
```

---

## 方案三：通用幂等工具类

封装通用的数据库幂等工具，复用性更强。

### 1. 幂等注解

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    /**
     * 任务名称
     */
    String taskName();

    /**
     * 任务超时时间（分钟）
     */
    long timeoutMinutes() default 30;

    /**
     * 是否删除过期记录
     */
    boolean deleteExpired() default true;
}
```

### 2. 幂等切面

```java
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotentAspect {

    private final TaskRecordMapper taskRecordMapper;

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        String taskName = idempotent.taskName();
        String taskId = taskName + ":" + LocalDate.now();
        long timeoutMinutes = idempotent.timeoutMinutes();

        // 1. 查找现有记录
        TaskRecord existingRecord = taskRecordMapper.selectByTaskId(taskId);

        if (existingRecord != null) {
            return handleExistingRecord(existingRecord, timeoutMinutes, taskId);
        }

        // 2. 创建新记录
        TaskRecord newRecord = createRecord(taskId);
        try {
            taskRecordMapper.insert(newRecord);

            // 3. 执行任务
            return executeTask(joinPoint, newRecord);

        } catch (DuplicateKeyException e) {
            log.info("任务已在其他实例执行: {}", taskId);
            return null;
        } catch (Exception e) {
            log.error("任务执行失败: {}", taskId, e);
            updateStatus(newRecord.getId(), TaskStatus.FAILED, e.getMessage());
            throw e;
        }
    }

    private Object handleExistingRecord(TaskRecord record, long timeoutMinutes, String taskId) {
        if (record.isExpired(timeoutMinutes)) {
            log.warn("任务超时: {}", taskId);
            if (idempotent.deleteExpired()) {
                taskRecordMapper.deleteById(record.getId());
                // 递归重试（有限次数）
                return retry(taskId);
            }
        }

        log.info("任务已存在: {}, 状态: {}", taskId, record.getStatus());
        return null;
    }

    private Object executeTask(ProceedingJoinPoint joinPoint, TaskRecord record) throws Throwable {
        updateStatus(record.getId(), TaskStatus.RUNNING, null);
        record.setStartTime(LocalDateTime.now());

        try {
            Object result = joinPoint.proceed();

            record.setEndTime(LocalDateTime.now());
            record.setDurationMs(Duration.between(record.getStartTime(), record.getEndTime()).toMillis());
            updateStatus(record.getId(), TaskStatus.SUCCESS, null);

            return result;

        } catch (Throwable e) {
            updateStatus(record.getId(), TaskStatus.FAILED, e.getMessage());
            throw e;
        }
    }

    private TaskRecord createRecord(String taskId) {
        TaskRecord record = new TaskRecord();
        record.setTaskId(taskId);
        record.setTaskDate(LocalDate.now());
        record.setStatus(TaskStatus.PENDING.name());
        return record;
    }

    private void updateStatus(Long id, TaskStatus status, String errorMsg) {
        TaskRecord record = new TaskRecord();
        record.setId(id);
        record.setStatus(status.name());
        record.setErrorMsg(errorMsg);
        taskRecordMapper.updateById(record);
    }
}
```

### 3. 使用示例

```java
@Service
@Slf4j
public class TaskService {

    @Idempotent(taskName = "daily-report", timeoutMinutes = 30)
    @Scheduled(cron = "0 0 2 * * ?")
    public void dailyReport() {
        log.info("执行每日报表任务");
        // 业务逻辑
    }

    @Idempotent(taskName = "sync-data", timeoutMinutes = 60)
    @Scheduled(cron = "0 */5 * * * ?")
    public void syncData() {
        log.info("执行数据同步任务");
        // 业务逻辑
    }
}
```

---

## 高级场景处理

### 场景1：任务执行时间超过24小时

```java
@Service
@RequiredArgsConstructor
public class LongRunningTaskService {

    private final TaskRecordMapper taskRecordMapper;

    @Scheduled(cron = "0 0 2 * * ?")
    public void longRunningTask() {
        // 使用更细粒度的任务ID：包含小时
        String taskId = "long-task-" + LocalDate.now() + "-" + LocalDateTime.now().getHour();

        try {
            TaskRecord record = new TaskRecord();
            record.setTaskId(taskId);
            record.setTaskDate(LocalDate.now());
            record.setStatus(TaskStatus.RUNNING.name());
            record.setStartTime(LocalDateTime.now());
            taskRecordMapper.insert(record);

            doLongRunningTask();

            record.setStatus(TaskStatus.SUCCESS.name());
            record.setEndTime(LocalDateTime.now());
            taskRecordMapper.updateById(record);

        } catch (DuplicateKeyException e) {
            log.info("任务正在执行或已完成: {}", taskId);
        }
    }
}
```

### 场景2：任务需要分批处理

```java
@Service
@RequiredArgsConstructor
public class BatchTaskService {

    private final TaskRecordMapper taskRecordMapper;

    @Scheduled(cron = "0 0 2 * * ?")
    public void batchProcess() {
        String batchId = "batch-" + LocalDate.now();
        int batchSize = 1000;
        int offset = 0;

        while (true) {
            String taskId = batchId + "-batch-" + offset;

            try {
                TaskRecord record = new TaskRecord();
                record.setTaskId(taskId);
                record.setTaskDate(LocalDate.now());
                record.setStatus(TaskStatus.RUNNING.name());
                taskRecordMapper.insert(record);

                List<Data> dataList = fetchData(offset, batchSize);
                if (dataList.isEmpty()) {
                    break;
                }

                processBatch(dataList);

                record.setStatus(TaskStatus.SUCCESS.name());
                taskRecordMapper.updateById(record);

                offset += batchSize;

            } catch (DuplicateKeyException e) {
                log.info("批次已处理: {}", taskId);
                offset += batchSize; // 跳过已处理的批次
            }
        }
    }
}
```

### 场景3：分布式锁 + 数据库幂等（双重保险）

```java
@Service
@RequiredArgsConstructor
public class HybridTaskService {

    private final RedissonClient redissonClient;
    private final TaskRecordMapper taskRecordMapper;

    @Scheduled(cron = "0 0 2 * * ?")
    public void hybridTask() {
        String lockKey = "task:hybrid";
        String taskId = "hybrid-" + LocalDate.now();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 第一层：Redis 锁
            if (!lock.tryLock(0, 10, TimeUnit.MINUTES)) {
                log.info("Redis锁获取失败，任务可能正在执行");
                return;
            }

            try {
                // 第二层：数据库唯一约束
                TaskRecord record = new TaskRecord();
                record.setTaskId(taskId);
                record.setTaskDate(LocalDate.now());
                taskRecordMapper.insert(record);

                doTask();

                record.setStatus(TaskStatus.SUCCESS.name());
                taskRecordMapper.updateById(record);

            } catch (DuplicateKeyException e) {
                log.info("任务已在其他实例执行: {}", taskId);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

---

## 监控与清理

### 定期清理历史记录

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskCleanupService {

    private final TaskRecordMapper taskRecordMapper;

    // 每天凌晨3点清理30天前的记录
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOldRecords() {
        LocalDate cutoffDate = LocalDate.now().minusDays(30);

        int deletedCount = taskRecordMapper.deleteOldRecords(cutoffDate);
        log.info("清理了 {} 条过期任务记录", deletedCount);
    }
}
```

```java
// Mapper 接口
public interface TaskRecordMapper extends BaseMapper<TaskRecord> {

    @Delete("DELETE FROM task_record WHERE task_date < #{cutoffDate}")
    int deleteOldRecords(@Param("cutoffDate") LocalDate cutoffDate);
}
```

### 任务执行监控

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskMonitorService {

    private final TaskRecordMapper taskRecordMapper;

    @Scheduled(cron = "0 */10 * * * ?") // 每10分钟检查一次
    public void monitorRunningTasks() {
        List<TaskRecord> runningTasks = taskRecordMapper.selectRunningTasks();

        for (TaskRecord task : runningTasks) {
            long runningMinutes = Duration.between(task.getStartTime(), LocalDateTime.now()).toMinutes();

            if (runningMinutes > 60) { // 超过1小时
                log.warn("任务执行时间过长: {}, 已运行: {} 分钟, 状态: {}",
                    task.getTaskId(), runningMinutes, task.getStatus());

                // 发送告警
                sendAlert(task, runningMinutes);
            }
        }
    }

    private void sendAlert(TaskRecord task, long runningMinutes) {
        // 发送告警通知（邮件、钉钉、企业微信等）
    }
}
```

```java
// Mapper 接口
public interface TaskRecordMapper extends BaseMapper<TaskRecord> {

    @Select("SELECT * FROM task_record WHERE status = 'RUNNING'")
    List<TaskRecord> selectRunningTasks();
}
```

---

## 优缺点总结

### 优点

| 优点 | 说明 |
|------|------|
| ✅ 可靠性最高 | 数据库 ACID 保证，不会出现并发问题 |
| ✅ 无额外依赖 | 不需要 Redis、ZooKeeper 等中间件 |
| ✅ 天然幂等 | 唯一索引天然保证幂等性 |
| ✅ 实现简单 | 代码逻辑简单，易于理解和维护 |
| ✅ 可追溯 | 记录完整的任务执行历史 |
| ✅ 支持状态跟踪 | 可以跟踪任务执行状态 |
| ✅ 便于监控 | 可以查询任务执行情况 |

### 缺点

| 缺点 | 解决方案 |
|------|---------|
| ❌ 性能相对较低 | 对于高频任务不适用，建议使用 Redis 锁 |
| ❌ 增加 DB 压力 | 定期清理历史记录 |
| ❌ 依赖数据库可用性 | 数据库宕机时无法执行 |
| ❌ 需要建表 | 对于简单任务过于繁琐 |

---

## 适用场景

| 场景 | 是否推荐 | 原因 |
|------|---------|------|
| 低频定时任务（每天1次） | ✅ 强烈推荐 | 性能影响可忽略 |
| 任务涉及数据库操作 | ✅ 强烈推荐 | 可以利用事务保证一致性 |
| 强一致性要求 | ✅ 强烈推荐 | 数据库 ACID 保证 |
| 高频定时任务（每分钟） | ⚠️ 谨慎使用 | 可能对数据库造成压力 |
| 任务不涉及数据库 | ⚠️ 谨慎使用 | 额外的数据库开销 |
| 无数据库环境 | ❌ 不推荐 | 无法使用 |

---

## 参考资料

- MySQL 唯一索引：https://dev.mysql.com/doc/refman/8.0/en/create-index.html
- MyBatis-Plus 官方文档：https://baomidou.com
