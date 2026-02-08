# 什么是Java的ForkJoinPool?

## 回答

ForkJoinPool是JDK7引入的用于并行计算的线程池，采用分治思想和工作窃取算法：

```
┌─────────────────────────────────────────────────────────────┐
│                   Fork/Join 分治思想                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│                    ┌───────────────┐                       │
│                    │   大任务       │                       │
│                    └───────┬───────┘                       │
│                           fork                              │
│                    ┌───────┴───────┐                       │
│              ┌─────┴─────┐   ┌─────┴─────┐                 │
│              │  子任务1   │   │  子任务2   │                 │
│              └─────┬─────┘   └─────┬─────┘                 │
│                   fork            fork                      │
│              ┌────┴────┐    ┌────┴────┐                    │
│              │ 1-1 │1-2│    │ 2-1 │2-2│  继续分解           │
│              └────┬────┘    └────┬────┘                    │
│                   │              │                          │
│                  join           join                        │
│                   └──────┬───────┘                         │
│                        join                                 │
│                    ┌─────┴─────┐                           │
│                    │  合并结果  │                           │
│                    └───────────┘                           │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 工作窃取算法

```
┌─────────────────────────────────────────────────────────────┐
│                   Work Stealing                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   Worker1的队列（双端队列）     Worker2的队列                 │
│   ┌───────────────────┐       ┌───────────────────┐        │
│   │ T1 │ T2 │ T3 │ T4 │       │    │    │    │    │        │
│   └───────────────────┘       └───────────────────┘        │
│     ↑                           从尾部窃取 ↙                │
│     │ 从头部取任务执行                                       │
│     │                                                       │
│   Worker1繁忙              Worker2空闲，窃取Worker1的T4      │
│                                                             │
│   优点：                                                     │
│   • 充分利用多核CPU                                          │
│   • 减少线程间竞争（两端操作）                                │
│   • 提高任务吞吐量                                           │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 核心类

```java
// 1. ForkJoinPool - 线程池
ForkJoinPool pool = new ForkJoinPool();           // 默认CPU核数
ForkJoinPool pool = new ForkJoinPool(4);          // 指定并行度
ForkJoinPool pool = ForkJoinPool.commonPool();    // 公共池

// 2. ForkJoinTask - 任务基类
// ├── RecursiveAction  无返回值
// └── RecursiveTask<V> 有返回值

// 3. 提交任务
pool.invoke(task);       // 同步执行，阻塞等待结果
pool.execute(task);      // 异步执行
pool.submit(task);       // 异步执行，返回Future
```

## 使用示例

### RecursiveTask（有返回值）

```java
// 计算1到n的和
public class SumTask extends RecursiveTask<Long> {
    private static final int THRESHOLD = 1000;  // 阈值
    private final long start;
    private final long end;
    
    public SumTask(long start, long end) {
        this.start = start;
        this.end = end;
    }
    
    @Override
    protected Long compute() {
        // 小于阈值，直接计算
        if (end - start <= THRESHOLD) {
            long sum = 0;
            for (long i = start; i <= end; i++) {
                sum += i;
            }
            return sum;
        }
        
        // 大于阈值，分解任务
        long mid = (start + end) / 2;
        SumTask left = new SumTask(start, mid);
        SumTask right = new SumTask(mid + 1, end);
        
        // fork: 异步执行子任务
        left.fork();
        right.fork();
        
        // join: 等待子任务完成，合并结果
        return left.join() + right.join();
    }
}

// 使用
ForkJoinPool pool = new ForkJoinPool();
Long result = pool.invoke(new SumTask(1, 100_000_000));
System.out.println("结果: " + result);
```

### RecursiveAction（无返回值）

```java
// 并行排序
public class ParallelMergeSort extends RecursiveAction {
    private static final int THRESHOLD = 1000;
    private final int[] array;
    private final int start, end;
    
    public ParallelMergeSort(int[] array, int start, int end) {
        this.array = array;
        this.start = start;
        this.end = end;
    }
    
    @Override
    protected void compute() {
        if (end - start <= THRESHOLD) {
            Arrays.sort(array, start, end);
            return;
        }
        
        int mid = (start + end) / 2;
        ParallelMergeSort left = new ParallelMergeSort(array, start, mid);
        ParallelMergeSort right = new ParallelMergeSort(array, mid, end);
        
        // invokeAll: 同时fork多个任务并等待
        invokeAll(left, right);
        
        // 合并已排序的两部分
        merge(array, start, mid, end);
    }
}
```

## 优化技巧

### fork/join顺序优化

```java
// 不推荐：两个fork后再join
left.fork();
right.fork();
return left.join() + right.join();

// 推荐：一个fork，一个compute
left.fork();
Long rightResult = right.compute();  // 当前线程执行
Long leftResult = left.join();       // 等待fork的任务
return leftResult + rightResult;

// 或使用invokeAll
invokeAll(left, right);
return left.join() + right.join();
```

### 阈值设置

```java
// 阈值太小：任务分解开销大于计算开销
// 阈值太大：无法充分利用并行

// 经验值：
// 总任务数 = 问题规模 / 阈值
// 理想情况：总任务数 = CPU核数 * 几倍（如10-100倍）
```

## Stream并行流

```java
// Stream的parallelStream()底层使用ForkJoinPool.commonPool()
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);

int sum = numbers.parallelStream()
    .mapToInt(Integer::intValue)
    .sum();

// 自定义线程池执行
ForkJoinPool customPool = new ForkJoinPool(4);
int result = customPool.submit(() ->
    numbers.parallelStream()
        .mapToInt(Integer::intValue)
        .sum()
).get();
```

## 与普通线程池对比

```
┌─────────────────────────────────────────────────────────────┐
│           ForkJoinPool vs ThreadPoolExecutor                │
├─────────────────┬─────────────────┬─────────────────────────┤
│ 特性            │ ForkJoinPool    │ ThreadPoolExecutor      │
├─────────────────┼─────────────────┼─────────────────────────┤
│ 任务队列        │ 每个线程一个    │ 共享一个                 │
├─────────────────┼─────────────────┼─────────────────────────┤
│ 任务类型        │ 可分解的大任务  │ 独立的小任务             │
├─────────────────┼─────────────────┼─────────────────────────┤
│ 工作窃取        │ ✅ 支持         │ ❌ 不支持                │
├─────────────────┼─────────────────┼─────────────────────────┤
│ 适用场景        │ CPU密集型计算   │ IO密集型/混合任务        │
├─────────────────┼─────────────────┼─────────────────────────┤
│ 任务依赖        │ 适合有依赖关系  │ 适合独立任务             │
└─────────────────┴─────────────────┴─────────────────────────┘
```

## 应用场景

| 场景 | 说明 |
|------|------|
| **并行计算** | 大数组求和、统计 |
| **归并排序** | 分治排序算法 |
| **递归遍历** | 文件目录遍历 |
| **图像处理** | 分块并行处理 |
| **Stream并行** | parallelStream底层 |

## 面试回答

### 30秒版本
> ForkJoinPool是JDK7引入的并行计算线程池，核心是分治思想和工作窃取算法。大任务fork()分解为子任务，子任务结果join()合并。每个线程有独立的双端队列，空闲线程从其他线程队列尾部窃取任务，提高CPU利用率。RecursiveTask有返回值，RecursiveAction无返回值。Stream的parallelStream()底层就是ForkJoinPool。

### 1分钟版本
> ForkJoinPool是JDK7引入的用于并行计算的线程池，采用**分治思想**和**工作窃取算法**。
> 
> **分治思想**：大任务递归分解为小任务，小任务并行执行后合并结果。通过fork()异步提交子任务，join()等待获取结果。任务类继承RecursiveTask（有返回值）或RecursiveAction（无返回值），在compute()方法中实现分解逻辑。
> 
> **工作窃取**：每个工作线程有独立的双端队列，自己从头部取任务执行，空闲时从其他线程队列尾部窃取任务。减少线程竞争，提高CPU利用率。
> 
> **使用技巧**：阈值设置要合理，太小分解开销大，太大并行度低。推荐一个fork一个compute，减少任务等待。
> 
> **应用场景**：大数组并行计算、递归算法并行化、Stream的parallelStream()底层就是ForkJoinPool.commonPool()。适合CPU密集型可分解任务。

## 相关问题
- [[thread-pool-principle]] - 线程池原理
- [[completable-future]] - CompletableFuture
- [[executors-thread-pools]] - Executors线程池
