# Sa-Token + Redis Demo

This module demonstrates:

1. Why default in-memory session storage breaks in multi-node deployment.
2. How to switch Sa-Token session storage to Redis shared state.
3. How to verify Redis connection and real storage implementation at runtime.
4. How to switch serializer mode (`json`, `jdk-base64`, `jdk-hex`, `jdk-iso-8859-1`).

## Module Name

`sa-token-demo-redis`

## Endpoints

- `POST /redis-demo/login?userId=10001&password=123456`
- `GET /redis-demo/me`
- `PUT /redis-demo/nickname?nickname=alice`
- `GET /redis-demo/storage`
- `GET /redis-demo/session-by-login-id?loginId=10001`
- `GET /redis-demo/is-login`
- `POST /redis-demo/logout`

## Quick Start

1. Start Redis:

```bash
docker compose -f sa-token-demo-redis/docker-compose.yml up -d
```

2. Start node A:

```bash
mvn -pl sa-token-demo-redis spring-boot:run -Dspring-boot.run.profiles=node-a
```

3. Start node B:

```bash
mvn -pl sa-token-demo-redis spring-boot:run -Dspring-boot.run.profiles=node-b
```

4. Login on node A:

```bash
curl -X POST "http://127.0.0.1:8091/redis-demo/login?userId=10001&password=123456"
```

5. Access node B with the same `satoken` value and verify cross-node consistency:

```bash
curl "http://127.0.0.1:8092/redis-demo/me" -H "satoken: <token>"
```

6. Check actual storage/serializer implementation:

```bash
curl "http://127.0.0.1:8091/redis-demo/storage"
```

## Reproduce "each node manages its own session"

Run both nodes with `memory` profile:

```bash
mvn -pl sa-token-demo-redis spring-boot:run -Dspring-boot.run.profiles=node-a,memory
mvn -pl sa-token-demo-redis spring-boot:run -Dspring-boot.run.profiles=node-b,memory
```

In this mode, `SaTokenDaoDefaultImpl` is forced and sessions are process-local.

## Serializer Switch

Use startup arg:

```bash
mvn -pl sa-token-demo-redis spring-boot:run -Dspring-boot.run.arguments="--demo.serializer=jdk-base64"
```

Supported values:

- `json`
- `jdk-base64`
- `jdk-hex`
- `jdk-iso-8859-1`

## Spring Boot 3.x Note

If you migrate this module to Spring Boot 3.x, change `spring.redis.*` to `spring.data.redis.*`.
