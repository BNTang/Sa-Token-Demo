# Java 中 for 循环与 foreach 循环的区别是什么？

## 两种循环对比

```
┌─────────────────────────────────────────────────────────────┐
│                    for vs foreach                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   传统 for 循环:                                             │
│   for (int i = 0; i < arr.length; i++) {                    │
│       System.out.println(arr[i]);                           │
│   }                                                         │
│                                                             │
│   增强 for 循环 (foreach):                                   │
│   for (int num : arr) {                                     │
│       System.out.println(num);                              │
│   }                                                         │
│                                                             │
│   引入版本: Java 5                                          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 对比表

```
┌─────────────────────────────────────────────────────────────┐
│                    对比总结                                  │
├──────────────────┬──────────────────┬───────────────────────┤
│   特性           │   for            │   foreach             │
├──────────────────┼──────────────────┼───────────────────────┤
│   语法           │   复杂           │   简洁                │
│   索引访问       │   有索引         │   无索引              │
│   修改元素       │   可以           │   不能                │
│   删除元素       │   可以(需小心)   │   不能(抛异常)        │
│   遍历方向       │   正向/逆向      │   只能正向            │
│   适用对象       │   数组           │   数组、Iterable      │
│   底层实现       │   -              │   迭代器              │
└──────────────────┴──────────────────┴───────────────────────┘
```

## 代码对比

### 基本遍历

```java
int[] arr = {1, 2, 3, 4, 5};
List<String> list = Arrays.asList("A", "B", "C");

// for 循环
for (int i = 0; i < arr.length; i++) {
    System.out.println(arr[i]);
}

for (int i = 0; i < list.size(); i++) {
    System.out.println(list.get(i));
}

// foreach 循环 (更简洁)
for (int num : arr) {
    System.out.println(num);
}

for (String s : list) {
    System.out.println(s);
}
```

### 需要索引的场景

```java
// ✅ for 循环: 可以获取索引
for (int i = 0; i < arr.length; i++) {
    System.out.println("Index " + i + ": " + arr[i]);
}

// ❌ foreach: 没有索引
// 需要额外变量
int index = 0;
for (int num : arr) {
    System.out.println("Index " + index++ + ": " + num);
}
```

### 修改元素

```java
int[] arr = {1, 2, 3};

// ✅ for 循环: 可以修改
for (int i = 0; i < arr.length; i++) {
    arr[i] = arr[i] * 2;  // 直接修改数组
}

// ❌ foreach: 不能修改元素
for (int num : arr) {
    num = num * 2;  // 只修改了局部变量，数组不变!
}
```

### 删除元素

```java
List<String> list = new ArrayList<>(Arrays.asList("A", "B", "C"));

// ❌ foreach: 删除会抛 ConcurrentModificationException
for (String s : list) {
    if (s.equals("B")) {
        list.remove(s);  // 抛异常!
    }
}

// ✅ for 循环: 可以删除 (倒序遍历)
for (int i = list.size() - 1; i >= 0; i--) {
    if (list.get(i).equals("B")) {
        list.remove(i);  // 正确删除
    }
}

// ✅ 推荐: Iterator.remove()
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    if (it.next().equals("B")) {
        it.remove();
    }
}

// ✅ Java 8+: removeIf
list.removeIf(s -> s.equals("B"));
```

## foreach 底层原理

```java
// foreach 编译后的代码

// 数组的 foreach
int[] arr = {1, 2, 3};
for (int num : arr) {
    System.out.println(num);
}
// 编译后 (伪代码):
for (int i = 0; i < arr.length; i++) {
    int num = arr[i];
    System.out.println(num);
}

// 集合的 foreach
List<String> list = Arrays.asList("A", "B", "C");
for (String s : list) {
    System.out.println(s);
}
// 编译后 (使用迭代器):
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    String s = it.next();
    System.out.println(s);
}
```

## 使用建议

```
┌─────────────────────────────────────────────────────────────┐
│                    使用建议                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   使用 foreach:                                              │
│   ├── 只需遍历，不需要索引                                  │
│   ├── 不需要修改元素                                        │
│   └── 代码简洁优先                                          │
│                                                             │
│   使用 for:                                                  │
│   ├── 需要索引                                              │
│   ├── 需要修改元素                                          │
│   ├── 需要逆向遍历                                          │
│   ├── 需要同时遍历多个集合                                  │
│   └── 需要删除元素                                          │
│                                                             │
│   性能:                                                      │
│   • ArrayList: for 略快 (直接索引访问)                      │
│   • LinkedList: foreach 更快 (顺序访问)                     │
│   • 一般场景差异可忽略                                      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 面试回答

### 30秒版本

> **foreach** 语法简洁，底层用迭代器，适合只读遍历。**for** 循环有索引，可修改/删除元素，支持逆向遍历。foreach 不能修改元素（局部变量），遍历时删除会抛 ConcurrentModificationException。需要索引、修改、删除时用 for；只遍历用 foreach。

### 1分钟版本

> **语法**：
> - for：`for (int i = 0; i < n; i++)`
> - foreach：`for (元素 : 集合)`
>
> **区别**：
> - 索引：for 有，foreach 无
> - 修改：for 可以，foreach 不行
> - 删除：for 可以，foreach 抛异常
> - 方向：for 可逆向，foreach 只正向
>
> **底层**：
> - 数组 foreach → 普通 for
> - 集合 foreach → Iterator
>
> **选择**：
> - 只遍历 → foreach（简洁）
> - 需索引/修改/删除 → for

---

*关联文档：[java-iterator.md](java-iterator.md)*
