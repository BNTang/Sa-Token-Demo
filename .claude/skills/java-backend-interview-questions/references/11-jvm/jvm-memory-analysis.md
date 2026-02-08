# 怎么分析 JVM 当前的内存占用情况？OOM 后怎么分析？

## 内存分析工具概览

```
┌─────────────────────────────────────────────────────────────┐
│                    JVM 内存分析工具                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   命令行工具 (JDK 自带):                                    │
│   ├── jps      - 查看 Java 进程                             │
│   ├── jstat    - GC 统计信息                                │
│   ├── jmap     - 内存映射、堆 dump                          │
│   ├── jstack   - 线程堆栈                                   │
│   └── jcmd     - 综合诊断命令                               │
│                                                             │
│   可视化工具:                                                │
│   ├── VisualVM     - 综合监控                               │
│   ├── JConsole     - JMX 监控                               │
│   ├── MAT          - 内存分析 (Eclipse Memory Analyzer)     │
│   ├── JProfiler    - 商业工具                               │
│   └── Arthas       - 阿里开源诊断工具                       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 运行时内存分析

### 1. jps - 查看 Java 进程

```bash
# 查看 Java 进程
jps -l
# 输出:
# 12345 com.example.Application
# 12346 org.apache.catalina.startup.Bootstrap
```

### 2. jstat - GC 统计

```bash
# GC 统计 (每 1 秒输出一次)
jstat -gcutil <pid> 1000

# 输出说明:
#   S0     S1     E      O      M     CCS    YGC   YGCT   FGC  FGCT    GCT
#   0.00  99.23  45.67  78.90  95.12  91.23   25   0.345   3   0.567  0.912
```

```
┌─────────────────────────────────────────────────────────────┐
│                    jstat 字段说明                            │
├─────────────────────────────────────────────────────────────┤
│   S0/S1  - Survivor 0/1 区使用率                            │
│   E      - Eden 区使用率                                    │
│   O      - Old 区使用率                                     │
│   M      - Metaspace 使用率                                 │
│   CCS    - 压缩类空间使用率                                 │
│   YGC    - Young GC 次数                                    │
│   YGCT   - Young GC 总耗时 (秒)                             │
│   FGC    - Full GC 次数                                     │
│   FGCT   - Full GC 总耗时 (秒)                              │
│   GCT    - GC 总耗时                                        │
└─────────────────────────────────────────────────────────────┘
```

### 3. jmap - 内存分析

```bash
# 查看堆内存概况
jmap -heap <pid>

# 输出示例:
# Heap Configuration:
#    MaxHeapSize      = 4294967296 (4096.0MB)
#    NewSize          = 1431633920 (1365.5MB)
#    OldSize          = 2863333376 (2730.5MB)
# 
# Heap Usage:
# Eden Space:
#    capacity = 1073741824 (1024.0MB)
#    used     = 536870912 (512.0MB)
#    free     = 536870912 (512.0MB)
#    50.0% used
```

```bash
# 查看对象统计 (按大小排序)
jmap -histo <pid> | head -20

# 输出:
#  num   #instances    #bytes  class name
#  1:      1234567   123456789  [B (byte数组)
#  2:       987654    98765432  java.lang.String
#  3:       654321    65432100  [C (char数组)
```

```bash
# 只看存活对象 (会触发 Full GC)
jmap -histo:live <pid>
```

### 4. jcmd - 综合诊断

```bash
# 查看堆内存
jcmd <pid> GC.heap_info

# 输出 GC 日志
jcmd <pid> GC.run   # 触发 GC

# 生成 dump 文件
jcmd <pid> GC.heap_dump /path/to/dump.hprof
```

## OOM 后分析

### 1. 开启 OOM Dump

```bash
# JVM 启动参数
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/path/to/dumps/

# 完整示例
java -Xmx4g -Xms4g \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/data/dumps/ \
     -jar app.jar
```

### 2. 手动生成 Dump

```bash
# 方式1: jmap
jmap -dump:format=b,file=heap.hprof <pid>

# 方式2: jcmd
jcmd <pid> GC.heap_dump heap.hprof

# 方式3: 强制 dump (live 对象)
jmap -dump:live,format=b,file=heap.hprof <pid>
```

### 3. MAT 分析 Dump

```
┌─────────────────────────────────────────────────────────────┐
│                    MAT 分析步骤                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 打开 dump 文件                                         │
│      File → Open Heap Dump                                  │
│                                                             │
│   2. 查看概览报告                                           │
│      Leak Suspects Report - 内存泄漏嫌疑报告                │
│                                                             │
│   3. 常用视图:                                               │
│      ┌─────────────────────────────────────────────────┐   │
│      │ Histogram     - 类实例统计                       │   │
│      │ Dominator Tree - 支配树 (谁持有最多内存)         │   │
│      │ Top Consumers - 内存消费大户                     │   │
│      │ OQL           - 对象查询语言                     │   │
│      └─────────────────────────────────────────────────┘   │
│                                                             │
│   4. 分析 GC Roots 引用链                                   │
│      右键对象 → Path to GC Roots → exclude weak references │
│      找出哪些对象持有引用导致无法回收                       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 4. 常见内存泄漏场景

```
┌─────────────────────────────────────────────────────────────┐
│                    常见内存泄漏                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 集合类未清理                                           │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  static List<Object> cache = new ArrayList<>();      │  │
│   │  public void add(Object obj) {                       │  │
│   │      cache.add(obj);  // 只增不减                    │  │
│   │  }                                                   │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   2. 未关闭的资源                                           │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  Connection conn = dataSource.getConnection();       │  │
│   │  // 忘记 close()                                     │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   3. 监听器未移除                                           │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  eventBus.register(listener);                        │  │
│   │  // 忘记 unregister()                                │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   4. ThreadLocal 未清理                                     │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  threadLocal.set(value);                             │  │
│   │  // 线程池场景忘记 remove()                          │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## Arthas 快速诊断

```bash
# 启动 Arthas
java -jar arthas-boot.jar

# 选择进程后进入交互模式

# 查看堆内存
dashboard

# 查看对象实例数
sc -d com.example.MyClass
sm -d com.example.MyClass

# 查看堆对象统计
heapdump --live /path/to/dump.hprof

# 火焰图 (CPU)
profiler start
# ... 等待采样
profiler stop
```

## 面试回答

### 30秒版本

> **运行时分析**：`jstat -gcutil` 看 GC 统计，`jmap -heap` 看堆内存，`jmap -histo` 看对象统计。**OOM 后分析**：启动参数加 `-XX:+HeapDumpOnOutOfMemoryError` 自动 dump，用 **MAT** 分析，重点看 Dominator Tree 找内存大户，Path to GC Roots 找引用链定位泄漏。

### 1分钟版本

> **运行时内存分析**：
> - `jstat -gcutil <pid> 1000`：GC 统计，Eden/Old/Metaspace 使用率
> - `jmap -heap <pid>`：堆内存配置和使用情况
> - `jmap -histo <pid>`：对象实例统计
>
> **OOM 分析**：
> 1. 开启 dump：
>    ```
>    -XX:+HeapDumpOnOutOfMemoryError
>    -XX:HeapDumpPath=/path/
>    ```
>
> 2. MAT 分析：
>    - **Leak Suspects**：自动分析泄漏嫌疑
>    - **Dominator Tree**：找内存占用大户
>    - **Path to GC Roots**：找引用链，定位泄漏根因
>
> **常见泄漏**：
> - 静态集合只增不减
> - 资源未关闭
> - ThreadLocal 未 remove
> - 监听器未注销

---

*关联文档：[jvm-oom-scenarios.md](jvm-oom-scenarios.md) | [cpu-high-troubleshoot.md](../12-troubleshoot/cpu-high-troubleshoot.md)*
