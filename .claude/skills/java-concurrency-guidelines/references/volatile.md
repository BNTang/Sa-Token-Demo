# volatile 规范

> Java 并发编程规范 - volatile 关键字的语义、场景与限制

---

## 1. 核心语义

### 1.1 四层能力模型

| 层级 | 内容 |
|------|------|
| **语义层** | 保证变量在多线程间的可见性和禁止指令重排序 |
| **行为层** | 写操作立即刷新到主内存；读操作从主内存读取 |
| **影响层** | 无锁的轻量级同步，但不保证复合操作的原子性 |
| **决策层** | 适用于状态标志、单次发布；不适用于 i++ 等复合操作 |

### 1.2 是什么 / 不是什么

| 是什么 | 不是什么 |
|--------|---------|
| 可见性保证 | 原子性保证（复合操作不原子） |
| 禁止重排序 | 互斥锁（多线程可同时读写） |
| 轻量级同步 | synchronized 的替代品 |

### 1.3 一句话定义

> volatile = 可见性 + 有序性，但无原子性

---

## 2. 系统行为

### 2.1 内存屏障

| 操作 | 插入的屏障 | 效果 |
|------|-----------|------|
| volatile 写 | StoreStore + StoreLoad | 写前不重排，写后立即对其他线程可见 |
| volatile 读 | LoadLoad + LoadStore | 读后操作不会重排到读前 |

### 2.2 可见性原理

```
线程 A                          线程 B
   │                               │
   ▼                               │
[修改 volatile 变量]               │
   │                               │
   ▼                               │
[立即刷新到主内存]  ─────────────► [从主内存读取]
                                   │
                                   ▼
                              [获得最新值]
```

### 2.3 禁止重排序规则

| 第一个操作 | 第二个操作 | 能否重排 |
|-----------|-----------|---------|
| 普通读/写 | volatile 写 | ❌ 禁止 |
| volatile 写 | volatile 读 | ❌ 禁止 |
| volatile 读 | 普通读/写 | ❌ 禁止 |
| volatile 读 | volatile 写 | ❌ 禁止 |

---

## 3. 影响分析

### 3.1 正向收益

| 收益 | 说明 |
|------|------|
| 无锁 | 不会导致线程阻塞 |
| 轻量 | 比 synchronized 开销小 |
| 简洁 | 一个关键字搞定可见性 |

### 3.2 负向成本

| 成本 | 说明 |
|------|------|
| 不保证原子性 | i++ 仍然线程不安全 |
| 性能影响 | 每次读写都访问主内存，禁用 CPU 缓存优化 |
| 易误用 | 开发者常误以为 volatile 等于线程安全 |

---

## 4. 风险边界（Failure Modes）

### 4.1 典型误用

```java
// ❌ 错误：volatile 不能保证复合操作的原子性
private volatile int count = 0;

public void increment() {
    count++;  // 读取 → 加1 → 写回，三步可被打断
}

// ✅ 正确：使用 AtomicInteger
private AtomicInteger count = new AtomicInteger(0);

public void increment() {
    count.incrementAndGet();  // CAS 原子操作
}
```

### 4.2 隐性失败场景

| 场景 | 问题 | 解决方案 |
|------|------|---------|
| 依赖当前值的计算 | count = count + 1 不是原子操作 | Atomic 类或锁 |
| 多个 volatile 变量组合判断 | 组合判断不是原子的 | 锁或 CAS |
| volatile 数组 | 只保证引用可见，元素不保证 | 使用 AtomicReferenceArray |

---

## 5. 决策规则

### 5.1 适用条件（全部满足才能用 volatile）

| 条件 | 说明 |
|------|------|
| 写操作不依赖当前值 | 如 flag = true，而非 count++ |
| 或只有单一线程写入 | 一写多读场景 |
| 变量不参与不变式 | 不与其他变量联合约束 |

### 5.2 决策树

```
需要保证原子性吗？
├── 是 → 用 synchronized 或 Atomic 类
└── 否 → 只需保证可见性？
          ├── 是 → volatile
          └── 否 → 普通变量
```

### 5.3 典型场景

| 场景 | 代码示例 | 说明 |
|------|---------|------|
| 状态标志 | `volatile boolean running = true;` | 控制循环退出 |
| 单次安全发布 | `volatile Config config;` | 配置对象替换 |
| 双重检查锁定 | `volatile Singleton instance;` | 防止返回未初始化对象 |

### 5.4 禁用条件

| 场景 | 原因 | 替代方案 |
|------|------|---------|
| i++、i-- | 复合操作不原子 | AtomicInteger |
| a = a + b | 依赖当前值 | synchronized |
| 多变量联合判断 | 组合不是原子的 | synchronized |

---

## 6. volatile vs synchronized

| 特性 | volatile | synchronized |
|------|----------|--------------|
| 原子性 | ❌ 复合操作不保证 | ✅ |
| 可见性 | ✅ | ✅ |
| 有序性 | ✅ | ✅ |
| 阻塞 | ❌ 不阻塞 | ✅ 可能阻塞 |
| 使用场景 | 状态标志、一写多读 | 临界区保护 |
| 性能 | 轻量 | 相对重量 |

---

## 7. 典型代码模式

### 7.1 状态标志

```java
// ✅ 正确用法：停止标志
private volatile boolean running = true;

public void run() {
    while (running) {  // 读取 volatile
        doWork();
    }
}

public void stop() {
    running = false;  // 写入 volatile，立即对其他线程可见
}
```

### 7.2 双重检查锁定（DCL）单例

```java
public class Singleton {
    // 必须用 volatile，防止指令重排导致返回未初始化的对象
    private static volatile Singleton instance;
    
    public static Singleton getInstance() {
        if (instance == null) {                 // 第一次检查（无锁）
            synchronized (Singleton.class) {
                if (instance == null) {         // 第二次检查（有锁）
                    instance = new Singleton(); // volatile 防止重排序
                }
            }
        }
        return instance;
    }
}
```

**为什么必须用 volatile？**

| 不用 volatile 的问题 | 说明 |
|---------------------|------|
| `instance = new Singleton()` 包含三步 | 1. 分配内存 2. 初始化对象 3. 引用赋值 |
| 步骤 2 和 3 可能重排序 | 先赋值引用，再初始化 |
| 其他线程可能拿到未初始化的对象 | instance != null 但字段还是默认值 |

### 7.3 配置热更新

```java
// ✅ 正确用法：配置对象替换
private volatile Config config;

public Config getConfig() {
    return config;  // 总是读取最新配置
}

public void updateConfig(Config newConfig) {
    config = newConfig;  // 新配置立即对所有线程可见
}
```

---

## 8. 记忆锚点

| 概念 | 记忆句（≤20字） |
|------|----------------|
| volatile 核心 | 可见 + 有序，无原子 |
| 适用场景 | 状态标志、一写多读 |
| 禁用场景 | i++ 不行，要用 Atomic |
| DCL 单例 | 必须 volatile，否则拿到半成品 |
