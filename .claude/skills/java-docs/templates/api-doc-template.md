# ${projectName} 接口设计说明书

> 编制单位：${companyName}
> 生成时间：${timestamp}
> Spring Boot 版本：${springBootVersion}

---

## 目  录

1. 接口规范
   1.1. 调用说明
   1.2. 共用的参数
   1.3. 返回数据说明
2. 接口方法列表
   2.1. 本系统提供的接口
   2.2. 本系统需要访问的第三方接口
3. 数据字典

---

## 1. 接口规范

### 1.1. 调用说明

#### 1.1.1. 调用参数结构

**请求格式**：所有接口使用 HTTP POST 方法，请求体为 JSON 格式。

**通用请求头**：

| 字段 | 类型 | 说明 |
|------|------|------|
| Content-Type | string | application/json |
| Authorization | string | Bearer {token}（需要认证的接口） |

**请求参数结构**：

| 字段 | 字段类型 | 说明 |
|------|---------|------|
| ${requestMainField} | ${requestMainFieldType} | ${requestMainFieldDesc} |

**请求示例**：

```json
${requestExample}
```

#### 1.1.2. 完整 JAVA 代码示例

```java
// 使用 RestTemplate 调用示例
RestTemplate restTemplate = new RestTemplate();
HttpHeaders headers = new HttpHeaders();
headers.setContentType(MediaType.APPLICATION_JSON);
headers.set("Authorization", "Bearer " + token);

String requestJson = "${requestExample}";
HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

String url = "${baseUrl}${apiPath}";
String response = restTemplate.postForObject(url, entity, String.class);
System.out.println(response);
```

### 1.2. 共用的参数

注：以下参数是很多接口能共用的参数。

#### 1.2.1. 通用请求返回状态码

| 返回值 | 说明 |
|--------|------|
| 200 | 请求成功 |
| 201 | 请求处理成功 |
| 204 | 任务提交成功 |

#### 1.2.2. 异常状态码

| 返回值 | 说明 |
|--------|------|
| 400 | 服务器未能处理请求，请求参数错误 |
| 401 | 用户名和密码验证未通过，未认证 |
| 403 | 请求地址禁止访问，无权限 |
| 404 | 请求地址不存在 |
| 405 | 请求中指定的方法不被允许 |
| 408 | 请求超出了服务器的等待时间 |
| 409 | 由于冲突，请求无法被完成 |
| 500 | 请求未完成，服务异常 |
| 501 | 请求未完成，服务器不支持所请求的功能 |
| 503 | 请求未完成，系统暂时异常 |

#### 1.2.3. ${commonObject1Name} 对象数据结构说明

${commonObject1Desc}

| 参数 | 参数类型 | 说明 |
|------|---------|------|
${commonObject1Fields}

#### 1.2.4. ${commonObject2Name} 数据结构说明

${commonObject2Desc}

| 参数 | 参数类型 | 说明 |
|------|---------|------|
${commonObject2Fields}

${moreCommonObjects}

### 1.3. 返回数据说明

#### 1.3.1. 返回数据结构

返回数据为符合 JSON 结构的字符串。

**统一返回格式**：

| 字段名 | 字段类型 | 说明 |
|--------|---------|------|
| code | string/int | 接口调用情况，200 表示成功，其他表示失败 |
| message | string | 返回消息 |
| data | object/string/array | 返回的具体数据 |

**返回示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

---

## 2. 接口方法列表

### 2.1. 本系统提供的接口

${apiList}

#### ${apiIndex}. ${apiName}

**1) 接口功能介绍**

${apiDescription}

**2) 调用方法**

```
POST ${apiPath}
```

**3) 请求信息**

| 参数 | 是否必选 | 参数类型 | 说明 |
|------|---------|---------|------|
${apiRequestParams}

**请求示例**：

```json
${apiRequestExample}
```

**4) 响应信息**

| 参数 | 参数类型 | 说明 |
|------|---------|------|
${apiResponseParams}

**5) 响应示例**

正常返回示例：

```json
${apiResponseSuccessExample}
```

错误返回示例：

```json
${apiResponseErrorExample}
```

---

### 2.2. 本系统需要访问的第三方接口

${thirdPartyApiList}

#### ${tpApiIndex}. ${tpApiName}

**1) 接口功能介绍**

${tpApiDescription}

**2) 调用方法**

```
${tpApiMethod} ${tpApiPath}
```

**3) 请求信息**

| 参数 | 是否必选 | 参数类型 | 说明 |
|------|---------|---------|------|
${tpApiRequestParams}

**4) 响应信息**

| 参数 | 参数类型 | 说明 |
|------|---------|------|
${tpApiResponseParams}

**5) 响应示例**

正常返回示例：

```json
${tpApiSuccessExample}
```

错误返回示例：

```json
${tpApiErrorExample}
```

---

## 3. 数据字典

${dataDictionaryList}

### 3.${dictIndex}. ${dictName}

| 序号 | 字典代码 | 字典值 |
|------|---------|--------|
${dictItems}

---

## 附录

### A. Swagger/Knife4j 在线文档

**本地开发环境访问地址**：

| 文档 | 地址 |
|------|------|
| Knife4j UI | `http://localhost:${port}${contextPath}/doc.html` |
| Swagger UI | `http://localhost:${port}${contextPath}/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:${port}${contextPath}/v3/api-docs` |

> **说明**：Knife4j 支持在线调试和离线文档导出（Markdown/Word/HTML/PDF）。
> 访问 `/doc.html` 后点击「文档管理」→「离线文档」即可导出。

**生产环境访问地址**：

| 文档 | 地址 |
|------|------|
| Knife4j UI | `http://${server}:${port}${contextPath}/doc.html` |

> **注意**：生产环境建议关闭 Swagger，仅在需要时开启。

### B. 基础信息

| 项目 | 说明 |
|------|------|
| 项目名称 | ${projectName} |
| 基础 URL | ${baseUrl} |
| Spring Boot 版本 | ${springBootVersion} |
| Java 版本 | ${javaVersion} |

### C. 分页参数说明

| 参数 | 类型 | 必填 | 说明 | 默认值 |
|------|------|------|------|--------|
| pageNum | int | 否 | 页码 | 1 |
| pageSize | int | 否 | 每页数量 | 10 |
