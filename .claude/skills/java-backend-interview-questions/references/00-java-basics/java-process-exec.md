# 如何在 Java 中调用外部可执行程序或系统命令？

## 三种方式

```
┌─────────────────────────────────────────────────────────────┐
│                    调用外部程序的方式                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. Runtime.exec()         传统方式，API 简单              │
│   2. ProcessBuilder          推荐方式，功能更强             │
│   3. ProcessHandle (Java 9+) 进程管理，获取进程信息         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## Runtime.exec()

```java
// 方式1: 简单命令
Process process = Runtime.getRuntime().exec("ping localhost");

// 方式2: 带参数的命令 (推荐用数组形式)
String[] cmd = {"ping", "-c", "3", "localhost"};
Process process = Runtime.getRuntime().exec(cmd);

// 方式3: 指定工作目录和环境变量
String[] cmd = {"ls", "-la"};
String[] envp = {"PATH=/usr/bin"};
File dir = new File("/home/user");
Process process = Runtime.getRuntime().exec(cmd, envp, dir);

// 读取输出
try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(process.getInputStream()))) {
    String line;
    while ((line = reader.readLine()) != null) {
        System.out.println(line);
    }
}

// 等待进程结束
int exitCode = process.waitFor();
System.out.println("Exit code: " + exitCode);
```

## ProcessBuilder (推荐)

```java
// 基本使用
ProcessBuilder pb = new ProcessBuilder("ping", "-c", "3", "localhost");

// 设置工作目录
pb.directory(new File("/home/user"));

// 合并错误流到标准输出
pb.redirectErrorStream(true);

// 重定向输出到文件
pb.redirectOutput(new File("output.txt"));

// 设置环境变量
Map<String, String> env = pb.environment();
env.put("MY_VAR", "value");

// 启动进程
Process process = pb.start();

// 读取输出
try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(process.getInputStream()))) {
    reader.lines().forEach(System.out::println);
}

// 等待完成并获取退出码
int exitCode = process.waitFor();
```

## 完整示例

```java
public class CommandExecutor {
    
    public static CommandResult execute(String... command) {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        
        StringBuilder output = new StringBuilder();
        int exitCode = -1;
        
        try {
            Process process = pb.start();
            
            // 读取输出 (必须读取，否则可能阻塞)
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            // 等待进程结束，设置超时
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();  // 超时强制终止
                throw new RuntimeException("Command timed out");
            }
            
            exitCode = process.exitValue();
            
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Command execution failed", e);
        }
        
        return new CommandResult(exitCode, output.toString());
    }
    
    record CommandResult(int exitCode, String output) {}
}

// 使用
CommandResult result = CommandExecutor.execute("ls", "-la");
System.out.println("Exit: " + result.exitCode());
System.out.println("Output: " + result.output());
```

## 注意事项

```
┌─────────────────────────────────────────────────────────────┐
│                    重要注意事项                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 必须读取输出流                                          │
│      └── 否则缓冲区满会导致进程阻塞                         │
│                                                             │
│   2. 同时读取 stdout 和 stderr                              │
│      └── 或使用 redirectErrorStream(true) 合并              │
│                                                             │
│   3. 设置超时                                                │
│      └── 防止进程无限等待                                   │
│                                                             │
│   4. 正确处理退出码                                          │
│      └── 0 通常表示成功，非 0 表示失败                      │
│                                                             │
│   5. 安全性                                                  │
│      └── 避免命令注入，不要拼接用户输入                     │
│                                                             │
│   6. 跨平台                                                  │
│      └── Windows 用 cmd /c，Linux 用 /bin/sh -c            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 跨平台执行

```java
public static Process executeCommand(String command) throws IOException {
    ProcessBuilder pb;
    String os = System.getProperty("os.name").toLowerCase();
    
    if (os.contains("win")) {
        pb = new ProcessBuilder("cmd", "/c", command);
    } else {
        pb = new ProcessBuilder("/bin/sh", "-c", command);
    }
    
    pb.redirectErrorStream(true);
    return pb.start();
}
```

## 面试回答

### 30秒版本

> 调用外部程序主要用 **ProcessBuilder**（推荐）或 **Runtime.exec()**。ProcessBuilder 功能更强，支持设置工作目录、环境变量、重定向输出。关键注意：**必须读取输出流**（否则阻塞）、设置超时、处理退出码。跨平台需区分 Windows(cmd /c) 和 Linux(/bin/sh -c)。

### 1分钟版本

> **两种方式**：
> - Runtime.exec()：简单但功能有限
> - ProcessBuilder：推荐，功能完整
>
> **ProcessBuilder 优势**：
> - 设置工作目录、环境变量
> - 重定向输入输出
> - 合并错误流
>
> **关键注意点**：
> - 必须读取输出流，否则阻塞
> - 设置超时防止无限等待
> - 处理退出码判断成功失败
>
> **安全性**：
> - 避免命令注入
> - 用数组形式传参，不要拼接字符串

---

*关联文档：[java-io-stream.md](java-io-stream.md)*
