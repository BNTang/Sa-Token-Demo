# 测试规范

> Java/Spring Boot 编码规范 - 测试

---

## 测试类型

| 类型 | 命名 | 说明 | 是否必须 |
|-----|------|-----|---------|
| 单元测试 | `*Test.java` | 隔离外部依赖，纯逻辑测试 | **必须** |
| 集成测试 | `*IT.java` | 需要数据库、Redis 等真实环境 | 视环境 |

---

## 单元测试

### 基本结构

```java
/**
 * ProductService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private ProductService productService;

    @Test
    @DisplayName("根据ID查询商品 - 成功")
    void getProductById_success() {
        // given
        Long productId = 1L;
        Product product = new Product();
        product.setId(productId);
        product.setName("测试商品");

        when(productMapper.selectById(productId)).thenReturn(product);

        // when
        Product result = productService.getProductById(productId);

        // then
        assertNotNull(result);
        assertEquals(productId, result.getId());
        assertEquals("测试商品", result.getName());
        verify(productMapper).selectById(productId);
    }

    @Test
    @DisplayName("根据ID查询商品 - 不存在")
    void getProductById_notFound() {
        // given
        Long productId = 999L;
        when(productMapper.selectById(productId)).thenReturn(null);

        // when & then
        assertThrows(ServiceException.class, () -> {
            productService.getProductById(productId);
        });
    }
}
```

### Mock 使用

```java
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    @Test
    void testMock() {
        // 1. Mock 返回值
        when(productMapper.selectById(1L)).thenReturn(new Product());

        // 2. Mock 抛异常
        when(productMapper.selectById(999L))
            .thenThrow(new ServiceException("商品不存在"));

        // 3. Mock 链式调用
        when(productMapper.selectList(any()))
            .thenReturn(Collections.emptyList());

        // 4. Mock 多次调用
        when(productMapper.selectById(1L))
            .thenReturn(new Product())  // 第一次
            .thenReturn(null);           // 第二次

        // 5. Mock void 方法
        doNothing().when(productMapper).insert(any());

        // 6. 验证调用次数
        verify(productMapper, times(1)).selectById(1L);
        verify(productMapper, never()).deleteById(any());
    }
}
```

### 参数化测试

```java
@ParameterizedTest
@DisplayName("价格校验")
@CsvSource({
    "0.01, true",
    "100.00, true",
    "0.00, false",
    "-1.00, false"
})
void validatePrice(BigDecimal price, boolean expected) {
    assertEquals(expected, PriceValidator.isValid(price));
}

@ParameterizedTest
@DisplayName("状态枚举测试")
@EnumSource(OrderStatus.class)
void testOrderStatus(OrderStatus status) {
    assertNotNull(status.getCode());
    assertNotNull(status.getDesc());
}
```

---

## 集成测试

### 基本配置

```java
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.yml")
@Transactional
class OrderServiceIT {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderMapper orderMapper;

    @Test
    @DisplayName("创建订单集成测试")
    void createOrder() {
        // given
        OrderCreateReq req = new OrderCreateReq();
        req.setUserId(1L);
        req.setProductId(1L);
        req.setQuantity(2);

        // when
        Long orderId = orderService.createOrder(req);

        // then
        Order order = orderMapper.selectById(orderId);
        assertNotNull(order);
        assertEquals(1L, order.getUserId());
        assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatus());
    }
}
```

### 数据库测试

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(locations = "classpath:application-test.yml")
class ProductRepositoryTest {

    @Autowired
    private ProductMapper productMapper;

    @Test
    @DisplayName("保存商品")
    void saveProduct() {
        // given
        Product product = new Product();
        product.setName("测试商品");
        product.setPrice(new BigDecimal("99.99"));

        // when
        productMapper.insert(product);

        // then
        Product saved = productMapper.selectById(product.getId());
        assertNotNull(saved);
        assertEquals("测试商品", saved.getName());
    }
}
```

### MockMvc 控制器测试

```java
@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("分页查询接口测试")
    void getPage() throws Exception {
        mockMvc.perform(post("/product/page")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ProductPageReq())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.records").isArray());
    }
}
```

---

## 测试覆盖要求

### 必须覆盖的场景

| 场景 | 说明 |
|------|------|
| 正常流程 | happy path，输入正常数据 |
| 边界条件 | 空值、null、0、最大值、最小值 |
| 异常分支 | 参数错误、业务异常 |
| 依赖返回异常 | Mock 依赖抛异常 |

### 测试示例

```java
class ProductServiceTest {

    // 1. 正常流程
    @Test
    void addProduct_success() { }

    // 2. 边界条件
    @Test
    void addProduct_nullName() {
        assertThrows(ValidationException.class, () -> {
            productService.addProduct(new Product());
        });
    }

    @Test
    void addProduct_zeroPrice() {
        Product product = new Product();
        product.setName("测试");
        product.setPrice(BigDecimal.ZERO);

        assertThrows(ServiceException.class, () -> {
            productService.addProduct(product);
        });
    }

    // 3. 异常分支
    @Test
    void addProduct_duplicateCode() {
        when(productMapper.selectByCode("CODE001"))
            .thenReturn(new Product());

        assertThrows(ServiceException.class, () -> {
            productService.addProduct(buildProduct("CODE001"));
        });
    }

    // 4. 依赖异常
    @Test
    void addProduct_mapperException() {
        when(productMapper.insert(any()))
            .thenThrow(new RuntimeException("数据库异常"));

        assertThrows(ServiceException.class, () -> {
            productService.addProduct(buildProduct("CODE001"));
        });
    }
}
```

---

## 测试命名规范

### 方法命名

```java
// 格式：方法名_场景_预期结果
void getProductById_success_returnProduct()
void getProductById_notFound_throwException()
void getProductById_nullId_throwIllegalArgumentException()

// 或简洁格式
void getProductById_success()
void getProductById_notFound()
void getProductById_nullId()
```

### 测试类命名

```
ProductServiceTest.java      # Service 测试
ProductControllerTest.java   # Controller 测试
ProductMapperTest.java       # Mapper 测试
OrderServiceIT.java          # 集成测试
```

---

## 常用断言

```java
@Test
void assertions() {
    // 对象断言
    assertNotNull(object);
    assertNull(object);
    assertEquals(expected, actual);
    assertNotEquals(expected, actual);
    assertSame(expected, actual);      // 同一引用
    assertNotSame(expected, actual);

    // 布尔断言
    assertTrue(condition);
    assertFalse(condition);

    // 异常断言
    assertThrows(ExceptionType.class, () -> {
        // 触发异常的代码
    });

    // 数组断言
    assertArrayEquals(expected, actual);

    // 集合断言
    assertEquals(3, list.size());

    // 多重断言（全部执行后统一报告）
    assertAll("product",
        () -> assertEquals("商品名称", product.getName()),
        () -> assertEquals(new BigDecimal("99.99"), product.getPrice()),
        () -> assertTrue(product.getStock() > 0)
    );
}
```

---

## 测试规范速查表

| 规范 | 要点 |
|------|------|
| **命名** | `方法名_场景_预期结果` |
| **结构** | given-when-then |
| **Mock** | 使用 @Mock + @InjectMocks |
| **覆盖** | 正常、边界、异常都要测 |
| **隔离** | 单元测试不应依赖外部环境 |
| **回滚** | 集成测试使用 @Transactional 回滚 |
| **断言** | 使用 assertAll 批量断言 |
