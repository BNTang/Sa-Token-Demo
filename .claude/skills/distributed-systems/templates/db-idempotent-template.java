/**
 * 数据库唯一约束幂等模板
 *
 * 使用数据库唯一约束实现定时任务防重执行
 *
 * 依赖：
 * <dependency>
 *   <groupId>com.baomidou</groupId>
 *   <artifactId>mybatis-plus-boot-starter</artifactId>
 *   <version>3.5.5</version>
 * </dependency>
 */

package com.example.template;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 任务记录实体
 */
@Data
@TableName("task_record")
class TaskRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务ID（唯一键的一部分）
     */
    private String taskId;

    /**
     * 任务日期（唯一键的一部分）
     */
    private LocalDate taskDate;

    /**
     * 任务状态：PENDING/RUNNING/SUCCESS/FAILED/TIMEOUT
     */
    private String status;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 执行耗时（毫秒）
     */
    private Long durationMs;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 元数据（JSON格式）
     */
    private String metadata;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 判断是否超时
     */
    public boolean isExpired(long timeoutMinutes) {
        return "RUNNING".equals(status)
            && startTime != null
            && Duration.between(startTime, LocalDateTime.now()).toMinutes() > timeoutMinutes;
    }
}

/**
 * 任务记录 Mapper
 */
interface TaskRecordMapper extends com.baomidou.mybatisplus.core.mapper.BaseMapper<TaskRecord> {

    /**
     * 根据 taskId 查询
     */
    default TaskRecord selectByTaskId(String taskId) {
        return selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TaskRecord>()
            .eq(TaskRecord::getTaskId, taskId)
            .last("LIMIT 1"));
    }

    /**
     * 查询运行中的任务
     */
    default java.util.List<TaskRecord> selectRunningTasks() {
        return selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TaskRecord>()
            .eq(TaskRecord::getStatus, "RUNNING"));
    }

    /**
     * 统计指定时间后的任务数量
     */
    default int countByCreateTimeAfter(LocalDateTime time) {
        return Math.toIntExact(selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TaskRecord>()
                .ge(TaskRecord::getCreateTime, time)
        ));
    }

    /**
     * 统计指定状态和时间后的任务数量
     */
    default int countByStatusAndCreateTimeAfter(String status, LocalDateTime time) {
        return Math.toIntExact(selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TaskRecord>()
                .eq(TaskRecord::getStatus, status)
                .ge(TaskRecord::getCreateTime, time)
        ));
    }

    /**
     * 删除过期记录
     */
    default int deleteOldRecords(LocalDate cutoffDate) {
        return delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TaskRecord>()
            .lt(TaskRecord::getTaskDate, cutoffDate));
    }
}

/**
 * 数据库幂等服务
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseIdempotentTemplate {

    private final TaskRecordMapper taskRecordMapper;

    /**
     * 执行幂等任务
     *
     * @param taskName        任务名称
     * @param task            任务逻辑
     * @param timeoutMinutes  超时时间（分钟）
     * @return true-执行成功，false-跳过或失败
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean executeIdempotentTask(
        String taskName,
        Runnable task,
        long timeoutMinutes
    ) {
        String taskId = taskName + ":" + LocalDate.now();

        // 1. 检查现有记录
        TaskRecord existingRecord = taskRecordMapper.selectByTaskId(taskId);

        if (existingRecord != null) {
            return handleExistingRecord(existingRecord, timeoutMinutes);
        }

        // 2. 创建新记录
        return executeNewTask(taskId, task);
    }

    /**
     * 处理已存在的任务记录
     */
    private boolean handleExistingRecord(TaskRecord record, long timeoutMinutes) {
        // 检查是否超时
        if (record.isExpired(timeoutMinutes)) {
            log.warn("任务超时: {}, 已运行: {} 分钟",
                record.getTaskId(),
                Duration.between(record.getStartTime(), LocalDateTime.now()).toMinutes());

            // 标记为超时
            record.setStatus("TIMEOUT");
            record.setEndTime(LocalDateTime.now());
            taskRecordMapper.updateById(record);

            // 删除记录，允许重试
            taskRecordMapper.deleteById(record.getId());
            return false;
        }

        // 检查状态
        switch (record.getStatus()) {
            case "SUCCESS":
                log.info("任务已成功执行: {}", record.getTaskId());
                return true;
            case "RUNNING":
                log.info("任务正在执行中: {}", record.getTaskId());
                return false;
            case "FAILED":
                log.warn("任务之前执行失败: {}, 错误: {}",
                    record.getTaskId(), record.getErrorMsg());
                return false;
            default:
                log.warn("任务状态异常: {}, 状态: {}",
                    record.getTaskId(), record.getStatus());
                return false;
        }
    }

    /**
     * 执行新任务
     */
    private boolean executeNewTask(String taskId, Runnable task) {
        TaskRecord newRecord = new TaskRecord();
        newRecord.setTaskId(taskId);
        newRecord.setTaskDate(LocalDate.now());
        newRecord.setStatus("PENDING");
        newRecord.setCreateTime(LocalDateTime.now());

        try {
            // 插入记录（唯一索引保证幂等）
            taskRecordMapper.insert(newRecord);

            // 更新为执行中
            newRecord.setStatus("RUNNING");
            newRecord.setStartTime(LocalDateTime.now());
            taskRecordMapper.updateById(newRecord);

            // 执行任务
            long startTime = System.currentTimeMillis();
            try {
                task.run();

                // 执行成功
                newRecord.setStatus("SUCCESS");
                newRecord.setEndTime(LocalDateTime.now());
                newRecord.setDurationMs(System.currentTimeMillis() - startTime);
                taskRecordMapper.updateById(newRecord);

                log.info("任务执行成功: {}, 耗时: {}ms",
                    taskId, newRecord.getDurationMs());
                return true;

            } catch (Exception e) {
                // 任务执行失败
                newRecord.setStatus("FAILED");
                newRecord.setEndTime(LocalDateTime.now());
                newRecord.setDurationMs(System.currentTimeMillis() - startTime);
                newRecord.setErrorMsg(e.getMessage());
                taskRecordMapper.updateById(newRecord);

                log.error("任务执行失败: {}", taskId, e);
                throw e;
            }

        } catch (org.springframework.dao.DuplicateKeyException e) {
            log.info("任务已在其他实例执行: {}", taskId);
            return false;
        }
    }
}

/**
 * 使用示例
 */
@Component
@RequiredArgsConstructor
@Slf4j
class ScheduledTaskExample {

    private final DatabaseIdempotentTemplate idempotentTemplate;

    /**
     * 示例1：基础用法
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void basicExample() {
        idempotentTemplate.executeIdempotentTask(
            "daily-report",
            () -> {
                log.info("生成每日报表");
                doDailyReport();
            },
            30  // 超时时间：30分钟
        );
    }

    /**
     * 示例2：数据同步任务
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void syncDataExample() {
        idempotentTemplate.executeIdempotentTask(
            "sync-data",
            () -> {
                log.info("同步数据");
                int count = syncData();
                log.info("同步了 {} 条数据", count);
            },
            10  // 超时时间：10分钟
        );
    }

    /**
     * 示例3：数据清理任务
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExample() {
        idempotentTemplate.executeIdempotentTask(
            "cleanup-data",
            () -> {
                log.info("清理过期数据");
                int deleted = cleanupData();
                log.info("清理了 {} 条数据", deleted);
            },
            60  // 超时时间：60分钟
        );
    }

    /**
     * 示例4：报表生成任务（带参数）
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void reportWithParamsExample() {
        String reportType = "sales";
        String taskId = "generate-report-" + reportType;

        idempotentTemplate.executeIdempotentTask(
            taskId,
            () -> {
                log.info("生成报表: {}", reportType);
                generateReport(reportType);
            },
            15
        );
    }

    // ==================== 业务方法 ====================

    private void doDailyReport() {
        // 生成每日报表的业务逻辑
    }

    private int syncData() {
        // 数据同步的业务逻辑
        return 100;
    }

    private int cleanupData() {
        // 数据清理的业务逻辑
        return 50;
    }

    private void generateReport(String reportType) {
        // 生成报表的业务逻辑
    }
}

/**
 * 建表语句
 */
/*
CREATE TABLE task_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    task_id VARCHAR(128) NOT NULL COMMENT '任务ID',
    task_date DATE NOT NULL COMMENT '任务日期',
    status VARCHAR(32) DEFAULT 'SUCCESS' COMMENT '状态：PENDING/RUNNING/SUCCESS/FAILED/TIMEOUT',
    start_time DATETIME DEFAULT NULL COMMENT '开始时间',
    end_time DATETIME DEFAULT NULL COMMENT '结束时间',
    duration_ms BIGINT DEFAULT NULL COMMENT '执行耗时(毫秒)',
    error_msg TEXT COMMENT '错误信息',
    metadata JSON COMMENT '元数据',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_task (task_date, task_id) COMMENT '任务唯一键',
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务执行记录';
*/

/**
 * MyBatis-Plus 配置
 */
/*
@Configuration
class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 自动填充
        interceptor.addInnerHandler(new com.baomidou.mybatisplus.extension.handlers.MybatisPlusInterceptor() {
            // 自动填充 create_time 和 update_time
        });

        return interceptor;
    }
}
*/
