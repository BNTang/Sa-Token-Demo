# OOP 规约

> Java/Spring Boot 编码规范 - 面向对象编程规约
> 参考：阿里巴巴 Java 开发手册

---

## 静态成员访问（强制）

**【强制】避免通过类的对象引用访问静态变量或静态方法，无谓增加编译器解析成本，直接用类名来访问。**

```java
// ❌ 反例 - 通过对象访问静态成员
User user = new User();
String format = user.DATE_FORMAT;         // 静态变量
String result = user.formatDate(date);    // 静态方法

// ✅ 正例 - 通过类名访问静态成员
String format = User.DATE_FORMAT;
String result = User.formatDate(date);
```

---

## equals 方法（强制）

**【强制】所有的相同类型的包装类对象之间值的比较，全部使用 equals 方法比较。**

> 说明：对于 Integer 在 -128 ~ 127 范围内的赋值，Integer 对象在 IntegerCache.cache 中产生，
> 会复用已有对象，此区间可用 == 比较，但区间之外都是新对象，用 == 会返回 false。

```java
// ❌ 反例 - 使用 == 比较包装类
Integer a = 128;
Integer b = 128;
if (a == b) {                             // false，超出缓存范围
    // ...
}

Integer c = 100;
Integer d = 100;
if (c == d) {                             // true，但依赖缓存，不推荐
    // ...
}

// ✅ 正例 - 使用 equals 比较
Integer a = 128;
Integer b = 128;
if (a.equals(b)) {                        // true
    // ...
}

// ✅ 正例 - 使用 Objects.equals 避免 NPE
if (Objects.equals(a, b)) {
    // ...
}
```

---

## 字符串比较（强制）

**【强制】对于字符串比较，应使用 equals 方法，并将常量放在前面，避免 NPE。**

```java
// ❌ 反例 - 可能 NPE
String status = getStatus();              // 可能返回 null
if (status.equals("SUCCESS")) {           // NPE!
    // ...
}

// ✅ 正例 - 常量在前
if ("SUCCESS".equals(status)) {           // 不会 NPE
    // ...
}

// ✅ 正例 - 使用 Objects.equals
if (Objects.equals(status, "SUCCESS")) {
    // ...
}

// ✅ 正例 - 使用常量类
if (StatusConstants.SUCCESS.equals(status)) {
    // ...
}
```

---

## Object 的 equals 方法（强制）

**【强制】Object 的 equals 方法容易抛空指针异常，应使用常量或确定有值的对象来调用 equals。**

```java
// ❌ 反例
public void checkUser(User user) {
    if (user.getName().equals("admin")) { // user 或 name 可能为 null
        // ...
    }
}

// ✅ 正例
public void checkUser(User user) {
    if (user != null && "admin".equals(user.getName())) {
        // ...
    }
}

// ✅ 正例 - Optional 处理
public void checkUser(User user) {
    Optional.ofNullable(user)
            .map(User::getName)
            .filter("admin"::equals)
            .ifPresent(name -> {
                // ...
            });
}
```

---

## 基本数据类型与包装类

### 使用规则

| 场景 | 类型 | 说明 |
|------|------|------|
| **POJO 类属性** | 包装类 | 避免默认值歧义 |
| **RPC 方法返回值/参数** | 包装类 | 避免 NPE 和序列化问题 |
| **局部变量** | 基本类型 | 性能更好 |

```java
// ❌ 反例 - POJO 使用基本类型
public class ProductDO {
    private long id;                      // 默认值 0，无法区分"未设置"和"值为0"
    private int stock;                    // 默认值 0
    private boolean deleted;              // 默认值 false
}

// ✅ 正例 - POJO 使用包装类
public class ProductDO {
    private Long id;                      // 默认值 null
    private Integer stock;                // 默认值 null
    private Boolean deleted;              // 默认值 null
}

// ✅ 正例 - 局部变量使用基本类型
public void calculate() {
    int sum = 0;                          // 局部变量用基本类型
    long count = 0L;
    for (int i = 0; i < 100; i++) {
        sum += i;
    }
}
```

### 自动拆装箱注意

```java
// ❌ 反例 - 自动拆箱 NPE
Integer count = null;
int value = count;                        // NPE!

// ✅ 正例 - 判空处理
Integer count = getCount();
int value = count != null ? count : 0;

// ✅ 正例 - Optional 处理
int value = Optional.ofNullable(getCount()).orElse(0);
```

---

## 构造方法规则

**【强制】构造方法里面禁止加入任何业务逻辑，如果有初始化逻辑，请放在 init 方法中。**

```java
// ❌ 反例 - 构造方法包含业务逻辑
public class OrderService {
    private Map<String, Order> cache;

    public OrderService() {
        this.cache = new HashMap<>();
        loadOrdersFromDatabase();         // 业务逻辑
        validateCache();                  // 业务逻辑
        sendNotification();               // 业务逻辑
    }
}

// ✅ 正例 - 使用 init 方法
public class OrderService {
    private Map<String, Order> cache;

    public OrderService() {
        this.cache = new HashMap<>();
    }

    @PostConstruct
    public void init() {
        loadOrdersFromDatabase();
        validateCache();
        sendNotification();
    }
}
```

---

## POJO 类规则

### toString 方法

**【推荐】POJO 类必须实现 toString 方法，便于日志输出和调试。**

```java
// ✅ 正例 - 使用 Lombok
@Data                                     // 自动生成 toString
public class ProductDTO {
    private Long id;
    private String name;
    private BigDecimal price;
}

// ✅ 正例 - 手动实现
public class ProductDTO {
    private Long id;
    private String name;

    @Override
    public String toString() {
        return "ProductDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
```

### equals 和 hashCode

**【强制】只要重写 equals，就必须重写 hashCode；Set 存储的对象、Map 的 key 必须重写这两个方法。**

```java
// ✅ 正例 - 使用 Lombok
@Data                                     // 自动生成 equals 和 hashCode
public class ProductDTO {
    private Long id;
    private String name;
}

// ✅ 正例 - 继承时使用
@Data
@EqualsAndHashCode(callSuper = true)      // 包含父类字段
public class ProductDO extends BaseDO {
    private Long id;
    private String name;
}
```

---

## 接口与实现类规则

### 接口方法签名

**【推荐】接口类中的方法和属性不要加任何修饰符号（public 也不要加），保持代码简洁。**

```java
// ❌ 反例
public interface IProductService {
    public abstract void save(Product product);
    public static final String VERSION = "1.0";
}

// ✅ 正例
public interface IProductService {
    void save(Product product);           // 默认 public abstract
    String VERSION = "1.0";               // 默认 public static final
}
```

### 实现类命名

| 场景 | 接口 | 实现类 |
|------|------|--------|
| Service 和 DAO 类 | `IProductService` | `ProductServiceImpl` |
| 能力型接口 | `Translatable` | `AbstractTranslator` |

```java
// ✅ 正例 - Service 接口和实现
public interface IProductService {
    Product getById(Long id);
}

@Service
public class ProductServiceImpl implements IProductService {
    @Override
    public Product getById(Long id) {
        // ...
    }
}

// ✅ 正例 - 能力型接口
public interface Translatable {
    String translate(String text);
}

public abstract class AbstractTranslator implements Translatable {
    // ...
}
```

---

## 覆写方法规则

**【强制】所有的覆写方法，必须加 @Override 注解。**

```java
// ❌ 反例 - 缺少 @Override
public class ProductServiceImpl implements IProductService {
    public Product getById(Long id) {     // 缺少 @Override
        // ...
    }
}

// ✅ 正例
public class ProductServiceImpl implements IProductService {
    @Override
    public Product getById(Long id) {
        // ...
    }
}
```

**说明：** @Override 可以准确判断是否覆盖成功，如果方法签名发生变化，编译器会报错。

---

## 可变参数规则

**【强制】相同参数类型，相同业务含义，才可以使用 Java 的可变参数，避免使用 Object。**

```java
// ❌ 反例 - 使用 Object 可变参数
public void process(Object... args) {     // 类型不安全
    // ...
}

// ✅ 正例 - 明确参数类型
public void addItems(String... items) {
    for (String item : items) {
        // ...
    }
}

// ✅ 正例 - 使用泛型
public <T> void addItems(List<T> items) {
    // ...
}
```

---

## 过期方法规则

**【强制】外部正在调用或者二方库依赖的接口，不允许修改方法签名，避免对接口调用方产生影响。**

**【推荐】接口过时必须加 @Deprecated 注解，并清晰说明新的接口或服务是什么。**

```java
// ✅ 正例
public interface IUserService {

    /**
     * 获取用户信息
     * @deprecated 使用 {@link #getUserById(Long)} 代替
     */
    @Deprecated
    User getUser(Long id);

    /**
     * 获取用户信息（新版）
     */
    User getUserById(Long id);
}
```

---

## 序列化规则

**【强制】序列化类新增属性时，不要修改 serialVersionUID 字段，避免反序列化失败。**

```java
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;  // 不要修改

    private Long id;
    private String name;
    private BigDecimal price;             // 新增字段不影响 serialVersionUID
}
```

---

## 集合规则

### subList 注意事项

**【强制】ArrayList 的 subList 结果不可强转成 ArrayList，否则会抛 ClassCastException。**

```java
// ❌ 反例
List<String> list = new ArrayList<>();
list.add("a");
list.add("b");
ArrayList<String> subList = (ArrayList<String>) list.subList(0, 1);  // ClassCastException

// ✅ 正例
List<String> subList = list.subList(0, 1);
// 或者创建新 ArrayList
List<String> newList = new ArrayList<>(list.subList(0, 1));
```

### Arrays.asList 注意事项

**【强制】使用 Arrays.asList() 把数组转换成集合时，不能使用其修改集合相关的方法。**

```java
// ❌ 反例
List<String> list = Arrays.asList("a", "b", "c");
list.add("d");                            // UnsupportedOperationException
list.remove(0);                           // UnsupportedOperationException

// ✅ 正例 - 需要修改时创建新 ArrayList
List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
list.add("d");                            // OK
```

---

## OOP 禁则速查

| ❌ 禁止 | ✅ 正确 | 原因 |
|--------|--------|------|
| 对象引用访问静态成员 | 类名访问静态成员 | 减少编译解析成本 |
| `==` 比较包装类 | `equals` 或 `Objects.equals` | 缓存范围外会返回 false |
| `object.equals(constant)` | `constant.equals(object)` | 避免 NPE |
| 构造方法含业务逻辑 | `@PostConstruct` init 方法 | 职责分离 |
| POJO 用基本类型 | POJO 用包装类 | 避免默认值歧义 |
| 覆写方法不加 @Override | 必须加 @Override | 编译期检查 |
| 修改过时接口签名 | 新增方法 + @Deprecated | 兼容性 |
| 强转 subList | 直接使用 List 类型 | 避免 ClassCastException |
| 修改 Arrays.asList | new ArrayList() 包装 | 避免 UnsupportedOperationException |
