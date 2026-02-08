# Java 中的深拷贝和浅拷贝有什么区别？

## 拷贝概念

```
┌─────────────────────────────────────────────────────────────┐
│                    深拷贝 vs 浅拷贝                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   浅拷贝 (Shallow Copy):                                     │
│   • 只复制对象本身，不复制引用的对象                        │
│   • 新旧对象共享引用类型字段                                │
│                                                             │
│   深拷贝 (Deep Copy):                                        │
│   • 复制对象本身及其引用的所有对象                          │
│   • 新旧对象完全独立                                        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 图示对比

```
┌─────────────────────────────────────────────────────────────┐
│                    浅拷贝示意图                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   原对象                      浅拷贝后                      │
│   ┌───────────────┐          ┌───────────────┐             │
│   │  User         │          │  User (copy)  │             │
│   │  name = "Tom" │  复制    │  name = "Tom" │  ← 基本类型 │
│   │  age = 18     │  ───→    │  age = 18     │    独立     │
│   │  addr ────────┼───┐      │  addr ────────┼───┐        │
│   └───────────────┘   │      └───────────────┘   │        │
│                       │                          │ 同一个  │
│                       ▼                          ▼        │
│                   ┌──────────────────────────────┐        │
│                   │  Address                      │        │
│                   │  city = "Beijing"             │        │
│                   └──────────────────────────────┘        │
│                                                             │
│   修改 copy.addr.city 会影响原对象!                         │
│                                                             │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    深拷贝示意图                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   原对象                      深拷贝后                      │
│   ┌───────────────┐          ┌───────────────┐             │
│   │  User         │          │  User (copy)  │             │
│   │  name = "Tom" │  复制    │  name = "Tom" │             │
│   │  age = 18     │  ───→    │  age = 18     │             │
│   │  addr ─────┐  │          │  addr ─────┐  │             │
│   └────────────┼──┘          └────────────┼──┘             │
│                ▼                          ▼                │
│        ┌──────────────┐          ┌──────────────┐         │
│        │  Address     │          │  Address     │  独立!  │
│        │  "Beijing"   │          │  "Beijing"   │         │
│        └──────────────┘          └──────────────┘         │
│                                                             │
│   修改 copy.addr.city 不影响原对象                          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 代码实现

### 浅拷贝

```java
// 1. Object.clone() 默认是浅拷贝
class User implements Cloneable {
    private String name;
    private Address addr;
    
    @Override
    public User clone() throws CloneNotSupportedException {
        return (User) super.clone();  // 浅拷贝
    }
}

User user1 = new User("Tom", new Address("Beijing"));
User user2 = user1.clone();

user2.getAddr().setCity("Shanghai");
System.out.println(user1.getAddr().getCity());  // Shanghai (被影响了!)
```

### 深拷贝方式一：手动实现

```java
class User implements Cloneable {
    private String name;
    private Address addr;
    
    @Override
    public User clone() throws CloneNotSupportedException {
        User cloned = (User) super.clone();
        // 手动深拷贝引用类型字段
        cloned.addr = this.addr.clone();
        return cloned;
    }
}

class Address implements Cloneable {
    private String city;
    
    @Override
    public Address clone() throws CloneNotSupportedException {
        return (Address) super.clone();
    }
}
```

### 深拷贝方式二：序列化

```java
// 通过序列化实现深拷贝
public static <T extends Serializable> T deepCopy(T obj) {
    try {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(obj);
        
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bis);
        return (T) ois.readObject();
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
}

// 要求对象及其引用的对象都实现 Serializable
```

### 深拷贝方式三：JSON 序列化

```java
// 使用 JSON 库 (如 Jackson、Gson)
User copy = objectMapper.readValue(
    objectMapper.writeValueAsString(original),
    User.class
);
```

## 深拷贝工具

```
┌─────────────────────────────────────────────────────────────┐
│                    常用深拷贝工具                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. Apache Commons BeanUtils                               │
│      BeanUtils.cloneBean(obj)                               │
│                                                             │
│   2. Spring BeanUtils                                        │
│      BeanUtils.copyProperties(source, target)  // 浅拷贝!   │
│                                                             │
│   3. Apache Commons Lang                                     │
│      SerializationUtils.clone(obj)  // 需实现 Serializable  │
│                                                             │
│   4. Jackson/Gson                                            │
│      JSON 序列化再反序列化                                  │
│                                                             │
│   5. MapStruct                                               │
│      编译时生成映射代码，高性能                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> **浅拷贝**只复制对象本身，引用类型字段共享同一对象，修改会相互影响。**深拷贝**复制对象及其所有引用的对象，完全独立。Object.clone() 默认是浅拷贝。深拷贝实现：1）手动 clone 所有引用字段；2）序列化反序列化；3）JSON 转换。

### 1分钟版本

> **浅拷贝**：
> - 只复制对象本身
> - 引用字段共享
> - Object.clone() 默认行为
>
> **深拷贝**：
> - 复制对象及所有引用对象
> - 新旧对象完全独立
>
> **实现方式**：
> - 手动：重写 clone()，逐个拷贝引用字段
> - 序列化：ObjectOutputStream/ObjectInputStream
> - JSON：Jackson/Gson 转换
>
> **注意**：
> - clone() 要实现 Cloneable
> - 序列化要实现 Serializable
> - String 等不可变类无需深拷贝

---

*关联文档：[java-serialization.md](java-serialization.md) | [java-immutable-class.md](java-immutable-class.md)*
