# Sa-Token + Redis 演示项目

这个模块对应“Sa-Token 集成 Redis”章节，重点演示 4 件事：

1. 默认内存模式在多节点下为什么会出现登录态不一致。
2. 如何把会话层从进程内状态切到 Redis 共享状态。
3. 如何快速确认项目到底有没有真正落到 Redis。
4. 如何切换序列化方案（`json` / `jdk-*`）并观察效果。

## 模块路径

`sa-token-demo-redis`

## 演示接口

- `POST /redis-demo/login?userId=10001&password=123456`
- `GET /redis-demo/me`
- `PUT /redis-demo/nickname?nickname=alice`
- `GET /redis-demo/storage`
- `GET /redis-demo/session-by-login-id?loginId=10001`
- `GET /redis-demo/is-login`
- `POST /redis-demo/logout`

其中 `GET /redis-demo/storage` 用于排障，返回：

- 当前节点标识 `nodeId`
- 当前 Sa-Token Dao 实现类（是否为 Redis Dao）
- 当前序列化实现类
- Redis `ping` 与读写自检结果

## 快速启动

1. 启动 Redis：

```bash
docker compose -f sa-token-demo-redis/docker-compose.yml up -d
```

2. 启动节点 A（端口 8091）：

```bash
mvn -pl sa-token-demo-redis spring-boot:run -Dspring-boot.run.profiles=node-a
```

3. 启动节点 B（端口 8092）：

```bash
mvn -pl sa-token-demo-redis spring-boot:run -Dspring-boot.run.profiles=node-b
```

4. 在节点 A 登录：

```bash
curl -X POST "http://127.0.0.1:8091/redis-demo/login?userId=10001&password=123456"
```

5. 带同一个 `satoken` 请求节点 B，验证会话共享：

```bash
curl "http://127.0.0.1:8092/redis-demo/me" -H "satoken: <token>"
```

6. 查看存储诊断信息：

```bash
curl "http://127.0.0.1:8091/redis-demo/storage"
```

## 复现“各管各的”内存模式

同时以 `memory` profile 启动两个节点：

```bash
mvn -pl sa-token-demo-redis spring-boot:run -Dspring-boot.run.profiles=node-a,memory
mvn -pl sa-token-demo-redis spring-boot:run -Dspring-boot.run.profiles=node-b,memory
```

此时会强制使用 `SaTokenDaoDefaultImpl`，会话不再共享，可直接复现多节点不一致问题。

## 序列化切换

启动参数示例：

```bash
mvn -pl sa-token-demo-redis spring-boot:run -Dspring-boot.run.arguments="--demo.serializer=jdk-base64"
```

支持值：

- `json`
- `jdk-base64`
- `jdk-hex`
- `jdk-iso-8859-1`

## 排障检查清单

1. `storage` 接口里 `daoClass` 是否是 Redis 相关实现。
2. `storage.redis.ping` 是否返回 `PONG`。
3. 两个节点使用的 Redis 是否是同一实例（host/port/db 一致）。
4. `sa-token` 与 `sa-token-redis-template` 版本是否一致。
5. Spring Boot 3.x 下是否已改成 `spring.data.redis.*` 前缀。
