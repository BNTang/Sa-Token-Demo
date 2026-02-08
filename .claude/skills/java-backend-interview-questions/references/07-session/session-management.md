# Cookie、Session、Token

> Java 后端面试知识点 - 会话管理

---

## 三者对比

| 特性 | Cookie | Session | Token (JWT) |
|------|--------|---------|-------------|
| **存储位置** | 客户端（浏览器） | 服务端 | 客户端 |
| **安全性** | 较低 | 较高 | 中等（需加密） |
| **大小限制** | 4KB | 无限制 | 无限制（但建议小） |
| **有效期** | 可设置 | 服务端控制 | Token 自带过期时间 |
| **跨域** | 同源限制 | 需配合 Cookie | 支持跨域 |
| **服务端存储** | 无 | 需要（内存/Redis） | 无需 |
| **分布式支持** | 天然支持 | 需要共享存储 | 天然支持 |

---

## 1. Cookie

### 工作原理

```
1. 客户端首次请求
   Client ──────────────────────> Server
   
2. 服务端响应，设置 Cookie
   Client <────────────────────── Server
                Set-Cookie: session_id=abc123
                
3. 后续请求自动携带 Cookie
   Client ──────────────────────> Server
           Cookie: session_id=abc123
```

### 代码实现

```java
@RestController
public class CookieController {
    
    @GetMapping("/login")
    public String login(HttpServletResponse response) {
        // 创建 Cookie
        Cookie cookie = new Cookie("user_token", "abc123");
        cookie.setMaxAge(7 * 24 * 60 * 60);  // 7天
        cookie.setPath("/");
        cookie.setHttpOnly(true);   // 防止 XSS
        cookie.setSecure(true);     // 仅 HTTPS
        cookie.setSameSite("Strict"); // 防止 CSRF
        
        response.addCookie(cookie);
        return "登录成功";
    }
    
    @GetMapping("/user")
    public String getUser(@CookieValue("user_token") String token) {
        return "Token: " + token;
    }
    
    @GetMapping("/logout")
    public String logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("user_token", null);
        cookie.setMaxAge(0);  // 删除 Cookie
        cookie.setPath("/");
        response.addCookie(cookie);
        return "已登出";
    }
}
```

### 安全配置

```java
// Spring Security 配置
@Configuration
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()));
        return http.build();
    }
}
```

---

## 2. Session

### 工作原理

```
1. 客户端首次请求
   Client ──────────────────────> Server
   
2. 服务端创建 Session，返回 Session ID（通过 Cookie）
   Client <────────────────────── Server
                Set-Cookie: JSESSIONID=xyz789
                
   Server 内存/Redis:
   ┌─────────────────────────────────────┐
   │ Session Store                       │
   │ xyz789 → { userId: 1, name: "张三" } │
   └─────────────────────────────────────┘
                
3. 后续请求携带 Session ID
   Client ──────────────────────> Server
           Cookie: JSESSIONID=xyz789
           
   Server 根据 ID 查找 Session 数据
```

### 代码实现

```java
@RestController
public class SessionController {
    
    @PostMapping("/login")
    public String login(@RequestBody LoginRequest request, 
                        HttpSession session) {
        // 验证用户
        User user = userService.authenticate(request);
        
        // 存入 Session
        session.setAttribute("user", user);
        session.setMaxInactiveInterval(30 * 60);  // 30分钟
        
        return "登录成功";
    }
    
    @GetMapping("/user")
    public User getUser(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            throw new UnauthorizedException("未登录");
        }
        return user;
    }
    
    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "已登出";
    }
}
```

### 分布式 Session（Spring Session + Redis）

```xml
<dependency>
    <groupId>org.springframework.session</groupId>
    <artifactId>spring-session-data-redis</artifactId>
</dependency>
```

```yaml
spring:
  session:
    store-type: redis
    timeout: 30m
  redis:
    host: localhost
    port: 6379
```

```java
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800)
public class SessionConfig {
    
    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        return new GenericJackson2JsonRedisSerializer();
    }
}
```

---

## 3. Token (JWT)

### 工作原理

```
1. 客户端登录
   Client ──────────────────────> Server
           { username, password }
           
2. 服务端验证，生成 JWT Token
   Client <────────────────────── Server
           { token: "eyJhbGc..." }
           
3. 客户端存储 Token（LocalStorage 或 Cookie）

4. 后续请求携带 Token
   Client ──────────────────────> Server
           Authorization: Bearer eyJhbGc...
           
   Server 验证 Token 签名，解析用户信息
   （无需查询存储）
```

### JWT 结构

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.
eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IuW8oOS4iSIsImlhdCI6MTUxNjIzOTAyMn0.
SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c

┌─────────────────┐
│     Header      │  {"alg": "HS256", "typ": "JWT"}
├─────────────────┤
│     Payload     │  {"sub": "1234567890", "name": "张三", "iat": 1516239022}
├─────────────────┤
│    Signature    │  HMACSHA256(base64(header) + "." + base64(payload), secret)
└─────────────────┘
```

### 代码实现

```java
@Component
public class JwtUtils {
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration}")
    private long expiration;  // 毫秒
    
    // 生成 Token
    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
            .setSubject(String.valueOf(user.getId()))
            .claim("username", user.getUsername())
            .claim("roles", user.getRoles())
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(SignatureAlgorithm.HS256, secret)
            .compact();
    }
    
    // 解析 Token
    public Claims parseToken(String token) {
        return Jwts.parser()
            .setSigningKey(secret)
            .parseClaimsJws(token)
            .getBody();
    }
    
    // 验证 Token
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secret).parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
    
    // 从 Token 获取用户 ID
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }
}
```

### JWT 过滤器

```java
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) 
            throws ServletException, IOException {
        
        String token = getTokenFromRequest(request);
        
        if (StringUtils.hasText(token) && jwtUtils.validateToken(token)) {
            Long userId = jwtUtils.getUserIdFromToken(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(
                String.valueOf(userId));
            
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

### Refresh Token 机制

```java
@Service
@RequiredArgsConstructor
public class TokenService {
    
    private final JwtUtils jwtUtils;
    private final StringRedisTemplate redisTemplate;
    
    public TokenPair generateTokenPair(User user) {
        // Access Token: 短期有效（15分钟）
        String accessToken = jwtUtils.generateToken(user, 15 * 60 * 1000);
        
        // Refresh Token: 长期有效（7天），存储在 Redis
        String refreshToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
            "refresh:" + refreshToken, 
            String.valueOf(user.getId()),
            7, TimeUnit.DAYS);
        
        return new TokenPair(accessToken, refreshToken);
    }
    
    public TokenPair refreshToken(String refreshToken) {
        String userId = redisTemplate.opsForValue().get("refresh:" + refreshToken);
        if (userId == null) {
            throw new InvalidTokenException("Refresh Token 无效或已过期");
        }
        
        // 删除旧的 Refresh Token
        redisTemplate.delete("refresh:" + refreshToken);
        
        // 生成新的 Token 对
        User user = userService.getById(Long.parseLong(userId));
        return generateTokenPair(user);
    }
}
```

---

## 面试要点

### 核心答案

**问：Cookie、Session、Token 之间有什么区别？**

答：

| 维度 | Cookie | Session | Token (JWT) |
|------|--------|---------|-------------|
| **存储位置** | 客户端浏览器 | 服务端内存/Redis | 客户端（LocalStorage/Cookie） |
| **安全性** | 低（可被篡改） | 高（服务端存储） | 中（签名防篡改） |
| **服务端压力** | 无 | 有（存储会话） | 无（无状态） |
| **分布式** | 天然支持 | 需要共享存储 | 天然支持 |
| **跨域** | 受同源策略限制 | 依赖 Cookie | 支持（Header 传输） |

**适用场景**：
- **Cookie**：简单状态标记、用户偏好
- **Session**：传统 Web 应用、单体架构
- **Token**：分布式系统、前后端分离、移动端

---

## 编码最佳实践

### ✅ 推荐做法

```java
// 1. Cookie 安全设置
cookie.setHttpOnly(true);   // 防 XSS
cookie.setSecure(true);     // HTTPS Only
cookie.setSameSite("Strict"); // 防 CSRF

// 2. Session 使用 Redis 共享
@EnableRedisHttpSession

// 3. JWT 使用 Refresh Token 机制
// Access Token 短期 + Refresh Token 长期

// 4. Token 存储推荐
// - Web: HttpOnly Cookie（更安全）
// - 移动端: 安全存储
```

### ❌ 避免做法

```java
// ❌ Cookie 存储敏感信息
Cookie cookie = new Cookie("password", "123456");

// ❌ JWT 存储敏感信息
.claim("password", user.getPassword())

// ❌ 不设置过期时间
session.setMaxInactiveInterval(-1);  // 永不过期

// ❌ Token 不做刷新机制
// Access Token 过期就要重新登录
```
