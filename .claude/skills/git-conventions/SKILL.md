---
name: git-conventions
description: Git 使用规范指南，包括分支管理、提交信息格式、工作流程和最佳实践。当用户需要创建分支、编写 commit message、管理 Git 仓库或询问 Git 最佳实践时使用。
---

# Git 规范指南

> 版本: 1.0 | 更新: 2026-02-04

## 概述

本 Skill 提供完整的 Git 使用规范，帮助团队保持一致的版本控制实践，提高代码协作效率。

---

## 分支管理规范

### 分支命名约定

| 分支类型 | 命名格式 | 示例 |
|---------|---------|------|
| 主分支 | `main` 或 `master` | `main` |
| 开发分支 | `develop` | `develop` |
| 功能分支 | `feature/<描述>` | `feature/user-authentication` |
| 修复分支 | `fix/<描述>` 或 `bugfix/<描述>` | `fix/login-validation` |
| 热修复分支 | `hotfix/<描述>` | `hotfix/security-patch` |
| 发布分支 | `release/<版本号>` | `release/v1.2.0` |
| 文档分支 | `docs/<描述>` | `docs/api-documentation` |
| 重构分支 | `refactor/<描述>` | `refactor/database-layer` |

### 分支命名规则

- ✅ 使用小写字母和连字符
- ✅ 简洁但具有描述性
- ✅ 可包含 Issue 编号：`feature/123-user-login`
- ❌ 避免使用空格、下划线或特殊字符
- ❌ 避免过长的分支名（建议不超过 50 字符）

### 分支生命周期

```
main
  │
  ├── develop
  │     │
  │     ├── feature/xxx ──────┐
  │     │                     │ (合并后删除)
  │     ├── fix/xxx ──────────┤
  │     │                     │
  │     └─────────────────────┘
  │
  └── hotfix/xxx ──► main (紧急修复直接合并)
```

---

## Commit Message 规范

### 基础格式（Conventional Commits）

```
<type>(<scope>): <subject>

[body]

[footer]
```

### Type（类型）

| 类型 | 说明 | 示例 |
|-----|------|------|
| `feat` | 新功能 | `feat: add user registration` |
| `fix` | Bug 修复 | `fix: resolve login timeout issue` |
| `docs` | 文档更新 | `docs: update API documentation` |
| `style` | 代码格式（不影响逻辑） | `style: format with prettier` |
| `refactor` | 重构（不改变功能） | `refactor: simplify auth logic` |
| `perf` | 性能优化 | `perf: optimize database queries` |
| `test` | 测试相关 | `test: add unit tests for user service` |
| `chore` | 构建/工具变更 | `chore: update dependencies` |
| `ci` | CI 配置 | `ci: add GitHub Actions workflow` |
| `build` | 构建系统变更 | `build: upgrade webpack to v5` |
| `revert` | 回滚提交 | `revert: revert commit abc123` |

### Scope（范围）- 可选

指明改动影响的模块或组件：

```
feat(auth): implement OAuth2 login
fix(api): handle null response gracefully
docs(readme): add installation guide
```

### Subject（主题）规则

| 规则 | 说明 |
|-----|------|
| 使用祈使句 | "add feature" 而非 "added feature" |
| 首字母小写 | "add" 而非 "Add" |
| 不加句号 | 结尾不需要标点 |
| 限制长度 | 不超过 50 字符 |

### Body（正文）- 可选

用于解释**为什么**做这个改动：

```
feat(auth): add two-factor authentication

Two-factor authentication improves account security
by requiring a second verification step during login.

- Support TOTP-based authenticators
- Add backup codes for account recovery
- Rate limit verification attempts
```

### Footer（页脚）- 可选

用于关联 Issue 或标记破坏性变更：

```
feat(api): redesign user endpoint

BREAKING CHANGE: User endpoint now requires authentication.
The /api/users endpoint no longer supports anonymous access.

Closes #123
Refs #456, #789
```

### 完整示例

```
feat(user): add password reset functionality

Implement password reset flow with email verification.
Users can now request a password reset link via email.

- Add password reset request endpoint
- Implement email token generation
- Add token expiration (24 hours)
- Create password reset confirmation page

Closes #234
```

---

## 工作流程

### Feature Branch 工作流

```bash
# 1. 创建功能分支
git checkout develop
git pull origin develop
git checkout -b feature/new-feature

# 2. 开发并提交
git add .
git commit -m "feat: implement new feature"

# 3. 保持与 develop 同步
git fetch origin
git rebase origin/develop

# 4. 推送并创建 PR
git push origin feature/new-feature
```

### Git Flow 工作流

```
main ─────────────────────────────────────► (生产环境)
  │                                    ▲
  │                                    │
  ▼                                    │
develop ──► feature/* ──► develop ──► release/* ──► main
                                         │
                                         ▼
                                      hotfix/*
```

### Trunk-Based 工作流

```bash
# 短生命周期的功能分支
git checkout -b feature/small-change
# 开发（1-2 天内完成）
git commit -m "feat: small improvement"
git push origin feature/small-change
# 快速 review 后合并到 main
```

---

## 最佳实践

### 提交频率

| ✅ 推荐 | ❌ 避免 |
|--------|--------|
| 小而频繁的提交 | 大量改动一次提交 |
| 每个提交完成一个逻辑单元 | 混合多个不相关的改动 |
| 可编译/可运行的状态 | 提交损坏的代码 |

### 代码审查前

```bash
# 整理提交历史
git rebase -i origin/develop

# 检查改动
git diff origin/develop

# 确保无冲突
git fetch origin
git rebase origin/develop
```

### .gitignore 必备

```gitignore
# 依赖目录
node_modules/
vendor/
venv/

# 构建产物
dist/
build/
*.exe

# IDE 配置
.idea/
.vscode/
*.swp

# 环境变量
.env
.env.local
*.env

# 日志和临时文件
*.log
*.tmp
.cache/

# 系统文件
.DS_Store
Thumbs.db
```

### Git Hooks 推荐

```bash
# pre-commit: 代码格式化和 lint
# commit-msg: 验证 commit message 格式
# pre-push: 运行测试
```

---

## 常见问题处理

### 撤销最近的提交

```bash
# 保留改动，撤销提交
git reset --soft HEAD~1

# 完全撤销（慎用）
git reset --hard HEAD~1
```

### 修改最近的提交信息

```bash
git commit --amend -m "fix: correct commit message"
```

### 解决合并冲突

```bash
# 1. 查看冲突文件
git status

# 2. 手动编辑解决冲突

# 3. 标记为已解决
git add <file>

# 4. 继续合并/变基
git merge --continue
# 或
git rebase --continue
```

### 同步 Fork 仓库

```bash
git remote add upstream <原仓库地址>
git fetch upstream
git merge upstream/main
```

---

## 团队协作规范

### Pull Request 规范

1. **标题**：遵循 Commit Message 格式
2. **描述**：说明改动内容和原因
3. **关联 Issue**：使用 `Closes #123` 关联
4. **自检清单**：
   - [ ] 代码已通过 lint 检查
   - [ ] 已添加必要的测试
   - [ ] 已更新相关文档
   - [ ] 无合并冲突

### Code Review 原则

- 及时 Review（24 小时内）
- 建设性反馈
- 关注代码质量而非风格偏好
- 使用 Suggest 功能提供改进建议
