---
name: java-docs
description: |
  Java Spring Boot 项目文档生成器。

  分析项目结构、配置文件、Controller、枚举类，生成符合客户格式要求的接口设计说明书。
  同时生成部署手册（含启停脚本）和运维手册。

  文档格式特点：
  - 接口文档采用传统格式（接口规范、方法列表、数据字典）
  - 自动分析枚举类生成数据字典
  - 结合 Knife4j 提供在线文档和导出说明
  - 自动识别共用对象（统一返回、分页等）

  使用场景：当用户要求生成项目文档、接口设计说明书、部署手册、运维手册时调用。
  触发词：生成文档、接口设计说明书、接口文档、部署手册、运维手册、/api-doc、/deploy-doc、/ops-doc

user-invocable: true
allowed-tools: Read, Grep, Glob, Write, Edit
metadata:
  version: "1.0"
  author: java-team
  compatibility: Spring Boot 2.x / 3.x, Maven / Gradle
---

# Java 项目文档生成器

> 版本: 1.0 | 适配: Spring Boot 2.x / 3.x

---

## 概述

本 Skill 用于分析 Java Spring Boot 项目，自动生成以下文档：

| 文档类型 | 说明 | 输出位置 |
|---------|------|---------|
| **接口设计说明书** | 传统格式，包含接口规范、方法列表、数据字典 | `docs/api-doc.md` |
| **部署手册** | 环境要求、构建打包、部署方式、启停脚本 | `docs/deployment.md` |
| **运维手册** | 服务管理、日志管理、监控告警、故障排查、备份恢复 | `docs/operations.md` |

### 接口设计说明书格式

采用客户要求的传统格式：

```
1. 接口规范
   1.1 调用说明（调用参数结构、JAVA 代码示例）
   1.2 共用的参数（状态码、共用对象数据结构）
   1.3 返回数据说明
2. 接口方法列表
   2.1 本系统提供的接口
   2.2 本系统需要访问的第三方接口（可选）
3. 数据字典（自动从枚举类生成）
附录：Swagger/Knife4j 在线文档访问说明
```

### 特性

| 特性 | 说明 |
|------|------|
| **枚举类分析** | 自动扫描 `enums` 包，提取枚举项生成数据字典 |
| **共用对象识别** | 自动识别统一返回对象、分页对象 |
| **Knife4j 集成** | 提供在线文档地址，支持导出 Markdown/Word/HTML/PDF |
| **格式兼容** | 兼容传统接口文档格式要求 |

---

## 触发词

当用户使用以下命令时激活此技能：

| 命令 | 功能 |
|------|------|
| `/api-doc` | 生成接口设计文档 |
| `/deploy-doc` | 生成部署手册（含脚本） |
| `/ops-doc` | 生成运维手册 |
| `/java-docs` | 一键生成所有文档 |
| 生成文档 | 智能判断生成全部文档 |

---

## 工作流程

### 第一步：项目分析

分析以下文件获取项目信息：

| 文件 | 分析内容 |
|------|---------|
| `pom.xml` / `build.gradle` | 依赖版本、Spring Boot 版本、技术栈 |
| `src/main/resources/application*.yml` | 端口、数据源、Redis、配置项 |
| `src/main/java/**/controller/*.java` | Controller 类、API 路径、接口分组 |
| `src/main/java/**/dto/*.java` | 请求/响应对象，用于参数说明 |
| `src/main/java/**/enums/*.java` | 枚举类，用于生成数据字典 |
| `src/main/java/**/vo/*.java` | 响应对象，识别共用对象 |
| `src/main/java/**/config/*.java` | Swagger/Knife4j 配置 |
| `Dockerfile` | Docker 部署配置 |
| `scripts/*.sh` | 现有脚本（如果存在） |

### 第二步：生成文档

根据分析结果，在项目根目录创建 `docs/` 文件夹，并生成对应文档。

### 第三步：生成脚本（部署手册）

如果项目不存在启停脚本，自动生成以下脚本到 `scripts/` 目录：

| 脚本 | 说明 |
|------|------|
| `start.sh` | Linux 启动脚本 |
| `stop.sh` | Linux 停止脚本 |
| `restart.sh` | Linux 重启脚本 |
| `start.bat` | Windows 启动脚本 |

---

## 详细规范

点击查看各模块的详细实现规范：

| 主题 | 文件 | 内容概要 |
|------|------|---------|
| **接口分析** | [api-analysis.md](references/api-analysis.md) | Swagger 配置检测、接口扫描、访问地址生成 |
| **部署分析** | [deployment-analysis.md](references/deployment-analysis.md) | 环境依赖分析、部署方式判断、脚本生成 |
| **运维内容** | [ops-guide.md](references/ops-guide.md) | 监控指标、告警配置、故障排查流程 |

---

## 文档模板

使用以下模板生成标准文档：

| 模板 | 文件 | 用途 |
|------|------|------|
| **接口文档模板** | [templates/api-doc-template.md](templates/api-doc-template.md) | API 文档结构 |
| **部署手册模板** | [templates/deployment-template.md](templates/deployment-template.md) | 部署文档结构 |
| **运维手册模板** | [templates/ops-template.md](templates/ops-template.md) | 运维文档结构 |

---

## 使用示例

### 示例 1：生成所有文档

```
用户: /java-docs
```

执行步骤：
1. 分析项目结构和配置
2. 生成 `docs/api-doc.md`
3. 生成 `docs/deployment.md`
4. 生成 `docs/operations.md`
5. 如果不存在启停脚本，生成 `scripts/` 目录下的脚本

### 示例 2：仅生成接口文档

```
用户: /api-doc
```

执行步骤：
1. 分析配置文件获取端口
2. 检测 Swagger/Knife4j 配置
3. 扫描 Controller 获取接口清单
4. 生成 `docs/api-doc.md`

### 示例 3：生成部署手册

```
用户: /deploy-doc
```

执行步骤：
1. 分析依赖获取环境要求
2. 检测 Dockerfile 判断部署方式
3. 分析配置文件获取配置项
4. 生成 `docs/deployment.md`
5. 生成启停脚本（如果不存在）

---

## 注意事项

1. **接口文档格式**：采用客户要求的传统格式，包含接口规范、方法列表、数据字典三部分
2. **枚举类分析**：自动扫描 `enums` 包下的枚举类生成数据字典，支持 `code` + `desc` 模式
3. **共用对象识别**：自动识别统一返回对象（Result/CommonResult）、分页对象等
4. **Knife4j 导出**：提供在线文档访问地址，详细接口文档可通过 Knife4j 导出后合并
5. **部署方式自动检测**：根据是否存在 Dockerfile 自动判断部署方式
6. **脚本生成**：仅在 `scripts/` 目录不存在同名脚本时才生成
7. **配置安全**：生成文档时自动脱敏敏感配置（密码、密钥等）

---

## 版本历史

| 版本 | 日期 | 变更 |
|------|------|------|
| 1.1 | 2026-01-28 | 适配客户接口设计说明书格式，增加枚举类分析、共用对象识别 |
| 1.0 | 2026-01-28 | 初始版本，支持接口文档、部署手册、运维手册生成 |
