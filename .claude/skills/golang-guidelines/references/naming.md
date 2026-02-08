# 命名规范

> Go 语言编码规范 - 命名约定
> 参考：Effective Go、Uber Go Style Guide

---

## 基本规则（强制）

### 命名风格

| 类型 | 风格 | 示例 | 说明 |
|------|------|------|------|
| 变量 | `camelCase` | `userName`, `orderID` | 小写开头驼峰 |
| 函数/方法 | `CamelCase` / `camelCase` | `GetUser()` / `getUser()` | 导出大写，私有小写 |
| 常量 | `CamelCase` / `camelCase` | `MaxRetryCount` / `maxRetryCount` | 同函数规则 |
| 结构体 | `CamelCase` | `UserInfo`, `OrderDetail` | 大写开头 |
| 接口 | `CamelCase` + er | `Reader`, `Writer`, `Stringer` | 单方法接口用 er 后缀 |
| 包名 | `lowercase` | `http`, `json`, `userservice` | 全小写，无下划线 |

### 导出规则

**【强制】Go 使用首字母大小写控制访问权限：大写导出（public），小写私有（private）。**

```go
// ✅ 正例
package user

type User struct {           // 导出：其他包可访问
    ID   int64               // 导出字段
    Name string              // 导出字段
    age  int                 // 私有字段：仅包内可访问
}

func NewUser() *User { }     // 导出函数
func (u *User) GetAge() int  // 导出方法
func (u *User) validate() {} // 私有方法
```

---

## 变量命名

### 局部变量

**【强制】局部变量使用短小有意义的名称，作用域越小名称可以越短。**

```go
// ✅ 正例 - 短作用域使用短名称
for i := 0; i < len(users); i++ {
    u := users[i]
    // ...
}

// ✅ 正例 - 常见缩写
func (s *Server) ServeHTTP(w http.ResponseWriter, r *http.Request) {
    ctx := r.Context()
    // ...
}

// ❌ 反例 - 不必要的长名称
for index := 0; index < len(users); index++ {
    currentUser := users[index]
    // ...
}
```

### 常用缩写约定

| 缩写 | 含义 | 使用场景 |
|------|------|---------|
| `i`, `j`, `k` | 索引 | 循环变量 |
| `n` | 数量 | 计数器 |
| `v` | 值 | range value |
| `k` | 键 | map key |
| `s` | 字符串 | 字符串变量 |
| `b` | 字节/布尔 | []byte / bool |
| `r` | reader/request | io.Reader / http.Request |
| `w` | writer | io.Writer / http.ResponseWriter |
| `ctx` | context | context.Context |
| `err` | error | 错误变量 |

### 包级变量

**【推荐】包级变量使用描述性名称，避免缩写。**

```go
// ✅ 正例
var (
    DefaultTimeout = 30 * time.Second
    MaxRetryCount  = 3
)

// ❌ 反例
var (
    DefTO   = 30 * time.Second
    MaxRetry = 3
)
```

---

## 函数/方法命名

### 基本规则

**【强制】函数名应该是动词或动词短语，清晰表达其行为。**

```go
// ✅ 正例
func GetUser(id int64) (*User, error)
func CreateOrder(req *CreateOrderReq) (*Order, error)
func ValidateToken(token string) error
func IsAdmin(userID int64) bool

// ❌ 反例
func User(id int64) (*User, error)        // 名词，不清晰
func DoOrder(req *CreateOrderReq)          // Do 太泛
func Check(token string) error             // Check 什么？
```

### Getter/Setter

**【强制】Go 中 Getter 不使用 Get 前缀，Setter 使用 Set 前缀。**

```go
// ✅ 正例
type User struct {
    name string
}

func (u *User) Name() string {            // Getter：不加 Get
    return u.name
}

func (u *User) SetName(name string) {     // Setter：加 Set
    u.name = name
}

// ❌ 反例
func (u *User) GetName() string {         // 不要 Get 前缀
    return u.name
}
```

### 返回布尔的函数

**【推荐】返回布尔值的函数使用 Is/Has/Can/Should 等前缀。**

```go
// ✅ 正例
func IsValid(s string) bool
func HasPermission(userID int64, perm string) bool
func CanExecute(cmd string) bool
func ShouldRetry(err error) bool

// 或使用形容词
func Valid(s string) bool
func Empty(s string) bool
```

---

## 结构体命名

### 基本规则

**【强制】结构体名使用名词，表示其代表的实体。**

```go
// ✅ 正例
type User struct { }
type OrderDetail struct { }
type HTTPClient struct { }              // 缩写全大写

// ❌ 反例
type CreateUser struct { }              // 动词开头
type UserData struct { }                // 冗余后缀
type Userinfo struct { }                // 应该是 UserInfo
```

### 缩写处理

**【强制】缩写词保持全大写或全小写，不要混合。**

```go
// ✅ 正例
type HTTPClient struct { }              // HTTP 全大写
type URLParser struct { }               // URL 全大写
type xmlParser struct { }               // xml 全小写（私有）
var userID int64                        // ID 全大写

// ❌ 反例
type HttpClient struct { }              // 应该是 HTTP
type UrlParser struct { }               // 应该是 URL
var oderId string                       // 应该是 orderID
```

---

## 接口命名

### 单方法接口

**【强制】单方法接口使用方法名 + er 后缀。**

```go
// ✅ 正例 - 标准库风格
type Reader interface {
    Read(p []byte) (n int, err error)
}

type Writer interface {
    Write(p []byte) (n int, err error)
}

type Stringer interface {
    String() string
}

type Closer interface {
    Close() error
}

// ✅ 正例 - 业务接口
type UserGetter interface {
    GetUser(id int64) (*User, error)
}

type OrderCreator interface {
    CreateOrder(req *CreateOrderReq) (*Order, error)
}
```

### 多方法接口

**【推荐】多方法接口使用名词，描述其能力或角色。**

```go
// ✅ 正例
type UserService interface {
    GetUser(id int64) (*User, error)
    CreateUser(req *CreateUserReq) (*User, error)
    UpdateUser(req *UpdateUserReq) error
}

type Repository interface {
    Find(id int64) (*Entity, error)
    Save(entity *Entity) error
    Delete(id int64) error
}
```

---

## 包命名

### 基本规则

| 规则 | 示例 | 说明 |
|------|------|------|
| 全小写 | `userservice` | 不用下划线或驼峰 |
| 单数形式 | `user` 而非 `users` | 包名用单数 |
| 简短有意义 | `http`, `json` | 避免冗长 |
| 避免通用名 | ❌ `util`, `common` | 太泛，不推荐 |

```go
// ✅ 正例
package user
package order
package payment
package httputil                        // 特定工具包

// ❌ 反例
package userService                     // 驼峰
package user_service                    // 下划线
package users                           // 复数
package util                            // 太泛
package common                          // 太泛
```

### 包名与目录名

**【强制】包名应与目录名一致。**

```
project/
├── user/
│   └── user.go                         // package user
├── order/
│   └── order.go                        // package order
└── internal/
    └── cache/
        └── cache.go                    // package cache
```

---

## 常量命名

### 导出常量

**【强制】导出常量使用驼峰命名，不使用全大写下划线。**

```go
// ✅ 正例 - Go 风格
const (
    MaxRetryCount = 3
    DefaultTimeout = 30 * time.Second
    StatusPending = "pending"
    StatusCompleted = "completed"
)

// ❌ 反例 - 其他语言风格
const (
    MAX_RETRY_COUNT = 3                 // 不要下划线
    DEFAULT_TIMEOUT = 30 * time.Second
)
```

### 私有常量

```go
// ✅ 正例
const (
    maxRetryCount = 3                   // 私有常量小写开头
    defaultTimeout = 30 * time.Second
)
```

### 枚举常量

**【推荐】使用 iota 定义枚举，类型前缀 + 值名称。**

```go
// ✅ 正例
type OrderStatus int

const (
    OrderStatusPending OrderStatus = iota
    OrderStatusPaid
    OrderStatusShipped
    OrderStatusCompleted
    OrderStatusCancelled
)

// 可以实现 String() 方法
func (s OrderStatus) String() string {
    switch s {
    case OrderStatusPending:
        return "pending"
    case OrderStatusPaid:
        return "paid"
    // ...
    }
    return "unknown"
}
```

---

## 命名禁则速查

| ❌ 禁止 | ✅ 正确 | 原因 |
|--------|--------|------|
| `GetName()` | `Name()` | Go Getter 不加 Get |
| `MAX_COUNT` | `MaxCount` | Go 不用下划线常量 |
| `HttpClient` | `HTTPClient` | 缩写全大写 |
| `package userService` | `package user` | 包名全小写 |
| `package util` | 特定功能包名 | 避免通用名 |
| `var i int` (包级) | `var index int` | 包级变量用描述性名称 |
| `type CreateUser struct` | `type User struct` | 结构体用名词 |
