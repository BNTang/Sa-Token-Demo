# 集合处理规范

> Java/Spring Boot 编码规范 - 集合处理
> 参考：阿里巴巴 Java 开发手册

---

## ArrayList 使用规范

### 初始化容量

**【推荐】ArrayList 在初始化时指定集合初始大小。**

> 说明：ArrayList 默认容量 10，扩容时需要复制数组，影响性能。

```java
// ❌ 反例 - 不指定容量
List<User> users = new ArrayList<>();

// ✅ 正例 - 指定初始容量
List<User> users = new ArrayList<>(100);

// ✅ 正例 - 根据已知大小初始化
List<UserDTO> userDTOs = new ArrayList<>(userList.size());
```

### subList 陷阱

**【强制】ArrayList 的 subList 结果不可强转成 ArrayList，否则会抛出 ClassCastException。**

```java
// ❌ 反例 - 强转会抛异常
List<String> list = new ArrayList<>();
list.add("a");
list.add("b");
ArrayList<String> subList = (ArrayList<String>) list.subList(0, 1); // ClassCastException

// ✅ 正例
List<String> subList = list.subList(0, 1);
// 或创建新列表
List<String> newList = new ArrayList<>(list.subList(0, 1));
```

**【强制】subList 是原列表的视图，对其修改会影响原列表。**

```java
List<String> list = new ArrayList<>();
list.add("a");
list.add("b");
list.add("c");

List<String> subList = list.subList(0, 2);
subList.add("d");  // 会修改原列表！

System.out.println(list);     // [a, d, b, c]
System.out.println(subList);  // [a, d]
```

### Arrays.asList 陷阱

**【强制】使用 Arrays.asList() 把数组转换成集合时，不能使用其修改集合的方法。**

> 说明：asList 返回的是 Arrays 的内部类，没有实现 add/remove/clear 方法。

```java
// ❌ 反例 - 会抛 UnsupportedOperationException
List<String> list = Arrays.asList("a", "b", "c");
list.add("d");    // UnsupportedOperationException
list.remove(0);   // UnsupportedOperationException

// ✅ 正例 - 包装成 ArrayList
List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
list.add("d");    // OK

// ✅ 正例 - Java 9+ 使用 List.of（不可变）
List<String> immutableList = List.of("a", "b", "c");

// ✅ 正例 - 可变列表
List<String> mutableList = new ArrayList<>(List.of("a", "b", "c"));
```

---

## HashMap 使用规范

### 初始化容量

**【推荐】HashMap 在初始化时指定容量，避免扩容损耗。**

> 说明：初始容量 = 预期元素数量 / 0.75 + 1

```java
// ❌ 反例 - 默认容量 16
Map<Long, User> userMap = new HashMap<>();

// ✅ 正例 - 指定初始容量
// 预期存 100 个元素：100 / 0.75 + 1 = 134.33 → 取 2 的幂次 = 256
Map<Long, User> userMap = new HashMap<>(256);

// ✅ 正例 - 简化计算，预期容量 * 2
Map<Long, User> userMap = new HashMap<>(100 * 2);

// ✅ 正例 - 使用 Guava Maps
Map<Long, User> userMap = Maps.newHashMapWithExpectedSize(100);
```

### key 为自定义对象

**【强制】作为 HashMap 的 key 的对象，必须重写 hashCode 和 equals 方法。**

```java
// ❌ 反例 - 未重写 hashCode/equals
public class UserKey {
    private Long userId;
    private String type;
}

Map<UserKey, String> map = new HashMap<>();
map.put(new UserKey(1L, "VIP"), "data");
map.get(new UserKey(1L, "VIP"));  // 返回 null！

// ✅ 正例 - 使用 @Data 或手动重写
@Data
public class UserKey {
    private Long userId;
    private String type;
}

// 或使用 record（Java 14+）
public record UserKey(Long userId, String type) {}
```

---

## 集合遍历与删除

### 禁止 foreach 中修改

**【强制】不要在 foreach 循环里进行元素的 remove/add 操作，使用 Iterator 或 removeIf。**

```java
List<String> list = new ArrayList<>();
list.add("a");
list.add("b");
list.add("c");

// ❌ 反例 - 会抛 ConcurrentModificationException
for (String item : list) {
    if ("b".equals(item)) {
        list.remove(item);  // 异常！
    }
}

// ✅ 正例 - 使用 Iterator
Iterator<String> iterator = list.iterator();
while (iterator.hasNext()) {
    String item = iterator.next();
    if ("b".equals(item)) {
        iterator.remove();
    }
}

// ✅ 正例 - 使用 removeIf（Java 8+，推荐）
list.removeIf("b"::equals);

// ✅ 正例 - 使用 Stream filter
List<String> filtered = list.stream()
    .filter(item -> !"b".equals(item))
    .collect(Collectors.toList());
```

### 并发集合遍历

**【强制】在并发环境下，遍历 ConcurrentHashMap 时可以修改，但普通 Map 不行。**

```java
// ✅ ConcurrentHashMap 支持并发修改
ConcurrentHashMap<String, Integer> concurrentMap = new ConcurrentHashMap<>();
concurrentMap.put("a", 1);
concurrentMap.put("b", 2);

for (String key : concurrentMap.keySet()) {
    if (concurrentMap.get(key) == 1) {
        concurrentMap.remove(key);  // OK
    }
}

// ❌ 普通 HashMap 不支持
Map<String, Integer> map = new HashMap<>();
for (String key : map.keySet()) {
    map.remove(key);  // ConcurrentModificationException
}
```

---

## toArray 转换

**【强制】使用集合转数组的方法，必须使用集合的 toArray(T[] array)，传入类型完全一致、长度为 0 的空数组。**

```java
List<String> list = new ArrayList<>();
list.add("a");
list.add("b");

// ❌ 反例 - 直接 toArray() 返回 Object[]
Object[] array1 = list.toArray();

// ❌ 反例 - 指定长度（性能略差）
String[] array2 = list.toArray(new String[list.size()]);

// ✅ 正例 - 传入空数组（推荐，JVM 优化）
String[] array3 = list.toArray(new String[0]);

// ✅ 正例 - Java 11+ 使用方法引用
String[] array4 = list.toArray(String[]::new);
```

---

## 集合判空

**【推荐】判断集合是否为空，使用 isEmpty() 而非 size() == 0。**

```java
List<User> users = getUsers();

// ❌ 反例
if (users != null && users.size() > 0) {
    // ...
}

// ✅ 正例 - 使用 isEmpty()
if (users != null && !users.isEmpty()) {
    // ...
}

// ✅ 正例 - 使用 CollectionUtils（推荐）
if (CollectionUtils.isNotEmpty(users)) {
    // ...
}

// Map 同理
if (MapUtils.isNotEmpty(userMap)) {
    // ...
}
```

---

## 集合去重

**【推荐】利用 Set 元素唯一性进行去重。**

```java
List<Long> userIds = Arrays.asList(1L, 2L, 2L, 3L, 3L, 3L);

// ✅ 正例 - LinkedHashSet 保持顺序
List<Long> distinctIds = new ArrayList<>(new LinkedHashSet<>(userIds));

// ✅ 正例 - Stream distinct
List<Long> distinctIds2 = userIds.stream()
    .distinct()
    .collect(Collectors.toList());

// ✅ 正例 - 对象去重（按某字段）
List<User> distinctUsers = users.stream()
    .collect(Collectors.collectingAndThen(
        Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(User::getId))),
        ArrayList::new
    ));
```

---

## 集合转 Map

**【强制】使用 Collectors.toMap 时，必须处理 key 冲突和 null 值。**

```java
List<User> users = getUsers();

// ❌ 反例 - key 冲突会抛 IllegalStateException
Map<Long, User> userMap = users.stream()
    .collect(Collectors.toMap(User::getId, Function.identity()));

// ❌ 反例 - value 为 null 会抛 NullPointerException
Map<Long, String> nameMap = users.stream()
    .collect(Collectors.toMap(User::getId, User::getName)); // name 可能为 null

// ✅ 正例 - 处理 key 冲突
Map<Long, User> userMap = users.stream()
    .collect(Collectors.toMap(
        User::getId,
        Function.identity(),
        (existing, replacement) -> existing  // key 冲突时保留旧值
    ));

// ✅ 正例 - 处理 null 值
Map<Long, String> nameMap = users.stream()
    .filter(u -> u.getName() != null)
    .collect(Collectors.toMap(User::getId, User::getName));

// ✅ 正例 - 使用 Optional 处理
Map<Long, String> nameMap = users.stream()
    .collect(Collectors.toMap(
        User::getId,
        u -> Optional.ofNullable(u.getName()).orElse("")
    ));
```

---

## 不可变集合

**【推荐】返回集合时，考虑返回不可变集合防止被修改。**

```java
// ✅ 正例 - 返回不可变集合
public List<String> getSupportedTypes() {
    return Collections.unmodifiableList(Arrays.asList("A", "B", "C"));
}

// ✅ 正例 - Java 9+ List.of
public List<String> getSupportedTypes() {
    return List.of("A", "B", "C");
}

// ✅ 正例 - 返回副本
public List<User> getUsers() {
    return new ArrayList<>(this.userList);
}
```

---

## 禁则速查

| ❌ 禁止 | ✅ 正确 | 原因 |
|--------|--------|------|
| 强转 subList | 直接使用 List 类型 | ClassCastException |
| 修改 Arrays.asList | new ArrayList() 包装 | UnsupportedOperationException |
| foreach 中 remove | Iterator 或 removeIf | ConcurrentModificationException |
| toArray() 无参 | toArray(new T[0]) | 返回 Object[] |
| size() == 0 | isEmpty() | 语义更清晰 |
| toMap 不处理冲突 | 指定 mergeFunction | IllegalStateException |
| toMap value 为 null | 过滤或处理 null | NullPointerException |
| HashMap 不指定容量 | 指定初始容量 | 扩容影响性能 |
