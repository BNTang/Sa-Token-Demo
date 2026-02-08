---
name: java-concurrency-guidelines
description: |
  Java 并发编程规范与最佳实践。
  
  涵盖锁机制（synchronized、ReentrantLock、读写锁）、Java 内存模型（JMM）、
  volatile 关键字、ThreadLocal、happens-before 规则、指令重排序等核心并发知识。
  
  遵循"四层能力模型"：语义层、行为层、影响层、决策层，确保每个知识点都能
  回答"是什么、什么时候用、用错的代价"三个核心问题。
  
  使用场景：编写并发代码、代码审查、面试准备、排查并发问题时使用。
  触发词：并发、锁、synchronized、ReentrantLock、volatile、ThreadLocal、JMM

user-invocable: true
allowed-tools: Read, Grep, Glob, Write, Edit
metadata:
  version: "1.0"
  author: skill-hub
  compatibility: Java 8+
---

# Java 并发编程规范

> 版本: 1.0 | 更新: 2026-02-03
>
> 本规范基于 Java 并发编程核心机制，提供可落地的工程决策指导。

---

## 概述

本 Skill 将 Java 并发编程的核心概念转化为**可稳定做出正确工程决策的能力**。

### 技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| Java | 8+ | 基础并发 API |
| Java | 6+ | 锁升级机制、自适应自旋 |
| Java | 5+ | JUC 包、ReentrantLock |

---

## 必须说明（三要素）

- **做了什么**：将锁、内存模型、线程隔离等并发机制转化为明确的决策规则
- **为什么需要**：并发 Bug 隐蔽、难以复现、代价巨大（数据不一致、死锁、性能崩溃）
- **什么时候必须用**：多线程共享可变状态、需要保证可见性/原子性/有序性时

---

## 何时使用此 Skill

| 场景 | 触发词 |
|------|--------|
| 编写并发代码 | synchronized、锁、并发、多线程 |
| 选择锁类型 | 用什么锁、ReentrantLock 还是 synchronized |
| 内存可见性 | volatile、可见性、JMM |
| 线程隔离 | ThreadLocal、线程本地变量 |
| 代码审查 | 并发审查、线程安全检查 |
| 性能优化 | 锁优化、减少锁竞争 |

---

## 快速参考

### 核心决策速查

| 场景 | 选择 | 原因 |
|------|------|------|
| 简单同步 | `synchronized` | 自动释放、JVM 优化好 |
| 需要可中断/超时 | `ReentrantLock` | 提供 tryLock、lockInterruptibly |
| 需要公平锁 | `ReentrantLock(true)` | synchronized 不支持公平 |
| 读多写少 | `ReadWriteLock` | 读锁共享、写锁排他 |
| 只需可见性 | `volatile` | 无锁、轻量 |
| 线程隔离数据 | `ThreadLocal` | 空间换时间，避免锁 |

### 禁止项速查

| ❌ 禁止 | ✅ 正确做法 | 代价 |
|--------|-----------|------|
| 锁大范围代码 | 只锁必要临界区 | 性能下降 |
| Lock 不 finally 释放 | `try { } finally { unlock(); }` | 死锁 |
| ThreadLocal 不 remove | `finally { threadLocal.remove(); }` | 内存泄漏 |
| volatile 用于复合操作 | 用 Atomic 类或锁 | 原子性丢失 |
| 在构造函数中逸出 this | 构造完成后再发布 | final 可见性失效 |

---

## 详细规范目录

| 主题 | 文件 | 内容概要 |
|------|------|---------|
| **锁机制** | [locks.md](references/locks.md) | synchronized、ReentrantLock、读写锁的原理与选择 |
| **内存模型** | [jmm.md](references/jmm.md) | JMM、原子性/可见性/有序性、happens-before |
| **volatile** | [volatile.md](references/volatile.md) | volatile 语义、使用场景、限制 |
| **ThreadLocal** | [threadlocal.md](references/threadlocal.md) | 实现原理、弱引用机制、最佳实践 |
| **锁优化** | [lock-optimization.md](references/lock-optimization.md) | 锁升级、自旋、优化策略 |

---

## 代码评审 Checklist

### 并发必查项

| 检查点 | 说明 | 严重级别 |
|--------|------|---------|
| **锁释放** | Lock 是否在 finally 中释放 | 🔴 致命 |
| **ThreadLocal 清理** | 是否调用 remove() | 🔴 致命 |
| **volatile 误用** | 是否用于复合操作（i++） | 🔴 致命 |
| **锁粒度** | 是否锁范围过大 | 🟡 性能 |
| **死锁风险** | 是否按固定顺序获取多把锁 | 🔴 致命 |
| **双重检查锁定** | 单例是否用 volatile 修饰 | 🔴 致命 |

---

## 版本历史

| 版本 | 日期 | 变更 |
|------|------|------|
| 1.0 | 2026-02-03 | 初始版本：锁机制、JMM、volatile、ThreadLocal |
