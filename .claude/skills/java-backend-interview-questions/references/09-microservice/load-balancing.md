# 负载均衡算法有哪些？

## 负载均衡概述

```
┌─────────────────────────────────────────────────────────────┐
│                    负载均衡作用                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│                     客户端请求                               │
│                         │                                   │
│                         ▼                                   │
│                ┌─────────────────┐                         │
│                │   负载均衡器    │                         │
│                │  (选择服务器)   │                         │
│                └────────┬────────┘                         │
│                         │                                   │
│          ┌──────────────┼──────────────┐                   │
│          ▼              ▼              ▼                   │
│     ┌────────┐    ┌────────┐    ┌────────┐                │
│     │ Server1│    │ Server2│    │ Server3│                │
│     └────────┘    └────────┘    └────────┘                │
│                                                             │
│   作用：                                                     │
│   • 分发请求到多台服务器                                    │
│   • 提高系统吞吐量                                          │
│   • 避免单点故障                                            │
│   • 横向扩展能力                                            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 一、轮询 (Round Robin)

```
┌─────────────────────────────────────────────────────────────┐
│                    轮询算法                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   原理：按顺序依次分发请求                                   │
│                                                             │
│   请求序列: 1 → Server1                                      │
│            2 → Server2                                      │
│            3 → Server3                                      │
│            4 → Server1                                      │
│            5 → Server2                                      │
│            ...                                              │
│                                                             │
│   优点：简单、公平                                           │
│   缺点：不考虑服务器性能差异                                 │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

```java
public class RoundRobinLoadBalancer {
    
    private List<String> servers;
    private AtomicInteger index = new AtomicInteger(0);
    
    public String select() {
        int current = index.getAndIncrement();
        return servers.get(current % servers.size());
    }
}
```

## 二、加权轮询 (Weighted Round Robin)

```
┌─────────────────────────────────────────────────────────────┐
│                    加权轮询                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   配置：Server1 权重=5, Server2 权重=3, Server3 权重=2       │
│                                                             │
│   请求分配 (10个请求):                                       │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  Server1: █████ (5个)                               │  │
│   │  Server2: ███ (3个)                                 │  │
│   │  Server3: ██ (2个)                                  │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   适用：服务器性能不一致                                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

```java
/**
 * 加权轮询 - 平滑加权轮询 (Nginx)
 */
public class SmoothWeightedRoundRobin {
    
    private List<Server> servers;
    
    @Data
    static class Server {
        String address;
        int weight;          // 配置权重
        int currentWeight;   // 当前权重
    }
    
    public Server select() {
        int totalWeight = servers.stream().mapToInt(Server::getWeight).sum();
        
        Server selected = null;
        int maxCurrentWeight = Integer.MIN_VALUE;
        
        for (Server server : servers) {
            // 当前权重 += 配置权重
            server.setCurrentWeight(server.getCurrentWeight() + server.getWeight());
            
            if (server.getCurrentWeight() > maxCurrentWeight) {
                maxCurrentWeight = server.getCurrentWeight();
                selected = server;
            }
        }
        
        // 选中的服务器当前权重 -= 总权重
        selected.setCurrentWeight(selected.getCurrentWeight() - totalWeight);
        
        return selected;
    }
}
```

## 三、随机 (Random)

```
┌─────────────────────────────────────────────────────────────┐
│                    随机算法                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   原理：随机选择一台服务器                                   │
│                                                             │
│   请求分配: 随机                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  Request1 → Server3                                 │  │
│   │  Request2 → Server1                                 │  │
│   │  Request3 → Server1                                 │  │
│   │  Request4 → Server2                                 │  │
│   │  ...                                                │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   大量请求时趋近于轮询效果                                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

```java
public class RandomLoadBalancer {
    
    private List<String> servers;
    private Random random = new Random();
    
    public String select() {
        return servers.get(random.nextInt(servers.size()));
    }
    
    // 加权随机
    public String selectWithWeight(Map<String, Integer> serverWeights) {
        int totalWeight = serverWeights.values().stream().mapToInt(i -> i).sum();
        int randomWeight = random.nextInt(totalWeight);
        
        int currentWeight = 0;
        for (Map.Entry<String, Integer> entry : serverWeights.entrySet()) {
            currentWeight += entry.getValue();
            if (currentWeight > randomWeight) {
                return entry.getKey();
            }
        }
        return null;
    }
}
```

## 四、最少连接 (Least Connections)

```
┌─────────────────────────────────────────────────────────────┐
│                    最少连接                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   原理：选择当前连接数最少的服务器                           │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  Server1: 当前连接 10                               │  │
│   │  Server2: 当前连接 5  ← 选择这个                    │  │
│   │  Server3: 当前连接 8                                │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   适用：长连接场景，请求处理时间差异大                       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

```java
public class LeastConnectionsLoadBalancer {
    
    private Map<String, AtomicInteger> serverConnections = new ConcurrentHashMap<>();
    
    public String select() {
        return serverConnections.entrySet().stream()
            .min(Comparator.comparingInt(e -> e.getValue().get()))
            .map(Map.Entry::getKey)
            .orElse(null);
    }
    
    public void onConnectionStart(String server) {
        serverConnections.get(server).incrementAndGet();
    }
    
    public void onConnectionEnd(String server) {
        serverConnections.get(server).decrementAndGet();
    }
}
```

## 五、源地址哈希 (IP Hash)

```
┌─────────────────────────────────────────────────────────────┐
│                    源地址哈希                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   原理：根据客户端 IP 计算哈希值，分配到固定服务器           │
│                                                             │
│   hash(客户端IP) % 服务器数量 = 服务器索引                   │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  IP: 192.168.1.100 → hash → Server1 (每次都一样)    │  │
│   │  IP: 192.168.1.101 → hash → Server2                 │  │
│   │  IP: 192.168.1.102 → hash → Server1                 │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   优点：会话保持，同一客户端总是访问同一服务器               │
│   缺点：服务器变动时哈希结果变化                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

```java
public class IpHashLoadBalancer {
    
    private List<String> servers;
    
    public String select(String clientIp) {
        int hash = Math.abs(clientIp.hashCode());
        return servers.get(hash % servers.size());
    }
}
```

## 六、一致性哈希 (Consistent Hashing)

```
┌─────────────────────────────────────────────────────────────┐
│                    一致性哈希                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   原理：服务器和请求都映射到哈希环上                         │
│                                                             │
│              0                                              │
│              │                                              │
│       Server1●           ●Server2                           │
│             /             \                                 │
│            /               \                                │
│           /                 \                               │
│          /                   \                              │
│         ●Request → 顺时针找到Server2                        │
│          \                   /                              │
│           \                 /                               │
│            \               /                                │
│             \             /                                 │
│       Server3●           ●Server4                           │
│              │                                              │
│            2^32                                             │
│                                                             │
│   优点：服务器增减时，只影响相邻节点                         │
│   改进：虚拟节点解决数据倾斜                                 │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

```java
public class ConsistentHashLoadBalancer {
    
    private TreeMap<Long, String> virtualNodes = new TreeMap<>();
    private static final int VIRTUAL_NODE_NUM = 100;  // 每个真实节点对应的虚拟节点数
    
    public void addServer(String server) {
        for (int i = 0; i < VIRTUAL_NODE_NUM; i++) {
            long hash = hash(server + "#" + i);
            virtualNodes.put(hash, server);
        }
    }
    
    public void removeServer(String server) {
        for (int i = 0; i < VIRTUAL_NODE_NUM; i++) {
            long hash = hash(server + "#" + i);
            virtualNodes.remove(hash);
        }
    }
    
    public String select(String key) {
        long hash = hash(key);
        // 顺时针找到第一个节点
        Map.Entry<Long, String> entry = virtualNodes.ceilingEntry(hash);
        if (entry == null) {
            entry = virtualNodes.firstEntry();  // 环形查找
        }
        return entry.getValue();
    }
    
    private long hash(String key) {
        // 使用 MurmurHash 等高质量哈希函数
        return Math.abs(key.hashCode());
    }
}
```

## 算法对比

```
┌─────────────────────────────────────────────────────────────┐
│                    算法对比                                  │
├──────────────┬──────────────────────────────────────────────┤
│   算法        │   特点                                       │
├──────────────┼──────────────────────────────────────────────┤
│   轮询        │   简单公平，不考虑服务器差异                 │
│   加权轮询    │   考虑服务器性能差异                         │
│   随机        │   简单，大量请求趋于均匀                     │
│   最少连接    │   动态调整，适合长连接                       │
│   IP 哈希     │   会话保持，同一客户端固定服务器             │
│   一致性哈希  │   节点变动影响小，适合分布式缓存             │
├──────────────┴──────────────────────────────────────────────┤
│                                                             │
│   选型建议：                                                 │
│   • 无状态服务：轮询 / 加权轮询                             │
│   • 有状态服务：IP 哈希 / 一致性哈希                        │
│   • 长连接场景：最少连接                                    │
│   • 分布式缓存：一致性哈希                                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## Nginx 配置示例

```nginx
# 轮询 (默认)
upstream backend {
    server 192.168.1.1:8080;
    server 192.168.1.2:8080;
    server 192.168.1.3:8080;
}

# 加权轮询
upstream backend {
    server 192.168.1.1:8080 weight=5;
    server 192.168.1.2:8080 weight=3;
    server 192.168.1.3:8080 weight=2;
}

# IP 哈希
upstream backend {
    ip_hash;
    server 192.168.1.1:8080;
    server 192.168.1.2:8080;
}

# 最少连接
upstream backend {
    least_conn;
    server 192.168.1.1:8080;
    server 192.168.1.2:8080;
}
```

## 面试回答

### 30秒版本

> 常见负载均衡算法：1）**轮询**：依次分发；2）**加权轮询**：按权重分发；3）**随机**：随机选择；4）**最少连接**：选择连接数最少的；5）**IP 哈希**：同一客户端访问同一服务器；6）**一致性哈希**：服务器变动影响小，适合分布式缓存。

### 1分钟版本

> **常见算法**：
>
> 1. **轮询 (Round Robin)**
>    - 简单公平，依次分发
>    - 不考虑服务器性能差异
>
> 2. **加权轮询**
>    - 按权重比例分发
>    - 适用服务器性能不一致
>
> 3. **随机**
>    - 随机选择服务器
>    - 大量请求趋于均匀
>
> 4. **最少连接**
>    - 选择当前连接数最少的服务器
>    - 适合长连接场景
>
> 5. **IP 哈希**
>    - 根据客户端 IP 计算哈希
>    - 会话保持
>
> 6. **一致性哈希**
>    - 哈希环 + 虚拟节点
>    - 节点变动影响小
>    - 适合分布式缓存
>
> **选型**：无状态用轮询，有状态用哈希，长连接用最少连接。

---

*关联文档：[rpc-framework-design.md](../14-system-design/rpc-framework-design.md) | [distributed-lock.md](../04-redis/distributed-lock.md)*
