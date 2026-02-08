# 安全规范

> Java/Spring Boot 编码规范 - 安全

---

## SQL 注入防护

### 使用预编译语句

```java
// ✅ 正确：使用 #{} 预编译
@Select("SELECT * FROM user WHERE id = #{id}")
User selectById(Long id);

@Select("SELECT * FROM product WHERE name LIKE CONCAT('%', #{name}, '%')")
List<Product> searchByName(String name);

// ❌ 错误：使用 ${} 直接拼接，SQL 注入风险
@Select("SELECT * FROM user WHERE id = ${id}")
User selectByIdUnsafe(Long id);

// ❌ 错误：字符串拼接
String sql = "SELECT * FROM user WHERE name = '" + name + "'";
```

### 动态排序使用白名单

```xml
<!-- ✅ 正确：使用 choose 白名单 -->
ORDER BY
<choose>
    <when test="orderColumn == 'create_time'">create_time</when>
    <when test="orderColumn == 'update_time'">update_time</when>
    <when test="orderColumn == 'price'">price</when>
    <otherwise>id</otherwise>
</choose>
<choose>
    <when test="orderDirection == 'asc'">ASC</when>
    <otherwise>DESC</otherwise>
</choose>
```

---

## XSS 防护

### 全局 XSS 过滤

```java
/**
 * XSS 请求包装器
 */
public class XssRequestWrapper extends HttpServletRequestWrapper {

    public XssRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        return cleanXss(value);
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) {
            return null;
        }
        String[] cleanValues = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            cleanValues[i] = cleanXss(values[i]);
        }
        return cleanValues;
    }

    private String cleanXss(String value) {
        if (value == null) {
            return null;
        }
        // 使用 Jsoup 清理 HTML
        return Jsoup.clean(value, Whitelist.none());
    }
}

/**
 * XSS 过滤器
 */
@WebFilter(urlPatterns = "/*")
public class XssFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        XssRequestWrapper wrappedRequest = new XssRequestWrapper((HttpServletRequest) request);
        chain.doFilter(wrappedRequest, response);
    }
}
```

### 富文本白名单过滤

```java
/**
 * 富文本内容清理（允许部分标签）
 */
public class RichTextSanitizer {

    private static final Whitelist RICH_TEXT_WHITELIST = Whitelist.relaxed()
        .addTags("h1", "h2", "h3", "h4", "h5", "h6")
        .addAttributes("a", "href", "title")
        .addAttributes("img", "src", "alt", "title")
        .addProtocols("a", "href", "http", "https");

    public static String sanitize(String html) {
        if (StringUtils.isBlank(html)) {
            return "";
        }
        return Jsoup.clean(html, RICH_TEXT_WHITELIST);
    }
}
```

---

## 敏感数据保护

### 配置安全

```yaml
# ❌ 错误：敏感配置写在配置文件中
spring:
  datasource:
    password: MyPassword123  # 禁止

# ✅ 正确：使用环境变量或配置中心
spring:
  datasource:
    password: ${DB_PASSWORD}  # 从环境变量读取

# ✅ 正确：Nacos 配置中心
# 在 Nacos 中配置，按环境隔离
```

### 日志脱敏

```java
/**
 * 脱敏工具类
 */
public class DesensitizedUtil {

    /**
     * 手机号脱敏：138****5678
     */
    public static String mobilePhone(String str) {
        if (StringUtils.isBlank(str)) {
            return "";
        }
        return str.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
    }

    /**
     * 身份证脱敏：110101********1234
     */
    public static String idCardNum(String str) {
        if (StringUtils.isBlank(str)) {
            return "";
        }
        return str.replaceAll("(\\d{6})\\d{8}(\\d{4})", "$1********$2");
    }

    /**
     * 银行卡脱敏：6222****5678
     */
    public static String bankCard(String str) {
        if (StringUtils.isBlank(str)) {
            return "";
        }
        return str.replaceAll("(\\d{4})\\d+(\\d{4})", "$1****$2");
    }

    /**
     * 密码：全部隐藏
     */
    public static String password(String str) {
        if (StringUtils.isBlank(str)) {
            return "";
        }
        return "******";
    }
}

// 使用
log.info("[用户登录]，手机号: {}，姓名: {}",
    DesensitizedUtil.mobilePhone(user.getMobile()),
    DesensitizedUtil.chineseName(user.getName()));
```

### 密码加密存储

```java
/**
 * 密码加密工具
 */
@Component
public class PasswordEncoder {

    private static final int ITERATIONS = 10000;
    private static final int KEY_LENGTH = 256;

    /**
     * 加密密码
     */
    public String encode(String rawPassword) {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);

        PBEKeySpec spec = new PBEKeySpec(
            rawPassword.toCharArray(),
            salt,
            ITERATIONS,
            KEY_LENGTH
        );

        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return ITERATIONS + ":" + Base64.getEncoder().encodeToString(salt) +
                   ":" + Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("密码加密失败", e);
        }
    }

    /**
     * 校验密码
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        String[] parts = encodedPassword.split(":");
        int iterations = Integer.parseInt(parts[0]);
        byte[] salt = Base64.getDecoder().decode(parts[1]);
        byte[] hash = Base64.getDecoder().decode(parts[2]);

        PBEKeySpec spec = new PBEKeySpec(
            rawPassword.toCharArray(),
            salt,
            iterations,
            hash.length * 8
        );

        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] testHash = skf.generateSecret(spec).getEncoded();
            return Arrays.equals(hash, testHash);
        } catch (Exception e) {
            return false;
        }
    }
}
```

---

## 接口安全

### 接口限流

```java
/**
 * 接口限流注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 限流 key
     */
    String key() default "";

    /**
     * 时间窗口（秒）
     */
    int time() default 60;

    /**
     * 最大请求数
     */
    int count() default 100;
}

/**
 * 限流切面
 */
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final StringRedisTemplate redisTemplate;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        String key = "rate_limit:" + rateLimit.key();
        long time = rateLimit.time();
        long count = rateLimit.count();

        // 获取当前请求数
        String countStr = redisTemplate.opsForValue().get(key);
        long current = countStr == null ? 0 : Long.parseLong(countStr);

        if (current >= count) {
            throw exception(RATE_LIMIT_EXCEEDED);
        }

        // 递增计数
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, time, TimeUnit.SECONDS);

        return pjp.proceed();
    }
}

// 使用
@PostMapping("/login")
@RateLimit(key = "login", time = 60, count = 5)
public CommonResult<String> login(@RequestBody LoginReq req) {
    // ...
}
```

### 接口防刷

```java
/**
 * 基于 IP 的防刷
 */
@Component
public class AntiBrushService {

    private final StringRedisTemplate redisTemplate;

    public boolean checkAllowed(String key, int maxCount, int seconds) {
        String redisKey = "anti_brush:" + key;

        Long count = redisTemplate.opsForValue().increment(redisKey);
        if (count == null || count == 1) {
            redisTemplate.expire(redisKey, seconds, TimeUnit.SECONDS);
        }

        if (count > maxCount) {
            return false;
        }
        return true;
    }
}

// 使用
@PostMapping("/sms/send")
public CommonResult<Void> sendSms(@RequestBody SmsReq req, HttpServletRequest request) {
    String ip = getIp(request);
    String mobile = req.getMobile();

    // IP 限制：每分钟最多 10 次
    if (!antiBrushService.checkAllowed("ip:" + ip, 10, 60)) {
        throw exception(SMS_SEND_TOO_FREQUENT);
    }

    // 手机号限制：每天最多 5 次
    if (!antiBrushService.checkAllowed("mobile:" + mobile, 5, 86400)) {
        throw exception(SMS_SEND_LIMIT_EXCEEDED);
    }

    // 发送短信
    // ...

    return CommonResult.success();
}
```

---

## 安全规范速查表

| 安全点 | 规范 |
|--------|------|
| **SQL 注入** | 使用 `#{}` 预编译，禁止 `${}` 拼接 |
| **XSS 防护** | 全局过滤器 + 富文本白名单 |
| **配置安全** | 敏感配置放配置中心，禁止写代码 |
| **日志脱敏** | 手机号、身份证、密码必须脱敏 |
| **密码存储** | 使用 PBKDF2/BCrypt 加密 |
| **接口限流** | 添加 @RateLimit 注解 |
| **接口防刷** | IP + 业务规则双重限制 |
