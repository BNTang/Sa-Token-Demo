---
name: golang-guidelines
description: Go 语言编码规范最佳实践。涵盖命名约定、代码格式、错误处理、并发控制、接口设计、项目结构等。当编写 Go 代码或进行代码审查时使用。
metadata:
  author: skill-hub
  version: "1.0"
  compatibility: Go 1.18+
---

# Go 语言编码规范

> 版本: 1.0 | 更新: 2026-02-02
>
> 参考：Go 官方规范、Uber Go Style Guide、Google Go Style Guide

---

## 概述

本规范适用于 Go 语言项目开发，旨在确保代码质量、可维护性和团队协作效率。

### 技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| Go | 1.18+ | 支持泛型 |
| Go Modules | 默认 | 依赖管理 |
| gofmt/goimports | 最新 | 代码格式化 |

---

## 何时使用此 Skill

| 场景 | 触发词 |
|------|--------|
| 编写代码 | 写 Go、写 Golang、写函数、写结构体 |
| 命名咨询 | Go 命名、变量名、函数名、包名 |
| 代码审查 | Go 代码审查、code review |
| 最佳实践 | Go 最佳实践、规范、标准写法 |
| 并发编程 | goroutine、channel、sync |
| 错误处理 | Go 错误处理、error handling |

---

## 快速参考

### 核心规范速查

| 规范 | 要点 |
|------|------|
| **命名** | 驼峰命名、导出用大写开头、包名小写单词 |
| **格式** | 强制使用 `gofmt`，Tab 缩进 |
| **错误处理** | 必须处理 error、使用 `errors.Wrap` 添加上下文 |
| **并发** | 使用 context 传递取消信号、避免裸 goroutine |
| **接口** | 小接口原则、接口放调用方 |

### 禁止项速查

| ❌ 禁止 | ✅ 正确做法 |
|--------|-----------|
| 忽略 error `_ = fn()` | 处理或显式忽略并注释原因 |
| 裸 goroutine `go fn()` | 使用 errgroup 或 recover |
| `init()` 中复杂逻辑 | 显式初始化函数 |
| 导出全局变量 | 使用函数封装访问 |
| `panic` 替代 error | 仅在不可恢复时 panic |
| 包级别 context | context 作为第一参数传递 |

---

## 详细规范目录

| 主题 | 文件 | 内容概要 |
|------|------|---------|
| **命名规范** | [naming.md](references/naming.md) | 变量、函数、结构体、包、接口命名 |
| **代码格式** | [code-format.md](references/code-format.md) | gofmt、行长度、空行、注释 |
| **错误处理** | [error-handling.md](references/error-handling.md) | error 返回、错误包装、自定义错误 |
| **日志规范** | [logging.md](references/logging.md) | 结构化日志、日志级别、上下文 |
| **并发控制** | [concurrency.md](references/concurrency.md) | goroutine、channel、sync、context |
| **接口设计** | [interface.md](references/interface.md) | 小接口、隐式实现、接口位置 |
| **测试规范** | [testing.md](references/testing.md) | 表驱动测试、Mock、Benchmark |
| **性能优化** | [performance.md](references/performance.md) | 内存分配、切片预分配、字符串拼接 |
| **安全规范** | [security.md](references/security.md) | 输入校验、SQL 注入、敏感数据 |
| **项目结构** | [project-structure.md](references/project-structure.md) | 标准布局、/cmd, /internal, /pkg |

---

## 代码评审 Checklist

### 必查项

| 检查点 | 说明 |
|--------|------|
| **命名规范** | 是否符合 Go 命名惯例 |
| **错误处理** | error 是否被正确处理或包装 |
| **并发安全** | goroutine 是否有 recover，资源是否正确释放 |
| **context 使用** | 是否正确传递和使用 context |
| **接口设计** | 接口是否足够小，是否放在调用方 |
| **测试覆盖** | 核心逻辑是否有单元测试 |

---

## 版本历史

| 版本 | 日期 | 变更 |
|------|------|------|
| 1.0 | 2026-02-02 | 初始版本 |
