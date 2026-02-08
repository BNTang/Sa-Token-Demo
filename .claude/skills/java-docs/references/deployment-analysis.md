# 部署分析规范

> Java Spring Boot 项目文档生成器 - 部署分析模块

---

## 分析目标

分析项目获取部署相关信息：

| 信息项 | 来源 | 用途 |
|--------|------|------|
| JDK 版本 | `pom.xml` `java.version` 或 `maven.compiler` | 环境要求 |
| Spring Boot 版本 | `pom.xml` `spring-boot-starter-parent` | 版本说明 |
| 数据库 | `pom.xml` 依赖 + `application.yml` 配置 | 环境要求 |
| Redis | `pom.xml` 依赖 + `application.yml` 配置 | 环境要求 |
| 部署方式 | 是否存在 `Dockerfile` | 部署方案 |

---

## 环境依赖分析

### 依赖检测

在 `pom.xml` 或 `build.gradle` 中检测：

| 依赖 | 版本提取 | 环境要求 |
|------|---------|---------|
| Spring Boot | `<parent>` 版本 | - |
| MySQL | `mysql-connector-java` 版本 | MySQL 5.7+ / 8.0+ |
| PostgreSQL | `postgresql` 版本 | PostgreSQL 12+ |
| Redis | `spring-boot-starter-data-redis` | Redis 5.0+ |
| MongoDB | `spring-boot-starter-data-mongodb` | MongoDB 4.4+ |
| RabbitMQ | `spring-boot-starter-amqp` | RabbitMQ 3.8+ |
| Kafka | `spring-kafka` | Kafka 2.8+ |

### JDK 版本提取

从 `pom.xml` 提取：

```xml
<properties>
    <java.version>17</java.version>    <!-- 提取此项 -->
</properties>

<!-- 或 -->
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <source>17</source>      <!-- 或提取此项 -->
                <target>17</target>
            </configuration>
        </plugin>
    </plugins>
</build>
```

---

## 配置文件分析

### application.yml 配置提取

```yaml
server:
  port: 8080                    # 服务端口
  servlet:
    context-path: /api          # 上下文路径

spring:
  application:
    name: my-app                # 应用名称

  datasource:                   # 数据源配置
    url: jdbc:mysql://localhost:3306/mydb
    username: root
    password: ******            # 需脱敏
    driver-class-name: com.mysql.cj.jdbc.Driver

  redis:                        # Redis 配置
    host: localhost
    port: 6379
    database: 0
    password: ******            # 需脱敏（如果有）

  data:
    mongodb:                    # MongoDB 配置（如果有）
      uri: mongodb://localhost:27017/mydb
```

### 脱敏规则

生成文档时脱敏以下信息：
- 密码字段：`password` → `******`
- 密钥字段：`secret` / `key` → `******`
- Token：`token` → `******`
- URL 中的密码：`jdbc:mysql://user:******@host:port/db`

---

## 部署方式判断

### 判断逻辑

```python
if exists("Dockerfile"):
    deployment_type = "Docker"
    if exists("docker-compose.yml"):
        deployment_type = "Docker Compose"
elif exists("src/main/deploy/*.sh"):
    deployment_type = "传统脚本部署"
else:
    deployment_type = "JAR 包部署"
```

### Dockerfile 分析

提取以下信息：

| 信息项 | 提取方式 | 用途 |
|--------|---------|------|
| 基础镜像 | `FROM xxx` | 镜像构建说明 |
| 暴露端口 | `EXPOSE xxx` | 端口映射说明 |
| 启动命令 | `ENTRYPOINT / CMD` | 容器启动说明 |
| 工作目录 | `WORKDIR xxx` | 部署结构说明 |

---

## 启停脚本生成

### 生成条件

仅在以下情况下生成脚本：
1. `scripts/` 目录不存在同名脚本
2. 项目不存在已定义的启停脚本

### start.sh 模板

```bash
#!/bin/bash

# 应用名称
APP_NAME="${projectName}"
# JAR 文件路径
JAR_FILE="target/${projectName}.jar"
# JVM 参数
JVM_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"
# Spring 配置
SPRING_OPTS="--spring.profiles.active=prod"

# 启动函数
start() {
    echo "Starting ${APP_NAME}..."
    nohup java ${JVM_OPTS} -jar ${JAR_FILE} ${SPRING_OPTS} > /dev/null 2>&1 &
    echo "${APP_NAME} started with PID $!"
}

# 停止函数
stop() {
    echo "Stopping ${APP_NAME}..."
    PID=$(ps -ef | grep ${JAR_FILE} | grep -v grep | awk '{print $2}')
    if [ -n "$PID" ]; then
        kill -15 $PID
        echo "${APP_NAME} stopped (PID: $PID)"
    else
        echo "${APP_NAME} is not running"
    fi
}

# 重启函数
restart() {
    stop
    sleep 2
    start
}

# 主逻辑
case "$1" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        restart
        ;;
    *)
        echo "Usage: $0 {start|stop|restart}"
        exit 1
esac
```

### start.bat 模板（Windows）

```batch
@echo off
set APP_NAME=${projectName}
set JAR_FILE=target\${projectName}.jar
set JVM_OPTS=-Xms512m -Xmx1024m
set SPRING_OPTS=--spring.profiles.active=prod

echo Starting %APP_NAME%...
java %JVM_OPTS% -jar %JAR_FILE% %SPRING_OPTS%
pause
```

---

## 输出文档结构

生成的 `docs/deployment.md` 应包含：

```markdown
# 部署手册

## 1. 环境要求
### 1.1 基础环境
- JDK 版本
- 操作系统

### 1.2 依赖服务
- 数据库版本
- Redis 版本
- 其他依赖

## 2. 项目配置
### 2.1 配置文件说明
- application.yml
- application-prod.yml

### 2.2 环境变量配置
- 需配置的环境变量列表

## 3. 构建打包
### 3.1 Maven 构建
```bash
mvn clean package -DskipTests
```

### 3.2 Gradle 构建
```bash
./gradlew clean build -x test
```

## 4. 部署方式
### 4.1 JAR 包部署
- 上传 JAR 包
- 配置文件
- 启动服务

### 4.2 Docker 部署（如果存在 Dockerfile）
- 构建镜像
- 运行容器

### 4.3 Docker Compose 部署（如果存在）
- 编排文件说明
- 启动命令

## 5. 启停脚本
### 5.1 Linux 脚本
- start.sh
- stop.sh
- restart.sh

### 5.2 Windows 脚本
- start.bat

## 6. 验证检查
### 6.1 健康检查
- 健康检查端点

### 6.2 接口测试
- Swagger 访问验证
```
