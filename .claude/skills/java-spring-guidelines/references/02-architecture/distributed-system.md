# 分布式系统开发规范

> Java/Spring Boot 编码规范 - 分布式系统架构与部署

---

## 概述

当 Java 项目从**单机部署**演进到**分布式部署（多实例、多节点）**时，本质变化是：

**👉 原来"进程内能解决的问题"，现在都变成了"跨节点一致性问题"**

本规范涵盖分布式部署后必须重点考虑的问题，以及中间件集群部署的配置要点。

---

## 一、会话与状态管理

### 无状态化设计（强制要求）

**【强制】分布式环境下，所有业务服务必须实现无状态化。**

```java
// ❌ 错误：依赖本地 Session
@Controller
public class UserController {
    
    @PostMapping("/login")
    public String login(HttpSession session, LoginReq req) {
        User user = userService.login(req);
        session.setAttribute("user", user);  // ❌ 多实例下 Session 不共享
        return "success";
    }
}

// ✅ 正确：使用 Token 无状态化
@RestController
@RequiredArgsConstructor
public class UserController {
    
    private final IUserService userService;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @PostMapping("/login")
    public CommonResult<LoginRsp> login(@Valid @RequestBody LoginReq req) {
        // 1. 验证登录
        User user = userService.login(req);
        
        // 2. 生成 Token
        String token = UUID.randomUUID().toString();
        String key = "user:token:" + token;
        
        // 3. 存入 Redis（TTL 2小时）
        redisTemplate.opsForValue().set(key, user, 2, TimeUnit.HOURS);
        
        // 4. 返回 Token
        LoginRsp rsp = new LoginRsp();
        rsp.setToken(token);
        rsp.setUserId(user.getId());
        return CommonResult.success(rsp);
    }
    
    @GetMapping("/profile")
    public CommonResult<UserProfileRsp> getProfile(
        @RequestHeader("Authorization") String token
    ) {
        // 从 Redis 获取登录态
        String key = "user:token:" + token;
        User user = (User) redisTemplate.opsForValue().get(key);
        
        if (user == null) {
            throw exception(ErrorCode.UNAUTHORIZED);
        }
        
        return CommonResult.success(buildProfile(user));
    }
}
```

### JWT Token 方案（推荐）

```java
/**
 * JWT 工具类
 */
@Component
public class JwtTokenUtil {
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration:7200}")  // 默认 2 小时
    private Long expiration;
    
    /**
     * 生成 Token
     */
    public String generateToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        
        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000))
            .signWith(SignatureAlgorithm.HS512, secret)
            .compact();
    }
    
    /**
     * 解析 Token
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
            .setSigningKey(secret)
            .parseClaimsJws(token)
            .getBody();
    }
    
    /**
     * 校验 Token
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

### Session 共享方案（备选）

**仅在必须使用 Session 时采用。**

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.session</groupId>
    <artifactId>spring-session-data-redis</artifactId>
</dependency>
```

```yaml
# application.yml
spring:
  session:
    store-type: redis
    timeout: 7200  # 2小时
  redis:
    host: ${REDIS_HOST:localhost}
    port: 6379
    password: ${REDIS_PASSWORD}
    database: 1  # Session 专用库
```

```java
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 7200)
public class SessionConfig {
    // Spring Session 自动配置
}
```

---

## 二、并发控制与数据一致性

### 分布式锁（必备能力）

**【强制】多节点并发操作共享资源时，必须使用分布式锁。**

#### Redisson 分布式锁（推荐）

```xml
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.25.0</version>
</dependency>
```

```yaml
# application.yml
spring:
  redis:
    redisson:
      config: |
        singleServerConfig:
          address: "redis://${REDIS_HOST:localhost}:6379"
          password: ${REDIS_PASSWORD}
          database: 0
          connectionPoolSize: 64
          connectionMinimumIdleSize: 10
```

```java
/**
 * 分布式锁使用示例
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements IOrderService {
    
    private final RedissonClient redissonClient;
    private final OrderMapper orderMapper;
    
    /**
     * 创建订单（防止重复提交）
     */
    @Override
    public Long createOrder(OrderCreateReq req) {
        String lockKey = "order:create:" + req.getUserId();
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            // 尝试加锁，等待时间 3s，锁过期时间 10s
            boolean locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                throw exception(ErrorCode.ORDER_CREATE_BUSY);
            }
            
            // 业务逻辑
            return doCreateOrder(req);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw exception(ErrorCode.ORDER_CREATE_FAILED);
        } finally {
            // 释放锁（必须在 finally）
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
    
    /**
     * 库存扣减（高并发场景）
     */
    @Override
    public void deductStock(Long productId, Integer quantity) {
        String lockKey = "product:stock:" + productId;
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            lock.lock(5, TimeUnit.SECONDS);
            
            // 1. 查询库存
            Product product = productMapper.selectById(productId);
            if (product.getStock() < quantity) {
                throw exception(ErrorCode.STOCK_NOT_ENOUGH);
            }
            
            // 2. 扣减库存
            product.setStock(product.getStock() - quantity);
            productMapper.updateById(product);
            
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

#### 数据库层面控制

```java
/**
 * 乐观锁（version 字段）
 */
@Data
@TableName("t_product")
public class ProductDO {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Integer stock;
    
    @Version  // MyBatis-Plus 乐观锁
    private Integer version;
}

// Service 层
public void deductStock(Long productId, Integer quantity) {
    // 自动失败重试
    for (int i = 0; i < 3; i++) {
        ProductDO product = productMapper.selectById(productId);
        
        if (product.getStock() < quantity) {
            throw exception(ErrorCode.STOCK_NOT_ENOUGH);
        }
        
        product.setStock(product.getStock() - quantity);
        int updated = productMapper.updateById(product);  // version 自动 +1
        
        if (updated > 0) {
            return;  // 成功
        }
        // 失败则重试
    }
    
    throw exception(ErrorCode.STOCK_DEDUCT_FAILED);
}
```

```sql
-- 悲观锁（for update）
SELECT * FROM t_product WHERE id = #{id} FOR UPDATE;
```

### 幂等性设计（必备能力）

**【强制】所有写操作必须实现幂等性。**

```java
/**
 * 幂等性注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    
    /**
     * 幂等 Key 前缀
     */
    String prefix() default "idempotent";
    
    /**
     * 过期时间（秒）
     */
    long expireSeconds() default 300;
}

/**
 * 幂等性 AOP
 */
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotentAspect {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        // 1. 获取幂等 Token（从请求头）
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        String token = request.getHeader("Idempotent-Token");
        
        if (StringUtils.isBlank(token)) {
            throw exception(ErrorCode.IDEMPOTENT_TOKEN_REQUIRED);
        }
        
        // 2. 构造 Redis Key
        String key = idempotent.prefix() + ":" + token;
        
        // 3. 尝试设置（SETNX）
        Boolean success = redisTemplate.opsForValue()
            .setIfAbsent(key, "1", idempotent.expireSeconds(), TimeUnit.SECONDS);
        
        if (Boolean.FALSE.equals(success)) {
            throw exception(ErrorCode.DUPLICATE_REQUEST);
        }
        
        // 4. 执行业务
        try {
            return joinPoint.proceed();
        } catch (Exception e) {
            // 失败时删除 Key，允许重试
            redisTemplate.delete(key);
            throw e;
        }
    }
}

/**
 * 使用示例
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    
    private final IOrderService orderService;
    
    @PostMapping
    @Idempotent(prefix = "order:create", expireSeconds = 60)
    public CommonResult<Long> create(@Valid @RequestBody OrderCreateReq req) {
        Long orderId = orderService.createOrder(req);
        return CommonResult.success(orderId);
    }
}
```

**客户端获取幂等 Token：**

```java
/**
 * 获取幂等 Token 接口
 */
@GetMapping("/idempotent-token")
public CommonResult<String> getIdempotentToken() {
    String token = UUID.randomUUID().toString();
    return CommonResult.success(token);
}
```

---

## 三、分布式事务

### 事务方案选型

| 场景 | 方案 | 适用场景 | 一致性 | 性能 |
|------|------|---------|--------|------|
| 单库多表 | 本地事务 | MySQL | 强一致 | ⭐⭐⭐⭐⭐ |
| 多库操作 | Seata AT | 少量跨库 | 最终一致 | ⭐⭐⭐ |
| 业务补偿 | Seata TCC | 核心业务 | 强一致 | ⭐⭐ |
| 高并发 | 消息事务 | 异步场景 | 最终一致 | ⭐⭐⭐⭐ |
| 简单场景 | 本地消息表 | 自建方案 | 最终一致 | ⭐⭐⭐⭐ |

**【推荐】优先使用"最终一致性"方案。**

### Seata AT 模式（推荐）

```xml
<dependency>
    <groupId>io.seata</groupId>
    <artifactId>seata-spring-boot-starter</artifactId>
    <version>1.7.1</version>
</dependency>
```

```yaml
# application.yml
seata:
  enabled: true
  application-id: ${spring.application.name}
  tx-service-group: my-tx-group
  config:
    type: nacos
    nacos:
      server-addr: ${NACOS_ADDR}
      namespace: ${NACOS_NAMESPACE}
      group: SEATA_GROUP
  registry:
    type: nacos
    nacos:
      server-addr: ${NACOS_ADDR}
      namespace: ${NACOS_NAMESPACE}
      group: SEATA_GROUP
```

```java
/**
 * 分布式事务使用示例
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements IOrderService {
    
    private final OrderMapper orderMapper;
    private final PaymentClient paymentClient;  // Feign 调用支付服务
    private final InventoryClient inventoryClient;  // Feign 调用库存服务
    
    @Override
    @GlobalTransactional(name = "create-order", rollbackFor = Exception.class)
    public Long createOrder(OrderCreateReq req) {
        // 1. 创建订单
        OrderDO order = buildOrder(req);
        orderMapper.insert(order);
        
        // 2. 调用库存服务扣减库存（跨服务）
        inventoryClient.deductStock(req.getProductId(), req.getQuantity());
        
        // 3. 调用支付服务创建支付单（跨服务）
        paymentClient.createPayment(order.getId(), order.getAmount());
        
        return order.getId();
    }
}
```

### 消息事务（高并发场景）

```java
/**
 * 本地消息表方案
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements IOrderService {
    
    private final OrderMapper orderMapper;
    private final LocalMessageMapper messageMapper;
    private final RocketMQTemplate rocketMQTemplate;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(OrderCreateReq req) {
        // 1. 创建订单（本地事务）
        OrderDO order = buildOrder(req);
        orderMapper.insert(order);
        
        // 2. 插入本地消息表（同一事务）
        LocalMessageDO message = new LocalMessageDO();
        message.setBizId(order.getId().toString());
        message.setTopic("ORDER_CREATED");
        message.setContent(JSON.toJSONString(order));
        message.setStatus(0);  // 待发送
        messageMapper.insert(message);
        
        return order.getId();
    }
}

/**
 * 定时任务：扫描并发送消息
 */
@Component
@RequiredArgsConstructor
public class MessageSendJob {
    
    private final LocalMessageMapper messageMapper;
    private final RocketMQTemplate rocketMQTemplate;
    
    @Scheduled(fixedRate = 5000)  // 每 5 秒扫描一次
    public void sendPendingMessages() {
        List<LocalMessageDO> messages = messageMapper.selectPendingMessages(100);
        
        for (LocalMessageDO message : messages) {
            try {
                // 发送 MQ
                rocketMQTemplate.convertAndSend(message.getTopic(), message.getContent());
                
                // 标记已发送
                message.setStatus(1);
                messageMapper.updateById(message);
                
            } catch (Exception e) {
                log.error("[消息发送失败] messageId: {}", message.getId(), e);
                // 重试次数 +1
                message.setRetryCount(message.getRetryCount() + 1);
                messageMapper.updateById(message);
            }
        }
    }
}
```

---

## 四、缓存设计

### Redis 集群配置

```yaml
# application.yml - Redis Cluster 模式
spring:
  redis:
    cluster:
      nodes:
        - ${REDIS_NODE1:localhost:7001}
        - ${REDIS_NODE2:localhost:7002}
        - ${REDIS_NODE3:localhost:7003}
        - ${REDIS_NODE4:localhost:7004}
        - ${REDIS_NODE5:localhost:7005}
        - ${REDIS_NODE6:localhost:7006}
      max-redirects: 3
    password: ${REDIS_PASSWORD}
    lettuce:
      pool:
        max-active: 200
        max-idle: 50
        min-idle: 10
        max-wait: 3000ms
    timeout: 3000ms
```

### 缓存一致性策略

**【强制】分布式环境下，缓存更新必须考虑一致性问题。**

```java
/**
 * 缓存更新策略：延迟双删
 */
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements IProductService {
    
    private final ProductMapper productMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProduct(ProductUpdateReq req) {
        String cacheKey = "product:detail:" + req.getId();
        
        // 1. 先删缓存
        redisTemplate.delete(cacheKey);
        
        // 2. 更新数据库
        ProductDO product = new ProductDO();
        BeanUtils.copyProperties(req, product);
        productMapper.updateById(product);
        
        // 3. 延迟双删（异步执行，500ms 后再删一次）
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(500);
                redisTemplate.delete(cacheKey);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    @Override
    public ProductDetailRsp getProduct(Long id) {
        String cacheKey = "product:detail:" + id;
        
        // 1. 查缓存
        ProductDetailRsp cached = (ProductDetailRsp) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        // 2. 查数据库
        ProductDO product = productMapper.selectById(id);
        if (product == null) {
            // 缓存空值，防止缓存穿透
            redisTemplate.opsForValue().set(cacheKey, new ProductDetailRsp(), 5, TimeUnit.MINUTES);
            return null;
        }
        
        // 3. 写入缓存
        ProductDetailRsp rsp = buildDetailRsp(product);
        redisTemplate.opsForValue().set(cacheKey, rsp, 30, TimeUnit.MINUTES);
        
        return rsp;
    }
}
```

### 缓存击穿保护（热点 Key）

```java
/**
 * 缓存击穿保护：分布式锁 + 双重检查
 */
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements IProductService {
    
    private final ProductMapper productMapper;
    private final RedissonClient redissonClient;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Override
    public ProductDetailRsp getHotProduct(Long id) {
        String cacheKey = "product:hot:" + id;
        
        // 1. 查缓存
        ProductDetailRsp cached = (ProductDetailRsp) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        // 2. 缓存未命中，使用分布式锁防止击穿
        String lockKey = "product:lock:" + id;
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            lock.lock(5, TimeUnit.SECONDS);
            
            // 3. 双重检查
            cached = (ProductDetailRsp) redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return cached;
            }
            
            // 4. 查询数据库
            ProductDO product = productMapper.selectById(id);
            ProductDetailRsp rsp = buildDetailRsp(product);
            
            // 5. 写入缓存（热点数据 TTL 可以长一些）
            redisTemplate.opsForValue().set(cacheKey, rsp, 1, TimeUnit.HOURS);
            
            return rsp;
            
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

---

## 五、定时任务

### XXL-Job 分布式调度（推荐）

```xml
<dependency>
    <groupId>com.xuxueli</groupId>
    <artifactId>xxl-job-core</artifactId>
    <version>2.4.0</version>
</dependency>
```

```yaml
# application.yml
xxl:
  job:
    admin:
      addresses: ${XXL_JOB_ADMIN:http://localhost:8080/xxl-job-admin}
    executor:
      appname: ${spring.application.name}
      address:
      ip:
      port: 9999
      logpath: /data/applogs/xxl-job/jobhandler
      logretentiondays: 30
    accessToken: ${XXL_JOB_TOKEN}
```

```java
/**
 * XXL-Job 配置
 */
@Configuration
public class XxlJobConfig {
    
    @Value("${xxl.job.admin.addresses}")
    private String adminAddresses;
    
    @Value("${xxl.job.accessToken}")
    private String accessToken;
    
    @Value("${xxl.job.executor.appname}")
    private String appname;
    
    @Value("${xxl.job.executor.port}")
    private int port;
    
    @Value("${xxl.job.executor.logpath}")
    private String logPath;
    
    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
        xxlJobSpringExecutor.setAdminAddresses(adminAddresses);
        xxlJobSpringExecutor.setAppname(appname);
        xxlJobSpringExecutor.setPort(port);
        xxlJobSpringExecutor.setAccessToken(accessToken);
        xxlJobSpringExecutor.setLogPath(logPath);
        xxlJobSpringExecutor.setLogRetentionDays(30);
        return xxlJobSpringExecutor;
    }
}

/**
 * 定时任务示例
 */
@Component
@Slf4j
public class OrderJobHandler {
    
    /**
     * 订单超时关闭任务
     * 
     * 调度配置：
     * - 执行器：order-service
     * - 任务描述：订单超时关闭
     * - Cron：0 */5 * * * ?  （每 5 分钟）
     * - 路由策略：轮询（多实例负载均衡）
     */
    @XxlJob("orderTimeoutJob")
    public void closeTimeoutOrders() {
        log.info("[定时任务] 开始关闭超时订单");
        
        // 查询 30 分钟前创建且未支付的订单
        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(30);
        List<OrderDO> orders = orderMapper.selectTimeoutOrders(expireTime);
        
        for (OrderDO order : orders) {
            try {
                // 关闭订单
                orderService.closeOrder(order.getId());
                log.info("[定时任务] 订单已关闭: {}", order.getOrderNo());
            } catch (Exception e) {
                log.error("[定时任务] 订单关闭失败: {}", order.getOrderNo(), e);
            }
        }
        
        log.info("[定时任务] 完成，共处理 {} 个订单", orders.size());
    }
}
```

### @Scheduled + 分布式锁（简单场景）

```java
/**
 * @Scheduled + 分布式锁方案（适合简单场景）
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSyncJob {
    
    private final RedissonClient redissonClient;
    
    @Scheduled(cron = "0 0 2 * * ?")  // 每天凌晨 2 点
    public void syncData() {
        String lockKey = "job:data-sync";
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            // 尝试加锁，最多等待 0 秒，锁过期时间 10 分钟
            boolean locked = lock.tryLock(0, 10, TimeUnit.MINUTES);
            if (!locked) {
                log.info("[定时任务] 其他节点正在执行，跳过");
                return;
            }
            
            log.info("[定时任务] 开始数据同步");
            // 业务逻辑
            doSyncData();
            log.info("[定时任务] 数据同步完成");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
    
    private void doSyncData() {
        // 实际同步逻辑
    }
}
```

---

## 六、服务注册与发现

### Nacos 配置（推荐）

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
</dependency>
```

```yaml
# bootstrap.yml（优先级高于 application.yml）
spring:
  application:
    name: order-service
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_ADDR:localhost:8848}
        namespace: ${NACOS_NAMESPACE:public}
        group: ${NACOS_GROUP:DEFAULT_GROUP}
        username: ${NACOS_USERNAME:nacos}
        password: ${NACOS_PASSWORD:nacos}
        # 实例信息
        ip: ${SERVER_IP}  # 可选，自动获取
        port: ${server.port}
        weight: 1  # 权重
        cluster-name: ${CLUSTER_NAME:default}
        # 健康检查
        heart-beat-interval: 5000  # 心跳间隔 5s
        heart-beat-timeout: 15000  # 心跳超时 15s
        ip-delete-timeout: 30000   # 实例删除超时 30s
      config:
        server-addr: ${NACOS_ADDR:localhost:8848}
        namespace: ${NACOS_NAMESPACE:public}
        group: ${NACOS_GROUP:DEFAULT_GROUP}
        file-extension: yml
        # 共享配置
        shared-configs:
          - data-id: common-mysql.yml
            group: COMMON_GROUP
            refresh: true
          - data-id: common-redis.yml
            group: COMMON_GROUP
            refresh: true
```

```java
/**
 * 启动类
 */
@SpringBootApplication
@EnableDiscoveryClient  // 开启服务注册
public class OrderApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
```

### 服务调用（Feign + 负载均衡）

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
```

```java
/**
 * Feign 客户端
 */
@FeignClient(
    name = "product-service",  // 服务名（从 Nacos 获取）
    path = "/api/products",
    fallbackFactory = ProductClientFallbackFactory.class  // 熔断降级
)
public interface ProductClient {
    
    @GetMapping("/{id}")
    CommonResult<ProductDTO> getProduct(@PathVariable("id") Long id);
    
    @PostMapping("/deduct-stock")
    CommonResult<Void> deductStock(@RequestBody StockDeductReq req);
}

/**
 * 熔断降级
 */
@Component
@Slf4j
public class ProductClientFallbackFactory implements FallbackFactory<ProductClient> {
    
    @Override
    public ProductClient create(Throwable cause) {
        return new ProductClient() {
            
            @Override
            public CommonResult<ProductDTO> getProduct(Long id) {
                log.error("[Feign 调用失败] 获取商品详情，id: {}", id, cause);
                return CommonResult.error(ErrorCode.SERVICE_UNAVAILABLE, "商品服务暂时不可用");
            }
            
            @Override
            public CommonResult<Void> deductStock(StockDeductReq req) {
                log.error("[Feign 调用失败] 扣减库存，req: {}", req, cause);
                return CommonResult.error(ErrorCode.SERVICE_UNAVAILABLE, "库存服务暂时不可用");
            }
        };
    }
}
```

---

## 七、限流熔断降级（Sentinel）

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
</dependency>
```

```yaml
# application.yml
spring:
  cloud:
    sentinel:
      transport:
        dashboard: ${SENTINEL_DASHBOARD:localhost:8080}
        port: 8719  # 与 Dashboard 通信端口
      # 饥饿加载（启动时加载规则）
      eager: true
      # 数据源配置（Nacos）
      datasource:
        flow:
          nacos:
            server-addr: ${NACOS_ADDR}
            dataId: ${spring.application.name}-flow-rules
            groupId: SENTINEL_GROUP
            rule-type: flow
        degrade:
          nacos:
            server-addr: ${NACOS_ADDR}
            dataId: ${spring.application.name}-degrade-rules
            groupId: SENTINEL_GROUP
            rule-type: degrade
```

```java
/**
 * Sentinel 限流示例
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    /**
     * 创建订单（限流保护）
     */
    @PostMapping
    @SentinelResource(
        value = "createOrder",  // 资源名
        blockHandler = "createOrderBlockHandler",  // 限流处理
        fallback = "createOrderFallback"  // 异常降级
    )
    public CommonResult<Long> createOrder(@Valid @RequestBody OrderCreateReq req) {
        Long orderId = orderService.createOrder(req);
        return CommonResult.success(orderId);
    }
    
    /**
     * 限流处理（被限流时调用）
     */
    public CommonResult<Long> createOrderBlockHandler(
        OrderCreateReq req,
        BlockException ex
    ) {
        log.warn("[接口限流] 创建订单被限流: {}", req);
        return CommonResult.error(ErrorCode.TOO_MANY_REQUESTS, "系统繁忙，请稍后再试");
    }
    
    /**
     * 异常降级（业务异常时调用）
     */
    public CommonResult<Long> createOrderFallback(
        OrderCreateReq req,
        Throwable ex
    ) {
        log.error("[接口降级] 创建订单异常: {}", req, ex);
        return CommonResult.error(ErrorCode.SERVICE_ERROR, "订单创建失败，请重试");
    }
}

/**
 * 限流规则配置（也可以在 Sentinel Dashboard 配置）
 */
@Configuration
public class SentinelConfig {
    
    @PostConstruct
    public void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();
        
        // 创建订单接口：QPS 100
        FlowRule rule1 = new FlowRule();
        rule1.setResource("createOrder");
        rule1.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule1.setCount(100);
        rules.add(rule1);
        
        FlowRuleManager.loadRules(rules);
    }
    
    @PostConstruct
    public void initDegradeRules() {
        List<DegradeRule> rules = new ArrayList<>();
        
        // 创建订单接口：异常数超过 10 触发降级
        DegradeRule rule1 = new DegradeRule();
        rule1.setResource("createOrder");
        rule1.setGrade(CircuitBreakerStrategy.ERROR_COUNT.getType());
        rule1.setCount(10);
        rule1.setTimeWindow(60);  // 降级时间窗口 60s
        rules.add(rule1);
        
        DegradeRuleManager.loadRules(rules);
    }
}
```

---

## 八、配置中心

### Nacos 配置管理

**配置文件层次结构：**

```
Nacos 配置中心
├── common-mysql.yml           # 公共配置：MySQL
├── common-redis.yml           # 公共配置：Redis
├── common-mq.yml              # 公共配置：RocketMQ
├── order-service-dev.yml      # 订单服务 - 开发环境
├── order-service-test.yml     # 订单服务 - 测试环境
└── order-service-prod.yml     # 订单服务 - 生产环境
```

**配置示例：**

```yaml
# common-mysql.yml（Nacos 配置中心）
spring:
  datasource:
    type: com.zaxxer.hikari.HikariDataSource
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      pool-name: HikariPool
      minimum-idle: 10
      maximum-pool-size: 50
      max-lifetime: 1800000  # 30 分钟
      connection-timeout: 30000
      idle-timeout: 600000  # 10 分钟
      connection-test-query: SELECT 1

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    cache-enabled: false
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

```yaml
# order-service-prod.yml（Nacos 配置中心）
spring:
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:localhost}:3306/order_db?useSSL=true&serverTimezone=Asia/Shanghai
    username: ${MYSQL_USERNAME}
    password: ${MYSQL_PASSWORD}

# 业务配置
business:
  order:
    timeout-minutes: 30  # 订单超时时间
    max-items: 100       # 最大商品数
```

**动态配置刷新：**

```java
/**
 * 动态配置
 */
@Component
@RefreshScope  // 支持动态刷新
@ConfigurationProperties(prefix = "business.order")
@Data
public class OrderConfig {
    
    /**
     * 订单超时时间（分钟）
     */
    private Integer timeoutMinutes = 30;
    
    /**
     * 最大商品数
     */
    private Integer maxItems = 100;
}

/**
 * 使用配置
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements IOrderService {
    
    private final OrderConfig orderConfig;
    
    @Override
    public void createOrder(OrderCreateReq req) {
        // 使用动态配置
        if (req.getItems().size() > orderConfig.getMaxItems()) {
            throw exception(ErrorCode.ORDER_ITEMS_EXCEED_LIMIT);
        }
        
        // 业务逻辑...
    }
}
```

---

## 九、链路追踪

### SkyWalking 集成

```xml
<!-- 无需添加依赖，使用 Java Agent 方式 -->
```

**启动配置：**

```bash
# 启动脚本
java -javaagent:/path/to/skywalking-agent.jar \
     -Dskywalking.agent.service_name=order-service \
     -Dskywalking.collector.backend_service=skywalking-oap:11800 \
     -jar order-service.jar
```

**TraceId 传递：**

```java
/**
 * TraceId 拦截器（自动传递）
 */
@Component
public class TraceIdInterceptor implements HandlerInterceptor {
    
    private static final String TRACE_ID = "traceId";
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 从 SkyWalking 获取 TraceId
        String traceId = TraceContext.traceId();
        
        // 2. 放入 MDC（日志中可用）
        MDC.put(TRACE_ID, traceId);
        
        // 3. 放入响应头（方便排查）
        response.setHeader("X-Trace-Id", traceId);
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        MDC.remove(TRACE_ID);
    }
}
```

**日志配置：**

```xml
<!-- logback-spring.xml -->
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{traceId}] %-5level %logger{50} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

---

## 十、数据库集群配置

### MySQL 主从配置

```yaml
# application.yml - 读写分离
spring:
  datasource:
    # 主库（写）
    master:
      url: jdbc:mysql://${MYSQL_MASTER_HOST}:3306/order_db
      username: ${MYSQL_USERNAME}
      password: ${MYSQL_PASSWORD}
    # 从库（读）
    slave:
      url: jdbc:mysql://${MYSQL_SLAVE_HOST}:3306/order_db
      username: ${MYSQL_USERNAME}
      password: ${MYSQL_PASSWORD}

# MyBatis-Plus 读写分离插件
mybatis-plus:
  configuration:
    # 开启读写分离
    default-executor-type: reuse
```

```java
/**
 * 读写分离配置
 */
@Configuration
public class DataSourceConfig {
    
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.master")
    public DataSource masterDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.slave")
    public DataSource slaveDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    @Bean
    @Primary
    public DataSource dynamicDataSource() {
        DynamicRoutingDataSource dataSource = new DynamicRoutingDataSource();
        
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("master", masterDataSource());
        targetDataSources.put("slave", slaveDataSource());
        
        dataSource.setTargetDataSources(targetDataSources);
        dataSource.setDefaultTargetDataSource(masterDataSource());
        
        return dataSource;
    }
}

/**
 * 动态数据源
 */
public class DynamicRoutingDataSource extends AbstractRoutingDataSource {
    
    @Override
    protected Object determineCurrentLookupKey() {
        return DataSourceContextHolder.getDataSourceType();
    }
}

/**
 * 数据源上下文
 */
public class DataSourceContextHolder {
    
    private static final ThreadLocal<String> CONTEXT_HOLDER = new ThreadLocal<>();
    
    public static void setDataSourceType(String dataSourceType) {
        CONTEXT_HOLDER.set(dataSourceType);
    }
    
    public static String getDataSourceType() {
        return CONTEXT_HOLDER.get();
    }
    
    public static void clearDataSourceType() {
        CONTEXT_HOLDER.remove();
    }
}

/**
 * 读写分离注解
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataSource {
    String value() default "master";
}

/**
 * 读写分离 AOP
 */
@Aspect
@Component
@Order(1)  // 优先级高于事务
public class DataSourceAspect {
    
    @Around("@annotation(dataSource)")
    public Object around(ProceedingJoinPoint point, DataSource dataSource) throws Throwable {
        try {
            DataSourceContextHolder.setDataSourceType(dataSource.value());
            return point.proceed();
        } finally {
            DataSourceContextHolder.clearDataSourceType();
        }
    }
}

/**
 * 使用示例
 */
@Service
public class OrderServiceImpl implements IOrderService {
    
    // 写操作：走主库
    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataSource("master")
    public Long createOrder(OrderCreateReq req) {
        // 写操作
        return orderId;
    }
    
    // 读操作：走从库
    @Override
    @DataSource("slave")
    public OrderDetailRsp getOrder(Long id) {
        // 读操作
        return orderDetail;
    }
}
```

### ShardingSphere 分库分表

```xml
<dependency>
    <groupId>org.apache.shardingsphere</groupId>
    <artifactId>shardingsphere-jdbc-core</artifactId>
    <version>5.3.2</version>
</dependency>
```

```yaml
# application-sharding.yml
spring:
  shardingsphere:
    datasource:
      names: ds0,ds1
      # 数据源 0
      ds0:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://localhost:3306/order_db_0
        username: root
        password: password
      # 数据源 1
      ds1:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://localhost:3306/order_db_1
        username: root
        password: password
    
    # 分片规则
    rules:
      sharding:
        tables:
          # 订单表分片
          t_order:
            actual-data-nodes: ds$->{0..1}.t_order_$->{0..3}  # 2 库 4 表
            database-strategy:
              standard:
                sharding-column: user_id
                sharding-algorithm-name: order-db-inline
            table-strategy:
              standard:
                sharding-column: order_id
                sharding-algorithm-name: order-table-inline
            key-generate-strategy:
              column: order_id
              key-generator-name: snowflake
        
        # 分片算法
        sharding-algorithms:
          order-db-inline:
            type: INLINE
            props:
              algorithm-expression: ds$->{user_id % 2}
          order-table-inline:
            type: INLINE
            props:
              algorithm-expression: t_order_$->{order_id % 4}
        
        # 主键生成策略
        key-generators:
          snowflake:
            type: SNOWFLAKE
    
    props:
      sql-show: true  # 显示 SQL
```

---

## 十一、消息队列集群配置

### RocketMQ 集群配置

```yaml
# application.yml
rocketmq:
  name-server: ${ROCKETMQ_NAME_SERVER:localhost:9876}
  producer:
    group: ${spring.application.name}-producer
    send-message-timeout: 3000
    retry-times-when-send-failed: 2
    retry-times-when-send-async-failed: 2
    max-message-size: 4194304  # 4MB
  consumer:
    group: ${spring.application.name}-consumer
    pull-batch-size: 10
```

```java
/**
 * RocketMQ 生产者配置
 */
@Configuration
public class RocketMQConfig {
    
    @Bean
    public RocketMQTemplate rocketMQTemplate() {
        // Spring Boot 自动配置
        return new RocketMQTemplate();
    }
}

/**
 * 消息发送示例
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderMessageProducer {
    
    private final RocketMQTemplate rocketMQTemplate;
    
    /**
     * 发送普通消息
     */
    public void sendOrderCreatedMessage(OrderDO order) {
        String topic = "ORDER_CREATED";
        String tag = "create";
        String destination = topic + ":" + tag;
        
        OrderCreatedEvent event = buildEvent(order);
        
        SendResult sendResult = rocketMQTemplate.syncSend(
            destination,
            event,
            3000  // 超时时间
        );
        
        log.info("[MQ发送] 订单创建消息，orderId: {}, msgId: {}", 
                 order.getId(), sendResult.getMsgId());
    }
    
    /**
     * 发送事务消息
     */
    public void sendTransactionMessage(OrderDO order) {
        String topic = "ORDER_TRANSACTION";
        String destination = topic + ":create";
        
        OrderCreatedEvent event = buildEvent(order);
        
        TransactionSendResult sendResult = rocketMQTemplate.sendMessageInTransaction(
            destination,
            MessageBuilder.withPayload(event).build(),
            order.getId()  // 传递给 TransactionListener
        );
        
        log.info("[MQ事务消息] orderId: {}, msgId: {}, state: {}", 
                 order.getId(), sendResult.getMsgId(), sendResult.getLocalTransactionState());
    }
}

/**
 * 事务消息监听器
 */
@RocketMQTransactionListener
@Slf4j
public class OrderTransactionListener implements RocketMQLocalTransactionListener {
    
    @Resource
    private OrderMapper orderMapper;
    
    /**
     * 执行本地事务
     */
    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        Long orderId = (Long) arg;
        
        try {
            // 执行本地事务
            OrderDO order = orderMapper.selectById(orderId);
            if (order != null) {
                return RocketMQLocalTransactionState.COMMIT;
            }
            return RocketMQLocalTransactionState.ROLLBACK;
            
        } catch (Exception e) {
            log.error("[事务消息] 本地事务执行异常", e);
            return RocketMQLocalTransactionState.ROLLBACK;
        }
    }
    
    /**
     * 检查本地事务状态（用于事务回查）
     */
    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        String msgId = msg.getHeaders().get("rocketmq_KEYS", String.class);
        
        // 查询订单是否存在
        OrderDO order = orderMapper.selectById(msgId);
        if (order != null) {
            return RocketMQLocalTransactionState.COMMIT;
        }
        
        return RocketMQLocalTransactionState.ROLLBACK;
    }
}

/**
 * 消息消费者
 */
@Component
@RocketMQMessageListener(
    topic = "ORDER_CREATED",
    consumerGroup = "order-consumer-group",
    selectorExpression = "create",  // Tag 过滤
    consumeMode = ConsumeMode.CONCURRENTLY,  // 并发消费
    messageModel = MessageModel.CLUSTERING  // 集群模式
)
@Slf4j
public class OrderCreatedConsumer implements RocketMQListener<OrderCreatedEvent> {
    
    @Override
    public void onMessage(OrderCreatedEvent event) {
        log.info("[MQ消费] 订单创建消息，orderId: {}", event.getOrderId());
        
        try {
            // 业务处理（必须保证幂等性）
            processOrderCreated(event);
            
        } catch (Exception e) {
            log.error("[MQ消费] 处理失败，orderId: {}", event.getOrderId(), e);
            throw e;  // 抛出异常触发重试
        }
    }
    
    private void processOrderCreated(OrderCreatedEvent event) {
        // 幂等性检查
        String key = "order:processed:" + event.getOrderId();
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "1", 1, TimeUnit.DAYS);
        
        if (Boolean.FALSE.equals(success)) {
            log.info("[MQ消费] 消息已处理，跳过，orderId: {}", event.getOrderId());
            return;
        }
        
        // 实际业务处理
        // ...
    }
}
```

---

## 十二、分布式系统 Checklist

### 上线前检查清单

| 类别 | 检查项 | 说明 | 是否完成 |
|------|--------|------|---------|
| **会话管理** | 无状态化 | 使用 Token/JWT | ☐ |
| **并发控制** | 分布式锁 | Redis/Redisson | ☐ |
| **并发控制** | 幂等性设计 | 防止重复提交 | ☐ |
| **事务** | 分布式事务方案 | Seata/消息事务 | ☐ |
| **缓存** | Redis 集群 | Cluster 模式 | ☐ |
| **缓存** | 缓存一致性 | 延迟双删 | ☐ |
| **缓存** | 缓存击穿保护 | 分布式锁 | ☐ |
| **服务治理** | 服务注册发现 | Nacos | ☐ |
| **服务治理** | 负载均衡 | Ribbon/LoadBalancer | ☐ |
| **服务治理** | 限流熔断 | Sentinel | ☐ |
| **定时任务** | 分布式调度 | XXL-Job | ☐ |
| **配置** | 配置中心 | Nacos Config | ☐ |
| **配置** | 敏感配置 | 环境变量 | ☐ |
| **日志** | 链路追踪 | SkyWalking | ☐ |
| **日志** | TraceId 传递 | MDC | ☐ |
| **监控** | 应用监控 | Prometheus | ☐ |
| **监控** | 告警配置 | 异常率/RT | ☐ |
| **数据库** | 连接池配置 | Hikari | ☐ |
| **数据库** | 读写分离 | 主从复制 | ☐ |
| **消息队列** | 集群部署 | RocketMQ Cluster | ☐ |
| **消息队列** | 消息幂等 | 业务去重 | ☐ |

---

## 总结

**分布式部署的核心挑战：**

> **在多节点、不可靠网络的前提下，保证系统的一致性、稳定性和可扩展性。**

**关键设计原则：**

1. **无状态化**：服务必须无状态，状态外置到 Redis/DB
2. **幂等性**：所有写操作必须幂等
3. **容错性**：设计时假设服务会失败
4. **可观测性**：完善的日志、监控、链路追踪
5. **最终一致性**：接受异步，不追求强一致

**避坑指南：**

* ❌ 不要在分布式环境使用本地锁
* ❌ 不要假设数据库事务能跨服务
* ❌ 不要无限制重试
* ❌ 不要在多节点直接运行定时任务
* ❌ 不要忽略缓存一致性问题
