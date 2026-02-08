---
name: skill-authoring
description: Agent Skills 编写指南。当用户需要创建、编写 Skill 或构建 SKILL.md 文件时使用。支持中英文 description，英语不佳用户可使用中文描述。
---

# Skill 编写指南

> 版本: 1.0 | 更新: 2026-01-28

## 概述

本 Skill 提供编写 Agent Skills 的完整指南，帮助创建符合 Agent Skills 规范的 SKILL.md 文件。

**支持中文**：`description` 字段支持中文，英语不佳的用户可以放心使用中文描述。

---

## 什么是 Agent Skills

Agent Skills 是一种轻量级、开放格式，用于通过专业知识和工作流扩展 AI agent 的能力。

**核心概念**：一个 Skill 就是一个包含 `SKILL.md` 文件的文件夹，该文件包含元数据（`name` 和 `description`）和指令。

### 目录结构

```
skill-name/
├── SKILL.md          # 必需：指令 + 元数据
├── scripts/          # 可选：可执行代码
├── references/       # 可选：文档
└── assets/           # 可选：模板、资源
```

---

## SKILL.md 文件格式

### Frontmatter（必需）

```yaml
---
name: skill-name
description: A description of what this skill does and when to use it.
---
```

#### name 字段规则

| 规则 | 说明 |
|------|------|
| 长度 | 1-64 字符 |
| 字符 | 仅限小写字母、数字和连字符 (`a-z`, `0-9`, `-`) |
| 禁止 | 不能以 `-` 开头或结尾，不能包含连续的 `--` |
| 匹配 | 必须与父目录名一致 |

**有效示例：**
```yaml
name: pdf-processing
name: data-analysis
name: code-review
```

**无效示例：**
```yaml
name: PDF-Processing    # ❌ 大写字母
name: -pdf              # ❌ 以连字符开头
name: pdf--processing   # ❌ 连续连字符
```

#### description 字段规则

| 规则 | 说明 |
|------|------|
| 长度 | 1-1024 字符 |
| 内容 | 描述技能功能和使用场景 |
| 建议 | 包含特定关键词，帮助 AI 识别相关任务 |
| **语言** | **支持中英文，建议使用你最熟悉的语言** |

**好的示例（英文）：**
```yaml
description: Extracts text and tables from PDF files, fills PDF forms, and merges multiple PDFs. Use when working with PDF documents or when the user mentions PDFs, forms, or document extraction.
```

**好的示例（中文）：**
```yaml
description: 从 PDF 文件中提取文本和表格、填充 PDF 表单、合并多个 PDF 文档。当用户处理 PDF 文档或提及 PDF、表单、文档提取时使用。
```

**不好的示例：**
```yaml
description: Helps with PDFs.    # ❌ 太模糊
description: 帮助处理 PDF。      # ❌ 太模糊
```

### 可选字段

```yaml
---
name: pdf-processing
description: Extract text and tables from PDF files...
license: Apache-2.0
compatibility: Requires Python 3.10+, pdfplumber package
metadata:
  author: example-org
  version: "1.0"
---
```

| 字段 | 说明 |
|------|------|
| `license` | 许可证名称或引用文件 |
| `compatibility` | 环境要求（最多 500 字符） |
| `metadata` | 自定义键值对 |
| `allowed-tools` | 预批准工具列表（实验性） |

---

## Body 内容规范

Frontmatter 后的 Markdown 主体包含技能指令，没有格式限制。建议包含以下部分：

### 推荐结构

```markdown
# [Skill 名称]

> 版本: x.x | 更新: YYYY-MM-DD

## 概述
简要描述技能的用途和目标。

## 使用场景
说明何时激活此技能。

## 操作步骤
1. 步骤一
2. 步骤二
3. 步骤三

## 示例
### 输入示例
```
示例输入
```

### 输出示例
```
示例输出
```

## 边缘情况
列出常见问题和处理方式。

## 参考
引用相关文档或资源。
```

---

## 编写最佳实践

### 1. 技能专注性

每个 Skill 应专注于解决一个特定问题：

| ✅ 好的做法 | ❌ 避免 |
|------------|--------|
| `pdf-text-extract` - 专注 PDF 文本提取 | `pdf-helper` - 含糊不清 |
| `code-review-security` - 专注安全审查 | `code-stuff` - 范围过广 |

### 2. 清晰的描述

`description` 是 AI 决定是否激活技能的关键依据。**支持使用中文**：

```yaml
# ✅ 好的描述（英文）：具体、包含触发词
description: Review code for security vulnerabilities including SQL injection, XSS, and CSRF. Use when user asks for security audit, vulnerability scan, or security review.

# ✅ 好的描述（中文）：具体、包含触发词
description: 审查代码安全漏洞，包括 SQL 注入、XSS 和 CSRF。当用户要求安全审计、漏洞扫描或安全审查时使用。

# ❌ 差的描述：模糊、无触发词
description: Helps with code security.
description: 帮助处理代码安全。
```

### 3. 渐进式披露 (Progressive Disclosure)

Skills 应按以下层级组织，以高效使用上下文：

| 层级 | 内容 | Token 预估 |
|------|------|-----------|
| **元数据** | name + description | ~100 tokens |
| **指令** | SKILL.md 主体 | < 5000 tokens |
| **资源** | scripts/references/assets | 按需加载 |

**建议**：保持 SKILL.md 主体在 500 行以内，详细参考材料拆分到单独文件。

### 4. 文件引用

引用其他文件时使用相对路径：

```markdown
See [the reference guide](references/REFERENCE.md) for details.

Run the extraction script:
```bash
scripts/extract.py
```
```

保持引用层级不超过一级（从 SKILL.md 到直接引用的文件）。

---

## 可选目录使用

### scripts/ - 可执行代码

包含 agent 可以运行的脚本：

| 要求 | 说明 |
|------|------|
| 自包含 | 或清晰文档化依赖 |
| 错误处理 | 包含有用的错误消息 |
| 边缘情况 | 优雅处理边界情况 |

支持的语言取决于 agent 实现，常见选项：Python、Bash、JavaScript。

### references/ - 额外文档

| 文件 | 用途 |
|------|------|
| `REFERENCE.md` | 详细技术参考 |
| `FORMS.md` | 表单模板或结构化数据格式 |
| 领域文件 | 如 `finance.md`, `legal.md` 等 |

保持单个参考文件聚焦，agent 按需加载。

### assets/ - 静态资源

| 类型 | 示例 |
|------|------|
| 模板 | 文档模板、配置模板 |
| 图片 | 图表、示例图 |
| 数据 | 查找表、schema |

---

## 常见模式

### 模式 1：编码规范 Skill

```markdown
---
name: java-spring-guidelines
description: Java and Spring Boot coding standards and best practices. Use when writing Java code, Spring Boot controllers, services, or mappers.
---

# Java/Spring Boot 编码规范

## 命名约定
### 类命名
| 类型 | 规则 | 示例 |
|-----|------|-----|
| Controller | `*Controller` | `ProductController` |
| Service 接口 | `I*Service` | `IProductService` |
...
```

### 模式 2：工具使用 Skill

```markdown
---
name: git-workflow
description: Git workflow automation for branch management, commit creation, and pull requests. Use when user needs to create branches, commit code, or manage PRs.
---

## Git 工作流

### 创建分支
1. 从 main 分支创建功能分支
2. 使用命名约定：`feature/issue-description`
...
```

### 模式 3：领域知识 Skill

```markdown
---
name: api-integration
description: Integration guide for external APIs including authentication, rate limiting, and error handling. Use when building API clients or integrating third-party services.
---

## API 集成指南

### 认证方式
- OAuth 2.0
- API Key
- JWT Bearer Token
...
```

---

## 验证 Skill

使用 skills-ref 库验证技能：

```bash
skills-ref validate ./my-skill
```

检查项目：
- SKILL.md frontmatter 有效性
- 命名约定合规性
- 目录结构正确性

---

## 激活率优化

如果发现 AI 不常使用某个 Skill，可以：

### 1. 优化 description

```yaml
# 前：模糊（英文）
description: Helps with database queries.

# 后：具体（英文）
description: Write efficient SQL queries, optimize database performance, and handle N+1 problems. Use when user mentions database, SQL, query optimization, or performance issues.

# 前：模糊（中文）
description: 帮助处理数据库查询。

# 后：具体（中文）
description: 编写高效的 SQL 查询、优化数据库性能、处理 N+1 问题。当用户提及数据库、SQL、查询优化或性能问题时使用。
```

### 2. 添加触发词

在 description 中包含用户可能使用的关键词：

| 类别 | 英文关键词 | 中文关键词 |
|------|-----------|-----------|
| 动作词 | create, build, review, analyze | 创建、构建、审查、分析 |
| 领域词 | PDF, API, database, security | PDF、接口、数据库、安全 |
| 场景词 | error, bug, optimization | 错误、漏洞、优化 |

**中英文混用建议**：如果你的团队使用中英文混合交流，可以在 description 中同时包含两种语言的关键词：

```yaml
description: PDF 文档处理：提取文本、填充表单、合并文档。Extract text, fill forms, merge PDF documents. 当用户提及 PDF/文档处理 或 mentions PDF/documents 时使用。
```

### 3. 在 CLAUDE.md 中强调

项目根目录的 CLAUDE.md 可以强制要求：

```markdown
When working on Java/Spring code, you MUST consult the java-spring-guidelines skill FIRST before making any changes.
```

---

## 完整示例

### 示例 1：英文版

```markdown
---
name: code-review-checklist
description: Code review checklist covering security, performance, and best practices. Use when user asks for code review, PR review, or quality checks.
metadata:
  author: dev-team
  version: "1.0"
---

# Code Review Checklist

> 版本: 1.0 | 更新: 2026-01-28

## 安全检查

- [ ] 无 SQL 注入风险（使用参数化查询）
- [ ] 无 XSS 漏洞（输入验证和输出编码）
- [ ] 敏感数据已脱敏
- [ ] 认证授权正确实现

## 何时使用

When user asks for:
- Code review / PR review
- Quality check / 代码审查
- Security audit / 安全审计
```

### 示例 2：中文版（推荐英语不佳用户使用）

```markdown
---
name: java-spring-guidelines
description: Java 和 Spring Boot 编码规范最佳实践。包括命名约定、依赖注入、日志规范、异常处理、Controller/Service/Mapper 层规范、缓存、事务控制、数据库设计等。当编写 Java 代码、Spring Boot 控制器、服务层或映射器时使用。
metadata:
  author: dev-team
  version: "1.0"
---

# Java/Spring Boot 编码规范

> 版本: 1.0 | 更新: 2026-01-28

## 命名约定

### 类命名
| 类型 | 规则 | 示例 |
|-----|------|-----|
| Controller | `*Controller` | `ProductController` |
| Service 接口 | `I*Service` | `IProductService` |
| Service 实现 | `*ServiceImpl` | `ProductServiceImpl` |
| Mapper | `*Mapper` | `ProductMapper` |

## 何时使用

当用户进行以下操作时激活此技能：
- 编写 Java / Spring Boot 代码
- 创建 Controller / Service / Mapper
- 提问：如何命名、怎么写日志、异常怎么处理
- 要求：代码规范、最佳实践
```

---

## 快速检查清单

在创建新 Skill 前，确认：

| 检查项 | 说明 |
|--------|------|
| ✅ name 符合规范 | 小写、无特殊字符、与目录名一致 |
| ✅ description 清晰具体 | 说明功能和使用场景，包含关键词 |
| ✅ **语言选择** | **description 支持中文，使用你最熟悉的语言** |
| ✅ 专注单一职责 | 一个 Skill 解决一个问题 |
| ✅ 结构清晰 | 有概述、步骤、示例 |
| ✅ 长度适中 | SKILL.md 主体 < 500 行 |
| ✅ 可验证 | 使用 `skills-ref validate` 通过 |

---

## 语言选择建议

| 场景 | 建议语言 |
|------|---------|
| 你的团队主要使用中文交流 | **中文 description** |
| 你的团队主要使用英文交流 | 英文 description |
| 国际化/开源项目 | 英文 description |
| 不确定 | **中英文双语**（两种关键词都包含） |

**重要**：Claude 等 AI 模型对中文的理解能力很好，使用中文 description 不会影响技能激活效果。
