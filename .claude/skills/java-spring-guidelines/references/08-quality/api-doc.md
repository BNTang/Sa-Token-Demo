# 接口文档

> Java/Spring Boot 编码规范 - 接口文档（Apifox）

---

## Apifox 注解规范

### Controller 类注释

```java
/**
 * 商品管理 Controller
 *
 * 提供商品的增删改查接口
 */
@RestController
@RequestMapping("/product")
@Validated
public class ProductController {

}
```

### 方法注释

```java
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
 * 查询商品详情
 *
 * @param req 查询请求
 * @return 商品详情
 */
@PostMapping("/detail")
public CommonResult<ProductDetailRsp> getDetail(@Valid @RequestBody ProductDetailReq req) {
    return CommonResult.success(productService.getDetail(req.getId()));
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

/**
 * 修改商品
 *
 * @param req 商品信息
 * @return 空
 */
@PostMapping("/update")
public CommonResult<Void> update(@Valid @RequestBody ProductUpdateReq req) {
    productService.updateProduct(req);
    return CommonResult.success();
}

/**
 * 删除商品
 *
 * @param req 删除请求
 * @return 空
 */
@PostMapping("/delete")
public CommonResult<Void> delete(@Valid @RequestBody ProductDeleteReq req) {
    productService.deleteProduct(req.getId());
    return CommonResult.success();
}
```

---

## 请求对象注释

### 字段注释

```java
@Data
public class ProductPageReq {

    /**
     * 商品名称（模糊匹配）
     * @mock 阿莫西林
     */
    private String name;

    /**
     * 商品编码
     * @mock PRD001
     */
    private String code;

    /**
     * 分类ID
     * @mock 1
     */
    private Long categoryId;

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

### 新增请求示例

```java
@Data
public class ProductAddReq {

    /**
     * 商品编码（必填）
     * @mock PRD001
     */
    @NotBlank(message = "商品编码不能为空")
    private String code;

    /**
     * 商品名称（必填）
     * @mock 感冒灵颗粒
     */
    @NotBlank(message = "商品名称不能为空")
    @Length(max = 100, message = "商品名称最长100字符")
    private String name;

    /**
     * 商品价格（必填）
     * @mock 29.80
     */
    @NotNull(message = "商品价格不能为空")
    @DecimalMin(value = "0.01", message = "商品价格必须大于0")
    private BigDecimal price;

    /**
     * 库存数量（必填）
     * @mock 1000
     */
    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能为负数")
    private Integer stock;

    /**
     * 分类ID（必填）
     * @mock 1
     */
    @NotNull(message = "分类不能为空")
    private Long categoryId;

    /**
     * 商品描述（可选）
     * @mock 这是一个好商品
     */
    private String description;
}
```

---

## 响应对象注释

### 单个对象响应

```java
@Data
public class ProductDetailRsp {

    /**
     * 商品ID
     * @mock 1
     */
    private Long id;

    /**
     * 商品编码
     * @mock PRD001
     */
    private String code;

    /**
     * 商品名称
     * @mock 阿莫西林
     */
    private String name;

    /**
     * 商品价格
     * @mock 29.80
     */
    private BigDecimal price;

    /**
     * 库存数量
     * @mock 1000
     */
    private Integer stock;

    /**
     * 销量
     * @mock 500
     */
    private Integer sales;

    /**
     * 状态：0-下架，1-上架
     * @mock 1
     */
    private Integer status;

    /**
     * 商品主图
     * @mock https://example.com/product.jpg
     */
    private String image;

    /**
     * 创建时间
     * @mock 2026-01-28 12:00:00
     */
    private LocalDateTime createTime;
}
```

### 分页响应

```java
@Data
public class ProductPageDTO {

    /**
     * 商品ID
     * @mock 1
     */
    private Long id;

    /**
     * 商品名称
     * @mock 阿莫西林
     */
    private String name;

    /**
     * 商品价格
     * @mock 29.80
     */
    private BigDecimal price;

    /**
     * 库存
     * @mock 1000
     */
    private Integer stock;

    /**
     * 状态
     * @mock 1
     */
    private Integer status;
}
```

---

## 特殊标签

### @mock 标签

用于生成示例数据：

```java
/**
 * 手机号
 * @mock 13800138000
 */
private String mobile;

/**
 * 日期
 * @mock 2026-01-28
 */
private LocalDate date;

/**
 * 金额
 * @mock 99.99
 */
private BigDecimal amount;
```

### @ignore 标签

忽略字段，不生成文档：

```java
/**
 * 创建时间（内部字段）
 * @ignore
 */
private LocalDateTime createTime;

/**
 * 更新人（内部字段）
 * @ignore
 */
private String updater;
```

### @deprecated 标签

标记已弃用：

```java
/**
 * 查询商品列表（已弃用，请使用 /page 接口）
 * @deprecated 请使用 getPage 接口
 * @ignore
 */
@PostMapping("/list")
public CommonResult<List<ProductDTO>> list(@RequestBody ProductQueryReq req) {
    return CommonResult.success(productService.list(req));
}
```

---

## 完整接口示例

```java
/**
 * 商品管理 Controller
 *
 * 提供商品的增删改查功能
 */
@Slf4j
@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
@Validated
public class ProductController {

    private final IProductService productService;

    /**
     * 分页查询商品
     *
     * 支持按名称、编码、分类、状态等条件查询
     *
     * @param req 查询条件
     * @return 商品分页列表
     */
    @PostMapping("/page")
    public CommonResult<IPage<ProductPageDTO>> getPage(@Valid @RequestBody ProductPageReq req) {
        log.info("[商品查询]，分页查询，条件: {}", req);
        return CommonResult.success(productService.getPage(req));
    }

    /**
     * 查询商品详情
     *
     * @param req 查询请求
     * @return 商品详情
     */
    @PostMapping("/detail")
    public CommonResult<ProductDetailRsp> getDetail(@Valid @RequestBody ProductDetailReq req) {
        return CommonResult.success(productService.getDetail(req.getId()));
    }

    /**
     * 新增商品
     *
     * @param req 商品信息
     * @return 商品ID
     */
    @PostMapping("/add")
    public CommonResult<Long> add(@Valid @RequestBody ProductAddReq req) {
        log.info("[商品新增]，开始新增，编码: {}，名称: {}", req.getCode(), req.getName());
        Long productId = productService.addProduct(req);
        return CommonResult.success(productId);
    }

    /**
     * 修改商品
     *
     * @param req 商品信息
     * @return 空
     */
    @PostMapping("/update")
    public CommonResult<Void> update(@Valid @RequestBody ProductUpdateReq req) {
        log.info("[商品修改]，开始修改，商品ID: {}", req.getId());
        productService.updateProduct(req);
        return CommonResult.success();
    }

    /**
     * 删除商品
     *
     * @param req 删除请求
     * @return 空
     */
    @PostMapping("/delete")
    public CommonResult<Void> delete(@Valid @RequestBody ProductDeleteReq req) {
        log.info("[商品删除]，开始删除，商品ID: {}", req.getId());
        productService.deleteProduct(req.getId());
        return CommonResult.success();
    }

    /**
     * 上架/下架
     *
     * @param req 请求
     * @return 空
     */
    @PostMapping("/change-status")
    public CommonResult<Void> changeStatus(@Valid @RequestBody ProductChangeStatusReq req) {
        log.info("[商品状态]，修改状态，商品ID: {}，状态: {}", req.getId(), req.getStatus());
        productService.changeStatus(req.getId(), req.getStatus());
        return CommonResult.success();
    }
}
```

---

## 接口文档规范速查表

| 规范 | 要点 |
|------|------|
| **类注释** | 描述 Controller 的职责 |
| **方法注释** | 说明功能、参数、返回值 |
| **字段注释** | 描述字段含义和格式 |
| **@mock** | 提供示例值 |
| **@ignore** | 忽略内部字段 |
| **@deprecated** | 标记已弃用接口 |
| **校验注解** | 添加 @NotNull、@Min 等注解 |
