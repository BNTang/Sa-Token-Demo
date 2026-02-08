# Redis 跳表原理

> Java 后端面试知识点 - Redis 内部实现

---

## 什么是跳表

跳表（Skip List）是一种随机化的数据结构，通过在原有链表基础上增加多级索引，实现 O(log n) 的查找效率。

---

## 为什么 Redis 使用跳表

| 数据结构 | 查询 | 插入 | 删除 | 范围查询 | 实现复杂度 |
|---------|------|------|------|---------|-----------|
| **跳表** | O(log n) | O(log n) | O(log n) | ✅ 高效 | 简单 |
| 红黑树 | O(log n) | O(log n) | O(log n) | ❌ 困难 | 复杂 |
| B+树 | O(log n) | O(log n) | O(log n) | ✅ 高效 | 复杂 |

**Redis 选择跳表的原因**：
1. 实现简单，代码易于维护
2. 范围查询效率高（ZSet 的 ZRANGEBYSCORE）
3. 内存友好，指针开销可控
4. 插入删除不需要复杂的再平衡

---

## 跳表结构

```
Level 4:  head ─────────────────────────────────────────> 50 ─────────> NULL
                                                          │
Level 3:  head ────────────────> 25 ────────────────────> 50 ─────────> NULL
                                  │                       │
Level 2:  head ────────> 15 ────> 25 ────────> 40 ──────> 50 ─────────> NULL
                          │       │            │          │
Level 1:  head ──> 10 ──> 15 ──> 25 ──> 30 ──> 40 ──> 45 ──> 50 ──────> NULL
                   │      │      │      │      │      │      │
          data:   [10]   [15]   [25]   [30]   [40]   [45]   [50]

查找 45：
1. 从最高层开始
2. Level 4: head → 50（大于45，下降）
3. Level 3: head → 25 → 50（大于45，下降）
4. Level 2: 25 → 40 → 50（大于45，下降）
5. Level 1: 40 → 45（找到！）

只需 5 次比较，而非链表的 7 次
```

---

## Redis 跳表实现

### 数据结构定义

```c
// Redis 源码 server.h

// 跳表节点
typedef struct zskiplistNode {
    sds ele;                          // 元素值
    double score;                      // 分数
    struct zskiplistNode *backward;    // 后退指针（只有第一层有）
    struct zskiplistLevel {
        struct zskiplistNode *forward; // 前进指针
        unsigned long span;            // 跨度（用于计算排名）
    } level[];                         // 层级数组（柔性数组）
} zskiplistNode;

// 跳表
typedef struct zskiplist {
    struct zskiplistNode *header, *tail;  // 头尾指针
    unsigned long length;                  // 节点数量
    int level;                             // 最大层级
} zskiplist;
```

### 层级生成算法

```c
// 随机生成层级（幂次定律）
int zslRandomLevel(void) {
    int level = 1;
    // 每次有 1/4 概率增加一层
    while ((random() & 0xFFFF) < (ZSKIPLIST_P * 0xFFFF))
        level += 1;
    return (level < ZSKIPLIST_MAXLEVEL) ? level : ZSKIPLIST_MAXLEVEL;
}

// ZSKIPLIST_P = 0.25（1/4 概率）
// ZSKIPLIST_MAXLEVEL = 32（最大层数）
```

### 查找过程

```
查找 score=75 的元素：

Level 4:  head ────────────> 50 ─────────────> 100 ────> NULL
                              │                  │
Level 3:  head ────> 30 ────> 50 ────> 70 ────> 100 ────> NULL
                      │       │        │         │
Level 2:  head ─> 20 ─> 30 ─> 50 ─> 60 ─> 70 ─> 100 ────> NULL
                  │     │     │     │     │      │
Level 1:  head ─> 20 ─> 30 ─> 50 ─> 60 ─> 70 ─> 75 ─> 100 ─> NULL

路径：head → 50 → 70 → 75（找到）
```

---

## ZSet 底层实现

### 编码选择

ZSet 使用两种编码：

| 编码 | 条件 | 实现 |
|------|------|------|
| **ziplist** | 元素 < 128 且 每个元素 < 64 字节 | 压缩列表 |
| **skiplist** | 超出 ziplist 条件 | 跳表 + 字典 |

```
ZSet 底层结构（skiplist 编码）：

┌─────────────────────────────────────────────────────────┐
│                         zset                            │
├─────────────────────────────────────────────────────────┤
│  ┌─────────────────────┐  ┌─────────────────────────┐  │
│  │       dict          │  │      skiplist           │  │
│  │   (元素 → 分数)      │  │   (分数排序)             │  │
│  │   O(1) 查分数        │  │   O(log n) 范围查询      │  │
│  └─────────────────────┘  └─────────────────────────┘  │
└─────────────────────────────────────────────────────────┘

字典：member → score 映射，O(1) 获取分数
跳表：按 score 排序，支持范围查询
```

---

## Java 实现跳表

```java
public class SkipList<T> {
    
    private static final int MAX_LEVEL = 32;
    private static final double P = 0.25;
    
    private final Node<T> header;
    private int level;
    private int length;
    
    public SkipList() {
        this.header = new Node<>(null, Double.MIN_VALUE, MAX_LEVEL);
        this.level = 1;
        this.length = 0;
    }
    
    // 节点定义
    private static class Node<T> {
        T value;
        double score;
        Node<T>[] forward;
        
        @SuppressWarnings("unchecked")
        Node(T value, double score, int level) {
            this.value = value;
            this.score = score;
            this.forward = new Node[level];
        }
    }
    
    // 随机层级
    private int randomLevel() {
        int level = 1;
        while (Math.random() < P && level < MAX_LEVEL) {
            level++;
        }
        return level;
    }
    
    // 插入
    public void insert(T value, double score) {
        Node<T>[] update = new Node[MAX_LEVEL];
        Node<T> current = header;
        
        // 从最高层向下查找插入位置
        for (int i = level - 1; i >= 0; i--) {
            while (current.forward[i] != null && 
                   current.forward[i].score < score) {
                current = current.forward[i];
            }
            update[i] = current;
        }
        
        // 生成随机层级
        int newLevel = randomLevel();
        if (newLevel > level) {
            for (int i = level; i < newLevel; i++) {
                update[i] = header;
            }
            level = newLevel;
        }
        
        // 创建新节点
        Node<T> newNode = new Node<>(value, score, newLevel);
        
        // 更新指针
        for (int i = 0; i < newLevel; i++) {
            newNode.forward[i] = update[i].forward[i];
            update[i].forward[i] = newNode;
        }
        
        length++;
    }
    
    // 查找
    public T search(double score) {
        Node<T> current = header;
        
        for (int i = level - 1; i >= 0; i--) {
            while (current.forward[i] != null && 
                   current.forward[i].score < score) {
                current = current.forward[i];
            }
        }
        
        current = current.forward[0];
        if (current != null && current.score == score) {
            return current.value;
        }
        return null;
    }
    
    // 范围查询
    public List<T> rangeByScore(double min, double max) {
        List<T> result = new ArrayList<>();
        Node<T> current = header;
        
        // 找到 >= min 的第一个节点
        for (int i = level - 1; i >= 0; i--) {
            while (current.forward[i] != null && 
                   current.forward[i].score < min) {
                current = current.forward[i];
            }
        }
        
        current = current.forward[0];
        
        // 遍历直到 > max
        while (current != null && current.score <= max) {
            result.add(current.value);
            current = current.forward[0];
        }
        
        return result;
    }
}
```

---

## 面试要点

### 核心答案

**问：Redis 中跳表的实现原理是什么？**

答：

**基本结构**：
- 跳表是多层链表结构
- 每个节点包含：元素值、分数、多层前进指针
- 底层链表包含所有元素，上层是索引

**查询过程**：
1. 从最高层开始
2. 在每层向右查找，直到下一个节点分数大于目标
3. 降到下一层继续查找
4. 时间复杂度 O(log n)

**层级生成**：
- 随机化算法，1/4 概率增加一层
- 最大 32 层，保证效率

**为什么用跳表不用红黑树**：
1. 实现简单，易于维护
2. 范围查询效率高
3. 内存友好
4. 并发场景更容易加锁

---

## 编码最佳实践

### ✅ ZSet 使用推荐

```java
// 1. 合理使用 ZSet 排行榜
public Set<ZSetOperations.TypedTuple<Object>> getTopN(String key, int n) {
    return redisTemplate.opsForZSet()
        .reverseRangeWithScores(key, 0, n - 1);
}

// 2. 范围查询（利用跳表特性）
public Set<Object> getByScoreRange(String key, double min, double max) {
    return redisTemplate.opsForZSet().rangeByScore(key, min, max);
}

// 3. 计算排名（利用 span 属性）
public Long getRank(String key, Object member) {
    return redisTemplate.opsForZSet().reverseRank(key, member);
}
```

### ❌ 避免做法

```java
// ❌ 获取全部数据再排序（浪费跳表优势）
Set<Object> all = redisTemplate.opsForZSet().range(key, 0, -1);
// 客户端排序...

// ❌ 大量小范围查询（应合并为一次大范围查询）
for (int i = 0; i < 100; i++) {
    redisTemplate.opsForZSet().rangeByScore(key, i, i + 1);
}
```
