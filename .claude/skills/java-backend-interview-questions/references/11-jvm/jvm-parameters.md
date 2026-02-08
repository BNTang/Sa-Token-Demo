# 常用的 JVM 配置参数有哪些？

## 参数分类

```
┌─────────────────────────────────────────────────────────────┐
│                    JVM 参数分类                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 标准参数 (-)                                           │
│      └── 所有 JVM 都支持，如 -version, -classpath           │
│                                                             │
│   2. X 参数 (-X)                                            │
│      └── 非标准，主流 JVM 支持，如 -Xms, -Xmx               │
│                                                             │
│   3. XX 参数 (-XX)                                          │
│      └── 非标准，HotSpot 特有                               │
│      └── -XX:+Enable 开启某功能                             │
│      └── -XX:-Disable 关闭某功能                            │
│      └── -XX:Name=Value 设置值                              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 堆内存参数

```bash
# ======================== 堆内存 ========================
-Xms4g                    # 初始堆大小 (建议与 Xmx 相同)
-Xmx4g                    # 最大堆大小
-Xmn2g                    # 年轻代大小 (也可用 NewRatio)

# 年轻代与老年代比例
-XX:NewRatio=2            # 老年代:年轻代 = 2:1 (默认)

# Eden 与 Survivor 比例
-XX:SurvivorRatio=8       # Eden:S0:S1 = 8:1:1 (默认)

# ======================== 元空间 ========================
-XX:MetaspaceSize=256m    # 元空间初始大小
-XX:MaxMetaspaceSize=256m # 元空间最大值 (默认无限制)

# ======================== 直接内存 ========================
-XX:MaxDirectMemorySize=1g  # 直接内存最大值

# ======================== 栈 ========================
-Xss256k                  # 线程栈大小 (默认 1MB)
```

## GC 相关参数

```bash
# ======================== GC 选择 ========================
-XX:+UseSerialGC          # Serial GC (单线程，小内存)
-XX:+UseParallelGC        # Parallel GC (吞吐量优先，JDK8默认)
-XX:+UseConcMarkSweepGC   # CMS GC (已废弃)
-XX:+UseG1GC              # G1 GC (JDK9+默认)
-XX:+UseZGC               # ZGC (低延迟，JDK11+)
-XX:+UseShenandoahGC      # Shenandoah (低延迟)

# ======================== G1 参数 ========================
-XX:MaxGCPauseMillis=200  # 目标最大停顿时间 (毫秒)
-XX:G1HeapRegionSize=16m  # Region 大小 (1MB-32MB，2的幂)
-XX:InitiatingHeapOccupancyPercent=45  # 触发并发标记的堆占用率

# ======================== Parallel 参数 ========================
-XX:ParallelGCThreads=4   # 并行 GC 线程数
-XX:GCTimeRatio=99        # 吞吐量目标 (GC时间占比 1/(1+99)=1%)

# ======================== 通用 GC 参数 ========================
-XX:MaxTenuringThreshold=15  # 对象晋升老年代的年龄阈值
-XX:PretenureSizeThreshold=4m  # 大对象直接进入老年代的阈值

# ======================== GC 日志 ========================
# JDK 8
-XX:+PrintGCDetails
-XX:+PrintGCDateStamps
-XX:+PrintGCTimeStamps
-Xloggc:/var/log/gc.log
-XX:+UseGCLogFileRotation
-XX:NumberOfGCLogFiles=5
-XX:GCLogFileSize=10M

# JDK 9+
-Xlog:gc*:file=/var/log/gc.log:time,uptime,level,tags:filecount=5,filesize=10M
```

## 诊断调试参数

```bash
# ======================== OOM 诊断 ========================
-XX:+HeapDumpOnOutOfMemoryError           # OOM 时自动生成堆转储
-XX:HeapDumpPath=/var/log/heapdump.hprof  # 堆转储文件路径
-XX:OnOutOfMemoryError="kill -9 %p"       # OOM 时执行命令

# ======================== 类加载 ========================
-XX:+TraceClassLoading     # 跟踪类加载
-XX:+TraceClassUnloading   # 跟踪类卸载

# ======================== JIT 编译 ========================
-XX:+PrintCompilation      # 打印 JIT 编译信息
-XX:CompileThreshold=10000 # 方法调用多少次后 JIT 编译

# ======================== 远程调试 ========================
-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
```

## 性能优化参数

```bash
# ======================== 字符串 ========================
-XX:+UseStringDeduplication  # G1 下开启字符串去重
-XX:StringTableSize=1000003  # 字符串常量池大小 (质数)

# ======================== 锁优化 ========================
-XX:+UseBiasedLocking       # 开启偏向锁 (JDK15后默认关闭)
-XX:-UseBiasedLocking       # 关闭偏向锁
-XX:BiasedLockingStartupDelay=0  # 偏向锁启动延迟

# ======================== 内联优化 ========================
-XX:MaxInlineSize=35        # 方法内联的最大字节码大小
-XX:FreqInlineSize=325      # 热点方法内联的最大字节码大小

# ======================== TLAB ========================
-XX:+UseTLAB                # 开启 TLAB (默认开启)
-XX:TLABSize=512k           # TLAB 大小
```

## 生产环境配置示例

```bash
# ======================== 4核8G 服务器 (Web应用) ========================
java \
  -Xms4g -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=100 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/var/log/heapdump.hprof \
  -Xlog:gc*:file=/var/log/gc.log:time,uptime:filecount=5,filesize=20M \
  -jar app.jar

# ======================== 8核16G 服务器 (高并发) ========================
java \
  -Xms12g -Xmx12g \
  -XX:+UseG1GC \
  -XX:G1HeapRegionSize=16m \
  -XX:MaxGCPauseMillis=50 \
  -XX:InitiatingHeapOccupancyPercent=40 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/var/log/heapdump.hprof \
  -Xlog:gc*:file=/var/log/gc.log:time,uptime:filecount=10,filesize=50M \
  -jar app.jar

# ======================== 低延迟场景 (ZGC) ========================
java \
  -Xms8g -Xmx8g \
  -XX:+UseZGC \
  -XX:+HeapDumpOnOutOfMemoryError \
  -Xlog:gc*:file=/var/log/gc.log:time \
  -jar app.jar

# ======================== 容器环境 (K8s/Docker) ========================
java \
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:InitialRAMPercentage=75.0 \
  -XX:+UseG1GC \
  -XX:+HeapDumpOnOutOfMemoryError \
  -jar app.jar
```

## 常用参数速查表

```
┌─────────────────────────────────────────────────────────────┐
│                    常用参数速查                              │
├───────────────────────────┬─────────────────────────────────┤
│   参数                     │   说明                          │
├───────────────────────────┼─────────────────────────────────┤
│   -Xms / -Xmx             │   初始/最大堆大小                │
│   -Xmn                    │   年轻代大小                     │
│   -Xss                    │   线程栈大小                     │
│   -XX:MetaspaceSize       │   元空间初始大小                 │
│   -XX:+UseG1GC            │   使用 G1 垃圾收集器             │
│   -XX:MaxGCPauseMillis    │   G1 目标停顿时间                │
│   -XX:+HeapDumpOnOOM      │   OOM 时生成堆转储               │
│   -Xlog:gc*               │   GC 日志 (JDK9+)               │
│   -XX:MaxRAMPercentage    │   容器环境内存百分比             │
├───────────────────────────┴─────────────────────────────────┤
│   建议：                                                     │
│   • Xms = Xmx 避免动态调整                                  │
│   • 生产必须配置 GC 日志和 HeapDump                         │
│   • 容器使用 MaxRAMPercentage 而非固定值                    │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> 常用 JVM 参数：**堆内存**（-Xms、-Xmx、-Xmn）、**GC 选择**（-XX:+UseG1GC）、**GC 调优**（-XX:MaxGCPauseMillis）、**OOM 诊断**（-XX:+HeapDumpOnOutOfMemoryError）、**GC 日志**（-Xlog:gc*）。生产建议 Xms=Xmx，必须配置 GC 日志和堆转储。

### 1分钟版本

> **堆内存参数**：
> - `-Xms4g -Xmx4g`：堆大小（建议相等）
> - `-Xmn2g`：年轻代大小
> - `-XX:MetaspaceSize=256m`：元空间
>
> **GC 参数**：
> - `-XX:+UseG1GC`：使用 G1（推荐）
> - `-XX:MaxGCPauseMillis=100`：目标停顿时间
> - `-Xlog:gc*:file=/var/log/gc.log`：GC 日志
>
> **诊断参数**：
> - `-XX:+HeapDumpOnOutOfMemoryError`：OOM 时自动 dump
> - `-XX:HeapDumpPath=/path`：dump 文件路径
>
> **容器参数**：
> - `-XX:MaxRAMPercentage=75.0`：使用容器内存的 75%
>
> **最佳实践**：Xms=Xmx、开启 GC 日志、配置 HeapDump、容器用比例而非固定值。

---

*关联文档：[jvm-gc-tuning.md](jvm-gc-tuning.md) | [jvm-garbage-collectors.md](jvm-garbage-collectors.md) | [jvm-components.md](jvm-components.md)*
