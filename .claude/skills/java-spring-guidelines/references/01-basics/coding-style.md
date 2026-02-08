# 代码风格

> Java/Spring Boot 编码规范 - 代码风格

---

## Import 规则

| 规则 | 说明 |
|------|------|
| 禁止全限定类名 | 必须先 import 后使用简单类名 |
| 静态成员 | 使用 `import static` |
| 导入顺序 | static → JDK → 第三方 → 本项目 |
| 通配符导入 | ❌ 禁止 `import xxx.*` |

```java
// ✅ 正确：按顺序导入
import static com.dsl.base.exception.util.ServiceExceptionUtil.exception;
import static com.dsl.base.exception.util.ServiceExceptionUtil.exception0;

import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;

import com.dsl.entity.Product;
import com.dsl.service.IProductService;

// ❌ 错误：使用全限定类名
public class ProductController {
    private com.dsl.service.IProductService productService;  // 不推荐
}

// ❌ 错误：通配符导入
import com.dsl.service.*;  // 禁止
```

---

## 依赖注入

### 构造器注入（推荐）

```java
// ✅ 正确：使用 @RequiredArgsConstructor + final 字段
@Service
@RequiredArgsConstructor  // Lombok 生成构造器
public class ProductService {

    private final ProductMapper productMapper;
    private final CategoryService categoryService;

    // 业务方法
}

// 等价于手动编写
@Service
public class ProductService {

    private final ProductMapper productMapper;
    private final CategoryService categoryService;

    public ProductService(ProductMapper productMapper, CategoryService categoryService) {
        this.productMapper = productMapper;
        this.categoryService = categoryService;
    }
}
```

### 字段注入（禁止新代码使用）

```java
// ❌ 禁止：新代码禁止字段注入
@Service
public class ProductService {

    @Autowired
    private ProductMapper productMapper;  // 不推荐
}

// ⚠️ 存量代码不强制修改
```

### Setter 注入（可选）

```java
// ⚠️ Setter 注入可用于可选依赖
@Service
public class ProductService {

    private ProductMapper productMapper;

    @Autowired
    public void setProductMapper(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }
}
```

---

## Lombok 使用

### 推荐注解

```java
// Data 类：实体、DTO、Req、Rsp
@Data                              // getter + setter + toString + equals + hashCode
@AllArgsConstructor                // 全参构造器
@NoArgsConstructor                 // 无参构造器
public class ProductDTO {
    private Long id;
    private String name;
    private BigDecimal price;
}

// Entity 类：继承 BaseDO 时使用
@Data
@EqualsAndHashCode(callSuper = true)  // equals/hashCode 包含父类字段
public class Product extends BaseDO {
    private Long id;
    private String name;
}

// Service 类：使用构造器注入
@Service
@RequiredArgsConstructor             // 生成 final 字段的构造器
@Slf4j                               // 生成 log 对象
public class ProductService {
    private final ProductMapper productMapper;
}

// 枚举类
@Getter
@AllArgsConstructor
public enum OrderStatus {
    PENDING(1, "待支付"),
    PAID(2, "已支付");

    private final Integer code;
    private final String desc;
}
```

### 禁止的注解

```java
// ❌ 禁止：@Data 与 @Builder 混用（导致无参构造器问题）
@Data
@Builder
public class ProductDTO {
    private Long id;
    private String name;
}

// ✅ 正确：使用 @Builder + @NoArgsConstructor
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private Long id;
    private String name;
}
```

---

## 对象转换

### BeanUtils.copyProperties

```java
// ✅ 好的做法：简单对象转换
public ProductDTO toDTO(Product entity) {
    ProductDTO dto = new ProductDTO();
    BeanUtils.copyProperties(entity, dto);
    return dto;
}

// ✅ 批量转换
public List<ProductDTO> toDTOList(List<Product> entities) {
    return entities.stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
}

// ⚠️ 注意：浅拷贝，嵌套对象需手动处理
public OrderDetailRsp toRsp(Order entity) {
    OrderDetailRsp rsp = new OrderDetailRsp();
    BeanUtils.copyProperties(entity, rsp);

    // 嵌套对象手动处理
    if (entity.getUser() != null) {
        UserInfoRsp userInfo = new UserInfoRsp();
        BeanUtils.copyProperties(entity.getUser(), userInfo);
        rsp.setUserInfo(userInfo);
    }

    return rsp;
}
```

### MapStruct（推荐复杂场景）

```java
@Mapper(componentModel = "spring")
public interface ProductConverter {

    ProductDTO toDTO(Product entity);
    Product toEntity(ProductDTO dto);

    List<ProductDTO> toDTOList(List<Product> entities);

    @Mapping(target = "createTime", format = "yyyy-MM-dd HH:mm:ss")
    ProductDetailRsp toDetailRsp(Product entity);

    @Mapping(target = "categoryName", source = "category.name")
    ProductWithCategoryRsp toWithCategoryRsp(Product entity);
}

// 使用
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductConverter productConverter;

    public ProductDTO getProductById(Long id) {
        Product entity = productMapper.selectById(id);
        return productConverter.toDTO(entity);
    }
}
```

---

## 空安全处理

### 集合判空

```java
// ✅ 好的做法：使用工具类
if (CollectionUtils.isEmpty(list)) {
    return Collections.emptyList();
}

if (MapUtils.isEmpty(map)) {
    return Collections.emptyMap();
}

if (StringUtils.isBlank(str)) {
    return "";
}
```

### Optional 使用

```java
// 链式调用防空
String city = Optional.ofNullable(user)
    .map(User::getAddress)
    .map(Address::getCity)
    .orElse("");

// 带默认值
Integer age = Optional.ofNullable(user)
    .map(User::getAge)
    .orElse(18);

// 抛出异常
Order order = orderMapper.selectById(orderId);
OrderStatus status = Optional.ofNullable(order)
    .map(Order::getStatus)
    .orElseThrow(() -> exception(ORDER_NOT_EXISTS));

// 条件操作
Optional.ofNullable(user)
    .ifPresent(u -> sendWelcomeEmail(u));
```

### 三元运算符

```java
// ✅ 好的做法：简单判断用三元
String result = (str != null) ? str : "";

// ✅ 好的做法：使用工具类
String result = StringUtils.defaultIfBlank(str, "");

// ✅ 好的做法：Optional
String result = Optional.ofNullable(str).orElse("");
```

---

## 集合操作

### 初始化

```java
// ✅ 不可变空集合
List<String> emptyList = Collections.emptyList();
Set<String> emptySet = Collections.emptySet();
Map<String, String> emptyMap = Collections.emptyMap();

// ✅ 不可变单元素集合
List<String> singleList = Collections.singletonList("value");
Set<String> singleSet = Collections.singleton("value");

// ✅ 可变集合
List<String> list = new ArrayList<>();
Set<String> set = new HashSet<>();
Map<String, String> map = new HashMap<>();

// ✅ 指定初始容量（避免扩容）
List<String> list = new ArrayList<>(1000);
Map<String, String> map = new HashMap<>(16);
```

### 遍历

```java
// ✅ forEach + Lambda（推荐）
orders.forEach(order -> processOrder(order));

// ✅ 方法引用
orders.forEach(this::processOrder);

// ✅ 增强 for 循环
for (Order order : orders) {
    processOrder(order);
}

// ❌ 禁止：传统 for 循环（除非需要索引）
for (int i = 0; i < orders.size(); i++) {
    Order order = orders.get(i);
}
```

### Stream 操作

```java
// ✅ 过滤
List<Product> activeProducts = products.stream()
    .filter(p -> p.getStatus() == 1)
    .collect(Collectors.toList());

// ✅ 映射
List<Long> ids = orders.stream()
    .map(Order::getId)
    .collect(Collectors.toList());

// ✅ 转换为 Map
Map<Long, Order> orderMap = orders.stream()
    .collect(Collectors.toMap(Order::getId, Function.identity()));

// ✅ 分组
Map<Integer, List<Product>> byCategory = products.stream()
    .collect(Collectors.groupingBy(Product::getCategoryId));

// ✅ 去重
List<Long> distinctIds = ids.stream()
    .distinct()
    .collect(Collectors.toList());

// ✅ 排序
List<Product> sorted = products.stream()
    .sorted(Comparator.comparing(Product::getPrice).reversed())
    .collect(Collectors.toList());
```

---

## 字符串处理

```java
// ✅ 判断空
if (StringUtils.isBlank(str)) { }     // null 或空串或空白
if (StringUtils.isNotEmpty(str)) { }  // 非空

// ✅ 判断相等
if (Objects.equals(str1, str2)) { }   // 推荐（允许 null）
if (str1.equals(str2)) { }            // 可能 NPE

// ✅ 格式化
String msg = String.format("用户 %s 的余额是 %.2f", name, balance);

// ✅ 拼接
String result = String.join(",", list);  // 集合拼接
String path = Paths.get("a", "b", "c").toString();  // 路径拼接

// ✅ 字符串比较
if ("ACTIVE".equals(status)) { }  // 常量放前面，避免 NPE
```

---

## 日期时间

```java
// ✅ 使用 Java 8+ 时间 API
LocalDate date = LocalDate.now();
LocalTime time = LocalTime.now();
LocalDateTime dateTime = LocalDateTime.now();

// ✅ 格式化
DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
String formatted = dateTime.format(formatter);

// ✅ 解析
LocalDateTime parsed = LocalDateTime.parse(str, formatter);

// ✅ 计算
LocalDate tomorrow = LocalDate.now().plusDays(1);
LocalDate nextWeek = LocalDate.now().plusWeeks(1);

// ✅ 时间范围判断
boolean isBetween = date.isAfter(start) && date.isBefore(end);

// ❌ 禁止：使用 Date 和 SimpleDateFormat
```

---

## 代码风格速查表

| 规范 | 要点 |
|------|------|
| **Import** | 禁止全限定类名，禁止通配符导入 |
| **依赖注入** | `@RequiredArgsConstructor` + `private final` |
| **Lombok** | 实体用 `@Data`，Service 用 `@RequiredArgsConstructor` |
| **对象转换** | `BeanUtils.copyProperties` 或 MapStruct |
| **集合判空** | `CollectionUtils.isEmpty()` / `MapUtils.isEmpty()` |
| **字符串** | `StringUtils.isBlank()` / `Objects.equals()` |
| **日期** | Java 8 `LocalDate` / `LocalDateTime` |
| **Optional** | 链式调用防空 |
