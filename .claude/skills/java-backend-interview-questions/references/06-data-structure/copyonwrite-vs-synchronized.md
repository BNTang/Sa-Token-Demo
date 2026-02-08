# CopyOnWriteArrayList 和 Collections.synchronizedList 的区别

## 核心区别

```
┌─────────────────────────────────────────────────────────────┐
│        CopyOnWriteArrayList vs synchronizedList             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   CopyOnWriteArrayList:                                     │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  写时复制 (Copy-On-Write)                            │  │
│   │  • 写操作复制新数组                                  │  │
│   │  • 读操作无锁                                        │  │
│   │  • 读写分离                                          │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   synchronizedList:                                          │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  全局锁 (Synchronized)                               │  │
│   │  • 所有操作加锁                                      │  │
│   │  • 读写互斥                                          │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 对比表

```
┌──────────────────┬────────────────────┬────────────────────┐
│   特性           │ CopyOnWriteArrayList│ synchronizedList  │
├──────────────────┼────────────────────┼────────────────────┤
│   锁机制         │ ReentrantLock(写)  │ synchronized(全)   │
│   读操作         │ 无锁               │ 加锁               │
│   写操作         │ 加锁 + 复制数组    │ 加锁               │
│   迭代器         │ 快照，不抛ConcurrentModificationException  │ 需手动同步         │
│   内存消耗       │ 高 (复制数组)      │ 低                 │
│   写性能         │ 差 (复制开销)      │ 较好               │
│   读性能         │ 好 (无锁)          │ 差 (竞争)          │
│   适用场景       │ 读多写少           │ 通用               │
│   数据一致性     │ 弱一致性           │ 强一致性           │
└──────────────────┴────────────────────┴────────────────────┘
```

## CopyOnWriteArrayList 原理

```java
public class CopyOnWriteArrayList<E> {
    // volatile 保证可见性
    private transient volatile Object[] array;
    final transient ReentrantLock lock = new ReentrantLock();
    
    // 读操作: 无锁
    public E get(int index) {
        return (E) array[index];  // 直接读，无锁
    }
    
    // 写操作: 加锁 + 复制
    public boolean add(E e) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            // 复制新数组
            Object[] newElements = Arrays.copyOf(elements, len + 1);
            newElements[len] = e;
            // 替换引用
            setArray(newElements);
            return true;
        } finally {
            lock.unlock();
        }
    }
    
    // 迭代器: 快照
    public Iterator<E> iterator() {
        return new COWIterator<E>(getArray(), 0);  // 使用当前数组快照
    }
}
```

## synchronizedList 原理

```java
// 包装器模式
List<String> syncList = Collections.synchronizedList(new ArrayList<>());

// 内部实现
static class SynchronizedList<E> extends SynchronizedCollection<E> 
        implements List<E> {
    final List<E> list;
    final Object mutex;  // 锁对象
    
    // 所有操作都加锁
    public E get(int index) {
        synchronized (mutex) { return list.get(index); }
    }
    
    public E set(int index, E element) {
        synchronized (mutex) { return list.set(index, element); }
    }
    
    public void add(int index, E element) {
        synchronized (mutex) { list.add(index, element); }
    }
    
    // ⚠️ 迭代器需要手动同步
    public Iterator<E> iterator() {
        return list.iterator();  // 不是同步的!
    }
}

// 正确的迭代方式
synchronized (syncList) {
    Iterator<String> it = syncList.iterator();
    while (it.hasNext()) {
        System.out.println(it.next());
    }
}
```

## 使用场景

```
┌─────────────────────────────────────────────────────────────┐
│                    使用场景选择                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   CopyOnWriteArrayList:                                      │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  ✅ 读多写少 (读写比 > 10:1)                         │  │
│   │  ✅ 数据量小 (复制开销可接受)                        │  │
│   │  ✅ 需要安全迭代 (不抛异常)                          │  │
│   │  ✅ 事件监听器列表                                   │  │
│   │  ✅ 黑白名单                                         │  │
│   │  ❌ 写操作频繁                                       │  │
│   │  ❌ 数据量大                                         │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   synchronizedList:                                          │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  ✅ 读写均衡                                         │  │
│   │  ✅ 强一致性要求                                     │  │
│   │  ✅ 简单同步需求                                     │  │
│   │  ❌ 高并发读 (锁竞争)                                │  │
│   │  ❌ 需要安全迭代                                     │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   其他选择:                                                  │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  • ConcurrentLinkedQueue: 高并发队列                 │  │
│   │  • Vector: 过时，不推荐                              │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 代码示例

```java
// CopyOnWriteArrayList - 事件监听器
public class EventBus {
    private final CopyOnWriteArrayList<EventListener> listeners = 
        new CopyOnWriteArrayList<>();
    
    public void addListener(EventListener listener) {
        listeners.add(listener);  // 写操作少
    }
    
    public void fireEvent(Event event) {
        // 读操作多，无锁遍历
        for (EventListener listener : listeners) {
            listener.onEvent(event);
        }
    }
}

// synchronizedList - 普通同步需求
public class TaskManager {
    private final List<Task> tasks = 
        Collections.synchronizedList(new ArrayList<>());
    
    public void addTask(Task task) {
        tasks.add(task);
    }
    
    // 迭代需要手动同步
    public void processTasks() {
        synchronized (tasks) {
            for (Task task : tasks) {
                task.process();
            }
        }
    }
}
```

## 面试回答

### 30秒版本

> **CopyOnWriteArrayList**：写时复制，写加锁+复制数组，读无锁，适合**读多写少**，迭代安全但内存消耗大。**synchronizedList**：全局锁，读写都加锁，迭代需手动同步，适合读写均衡场景。高并发读选 CopyOnWrite，写频繁选 synchronized。

### 1分钟版本

> **CopyOnWriteArrayList**：
> - 写时复制：写操作复制新数组
> - 读无锁，性能好
> - 迭代器是快照，不抛异常
> - 缺点：内存消耗大，写性能差
> - 场景：读多写少、事件监听器
>
> **synchronizedList**：
> - 全局锁：所有操作加锁
> - 读写都互斥
> - 迭代需手动 synchronized
> - 场景：读写均衡、强一致性
>
> **选择**：
> - 读多写少 → CopyOnWriteArrayList
> - 写多或数据大 → synchronizedList

---

*关联文档：[java-collections.md](../00-java-basics/java-collections.md)*
