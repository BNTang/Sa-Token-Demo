# 如何设计一个秒杀系统？

## 系统特点与挑战

```
┌─────────────────────────────────────────────────────────────┐
│                    秒杀系统特点                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 高并发                                                 │
│      └── 短时间内大量用户同时请求                            │
│      └── 可能达到正常流量的 100-1000 倍                      │
│                                                             │
│   2. 库存有限                                               │
│      └── 商品数量远小于请求数量                              │
│      └── 必须保证不超卖                                      │
│                                                             │
│   3. 瞬时峰值                                               │
│      └── 流量在秒杀开始瞬间爆发                              │
│      └── 持续时间短（秒级到分钟级）                          │
│                                                             │
│   4. 恶意请求                                               │
│      └── 黄牛脚本、爬虫                                      │
│      └── 需要防刷机制                                        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                    秒杀系统架构                              │
├─────────────────────────────────────────────────────────────┐
│                                                             │
│   用户 ──→ CDN ──→ 负载均衡 ──→ API Gateway ──→ 秒杀服务     │
│            │                        │              │        │
│            ▼                        ▼              ▼        │
│        静态资源              限流/鉴权/防刷      Redis库存   │
│                                                    │        │
│                                                    ▼        │
│                                             异步下单队列    │
│                                                    │        │
│                                                    ▼        │
│                                               订单服务      │
│                                                    │        │
│                                                    ▼        │
│                                               数据库持久化   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 核心设计

### 1. 前端层

```javascript
// 1. 静态资源 CDN 化
// 页面模板、JS、CSS、图片全部放 CDN

// 2. 按钮防重复点击
let isSubmitting = false;
function submitSeckill() {
    if (isSubmitting) return;
    isSubmitting = true;
    
    // 发送请求
    fetch('/seckill/submit', {...})
        .finally(() => {
            setTimeout(() => isSubmitting = false, 3000);
        });
}

// 3. 动态获取 URL（防爬虫）
// 秒杀开始前，前端轮询获取真实的秒杀接口地址
let seckillUrl = null;
function fetchSeckillUrl() {
    fetch('/seckill/getUrl?productId=123')
        .then(res => res.json())
        .then(data => {
            seckillUrl = data.url;  // 动态生成的带 token 的 URL
        });
}
```

### 2. 网关层

```java
// API Gateway 限流配置 (Spring Cloud Gateway + Sentinel)
@Configuration
public class GatewayConfig {
    
    @Bean
    public RouteLocator seckillRoute(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("seckill", r -> r.path("/seckill/**")
                .filters(f -> f
                    .requestRateLimiter(c -> c
                        .setRateLimiter(redisRateLimiter())
                        .setKeyResolver(userKeyResolver()))  // 用户级限流
                    .filter(new AntiCrawlerFilter()))        // 防爬虫
                .uri("lb://seckill-service"))
            .build();
    }
    
    // 用户级限流：每用户每秒 1 次
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> Mono.just(
            exchange.getRequest().getHeaders().getFirst("X-User-Id")
        );
    }
}
```

### 3. 秒杀服务层

```java
@Service
public class SeckillService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    
    // Lua 脚本：库存预扣减 + 防止重复购买
    private static final String SECKILL_SCRIPT = 
        "local stock = tonumber(redis.call('GET', KEYS[1])) " +
        "if stock == nil or stock <= 0 then " +
        "    return -1 " +                          // 库存不足
        "end " +
        "if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then " +
        "    return -2 " +                          // 已购买过
        "end " +
        "redis.call('DECR', KEYS[1]) " +            // 扣减库存
        "redis.call('SADD', KEYS[2], ARGV[1]) " +   // 记录购买用户
        "return 1";                                  // 成功
    
    /**
     * 秒杀入口
     */
    public SeckillResult seckill(Long userId, Long productId) {
        // 1. 基本校验
        if (!isActivityValid(productId)) {
            return SeckillResult.fail("活动未开始或已结束");
        }
        
        // 2. Redis 预扣减库存（原子操作）
        String stockKey = "seckill:stock:" + productId;
        String boughtKey = "seckill:bought:" + productId;
        
        Long result = redisTemplate.execute(
            new DefaultRedisScript<>(SECKILL_SCRIPT, Long.class),
            Arrays.asList(stockKey, boughtKey),
            String.valueOf(userId)
        );
        
        if (result == -1) {
            return SeckillResult.fail("商品已售罄");
        }
        if (result == -2) {
            return SeckillResult.fail("您已购买过该商品");
        }
        
        // 3. 发送订单消息到 MQ（异步创建订单）
        SeckillOrderMessage message = new SeckillOrderMessage(userId, productId);
        rocketMQTemplate.asyncSend("seckill-order-topic", message, 
            new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    log.info("订单消息发送成功: {}", sendResult.getMsgId());
                }
                @Override
                public void onException(Throwable e) {
                    // 消息发送失败，回滚库存
                    redisTemplate.opsForValue().increment(stockKey, 1);
                    redisTemplate.opsForSet().remove(boughtKey, String.valueOf(userId));
                    log.error("订单消息发送失败", e);
                }
            });
        
        // 4. 返回排队结果（前端轮询订单状态）
        return SeckillResult.queuing("秒杀成功，订单创建中...");
    }
    
    /**
     * 查询秒杀结果（前端轮询）
     */
    public SeckillResult queryResult(Long userId, Long productId) {
        // 查询订单是否创建成功
        String orderKey = "seckill:order:" + productId + ":" + userId;
        String orderId = redisTemplate.opsForValue().get(orderKey);
        
        if (orderId != null) {
            return SeckillResult.success("秒杀成功", orderId);
        }
        
        // 检查是否在购买集合中
        String boughtKey = "seckill:bought:" + productId;
        if (redisTemplate.opsForSet().isMember(boughtKey, String.valueOf(userId))) {
            return SeckillResult.queuing("订单创建中，请稍候...");
        }
        
        return SeckillResult.fail("秒杀失败");
    }
}
```

### 4. 订单服务层（消费 MQ）

```java
@Service
@RocketMQMessageListener(topic = "seckill-order-topic", consumerGroup = "seckill-order-consumer")
public class SeckillOrderConsumer implements RocketMQListener<SeckillOrderMessage> {
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Override
    public void onMessage(SeckillOrderMessage message) {
        try {
            // 1. 创建订单（带事务，保证幂等）
            Order order = orderService.createSeckillOrder(
                message.getUserId(), 
                message.getProductId()
            );
            
            // 2. 订单创建成功，写入 Redis（供前端查询）
            String orderKey = "seckill:order:" + message.getProductId() + ":" + message.getUserId();
            redisTemplate.opsForValue().set(orderKey, order.getId(), 30, TimeUnit.MINUTES);
            
            log.info("秒杀订单创建成功: {}", order.getId());
            
        } catch (Exception e) {
            log.error("秒杀订单创建失败", e);
            // 根据异常类型决定是否重试或回滚
            throw e;  // 抛出异常，RocketMQ 会重试
        }
    }
}

@Service
public class OrderService {
    
    @Autowired
    private ProductMapper productMapper;
    
    @Autowired
    private OrderMapper orderMapper;
    
    @Transactional
    public Order createSeckillOrder(Long userId, Long productId) {
        // 1. 幂等校验（防止重复消费）
        Order existOrder = orderMapper.selectByUserAndProduct(userId, productId);
        if (existOrder != null) {
            return existOrder;
        }
        
        // 2. 数据库扣减库存（乐观锁 + 库存校验）
        int affected = productMapper.deductStock(productId, 1);
        if (affected == 0) {
            throw new BusinessException("库存扣减失败");
        }
        
        // 3. 创建订单
        Order order = new Order();
        order.setUserId(userId);
        order.setProductId(productId);
        order.setStatus(OrderStatus.CREATED);
        order.setCreateTime(LocalDateTime.now());
        orderMapper.insert(order);
        
        return order;
    }
}

// ProductMapper.xml
// <update id="deductStock">
//     UPDATE product 
//     SET stock = stock - #{quantity}
//     WHERE id = #{productId} AND stock >= #{quantity}
// </update>
```

### 5. 库存预热

```java
@Component
public class StockWarmUp {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Autowired
    private ProductMapper productMapper;
    
    /**
     * 秒杀活动开始前预热库存到 Redis
     */
    @Scheduled(cron = "0 0 9 * * ?")  // 每天 9 点执行
    public void warmUpStock() {
        // 获取今日秒杀商品
        List<SeckillProduct> products = productMapper.getTodaySeckillProducts();
        
        for (SeckillProduct product : products) {
            String stockKey = "seckill:stock:" + product.getId();
            String boughtKey = "seckill:bought:" + product.getId();
            
            // 预热库存
            redisTemplate.opsForValue().set(stockKey, 
                String.valueOf(product.getStock()));
            
            // 初始化购买用户集合
            redisTemplate.delete(boughtKey);
            
            log.info("库存预热完成: productId={}, stock={}", 
                product.getId(), product.getStock());
        }
    }
}
```

## 关键技术点

```
┌─────────────────────────────────────────────────────────────┐
│                     关键技术点                               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 流量控制                                               │
│      ├── 前端：按钮防抖、验证码、动态 URL                     │
│      ├── 网关：限流（用户/IP级）、黑名单                      │
│      └── 服务：令牌桶/滑动窗口限流                           │
│                                                             │
│   2. 库存控制                                               │
│      ├── Redis 预扣减（Lua 脚本保证原子性）                  │
│      ├── 异步下单（MQ 削峰填谷）                             │
│      └── 数据库乐观锁（stock >= 0 兜底）                     │
│                                                             │
│   3. 超卖防护                                               │
│      ├── Redis 原子扣减                                     │
│      ├── 用户购买记录（Set）                                │
│      └── 数据库唯一约束 (user_id, product_id)               │
│                                                             │
│   4. 热点数据                                               │
│      ├── 本地缓存（Caffeine）                               │
│      ├── Redis 集群                                         │
│      └── 分片设计（stock:1, stock:2...）                    │
│                                                             │
│   5. 降级兜底                                               │
│      ├── 熔断降级（Sentinel）                               │
│      ├── 静态化页面                                         │
│      └── 兜底返回"系统繁忙"                                 │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 流程总结

```
┌─────────────────────────────────────────────────────────────┐
│                    秒杀完整流程                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ① 活动开始前                                              │
│      └── 库存预热到 Redis                                   │
│                                                             │
│   ② 用户点击秒杀                                            │
│      └── 前端防抖 + 验证码                                   │
│                                                             │
│   ③ 网关层                                                  │
│      └── 限流 + 鉴权 + 防刷                                  │
│                                                             │
│   ④ 秒杀服务                                                │
│      ├── 校验活动状态                                        │
│      ├── Redis Lua 预扣库存（原子操作）                      │
│      └── 发送 MQ 消息                                       │
│                                                             │
│   ⑤ 订单服务（异步）                                        │
│      ├── 消费 MQ 消息                                       │
│      ├── 数据库扣库存 + 创建订单（事务）                      │
│      └── 写订单结果到 Redis                                  │
│                                                             │
│   ⑥ 前端轮询                                                │
│      └── 查询秒杀结果，跳转支付页                            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> 秒杀系统核心是 **限流削峰 + 异步处理 + 缓存库存**。具体：1）前端防抖、网关限流拦截大部分请求；2）Redis Lua 脚本原子预扣库存；3）MQ 异步创建订单削峰；4）数据库乐观锁兜底防超卖。

### 1分钟版本

> **架构设计**：
> 1. **前端层**：静态资源 CDN、按钮防抖、验证码、动态 URL（防爬虫）
> 2. **网关层**：用户/IP 级限流、黑名单、鉴权
> 3. **服务层**：Redis Lua 预扣库存（原子操作，防超卖+防重复购买）
> 4. **异步层**：MQ 异步下单，削峰填谷
> 5. **数据库**：乐观锁扣库存 `stock >= quantity`，唯一约束防重
>
> **核心保障**：
> - **防超卖**：Redis 原子扣减 + DB 乐观锁
> - **防重复**：Redis Set 记录购买用户 + DB 唯一约束
> - **高可用**：库存预热、熔断降级、静态化兜底
>
> **流程**：用户点击 → 网关限流 → Redis 预扣减 → MQ 发消息 → 异步创建订单 → 前端轮询结果

---

*关联文档：[redis-lua-script.md](../04-redis/redis-lua-script.md) | [message-reliability.md](../12-mq/message-reliability.md) | [distributed-id-generator.md](distributed-id-generator.md)*
