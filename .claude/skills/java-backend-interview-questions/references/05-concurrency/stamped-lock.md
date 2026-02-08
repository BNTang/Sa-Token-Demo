# 什么是Java的StampedLock?

## 回答

StampedLock是JDK8引入的高性能读写锁，支持乐观读模式：

```
┌─────────────────────────────────────────────────────────────┐
│                  StampedLock 三种模式                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐    │
│  │  写锁        │   │  悲观读锁     │   │  乐观读       │    │
│  │ writeLock()  │   │ readLock()   │   │ tryOptimistic │    │
│  │              │   │              │   │ Read()        │    │
│  │  独占        │   │  共享        │   │  无锁         │    │
│  │  阻塞其他    │   │  阻塞写      │   │  不阻塞任何   │    │
│  └──────────────┘   └──────────────┘   └──────────────┘    │
│                                                             │
│  乐观读：假设没有写操作，读完后validate()验证               │
│         如果验证失败，升级为悲观读锁重新读取                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 基本使用

### 写锁

```java
StampedLock lock = new StampedLock();

// 写锁
long stamp = lock.writeLock();
try {
    // 修改共享数据
    data = newValue;
} finally {
    lock.unlockWrite(stamp);
}
```

### 悲观读锁

```java
// 悲观读锁（与ReentrantReadWriteLock的读锁类似）
long stamp = lock.readLock();
try {
    // 读取共享数据
    return data;
} finally {
    lock.unlockRead(stamp);
}
```

### 乐观读（核心特性）

```java
// 乐观读（无锁）
long stamp = lock.tryOptimisticRead();  // 获取乐观读标记

// 读取共享数据（可能被修改）
int currentX = x;
int currentY = y;

// 验证期间是否有写操作
if (!lock.validate(stamp)) {
    // 验证失败，升级为悲观读锁
    stamp = lock.readLock();
    try {
        currentX = x;
        currentY = y;
    } finally {
        lock.unlockRead(stamp);
    }
}

return currentX + currentY;
```

## 完整示例

```java
public class Point {
    private double x, y;
    private final StampedLock lock = new StampedLock();
    
    // 写操作
    public void move(double deltaX, double deltaY) {
        long stamp = lock.writeLock();
        try {
            x += deltaX;
            y += deltaY;
        } finally {
            lock.unlockWrite(stamp);
        }
    }
    
    // 乐观读（推荐读多写少场景）
    public double distanceFromOrigin() {
        // 1. 尝试乐观读
        long stamp = lock.tryOptimisticRead();
        double currentX = x, currentY = y;
        
        // 2. 验证
        if (!lock.validate(stamp)) {
            // 3. 验证失败，升级为悲观读锁
            stamp = lock.readLock();
            try {
                currentX = x;
                currentY = y;
            } finally {
                lock.unlockRead(stamp);
            }
        }
        
        return Math.sqrt(currentX * currentX + currentY * currentY);
    }
    
    // 悲观读
    public double[] getPosition() {
        long stamp = lock.readLock();
        try {
            return new double[]{x, y};
        } finally {
            lock.unlockRead(stamp);
        }
    }
    
    // 锁升级：读锁 → 写锁
    public void moveIfAtOrigin(double newX, double newY) {
        long stamp = lock.readLock();
        try {
            while (x == 0.0 && y == 0.0) {
                // 尝试升级为写锁
                long writeStamp = lock.tryConvertToWriteLock(stamp);
                if (writeStamp != 0L) {
                    stamp = writeStamp;
                    x = newX;
                    y = newY;
                    break;
                } else {
                    // 升级失败，释放读锁，获取写锁
                    lock.unlockRead(stamp);
                    stamp = lock.writeLock();
                }
            }
        } finally {
            lock.unlock(stamp);
        }
    }
}
```

## 与ReentrantReadWriteLock对比

```
┌─────────────────────────────────────────────────────────────┐
│       StampedLock vs ReentrantReadWriteLock                 │
├───────────────────┬─────────────────┬───────────────────────┤
│ 特性              │ StampedLock     │ ReentrantReadWriteLock│
├───────────────────┼─────────────────┼───────────────────────┤
│ 乐观读            │ ✅ 支持         │ ❌ 不支持              │
├───────────────────┼─────────────────┼───────────────────────┤
│ 可重入            │ ❌ 不支持       │ ✅ 支持                │
├───────────────────┼─────────────────┼───────────────────────┤
│ 锁升级            │ ✅ 支持         │ ❌ 不支持              │
├───────────────────┼─────────────────┼───────────────────────┤
│ 条件变量Condition │ ❌ 不支持       │ ✅ 支持                │
├───────────────────┼─────────────────┼───────────────────────┤
│ 公平模式          │ ❌ 不支持       │ ✅ 支持                │
├───────────────────┼─────────────────┼───────────────────────┤
│ 读多写少性能      │ 更好           │ 一般                  │
└───────────────────┴─────────────────┴───────────────────────┘
```

## 核心方法

```java
StampedLock lock = new StampedLock();

// 写锁
long stamp = lock.writeLock();           // 阻塞获取
long stamp = lock.tryWriteLock();        // 非阻塞尝试
long stamp = lock.tryWriteLock(1, TimeUnit.SECONDS);  // 超时
lock.unlockWrite(stamp);

// 悲观读锁
long stamp = lock.readLock();            // 阻塞获取
long stamp = lock.tryReadLock();         // 非阻塞尝试
lock.unlockRead(stamp);

// 乐观读
long stamp = lock.tryOptimisticRead();   // 获取乐观读标记
boolean valid = lock.validate(stamp);    // 验证

// 锁转换
long writeStamp = lock.tryConvertToWriteLock(stamp);   // 升级为写锁
long readStamp = lock.tryConvertToReadLock(stamp);     // 降级为读锁

// 通用解锁
lock.unlock(stamp);
```

## 注意事项

```java
// ⚠️ StampedLock不可重入
StampedLock lock = new StampedLock();

long stamp1 = lock.writeLock();
long stamp2 = lock.writeLock();  // 死锁！

// ⚠️ 使用不当可能导致CPU飙升
// 乐观读循环中要注意添加退出条件

// ⚠️ 不支持Condition
// 如果需要条件等待，使用ReentrantLock

// ⚠️ 写锁不支持中断
long stamp = lock.writeLockInterruptibly();  // 可中断版本
```

## 性能对比

```
┌─────────────────────────────────────────────────────────────┐
│              读多写少场景性能对比                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  StampedLock乐观读:    ████████████████████  最快           │
│  StampedLock悲观读:    █████████████         较快           │
│  ReentrantReadWriteLock: ██████████          一般           │
│  synchronized:         █████                较慢            │
│                                                             │
│  乐观读在没有写竞争时，几乎无开销                             │
│  因为只是获取一个版本号，不需要CAS操作                        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 适用场景

| 场景 | 推荐 |
|------|------|
| 读多写少，追求极致性能 | ✅ StampedLock |
| 需要可重入 | ReentrantLock |
| 需要Condition条件等待 | ReentrantLock |
| 需要公平锁 | ReentrantReadWriteLock(fair) |
| 简单场景 | synchronized |

## 面试回答

### 30秒版本
> StampedLock是JDK8引入的高性能读写锁，支持三种模式：写锁独占、悲观读锁共享、乐观读无锁。乐观读核心思想是假设没有写操作，读完后validate()验证，失败则升级为悲观读锁。相比ReentrantReadWriteLock，乐观读在读多写少场景性能更好，但不可重入、不支持Condition。

### 1分钟版本
> StampedLock是JDK8引入的高性能锁，支持**三种模式**：
> 
> ①**写锁**writeLock()，独占模式阻塞其他所有操作；②**悲观读锁**readLock()，共享模式阻塞写操作；③**乐观读**tryOptimisticRead()，核心创新，无锁操作，只获取一个版本号stamp，读取数据后调用validate(stamp)验证期间是否有写操作，如果有则升级为悲观读锁重新读取。
> 
> **优势**：乐观读在没有写竞争时几乎零开销，适合读多写少场景。还支持锁升级tryConvertToWriteLock()。
> 
> **限制**：①不可重入，同一线程再次获取锁会死锁；②不支持Condition条件变量；③不支持公平模式。
> 
> **使用场景**：读远多于写且追求极致性能时使用，需要可重入或Condition时用ReentrantLock。

## 相关问题
- [[thread-synchronization]] - 线程同步
- [[aqs]] - AQS原理
- [[concurrent-utils]] - 并发工具类
