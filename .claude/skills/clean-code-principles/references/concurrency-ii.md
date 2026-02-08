# 并发编程 II (Concurrency II)

> **作者**: Brett L. Schuchert
> 
> 本附录支持并扩展了第13章"并发"的内容。这些主题相互独立，可按任意顺序阅读。

---

## 一、客户端/服务器示例

### 1.1 基本服务器

```java
// 简单的单线程服务器
ServerSocket serverSocket = new ServerSocket(8009);

while (keepProcessing) {
    try {
        Socket socket = serverSocket.accept();
        process(socket);  // 阻塞处理
    } catch (Exception e) {
        handle(e);
    }
}
```

### 1.2 性能测试

```java
@Test(timeout = 10000)
public void shouldRunInUnder10Seconds() throws Exception {
    Thread[] threads = createThreads();
    startAllThreads(threads);
    waitForAllThreadsToFinish(threads);
}
```

### 1.3 I/O密集 vs CPU密集

| 类型 | 特征 | 解决方案 |
|-----|------|---------|
| **I/O密集** | 等待socket、数据库、虚拟内存 | 多线程有效提升吞吐量 |
| **CPU密集** | 数值计算、正则表达式、GC | 增加CPU核心，线程帮助有限 |

### 1.4 添加线程

```java
// ❌ 简单但有问题的多线程方案
void process(final Socket socket) {
    if (socket == null)
        return;

    Runnable clientHandler = new Runnable() {
        public void run() {
            try {
                String message = MessageUtils.getMessage(socket);
                MessageUtils.sendMessage(socket, "Processed: " + message);
                closeIgnoringException(socket);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    Thread clientConnection = new Thread(clientHandler);
    clientConnection.start();  // 无限制地创建线程！
}
```

**问题**: 
- 没有线程数量限制
- 可能耗尽JVM资源
- 混合了多个职责

### 1.5 职责分离

服务器代码有多少个职责？
1. Socket连接管理
2. 客户端处理
3. 线程策略
4. 服务器关闭策略

**违反单一职责原则！**

```java
// ✅ 分离职责后的代码
public void run() {
    while (keepProcessing) {
        try {
            ClientConnection clientConnection = connectionManager.awaitClient();
            ClientRequestProcessor requestProcessor
                = new ClientRequestProcessor(clientConnection);
            clientScheduler.schedule(requestProcessor);  // 线程管理集中在这里
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    connectionManager.shutdown();
}
```

### 1.6 线程调度器接口

```java
public interface ClientScheduler {
    void schedule(ClientRequestProcessor requestProcessor);
}

// 每请求一个线程的实现
public class ThreadPerRequestScheduler implements ClientScheduler {
    public void schedule(final ClientRequestProcessor requestProcessor) {
        Runnable runnable = new Runnable() {
            public void run() {
                requestProcessor.process();
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }
}

// 使用Executor框架的实现（推荐）
public class ExecutorClientScheduler implements ClientScheduler {
    Executor executor;

    public ExecutorClientScheduler(int availableThreads) {
        executor = Executors.newFixedThreadPool(availableThreads);
    }

    public void schedule(final ClientRequestProcessor requestProcessor) {
        Runnable runnable = new Runnable() {
            public void run() {
                requestProcessor.process();
            }
        };
        executor.execute(runnable);
    }
}
```

---

## 二、可能的执行路径

### 2.1 简单示例

```java
public class IdGenerator {
    int lastIdUsed;

    public int incrementValue() {
        return ++lastIdUsed;
    }
}
```

### 2.2 两个线程的可能结果

假设 `lastIdUsed` 初始值为 93：

| 场景 | Thread 1 | Thread 2 | lastIdUsed |
|-----|----------|----------|------------|
| 正常1 | 94 | 95 | 95 |
| 正常2 | 95 | 94 | 95 |
| **异常** | **94** | **94** | **94** |

### 2.3 路径数量计算

对于 N 条指令和 T 个线程，可能的执行路径数：

$$\frac{(T \times N)!}{(N!)^T}$$

**示例**:
- `++lastIdUsed` 编译为 8 条字节码
- 2个线程：$\frac{16!}{8!^2} = 12,870$ 种可能路径
- 如果是 `long` 类型：2,704,156 种可能路径

```java
// 加上 synchronized 后
public synchronized void incrementValue() {
    ++lastIdUsed;
}
// 只有 2 种可能路径（T!种）
```

### 2.4 字节码分析

```java
// resetId() 的字节码 - 原子操作
ALOAD 0          // 加载 this
ICONST_0         // 加载常量 0
PUTFIELD lastId  // 存储字段
```

```java
// incrementValue() 的字节码 - 非原子操作
ALOAD 0          // 加载 this
DUP              // 复制栈顶
GETFIELD lastId  // 获取字段值
ICONST_1         // 加载常量 1
IADD             // 加法
DUP_X1           // 复制
PUTFIELD lastId  // 存储字段
IRETURN          // 返回
```

**关键点**: `++` 操作不是原子的！

### 2.5 原子操作定义

- **原子操作**: 不可中断的操作
- 32位值的赋值是原子的
- 64位值（long/double）的赋值**不是**原子的

```java
// ❌ long 赋值可能被中断
long value = 0L;  // 两次32位赋值

// ✅ 使用 volatile 或 synchronized
volatile long value = 0L;
```

---

## 三、了解你的库

### 3.1 Executor 框架

```java
// ✅ 使用 Executor 而非手动管理线程
ExecutorService executor = Executors.newFixedThreadPool(10);

// 提交任务
executor.submit(() -> processRequest());

// 关闭
executor.shutdown();
```

### 3.2 Future 模式

```java
public String processRequest(String message) throws Exception {
    Callable<String> makeExternalCall = new Callable<String>() {
        public String call() throws Exception {
            String result = "";
            // 执行外部请求
            return result;
        }
    };

    // 异步执行外部调用
    Future<String> result = executorService.submit(makeExternalCall);
    
    // 同时进行本地处理
    String partialResult = doSomeLocalProcessing();
    
    // 等待外部调用完成并合并结果
    return result.get() + partialResult;
}
```

### 3.3 非阻塞方案

```java
// ❌ 使用 synchronized（阻塞）
public class ObjectWithValue {
    private int value;
    
    public synchronized void incrementValue() {
        ++value;
    }
    
    public int getValue() {
        return value;
    }
}

// ✅ 使用 Atomic 类（非阻塞）
public class ObjectWithValue {
    private AtomicInteger value = new AtomicInteger(0);

    public void incrementValue() {
        value.incrementAndGet();
    }
    
    public int getValue() {
        return value.get();
    }
}
```

**CAS (Compare and Swap) 原理**:

```java
int variableBeingSet;

void simulateNonBlockingSet(int newValue) {
    int currentValue;
    do {
        currentValue = variableBeingSet;
    } while (currentValue != compareAndSwap(currentValue, newValue));
}

synchronized int compareAndSwap(int currentValue, int newValue) {
    if (variableBeingSet == currentValue) {
        variableBeingSet = newValue;
        return currentValue;
    }
    return variableBeingSet;
}
```

### 3.4 非线程安全的类

以下类本质上不是线程安全的：

| 类 | 问题 |
|---|------|
| `SimpleDateFormat` | 内部状态可变 |
| `Database Connections` | 连接状态 |
| `java.util` 容器 | 非同步的集合 |
| `Servlets` | 共享实例变量 |

```java
// ❌ 组合操作不是线程安全的
if (!hashTable.containsKey(someKey)) {
    hashTable.put(someKey, new SomeValue());
}

// ✅ 方案1：客户端加锁
synchronized (map) {
    if (!map.containsKey(key))
        map.put(key, value);
}

// ✅ 方案2：服务端加锁（Adapter）
public class WrappedHashtable<K, V> {
    private Map<K, V> map = new Hashtable<K, V>();

    public synchronized void putIfAbsent(K key, V value) {
        if (!map.containsKey(key))
            map.put(key, value);
    }
}

// ✅ 方案3：使用并发集合（最佳）
ConcurrentHashMap<Integer, String> map = new ConcurrentHashMap<>();
map.putIfAbsent(key, value);
```

---

## 四、方法间依赖可能破坏并发代码

### 4.1 问题示例

```java
public class IntegerIterator implements Iterator<Integer> {
    private Integer nextValue = 0;

    public synchronized boolean hasNext() {
        return nextValue < 100000;
    }
    
    public synchronized Integer next() {
        if (nextValue == 100000)
            throw new IteratorPastEndException();
        return nextValue++;
    }
}

// 客户端代码
IntegerIterator iterator = new IntegerIterator();
while (iterator.hasNext()) {
    int nextValue = iterator.next();  // 可能抛出异常！
    // 处理 nextValue
}
```

**问题**: Thread 1 调用 `hasNext()` 返回 true，被抢占；Thread 2 调用 `next()` 使 `hasNext()` 变为 false；Thread 1 恢复后调用 `next()` 抛出异常。

### 4.2 解决方案

#### 方案1：容忍失败

```java
// 捕获异常并清理
try {
    while (iterator.hasNext()) {
        int nextValue = iterator.next();
    }
} catch (IteratorPastEndException e) {
    // 清理
}
```

#### 方案2：客户端加锁

```java
IntegerIterator iterator = new IntegerIterator();

while (true) {
    int nextValue;
    synchronized (iterator) {
        if (!iterator.hasNext())
            break;
        nextValue = iterator.next();
    }
    doSomethingWith(nextValue);
}
```

**问题**: 所有客户端都必须记得加锁！

#### 方案3：服务端加锁（推荐）

```java
public class IntegerIteratorServerLocked {
    private Integer nextValue = 0;

    public synchronized Integer getNextOrNull() {
        if (nextValue < 100000)
            return nextValue++;
        else
            return null;
    }
}

// 客户端代码
while (true) {
    Integer nextValue = iterator.getNextOrNull();
    if (nextValue == null)
        break;
    // 处理 nextValue
}
```

### 4.3 服务端加锁的优势

| 优势 | 说明 |
|-----|------|
| 减少重复代码 | 客户端无需编写加锁代码 |
| 更好的性能 | 单线程部署可替换为非线程安全版本 |
| 减少出错可能 | 不依赖程序员记得加锁 |
| 单一策略 | 策略集中在服务端 |
| 缩小共享变量范围 | 客户端不知道共享变量存在 |

---

## 五、提升吞吐量

### 5.1 页面读取器

```java
public class PageReader {
    public String getPageFor(String url) {
        HttpMethod method = new GetMethod(url);
        try {
            httpClient.executeMethod(method);
            String response = method.getResponseBodyAsString();
            return response;
        } catch (Exception e) {
            handle(e);
        } finally {
            method.releaseConnection();
        }
    }
}

public class PageIterator {
    private PageReader reader;
    private URLIterator urls;

    public synchronized String getNextPageOrNull() {
        if (urls.hasNext())
            return getPageFor(urls.next());
        else
            return null;
    }

    public String getPageFor(String url) {
        return reader.getPageFor(url);
    }
}
```

### 5.2 单线程 vs 多线程吞吐量

**假设**:
- I/O时间获取页面：1秒
- 处理时间解析页面：0.5秒
- I/O需要0% CPU，处理需要100% CPU

**单线程**: N 个页面需要 1.5N 秒

**三线程**: 
- I/O等待期间可以处理其他页面
- 每秒可处理约2个页面
- 吞吐量提升3倍

```
单线程:  |--I/O--|--处理--|--I/O--|--处理--|
三线程:  |--I/O--|--处理--|
         |--I/O--|--处理--|
         |--I/O--|--处理--|
```

---

## 六、死锁

### 6.1 死锁场景

```
资源池:
- 10个数据库连接
- 10个MQ连接

操作:
- Create: 先获取数据库连接，再获取MQ连接
- Update: 先获取MQ连接，再获取数据库连接

场景:
1. 10个用户执行Create，获取了所有数据库连接，等待MQ连接
2. 10个用户执行Update，获取了所有MQ连接，等待数据库连接
3. 死锁！系统永不恢复
```

### 6.2 死锁的四个条件

必须同时满足以下四个条件才会发生死锁：

| 条件 | 描述 |
|-----|------|
| **互斥** | 资源不能同时被多个线程使用 |
| **持有并等待** | 线程持有资源的同时等待其他资源 |
| **不可抢占** | 线程不能从其他线程抢夺资源 |
| **循环等待** | 形成环形等待链 |

```
       T1 → R1
       ↑    ↓
       R2 ← T2
```

### 6.3 打破互斥

```java
// ✅ 使用允许同时访问的资源
AtomicInteger counter = new AtomicInteger(0);

// ✅ 增加资源数量
ExecutorService executor = Executors.newFixedThreadPool(100);

// ✅ 获取前检查所有资源可用
if (canAcquireAll(resources)) {
    acquireAll(resources);
}
```

### 6.4 打破持有并等待

```java
// ✅ 获取失败时释放所有资源并重试
public void acquireResources() {
    while (true) {
        if (tryAcquire(resource1)) {
            if (tryAcquire(resource2)) {
                return;  // 成功获取所有资源
            }
            release(resource1);  // 释放已获取的资源
        }
        Thread.sleep(randomBackoff());  // 随机退避
    }
}
```

**可能的问题**:
- **饥饿**: 某线程始终无法获取所需资源
- **活锁**: 多个线程反复获取/释放资源

### 6.5 打破不可抢占

```java
// ✅ 允许请求其他线程释放资源
public void acquireWithPreemption(Resource resource) {
    while (!resource.tryAcquire()) {
        Resource.Owner owner = resource.getOwner();
        if (owner.isWaiting()) {
            owner.releaseAll();  // 请求持有者释放
        }
    }
}
```

### 6.6 打破循环等待（最常用）

```java
// ✅ 所有线程按相同顺序获取资源
public void acquireInOrder() {
    // 总是先获取数据库连接，再获取MQ连接
    synchronized (dbConnection) {
        synchronized (mqConnection) {
            // 执行操作
        }
    }
}

// 更通用的方案：按资源ID排序
public void acquireResources(Resource... resources) {
    Arrays.sort(resources, Comparator.comparing(Resource::getId));
    for (Resource r : resources) {
        r.acquire();
    }
}
```

---

## 七、测试多线程代码

### 7.1 测试并发Bug的困难

```java
public class ClassWithThreadingProblem {
    int nextId;

    public int takeNextId() {
        return nextId++;  // 非原子操作
    }
}
```

```java
@Test
public void twoThreadsShouldFailEventually() throws Exception {
    final ClassWithThreadingProblem problem = new ClassWithThreadingProblem();

    Runnable runnable = () -> problem.takeNextId();

    for (int i = 0; i < 50000; ++i) {
        int startingId = problem.nextId;
        int expectedResult = 2 + startingId;

        Thread t1 = new Thread(runnable);
        Thread t2 = new Thread(runnable);
        t1.start();
        t2.start();
        t1.join();
        t2.join();

        int endingId = problem.nextId;

        if (endingId != expectedResult)
            return;  // 发现问题
    }

    fail("Should have exposed a threading issue but it did not.");
}
```

**问题**: 需要超过100万次迭代才能可靠地发现问题！

### 7.2 测试策略

| 策略 | 描述 |
|-----|------|
| **蒙特卡洛测试** | 随机调整参数，持续运行测试 |
| **多平台测试** | 在所有目标平台上重复运行 |
| **负载测试** | 模拟生产环境负载 |
| **工具辅助** | 使用ConTest等工具 |

### 7.3 使用 ConTest 工具

ConTest（IBM开发）通过插桩使非线程安全代码更容易失败：

```
未插桩: 约 1/10,000,000 次迭代失败
已插桩: 约 1/30 次迭代失败

实测失败迭代数: 13, 23, 0, 54, 16, 14, 6, 69, 107, 49, 2
```

---

## 八、代码审查检查清单

### 并发设计
- [ ] 线程管理代码是否集中在少数几个类中？
- [ ] 线程管理类是否只做线程管理？
- [ ] 是否使用了 Executor 框架而非手动创建线程？
- [ ] 是否正确识别了 I/O密集 vs CPU密集场景？

### 原子性
- [ ] 是否误认为 ++ 操作是原子的？
- [ ] 对于 64 位值（long/double）是否使用了 volatile 或同步？
- [ ] 是否优先使用 Atomic 类而非 synchronized？

### 线程安全
- [ ] 是否避免使用非线程安全的类（SimpleDateFormat等）？
- [ ] 组合操作是否正确同步？
- [ ] 是否优先使用 java.util.concurrent 集合？

### 死锁预防
- [ ] 是否所有线程按相同顺序获取资源？
- [ ] 是否避免持有锁的同时等待其他锁？
- [ ] 是否设置了锁超时？

### 测试
- [ ] 是否有专门的并发测试？
- [ ] 是否使用了代码插桩工具？
- [ ] 是否在多平台上运行测试？

---

## 核心引用

> "Focusing all concurrency code into a small number of classes is an example of applying the Single Responsibility Principle. In the case of concurrent programming, this becomes especially important because of its complexity."

> "There is no replacement for due diligence. Every boundary condition, every corner case, every quirk and exception represents something that can confound an elegant and intuitive algorithm."

> "Client-based locking really blows." — 1971年芝加哥冬天的教训

---

## 推荐阅读

- **《Java并发编程实战》** (Concurrent Programming in Java) - Doug Lea
- **《Java并发编程的艺术》**
- **IBM ConTest**: http://www.haifa.ibm.com/projects/verification/contest/

---

## 技术栈
- **语言**: Java 5+
- **并发框架**: java.util.concurrent
- **原子类**: AtomicInteger, AtomicLong, AtomicReference
- **线程池**: ExecutorService, Executors
- **并发集合**: ConcurrentHashMap, CopyOnWriteArrayList
- **测试工具**: ConTest (IBM)
