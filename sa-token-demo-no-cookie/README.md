# Sa-Token No-Cookie Demo

This module demonstrates a stable token auth flow for frontend/backend separation:

1. Login endpoint returns `tokenName` and `tokenValue`.
2. Frontend stores both values.
3. Every API call injects `{tokenName: tokenValue}` into request headers.
4. Backend reads token from header only (`is-read-cookie=false`).

## Module path

`sa-token-demo-no-cookie`

## Quick start

1. Start this module:

```bash
mvn -pl sa-token-demo-no-cookie spring-boot:run
```

2. Open demo page:

```text
http://127.0.0.1:8086/index.html
```

3. Use default credentials:

- username: `neo`
- password: `123456`

## API contract

- `POST /user/doLogin`
  - Request body:
    ```json
    { "username": "neo", "password": "123456" }
    ```
  - Response `data` contains:
    - `tokenName`
    - `tokenValue`
    - `loginId`
    - `username`
- `GET /user/info` (requires token header)
- `POST /user/logout` (requires token header)

## cURL self-check

1. Login and copy `tokenValue`:

```bash
curl -X POST http://127.0.0.1:8086/user/doLogin \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"neo\",\"password\":\"123456\"}"
```

2. Call protected endpoint with manual header:

```bash
curl http://127.0.0.1:8086/user/info \
  -H "satoken: REPLACE_WITH_TOKEN_VALUE"
```

## Frontend interceptor example (axios)

```ts
axios.interceptors.request.use((config) => {
  const tokenName = localStorage.getItem("tokenName") || "satoken";
  const tokenValue = localStorage.getItem("tokenValue");
  if (tokenValue) {
    config.headers[tokenName] = tokenValue;
  }
  return config;
});
```

## Key backend config

```yaml
sa-token:
  token-name: satoken
  is-read-header: true
  is-read-cookie: false
  timeout: 2592000
```
