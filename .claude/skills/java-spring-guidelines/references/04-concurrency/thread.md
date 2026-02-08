# 并发编程规范

> Java/Spring Boot 编码规范 - 并发编程
> 参考：阿里巴巴 Java 开发手册

---

## 线程池规范

### 禁止使用 Executors

**【强制】线程池不允许使用 Executors 创建，而要通过 ThreadPoolExecutor 创建。**

> 说明：Executors 创建的线程池存在资源耗尽风险：
> - FixedThreadPool 和 SingleThreadPool：允许的请求队列长度为 Integer.MAX_VALUE，可能堆积大量请求导致 OOM
> - CachedThreadPool：允许创建的线程数量为 Integer.MAX_VALUE，可能创建大量线程导致 OOM
> - ScheduledThreadPool：允许的请求队列长度为 Integer.MAX_VALUE，可能堆积大量请求导致 OOM

```java
// ❌ 反例 - 使用 Executors
ExecutorService executor = Executors.newFixedThreadPool(10);
ExecutorService executor = Executors.newCachedThreadPool();
ExecutorService executor = Executors.newSingleThreadExecutor();

// ✅ 正例 - 使用 ThreadPoolExecutor
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    5,                                      // corePoolSize 核心线程数
    10,                                     // maximumPoolSize 最大线程数
    60L,                                    // keepAliveTime 空闲线程存活时间
    TimeUnit.SECONDS,                       // 时间单位
    new LinkedBlockingQueue<>(1000),        // 工作队列，指定容量
    new ThreadFactoryBuilder()
        .setNameFormat("order-pool-%d")     // 线程命名
        .build(),
    new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略
);
```

### 线程池参数设置

| 参数 | CPU 密集型 | IO 密集型 | 说明 |
|------|-----------|----------|------|
| corePoolSize | CPU 核心数 + 1 | CPU 核心数 * 2 | 核心线程数 |
| maximumPoolSize | CPU 核心数 + 1 | CPU 核心数 * 4 | 最大线程数 |
| workQueue | 较小队列 | 较大队列 | 建议有界队列 |

```java
// ✅ 正例 - 配置类创建线程池
@Configuration
public class ThreadPoolConfig {

    @Bean("orderExecutor")
    public ThreadPoolExecutor orderExecutor() {
        int corePoolSize = Runtime.getRuntime().availableProcessors() * 2;
        return new ThreadPoolExecutor(
            corePoolSize,
            corePoolSize * 2,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(2000),
            new ThreadFactoryBuilder()
                .setNameFormat("order-pool-%d")
                .setUncaughtExceptionHandler((t, e) -> 
                    log.error("线程{}异常", t.getName(), e))
                .build(),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
```

### 拒绝策略选择

| 策略 | 说明 | 适用场景 |
|------|------|---------|
| `AbortPolicy` | 抛出异常（默认） | 要求任务不能丢失 |
| `CallerRunsPolicy` | 调用者线程执行 | 可接受执行变慢 |
| `DiscardPolicy` | 静默丢弃 | 允许任务丢失 |
| `DiscardOldestPolicy` | 丢弃最老任务 | 新任务优先级高 |

---

## 线程命名

**【强制】创建线程或线程池时请指定有意义的线程名称，方便出错时回溯。**

```java
// ❌ 反例 - 默认线程名
new Thread(() -> doSomething()).start();  // Thread-0, Thread-1...

// ✅ 正例 - 指定线程名
new Thread(() -> doSomething(), "order-sync-thread").start();

// ✅ 正例 - 使用 ThreadFactory
ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
    .setNameFormat("payment-pool-%d")
    .build();
```

---

## ThreadLocal 规范

### 必须 remove

**【强制】必须在 finally 块中调用 ThreadLocal 的 remove() 方法，避免内存泄漏。**

> 说明：在线程池环境下，线程会被复用，如果不清除 ThreadLocal，会导致数据错乱。

```java
// ❌ 反例 - 未清除 ThreadLocal
public class UserContext {
    private static final ThreadLocal<User> USER_HOLDER = new ThreadLocal<>();

    public static void setUser(User user) {
        USER_HOLDER.set(user);
    }

    public static User getUser() {
        return USER_HOLDER.get();
    }
}

// 使用
UserContext.setUser(user);
doSomething();
// 忘记 remove，线程池复用时会拿到上一个请求的用户！

// ✅ 正例 - 使用 try-finally 清除
public class UserContext {
    private static final ThreadLocal<User> USER_HOLDER = new ThreadLocal<>();

    public static void setUser(User user) {
        USER_HOLDER.set(user);
    }

    public static User getUser() {
        return USER_HOLDER.get();
    }

    public static void clear() {
        USER_HOLDER.remove();
    }
}

// 在 Filter 或 Interceptor 中
try {
    UserContext.setUser(user);
    filterChain.doFilter(request, response);
} finally {
    UserContext.clear();  // 必须清除
}
```

### TransmittableThreadLocal

**【推荐】跨线程传递使用 TransmittableThreadLocal（阿里开源）。**

```java
// 普通 ThreadLocal 无法传递到子线程
ThreadLocal<String> local = new ThreadLocal<>();
local.set("main");

executor.execute(() -> {
    System.out.println(local.get());  // null
});

// ✅ 使用 TransmittableThreadLocal + TtlRunnable
TransmittableThreadLocal<String> ttl = new TransmittableThreadLocal<>();
ttl.set("main");

executor.execute(TtlRunnable.get(() -> {
    System.out.println(ttl.get());  // "main"
}));
```

---

## 锁使用规范

### synchronized 使用

**【强制】对同一资源的加锁必须使用同一把锁。**

```java
// ❌ 反例 - 锁对象不一致
public class Counter {
    private int count = 0;

    public void add() {
        synchronized (new Object()) {  // 每次都是新对象，锁无效！
            count++;
        }
    }
}

// ✅ 正例 - 使用同一把锁
public class Counter {
    private int count = 0;
    private final Object lock = new Object();

    public void add() {
        synchronized (lock) {
            count++;
        }
    }

    // 或直接使用 this
    public synchronized void add() {
        count++;
    }
}
```

### 锁顺序

**【强制】多个锁时必须按照固定顺序加锁，避免死锁。**

```java
// ❌ 反例 - 可能死锁
// 线程 1
synchronized (lockA) {
    synchronized (lockB) { }
}
// 线程 2
synchronized (lockB) {
    synchronized (lockA) { }
}

// ✅ 正例 - 统一顺序
// 所有线程都先锁 A 再锁 B
synchronized (lockA) {
    synchronized (lockB) { }
}
```

### 锁超时

**【推荐】使用 ReentrantLock 时，优先使用 tryLock 带超时，避免无限等待。**

```java
private final ReentrantLock lock = new ReentrantLock();

// ❌ 反例 - 无限等待
public void process() {
    lock.lock();
    try {
        doSomething();
    } finally {
        lock.unlock();
    }
}

// ✅ 正例 - 带超时
public void process() {
    boolean acquired = false;
    try {
        acquired = lock.tryLock(5, TimeUnit.SECONDS);
        if (acquired) {
            doSomething();
        } else {
            throw new BusinessException("获取锁超时");
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new BusinessException("获取锁被中断");
    } finally {
        if (acquired) {
            lock.unlock();
        }
    }
}
```

---

## volatile 使用

**【强制】volatile 只保证可见性，不保证原子性。复合操作需要额外同步。**

```java
// ❌ 反例 - volatile 不保证原子性
private volatile int count = 0;

public void add() {
    count++;  // 不是原子操作！
}

// ✅ 正例 - 使用 AtomicInteger
private AtomicInteger count = new AtomicInteger(0);

public void add() {
    count.incrementAndGet();
}

// ✅ 正例 - volatile 用于状态标志
private volatile boolean running = true;

public void stop() {
    running = false;
}

public void run() {
    while (running) {
        doWork();
    }
}
```

---

## 双重检查锁定

**【强制】使用双重检查锁定时，被检查的变量必须是 volatile。**

```java
// ❌ 反例 - 未使用 volatile
public class Singleton {
    private static Singleton instance;

    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class) {
                if (instance == null) {
                    instance = new Singleton();  // 可能发生指令重排！
                }
            }
        }
        return instance;
    }
}

// ✅ 正例 - 使用 volatile
public class Singleton {
    private static volatile Singleton instance;

    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class) {
                if (instance == null) {
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}

// ✅ 正例 - 使用静态内部类（推荐）
public class Singleton {
    private Singleton() {}

    private static class Holder {
        private static final Singleton INSTANCE = new Singleton();
    }

    public static Singleton getInstance() {
        return Holder.INSTANCE;
    }
}
```

---

## 并发集合

**【推荐】高并发场景使用并发集合代替同步集合。**

| 同步集合 | 并发集合 | 场景 |
|---------|---------|------|
| `Hashtable` | `ConcurrentHashMap` | 高并发 Map |
| `Vector` | `CopyOnWriteArrayList` | 读多写少 |
| `Collections.synchronizedList` | `CopyOnWriteArrayList` | 读多写少 |
| `Collections.synchronizedMap` | `ConcurrentHashMap` | 高并发 Map |

```java
// ❌ 反例
Map<String, String> map = new Hashtable<>();
Map<String, String> map = Collections.synchronizedMap(new HashMap<>());

// ✅ 正例
ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
```

---

## SimpleDateFormat 线程安全

**【强制】SimpleDateFormat 是线程不安全的，不要作为类变量。**

```java
// ❌ 反例 - 线程不安全
public class DateUtil {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    public static String format(Date date) {
        return sdf.format(date);  // 并发问题！
    }
}

// ✅ 正例 - 每次创建新实例
public static String format(Date date) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    return sdf.format(date);
}

// ✅ 正例 - 使用 ThreadLocal
private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT = 
    ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));

public static String format(Date date) {
    return DATE_FORMAT.get().format(date);
}

// ✅ 正例 - 使用 DateTimeFormatter（推荐，线程安全）
private static final DateTimeFormatter FORMATTER = 
    DateTimeFormatter.ofPattern("yyyy-MM-dd");

public static String format(LocalDate date) {
    return date.format(FORMATTER);
}
```

---

## 禁则速查

| ❌ 禁止 | ✅ 正确 | 原因 |
|--------|--------|------|
| `Executors.newXxx()` | `ThreadPoolExecutor` | OOM 风险 |
| 未命名线程 | 指定线程名称 | 便于排查 |
| ThreadLocal 不 remove | finally 中 remove | 内存泄漏/数据错乱 |
| 每次 new 锁对象 | 使用同一把锁 | 锁失效 |
| 多锁无序 | 固定加锁顺序 | 死锁 |
| volatile 用于复合操作 | Atomic 类 | 非原子性 |
| 双重检查无 volatile | 加 volatile | 指令重排 |
| SimpleDateFormat 类变量 | ThreadLocal 或 DateTimeFormatter | 线程不安全 |
| Hashtable/Vector | ConcurrentHashMap/CopyOnWriteArrayList | 性能差 |
