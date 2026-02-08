# 你使用过哪些 JDK 提供的工具？

## JDK 工具概览

```
┌─────────────────────────────────────────────────────────────┐
│                    JDK 工具分类                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   编译与构建                诊断与监控                       │
│   ├── javac                ├── jps                          │
│   ├── jar                  ├── jstack                       │
│   ├── javadoc              ├── jmap                         │
│   └── jlink                ├── jstat                        │
│                            ├── jinfo                        │
│   调试工具                  ├── jcmd                         │
│   ├── jdb                  ├── jconsole                     │
│   └── jhsdb                └── jvisualvm                    │
│                                                             │
│   性能分析                  安全工具                         │
│   ├── jfr                  ├── keytool                      │
│   └── jmc                  └── jarsigner                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 常用诊断工具详解

### 1. jps - Java 进程状态

```bash
# 列出 Java 进程
jps
# 输出: 12345 Main

# 显示完整类名
jps -l
# 输出: 12345 com.example.Main

# 显示 JVM 参数
jps -v
# 输出: 12345 Main -Xmx512m -Xms256m

# 显示传递给 main 的参数
jps -m
```

### 2. jstack - 线程堆栈 ⭐重点

```bash
# 查看线程堆栈 (排查死锁、线程阻塞)
jstack <pid>

# 强制输出
jstack -F <pid>

# 常见用法: 排查 CPU 飙高
# 1. top -Hp <pid> 找到高 CPU 线程
# 2. 转换为 16 进制: printf "%x\n" <nid>
# 3. jstack <pid> | grep <nid> -A 30
```

```
┌─────────────────────────────────────────────────────────────┐
│                    jstack 输出示例                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   "main" #1 prio=5 os_prio=0 tid=0x00007f... nid=0x1a3      │
│        java.lang.Thread.State: RUNNABLE                      │
│        at com.example.Main.process(Main.java:25)            │
│        at com.example.Main.main(Main.java:10)               │
│                                                             │
│   Found one Java-level deadlock:                            │
│   =============================                              │
│   "Thread-1":                                                │
│     waiting to lock monitor 0x00007f... (object 0x...)      │
│     which is held by "Thread-0"                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 3. jmap - 内存映射 ⭐重点

```bash
# 堆内存统计
jmap -heap <pid>

# 对象统计 (按类)
jmap -histo <pid>

# 只看存活对象 (会触发 Full GC)
jmap -histo:live <pid>

# 导出堆转储文件 (最常用)
jmap -dump:format=b,file=heap.hprof <pid>

# 发生 OOM 时自动导出 (JVM 参数)
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/path/to/dump
```

### 4. jstat - GC 统计 ⭐重点

```bash
# 查看 GC 统计 (每秒刷新)
jstat -gc <pid> 1000

# 输出说明:
# S0C/S1C  - Survivor 0/1 容量
# S0U/S1U  - Survivor 0/1 已使用
# EC/EU    - Eden 容量/已使用
# OC/OU    - Old 容量/已使用
# MC/MU    - Metaspace 容量/已使用
# YGC/YGCT - Young GC 次数/时间
# FGC/FGCT - Full GC 次数/时间

# 查看 GC 比例
jstat -gcutil <pid> 1000

# 查看类加载
jstat -class <pid>
```

### 5. jinfo - 配置信息

```bash
# 查看所有 JVM 参数
jinfo -flags <pid>

# 查看特定参数
jinfo -flag MaxHeapSize <pid>

# 动态修改参数 (部分支持)
jinfo -flag +HeapDumpOnOutOfMemoryError <pid>
```

### 6. jcmd - 诊断命令

```bash
# 列出可用命令
jcmd <pid> help

# 线程堆栈 (替代 jstack)
jcmd <pid> Thread.print

# 堆统计
jcmd <pid> GC.heap_info

# 触发 GC
jcmd <pid> GC.run

# 导出堆转储
jcmd <pid> GC.heap_dump /path/to/dump.hprof

# 查看 JVM 参数
jcmd <pid> VM.flags
```

## 可视化工具

```
┌─────────────────────────────────────────────────────────────┐
│                    可视化工具                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   JConsole:                                                  │
│   • JDK 自带                                                │
│   • 基于 JMX                                                │
│   • 监控内存、线程、CPU                                     │
│   • 命令: jconsole                                          │
│                                                             │
│   VisualVM:                                                  │
│   • 更强大的可视化工具                                      │
│   • 支持插件扩展                                            │
│   • 可分析 heapdump                                         │
│   • JDK 9+ 需要单独下载                                     │
│                                                             │
│   JMC (Java Mission Control):                               │
│   • 生产级监控工具                                          │
│   • 配合 JFR (Flight Recorder)                              │
│   • 低开销，适合生产环境                                    │
│                                                             │
│   MAT (Eclipse Memory Analyzer):                            │
│   • 分析 heapdump                                           │
│   • 查找内存泄漏                                            │
│   • 不是 JDK 工具，但常用                                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 第三方工具

```
┌─────────────────────────────────────────────────────────────┐
│                    常用第三方工具                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   Arthas (阿里开源):                                         │
│   ├── 线上诊断神器                                          │
│   ├── 方法追踪、热替换                                      │
│   ├── watch/trace/monitor                                   │
│   └── 命令: java -jar arthas-boot.jar                       │
│                                                             │
│   Async-profiler:                                            │
│   ├── 低开销 CPU 分析                                       │
│   ├── 生成火焰图                                            │
│   └── 支持 Java/Native 混合分析                             │
│                                                             │
│   GCViewer / GCEasy:                                         │
│   └── GC 日志分析                                           │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 常见排查场景

```
┌─────────────────────────────────────────────────────────────┐
│                    问题排查对照                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   CPU 飙高:                                                  │
│   jps → top -Hp → jstack → 分析线程                         │
│                                                             │
│   内存溢出 (OOM):                                            │
│   jmap -dump → MAT 分析                                     │
│   或 -XX:+HeapDumpOnOutOfMemoryError                        │
│                                                             │
│   频繁 Full GC:                                              │
│   jstat -gcutil → 分析 GC 原因                              │
│                                                             │
│   死锁:                                                      │
│   jstack → 查找 "deadlock" 关键字                           │
│                                                             │
│   线程阻塞:                                                  │
│   jstack → 查看 BLOCKED/WAITING 线程                        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> 常用 JDK 工具：**jps** 查看 Java 进程；**jstack** 线程堆栈，排查死锁和 CPU 飙高；**jmap** 内存映射，导出 heapdump；**jstat** GC 统计；**jcmd** 综合诊断命令。可视化：**jconsole**、**VisualVM**。第三方：**Arthas** 是线上诊断神器，**MAT** 分析内存泄漏。

### 1分钟版本

> **进程与线程**：
> - jps：查看 Java 进程
> - jstack：线程堆栈，排查死锁/CPU 高
>
> **内存诊断**：
> - jmap：堆统计、导出 heapdump
> - jstat：GC 统计
>
> **综合工具**：
> - jcmd：替代上述多个工具
> - jinfo：JVM 参数查看/修改
>
> **可视化**：
> - jconsole：JMX 监控
> - VisualVM：综合监控
> - MAT：heapdump 分析
>
> **第三方**：
> - Arthas：线上诊断神器
> - Async-profiler：火焰图
>
> **场景**：
> - CPU 高：jstack 看线程
> - OOM：jmap dump + MAT
> - 频繁 GC：jstat 分析

---

*关联文档：[cpu-high-troubleshoot.md](../12-troubleshoot/cpu-high-troubleshoot.md) | [jvm-memory-analysis.md](../11-jvm/jvm-memory-analysis.md)*
