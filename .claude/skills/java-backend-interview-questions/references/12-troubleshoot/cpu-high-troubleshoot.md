# 线上 CPU 飙高如何排查？

## 排查流程

```
┌─────────────────────────────────────────────────────────────┐
│                    CPU 飙高排查流程                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. top 命令定位进程                                        │
│      ↓                                                      │
│   2. top -Hp 定位线程                                       │
│      ↓                                                      │
│   3. printf 转换线程 ID 为十六进制                          │
│      ↓                                                      │
│   4. jstack 导出线程堆栈                                    │
│      ↓                                                      │
│   5. 搜索对应线程，分析代码                                 │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 详细步骤

### Step 1: 定位 CPU 高的进程

```bash
# 查看系统负载和进程 CPU 使用情况
top

# 输出示例:
# PID    USER   PR  NI    VIRT    RES    SHR S  %CPU %MEM     TIME+ COMMAND
# 12345  root   20   0   10.5g   4.5g   128m S 300.0 28.0   5:23.45 java
```

### Step 2: 定位 CPU 高的线程

```bash
# 查看进程内的线程 CPU 使用情况
top -Hp <pid>
# 或
top -H -p <pid>

# 输出示例:
# PID    USER   PR  NI    VIRT    RES    SHR S  %CPU %MEM     TIME+ COMMAND
# 12367  root   20   0   10.5g   4.5g   128m R  98.0 28.0   1:23.45 java
# 12368  root   20   0   10.5g   4.5g   128m R  97.0 28.0   1:20.12 java
```

### Step 3: 转换线程 ID 为十六进制

```bash
# 将线程 ID 转换为十六进制 (jstack 用十六进制)
printf "%x\n" 12367
# 输出: 304f
```

### Step 4: 导出线程堆栈

```bash
# 导出线程堆栈到文件
jstack <pid> > thread_dump.txt

# 或直接搜索特定线程
jstack <pid> | grep -A 30 "0x304f"
```

### Step 5: 分析堆栈

```
┌─────────────────────────────────────────────────────────────┐
│                    堆栈分析示例                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   "pool-1-thread-5" #25 prio=5 os_prio=0 tid=0x00007f...    │
│      java.lang.Thread.State: RUNNABLE                       │
│        at java.util.regex.Pattern$GroupHead.match(...)      │
│        at java.util.regex.Pattern$Loop.match(...)           │
│        at java.util.regex.Pattern$GroupTail.match(...)      │
│        at java.util.regex.Pattern$Curly.match0(...)         │
│        at java.util.regex.Matcher.match(...)                │
│        at com.example.service.DataParser.parse(...)         │
│                                                             │
│   分析: 正则表达式匹配导致 CPU 飙高 (回溯过多)              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 一键脚本

```bash
#!/bin/bash
# cpu_analysis.sh - CPU 高排查一键脚本

# 获取 Java 进程 PID
PID=$(jps -l | grep -v Jps | awk '{print $1}' | head -1)

if [ -z "$PID" ]; then
    echo "No Java process found"
    exit 1
fi

echo "=== Java Process PID: $PID ==="

# 获取 CPU 最高的 5 个线程
echo -e "\n=== Top 5 CPU Threads ==="
top -Hp $PID -b -n 1 | head -12 | tail -5

# 获取线程堆栈
THREAD_IDS=$(top -Hp $PID -b -n 1 | head -12 | tail -5 | awk '{print $1}')

echo -e "\n=== Thread Stack Traces ==="
for TID in $THREAD_IDS; do
    HEX_TID=$(printf "%x" $TID)
    echo -e "\n--- Thread $TID (0x$HEX_TID) ---"
    jstack $PID | grep -A 15 "nid=0x$HEX_TID"
done
```

## 常见 CPU 飙高原因

```
┌─────────────────────────────────────────────────────────────┐
│                    常见原因                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 死循环                                                  │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  while (true) {                                      │  │
│   │      // 没有 sleep 或 break                          │  │
│   │  }                                                   │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   2. 正则表达式回溯                                         │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  // 贪婪匹配 + 回溯，CPU 爆炸                        │  │
│   │  "(a+)+b".matches(longString)                        │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   3. 频繁 Full GC                                           │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  jstat -gcutil <pid>                                 │  │
│   │  # FGC 列快速增长 → 内存不足或泄漏                   │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   4. 死锁导致 CPU 自旋                                      │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  jstack <pid> | grep -A 5 "deadlock"                 │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   5. 大量线程竞争锁                                         │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  jstack 中大量 BLOCKED 状态                          │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   6. 序列化/反序列化                                        │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  大对象的 JSON 序列化                                │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## Arthas 快速排查

```bash
# 启动 Arthas
java -jar arthas-boot.jar

# 查看仪表盘 (包含 CPU、内存、GC、线程)
dashboard

# 查看线程 CPU 使用情况
thread -n 5   # 显示 CPU 最高的 5 个线程

# 直接查看占用 CPU 最高的线程堆栈
thread -n 1

# 生成火焰图
profiler start
# 等待 30 秒
profiler stop --format html --file /tmp/flame.html
```

```
┌─────────────────────────────────────────────────────────────┐
│                    Arthas thread 输出                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   Threads Total: 45, NEW: 0, RUNNABLE: 15, BLOCKED: 0,      │
│   WAITING: 20, TIMED_WAITING: 10, TERMINATED: 0             │
│                                                             │
│   ID    NAME          GROUP      PRIORITY  STATE     %CPU   │
│   25    pool-1-5      main       5         RUNNABLE  98.5   │
│   26    pool-1-6      main       5         RUNNABLE  97.2   │
│                                                             │
│   点击线程 ID 可以查看完整堆栈                              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 火焰图分析

```bash
# 使用 async-profiler 生成火焰图
./profiler.sh -d 30 -f flame.html <pid>

# 火焰图解读:
# - X 轴: 方法调用栈 (宽度 = CPU 占用比例)
# - Y 轴: 调用深度
# - 越宽的函数，CPU 占用越多
# - 重点关注顶部最宽的函数
```

## 面试回答

### 30秒版本

> CPU 飙高排查步骤：1）`top` 找到 CPU 高的 Java 进程；2）`top -Hp <pid>` 找到 CPU 高的线程；3）`printf "%x"` 转十六进制；4）`jstack <pid>` 导出线程堆栈；5）搜索线程 ID 分析代码。常见原因：死循环、正则回溯、频繁 Full GC、锁竞争。也可用 Arthas 的 `thread -n 5` 快速定位。

### 1分钟版本

> **排查步骤**：
> ```bash
> # 1. 找进程
> top
> 
> # 2. 找线程
> top -Hp <pid>
> 
> # 3. 转十六进制
> printf "%x\n" <thread_id>
> 
> # 4. 导出堆栈
> jstack <pid> | grep -A 30 "0x<hex_tid>"
> ```
>
> **常见原因**：
> - 死循环（RUNNABLE 状态）
> - 正则表达式回溯
> - 频繁 Full GC（用 jstat 确认）
> - 锁竞争（BLOCKED 状态多）
>
> **快速工具**：
> - Arthas：`thread -n 5` 直接看 CPU 最高的线程
> - async-profiler：生成火焰图

---

*关联文档：[jvm-memory-analysis.md](../11-jvm/jvm-memory-analysis.md) | [jvm-oom-scenarios.md](../11-jvm/jvm-oom-scenarios.md)*
