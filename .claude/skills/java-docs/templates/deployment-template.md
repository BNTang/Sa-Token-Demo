# 部署手册模板

> 项目名称：${projectName}
> 生成时间：${timestamp}

---

## 1. 环境要求

### 1.1 基础环境

| 环境 | 版本要求 | 说明 |
|------|---------|------|
| JDK | ${javaVersion}+ | 推荐使用 LTS 版本 |
| 操作系统 | Linux / Windows | 推荐 CentOS 7+ / Ubuntu 18.04+ |

### 1.2 依赖服务

| 服务 | 版本要求 | 说明 |
|------|---------|------|
${dependencyList}

---

## 2. 项目配置

### 2.1 配置文件说明

| 文件 | 说明 |
|------|------|
| `application.yml` | 主配置文件 |
| `application-dev.yml` | 开发环境配置 |
| `application-prod.yml` | 生产环境配置 |

### 2.2 核心配置项

```yaml
server:
  port: ${port}                    # 服务端口
  servlet:
    context-path: ${contextPath}   # 上下文路径

spring:
  application:
    name: ${projectName}

  datasource:                      # 数据源配置
    url: jdbc:mysql://${dbHost}:${dbPort}/${dbName}
    username: ${dbUser}
    password: ${dbPassword}
    driver-class-name: com.mysql.cj.jdbc.Driver

${redisConfig}

${additionalConfig}
```

### 2.3 环境变量配置

可以通过环境变量覆盖配置：

| 变量名 | 说明 | 示例 |
|--------|------|------|
| `SPRING_PROFILES_ACTIVE` | 运行环境 | `prod` |
| `SERVER_PORT` | 服务端口 | `8080` |
| `SPRING_DATASOURCE_URL` | 数据库地址 | `jdbc:mysql://localhost:3306/db` |
| `SPRING_DATASOURCE_USERNAME` | 数据库用户 | `root` |
| `SPRING_DATASOURCE_PASSWORD` | 数据库密码 | `******` |
${envVarList}

---

## 3. 构建打包

### 3.1 Maven 构建

```bash
# 清理并打包（跳过测试）
mvn clean package -DskipTests

# 或运行测试后打包
mvn clean package
```

打包后的 JAR 文件位置：`target/${projectName}.jar`

### 3.2 Gradle 构建

```bash
# 清理并构建（跳过测试）
./gradlew clean build -x test

# 或运行测试后构建
./gradlew clean build
```

打包后的 JAR 文件位置：`build/libs/${projectName}.jar`

### 3.3 验证打包

```bash
# 查看 JAR 包内容
jar tf target/${projectName}.jar

# 测试运行（指定配置）
java -jar target/${projectName}.jar --spring.profiles.active=dev
```

---

## 4. 部署方式

${deploymentSection}

### 4.1 JAR 包部署

#### 4.1.1 部署步骤

1. **上传文件**

   ```bash
   # 创建部署目录
   mkdir -p /opt/${projectName}

   # 上传 JAR 包到部署目录
   scp target/${projectName}.jar user@server:/opt/${projectName}/
   ```

2. **配置文件**

   将配置文件放在 JAR 同级目录：

   ```bash
   /opt/${projectName}/
   ├── ${projectName}.jar
   └── config/
       └── application-prod.yml
   ```

3. **启动服务**

   使用提供的启动脚本：

   ```bash
   cd /opt/${projectName}
   ./scripts/start.sh
   ```

#### 4.1.2 JVM 参数建议

```bash
# 基础配置
-Xms512m -Xmx1024m               # 堆内存
-XX:MetaspaceSize=128m           # 元空间
-XX:MaxMetaspaceSize=256m
-XX:+UseG1GC                     # G1 垃圾回收器

# 生产环境建议
-server                           # 服务器模式
-XX:+HeapDumpOnOutOfMemoryError  # OOM 时导出堆
-XX:HeapDumpPath=/logs/heapdump.hprof
-XX:+PrintGCDetails              # 打印 GC 详情
-XX:+PrintGCDateStamps           # 打印 GC 时间戳
-Xloggc:/logs/gc.log             # GC 日志文件
```

#### 4.1.3 启动命令示例

```bash
# 基础启动
java -jar ${projectName}.jar

# 指定配置文件
java -jar ${projectName}.jar --spring.profiles.active=prod

# 指定 JVM 参数
java -Xms512m -Xmx1024m -jar ${projectName}.jar --spring.profiles.active=prod

# 后台运行
nohup java -jar ${projectName}.jar --spring.profiles.active=prod > /dev/null 2>&1 &
```

### 4.2 Docker 部署

#### 4.2.1 构建镜像

```bash
# 构建镜像
docker build -t ${projectName}:latest .

# 查看镜像
docker images | grep ${projectName}
```

#### 4.2.2 运行容器

```bash
# 基础运行
docker run -d \
  --name ${projectName} \
  -p ${port}:${port} \
  ${projectName}:latest

# 挂载配置文件
docker run -d \
  --name ${projectName} \
  -p ${port}:${port} \
  -v /opt/config:/app/config \
  ${projectName}:latest

# 挂载日志目录
docker run -d \
  --name ${projectName} \
  -p ${port}:${port} \
  -v /opt/logs:/app/logs \
  ${projectName}:latest
```

#### 4.2.3 容器管理

```bash
# 查看容器状态
docker ps -a | grep ${projectName}

# 查看容器日志
docker logs -f ${projectName}

# 进入容器
docker exec -it ${projectName} /bin/bash

# 停止容器
docker stop ${projectName}

# 启动容器
docker start ${projectName}

# 重启容器
docker restart ${projectName}

# 删除容器
docker rm -f ${projectName}
```

### 4.3 Docker Compose 部署

#### 4.3.1 启动服务

```bash
# 启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

#### 4.3.2 停止服务

```bash
# 停止所有服务
docker-compose down

# 停止并删除数据卷
docker-compose down -v
```

---

## 5. 启停脚本

### 5.1 Linux 脚本

脚本位置：`scripts/`

| 脚本 | 功能 | 用法 |
|------|------|------|
| `start.sh` | 启动服务 | `./scripts/start.sh` |
| `stop.sh` | 停止服务 | `./scripts/stop.sh` |
| `restart.sh` | 重启服务 | `./scripts/restart.sh` |

#### start.sh

```bash
#!/bin/bash

APP_NAME="${projectName}"
JAR_FILE="${projectName}.jar"
JVM_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"
SPRING_OPTS="--spring.profiles.active=prod"

start() {
    echo "Starting ${APP_NAME}..."
    nohup java ${JVM_OPTS} -jar ${JAR_FILE} ${SPRING_OPTS} > /dev/null 2>&1 &
    echo "${APP_NAME} started with PID $!"
}

start
```

#### stop.sh

```bash
#!/bin/bash

APP_NAME="${projectName}"
JAR_FILE="${projectName}.jar"

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

stop
```

### 5.2 Windows 脚本

脚本位置：`scripts/`

| 脚本 | 功能 | 用法 |
|------|------|------|
| `start.bat` | 启动服务 | 双击运行 |

#### start.bat

```batch
@echo off
set APP_NAME=${projectName}
set JAR_FILE=${projectName}.jar
set JVM_OPTS=-Xms512m -Xmx1024m
set SPRING_OPTS=--spring.profiles.active=prod

echo Starting %APP_NAME%...
java %JVM_OPTS% -jar %JAR_FILE% %SPRING_OPTS%
pause
```

---

## 6. 验证检查

### 6.1 健康检查

| 检查项 | 命令/方式 | 预期结果 |
|--------|----------|---------|
| 服务进程 | `ps -ef \| grep ${projectName}.jar` | 进程存在 |
| 端口监听 | `netstat -tuln \| grep ${port}` | 端口监听 |
| 健康检查 | `curl http://localhost:${port}/actuator/health` | `{"status":"UP"}` |

### 6.2 接口测试

```bash
# 测试健康检查端点
curl http://localhost:${port}/actuator/health

# 测试一个接口
curl -X POST http://localhost:${port}${contextPath}/xxx/xxx \
  -H "Content-Type: application/json" \
  -d '{}'
```

### 6.3 Swagger 访问验证

访问以下地址验证 Swagger 是否正常：

```
http://localhost:${port}${contextPath}/doc.html
```

---

## 7. 常见问题

### 7.1 端口被占用

**现象**：启动时报 `Address already in use`

**解决**：

```bash
# 查找占用端口的进程
lsof -i :${port}

# 或
netstat -tuln | grep ${port}

# 杀掉进程
kill -9 <PID>
```

### 7.2 内存不足

**现象**：启动时报 `Java heap space`

**解决**：调整 JVM 参数

```bash
# 增加堆内存
java -Xms1024m -Xmx2048m -jar ${projectName}.jar
```

### 7.3 数据库连接失败

**现象**：报 `Could not create connection to database server`

**解决**：

1. 检查数据库服务是否启动
2. 检查数据库地址和端口是否正确
3. 检查用户名和密码是否正确
4. 检查防火墙是否开放端口
