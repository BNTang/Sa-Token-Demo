# Spring MVC 具体的工作原理？

## 核心组件

```
┌─────────────────────────────────────────────────────────────┐
│                    Spring MVC 核心组件                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. DispatcherServlet - 前端控制器 (核心)                  │
│      └── 接收所有请求，分发给各组件处理                     │
│                                                             │
│   2. HandlerMapping - 处理器映射器                          │
│      └── 根据 URL 找到对应的 Handler (Controller)           │
│                                                             │
│   3. HandlerAdapter - 处理器适配器                          │
│      └── 适配不同类型的 Handler，执行 Handler               │
│                                                             │
│   4. Handler (Controller) - 处理器                          │
│      └── 业务逻辑处理                                       │
│                                                             │
│   5. ViewResolver - 视图解析器                              │
│      └── 将逻辑视图名解析为具体 View                        │
│                                                             │
│   6. View - 视图                                            │
│      └── 渲染数据，生成响应                                 │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 请求处理流程

```
┌─────────────────────────────────────────────────────────────┐
│                    Spring MVC 请求流程                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   客户端请求                                                 │
│       │                                                     │
│       ▼                                                     │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  1. DispatcherServlet 接收请求                       │  │
│   └─────────────────────────────────────────────────────┘  │
│       │                                                     │
│       ▼                                                     │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  2. HandlerMapping 查找 Handler                      │  │
│   │     - 根据 URL 匹配 @RequestMapping                  │  │
│   │     - 返回 HandlerExecutionChain (Handler + 拦截器)  │  │
│   └─────────────────────────────────────────────────────┘  │
│       │                                                     │
│       ▼                                                     │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  3. HandlerAdapter 执行 Handler                      │  │
│   │     - 参数解析 (@RequestParam, @RequestBody 等)      │  │
│   │     - 调用 Controller 方法                           │  │
│   │     - 返回值处理 (@ResponseBody 等)                  │  │
│   └─────────────────────────────────────────────────────┘  │
│       │                                                     │
│       ▼                                                     │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  4. 返回 ModelAndView                                │  │
│   │     - 包含模型数据和视图名                           │  │
│   │     - @ResponseBody 直接返回数据，跳过视图解析       │  │
│   └─────────────────────────────────────────────────────┘  │
│       │                                                     │
│       ▼                                                     │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  5. ViewResolver 解析视图                            │  │
│   │     - 将视图名解析为具体 View 对象                   │  │
│   │     - 如 "user/list" → /WEB-INF/views/user/list.jsp  │  │
│   └─────────────────────────────────────────────────────┘  │
│       │                                                     │
│       ▼                                                     │
│   ┌─────────────────────────────────────────────────────┐  │
│   │  6. View 渲染                                        │  │
│   │     - 将模型数据填充到视图                           │  │
│   │     - 生成 HTML 响应                                 │  │
│   └─────────────────────────────────────────────────────┘  │
│       │                                                     │
│       ▼                                                     │
│   响应客户端                                                 │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 流程图解

```
┌─────────────────────────────────────────────────────────────┐
│                    完整流程图                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────┐         ┌───────────────────┐                │
│  │  Client  │ ──1──→  │ DispatcherServlet │                │
│  └──────────┘         └─────────┬─────────┘                │
│       ↑                         │                          │
│       │                         │ 2                        │
│       │                         ▼                          │
│       │               ┌─────────────────────┐              │
│       │               │   HandlerMapping    │              │
│       │               └─────────┬───────────┘              │
│       │                         │ 3                        │
│       │                         ▼                          │
│       │               ┌─────────────────────┐              │
│       │               │   HandlerAdapter    │              │
│       │               └─────────┬───────────┘              │
│       │                         │ 4                        │
│       │                         ▼                          │
│       │               ┌─────────────────────┐              │
│       │               │   Controller        │              │
│       │               └─────────┬───────────┘              │
│       │                         │ 5 (ModelAndView)         │
│       │                         ▼                          │
│       │               ┌─────────────────────┐              │
│       │               │   ViewResolver      │              │
│       │               └─────────┬───────────┘              │
│       │                         │ 6                        │
│       │                         ▼                          │
│       │               ┌─────────────────────┐              │
│       └───────7───────│       View          │              │
│                       └─────────────────────┘              │
│                                                             │
│   1. 请求  2. 查找Handler  3. 执行Handler                   │
│   4. 调用Controller  5. 返回ModelAndView                    │
│   6. 解析视图  7. 渲染响应                                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 代码示例

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    /**
     * 处理流程:
     * 1. DispatcherServlet 接收 GET /api/users/1
     * 2. HandlerMapping 找到此方法
     * 3. HandlerAdapter 解析 @PathVariable
     * 4. 执行方法，返回 User 对象
     * 5. @RestController = @ResponseBody，直接序列化为 JSON
     * 6. 跳过视图解析，直接响应
     */
    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.findById(id);
    }
    
    /**
     * 参数解析示例
     */
    @PostMapping
    public Result createUser(
            @RequestBody UserDTO dto,           // 请求体 JSON
            @RequestHeader("Token") String token, // 请求头
            @RequestParam(defaultValue = "1") int page, // 查询参数
            HttpServletRequest request) {       // 原生请求对象
        return userService.create(dto);
    }
}
```

```java
/**
 * 返回视图的传统方式
 */
@Controller
@RequestMapping("/users")
public class UserViewController {
    
    @GetMapping("/list")
    public String listUsers(Model model) {
        List<User> users = userService.findAll();
        model.addAttribute("users", users);
        
        // 返回视图名，ViewResolver 解析为
        // /WEB-INF/views/user/list.html
        return "user/list";
    }
}
```

## 拦截器

```java
/**
 * 拦截器 - 在 Handler 执行前后插入逻辑
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {
    
    // Handler 执行前
    @Override
    public boolean preHandle(HttpServletRequest request, 
            HttpServletResponse response, Object handler) {
        String token = request.getHeader("Authorization");
        if (!validateToken(token)) {
            response.setStatus(401);
            return false;  // 中断请求
        }
        return true;  // 继续执行
    }
    
    // Handler 执行后，视图渲染前
    @Override
    public void postHandle(HttpServletRequest request, 
            HttpServletResponse response, Object handler,
            ModelAndView modelAndView) {
        // 可以修改 ModelAndView
    }
    
    // 请求完成后 (视图渲染后)
    @Override
    public void afterCompletion(HttpServletRequest request, 
            HttpServletResponse response, Object handler, Exception ex) {
        // 清理资源
    }
}
```

## @RestController vs @Controller

```
┌─────────────────────────────────────────────────────────────┐
│                    注解对比                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   @Controller                                               │
│   ├── 返回视图名，需要 ViewResolver 解析                    │
│   └── 配合 @ResponseBody 返回 JSON                          │
│                                                             │
│   @RestController = @Controller + @ResponseBody             │
│   ├── 直接返回数据 (JSON/XML)                               │
│   ├── 跳过视图解析                                          │
│   └── 前后端分离项目推荐使用                                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> Spring MVC 流程：1）**DispatcherServlet** 接收请求；2）**HandlerMapping** 根据 URL 找到 Controller；3）**HandlerAdapter** 执行 Controller 方法（参数解析、返回值处理）；4）返回 **ModelAndView**；5）**ViewResolver** 解析视图；6）**View** 渲染响应。@RestController 直接返回 JSON，跳过视图解析。

### 1分钟版本

> **核心组件**：
> - **DispatcherServlet**：前端控制器，接收所有请求
> - **HandlerMapping**：根据 URL 找 Controller
> - **HandlerAdapter**：执行 Controller，处理参数和返回值
> - **ViewResolver**：解析视图名为 View 对象
> - **View**：渲染页面
>
> **请求流程**：
> 1. 请求 → DispatcherServlet
> 2. HandlerMapping 找到 Handler (Controller + 拦截器)
> 3. HandlerAdapter 执行 Handler
>    - 解析 @RequestParam、@RequestBody 等
>    - 调用 Controller 方法
> 4. 返回 ModelAndView
> 5. ViewResolver 解析视图
> 6. View 渲染，响应客户端
>
> **@RestController**：
> - = @Controller + @ResponseBody
> - 直接返回 JSON，跳过视图解析
> - 前后端分离标配

---

*关联文档：[spring-ioc.md](spring-ioc.md) | [spring-aop.md](spring-aop.md)*
