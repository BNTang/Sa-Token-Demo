# Controller 层规范

> Java/Spring Boot 编码规范 - Controller 层

---

## 职责边界

**Controller 只负责三件事：**

1. 接收参数
2. 调用 Service
3. 返回结果

❌ **禁止在 Controller 写业务逻辑**（查询数据库、条件判断、数据处理等）

```java
// ❌ 错误：Controller 包含业务逻辑
@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductMapper productMapper;  // ❌ 不应直接注入 Mapper

    @PostMapping("/update")
    public CommonResult<Void> update(@RequestBody ProductUpdateReq req) {
        // ❌ 业务逻辑在 Controller
        Product product = productMapper.selectById(req.getId());
        if (product == null) {
            throw exception(PRODUCT_NOT_EXISTS);
        }
        if (product.getStatus() == 0) {
            throw exception(PRODUCT_STATUS_ERROR);
        }
        product.setName(req.getName());
        productMapper.updateById(product);
        return CommonResult.success();
    }
}

// ✅ 正确：只做参数接收和 Service 调用
@RestController
@RequestMapping("/product")
public class ProductController {

    private final IProductService productService;

    @PostMapping("/update")
    public CommonResult<Void> update(@Valid @RequestBody ProductUpdateReq req) {
        productService.updateProduct(req);
        return CommonResult.success();
    }
}
```

---

## RESTful 接口规范

### HTTP 方法约定

**【推荐】使用标准的 RESTful 风格设计接口。**

| 场景 | 方法 | 路径示例 | 说明 |
|------|------|---------|------|
| 查询单个资源 | **GET** | `/products/{id}` | 幂等操作 |
| 查询列表（简单） | **GET** | `/products?name=xxx&status=1` | URL 查询参数 |
| 查询列表（复杂） | **POST** | `/products/query` | 复杂条件用 POST |
| 分页查询 | **POST** | `/products/page` | 避免 URL 过长 |
| 新增资源 | **POST** | `/products` | 非幂等操作 |
| 完整更新资源 | **PUT** | `/products/{id}` | 幂等操作，更新全部字段 |
| 部分更新资源 | **PATCH** | `/products/{id}` | 幂等操作，更新部分字段 |
| 删除资源 | **DELETE** | `/products/{id}` | 幂等操作 |

**RESTful 设计原则：**

1. **使用名词复数形式**：`/products` 而不是 `/product`
2. **使用资源路径而非动词**：`/products/{id}` 而不是 `/product/get`
3. **HTTP 方法表示操作**：用 GET/POST/PUT/DELETE 表示 CRUD
4. **幂等性**：GET/PUT/DELETE 是幂等的，POST 不是
5. **状态码语义化**：200 成功、201 创建、204 无内容、400 参数错误、404 未找到

```java
@RestController
@RequestMapping("/products")  // 使用复数形式
public class ProductController {

    private final IProductService productService;

    // GET - 查询单个资源
    @GetMapping("/{id}")
    public CommonResult<ProductDetailRsp> getById(@PathVariable Long id) {
        return CommonResult.success(productService.getDetail(id));
    }

    // GET - 简单列表查询（URL 参数）
    @GetMapping
    public CommonResult<List<ProductDTO>> list(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) Integer status
    ) {
        return CommonResult.success(productService.listProducts(name, status));
    }

    // POST - 复杂条件查询
    @PostMapping("/query")
    public CommonResult<List<ProductDTO>> query(@Valid @RequestBody ProductQueryReq req) {
        return CommonResult.success(productService.listProducts(req));
    }

    // POST - 分页查询
    @PostMapping("/page")
    public CommonResult<IPage<ProductDTO>> page(@Valid @RequestBody ProductPageReq req) {
        return CommonResult.success(productService.getPage(req));
    }

    // POST - 新增资源
    @PostMapping
    public CommonResult<Long> create(@Valid @RequestBody ProductAddReq req) {
        Long productId = productService.addProduct(req);
        return CommonResult.success(productId);
    }

    // PUT - 完整更新资源
    @PutMapping("/{id}")
    public CommonResult<Void> update(
        @PathVariable Long id,
        @Valid @RequestBody ProductUpdateReq req
    ) {
        productService.updateProduct(id, req);
        return CommonResult.success();
    }

    // PATCH - 部分更新资源（如修改状态）
    @PatchMapping("/{id}/status")
    public CommonResult<Void> updateStatus(
        @PathVariable Long id,
        @RequestParam Integer status
    ) {
        productService.changeStatus(id, status);
        return CommonResult.success();
    }

    // DELETE - 删除资源
    @DeleteMapping("/{id}")
    public CommonResult<Void> delete(@PathVariable Long id) {
        productService.deleteProduct(id);
        return CommonResult.success();
    }
}
```

---

## 参数校验

### 基本校验

```java
// 1. Controller 类添加 @Validated
@Validated
@RestController
@RequestMapping("/product")
public class ProductController {

    // 2. 方法参数使用 @Valid @RequestBody
    @PostMapping("/add")
    public CommonResult<Long> add(@Valid @RequestBody ProductAddReq req) {
        return CommonResult.success(productService.addProduct(req));
    }
}
```

### 请求对象校验注解

```java
@Data
public class ProductAddReq {

    @NotBlank(message = "商品编码不能为空")
    private String itemCode;

    @NotBlank(message = "商品名称不能为空")
    @Length(max = 100, message = "商品名称最长100字符")
    private String name;

    @NotNull(message = "商品价格不能为空")
    @DecimalMin(value = "0.01", message = "商品价格必须大于0")
    private BigDecimal price;

    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能为负数")
    private Integer stock;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String contactPhone;

    @Email(message = "邮箱格式不正确")
    private String email;
}
```

### 嵌套对象校验

嵌套对象需要加 `@Valid` 才能触发内部校验：

```java
@Data
public class OrderCreateReq {

    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @NotNull(message = "用户信息不能为空")
    @Valid  // 必须加 @Valid
    private UserInfo userInfo;

    @NotEmpty(message = "订单项不能为空")
    @Valid  // 必须加 @Valid
    private List<OrderItemReq> items;
}

@Data
public class UserInfo {

    @NotBlank(message = "用户姓名不能为空")
    private String name;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String mobile;
}
```

### 路径变量校验

```java
// RESTful 风格：资源ID作为路径变量
@GetMapping("/{id}")
public CommonResult<ProductDetailRsp> getById(
    @PathVariable("id")
    @Min(value = 1, message = "ID 必须大于0")
    Long id
) {
    return CommonResult.success(productService.getDetail(id));
}

// 部分更新：状态修改
@PatchMapping("/{id}/status")
public CommonResult<Void> updateStatus(
    @PathVariable("id")
    @Min(value = 1, message = "ID 必须大于0")
    Long id,
    @RequestParam
    @Min(value = 0, message = "状态值不合法")
    @Max(value = 10, message = "状态值不合法")
    Integer status
) {
    productService.changeStatus(id, status);
    return CommonResult.success();
}
```

### 查询参数校验

```java
@GetMapping("/search")
public CommonResult<List<ProductDTO>> search(
    @RequestParam(required = false)
    @Length(max = 50, message = "关键词最长50字符")
    String keyword,

    @RequestParam(required = false)
    @Min(value = 1, message = "页码必须大于0")
    @Max(value = 1000, message = "页码不能超过1000")
    Integer pageNum
) {
    return CommonResult.success(productService.search(keyword, pageNum));
}
```

---

## 返回类型规范

### 统一返回结构

使用 `CommonResult<T>` 统一返回格式：

```java
public class CommonResult<T> {
    private Integer code;
    private String message;
    private T data;

    public static <T> CommonResult<T> success() {
        return new CommonResult<>(0, "成功", null);
    }

    public static <T> CommonResult<T> success(T data) {
        return new CommonResult<>(0, "成功", data);
    }

    public static <T> CommonResult<T> error(Integer code, String message) {
        return new CommonResult<>(code, message, null);
    }
}
```

### 常见返回类型

```java
// 1. GET - 返回单个对象
public CommonResult<ProductDetailRsp> getById(@PathVariable Long id)

// 2. GET - 返回列表
public CommonResult<List<ProductDTO>> list(@RequestParam String name)

// 3. POST - 返回分页
public CommonResult<IPage<ProductPageDTO>> page(@Valid @RequestBody ProductPageReq req)

// 4. POST - 返回新建资源 ID
public CommonResult<Long> create(@Valid @RequestBody ProductAddReq req)

// 5. PUT/PATCH/DELETE - 无返回数据
public CommonResult<Void> update(@PathVariable Long id, @Valid @RequestBody ProductUpdateReq req)
```

---

## 接口文档注解

### Javadoc 规范

```java
/**
 * 商品管理 Controller
 */
@RestController
@RequestMapping("/product")
@Validated
public class ProductController {

    private final IProductService productService;

    /**
     * 分页查询商品
     *
     * @param req 查询条件
     * @return 商品分页列表
     */
    @PostMapping("/page")
    public CommonResult<IPage<ProductDTO>> getPage(@Valid @RequestBody ProductPageReq req) {
        return CommonResult.success(productService.getPage(req));
    }

    /**
     * 新增商品
     *
     * @param req 商品信息
     * @return 商品ID
     */
    @PostMapping("/add")
    public CommonResult<Long> add(@Valid @RequestBody ProductAddReq req) {
        return CommonResult.success(productService.addProduct(req));
    }
}
```

### 字段注释规范

```java
@Data
public class ProductPageReq {

    /**
     * 商品名称（模糊匹配）
     * @mock 阿莫西林
     */
    private String name;

    /**
     * 状态：0-下架，1-上架
     * @mock 1
     */
    private Integer status;

    /**
     * 页码
     * @mock 1
     */
    @Min(value = 1, message = "页码必须大于0")
    private Integer pageNum = 1;

    /**
     * 每页数量
     * @mock 10
     */
    @Min(value = 1, message = "每页数量必须大于0")
    @Max(value = 100, message = "每页数量不能超过100")
    private Integer pageSize = 10;
}
```

### 特殊标签

| 标签 | 用途 |
|------|------|
| `@mock` | 字段示例值 |
| `@ignore` | 忽略该字段，不生成文档 |

```java
/**
 * 创建时间（内部字段）
 * @ignore
 */
private LocalDateTime createTime;
```

---

## RESTful Controller 完整示例

```java
/**
 * 商品管理 Controller
 * 
 * RESTful 接口设计：
 * - GET    /products          查询列表
 * - GET    /products/{id}     查询详情
 * - POST   /products          新增商品
 * - POST   /products/query    复杂查询
 * - POST   /products/page     分页查询
 * - PUT    /products/{id}     完整更新
 * - PATCH  /products/{id}     部分更新
 * - DELETE /products/{id}     删除商品
 */
@Slf4j
@RestController
@RequestMapping("/products")  // 使用复数形式
@RequiredArgsConstructor
@Validated
public class ProductController {

    private final IProductService productService;

    /**
     * 查询商品列表（简单查询）
     */
    @GetMapping
    public CommonResult<List<ProductDTO>> list(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) Integer status
    ) {
        log.info("[商品查询]，列表查询，名称: {}，状态: {}", name, status);
        List<ProductDTO> list = productService.listProducts(name, status);
        return CommonResult.success(list);
    }

    /**
     * 查询商品详情
     */
    @GetMapping("/{id}")
    public CommonResult<ProductDetailRsp> getById(@PathVariable Long id) {
        log.info("[商品查询]，查询详情，商品ID: {}", id);
        ProductDetailRsp detail = productService.getDetail(id);
        return CommonResult.success(detail);
    }

    /**
     * 复杂条件查询
     */
    @PostMapping("/query")
    public CommonResult<List<ProductDTO>> query(@Valid @RequestBody ProductQueryReq req) {
        log.info("[商品查询]，复杂查询，条件: {}", req);
        List<ProductDTO> list = productService.listProducts(req);
        return CommonResult.success(list);
    }

    /**
     * 分页查询商品
     */
    @PostMapping("/page")
    public CommonResult<IPage<ProductDTO>> page(@Valid @RequestBody ProductPageReq req) {
        log.info("[商品查询]，分页查询，条件: {}", req);
        IPage<ProductDTO> page = productService.getPage(req);
        return CommonResult.success(page);
    }

    /**
     * 新增商品
     */
    @PostMapping
    public CommonResult<Long> create(@Valid @RequestBody ProductAddReq req) {
        log.info("[商品新增]，新增商品，编码: {}，名称: {}", req.getCode(), req.getName());
        Long productId = productService.addProduct(req);
        return CommonResult.success(productId);
    }

    /**
     * 完整更新商品
     */
    @PutMapping("/{id}")
    public CommonResult<Void> update(
        @PathVariable Long id,
        @Valid @RequestBody ProductUpdateReq req
    ) {
        log.info("[商品修改]，完整更新，ID: {}", id);
        productService.updateProduct(id, req);
        return CommonResult.success();
    }

    /**
     * 部分更新：修改商品状态
     */
    @PatchMapping("/{id}/status")
    public CommonResult<Void> updateStatus(
        @PathVariable Long id,
        @RequestParam Integer status
    ) {
        log.info("[商品状态]，修改状态，ID: {}，状态: {}", id, status);
        productService.changeStatus(id, status);
        return CommonResult.success();
    }

    /**
     * 部分更新：修改商品库存
     */
    @PatchMapping("/{id}/stock")
    public CommonResult<Void> updateStock(
        @PathVariable Long id,
        @RequestParam Integer stock
    ) {
        log.info("[商品库存]，修改库存，ID: {}，库存: {}", id, stock);
        productService.updateStock(id, stock);
        return CommonResult.success();
    }

    /**
     * 删除商品
     */
    @DeleteMapping("/{id}")
    public CommonResult<Void> delete(@PathVariable Long id) {
        log.info("[商品删除]，删除商品，ID: {}", id);
        productService.deleteProduct(id);
        return CommonResult.success();
    }
}
```

---

## RESTful Controller 规范速查表

| 规范 | 要点 |
|------|------|
| **职责边界** | 只接收参数、调用 Service、返回结果 |
| **RESTful 设计** | 使用名词复数 `/products`、用 HTTP 方法表示操作 |
| **HTTP 方法** | GET 查询、POST 新增/复杂查询、PUT 完整更新、PATCH 部分更新、DELETE 删除 |
| **路径设计** | `/products/{id}` 资源路径，避免动词 `/products/get` |
| **幂等性** | GET/PUT/PATCH/DELETE 幂等，POST 非幂等 |
| **参数校验** | `@Valid @RequestBody` + 字段校验注解 |
| **嵌套校验** | 嵌套对象加 `@Valid` |
| **返回类型** | `CommonResult<T>` 统一返回 |
| **日志记录** | 接口入口记录关键参数 |
| **禁止操作** | ❌ 禁止注入 Mapper、❌ 禁止写业务逻辑 |

### RESTful HTTP 方法速查

| 操作 | HTTP 方法 | 路径示例 | 幂等性 | 说明 |
|------|----------|---------|--------|------|
| 查询单个 | **GET** | `/products/123` | ✅ 是 | 通过路径变量传递ID |
| 查询列表 | **GET** | `/products?status=1` | ✅ 是 | 简单条件用URL参数 |
| 复杂查询 | **POST** | `/products/query` | ❌ 否 | 复杂条件用请求体 |
| 分页查询 | **POST** | `/products/page` | ❌ 否 | 避免URL过长 |
| 新增 | **POST** | `/products` | ❌ 否 | 创建新资源 |
| 完整更新 | **PUT** | `/products/123` | ✅ 是 | 更新全部字段 |
| 部分更新 | **PATCH** | `/products/123/status` | ✅ 是 | 更新部分字段 |
| 删除 | **DELETE** | `/products/123` | ✅ 是 | 删除资源 |
