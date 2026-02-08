# 如何优化 Java 中的锁的使用？

## 锁优化策略总览

```
┌─────────────────────────────────────────────────────────────┐
│                    锁优化策略分类                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 减少锁的粒度                                           │
│      ├── 分段锁 (ConcurrentHashMap)                         │
│      ├── 锁分离 (ReadWriteLock)                             │
│      └── 按 Key 加锁                                        │
│                                                             │
│   2. 减少锁的持有时间                                        │
│      ├── 缩小同步代码块                                      │
│      ├── 避免锁中做 IO 操作                                  │
│      └── 快速失败                                           │
│                                                             │
│   3. 锁的替代方案                                           │
│      ├── 无锁编程 (CAS, Atomic)                             │
│      ├── ThreadLocal                                        │
│      └── 不可变对象                                         │
│                                                             │
│   4. 选择合适的锁                                           │
│      ├── synchronized vs ReentrantLock                      │
│      ├── 公平锁 vs 非公平锁                                 │
│      └── 读写锁 vs 互斥锁                                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 具体优化方法

### 1. 减少锁粒度

```java
// ❌ 粗粒度锁：整个方法加锁
public class CoarseLock {
    private final Map<String, Object> cache = new HashMap<>();
    
    public synchronized void process(String key, Object value) {
        // 预处理（不需要加锁）
        Object processed = preProcess(value);
        // 更新缓存
        cache.put(key, processed);
        // 后处理（不需要加锁）
        postProcess(processed);
    }
}

// ✅ 细粒度锁：只锁必要的部分
public class FineLock {
    private final Map<String, Object> cache = new HashMap<>();
    
    public void process(String key, Object value) {
        // 预处理（无锁）
        Object processed = preProcess(value);
        
        // 只锁缓存更新
        synchronized (cache) {
            cache.put(key, processed);
        }
        
        // 后处理（无锁）
        postProcess(processed);
    }
}

// ✅ 分段锁：ConcurrentHashMap 思想
public class SegmentLock<K, V> {
    private static final int SEGMENT_COUNT = 16;
    private final Object[] locks = new Object[SEGMENT_COUNT];
    private final Map<K, V>[] segments;
    
    @SuppressWarnings("unchecked")
    public SegmentLock() {
        segments = new HashMap[SEGMENT_COUNT];
        for (int i = 0; i < SEGMENT_COUNT; i++) {
            locks[i] = new Object();
            segments[i] = new HashMap<>();
        }
    }
    
    public void put(K key, V value) {
        int index = Math.abs(key.hashCode()) % SEGMENT_COUNT;
        synchronized (locks[index]) {
            segments[index].put(key, value);
        }
    }
}
```

### 2. 锁分离（读写锁）

```java
// ❌ 读写都用互斥锁
public class MutexCache<K, V> {
    private final Map<K, V> cache = new HashMap<>();
    
    public synchronized V get(K key) {
        return cache.get(key);  // 读操作也被序列化
    }
    
    public synchronized void put(K key, V value) {
        cache.put(key, value);
    }
}

// ✅ 使用读写锁：读读并发，读写互斥
public class RWLockCache<K, V> {
    private final Map<K, V> cache = new HashMap<>();
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();
    
    public V get(K key) {
        readLock.lock();
        try {
            return cache.get(key);  // 多个读可以并发
        } finally {
            readLock.unlock();
        }
    }
    
    public void put(K key, V value) {
        writeLock.lock();
        try {
            cache.put(key, value);
        } finally {
            writeLock.unlock();
        }
    }
}

// ✅ 更进一步：StampedLock（乐观读）
public class StampedLockCache<K, V> {
    private final Map<K, V> cache = new HashMap<>();
    private final StampedLock sl = new StampedLock();
    
    public V get(K key) {
        // 乐观读：不加锁
        long stamp = sl.tryOptimisticRead();
        V value = cache.get(key);
        
        // 验证期间是否有写操作
        if (!sl.validate(stamp)) {
            // 乐观读失败，升级为悲观读锁
            stamp = sl.readLock();
            try {
                value = cache.get(key);
            } finally {
                sl.unlockRead(stamp);
            }
        }
        return value;
    }
}
```

### 3. 按 Key 加锁

```java
// ❌ 全局锁：所有操作序列化
public class GlobalLockService {
    public synchronized void processOrder(Long orderId) {
        // 处理订单
    }
}

// ✅ 按订单 ID 加锁：不同订单可并发
public class KeyLockService {
    // 使用 Guava Striped
    private final Striped<Lock> locks = Striped.lock(256);
    
    public void processOrder(Long orderId) {
        Lock lock = locks.get(orderId);
        lock.lock();
        try {
            // 处理订单
        } finally {
            lock.unlock();
        }
    }
}

// 或者使用 intern() 字符串池（注意内存泄漏风险）
public class InternLockService {
    public void processOrder(Long orderId) {
        String lockKey = ("order:" + orderId).intern();
        synchronized (lockKey) {
            // 处理订单
        }
    }
}
```

### 4. 无锁编程

```java
// ❌ 锁计数器
public class LockCounter {
    private int count = 0;
    
    public synchronized void increment() {
        count++;
    }
}

// ✅ 无锁计数器：Atomic
public class AtomicCounter {
    private final AtomicInteger count = new AtomicInteger(0);
    
    public void increment() {
        count.incrementAndGet();  // CAS 操作
    }
}

// ✅ 高并发计数器：LongAdder（分段累加）
public class LongAdderCounter {
    private final LongAdder count = new LongAdder();
    
    public void increment() {
        count.increment();  // 分散热点
    }
    
    public long get() {
        return count.sum();
    }
}

// ✅ CAS 实现无锁栈
public class ConcurrentStack<E> {
    private final AtomicReference<Node<E>> top = new AtomicReference<>();
    
    public void push(E item) {
        Node<E> newHead = new Node<>(item);
        Node<E> oldHead;
        do {
            oldHead = top.get();
            newHead.next = oldHead;
        } while (!top.compareAndSet(oldHead, newHead));  // CAS 自旋
    }
    
    public E pop() {
        Node<E> oldHead;
        Node<E> newHead;
        do {
            oldHead = top.get();
            if (oldHead == null) return null;
            newHead = oldHead.next;
        } while (!top.compareAndSet(oldHead, newHead));
        return oldHead.item;
    }
    
    private static class Node<E> {
        final E item;
        Node<E> next;
        Node(E item) { this.item = item; }
    }
}
```

### 5. ThreadLocal 避免竞争

```java
// ❌ 共享可变对象需要同步
public class SharedFormatter {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    
    public synchronized String format(Date date) {
        return sdf.format(date);  // SimpleDateFormat 不是线程安全的
    }
}

// ✅ ThreadLocal：每个线程独立副本，无需同步
public class ThreadLocalFormatter {
    private static final ThreadLocal<SimpleDateFormat> dateFormat = 
        ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));
    
    public String format(Date date) {
        return dateFormat.get().format(date);  // 无锁
    }
}

// ✅ 或使用线程安全的 DateTimeFormatter (Java 8+)
public class SafeFormatter {
    private static final DateTimeFormatter formatter = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    public String format(LocalDate date) {
        return date.format(formatter);  // 不可变对象，线程安全
    }
}
```

### 6. 减少锁持有时间

```java
// ❌ 锁中做 IO 操作
public synchronized void saveWithLock(User user) {
    // 校验（快速）
    validate(user);
    // 保存到数据库（慢）
    userDao.save(user);
    // 发送通知（慢）
    notificationService.send(user);
}

// ✅ 只在必要时加锁
public void saveOptimized(User user) {
    // 校验（无锁）
    validate(user);
    
    // 只在更新共享状态时加锁
    synchronized (this) {
        updateCache(user);
    }
    
    // IO 操作放到锁外面
    userDao.save(user);
    notificationService.send(user);
}
```

### 7. 选择合适的锁

```java
// 场景 1：简单同步 → synchronized
public synchronized void simpleMethod() { }

// 场景 2：需要超时/中断 → ReentrantLock
public void complexMethod() {
    if (lock.tryLock(1, TimeUnit.SECONDS)) {
        try { /* ... */ } 
        finally { lock.unlock(); }
    }
}

// 场景 3：读多写少 → ReadWriteLock
ReadWriteLock rwLock = new ReentrantReadWriteLock();

// 场景 4：高并发计数 → LongAdder
LongAdder counter = new LongAdder();

// 场景 5：只需可见性 → volatile
private volatile boolean flag;

// 场景 6：单次初始化 → 双重检查锁或 Holder 模式
```

## 锁优化对比表

```
┌─────────────────────────────────────────────────────────────┐
│                    锁优化方案对比                            │
├─────────────────┬───────────────────────────────────────────┤
│   问题           │   优化方案                                │
├─────────────────┼───────────────────────────────────────────┤
│   锁粒度太粗     │   分段锁、按 Key 加锁                       │
│   读多写少       │   ReadWriteLock、StampedLock              │
│   高并发计数     │   LongAdder、AtomicLong                   │
│   线程私有数据   │   ThreadLocal                             │
│   可见性需求     │   volatile                                │
│   单例初始化     │   DCL + volatile、静态内部类               │
│   锁竞争激烈     │   无锁 CAS、分布式锁分散                    │
│   死锁风险       │   tryLock + 超时、资源排序                 │
│   锁持有时间长   │   缩小临界区、锁外做 IO                     │
└─────────────────┴───────────────────────────────────────────┘
```

## 最佳实践

### ✅ 推荐做法

```java
// 1. 优先使用并发容器
ConcurrentHashMap<String, Object> map = new ConcurrentHashMap<>();
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
BlockingQueue<Task> queue = new LinkedBlockingQueue<>();

// 2. 使用更细粒度的锁
private final Object userLock = new Object();
private final Object orderLock = new Object();

// 3. 考虑无锁方案
AtomicReference<State> state = new AtomicReference<>();
LongAdder counter = new LongAdder();

// 4. 使用 try-finally 确保释放锁
lock.lock();
try {
    // ...
} finally {
    lock.unlock();
}
```

### ❌ 避免做法

```java
// 1. 避免锁中嵌套锁（死锁风险）
synchronized (lockA) {
    synchronized (lockB) {  // 可能死锁
        // ...
    }
}

// 2. 避免锁中做耗时操作
synchronized (lock) {
    httpClient.call();  // 网络 IO
    Thread.sleep(1000); // 睡眠
}

// 3. 避免使用 String/Integer 等作为锁对象
synchronized (orderId.toString()) {  // 不同对象可能是同一个字符串
    // ...
}

// 4. 避免过度同步
public synchronized int getCount() {
    return this.count;  // 简单读取不需要同步，用 volatile
}
```

## 面试回答

### 30秒版本

> 锁优化主要从四方面入手：1）**减少锁粒度**——分段锁、按 Key 加锁；2）**减少锁持有时间**——缩小同步块，锁外做 IO；3）**锁替代方案**——CAS 无锁、ThreadLocal、不可变对象；4）**选择合适的锁**——读写锁、LongAdder 等。

### 1分钟版本

> **锁优化策略**：
>
> 1. **减少锁粒度**：
>    - 分段锁（ConcurrentHashMap 思想）
>    - 按业务 Key 加锁（Guava Striped）
>    - 读写分离（ReadWriteLock）
>
> 2. **减少锁持有时间**：
>    - 缩小同步代码块
>    - 避免锁中做 IO/网络调用
>
> 3. **无锁替代**：
>    - CAS 原子操作（AtomicInteger）
>    - 高并发计数用 LongAdder（分段累加）
>    - 线程私有用 ThreadLocal
>    - 只需可见性用 volatile
>
> 4. **选择合适的锁**：
>    - 简单场景：synchronized
>    - 需要超时/中断：ReentrantLock
>    - 读多写少：ReadWriteLock / StampedLock
>
> **避免**：锁中嵌套锁、String 作锁对象、锁中做耗时操作。

---

*关联文档：[synchronized-vs-reentrantlock.md](synchronized-vs-reentrantlock.md) | [java-cas.md](java-cas.md) | [concurrent-tools.md](concurrent-tools.md)*
